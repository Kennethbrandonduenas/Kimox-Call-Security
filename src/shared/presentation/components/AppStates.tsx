import React from 'react';
import {ActivityIndicator, StyleSheet, Text, View} from 'react-native';
import {AppButton} from './AppButton';
import {palette, spacing} from '../theme';
export function AppLoadingState({message = 'Cargando…'}: {message?: string}) {return <View style={s.state}><ActivityIndicator color={palette.blue}/><Text style={s.text}>{message}</Text></View>;}
export function AppErrorState({message, onRetry}: {message: string; onRetry?: () => void}) {return <View style={s.state}><Text style={s.error}>{message}</Text>{onRetry && <AppButton title="Reintentar" variant="secondary" compact onPress={onRetry}/>}</View>;}
const s = StyleSheet.create({state: {alignItems: 'center', gap: spacing.md, padding: spacing.xxl}, text: {color: palette.muted}, error: {color: palette.danger, lineHeight: 19, textAlign: 'center'}});
