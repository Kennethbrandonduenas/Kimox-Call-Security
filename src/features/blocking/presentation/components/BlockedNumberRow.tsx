import React from 'react';
import {Text} from 'react-native';
import {AppButton, AppListItem} from '../../../../shared/presentation/components';
import {BlockedNumber} from '../../domain/BlockedNumber';

export function BlockedNumberRow({item, onRemove}: {item: BlockedNumber; onRemove: () => void}) {
  return <AppListItem title={item.number} subtitle={item.label} leading={<Text>⊘</Text>} trailing={<AppButton title="Quitar" variant="ghost" compact onPress={onRemove}/>}/>;
}
