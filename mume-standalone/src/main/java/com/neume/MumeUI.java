package com.neume;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * MumeUI: The graphical interface for the standalone Mume port.
 * Manages audio settings and provides the "human in the loop" curation interface.
 */
public class MumeUI extends JFrame {
    private AudioContext ac;
    private MumeAnalyzer analyzer;
    private Gain inputGain;
    
    // UI Elements
    private final JTextArea statusArea;
    private final JButton btnStartStop;
    private final JButton btnCommit;
    private final JButton btnClear;
    private final JTextField txtMumeID;
    private final JSlider sliderRes;
    private final JSlider sliderAmpRes;
    
    public MumeUI() {
        super("mxj Mume Standalone");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);

        // --- 1. Audio Control Panel ---
        JPanel pnlAudio = new JPanel();
        pnlAudio.setBorder(BorderFactory.createTitledBorder("Audio Settings"));
        
        btnStartStop = new JButton("Start Audio Context");
        btnStartStop.addActionListener(e -> toggleAudio());
        pnlAudio.add(btnStartStop);
        
        add(pnlAudio, BorderLayout.NORTH);

        // --- 2. Mume Curation Panel ---
        JPanel pnlCuration = new JPanel(new GridLayout(8, 1, 5, 5));
        pnlCuration.setBorder(BorderFactory.createTitledBorder("Mume Curation (Human-in-the-Loop)"));
        
        txtMumeID = new JTextField("standalone-mume-01");
        pnlCuration.add(new JLabel("Current Mume ID:"));
        pnlCuration.add(txtMumeID);
        
        pnlCuration.add(new JLabel("Frequency Resolution (Hz):"));
        sliderRes = new JSlider(1, 100, 50); // Represents 0.1 to 10.0 Hz
        sliderRes.addChangeListener(e -> {
            float val = sliderRes.getValue() / 10.0f;
            if (analyzer != null) analyzer.setFrequencyResolution(val);
        });
        pnlCuration.add(sliderRes);

        pnlCuration.add(new JLabel("Amplitude Threshold (0.0 - 0.1):"));
        sliderAmpRes = new JSlider(0, 100, 10); // Represents 0.0 to 0.1
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
        
        add(pnlCuration, BorderLayout.CENTER);

        // --- 3. Status/Logging Area ---
        statusArea = new JTextArea();
        statusArea.setEditable(false);
        add(new JScrollPane(statusArea), BorderLayout.SOUTH);

        setVisible(true);
    }

    private void toggleAudio() {
        if (ac == null) {
            startAudio();
            btnStartStop.setText("Stop Audio Context");
            btnCommit.setEnabled(true);
            btnClear.setEnabled(true);
        } else {
            stopAudio();
            btnStartStop.setText("Start Audio Context");
            btnCommit.setEnabled(false);
            btnClear.setEnabled(false);
        }
    }

    private void startAudio() {
        JavaSoundAudioIO io = new JavaSoundAudioIO();
        ac = new AudioContext(io);
        
        analyzer = new MumeAnalyzer(ac, 1024);
        // Set initial resolution from sliders
        analyzer.setFrequencyResolution(sliderRes.getValue() / 10.0f);
        analyzer.setAmplitudeResolution(sliderAmpRes.getValue() / 1000.0f);
        
        ac.out.addDependent(analyzer);
        
        inputGain = new Gain(ac, 1, 1.0f);
        inputGain.addInput(ac.getAudioInput());
        analyzer.addInput(inputGain);
        
        ac.start();
        log("AudioContext started using default input device.");
    }

    private void stopAudio() {
        if (ac != null) {
            ac.stop();
            ac = null;
            log("AudioContext stopped.");
        }
    }

    private void commitMume() {
        if (analyzer != null) {
            MumeData currentMume = analyzer.getMume(0, txtMumeID.getText(), 0.1f);
            log("COMMITTED: " + currentMume.id + " (Mass: " + String.format("%.2f", currentMume.weight) + ")");
        }
    }

    private void log(String msg) {
        statusArea.append(msg + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MumeUI::new);
    }
}
