import React from 'react';
import {Text, View} from 'react-native';
import {useMutation, useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {Card} from '../../components/Card';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {pedidosDeEstadia, cancelarPedido} from '../../api/pedidos';
import {formatearMonto} from '../../utils/money';
import type {PedidoResponse} from '../../api/types';
import type {RootScreenProps} from '../../navigation/types';

const ESTADOS_EN_CURSO = ['CREADO', 'PAGO_PENDIENTE', 'PAGO_RECHAZADO', 'CONFIRMADO', 'EN_PREPARACION', 'EN_CAMINO'];
const CANCELABLES_POR_CLIENTE = ['CREADO', 'PAGO_PENDIENTE', 'PAGO_RECHAZADO'];

/**
 * Adaptación real (etapa 16): la decisión de negocio de la etapa 12 fue
 * BLOQUEAR el cierre con pedidos en curso, sin opción de "forzar" - el
 * backend no tiene ese endpoint. Se explica la consecuencia real: esperar
 * (ya pagado, va a llegar) o cancelar individualmente los pedidos que
 * todavía se puede (antes de CONFIRMADO) para destrabar el cierre.
 */
export function S32CierreBloqueado({navigation}: RootScreenProps<'S32CierreBloqueado'>) {
  const {tokens, spacing} = useTheme();
  const estadia = useStayStore(s => s.estadia)!;

  const pedidosQuery = useQuery({
    queryKey: ['pedidos', estadia.publicId],
    queryFn: () => pedidosDeEstadia(estadia.publicId),
    refetchInterval: 10000,
  });
  const cancelarMutation = useMutation({
    mutationFn: (publicId: string) => cancelarPedido(publicId, 'El cliente decidió cerrar su estadía'),
    onSuccess: () => pedidosQuery.refetch(),
  });

  const enCurso = (pedidosQuery.data ?? []).filter((p: PedidoResponse) => ESTADOS_EN_CURSO.includes(p.estado));

  return (
    <ScreenContainer scroll>
      <StateRow tipo="advertencia" texto="Todavía no podés cerrar tu estadía" tamano={28} />
      <Text style={{color: tokens['color.on-surface-muted'], marginVertical: spacing.md}}>
        Tenés pedidos en curso. Si ya los pagaste, van a llegar igual - podés esperar, o cancelar los que todavía se
        puede para destrabar el cierre.
      </Text>

      {enCurso.map(pedido => (
        <Card key={pedido.publicId} style={{marginBottom: spacing.sm}}>
          <View style={{flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center'}}>
            <View>
              <Text style={{color: tokens['color.on-surface'], fontWeight: '700'}}>
                {pedido.estado.replace('_', ' ')} · {formatearMonto(pedido.total)}
              </Text>
            </View>
            {CANCELABLES_POR_CLIENTE.includes(pedido.estado) ? (
              <Button
                titulo="Cancelar"
                variante="contorno"
                fullWidth={false}
                onPress={() => cancelarMutation.mutate(pedido.publicId)}
                cargando={cancelarMutation.isPending}
              />
            ) : null}
          </View>
        </Card>
      ))}

      <View style={{marginTop: spacing.xl}}>
        <Button titulo="Reintentar cierre" onPress={() => navigation.replace('S29ResumenConsumo')} variante="secundario" />
      </View>
      <View style={{marginTop: spacing.md}}>
        <Button titulo="Esperar y volver más tarde" onPress={() => navigation.goBack()} variante="contorno" />
      </View>
    </ScreenContainer>
  );
}
