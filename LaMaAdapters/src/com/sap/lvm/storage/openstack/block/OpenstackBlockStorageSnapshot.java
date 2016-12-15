package com.sap.lvm.storage.openstack.block;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.model.storage.block.VolumeSnapshot;

import com.sap.lvm.storage.openstack.util.CloudClientException;
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

public class OpenstackBlockStorageSnapshot implements IStorageSnapshot {
	
	OpenstackBlockCloudStorageController openstackClient = null;
	private IJavaEeLog logger;

    public OpenstackBlockStorageSnapshot(OpenstackBlockCloudStorageController openstackClient, IJavaEeLog logger) {
		this.openstackClient = openstackClient;
		this.logger = logger;
	}

	@Override
	public StorageOperationResponse<DeleteSnapshotsResponse> deleteSnapshots(
			DeleteSnapshotsRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "deleteSnapshots: snapshots:" +request.snapshotsToBeDeleted , null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
	    List<StorageSnapshotVolume> storageSnapshots = request.snapshotsToBeDeleted;
	    for (StorageSnapshotVolume snapshotVolume : storageSnapshots) {
			try {
				this.openstackClient.deleteSnapshot(snapshotVolume.storageVolumeId);
			} catch (CloudClientException e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "deleteSnapshots: " +e.getMessage(), null, e);
				logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
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
	public StorageOperationResponse<ListSnapshotsResponse> listSnapshots(
			ListSnapshotsRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "listSnapshots: volumes:" +request.storageVolumes , null);
		StorageOperationResponse<ListSnapshotsResponse> response = null;
		try {
		//	List<VolumeSnapshot> snapshots  = null;
			Map<StorageVolume, List<StorageSnapshotVolume>> internalSnapshots = new HashMap<StorageVolume, List<StorageSnapshotVolume>>();
			//for(StorageVolume volume:request.storageVolumes){
			//	snapshots = ((Object) openstackClient).getVolumeSnapshots(volume.storageVolumeId);
			//	internalSnapshots.put(volume, OpenstackAdapterUtil.transformToStorageVolumeMap(snapshots, volume.storageSystemId));	
		//	}
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
	public StorageOperationResponse<PostProcessTakeSnapshotResponse> postProcessTakeSnapshot(
			PostProcessTakeSnapshotRequest request) {
		
		PostProcessTakeSnapshotResponse payload = new PostProcessTakeSnapshotResponse();
		StorageOperationResponse<PostProcessTakeSnapshotResponse> response = new StorageOperationResponse<PostProcessTakeSnapshotResponse>(payload);
		response.setContext(request.takeSnapshotResult);
		StorageOperationId id = new StorageOperationId();
		id.id = OpenstackAdapterUtil.generateOperationId();
		id.type = "snapshot";
		response.setId(id);
		response.setPercentCompleted(0);
		response.setStatus(StorageOperationStatus.EXECUTING);
		return response;
	}

	@Override
	public StorageOperationResponse<PrepareTakeSnapshotResponse> prepareTakeSnapshot(
			PrepareTakeSnapshotRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "prepareTakeSnapshot: snapshotName:" +request.snapshotName + " volumes:" + request.storageVolumesToBeSnapshot , null);
		PrepareTakeSnapshotResponse payload = new PrepareTakeSnapshotResponse();
		payload.prepareTakeSnapshotResult = request;
		StorageOperationResponse<PrepareTakeSnapshotResponse> response = new StorageOperationResponse<PrepareTakeSnapshotResponse>(payload);
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;
	}

	@Override
	public StorageOperationResponse<TakeSnapshotResponse> takeSnapshot(
			TakeSnapshotRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "takeSnapshot: " + request, null);
		ArrayList<StorageSnapshotVolume> storageVolumeSnapshotList = new ArrayList<StorageSnapshotVolume>();
		StorageOperationResponse<TakeSnapshotResponse> response = null;
		
		PrepareTakeSnapshotRequest prepareRequest = (PrepareTakeSnapshotRequest) request.prepareTakeSnapshotResult;
		OpenstackBlockSnapshotVolumesContext context = new OpenstackBlockSnapshotVolumesContext();
		context.snapshotIds = new ArrayList<String>();
		for (StorageVolume volume : prepareRequest.storageVolumesToBeSnapshot) {
			VolumeSnapshot snapshot = null;
			try {
	
				StorageSnapshotVolume storageVolumeSnapshot = OpenstackAdapterUtil.toStorageSnapshot(snapshot, volume.storageSystemId);
				storageVolumeSnapshotList.add(storageVolumeSnapshot);
				context.snapshotIds.add(storageVolumeSnapshot.storageVolumeId);
	
			} catch (Exception e) {
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
	
	public StorageOperationResponse<PostProcessTakeSnapshotResponse> cancelSnapshots(
			StorageOperationId operationId, OpenstackBlockSnapshotVolumesContext context) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelSnapshots: operationId:" + operationId +" snapshots:" +context.snapshots , null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
	    for (String snapshotId:context.snapshotIds) {
			try {
				logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelSnapshots: operationId:" + operationId +" deleting snapshot: " + snapshotId , null);
		//	 openstackClient.deleteSnapsho(snapshotId);

			} catch (Exception e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelSnapshots", null,e);
		    	logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
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
	
	public StorageOperationResponse<PostProcessTakeSnapshotResponse> getOperationStatus(
			StorageOperationId operationId, OpenstackBlockSnapshotVolumesContext context)
			throws InfrastructAdapterException {
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId:" + operationId +" snapshots:" +context.snapshots , null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();

		String snapshotState = null;
	    String snapshotId = null;

	    int operationProgress = 0; 
	    boolean pending = false;
	    boolean failed = false;
		for (int i=0;i<context.snapshotIds.size();i++) {
	    	try {
	    	   snapshotId = context.snapshotIds.get(i);
	    	   if (snapshotId == null) 
	    		   continue;//snapshot already completed
	    	 //  snapshot = openstackClient.getSnapshotStatus(snapshotId);
	    	 //  snapshotState  = snapshot.getState();
	    	    logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId:" + operationId +" checking snapshot:" + snapshotId + " state is: " + snapshotState , null);
//	    	    if (snapshotState.toString().equals("pending")|| (snapshotState.toString().equals("creating"))) {
//	    		//   progress = Integer.parseInt(snapshot.getProgress()); //TODO might need to be trimmed if it has % at the end
//	    		   if (progress < operationProgress) operationProgress = progress;
//	    		   pending = true;
//	    	   } else if (snapshotState.equals("completed")) {
//	    		  context.snapshotIds.set(i,null);
//	    	   } else if (snapshotState.equals("error")) {
//	    		   logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), "Failed to Snapshot volume " + context.volumes.get(i).storageVolumeId +". Snapshot process terminated with ERROR") );	
//	    	       failed = true;
//	    		   break;
//	    	   } else {
//	    		   throw new IllegalStateException("Failed to Snapshot volume " + context.volumes.get(i).storageVolumeId +". Snapshot process returned unexpected status: " + snapshotState);
//	    	   }
	    	} catch (Exception e) {
	    	   failed = true;
	    	   logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
	    	   logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
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
	public static class OpenstackBlockSnapshotVolumesContext implements IStorageOperationContext, Serializable{

		private static final long serialVersionUID = 4044648480699879101L;

		@SerializableField
		public List<StorageVolume> volumes;

		@SerializableField
		public List<String> snapshotIds;
		
		@SerializableField
		public ArrayList<StorageSnapshotVolume> snapshots;

	}
}

