/* eslint-disable react-native/no-inline-styles */
import React, {useEffect, useMemo, useState} from 'react';
import {Alert, ScrollView, Text, TextInput, TouchableOpacity, View} from 'react-native';
import {
  CallHistoryEntry,
  clearHistory,
  loadHistory,
} from '../../blocking/application/blockingService';
import {colors, styles} from '../../blocking/presentation/styles';

export function HistoryScreen() {
  const [events, setEvents] = useState<CallHistoryEntry[]>([]);
  const [query, setQuery] = useState('');
  const [action, setAction] = useState<'all' | 'block' | 'reject' | 'silence'>('all');

  useEffect(() => { loadHistory().then(setEvents); }, []);

  const filtered = useMemo(() => events.filter(event => {
    const matchesQuery = !query || event.number.toLowerCase().includes(query.toLowerCase()) || event.reason.toLowerCase().includes(query.toLowerCase());
    return matchesQuery && (action === 'all' || event.action === action);
  }), [events, query, action]);

  const removeAll = () => Alert.alert('Borrar historial', 'Esta acción no se puede deshacer.', [
    {text: 'Cancelar', style: 'cancel'},
    {text: 'Borrar', style: 'destructive', onPress: () => { clearHistory(); setEvents([]); }},
  ]);

  return <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{paddingBottom: 120}}><Text style={styles.pageTitle}>Historial protegido</Text><Text style={styles.pageSubtitle}>Actividad almacenada únicamente en este dispositivo.</Text><TextInput value={query} onChangeText={setQuery} placeholder="Buscar número o motivo" placeholderTextColor={colors.muted} style={styles.input} /><View style={{flexDirection: 'row', gap: 6, marginVertical: 12}}>{(['all', 'block', 'reject', 'silence'] as const).map(value => <TouchableOpacity key={value} onPress={() => setAction(value)} style={{paddingHorizontal: 10, paddingVertical: 7, borderRadius: 14, backgroundColor: action === value ? colors.blue : '#fff'}}><Text style={{fontSize: 11, color: action === value ? '#fff' : colors.text}}>{value === 'all' ? 'Todas' : value === 'block' ? 'Bloqueadas' : value === 'reject' ? 'Rechazadas' : 'Silenciadas'}</Text></TouchableOpacity>)}</View>{filtered.length === 0 && <Text style={styles.muted}>No hay llamadas que coincidan.</Text>}{filtered.map(event => <View key={event.id} style={styles.historyRow}><View style={styles.callIcon}><Text>×</Text></View><View style={styles.rowMain}><Text style={styles.number}>{event.number}</Text><Text style={styles.muted}>{event.reason} · {translateAction(event.action)}</Text><Text style={styles.muted}>{new Date(event.timestamp).toLocaleString()}</Text></View></View>)}{events.length > 0 && <TouchableOpacity style={styles.secondaryButton} onPress={removeAll}><Text style={styles.remove}>Borrar historial</Text></TouchableOpacity>}</ScrollView>;
}

function translateAction(action: string) {
  if (action === 'silence') return 'Silenciada';
  if (action === 'block') return 'Bloqueada';
  return 'Rechazada';
}
