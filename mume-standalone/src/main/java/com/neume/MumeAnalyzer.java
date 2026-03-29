package com.neume;

import net.beadsproject.beads.analysis.featureextractors.FFT;
import net.beadsproject.beads.analysis.featureextractors.PowerSpectrum;
import net.beadsproject.beads.analysis.featureextractors.SpectralPeaks;
import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.core.UGen;
import net.beadsproject.beads.core.TimeStamp;
import com.cycling74.max.Atom;

import java.util.*;

/**
 * MumeAnalyzer: Analysis engine for Beads standalone.
 */
public class MumeAnalyzer extends UGen {
    private final FFT fft;
    private final PowerSpectrum ps;
    private final SpectralPeaks peaks;
    private final int numPeaks;
    
    // Histogramming parameters
    private float frequencyResolution = 5.0f;
    private float amplitudeResolution = 0.01f;
    private final Map<Float, FrequencyBin> histogram = new HashMap<>();

    public MumeAnalyzer(AudioContext ac, int fftSize) {
        this(ac, fftSize, 24);
    }

    public MumeAnalyzer(AudioContext ac, int fftSize, int numPeaks) {
        super(ac, 1, 0); 
        this.numPeaks = numPeaks;
        
        // 1. Initialize the analysis chain
        this.fft = new FFT();
        this.ps = new PowerSpectrum();
        this.peaks = new SpectralPeaks(ac, numPeaks);
        
        // Link the chain
        this.fft.addListener(ps);
        this.ps.addListener(peaks);
    }

    @Override
    public void calculateBuffer() {
        // Use the context's built-in TimeStamp generator
        TimeStamp ts = context.generateTimeStamp(0);
        fft.process(ts, ts, bufIn[0]);
        
        // Retrieve the identified peaks
        float[][] peakData = (float[][]) peaks.getFeatures();
        if (peakData != null) {
            processPeaks(peakData);
        }
    }

    private void processPeaks(float[][] peakData) {
        for (float[] peak : peakData) {
            float freq = peak[0];
            float amp = peak[1];
            if (amp < amplitudeResolution) continue;

            float binnedFreq = Math.round(freq / frequencyResolution) * frequencyResolution;
            float binnedAmp = Math.round(amp / amplitudeResolution) * amplitudeResolution;

            histogram.compute(binnedFreq, (k, v) -> {
                if (v == null) return new FrequencyBin(binnedFreq, binnedAmp);
                v.update(binnedAmp);
                return v;
            });
        }
    }

    public MumeData getMume(int index, String id, float threshold) {
        List<FrequencyBin> sortedBins = getSortedBins();
        
        float totalWeight = 0;
        float topFreq = 0;
        float topAmp = 0;
        float maxBinWeight = -1.0f;

        Atom[] spectral = new Atom[numPeaks * 2];
        int peakCount = 0;

        for (FrequencyBin bin : sortedBins) {
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

        // Pad the rest of the spectral array with zeros if needed
        for (int i = peakCount; i < numPeaks; i++) {
            spectral[i * 2] = Atom.newAtom(0.0f);
            spectral[i * 2 + 1] = Atom.newAtom(0.0f);
        }

        return new MumeData(index, id, topFreq, topAmp, totalWeight, threshold, spectral);
    }

    private List<FrequencyBin> getSortedBins() {
        List<FrequencyBin> bins = new ArrayList<>(histogram.values());
        bins.sort((a, b) -> Float.compare(b.getWeight(), a.getWeight()));
        return bins;
    }

    public void setFrequencyResolution(float res) { this.frequencyResolution = res; }
    public void setAmplitudeResolution(float res) { this.amplitudeResolution = res; }
    public void clear() { histogram.clear(); }
}
