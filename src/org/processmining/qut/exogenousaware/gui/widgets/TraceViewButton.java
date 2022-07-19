package org.processmining.qut.exogenousaware.gui.widgets;

import java.awt.Color;
import java.awt.Dimension;

import com.fluxicon.slickerbox.components.SlickerButton;

public class TraceViewButton extends SlickerButton{
	
	Color ACTIVE = new Color(45,164,78,255);
	Color UNACTIVE = Color.darkGray;
	private boolean active = false;
	Dimension preferred;
	
	public TraceViewButton(String text) {
		super(text);
		this.COLOR_BG = UNACTIVE;
		this.COLOR_HILIGHT_MOUSEOVER = UNACTIVE;
		this.COLOR_BG_MOUSEOVER = ACTIVE;
		this.initialize();
		int width = (this.width + 15);
		int height = (this.height);
		preferred = new Dimension(width, height);
	}
	
	public void setActive(boolean active) {
		this.active = active;
		if (this.active) {
			this.COLOR_BG = ACTIVE;
			this.COLOR_BG_ACTIVE = ACTIVE;
			this.COLOR_BG_DISABLED = ACTIVE;
			this.COLOR_BG_FOCUS = ACTIVE;
			this.width += 15;
		} else {
			this.COLOR_BG = UNACTIVE;
			this.COLOR_BG_ACTIVE = UNACTIVE;
			this.COLOR_BG_DISABLED = UNACTIVE;
			this.COLOR_BG_FOCUS = UNACTIVE;
			this.width -= 15;
		}
		this.initialize();
	}
	
	@Override
	public int getWidth() {
		return (int) preferred.getWidth() -15;
	}
	
	@Override
	public int getHeight() {
		return (int) preferred.getHeight();
	}
	
	
}
