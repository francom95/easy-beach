import React from 'react';
import {Text, View} from 'react-native';
import {useQuery} from '@tanstack/react-query';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {EstadoCargando} from '../../components/EstadoCarga';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {listarPromociones} from '../../api/catalogo';
import {formatearMonto} from '../../utils/money';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Gap real documentado (etapa 16, alcance acotado - a diferencia del
 * endpoint de ubicaciones, que sí hacía falta agregar porque bloqueaba el
 * flujo central): el endpoint público de promociones (etapa 14) no expone
 * QUÉ productos ni en qué cantidad forman el combo - solo nombre/tipo/valor.
 * Esa info hoy solo la ve el admin. No bloquea la compra: el descuento del
 * combo se calcula automáticamente en el servidor al armar el pedido si el
 * carrito ya tiene los productos correctos (etapa 14), sin que el cliente
 * tenga que "activarlo" a mano - así que esta pantalla es informativa, no
 * un selector de items del combo.
 */
export function S27DetalleCombo({route, navigation}: RootScreenProps<'S27DetalleCombo'>) {
  const {promocionId} = route.params;
  const {tokens, spacing, typeScale} = useTheme();
  const balnearioSlug = useStayStore(s => s.balnearioSlug)!;
  const promosQuery = useQuery({queryKey: ['promociones', balnearioSlug], queryFn: () => listarPromociones(balnearioSlug)});
  const combo = promosQuery.data?.find(p => p.id === promocionId);

  if (promosQuery.isLoading) {
    return (
      <ScreenContainer>
        <EstadoCargando />
      </ScreenContainer>
    );
  }

  return (
    <ScreenContainer>
      <View style={{flex: 1, justifyContent: 'center', gap: spacing.md}}>
        <Text style={{fontSize: typeScale.title, fontWeight: '800', color: tokens['color.on-background']}}>
          {combo?.nombre ?? 'Combo'}
        </Text>
        <Text style={{color: tokens['color.on-surface-muted']}}>
          Precio combo: {combo ? formatearMonto(combo.valor) : '—'}
        </Text>
        <Text style={{color: tokens['color.on-surface-muted']}}>
          El descuento se aplica automáticamente al pagar si tu pedido incluye los productos del combo.
        </Text>
        <Button titulo="Ir al menú" onPress={() => navigation.navigate('MainTabs')} />
      </View>
    </ScreenContainer>
  );
}
