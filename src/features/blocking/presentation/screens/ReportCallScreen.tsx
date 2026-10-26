import React, {useState} from 'react';
import {Alert} from 'react-native';
import {AppButton, AppNotice, AppPageHeader, AppScreen, AppTextField} from '../../../../shared/presentation/components';
import {syncBlockedNumbers} from '../../application/blockingService';
import {BlockedNumber} from '../../domain/BlockedNumber';
import {normalizePhoneNumber} from '../../domain/phoneNumber';

type Props = {blocked: BlockedNumber[]; setBlocked: (numbers: BlockedNumber[]) => void};

export function ReportCallScreen({blocked, setBlocked}: Props) {
  const [number, setNumber] = useState('');
  const normalized = normalizePhoneNumber(number.trim());
  const isValid = normalized.replace(/\D/g, '').length >= 7;
  const block = async () => {
    if (!isValid) return Alert.alert('Número inválido', 'Ingresa el número de la llamada para bloquearlo.');
    if (blocked.some(item => normalizePhoneNumber(item.number) === normalized)) return Alert.alert('Ya está bloqueado', 'Este número ya está en tu lista de bloqueos.');
    const updated = [...blocked, {number: normalized, label: 'Reportado manualmente'}];
    try {
      await syncBlockedNumbers(updated);
      setBlocked(updated); setNumber('');
      Alert.alert('Número bloqueado', 'Kimox lo sincronizó con Call Directory de iOS.');
    } catch {
      Alert.alert('No se pudo sincronizar', 'Revisa que la extensión Call Directory esté configurada y activada en el iPhone.');
    }
  };
  const allow = () => {
    if (!isValid) return Alert.alert('Número inválido', 'Ingresa el número de la llamada para registrarlo.');
    setNumber(''); Alert.alert('Llamada permitida', 'No se agregará ninguna regla de bloqueo para este número.');
  };
  return <AppScreen>
    <AppPageHeader title="Reportar llamada" subtitle="En iPhone, reporta manualmente una llamada para decidir qué hacer con su número." />
    <AppNotice tone="info" title="Funcionamiento de iOS" message="iOS no avisa a Kimox cuando una llamada termina. Esta pantalla permite aplicar la decisión manualmente y sincronizarla con Call Directory." />
    <AppTextField label="Número de teléfono" value={number} onChangeText={setNumber} placeholder="+51 999 999 999" keyboardType="phone-pad" helper="Puedes copiar y pegar el número desde el historial del teléfono." />
    <AppButton title="Bloquear número" onPress={block} />
    <AppButton title="Permitir llamada" variant="secondary" onPress={allow} />
    <AppButton title="Omitir" variant="ghost" onPress={() => setNumber('')} />
  </AppScreen>;
}
