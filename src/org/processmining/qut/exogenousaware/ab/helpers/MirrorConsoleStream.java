package org.processmining.qut.exogenousaware.ab.helpers;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.commons.io.output.TeeOutputStream;

public class MirrorConsoleStream {

	private OutputStream splitter;
	private PrintStream oldSysOut;
	
	
	
	public PrintStream create(String debugPath) throws Throwable{
		this.oldSysOut = System.out;
		this.splitter = new TeeOutputStream(this.oldSysOut,
				new PrintStream(
						new BufferedOutputStream(new FileOutputStream(debugPath))));
		System.setOut(new PrintStream(this.splitter));
		return this.oldSysOut;
	}
	
	public void close() throws Throwable{
		System.setOut(this.oldSysOut);
		this.splitter.flush();
		this.splitter.close();
	}
	
	public void flush() throws Throwable {
		this.splitter.flush();
	}
	
}
