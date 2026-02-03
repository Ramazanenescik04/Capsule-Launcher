package net.capsule.update;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URI;

import javax.swing.JPanel;

import net.capsule.update.util.Util;

public class ImagePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private BufferedImage img = Util.getImageWeb(URI.create("http://capsule.net.tr/WebB.png"));
	
	@Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (img == null) return;

        Graphics2D g2d = (Graphics2D) g;

        // Kaliteli render ayarları
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                			 RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                             RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);

        int panelWidth = getWidth();
        
        if (panelWidth <= 0 || getHeight() <= 0) {
            return; // layout hazır değil
        }

        // Oran korunur
        double scale = (double) panelWidth / img.getWidth();
        int newHeight = (int) (img.getHeight() * scale);

        g2d.drawImage(
        		img,
                0, 0,
                panelWidth, newHeight,
                null
        );
	}
}