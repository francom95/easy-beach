import React from 'react';
import {StyleSheet, Text, View} from 'react-native';
import {useTheme} from '../theme/ThemeProvider';
import {useNetworkStatus} from '../utils/useNetworkStatus';
import {IconWifiOff} from './icons/StateIcons';

/**
 * Patrón "banner amarillo arriba con hora de última actualización" (etapa 07
 * S09/S21): la app sigue usable con datos cacheados, nunca un bloqueo total
 * salvo que la acción en curso sea crítica (eso lo maneja S33 aparte).
 */
export function BannerSinConexion({ultimaActualizacion}: {ultimaActualizacion?: Date}) {
  const conectado = useNetworkStatus();
  const {tokens, spacing} = useTheme();

  if (conectado) {
    return null;
  }

  const hora = ultimaActualizacion
    ? ultimaActualizacion.toLocaleTimeString('es-AR', {hour: '2-digit', minute: '2-digit'})
    : null;

  const colorTexto = tokens['color.on-warning']!;
  return (
    <View style={[styles.banner, {backgroundColor: tokens['color.warning'], padding: spacing.sm}]}>
      <View style={[styles.fila, {gap: spacing.sm}]}>
        <IconWifiOff color={colorTexto} size={16} />
        <Text style={[styles.textoPrincipal, {color: colorTexto}]}>
          {hora ? `Sin conexión · última actualización ${hora}` : 'Sin conexión'}
        </Text>
      </View>
      <Text style={[styles.textoSecundario, {color: colorTexto}]}>Los datos pueden no estar al día</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: {flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap'},
  fila: {flexDirection: 'row', alignItems: 'center'},
  textoPrincipal: {fontWeight: '600'},
  textoSecundario: {fontSize: 12, opacity: 0.9},
});
