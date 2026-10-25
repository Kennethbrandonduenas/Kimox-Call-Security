import React, {ReactNode} from 'react';
import {StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {AppCard} from './AppCard';
import {palette, spacing} from '../theme';
export function AppAccordion({header, children, expanded, onToggle}: {header: ReactNode; children: ReactNode; expanded: boolean; onToggle: () => void}) {
  return <AppCard style={s.card}><TouchableOpacity accessibilityRole="button" accessibilityState={{expanded}} style={s.header} onPress={onToggle}><View style={s.headerContent}>{header}</View><Text style={s.icon}>{expanded ? '⌃' : '⌄'}</Text></TouchableOpacity>{expanded && <View style={s.content}>{children}</View>}</AppCard>;
}
const s = StyleSheet.create({card: {marginBottom: spacing.sm, padding: 0}, header: {alignItems: 'center', flexDirection: 'row', padding: spacing.md}, headerContent: {flex: 1}, icon: {color: palette.muted, fontSize: 20}, content: {borderTopColor: palette.line, borderTopWidth: 1, padding: spacing.md}});
