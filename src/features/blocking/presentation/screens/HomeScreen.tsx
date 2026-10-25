import React from 'react';
import {StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {AppButton, AppCard, AppScreen, AppSectionHeader} from '../../../../shared/presentation/components';
import {Stat} from '../components/Stat';
import {styles} from '../styles';

type Props = {protection: boolean; setProtection: (value: boolean) => void; blockedCount: number; historyCount: number; openBlocked: () => void; openHistory: () => void};
export function HomeScreen({protection, setProtection, blockedCount, historyCount, openBlocked, openHistory}: Props) {
  return <AppScreen contentStyle={s.content}>
    <View style={[styles.statusCard, protection ? styles.active : styles.paused]}><View style={s.statusContent}><Text style={styles.statusCaption}>ESTADO DEL DISPOSITIVO</Text><Text style={styles.statusTitle}>{protection ? 'Protección activa' : 'Protección pausada'}</Text><Text style={styles.statusDescription}>{protection ? 'Las llamadas sospechosas se analizarán en segundo plano.' : 'Activa la protección para filtrar llamadas.'}</Text></View><TouchableOpacity accessibilityRole="switch" accessibilityState={{checked: protection}} onPress={() => setProtection(!protection)}><Text style={s.protectionToggle}>{protection ? '●' : '○'}</Text></TouchableOpacity></View>
    <AppSectionHeader title="Resumen de seguridad" />
    <View style={styles.statsRow}><Stat value={String(historyCount)} label="Bloqueadas"/><Stat value={String(blockedCount)} label="En tu lista"/><Stat value="0" label="Riesgo actual"/></View>
    <AppSectionHeader title="Acciones rápidas" />
    <AppButton title="＋  Bloquear un número" onPress={openBlocked} />
    <AppButton title="Ver historial de llamadas" variant="secondary" onPress={openHistory} style={s.buttonGap} />
    <AppCard tone="success" style={s.info}><Text style={styles.infoTitle}>Protección en segundo plano</Text><Text style={styles.infoText}>Kimox analizará las llamadas aunque la aplicación esté cerrada.</Text></AppCard>
  </AppScreen>;
}
const s = StyleSheet.create({content: {paddingBottom: 24}, statusContent: {flex: 1}, protectionToggle: {color: '#fff', fontSize: 30}, buttonGap: {marginTop: 10}, info: {marginTop: 22}});
