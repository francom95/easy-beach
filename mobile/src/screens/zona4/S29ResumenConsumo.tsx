import React, {useMemo} from 'react';
import {Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Card} from '../../components/Card';
import {Button} from '../../components/Button';
import {EstadoCargando} from '../../components/EstadoCarga';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {pedidosDeEstadia} from '../../api/pedidos';
import {formatearMonto} from '../../utils/money';
import type {RootScreenProps} from '../../navigation/types';

const ESTADOS_EN_CURSO = ['CREADO', 'PAGO_PENDIENTE', 'PAGO_RECHAZADO', 'CONFIRMADO', 'EN_PREPARACION', 'EN_CAMINO'];

/**
 * Refuerza el valor entregado (etapa 07): vista concentrada de todo lo
 * consumido + ahorro por promociones. El check "nada pendiente" es
 * requisito de cierre - si hay pedidos en curso, el botón de cerrar lleva a
 * S32 en vez de S30 (la confirmación real la hace el servidor de todas
 * formas - etapa 12 - esto es una vista previa, no autoritativa).
 */
export function S29ResumenConsumo({navigation}: RootScreenProps<'S29ResumenConsumo'>) {
  const {tokens, spacing, typeScale} = useTheme();
  const estadia = useStayStore(s => s.estadia)!;

  const pedidosQuery = useQuery({queryKey: ['pedidos', estadia.publicId], queryFn: () => pedidosDeEstadia(estadia.publicId)});

  const {entregados, totalConsumido, totalAhorrado, hayPedidosEnCurso} = useMemo(() => {
    const pedidos = pedidosQuery.data ?? [];
    const entregadosLista = pedidos.filter(p => p.estado === 'ENTREGADO');
    return {
      entregados: entregadosLista,
      totalConsumido: entregadosLista.reduce((acc, p) => acc + Number(p.total), 0),
      totalAhorrado: entregadosLista.reduce((acc, p) => acc + Number(p.descuentoTotal), 0),
      hayPedidosEnCurso: pedidos.some(p => ESTADOS_EN_CURSO.includes(p.estado)),
    };
  }, [pedidosQuery.data]);

  if (pedidosQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer scroll>
      <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.lg}}>
        Tu resumen
      </Text>

      <Card style={{marginBottom: spacing.md}}>
        <Text style={{color: tokens['color.on-surface-muted']}}>Pedidos entregados</Text>
        <Text style={{fontWeight: '700', color: tokens['color.on-surface']}}>{entregados.length}</Text>
      </Card>
      <Card style={{marginBottom: spacing.md}}>
        <Text style={{color: tokens['color.on-surface-muted']}}>Total consumido</Text>
        <Text style={{fontWeight: '800', fontSize: typeScale.price, color: tokens['color.on-surface']}}>
          {formatearMonto(totalConsumido)}
        </Text>
      </Card>
      {totalAhorrado > 0 ? (
        <Card style={{marginBottom: spacing.md}}>
          <Text style={{color: tokens['color.on-surface-muted']}}>Ahorraste con promociones</Text>
          <Text style={{fontWeight: '700', color: tokens['color.success']}}>{formatearMonto(totalAhorrado)}</Text>
        </Card>
      ) : null}

      <View style={{marginVertical: spacing.md}}>
        <StateRow
          tipo={hayPedidosEnCurso ? 'advertencia' : 'exito'}
          texto={hayPedidosEnCurso ? 'Tenés pedidos en curso' : 'Nada pendiente'}
        />
      </View>

      <Button
        titulo="Cerrar mi estadía"
        onPress={() => navigation.navigate(hayPedidosEnCurso ? 'S32CierreBloqueado' : 'S30ConfirmarCierre')}
        variante="contorno"
      />
      <View style={{marginTop: spacing.md}}>
        <Button titulo="Me quedo un rato más" onPress={() => navigation.goBack()} variante="contorno" />
      </View>
    </ScreenContainer>
  );
}
