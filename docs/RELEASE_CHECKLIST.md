# Lista de publicación

## Técnica

- Ejecutar `npm run verify`, `gradlew testDebugUnitTest` y `gradlew bundleRelease`.
- Incrementar `versionCode` y `versionName`.
- Firmar con upload key privada; verificar que no sea `debug.keystore`.
- Probar el AAB mediante Internal App Sharing y completar la matriz física.
- Revisar cierres, ANR, batería y comportamiento al reiniciar.

## Google Play

- Definir nombre legal y correo de privacidad.
- Publicar la política de privacidad en una URL pública.
- Completar Data safety declarando tratamiento local de contactos e historial de llamadas según la clasificación vigente de Play Console.
- Justificar el rol de filtrado y permisos sensibles con video si Play Console lo solicita.
- No presentar el escáner orientativo como garantía antivirus.
- Preparar icono 512×512, gráfico 1024×500, capturas y textos de ficha.
- Completar clasificación de contenido, audiencia, anuncios y acceso a la app.

## Seguridad

- Guardar keystore y contraseñas fuera del repositorio y mantener copia segura.
- Confirmar ocultación de números en notificaciones.
- Revisar que logs, capturas y reportes no incluyan teléfonos reales.
- Probar borrado de datos e importaciones dañadas o manipuladas.
