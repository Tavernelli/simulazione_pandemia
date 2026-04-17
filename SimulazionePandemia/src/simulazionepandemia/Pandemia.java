/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package simulazionepandemia;

import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.List;
import javax.swing.*;

/**
 * Pannello principale della simulazione pandemica.
 * 
 * Questa classe gestisce:
 * - Il rendering grafico di tutte le persone
 * - La logica di movimento e collisione
 * - La propagazione dell'infezione
 * - Il ciclo di vita delle persone (infezione → guarigione/morte/immunità)
 * - La gestione multi-thread per prestazioni ottimali
 * 
 * OTTIMIZZAZIONI IMPLEMENTATE:
 * - ExecutorService per calcoli paralleli delle collisioni
 * - Sincronizzazione thread-safe della lista di persone
 * - Double buffering automatico di JPanel per rendering fluido
 * - Volatile e synchronized per accesso concorrente sicuro
 * 
 * @author Andrea
 */
public class Pandemia extends JPanel implements ActionListener {

    // ============================================
    // COSTANTI DI SIMULAZIONE
    // ============================================
    
    /**
     * Diametro di visualizzazione di ogni persona in pixel
     */
    private static final int DIAMETRO = 12;
    
    /**
     * Distanza massima (in pixel) per il contagio tra due persone
     */
    private static final int DISTANZA_INFEZIONE = 20;
    
    /**
     * Dimensione della cella per il spatial hashing (uguale alla distanza di infezione)
     */
    private static final int CELL_SIZE = DISTANZA_INFEZIONE;
    
    /**
     * Velocità massima di movimento delle persone (pixel per frame)
     */
    private static final int VMAX = 5;
    
    /**
     * Intervallo di aggiornamento della scena in millisecondi
     */
    private static final int SCENE_DELAY = 100;
    
    /**
     * Intervallo di aggiornamento del cerchio di infettività in millisecondi
     */
    private static final int CIRCLE_DELAY = 50;
    
    /**
     * Dimensione massima del cerchio che indica il raggio di infezione
     */
    private static final int MAX_DIMENSIONE = 30;

    // ============================================
    // GESTIONE MULTI-THREADING
    // ============================================
    
    /**
     * Pool di thread per calcoli paralleli delle collisioni.
     * Utilizza un numero di thread pari ai core disponibili per prestazioni ottimali.
     */
    private final ExecutorService executorService;
    
    /**
     * Esecutore per l'aggiornamento della simulazione off-EDT
     */
    private ScheduledExecutorService updateExecutor;
    
    /**
     * Lista thread-safe di tutte le persone nella simulazione.
     * Sincronizzata per permettere accesso concorrente sicuro da più thread.
     */
    private static final List<Persona> persone = new CopyOnWriteArrayList<>();

    // ============================================
    // TIMER E CONTROLLO SIMULAZIONE
    // ============================================
    
    /**
     * Timer principale che controlla il loop della simulazione
     */
    private final Timer loop;
    
    /**
     * Timer per l'animazione del cerchio di infettività
     */
    private Timer loopCircleTimer;
    
    /**
     * Timer per la gestione del blocco del rettangolo di quarantena
     */
    private Timer timerBloccoRettangolo;
    
    /**
     * Flag che indica se la simulazione è in esecuzione
     */
    private volatile boolean inizio = false;

    // ============================================
    // DIMENSIONI E RENDERING
    // ============================================
    
    /**
     * Larghezza del pannello di simulazione in pixel
     */
    private final int boardWidth;
    
    /**
     * Altezza del pannello di simulazione in pixel
     */
    private final int boardHeight;
    
    /**
     * Raggio corrente del cerchio di infettività (animato)
     */
    private volatile int radius = 0;
    
    /**
     * Ritardo (ms) tra gli aggiornamenti della simulazione; controllato dallo slider
     */
    private volatile int updateDelayMillis = 16;

    // ============================================
    // PARAMETRI EPIDEMIOLOGICI
    // ============================================
    
    /**
     * Numero totale di persone all'inizio della simulazione
     */
    public static int PopolazioneIniziale;
    
    /**
     * Numero di persone infette all'inizio della simulazione
     */
    public static int PopolazioneInfettaInziale;
    
    /**
     * Probabilità di contagio durante un contatto (valore assoluto calcolato dalla percentuale)
     */
    private double Infettivita;
    
    /**
     * Probabilità che un infetto sia asintomatico (valore assoluto calcolato dalla percentuale)
     */
    private double Asintomatici;
    
    /**
     * Probabilità che un infetto muoia (valore assoluto calcolato dalla percentuale)
     */
    private double MortiInfetti;
    
    /**
     * Probabilità che un guarito diventi immune (valore assoluto calcolato dalla percentuale)
     */
    private double immuni;

    // ============================================
    // GESTIONE INTERAZIONE UTENTE
    // ============================================
    
    /**
     * Gestore del rettangolo di selezione con il mouse (per vaccino/quarantena)
     */
    RettangoloMouse rettangoloMouse;
    
    /**
     * Flag che indica se il rettangolo di selezione è attualmente bloccato
     */
    private boolean bloccoRettangolo = false;
    
    /**
     * Generatore di numeri casuali thread-safe per la simulazione stocastica
     */
    private final Random random;

    /**
     * Costruttore del pannello di simulazione.
     * 
     * Inizializza:
     * - Le dimensioni del pannello (600x600 pixel)
     * - Il pool di thread per calcoli paralleli
     * - I timer per la simulazione e l'animazione
     * - Il gestore del mouse per vaccino/quarantena
     * - La lista delle persone (inizialmente vuota)
     */
    public Pandemia() {
        initComponents();
        
        // Imposta le dimensioni del pannello
        this.boardWidth = 600;
        this.boardHeight = this.boardWidth;
        setPreferredSize(new Dimension(this.boardWidth, this.boardHeight));

        // Inizializza il generatore di numeri casuali
        random = new Random();
        
        // Crea il pool di thread per calcoli paralleli
        // Usa un numero di thread pari ai processori disponibili per prestazioni ottimali
        int numThreads = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(numThreads);

        // Inizializza il timer principale della simulazione
        loop = new Timer(SCENE_DELAY, this);

        // Inizializza la gestione del rettangolo di selezione con il mouse
        rettangoloMouse = new RettangoloMouse(0, 0, 0, 0, false);
        addMouseListener(rettangoloMouse);
        addMouseMotionListener(rettangoloMouse);

        // Inizializza il timer per l'animazione del cerchio di infettività
        // Usa una lambda function per aggiornare il raggio del cerchio
        loopCircleTimer = new Timer(CIRCLE_DELAY, (ActionEvent e) -> {
            if (inizio) {
                synchronized (this) {
                    // Incrementa il raggio e lo riporta a 0 quando raggiunge il massimo
                    radius = (radius + 1) % MAX_DIMENSIONE;
                    repaint();
                }
            } else {
                radius = 0;
                loopCircleTimer.stop();
            }
        });
        loopCircleTimer.start();
    }
    
    /**
     * Restituisce la lista di tutte le persone nella simulazione.
     * Metodo thread-safe grazie all'uso di CopyOnWriteArrayList.
     * 
     * @return Lista delle persone
     */
    public static List<Persona> getPersone() {
        return persone;
    }
    
    /**
     * Metodo chiamato quando l'applicazione viene chiusa.
     * Libera le risorse del pool di thread per evitare memory leak.
     */
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                // Attende fino a 5 secondi che i thread completino le operazioni
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (updateExecutor != null && !updateExecutor.isShutdown()) {
            updateExecutor.shutdownNow();
        }
    }

    // ============================================
    // METODI DI CONTROLLO SIMULAZIONE
    // ============================================
    
    /**
     * Avvia o ferma la simulazione.
     * 
     * Quando si avvia:
     * - Attiva i timer principali (loop e animazione cerchio)
     * - Riprende tutti i timer di infezione delle persone
     * 
     * Quando si ferma:
     * - Ferma tutti i timer
     * - Mette in pausa i timer di infezione
     * 
     * @param start true per avviare, false per fermare
     */
    public void setAppState(boolean start) {
        inizio = start;
        if (start) {
            // Avvia l'aggiornamento off-EDT per ridurre gli scatti
            if (updateExecutor == null || updateExecutor.isShutdown()) {
                updateExecutor = Executors.newSingleThreadScheduledExecutor();
            }
            updateExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (!inizio) return;
                    move();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            repaint();
                        }
                    });
                }
            }, 0, updateDelayMillis, TimeUnit.MILLISECONDS);
            // Mantieni solo il timer del cerchio per l'animazione visiva
            loopCircleTimer.start();
            // Riprende tutti i timer di infezione
            persone.forEach(Persona::resumeTimer);
        } else {
            loop.stop();
            loopCircleTimer.stop();
            if (updateExecutor != null && !updateExecutor.isShutdown()) {
                updateExecutor.shutdownNow();
            }
            // Mette in pausa tutti i timer di infezione
            persone.forEach(Persona::pauseTimer);
        }
    }

    /**
     * Aggiorna il ritardo tra gli aggiornamenti della simulazione (ms).
     * Se la simulazione è in esecuzione, riavvia lo scheduler con il nuovo valore.
     */
    public void setUpdateDelayMillis(int delayMs) {
        if (delayMs < 5) delayMs = 5;
        if (delayMs > 200) delayMs = 200;
        this.updateDelayMillis = delayMs;
        if (inizio) {
            // Riavvia lo scheduler con il nuovo periodo
            if (updateExecutor != null && !updateExecutor.isShutdown()) {
                updateExecutor.shutdownNow();
            }
            updateExecutor = Executors.newSingleThreadScheduledExecutor();
            final int period = this.updateDelayMillis;
            updateExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (!inizio) return;
                    move();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            repaint();
                        }
                    });
                }
            }, 0, period, TimeUnit.MILLISECONDS);
        }
    }

    public int getUpdateDelayMillis() {
        return updateDelayMillis;
    }

    /**
     * Attiva il listener del mouse per permettere la selezione di un'area
     * (per vaccino o quarantena).
     */
    public void setListener() {
        rettangoloMouse = new RettangoloMouse(0, 0, 0, 0, true);
        addMouseListener(rettangoloMouse);
        addMouseMotionListener(rettangoloMouse);
    }

    /**
     * Disattiva il listener del mouse.
     */
    public void RemoveListener() {
        rettangoloMouse.setMouseListenerIsActive(false);
        removeMouseListener(rettangoloMouse);
    }

    /**
     * Inizializza la popolazione per la simulazione.
     * 
     * Crea:
     * - Persone suscettibili (sane ma vulnerabili)
     * - Persone già infette all'inizio
     * 
     * Thread-safe: usa CopyOnWriteArrayList per accesso concorrente sicuro.
     * 
     * @param popolazioneIniziale Numero totale di persone
     * @param popolazioneInfettaInziale Numero di persone inizialmente infette
     */
    public void SetPopolazioneIniziale(int popolazioneIniziale, int popolazioneInfettaInziale) {
        persone.clear();
        Pandemia.PopolazioneIniziale = popolazioneIniziale;
        Pandemia.PopolazioneInfettaInziale = popolazioneInfettaInziale;

        // Crea le persone suscettibili (sane)
        for (int i = 0; i < Pandemia.PopolazioneIniziale - Pandemia.PopolazioneInfettaInziale; i++) {
            persone.add(Inizializza(State.Suscettibile));
        }

        // Crea le persone già infette e avvia il loro timer
        for (int i = 0; i < Pandemia.PopolazioneInfettaInziale; i++) {
            Persona p = Inizializza(State.Infetto);
            persone.add(p);
            p.infetta();
        }
    }

    // ============================================
    // METODI DI CONFIGURAZIONE PARAMETRI EPIDEMIOLOGICI
    // ============================================
    
    /**
     * Configura la probabilità di contagio.
     * 
     * @param infettivita Percentuale di contagio (0-100)
     * @param popolazione Numero totale di persone
     */
    public void SetInfettività(double infettivita, double popolazione) {
        // La probabilità è espressa come frazione di 1.
        Infettivita = infettivita / 100.0;
    }

    /**
     * Configura la percentuale di asintomatici tra gli infetti.
     * 
     * @param asintomatici Percentuale di asintomatici (0-100)
     * @param infettivita Parametro non utilizzato nella logica attuale
     */
    public void SetAsintomatici(double asintomatici, double infettivita) {
        Asintomatici = asintomatici / 100.0;
    }

    /**
     * Configura la percentuale di mortalità tra gli infetti.
     * 
     * @param mortiInfetti Percentuale di mortalità (0-100)
     * @param infettivita Parametro non utilizzato nella logica attuale
     */
    public void SetmortiInfetti(double mortiInfetti, double infettivita) {
        MortiInfetti = mortiInfetti / 100.0;
    }

    /**
     * Configura la percentuale di guariti che diventano immuni.
     * 
     * @param immunità Percentuale di immunità (0-100)
     * @param infettivita Parametro non utilizzato nella logica attuale
     */
    public void SetGuaritiImmuni(double immunità, double infettivita) {
        immuni = immunità / 100.0;
    }

    /**
     * Inizializza una nuova persona con posizione e direzione casuali.
     * 
     * La persona viene posizionata:
     * - In una posizione X,Y casuale all'interno del pannello
     * - Con una velocità casuale tra 1 e VMAX (1-5)
     * - Con una direzione di movimento casuale (-1, 0, +1) per X e Y
     * - Assicurando che non sia completamente ferma (almeno un asse deve muoversi)
     * 
     * @param stato Stato di salute iniziale della persona
     * @return Nuova persona inizializzata
     */
    public Persona Inizializza(State stato) {
        // Posizione casuale all'interno del pannello
        int x = random.nextInt(boardWidth - (DIAMETRO / 2));
        int y = random.nextInt(boardHeight - (DIAMETRO / 2));
        
        // Velocità casuale tra 1 e 5
        int v = random.nextInt(VMAX) + 1;
        
        // Direzione X casuale (-1, 0, +1)
        int dX = random.nextInt(3) - 1;
        
        // Direzione Y: se dX è 0, deve essere per forza -1 o +1
        // altrimenti la persona sarebbe ferma
        int dY = (dX == 0) ? (random.nextBoolean() ? 1 : -1) : (random.nextInt(3) - 1);

        return new Persona(x, y, v, dX, dY, stato);
    }

    // ============================================
    // METODI DI RENDERING GRAFICO
    // ============================================
    
    /**
     * Override del metodo paintComponent per il rendering custom.
     * Viene chiamato automaticamente da Swing quando il pannello deve essere ridisegnato.
     * 
     * @param g Contesto grafico
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (inizio) {
            draw(g);
        }
    }

    /**
     * Disegna tutte le persone e le visualizzazioni della simulazione.
     * 
     * Rendering differenziato per stato:
     * - Suscettibile: cerchio grigio
     * - Infetto/Asintomatico: quadrato con cerchio di infezione animato
     * - Morto: croce rossa
     * - Immune/Vaccinato: cerchio giallo
     * 
     * Le persone in quarantena hanno un bordo blu.
     * 
     * @param g Contesto grafico
     */
    public void draw(Graphics g) {
        // Disegna ogni persona con visualizzazione specifica per il suo stato
        for (Persona p : persone) {
            State state = p.getStato();
            Color color = state.getColor();
            int stateValue = state.getValue();
            
            // Switch compatibile con Java 8
            if (stateValue == 6 || stateValue == 4) { // Infetto (6) o Infettivo (4)
                // Disegna quadrato 3D per persona infettiva
                g.setColor(color);
                g.fill3DRect(p.getX() - (DIAMETRO / 2), p.getY() - (DIAMETRO / 2), 
                             DIAMETRO, DIAMETRO, true);
                
                // Bordo blu se in quarantena, nero altrimenti
                if (p.isBloccato() && stateValue != 0) {
                    g.setColor(Color.BLUE);
                } else {
                    g.setColor(Color.BLACK);
                }
                g.drawRect(p.getX() - (DIAMETRO / 2), p.getY() - (DIAMETRO / 2), 
                           DIAMETRO, DIAMETRO);
                
                // Disegna il cerchio di infezione animato
                int diameter = 2 * radius;
                g.setColor(Color.RED);
                g.drawOval(p.getX() - radius, p.getY() - radius, diameter, diameter);
                
            } else if (stateValue == 2) { // Sintomatico/Morto
                // Disegna una croce per rappresentare una persona morta
                g.setColor(color);
                int l = DIAMETRO / 2;
                g.drawLine(p.getX() - l, p.getY() - l, p.getX() + l, p.getY() + l);
                g.drawLine(p.getX() - l, p.getY() + l, p.getX() + l, p.getY() - l);
                
            } else if (stateValue == 0) { // None/Vaccinato
                // Disegna cerchio pieno per persona vaccinata
                g.setColor(color);
                g.fillOval(p.getX() - (DIAMETRO / 2), p.getY() - (DIAMETRO / 2), 
                           DIAMETRO, DIAMETRO);
                g.setColor(Color.BLACK);
                g.drawOval(p.getX() - (DIAMETRO / 2), p.getY() - (DIAMETRO / 2), 
                           DIAMETRO, DIAMETRO);
                
            } else { // Suscettibile e altri stati (default)
                // Disegna cerchio pieno colorato
                g.setColor(color);
                g.fillOval(p.getX() - (DIAMETRO / 2), p.getY() - (DIAMETRO / 2), 
                           DIAMETRO, DIAMETRO);
                
                // Bordo blu se in quarantena, nero altrimenti
                if (p.isBloccato()) {
                    g.setColor(Color.BLUE);
                } else {
                    g.setColor(Color.BLACK);
                }
                g.drawOval(p.getX() - (DIAMETRO / 2), p.getY() - (DIAMETRO / 2), 
                           DIAMETRO, DIAMETRO);
            }
        }
        
        // Disegna il rettangolo di selezione (vaccino/quarantena) se attivo
        if (rettangoloMouse.isMouseListenerIsActive()) {
            // Rosso semi-trasparente per vaccino, blu per quarantena
            Color c = Pannello_di_controllo.getCheckBoxState() == 1 
                ? new Color(1f, 0f, 0f, .4f)
                : new Color(0f, 0f, 1f, .4f);
            g.setColor(c);
            
            // Calcola le coordinate normalizzate del rettangolo
            int px = Math.min(rettangoloMouse.getRx(), rettangoloMouse.getRx2());
            int py = Math.min(rettangoloMouse.getRy(), rettangoloMouse.getRy2());
            int pw = Math.abs(rettangoloMouse.getRx() - rettangoloMouse.getRx2());
            int ph = Math.abs(rettangoloMouse.getRy() - rettangoloMouse.getRy2());
            g.drawRect(px, py, pw, ph);
        }
    }

    // ============================================
    // METODI DI MOVIMENTO E FISICA DELLA SIMULAZIONE
    // ============================================
    
    /**
     * Metodo principale che aggiorna lo stato della simulazione.
     * 
     * Questo metodo esegue:
     * 1. Controllo del ciclo vitale degli infetti (morte/guarigione/immunità)
     * 2. Movimento delle persone
     * 3. Gestione delle collisioni con i bordi
     * 4. Rilevamento e gestione delle collisioni tra persone (contagio)
     * 5. Gestione della quarantena
     * 
     * OTTIMIZZAZIONE: Il metodo elabora tutte le persone in sequenza ma usa
     * operazioni sincronizzate per garantire thread-safety.
     */
    public void move() {
        // Itera su tutte le persone nella simulazione
        for (Persona p1 : persone) {
            // ========================================
            // 1. GESTIONE CICLO VITALE DELL'INFEZIONE
            // ========================================
            
            // Controlla se il timer dell'infezione è scaduto
            // Se sì, la persona può morire, guarire o diventare immune
            double probabilitàMorteImmunizzazione = random.nextDouble();

            if (p1.getTimer() != null && !p1.getTimer().isRunning()) {
                // Timer scaduto: determina l'esito dell'infezione
                
                if (p1.getStato() == State.Infetto && 
                    probabilitàMorteImmunizzazione < MortiInfetti / 100.0) {
                    // La persona muore
                    cambiaStato(p1, State.Morto);
                    
                } else if ((p1.getStato() == State.Infetto || p1.getStato() == State.Asintomatico)
                        && probabilitàMorteImmunizzazione < immuni / 100) {
                    // La persona guarisce e diventa immune
                    cambiaStato(p1, State.Vaccinato);
                    
                } else if (p1.getStato() == State.Infetto || p1.getStato() == State.Asintomatico) {
                    // La persona guarisce ma torna suscettibile
                    cambiaStato(p1, State.Suscettibile);
                }
            }

            // ========================================
            // 2. GESTIONE MOVIMENTO
            // ========================================
            
            // Le persone in quarantena non si muovono
            if (p1.isBloccato()) {
                continue;
            }

            int previousDirectionX = p1.getDirezioneX();
            int previousDirectionY = p1.getDirezioneY();

            if (p1.getStato() != State.Morto) {
                p1.setX(p1.getX() + p1.getDirezioneX() * p1.getVelocità());
                p1.setY(p1.getY() + p1.getDirezioneY() * p1.getVelocità());
            }

            // Controllo se la persona è uscita dai bordi
            if (p1.getX() < 0 || p1.getX() > boardWidth) {
                // Inverte la direzione sull'asse X e corregge la posizione
                p1.setDirezioneX(p1.getDirezioneX() * -1);
                p1.setX(Math.max(0, Math.min(boardWidth, p1.getX())));
                if (previousDirectionY == p1.getDirezioneY()) {
                    p1.setDirezioneY(getRandomDirection(previousDirectionY));
                }
            }

            if (p1.getY() < 0 || p1.getY() > boardHeight) {
                // Inverte la direzione sull'asse Y e corregge la posizione
                p1.setDirezioneY(p1.getDirezioneY() * -1);
                p1.setY(Math.max(0, Math.min(boardHeight, p1.getY())));
                if (previousDirectionX == p1.getDirezioneX()) {
                    p1.setDirezioneX(getRandomDirection(previousDirectionX));
                }
            }

            // Gestione delle collisioni rinviata a dopo il movimento (spatial hashing)

            // check bordi rettangolo
            if (rettangoloMouse.isMouseListenerIsActive() && rettangoloMouse.contains(p1.getX(), p1.getY())
                    && p1.getStato() != State.Vaccinato) {
                // Imposta il blocco per tutte le persone nel rettangolo
                if (Pannello_di_controllo.getCheckBoxState() == -1) {
                    if (!bloccoRettangolo) {

                        bloccoRettangolo = true;

                        int tempoBlocco = Pannello_di_controllo.getTempoQuarantena();
                        timerBloccoRettangolo = new Timer(tempoBlocco, (e) -> {

                            bloccoRettangolo = false;

                            // Sblocca tutte le persone nel rettangolo
                            for (Persona persona : persone) {

                                persona.setBloccato(false);

                            }
                            timerBloccoRettangolo.stop();
                            // Resetta il rettangolo
                            rettangoloMouse.setStartPoint(0, 0);
                            rettangoloMouse.setEndPoint(0, 0);
                        });
                        timerBloccoRettangolo.start();
                    }
                    // Blocca la persona corrente
                    p1.setBloccato(true);
                }

            }

        }

        // Secondo passaggio: Spatial hashing per collisioni/contagio (riduce O(n^2))
        int nPersone = persone.size();
        if (nPersone > 1) {
            final int cellRange = 1; // controlla cella corrente e adiacenti
            final int maxDist2 = DISTANZA_INFEZIONE * DISTANZA_INFEZIONE;

            // Griglia: chiave -> lista indici persone nella cella
            Map<Integer, java.util.ArrayList<Integer>> grid = new HashMap<Integer, java.util.ArrayList<Integer>>();
            for (int i = 0; i < nPersone; i++) {
                Persona p = persone.get(i);
                int cx = p.getX() / CELL_SIZE;
                int cy = p.getY() / CELL_SIZE;
                int key = cellKey(cx, cy);
                java.util.ArrayList<Integer> list = grid.get(key);
                if (list == null) {
                    list = new java.util.ArrayList<Integer>();
                    grid.put(key, list);
                }
                list.add(Integer.valueOf(i));
            }

            for (int i = 0; i < nPersone; i++) {
                Persona p1 = persone.get(i);
                int cx = p1.getX() / CELL_SIZE;
                int cy = p1.getY() / CELL_SIZE;

                for (int gx = cx - cellRange; gx <= cx + cellRange; gx++) {
                    for (int gy = cy - cellRange; gy <= cy + cellRange; gy++) {
                        java.util.ArrayList<Integer> bucket = grid.get(cellKey(gx, gy));
                        if (bucket == null) continue;
                        for (int b = 0; b < bucket.size(); b++) {
                            int j = bucket.get(b).intValue();
                            if (j <= i) continue; // evita doppioni
                            Persona p2 = persone.get(j);

                            int dx = p2.getX() - p1.getX();
                            int dy = p2.getY() - p1.getY();
                            int dist2 = dx * dx + dy * dy;
                            if (dist2 <= maxDist2) {
                                double probabilità = random.nextDouble();
                                double infettivitàPercentuale = Infettivita / 100.0;
                                double asintomaticiPercentuale = Asintomatici / 100.0;

                                if ((p1.getStato() == State.Infetto || p1.getStato() == State.Asintomatico)
                                        && p2.getStato() == State.Suscettibile) {
                                    if (probabilità < infettivitàPercentuale) {
                                        if (probabilità < asintomaticiPercentuale) {
                                            cambiaStato(p2, State.Asintomatico);
                                        } else {
                                            cambiaStato(p2, State.Infetto);
                                        }
                                        p2.infetta();
                                    }
                                } else if ((p2.getStato() == State.Infetto || p2.getStato() == State.Asintomatico)
                                        && p1.getStato() == State.Suscettibile) {
                                    if (probabilità < infettivitàPercentuale) {
                                        if (probabilità < asintomaticiPercentuale) {
                                            cambiaStato(p1, State.Asintomatico);
                                        } else {
                                            cambiaStato(p1, State.Infetto);
                                        }
                                        p1.infetta();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // Metodo per ottenere una direzione diversa da quella precedente

    private int getRandomDirection(int previousDirection) {
        int direction;
        do {
            direction = random.nextInt(3) - 1; // Genera -1, 0 o 1 casualmente
        } while (direction == previousDirection);
        return direction;
    }

    // Metodo per verificare la collisione tra due persone
    public boolean collisione(Persona p1, Persona p2) {
        // Versione ottimizzata senza calcolo della radice quadrata
        int dx = p2.getX() - p1.getX();
        int dy = p2.getY() - p1.getY();
        return (dx * dx + dy * dy) <= (DISTANZA_INFEZIONE * DISTANZA_INFEZIONE);
    }

    private int cellKey(int cx, int cy) {
        // Chiave semplice per la griglia (i valori di cx,cy sono piccoli e non negativi)
        return cx * 10000 + cy;
    }

    // Metodo per cambiare lo stato di una persona
    public void cambiaStato(Persona persona, State stato) {
        State statoPrecedente = persona.getStato();
        persona.setStato(stato);

        if (stato == State.Morto) {
            PopolazioneIniziale--;
        }

        if (statoPrecedente == State.Infetto || statoPrecedente == State.Asintomatico) {
            PopolazioneInfettaInziale--;
        }
        if (stato == State.Infetto || stato == State.Asintomatico) {
            PopolazioneInfettaInziale++;
        }

        int[] attuali = calcolaNumeroInfettiImmuniAttuali();
        int infettiAttuali = attuali[0];
        int immuniAttuali = attuali[1];

        SetInfettività(Pannello_di_controllo.getNuoviInfetti(), PopolazioneIniziale);
        SetAsintomatici(Pannello_di_controllo.getAsintomatici(), infettiAttuali);
        SetmortiInfetti(Pannello_di_controllo.getMortiInfetti(), infettiAttuali);
        SetGuaritiImmuni(Pannello_di_controllo.getImmuniGuariti(), infettiAttuali);
        Pannello_di_controllo.setUI(PopolazioneIniziale, PopolazioneInfettaInziale, immuniAttuali);
        if (PopolazioneIniziale == 0) {
            inizio = false;
        }
    }

    // Metodo per calcolare il numero attuale di infetti e immuni
    public static int[] calcolaNumeroInfettiImmuniAttuali() {
        int contaInfetti = 0;
        int contaImmuni = 0;
        for (Persona p : persone) {
            if (p.getStato() == State.Infetto || p.getStato() == State.Asintomatico) {
                contaInfetti++;
            }
            if (p.getStato() == State.Vaccinato) {
                contaImmuni++;
            }
        }
        return new int[] {contaInfetti, contaImmuni};
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    
    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE));
    }// </editor-fold>//GEN-END:initComponents

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint(); // aggiorno lo schermo
        if (!inizio) {
            loop.stop();
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
