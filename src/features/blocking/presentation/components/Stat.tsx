import React from 'react';
import {Text, View} from 'react-native';
import {styles} from '../styles';
export function Stat({value, label}: {value: string; label: string}) {return <View style={styles.stat}><Text style={styles.statValue}>{value}</Text><Text style={styles.statLabel}>{label}</Text></View>}
