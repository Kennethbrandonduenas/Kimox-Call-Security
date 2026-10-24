/* eslint-disable react-native/no-inline-styles */
import React, {useState} from 'react';
import {Alert, ScrollView, Text, TextInput, TouchableOpacity, View} from 'react-native';
import {BlockedNumber} from '../../domain/BlockedNumber';
import {normalizePhoneNumber} from '../../domain/phoneNumber';
import {requestCallScreeningRole, syncBlockedNumbers} from '../../application/blockingService';
import {BlockedNumberRow} from '../components/BlockedNumberRow';
import {colors, styles} from '../styles';

export function BlockedNumbersScreen({blocked, setBlocked}: {blocked: BlockedNumber[]; setBlocked: (value: BlockedNumber[]) => void}) {
  const [value, setValue] = useState('');

  const add = () => {
    const number = normalizePhoneNumber(value.trim());
    if (number.replace(/\D/g, '').length < 7) return Alert.alert('Número inválido', 'Introduce un número válido.');
    if (blocked.some(item => normalizePhoneNumber(item.number) === number)) return Alert.alert('Número existente', 'Este número ya está bloqueado.');
    const updated = [...blocked, {number, label: 'Bloqueo personal'}];
    setBlocked(updated);
    syncBlockedNumbers(updated);
    setValue('');
  };

  const remove = (item: BlockedNumber) => Alert.alert('Quitar bloqueo', `¿Permitir nuevamente llamadas de ${item.number}?`, [
    {text: 'Cancelar', style: 'cancel'},
    {text: 'Quitar', style: 'destructive', onPress: () => { const updated = blocked.filter(valueToKeep => valueToKeep.number !== item.number); setBlocked(updated); syncBlockedNumbers(updated); }},
  ]);

  return <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{paddingBottom: 120}}><Text style={styles.pageTitle}>Números bloqueados</Text><Text style={styles.pageSubtitle}>Kimox aplicará a estos números la acción elegida en Reglas.</Text><TouchableOpacity style={styles.roleButton} onPress={requestCallScreeningRole}><Text style={styles.roleButtonText}>Activar filtrado y permisos</Text></TouchableOpacity><View style={styles.addRow}><TextInput value={value} onChangeText={setValue} placeholder="+51 999 999 999" placeholderTextColor={colors.muted} keyboardType="phone-pad" style={styles.input} /><TouchableOpacity style={styles.addButton} onPress={add}><Text style={styles.addButtonText}>Añadir</Text></TouchableOpacity></View>{blocked.length === 0 && <Text style={styles.muted}>No hay números bloqueados.</Text>}{blocked.map(item => <BlockedNumberRow key={item.number} item={item} onRemove={() => remove(item)} />)}</ScrollView>;
}
