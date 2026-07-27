import React from 'react';
import {ActivityIndicator, StyleSheet, Text, View} from 'react-native';
import {useTheme} from '../theme/ThemeProvider';
import {Button} from './Button';
import {StateRow} from './StateRow';

/**
 * Toda pantalla de la etapa 07 define sus 4 estados (data/empty/loading/
 * error) - estos tres componentes son la implementación común para no
 * repetirla pantalla por pantalla.
 */

export function EstadoCargando({texto = 'Cargando...'}: {texto?: string}) {
  const {tokens, spacing} = useTheme();
  return (
    <View style={[styles.centro, {gap: spacing.md}]}>
      <ActivityIndicator size="large" color={tokens['color.primary']} />
      <Text style={{color: tokens['color.on-surface-muted']}}>{texto}</Text>
    </View>
  );
}

export function EstadoVacio({titulo, descripcion}: {titulo: string; descripcion?: string}) {
  const {tokens, spacing, typeScale} = useTheme();
  return (
    <View style={[styles.centro, {gap: spacing.sm}]}>
      <Text style={{fontSize: typeScale.title, fontWeight: '700', color: tokens['color.on-surface']}}>{titulo}</Text>
      {descripcion ? <Text style={{color: tokens['color.on-surface-muted'], textAlign: 'center'}}>{descripcion}</Text> : null}
    </View>
  );
}

export function EstadoError({mensaje, onReintentar}: {mensaje: string; onReintentar?: () => void}) {
  const {spacing} = useTheme();
  return (
    <View style={[styles.centro, {gap: spacing.md}]}>
      <StateRow tipo="error" texto={mensaje} tamano={28} />
      {onReintentar ? <Button titulo="Reintentar" onPress={onReintentar} fullWidth={false} variante="contorno" /> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  centro: {flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24},
});
