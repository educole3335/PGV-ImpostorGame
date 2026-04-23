package com.impostor.server;

import com.impostor.common.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hilo que gestiona la comunicación con un cliente concreto.
 *
 * Cada instancia de ClientHandler se ejecuta en su propio hilo,
 * lo que permite que el servidor atienda varios clientes simultáneamente.
 *
 * Librerías empleadas:
 * - java.net.Socket → extremo de la conexión TCP con el cliente
 * - java.io.BufferedReader → lectura eficiente de mensajes de texto
 * - java.io.PrintWriter → escritura de mensajes de texto al cliente
 * - java.lang.Thread → ejecución concurrente (via Runnable)
 */
public class ClientHandler implements Runnable {

    private static final Logger LOG = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final GameSession session;
    private BufferedReader in;
    private PrintWriter out;
    private String playerName;
    private boolean disconnected = false;

    public ClientHandler(Socket socket, GameSession session) {
        this.socket = socket;
        this.session = session;
    }

    // ── Ciclo principal del hilo ──────────────────────────────────────────────

    @Override
    public void run() {
        try {
            // Inicializar flujos de E/S
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            LOG.info("Cliente conectado desde: " + socket.getInetAddress());

            // Primer mensaje esperado: JOIN|nombre
            String line = in.readLine();
            if (line == null || !Protocol.getType(line).equals(Protocol.JOIN)) {
                sendMessage(Protocol.build(Protocol.ERROR, "Se esperaba JOIN|nombre"));
                return;
            }

            playerName = Protocol.getData(line).trim();
            if (playerName.isEmpty()) {
                sendMessage(Protocol.build(Protocol.ERROR, "El nombre no puede estar vacío"));
                return;
            }

            if (!session.addPlayer(playerName, this)) {
                sendMessage(Protocol.build(Protocol.ERROR, "Nombre duplicado o sala llena"));
                return;
            }

            sendMessage(Protocol.build(Protocol.ACK, "Bienvenido, " + playerName));

            // Bucle de mensajes
            while ((line = in.readLine()) != null) {
                handleMessage(line);
            }

        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error de E/S con cliente " + playerName + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // ── Procesamiento de mensajes entrantes ───────────────────────────────────

    /**
     * Redirige cada mensaje recibido al método correspondiente de la sesión.
     */
    private void handleMessage(String raw) {
        String type = Protocol.getType(raw);
        String data = Protocol.getData(raw);
        LOG.fine("Recibido de " + playerName + ": " + raw);

        switch (type) {
            case Protocol.WORD:
                session.receiveWord(playerName, data);
                break;

            case Protocol.VOTE_DECISION:
                boolean wants = "YES".equalsIgnoreCase(data);
                session.receiveVoteDecision(playerName, wants);
                break;

            case Protocol.VOTE:
                session.receiveVote(playerName, data);
                break;

            case Protocol.GUESS:
                session.receiveGuess(playerName, data);
                break;

            case Protocol.QUIT:
                LOG.info(playerName + " ha enviado QUIT.");
                disconnect();
                break;

            default:
                sendMessage(Protocol.build(Protocol.ERROR, "Mensaje desconocido: " + type));
        }
    }

    // ── Envío de mensajes ─────────────────────────────────────────────────────

    /**
     * Envía un mensaje al cliente de forma thread-safe.
     * PrintWriter con autoFlush=true garantiza el envío inmediato.
     */
    public synchronized void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // ── Cierre de conexión ────────────────────────────────────────────────────

    private synchronized void disconnect() {
        if (disconnected) {
            return;
        }
        disconnected = true;
        try {
            if (playerName != null)
                session.removePlayer(playerName);
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null && !socket.isClosed())
                socket.close();
            LOG.info("Conexión cerrada para: " + playerName);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error al cerrar conexión: " + e.getMessage());
        }
    }

    public String getPlayerName() {
        return playerName;
    }
}
