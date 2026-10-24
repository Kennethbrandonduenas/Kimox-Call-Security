import React, {useEffect, useState} from 'react';
import {
  ActivityIndicator,
  Alert,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {
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

type ListKey = 'whitelist' | 'blockedPrefixes' | 'blockedCountries';

export function SecurityRulesScreen() {
  const [rules, setRules] = useState<SecurityRules>(DEFAULT_SECURITY_RULES);
  const [loading, setLoading] = useState(true);
  const [whitelistInput, setWhitelistInput] = useState('');
  const [prefixInput, setPrefixInput] = useState('');
  const [countryInput, setCountryInput] = useState('');

  useEffect(() => {
    loadSecurityRules()
      .then(setRules)
      .finally(() => setLoading(false));
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
    return <ActivityIndicator style={localStyles.loader} color={colors.blue} />;
  }

  return (
    <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={localStyles.scrollContent}>
      <Text style={styles.pageTitle}>Reglas de seguridad</Text>
      <Text style={styles.pageSubtitle}>
        La lista blanca siempre tiene prioridad sobre cualquier bloqueo.
      </Text>

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

      <Text style={localStyles.sectionTitle}>Reglas automáticas</Text>
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

      <Text style={localStyles.sectionTitle}>Retención del historial</Text>
      <View style={localStyles.actionRow}>
        {([7, 30, 90, 0] as const).map(days => (
          <TouchableOpacity
            key={days}
            style={[localStyles.actionButton, rules.historyRetentionDays === days && localStyles.actionSelected]}
            onPress={() => updateRules({...rules, historyRetentionDays: days})}>
            <Text style={[localStyles.actionText, rules.historyRetentionDays === days && localStyles.actionTextSelected]}>
              {days === 0 ? 'Siempre' : `${days} días`}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={localStyles.sectionTitle}>Acción al coincidir</Text>
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
    </ScrollView>
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
  return <View style={localStyles.section}><Text style={localStyles.sectionTitle}>{title}</Text><Text style={localStyles.description}>{description}</Text><View style={styles.addRow}><TextInput value={value} onChangeText={onChange} placeholder={placeholder} placeholderTextColor={colors.muted} keyboardType="phone-pad" style={styles.input} /><TouchableOpacity style={styles.addButton} onPress={onAdd}><Text style={styles.addButtonText}>Añadir</Text></TouchableOpacity></View></View>;
}

function RuleValues({values, empty, onRemove}: {values: string[]; empty: string; onRemove: (value: string) => void}) {
  if (!values.length) return <Text style={localStyles.empty}>{empty}</Text>;
  return <View style={localStyles.chips}>{values.map(value => <TouchableOpacity key={value} style={localStyles.chip} onPress={() => onRemove(value)}><Text style={localStyles.chipText}>{value}</Text><Text style={localStyles.chipRemove}> ×</Text></TouchableOpacity>)}</View>;
}

function ToggleRow({label, description, value, onChange}: {label: string; description: string; value: boolean; onChange: (value: boolean) => void}) {
  return <View style={localStyles.toggleRow}><View style={styles.rowMain}><Text style={styles.number}>{label}</Text><Text style={styles.muted}>{description}</Text></View><Switch value={value} onValueChange={onChange} trackColor={{false: '#aab4c3', true: colors.mint}} /></View>;
}

const actionLabels: Record<ScreeningAction, string> = {block: 'Bloquear', reject: 'Rechazar', silence: 'Silenciar'};
function ActionButton({action, selected, onPress}: {action: ScreeningAction; selected: boolean; onPress: () => void}) {
  return <TouchableOpacity style={[localStyles.actionButton, selected && localStyles.actionSelected]} onPress={onPress}><Text style={[localStyles.actionText, selected && localStyles.actionTextSelected]}>{actionLabels[action]}</Text></TouchableOpacity>;
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
  actionRow: {flexDirection: 'row', gap: 8},
  actionButton: {flex: 1, paddingVertical: 12, alignItems: 'center', borderRadius: 10, borderWidth: 1, borderColor: colors.line, backgroundColor: '#fff'},
  actionSelected: {backgroundColor: colors.blue, borderColor: colors.blue},
  actionText: {color: colors.text, fontSize: 12, fontWeight: '800'},
  actionTextSelected: {color: '#fff'},
  footnote: {color: colors.muted, fontSize: 11, lineHeight: 16, marginTop: 10},
});
