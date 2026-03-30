package com.neume;

import javax.swing.*;
import java.awt.*;

/**
 * AudioLevelMeter: A simple graphical meter to visualize audio levels.
 */
public class AudioLevelMeter extends JPanel {
    private float level = 0.0f; // 0.0 to 1.0

    public AudioLevelMeter() {
        setPreferredSize(new Dimension(20, 100));
        setBackground(Color.DARK_GRAY);
    }

    public void setLevel(float level) {
        this.level = Math.min(1.0f, Math.max(0.0f, level));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        int fillH = (int) (level * h);

        // Draw meter
        if (level > 0.8f) g.setColor(Color.RED);
        else if (level > 0.5f) g.setColor(Color.YELLOW);
        else g.setColor(Color.GREEN);

        g.fillRect(0, h - fillH, w, fillH);
        
        // Draw border
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, w - 1, h - 1);
    }
}
