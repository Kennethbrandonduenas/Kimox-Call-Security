import React, {ReactNode} from 'react';
import {ScrollView, StyleProp, StyleSheet, ViewStyle} from 'react-native';
export function AppScreen({children, contentStyle}: {children: ReactNode; contentStyle?: StyleProp<ViewStyle>}) {
  return <ScrollView showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled" contentContainerStyle={[s.content, contentStyle]}>{children}</ScrollView>;
}
const s = StyleSheet.create({content: {paddingBottom: 120}});
