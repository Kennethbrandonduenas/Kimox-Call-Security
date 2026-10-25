import {useColorScheme} from 'react-native';
import {palette} from '.';

const darkPalette = {...palette, background: '#07111f', surface: '#102039', text: '#f2f6fc', muted: '#a8b4c6', line: '#263a55'};
export function useAppTheme() {
  const scheme = useColorScheme();
  return {isDark: scheme === 'dark', colors: scheme === 'dark' ? darkPalette : palette};
}
