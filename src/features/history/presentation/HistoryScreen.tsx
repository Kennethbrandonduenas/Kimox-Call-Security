import React, {useEffect, useMemo, useState} from 'react';
import {Alert, StyleSheet, Text, View} from 'react-native';
import {AppButton, AppChip, AppEmptyState, AppListItem, AppPageHeader, AppScreen, AppTextField} from '../../../shared/presentation/components';
import {CallHistoryEntry, clearHistory, loadHistory} from '../../blocking/application/blockingService';

export function HistoryScreen() {
  const [events, setEvents] = useState<CallHistoryEntry[]>([]);
  const [query, setQuery] = useState('');
  const [action, setAction] = useState<'all' | 'allowed' | 'block' | 'reject' | 'silence'>('all');
  useEffect(() => { loadHistory().then(setEvents); }, []);
  const filtered = useMemo(() => events.filter(event => {
    const matches = !query || event.number.toLowerCase().includes(query.toLowerCase()) || event.reason.toLowerCase().includes(query.toLowerCase());
    return matches && (action === 'all' || event.action === action);
  }), [events, query, action]);
  const removeAll = () => Alert.alert('Borrar historial', 'Esta acción no se puede deshacer.', [
    {text: 'Cancelar', style: 'cancel'}, {text: 'Borrar', style: 'destructive', onPress: () => { clearHistory(); setEvents([]); }},
  ]);
  return <AppScreen>
    <AppPageHeader title="Historial protegido" subtitle="Actividad almacenada únicamente en este dispositivo." />
    <AppTextField value={query} onChangeText={setQuery} placeholder="Buscar número o motivo" />
    <View style={s.filters}>{(['all', 'allowed', 'block', 'reject', 'silence'] as const).map(value => <AppChip key={value} selected={action === value} onPress={() => setAction(value)} label={value === 'all' ? 'Todas' : value === 'allowed' ? 'Permitidas' : value === 'block' ? 'Bloqueadas' : value === 'reject' ? 'Rechazadas' : 'Silenciadas'} />)}</View>
    {filtered.length === 0 && <AppEmptyState message="No hay llamadas que coincidan." />}
    {filtered.map(event => <AppListItem key={event.id} title={event.number} subtitle={`${event.reason} · ${translateAction(event.action)}`} detail={new Date(event.timestamp).toLocaleString()} leading={<Text>×</Text>}/>)}
    {events.length > 0 && <AppButton title="Borrar historial" variant="danger" onPress={removeAll} />}
  </AppScreen>;
}
function translateAction(action: string) {if (action === 'allowed') return 'Permitida'; if (action === 'silence') return 'Silenciada'; if (action === 'block') return 'Bloqueada'; return 'Rechazada';}
const s = StyleSheet.create({filters: {flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginVertical: 12}});
