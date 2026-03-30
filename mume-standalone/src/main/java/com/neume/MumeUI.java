package com.neume;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.*;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;

import javax.swing.*;
import java.awt.*;

/**
 * MumeUI: Optimized for performance with larger buffer size and improved metering.
 */
public class MumeUI extends JFrame {
    private AudioContext ac;
    private MumeAnalyzer analyzer;
    private Gain inputGain;
    private MumeVisualizer visualizer;
    private MumeScore mumeScore;
    
    private WavePlayer auditionOsc;
    private Glide auditionFreq;
    private Envelope auditionEnv;
    private Gain auditionGain;
    
    private AudioLevelMeter outputMeter;
    private RMS rms;
    private Gain masterGain;
    private javax.swing.Timer meterTimer;
    
    private final JTextArea statusArea;
    private final JButton btnStartStop;
    private final JButton btnCommit;
    private final JButton btnClear;
    private final JButton btnShowScore;
    private final JSlider sliderMasterGain;
    private final JTextField txtMumeID;
    private final JSlider sliderRes;
    private final JSlider sliderAmpRes;
    
    public MumeUI() {
        super("Mume Standalone");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 850);

        mumeScore = new MumeScore();
        mumeScore.setOnNoteSelected(this::playAuditionPitch);
        mumeScore.setVisible(false);

        visualizer = new MumeVisualizer(null);
        add(visualizer, BorderLayout.NORTH);

        JPanel pnlCenter = new JPanel(new BorderLayout());
        
        JPanel pnlOutput = new JPanel(new BorderLayout());
        pnlOutput.setBorder(BorderFactory.createTitledBorder("Output"));
        outputMeter = new AudioLevelMeter();
        pnlOutput.add(outputMeter, BorderLayout.CENTER);
        
        sliderMasterGain = new JSlider(JSlider.VERTICAL, 0, 100, 70);
        sliderMasterGain.addChangeListener(e -> {
            float val = sliderMasterGain.getValue() / 100.0f;
            if (masterGain != null) masterGain.setGain(val);
        });
        pnlOutput.add(sliderMasterGain, BorderLayout.EAST);
        pnlCenter.add(pnlOutput, BorderLayout.EAST);

        JPanel pnlCuration = new JPanel(new GridLayout(11, 1, 5, 2));
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

        JButton btnTestTone = new JButton("Test Tone (440Hz)");
        btnTestTone.addActionListener(e -> playAuditionPitch(69.0));
        pnlCuration.add(btnTestTone);

        btnStartStop = new JButton("Start Audio Context");
        btnStartStop.addActionListener(e -> toggleAudio());
        pnlCuration.add(btnStartStop);
        
        pnlCenter.add(pnlCuration, BorderLayout.CENTER);
        add(pnlCenter, BorderLayout.CENTER);

        statusArea = new JTextArea(8, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(statusArea), BorderLayout.SOUTH);

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
        // Increase buffer size to 2048 to prevent crackling during FFT analysis
        ac = new AudioContext(io, 2048);
        
        analyzer = new MumeAnalyzer(ac, 1024);
        ac.out.addDependent(analyzer);
        visualizer.setAnalyzer(analyzer);
        
        inputGain = new Gain(ac, 1, 1.0f);
        inputGain.addInput(ac.getAudioInput());
        analyzer.addInput(inputGain);
        
        auditionFreq = new Glide(ac, 440.0f, 20.0f);
        auditionOsc = new WavePlayer(ac, auditionFreq, Buffer.SINE);
        auditionEnv = new Envelope(ac, 0.0f);
        auditionGain = new Gain(ac, 1, auditionEnv);
        auditionGain.addInput(auditionOsc);
        
        masterGain = new Gain(ac, 1, sliderMasterGain.getValue() / 100.0f);
        masterGain.addInput(auditionGain);
        ac.out.addInput(masterGain);
        
        rms = new RMS(ac, 1, 100);
        rms.addInput(masterGain);
        ac.out.addDependent(rms);
        
        ac.start();
        log("AudioContext started (Buffer: 2048).");
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
            float val = rms.getOutBuffer(0)[0];
            outputMeter.setLevel(val * 5.0f);
        }
    }

    private void playAuditionPitch(double midiPitch) {
        if (ac == null) return;
        float freq = (float) (440.0 * Math.pow(2.0, (midiPitch - 69.0) / 12.0));
        auditionFreq.setValue(freq);
        auditionEnv.addSegment(0.5f, 50);
        auditionEnv.addSegment(0.5f, 400);
        auditionEnv.addSegment(0.0f, 50);
        // log("Auditioning: " + String.format("%.1f", freq) + " Hz");
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
