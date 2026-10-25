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
};

export type DiagnosticStatus = {
  callScreening: boolean;
  notifications: boolean;
  contacts: boolean;
  overlay: boolean;
  accessibility: boolean;
  batteryUnrestricted: boolean;
  backgroundReady: boolean;
  periodicAudit: boolean;
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
  openAccessibilitySettings?: () => void;
  openOverlaySettings?: () => void;
  openBatterySettings?: () => void;
  openManufacturerBatterySettings?: () => void;
  encryptBackup?: (content: string, password: string) => Promise<string>;
  decryptBackup?: (content: string, password: string) => Promise<string>;
};

const native = (NativeModules.KimoxSecurity ?? {}) as KimoxNativeModule;

export const syncBlockedNumbers = (numbers: BlockedNumber[]) =>
  native.setBlockedNumbers?.(numbers.map(item => item.number));

export async function loadBlockedNumbers(): Promise<BlockedNumber[]> {
  const numbers = (await native.getBlockedNumbers?.()) ?? [];
  return numbers.map(number => ({number, label: 'Bloqueo personal'}));
}

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
  return values
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
    .filter(item => Number.isFinite(item.timestamp))
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
