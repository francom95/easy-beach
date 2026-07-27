import React from 'react';
import {ActivityIndicator, Pressable, StyleSheet, Text} from 'react-native';
import {useTheme} from '../theme/ThemeProvider';

type Variante = 'primario' | 'secundario' | 'destructivo' | 'contorno';

type Props = {
  titulo: string;
  onPress: () => void;
  variante?: Variante;
  cargando?: boolean;
  deshabilitado?: boolean;
  fullWidth?: boolean;
};

/**
 * `destructivo` es el único botón con relleno rojo (etapa 07 S30: "el botón
 * destructivo va relleno, no en contorno - lee distinto a propósito"). El
 * resto de las acciones "salir"/"cancelar" van en `contorno` para no
 * competir visualmente con los CTA de compra.
 */
export function Button({titulo, onPress, variante = 'primario', cargando, deshabilitado, fullWidth = true}: Props) {
  const {tokens, radii, spacing, typeScale} = useTheme();
  const inhabilitado = deshabilitado || cargando;

  const estilosPorVariante: Record<Variante, {fondo: string; texto: string; borde?: string}> = {
    primario: {fondo: tokens['color.primary']!, texto: tokens['color.on-primary']!},
    secundario: {fondo: tokens['color.secondary']!, texto: tokens['color.on-secondary']!},
    destructivo: {fondo: tokens['color.error']!, texto: tokens['color.on-error']!},
    contorno: {fondo: 'transparent', texto: tokens['color.on-surface']!, borde: tokens['color.border']},
  };
  const estilo = estilosPorVariante[variante];

  return (
    <Pressable
      onPress={onPress}
      disabled={inhabilitado}
      style={({pressed}) => [
        styles.base,
        {
          backgroundColor: estilo.fondo,
          borderRadius: radii.pill,
          paddingVertical: spacing.md,
          paddingHorizontal: spacing.xl,
          borderWidth: estilo.borde ? 1.5 : 0,
          borderColor: estilo.borde,
          opacity: inhabilitado ? 0.5 : pressed ? 0.85 : 1,
          alignSelf: fullWidth ? 'stretch' : 'flex-start',
        },
      ]}>
      {cargando ? (
        <ActivityIndicator color={estilo.texto} />
      ) : (
        <Text style={[styles.texto, {color: estilo.texto, fontSize: typeScale.body}]} numberOfLines={1}>
          {titulo}
        </Text>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  base: {
    minHeight: 52,
    alignItems: 'center',
    justifyContent: 'center',
  },
  texto: {
    fontWeight: '700',
  },
});
