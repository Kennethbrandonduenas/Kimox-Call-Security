/* eslint-disable react-native/no-inline-styles */
import React, {useEffect, useState} from 'react';
import {
  StatusBar,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {SafeAreaProvider, SafeAreaView} from 'react-native-safe-area-context';
import {BlockedNumber} from './src/features/blocking/domain/BlockedNumber';
import {
  loadBlockedNumbers,
  loadHistory,
  getProtectionEnabled,
  setProtectionEnabled,
} from './src/features/blocking/application/blockingService';
import {HomeScreen} from './src/features/blocking/presentation/screens/HomeScreen';
import {BlockedNumbersScreen} from './src/features/blocking/presentation/screens/BlockedNumbersScreen';
import {SecurityRulesScreen} from './src/features/blocking/presentation/screens/SecurityRulesScreen';
import {HistoryScreen} from './src/features/history/presentation/HistoryScreen';
import {MalwareScannerScreen} from './src/features/malware/presentation/MalwareScannerScreen';
import {SettingsScreen} from './src/features/diagnostics/presentation/SettingsScreen';
import {styles} from './src/features/blocking/presentation/styles';

type Tab = 'home' | 'blocked' | 'history' | 'malware' | 'rules' | 'settings';

export default function App() {
  const [tab, setTab] = useState<Tab>('home');
  const [protection, setProtectionState] = useState(true);
  const [blocked, setBlocked] = useState<BlockedNumber[]>([]);
  const [historyCount, setHistoryCount] = useState(0);

  useEffect(() => {
    loadBlockedNumbers().then(setBlocked);
    loadHistory().then(history => setHistoryCount(history.length));
  }, [tab]);

  useEffect(() => {
    getProtectionEnabled().then(setProtectionState);
  }, []);

  const setProtection = (enabled: boolean) => {
    setProtectionState(enabled);
    setProtectionEnabled(enabled);
  };

  const screen = tab === 'home'
    ? <HomeScreen protection={protection} setProtection={setProtection} blockedCount={blocked.length} historyCount={historyCount} openBlocked={() => setTab('blocked')} openHistory={() => setTab('history')} />
    : tab === 'blocked'
      ? <BlockedNumbersScreen blocked={blocked} setBlocked={setBlocked} />
      : tab === 'history'
        ? <HistoryScreen />
        : tab === 'malware'
          ? <MalwareScannerScreen />
          : tab === 'settings'
            ? <SettingsScreen />
            : <SecurityRulesScreen />;

  return (
    <SafeAreaProvider>
    <SafeAreaView style={styles.safe}>
      <StatusBar barStyle="light-content" />
      <View style={styles.header}>
        <View>
          <Text style={styles.eyebrow}>KIMOX</Text>
          <Text style={styles.title}>Mobile Security</Text>
        </View>
        <View style={styles.shield}><Text style={styles.shieldText}>✓</Text></View>
      </View>
      <View style={[styles.content, {flex: 1}]}>{screen}</View>
      <View style={styles.bottomBar}>
        <Nav label="Inicio" icon="⌂" active={tab === 'home'} onPress={() => setTab('home')} />
        <Nav label="Bloqueos" icon="⊘" active={tab === 'blocked'} onPress={() => setTab('blocked')} />
        <Nav label="Reglas" icon="⚙" active={tab === 'rules'} onPress={() => setTab('rules')} />
        <Nav label="Antimalware" icon="◆" active={tab === 'malware'} onPress={() => setTab('malware')} />
        <Nav label="Ajustes" icon="⋮" active={tab === 'settings'} onPress={() => setTab('settings')} />
      </View>
    </SafeAreaView>
    </SafeAreaProvider>
  );
}

function Nav({label, icon, active, onPress}: {label: string; icon: string; active: boolean; onPress: () => void}) {
  return <TouchableOpacity style={styles.navItem} onPress={onPress}><Text style={[styles.navIcon, active && styles.navActive]}>{icon}</Text><Text style={[styles.navLabel, active && styles.navActive]}>{label}</Text></TouchableOpacity>;
}
