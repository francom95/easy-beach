'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  actualizarBranding,
  obtenerBrandingPropio,
  subirAssetBranding,
  type AssetType,
  type BrandingUpdateInput,
  type TypographyFamily,
} from '../../../api/branding';
import { assetUrl } from '../../../api/config';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { EstadoCargando } from '../../../components/EstadoCarga';
import { useToast } from '../../../components/Toast';
import { PAREJAS } from './fonts';
import styles from './page.module.css';

const COLOR_CAMPOS: { key: keyof BrandingUpdateInput; label: string }[] = [
  { key: 'colorPrimary', label: 'Primario' },
  { key: 'colorSecondary', label: 'Secundario' },
  { key: 'colorBackground', label: 'Fondo' },
  { key: 'colorSurface', label: 'Superficie' },
  { key: 'colorSuccess', label: 'Éxito' },
  { key: 'colorWarning', label: 'Advertencia' },
  { key: 'colorError', label: 'Error' },
  { key: 'colorInfo', label: 'Info' },
];

const TIPOGRAFIAS: TypographyFamily[] = ['clara', 'amigable', 'elegante', 'energica'];

const ASSET_TILES: { tipo: AssetType; label: string; tokenKey: string }[] = [
  { tipo: 'LOGO', label: 'Logo', tokenKey: 'asset.logo' },
  { tipo: 'LOGO_COMPACT', label: 'Logo compacto', tokenKey: 'asset.logo-compact' },
  { tipo: 'COVER', label: 'Portada', tokenKey: 'asset.cover' },
  { tipo: 'SPLASH', label: 'Splash', tokenKey: 'asset.splash' },
];

export default function IdentidadVisualPage() {
  const { data, isLoading } = useQuery({ queryKey: ['mi-branding'], queryFn: obtenerBrandingPropio });
  const queryClient = useQueryClient();
  const { mostrar } = useToast();

  // Estado editable derivado de `data`: sin efecto (react-hooks/set-state-in-effect
  // lo marca como cascading render) - se calcula en render, y `formOverride` guarda
  // solo lo que el admin ya tocó, para no pisar sus ediciones en cada refetch.
  const [formOverride, setFormOverride] = useState<BrandingUpdateInput | null>(null);
  const [sugerencias, setSugerencias] = useState<Record<string, string> | null>(null);

  const form = formOverride ?? (data ? formInicialDesde(data) : null);
  const setForm = setFormOverride;

  const guardarMut = useMutation({
    mutationFn: (input: BrandingUpdateInput) => actualizarBranding(input),
    onSuccess: resultado => {
      if (resultado.aplicado) {
        mostrar('Theme guardado y publicado', 'exito');
        setSugerencias(null);
        queryClient.invalidateQueries({ queryKey: ['mi-branding'] });
      } else {
        setSugerencias(resultado.ajustesPropuestos);
        mostrar('Algunos colores no cumplen contraste — revisá la sugerencia', 'info');
      }
    },
    onError: () => mostrar('No pudimos guardar el theme', 'error'),
  });

  const subirAssetMut = useMutation({
    mutationFn: ({ tipo, file }: { tipo: AssetType; file: File }) => subirAssetBranding(tipo, file),
    onSuccess: () => {
      mostrar('Imagen actualizada', 'exito');
      queryClient.invalidateQueries({ queryKey: ['mi-branding'] });
    },
    onError: () => mostrar('No pudimos subir la imagen (¿es PNG/JPEG/SVG?)', 'error'),
  });

  if (isLoading || !form) return <EstadoCargando texto="Cargando identidad visual…" />;

  function aceptarSugerencia() {
    if (!sugerencias || !form) return;
    setForm({ ...form, ...mapearSugerenciasAForm(sugerencias), aceptarSugerencia: true });
    guardarMut.mutate({ ...form, ...mapearSugerenciasAForm(sugerencias), aceptarSugerencia: true });
  }

  const pareja = PAREJAS[form.typographyFamily];

  return (
    <div className={styles.wrap}>
      <h1 className={styles.titulo}>Identidad visual</h1>
      <div className={styles.layout}>
        <Card className={styles.editor}>
          <label className={styles.campo}>
            Nombre del theme
            <input
              className={styles.input}
              value={form.themeName}
              onChange={e => setForm({ ...form, themeName: e.target.value })}
            />
          </label>

          <div className={styles.colorGrid}>
            {COLOR_CAMPOS.map(c => (
              <label key={c.key} className={styles.colorCampo}>
                <span>{c.label}</span>
                <div className={styles.colorInputRow}>
                  <input
                    type="color"
                    value={form[c.key] as string}
                    onChange={e => setForm({ ...form, [c.key]: e.target.value.toUpperCase() })}
                  />
                  <input
                    className={styles.hexInput}
                    value={form[c.key] as string}
                    onChange={e => setForm({ ...form, [c.key]: e.target.value })}
                  />
                </div>
              </label>
            ))}
          </div>

          <label className={styles.campo}>
            Tipografía
            <select
              className={styles.input}
              value={form.typographyFamily}
              onChange={e => setForm({ ...form, typographyFamily: e.target.value as TypographyFamily })}
            >
              {TIPOGRAFIAS.map(t => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>

          {sugerencias ? (
            <div className={styles.sugerenciaBanner}>
              <strong>Te propusimos el tono más cercano que sí cumple contraste:</strong>
              <ul>
                {Object.entries(sugerencias).map(([k, v]) => (
                  <li key={k}>
                    {k}: <code>{v}</code>
                  </li>
                ))}
              </ul>
              <Button size="md" onClick={aceptarSugerencia}>
                Aceptar sugerencia y guardar
              </Button>
            </div>
          ) : null}

          <div className={styles.assetGrid}>
            {ASSET_TILES.map(a => (
              <label key={a.tipo} className={styles.assetTile}>
                <span>{a.label}</span>
                {data?.[a.tokenKey] ? (
                  // eslint-disable-next-line @next/next/no-img-element
                  <img src={assetUrl(String(data[a.tokenKey]))} alt={a.label} className={styles.assetPreview} />
                ) : (
                  <div className={styles.assetPlaceholder}>Default EasyBeach</div>
                )}
                <input
                  type="file"
                  accept="image/png,image/jpeg,image/svg+xml"
                  onChange={e => {
                    const file = e.target.files?.[0];
                    if (file) subirAssetMut.mutate({ tipo: a.tipo, file });
                  }}
                />
              </label>
            ))}
          </div>

          <Button
            fullWidth
            size="lg"
            onClick={() => guardarMut.mutate(form)}
            cargando={guardarMut.isPending}
          >
            Guardar y publicar theme
          </Button>
        </Card>

        <div className={styles.previewFrame} style={{ background: form.colorBackground }}>
          <div className={styles.previewHeader} style={{ background: form.colorSecondary }}>
            <span style={{ fontFamily: pareja.display, color: '#fff', fontWeight: 700 }}>{form.themeName || 'Mi Balneario'}</span>
          </div>
          <div className={styles.previewBody}>
            <div className={styles.previewCard} style={{ background: form.colorSurface, borderColor: form.colorBackground }}>
              <span style={{ fontFamily: pareja.ui, color: '#1a1a1a' }}>Cerveza bien fría</span>
              <span style={{ fontFamily: pareja.ui, color: form.colorPrimary, fontWeight: 700 }}>$2.000</span>
            </div>
            <button
              className={styles.previewBtn}
              style={{ background: form.colorPrimary, fontFamily: pareja.ui }}
            >
              Agregar al carrito
            </button>
            <span className={styles.previewNote} style={{ fontFamily: pareja.ui }}>
              Vista previa — así lo ve el cliente en la app
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

function formInicialDesde(data: Record<string, unknown>): BrandingUpdateInput {
  return {
    themeName: String(data['theme.name'] ?? ''),
    colorPrimary: String(data['color.primary'] ?? '#C95100'),
    colorSecondary: String(data['color.secondary'] ?? '#17437B'),
    colorBackground: String(data['color.background'] ?? '#F5EFE2'),
    colorSurface: String(data['color.surface'] ?? '#FFFFFF'),
    colorSuccess: String(data['color.success'] ?? '#1E7D3C'),
    colorWarning: String(data['color.warning'] ?? '#B25E00'),
    colorError: String(data['color.error'] ?? '#C22F2F'),
    colorInfo: String(data['color.info'] ?? '#1D62B4'),
    typographyFamily: (data['typography.family'] as TypographyFamily) ?? 'clara',
    aceptarSugerencia: false,
  };
}

function mapearSugerenciasAForm(sugerencias: Record<string, string>): Partial<BrandingUpdateInput> {
  const mapa: Record<string, keyof BrandingUpdateInput> = {
    'color.primary': 'colorPrimary',
    'color.secondary': 'colorSecondary',
    'color.background': 'colorBackground',
    'color.surface': 'colorSurface',
    'color.success': 'colorSuccess',
    'color.warning': 'colorWarning',
    'color.error': 'colorError',
    'color.info': 'colorInfo',
  };
  const resultado: Partial<BrandingUpdateInput> = {};
  for (const [k, v] of Object.entries(sugerencias)) {
    const campo = mapa[k];
    if (campo) (resultado as Record<string, string>)[campo] = v;
  }
  return resultado;
}
