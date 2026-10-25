import React, {ErrorInfo, ReactNode} from 'react';
import {NativeModules, StyleSheet, Text, View} from 'react-native';
import {AppButton} from './components';

type Props = {children: ReactNode};
type State = {error: Error | null};

export class AppErrorBoundary extends React.Component<Props, State> {
  state: State = {error: null};

  static getDerivedStateFromError(error: Error): State {
    return {error};
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    NativeModules.KimoxSecurity?.recordDiagnostic?.(
      'ERROR',
      'JAVASCRIPT_UI_EXCEPTION',
      `${error.name}: ${error.message}\n${info.componentStack ?? ''}`,
    );
  }

  private retry = () => this.setState({error: null});

  render() {
    const {error} = this.state;
    if (!error) return this.props.children;
    return (
      <View style={localStyles.container}>
        <Text style={localStyles.brand}>KIMOX</Text>
        <Text style={localStyles.title}>Ocurrió un error recuperable</Text>
        <Text style={localStyles.message}>
          La aplicación protegió tus datos y registró el problema. Puedes intentar cargar la pantalla nuevamente.
        </Text>
        <View style={localStyles.errorBox}>
          <Text selectable style={localStyles.errorText}>
            {error.name}: {error.message}
          </Text>
        </View>
        <AppButton title="Reintentar" onPress={this.retry} style={localStyles.button} />
      </View>
    );
  }
}

const localStyles = StyleSheet.create({
  container: {flex: 1, backgroundColor: '#f7f9fc', justifyContent: 'center', padding: 28},
  brand: {color: '#1c63f2', fontSize: 13, fontWeight: '900', letterSpacing: 4},
  title: {color: '#10233f', fontSize: 25, fontWeight: '900', marginTop: 10},
  message: {color: '#56657a', fontSize: 15, lineHeight: 22, marginTop: 12},
  errorBox: {backgroundColor: '#fff0f0', borderRadius: 12, padding: 14, marginTop: 20},
  errorText: {color: '#9e2937', fontSize: 12},
  button: {marginTop: 22},
});
