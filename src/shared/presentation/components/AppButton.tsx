import React from 'react';
import {ActivityIndicator, StyleSheet, Text, TouchableOpacity, ViewStyle} from 'react-native';
import {palette, radius, spacing} from '../theme';

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost' | 'success';
export function AppButton({title, onPress, variant = 'primary', loading = false, disabled = false, compact = false, style}: {
  title: string; onPress: () => void; variant?: Variant; loading?: boolean; disabled?: boolean; compact?: boolean; style?: ViewStyle;
}) {
  const inactive = disabled || loading;
  return <TouchableOpacity accessibilityRole="button" disabled={inactive} onPress={onPress}
    style={[s.base, s[variant], compact && s.compact, inactive && s.disabled, style]}>
    {loading && <ActivityIndicator size="small" color={variant === 'primary' ? '#fff' : palette.blue} />}
    <Text style={[s.text, s[`${variant}Text`]]}>{title}</Text>
  </TouchableOpacity>;
}
const s = StyleSheet.create({
  base: {alignItems: 'center', borderRadius: radius.md, borderWidth: 1, flexDirection: 'row', gap: spacing.sm, justifyContent: 'center', minHeight: 48, paddingHorizontal: spacing.lg, paddingVertical: spacing.md},
  compact: {minHeight: 40, paddingHorizontal: spacing.md, paddingVertical: spacing.sm}, disabled: {opacity: 0.45},
  primary: {backgroundColor: palette.blue, borderColor: palette.blue}, secondary: {backgroundColor: palette.surface, borderColor: palette.line},
  danger: {backgroundColor: palette.dangerSoft, borderColor: palette.dangerSoft}, ghost: {backgroundColor: 'transparent', borderColor: 'transparent'},
  success: {backgroundColor: palette.successSoft, borderColor: palette.successSoft}, text: {fontSize: 14, fontWeight: '800'},
  primaryText: {color: '#fff'}, secondaryText: {color: palette.text}, dangerText: {color: palette.danger}, ghostText: {color: palette.blue}, successText: {color: palette.success},
});
