package com.neume;

import net.beadsproject.beads.analysis.featureextractors.FFT;
import net.beadsproject.beads.analysis.featureextractors.PowerSpectrum;
import net.beadsproject.beads.analysis.featureextractors.SpectralPeaks;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.core.TimeStamp;
import com.cycling74.max.Atom;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MumeAnalyzer: Sequential Capture -> Process model.
 * Uses a pre-allocated array for zero-allocation in the audio thread.
 */
public class MumeAnalyzer extends UGen {
    private final FFT fft;
    private final PowerSpectrum ps;
    private final SpectralPeaks peaks;
    private final int numPeaks;
    
    private float frequencyResolution = 1.0f;
    private float amplitudeResolution = 0.01f;
    
    // Pre-allocated "Coll" to avoid GC pressure using flat arrays
    private static final int MAX_CAPTURED_PEAKS = 200000;
    private final float[] capturedFrequencies = new float[MAX_CAPTURED_PEAKS];
    private final float[] capturedAmplitudes = new float[MAX_CAPTURED_PEAKS];
    private int captureIndex = 0;
    
    private final Map<Float, FrequencyBin> histogram = new ConcurrentHashMap<>();

    public MumeAnalyzer(AudioContext ac, int fftSize) {
        this(ac, fftSize, 24);
    }

    public MumeAnalyzer(AudioContext ac, int fftSize, int numPeaks) {
        super(ac, 1, 0); 
        this.numPeaks = numPeaks;
        this.fft = new FFT();
        this.ps = new PowerSpectrum();
        this.peaks = new SpectralPeaks(ac, numPeaks);
        this.fft.addListener(ps);
        this.ps.addListener(peaks);
    }

    @Override
    public void calculateBuffer() {
        TimeStamp ts = context.generateTimeStamp(0);
        fft.process(ts, ts, bufIn[0]);
        
        float[][] peakData = (float[][]) peaks.getFeatures();
        if (peakData != null) {
            for (int i = 0; i < peakData.length; i++) {
                if (captureIndex < MAX_CAPTURED_PEAKS) {
                    float freq = peakData[i][0];
                    float amp = peakData[i][1];
                    if (amp >= amplitudeResolution) {
                        capturedFrequencies[captureIndex] = freq;
                        capturedAmplitudes[captureIndex] = amp;
                        captureIndex++;
                    }
                }
            }
        }
    }

    public void resetCapture() {
        captureIndex = 0;
    }

    /**
     * Sequential Magnitude Calculation (Histo Analysis)
     */
    public void processCapturedData() {
        histogram.clear();
        int totalCaptured = captureIndex;
        
        for (int i = 0; i < totalCaptured; i++) {
            float freq = capturedFrequencies[i];
            float amp = capturedAmplitudes[i];

            float binnedFreq = Math.round(freq / frequencyResolution) * frequencyResolution;
            float binnedAmp = Math.round(amp / amplitudeResolution) * amplitudeResolution;

            histogram.compute(binnedFreq, (k, v) -> {
                if (v == null) return new FrequencyBin(binnedFreq, binnedAmp);
                v.update(binnedAmp);
                return v;
            });
        }
        resetCapture();
    }

    public List<FrequencyBin> getAllBins() {
        return new ArrayList<>(histogram.values());
    }

    public MumeData getMume(int index, String id, float threshold) {
        List<FrequencyBin> bins = getAllBins();
        // Remove bins below threshold before sorting/processing
        bins.removeIf(bin -> bin.maxAmplitude < threshold);
        bins.sort((a, b) -> Float.compare(b.getWeight(), a.getWeight()));
        
        float totalWeight = 0;
        float topFreq = 0, topAmp = 0;
        float maxBinWeight = -1.0f;
        Atom[] spectral = new Atom[numPeaks * 2];
        int peakCount = 0;

        for (FrequencyBin bin : bins) {
            float w = bin.getWeight();
            totalWeight += w;
            if (w > maxBinWeight) {
                maxBinWeight = w;
                topFreq = bin.frequency;
                topAmp = bin.maxAmplitude;
            }
            if (peakCount < numPeaks) {
                spectral[peakCount * 2] = Atom.newAtom(bin.frequency);
                spectral[peakCount * 2 + 1] = Atom.newAtom(bin.maxAmplitude);
                peakCount++;
            }
        }
        for (int i = peakCount; i < numPeaks; i++) {
            spectral[i * 2] = Atom.newAtom(0.0f);
            spectral[i * 2 + 1] = Atom.newAtom(0.0f);
        }
        return new MumeData(index, id, topFreq, topAmp, totalWeight, threshold, spectral);
    }

    public void setFrequencyResolution(float res) { this.frequencyResolution = res; }
    public void setAmplitudeResolution(float res) { this.amplitudeResolution = res; }
    public void clear() { 
        histogram.clear(); 
        resetCapture();
    }
}
