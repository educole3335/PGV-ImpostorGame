package com.impostor.common;

/**
 * Utilidades para formateo de interfaz de usuario en consola.
 * Proporciona colores ANSI, separadores y formatos para mejorar
 * la presentación visual de los mensajes.
 */
public class UIUtil {

    // ── Códigos ANSI para colores ────────────────────────────────────────────
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";

    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";

    // ── Separadores y decoraciones ───────────────────────────────────────────
    public static final String SEP_LONG = "════════════════════════════════════════════════════════════";
    public static final String SEP_SHORT = "──────────────────────────────────────────────────────────";
    public static final String TITLE_START = "╔" + "════════════════════════════════════════════════════════╗";
    public static final String TITLE_END = "╚" + "════════════════════════════════════════════════════════╝";

    // ── Métodos de formato ───────────────────────────────────────────────────

    /**
     * Retorna el texto con color
     */
    public static String colored(String text, String color) {
        return color + text + RESET;
    }

    /**
     * Retorna el texto en negrita
     */
    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    /**
     * Retorna el texto con color y negrita
     */
    public static String coloredBold(String text, String color) {
        return color + BOLD + text + RESET;
    }

    /**
     * Imprime un separador largo
     */
    public static void printSeparator() {
        System.out.println(colored(SEP_LONG, CYAN));
    }

    /**
     * Imprime un título centrado
     */
    public static void printTitle(String title) {
        System.out.println(colored(TITLE_START, CYAN));
        String centered = centerText(title, 58);
        System.out.println(colored("║ " + centered + " ║", CYAN));
        System.out.println(colored(TITLE_END, CYAN));
    }

    /**
     * Centra un texto dentro de un ancho específico
     */
    private static String centerText(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int totalPadding = width - text.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        return " ".repeat(Math.max(0, leftPadding)) + text + " ".repeat(Math.max(0, rightPadding));
    }

    /**
     * Retorna un mensaje de éxito (verde)
     */
    public static String success(String message) {
        return colored("✓ " + message, GREEN);
    }

    /**
     * Retorna un mensaje de error (rojo)
     */
    public static String error(String message) {
        return colored("✗ " + message, RED);
    }

    /**
     * Retorna un mensaje de advertencia (amarillo)
     */
    public static String warning(String message) {
        return colored("⚠ " + message, YELLOW);
    }

    /**
     * Retorna un mensaje de información (azul)
     */
    public static String info(String message) {
        return colored("ℹ " + message, BLUE);
    }

    /**
     * Retorna un mensaje destacado (magenta)
     */
    public static String highlight(String message) {
        return coloredBold(message, MAGENTA);
    }

    /**
     * Formatea un par clave-valor
     */
    public static String formatKeyValue(String key, String value) {
        return colored(key + ": ", BOLD) + value;
    }

    /**
     * Formatea una opción de lista
     */
    public static String formatOption(int number, String text) {
        return colored(number + ") ", CYAN) + text;
    }
}
