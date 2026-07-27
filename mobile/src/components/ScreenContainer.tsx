import React from 'react';
import {ScrollView, StyleSheet, View, ViewStyle} from 'react-native';
import {SafeAreaView} from 'react-native-safe-area-context';
import {useTheme} from '../theme/ThemeProvider';

type Props = {
  children: React.ReactNode;
  scroll?: boolean;
  style?: ViewStyle;
};

/** Fondo/padding coherentes con el theme activo en TODAS las pantallas - nada de colores hardcodeados (etapa 06). */
export function ScreenContainer({children, scroll = false, style}: Props) {
  const {tokens, spacing} = useTheme();
  return (
    <SafeAreaView style={[styles.safe, {backgroundColor: tokens['color.background']}]} edges={['top', 'bottom']}>
      {scroll ? (
        <ScrollView style={styles.flex} contentContainerStyle={{padding: spacing.lg}}>
          {children}
        </ScrollView>
      ) : (
        <View style={[styles.flex, {padding: spacing.lg}, style]}>{children}</View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {flex: 1},
  flex: {flex: 1},
});
