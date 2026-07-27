'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  activarBalneario,
  crearBalneario,
  descargarCsvPlataforma,
  listarBalnearios,
  listarSuscripciones,
  reportePlataforma,
  suspenderBalneario,
} from '../../api/superAdmin';
import type { BalnearioResponse } from '../../api/types';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { Badge } from '../../components/Badge';
import { EstadoCargando, EstadoError } from '../../components/EstadoCarga';
import { useToast } from '../../components/Toast';
import { ApiError } from '../../api/ApiError';
import { formatearMonto } from '../../utils/money';
import styles from './page.module.css';

function FilaBalneario({
  balneario,
  volumen,
  onSuspender,
  onActivar,
}: {
  balneario: BalnearioResponse;
  volumen: { cantidadPedidos: number; facturacion: string } | undefined;
  onSuspender: (b: BalnearioResponse) => void;
  onActivar: (b: BalnearioResponse) => void;
}) {
  const router = useRouter();
  const suscripciones = useQuery({
    queryKey: ['suscripciones', balneario.id],
    queryFn: () => listarSuscripciones(balneario.id),
  });
  const vigente = suscripciones.data?.find(s => s.estado === 'ACTIVA') ?? suscripciones.data?.[0];

  return (
    <tr className={balneario.estado === 'SUSPENDIDO' ? styles.filaSuspendida : ''}>
      <td>
        <strong>{balneario.nombre}</strong>
        <div className={styles.slug}>{balneario.slug}</div>
      </td>
      <td>
        <Badge tono={balneario.estado === 'ACTIVO' ? 'exito' : 'error'}>{balneario.estado}</Badge>
        {balneario.operativo ? (
          <div className={styles.operativo}>● Operativo</div>
        ) : (
          <div className={styles.noOperativo}>○ No operativo</div>
        )}
      </td>
      <td>
        {vigente ? (
          <Badge tono={vigente.estado === 'ACTIVA' ? 'exito' : vigente.estado === 'PENDIENTE' ? 'advertencia' : 'neutro'}>
            {vigente.estado}
          </Badge>
        ) : (
          <span className={styles.sinDato}>Sin suscripción</span>
        )}
      </td>
      <td>
        {volumen ? (
          <>
            <div>{volumen.cantidadPedidos} pedidos</div>
            <div className={styles.facturacion}>{formatearMonto(volumen.facturacion)}</div>
          </>
        ) : (
          <span className={styles.sinDato}>—</span>
        )}
      </td>
      <td className={styles.acciones}>
        <button className={styles.linkBtn} onClick={() => router.push(`/super-admin/balnearios/${balneario.id}`)}>
          Ver
        </button>
        {balneario.estado === 'ACTIVO' ? (
          <button className={styles.linkBtnDanger} onClick={() => onSuspender(balneario)}>
            Suspender
          </button>
        ) : (
          <button className={styles.linkBtn} onClick={() => onActivar(balneario)}>
            Reactivar
          </button>
        )}
      </td>
    </tr>
  );
}

export default function SuperAdminBalneariosPage() {
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const [mostrarNuevo, setMostrarNuevo] = useState(false);
  const [mostrarMotivo, setMostrarMotivo] = useState<{ balneario: BalnearioResponse; accion: 'suspender' | 'activar' } | null>(
    null,
  );
  const [motivo, setMotivo] = useState('');
  const [form, setForm] = useState({ slug: '', nombre: '', emailContactoBalneario: '', telefono: '', nombreAdmin: '', emailAdmin: '' });
  const [altaCreada, setAltaCreada] = useState<{ slug: string; emailAdmin: string; passwordTemporalAdmin: string } | null>(null);

  const balnearios = useQuery({ queryKey: ['super-admin-balnearios'], queryFn: () => listarBalnearios() });
  const plataforma = useQuery({ queryKey: ['reporte-plataforma'], queryFn: reportePlataforma });

  const invalidarBalnearios = () => queryClient.invalidateQueries({ queryKey: ['super-admin-balnearios'] });

  const crearMut = useMutation({
    mutationFn: () => crearBalneario(form),
    onSuccess: resultado => {
      setAltaCreada({
        slug: resultado.balneario.slug,
        emailAdmin: resultado.emailAdmin,
        passwordTemporalAdmin: resultado.passwordTemporalAdmin,
      });
      setForm({ slug: '', nombre: '', emailContactoBalneario: '', telefono: '', nombreAdmin: '', emailAdmin: '' });
      setMostrarNuevo(false);
      invalidarBalnearios();
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos crear el balneario', 'error'),
  });

  const motivoMut = useMutation({
    mutationFn: () => {
      if (!mostrarMotivo) return Promise.reject(new Error('sin acción'));
      return mostrarMotivo.accion === 'suspender'
        ? suspenderBalneario(mostrarMotivo.balneario.id, motivo)
        : activarBalneario(mostrarMotivo.balneario.id, motivo);
    },
    onSuccess: () => {
      mostrar(mostrarMotivo?.accion === 'suspender' ? 'Balneario suspendido' : 'Balneario reactivado', 'info');
      setMostrarMotivo(null);
      setMotivo('');
      invalidarBalnearios();
      queryClient.invalidateQueries({ queryKey: ['reporte-plataforma'] });
    },
    onError: e => mostrar(e instanceof ApiError ? e.detail : 'No pudimos completar la acción', 'error'),
  });

  if (balnearios.isLoading || plataforma.isLoading) return <EstadoCargando texto="Cargando balnearios…" />;
  if (balnearios.isError) return <EstadoError mensaje="No pudimos cargar los balnearios." onReintentar={() => balnearios.refetch()} />;

  const lista = balnearios.data?.content ?? [];
  const volumenPorId = new Map((plataforma.data?.volumenPorBalneario ?? []).map(v => [v.balnearioId, v]));
  const facturacionTotal = (plataforma.data?.volumenPorBalneario ?? []).reduce((acc, v) => acc + Number(v.facturacion), 0);

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <h1 className={styles.titulo}>Balnearios</h1>
        <div className={styles.headerAcciones}>
          <Button variant="outline" onClick={() => descargarCsvPlataforma()}>
            Exportar CSV
          </Button>
          <Button onClick={() => setMostrarNuevo(true)}>+ Nuevo balneario</Button>
        </div>
      </div>

      <div className={styles.kpiGrid}>
        <Card>
          <div className={styles.kpiLabel}>Balnearios activos</div>
          <div className={styles.kpiValor}>{plataforma.data?.balneariosActivos ?? '—'}</div>
        </Card>
        <Card>
          <div className={styles.kpiLabel}>Facturación (temporada en curso)</div>
          <div className={styles.kpiValor}>{formatearMonto(facturacionTotal)}</div>
        </Card>
      </div>

      {altaCreada ? (
        <Card className={styles.tempPasswordCard}>
          <strong>
            Balneario {altaCreada.slug} creado — admin: {altaCreada.emailAdmin}
          </strong>
          <p>
            Contraseña temporal (no se envía por email en esta versión — compartila vos mismo):{' '}
            <code className={styles.tempPassword}>{altaCreada.passwordTemporalAdmin}</code>
          </p>
          <button className={styles.cerrarHint} onClick={() => setAltaCreada(null)}>
            Cerrar
          </button>
        </Card>
      ) : null}

      {lista.length === 0 ? (
        <p className={styles.vacio}>Todavía no hay balnearios dados de alta.</p>
      ) : (
        <div className={styles.tablaWrap}>
          <table className={styles.tabla}>
            <thead>
              <tr>
                <th>Balneario</th>
                <th>Estado</th>
                <th>Suscripción</th>
                <th>Pedidos / facturación</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {lista.map(b => (
                <FilaBalneario
                  key={b.id}
                  balneario={b}
                  volumen={volumenPorId.get(b.id)}
                  onSuspender={bal => setMostrarMotivo({ balneario: bal, accion: 'suspender' })}
                  onActivar={bal => setMostrarMotivo({ balneario: bal, accion: 'activar' })}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}

      {mostrarNuevo ? (
        <div className={styles.modalOverlay} onClick={() => setMostrarNuevo(false)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>Nuevo balneario</h3>
            <input
              className={styles.input}
              placeholder="Slug (ej: mar-azul)"
              value={form.slug}
              onChange={e => setForm({ ...form, slug: e.target.value })}
            />
            <input
              className={styles.input}
              placeholder="Nombre del balneario"
              value={form.nombre}
              onChange={e => setForm({ ...form, nombre: e.target.value })}
            />
            <input
              className={styles.input}
              placeholder="Email de contacto"
              type="email"
              value={form.emailContactoBalneario}
              onChange={e => setForm({ ...form, emailContactoBalneario: e.target.value })}
            />
            <input
              className={styles.input}
              placeholder="Teléfono"
              value={form.telefono}
              onChange={e => setForm({ ...form, telefono: e.target.value })}
            />
            <hr className={styles.separador} />
            <input
              className={styles.input}
              placeholder="Nombre del admin"
              value={form.nombreAdmin}
              onChange={e => setForm({ ...form, nombreAdmin: e.target.value })}
            />
            <input
              className={styles.input}
              placeholder="Email del admin"
              type="email"
              value={form.emailAdmin}
              onChange={e => setForm({ ...form, emailAdmin: e.target.value })}
            />
            <div className={styles.modalAcciones}>
              <Button variant="ghost" onClick={() => setMostrarNuevo(false)}>
                Cancelar
              </Button>
              <Button
                onClick={() => crearMut.mutate()}
                cargando={crearMut.isPending}
                disabled={!form.slug.trim() || !form.nombre.trim() || !form.emailAdmin.trim() || !form.nombreAdmin.trim()}
              >
                Crear
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {mostrarMotivo ? (
        <div className={styles.modalOverlay} onClick={() => setMostrarMotivo(null)}>
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <h3>
              {mostrarMotivo.accion === 'suspender' ? 'Suspender' : 'Reactivar'} {mostrarMotivo.balneario.nombre}
            </h3>
            {mostrarMotivo.accion === 'suspender' ? (
              <p className={styles.advertencia}>
                El balneario deja de ser operativo de inmediato: no se pueden abrir nuevas estadías ni crear pedidos nuevos.
                Las estadías y pedidos ya en curso no se ven afectados automáticamente.
              </p>
            ) : null}
            <label className={styles.campo}>
              Motivo (queda en auditoría)
              <textarea className={styles.textarea} value={motivo} onChange={e => setMotivo(e.target.value)} rows={3} />
            </label>
            <div className={styles.modalAcciones}>
              <Button variant="ghost" onClick={() => setMostrarMotivo(null)}>
                Cancelar
              </Button>
              <Button
                variant={mostrarMotivo.accion === 'suspender' ? 'danger' : 'primary'}
                onClick={() => motivoMut.mutate()}
                cargando={motivoMut.isPending}
                disabled={!motivo.trim()}
              >
                {mostrarMotivo.accion === 'suspender' ? 'Suspender' : 'Reactivar'}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}
