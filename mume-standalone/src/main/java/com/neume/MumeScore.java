package com.neume;

import com.softsynth.jmsl.*;
import com.softsynth.jmsl.score.*;
import java.util.*;
import java.io.File;
import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/**
 * MumeScore: A standalone pitch visualizer with auditioning support.
 */
public class MumeScore implements PlayLurker {
    private Score score;
    private Frame frame;
    private Consumer<Double> onNoteSelected;

    public MumeScore() {
        ensureJmslPrefsExist();
        JMSL.verbosity = 0;

        this.score = createNewScore();
        setupFrame();
    }

    private void setupFrame() {
        if (frame != null) frame.dispose();
        
        frame = new Frame("Mume Pitch Landscape");
        frame.setLayout(new BorderLayout());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
            }
        });
        
        frame.add(score.getScoreCanvas().getComponent(), BorderLayout.CENTER);
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    private Score createNewScore() {
        // Score(numStaves, width, height)
        Score s = new Score(2, 800, 540);
        s.setNumTracksPerStaff(1);
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
        newScore.addMeasure();
        newScore.setCurrentMeasureNumber(0);

        List<FrequencyBin> sortedBins = new ArrayList<>(bins);
        sortedBins.sort((a, b) -> Float.compare(b.getWeight(), a.getWeight()));
        int count = Math.min(sortedBins.size(), 12);

        for (int i = 0; i < count; i++) {
            FrequencyBin bin = sortedBins.get(i);
            double midi = freqToMidi48TET(bin.frequency);
            if (midi < 0 || midi > 127) continue;
            
            int staffIndex = (midi >= 60) ? 0 : 1;

            Note note = NoteFactory.makeNote(4.0);
            note.setData(4.0, midi, 64.0, 0.8);
            
            newScore.setCurrentStaffNumber(staffIndex);
            newScore.addNote(note); 
        }

        attachLurker(newScore);

        frame.removeAll();
        frame.add(newScore.getScoreCanvas().getComponent(), BorderLayout.CENTER);
        frame.revalidate();
        
        this.score = newScore;
        this.score.render();
    }

    private void attachLurker(Score s) {
        for (int var1 = 0; var1 < s.size(); ++var1) {
            Measure var2 = s.getMeasure(var1);
            for (int var3 = 0; var3 < s.getNumStaves(); ++var3) {
                Staff var4 = var2.getStaff(var3);
                Enumeration var5 = var4.elements();
                while (var5.hasMoreElements()) {
                    Track var6 = (Track) var5.nextElement();
                    var6.addPlayLurker(this);
                }
            }
        }
    }

    // --- PlayLurker Implementation ---

    @Override
    public void notifyPlayLurker(double playtime, MusicJob job, int index) {
        if (onNoteSelected != null && job instanceof com.softsynth.jmsl.score.Track) {
            com.softsynth.jmsl.score.Track track = (com.softsynth.jmsl.score.Track) job;
            Note note = track.getNote(index);
            if (note != null) {
                double[] perfData = note.getPerformanceData();
                if (perfData != null && perfData.length > 1) {
                    double pitch = perfData[1]; // Index 1 is PITCH_DATA
                    onNoteSelected.accept(pitch);
                }
            }
        }
    }

    private double freqToMidi48TET(float freq) {
        if (freq <= 0) return 0;
        double midi = 69 + 12 * (Math.log(freq / 440.0) / Math.log(2));
        return Math.round(midi * 4.0) / 4.0;
    }

    public void setVisible(boolean visible) {
        if (frame != null) frame.setVisible(visible);
    }
}
