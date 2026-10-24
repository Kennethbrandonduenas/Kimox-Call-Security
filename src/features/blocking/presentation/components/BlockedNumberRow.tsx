import React from 'react';
import {Text, TouchableOpacity, View} from 'react-native';
import {BlockedNumber} from '../../domain/BlockedNumber';
import {styles} from '../styles';

export function BlockedNumberRow({item, onRemove}: {item: BlockedNumber; onRemove: () => void}) {
  return <View style={styles.blockedRow}><View style={styles.blockIcon}><Text>⊘</Text></View><View style={styles.rowMain}><Text style={styles.number}>{item.number}</Text><Text style={styles.muted}>{item.label}</Text></View><TouchableOpacity onPress={onRemove}><Text style={styles.remove}>Quitar</Text></TouchableOpacity></View>;
}
