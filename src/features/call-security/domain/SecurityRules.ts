export type ScreeningAction = 'block' | 'reject' | 'silence';

export type SecurityRules = {
  whitelist: string[];
  blockedPrefixes: string[];
  blockedCountries: string[];
  blockPrivate: boolean;
  blockUnknown: boolean;
  blockInternational: boolean;
  allowInternationalContacts: boolean;
  localCountryCode: string;
  postCallPromptEnabled: boolean;
  action: ScreeningAction;
  notificationsEnabled: boolean;
  hideNumbersInNotifications: boolean;
  historyRetentionDays: 7 | 30 | 90 | 0;
};

export const DEFAULT_SECURITY_RULES: SecurityRules = {
  whitelist: [],
  blockedPrefixes: [],
  blockedCountries: [],
  blockPrivate: true,
  blockUnknown: false,
  blockInternational: true,
  allowInternationalContacts: true,
  localCountryCode: '51',
  postCallPromptEnabled: true,
  action: 'reject',
  notificationsEnabled: true,
  hideNumbersInNotifications: true,
  historyRetentionDays: 30,
};
