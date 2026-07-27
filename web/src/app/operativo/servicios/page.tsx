'use client';

import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useColaServicios } from '../../../hooks/useOperativoQueries';
import { transicionarSolicitudServicio } from '../../../api/servicios';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import { EmptyState } from '../../../components/EmptyState';
import { Button } from '../../../components/Button';
import { useToast } from '../../../components/Toast';
import { formatearAntiguedad, minutosDesde, tonoPorAntiguedad } from '../../../utils/tiempo';
import type { EstadoSolicitudServicio } from '../../../api/types';
import styles from '../pedidos/page.module.css';
import own from './page.module.css';

const COLUMNAS: { estado: EstadoSolicitudServicio; titulo: string; siguiente: EstadoSolicitudServicio; accion: string }[] = [
  { estado: 'PENDIENTE', titulo: 'Pendientes', siguiente: 'EN_CURSO', accion: 'Tomar' },
  { estado: 'EN_CURSO', titulo: 'En curso', siguiente: 'RESUELTA', accion: 'Resuelta' },
];

export default function ServiciosPage() {
  const { data, isLoading, isError, refetch } = useColaServicios();
  const queryClient = useQueryClient();
  const { mostrar } = useToast();

  const transicion = useMutation({
    mutationFn: ({ publicId, estado }: { publicId: string; estado: EstadoSolicitudServicio }) =>
      transicionarSolicitudServicio(publicId, estado),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operativo-servicios'] });
      mostrar('Solicitud actualizada', 'exito');
    },
    onError: () => mostrar('No pudimos actualizar la solicitud', 'error'),
  });

  if (isLoading) return <EstadoCargando texto="Cargando solicitudes…" />;
  if (isError) return <EstadoError mensaje="No pudimos cargar las solicitudes." onReintentar={() => refetch()} />;

  const solicitudes = data ?? [];
  if (solicitudes.length === 0) {
    return <EmptyState titulo="Sin solicitudes pendientes" descripcion="Escuchando en vivo…" />;
  }

  return (
    <div className={own.board2}>
      {COLUMNAS.map(col => {
        const items = solicitudes.filter(s => s.estado === col.estado);
        return (
          <div key={col.estado} className={styles.columna}>
            <div className={styles.columnaHeader}>
              <span>{col.titulo}</span>
              <span className={styles.contador}>{items.length}</span>
            </div>
            <div className={styles.tarjetas}>
              {items.map(sol => {
                const minutos = minutosDesde(sol.createdAt);
                return (
                  <div key={sol.publicId} className={styles.tarjeta}>
                    <div className={styles.tarjetaTop}>
                      <strong className={styles.ubicacion}>{sol.ubicacionIdentificador}</strong>
                      <span className={[styles.antiguedad, styles[tonoPorAntiguedad(minutos)]].join(' ')}>
                        {formatearAntiguedad(minutos)}
                      </span>
                    </div>
                    <div>{sol.tipoServicioNombre}</div>
                    {sol.nota ? <p className={own.nota}>&ldquo;{sol.nota}&rdquo;</p> : null}
                    <Button
                      fullWidth
                      onClick={() => transicion.mutate({ publicId: sol.publicId, estado: col.siguiente })}
                      cargando={transicion.isPending}
                    >
                      {col.accion}
                    </Button>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
