# Kimox Call Security

Aplicación React Native de protección local de llamadas para Android. No requiere cuenta ni backend.

## Funciones

- Lista blanca y bloqueo por número, prefijo o código de país.
- Tratamiento de llamadas privadas y números fuera de contactos.
- Acciones de bloqueo, rechazo o silencio.
- Historial filtrable con retención de 7, 30, 90 días o indefinida.
- Notificaciones opcionales con números ocultos por defecto.
- Importación y exportación local de reglas, opcionalmente cifrada con contraseña.
- Reglas e historial cifrados con AES-GCM y una clave protegida por Android Keystore.
- Diagnóstico de rol de filtrado, contactos, notificaciones y batería.
- Revisión orientativa de permisos sensibles de aplicaciones visibles para Android.
- Diagnóstico local rotativo y actividad nativa `Kimox Recovery` independiente de React Native.

El análisis antimalware es informativo: el aislamiento de Android impide inspeccionar completamente otras aplicaciones y no permite certificar que un dispositivo esté libre de malware.

## Recuperación y diagnóstico

Recovery se conserva deshabilitado en las compilaciones normales. Para crear un APK temporal de diagnóstico que añada el acceso **Kimox Recovery**, compila con:

```powershell
cd android
.\gradlew.bat assembleStandalone -PkimoxRecoveryEnabled=true -PreactNativeArchitectures=arm64-v8a
```

En ese APK de diagnóstico, si la aplicación principal se cierra al iniciar:

1. Abre `Kimox Recovery` desde el launcher del teléfono.
2. Pulsa **Compartir diagnóstico TXT** para enviar el registro técnico.
3. Si el registro indica datos cifrados dañados, usa **Restablecer datos protegidos**.
4. Pulsa **Intentar abrir Kimox**.

Recovery se ejecuta en el proceso Android aislado `:recovery` y no inicializa React Native. Los registros se rotan localmente, ocultan números largos y no guardan contraseñas ni claves criptográficas.

## Desarrollo

Requisitos: Node.js 22.11+, JDK 17 y Android SDK.

```powershell
npm install
npm run verify
npm run android
```

## Release Android

Para generar un APK autónomo ARM64 de pruebas que no dependa de Metro:

```powershell
cd android
./gradlew assembleStandalone -PreactNativeArchitectures=arm64-v8a
```

El archivo se genera en `android/app/build/outputs/apk/standalone/app-standalone.apk`. No distribuir `app-debug.apk` a teléfonos desconectados, porque esa variante necesita Metro.

Nunca se firma una versión release con la llave de depuración. Crea una llave fuera del repositorio y define estas propiedades en `~/.gradle/gradle.properties`:

```properties
KIMOX_UPLOAD_STORE_FILE=C:/ruta/privada/kimox-upload.keystore
KIMOX_UPLOAD_STORE_PASSWORD=CAMBIAR
KIMOX_UPLOAD_KEY_ALIAS=kimox-upload
KIMOX_UPLOAD_KEY_PASSWORD=CAMBIAR
```

Ejecuta `android/gradlew bundleRelease`. El AAB aparecerá en `android/app/build/outputs/bundle/release/app-release.aab`.

## Validación y publicación

- [Matriz de pruebas](docs/DEVICE_TEST_MATRIX.md)
- [Lista de publicación](docs/RELEASE_CHECKLIST.md)
- [Política de privacidad](docs/PRIVACY_POLICY.md)
- [Configuración iOS](docs/IOS_CALL_DIRECTORY.md)
