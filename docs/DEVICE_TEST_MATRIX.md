# Matriz de pruebas físicas

Ejecutar en al menos Pixel/Android limpio, Samsung One UI, Xiaomi/HyperOS y Motorola; cubrir Android 10, 12, 14 y la versión objetivo vigente. Añadir un equipo doble SIM.

| Caso | Preparación | Resultado esperado |
|---|---|---|
| Rol de filtrado | Conceder rol desde Diagnóstico | Estado Correcto |
| Número exacto | Agregar emisor a bloqueados | Acción elegida e historial |
| Lista blanca | Mismo número en blanca y bloqueada | La llamada entra |
| Prefijo/país | Agregar prefijo del emisor | Coincidencia y motivo correcto |
| Privada | Ocultar identificador del emisor | Se aplica regla privada |
| Desconocida | Conceder contactos; emisor no guardado | Se aplica regla desconocida |
| Sin contactos | Revocar contactos | No se bloquea como desconocida |
| Silencio | Elegir Silenciar | Llamada visible sin timbre |
| App cerrada | Forzar cierre y llamar | El servicio sigue filtrando |
| Reinicio | Reiniciar el teléfono | Reglas y protección persisten |
| Doble SIM | Llamar a ambas SIM | Comportamiento consistente |
| Notificación privada | Ocultación activada | Solo últimos cuatro dígitos |
| Retención | Cambiar política y cargar historial | Eventos vencidos desaparecen |
| Importación | Importar JSON válido/inválido | Válido aplica; inválido se rechaza |
| Borrado | Borrar todos los datos | Reglas e historial quedan vacíos |

Registrar fabricante, modelo, Android, operador, SIM, resultado, evidencia y fecha.
