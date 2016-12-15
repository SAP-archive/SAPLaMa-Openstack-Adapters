package com.sap.lvm.virtual.openstack;


import java.util.ArrayList;
import java.util.List;

import com.sap.lvm.CloudClientException;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.virtualization.adapter.api.base.IVirtOpContext;
import com.sap.tc.vcm.virtualization.adapter.api.base.IVirtOpResponsePayload;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpResponse;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpSyncResponse;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOperationId;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpResponse.VirtLogMessage;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpResponse.VirtOperationStatus;
import com.sap.tc.vcm.virtualization.adapter.api.operation.VirtDefaultOperation;
import com.sap.tc.vcm.virtualization.adapter.api.operation.async.ExecuteOperationResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.sync.ExecuteOperationSyncResponse;
import com.sap.tc.vcm.virtualization.adapter.api.types.LegacyVirtEntityType;


public class OpenStackUtil {
	
	public static VirtOpResponse<ExecuteOperationResponse> createVirtOperationResponse(String instanceId, VirtDefaultOperation op, ExecuteOperationResponse payLoad) {

		VirtOpResponse<ExecuteOperationResponse> response = new VirtOpResponse<ExecuteOperationResponse>();
		IVirtOpContext context = new OpenStackVirtOpContext(op.name(), instanceId);
		((OpenStackVirtOpContext) context).setEntityType(LegacyVirtEntityType.VIRTUAL_HOST.name());
		response.setContext(context);
		VirtOperationId id = new VirtOperationId();
		id.id = op.name();
		id.type = op.name();
		response.setId(id);
		response.setPayload(payLoad);
		response.setPercentCompleted(0);
		response.setStatus(VirtOperationStatus.INITIAL);
		response.setLogMessages(new ArrayList<VirtLogMessage>());
		return response;

	}
	
	public static VirtOpResponse<ExecuteOperationResponse> createVirtOperationResponseWithContext(String instanceId, VirtDefaultOperation op, ExecuteOperationResponse payLoad, IVirtOpContext context) {

		VirtOpResponse<ExecuteOperationResponse> response = new VirtOpResponse<ExecuteOperationResponse>();
		response.setContext(context);
		VirtOperationId id = new VirtOperationId();
		id.id = op.name();
		id.type = op.name();
		response.setId(id);
		response.setPayload(payLoad);
		response.setPercentCompleted(0);
		response.setStatus(VirtOperationStatus.INITIAL);
		response.setLogMessages(new ArrayList<VirtLogMessage>());
		return response;

	}
	
	public static <P extends IVirtOpResponsePayload> VirtOpResponse<P> createFailedResponseWithException(Class<P> payloadClass, CloudClientException e) {

		VirtLogMessage logMmessage = new VirtLogMessage(IJavaEeLog.SEVERITY_ERROR, "OpenStack", System.currentTimeMillis(), e.getMessage() + "\n" + "Caused by: " + e.getCause() + ".");
		List<VirtLogMessage> logMessages = new ArrayList<VirtLogMessage>();
		logMessages.add(logMmessage);
		return new VirtOpResponse<P>(null, VirtOperationStatus.FAILED, logMessages);
	}
	
	public static <P extends IVirtOpResponsePayload> VirtOpSyncResponse<P> createFailedSynchResponseWithException(Class<P> payloadClass, CloudClientException e) {

		VirtLogMessage logMmessage = new VirtLogMessage(IJavaEeLog.SEVERITY_ERROR, OpenStackConstants.OpenStack_VIRTUALIZATION_ADAPTER, System.currentTimeMillis(), e.getMessage() + "\n" + "Caused by: " + e.getCause() + ".");
		List<VirtLogMessage> logMessages = new ArrayList<VirtLogMessage>();
		logMessages.add(logMmessage);
		return new VirtOpSyncResponse<P>(logMessages);
	}
	
	public static VirtOpSyncResponse<ExecuteOperationSyncResponse> createSynchVirtOperationResponse(VirtDefaultOperation op, ExecuteOperationSyncResponse payLoad) {

		VirtOperationId id = new VirtOperationId();
		id.id = op.name();
		id.type = op.name();
		List<VirtLogMessage> logMessages = new ArrayList<VirtLogMessage>();

		VirtOpSyncResponse<ExecuteOperationSyncResponse> response = new VirtOpSyncResponse<ExecuteOperationSyncResponse>(payLoad, logMessages);

		response.setId(id);
		response.setPercentCompleted(100);
		response.setLogMessages(new ArrayList<VirtLogMessage>());
		return response;

	}
}
