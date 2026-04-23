package com.impostor.model;

/**
 * Representa a un jugador dentro de la partida.
 * Almacena su nombre, rol, pista (si aplica) y estado en la partida.
 */
public class Player {

    public enum Role { SOCIALISTA, IMPOSTOR }

    private final String name;
    private Role   role;
    private String hint;          // Pista del impostor (puede ser null)
    private boolean eliminated;
    private String  wordSaidThisRound;

    public Player(String name) {
        this.name      = name;
        this.eliminated = false;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String  getName()               { return name; }
    public Role    getRole()               { return role; }
    public void    setRole(Role role)      { this.role = role; }
    public String  getHint()              { return hint; }
    public void    setHint(String hint)   { this.hint = hint; }
    public boolean isEliminated()         { return eliminated; }
    public void    setEliminated(boolean e){ this.eliminated = e; }
    public boolean isImpostor()           { return role == Role.IMPOSTOR; }
    public String  getWordSaidThisRound() { return wordSaidThisRound; }
    public void    setWordSaidThisRound(String w) { this.wordSaidThisRound = w; }

    @Override
    public String toString() {
        return String.format("Player{name='%s', role=%s, eliminated=%b}", name, role, eliminated);
    }
}
