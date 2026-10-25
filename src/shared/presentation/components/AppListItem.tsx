import React, {ReactNode} from 'react';
import {StyleSheet, Text, View} from 'react-native';
import {palette, radius, spacing} from '../theme';
export function AppListItem({title, subtitle, detail, leading, trailing}: {title: string; subtitle?: string; detail?: string; leading?: ReactNode; trailing?: ReactNode}) {
  return <View style={s.row}>{leading && <View style={s.leading}>{leading}</View>}<View style={s.content}><Text style={s.title}>{title}</Text>{subtitle && <Text style={s.subtitle}>{subtitle}</Text>}{detail && <Text style={s.subtitle}>{detail}</Text>}</View>{trailing}</View>;
}
const s = StyleSheet.create({row: {alignItems: 'center', backgroundColor: palette.surface, borderColor: palette.line, borderRadius: radius.md, borderWidth: 1, flexDirection: 'row', marginBottom: spacing.sm, padding: spacing.md}, leading: {marginRight: spacing.md}, content: {flex: 1}, title: {color: palette.text, fontSize: 13, fontWeight: '800'}, subtitle: {color: palette.muted, fontSize: 10, lineHeight: 15, marginTop: spacing.xs}});
