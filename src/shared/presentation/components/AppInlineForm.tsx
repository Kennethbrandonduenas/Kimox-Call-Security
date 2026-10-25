import React from 'react';
import {StyleSheet, TextInputProps, View} from 'react-native';
import {AppButton} from './AppButton';
import {AppTextField} from './AppTextField';
import {spacing} from '../theme';
export function AppInlineForm({buttonTitle = 'Añadir', onSubmit, disabled, ...fieldProps}: TextInputProps & {buttonTitle?: string; onSubmit: () => void; disabled?: boolean}) {
  return <View style={s.row}><AppTextField {...fieldProps} onSubmitEditing={onSubmit}/><AppButton title={buttonTitle} compact disabled={disabled} onPress={onSubmit}/></View>;
}
const s = StyleSheet.create({row: {alignItems: 'center', flexDirection: 'row', gap: spacing.sm}});
