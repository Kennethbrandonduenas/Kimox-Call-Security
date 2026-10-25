import React from 'react';
import {StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {palette, radius, spacing} from '../theme';
type Tone = 'success' | 'warning' | 'danger' | 'info';
export function AppStatusRow({title, status, tone, onPress}: {title: string; status: string; tone: Tone; onPress?: () => void}) {
  return <TouchableOpacity disabled={!onPress} onPress={onPress} style={s.row}><View style={[s.dot, s[tone]]}/><Text style={s.title}>{title}</Text><Text style={[s.status, s[`${tone}Text`]]}>{status}</Text></TouchableOpacity>;
}
const s = StyleSheet.create({row: {alignItems: 'center', backgroundColor: palette.surface, borderColor: palette.line, borderRadius: radius.md, borderWidth: 1, flexDirection: 'row', marginBottom: spacing.sm, padding: spacing.md}, dot: {borderRadius: 5, height: 9, marginRight: 10, width: 9}, title: {color: palette.text, flex: 1, fontWeight: '700'}, status: {fontSize: 11, fontWeight: '800'}, success: {backgroundColor: '#1fa77a'}, warning: {backgroundColor: '#f5a623'}, danger: {backgroundColor: palette.danger}, info: {backgroundColor: palette.blue}, successText: {color: palette.success}, warningText: {color: palette.warning}, dangerText: {color: palette.danger}, infoText: {color: palette.blue}});
