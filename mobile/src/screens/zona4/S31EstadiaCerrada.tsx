import React from 'react';
import {Text, View} from 'react-native';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import {useStayStore} from '../../store/stayStore';
import {useCartStore} from '../../store/cartStore';
import {formatearMonto} from '../../utils/money';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Última pantalla con identidad del balneario (etapa 06): agradece, cierra
 * la cuenta, planta un incentivo de vuelta. Solo tocando "volver a la lista
 * de balnearios" se recupera la marca EasyBeach (se decachea el theme acá,
 * no antes) - es el único camino de regreso, junto con S36.
 */
export function S31EstadiaCerrada({route, navigation}: RootScreenProps<'S31EstadiaCerrada'>) {
  const {resumen} = route.params;
  const {tokens, spacing, typeScale, volverAMarcaEasyBeach} = useTheme();
  const balnearioNombre = useStayStore(s => s.balnearioNombre);
  const volverASelectorDeBalnearios = useStayStore(s => s.volverASelectorDeBalnearios);
  const vaciarCarrito = useCartStore(s => s.vaciar);

  function volverALaLista() {
    vaciarCarrito();
    volverASelectorDeBalnearios();
    volverAMarcaEasyBeach();
    navigation.reset({index: 0, routes: [{name: 'S02SelectorBalneario'}]});
  }

  return (
    <ScreenContainer>
      <View style={{flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.lg}}>
        <StateRow tipo="exito" texto={`¡Gracias por elegir ${balnearioNombre}!`} tamano={32} />
        <Text style={{color: tokens['color.on-surface-muted'], textAlign: 'center'}}>
          Pasaste {resumen.diasDeEstadia} día(s), {resumen.cantidadPedidos} pedido(s) y consumiste{' '}
          {formatearMonto(resumen.montoTotal)}. Te esperamos la próxima.
        </Text>
        <View style={{width: '100%', marginTop: spacing.xl}}>
          <Button titulo="Volver a la lista de balnearios" onPress={volverALaLista} />
        </View>
      </View>
    </ScreenContainer>
  );
}
