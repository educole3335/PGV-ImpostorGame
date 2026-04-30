# Apartado 5. Esquema de seguridad basado en roles

Si la aplicación creciera a un proyecto mayor, conviene separar responsabilidades de seguridad por roles y permisos.

## Roles propuestos

- `Jugador`: puede unirse a partidas, enviar palabras, votar y jugar.
- `Impostor`: además de las acciones de jugador, recibe una pista parcial y una fase final de adivinanza.
- `Moderador`: puede iniciar/cerrar partidas, expulsar usuarios y revisar incidencias.
- `Administrador`: gestiona configuración, permisos, logs y mantenimiento del sistema.

## Controles sugeridos

- Autenticación antes de entrar a la sala.
- Autorización por rol para cada acción sensible.
- Registro de auditoría de votos, expulsiones y desconexiones.
- Secretos y claves sacados del código fuente y movidos a variables de entorno.
- Cifrado obligatorio en tránsito, igual que en esta práctica.

## Evolución natural

Este esquema permite escalar desde una partida local a una plataforma multijugador con moderación y control de acceso más fino.
