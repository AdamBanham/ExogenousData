package org.processmining.qut.exogenousaware.data.out;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.deckfour.spex.SXDocument;
import org.deckfour.spex.SXTag;
import org.deckfour.xes.classification.XEventAttributeClassifier;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.logging.XLogging;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XesXmlSerializer;
import org.deckfour.xes.util.XRuntimeUtils;
import org.deckfour.xes.util.XTokenHelper;
import org.processmining.qut.exogenousaware.data.ExogenousAnnotatedLog;

public class AnnotatedSerializer extends XesXmlSerializer{
	
	@Override
	public String getDescription() {
		return "Exogenous Annotated XES Event Log";
	}
	
	@Override
	public String getName() {
		return "EAXES XML";
	}
	
	@Override
	public String[] getSuffices() {
		return new String[] { "eaxes" };
	}

	public void serialize(ExogenousAnnotatedLog source, OutputStream out) throws IOException {
		XLogging.log("start serializing log to EAXES.XML",
				XLogging.Importance.DEBUG);
		long start = System.currentTimeMillis();
		SXDocument doc = new SXDocument(out);
		doc.addComment("This file has been generated with the OpenXES library. It conforms");
		doc.addComment("to the XML serialization of the XES standard for log storage and");
		doc.addComment("management.");
		doc.addComment("XES standard version: " + XRuntimeUtils.XES_VERSION);
		doc.addComment("OpenXES library version: "
				+ XRuntimeUtils.OPENXES_VERSION);
		doc.addComment("OpenXES is available from http://www.openxes.org/");
		SXTag logTag = doc.addNode("log");
		logTag.addAttribute("xes.version", XRuntimeUtils.XES_VERSION);
		logTag.addAttribute("xes.features", "nested-attributes");
		logTag.addAttribute("openxes.version", XRuntimeUtils.OPENXES_VERSION);
//		logTag.addAttribute("xmlns", "http://www.xes-standard.org/");
		// define extensions
		for (XExtension extension : source.getEndogenousLog().getExtensions()) {
			SXTag extensionTag = logTag.addChildNode("extension");
			extensionTag.addAttribute("name", extension.getName());
			extensionTag.addAttribute("prefix", extension.getPrefix());
			extensionTag.addAttribute("uri", extension.getUri().toString());
		}
		// define global attributes
		addGlobalAttributes(logTag, "trace", source.getEndogenousLog().getGlobalTraceAttributes());
		addGlobalAttributes(logTag, "event", source.getEndogenousLog().getGlobalEventAttributes());
		// define classifiers
		for (XEventClassifier classifier : source.getEndogenousLog().getClassifiers()) {
			if (classifier instanceof XEventAttributeClassifier) {
				XEventAttributeClassifier attrClass = (XEventAttributeClassifier) classifier;
				SXTag clsTag = logTag.addChildNode("classifier");
				clsTag.addAttribute("name", attrClass.name());
				clsTag.addAttribute("keys", XTokenHelper
						.formatTokenString((List<String>) Arrays
								.asList(attrClass.getDefiningAttributeKeys())));
			}
		}
		// add log attributes
		addAttributes(logTag, source.getEndogenousLog().getAttributes().values());
		for (XTrace trace : source.getEndogenousLog()) {
			SXTag traceTag = logTag.addChildNode("trace");
			addAttributes(traceTag, trace.getAttributes().values());
			for (XEvent event : trace) {
				SXTag eventTag = traceTag.addChildNode("event");
				addAttributes(eventTag, event.getAttributes().values());
			}
		}
		// add new log-like element for exogenous log
		SXTag exoTag = logTag.addChildNode("exogenousUniverse");
		exoTag.addAttribute("xes.version", XRuntimeUtils.XES_VERSION);
		exoTag.addAttribute("xes.features", "nested-attributes");
		exoTag.addAttribute("openxes.version", XRuntimeUtils.OPENXES_VERSION);
		// define extensions
		for (XExtension extension : source.getExoSubseries().getExtensions()) {
			SXTag extensionTag = exoTag.addChildNode("extension");
			extensionTag.addAttribute("name", extension.getName());
			extensionTag.addAttribute("prefix", extension.getPrefix());
			extensionTag.addAttribute("uri", extension.getUri().toString());
		}
		// define global attributes
		addGlobalAttributes(exoTag, "trace", source.getExoSubseries().getGlobalTraceAttributes());
		addGlobalAttributes(exoTag, "event", source.getExoSubseries().getGlobalEventAttributes());
		// define classifiers
		for (XEventClassifier classifier : source.getExoSubseries().getClassifiers()) {
			if (classifier instanceof XEventAttributeClassifier) {
				XEventAttributeClassifier attrClass = (XEventAttributeClassifier) classifier;
				SXTag clsTag = exoTag.addChildNode("classifier");
				clsTag.addAttribute("name", attrClass.name());
				clsTag.addAttribute("keys", XTokenHelper
						.formatTokenString((List<String>) Arrays
								.asList(attrClass.getDefiningAttributeKeys())));
			}
		}
		// add log attributes
		addAttributes(exoTag, source.getExoSubseries().getAttributes().values());
		for (XTrace trace : source.getExoSubseries()) {
			SXTag traceTag = exoTag.addChildNode("trace");
			addAttributes(traceTag, trace.getAttributes().values());
			for (XEvent event : trace) {
				SXTag eventTag = traceTag.addChildNode("event");
				addAttributes(eventTag, event.getAttributes().values());
			}
		}
		//
		doc.close();
		String duration = " (" + (System.currentTimeMillis() - start)
				+ " msec.)";
		XLogging.log("finished serializing exogenous annotated log" + duration,
				XLogging.Importance.DEBUG);
		
	}

	
	
}
