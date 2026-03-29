package com.neume;

/**
 * FrequencyBin: A utility class to store histogram bin data
 * used for spectral analysis in both Mume (Max) and MumeAnalyzer (Beads).
 */
public class FrequencyBin {
    public float frequency;
    public int count;
    public float maxAmplitude;

    public FrequencyBin(float freq, float amp) {
        this.frequency = freq;
        this.count = 1;
        this.maxAmplitude = amp;
    }

    public void update(float amp) {
        this.count++;
        if (amp > this.maxAmplitude) {
            this.maxAmplitude = amp;
        }
    }

    /**
     * Calculates the "Spectral Mass" or weight of the bin
     */
    public float getWeight() {
        return this.count * this.maxAmplitude;
    }
}
