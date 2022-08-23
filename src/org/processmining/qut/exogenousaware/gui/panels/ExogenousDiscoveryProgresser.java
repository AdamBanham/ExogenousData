package org.processmining.qut.exogenousaware.gui.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Setter;

@Builder
public class ExogenousDiscoveryProgresser extends JPanel {
	
	@Default private List<ProgressState> states = new ArrayList();
	@Default private ProgressBarUpdater updater = null;
	
	public ExogenousDiscoveryProgresser setup() {
		
		setPreferredSize(new Dimension(1000,100));
		setOpaque(false);
		
		StateChangeListener listener = new StateChangeListener(this);
		
		
//		create constraints
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = c.BOTH;
		c.anchor = c.WEST;
		c.weightx = 3.0;
		c.weighty = 1.0;
		c.insets = new Insets(0,0,0,3);
		setLayout(new GridBagLayout());
//		build sets
//		add alignment section
		c.gridwidth = 2;
		JComponent box = new JPanel();
//		create state
		ProgressState state = ProgressType.Alignment.createState(0);
		state.setListener(listener);
		states.add(state);
		state.setController(box);
		add(box, c);
		c.gridx = 2;
		c.gridwidth = 1;
//		add stat section
		box = new JPanel();
		state = ProgressType.Stats.createState(0);
		state.setListener(listener);
		state.setController(box);
		states.add(state);
		c.weightx = 1.0;
		add(box, c);
		c.gridx++;
//		add decision mining section
		box = new JPanel();
		state = ProgressType.Investigation.createState(0);
		state.setListener(listener);
		state.setController(box);
		c.weightx = 2.0;
		states.add(state);
		add(box, c);
		c.gridx++;
//		add measurement section
		box = new JPanel();
		state = ProgressType.Measurements.createState(0);
		state.setListener(listener);
		state.setController(box);
		states.add(state);
		c.weightx = 2.0;
		c.insets = new Insets(0,0,0,0);
		add(box, c);
		c.gridx++;
		
		
//		perform layout
		this.validate();		
		
		return this;
	}
	
	
	@Override
	public void validate() {
		super.validate();
		for(ProgressState state: states) {
			state.validate();
		}
	}
	
	public boolean update() {
		boolean repeat = false; 
		for(ProgressState state: states) {
			repeat = repeat | state.update();
		}
		return repeat;
	}
	
	public ProgressState getState(ProgressType type){
		for(ProgressState state: states) {
			if (state.getType().equals(type)) {
				return state;
			}
		}
		return null;
	}
	
	public void triggerUpdateThread() {
		if (this.updater != null) {
			if (!this.updater.isDone()) {
				this.updater.setCancelled(true);
			} 
			this.updater = new ProgressBarUpdater(this);
			this.updater.start();
		} else {
			this.updater = new ProgressBarUpdater(this);
			this.updater.start();
		}
	}
	
	
	public static class ProgressState {
		
//		internal states
		@Setter @Getter private int progress;
		private float step;
		@Setter private float current;
		@Setter private float total;
		@Getter private ProgressType type;
		
//		gui widgets
		private JComponent controller;
		private Component progressbar;
		private Component filler;
		private JLabel caption;
		
//		listeners
		@Setter private StateListener listener;
		
		
		public ProgressState(int progress, ProgressType type) {
			this.progress = progress;
			this.type = type;
			this.total = 100f;
			this.step = progress/100.0f;
			this.current = 0.00f;
			
			this.caption = new JLabel(type.name);
			this.caption.setForeground(Color.white);
		}
		
		private float[] computeWeights() {
			float[] weights = new float[2];
			
			weights[0] = (current/total);
			weights[1] = 1 - (current/total);
			
			return weights;
		}
		
		public boolean update() {
			boolean changed = step();
			if (progress > 0) {
				progressbar.setBackground(Color.blue.darker());
			} else {
				progressbar.setBackground(null);
			}
			
			this.caption.setText(this.type.name + String.format("(%.1f %%)", (current/total) * 100.0f) );
			
			if (changed) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						
						public void run() {
							// TODO Auto-generated method stub
							validate();
						}
					});
				} catch (InvocationTargetException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return true;
			}
			
			
			return false;
		}
		
		public boolean step() {
			if (Math.abs(current - progress) > 0.001f) {
				if (current < progress) {
					current = Math.min(this.current + this.step, progress);
				} else {
					current = Math.max(this.current + this.step, progress);
				}
				return true;
			}
			return false;
		}
		
		public void setController(JComponent box) {
			this.controller = box;
			controller.setBackground(Color.DARK_GRAY);
			controller.setLayout(new GridBagLayout());
			controller.removeAll();
			
			filler = new JPanel();
			filler.setBackground(Color.LIGHT_GRAY);
			
			float[] weights = computeWeights();
			
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = weights[0];
			c.weighty = 1.0;
			c.gridx = 0;
			c.gridy = 0;
			
			
			progressbar = new JPanel();
			progressbar.setBackground(Color.blue);
			if (weights[0] < 0.01) {
				c.fill = c.NONE;
			} else {
				c.fill = c.BOTH;
				controller.add(progressbar, c);
			}
			
			
			c.weightx = weights[1];
			c.fill = c.BOTH;
			c.gridx++;
			
			controller.add(filler, c);
			
			c.gridy = 1;
			c.gridx = 0;
			c.gridwidth = 2;
			c.anchor = c.CENTER;
			c.fill = c.NONE;
			c.weightx = 1.0;
			
			controller.add(caption, c);
			
			
			controller.validate();
		}
		
		public JComponent getController() {
			return controller;
		}
		
		public void validate() {
			controller.removeAll();
			
			float[] weights = computeWeights();
			
			GridBagConstraints c = new GridBagConstraints();
			c.weightx = weights[0];
			c.fill = c.BOTH;
			c.weighty = 1.0;
			c.gridx = 0;
			c.gridy = 0;
			
			if (weights[0] > 0.01) {
				controller.add(progressbar, c);
			} 			
			
			c.weightx = weights[1];
			c.gridx++;
			
			if (weights[1] > 0.01) {
				controller.add(filler, c);
			}
			
			c.gridy = 1;
			c.gridx = 0;
			c.gridwidth = 2;
			c.anchor = c.CENTER;
			c.fill = c.NONE;
			c.weightx = 1.0;
			
			controller.add(caption, c);
			
			controller.validate();
		}
		
		public void increment() {
			this.progress++;
			float diff = progress - current;
			this.step = diff / 100.0f;
			this.update();
			if (this.listener != null) {
				listener.incremented();
			}
			
		}
		
		public void increment(int inc) {
			this.progress = this.progress + inc;
			float diff = progress - current;
			this.step = diff / 100.0f;
			this.update();
			if (this.listener != null) {
				listener.incremented();
			}
		}
		
		public void reset() {
			this.current = 0f;
			this.step = 0f;
			this.total = 100;
			this.progress = 0;
			this.caption.setText("");
			this.update();
			this.validate();
		}
		
		public void setCaption(String caption) {
			this.caption.setText(caption);
		}
	}
	
	private interface StateListener {
		public void incremented();
	}
	
	public class StateChangeListener implements StateListener {
		
		private ExogenousDiscoveryProgresser host;
		
		public StateChangeListener(ExogenousDiscoveryProgresser host) {
			this.host = host;
		}

		public void incremented() {
			host.triggerUpdateThread();
		}
		
	}
	
	public static enum ProgressType {
		Alignment("Alignment Precompute"),
		Stats("Statistics"),
		Investigation("Decision Mining"),
		Measurements("Decision Point Measurement");
		
		@Getter private String name;
		
		private ProgressType(String name) {
			this.name = name;
		}
		
		public ProgressState createState(int progress) {
			return new ProgressState(progress, this);
		}
		
	}
	
	private class ProgressBarUpdater extends Thread {
		
		private ExogenousDiscoveryProgresser host;
		@Getter private boolean done = false;
		@Setter @Getter private boolean cancelled = false;
		
		public ProgressBarUpdater(ExogenousDiscoveryProgresser host) {
			this.host = host;
		}

		public void run() {
			
			while(!host.isShowing()) {
				if (isCancelled()) {
					return;
				}
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					
				}
			}
			
			if (isCancelled()) {
				return;
			}
			
			while(host.update()) {
				if (isCancelled()) {
					return;
				}
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) {
					
				}
			}
			
			if (isCancelled()) {
				return;
			}
			
			done = true;
			return;
			
		}
		
	}


}
