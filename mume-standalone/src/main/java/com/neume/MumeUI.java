package com.neume;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.*;
import net.beadsproject.beads.core.io.JavaSoundAudioIO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * MumeUI: Refactored to follow a sequential "Capture -> Process" workflow.
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

    private OscillatorBank resynthOscBank;
    private Envelope resynthEnv;
    private Gain resynthGain;
    
    private AudioLevelMeter outputMeter;
    private RMS rms;
    private Gain masterGain;
    private javax.swing.Timer meterTimer;
    
    private final JTextArea statusArea;
    private final JButton btnStartStop;
    private final JButton btnCommit;
    private final JButton btnResynthesize;
    private final JButton btnClear;
    private final JButton btnShowScore;
    private final JButton btnQuit;
    private final JButton btnAnalyze;
    private final JSlider sliderMasterGain;
    private final JTextField txtMumeID;
    private final JSlider sliderRes;
    private final JSlider sliderAmpRes;

    private volatile boolean keepRunning = true;
    
    public MumeUI() {
        super("Mume Standalone");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 950);

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
        
        sliderMasterGain = new JSlider(JSlider.VERTICAL, 0, 100, 40); 
        sliderMasterGain.addChangeListener(e -> {
            float val = (sliderMasterGain.getValue() / 100.0f) * 0.5f;
            if (masterGain != null) masterGain.setGain(val);
        });
        pnlOutput.add(sliderMasterGain, BorderLayout.EAST);
        pnlCenter.add(pnlOutput, BorderLayout.EAST);

        JPanel pnlCuration = new JPanel(new GridLayout(14, 1, 5, 2));
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
        
        btnAnalyze = new JButton("HOLD TO CAPTURE PEAKS");
        btnAnalyze.setEnabled(false);
        btnAnalyze.setBackground(new Color(200, 230, 200));
        btnAnalyze.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (ac != null && analyzer != null && btnAnalyze.isEnabled()) {
                    analyzer.resetCapture(); 
                    ac.out.addDependent(analyzer); 
                    
                    if (inputGain != null) {
                        analyzer.addInput(inputGain);
                    } else {
                        analyzer.addInput(masterGain); 
                    }

                    btnAnalyze.setText("CAPTURING...");
                    btnAnalyze.setBackground(Color.RED);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (ac != null && analyzer != null) {
                    ac.out.removeDependent(analyzer); 
                    analyzer.clearInputConnections(); 
                    
                    btnAnalyze.setText("PROCESSING...");
                    btnAnalyze.setBackground(Color.ORANGE);
                    
                    new Thread(() -> {
                        analyzer.processCapturedData();
                        SwingUtilities.invokeLater(() -> {
                            btnAnalyze.setText("HOLD TO CAPTURE PEAKS");
                            btnAnalyze.setBackground(new Color(200, 230, 200));
                            log("Spectral data processed.");
                        });
                    }).start();
                }
            }
        });
        pnlCuration.add(btnAnalyze);

        btnCommit = new JButton("Commit Current Spectral State");
        btnCommit.setEnabled(false);
        btnCommit.addActionListener(e -> commitMume());
        pnlCuration.add(btnCommit);

        btnResynthesize = new JButton("Resynthesize Current State");
        btnResynthesize.setEnabled(false);
        btnResynthesize.addActionListener(e -> resynthesize());
        pnlCuration.add(btnResynthesize);
        
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

        btnQuit = new JButton("Quit Application");
        btnQuit.setBackground(new Color(255, 200, 200));
        btnQuit.addActionListener(e -> {
            keepRunning = false;
            log("Exiting application...");
            System.exit(0);
        });
        pnlCuration.add(btnQuit);
        
        pnlCenter.add(pnlCuration, BorderLayout.CENTER);
        add(pnlCenter, BorderLayout.CENTER);

        statusArea = new JTextArea(8, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(statusArea), BorderLayout.SOUTH);

        meterTimer = new javax.swing.Timer(33, e -> updateMeter());
        setVisible(true);

        startProcessLoop();
    }

    private void startProcessLoop() {
        new Thread(() -> {
            log("Background process loop started.");
            do {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } while (keepRunning);
            log("Background process loop stopped.");
        }).start();
    }

    private void toggleAudio() {
        if (ac == null) {
            startAudio();
            btnStartStop.setText("Stop Audio Context");
            btnCommit.setEnabled(true);
            btnResynthesize.setEnabled(true);
            btnClear.setEnabled(true);
            btnAnalyze.setEnabled(true);
            meterTimer.start();
        } else {
            stopAudio();
            btnStartStop.setText("Start Audio Context");
            btnCommit.setEnabled(false);
            btnResynthesize.setEnabled(false);
            btnClear.setEnabled(false);
            btnAnalyze.setEnabled(false);
            meterTimer.stop();
            outputMeter.setLevel(0);
            
            try { Thread.sleep(200); } catch (Exception ex) {}
        }
    }

    private void startAudio() {
        JavaSoundAudioIO io = new JavaSoundAudioIO();
        ac = new AudioContext(io, 1024);
        
        analyzer = new MumeAnalyzer(ac, 1024);
        
        auditionFreq = new Glide(ac, 440.0f, 20.0f);
        auditionOsc = new WavePlayer(ac, auditionFreq, Buffer.SINE);
        auditionEnv = new Envelope(ac, 0.0f);
        auditionGain = new Gain(ac, 1, auditionEnv);
        auditionGain.addInput(auditionOsc);

        resynthOscBank = new OscillatorBank(ac, Buffer.SINE, 24);
        resynthEnv = new Envelope(ac, 0.0f);
        resynthGain = new Gain(ac, 1, resynthEnv);
        resynthGain.addInput(resynthOscBank);
        
        float initialGain = (sliderMasterGain.getValue() / 100.0f) * 0.5f;
        masterGain = new Gain(ac, 1, initialGain);
        masterGain.addInput(auditionGain);
        masterGain.addInput(resynthGain);
        ac.out.addInput(masterGain);
        
        rms = new RMS(ac, 1, 100);
        rms.addInput(masterGain);
        ac.out.addDependent(rms);
        
        try {
            inputGain = new Gain(ac, 1, 0.2f); 
            inputGain.addInput(ac.getAudioInput());
            log("Audio Input initialized.");
        } catch (Exception e) {
            inputGain = null; 
            log("WARNING: Mic busy. Internal analysis mode active.");
        }
        
        visualizer.setAnalyzer(analyzer);
        ac.start();
        log("AudioContext started.");
    }

    private void stopAudio() {
        if (ac != null) {
            ac.stop();
            // Disconnect inputs from the master output gain
            if (ac.out != null) {
                ac.out.clearInputConnections();
            }
            ac = null;
            
            analyzer = null;
            inputGain = null;
            rms = null;
            masterGain = null;
            
            log("AudioContext stopped.");
        }
    }

    private void updateMeter() {
        if (rms != null) {
            try {
                float val = rms.getOutBuffer(0)[0];
                outputMeter.setLevel(val * 5.0f);
            } catch (Exception e) {
                // Ignore transient errors during stop
            }
        }
    }

    private void playAuditionPitch(double midiPitch) {
        if (ac == null) return;
        float freq = (float) (440.0 * Math.pow(2.0, (midiPitch - 69.0) / 12.0));
        auditionFreq.setValue(freq);
        auditionEnv.addSegment(0.2f, 50); 
        auditionEnv.addSegment(0.2f, 400);
        auditionEnv.addSegment(0.0f, 50);
    }

    private void resynthesize() {
        if (analyzer != null && resynthOscBank != null) {
            MumeData currentMume = analyzer.getMume(0, txtMumeID.getText(), 0.1f);
            float[] frequencies = new float[currentMume.spectralData.length / 2];
            float[] gains = new float[currentMume.spectralData.length / 2];
            for (int i = 0; i < frequencies.length; i++) {
                frequencies[i] = currentMume.spectralData[i * 2].getFloat();
                gains[i] = currentMume.spectralData[i * 2 + 1].getFloat();
            }
            resynthOscBank.setFrequencies(frequencies);
            resynthOscBank.setGains(gains);
            resynthEnv.addSegment(0.4f, 50);
            resynthEnv.addSegment(0.4f, 1000);
            resynthEnv.addSegment(0.0f, 500);
            log("Resynthesizing: " + currentMume.id + " (" + frequencies.length + " partials)");
        }
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
