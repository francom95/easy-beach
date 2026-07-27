'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { actualizarTipoServicio, crearTipoServicio, eliminarTipoServicio, listarTiposServicio } from '../../../api/servicios';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import { EmptyState } from '../../../components/EmptyState';
import { useToast } from '../../../components/Toast';
import styles from './page.module.css';

export default function TiposServicioPage() {
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['tipos-servicio'], queryFn: listarTiposServicio });
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const [nombre, setNombre] = useState('');

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['tipos-servicio'] });

  const crearMut = useMutation({
    mutationFn: () => crearTipoServicio(nombre, true, (data?.length ?? 0) + 1),
    onSuccess: () => {
      setNombre('');
      invalidar();
    },
    onError: () => mostrar('No pudimos crear el tipo de servicio', 'error'),
  });

  const toggleMut = useMutation({
    mutationFn: ({ id, nombreActual, activo, orden }: { id: number; nombreActual: string; activo: boolean; orden: number }) =>
      actualizarTipoServicio(id, nombreActual, activo, orden),
    onSuccess: invalidar,
  });

  const eliminarMut = useMutation({
    mutationFn: (id: number) => eliminarTipoServicio(id),
    onSuccess: invalidar,
    onError: () => mostrar('No pudimos eliminar el tipo de servicio', 'error'),
  });

  if (isLoading) return <EstadoCargando texto="Cargando tipos de servicio…" />;
  if (isError) return <EstadoError mensaje="No pudimos cargarlos." onReintentar={() => refetch()} />;

  const tipos = data ?? [];

  return (
    <div className={styles.wrap}>
      <h1 className={styles.titulo}>Tipos de servicio</h1>
      <p className={styles.subtitulo}>Lo que el carpero ve al despachar solicitudes (ej. toallas, sombrilla extra, hielo).</p>

      {tipos.length === 0 ? (
        <EmptyState titulo="Sin tipos de servicio" descripcion="Agregá el primero para habilitar el pedido de servicios." />
      ) : (
        <div className={styles.lista}>
          {tipos.map(t => (
            <Card key={t.id} className={styles.fila}>
              <span className={styles.nombre}>{t.nombre}</span>
              <label className={styles.switch}>
                <input
                  type="checkbox"
                  checked={t.activo}
                  onChange={e => toggleMut.mutate({ id: t.id, nombreActual: t.nombre, activo: e.target.checked, orden: t.orden })}
                />
                <span className={styles.slider} />
              </label>
              <button
                className={styles.borrarChico}
                onClick={() => {
                  if (confirm(`¿Eliminar "${t.nombre}"?`)) eliminarMut.mutate(t.id);
                }}
              >
                Eliminar
              </button>
            </Card>
          ))}
        </div>
      )}

      <div className={styles.nuevoRow}>
        <input className={styles.input} placeholder="Nuevo tipo de servicio" value={nombre} onChange={e => setNombre(e.target.value)} />
        <Button disabled={!nombre.trim()} onClick={() => crearMut.mutate()} cargando={crearMut.isPending}>
          + Nuevo
        </Button>
      </div>
    </div>
  );
}
