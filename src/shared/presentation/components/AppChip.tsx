import React from 'react';
import {StyleSheet, Text, TouchableOpacity} from 'react-native';
import {palette, radius, spacing} from '../theme';
export function AppChip({label, selected = false, onPress, tone = 'default'}: {label: string; selected?: boolean; onPress: () => void; tone?: 'default' | 'danger'}) {
  return <TouchableOpacity onPress={onPress} style={[s.chip, selected && s.selected, tone === 'danger' && s.danger]}><Text style={[s.text, selected && s.selectedText, tone === 'danger' && s.dangerText]}>{label}</Text></TouchableOpacity>;
}
const s = StyleSheet.create({chip: {backgroundColor: palette.surface, borderColor: palette.line, borderRadius: radius.pill, borderWidth: 1, paddingHorizontal: spacing.md, paddingVertical: spacing.sm}, selected: {backgroundColor: palette.blue, borderColor: palette.blue}, danger: {backgroundColor: palette.dangerSoft}, text: {color: palette.text, fontSize: 11, fontWeight: '700'}, selectedText: {color: '#fff'}, dangerText: {color: palette.danger}});
