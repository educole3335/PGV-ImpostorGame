# Apartado 3. Aplicación cliente y servidor con tránsito cifrado

La protección del tránsito se resolvió en los puntos de frontera de red, sin alterar la lógica de juego.

## Cambios realizados

- El cliente cifra todos los mensajes antes de hacer `send()`.
- El cliente descifra todo lo que recibe antes de interpretarlo con `Protocol`.
- El servidor descifra los mensajes entrantes antes de procesarlos.
- El servidor cifra cada respuesta al cliente dentro de `sendMessage()`.

## Resultado funcional

El protocolo lógico sigue siendo el mismo, pero el contenido transportado por TCP ya no es legible a simple vista en la red.
