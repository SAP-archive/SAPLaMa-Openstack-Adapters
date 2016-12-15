package com.sap.lvm.storage.openstack.util; 

import java.util.ArrayList;
import java.util.List;



import com.sap.lvm.util.MiscUtil;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.storage.adapter.api.base.response.IStorageOperationResponsePayload;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageLogMessage;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageOperationStatus;

@SuppressWarnings("nls")
public class StorageAdapterImplHelper {
	
	public static <P extends IStorageOperationResponsePayload> StorageOperationResponse<P> createFailedResponse(String msg, Class<P> payloadClass) {
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
		logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "SMI-S", System.currentTimeMillis(), msg));
		return createFailedResponse(logMessages, payloadClass);
	}

	public static <P extends IStorageOperationResponsePayload> StorageOperationResponse<P> createFailedResponse(List<StorageLogMessage> logMessages, Class<P> payloadClass) {
		return new StorageOperationResponse<P>(null, StorageOperationStatus.FAILED, logMessages);
	}
	
	public static StorageAdapterException getStorageAdapterExceptionFromLogMessages(List<StorageLogMessage> logMessages) {
		StorageLogMessage message = logMessages.iterator().next();
		logMessages.remove(0);
		return getStorageAdapterExceptionFromLogMessages(message, logMessages);
	}
	
	private static StorageAdapterException getStorageAdapterExceptionFromLogMessages(StorageLogMessage message, List<StorageLogMessage> causes) {
		if (MiscUtil.nullOrEmpty(causes))
			return new StorageAdapterException(message.message);
		else {
			StorageLogMessage nextMessage = causes.iterator().next();
			causes.remove(0);
			return new StorageAdapterException(message.message, getStorageAdapterExceptionFromLogMessages(nextMessage, causes));
		}
	}
	
}
