import React, {ReactNode} from 'react';
import {StyleSheet, View, ViewStyle} from 'react-native';
import {palette, radius, spacing} from '../theme';
export function AppCard({children, tone = 'default', style}: {children: ReactNode; tone?: 'default' | 'info' | 'success' | 'warning' | 'danger'; style?: ViewStyle}) {
  return <View style={[s.base, s[tone], style]}>{children}</View>;
}
const s = StyleSheet.create({base: {borderRadius: radius.lg, borderWidth: 1, padding: spacing.lg}, default: {backgroundColor: palette.surface, borderColor: palette.line}, info: {backgroundColor: palette.infoSoft, borderColor: palette.infoSoft}, success: {backgroundColor: palette.successSoft, borderColor: palette.successSoft}, warning: {backgroundColor: palette.warningSoft, borderColor: palette.warningSoft}, danger: {backgroundColor: palette.dangerSoft, borderColor: palette.dangerSoft}});
