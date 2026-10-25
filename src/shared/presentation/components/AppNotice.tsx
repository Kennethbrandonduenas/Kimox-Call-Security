import React from 'react';
import {StyleSheet, Text} from 'react-native';
import {AppCard} from './AppCard';
import {palette, spacing} from '../theme';
type Tone = 'info' | 'success' | 'warning' | 'danger';
export function AppNotice({title, message, tone = 'info'}: {title: string; message: string; tone?: Tone}) {return <AppCard tone={tone} style={s.card}><Text style={[s.title, s[`${tone}Text`]]}>{title}</Text><Text style={s.message}>{message}</Text></AppCard>;}
const s = StyleSheet.create({card: {marginTop: spacing.lg}, title: {fontSize: 13, fontWeight: '800'}, message: {color: palette.muted, fontSize: 11, lineHeight: 17, marginTop: spacing.xs}, infoText: {color: palette.blue}, successText: {color: palette.success}, warningText: {color: palette.warning}, dangerText: {color: palette.danger}});
