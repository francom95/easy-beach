import React from 'react';
import {View} from 'react-native';
import {ScreenContainer} from '../../components/ScreenContainer';
import {Button} from '../../components/Button';
import {StateRow} from '../../components/StateRow';
import {useTheme} from '../../theme/ThemeProvider';
import type {RootScreenProps} from '../../navigation/types';

/**
 * Solo aparece cuando una acción crítica falla del todo (etapa 07) - en
 * cualquier otro lado el patrón es el banner amarillo (ver
 * BannerSinConexion), la app sigue usable con lo cacheado.
 */
export function S33SinConexion({route, navigation}: RootScreenProps<'S33SinConexion'>) {
  const {spacing} = useTheme();
  const {alReintentar} = route.params;

  return (
    <ScreenContainer>
      <View style={{flex: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.lg}}>
        <StateRow tipo="advertencia" texto="Sin conexión" tamano={36} />
        <Button
          titulo="Reintentar"
          onPress={() => {
            if (alReintentar) {
              alReintentar();
            } else {
              navigation.goBack();
            }
          }}
          fullWidth={false}
        />
      </View>
    </ScreenContainer>
  );
}
