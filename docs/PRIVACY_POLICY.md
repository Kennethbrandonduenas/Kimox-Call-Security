# Política de privacidad de Kimox Call Security

Última actualización: 24 de octubre de 2026.

Kimox Call Security funciona sin cuenta y sin backend. Las reglas, números bloqueados, preferencias e historial se guardan cifrados localmente mediante una clave protegida por Android Keystore. La aplicación no vende ni transmite esta información a servidores del desarrollador.

## Permisos

- Contactos: se utiliza localmente para determinar si una llamada pertenece a un contacto cuando se activa la regla de números desconocidos.
- Filtrado de llamadas: permite evaluar y responder a llamadas entrantes conforme a las reglas elegidas por el usuario.
- Notificaciones: informa localmente sobre llamadas filtradas. Los números se ocultan por defecto.

La revisión de aplicaciones instaladas muestra únicamente información que Android permite consultar. Es orientativa y no sustituye un producto antivirus certificado.

## Auditor local de seguridad

El auditor requiere acceso a la lista de aplicaciones instaladas para mostrar nombre de paquete, tienda de procedencia, versión, fechas, permisos declarados o concedidos y certificado de firma. Kimox calcula localmente huellas SHA-256, conserva hasta diez resúmenes de análisis y compara cambios de firma, versión y permisos. También registra localmente instalaciones, actualizaciones y eliminaciones.

El usuario debe aceptar expresamente este análisis antes de utilizarlo. Puede marcar aplicaciones como confiables y borrar en cualquier momento análisis, eventos y decisiones de confianza. Un cambio de firma revoca automáticamente la confianza anterior.

Kimox permite seleccionar voluntariamente un APK mediante el selector de archivos de Android. El archivo se copia temporalmente para calcular su hash, certificado y permisos, y la copia temporal se elimina al finalizar. Kimox no recorre automáticamente las fotografías, documentos ni carpetas privadas del usuario.

Las revisiones periódicas utilizan el programador de tareas de Android y pueden mostrar recordatorios locales. No se envían inventarios de aplicaciones, APK, hashes ni resultados a servidores del desarrollador.

Las evaluaciones señalan indicadores y combinaciones de riesgo, pero no garantizan que una aplicación sea maliciosa o segura. Kimox no elimina aplicaciones sin confirmación del usuario.

La aplicación conserva registros técnicos locales para diagnosticar cierres. Estos registros incluyen versión del sistema, modelo, eventos de inicialización y trazas de error; los números largos y valores sensibles se ocultan automáticamente. El usuario puede compartirlos o borrarlos desde Kimox Recovery.

El usuario puede borrar el historial o todos los datos desde Ajustes. La desinstalación elimina los datos de la aplicación; las copias exportadas quedan bajo control del usuario y pueden protegerse con contraseña. Android Backup está deshabilitado.

Antes de publicar, sustituir este párrafo por el nombre legal y correo de contacto del responsable de la aplicación.
