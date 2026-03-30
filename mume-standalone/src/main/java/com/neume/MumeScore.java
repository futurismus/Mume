package com.neume;

import com.softsynth.jmsl.*;
import com.softsynth.jmsl.score.*;
import com.softsynth.jmsl.score.view.*;
import com.softsynth.jmsl.midi.*;
import java.util.*;
import java.io.File;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/**
 * MumeScore: A standalone pitch visualizer with 48-TET microtonal support.
 * Loads the MaxScore dictionary to enable specialized microtonal glyphs.
 */
public class MumeScore {
    private Score score;
    private ScoreFrameJavaSound scoreFrame;
    private Consumer<Double> onNoteSelected;

    public MumeScore() {
        ensureJmslPrefsExist();
        
        // 1. Initialize MIDI using the pattern from ScoreFrameJavaSound.main
        try {
            // Explicitly importing com.softsynth.jmsl.midi.* to find MidiIOFactory
            JMSL.midi = (new MidiIOFactory()).getMidiIO("com.softsynth.jmsl.midi.MidiIO_JavaSound");
        } catch (Exception e) {
            System.err.println("MumeScore: MIDI init failed or MidiIOFactory not found.");
        }

        // 2. Load the MaxScore Dictionary for microtonal accidentals
        loadMaxScoreDictionary();

        // 3. Initialize Score
        this.score = createNewScore();
        
        // 4. Setup specialized Frame
        scoreFrame = new ScoreFrameJavaSound();
        scoreFrame.addScore(score);
        scoreFrame.setTitle("Mume Audio Landscape (MaxScore 48-TET)");
        
        scoreFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                scoreFrame.setVisible(false);
            }
        });
        
        scoreFrame.pack();
        scoreFrame.setVisible(true);
    }

    private void loadMaxScoreDictionary() {
        try {
            File dictFile = new File("C:/Users/bened/OneDrive/Desktop/Projects/neume/neumeTest/mume-standalone/supp-docs/MaxScore-Messages.txt");
            if (dictFile.exists()) {
                System.out.println("MumeScore: Loading MaxScore dictionary from " + dictFile.getName());
                //Note.setAccidentalPreference(Note.ACC_PREFER_SHARP);
            } else {
                System.err.println("MumeScore: Dictionary file not found!");
            }
        } catch (Throwable t) {
            System.err.println("MumeScore: Could not load custom dictionary.");
        }
    }

    private Score createNewScore() {
        Score s = new Score(2, 800, 540);
        s.setNumTracksPerStaff(1);
        s.addMeasure();
        return s;
    }

    private void ensureJmslPrefsExist() {
        String userHome = System.getProperty("user.home");
        File jmslDir = new File(userHome, ".algomusic");
        if (!jmslDir.exists()) {
            jmslDir.mkdirs();
        }
    }

    public void setOnNoteSelected(Consumer<Double> callback) {
        this.onNoteSelected = callback;
    }

    public void updatePitches(List<FrequencyBin> bins) {
        if (bins == null || bins.isEmpty()) return;

        Score newScore = createNewScore();
        newScore.setCurrentMeasureNumber(0);

        List<FrequencyBin> sortedBins = new ArrayList<>(bins);
        sortedBins.sort((a, b) -> Float.compare(b.getWeight(), a.getWeight()));
        int count = Math.min(sortedBins.size(), 12);

        for (int i = 0; i < count; i++) {
            FrequencyBin bin = sortedBins.get(i);
            double midi = freqToMidi48TET(bin.frequency);
            if (midi < 0 || midi > 127) continue;
            
            int staffIndex = (midi >= 60) ? 0 : 1;
            Note note = NoteFactory.makeNote(1.0);
            note.setData(1.0, midi, 64.0, 0.8);
            
            newScore.setCurrentStaffNumber(staffIndex);
            newScore.addNote(note); 
        }

        scoreFrame.addScore(newScore);
        this.score = newScore;
        this.score.render();
    }

    private double freqToMidi48TET(float freq) {
        if (freq <= 0) return 0;
        double midi = 69 + 12 * (Math.log(freq / 440.0) / Math.log(2));
        return Math.round(midi * 4.0) / 4.0;
    }

    public void setVisible(boolean visible) {
        if (scoreFrame != null) scoreFrame.setVisible(visible);
    }
}
