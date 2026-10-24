import {CallRule} from '../../call-security/domain/CallRule';
import {SecurityRules} from '../domain/Rule';
export const addRule = (state: SecurityRules, rule: CallRule): SecurityRules => ({...state, rules: [...state.rules.filter(item => item.id !== rule.id), rule]});
export const removeRule = (state: SecurityRules, id: string): SecurityRules => ({...state, rules: state.rules.filter(rule => rule.id !== id)});
export const addWhitelistNumber = (state: SecurityRules, number: string): SecurityRules => state.whitelist.includes(number) ? state : {...state, whitelist: [...state.whitelist, number]};
export const removeWhitelistNumber = (state: SecurityRules, number: string): SecurityRules => ({...state, whitelist: state.whitelist.filter(item => item !== number)});
