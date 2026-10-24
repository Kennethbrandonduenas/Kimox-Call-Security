export const normalizePhoneNumber = (value: string) => value.replace(/[^0-9+]/g, '').replace(/^00/, '+');
