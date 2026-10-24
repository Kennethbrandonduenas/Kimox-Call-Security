import type {CallAction, CallRule, IncomingCall} from '../../call-security/domain/CallRule';
import {evaluateCall} from '../../call-security/domain/CallRule';
export type SecurityRules = {whitelist: string[]; rules: CallRule[]};
export type {CallAction, CallRule, IncomingCall};
export {evaluateCall};
