export type ScreeningAction = 'block' | 'reject' | 'silence';

export type SecurityRules = {
  whitelist: string[];
  blockedPrefixes: string[];
  blockedCountries: string[];
  blockPrivate: boolean;
  blockUnknown: boolean;
  action: ScreeningAction;
  notificationsEnabled: boolean;
  hideNumbersInNotifications: boolean;
  historyRetentionDays: 7 | 30 | 90 | 0;
};

export const DEFAULT_SECURITY_RULES: SecurityRules = {
  whitelist: [],
  blockedPrefixes: [],
  blockedCountries: [],
  blockPrivate: false,
  blockUnknown: false,
  action: 'reject',
  notificationsEnabled: true,
  hideNumbersInNotifications: true,
  historyRetentionDays: 30,
};
