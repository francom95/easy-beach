import React from 'react';
import {Text, View} from 'react-native';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Vive ANTES de la frontera white-label a propósito (etapa 07): el theme
 * del balneario nunca llega a cargar si no está operativo. Nunca dice
 * "suspendido por falta de pago" - siempre "cerrado por temporada" de cara
 * al cliente (el motivo real es un problema comercial entre EasyBeach y el
 * balneario, no algo que el cliente necesite saber).
 */
export function S34FueraDeTemporada({route, navigation}: RootScreenProps<'S34FueraDeTemporada'>) {
  const {tokens, spacing, typeScale} = useTheme();
  const {nombreBalneario} = route.params;

  return (
    <ScreenContainer>
      <View style={{flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.lg}}>
        <StateRow tipo="info" texto={`${nombreBalneario} está cerrado por temporada`} tamano={28} />
        <Text style={{color: tokens['color.on-surface-muted'], textAlign: 'center', fontSize: typeScale.body}}>
          Elegí otro balneario para seguir.
        </Text>
        <Button
          titulo="Elegir otro balneario"
          onPress={() => navigation.reset({index: 0, routes: [{name: 'S02SelectorBalneario'}]})}
        />
      </View>
    </ScreenContainer>
  );
}
