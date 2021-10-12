package org.processmining.qut.exogenousaware.ab.helpers;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Executor;

import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.RecursiveCallException;
import org.processmining.framework.plugin.events.Logger.MessageLevel;
import org.processmining.framework.plugin.events.PluginLifeCycleEventListener.List;
import org.processmining.framework.plugin.events.ProgressEventListener.ListenerList;
import org.processmining.framework.plugin.impl.FieldSetException;
import org.processmining.framework.providedobjects.ProvidedObjectManager;
import org.processmining.framework.util.Pair;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.connections.ConnectionManager;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.PluginContextID;
import org.processmining.framework.plugin.PluginDescriptor;
import org.processmining.framework.plugin.PluginExecutionResult;
import org.processmining.framework.plugin.PluginManager;
import org.processmining.framework.plugin.PluginParameterBinding;
import org.processmining.framework.plugin.ProMFuture;


public class progressListener implements Progress, PluginContext {
	
	private int value = 0;
	private int total = 0;
	private long start = new Date().getTime();
	
	@Override
	public void setMinimum(int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMaximum(int value) {
		// TODO Auto-generated method stub
		this.total = value;
		
	}

	@Override
	public void setValue(int value) {
		// TODO Auto-generated method stub
		this.value = value;
		
	}

	@Override
	public void setCaption(String message) {
		// TODO Auto-generated method stub
		System.out.println("[Progress Info] "+message);
	}

	@Override
	public String getCaption() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getValue() {
		// TODO Auto-generated method stub
		return this.value;
	}

	@Override
	public void inc() {
		// TODO Auto-generated method stub
		this.value++;
		if (this.value % 10 == 0) {
			System.out.println("[Progress] Completed "+ this.value+"/"+ this.total + " jobs.");
			System.out.println("[Progress] Current Runtime: "+
					(new Date().getTime() - this.start) 
					+ " ms.");
		}
		
	}

	@Override
	public void setIndeterminate(boolean makeIndeterminate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isIndeterminate() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getMinimum() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaximum() {
		// TODO Auto-generated method stub
		return this.total;
	}

	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public PluginManager getPluginManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProvidedObjectManager getProvidedObjectManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConnectionManager getConnectionManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginContextID createNewPluginContextID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void invokePlugin(PluginDescriptor plugin, int index, Object... objects) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invokeBinding(PluginParameterBinding binding, Object... objects) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Class<? extends PluginContext> getPluginContextType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T, C extends Connection> Collection<T> tryToFindOrConstructAllObjects(Class<T> type,
			Class<C> connectionType, String role, Object... input) throws ConnectionCannotBeObtained {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T, C extends Connection> T tryToFindOrConstructFirstObject(Class<T> type, Class<C> connectionType,
			String role, Object... input) throws ConnectionCannotBeObtained {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T, C extends Connection> T tryToFindOrConstructFirstNamedObject(Class<T> type, String name,
			Class<C> connectionType, String role, Object... input) throws ConnectionCannotBeObtained {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginContext createChildContext(String label) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Progress getProgress() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public ListenerList getProgressEventListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getPluginLifeCycleEventListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginContextID getID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Pair<PluginDescriptor, Integer> getPluginDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginContext getParentContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public java.util.List<PluginContext> getChildContexts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginExecutionResult getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ProMFuture<?> getFutureResult(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Executor getExecutor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isDistantChildOf(PluginContext context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFuture(PluginExecutionResult resultToBe) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setPluginDescriptor(PluginDescriptor descriptor, int methodIndex)
			throws FieldSetException, RecursiveCallException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasPluginDescriptorInPath(PluginDescriptor descriptor, int methodIndex) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void log(String message, MessageLevel level) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void log(String message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void log(Throwable exception) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public org.processmining.framework.plugin.events.Logger.ListenerList getLoggingListeners() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PluginContext getRootContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteChild(PluginContext child) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T extends Connection> T addConnection(T c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

}
