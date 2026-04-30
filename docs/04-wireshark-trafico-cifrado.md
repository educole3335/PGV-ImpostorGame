# Apartado 4. Capturas de Wireshark con tráfico no legible

Tras activar el cifrado, una nueva captura de Wireshark debe mostrar que el contenido ya no se entiende como texto plano.

## Guía de captura

Este apartado debe capturarse después de activar el cifrado en cliente y servidor.

### Pasos recomendados

1. Compila y ejecuta la versión cifrada del juego.
2. Abre Wireshark y filtra por el mismo puerto usado por la aplicación.
3. Inicia servidor y cliente.
4. Provoca al menos un intercambio real de mensajes, por ejemplo `JOIN`, una palabra y una respuesta del servidor.
5. Guarda una captura donde el payload ya no se lea como texto humano.

### Qué debe verse en la evidencia

- Paquetes TCP normales, sin romper la comunicación.
- Cadenas Base64 o bytes opacos en el contenido.
- Ausencia de palabras, votos o roles legibles directamente.


## Qué debe verse

- Los paquetes siguen existiendo y la conexión TCP continúa normal.
- El contenido aparece como una cadena Base64 o como datos opacos.
- No se identifican directamente palabras, votos ni roles en la traza.

## Captura sugerida

Inserta aquí dos evidencias:

1. Un paquete cliente → servidor con el mensaje cifrado.

2. Un paquete servidor → cliente con la respuesta cifrada.

## Criterio de validación

La captura es correcta si no es posible leer el texto original sin descifrarlo fuera del protocolo.

## Comprobación rápida

Si Wireshark solo muestra datos opacos o una cadena cifrada sin significado directo, la captura es válida para este apartado.
