package org.processmining.qut.exogenousaware.gui.dot.panels.helpers;

import java.awt.Dimension;
import java.io.File;
import java.util.Properties;

import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.processmining.qut.exogenousaware.gui.dot.panels.NavigableSVGPanelG2;

public class ExporterG2SVG extends ExporterG2 {

	protected String getExtension() {
		return "svg";
	}

	public void export(NavigableSVGPanelG2 panel, File file) throws Exception {
		
		System.out.println("[DotExporter] Exporting graph...");
		double width = panel.getImage().getViewRect().getWidth();
		double height = panel.getImage().getViewRect().getHeight();
		
		Dimension dimension = new Dimension((int) Math.ceil(width), (int) Math.ceil(height));
		VectorGraphics g = new SVGGraphics2D(file, dimension);
		Properties p = new Properties(SVGGraphics2D.getDefaultProperties());
		g.setProperties(p);
		g.startExport();
		panel.print(g);
		g.endExport();
		System.out.println("[DotExporter] Exported graph to :: " + file.getName());

	}

}
