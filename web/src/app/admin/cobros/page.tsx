'use client';

import { useEffect, useRef } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { desvincularMp, estadoVinculacionMp, iniciarVinculacionMp } from '../../../api/mercadopago';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { EstadoCargando } from '../../../components/EstadoCarga';
import { useToast } from '../../../components/Toast';
import styles from './page.module.css';

export default function CobrosPage() {
  const { data, isLoading } = useQuery({ queryKey: ['mp-estado'], queryFn: estadoVinculacionMp });
  const queryClient = useQueryClient();
  const { mostrar } = useToast();
  const popupRef = useRef<Window | null>(null);

  const iniciarMut = useMutation({
    mutationFn: iniciarVinculacionMp,
    onSuccess: ({ urlAutorizacion }) => {
      popupRef.current = window.open(urlAutorizacion, 'mp-oauth', 'width=520,height=680');
    },
    onError: () => mostrar('No pudimos iniciar la vinculación con Mercado Pago', 'error'),
  });

  const desvincularMut = useMutation({
    mutationFn: desvincularMp,
    onSuccess: () => {
      mostrar('Cuenta desvinculada', 'info');
      queryClient.invalidateQueries({ queryKey: ['mp-estado'] });
    },
  });

  // Al volver a la pestaña (el admin completó el flujo en la ventana de MP), refrescar el estado.
  useEffect(() => {
    function onFocus() {
      queryClient.invalidateQueries({ queryKey: ['mp-estado'] });
    }
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [queryClient]);

  if (isLoading) return <EstadoCargando texto="Cargando estado de Mercado Pago…" />;

  const estado = data?.estado ?? 'DESVINCULADA';

  return (
    <div className={styles.wrap}>
      <h1 className={styles.titulo}>Cobros</h1>

      {estado === 'VINCULADA' ? (
        <Card className={styles.cardVinculada}>
          <div className={styles.estadoLabel}>✓ Vinculada</div>
          <p>Tu cuenta de Mercado Pago está conectada y los clientes pueden pagar sus pedidos.</p>
          {data?.mpUserId ? <p className={styles.mpId}>Cuenta: {data.mpUserId}</p> : null}
          <Button variant="outline" onClick={() => iniciarMut.mutate()} cargando={iniciarMut.isPending}>
            Re-vincular o cambiar cuenta
          </Button>
          <button className={styles.desvincularLink} onClick={() => desvincularMut.mutate()}>
            Desvincular
          </button>
        </Card>
      ) : estado === 'EXPIRADA' ? (
        <Card className={styles.cardExpirada}>
          <div className={styles.estadoLabel}>⚠ Vinculación expirada</div>
          <p>Los pedidos nuevos están frenados hasta que re-vincules la cuenta.</p>
          <Button onClick={() => iniciarMut.mutate()} cargando={iniciarMut.isPending}>
            Re-vincular ahora
          </Button>
        </Card>
      ) : (
        <Card className={styles.cardSinVincular}>
          <div className={styles.estadoLabel}>⚠ Sin vincular</div>
          <p>Sin una cuenta de Mercado Pago vinculada, el balneario no puede cobrar pedidos.</p>
          <Button onClick={() => iniciarMut.mutate()} cargando={iniciarMut.isPending}>
            Vincular cuenta de Mercado Pago
          </Button>
        </Card>
      )}
    </div>
  );
}
