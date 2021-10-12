package org.processmining.qut.exogenousaware.gui.listeners;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

import org.deckfour.xes.model.XTrace;
import org.processmining.qut.exogenousaware.gui.ExogenousTraceView;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class EndoTraceListener implements MouseListener{

	@NonNull JPanel clicked;
	@NonNull ExogenousTraceView source;
	@NonNull XTrace endo;
	
	@Override
	public void mouseClicked(MouseEvent e) {
		
		if (this.source.setSelectEndogenous(this.endo, this)) {
			this.highlightClicked();
		} else {
			this.resetClicked();
		}
		
	}
	
	public void resetClicked() {
		clicked.setBackground(Color.LIGHT_GRAY);
	}
	
	public void highlightClicked() {
		clicked.setBackground(Color.GREEN);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}
