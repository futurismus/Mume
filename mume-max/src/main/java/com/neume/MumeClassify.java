package com.neume;

import com.cycling74.max.*;
import java.util.*;

/**
 * MumeClassify: A simple Nearest-Neighbor classifier for spectral vectors.
 */
public class MumeClassify extends MaxObject {
    private List<MumeData> models = new ArrayList<>();
    private float freqScale = 1000.0f;

    public MumeClassify() {
        declareInlets(new int[]{DataTypes.ALL});
        declareOutlets(new int[]{
            DataTypes.ALL, // 0: ID
            DataTypes.ALL, // 1: Index
            DataTypes.ALL, // 2: Stored Spectral Vector
            DataTypes.ALL, // 3: Best Distance Score
            DataTypes.ALL  // 4: Full Distance Landscape
        }); 
    }

    public void train(Atom[] list) {
        if (list.length < 6) {
            error("MumeClassify: Training list too short.");
            return;
        }
        MumeData data = new MumeData(list);
        models.add(data);
        post("MumeClassify: Trained model '" + data.id + "'");
    }

    public void list(Atom[] list) {
        if (models.isEmpty()) {
            error("MumeClassify: No trained models available.");
            return;
        }
        float[] liveVector = Atom.toFloat(list);
        MumeData bestMatch = null;
        float minDistance = Float.MAX_VALUE;
        Atom[] landscape = new Atom[(models.size() * 2) + 1];
        landscape[0] = Atom.newAtom("landscape");
        for (int i = 0; i < models.size(); i++) {
            MumeData model = models.get(i);
            float dist = calculateDistance(liveVector, Atom.toFloat(model.spectralData));
            landscape[i * 2 + 1] = Atom.newAtom(model.id);
            landscape[i * 2 + 2] = Atom.newAtom(dist);
            if (dist < minDistance) {
                minDistance = dist;
                bestMatch = model;
            }
        }
        if (bestMatch != null) outputMatch(bestMatch, minDistance, landscape);
    }

    private void outputMatch(MumeData match, float distance, Atom[] landscape) {
        outlet(0, match.id);
        outlet(1, match.index);
        outlet(2, match.spectralData);
        outlet(3, distance);
        outlet(4, landscape);
    }

    public void recall(String id) {
        for (MumeData model : models) {
            if (model.id.equals(id)) {
                outputMatch(model, 0.0f, new Atom[]{Atom.newAtom("landscape"), Atom.newAtom(id), Atom.newAtom(0.0f)});
                return;
            }
        }
        error("MumeClassify: ID '" + id + "' not found in classifier memory.");
    }

    private float calculateDistance(float[] v1, float[] v2) {
        float sum = 0;
        int len = Math.min(v1.length, v2.length);
        if (len % 2 != 0) len--; 
        for (int i = 0; i < len; i += 2) {
            float dFreq = (v1[i] - v2[i]) / Math.max(1.0f, freqScale);
            float dAmp = (v1[i+1] - v2[i+1]);
            sum += (dFreq * dFreq) + (dAmp * dAmp);
        }
        return (float) Math.sqrt(sum);
    }

    public void clear() {
        models.clear();
        post("MumeClassify: All models cleared.");
        size();
    }

    public void size() {
        outlet(4, new Atom[]{Atom.newAtom("size"), Atom.newAtom(models.size())});
    }

    public void sensitivity(float val) { this.freqScale = val; }
    
    public void anything(String msg, Atom[] args) {
        if (msg.equals("add")) train(args);
        else if (msg.equals("bestmatch")) list(args);
    }
}
