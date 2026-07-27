import React from 'react';
import {StyleSheet, Text, View} from 'react-native';
import {useTheme} from '../theme/ThemeProvider';
import {
  IconAlertTriangle,
  IconCheckCircle,
  IconClock,
  IconInfoCircle,
  IconXCircle,
} from './icons/StateIcons';

export type TipoEstado = 'exito' | 'espera' | 'advertencia' | 'error' | 'info';

const ICONO_POR_TIPO = {
  exito: IconCheckCircle,
  espera: IconClock,
  advertencia: IconAlertTriangle,
  error: IconXCircle,
  info: IconInfoCircle,
};

/**
 * Regla dura de accesibilidad (etapa 06 §normativo 3): "los estados siempre
 * van icono + texto, nunca color solo". Este componente es el único punto
 * donde se pinta un estado - evita que alguna pantalla lo haga solo con color.
 */
export function StateRow({tipo, texto, tamano = 16}: {tipo: TipoEstado; texto: string; tamano?: number}) {
  const {tokens, spacing} = useTheme();
  const colorPorTipo: Record<TipoEstado, string> = {
    exito: tokens['color.success']!,
    espera: tokens['color.info']!,
    advertencia: tokens['color.warning']!,
    error: tokens['color.error']!,
    info: tokens['color.info']!,
  };
  const Icono = ICONO_POR_TIPO[tipo];
  const color = colorPorTipo[tipo];

  return (
    <View style={[styles.fila, {gap: spacing.sm}]}>
      <Icono color={color} size={tamano} />
      <Text style={[styles.texto, {color}]}>{texto}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  fila: {flexDirection: 'row', alignItems: 'center'},
  texto: {fontWeight: '600', flexShrink: 1},
});
