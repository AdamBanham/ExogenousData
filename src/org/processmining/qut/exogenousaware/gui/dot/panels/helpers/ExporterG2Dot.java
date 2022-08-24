package org.processmining.qut.exogenousaware.gui.dot.panels.helpers;

import java.io.File;
import java.io.PrintWriter;

import org.processmining.qut.exogenousaware.gui.dot.panels.DotPanelG2;
import org.processmining.qut.exogenousaware.gui.dot.panels.NavigableSVGPanelG2;

public class ExporterG2Dot extends ExporterG2 {
	
	@Override
	public String getDescription() {
		return "dot (graph structure)";
	};

	protected String getExtension() {
		return "dot";
	}

	public void export(NavigableSVGPanelG2 panel, File file) throws Exception {
		
		if (panel instanceof DotPanelG2) {
			System.out.println("[DotExporter] exporting graph...");
			PrintWriter writer = new PrintWriter(file);
			writer.print(((DotPanelG2) panel).getDot());
			writer.close();
			System.out.println("[DotExporter] exported graph to :: "+ file.getName());
		} else {
			System.out.println("[DotExporter] unable to export dot structure...");
		}

	}

}
