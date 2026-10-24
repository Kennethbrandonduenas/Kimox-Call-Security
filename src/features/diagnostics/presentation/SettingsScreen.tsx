import React, {useEffect, useState} from 'react';
import {Alert, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View} from 'react-native';
import {
  clearAllData, clearHistory, DiagnosticStatus, getDiagnostics,
  openAccessibilitySettings, openAppSettings, openBatterySettings,
  openOverlaySettings, requestCallScreeningRole,
} from '../../blocking/application/blockingService';
import {exportRules, importRules} from '../../rules-manager/application/RulesBackupService';
import {colors, styles} from '../../blocking/presentation/styles';

const emptyDiagnostics: DiagnosticStatus = {
  callScreening: false, notifications: false, contacts: false,
  overlay: false, accessibility: false, batteryUnrestricted: false,
};

export function SettingsScreen() {
  const [diagnostics, setDiagnostics] = useState(emptyDiagnostics);
  const [backupText, setBackupText] = useState('');
  const [backupPassword, setBackupPassword] = useState('');
  const refresh = () => getDiagnostics().then(setDiagnostics);
  useEffect(() => { refresh(); }, []);

  const importBackup = async () => {
    try {
      await importRules(backupText, backupPassword);
      setBackupText('');
      Alert.alert('Respaldo importado', 'Las reglas se aplicaron correctamente.');
    } catch (error) {
      Alert.alert('No se pudo importar', error instanceof Error ? error.message : 'Contenido inválido.');
    }
  };

  const deleteAll = () => Alert.alert('Borrar todos los datos', 'Se eliminarán reglas, números bloqueados, preferencias e historial.', [
    {text: 'Cancelar', style: 'cancel'},
    {text: 'Borrar', style: 'destructive', onPress: () => { clearAllData(); setBackupText(''); Alert.alert('Datos eliminados'); }},
  ]);

  return <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={localStyles.content}>
    <Text style={styles.pageTitle}>Ajustes y diagnóstico</Text>
    <Text style={styles.pageSubtitle}>Comprueba que Android permita a Kimox proteger tus llamadas.</Text>
    <DiagnosticRow label="Filtrado de llamadas" enabled={diagnostics.callScreening} onPress={requestCallScreeningRole} />
    <DiagnosticRow label="Acceso a contactos" enabled={diagnostics.contacts} onPress={requestCallScreeningRole} />
    <DiagnosticRow label="Notificaciones" enabled={diagnostics.notifications} onPress={requestCallScreeningRole} />
    <DiagnosticRow label="Sin restricción de batería" enabled={diagnostics.batteryUnrestricted} onPress={openBatterySettings} />
    <DiagnosticRow label="Superposición autorizada" enabled={diagnostics.overlay} onPress={openOverlaySettings} />
    <DiagnosticRow label="Servicio de accesibilidad activo" enabled={diagnostics.accessibility} onPress={openAccessibilitySettings} warningOnly />
    <TouchableOpacity style={styles.secondaryButton} onPress={refresh}><Text style={styles.secondaryButtonText}>Actualizar diagnóstico</Text></TouchableOpacity>

    <Text style={localStyles.sectionTitle}>Respaldo local</Text>
    <Text style={localStyles.description}>Usa una contraseña de 8 caracteres o más para cifrarlo. Sin contraseña se exportará como JSON legible.</Text>
    <TextInput value={backupPassword} onChangeText={setBackupPassword} secureTextEntry placeholder="Contraseña del respaldo (opcional)" placeholderTextColor={colors.muted} style={styles.input} />
    <TouchableOpacity style={styles.primaryButton} onPress={() => exportRules(backupPassword).catch(error => Alert.alert('No se pudo compartir', error instanceof Error ? error.message : 'Error de cifrado.'))}><Text style={styles.primaryButtonText}>Exportar reglas</Text></TouchableOpacity>
    <TextInput value={backupText} onChangeText={setBackupText} multiline placeholder="Pega aquí el respaldo JSON o cifrado" placeholderTextColor={colors.muted} style={localStyles.backupInput} />
    <TouchableOpacity style={[styles.secondaryButton, !backupText.trim() && localStyles.disabled]} disabled={!backupText.trim()} onPress={importBackup}><Text style={styles.secondaryButtonText}>Importar respaldo</Text></TouchableOpacity>

    <Text style={localStyles.sectionTitle}>Datos y privacidad</Text>
    <Text style={localStyles.description}>Reglas e historial se cifran mediante Android Keystore y permanecen en este dispositivo.</Text>
    <TouchableOpacity style={styles.secondaryButton} onPress={() => Alert.alert('Borrar historial', '¿Deseas borrar todo el historial?', [{text: 'Cancelar', style: 'cancel'}, {text: 'Borrar', style: 'destructive', onPress: clearHistory}])}><Text style={styles.remove}>Borrar historial</Text></TouchableOpacity>
    <TouchableOpacity style={localStyles.dangerButton} onPress={deleteAll}><Text style={localStyles.dangerText}>Borrar todos mis datos</Text></TouchableOpacity>
    <TouchableOpacity style={styles.secondaryButton} onPress={openAppSettings}><Text style={styles.secondaryButtonText}>Abrir ajustes de Android</Text></TouchableOpacity>
  </ScrollView>;
}

function DiagnosticRow({label, enabled, onPress, warningOnly}: {label: string; enabled: boolean; onPress: () => void; warningOnly?: boolean}) {
  const status = warningOnly ? (enabled ? 'Activo' : 'Inactivo') : (enabled ? 'Correcto' : 'Requiere atención');
  const dotStyle = enabled ? (warningOnly ? localStyles.dotWarning : localStyles.dotOk) : localStyles.dotError;
  const statusStyle = enabled ? (warningOnly ? localStyles.statusWarning : localStyles.statusOk) : localStyles.statusError;
  return <TouchableOpacity style={localStyles.diagnosticRow} onPress={onPress}><View style={[localStyles.dot, dotStyle]} /><Text style={localStyles.diagnosticLabel}>{label}</Text><Text style={[localStyles.status, statusStyle]}>{status}</Text></TouchableOpacity>;
}

const localStyles = StyleSheet.create({
  content: {paddingBottom: 120},
  diagnosticRow: {backgroundColor: '#fff', borderWidth: 1, borderColor: colors.line, borderRadius: 12, padding: 14, flexDirection: 'row', alignItems: 'center', marginBottom: 8},
  dot: {width: 9, height: 9, borderRadius: 5, marginRight: 10},
  dotOk: {backgroundColor: '#1fa77a'}, dotWarning: {backgroundColor: '#f5a623'}, dotError: {backgroundColor: '#df4f5b'},
  diagnosticLabel: {flex: 1, color: colors.text, fontWeight: '700'},
  status: {fontSize: 11, fontWeight: '800'},
  statusOk: {color: '#167458'}, statusWarning: {color: '#a46a00'}, statusError: {color: '#c73948'},
  sectionTitle: {color: colors.text, fontSize: 17, fontWeight: '800', marginTop: 25, marginBottom: 6},
  description: {color: colors.muted, fontSize: 12, lineHeight: 18, marginBottom: 12},
  backupInput: {minHeight: 100, textAlignVertical: 'top', marginTop: 12, padding: 12, backgroundColor: '#fff', color: colors.text, borderWidth: 1, borderColor: colors.line, borderRadius: 12},
  disabled: {opacity: 0.45},
  dangerButton: {borderRadius: 12, paddingVertical: 15, alignItems: 'center', marginTop: 10, backgroundColor: '#fff0f1'},
  dangerText: {color: '#c73948', fontWeight: '800'},
});
