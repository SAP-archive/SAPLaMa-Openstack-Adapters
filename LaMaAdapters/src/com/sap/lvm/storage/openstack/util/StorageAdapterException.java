package com.sap.lvm.storage.openstack.util;
 
import java.util.ArrayList;
import java.util.List;


import com.sap.tc.vcm.infrastructure.api.adapter.InfrastructAdapterException;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageLogMessage;

public class StorageAdapterException extends InfrastructAdapterException {

	private static final long serialVersionUID = 1L;
	
	private List<StorageLogMessage> storageLogMessages;
	
	public StorageAdapterException(String message, List<StorageLogMessage> logMessages, Throwable cause) {
		super(message, cause);
		setLogMessages(message, logMessages);
	}
	
	public StorageAdapterException(String message, List<StorageLogMessage> logMessages) {
		super(message);
		setLogMessages(message, logMessages);
	}
	
	public StorageAdapterException(String message, Throwable cause) {
		super(message, cause);
		setLogMessages(message, null);
	}
	
	public StorageAdapterException(String message) {
		super(message);
		setLogMessages(message, null);
	}
	
	public StorageAdapterException(Throwable cause) {
		super(cause);
		setLogMessages(cause.toString(), null);
	}
	
	
	private void setLogMessages(String message, List<StorageLogMessage> logMessages) {
		if (logMessages!=null)
			this.storageLogMessages = logMessages;
		else
			this.storageLogMessages = new ArrayList<StorageLogMessage>(1);
		storageLogMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "LVM", System.currentTimeMillis(), message));
	}

	public List<StorageLogMessage> getStorageLogMessages() {
		return storageLogMessages;
	}

}
