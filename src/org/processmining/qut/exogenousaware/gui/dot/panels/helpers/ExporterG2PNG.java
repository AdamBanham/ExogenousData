package org.processmining.qut.exogenousaware.gui.dot.panels.helpers;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.processmining.qut.exogenousaware.gui.dot.panels.NavigableSVGPanelG2;

public class ExporterG2PNG extends ExporterG2 {

	protected String getExtension() {
		return "png";
	}

	public void export(NavigableSVGPanelG2 panel, File file) throws Exception {
		
		System.out.println("[DotExporter] Exporting graph...");
		float width = (float) panel.getImage().getViewRect().getWidth();
		float height = (float) panel.getImage().getViewRect().getHeight();

		BufferedImage bi = new BufferedImage(Math.round(width), Math.round(height), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		panel.print(g);
		ImageIO.write(bi, "PNG", file);
		System.out.println("[DotExporter] Exported graph to :: " + file.getName());
	}

}
