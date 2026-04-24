package com.impostor.client;

import com.impostor.common.Protocol;
import com.impostor.common.UIUtil;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cliente del Juego del Impostor.
 *
 * Responsabilidades:
 * - Conectarse al servidor mediante un socket TCP.
 * - Lanzar un hilo receptor para escuchar mensajes del servidor en paralelo.
 * - Leer la entrada del usuario por consola y enviar los mensajes adecuados.
 *
 * Librerías empleadas:
 * - java.net.Socket → conexión TCP con el servidor
 * - java.io.BufferedReader → recepción de mensajes del servidor
 * - java.io.PrintWriter → envío de mensajes al servidor
 * - java.util.Scanner → lectura de entrada del usuario por consola
 * - java.lang.Thread → hilo receptor de mensajes del servidor
 *
 * Uso:
 * java com.impostor.client.GameClient [host] [puerto] [nombre]
 * Ejemplo: java com.impostor.client.GameClient localhost 5555 Ana
 */
public class GameClient {

    private static final Logger LOG = Logger.getLogger(GameClient.class.getName());
    private static final Scanner CONSOLE_SCANNER = new Scanner(System.in);

    private final String host;
    private final int port;
    private final String playerName;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = true;
    private final List<String> voteCandidates = new ArrayList<>();

    // ── Constructor ──────────────────────────────────────────────────────────

    public GameClient(String host, int port, String playerName) {
        this.host = host;
        this.port = port;
        this.playerName = playerName;
    }

    // ── Punto de entrada ─────────────────────────────────────────────────────

    public static void main(String[] args) {
        String host = "localhost";
        int port = 5555;
        String name = null;

        if (args.length >= 1)
            host = args[0];
        if (args.length >= 2) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        if (args.length >= 3)
            name = args[2];

        if (name == null || name.isBlank()) {
            System.out.print("Introduce tu nombre: ");
            name = CONSOLE_SCANNER.nextLine().trim();
        }

        new GameClient(host, port, name).start();
    }

    // ── Inicio de la conexión ─────────────────────────────────────────────────

    public void start() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            printWelcome();
            System.out.println("Conectado al servidor " + host + ":" + port);

            // Hilo receptor: escucha mensajes del servidor de forma asíncrona
            Thread receiver = new Thread(this::receiveLoop, "ReceiverThread");
            receiver.setDaemon(true);
            receiver.start();

            // Enviar JOIN
            send(Protocol.build(Protocol.JOIN, playerName));

            // Bucle de entrada del usuario
            while (running && CONSOLE_SCANNER.hasNextLine()) {
                String line = CONSOLE_SCANNER.nextLine().trim();
                if (!line.isEmpty())
                    processUserInput(line);
            }

        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // ── Bucle receptor de mensajes del servidor ───────────────────────────────

    /**
     * Se ejecuta en un hilo separado, escuchando continuamente los mensajes
     * que envía el servidor y mostrándolos por pantalla.
     */
    private void receiveLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                handleServerMessage(line);
            }
        } catch (IOException e) {
            if (running)
                LOG.log(Level.WARNING, "Conexión perdida: " + e.getMessage());
        } finally {
            running = false;
        }
    }

    // ── Procesamiento de mensajes del servidor ────────────────────────────────

    private void handleServerMessage(String raw) {
        String type = Protocol.getType(raw);
        String data = Protocol.getData(raw);

        switch (type) {

            case Protocol.ACK:
                printLine(UIUtil.success(data));
                break;

            case Protocol.INFO:
                printLine(UIUtil.info(data));
                break;

            case Protocol.ROLE:
                printRole(data);
                break;

            case Protocol.NEW_ROUND:
                System.out.println();
                UIUtil.printSeparator();
                System.out.println(UIUtil.coloredBold("RONDA " + data + " EN CURSO", UIUtil.CYAN));
                UIUtil.printSeparator();
                break;

            case Protocol.ORDER:
                printLine(UIUtil.info("Orden de turno: " + data.replace(Protocol.LIST_SEP, " → ")));
                break;

            case Protocol.YOUR_TURN:
                System.out.println();
                printLine(UIUtil.coloredBold("[TU TURNO] Escribe una palabra y pulsa Enter:", UIUtil.MAGENTA));
                pendingAction = PendingAction.WORD;
                break;

            case Protocol.WORD_PLAYED:
                printWordPlayed(data);
                break;

            case Protocol.WORDS_SUMMARY:
                printWordsSummary(data);
                break;

            case Protocol.ASK_VOTE:
                System.out.println();
                printLine(UIUtil.highlight("[VOTACION] ¿Quieres votar para expulsar a alguien? (s/n):"));
                pendingAction = PendingAction.VOTE_DECISION;
                break;

            case Protocol.CAST_VOTE:
                showVoteCandidates(data);
                pendingAction = PendingAction.VOTE;
                break;

            case Protocol.EXPELLED:
                printExpelled(data);
                break;

            case Protocol.GUESS_NOW:
                System.out.println();
                printLine(
                        UIUtil.coloredBold("[ULTIMA OPORTUNIDAD] Has sido expulsado y eras el impostor.", UIUtil.RED));
                printLine(UIUtil.colored("Adivina la palabra secreta para intentar ganar:", UIUtil.YELLOW));
                pendingAction = PendingAction.GUESS;
                break;

            case Protocol.RESULT:
                printResult(data);
                running = false;
                break;

            case Protocol.ERROR:
                printLine(UIUtil.error(data));
                break;

            default:
                printLine(UIUtil.warning("Mensaje desconocido: " + raw));
        }
    }

    // ── Estado de acción pendiente ────────────────────────────────────────────

    private enum PendingAction {
        NONE, WORD, VOTE_DECISION, VOTE, GUESS
    }

    private volatile PendingAction pendingAction = PendingAction.NONE;

    /**
     * Procesa la entrada del usuario según la acción que el servidor esté
     * esperando. Incluye validaciones para evitar errores.
     */
    private void processUserInput(String input) {
        switch (pendingAction) {

            case WORD:
                String word = input.trim();
                if (word.isEmpty()) {
                    printLine(UIUtil.warning("La palabra no puede estar vacía."));
                } else if (word.length() > 20) {
                    printLine(UIUtil.warning("La palabra es demasiado larga (máx 20 caracteres)."));
                } else {
                    send(Protocol.build(Protocol.WORD, word));
                    pendingAction = PendingAction.NONE;
                }
                break;

            case VOTE_DECISION:
                boolean yes = input.equalsIgnoreCase("s") || input.equalsIgnoreCase("si")
                        || input.equalsIgnoreCase("sí") || input.equalsIgnoreCase("y");
                send(Protocol.build(Protocol.VOTE_DECISION, yes ? "YES" : "NO"));
                pendingAction = PendingAction.NONE;
                break;

            case VOTE:
                String target = resolveVoteTarget(input);
                if (target == null) {
                    printLine(UIUtil.error("Selecciona un número válido de la lista."));
                    break;
                }
                send(Protocol.build(Protocol.VOTE, target));
                pendingAction = PendingAction.NONE;
                voteCandidates.clear();
                break;

            case GUESS:
                String guess = input.trim();
                if (guess.isEmpty()) {
                    printLine(UIUtil.warning("La adivinanza no puede estar vacía."));
                } else {
                    send(Protocol.build(Protocol.GUESS, guess));
                    pendingAction = PendingAction.NONE;
                }
                break;

            case NONE:
                if (input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("salir")) {
                    send(Protocol.build(Protocol.QUIT, ""));
                    running = false;
                }
                break;
        }
    }

    // ── Helpers de presentación ───────────────────────────────────────────────

    private void printRole(String data) {
        String[] parts = data.split(Protocol.PAIR_SEP, 2);
        String rol = parts[0];
        String info = parts.length > 1 ? parts[1] : "";
        System.out.println();
        UIUtil.printSeparator();
        if ("IMPOSTOR".equals(rol)) {
            System.out.println(UIUtil.coloredBold("TU ROL: IMPOSTOR", UIUtil.RED));
            if ("SIN_PISTA".equals(info)) {
                System.out.println(UIUtil.warning("Pista: No tienes pista"));
            } else {
                System.out.println(UIUtil.info("Pista: " + info));
            }
            System.out.println(UIUtil.colored("Objetivo: Pasa desapercibido y evita que te expulsen.", UIUtil.YELLOW));
        } else {
            System.out.println(UIUtil.coloredBold("TU ROL: SOCIALISTA", UIUtil.GREEN));
            System.out.println(UIUtil.success("Palabra secreta: " + info));
            System.out.println(UIUtil.colored("Objetivo: Detecta al impostor y expulsalo.", UIUtil.YELLOW));
        }
        UIUtil.printSeparator();
    }

    private void printWordPlayed(String data) {
        String[] parts = data.split(Protocol.PAIR_SEP, 2);
        if (parts.length == 2) {
            String who = parts[0];
            String word = parts[1];
            if (who.equals(playerName)) {
                printLine(UIUtil.colored("  [TU CHAT] Tu dijiste: " + UIUtil.coloredBold(word, UIUtil.GREEN),
                        UIUtil.CYAN));
            } else {
                printLine(UIUtil.colored("  [CHAT] " + who + " dijo: " + UIUtil.coloredBold(word, UIUtil.YELLOW),
                        UIUtil.CYAN));
            }
        }
    }

    private void printWordsSummary(String data) {
        System.out.println();
        UIUtil.printSeparator();
        System.out.println(UIUtil.coloredBold("RESUMEN DE LA RONDA", UIUtil.CYAN));
        UIUtil.printSeparator();
        for (String pair : data.split(Protocol.LIST_SEP)) {
            String[] kv = pair.split(Protocol.PAIR_SEP, 2);
            if (kv.length == 2) {
                String marker = kv[0].equals(playerName) ? UIUtil.colored(" (tú)", UIUtil.GREEN) : "";
                String word = UIUtil.colored(kv[1], UIUtil.YELLOW);
                System.out.printf("  %-18s : %s%s%n", kv[0], word, marker);
            }
        }
        UIUtil.printSeparator();
    }

    private void printExpelled(String data) {
        String[] parts = data.split(Protocol.PAIR_SEP, 2);
        String name = parts[0];
        boolean isImpostor = parts.length > 1 && Boolean.parseBoolean(parts[1]);
        System.out.println();
        UIUtil.printSeparator();
        if (isImpostor) {
            System.out.println(UIUtil.coloredBold("¡¡IMPOSTOR EXPULSADO!!", UIUtil.RED));
            System.out.println(UIUtil.colored("Jugador: " + name, UIUtil.YELLOW));
        } else {
            System.out.println(UIUtil.colored("Jugador expulsado: " + name, UIUtil.YELLOW));
            System.out.println(UIUtil.success("Era socialista"));
        }
        UIUtil.printSeparator();
    }

    private void showVoteCandidates(String data) {
        voteCandidates.clear();
        if (!data.isBlank()) {
            for (String candidate : data.split(Protocol.LIST_SEP)) {
                String trimmed = candidate.trim();
                if (!trimmed.isEmpty()) {
                    voteCandidates.add(trimmed);
                }
            }
        }

        System.out.println();
        printLine(UIUtil.highlight("[VOTA] Elige a quien crees que es el impostor."));
        for (int i = 0; i < voteCandidates.size(); i++) {
            printLine(UIUtil.formatOption(i + 1, voteCandidates.get(i)));
        }
        printLine(UIUtil.colored("Escribe el número de la persona:", UIUtil.BLUE));
    }

    private String resolveVoteTarget(String input) {
        String trimmed = input.trim();

        try {
            int choice = Integer.parseInt(trimmed);
            switch (choice) {
                case 1:
                    return getVoteCandidate(0);
                case 2:
                    return getVoteCandidate(1);
                case 3:
                    return getVoteCandidate(2);
                case 4:
                    return getVoteCandidate(3);
                case 5:
                    return getVoteCandidate(4);
                case 6:
                    return getVoteCandidate(5);
                case 7:
                    return getVoteCandidate(6);
                case 8:
                    return getVoteCandidate(7);
                case 9:
                    return getVoteCandidate(8);
                case 10:
                    return getVoteCandidate(9);
                default:
                    return null;
            }
        } catch (NumberFormatException ignored) {
            for (String candidate : voteCandidates) {
                if (candidate.equalsIgnoreCase(trimmed)) {
                    return candidate;
                }
            }
            return null;
        }
    }

    private String getVoteCandidate(int index) {
        return index >= 0 && index < voteCandidates.size() ? voteCandidates.get(index) : null;
    }

    private void printResult(String data) {
        String[] parts = data.split(Protocol.PAIR_SEP, 3);
        String outcome = parts[0];
        String word = parts.length > 1 ? parts[1] : "?";
        String impostores = parts.length > 2 ? parts[2] : "?";
        System.out.println();
        UIUtil.printSeparator();
        System.out.println(UIUtil.coloredBold("FIN DE PARTIDA", UIUtil.MAGENTA));
        UIUtil.printSeparator();
        if ("SOCIALISTAS_WIN".equals(outcome)) {
            System.out.println(UIUtil.success("¡GANAN LOS SOCIALISTAS!"));
        } else {
            System.out.println(UIUtil.colored("¡GANA EL IMPOSTOR!", UIUtil.RED));
        }
        System.out.println();
        System.out.println(UIUtil.formatKeyValue("Palabra secreta", UIUtil.coloredBold(word, UIUtil.YELLOW)));
        System.out.println(UIUtil.formatKeyValue("Impostor(es)", impostores));
        UIUtil.printSeparator();
    }

    private void printLine(String msg) {
        System.out.println(msg);
    }

    private void printWelcome() {
        UIUtil.printTitle("JUEGO DEL IMPOSTOR - CLIENTE");
        System.out.println(UIUtil.info("Consejo: habla como si conocieras la palabra, pero sin decirla literal."));
        System.out.println();
    }

    // ── Envío y cierre ────────────────────────────────────────────────────────

    private void send(String message) {
        if (out != null)
            out.println(message);
    }

    private void disconnect() {
        running = false;
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error al cerrar cliente: " + e.getMessage());
        }
        System.out.println("Desconectado del servidor.");
    }
}
