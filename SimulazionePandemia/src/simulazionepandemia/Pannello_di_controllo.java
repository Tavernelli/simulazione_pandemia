package simulazionepandemia;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Pannello di controllo minimale (compatibile Java 8).
 */
public class Pannello_di_controllo extends JFrame {
        private static final DecimalFormat FMT = new DecimalFormat("0.####");
        private final Pandemia pandemia;
        private JFrame frameSim;
        private boolean running = false;
        private boolean firstStart = true;
        private static int infettiIniziali;
        private static int popolazioneIniziale;
        private static int asintomaticiPerc;
        private static int infettivitaPerc;
        private static int immuniPerc;
        private static int mortiPerc;
        private static int checkBoxState; // 0 nessuna, 1 vaccino, -1 quarantena
        private static int tempoQuarantenaMs;
        private JSpinner spPopolazione;
        private JSpinner spInfetti;
        private JSpinner spInfettivita;
        private JSpinner spAsintomatici;
        private JSpinner spMorti;
        private JSpinner spImmuni;
        private JSpinner spTempoQuarantena;
        private JCheckBox chkPercentualeInfetti;
        private JCheckBox chkMortiAttivi;
        private JRadioButton rbVuoto;
        private JRadioButton rbVaccino;
        private JRadioButton rbQuarantena;
        private static JLabel lblPopolazione;
        private static JLabel lblInfetti;
        private static JLabel lblImmuni;
        private static JLabel lblMediaInfetti;
        private JButton btnStart;
        public Pannello_di_controllo() {
                super("Pannello di controllo");
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                setResizable(false);
                pandemia = new Pandemia();
                buildUI();
                pack();
                setLocationRelativeTo(null);
                frameSim = new JFrame("Pandemia");
                frameSim.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frameSim.add(pandemia);
                frameSim.pack();
                frameSim.setLocation(getX() + getWidth() + 10, getY());
                frameSim.setVisible(true);
                showSpeedDialog();
        }
        private void buildUI() {
                JPanel root = new JPanel(new BorderLayout(8,8));
                setContentPane(root);
                JPanel params = new JPanel(new GridLayout(0,2,4,4));
                spPopolazione = spinner(100,1,10000,1);
                spInfetti = spinner(1,1,10000,1);
                spInfettivita = spinner(33,0,100,1);
                spAsintomatici = spinner(20,0,100,1);
                spMorti = spinner(10,0,100,1);
                spImmuni = spinner(20,0,100,1);
                spTempoQuarantena = spinner(10000,0,600000,500);
                chkPercentualeInfetti = new JCheckBox("Infetti iniziali in %", false);
                chkMortiAttivi = new JCheckBox("Morti attivi", true);
                params.add(new JLabel("Popolazione iniziale")); params.add(spPopolazione);
                params.add(new JLabel("Infetti iniziali")); params.add(spInfetti);
                params.add(chkPercentualeInfetti); params.add(new JLabel());
                params.add(new JLabel("Infettività (%)")); params.add(spInfettivita);
                params.add(new JLabel("Asintomatici / infetti (%)")); params.add(spAsintomatici);
                params.add(new JLabel("Morti per infetti (%)")); params.add(spMorti);
                params.add(chkMortiAttivi); params.add(new JLabel());
                params.add(new JLabel("Immuni sopravvissuti (%)")); params.add(spImmuni);
                params.add(new JLabel("Tempo quarantena (ms)")); params.add(spTempoQuarantena);
                add(params, BorderLayout.NORTH);
                JPanel mode = new JPanel(new GridLayout(1,3));
                ButtonGroup grp = new ButtonGroup();
                rbVuoto = new JRadioButton("Nessuna", true);
                rbVaccino = new JRadioButton("Vaccino");
                rbQuarantena = new JRadioButton("Quarantena");
                grp.add(rbVuoto); grp.add(rbVaccino); grp.add(rbQuarantena);
                ChangeListener cl = e -> updateModeState();
                rbVuoto.addChangeListener(cl); rbVaccino.addChangeListener(cl); rbQuarantena.addChangeListener(cl);
                mode.add(rbVuoto); mode.add(rbVaccino); mode.add(rbQuarantena);
                add(mode, BorderLayout.CENTER);
                JPanel stato = new JPanel(new GridLayout(2,2,4,4));
                lblPopolazione = new JLabel("Pop: 0");
                lblInfetti = new JLabel("Infetti: 0");
                lblImmuni = new JLabel("Immuni: 0");
                lblMediaInfetti = new JLabel("Media: 0");
                stato.add(lblPopolazione); stato.add(lblInfetti); stato.add(lblImmuni); stato.add(lblMediaInfetti);
                add(stato, BorderLayout.SOUTH);
                JPanel actions = new JPanel();
                btnStart = new JButton("Avvia");
                JButton btnHelp = new JButton("Help");
                JButton btnRestart = new JButton("Riavvia");
                JButton btnExit = new JButton("Chiudi");
                btnStart.addActionListener(e -> toggleStart());
                btnHelp.addActionListener(e -> showHelpDialog());
                btnRestart.addActionListener(e -> restartApp());
                btnExit.addActionListener(e -> exitApp());
                actions.add(btnStart); actions.add(btnHelp); actions.add(btnRestart); actions.add(btnExit);
                add(actions, BorderLayout.WEST);
        }
        private JSpinner spinner(int value,int min,int max,int step){ return new JSpinner(new SpinnerNumberModel(value,min,max,step)); }
        private void toggleStart() {
                if (!running) {
                        if (firstStart) { readInitialParameters(); firstStart = false; }
                        pandemia.setAppState(true);
                        btnStart.setText("Stop");
                        running = true;
                } else {
                        pandemia.setAppState(false);
                        btnStart.setText("Avvia");
                        running = false;
                }
        }
        private void readInitialParameters() {
                popolazioneIniziale = ((Integer)spPopolazione.getValue()).intValue();
                infettiIniziali = ((Integer)spInfetti.getValue()).intValue();
                infettivitaPerc = ((Integer)spInfettivita.getValue()).intValue();
                asintomaticiPerc = ((Integer)spAsintomatici.getValue()).intValue();
                mortiPerc = chkMortiAttivi.isSelected() ? ((Integer)spMorti.getValue()).intValue() : 0;
                immuniPerc = ((Integer)spImmuni.getValue()).intValue();
                tempoQuarantenaMs = ((Integer)spTempoQuarantena.getValue()).intValue();
                if (chkPercentualeInfetti.isSelected()) { infettiIniziali = Math.round(popolazioneIniziale * infettiIniziali / 100f); }
                pandemia.SetPopolazioneIniziale(popolazioneIniziale, infettiIniziali);
                pandemia.SetInfettività(infettivitaPerc, popolazioneIniziale);
                pandemia.SetAsintomatici(asintomaticiPerc, infettivitaPerc);
                pandemia.SetmortiInfetti(mortiPerc, infettivitaPerc);
                pandemia.SetGuaritiImmuni(immuniPerc, infettivitaPerc);
                lblPopolazione.setText("Pop: " + popolazioneIniziale);
                lblInfetti.setText("Infetti: " + infettiIniziali);
                lblMediaInfetti.setText("Media: 0");
        }
        private void updateModeState() {
                if (rbVuoto.isSelected()) { checkBoxState = 0; tempoQuarantenaMs = 0; pandemia.RemoveListener(); }
                else if (rbVaccino.isSelected()) { checkBoxState = 1; tempoQuarantenaMs = 0; pandemia.setListener(); }
                else if (rbQuarantena.isSelected()) { checkBoxState = -1; tempoQuarantenaMs = ((Integer)spTempoQuarantena.getValue()).intValue(); pandemia.setListener(); }
        }
        private void showSpeedDialog() {
                JDialog dlg = new JDialog(this, "Velocità", false);
                JPanel p = new JPanel(new BorderLayout(4,4));
                JLabel label = new JLabel("Ritardo aggiornamento (ms)");
                int cur = 16; try { cur = pandemia.getUpdateDelayMillis(); } catch (Throwable t) {}
                JSlider slider = new JSlider(5,150,cur);
                slider.setMajorTickSpacing(25); slider.setMinorTickSpacing(5); slider.setPaintTicks(true);
                JLabel val = new JLabel(String.valueOf(cur));
                slider.addChangeListener(new ChangeListener(){ public void stateChanged(ChangeEvent e){ int v=slider.getValue(); val.setText(String.valueOf(v)); pandemia.setUpdateDelayMillis(v);} });
                p.add(label,BorderLayout.NORTH); p.add(slider,BorderLayout.CENTER); p.add(val,BorderLayout.EAST);
                dlg.getContentPane().add(p); dlg.pack(); dlg.setLocationRelativeTo(this); dlg.setVisible(true);
        }
        private void showHelpDialog() {
                StringBuilder sb = new StringBuilder();
                sb.append("Guida rapida\n\n")
                  .append("Avvia/Stop: controlla simulazione.\n")
                  .append("Popolazione / Infetti iniziali (opzione %).\n")
                  .append("Infettività, Asintomatici, Morti, Immuni gestiscono transizioni.\n")
                  .append("Modalità area: Vaccino immunizza; Quarantena blocca per ms.\n")
                  .append("Velocità: slider ms tra update.\n")
                  .append("Colori: grigio suscettibile, rosso/arancio infetto/asintomatico, giallo immune, croce morto.\n");
                JOptionPane.showMessageDialog(this, sb.toString(), "Help", JOptionPane.INFORMATION_MESSAGE);
        }

        private void restartApp() {
                try { pandemia.setAppState(false); } catch (Throwable ignore) {}
                try { if (frameSim != null) frameSim.dispose(); } catch (Throwable ignore) {}
                try { dispose(); } catch (Throwable ignore) {}
                SwingUtilities.invokeLater(() -> {
                        Pannello_di_controllo p = new Pannello_di_controllo();
                        p.setVisible(true);
                });
        }

        private void exitApp() {
                try { pandemia.setAppState(false); } catch (Throwable ignore) {}
                try { if (frameSim != null) frameSim.dispose(); } catch (Throwable ignore) {}
                try { dispose(); } catch (Throwable ignore) {}
                System.exit(0);
        }
        public static void setUI(int popolazione, int infetti, double immuni) {
                if (lblPopolazione == null) return;
                final String popText = "Pop: " + popolazione;
                final String infText = "Infetti: " + infetti;
                final String immText = "Immuni: " + ((int)immuni);
                final String mediaText = "Media: " + FMT.format(infetti > 0 ? (double)(infetti - infettiIniziali)/infetti : 0.0);
                SwingUtilities.invokeLater(() -> {
                        lblPopolazione.setText(popText);
                        lblInfetti.setText(infText);
                        lblImmuni.setText(immText);
                        lblMediaInfetti.setText(mediaText);
                });
        }
public static int getTempoQuarantena() { return tempoQuarantenaMs; }
public static int getCheckBoxState() { return checkBoxState; }
public static int getAsintomatici() { return asintomaticiPerc; }
public static int getNuoviInfetti() { return infettivitaPerc; }
public static int getMortiInfetti() { return mortiPerc; }
public static int getImmuniGuariti() { return immuniPerc; }
}
