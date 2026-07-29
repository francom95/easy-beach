import React, {useEffect} from 'react';
import {Text, View} from 'react-native';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import type {RootScreenProps} from '../../navigation/types';

const DURACION_CELEBRACION_MS = 3000;

/**
 * Momento corto (3s, saltable) que cierra la venta: si había carrito armado
 * mientras se esperaba (S06), el CTA principal es "ir a pagar"; si no,
 * "seguir mirando el menú". No se muestra el nombre del carpero validador:
 * `EstadiaResponse` no lo expone (gap cosmético, no bloqueante - documentado
 * en el entregable, a diferencia del endpoint de ubicaciones que sí hacía
 * falta agregar).
 */
export function S07EstadiaActiva({navigation}: RootScreenProps<'S07EstadiaActiva'>) {
  const {tokens, spacing} = useTheme();
  const estadia = useStayStore(s => s.estadia);
  const cantidadEnCarrito = useCartStore(s => s.cantidadTotal());

  useEffect(() => {
    const timer = setTimeout(() => continuar(), DURACION_CELEBRACION_MS);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function continuar() {
    navigation.reset({
      index: cantidadEnCarrito > 0 ? 1 : 0,
      routes: cantidadEnCarrito > 0 ? [{name: 'MainTabs'}, {name: 'S15Carrito'}] : [{name: 'MainTabs'}],
    });
  }

  return (
    <ScreenContainer>
      <View style={{flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.md}}>
        <StateRow tipo="exito" texto="¡Estadía confirmada!" tamano={40} />
        <Text style={{color: tokens['color.on-surface-muted'], textAlign: 'center'}}>
          Tu ubicación {estadia?.ubicacionIdentificador ?? ''} quedó confirmada por el equipo del balneario.
        </Text>
        <View style={{marginTop: spacing.xl, width: '100%'}}>
          <Button
            titulo={cantidadEnCarrito > 0 ? 'Ir a pagar mi pedido' : 'Seguir mirando el menú'}
            onPress={continuar}
          />
        </View>
      </View>
    </ScreenContainer>
  );
}
