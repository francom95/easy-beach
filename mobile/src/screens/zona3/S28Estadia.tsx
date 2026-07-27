import React from 'react';
import {Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Card} from '../../components/Card';
import {Button} from '../../components/Button';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {pedidosDeEstadia} from '../../api/pedidos';
import {formatearMonto} from '../../utils/money';
import type {TabScreenProps} from '../../navigation/types';

/**
 * Vive en la tab bar porque es administrativa (etapa 07): ubicación,
 * consumo acumulado, servicios, cierre. El botón de cerrar va en contorno
 * (nunca relleno) para no competir visualmente con los CTA de compra.
 */
export function S28Estadia({navigation}: TabScreenProps<'S28Estadia'>) {
  const {tokens, spacing, typeScale} = useTheme();
  const estadia = useStayStore(s => s.estadia);
  const balnearioNombre = useStayStore(s => s.balnearioNombre);

  const pedidosQuery = useQuery({
    queryKey: ['pedidos', estadia?.publicId],
    queryFn: () => pedidosDeEstadia(estadia!.publicId),
    enabled: Boolean(estadia),
  });

  const consumoTotal = (pedidosQuery.data ?? [])
    .filter(p => p.estado === 'ENTREGADO')
    .reduce((acc, p) => acc + Number(p.total), 0);

  return (
    <ScreenContainer scroll>
      <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background'], marginBottom: spacing.lg}}>
        Tu estadía
      </Text>

      <Card style={{marginBottom: spacing.md}}>
        <Text style={{color: tokens['color.on-surface-muted']}}>Balneario</Text>
        <Text style={{fontWeight: '700', color: tokens['color.on-surface']}}>{balnearioNombre}</Text>
      </Card>
      <Card style={{marginBottom: spacing.md}}>
        <Text style={{color: tokens['color.on-surface-muted']}}>Ubicación</Text>
        <Text style={{fontWeight: '700', color: tokens['color.on-surface']}}>{estadia?.ubicacionIdentificador}</Text>
      </Card>
      <Card style={{marginBottom: spacing.xl}}>
        <Text style={{color: tokens['color.on-surface-muted']}}>Consumo acumulado</Text>
        <Text style={{fontWeight: '800', fontSize: typeScale.price, color: tokens['color.on-surface']}}>
          {formatearMonto(consumoTotal)}
        </Text>
      </Card>

      <Button titulo="Pedir servicio al carpero" onPress={() => navigation.navigate('S23ServiciosHoja')} variante="secundario" />
      <View style={{marginTop: spacing.md}}>
        <Button titulo="Ver promociones" onPress={() => navigation.navigate('S26SeccionPromociones')} variante="contorno" />
      </View>

      <View style={{marginTop: spacing.xxl}}>
        <Button titulo="Cerrar mi estadía" onPress={() => navigation.navigate('S29ResumenConsumo')} variante="contorno" />
      </View>
    </ScreenContainer>
  );
}
