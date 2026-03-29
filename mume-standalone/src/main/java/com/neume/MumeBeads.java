package com.neume;

import net.beadsproject.beads.core.AudioContext;
import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.OscillatorBank;
import com.cycling74.max.Atom;

/**
 * MumeBeads: Port of the Mume concept to the Beads library.
 * This class demonstrates how to take a MumeData object (spectral vector)
 * and resynthesize it using a Beads OscillatorBank.
 */
public class MumeBeads {
    
    public static void main(String[] args) {
        // 1. Initialize Audio
        AudioContext ac = new AudioContext();
        
        // 2. Create a Mock MumeData (representing a simplified spectral profile)
        // Format: [freq1, amp1, freq2, amp2...]
        Atom[] mockSpectral = new Atom[] {
            Atom.newAtom(440.0f), Atom.newAtom(0.5f),   // Fundamental
            Atom.newAtom(880.0f), Atom.newAtom(0.2f),   // 1st Harmonic
            Atom.newAtom(1320.0f), Atom.newAtom(0.1f)   // 2nd Harmonic
        };
        
        MumeData mume = new MumeData(1, "mume-osc-bank", 440.0f, 0.8f, 1.0f, 0.1f, mockSpectral);
        
        System.out.println("Starting MumeBeads Resynthesis...");
        System.out.println("Mume ID: " + mume.id);

        // 3. Set up the OscillatorBank
        // We'll use Sine wave (Buffer.SINE) for additive synthesis
        OscillatorBank oscBank = new OscillatorBank(ac, Buffer.SINE, mume.spectralData.length / 2);
        
        // 4. Map the Mume spectral data to the OscillatorBank
        float[] frequencies = new float[mume.spectralData.length / 2];
        float[] gains = new float[mume.spectralData.length / 2];
        
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = mume.spectralData[i * 2].getFloat();
            gains[i] = mume.spectralData[i * 2 + 1].getFloat();
        }
        
        oscBank.setFrequencies(frequencies);
        oscBank.setGains(gains);

        // 5. Connect to Output
        Gain masterGain = new Gain(ac, 1, 0.2f); // Set master volume to 20%
        masterGain.addInput(oscBank);
        ac.out.addInput(masterGain);
        
        // 6. Start Audio
        ac.start();
        
        System.out.println("Audio Started. Resynthesizing " + frequencies.length + " harmonics.");
    }
}
