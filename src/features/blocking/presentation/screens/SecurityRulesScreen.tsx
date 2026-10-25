import React, {useEffect, useState} from 'react';
import {
  Alert,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import {
  DeviceCountry,
  detectDeviceCountry,
  loadSecurityRules,
  saveSecurityRules,
} from '../../application/blockingService';
import {
  DEFAULT_SECURITY_RULES,
  ScreeningAction,
  SecurityRules,
} from '../../../call-security/domain/SecurityRules';
import {normalizePhoneNumber} from '../../domain/phoneNumber';
import {colors, styles} from '../styles';
import {AppButton, AppChip, AppEmptyState, AppInlineForm, AppLoadingState, AppPageHeader, AppScreen, AppSectionHeader, AppSwitchRow, AppTextField} from '../../../../shared/presentation/components';

type ListKey = 'whitelist' | 'blockedPrefixes' | 'blockedCountries';

export function SecurityRulesScreen() {
  const [rules, setRules] = useState<SecurityRules>(DEFAULT_SECURITY_RULES);
  const [loading, setLoading] = useState(true);
  const [whitelistInput, setWhitelistInput] = useState('');
  const [prefixInput, setPrefixInput] = useState('');
  const [countryInput, setCountryInput] = useState('');
  const [deviceCountry, setDeviceCountry] = useState<DeviceCountry | null>(null);

  useEffect(() => {
    loadSecurityRules()
      .then(setRules)
      .finally(() => setLoading(false));
    detectDeviceCountry().then(setDeviceCountry);
  }, []);

  const updateRules = (next: SecurityRules) => {
    setRules(next);
    saveSecurityRules(next);
  };

  const addValue = (
    key: ListKey,
    rawValue: string,
    clear: () => void,
  ) => {
    const value = normalizePhoneNumber(rawValue);
    if (!value || value.replace(/\D/g, '').length < 2) {
      Alert.alert('Valor inválido', 'Introduce al menos dos dígitos.');
      return;
    }
    if (rules[key].includes(value)) {
      Alert.alert('Regla existente', 'Ese valor ya está configurado.');
      return;
    }
    updateRules({...rules, [key]: [...rules[key], value]});
    clear();
  };

  const removeValue = (key: ListKey, value: string) => {
    Alert.alert('Eliminar regla', `¿Deseas eliminar ${value}?`, [
      {text: 'Cancelar', style: 'cancel'},
      {
        text: 'Eliminar',
        style: 'destructive',
        onPress: () =>
          updateRules({...rules, [key]: rules[key].filter(item => item !== value)}),
      },
    ]);
  };

  if (loading) {
    return <AppLoadingState message="Cargando reglas…" />;
  }

  return (
    <AppScreen>
      <AppPageHeader title="Reglas de seguridad" subtitle="La lista blanca siempre tiene prioridad sobre cualquier bloqueo." />

      <RuleInput
        title="Lista blanca"
        description="Números que siempre podrán llamarte."
        placeholder="+51 999 999 999"
        value={whitelistInput}
        onChange={setWhitelistInput}
        onAdd={() => addValue('whitelist', whitelistInput, () => setWhitelistInput(''))}
      />
      <RuleValues values={rules.whitelist} empty="No hay números permitidos." onRemove={value => removeValue('whitelist', value)} />

      <RuleInput
        title="Prefijos bloqueados"
        description="Ejemplo: 900 bloqueará números que empiecen por 900."
        placeholder="900"
        value={prefixInput}
        onChange={setPrefixInput}
        onAdd={() => addValue('blockedPrefixes', prefixInput, () => setPrefixInput(''))}
      />
      <RuleValues values={rules.blockedPrefixes} empty="No hay prefijos bloqueados." onRemove={value => removeValue('blockedPrefixes', value)} />

      <RuleInput
        title="Países bloqueados"
        description="Introduce el código internacional, por ejemplo +34."
        placeholder="+34"
        value={countryInput}
        onChange={setCountryInput}
        onAdd={() => addValue('blockedCountries', countryInput, () => setCountryInput(''))}
      />
      <RuleValues values={rules.blockedCountries} empty="No hay países bloqueados." onRemove={value => removeValue('blockedCountries', value)} />

      <AppSectionHeader title="Reglas automáticas" />
      <View style={localStyles.countryCard}>
        <Text style={styles.number}>País local</Text>
        <Text style={styles.muted}>
          {deviceCountry
            ? `${deviceCountry.name} (+${deviceCountry.dialingCode || rules.localCountryCode}) · ${countrySource(deviceCountry.source)}`
            : `Código configurado: +${rules.localCountryCode}`}
        </Text>
        <View style={localStyles.countryCodeRow}>
          <Text style={localStyles.plus}>+</Text>
          <AppTextField
            value={rules.localCountryCode}
            onChangeText={value => updateRules({...rules, localCountryCode: normalizePhoneNumber(value)})}
            placeholder="51"
            keyboardType="number-pad"
            style={localStyles.countryCodeInput}
          />
          <AppButton title="Usar detectado" compact onPress={() => deviceCountry?.dialingCode && updateRules({...rules, localCountryCode: deviceCountry.dialingCode})} />
        </View>
      </View>
      <ToggleRow
        label="Bloquear llamadas internacionales"
        description={`Bloquea códigos internacionales distintos de +${rules.localCountryCode || '51'}. La lista blanca siempre se permite.`}
        value={rules.blockInternational}
        onChange={value => updateRules({...rules, blockInternational: value})}
      />
      <ToggleRow
        label="Permitir contactos internacionales"
        description="No bloquea llamadas internacionales si el número está guardado en tus contactos."
        value={rules.allowInternationalContacts}
        onChange={value => updateRules({...rules, allowInternationalContacts: value})}
      />
      <ToggleRow
        label="Números privados"
        description="Filtra llamadas que ocultan el número."
        value={rules.blockPrivate}
        onChange={value => updateRules({...rules, blockPrivate: value})}
      />
      <ToggleRow
        label="Números desconocidos"
        description="Filtra números que no están en tus contactos."
        value={rules.blockUnknown}
        onChange={value => updateRules({...rules, blockUnknown: value})}
      />
      <ToggleRow
        label="Preguntar después de una llamada"
        description="Para números desconocidos permitidos, muestra Bloquear, Permitir siempre o Descartar al finalizar."
        value={rules.postCallPromptEnabled}
        onChange={value => updateRules({...rules, postCallPromptEnabled: value})}
      />
      <ToggleRow
        label="Notificaciones"
        description="Avisa cuando Kimox filtre una llamada."
        value={rules.notificationsEnabled}
        onChange={value => updateRules({...rules, notificationsEnabled: value})}
      />
      <ToggleRow
        label="Ocultar números en notificaciones"
        description="Muestra únicamente los últimos cuatro dígitos."
        value={rules.hideNumbersInNotifications}
        onChange={value => updateRules({...rules, hideNumbersInNotifications: value})}
      />

      <AppSectionHeader title="Retención del historial" />
      <View style={localStyles.actionRow}>
        {([7, 30, 90, 0] as const).map(days => (
          <AppChip key={days} selected={rules.historyRetentionDays === days} onPress={() => updateRules({...rules, historyRetentionDays: days})} label={days === 0 ? 'Siempre' : `${days} días`} />
        ))}
      </View>

      <AppSectionHeader title="Acción al coincidir" />
      <View style={localStyles.actionRow}>
        {(['block', 'reject', 'silence'] as ScreeningAction[]).map(action => (
          <ActionButton
            key={action}
            action={action}
            selected={rules.action === action}
            onPress={() => updateRules({...rules, action})}
          />
        ))}
      </View>
      <Text style={localStyles.footnote}>
        “Silenciar” deja visible la llamada sin hacer sonar el teléfono. “Rechazar” la corta; “Bloquear” impide que continúe.
      </Text>
    </AppScreen>
  );
}

function RuleInput({title, description, placeholder, value, onChange, onAdd}: {
  title: string;
  description: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
  onAdd: () => void;
}) {
  return <View style={localStyles.section}><AppSectionHeader title={title} description={description} /><AppInlineForm value={value} onChangeText={onChange} placeholder={placeholder} keyboardType="phone-pad" onSubmit={onAdd}/></View>;
}

function RuleValues({values, empty, onRemove}: {values: string[]; empty: string; onRemove: (value: string) => void}) {
  if (!values.length) return <AppEmptyState message={empty} />;
  return <View style={localStyles.chips}>{values.map(value => <AppChip key={value} label={`${value} ×`} tone="danger" onPress={() => onRemove(value)} />)}</View>;
}

function ToggleRow({label, description, value, onChange}: {label: string; description: string; value: boolean; onChange: (value: boolean) => void}) {
  return <AppSwitchRow title={label} description={description} value={value} onValueChange={onChange} />;
}

function countrySource(source: DeviceCountry['source']) {
  if (source === 'SIM') return 'detectado por SIM';
  if (source === 'RED_MOVIL') return 'detectado por red móvil';
  if (source === 'REGION_ANDROID') return 'detectado por región';
  return 'valor predeterminado';
}

const actionLabels: Record<ScreeningAction, string> = {block: 'Bloquear', reject: 'Rechazar', silence: 'Silenciar'};
function ActionButton({action, selected, onPress}: {action: ScreeningAction; selected: boolean; onPress: () => void}) {
  return <AppChip selected={selected} onPress={onPress} label={actionLabels[action]} />;
}

const localStyles = StyleSheet.create({
  scrollContent: {paddingBottom: 120},
  loader: {marginTop: 60},
  section: {marginTop: 18},
  sectionTitle: {color: colors.text, fontSize: 16, fontWeight: '800', marginBottom: 5},
  description: {color: colors.muted, fontSize: 12, marginBottom: 10},
  empty: {color: colors.muted, fontSize: 12, marginBottom: 4},
  chips: {flexDirection: 'row', flexWrap: 'wrap', gap: 8},
  chip: {flexDirection: 'row', backgroundColor: '#e8efff', borderRadius: 18, paddingHorizontal: 12, paddingVertical: 8},
  chipText: {color: colors.text, fontWeight: '700'},
  chipRemove: {color: '#c73948', fontWeight: '900'},
  toggleRow: {backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: 12, padding: 14, flexDirection: 'row', alignItems: 'center', marginBottom: 9},
  countryCard: {backgroundColor: '#eaf8f5', borderRadius: 12, padding: 14, marginBottom: 9},
  countryCodeRow: {flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 10},
  countryCodeInput: {minHeight: 44},
  plus: {color: colors.text, fontSize: 20, fontWeight: '800'},
  actionRow: {flexDirection: 'row', gap: 8},
  actionButton: {flex: 1, paddingVertical: 12, alignItems: 'center', borderRadius: 10, borderWidth: 1, borderColor: colors.line, backgroundColor: '#fff'},
  actionSelected: {backgroundColor: colors.blue, borderColor: colors.blue},
  actionText: {color: colors.text, fontSize: 12, fontWeight: '800'},
  actionTextSelected: {color: '#fff'},
  footnote: {color: colors.muted, fontSize: 11, lineHeight: 16, marginTop: 10},
});
