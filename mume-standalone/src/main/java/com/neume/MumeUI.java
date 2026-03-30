package com.neume;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.*;
import net.beadsproject.beads.analysis.featureextractors.PeakDetector;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;

import javax.swing.*;
import java.awt.*;

/**
 * MumeUI: The graphical interface for the standalone Mume port.
 * Includes interactive auditioning and visual audio metering.
 */
public class MumeUI extends JFrame {
    private AudioContext ac;
    private MumeAnalyzer analyzer;
    private Gain inputGain;
    private MumeVisualizer visualizer;
    private MumeScore mumeScore;
    
    // Auditioning Synth
    private WavePlayer auditionOsc;
    private Glide auditionFreq;
    private Envelope auditionEnv;
    private Gain auditionGain;
    
    // Metering
    private AudioLevelMeter outputMeter;
    private RMS rms;
    private javax.swing.Timer meterTimer;
    
    // UI Elements
    private final JTextArea statusArea;
    private final JButton btnStartStop;
    private final JButton btnCommit;
    private final JButton btnClear;
    private final JButton btnShowScore;
    private final JTextField txtMumeID;
    private final JSlider sliderRes;
    private final JSlider sliderAmpRes;
    
    public MumeUI() {
        super("Mume Standalone");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 800);

        // Initialize Score Window
        mumeScore = new MumeScore();
        mumeScore.setOnNoteSelected(this::playAuditionPitch);
        mumeScore.setVisible(false);

        // --- 1. Visualizer Panel ---
        visualizer = new MumeVisualizer(null);
        add(visualizer, BorderLayout.NORTH);

        // --- 2. Main Center Panel ---
        JPanel pnlCenter = new JPanel(new BorderLayout());
        
        // Metering on the side
        outputMeter = new AudioLevelMeter();
        pnlCenter.add(outputMeter, BorderLayout.EAST);

        // Curation Controls
        JPanel pnlCuration = new JPanel(new GridLayout(10, 1, 5, 2));
        pnlCuration.setBorder(BorderFactory.createTitledBorder("Mume Curation"));
        
        txtMumeID = new JTextField("beads-mume-01");
        pnlCuration.add(new JLabel("Current Mume ID:"));
        pnlCuration.add(txtMumeID);
        
        pnlCuration.add(new JLabel("Frequency Resolution (Hz):"));
        sliderRes = new JSlider(1, 100, 50);
        sliderRes.addChangeListener(e -> {
            float val = sliderRes.getValue() / 10.0f;
            if (analyzer != null) analyzer.setFrequencyResolution(val);
        });
        pnlCuration.add(sliderRes);

        pnlCuration.add(new JLabel("Amplitude Threshold:"));
        sliderAmpRes = new JSlider(0, 100, 10);
        sliderAmpRes.addChangeListener(e -> {
            float val = sliderAmpRes.getValue() / 1000.0f;
            if (analyzer != null) analyzer.setAmplitudeResolution(val);
        });
        pnlCuration.add(sliderAmpRes);
        
        btnCommit = new JButton("Commit Current Spectral State");
        btnCommit.setEnabled(false);
        btnCommit.addActionListener(e -> commitMume());
        pnlCuration.add(btnCommit);
        
        btnClear = new JButton("Clear Analysis Buffer");
        btnClear.setEnabled(false);
        btnClear.addActionListener(e -> {
            if (analyzer != null) analyzer.clear();
            log("Analysis buffer cleared.");
        });
        pnlCuration.add(btnClear);

        btnShowScore = new JButton("Show Pitch Landscape");
        btnShowScore.addActionListener(e -> mumeScore.setVisible(true));
        pnlCuration.add(btnShowScore);

        btnStartStop = new JButton("Start Audio Context");
        btnStartStop.addActionListener(e -> toggleAudio());
        pnlCuration.add(btnStartStop);
        
        pnlCenter.add(pnlCuration, BorderLayout.CENTER);
        add(pnlCenter, BorderLayout.CENTER);

        // --- 3. Status Area ---
        statusArea = new JTextArea(8, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(statusArea), BorderLayout.SOUTH);

        // Timer for the meter (~30 FPS)
        meterTimer = new javax.swing.Timer(33, e -> updateMeter());
        
        setVisible(true);
    }

    private void toggleAudio() {
        if (ac == null) {
            startAudio();
            btnStartStop.setText("Stop Audio Context");
            btnCommit.setEnabled(true);
            btnClear.setEnabled(true);
            meterTimer.start();
        } else {
            stopAudio();
            btnStartStop.setText("Start Audio Context");
            btnCommit.setEnabled(false);
            btnClear.setEnabled(false);
            meterTimer.stop();
            outputMeter.setLevel(0);
        }
    }

    private void startAudio() {
        JavaSoundAudioIO io = new JavaSoundAudioIO();
        ac = new AudioContext(io);
        
        analyzer = new MumeAnalyzer(ac, 1024);
        ac.out.addDependent(analyzer);
        visualizer.setAnalyzer(analyzer);
        
        inputGain = new Gain(ac, 1, 1.0f);
        inputGain.addInput(ac.getAudioInput());
        analyzer.addInput(inputGain);
        
        // Setup Auditioning Synth
        auditionFreq = new Glide(ac, 440.0f, 20.0f);
        auditionOsc = new WavePlayer(ac, auditionFreq, Buffer.SINE);
        auditionEnv = new Envelope(ac, 0.0f);
        auditionGain = new Gain(ac, 1, auditionEnv);
        auditionGain.addInput(auditionOsc);
        
        // Master Output
        Gain masterGain = new Gain(ac, 1, 0.5f);
        masterGain.addInput(auditionGain);
        ac.out.addInput(masterGain);
        
        // Output Metering (RMS analysis)
        rms = new RMS(ac, 1);
        rms.addInput(ac.out);
        ac.out.addDependent(rms);
        
        ac.start();
        log("AudioContext started. Ready to audition.");
    }

    private void stopAudio() {
        if (ac != null) {
            ac.stop();
            ac = null;
            visualizer.setAnalyzer(null);
            log("AudioContext stopped.");
        }
    }

    private void updateMeter() {
        if (rms != null) {
            // Get RMS level and scale for visibility
            float val = rms.getValue();
            outputMeter.setLevel(val * 2.0f); // Scale up for visual feedback
        }
    }

    private void playAuditionPitch(double midiPitch) {
        if (ac == null) return;
        
        float freq = (float) (440.0 * Math.pow(2.0, (midiPitch - 69.0) / 12.0));
        auditionFreq.setValue(freq);
        
        // 500ms blip
        auditionEnv.addSegment(0.5f, 50);  // Attack
        auditionEnv.addSegment(0.5f, 400); // Sustain
        auditionEnv.addSegment(0.0f, 50);  // Release
        
        log("Auditioning MIDI: " + String.format("%.2f", midiPitch));
    }

    private void commitMume() {
        if (analyzer != null) {
            MumeData currentMume = analyzer.getMume(0, txtMumeID.getText(), 0.1f);
            log("COMMITTED: " + currentMume.id + " (Mass: " + String.format("%.2f", currentMume.weight) + ")");
            mumeScore.updatePitches(analyzer.getAllBins());
        }
    }

    private void log(String msg) {
        statusArea.append("> " + msg + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MumeUI::new);
    }
}
