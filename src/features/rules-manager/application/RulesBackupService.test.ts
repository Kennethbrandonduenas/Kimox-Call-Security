jest.mock('react-native', () => ({
  NativeModules: {KimoxSecurity: {}},
  Platform: {OS: 'android'},
  Share: {share: jest.fn()},
}));

import {importRules} from './RulesBackupService';

describe('RulesBackupService', () => {
  it('sanitizes duplicated values and supplies privacy defaults', async () => {
    const result = await importRules(JSON.stringify({
      version: 1,
      blockedNumbers: [' 123 ', '123'],
      rules: {
        whitelist: [],
        blockedPrefixes: ['90', '90'],
        blockedCountries: [],
        blockPrivate: true,
        blockUnknown: false,
        notificationsEnabled: true,
        action: 'reject',
      },
    }));

    expect(result.blocked).toHaveLength(1);
    expect(result.rules.blockedPrefixes).toEqual(['90']);
    expect(result.rules.hideNumbersInNotifications).toBe(true);
    expect(result.rules.historyRetentionDays).toBe(30);
  });

  it('enables private-number blocking for older backups without that setting', async () => {
    const result = await importRules(JSON.stringify({
      version: 1,
      blockedNumbers: [],
      rules: {
        whitelist: [],
        blockedPrefixes: [],
        blockedCountries: [],
        notificationsEnabled: true,
        action: 'reject',
      },
    }));

    expect(result.rules.blockPrivate).toBe(true);
    expect(result.rules.blockInternational).toBe(true);
    expect(result.rules.allowInternationalContacts).toBe(true);
    expect(result.rules.blockUnknown).toBe(false);
    expect(result.rules.action).toBe('reject');
    expect(result.rules.localCountryCode).toBe('51');
    expect(result.rules.postCallPromptEnabled).toBe(true);
  });

  it('rejects unsupported actions and malformed arrays', async () => {
    await expect(importRules('{"version":1,"blockedNumbers":[],"rules":{"action":"erase"}}')).rejects.toThrow();
    await expect(importRules('{"version":1,"blockedNumbers":"123","rules":{}}')).rejects.toThrow();
  });
});
