'use client';

import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { listarAuditoria, listarBalnearios } from '../../../api/superAdmin';
import { Button } from '../../../components/Button';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import styles from './page.module.css';

function formatearFecha(iso: string): string {
  return new Date(iso).toLocaleString('es-AR');
}

export default function AuditoriaPage() {
  const [balnearioId, setBalnearioId] = useState<number | null>(null);
  const [page, setPage] = useState(0);

  const balnearios = useQuery({ queryKey: ['super-admin-balnearios-filtro'], queryFn: () => listarBalnearios(0, 100) });
  const auditoria = useQuery({
    queryKey: ['auditoria', balnearioId, page],
    queryFn: () => listarAuditoria(balnearioId, page, 20),
  });

  const nombrePorId = new Map((balnearios.data?.content ?? []).map(b => [b.id, b.nombre]));

  if (auditoria.isLoading) return <EstadoCargando texto="Cargando auditoría…" />;
  if (auditoria.isError) return <EstadoError mensaje="No pudimos cargar la auditoría." onReintentar={() => auditoria.refetch()} />;

  const registros = auditoria.data?.content ?? [];

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <h1 className={styles.titulo}>Auditoría</h1>
        <select
          className={styles.filtro}
          value={balnearioId ?? ''}
          onChange={e => {
            setBalnearioId(e.target.value ? Number(e.target.value) : null);
            setPage(0);
          }}
        >
          <option value="">Todos los balnearios</option>
          {(balnearios.data?.content ?? []).map(b => (
            <option key={b.id} value={b.id}>
              {b.nombre}
            </option>
          ))}
        </select>
      </div>

      {registros.length === 0 ? (
        <p className={styles.vacio}>No hay acciones registradas todavía.</p>
      ) : (
        <div className={styles.tablaWrap}>
          <table className={styles.tabla}>
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Acción</th>
                <th>Entidad</th>
                <th>Balneario</th>
                <th>Actor (usuarioId)</th>
              </tr>
            </thead>
            <tbody>
              {registros.map(a => (
                <tr key={a.id}>
                  <td>{formatearFecha(a.createdAt)}</td>
                  <td>
                    <code className={styles.accion}>{a.accion}</code>
                  </td>
                  <td>
                    {a.entidadTipo}
                    {a.entidadId ? ` #${a.entidadId}` : ''}
                  </td>
                  <td>{a.balnearioId ? nombrePorId.get(a.balnearioId) ?? `#${a.balnearioId}` : '—'}</td>
                  <td>{a.actorUsuarioId}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className={styles.paginacion}>
        <Button variant="outline" size="md" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>
          Anterior
        </Button>
        <span className={styles.paginaInfo}>
          Página {(auditoria.data?.page ?? 0) + 1} de {Math.max(1, auditoria.data?.totalPages ?? 1)}
        </span>
        <Button
          variant="outline"
          size="md"
          onClick={() => setPage(p => p + 1)}
          disabled={(auditoria.data?.page ?? 0) + 1 >= (auditoria.data?.totalPages ?? 1)}
        >
          Siguiente
        </Button>
      </div>
    </div>
  );
}
