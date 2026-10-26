# Kimox Call Directory para iOS

La protección de llamadas de iOS se implementa con una extensión de Call Directory. iOS consulta números ya precargados y puede bloquearlos o etiquetarlos aunque la aplicación principal no esté abierta.

## Configuración necesaria en macOS/Xcode

1. Abrir `ios/KimoxCallSecurity.xcworkspace` en Xcode.
2. Añadir **File > New > Target > Call Directory Extension** con el nombre `KimoxCallDirectory`.
3. Usar el identificador `com.kimoxcallsecurity.KimoxCallDirectory` o actualizar el valor `extensionIdentifier` en `KimoxCallDirectoryStore.swift`.
4. Copiar `ios/KimoxCallDirectoryExtension/KimoxCallDirectoryProvider.swift` y `ios/KimoxCallDirectoryShared.swift` al nuevo target y usar su `Info.plist`.
5. En el target principal y el de extensión, activar **Signing & Capabilities > App Groups** y añadir `group.com.kimoxcallsecurity.shared`.
6. Añadir `KimoxCallDirectoryStore.swift`, `KimoxCallDirectoryShared.swift`, `KimoxCallDirectoryModule.swift` y `KimoxCallDirectoryModule.m` al target principal.
7. Activar la extensión en **Ajustes > Teléfono > Bloqueo e identificación de llamadas > Kimox** en un iPhone físico.

La app React Native invoca `syncIosCallDirectory(números, etiquetas)` para guardar la lista y pedir a iOS una recarga. `getIosCallDirectoryStatus()` entrega el estado de la extensión y la fecha de la última sincronización.

## Límites deliberados

- Solo números exactos normalizados en formato internacional; no hay evaluación por país, prefijo o reputación durante la llamada.
- No se debe intentar acceso de red desde la extensión.
- La extensión se valida en un iPhone real; el simulador no procesa llamadas telefónicas reales.
