# Apartado 4. Capturas de Wireshark con tráfico no legible

Tras activar el cifrado, una nueva captura de Wireshark debe mostrar que el contenido ya no se entiende como texto plano.

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
