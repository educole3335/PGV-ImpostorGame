# Juego del Impostor — Aplicación Cliente/Servidor con Sockets

> **UD3 – Práctica 1** | Programación de Servicios y Procesos  
> Colegio Salesianos La Cuesta – San Juan Bosco

---

## Índice

1. [Descripción y escenario](#1-descripción-y-escenario)
2. [Roles: cliente y servidor](#2-roles-cliente-y-servidor)
3. [Librerías y clases Java empleadas](#3-librerías-y-clases-java-empleadas)
4. [Arquitectura y estructura del proyecto](#4-arquitectura-y-estructura-del-proyecto)
5. [Protocolo de comunicación](#5-protocolo-de-comunicación)
6. [Compilación y ejecución](#6-compilación-y-ejecución)
7. [Prueba de funcionamiento](#7-prueba-de-funcionamiento)
8. [Control de excepciones](#8-control-de-excepciones)
9. [Documentación de la práctica](#9-documentación-de-la-práctica)

---

## 1. Descripción y escenario

### Justificación del escenario

El **Juego del Impostor** es un juego de deducción social en red donde un grupo de jugadores recibe la misma palabra secreta, excepto uno o varios **impostores**, que reciben únicamente una pista (o nada). Durante cada ronda, los jugadores dicen una palabra relacionada con la secreta intentando demostrar que la conocen, mientras el impostor intenta camuflarse. Al final de cada ronda, los jugadores votan para expulsar a quien crean que es el impostor. Si solo quedan dos personas y una de ellas es el impostor, el impostor gana automáticamente. Si el impostor es expulsado, puede intentar adivinar la palabra secreta para cambiar el resultado. Cuando hay varios impostores, todos los expulsados tienen su oportunidad de adivinar al final.

Este escenario **requiere necesariamente comunicación en red** porque:

- Múltiples jugadores (en máquinas distintas o en la misma) deben recibir información diferente (rol, palabra o pista) de forma privada.
- El servidor debe sincronizar los turnos, recoger votos y difundir resultados de forma simultánea.
- La partida tiene estado global compartido que debe gestionarse de forma concurrente y thread-safe.

### Flujo de la partida

```
Lobby → Asignación de roles → Ronda (turnos de palabras) → Decisión de voto
  → Votación → Expulsión → [¿Impostor? → Adivinanza] → Resultado
                                ↑________________________↓ (si continúa)
```

---

## 2. Roles: cliente y servidor

### Servidor (`GameServer` + `ClientHandler` + `GameSession`)

| Responsabilidad      | Descripción                                                              |
| -------------------- | ------------------------------------------------------------------------ |
| Aceptar conexiones   | `ServerSocket.accept()` en bucle, crea un `ClientHandler` por cliente    |
| Gestión concurrente  | Cada `ClientHandler` corre en su propio `Thread`                         |
| Estado del juego     | `GameSession` mantiene el estado global, sincronizado con `synchronized` |
| Sortear roles        | Asigna aleatoriamente impostores y socialistas                           |
| Dirigir turnos       | Gestiona el orden aleatorio de palabras en cada ronda                    |
| Gestionar votaciones | Recoge votos, resuelve empates por mayor número de votos                 |
| Difundir mensajes    | `broadcast()` envía mensajes a todos los clientes conectados             |
| Cierre limpio        | Detecta desconexiones y notifica al resto                                |

### Cliente (`GameClient`)

| Responsabilidad    | Descripción                                                       |
| ------------------ | ----------------------------------------------------------------- |
| Conectarse         | Abre `Socket` TCP al servidor                                     |
| Hilo receptor      | `ReceiverThread` escucha mensajes del servidor de forma asíncrona |
| Entrada de usuario | Bucle principal lee `Scanner` y envía mensajes al servidor        |
| Presentación       | Muestra el estado del juego con formato legible en consola        |
| Cierre limpio      | Envía `QUIT` y cierra todos los flujos de E/S                     |

---

## 3. Librerías y clases Java empleadas

### `java.net` — Comunicación en red

| Clase          | Uso                                                           |
| -------------- | ------------------------------------------------------------- |
| `ServerSocket` | Abre el puerto TCP del servidor y acepta conexiones entrantes |
| `Socket`       | Representa el extremo de la conexión entre cliente y servidor |

### `java.io` — Flujos de entrada/salida

| Clase                | Uso                                                                      |
| -------------------- | ------------------------------------------------------------------------ |
| `BufferedReader`     | Lectura eficiente de mensajes de texto línea a línea                     |
| `InputStreamReader`  | Convierte el `InputStream` del socket a caracteres UTF-8                 |
| `PrintWriter`        | Escritura de mensajes de texto con `autoFlush=true` para envío inmediato |
| `OutputStreamWriter` | Convierte el `OutputStream` del socket a caracteres UTF-8                |

### `java.lang` — Concurrencia básica

| Clase      | Uso                                                                  |
| ---------- | -------------------------------------------------------------------- |
| `Thread`   | Hilo por cliente en el servidor; hilo receptor en el cliente         |
| `Runnable` | Interfaz implementada por `ClientHandler` para ejecutarse en un hilo |

### `java.util.concurrent` — Thread-safety

| Clase                          | Uso                                                          |
| ------------------------------ | ------------------------------------------------------------ |
| `ConcurrentHashMap`            | Mapa thread-safe de nombre→handler y de nombre→votos         |
| `AtomicInteger`                | Contadores de votos y sincronización sin bloqueos explícitos |
| `Collections.synchronizedList` | Lista de jugadores protegida frente a accesos concurrentes   |

### `java.util` — Utilidades

| Clase                   | Uso                                                      |
| ----------------------- | -------------------------------------------------------- |
| `Scanner`               | Lectura de entrada del usuario por consola               |
| `Collections.shuffle()` | Aleatorizar el orden de turnos y el sorteo de impostores |
| `Random`                | Selección aleatoria de palabra y pista                   |
| `Logger`                | Registro de eventos en servidor y cliente                |

---

## 4. Arquitectura y estructura del proyecto

```
impostor-game/
├── pom.xml
├── README.md
└── src/main/java/com/impostor/
    ├── common/
    │   ├── Protocol.java       ← Constantes y utilidades del protocolo
    │   └── WordBank.java       ← Banco de palabras y pistas
    ├── model/
    │   └── Player.java         ← Modelo de jugador (nombre, rol, estado)
    ├── server/
    │   ├── GameServer.java     ← Punto de entrada del servidor
    │   ├── ClientHandler.java  ← Hilo de atención a un cliente (Runnable)
    │   └── GameSession.java    ← Estado y lógica completa de la partida
    └── client/
        └── GameClient.java     ← Cliente interactivo por consola
```

### Diagrama de hilos

```
GameServer (hilo principal)
│
├── Thread → ClientHandler (Jugador 1)  ──┐
├── Thread → ClientHandler (Jugador 2)  ──┤──► GameSession (compartida, synchronized)
├── Thread → ClientHandler (Jugador 3)  ──┤
└── Thread → ClientHandler (Jugador N)  ──┘

GameClient
├── Hilo principal → Scanner (entrada usuario)
└── ReceiverThread → BufferedReader (mensajes servidor)
```

---

## 5. Protocolo de comunicación

Todos los mensajes siguen el formato: `TIPO|datos`

### Cliente → Servidor

| Mensaje         | Formato                  | Descripción                             |
| --------------- | ------------------------ | --------------------------------------- |
| `JOIN`          | `JOIN\|nombre`           | El jugador se une a la sala             |
| `WORD`          | `WORD\|palabra`          | El jugador dice su palabra del turno    |
| `VOTE_DECISION` | `VOTE_DECISION\|YES\|NO` | Si quiere votar o pasar                 |
| `VOTE`          | `VOTE\|nombre`           | Vota por expulsar a ese jugador         |
| `GUESS`         | `GUESS\|palabra`         | El impostor intenta adivinar la palabra |
| `QUIT`          | `QUIT\|`                 | Cierre limpio de la conexión            |

### Servidor → Cliente

| Mensaje         | Formato                                             | Descripción                  |
| --------------- | --------------------------------------------------- | ---------------------------- |
| `ACK`           | `ACK\|texto`                                        | Confirmación de unión        |
| `ROLE`          | `ROLE\|IMPOSTOR:pista` o `ROLE\|SOCIALISTA:palabra` | Rol asignado                 |
| `NEW_ROUND`     | `NEW_ROUND\|nRonda`                                 | Inicio de nueva ronda        |
| `ORDER`         | `ORDER\|nombre1,nombre2,...`                        | Orden de turnos              |
| `YOUR_TURN`     | `YOUR_TURN\|`                                       | Solicita palabra al jugador  |
| `WORDS_SUMMARY` | `WORDS_SUMMARY\|n1:p1,n2:p2,...`                    | Palabras dichas en la ronda  |
| `ASK_VOTE`      | `ASK_VOTE\|`                                        | Pregunta si votar            |
| `CAST_VOTE`     | `CAST_VOTE\|candidatos`                             | Solicita el voto por número  |
| `EXPELLED`      | `EXPELLED\|nombre:esImpostor`                       | Jugador expulsado            |
| `GUESS_NOW`     | `GUESS_NOW\|`                                       | Pide al impostor que adivine |
| `RESULT`        | `RESULT\|resultado:palabra:impostores`              | Fin de partida               |
| `INFO`          | `INFO\|texto`                                       | Información general          |
| `ERROR`         | `ERROR\|descripción`                                | Error del servidor           |

---

## 6. Compilación y ejecución

### Requisitos

- Java 17 o superior
- Maven 3.6+ (opcional, también se puede compilar con `javac`)

### Con Maven

```bash
# Compilar y empaquetar
mvn package

# Iniciar con Spring Boot (equivale a arrancar el servidor en 5555, 4 jugadores, 1 impostor, pista=true)
mvn spring-boot:run

# Ejecutar servidor en modo interactivo (pregunta puerto, jugadores, impostores y pista)
java -cp target/ImpostorGame-1.0.0-server.jar com.impostor.server.GameServer

# Ejecutar servidor (4 jugadores, 1 impostor, con pista)
java -cp target/ImpostorGame-1.0.0-server.jar com.impostor.server.GameServer 5555 4 1 true

# Ejecutar cliente (en terminales separadas)
java -cp target/ImpostorGame-1.0.0-client.jar com.impostor.client.GameClient localhost 5555 Ana
java -cp target/ImpostorGame-1.0.0-client.jar com.impostor.client.GameClient localhost 5555 Luis
java -cp target/ImpostorGame-1.0.0-client.jar com.impostor.client.GameClient localhost 5555 Sara
java -cp target/ImpostorGame-1.0.0-client.jar com.impostor.client.GameClient localhost 5555 Marco
```

### Con Spring Boot Dashboard (VS Code)

1. Abre la vista **Spring Boot Dashboard**.
2. Refresca proyectos Maven/Java si no aparece al momento.
3. Debes ver la app `ImpostorGameApplication`.
4. Pulsa **Run** sobre esa app.
5. El servidor se inicia con: `5555 4 1 true`.

Si quieres cambiar esos valores, lanza con argumentos en VS Code: `-- 5556 5 1 false`.

### Con javac (sin Maven)

```bash
# Compilar
find src -name "*.java" | xargs javac -d out -encoding UTF-8

# Servidor interactivo
java -cp out com.impostor.server.GameServer

# Servidor
java -cp out com.impostor.server.GameServer 5555 4 1 true

# Cliente
java -cp out com.impostor.client.GameClient localhost 5555 NombreJugador
```

### Parámetros del servidor

```
java GameServer [puerto] [nJugadores] [nImpostores] [pista:true|false]
```

Si no pasas argumentos, el servidor entra en modo interactivo y te pide por consola:

- Cantidad de jugadores
- Cantidad de impostores
- Si los impostores tienen pista o no

| Parámetro   | Por defecto | Descripción                     |
| ----------- | ----------- | ------------------------------- |
| puerto      | 5555        | Puerto TCP de escucha           |
| nJugadores  | 4           | Total de jugadores (mín. 3)     |
| nImpostores | 1           | Número de impostores (mín. 1)   |
| pista       | true        | Si el impostor recibe una pista |

---

## 7. Prueba de funcionamiento

### Escenario de prueba: 3 jugadores, 1 impostor, con pista

**Terminal 1 — Servidor:**

```
java GameServer 5555 3 1 true
→ Esperando conexiones...
→ Nueva conexión desde: /127.0.0.1
→ Jugador unido: Ana (1/3)
→ Jugador unido: Luis (2/3)
→ Jugador unido: Sara (3/3)
→ Partida iniciada. Palabra: Volcán
→ Ana dijo: fuego
→ Luis dijo: montaña
→ Sara dijo: cráter           ← Sara es impostor, usó su pista
→ Expulsado: Sara (impostor=true)
→ Impostor adivina: 'Volcán' → correcto
→ Fin: IMPOSTOR_WINS:Volcán:Sara
```

**Terminal 2 — Ana (socialista):**

```
✓ Bienvenido, Ana
│ Tu rol: SOCIALISTA          │
│ Palabra: Volcán             │
══════════  RONDA 1  ══════════
Orden: Luis → Sara → Ana
¡Es TU turno! → fuego
Palabras: Luis:montaña, Sara:cráter, Ana:fuego
¿Votar? (s/n): s
Candidatos: Luis, Sara, Ana → Sara
Expulsado: Sara — ¿Era impostor? SÍ
[INFO] Sara es el impostor. Tiene una oportunidad de adivinar.
╔══════════════════════════════╗
║  ¡EL IMPOSTOR HA GANADO!    ║
║  Palabra: Volcán             ║
╚══════════════════════════════╝
```

### Prueba de cierre y excepciones

- Si un cliente se desconecta inesperadamente, el servidor captura la `IOException`, llama a `removePlayer()` y notifica al resto con un mensaje `INFO`.
- Si se introduce un nombre duplicado, el servidor responde `ERROR|Nombre duplicado o sala llena` y cierra esa conexión.
- Si el servidor cae, el `ReceiverThread` del cliente detecta el fin de stream (`readLine() == null`) y termina el proceso limpiamente.

---

## 8. Control de excepciones

| Situación                | Excepción               | Tratamiento                                    |
| ------------------------ | ----------------------- | ---------------------------------------------- | -------------------- |
| Puerto ocupado           | `IOException`           | Mensaje de error y `System.exit(1)`            |
| Cliente desconectado     | `IOException`           | `disconnect()` + notificación broadcast        |
| Nombre vacío o duplicado | — (validación)          | Mensaje `ERROR` y cierre de conexión           |
| Argumento inválido       | `NumberFormatException` | Valores por defecto + aviso                    |
| Flujos ya cerrados       | `IOException`           | Bloque `finally` con comprobaciones de nulidad |
| Mensaje desconocido      | —                       | Respuesta `ERROR                               | Mensaje desconocido` |

Todos los flujos de E/S (`Socket`, `BufferedReader`, `PrintWriter`) se cierran en bloques `finally` para garantizar la liberación de recursos independientemente de cómo termine la ejecución.

---

## 9. Documentación de la práctica

Cada apartado de la práctica queda documentado en un archivo Markdown independiente para mantener el proceso ordenado y enlazado desde este README principal:

1. [Apartado 1. Escaneo con Wireshark](docs/01-wireshark-trafico.md)
2. [Apartado 2. Clase de cifrado y descifrado](docs/02-clase-cifrado.md)
3. [Apartado 3. Tráfico cliente/servidor cifrado](docs/03-aplicacion-cifrada.md)
4. [Apartado 4. Capturas de Wireshark con el tráfico protegido](docs/04-wireshark-trafico-cifrado.md)
5. [Apartado 5. Esquema de seguridad basado en roles](docs/05-esquema-seguridad-roles.md)

Las capturas de Wireshark pueden adjuntarse más adelante en esos documentos si quieres completar la evidencia visual de la práctica.
