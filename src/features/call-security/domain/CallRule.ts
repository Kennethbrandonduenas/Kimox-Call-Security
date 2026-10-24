export type CallAction = 'allow' | 'block' | 'reject' | 'silence';
export type CallRuleType = 'number' | 'prefix' | 'country' | 'unknown' | 'private';
export type CallRule = {id: string; type: CallRuleType; value?: string; action: CallAction; enabled: boolean; priority: number};

export type IncomingCall = {number?: string; isPrivate: boolean; isInContacts: boolean};

export function evaluateCall(call: IncomingCall, rules: CallRule[], whitelist: string[]): CallAction {
  if (call.number && whitelist.includes(call.number)) return 'allow';
  const ordered = rules.filter(rule => rule.enabled).sort((a, b) => b.priority - a.priority);
  const match = ordered.find(rule => {
    if (rule.type === 'private') return call.isPrivate;
    if (rule.type === 'unknown') return !call.isPrivate && !call.isInContacts;
    if (!call.number || !rule.value) return false;
    return rule.type === 'number' ? call.number === rule.value : call.number.startsWith(rule.value);
  });
  return match?.action ?? 'allow';
}
