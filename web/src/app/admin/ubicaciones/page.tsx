'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  cambiarEstadoUbicacion,
  crearUbicacion,
  crearUbicacionesMasivo,
  eliminarUbicacion,
  listarUbicaciones,
} from '../../../api/ubicaciones';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { Badge } from '../../../components/Badge';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import { EmptyState } from '../../../components/EmptyState';
import { useToast } from '../../../components/Toast';
import { ApiError } from '../../../api/ApiError';
import type { TipoUbicacion } from '../../../api/types';
import styles from './page.module.css';

const TIPOS: TipoUbicacion[] = ['CARPA', 'SOMBRILLA', 'MESA', 'SECTOR'];

export default function UbicacionesPage() {
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['ubicaciones'], queryFn: listarUbicaciones });
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const [filtro, setFiltro] = useState<TipoUbicacion | null>(null);
  const [mostrarAlta, setMostrarAlta] = useState(false);
  const [tipo, setTipo] = useState<TipoUbicacion>('SOMBRILLA');
  const [identificador, setIdentificador] = useState('');
  const [masivo, setMasivo] = useState(false);
  const [prefijo, setPrefijo] = useState('S-');
  const [desde, setDesde] = useState(1);
  const [hasta, setHasta] = useState(10);

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['ubicaciones'] });

  const crearMut = useMutation({
    mutationFn: () => (masivo ? crearUbicacionesMasivo(tipo, prefijo, desde, hasta) : crearUbicacion(tipo, identificador).then(u => [u])),
    onSuccess: () => {
      mostrar('Ubicación creada', 'exito');
      setIdentificador('');
      setMostrarAlta(false);
      invalidar();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos crear la ubicación', 'error'),
  });

  const toggleEstadoMut = useMutation({
    mutationFn: ({ id, activa }: { id: number; activa: boolean }) => cambiarEstadoUbicacion(id, activa ? 'ACTIVA' : 'INACTIVA'),
    onSuccess: invalidar,
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos cambiar el estado', 'error'),
  });

  const eliminarMut = useMutation({
    mutationFn: (id: number) => eliminarUbicacion(id),
    onSuccess: invalidar,
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos eliminar la ubicación', 'error'),
  });

  if (isLoading) return <EstadoCargando texto="Cargando ubicaciones…" />;
  if (isError) return <EstadoError mensaje="No pudimos cargar las ubicaciones." onReintentar={() => refetch()} />;

  const ubicaciones = data ?? [];
  const visibles = filtro ? ubicaciones.filter(u => u.tipo === filtro) : ubicaciones;

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <h1 className={styles.titulo}>Ubicaciones</h1>
        <Button onClick={() => setMostrarAlta(true)}>+ Ubicación</Button>
      </div>

      <div className={styles.chips}>
        <button className={[styles.chip, filtro === null ? styles.chipActivo : ''].join(' ')} onClick={() => setFiltro(null)}>
          Todas
        </button>
        {TIPOS.map(t => (
          <button key={t} className={[styles.chip, filtro === t ? styles.chipActivo : ''].join(' ')} onClick={() => setFiltro(t)}>
            {t}
          </button>
        ))}
      </div>

      {visibles.length === 0 ? (
        <EmptyState titulo="Sin ubicaciones" descripcion="Agregá la primera para que los clientes puedan elegirla." />
      ) : (
        <div className={styles.grid}>
          {visibles.map(u => (
            <Card key={u.id} className={styles.tarjeta}>
              <div className={styles.tarjetaTop}>
                <strong>{u.identificador}</strong>
                <Badge tono={u.estado === 'ACTIVA' ? 'exito' : 'neutro'}>{u.estado}</Badge>
              </div>
              <div className={styles.tipoTexto}>{u.tipo}</div>
              <div className={styles.tarjetaAcciones}>
                <label className={styles.switch}>
                  <input
                    type="checkbox"
                    checked={u.estado === 'ACTIVA'}
                    onChange={e => toggleEstadoMut.mutate({ id: u.id, activa: e.target.checked })}
                  />
                  <span className={styles.slider} />
                </label>
                <button
                  className={styles.borrarChico}
                  onClick={() => {
                    if (confirm(`¿Eliminar "${u.identificador}"?`)) eliminarMut.mutate(u.id);
                  }}
                >
                  Eliminar
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {mostrarAlta ? (
        <div className={styles.modalOverlay} onClick={() => setMostrarAlta(false)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>Nueva ubicación</h3>
            <label className={styles.checkboxRow}>
              <input type="checkbox" checked={masivo} onChange={e => setMasivo(e.target.checked)} />
              Alta masiva (varias a la vez)
            </label>
            <select className={styles.input} value={tipo} onChange={e => setTipo(e.target.value as TipoUbicacion)}>
              {TIPOS.map(t => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            {masivo ? (
              <>
                <input className={styles.input} placeholder="Prefijo (ej. S-)" value={prefijo} onChange={e => setPrefijo(e.target.value)} />
                <div className={styles.rango}>
                  <input className={styles.input} type="number" value={desde} onChange={e => setDesde(Number(e.target.value))} />
                  <span>a</span>
                  <input className={styles.input} type="number" value={hasta} onChange={e => setHasta(Number(e.target.value))} />
                </div>
                <p className={styles.hint}>
                  Crea {Math.max(0, hasta - desde + 1)} ubicaciones: {prefijo}
                  {desde} … {prefijo}
                  {hasta}
                </p>
              </>
            ) : (
              <input
                className={styles.input}
                placeholder="Identificador (ej. Carpa 12)"
                value={identificador}
                onChange={e => setIdentificador(e.target.value)}
              />
            )}
            <div className={styles.modalAcciones}>
              <Button variant="ghost" onClick={() => setMostrarAlta(false)}>
                Cancelar
              </Button>
              <Button
                onClick={() => crearMut.mutate()}
                cargando={crearMut.isPending}
                disabled={masivo ? hasta < desde : !identificador.trim()}
              >
                Crear
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
