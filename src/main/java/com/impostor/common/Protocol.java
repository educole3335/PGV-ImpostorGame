package com.impostor.common;

/**
 * Protocolo de comunicación entre cliente y servidor.
 * Define los tipos de mensajes intercambiados mediante sockets.
 *
 * Formato de mensaje: TIPO|datos
 */
public class Protocol {

    // ── Mensajes cliente → servidor ──────────────────────────────────────────
    /** El cliente envía su nombre de jugador. Formato: JOIN|nombre */
    public static final String JOIN          = "JOIN";

    /** El cliente está listo para ver su rol. Formato: READY| */
    public static final String READY         = "READY";

    /** El cliente envía la palabra que dijo en la ronda. Formato: WORD|palabra */
    public static final String WORD          = "WORD";

    /** El cliente vota por expulsar a alguien. Formato: VOTE|nombreJugador */
    public static final String VOTE          = "VOTE";

    /** El cliente decide si votar o pasar. Formato: VOTE_DECISION|YES o NO */
    public static final String VOTE_DECISION = "VOTE_DECISION";

    /** El impostor expulsado intenta adivinar la palabra. Formato: GUESS|palabra */
    public static final String GUESS         = "GUESS";

    /** El cliente se desconecta limpiamente. Formato: QUIT| */
    public static final String QUIT          = "QUIT";

    // ── Mensajes servidor → cliente ──────────────────────────────────────────
    /** Confirma la unión. Formato: ACK|Bienvenido, nombre */
    public static final String ACK           = "ACK";

    /** Informa al cliente de su rol. Formato: ROLE|IMPOSTOR:pista  o  ROLE|SOCIALISTA:palabra */
    public static final String ROLE          = "ROLE";

    /** Informa del orden de turno esta ronda. Formato: ORDER|nombre1,nombre2,... */
    public static final String ORDER         = "ORDER";

    /** Pide al jugador su turno de palabra. Formato: YOUR_TURN| */
    public static final String YOUR_TURN     = "YOUR_TURN";

    /** Difunde en tiempo real la palabra que acaba de decir un jugador. Formato: WORD_PLAYED|nombre:palabra */
    public static final String WORD_PLAYED   = "WORD_PLAYED";

    /** Difunde todas las palabras dichas al final de la ronda. Formato: WORDS_SUMMARY|nombre1:palabra1,nombre2:palabra2,... */
    public static final String WORDS_SUMMARY = "WORDS_SUMMARY";

    /** Pregunta si el cliente quiere votar. Formato: ASK_VOTE| */
    public static final String ASK_VOTE      = "ASK_VOTE";

    /** Pide al cliente que vote. Formato: CAST_VOTE|nombre1,nombre2,... */
    public static final String CAST_VOTE     = "CAST_VOTE";

    /** Anuncia al jugador expulsado. Formato: EXPELLED|nombre:esImpostor */
    public static final String EXPELLED      = "EXPELLED";

    /** Pide al impostor expulsado que adivine. Formato: GUESS_NOW| */
    public static final String GUESS_NOW     = "GUESS_NOW";

    /** Resultado final de la partida. Formato: RESULT|IMPOSTOR_WINS o SOCIALISTAS_WIN:palabra:impostores */
    public static final String RESULT        = "RESULT";

    /** Inicia una nueva ronda. Formato: NEW_ROUND|numeroRonda */
    public static final String NEW_ROUND     = "NEW_ROUND";

    /** Mensaje informativo general. Formato: INFO|texto */
    public static final String INFO          = "INFO";

    /** Error del servidor. Formato: ERROR|descripcion */
    public static final String ERROR         = "ERROR";

    // ── Utilidades ───────────────────────────────────────────────────────────
    public static final String SEPARATOR     = "|";
    public static final String LIST_SEP      = ",";
    public static final String PAIR_SEP      = ":";

    /** Construye un mensaje con el formato TIPO|datos */
    public static String build(String type, String data) {
        return type + SEPARATOR + data;
    }

    /** Extrae el tipo del mensaje */
    public static String getType(String message) {
        int idx = message.indexOf(SEPARATOR);
        return idx >= 0 ? message.substring(0, idx) : message;
    }

    /** Extrae los datos del mensaje */
    public static String getData(String message) {
        int idx = message.indexOf(SEPARATOR);
        return idx >= 0 ? message.substring(idx + 1) : "";
    }
}
