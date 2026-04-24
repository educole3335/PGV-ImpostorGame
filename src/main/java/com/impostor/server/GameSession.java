package com.impostor.server;

import com.impostor.common.Protocol;
import com.impostor.common.WordBank;
import com.impostor.model.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Gestiona el estado completo de una partida del Juego del Impostor.
 *
 * Esta clase es thread-safe: los métodos críticos están sincronizados
 * para que varios hilos (uno por cliente) puedan interactuar de forma segura.
 *
 * Ciclo de la partida:
 * 1. Fase de unión (LOBBY): se esperan los jugadores configurados.
 * 2. Asignación de roles: se sortean impostores y se reparte la palabra/pista.
 * 3. Rondas: orden aleatorio → cada jugador dice una palabra → votación →
 * expulsión.
 * 4. Fin: el impostor es expulsado (y acierta/falla) o todos los impostores son
 * expulsados.
 */
public class GameSession {

    private static final Logger LOG = Logger.getLogger(GameSession.class.getName());

    // ── Configuración ────────────────────────────────────────────────────────
    private final int totalPlayers;
    private final int numImpostors;
    private final boolean impostorHasHint;

    // ── Estado ───────────────────────────────────────────────────────────────
    /** Mapa nombre → manejador del cliente */
    private final Map<String, ClientHandler> handlers = new ConcurrentHashMap<>();
    /** Lista ordenada de jugadores (orden de unión) */
    private final List<Player> players = Collections.synchronizedList(new ArrayList<>());

    private WordBank.Entry currentWord;
    private int round = 0;
    private Phase phase = Phase.LOBBY;

    /** Contadores de sincronización entre hilos */
    private final AtomicInteger wordCount = new AtomicInteger(0);
    private final AtomicInteger voteDecisionYes = new AtomicInteger(0);
    private final AtomicInteger voteDecisionNo = new AtomicInteger(0);
    private final AtomicInteger voteDecisionCount = new AtomicInteger(0);
    private final Map<String, AtomicInteger> votes = new ConcurrentHashMap<>();

    private List<String> roundOrder = new ArrayList<>();
    private int turnIndex = 0;
    private final List<String> expelledImpostors = new ArrayList<>();
    private final Deque<String> pendingGuessQueue = new ArrayDeque<>();
    private String currentGuessingPlayer = null;

    enum Phase {
        LOBBY, ROLES, ROUND, VOTE_DECISION, VOTING, EXPELLED_GUESS, FINISHED
    }

    // ── Constructor ──────────────────────────────────────────────────────────

    public GameSession(int totalPlayers, int numImpostors, boolean impostorHasHint) {
        this.totalPlayers = totalPlayers;
        this.numImpostors = numImpostors;
        this.impostorHasHint = impostorHasHint;
    }

    // ── Fase de lobby ────────────────────────────────────────────────────────

    /**
     * Registra un nuevo jugador. Devuelve false si el nombre ya existe o la sala
     * está llena.
     * 
     * Thread-safe: usa synchronized para evitar condiciones de carrera.
     */
    public synchronized boolean addPlayer(String name, ClientHandler handler) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (players.size() >= totalPlayers) {
            return false;
        }
        if (handlers.containsKey(name)) {
            return false;
        }

        try {
            Player p = new Player(name);
            players.add(p);
            handlers.put(name, handler);
            LOG.info("Jugador unido: " + name + " (" + players.size() + "/" + totalPlayers + ")");

            broadcast(Protocol.build(Protocol.INFO,
                    name + " se ha unido. (" + players.size() + "/" + totalPlayers + ")"));

            if (players.size() == totalPlayers) {
                startGame();
            }
            return true;
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.SEVERE, "Error al agregar jugador: " + e.getMessage());
            return false;
        }
    }

    // ── Inicio y asignación de roles ─────────────────────────────────────────

    private void startGame() {
        phase = Phase.ROLES;
        currentWord = WordBank.random();
        expelledImpostors.clear();
        pendingGuessQueue.clear();
        currentGuessingPlayer = null;
        LOG.info("Partida iniciada. Palabra: " + currentWord.getWord());

        // Sortear impostores
        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled);
        List<String> impostorNames = new ArrayList<>();
        for (int i = 0; i < numImpostors; i++) {
            Player imp = shuffled.get(i);
            imp.setRole(Player.Role.IMPOSTOR);
            String hint = impostorHasHint ? currentWord.randomHint() : null;
            imp.setHint(hint);
            impostorNames.add(imp.getName());
        }
        // El resto son socialistas
        for (Player p : players) {
            if (!p.isImpostor())
                p.setRole(Player.Role.SOCIALISTA);
        }

        // Enviar rol a cada jugador
        for (Player p : players) {
            ClientHandler h = handlers.get(p.getName());
            if (h == null)
                continue;
            if (p.isImpostor()) {
                String hintPart = (p.getHint() != null) ? p.getHint() : "SIN_PISTA";
                h.sendMessage(Protocol.build(Protocol.ROLE, "IMPOSTOR" + Protocol.PAIR_SEP + hintPart));
            } else {
                h.sendMessage(Protocol.build(Protocol.ROLE, "SOCIALISTA" + Protocol.PAIR_SEP + currentWord.getWord()));
            }
        }

        startNewRound();
    }

    // ── Gestión de rondas ─────────────────────────────────────────────────────

    private void startNewRound() {
        if (hasTwoPlayersWithOneImpostor()) {
            endGame(false);
            return;
        }

        round++;
        phase = Phase.ROUND;
        turnIndex = 0;
        wordCount.set(0);

        List<Player> active = getActivePlayers();
        roundOrder = new ArrayList<>(active.stream().map(Player::getName).collect(Collectors.toList()));
        Collections.shuffle(roundOrder);

        // Limpiar palabras de la ronda anterior
        for (Player p : active)
            p.setWordSaidThisRound(null);

        String orderStr = String.join(Protocol.LIST_SEP, roundOrder);
        broadcast(Protocol.build(Protocol.NEW_ROUND, String.valueOf(round)));
        broadcast(Protocol.build(Protocol.ORDER, orderStr));

        askNextTurn();
    }

    /** Pide al siguiente jugador en el orden que diga su palabra */
    private void askNextTurn() {
        if (turnIndex >= roundOrder.size()) {
            // Todos dijeron su palabra → mostrar resumen
            broadcastWordsSummary();
            askVoteDecision();
            return;
        }
        String name = roundOrder.get(turnIndex);
        ClientHandler h = handlers.get(name);
        if (h != null)
            h.sendMessage(Protocol.build(Protocol.YOUR_TURN, ""));
    }

    // ── Recepción de palabras ─────────────────────────────────────────────────

    public synchronized void receiveWord(String playerName, String word) {
        Player p = findPlayer(playerName);
        if (p == null || phase != Phase.ROUND)
            return;

        p.setWordSaidThisRound(word);
        wordCount.incrementAndGet();
        LOG.info(playerName + " dijo: " + word);

        turnIndex++;
        // Difundir la palabra dicha a todos los jugadores en tiempo real
        broadcast(Protocol.build(Protocol.WORD_PLAYED, playerName + Protocol.PAIR_SEP + word));
        askNextTurn();
    }

    private void broadcastWordsSummary() {
        StringBuilder sb = new StringBuilder();
        for (String name : roundOrder) {
            Player p = findPlayer(name);
            if (p != null) {
                if (sb.length() > 0)
                    sb.append(Protocol.LIST_SEP);
                sb.append(name).append(Protocol.PAIR_SEP)
                        .append(p.getWordSaidThisRound() != null ? p.getWordSaidThisRound() : "?");
            }
        }
        broadcast(Protocol.build(Protocol.WORDS_SUMMARY, sb.toString()));
    }

    // ── Decisión de voto ──────────────────────────────────────────────────────

    private void askVoteDecision() {
        phase = Phase.VOTE_DECISION;
        voteDecisionYes.set(0);
        voteDecisionNo.set(0);
        voteDecisionCount.set(0);
        broadcast(Protocol.build(Protocol.ASK_VOTE, ""));
    }

    public synchronized void receiveVoteDecision(String playerName, boolean wantsVote) {
        if (phase != Phase.VOTE_DECISION)
            return;
        voteDecisionCount.incrementAndGet();
        if (wantsVote)
            voteDecisionYes.incrementAndGet();
        else
            voteDecisionNo.incrementAndGet();

        int active = getActivePlayers().size();
        if (voteDecisionCount.get() >= active) {
            if (voteDecisionYes.get() > voteDecisionNo.get()) {
                startVoting();
            } else {
                startNewRound();
            }
        }
    }

    // ── Votación ──────────────────────────────────────────────────────────────

    private void startVoting() {
        phase = Phase.VOTING;
        votes.clear();
        List<Player> active = getActivePlayers();
        active.forEach(p -> votes.put(p.getName(), new AtomicInteger(0)));

        String candidates = active.stream().map(Player::getName)
                .collect(Collectors.joining(Protocol.LIST_SEP));
        broadcast(Protocol.build(Protocol.CAST_VOTE, candidates));
    }

    public synchronized void receiveVote(String voterName, String targetName) {
        if (phase != Phase.VOTING)
            return;
        AtomicInteger cnt = votes.get(targetName);
        if (cnt != null) {
            cnt.incrementAndGet();
            LOG.info(voterName + " vota por " + targetName);
        }

        // Si todos los activos han votado → resolver
        long totalVotes = votes.values().stream().mapToLong(AtomicInteger::get).sum();
        if (totalVotes >= getActivePlayers().size()) {
            resolveVoting();
        }
    }

    private void resolveVoting() {
        String expelled = votes.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().get()))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (expelled == null)
            return;

        Player expPlayer = findPlayer(expelled);
        if (expPlayer == null)
            return;

        expPlayer.setEliminated(true);
        boolean isImpostor = expPlayer.isImpostor();
        broadcast(Protocol.build(Protocol.EXPELLED, expelled + Protocol.PAIR_SEP + isImpostor));
        LOG.info("Expulsado: " + expelled + " (impostor=" + isImpostor + ")");

        if (hasTwoPlayersWithOneImpostor()) {
            endGame(false);
            return;
        }

        if (isImpostor) {
            expelledImpostors.add(expelled);
            long remainingImpostors = getActivePlayers().stream().filter(Player::isImpostor).count();
            if (numImpostors == 1) {
                startGuessSequence(Collections.singletonList(expelled),
                        expelled + " es el impostor. Tiene una oportunidad de adivinar la palabra.");
            } else if (remainingImpostors == 0) {
                startGuessSequence(new ArrayList<>(expelledImpostors),
                        "Todos los impostores han sido expulsados. Tendrán una última oportunidad de adivinar la palabra.");
            } else {
                startNewRound();
            }
        } else {
            long remainingImpostors = getActivePlayers().stream().filter(Player::isImpostor).count();
            if (remainingImpostors == 0) {
                endGame(true); // Todos los impostores expulsados
            } else {
                startNewRound();
            }
        }
    }

    // ── Adivinanza del impostor ───────────────────────────────────────────────

    public synchronized void receiveGuess(String playerName, String guess) {
        if (phase != Phase.EXPELLED_GUESS)
            return;
        if (!playerName.equals(currentGuessingPlayer))
            return;

        boolean correct = guess.trim().equalsIgnoreCase(currentWord.getWord().trim());
        LOG.info("Impostor adivina: '" + guess + "' → " + (correct ? "correcto" : "incorrecto"));

        if (correct) {
            endGame(false); // Impostor gana
            return;
        }

        if (!pendingGuessQueue.isEmpty()) {
            askNextPendingGuess();
        } else {
            endGame(true); // Socialistas ganan
        }
    }

    // ── Fin de partida ────────────────────────────────────────────────────────

    private void endGame(boolean socialistasWin) {
        phase = Phase.FINISHED;
        pendingGuessQueue.clear();
        currentGuessingPlayer = null;
        String impostorNames = players.stream()
                .filter(Player::isImpostor)
                .map(Player::getName)
                .collect(Collectors.joining(Protocol.LIST_SEP));

        String result = (socialistasWin ? "SOCIALISTAS_WIN" : "IMPOSTOR_WINS")
                + Protocol.PAIR_SEP + currentWord.getWord()
                + Protocol.PAIR_SEP + impostorNames;

        broadcast(Protocol.build(Protocol.RESULT, result));
        LOG.info("Fin de partida. " + result);
    }

    // ── Desconexión ───────────────────────────────────────────────────────────

    /**
     * Elimina un jugador cuando se desconecta. Notifica a otros jugadores
     * y realiza limpieza necesaria.
     * 
     * Thread-safe para evitar duplicar desconexiones.
     */
    public synchronized void removePlayer(String name) {
        if (name == null || !handlers.containsKey(name)) {
            return;
        }

        try {
            handlers.remove(name);
            Player p = findPlayer(name);
            if (p != null) {
                p.setEliminated(true);
            }
            broadcast(Protocol.build(Protocol.INFO, name + " se ha desconectado."));
            LOG.warning("Jugador desconectado: " + name);

            // Si todos se desconectaron, terminar la partida
            if (getActivePlayers().isEmpty()) {
                phase = Phase.FINISHED;
            }
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.WARNING, "Error al remover jugador: " + e.getMessage());
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private void broadcast(String message) {
        for (ClientHandler h : handlers.values()) {
            h.sendMessage(message);
        }
    }

    private List<Player> getActivePlayers() {
        return players.stream().filter(p -> !p.isEliminated()).collect(Collectors.toList());
    }

    private boolean hasTwoPlayersWithOneImpostor() {
        List<Player> active = getActivePlayers();
        long activeImpostors = active.stream().filter(Player::isImpostor).count();
        return active.size() == 2 && activeImpostors == 1;
    }

    private void startGuessSequence(List<String> guessers, String infoMessage) {
        pendingGuessQueue.clear();
        pendingGuessQueue.addAll(guessers);
        phase = Phase.EXPELLED_GUESS;
        if (infoMessage != null && !infoMessage.isBlank()) {
            broadcast(Protocol.build(Protocol.INFO, infoMessage));
        }
        askNextPendingGuess();
    }

    private void askNextPendingGuess() {
        currentGuessingPlayer = pendingGuessQueue.pollFirst();
        if (currentGuessingPlayer == null) {
            endGame(true);
            return;
        }

        ClientHandler handler = handlers.get(currentGuessingPlayer);
        if (handler != null) {
            handler.sendMessage(Protocol.build(Protocol.GUESS_NOW, ""));
        }
    }

    private Player findPlayer(String name) {
        return players.stream().filter(p -> p.getName().equals(name)).findFirst().orElse(null);
    }

    public boolean isFull() {
        return players.size() >= totalPlayers;
    }

    public boolean isFinished() {
        return phase == Phase.FINISHED;
    }

    public int getPlayerCount() {
        return players.size();
    }
}
