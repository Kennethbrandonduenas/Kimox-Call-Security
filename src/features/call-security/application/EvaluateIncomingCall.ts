import {CallAction, CallRule, evaluateCall, IncomingCall} from '../domain/CallRule';
export function evaluateIncomingCall(call: IncomingCall, rules: CallRule[], whitelist: string[]): CallAction { return evaluateCall(call, rules, whitelist); }
