import {normalizePhoneNumber} from './phoneNumber';
test('normalizes a phone number', () => { expect(normalizePhoneNumber('+51 987 654 321')).toBe('+51987654321'); });
