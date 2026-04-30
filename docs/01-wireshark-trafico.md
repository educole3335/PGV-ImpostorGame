# Apartado 1. Escaneo del tránsito de paquetes con Wireshark

En esta práctica se capturó el tráfico entre cliente y servidor antes de aplicar el cifrado para verificar el protocolo de mensajes `TIPO|datos`.

## Guía de captura

Para este apartado, la captura debe hacerse cuando el juego todavía transmite el protocolo en texto plano.

### Pasos recomendados

1. Abre Wireshark antes de arrancar el servidor.
2. Selecciona la interfaz de red local que esté usando el cliente y el servidor.
3. Aplica un filtro de captura o de visualización por el puerto del juego, por ejemplo `tcp.port == 5555`.
4. Inicia el servidor y conecta al menos un cliente.
5. Espera a que aparezcan mensajes como `JOIN`, `ACK`, `ROLE` o `WORD`.
6. Detén la captura justo después de un intercambio claro cliente/servidor.

### Qué debe verse en la evidencia

- La conexión TCP entre cliente y servidor.
- El contenido legible del protocolo si se inspecciona el paquete o el flujo TCP.
- Mensajes que permitan demostrar que el texto viaja sin cifrado.

### Captura recomendada

Incluye una captura donde se vea una conversación completa o parcial entre cliente y servidor, con el puerto filtrado y el contenido claramente legible.

## Qué se observa en la captura

- Conexión TCP entre el cliente y el servidor en el puerto configurado.
- Mensajes `JOIN`, `ACK`, `ROLE`, `WORD`, `VOTE` y `RESULT` visibles en texto plano.
- Intercambio de palabras, votos y resultados legibles directamente desde la traza.

## Criterio de validación

La evidencia correcta es una captura donde Wireshark permita leer el contenido del protocolo sin necesidad de decodificación adicional.

## Comprobación rápida

Si puedes leer directamente una palabra, un voto o un rol en la vista de Wireshark, la captura es válida para este apartado.
