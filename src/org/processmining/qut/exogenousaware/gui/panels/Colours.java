package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Colours {
	private Colours() {};
	
	public static Color CHART_BACKGROUND = new Color(233,233,233);
	
	
//	colour palette for graphs showing groups
	public static Color GRAPH_DARK_BLUE = new Color(0,63,92,255);
	public static Color GRAPH_NAVY_BLUE = new Color(55,76,128);
	public static Color GRAPH_PURPLE = new Color(122,81,149);
	public static Color GRAPH_BLUE_RED = new Color(188,80,144);
	public static Color GRAPH_LIGHT_RED = new Color(239,86,177);
	public static Color GRAPH_DARK_ORANGE = new Color(255,118,74);
	public static Color GRAPH_GOLD = new Color(255,166,0,255);
	
	public static final List<Color> graphPalette = new ArrayList<Color>(){{
		add(GRAPH_DARK_BLUE);
		add(GRAPH_NAVY_BLUE);
		add(GRAPH_PURPLE);
		add(GRAPH_BLUE_RED);
		add(GRAPH_LIGHT_RED);
		add(GRAPH_DARK_ORANGE);
		add(GRAPH_GOLD);
	}};
	
	public static Color getGraphPaletteColour(int pos) {
		if (Math.abs(pos) > 7 | pos == 0) {
			throw new IllegalArgumentException("The graph palette only has 7 colours, pos must be between [-7,7] not including '0'");
		}
		
		if(pos < 0) {
			pos = graphPalette.size() - Math.abs(pos);
			return graphPalette.get(pos);
		} else {
			return graphPalette.get(pos - 1);
		}
	}
}
