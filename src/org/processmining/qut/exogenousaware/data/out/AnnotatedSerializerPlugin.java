package org.processmining.qut.exogenousaware.data.out;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;

@Plugin(name = "Export Exogenous Annotated Log", parameterLabels = { "Exogenous Annotated Log", "File" }, returnLabels = {  }, returnTypes = {  }, userAccessible = true, level = PluginLevel.NightlyBuild)
@UIExportPlugin(extension ="eaxes", description ="Exogenous Annotated Log" )
public class AnnotatedSerializerPlugin {

	@PluginVariant(variantLabel = "Export Exogenous Annotated Log to XES-like file", requiredParameterLabels = { 0, 1 })
	public void serialize(PluginContext context, ExogenousAnnotatedLog source, File file) throws IOException {
		new AnnotatedSerializer().serialize(source, new FileOutputStream(file));
	}
}
