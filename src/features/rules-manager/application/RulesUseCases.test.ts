import {addRule, addWhitelistNumber, removeRule, removeWhitelistNumber} from './RulesUseCases';
import {SecurityRules} from '../domain/Rule';
const base: SecurityRules = {whitelist: [], rules: []};
test('adds and removes a rule', () => {const rule={id:'1',type:'prefix' as const,value:'900',action:'block' as const,enabled:true,priority:1}; expect(addRule(base,rule).rules).toHaveLength(1); expect(removeRule(addRule(base,rule),'1').rules).toHaveLength(0);});
test('manages whitelist numbers without duplicates', () => {const next=addWhitelistNumber(addWhitelistNumber(base,'51999'),'51999'); expect(next.whitelist).toEqual(['51999']); expect(removeWhitelistNumber(next,'51999').whitelist).toEqual([]);});
