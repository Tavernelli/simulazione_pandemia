/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package simulazionepandemia;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Classe che gestisce la selezione di un'area rettangolare con il mouse.
 * 
 * Utilizzata per implementare due funzionalità:
 * 1. VACCINO: Seleziona un'area e vaccina tutte le persone al suo interno
 * 2. QUARANTENA: Seleziona un'area e blocca le persone per un tempo determinato
 * 
 * Estende MouseAdapter per gestire eventi del mouse in modo efficiente.
 * 
 * @author Andrea
 */
class RettangoloMouse extends MouseAdapter {

    // ============================================
    // ATTRIBUTI DEL RETTANGOLO
    // ============================================
    
    /**
     * Coordinata X del punto iniziale del rettangolo (dove l'utente ha premuto il mouse)
     */
    private int Rx;
    
    /**
     * Coordinata Y del punto iniziale del rettangolo (dove l'utente ha premuto il mouse)
     */
    private int Ry;
    
    /**
     * Coordinata X del punto finale del rettangolo (dove l'utente ha rilasciato il mouse)
     */
    private int Rx2;
    
    /**
     * Coordinata Y del punto finale del rettangolo (dove l'utente ha rilasciato il mouse)
     */
    private int Ry2;
    
    /**
     * Flag che indica se il listener del mouse è attivo.
     * Quando false, il rettangolo non viene visualizzato né gestito.
     */
    private boolean mouseListenerIsActive;

    /**
     * Costruttore del gestore del rettangolo di selezione.
     * 
     * @param Rx Coordinata X iniziale
     * @param Ry Coordinata Y iniziale
     * @param Rx2 Coordinata X finale
     * @param Ry2 Coordinata Y finale
     * @param mouseListenerIsActive Se true, il listener è attivo
     */
    public RettangoloMouse(int Rx, int Ry, int Rx2, int Ry2, boolean mouseListenerIsActive) {
        this.Rx = Rx;
        this.Ry = Ry;
        this.Rx2 = Rx2;
        this.Ry2 = Ry2;
        this.mouseListenerIsActive = mouseListenerIsActive;
    }

    // ============================================
    // GETTER E SETTER
    // ============================================
    
    /**
     * Attiva o disattiva il listener del mouse.
     * @param mouseListenerIsActive true per attivare, false per disattivare
     */
    public void setMouseListenerIsActive(boolean mouseListenerIsActive) {
        this.mouseListenerIsActive = mouseListenerIsActive;
    }

    /**
     * Verifica se il listener del mouse è attivo.
     * @return true se attivo, false altrimenti
     */
    public boolean isMouseListenerIsActive() {
        return mouseListenerIsActive;
    }

    /**
     * Imposta la coordinata X iniziale.
     * @param Rx Nuova coordinata X iniziale
     */
    public void setRx(int Rx) {
        this.Rx = Rx;
    }

    /**
     * Imposta la coordinata Y iniziale.
     * @param Ry Nuova coordinata Y iniziale
     */
    public void setRy(int Ry) {
        this.Ry = Ry;
    }

    /**
     * Imposta la coordinata X finale.
     * @param Rx2 Nuova coordinata X finale
     */
    public void setRx2(int Rx2) {
        this.Rx2 = Rx2;
    }

    /**
     * Imposta la coordinata Y finale.
     * @param Ry2 Nuova coordinata Y finale
     */
    public void setRy2(int Ry2) {
        this.Ry2 = Ry2;
    }

    /**
     * Ottiene la coordinata X iniziale.
     * @return Coordinata X iniziale
     */
    public int getRx() {
        return Rx;
    }

    /**
     * Ottiene la coordinata Y iniziale.
     * @return Coordinata Y iniziale
     */
    public int getRy() {
        return Ry;
    }

    /**
     * Ottiene la coordinata X finale.
     * @return Coordinata X finale
     */
    public int getRx2() {
        return Rx2;
    }

    /**
     * Ottiene la coordinata Y finale.
     * @return Coordinata Y finale
     */
    public int getRy2() {
        return Ry2;
    }

    // ============================================
    // GESTIONE EVENTI MOUSE
    // ============================================
    
    /**
     * Chiamato quando l'utente preme il pulsante del mouse.
     * Registra il punto iniziale del rettangolo.
     * 
     * @param e Evento del mouse
     */
    @Override
    public void mousePressed(MouseEvent e) {
        setStartPoint(e.getX(), e.getY());
    }

    /**
     * Chiamato quando l'utente trascina il mouse con il pulsante premuto.
     * Aggiorna continuamente il punto finale per mostrare il rettangolo in tempo reale.
     * 
     * @param e Evento del mouse
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        setEndPoint(e.getX(), e.getY());
    }

    /**
     * Chiamato quando l'utente rilascia il pulsante del mouse.
     * Finalizza il rettangolo e applica l'azione (vaccino o quarantena)
     * a tutte le persone all'interno dell'area selezionata.
     * 
     * @param e Evento del mouse
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        setEndPoint(e.getX(), e.getY());
        
        // Conta quanti infetti sono stati vaccinati (per aggiornare le statistiche)
        int contaInfettiVaccinati = 0;
        
        // Itera su tutte le persone nella simulazione
        for (Persona p1 : Pandemia.getPersone()) {
            // Verifica se la persona è all'interno del rettangolo
            if (contains(p1.getX(), p1.getY())) {
                
                // ========================================
                // MODALITÀ VACCINO (checkBoxState == 1)
                // ========================================
                if (Pannello_di_controllo.getCheckBoxState() == 1) {
                    // Vaccina solo le persone suscettibili, infette o asintomatiche
                    if (p1.getStato() == State.Suscettibile || 
                        p1.getStato() == State.Infetto || 
                        p1.getStato() == State.Asintomatico) {
                        
                        // Se la persona era infetta, conta per le statistiche
                        if (p1.getStato() == State.Infetto || 
                            p1.getStato() == State.Asintomatico) {
                            contaInfettiVaccinati++;
                        }
                        
                        // Vaccina la persona (la rende immune)
                        p1.setStato(State.Vaccinato);
                    }
                    
                } 
                // ========================================
                // MODALITÀ QUARANTENA (checkBoxState == -1)
                // ========================================
                else if (Pannello_di_controllo.getCheckBoxState() == -1) {
                    // Blocca le persone in quarantena
                    // (la gestione del timer di sblocco è in Pandemia.move())
                    setEndPoint(e.getX(), e.getY());
                }
                
                // NOTA: Se una persona già vaccinata viene ri-selezionata,
                // può tornare suscettibile (funzionalità attualmente commentata sotto)
            }
        }
        
        // Aggiorna le statistiche della popolazione
        // Riduce il numero di infetti in base a quanti sono stati vaccinati
        Pandemia.PopolazioneInfettaInziale -= contaInfettiVaccinati;
        
        // Aggiorna l'interfaccia utente con i nuovi valori
        int[] currentCounts = Pandemia.calcolaNumeroInfettiImmuniAttuali();
        Pannello_di_controllo.setUI(
            Pandemia.PopolazioneIniziale,
            Pandemia.PopolazioneInfettaInziale,
            currentCounts[1]
        );
    }

    // ============================================
    // METODI DI UTILITÀ
    // ============================================
    
    /**
     * Imposta il punto iniziale del rettangolo.
     * Chiamato quando l'utente preme il mouse.
     * 
     * @param Rx Coordinata X iniziale
     * @param Ry Coordinata Y iniziale
     */
    public void setStartPoint(int Rx, int Ry) {
        this.Rx = Rx;
        this.Ry = Ry;
    }

    /**
     * Imposta il punto finale del rettangolo.
     * Chiamato quando l'utente trascina o rilascia il mouse.
     * 
     * @param Rx2 Coordinata X finale
     * @param Ry2 Coordinata Y finale
     */
    public void setEndPoint(int Rx2, int Ry2) {
        this.Rx2 = Rx2;
        this.Ry2 = Ry2;
    }

    /**
     * Verifica se un punto (x, y) è all'interno del rettangolo selezionato.
     * 
     * Il metodo normalizza il rettangolo per gestire correttamente i casi in cui
     * l'utente trascina da destra a sinistra o dal basso verso l'alto.
     * 
     * @param x Coordinata X del punto da verificare
     * @param y Coordinata Y del punto da verificare
     * @return true se il punto è all'interno del rettangolo, false altrimenti
     */
    public boolean contains(int x, int y) {
        // Normalizza le coordinate del rettangolo
        // (gestisce il caso in cui il rettangolo venga tracciato in qualsiasi direzione)
        int px = Math.min(getRx(), getRx2());  // X più a sinistra
        int py = Math.min(getRy(), getRy2());  // Y più in alto
        int pw = Math.abs(getRx() - getRx2()); // Larghezza
        int ph = Math.abs(getRy() - getRy2()); // Altezza
        
        // Verifica se il punto è all'interno del rettangolo normalizzato
        return x >= px && x <= px + pw && y >= py && y <= py + ph;
    }
}
