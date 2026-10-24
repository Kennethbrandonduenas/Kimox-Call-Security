import {evaluateCall} from './CallRule';

test('whitelist takes priority over blocking rules', () => {
  expect(evaluateCall(
    {number: '51999', isPrivate: false, isInContacts: false},
    [{id: '1', type: 'prefix', value: '51', action: 'block', enabled: true, priority: 1}],
    ['51999'],
  )).toBe('allow');
});

test('uses the highest-priority matching rule', () => {
  expect(evaluateCall(
    {number: '900123', isPrivate: false, isInContacts: false},
    [
      {id: '1', type: 'unknown', action: 'silence', enabled: true, priority: 1},
      {id: '2', type: 'prefix', value: '900', action: 'reject', enabled: true, priority: 10},
    ],
    [],
  )).toBe('reject');
});

test('matches private calls', () => {
  expect(evaluateCall(
    {isPrivate: true, isInContacts: false},
    [{id: '1', type: 'private', action: 'block', enabled: true, priority: 1}],
    [],
  )).toBe('block');
});
