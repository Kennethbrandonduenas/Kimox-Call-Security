/**
 * @format
 */

import { AppRegistry, NativeModules } from 'react-native';
import App from './App';
import { name as appName } from './app.json';

const diagnostics = NativeModules.KimoxSecurity;
diagnostics?.recordDiagnostic?.('INFO', 'JAVASCRIPT_ENTRY_STARTED', null);

const errorUtils = global.ErrorUtils;
const previousHandler = errorUtils?.getGlobalHandler?.();
errorUtils?.setGlobalHandler?.((error, isFatal) => {
  diagnostics?.recordDiagnostic?.(
    isFatal ? 'FATAL' : 'ERROR',
    'JAVASCRIPT_UNHANDLED_EXCEPTION',
    `${error?.name ?? 'Error'}: ${error?.message ?? String(error)}\n${error?.stack ?? ''}`,
  );
  previousHandler?.(error, isFatal);
});

AppRegistry.registerComponent(appName, () => App);
