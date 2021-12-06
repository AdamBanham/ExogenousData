package org.processmining.qut.exogenousaware.data;

public enum ExogenousDatasetType {
	DISCRETE("Discrete (String)"),
	NUMERICAL("Continous");
	
	private String label;
	
	private ExogenousDatasetType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return this.label;
	}
}
