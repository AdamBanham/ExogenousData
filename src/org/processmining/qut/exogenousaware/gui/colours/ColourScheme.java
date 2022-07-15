package org.processmining.qut.exogenousaware.gui.colours;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ColourScheme {
	
	
	public static Color red = new Color(250,69,73);
	public static Color purple = new Color(164,117,249);
	public static Color pink = new Color(232,90,173);
	public static Color coral = new Color(236,101,71);
	public static Color blue = new Color(33,139,255);
	public static Color green = new Color(45,164,78);
	public static Color yellow = new Color(191,135,0);
	public static Color orange = new Color(225,111,36);
	
	public static List<Color> getDarkerSampleSpace(Color c, int length){
		List<Color> returner = new ArrayList<>();

		
		// start at sample colour
		Color curr = new Color(c.getRed(),c.getGreen(),c.getBlue());
		float[] hsv = new float[3];
		Color.RGBtoHSB(curr.getRed(), curr.getGreen(), curr.getGreen(), hsv);
		returner.add(copyColour(curr));
		
		// make darker colours via HSB (B)
		// by making colours saturation higher
		// and decreasing brightness
		float factor = 0.15f;
		for(int i=0; i< length; i++) {
			hsv[1] = hsv[1] * (1.0f + factor);
			hsv[2] = hsv[2] * (1.0f - factor * 2);
			if (hsv[2] < 0.01f) {
				hsv[2] = 1.0f;
			}
			if (hsv[1] > 1.0f) {
				hsv[1] = 1.0f;
			}
			
			int swapper = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
			curr = new Color(swapper);
			returner.add(copyColour(curr));
		}
		
		return returner;
	}
	
	private static Color copyColour(Color c) {
		return new Color(c.getRed(),c.getGreen(), c.getBlue());
	}
	
	public static Color ligthenColour(Color c) {
		float[] hsv = new float[3];
		Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getGreen(), hsv);
		float mutplus = 1.01f;
		float mutminus = 0.95f;
		
		hsv[1] = hsv[1] * mutminus;
		hsv[2] = hsv[2] * mutplus;
		if (hsv[1] < 0.01f) {
			hsv[1] = 0.01f;
		}
		if (hsv[2] > 1.0f) {
			hsv[2] = 1.0f;
		}
		
		int swapper = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
		return copyColour(new Color(swapper));
	}
	
	private ColourScheme() {};
	
	

}
