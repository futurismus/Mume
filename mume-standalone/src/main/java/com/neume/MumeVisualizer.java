package com.neume;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MumeVisualizer: A real-time histogram display for Mume spectral mass.
 */
public class MumeVisualizer extends JPanel {
    private MumeAnalyzer analyzer;
    private final Timer refreshTimer;

    public MumeVisualizer(MumeAnalyzer analyzer) {
        this.analyzer = analyzer;
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(500, 200));

        // Refresh at ~30 FPS
        refreshTimer = new Timer(33, e -> repaint());
        refreshTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (analyzer == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<FrequencyBin> bins = analyzer.getAllBins();
        if (bins.isEmpty()) return;

        int w = getWidth();
        int h = getHeight();
        float maxWeight = 0;
        float maxFreq = 5000; // Limit visualization to 5kHz for clarity

        // Find max weight for scaling
        for (FrequencyBin bin : bins) {
            if (bin.getWeight() > maxWeight) maxWeight = bin.getWeight();
        }

        g2.setColor(new Color(0, 255, 100, 150));
        
        for (FrequencyBin bin : bins) {
            if (bin.frequency > maxFreq) continue;

            // Map frequency to X (Linear for now)
            int x = (int) ((bin.frequency / maxFreq) * w);
            
            // Map weight to Height (Normalized)
            float normWeight = bin.getWeight() / (maxWeight + 0.0001f);
            int barHeight = (int) (normWeight * h);

            g2.fillRect(x - 1, h - barHeight, 2, barHeight);
        }

        // Draw scale info
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2.drawString("Max Mass: " + String.format("%.2f", maxWeight), 10, 20);
        g2.drawString("5kHz", w - 40, h - 5);
    }

    public void setAnalyzer(MumeAnalyzer analyzer) {
        this.analyzer = analyzer;
    }
}
