import React from 'react';
import {StyleSheet, Text, View} from 'react-native';
import {palette, radius, spacing} from '../theme';
type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger';
export function AppBadge({label, tone = 'neutral'}: {label: string; tone?: Tone}) {return <View style={[s.base, s[tone]]}><Text style={[s.text, s[`${tone}Text`]]}>{label}</Text></View>;}
const s = StyleSheet.create({base: {alignSelf: 'flex-start', borderRadius: radius.pill, paddingHorizontal: spacing.sm, paddingVertical: spacing.xs}, text: {fontSize: 10, fontWeight: '800'}, neutral: {backgroundColor: '#eef1f5'}, info: {backgroundColor: palette.infoSoft}, success: {backgroundColor: palette.successSoft}, warning: {backgroundColor: palette.warningSoft}, danger: {backgroundColor: palette.dangerSoft}, neutralText: {color: palette.muted}, infoText: {color: palette.blue}, successText: {color: palette.success}, warningText: {color: palette.warning}, dangerText: {color: palette.danger}});
