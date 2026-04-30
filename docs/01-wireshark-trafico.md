# Apartado 1. Escaneo del tránsito de paquetes con Wireshark

En esta práctica se capturó el tráfico entre cliente y servidor antes de aplicar el cifrado para verificar el protocolo de mensajes `TIPO|datos`.

## Qué se observa en la captura

- Conexión TCP entre el cliente y el servidor en el puerto configurado.
- Mensajes `JOIN`, `ACK`, `ROLE`, `WORD`, `VOTE` y `RESULT` visibles en texto plano.
- Intercambio de palabras, votos y resultados legibles directamente desde la traza.

## Criterio de validación

La evidencia correcta es una captura donde Wireshark permita leer el contenido del protocolo sin necesidad de decodificación adicional.
