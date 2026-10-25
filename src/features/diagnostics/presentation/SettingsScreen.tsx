import React, {useEffect, useState} from 'react';
import {Alert, StyleSheet} from 'react-native';
import {AppButton, AppPageHeader, AppScreen, AppSectionHeader, AppStatusRow, AppTextField} from '../../../shared/presentation/components';
import {
  clearAllData, clearHistory, DiagnosticStatus, getDiagnostics, openAccessibilitySettings,
  openAppSettings, openBatterySettings, openManufacturerBatterySettings, openOverlaySettings,
  requestCallScreeningRole,
} from '../../blocking/application/blockingService';
import {exportRules, importRules} from '../../rules-manager/application/RulesBackupService';

const emptyDiagnostics: DiagnosticStatus = {callScreening: false, notifications: false, contacts: false, overlay: false, accessibility: false, batteryUnrestricted: false, backgroundReady: false, periodicAudit: false, legacyCallReceiver: false, lastPhoneEventAt: 0, lastCallFilterAt: 0};

export function SettingsScreen() {
  const [diagnostics, setDiagnostics] = useState(emptyDiagnostics);
  const [backupText, setBackupText] = useState('');
  const [backupPassword, setBackupPassword] = useState('');
  const refresh = () => getDiagnostics().then(setDiagnostics);
  useEffect(() => { refresh(); }, []);
  const importBackup = async () => {try {await importRules(backupText, backupPassword); setBackupText(''); Alert.alert('Respaldo importado', 'Las reglas se aplicaron correctamente.');} catch (error) {Alert.alert('No se pudo importar', error instanceof Error ? error.message : 'Contenido inválido.');}};
  const deleteAll = () => Alert.alert('Borrar todos los datos', 'Se eliminarán reglas, números bloqueados, preferencias e historial.', [{text: 'Cancelar', style: 'cancel'}, {text: 'Borrar', style: 'destructive', onPress: () => {clearAllData(); setBackupText(''); Alert.alert('Datos eliminados');}}]);
  return <AppScreen>
    <AppPageHeader title="Ajustes y diagnóstico" subtitle="Comprueba que Android permita a Kimox proteger tus llamadas." />
    <Diagnostic title="Filtrado de llamadas" enabled={diagnostics.callScreening} onPress={requestCallScreeningRole}/>
    <AppStatusRow title="Señal telefónica detectada" status={lastActivity(diagnostics.lastPhoneEventAt)} tone={diagnostics.lastPhoneEventAt ? 'success' : 'warning'} />
    <AppStatusRow title="Última evaluación de llamada" status={lastActivity(diagnostics.lastCallFilterAt)} tone={diagnostics.lastCallFilterAt ? 'success' : 'warning'} />
    <Diagnostic title="Acceso a contactos" enabled={diagnostics.contacts} onPress={requestCallScreeningRole}/>
    <Diagnostic title="Notificaciones" enabled={diagnostics.notifications} onPress={requestCallScreeningRole}/>
    <AppStatusRow title="Protección en segundo plano" status={diagnostics.backgroundReady ? 'Preparada' : 'Pausada'} tone={diagnostics.backgroundReady ? 'success' : 'danger'} />
    <Diagnostic title="Sin restricción de batería" enabled={diagnostics.batteryUnrestricted} onPress={openBatterySettings}/>
    <Diagnostic title="Superposición autorizada" enabled={diagnostics.overlay} onPress={openOverlaySettings}/>
    <AppStatusRow title="Servicio de accesibilidad" status={diagnostics.accessibility ? 'Activo' : 'Inactivo'} tone={diagnostics.accessibility ? 'warning' : 'success'} onPress={openAccessibilitySettings}/>
    <AppButton title="Configurar inicio automático (Huawei)" variant="secondary" onPress={openManufacturerBatterySettings}/>
    <AppStatusRow title="Auditoría periódica" status={diagnostics.periodicAudit ? 'Activa' : 'Opcional'} tone={diagnostics.periodicAudit ? 'success' : 'warning'} />
    <AppButton title="Actualizar diagnóstico" variant="secondary" onPress={refresh} style={s.gap}/>

    <AppSectionHeader title="Respaldo local" description="Usa una contraseña de 8 caracteres o más para cifrarlo. Sin contraseña será JSON legible." />
    <AppTextField value={backupPassword} onChangeText={setBackupPassword} secureTextEntry placeholder="Contraseña del respaldo (opcional)" />
    <AppButton title="Exportar reglas" onPress={() => exportRules(backupPassword).catch(error => Alert.alert('No se pudo compartir', error instanceof Error ? error.message : 'Error de cifrado.'))} style={s.gap}/>
    <AppTextField value={backupText} onChangeText={setBackupText} multiline placeholder="Pega aquí el respaldo JSON o cifrado" />
    <AppButton title="Importar respaldo" variant="secondary" disabled={!backupText.trim()} onPress={importBackup} style={s.gap}/>

    <AppSectionHeader title="Datos y privacidad" description="Reglas e historial se cifran mediante Android Keystore y permanecen en este dispositivo." />
    <AppButton title="Borrar historial" variant="danger" onPress={() => Alert.alert('Borrar historial', '¿Deseas borrar todo el historial?', [{text: 'Cancelar', style: 'cancel'}, {text: 'Borrar', style: 'destructive', onPress: clearHistory}])}/>
    <AppButton title="Borrar todos mis datos" variant="danger" onPress={deleteAll} style={s.gap}/>
    <AppButton title="Abrir ajustes de Android" variant="secondary" onPress={openAppSettings} style={s.gap}/>
  </AppScreen>;
}
function Diagnostic({title, enabled, onPress}: {title: string; enabled: boolean; onPress: () => void}) {return <AppStatusRow title={title} status={enabled ? 'Correcto' : 'Requiere atención'} tone={enabled ? 'success' : 'danger'} onPress={onPress}/>;}
function lastActivity(timestamp: number) {return timestamp ? new Date(timestamp).toLocaleString() : 'Aún no recibida';}
const s = StyleSheet.create({gap: {marginTop: 10}});
