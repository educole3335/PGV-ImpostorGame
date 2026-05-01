# Apartado 1. Escaneo del tránsito de paquetes con Wireshark

En esta práctica se capturó el tráfico entre cliente y servidor antes de aplicar el cifrado para verificar el protocolo de mensajes `TIPO|datos`.

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
