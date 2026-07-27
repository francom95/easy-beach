'use client';

import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useEstadiasPendientes } from '../../../hooks/useOperativoQueries';
import { rechazarEstadia, validarEstadia } from '../../../api/estadias';
import { EstadoCargando, EstadoError } from '../../../components/EstadoCarga';
import { EmptyState } from '../../../components/EmptyState';
import { Button } from '../../../components/Button';
import { useToast } from '../../../components/Toast';
import { formatearAntiguedad, minutosDesde } from '../../../utils/tiempo';
import type { EstadiaPendienteResponse } from '../../../api/types';
import styles from './page.module.css';

function iniciales(nombre: string | null): string {
  if (!nombre) return '?';
  return nombre
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(p => p[0]?.toUpperCase())
    .join('');
}

export default function EstadiasPage() {
  const { data, isLoading, isError, refetch } = useEstadiasPendientes();
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const [rechazando, setRechazando] = useState<EstadiaPendienteResponse | null>(null);
  const [motivo, setMotivo] = useState('');

  const validar = useMutation({
    mutationFn: (publicId: string) => validarEstadia(publicId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operativo-estadias-pendientes'] });
      mostrar('Estadía confirmada', 'exito');
    },
    onError: () => mostrar('No pudimos confirmar la estadía', 'error'),
  });

  const rechazar = useMutation({
    mutationFn: ({ publicId, motivo }: { publicId: string; motivo: string }) => rechazarEstadia(publicId, motivo),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['operativo-estadias-pendientes'] });
      mostrar('Solicitud rechazada', 'info');
      setRechazando(null);
      setMotivo('');
    },
    onError: () => mostrar('No pudimos rechazar la solicitud', 'error'),
  });

  if (isLoading) return <EstadoCargando texto="Cargando validaciones…" />;
  if (isError) return <EstadoError mensaje="No pudimos cargar las validaciones." onReintentar={() => refetch()} />;

  const pendientes = data ?? [];

  return (
    <>
      <div className={styles.banner}>
        Antes de confirmar, verificá que el cliente esté físicamente en la ubicación que reclama.
      </div>

      {pendientes.length === 0 ? (
        <EmptyState titulo="Sin solicitudes pendientes de validar" descripcion="Escuchando en vivo…" />
      ) : (
        <div className={styles.grid}>
          {pendientes.map(est => {
            const minutos = minutosDesde(est.fechaSolicitud);
            return (
              <div key={est.publicId} className={styles.tarjeta}>
                <div className={styles.clienteRow}>
                  <span className={styles.avatar}>{iniciales(est.clienteNombre)}</span>
                  <div>
                    <div className={styles.clienteNombre}>{est.clienteNombre ?? 'Cliente'}</div>
                    <div className={styles.ubicacion}>{est.ubicacionIdentificador}</div>
                  </div>
                  <span className={styles.antiguedad}>{formatearAntiguedad(minutos)}</span>
                </div>
                <div className={styles.acciones}>
                  <Button variant="primary" fullWidth onClick={() => validar.mutate(est.publicId)} cargando={validar.isPending}>
                    ✓ Confirmar
                  </Button>
                  <Button variant="outline" fullWidth onClick={() => setRechazando(est)}>
                    Rechazar
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {rechazando ? (
        <div className={styles.modalOverlay}>
          <div className={styles.modal}>
            <h3>Rechazar solicitud de {rechazando.clienteNombre ?? 'cliente'}</h3>
            <textarea
              className={styles.textarea}
              placeholder="Motivo del rechazo"
              value={motivo}
              onChange={e => setMotivo(e.target.value)}
              rows={3}
            />
            <div className={styles.modalAcciones}>
              <Button variant="ghost" onClick={() => setRechazando(null)}>
                Cancelar
              </Button>
              <Button
                variant="danger"
                disabled={!motivo.trim()}
                cargando={rechazar.isPending}
                onClick={() => rechazar.mutate({ publicId: rechazando.publicId, motivo })}
              >
                Rechazar
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
