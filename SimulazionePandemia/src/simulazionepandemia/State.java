/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package simulazionepandemia;

import java.awt.Color;

/**
 * Enumerazione che rappresenta gli stati di salute possibili per una persona nella simulazione.
 * 
 * Utilizza un sistema di flag binari per combinare stati multipli:
 * - None (0b000): Nessuno stato particolare
 * - Suscettibile (0b001): Può essere infettato
 * - Sintomatico (0b010): Presenta sintomi
 * - Infettivo (0b100): Può contagiare altri
 * 
 * Stati composti (combinazioni):
 * - Vaccinato = None (immune, non può infettare né essere infettato)
 * - Infetto = Sintomatico + Infettivo (mostra sintomi e contagia)
 * - Morto = Sintomatico (era sintomatico, ora morto)
 * - Asintomatico = Infettivo (può contagiare ma senza sintomi)
 * 
 * Ogni stato ha un colore associato per la visualizzazione:
 * - Giallo: Vaccinato/Immune
 * - Grigio: Suscettibile
 * - Rosso: Sintomatico
 * - Arancione: Infettivo
 * - Combinazioni: Mix dei colori base
 * 
 * @author Andrea
 */
public enum State {
    // ============================================
    // STATI BASE (con valori binari e colori)
    // ============================================
    
    /**
     * Nessuno stato particolare - Usato per persone vaccinate/immuni.
     * Valore: 0b000 (0), Colore: Giallo
     */
    None(0b000, Color.YELLOW),
    
    /**
     * Suscettibile all'infezione - Persona sana ma vulnerabile.
     * Valore: 0b001 (1), Colore: Grigio
     */
    Suscettibile(0b001, Color.GRAY),
    
    /**
     * Presenta sintomi visibili della malattia.
     * Valore: 0b010 (2), Colore: Rosso
     */
    Sintomatico(0b010, Color.RED),
    
    /**
     * Può trasmettere l'infezione ad altri.
     * Valore: 0b100 (4), Colore: Arancione
     */
    Infettivo(0b100, Color.ORANGE),

    // ============================================
    // STATI COMPOSTI (combinazioni degli stati base)
    // ============================================
    
    /**
     * Vaccinato/Immune - Non può essere infettato né infettare.
     * Combinazione: None
     * Valore: 0, Colore: Giallo
     */
    Vaccinato(None),
    
    /**
     * Infetto con sintomi - Presenta sintomi E può contagiare.
     * Combinazione: Sintomatico | Infettivo
     * Valore: 6 (0b110), Colore: Mix Rosso-Arancione
     */
    Infetto(Sintomatico, Infettivo),
    
    /**
     * Morto - Era sintomatico e poi è deceduto.
     * Combinazione: Sintomatico
     * Valore: 2, Colore: Rosso
     */
    Morto(Sintomatico),
    
    /**
     * Asintomatico - Infetto senza sintomi ma può contagiare.
     * Combinazione: Infettivo
     * Valore: 4, Colore: Arancione
     */
    Asintomatico(Infettivo);

    // ============================================
    // ATTRIBUTI
    // ============================================
    
    /**
     * Valore numerico dello stato (combinazione di flag binari)
     */
    private final int value;
    
    /**
     * Colore associato allo stato per la visualizzazione
     */
    private final Color color;

    // ============================================
    // COSTRUTTORI
    // ============================================
    
    /**
     * Costruttore per stati base con valore e colore espliciti.
     * 
     * @param value Valore binario dello stato
     * @param color Colore associato
     */
    State(int value, Color color) {
        this.value = value;
        this.color = color;
    }

    /**
     * Costruttore per stati composti che combina più stati base.
     * 
     * Funziona combinando:
     * - I valori binari con OR bit a bit (|)
     * - I colori con AND sui canali RGB (&)
     * 
     * Esempio: Infetto = Sintomatico | Infettivo
     * - Valore: 0b010 | 0b100 = 0b110 (6)
     * - Colore: Mix di Rosso e Arancione
     * 
     * @param states Stati da combinare (parametri variadici)
     */
    State(State... states) {
        int combinedValue = 0;
        int red = 255;
        int green = 255;
        int blue = 255;
        Color combinedColor = null;
        
        // Combina tutti gli stati passati
        for (State state : states) {
            // OR bit a bit per i valori
            combinedValue |= state.value;
            
            // AND bit a bit per i canali di colore
            // Questo crea un mix visivo dei colori
            red &= state.color.getRed();
            green &= state.color.getGreen();
            blue &= state.color.getBlue();
            
            // Crea il nuovo colore combinato
            combinedColor = new Color(red, green, blue);
        }
        
        this.value = combinedValue;
        this.color = combinedColor;
    }

    // ============================================
    // METODI GETTER
    // ============================================
    
    /**
     * Restituisce il valore numerico dello stato.
     * Utile per confronti rapidi negli switch case.
     * 
     * @return Valore intero dello stato
     */
    public int getValue() {
        return value;
    }

    /**
     * Restituisce il colore associato allo stato.
     * Usato per il rendering grafico delle persone.
     * 
     * @return Colore dello stato
     */
    public Color getColor() {
        return color;
    }
}
