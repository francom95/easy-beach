import React, {useEffect, useRef} from 'react';
import {Animated, Dimensions, StyleSheet} from 'react-native';
import type {RootScreenProps} from '../../navigation/types';

const {width, height} = Dimensions.get('window');
const DIAGONAL = Math.sqrt(width * width + height * height);

/**
 * Única transición entre las dos identidades (etapa 06): expansión radial
 * del `color.primary` del balneario elegido, 250ms, y recién ahí se navega
 * a S04 (splash del balneario) - último instante donde existe la marca
 * EasyBeach de fondo.
 */
export function S03Transicion({route, navigation}: RootScreenProps<'S03Transicion'>) {
  const {slug, nombre, colorOrigen} = route.params;
  const escala = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.timing(escala, {
      toValue: 1,
      duration: 250,
      useNativeDriver: true,
    }).start(() => {
      navigation.replace('S04SplashBalneario', {slug, nombre});
    });
  }, [escala, navigation, slug, nombre]);

  return (
    <Animated.View
      style={[
        styles.circulo,
        {
          backgroundColor: colorOrigen,
          transform: [{scale: escala.interpolate({inputRange: [0, 1], outputRange: [0, (DIAGONAL / 40) * 2]})}],
        },
      ]}
    />
  );
}

const styles = StyleSheet.create({
  circulo: {
    position: 'absolute',
    top: height / 2 - 20,
    left: width / 2 - 20,
    width: 40,
    height: 40,
    borderRadius: 20,
  },
});
