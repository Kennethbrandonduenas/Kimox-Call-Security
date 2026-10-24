# Extensión Call Directory para iOS

Esta parte debe finalizarse en macOS con Xcode y una cuenta de Apple Developer.

1. Abrir `ios/KimoxCallSecurity.xcodeproj` y crear un target Call Directory Extension.
2. Incorporar `KimoxCallDirectoryProvider.swift` al target de la extensión, no al target principal.
3. Crear un App Group para la app y la extensión y activar la capacidad en ambos targets.
4. Guardar números normalizados en el contenedor compartido y solicitar recarga con `CXCallDirectoryManager`.
5. Configurar identificadores de bundle, equipos, certificados y perfiles de aprovisionamiento.
6. Probar habilitando la extensión en Ajustes > Teléfono > Bloqueo e identificación de llamadas.

iOS carga listas de identificación/bloqueo mediante la extensión; no ofrece el mismo filtrado dinámico de llamadas que `CallScreeningService` en Android. Las pruebas requieren iPhone físico.
