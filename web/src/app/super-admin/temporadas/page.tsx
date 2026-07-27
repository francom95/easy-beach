'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cambiarEstadoTemporada, crearTemporada, listarTemporadas } from '../../../api/superAdmin';
import type { EstadoTemporada } from '../../../api/types';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { Badge } from '../../../components/Badge';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import { useToast } from '../../../components/Toast';
import { ApiError } from '../../../api/ApiError';
import styles from './page.module.css';

const ESTADOS: EstadoTemporada[] = ['PLANIFICADA', 'EN_CURSO', 'CERRADA'];

function tonoEstado(estado: EstadoTemporada) {
  if (estado === 'EN_CURSO') return 'exito' as const;
  if (estado === 'PLANIFICADA') return 'advertencia' as const;
  return 'neutro' as const;
}

export default function TemporadasPage() {
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['temporadas'], queryFn: listarTemporadas });
  const [mostrarForm, setMostrarForm] = useState(false);
  const [form, setForm] = useState({ nombre: '', fechaInicio: '', fechaFin: '' });

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['temporadas'] });

  const crearMut = useMutation({
    mutationFn: () => crearTemporada(form),
    onSuccess: () => {
      mostrar('Temporada creada', 'exito');
      setForm({ nombre: '', fechaInicio: '', fechaFin: '' });
      setMostrarForm(false);
      invalidar();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos crear la temporada', 'error'),
  });

  const estadoMut = useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: EstadoTemporada }) => cambiarEstadoTemporada(id, estado),
    onSuccess: () => {
      mostrar('Estado actualizado', 'info');
      invalidar();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos cambiar el estado', 'error'),
  });

  if (isLoading) return <EstadoCargando texto="Cargando temporadas…" />;
  if (isError) return <EstadoError mensaje="No pudimos cargar las temporadas." onReintentar={() => refetch()} />;

  const temporadas = data ?? [];

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <h1 className={styles.titulo}>Temporadas</h1>
        <Button onClick={() => setMostrarForm(true)}>+ Nueva temporada</Button>
      </div>

      <div className={styles.grid}>
        {temporadas.map(t => (
          <Card key={t.id} className={t.estado === 'CERRADA' ? styles.temporadaCardCerrada : styles.temporadaCard}>
            <div className={styles.temporadaHeader}>
              <span className={[styles.dot, styles[`dot${t.estado}`]].join(' ')} />
              <strong>{t.nombre}</strong>
            </div>
            <div className={styles.rango}>
              {t.fechaInicio} → {t.fechaFin}
            </div>
            <Badge tono={tonoEstado(t.estado)}>{t.estado}</Badge>
            <select
              className={styles.selectEstado}
              value={t.estado}
              onChange={e => estadoMut.mutate({ id: t.id, estado: e.target.value as EstadoTemporada })}
            >
              {ESTADOS.map(estado => (
                <option key={estado} value={estado}>
                  {estado}
                </option>
              ))}
            </select>
          </Card>
        ))}
      </div>

      {mostrarForm ? (
        <div className={styles.modalOverlay} onClick={() => setMostrarForm(false)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>Nueva temporada</h3>
            <input
              className={styles.input}
              placeholder="Nombre (ej: Verano 26-27)"
              value={form.nombre}
              onChange={e => setForm({ ...form, nombre: e.target.value })}
            />
            <label className={styles.campo}>
              Fecha de inicio
              <input
                className={styles.input}
                type="date"
                value={form.fechaInicio}
                onChange={e => setForm({ ...form, fechaInicio: e.target.value })}
              />
            </label>
            <label className={styles.campo}>
              Fecha de fin
              <input
                className={styles.input}
                type="date"
                value={form.fechaFin}
                onChange={e => setForm({ ...form, fechaFin: e.target.value })}
              />
            </label>
            <div className={styles.modalAcciones}>
              <Button variant="ghost" onClick={() => setMostrarForm(false)}>
                Cancelar
              </Button>
              <Button
                onClick={() => crearMut.mutate()}
                cargando={crearMut.isPending}
                disabled={!form.nombre.trim() || !form.fechaInicio || !form.fechaFin}
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
