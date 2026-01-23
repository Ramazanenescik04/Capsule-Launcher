package net.capsule.update;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.net.URI;

import javax.swing.JPanel;

import net.capsule.update.util.Util;

public class ImagePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private BufferedImage img = Util.getImageWeb(URI.create("http://capsule.net.tr/WebB.png"));
	
	@Override
	protected void paintBorder(Graphics g) {
		super.paintBorder(g);
		
		g.setColor(Color.black);
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
		
		g.drawImage(img, 0, 0, this.getWidth(), this.getHeight(), null);
	}
}
