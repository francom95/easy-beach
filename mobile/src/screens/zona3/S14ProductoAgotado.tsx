import React from 'react';
import {View} from 'react-native';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Adaptación real (etapa 16): el menú público (etapa 11) ya excluye los
 * productos no disponibles de la respuesta - "on/off instantáneo" es
 * justamente ese criterio de aceptación. Por eso esta pantalla no se llega
 * tocando un producto atenuado en el listado (no existen, nunca llegan al
 * cliente): se llega cuando el SERVIDOR rechaza un ítem al revalidar el
 * carrito (422 `ITEM_NO_DISPONIBLE`) porque cambió de estado en la ventana
 * entre que se cacheó el menú (etapa 11: cache corto, 15s) y el checkout -
 * ver S15. Sin un endpoint de "productos similares", la salida es volver
 * al menú (honesto, no se inventa una recomendación que no existe).
 */
export function S14ProductoAgotado({navigation}: RootScreenProps<'S14ProductoAgotado'>) {
  const {spacing} = useTheme();
  return (
    <ScreenContainer>
      <View style={{flex: 1, justifyContent: 'center', gap: spacing.lg}}>
        <StateRow tipo="advertencia" texto="Este producto ya no está disponible" tamano={32} />
        <Button titulo="Volver al menú" onPress={() => navigation.reset({index: 0, routes: [{name: 'MainTabs'}]})} />
      </View>
    </ScreenContainer>
  );
}
