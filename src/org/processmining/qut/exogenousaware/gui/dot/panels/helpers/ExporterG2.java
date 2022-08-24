package org.processmining.qut.exogenousaware.gui.dot.panels.helpers;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.processmining.qut.exogenousaware.gui.dot.panels.NavigableSVGPanelG2;


public abstract class ExporterG2 extends FileFilter {
	protected abstract String getExtension();

	public abstract void export(NavigableSVGPanelG2 panel, File file) throws Exception;

	public String getDescription() {
		return getExtension();
	}
	
	public File addExtension(File file) {
		if (!file.getName().endsWith("." + getExtension())) {
			return new File(file + "." + getExtension());
		}
		return file;
	}

	@Override
	public boolean accept(final File file) {
		String extension = "";
		int i = file.getName().lastIndexOf('.');
		if (i >= 0) {
			extension = file.getName().substring(i + 1);
		}
		return file.isDirectory() || extension.toLowerCase().equals(getExtension());
	}
}
