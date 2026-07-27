'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  actualizarBalneario,
  cambiarEstadoSuscripcion,
  listarPlanes,
  listarSuscripciones,
  listarTemporadas,
  obtenerBalneario,
  suscribirBalneario,
} from '../../../../api/superAdmin';
import type { EstadoSuscripcion } from '../../../../api/types';
import { Button } from '../../../../components/Button';
import { Card } from '../../../../components/Card';
import { Badge } from '../../../../components/Badge';
import { EstadoCargando, EstadoError } from '../../../../components/EstadoCarga';
import { useToast } from '../../../../components/Toast';
import { ApiError } from '../../../../api/ApiError';
import styles from './page.module.css';

const ESTADOS_SUSCRIPCION: EstadoSuscripcion[] = ['PENDIENTE', 'ACTIVA', 'SUSPENDIDA', 'FINALIZADA'];

function tonoEstadoSuscripcion(estado: EstadoSuscripcion) {
  if (estado === 'ACTIVA') return 'exito' as const;
  if (estado === 'PENDIENTE') return 'advertencia' as const;
  if (estado === 'SUSPENDIDA') return 'error' as const;
  return 'neutro' as const;
}

export default function BalnearioDetallePage() {
  const params = useParams<{ id: string }>();
  const balnearioId = Number(params.id);
  const router = useRouter();
  const queryClient = useQueryClient();
  const { mostrar } = useToast();

  const balneario = useQuery({ queryKey: ['super-admin-balneario', balnearioId], queryFn: () => obtenerBalneario(balnearioId) });
  const suscripciones = useQuery({ queryKey: ['suscripciones', balnearioId], queryFn: () => listarSuscripciones(balnearioId) });
  const planes = useQuery({ queryKey: ['planes'], queryFn: listarPlanes });
  const temporadas = useQuery({ queryKey: ['temporadas'], queryFn: listarTemporadas });

  const [editando, setEditando] = useState(false);
  const [form, setForm] = useState({ nombre: '', emailContacto: '', telefono: '' });
  const [planId, setPlanId] = useState<number | null>(null);
  const [temporadaId, setTemporadaId] = useState<number | null>(null);

  const invalidarSuscripciones = () => queryClient.invalidateQueries({ queryKey: ['suscripciones', balnearioId] });

  const guardarMut = useMutation({
    mutationFn: () => actualizarBalneario(balnearioId, form),
    onSuccess: () => {
      mostrar('Datos actualizados', 'exito');
      setEditando(false);
      queryClient.invalidateQueries({ queryKey: ['super-admin-balneario', balnearioId] });
      queryClient.invalidateQueries({ queryKey: ['super-admin-balnearios'] });
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos guardar los cambios', 'error'),
  });

  const suscribirMut = useMutation({
    mutationFn: () => {
      if (!planId || !temporadaId) return Promise.reject(new Error('faltan datos'));
      return suscribirBalneario(balnearioId, planId, temporadaId);
    },
    onSuccess: () => {
      mostrar('Suscripción creada', 'exito');
      setPlanId(null);
      setTemporadaId(null);
      invalidarSuscripciones();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos crear la suscripción', 'error'),
  });

  const cambiarEstadoMut = useMutation({
    mutationFn: ({ suscripcionId, estado }: { suscripcionId: number; estado: EstadoSuscripcion }) =>
      cambiarEstadoSuscripcion(balnearioId, suscripcionId, estado),
    onSuccess: () => {
      mostrar('Estado de suscripción actualizado', 'info');
      invalidarSuscripciones();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos cambiar el estado', 'error'),
  });

  if (balneario.isLoading) return <EstadoCargando texto="Cargando balneario…" />;
  if (balneario.isError || !balneario.data) {
    return <EstadoError mensaje="No pudimos cargar el balneario." onReintentar={() => balneario.refetch()} />;
  }

  const b = balneario.data;

  function empezarEdicion() {
    setForm({ nombre: b.nombre, emailContacto: b.emailContacto, telefono: b.telefono });
    setEditando(true);
  }

  return (
    <div className={styles.wrap}>
      <button className={styles.volver} onClick={() => router.push('/super-admin')}>
        ← Volver a balnearios
      </button>

      <div className={styles.header}>
        <div>
          <h1 className={styles.titulo}>{b.nombre}</h1>
          <div className={styles.slug}>{b.slug}</div>
        </div>
        <div className={styles.badges}>
          <Badge tono={b.estado === 'ACTIVO' ? 'exito' : 'error'}>{b.estado}</Badge>
          <Badge tono={b.operativo ? 'exito' : 'neutro'}>{b.operativo ? 'Operativo' : 'No operativo'}</Badge>
        </div>
      </div>

      <Card className={styles.datosCard}>
        <div className={styles.datosHeader}>
          <h2 className={styles.seccionTitulo}>Datos de contacto</h2>
          {!editando ? (
            <Button variant="outline" size="md" onClick={empezarEdicion}>
              Editar
            </Button>
          ) : null}
        </div>
        {editando ? (
          <div className={styles.formEdicion}>
            <input
              className={styles.input}
              placeholder="Nombre"
              value={form.nombre}
              onChange={e => setForm({ ...form, nombre: e.target.value })}
            />
            <input
              className={styles.input}
              placeholder="Email de contacto"
              type="email"
              value={form.emailContacto}
              onChange={e => setForm({ ...form, emailContacto: e.target.value })}
            />
            <input
              className={styles.input}
              placeholder="Teléfono"
              value={form.telefono}
              onChange={e => setForm({ ...form, telefono: e.target.value })}
            />
            <div className={styles.modalAcciones}>
              <Button variant="ghost" size="md" onClick={() => setEditando(false)}>
                Cancelar
              </Button>
              <Button size="md" onClick={() => guardarMut.mutate()} cargando={guardarMut.isPending}>
                Guardar
              </Button>
            </div>
          </div>
        ) : (
          <dl className={styles.datosLista}>
            <dt>Email</dt>
            <dd>{b.emailContacto}</dd>
            <dt>Teléfono</dt>
            <dd>{b.telefono || '—'}</dd>
          </dl>
        )}
      </Card>

      <Card>
        <h2 className={styles.seccionTitulo}>Suscripciones</h2>
        {suscripciones.isLoading ? (
          <EstadoCargando texto="Cargando suscripciones…" />
        ) : (suscripciones.data ?? []).length === 0 ? (
          <p className={styles.vacio}>Este balneario no tiene suscripciones todavía.</p>
        ) : (
          <div className={styles.suscripcionesLista}>
            {(suscripciones.data ?? []).map(s => {
              const plan = planes.data?.find(p => p.id === s.planId);
              const temporada = temporadas.data?.find(t => t.id === s.temporadaId);
              return (
                <div key={s.id} className={styles.suscripcionRow}>
                  <div>
                    <strong>{plan?.nombre ?? `Plan #${s.planId}`}</strong>
                    <div className={styles.suscripcionSub}>{temporada?.nombre ?? `Temporada #${s.temporadaId}`}</div>
                  </div>
                  <Badge tono={tonoEstadoSuscripcion(s.estado)}>{s.estado}</Badge>
                  <select
                    className={styles.selectEstado}
                    value={s.estado}
                    onChange={e => cambiarEstadoMut.mutate({ suscripcionId: s.id, estado: e.target.value as EstadoSuscripcion })}
                  >
                    {ESTADOS_SUSCRIPCION.map(estado => (
                      <option key={estado} value={estado}>
                        {estado}
                      </option>
                    ))}
                  </select>
                </div>
              );
            })}
          </div>
        )}

        <div className={styles.suscribirForm}>
          <h3 className={styles.subTitulo}>Suscribir a un plan</h3>
          <div className={styles.suscribirRow}>
            <select
              className={styles.input}
              value={planId ?? ''}
              onChange={e => setPlanId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Plan…</option>
              {(planes.data ?? []).filter(p => p.activo).map(p => (
                <option key={p.id} value={p.id}>
                  {p.nombre}
                </option>
              ))}
            </select>
            <select
              className={styles.input}
              value={temporadaId ?? ''}
              onChange={e => setTemporadaId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Temporada…</option>
              {(temporadas.data ?? []).map(t => (
                <option key={t.id} value={t.id}>
                  {t.nombre} ({t.estado})
                </option>
              ))}
            </select>
            <Button size="md" onClick={() => suscribirMut.mutate()} cargando={suscribirMut.isPending} disabled={!planId || !temporadaId}>
              Suscribir
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
