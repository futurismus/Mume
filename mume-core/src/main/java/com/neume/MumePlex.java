package com.neume;

import java.io.*;
import java.util.*;

/**
 * MumePlex: A shared corpus manager for Mume spectral objects.
 * Handles storage, retrieval, and file-based persistence for Agent training.
 */
public class MumePlex {
    private final Map<String, MumeData> library = new LinkedHashMap<>();
    private final Map<Integer, String> indexMap = new HashMap<>();

    public MumePlex() {}

    public void add(MumeData data) {
        if (data == null) return;
        library.put(data.id, data);
        indexMap.put(data.index, data.id);
    }

    public MumeData get(String id) {
        return library.get(id);
    }

    public MumeData get(int index) {
        String id = indexMap.get(index);
        return (id != null) ? library.get(id) : null;
    }

    public List<MumeData> getAll() {
        return new ArrayList<>(library.values());
    }

    public int size() {
        return library.size();
    }

    public void clear() {
        library.clear();
        indexMap.clear();
    }

    /**
     * Saves the entire corpus to a simple CSV-like format 
     * (or we can use JSON if a library like GSON is available).
     * This is the training data for the Notation Sequence Agents.
     */
    public void saveToDataset(String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Header
            writer.println("index,id,frequency,amplitude,weight,threshold,spectral_vector");
            
            for (MumeData mume : library.values()) {
                StringBuilder sb = new StringBuilder();
                sb.append(mume.index).append(",");
                sb.append(mume.id).append(",");
                sb.append(mume.frequency).append(",");
                sb.append(mume.amplitude).append(",");
                sb.append(mume.weight).append(",");
                sb.append(mume.threshold).append(",");
                
                // Serialize the spectral vector as a space-separated string
                float[] vector = mume.getFloatVector();
                for (int i = 0; i < vector.length; i++) {
                    sb.append(vector[i]);
                    if (i < vector.length - 1) sb.append(" ");
                }
                writer.println(sb.toString());
            }
        }
    }

    /**
     * Loads a dataset back into the library.
     */
    public void loadFromDataset(String filePath) throws IOException {
        clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 6) continue;
                
                int index = Integer.parseInt(parts[0]);
                String id = parts[1];
                float freq = Float.parseFloat(parts[2]);
                float amp = Float.parseFloat(parts[3]);
                float weight = Float.parseFloat(parts[4]);
                float threshold = Float.parseFloat(parts[5]);
                
                com.cycling74.max.Atom[] spectral = new com.cycling74.max.Atom[0];
                if (parts.length > 6) {
                    String[] vectorParts = parts[6].split(" ");
                    spectral = new com.cycling74.max.Atom[vectorParts.length];
                    for (int i = 0; i < vectorParts.length; i++) {
                        spectral[i] = com.cycling74.max.Atom.newAtom(Float.parseFloat(vectorParts[i]));
                    }
                }
                
                add(new MumeData(index, id, freq, amp, weight, threshold, spectral));
            }
        }
    }
}
