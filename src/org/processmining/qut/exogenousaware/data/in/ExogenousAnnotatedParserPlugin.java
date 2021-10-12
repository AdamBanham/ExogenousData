package org.processmining.qut.exogenousaware.data.in;

import java.io.File;

import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;

@Plugin(name = "Import Exogenous Annotated Log", parameterLabels = { "File" }, returnLabels = { "Exogenous Annotated Log" }, returnTypes = { ExogenousAnnotatedLog.class })
@UIImportPlugin(description = "Exogenous Annotated Log", extensions = { "eaxes" })
public class ExogenousAnnotatedParserPlugin {

	@PluginVariant(requiredParameterLabels = { 0 })
	public ExogenousAnnotatedLog parse(UIPluginContext context, File file) throws Exception {
		return ExogenousAnnotatedParser.parse(file).setup(context);
	}
}
