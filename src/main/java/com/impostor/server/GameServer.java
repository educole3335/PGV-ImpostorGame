package com.impostor.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor principal del Juego del Impostor.
 *
 * Responsabilidades:
 * - Escuchar conexiones entrantes en el puerto configurado.
 * - Crear un hilo (ClientHandler) por cada cliente conectado, permitiendo
 * la comunicación simultánea (concurrente) de varios jugadores.
 * - Gestionar la configuración de la partida (jugadores, impostores, pistas).
 *
 * Librerías empleadas:
 * - java.net.ServerSocket → abre el puerto y acepta conexiones TCP
 * - java.net.Socket → representa la conexión individual con cada cliente
 * - java.lang.Thread → hilo de ejecución por cliente
 *
 * Uso:
 * java com.impostor.server.GameServer [puerto] [jugadores] [impostores]
 * [pista:true|false]
 * Ejemplo: java com.impostor.server.GameServer 5555 4 1 true
 */
public class GameServer {

    private static final Logger LOG = Logger.getLogger(GameServer.class.getName());
    private static final int PORT_DEFAULT = 5555;
    private static final int PLAYERS_DEFAULT = 4;
    private static final int IMPOST_DEFAULT = 1;

    public static void main(String[] args) {
        int port = PORT_DEFAULT;
        int totalPlayers = PLAYERS_DEFAULT;
        int numImpostors = IMPOST_DEFAULT;
        boolean impostorHint = true;

        if (args.length > 0) {
            // Modo clásico: configuración por argumentos de línea de comandos
            try {
                if (args.length >= 1)
                    port = Integer.parseInt(args[0]);
                if (args.length >= 2)
                    totalPlayers = Integer.parseInt(args[1]);
                if (args.length >= 3)
                    numImpostors = Integer.parseInt(args[2]);
                if (args.length >= 4)
                    impostorHint = Boolean.parseBoolean(args[3]);
            } catch (NumberFormatException e) {
                System.err.println(
                        "Argumentos inválidos. Usa: GameServer [puerto] [jugadores] [impostores] [pista:true|false]");
                System.exit(1);
            }
        } else {
            // Modo interactivo: pedir datos por consola
            Scanner scanner = new Scanner(System.in);
            port = askInt(scanner,
                    "Puerto [" + PORT_DEFAULT + "]: ",
                    PORT_DEFAULT,
                    1,
                    65535);

            totalPlayers = askInt(scanner,
                    "Cantidad de jugadores (min 3) [" + PLAYERS_DEFAULT + "]: ",
                    PLAYERS_DEFAULT,
                    3,
                    Integer.MAX_VALUE);

            numImpostors = askInt(scanner,
                    "Cantidad de impostores (1 a " + (totalPlayers - 1) + ") [" + IMPOST_DEFAULT + "]: ",
                    IMPOST_DEFAULT,
                    1,
                    totalPlayers - 1);

            impostorHint = askBoolean(scanner,
                    "Los impostores tienen pista? (s/n) [s]: ",
                    true);
        }

        // Validaciones mínimas del protocolo del juego
        if (totalPlayers < 3) {
            System.err.println("Se necesitan al menos 3 jugadores.");
            System.exit(1);
        }
        if (numImpostors < 1 || numImpostors >= totalPlayers) {
            System.err.println("Número de impostores inválido.");
            System.exit(1);
        }

        System.out.println("\u001B[36m╔══════════════════════════════════════════╗\u001B[0m");
        System.out.println("\u001B[36m║       JUEGO DEL IMPOSTOR - SERVIDOR      ║\u001B[0m");
        System.out.println("\u001B[36m╚══════════════════════════════════════════╝\u001B[0m");
        System.out.printf("  \u001B[1mPuerto:\u001B[0m      \u001B[33m%d\u001B[0m%n", port);
        System.out.printf("  \u001B[1mJugadores:\u001B[0m   \u001B[33m%d\u001B[0m%n", totalPlayers);
        System.out.printf("  \u001B[1mImpostores:\u001B[0m  \u001B[33m%d\u001B[0m%n", numImpostors);
        System.out.printf("  \u001B[1mPista:\u001B[0m       \u001B[33m%s\u001B[0m%n", impostorHint ? "Sí" : "No");
        System.out.println("  \u001B[32m✓ Esperando conexiones...\u001B[0m");
        System.out.println();

        // Crear sesión de juego compartida por todos los clientes
        GameSession session = new GameSession(totalPlayers, numImpostors, impostorHint);

        // Abrir ServerSocket y aceptar clientes
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!session.isFinished()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("\u001B[32m✓ Nueva conexión:\u001B[0m " + clientSocket.getInetAddress()
                            + "  (\u001B[33m" + session.getPlayerCount() + "/" + totalPlayers + "\u001B[0m)");

                    // Crear y lanzar hilo dedicado para este cliente
                    ClientHandler handler = new ClientHandler(clientSocket, session);
                    Thread thread = new Thread(handler);
                    thread.setName("ClientThread-" + clientSocket.getInetAddress());
                    thread.setDaemon(true);
                    thread.start();

                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Error aceptando cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("\u001B[31m✗ Error: No se pudo abrir el puerto " + port + "\u001B[0m");
            LOG.log(Level.SEVERE, "No se pudo abrir el puerto " + port + ": " + e.getMessage());
            System.exit(1);
        }

        System.out.println("\u001B[32m✓ Servidor finalizado.\u001B[0m");
    }

    private static int askInt(Scanner scanner, String prompt, int defaultValue, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (line.isEmpty())
                return defaultValue;
            try {
                int value = Integer.parseInt(line);
                if (value < min || value > max) {
                    System.out.printf("\u001B[33m⚠ Valor fuera de rango (%d - %d).\u001B[0m%n", min, max);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("\u001B[31m✗ Introduce un número válido.\u001B[0m");
            }
        }
    }

    private static boolean askBoolean(Scanner scanner, String prompt, boolean defaultValue) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim().toLowerCase();
            if (line.isEmpty())
                return defaultValue;
            if (line.equals("s") || line.equals("si") || line.equals("sí") || line.equals("y") || line.equals("yes")) {
                return true;
            }
            if (line.equals("n") || line.equals("no")) {
                return false;
            }
            System.out.println("\u001B[31m✗ Respuesta no válida. Escribe 's' o 'n'.\u001B[0m");
        }
    }
}
