'use client';

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { actualizarPlan, crearPlan, listarPlanes } from '../../../api/superAdmin';
import type { PlanResponse } from '../../../api/types';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { Badge } from '../../../components/Badge';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import { useToast } from '../../../components/Toast';
import { ApiError } from '../../../api/ApiError';
import { formatearMonto } from '../../../utils/money';
import styles from './page.module.css';

const FORM_VACIO = { nombre: '', descripcion: '', precio: '', activo: true };

export default function PlanesPage() {
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const { data, isLoading, isError, refetch } = useQuery({ queryKey: ['planes'], queryFn: listarPlanes });
  const [mostrarForm, setMostrarForm] = useState(false);
  const [editando, setEditando] = useState<PlanResponse | null>(null);
  const [form, setForm] = useState(FORM_VACIO);

  const invalidar = () => queryClient.invalidateQueries({ queryKey: ['planes'] });

  const guardarMut = useMutation({
    mutationFn: () =>
      editando
        ? actualizarPlan(editando.id, { ...form, precio: form.precio })
        : crearPlan({ ...form, precio: form.precio }),
    onSuccess: () => {
      mostrar(editando ? 'Plan actualizado' : 'Plan creado', 'exito');
      cerrarForm();
      invalidar();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos guardar el plan', 'error'),
  });

  function abrirNuevo() {
    setEditando(null);
    setForm(FORM_VACIO);
    setMostrarForm(true);
  }

  function abrirEdicion(plan: PlanResponse) {
    setEditando(plan);
    setForm({ nombre: plan.nombre, descripcion: plan.descripcion ?? '', precio: plan.precio, activo: plan.activo });
    setMostrarForm(true);
  }

  function cerrarForm() {
    setMostrarForm(false);
    setEditando(null);
    setForm(FORM_VACIO);
  }

  function alternarActivo(plan: PlanResponse) {
    actualizarPlan(plan.id, {
      nombre: plan.nombre,
      descripcion: plan.descripcion ?? '',
      precio: plan.precio,
      activo: !plan.activo,
    })
      .then(() => {
        mostrar(plan.activo ? 'Plan desactivado' : 'Plan activado', 'info');
        invalidar();
      })
      .catch(e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos cambiar el estado', 'error'));
  }

  if (isLoading) return <EstadoCargando texto="Cargando planes…" />;
  if (isError) return <EstadoError mensaje="No pudimos cargar los planes." onReintentar={() => refetch()} />;

  const planes = data ?? [];

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <h1 className={styles.titulo}>Planes</h1>
        <Button onClick={abrirNuevo}>+ Nuevo plan</Button>
      </div>

      <div className={styles.grid}>
        {planes.map(p => (
          <Card key={p.id} className={p.activo ? styles.planCard : styles.planCardInactivo}>
            <div className={styles.planHeader}>
              <strong>{p.nombre}</strong>
              <Badge tono={p.activo ? 'exito' : 'neutro'}>{p.activo ? 'ACTIVO' : 'INACTIVO'}</Badge>
            </div>
            {p.descripcion ? <p className={styles.descripcion}>{p.descripcion}</p> : null}
            <div className={styles.precio}>{formatearMonto(p.precio)}</div>
            <div className={styles.planAcciones}>
              <button className={styles.linkBtn} onClick={() => abrirEdicion(p)}>
                Editar
              </button>
              <button className={styles.linkBtn} onClick={() => alternarActivo(p)}>
                {p.activo ? 'Desactivar' : 'Activar'}
              </button>
            </div>
          </Card>
        ))}
      </div>

      {mostrarForm ? (
        <div className={styles.modalOverlay} onClick={cerrarForm}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>{editando ? 'Editar plan' : 'Nuevo plan'}</h3>
            <input
              className={styles.input}
              placeholder="Nombre"
              value={form.nombre}
              onChange={e => setForm({ ...form, nombre: e.target.value })}
            />
            <textarea
              className={styles.textarea}
              placeholder="Descripción"
              value={form.descripcion}
              onChange={e => setForm({ ...form, descripcion: e.target.value })}
              rows={3}
            />
            <input
              className={styles.input}
              placeholder="Precio"
              type="number"
              min="0"
              step="0.01"
              value={form.precio}
              onChange={e => setForm({ ...form, precio: e.target.value })}
            />
            <label className={styles.checkboxRow}>
              <input type="checkbox" checked={form.activo} onChange={e => setForm({ ...form, activo: e.target.checked })} />
              Activo
            </label>
            <div className={styles.modalAcciones}>
              <Button variant="ghost" onClick={cerrarForm}>
                Cancelar
              </Button>
              <Button onClick={() => guardarMut.mutate()} cargando={guardarMut.isPending} disabled={!form.nombre.trim() || !form.precio}>
                Guardar
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
