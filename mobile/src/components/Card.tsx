import React from 'react';
import {StyleSheet, View, ViewStyle} from 'react-native';
import {useTheme} from '../theme/ThemeProvider';

export function Card({children, style}: {children: React.ReactNode; style?: ViewStyle}) {
  const {tokens, radii, spacing, shadow} = useTheme();
  return (
    <View
      style={[
        styles.base,
        shadow.low,
        {
          backgroundColor: tokens['color.surface'],
          borderRadius: radii.card,
          borderColor: tokens['color.border'],
          padding: spacing.lg,
        },
        style,
      ]}>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  base: {borderWidth: StyleSheet.hairlineWidth},
});
