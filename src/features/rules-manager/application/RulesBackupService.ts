import {Share} from 'react-native';
import {BlockedNumber} from '../../blocking/domain/BlockedNumber';
import {
  loadBlockedNumbers,
  loadSecurityRules,
  saveSecurityRules,
  syncBlockedNumbers,
  encryptBackup,
  decryptBackup,
} from '../../blocking/application/blockingService';
import {DEFAULT_SECURITY_RULES, SecurityRules} from '../../call-security/domain/SecurityRules';

type RulesBackup = {
  version: 1;
  exportedAt: string;
  blockedNumbers: string[];
  rules: SecurityRules;
};

export async function exportRules(password = ''): Promise<string> {
  const [blocked, rules] = await Promise.all([loadBlockedNumbers(), loadSecurityRules()]);
  const backup: RulesBackup = {
    version: 1,
    exportedAt: new Date().toISOString(),
    blockedNumbers: blocked.map(item => item.number),
    rules,
  };
  const json = JSON.stringify(backup, null, 2);
  const output = password ? await encryptBackup(json, password) : json;
  await Share.share({title: 'Respaldo Kimox Call Security', message: output});
  return output;
}

export async function importRules(content: string, password = ''): Promise<{blocked: BlockedNumber[]; rules: SecurityRules}> {
  const json = content.trim().startsWith('kimoxenc1:')
    ? await decryptBackup(content, password)
    : content;
  const parsed = JSON.parse(json) as Partial<RulesBackup>;
  if (parsed.version !== 1 || !Array.isArray(parsed.blockedNumbers) || !parsed.rules) {
    throw new Error('El archivo no es un respaldo Kimox válido.');
  }
  const action = parsed.rules.action;
  if (!['block', 'reject', 'silence'].includes(action)) {
    throw new Error('La acción configurada no es válida.');
  }
  const rules: SecurityRules = {
    ...DEFAULT_SECURITY_RULES,
    ...parsed.rules,
    whitelist: stringArray(parsed.rules.whitelist),
    blockedPrefixes: stringArray(parsed.rules.blockedPrefixes),
    blockedCountries: stringArray(parsed.rules.blockedCountries),
    blockPrivate: booleanValue(parsed.rules.blockPrivate, false),
    blockUnknown: booleanValue(parsed.rules.blockUnknown, false),
    notificationsEnabled: booleanValue(parsed.rules.notificationsEnabled, true),
    hideNumbersInNotifications: booleanValue(parsed.rules.hideNumbersInNotifications, true),
    historyRetentionDays: retentionValue(parsed.rules.historyRetentionDays),
  };
  const blocked = stringArray(parsed.blockedNumbers).map(number => ({number, label: 'Importado'}));
  saveSecurityRules(rules);
  syncBlockedNumbers(blocked);
  return {blocked, rules};
}

function booleanValue(value: unknown, fallback: boolean): boolean {
  return typeof value === 'boolean' ? value : fallback;
}

function retentionValue(value: unknown): 7 | 30 | 90 | 0 {
  return value === 0 || value === 7 || value === 30 || value === 90 ? value : 30;
}

function stringArray(value: unknown): string[] {
  if (!Array.isArray(value) || value.some(item => typeof item !== 'string')) {
    throw new Error('El respaldo contiene una lista inválida.');
  }
  return [...new Set(value.map(item => item.trim()).filter(Boolean))];
}
