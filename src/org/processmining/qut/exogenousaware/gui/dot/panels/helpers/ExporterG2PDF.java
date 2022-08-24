package org.processmining.qut.exogenousaware.gui.dot.panels.helpers;

import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;

import org.processmining.qut.exogenousaware.gui.dot.panels.NavigableSVGPanelG2;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;

public class ExporterG2PDF extends ExporterG2 {

	protected String getExtension() {
		return "pdf";
	}

	public void export(NavigableSVGPanelG2 panel, File file) throws Exception {
		
		System.out.println("[DotExporter] Exporting graph...");
		float width = (float) panel.getImage().getViewRect().getWidth();
		float height = (float) panel.getImage().getViewRect().getHeight();

		Document document = new Document(new Rectangle(width, height), 0, 0, 0, 0);
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
		document.open();
		PdfContentByte canvas = writer.getDirectContent();
		
		Graphics2D g = new PdfGraphics2D(canvas, width, height);
		panel.print(g);
		g.dispose();
		
		document.close();
		System.out.println("[DotExporter] Exported graph to :: " + file.getName());
	}

}
