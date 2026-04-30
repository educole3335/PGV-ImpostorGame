# Apartado 2. Clase con métodos de encriptación y desencriptación

Se añadió la clase [CryptoUtil](../src/main/java/com/impostor/common/CryptoUtil.java) para cifrar y descifrar cada mensaje antes de enviarlo por la red.

## Características

- Cifrado simétrico con AES/GCM.
- Clave compartida derivada mediante PBKDF2.
- Codificación Base64 para poder transportar el mensaje cifrado como una línea de texto.

## Uso

- `encrypt(texto)`: convierte el mensaje legible en una cadena cifrada.
- `decrypt(textoCifrado)`: recupera el mensaje original para que el protocolo siga funcionando igual.

## Nota técnica

En un proyecto real, la frase secreta debería venir de variables de entorno o de un almacén seguro de secretos. En esta práctica se usa una clave compartida fija para simplificar la demostración.
