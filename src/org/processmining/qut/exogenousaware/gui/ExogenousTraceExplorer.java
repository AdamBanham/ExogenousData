package org.processmining.qut.exogenousaware.gui;

import javax.swing.JComponent;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;

@Plugin(
		name="Exogenous Trace Explorer",
		level=PluginLevel.NightlyBuild,
		returnLabels= {"Exogenous Annotated Explorer UI"},
		returnTypes = { JComponent.class },
		userAccessible = true, 
		parameterLabels = { "" }
)
@Visualizer
public class ExogenousTraceExplorer {

	@PluginVariant(requiredParameterLabels = { 0 })
	public JComponent visualise(PluginContext pluginContext, ExogenousTraceView main) {
		return main.getComponent();
	}
}
