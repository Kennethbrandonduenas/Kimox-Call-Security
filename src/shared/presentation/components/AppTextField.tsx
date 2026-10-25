import React from 'react';
import {StyleSheet, Text, TextInput, TextInputProps, View} from 'react-native';
import {palette, radius, spacing} from '../theme';

export function AppTextField({label, helper, error, multiline, ...props}: TextInputProps & {label?: string; helper?: string; error?: string}) {
  return <View style={s.container}>
    {label && <Text style={s.label}>{label}</Text>}
    <TextInput {...props} multiline={multiline} placeholderTextColor={palette.muted}
      style={[s.input, multiline && s.multiline, error ? s.errorInput : undefined, props.style]} />
    {(error || helper) && <Text style={[s.helper, error ? s.error : undefined]}>{error || helper}</Text>}
  </View>;
}
const s = StyleSheet.create({
  container: {flex: 1}, label: {color: palette.text, fontSize: 12, fontWeight: '700', marginBottom: spacing.sm},
  input: {backgroundColor: palette.surface, borderColor: palette.line, borderRadius: radius.md, borderWidth: 1, color: palette.text, minHeight: 48, paddingHorizontal: spacing.md},
  multiline: {minHeight: 100, paddingTop: spacing.md, textAlignVertical: 'top'}, helper: {color: palette.muted, fontSize: 10, marginTop: spacing.xs},
  errorInput: {borderColor: palette.danger}, error: {color: palette.danger},
});
