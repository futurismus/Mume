package com.neume;

import com.cycling74.max.*;

/**
 * MumeData: A utility class to encapsulate the Mume state.
 * Handles parsing from Atom arrays and serialization back to Atom arrays.
 */
public class MumeData {
    public int index;
    public String id;
    public float frequency;
    public float amplitude;
    public float weight;
    public float threshold;
    public Atom[] spectralData;

    public MumeData(int index, String id, float freq, float amp, float weight, float threshold, Atom[] spectral) {
        this.index = index;
        this.id = id;
        this.frequency = freq;
        this.amplitude = amp;
        this.weight = weight;
        this.threshold = threshold;
        this.spectralData = (spectral != null) ? spectral : new Atom[0];
    }

    public MumeData(Atom[] args) {
        this.index = (args.length > 0) ? args[0].getInt() : 0;
        this.id = (args.length > 1) ? args[1].getString() : "unknown";
        this.frequency = (args.length > 2) ? args[2].getFloat() : 0.0f;
        this.amplitude = (args.length > 3) ? args[3].getFloat() : 0.0f;
        this.weight = (args.length > 4) ? args[4].getFloat() : 0.0f;
        this.threshold = (args.length > 5) ? args[5].getFloat() : 0.0f;

        if (args.length > 6) {
            int specLen = args.length - 6;
            this.spectralData = new Atom[specLen];
            System.arraycopy(args, 6, this.spectralData, 0, specLen);
        } else {
            this.spectralData = new Atom[0];
        }
    }

    public Atom[] toFullState() {
        Atom[] state = new Atom[6 + spectralData.length];
        state[0] = Atom.newAtom(index);
        state[1] = Atom.newAtom(id);
        state[2] = Atom.newAtom(frequency);
        state[3] = Atom.newAtom(amplitude);
        state[4] = Atom.newAtom(weight);
        state[5] = Atom.newAtom(threshold);
        System.arraycopy(spectralData, 0, state, 6, spectralData.length);
        return state;
    }

    /**
     * Helper to get a float array for math/classification
     */
    public float[] getFloatVector() {
        return Atom.toFloat(spectralData);
    }
}
