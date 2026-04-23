package com.impostor.common;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Banco de palabras del juego del Impostor.
 * Cada palabra incluye varias pistas para el impostor.
 */
public class WordBank {

    /** Representa una entrada del banco: palabra + lista de pistas */
    public static class Entry {
        private final String word;
        private final List<String> hints;

        public Entry(String word, String... hints) {
            this.word  = word;
            this.hints = Arrays.asList(hints);
        }

        public String getWord()  { return word; }
        public List<String> getHints() { return hints; }

        /** Devuelve una pista aleatoria */
        public String randomHint() {
            return hints.get(new Random().nextInt(hints.size()));
        }
    }

    private static final List<Entry> WORDS = Arrays.asList(
        new Entry("Playa",       "arena", "olas", "sombrilla", "bañador", "chiringuito"),
        new Entry("Pizza",       "mozzarella", "horno", "pepperoni", "masa", "napolitana"),
        new Entry("Hospital",    "bata", "jeringuilla", "camilla", "urgencias", "estetoscopio"),
        new Entry("Avión",       "pista", "altitud", "turbulencia", "pasaporte", "azafata"),
        new Entry("Biblioteca",  "silencio", "estante", "préstamo", "marcapáginas", "catálogo"),
        new Entry("Volcán",      "lava", "ceniza", "erupción", "magma", "cráter"),
        new Entry("Submarino",   "periscopio", "profundidad", "sonar", "torpedo", "escotilla"),
        new Entry("Carnaval",    "disfraz", "confeti", "desfile", "comparsa", "antifaz"),
        new Entry("Videojuego",  "mando", "nivel", "pantalla", "respawn", "píxel"),
        new Entry("Mercado",     "puesto", "regatear", "verdulero", "báscula", "bolsa"),
        new Entry("Castillo",    "torre", "foso", "almena", "puente levadizo", "escudo"),
        new Entry("Circo",       "trapecio", "payaso", "domador", "carpa", "malabarismo"),
        new Entry("Farmacia",    "receta", "pastilla", "jarabe", "mostrador", "genérico"),
        new Entry("Tren",        "andén", "vagón", "raíl", "locomotora", "revisores"),
        new Entry("Museo",       "exposición", "cuadro", "guía", "vitrina", "escultura")
    );

    private static final Random RNG = new Random();

    /** Devuelve una entrada aleatoria del banco */
    public static Entry random() {
        return WORDS.get(RNG.nextInt(WORDS.size()));
    }
}
