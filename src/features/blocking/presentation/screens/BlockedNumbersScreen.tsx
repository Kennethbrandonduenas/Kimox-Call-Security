import React, {useState} from 'react';
import {Alert, View} from 'react-native';
import {AppButton, AppEmptyState, AppInlineForm, AppListItem, AppPageHeader, AppScreen} from '../../../../shared/presentation/components';
import {BlockedNumber} from '../../domain/BlockedNumber';
import {normalizePhoneNumber} from '../../domain/phoneNumber';
import {blockDeviceContact, DeviceContact, loadDeviceContacts, requestCallScreeningRole, syncBlockedNumbers, unblockNumber} from '../../application/blockingService';
import {BlockedNumberRow} from '../components/BlockedNumberRow';

export function BlockedNumbersScreen({blocked, setBlocked}: {blocked: BlockedNumber[]; setBlocked: (value: BlockedNumber[]) => void}) {
  const [value, setValue] = useState('');
  const [contacts, setContacts] = useState<DeviceContact[]>([]);
  const [showContacts, setShowContacts] = useState(false);
  const [contactQuery, setContactQuery] = useState('');
  const add = () => {
    const number = normalizePhoneNumber(value.trim());
    if (number.replace(/\D/g, '').length < 7) return Alert.alert('Número inválido', 'Introduce un número válido.');
    if (blocked.some(item => normalizePhoneNumber(item.number) === number)) return Alert.alert('Número existente', 'Este número ya está bloqueado.');
    const updated = [...blocked, {number, label: 'Bloqueo personal'}];
    setBlocked(updated); syncBlockedNumbers(updated); setValue('');
  };
  const remove = (item: BlockedNumber) => Alert.alert('Quitar bloqueo', `¿Permitir nuevamente llamadas de ${item.number}?`, [
    {text: 'Cancelar', style: 'cancel'},
    {text: 'Quitar', style: 'destructive', onPress: () => { const updated = blocked.filter(kept => kept.number !== item.number); setBlocked(updated); unblockNumber(item.number); syncBlockedNumbers(updated); }},
  ]);
  const openContacts = async () => {
    const values = await loadDeviceContacts();
    if (!values.length) return Alert.alert('Sin contactos', 'Autoriza el acceso a contactos en Ajustes para seleccionar uno.');
    setContacts(values); setShowContacts(true);
  };
  const addContact = (contact: DeviceContact) => {
    if (blocked.some(item => normalizePhoneNumber(item.number) === normalizePhoneNumber(contact.number))) return;
    const updated = [...blocked, {number: contact.number, label: contact.name}];
    setBlocked(updated); blockDeviceContact(contact);
  };
  const visibleContacts = contacts.filter(contact => `${contact.name} ${contact.number}`.toLowerCase().includes(contactQuery.toLowerCase())).slice(0, 80);
  return <AppScreen>
    <AppPageHeader title="Números bloqueados" subtitle="Kimox aplicará a estos números la acción elegida en Reglas." />
    <AppButton title="Activar filtrado y permisos" variant="success" onPress={requestCallScreeningRole} />
    <AppInlineForm value={value} onChangeText={setValue} placeholder="+51 999 999 999" keyboardType="phone-pad" onSubmit={add}/>
    <AppButton title="Bloquear un contacto" variant="secondary" onPress={openContacts}/>
    {showContacts && <View>
      <AppInlineForm value={contactQuery} onChangeText={setContactQuery} placeholder="Buscar contacto" onSubmit={() => undefined}/>
      {visibleContacts.map(contact => <AppListItem key={`${contact.number}-${contact.name}`} title={contact.name} subtitle={contact.number} trailing={<AppButton title="Bloquear" variant="ghost" compact onPress={() => addContact(contact)}/>}/>)}
    </View>}
    {blocked.length === 0 && <AppEmptyState message="No hay números bloqueados." />}
    {blocked.map(item => <BlockedNumberRow key={item.number} item={item} onRemove={() => remove(item)} />)}
  </AppScreen>;
}
