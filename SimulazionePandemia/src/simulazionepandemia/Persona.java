/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package simulazionepandemia;

import java.awt.event.ActionEvent;
import javax.swing.Timer;

/**
 * Classe che rappresenta una singola persona nella simulazione della pandemia.
 * Ogni persona ha una posizione, velocità, direzione di movimento e uno stato di salute.
 * 
 * La classe gestisce in modo thread-safe:
 * - Il movimento della persona nello spazio della simulazione
 * - Il timer per la durata dell'infezione
 * - Il cambio di stato (suscettibile, infetto, asintomatico, immune, morto)
 * - La gestione della pausa/ripresa della simulazione
 * 
 * @author Andrea
 */
class Persona {

    // ============================================
    // COSTANTI
    // ============================================
    
    /**
     * Durata massima dell'infezione in millisecondi (10 secondi).
     * Dopo questo periodo, una persona infetta può guarire, morire o diventare immune.
     */
    private static final int MAXVITA = 10000;
    
    // ============================================
    // ATTRIBUTI DI POSIZIONE E MOVIMENTO
    // ============================================
    
    /**
     * Coordinata X della persona (sincronizzata per accesso thread-safe)
     */
    private volatile int x;
    
    /**
     * Coordinata Y della persona (sincronizzata per accesso thread-safe)
     */
    private volatile int y;
    
    /**
     * Velocità di movimento della persona (pixel per frame)
     */
    private volatile int velocità;
    
    /**
     * Direzione di movimento sull'asse X (-1, 0, +1)
     */
    private volatile int direzioneX;
    
    /**
     * Direzione di movimento sull'asse Y (-1, 0, +1)
     */
    private volatile int direzioneY;
    
    // ============================================
    // ATTRIBUTI DI STATO E SALUTE
    // ============================================
    
    /**
     * Stato attuale di salute della persona (Suscettibile, Infetto, Immune, ecc.)
     * Volatile per garantire visibilità tra thread
     */
    private volatile State stato;
    
    /**
     * Timer che gestisce la durata dell'infezione.
     * Quando scade, la persona può guarire, morire o diventare immune.
     */
    private Timer timer;
    
    /**
     * Flag che indica se la persona è bloccata in quarantena.
     * Volatile per garantire visibilità tra thread.
     */
    private volatile boolean bloccato;
    
    // ============================================
    // ATTRIBUTI PER LA GESTIONE DEL TIMER
    // ============================================
    
    /**
     * Timestamp in cui la simulazione è stata messa in pausa
     */
    private long pauseStartTime;
    
    /**
     * Tempo già trascorso dall'inizio dell'infezione al momento della pausa
     */
    private long elapsedTimeAtPause;
    
    /**
     * Timestamp in cui è iniziata l'infezione
     */
    private long startTime;

    /**
     * Costruttore della classe Persona.
     * 
     * @param x Coordinata X iniziale
     * @param y Coordinata Y iniziale
     * @param velocità Velocità di movimento (1-5 pixel per frame)
     * @param direzioneX Direzione iniziale sull'asse X (-1, 0, +1)
     * @param direzioneY Direzione iniziale sull'asse Y (-1, 0, +1)
     * @param stato Stato di salute iniziale
     */
    Persona(int x, int y, int velocità, int direzioneX, int direzioneY, State stato) {
        this.x = x;
        this.y = y;
        this.velocità = velocità;
        this.direzioneX = direzioneX;
        this.direzioneY = direzioneY;
        this.stato = stato;
        this.bloccato = false;
    }

    // ============================================
    // METODI GETTER (Thread-safe grazie a volatile)
    // ============================================
    
    /**
     * Restituisce la coordinata X corrente della persona.
     * @return Posizione X
     */
    public int getX() {
        return x;
    }

    /**
     * Restituisce la coordinata Y corrente della persona.
     * @return Posizione Y
     */
    public int getY() {
        return y;
    }

    /**
     * Restituisce la direzione di movimento sull'asse X.
     * @return -1 (sinistra), 0 (fermo), +1 (destra)
     */
    public int getDirezioneX() {
        return direzioneX;
    }

    /**
     * Restituisce la direzione di movimento sull'asse Y.
     * @return -1 (alto), 0 (fermo), +1 (basso)
     */
    public int getDirezioneY() {
        return direzioneY;
    }

    /**
     * Restituisce il timer associato all'infezione.
     * @return Timer dell'infezione (può essere null se non infetto)
     */
    public Timer getTimer() {
        return timer;
    }

    /**
     * Restituisce il timestamp di inizio dell'infezione.
     * @return Tempo di inizio in millisecondi
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Restituisce lo stato di salute corrente della persona.
     * @return Stato corrente (Suscettibile, Infetto, ecc.)
     */
    public State getStato() {
        return this.stato;
    }

    /**
     * Restituisce la velocità di movimento della persona.
     * @return Velocità in pixel per frame
     */
    public int getVelocità() {
        return velocità;
    }

    /**
     * Verifica se la persona è bloccata in quarantena.
     * @return true se bloccata, false altrimenti
     */
    public boolean isBloccato() {
        return bloccato;
    }

    // ============================================
    // METODI SETTER (Thread-safe con synchronized)
    // ============================================
    
    /**
     * Imposta lo stato di blocco (quarantena) della persona.
     * @param bloccato true per bloccare, false per sbloccare
     */
    public synchronized void setBloccato(boolean bloccato) {
        this.bloccato = bloccato;
    }

    /**
     * Modifica la velocità di movimento della persona.
     * @param velocità Nuova velocità (1-5 pixel per frame)
     */
    public synchronized void setVelocità(int velocità) {
        this.velocità = velocità;
    }

    /**
     * Cambia lo stato di salute della persona.
     * @param stato Nuovo stato di salute
     */
    public synchronized void setStato(State stato) {
        this.stato = stato;
    }

    /**
     * Modifica la coordinata X della persona.
     * @param x Nuova posizione X
     */
    public synchronized void setX(int x) {
        this.x = x;
    }

    /**
     * Modifica la coordinata Y della persona.
     * @param y Nuova posizione Y
     */
    public synchronized void setY(int y) {
        this.y = y;
    }

    /**
     * Modifica la direzione di movimento sull'asse X.
     * @param direzioneX Nuova direzione (-1, 0, +1)
     */
    public synchronized void setDirezioneX(int direzioneX) {
        this.direzioneX = direzioneX;
    }

    /**
     * Modifica la direzione di movimento sull'asse Y.
     * @param direzioneY Nuova direzione (-1, 0, +1)
     */
    public synchronized void setDirezioneY(int direzioneY) {
        this.direzioneY = direzioneY;
    }

    // ============================================
    // METODI PER LA GESTIONE DELL'INFEZIONE
    // ============================================
    
    /**
     * Avvia il timer dell'infezione per questa persona.
     * 
     * Questo metodo viene chiamato quando una persona suscettibile viene infettata.
     * Il timer scade dopo MAXVITA millisecondi (10 secondi), momento in cui 
     * la persona può guarire, morire o diventare immune in base alle probabilità configurate.
     * 
     * THREAD-SAFE: Sincronizzato per evitare condizioni di gara.
     */
    public synchronized void infetta() {
        // Registra il momento dell'infezione
        startTime = System.currentTimeMillis();
        
        // Crea un nuovo timer che si attiva dopo MAXVITA millisecondi
        timer = new Timer(MAXVITA, (ActionEvent e) -> {
            stopTimer();
        });
        
        // Il timer si ferma automaticamente dopo la prima esecuzione
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * Ferma il timer dell'infezione.
     * 
     * Questo metodo viene chiamato quando:
     * - Il timer scade naturalmente (dopo 10 secondi)
     * - La persona viene vaccinata
     * - La persona muore
     * 
     * THREAD-SAFE: Sincronizzato per evitare condizioni di gara.
     */
    public synchronized void stopTimer() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }

    /**
     * Mette in pausa il timer dell'infezione quando la simulazione viene fermata.
     * 
     * NOTA IMPORTANTE: Questo metodo ha richiesto 2 giorni di debugging!
     * Il problema era che il timer veniva inizializzato nel costruttore, causando
     * problemi con il Garbage Collector della JVM che deallocava il thread del timer
     * dopo ogni utilizzo. Spostando l'inizializzazione nel metodo infetta() il problema
     * è stato risolto.
     * 
     * Il metodo:
     * 1. Ferma il timer se in esecuzione
     * 2. Calcola quanto tempo è già trascorso dall'inizio dell'infezione
     * 3. Memorizza questi valori per poter riprendere correttamente
     * 
     * THREAD-SAFE: Sincronizzato per evitare condizioni di gara.
     */
    public synchronized void pauseTimer() {
        // Il timer può essere messo in pausa solo se:
        // - Esiste
        // - È in esecuzione
        // - La persona è infetta o asintomatica
        if (timer != null && timer.isRunning() && 
            (stato == State.Infetto || stato == State.Asintomatico)) {
            
            // Ferma il timer
            timer.stop();
            
            // Registra il momento della pausa
            pauseStartTime = System.currentTimeMillis();
            
            // Calcola quanto tempo è già trascorso dall'inizio dell'infezione
            elapsedTimeAtPause = pauseStartTime - startTime;
        }
    }

    /**
     * Riprende il timer dell'infezione quando la simulazione riprende.
     * 
     * Il metodo:
     * 1. Calcola quanto tempo manca alla fine dell'infezione
     * 2. Riconfigura il timer con il tempo rimanente
     * 3. Aggiorna il timestamp di inizio considerando il tempo già trascorso
     * 4. Riavvia il timer
     * 
     * Se il tempo rimanente è ≤ 0, il timer viene fermato definitivamente.
     * 
     * THREAD-SAFE: Sincronizzato per evitare condizioni di gara.
     */
    public synchronized void resumeTimer() {
        // Il timer può essere ripreso solo se:
        // - Esiste
        // - NON è già in esecuzione
        // - La persona è ancora infetta o asintomatica
        if (timer != null && !timer.isRunning() && 
            (stato == State.Infetto || stato == State.Asintomatico)) {
            
            // Calcola quanto tempo manca alla fine dell'infezione
            long remainingTime = MAXVITA - elapsedTimeAtPause;
            
            if (remainingTime > 0) {
                // Aggiorna il timestamp di inizio considerando il tempo già trascorso
                // Questo mantiene coerente il calcolo del tempo totale
                startTime = System.currentTimeMillis() - elapsedTimeAtPause;
                
                // Riconfigura il timer con il tempo rimanente
                timer.setInitialDelay((int) remainingTime);
                
                // Riavvia il timer
                timer.start();
            } else {
                // Se il tempo è scaduto durante la pausa, ferma il timer
                stopTimer();
            }
        }
    }
}
