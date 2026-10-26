import {NativeModules, Platform} from 'react-native';
import {BlockedNumber} from '../domain/BlockedNumber';
import {
  DEFAULT_SECURITY_RULES,
  SecurityRules,
} from '../../call-security/domain/SecurityRules';

export type CallHistoryEntry = {
  id: string;
  number: string;
  action: string;
  reason: string;
  timestamp: number;
  contactName?: string;
};

export type DeviceContact = {number: string; name: string};

export type DiagnosticStatus = {
  callScreening: boolean;
  notifications: boolean;
  contacts: boolean;
  overlay: boolean;
  accessibility: boolean;
  batteryUnrestricted: boolean;
  backgroundReady: boolean;
  periodicAudit: boolean;
  legacyCallReceiver: boolean;
  lastPhoneEventAt: number;
  lastCallFilterAt: number;
  lastCallOutcome: string;
  screeningMode: 'SYSTEM_SCREENING' | 'LEGACY_ANDROID_9' | 'LEGACY_ANDROID_7_8' | 'LIMITED_LEGACY';
  compatibilityMessage: string;
  manufacturer: string;
};

export type DeviceCountry = {
  iso: string;
  name: string;
  dialingCode: string;
  source: 'SIM' | 'RED_MOVIL' | 'REGION_ANDROID' | 'PREDETERMINADO';
};

type KimoxNativeModule = {
  setBlockedNumbers?: (numbers: string[]) => void;
  getBlockedNumbers?: () => Promise<string[]>;
  getBlockedNumberLabels?: () => Promise<Record<string, string>>;
  blockContact?: (number: string, label: string) => void;
  unblockNumber?: (number: string) => void;
  getContacts?: () => Promise<DeviceContact[]>;
  getContactNames?: (numbers: string[]) => Promise<Record<string, string>>;
  saveSecurityRules?: (rules: SecurityRules) => void;
  getSecurityRules?: () => Promise<SecurityRules>;
  getHistory?: () => Promise<string[]>;
  clearHistory?: () => void;
  clearAllData?: () => void;
  requestCallScreeningRole?: () => void;
  isCallScreeningEnabled?: () => Promise<boolean>;
  setProtectionEnabled?: (enabled: boolean) => void;
  getProtectionEnabled?: () => Promise<boolean>;
  getDiagnostics?: () => Promise<DiagnosticStatus>;
  detectDeviceCountry?: () => Promise<DeviceCountry>;
  openAppSettings?: () => void;
  openNotificationSettings?: () => void;
  openAccessibilitySettings?: () => void;
  openOverlaySettings?: () => void;
  openBatterySettings?: () => void;
  openManufacturerBatterySettings?: () => void;
  encryptBackup?: (content: string, password: string) => Promise<string>;
  decryptBackup?: (content: string, password: string) => Promise<string>;
};

const native = (NativeModules.KimoxSecurity ?? {}) as KimoxNativeModule;

type IosCallDirectoryModule = {
  sync?: (blocked: string[], labels: Record<string, string>) => Promise<boolean>;
  status?: () => Promise<{status: 'enabled' | 'disabled' | 'unknown'; lastSyncAt: number; lastError: string}>;
  entries?: () => Promise<{blocked: string[]; labels: Record<string, string>}>;
};

const iosCallDirectory = (NativeModules.KimoxCallDirectoryModule ?? {}) as IosCallDirectoryModule;

export const syncBlockedNumbers = (numbers: BlockedNumber[]) => {
  if (Platform.OS === 'ios') {
    const labels = Object.fromEntries(numbers.map(item => [item.number.replace(/\D/g, ''), item.label]));
    return syncIosCallDirectory(numbers, labels);
  }
  return native.setBlockedNumbers?.(numbers.map(item => item.number));
};

export const syncIosCallDirectory = (numbers: BlockedNumber[], labels: Record<string, string> = {}) =>
  iosCallDirectory.sync?.(numbers.map(item => item.number), labels);

export const getIosCallDirectoryStatus = () => iosCallDirectory.status?.();

export async function loadBlockedNumbers(): Promise<BlockedNumber[]> {
  if (Platform.OS === 'ios') {
    const entries = await iosCallDirectory.entries?.() ?? {blocked: [], labels: {}};
    return entries.blocked.map(number => ({number, label: entries.labels[number] ?? 'Bloqueo personal'}));
  }
  const numbers = (await native.getBlockedNumbers?.()) ?? [];
  const labels = (await native.getBlockedNumberLabels?.()) ?? {};
  return numbers.map(number => ({number, label: labels[number.replace(/\D/g, '')] ?? 'Bloqueo personal'}));
}

export const loadDeviceContacts = async (): Promise<DeviceContact[]> =>
  (await native.getContacts?.()) ?? [];

export const blockDeviceContact = (contact: DeviceContact) => native.blockContact?.(contact.number, contact.name);
export const unblockNumber = (number: string) => native.unblockNumber?.(number);

export const saveSecurityRules = (rules: SecurityRules) =>
  native.saveSecurityRules?.(rules);

export async function loadSecurityRules(): Promise<SecurityRules> {
  if (Platform.OS !== 'android' || !native.getSecurityRules) {
    return DEFAULT_SECURITY_RULES;
  }
  return {...DEFAULT_SECURITY_RULES, ...(await native.getSecurityRules())};
}

export async function loadHistory(): Promise<CallHistoryEntry[]> {
  const values = (await native.getHistory?.()) ?? [];
  const parsed = values
    .map(raw => {
      const [timestamp, number, action, ...reasonParts] = raw.split('|');
      return {
        id: `${timestamp}-${number}`,
        number,
        action,
        reason: reasonParts.join('|'),
        timestamp: Number(timestamp),
      };
    })
    .filter(item => Number.isFinite(item.timestamp));
  const names = (await native.getContactNames?.(parsed.map(item => item.number))) ?? {};
  return parsed.map(item => ({...item, contactName: names[item.number.replace(/\D/g, '')]}))
    .sort((a, b) => b.timestamp - a.timestamp);
}

export const requestCallScreeningRole = () =>
  native.requestCallScreeningRole?.();
export const isCallScreeningEnabled = async () =>
  (await native.isCallScreeningEnabled?.()) ?? false;
export const setProtectionEnabled = (enabled: boolean) =>
  native.setProtectionEnabled?.(enabled);
export const getProtectionEnabled = async () =>
  (await native.getProtectionEnabled?.()) ?? true;
export const getDiagnostics = async (): Promise<DiagnosticStatus> =>
  (await native.getDiagnostics?.()) ?? {
    callScreening: false,
    notifications: false,
    contacts: false,
    overlay: false,
    accessibility: false,
    batteryUnrestricted: false,
    backgroundReady: false,
    periodicAudit: false,
    legacyCallReceiver: false,
    lastPhoneEventAt: 0,
    lastCallFilterAt: 0,
    lastCallOutcome: 'No disponible fuera de Android.',
    screeningMode: 'LIMITED_LEGACY',
    compatibilityMessage: 'La protección de llamadas requiere Android.',
    manufacturer: '',
  };
export const detectDeviceCountry = async (): Promise<DeviceCountry> =>
  (await native.detectDeviceCountry?.()) ?? {
    iso: 'PE',
    name: 'Perú',
    dialingCode: '51',
    source: 'PREDETERMINADO',
  };
export const clearHistory = () => native.clearHistory?.();
export const clearAllData = () => native.clearAllData?.();
export const openAppSettings = () => native.openAppSettings?.();
export const openNotificationSettings = () => native.openNotificationSettings?.();
export const openAccessibilitySettings = () =>
  native.openAccessibilitySettings?.();
export const openOverlaySettings = () => native.openOverlaySettings?.();
export const openBatterySettings = () => native.openBatterySettings?.();
export const openManufacturerBatterySettings = () => native.openManufacturerBatterySettings?.();
export async function encryptBackup(content: string, password: string) {
  if (!native.encryptBackup) throw new Error('El cifrado de respaldos requiere Android.');
  return native.encryptBackup(content, password);
}
export async function decryptBackup(content: string, password: string) {
  if (!native.decryptBackup) throw new Error('El descifrado de respaldos requiere Android.');
  return native.decryptBackup(content, password);
}
