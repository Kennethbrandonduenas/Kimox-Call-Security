import React from 'react';
import {StyleSheet, Switch, Text, View} from 'react-native';
import {palette, radius, spacing} from '../theme';
export function AppSwitchRow({title, description, value, onValueChange}: {title: string; description?: string; value: boolean; onValueChange: (value: boolean) => void}) {
  return <View style={s.row}><View style={s.content}><Text style={s.title}>{title}</Text>{description && <Text style={s.description}>{description}</Text>}</View><Switch value={value} onValueChange={onValueChange} trackColor={{false: '#aab4c3', true: palette.mint}} /></View>;
}
const s = StyleSheet.create({row: {alignItems: 'center', backgroundColor: palette.surface, borderColor: palette.line, borderRadius: radius.md, borderWidth: 1, flexDirection: 'row', marginBottom: spacing.sm, padding: spacing.md}, content: {flex: 1, marginRight: spacing.md}, title: {color: palette.text, fontSize: 14, fontWeight: '800'}, description: {color: palette.muted, fontSize: 11, lineHeight: 16, marginTop: spacing.xs}});
