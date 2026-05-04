# Apartado 5. Esquema de seguridad basado en roles

Si la aplicación creciera a un proyecto mayor, conviene separar responsabilidades de seguridad por roles y permisos.

## Roles propuestos

- `Jugador`: puede unirse a partidas, enviar palabras, votar y jugar.
- `Impostor`: además de las acciones de jugador, recibe una pista parcial y una fase final de adivinanza.
- `Moderador`: puede iniciar/cerrar partidas, expulsar usuarios y revisar incidencias.
- `Administrador`: gestiona configuración, permisos, logs y mantenimiento del sistema.


