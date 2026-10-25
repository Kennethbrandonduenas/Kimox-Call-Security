import React, {useState} from 'react';
import {Alert} from 'react-native';
import {AppButton, AppEmptyState, AppInlineForm, AppPageHeader, AppScreen} from '../../../../shared/presentation/components';
import {BlockedNumber} from '../../domain/BlockedNumber';
import {normalizePhoneNumber} from '../../domain/phoneNumber';
import {requestCallScreeningRole, syncBlockedNumbers} from '../../application/blockingService';
import {BlockedNumberRow} from '../components/BlockedNumberRow';

export function BlockedNumbersScreen({blocked, setBlocked}: {blocked: BlockedNumber[]; setBlocked: (value: BlockedNumber[]) => void}) {
  const [value, setValue] = useState('');
  const add = () => {
    const number = normalizePhoneNumber(value.trim());
    if (number.replace(/\D/g, '').length < 7) return Alert.alert('Número inválido', 'Introduce un número válido.');
    if (blocked.some(item => normalizePhoneNumber(item.number) === number)) return Alert.alert('Número existente', 'Este número ya está bloqueado.');
    const updated = [...blocked, {number, label: 'Bloqueo personal'}];
    setBlocked(updated); syncBlockedNumbers(updated); setValue('');
  };
  const remove = (item: BlockedNumber) => Alert.alert('Quitar bloqueo', `¿Permitir nuevamente llamadas de ${item.number}?`, [
    {text: 'Cancelar', style: 'cancel'},
    {text: 'Quitar', style: 'destructive', onPress: () => { const updated = blocked.filter(kept => kept.number !== item.number); setBlocked(updated); syncBlockedNumbers(updated); }},
  ]);
  return <AppScreen>
    <AppPageHeader title="Números bloqueados" subtitle="Kimox aplicará a estos números la acción elegida en Reglas." />
    <AppButton title="Activar filtrado y permisos" variant="success" onPress={requestCallScreeningRole} />
    <AppInlineForm value={value} onChangeText={setValue} placeholder="+51 999 999 999" keyboardType="phone-pad" onSubmit={add}/>
    {blocked.length === 0 && <AppEmptyState message="No hay números bloqueados." />}
    {blocked.map(item => <BlockedNumberRow key={item.number} item={item} onRemove={() => remove(item)} />)}
  </AppScreen>;
}
