'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  cambiarEstadoPromocion,
  crearPromocion,
  eliminarPromocion,
  listarPromociones,
} from '../../../api/promociones';
import { listarCategorias, listarProductos } from '../../../api/catalogo';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { Badge } from '../../../components/Badge';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import { EmptyState } from '../../../components/EmptyState';
import { useToast } from '../../../components/Toast';
import { ApiError } from '../../../api/ApiError';
import type { AlcancePromocion, ComboItem, PromocionInput, TipoPromocion } from '../../../api/types';
import styles from './page.module.css';

const DIAS = ['LUN', 'MAR', 'MIE', 'JUE', 'VIE', 'SAB', 'DOM'];

const TIPO_LABEL: Record<TipoPromocion, string> = {
  DESCUENTO_PORCENTUAL: 'Descuento %',
  COMBO: 'Combo',
  HAPPY_HOUR: 'Happy hour',
};

function promocionVacia(tipo: TipoPromocion): PromocionInput {
  return {
    nombre: '',
    tipo,
    activa: true,
    valor: '0',
    vigenciaDesde: null,
    vigenciaHasta: null,
    franjaHoraDesde: null,
    franjaHoraHasta: null,
    diasSemana: null,
    alcances: [],
    comboItems: [],
  };
}

export default function PromocionesPage() {
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['promociones'], queryFn: listarPromociones });
  const categoriasQuery = useQuery({ queryKey: ['categorias'], queryFn: listarCategorias });
  const productosQuery = useQuery({ queryKey: ['productos'], queryFn: listarProductos });
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const [creando, setCreando] = useState(false);
  const [form, setForm] = useState<PromocionInput>(promocionVacia('DESCUENTO_PORCENTUAL'));

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['promociones'] });

  const crearMut = useMutation({
    mutationFn: () => crearPromocion(form),
    onSuccess: () => {
      mostrar('Promoción creada', 'exito');
      setCreando(false);
      setForm(promocionVacia('DESCUENTO_PORCENTUAL'));
      invalidar();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos crear la promoción', 'error'),
  });

  const toggleMut = useMutation({
    mutationFn: ({ id, activa }: { id: number; activa: boolean }) => cambiarEstadoPromocion(id, activa ? 'ACTIVA' : 'INACTIVA'),
    onSuccess: invalidar,
  });

  const eliminarMut = useMutation({
    mutationFn: (id: number) => eliminarPromocion(id),
    onSuccess: invalidar,
    onError: () => mostrar('No pudimos eliminar la promoción', 'error'),
  });

  if (isLoading) return <EstadoCargando texto="Cargando promociones…" />;
  if (isError) return <EstadoError mensaje="No pudimos cargar las promociones." onReintentar={() => refetch()} />;

  const promociones = data ?? [];
  const categorias = categoriasQuery.data ?? [];
  const productos = productosQuery.data ?? [];

  function toggleDia(dia: string) {
    const actuales = form.diasSemana ? form.diasSemana.split(',') : [];
    const nuevos = actuales.includes(dia) ? actuales.filter(d => d !== dia) : [...actuales, dia];
    setForm({ ...form, diasSemana: nuevos.length ? nuevos.join(',') : null });
  }

  function agregarAlcanceCategoria(id: number) {
    const nuevo: AlcancePromocion = { tipoAlcance: 'CATEGORIA', referenciaId: id };
    setForm({ ...form, alcances: [...(form.alcances ?? []), nuevo] });
  }

  function agregarComboItem(productoId: number) {
    const nuevo: ComboItem = { productoId, cantidad: 1 };
    setForm({ ...form, comboItems: [...(form.comboItems ?? []), nuevo] });
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <h1 className={styles.titulo}>Promociones</h1>
        <Button onClick={() => setCreando(true)}>+ Nueva promoción</Button>
      </div>

      {promociones.length === 0 ? (
        <EmptyState titulo="Sin promociones" descripcion="Creá una para incentivar horarios de poca venta." />
      ) : (
        <div className={styles.lista}>
          {promociones.map(p => (
            <Card key={p.id} className={styles.fila}>
              <div className={styles.filaInfo}>
                <strong>{p.nombre}</strong>
                <span className={styles.tipoChico}>{TIPO_LABEL[p.tipo]}</span>
              </div>
              <Badge tono={p.estado === 'ACTIVA' ? 'exito' : 'neutro'}>{p.estado}</Badge>
              <label className={styles.switch}>
                <input
                  type="checkbox"
                  checked={p.estado === 'ACTIVA'}
                  onChange={e => toggleMut.mutate({ id: p.id, activa: e.target.checked })}
                />
                <span className={styles.slider} />
              </label>
              <button className={styles.borrarChico} onClick={() => eliminarMut.mutate(p.id)}>
                Eliminar
              </button>
            </Card>
          ))}
        </div>
      )}

      {creando ? (
        <div className={styles.modalOverlay} onClick={() => setCreando(false)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>Nueva promoción</h3>

            <div className={styles.tipoCards}>
              {(Object.keys(TIPO_LABEL) as TipoPromocion[]).map(t => (
                <button
                  key={t}
                  className={[styles.tipoCard, form.tipo === t ? styles.tipoCardActiva : ''].join(' ')}
                  onClick={() => setForm(promocionVacia(t))}
                >
                  {TIPO_LABEL[t]}
                </button>
              ))}
            </div>

            <input className={styles.input} placeholder="Nombre" value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} />

            {form.tipo !== 'COMBO' ? (
              <label className={styles.campo}>
                Descuento (%)
                <input
                  className={styles.input}
                  type="number"
                  value={form.valor}
                  onChange={e => setForm({ ...form, valor: e.target.value })}
                />
              </label>
            ) : (
              <label className={styles.campo}>
                Precio del combo
                <input
                  className={styles.input}
                  type="number"
                  value={form.valor}
                  onChange={e => setForm({ ...form, valor: e.target.value })}
                />
              </label>
            )}

            {form.tipo === 'HAPPY_HOUR' ? (
              <>
                <div className={styles.franja}>
                  <label className={styles.campo}>
                    Desde
                    <input
                      className={styles.input}
                      type="time"
                      value={form.franjaHoraDesde ?? ''}
                      onChange={e => setForm({ ...form, franjaHoraDesde: e.target.value || null })}
                    />
                  </label>
                  <label className={styles.campo}>
                    Hasta
                    <input
                      className={styles.input}
                      type="time"
                      value={form.franjaHoraHasta ?? ''}
                      onChange={e => setForm({ ...form, franjaHoraHasta: e.target.value || null })}
                    />
                  </label>
                </div>
                <div className={styles.dias}>
                  {DIAS.map(d => (
                    <button
                      key={d}
                      className={[styles.diaChip, form.diasSemana?.includes(d) ? styles.diaChipActivo : ''].join(' ')}
                      onClick={() => toggleDia(d)}
                    >
                      {d}
                    </button>
                  ))}
                </div>
              </>
            ) : null}

            {form.tipo !== 'COMBO' ? (
              <label className={styles.campo}>
                Aplica a categoría
                <select
                  className={styles.input}
                  onChange={e => e.target.value && agregarAlcanceCategoria(Number(e.target.value))}
                  defaultValue=""
                >
                  <option value="" disabled>
                    Elegir categoría…
                  </option>
                  {categorias.map(c => (
                    <option key={c.id} value={c.id}>
                      {c.nombre}
                    </option>
                  ))}
                </select>
                <div className={styles.chipsSeleccionados}>
                  {(form.alcances ?? []).map((a, i) => (
                    <span key={i} className={styles.chipSeleccionado}>
                      {categorias.find(c => c.id === a.referenciaId)?.nombre ?? a.referenciaId}
                      <button
                        onClick={() => setForm({ ...form, alcances: (form.alcances ?? []).filter((_, idx) => idx !== i) })}
                      >
                        ✕
                      </button>
                    </span>
                  ))}
                </div>
              </label>
            ) : (
              <label className={styles.campo}>
                Productos del combo
                <select className={styles.input} onChange={e => e.target.value && agregarComboItem(Number(e.target.value))} defaultValue="">
                  <option value="" disabled>
                    Agregar producto…
                  </option>
                  {productos.map(p => (
                    <option key={p.id} value={p.id}>
                      {p.nombre}
                    </option>
                  ))}
                </select>
                <div className={styles.chipsSeleccionados}>
                  {(form.comboItems ?? []).map((c, i) => (
                    <span key={i} className={styles.chipSeleccionado}>
                      {c.cantidad}× {productos.find(p => p.id === c.productoId)?.nombre ?? c.productoId}
                      <button
                        onClick={() => setForm({ ...form, comboItems: (form.comboItems ?? []).filter((_, idx) => idx !== i) })}
                      >
                        ✕
                      </button>
                    </span>
                  ))}
                </div>
              </label>
            )}

            <div className={styles.preview}>
              <span className={styles.previewLabel}>Vista previa para el cliente</span>
              <strong>{form.nombre || 'Nombre de la promoción'}</strong>
              <span>
                {form.tipo === 'COMBO' ? `Combo por ${form.valor || 0}` : `${form.valor || 0}% de descuento`}
              </span>
            </div>

            <div className={styles.modalAcciones}>
              <Button variant="ghost" onClick={() => setCreando(false)}>
                Cancelar
              </Button>
              <Button onClick={() => crearMut.mutate()} cargando={crearMut.isPending} disabled={!form.nombre.trim()}>
                Crear
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
