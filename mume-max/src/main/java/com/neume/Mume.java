package com.neume;

import com.cycling74.max.*;
import java.util.*;

/**
 * Mume: A spectral analysis and storage object for Max.
 * Analogy: A "neumonic" unit of spectral information.
 */
public class Mume extends MaxObject {
    
    // Standard metadata fields
    private int mePoint;
    private String mumeID;
    private float frequency;
    private float amplitude;
    private float weight;
    private float activationThreshold;

    // Use the shared FrequencyBin class from mume-core
    private Map<Float, FrequencyBin> histogram = new HashMap<>();
    private float frequencyResolution = 0.5f;
    private float amplitudeResolution = 0.01f;
    private boolean autoDump = false;

    public Mume() {
        this(new Atom[0]);
    }

    public Mume(Atom[] args) {
        declareInlets(new int[]{DataTypes.ALL, DataTypes.ALL});
        declareOutlets(new int[]{DataTypes.ALL, DataTypes.ALL, DataTypes.ALL}); // 0: Spectral, 1: Full State, 2: Energy Dist
        
        MumeData data = new MumeData(args);
        this.mePoint = data.index;
        this.mumeID = data.id;
        this.frequency = data.frequency;
        this.amplitude = data.amplitude;
        this.weight = data.weight;
        this.activationThreshold = data.threshold;
    }

    public void bang() {
        if (this.weight >= this.activationThreshold) {
            outlet(1, getMumeData(null).toFullState());
        } else {
            post("Mume '" + mumeID + "' below activation threshold: " + weight + "/" + activationThreshold);
        }
    }

    public void list(Atom[] list) {
        if (list.length > 0 && list.length % 2 == 0) {
            for (int i = 0; i < list.length; i += 2) {
                float freq = list[i].getFloat();
                float amp = list[i + 1].getFloat();
                
                if (amp < amplitudeResolution) continue;
                
                float binnedFreq = Math.round(freq / frequencyResolution) * frequencyResolution;
                float binnedAmp = Math.round(amp / amplitudeResolution) * amplitudeResolution;

                histogram.compute(binnedFreq, (k, v) -> {
                    if (v == null) return new FrequencyBin(binnedFreq, binnedAmp);
                    v.update(binnedAmp);
                    return v;
                });
            }
            updateWeightFromHistogram();
            if (autoDump) dump();
        } else {
            error("List must contain pairs of frequency and amplitude.");
        }
    }

    public void setMetadata(Atom[] args) {
        if (args == null || args.length == 0) return;
        MumeData incoming = new MumeData(args);
        if (!this.mumeID.equals("") && !this.mumeID.equals(incoming.id)) {
            outlet(1, getMumeData(null).toFullState());
            clear();
            this.mePoint++; 
        } else if (this.mumeID.equals("")) {
            this.mePoint = incoming.index;
        }
        this.mumeID = incoming.id;
        if (args.length >= 6) {
            this.frequency = incoming.frequency;
            this.amplitude = incoming.amplitude;
            this.weight = incoming.weight;
            this.activationThreshold = incoming.threshold;
        }
    }

    private MumeData getMumeData(Atom[] spectralData) {
        return new MumeData(mePoint, mumeID, frequency, amplitude, weight, activationThreshold, spectralData);
    }

    public void train() {
        if (this.weight >= this.activationThreshold) {
            List<FrequencyBin> bins = getSortedBins();
            int topBinsCount = Math.min(bins.size(), 24);
            Atom[] topSpectral = new Atom[topBinsCount * 2];
            for (int i = 0; i < topBinsCount; i++) {
                topSpectral[i * 2] = Atom.newAtom(bins.get(i).frequency);
                topSpectral[i * 2 + 1] = Atom.newAtom(bins.get(i).maxAmplitude);
            }
            outlet(1, getMumeData(topSpectral).toFullState());
        } else {
            error("Training rejected: Weight (" + weight + ") below threshold (" + activationThreshold + ")");
        }
    }

    private void updateWeightFromHistogram() {
        float totalWeight = 0;
        float maxBinWeight = -1.0f;
        FrequencyBin topBin = null;
        for (FrequencyBin bin : histogram.values()) {
            float binWeight = bin.getWeight();
            totalWeight += binWeight;
            if (binWeight > maxBinWeight) {
                maxBinWeight = binWeight;
                topBin = bin;
            }
        }
        this.weight = totalWeight;
        if (topBin != null) {
            this.frequency = topBin.frequency;
            this.amplitude = topBin.maxAmplitude;
        } else {
            this.frequency = 0.0f;
            this.amplitude = 0.0f;
        }
    }

    public void clear() {
        histogram.clear();
        this.frequency = 0.0f;
        this.amplitude = 0.0f;
        this.weight = 0.0f;
        post("Histogram cleared.");
    }

    public void frequencyResolution(float res) { this.frequencyResolution = res; }
    public void ampResolution(float res) { this.amplitudeResolution = res; }
    public void auto(int toggle) { this.autoDump = (toggle != 0); }

    public void dump() {
        if (this.weight < this.activationThreshold) return;
        List<FrequencyBin> bins = getSortedBins();
        Atom[] output = new Atom[bins.size() * 2];
        for (int i = 0; i < bins.size(); i++) {
            output[i * 2] = Atom.newAtom(bins.get(i).frequency);
            output[i * 2 + 1] = Atom.newAtom(bins.get(i).maxAmplitude);
        }
        outlet(0, output);
        outlet(1, getMumeData(output).toFullState());
        outputEnergyDistribution(bins);
    }

    private void outputEnergyDistribution(List<FrequencyBin> bins) {
        Atom[] energyList = new Atom[bins.size() * 2];
        for (int i = 0; i < bins.size(); i++) {
            energyList[i * 2] = Atom.newAtom(bins.get(i).frequency);
            energyList[i * 2 + 1] = Atom.newAtom(bins.get(i).getWeight());
        }
        outlet(2, energyList);
    }

    private List<FrequencyBin> getSortedBins() {
        List<FrequencyBin> bins = new ArrayList<>(histogram.values());
        bins.sort((a, b) -> {
            float weightA = a.getWeight();
            float weightB = b.getWeight();
            if (weightA != weightB) return Float.compare(weightB, weightA);
            return Float.compare(b.maxAmplitude, a.maxAmplitude);
        });
        return bins;
    }
}
