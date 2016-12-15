package com.sap.lvm.storage.openstack.file;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.model.manila.ShareSnapshot;
import org.openstack4j.model.manila.ShareSnapshot.Status;

import com.sap.lvm.CloudClientException;
import com.sap.lvm.storage.openstack.util.OpenstackAdapterUtil;
import com.sap.lvm.storage.openstack.util.StorageAdapterImplHelper;
import com.sap.tc.vcm.base.util.serialization.serializable.SerializableClass;
import com.sap.tc.vcm.base.util.serialization.serializable.SerializableField;
import com.sap.tc.vcm.infrastructure.api.adapter.InfrastructAdapterException;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.storage.adapter.api.base.IStorageOperationContext;
import com.sap.tc.vcm.storage.adapter.api.base.StorageOperationId;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageLogMessage;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageOperationStatus;
import com.sap.tc.vcm.storage.adapter.api.snapshot.DeleteSnapshotsRequest;
import com.sap.tc.vcm.storage.adapter.api.snapshot.DeleteSnapshotsResponse;
import com.sap.tc.vcm.storage.adapter.api.snapshot.IStorageSnapshot;
import com.sap.tc.vcm.storage.adapter.api.snapshot.ListSnapshotsRequest;
import com.sap.tc.vcm.storage.adapter.api.snapshot.ListSnapshotsResponse;
import com.sap.tc.vcm.storage.adapter.api.snapshot.PostProcessTakeSnapshotRequest;
import com.sap.tc.vcm.storage.adapter.api.snapshot.PostProcessTakeSnapshotResponse;
import com.sap.tc.vcm.storage.adapter.api.snapshot.PrepareTakeSnapshotRequest;
import com.sap.tc.vcm.storage.adapter.api.snapshot.PrepareTakeSnapshotResponse;
import com.sap.tc.vcm.storage.adapter.api.snapshot.TakeSnapshotRequest;
import com.sap.tc.vcm.storage.adapter.api.snapshot.TakeSnapshotResponse;
import com.sap.tc.vcm.storage.adapter.api.types.StorageSnapshotVolume;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolume;


public class OpenstackFileStorageSnapshot implements IStorageSnapshot {
	
	OpenstackFileCloudStorageController openstackClient = null;
	private IJavaEeLog logger;

    public OpenstackFileStorageSnapshot(OpenstackFileCloudStorageController openstackClient, IJavaEeLog logger) {
		this.openstackClient = openstackClient;
		this.logger = logger;
	}

	@Override
	public StorageOperationResponse<DeleteSnapshotsResponse> deleteSnapshots(DeleteSnapshotsRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "deleteSnapshots: snapshots:" +request.snapshotsToBeDeleted , null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
	    List<StorageSnapshotVolume> storageSnapshots = request.snapshotsToBeDeleted;
	    for (StorageSnapshotVolume snapshotVolume : storageSnapshots) {
			try {
				openstackClient.deleteSnapshot(snapshotVolume.storageVolumeId);
			} catch (CloudClientException e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "deleteSnapshots: " +e.getMessage(), null, e);
				logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));	
		    }
		}
		if (logMessages.size() > 0) {
			return StorageAdapterImplHelper.createFailedResponse(logMessages, DeleteSnapshotsResponse.class); 
		} else {
			DeleteSnapshotsResponse payload = new DeleteSnapshotsResponse();
			StorageOperationResponse<DeleteSnapshotsResponse> response = new StorageOperationResponse<DeleteSnapshotsResponse> (payload);	
			response.setPercentCompleted(100);
			response.setStatus(StorageOperationStatus.COMPLETED);
			return response;
		}
	}

	@Override
	public StorageOperationResponse<ListSnapshotsResponse> listSnapshots(ListSnapshotsRequest request) {
		
		List<StorageVolume> storageVolumes = request.storageVolumes;
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "listSnapshots: volumes:" +request.storageVolumes , null);
		StorageOperationResponse<ListSnapshotsResponse> response = null;
		try {
			Map<StorageVolume, List<StorageSnapshotVolume>> internalSnapshots = new HashMap<StorageVolume, List<StorageSnapshotVolume>>();
			for(StorageVolume volume:storageVolumes){
				List<ShareSnapshot> vcmSnapshots = openstackClient.getVCMSnapshots(volume.storageVolumeId);
				internalSnapshots.put(volume, OpenstackAdapterUtil.transformShareSnapshotToStorageVolumeMap(vcmSnapshots, volume.storageSystemId));	
			}
			ListSnapshotsResponse payload = new ListSnapshotsResponse();
			payload.storageVolumeToSnapshots = internalSnapshots;
			response = new StorageOperationResponse<ListSnapshotsResponse>(payload);
			logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "listSnapshots: found:" + internalSnapshots.size() + " snapshots: " + internalSnapshots , null);
		} catch (Exception e) {
			logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "listSnapshots:" + e.getMessage(), null,e);
			return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), ListSnapshotsResponse.class); 
		}
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;
	}

	@Override
	public StorageOperationResponse<PostProcessTakeSnapshotResponse> postProcessTakeSnapshot(PostProcessTakeSnapshotRequest request) {
		
		PostProcessTakeSnapshotResponse payload = new PostProcessTakeSnapshotResponse();
		StorageOperationResponse<PostProcessTakeSnapshotResponse> response = new StorageOperationResponse<PostProcessTakeSnapshotResponse>(payload);
		response.setContext(request.takeSnapshotResult);
		StorageOperationId id = new StorageOperationId();
		id.id = OpenstackAdapterUtil.generateOperationId();
		id.type = "snapshot";//TODO
		response.setId(id);
		response.setPercentCompleted(0);
		response.setStatus(StorageOperationStatus.EXECUTING);
		return response;
	}

	@Override
	public StorageOperationResponse<PrepareTakeSnapshotResponse> prepareTakeSnapshot(PrepareTakeSnapshotRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "prepareTakeSnapshot: snapshotName:" +request.snapshotName + " volumes:" + request.storageVolumesToBeSnapshot , null);
		PrepareTakeSnapshotResponse payload = new PrepareTakeSnapshotResponse();
		payload.prepareTakeSnapshotResult = request;
		StorageOperationResponse<PrepareTakeSnapshotResponse> response = new StorageOperationResponse<PrepareTakeSnapshotResponse>(payload);
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;
	}

	@Override
	public StorageOperationResponse<TakeSnapshotResponse> takeSnapshot(TakeSnapshotRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "takeSnapshot: " + request, null);
		ArrayList<StorageSnapshotVolume> storageVolumeSnapshotList = new ArrayList<StorageSnapshotVolume>();
		StorageOperationResponse<TakeSnapshotResponse> response = null;
		
		PrepareTakeSnapshotRequest prepareRequest = (PrepareTakeSnapshotRequest) request.prepareTakeSnapshotResult;
		OpenstackFileSnapshotVolumesContext context = new OpenstackFileSnapshotVolumesContext();
		context.snapshotIds = new ArrayList<String>();
		for (StorageVolume volume : prepareRequest.storageVolumesToBeSnapshot) {
			ShareSnapshot snapshot = null;
			try {
				snapshot = openstackClient.createShareSnapshot(volume.storageVolumeId, prepareRequest.snapshotName, prepareRequest.snapshotName);
				StorageSnapshotVolume storageVolumeSnapshot = OpenstackAdapterUtil.shareSnapshotToStorageSnapshot(snapshot, volume.storageSystemId);
				storageVolumeSnapshotList.add(storageVolumeSnapshot);
				context.snapshotIds.add(storageVolumeSnapshot.storageVolumeId);
			} catch (CloudClientException e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "takeSnapshot:" + e.getMessage(), null,e);
				cancelSnapshots(storageVolumeSnapshotList);
				return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), TakeSnapshotResponse.class); 
			}
	        context.snapshots = storageVolumeSnapshotList;
		}
		TakeSnapshotResponse payload = new TakeSnapshotResponse();
		context.volumes = prepareRequest.storageVolumesToBeSnapshot; 
		
		payload.takeSnapshotResult = context;
		response = new StorageOperationResponse<TakeSnapshotResponse>(payload);
		response.setContext(context);
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;
	}
    
	private void cancelSnapshots(List<StorageSnapshotVolume> snapshots) {
		DeleteSnapshotsRequest request = new DeleteSnapshotsRequest();
		request.snapshotsToBeDeleted = snapshots;
		deleteSnapshots(request);
	}
	
	public StorageOperationResponse<PostProcessTakeSnapshotResponse> cancelSnapshots(StorageOperationId operationId, OpenstackFileSnapshotVolumesContext context) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelSnapshots: operationId:" + operationId +" snapshots:" +context.snapshots , null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
	    for (String snapshotId:context.snapshotIds) {
			try {
				logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelSnapshots: operationId:" + operationId +" deleting snapshot: " + snapshotId , null);
		//	 openstackClient.deleteSnapsho(snapshotId);
//			} catch (CloudClientException e) {
			} catch (Exception e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelSnapshots", null,e);
		    	logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));	
		    }
		}
		if (logMessages.size() > 0) {
			return StorageAdapterImplHelper.createFailedResponse(logMessages, PostProcessTakeSnapshotResponse.class); 
		} else {
			PostProcessTakeSnapshotResponse payload = new PostProcessTakeSnapshotResponse();
			StorageOperationResponse<PostProcessTakeSnapshotResponse> response = new StorageOperationResponse<PostProcessTakeSnapshotResponse>(payload);	
			response.setStatus(StorageOperationStatus.CANCELLED);
			response.setId(operationId);
			return response;
		}
	}
	
	public StorageOperationResponse<PostProcessTakeSnapshotResponse> getOperationStatus(StorageOperationId operationId, OpenstackFileSnapshotVolumesContext context)
	throws InfrastructAdapterException {
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId:" + operationId +" snapshots:" +context.snapshots , null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
		Status snapshotState = null;
		String snapshotId = null;
		int progress = 0;
		int operationProgress = 0; 
		boolean pending = false;
		boolean failed = false;
		for (int i=0;i<context.snapshotIds.size();i++) {
			try {
				snapshotId = context.snapshotIds.get(i);
				if (snapshotId == null) continue; //snapshot already completed
				snapshotState  = openstackClient.getSnapshotStatus(snapshotId);
				logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId:" + operationId +" checking snapshot:" + snapshotId + " state is: " + snapshotState , null);
				if (snapshotState.equals(Status.CREATING)) {
					//   progress = Integer.parseInt(snapshot.getProgress()); //TODO might need to be trimmed if it has % at the end
					if (progress < operationProgress) operationProgress = progress;
					pending = true;

				} else if (snapshotState.equals(Status.AVAILABLE)) {
					context.snapshotIds.set(i,null);
				} else if (snapshotState.equals(Status.ERROR)) {
					logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), "Failed to Snapshot volume " + context.volumes.get(i).storageVolumeId +". Snapshot process terminated with ERROR") );	
					failed = true;
					break;
				} else {
					throw new IllegalStateException("Failed to Snapshot volume " + context.volumes.get(i).storageVolumeId +". Snapshot process returned unexpected status: " + snapshotState);
				}
			} catch (Exception e) {
				failed = true;
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
				logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));	
			}
		}
		if (failed) {
			//cleanup 
			cancelSnapshots(operationId, context);
			return  StorageAdapterImplHelper.createFailedResponse(logMessages, PostProcessTakeSnapshotResponse.class); 
		} else if (pending) {
			PostProcessTakeSnapshotResponse payload = new PostProcessTakeSnapshotResponse();
			StorageOperationResponse<PostProcessTakeSnapshotResponse> response = new StorageOperationResponse<PostProcessTakeSnapshotResponse>(payload);
			response.setContext(context);
			response.setId(operationId);
			response.setPercentCompleted(operationProgress); 
			response.setStatus(StorageOperationStatus.EXECUTING);
			return response;

		} else {
			PostProcessTakeSnapshotResponse payload = new PostProcessTakeSnapshotResponse();
			payload.storageVolumeToSnapshotMap = new HashMap<StorageVolume,StorageSnapshotVolume>();
			for (int i = 0; i <context.volumes.size();i++) {
				payload.storageVolumeToSnapshotMap.put(context.volumes.get(i), context.snapshots.get(i));
			}
			StorageOperationResponse<PostProcessTakeSnapshotResponse> response =   new StorageOperationResponse<PostProcessTakeSnapshotResponse>(payload);
			response.setId(operationId);
			response.setContext(context);
			response.setPercentCompleted(100);
			response.setStatus(StorageOperationStatus.COMPLETED);
			return response;
		}
	}
	
	@SerializableClass
	public static class OpenstackFileSnapshotVolumesContext implements IStorageOperationContext, Serializable{

		private static final long serialVersionUID = 4044648480699879101L;

		@SerializableField
		public List<StorageVolume> volumes;

		@SerializableField
		public List<String> snapshotIds;
		
		@SerializableField
		public ArrayList<StorageSnapshotVolume> snapshots;

	}
}

