package com.sap.lvm.storage.openstack.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.model.manila.Access;
import org.openstack4j.model.manila.Share;
import org.openstack4j.model.manila.Share.Status;
import org.openstack4j.model.manila.ShareSnapshot;

import com.sap.lvm.CloudClientException;
import com.sap.lvm.storage.openstack.util.OpenstackAdapterUtil;
import com.sap.lvm.storage.openstack.util.OpenstackConstants;
import com.sap.lvm.storage.openstack.util.StorageAdapterImplHelper;
import com.sap.nw.lm.aci.engine.api.base.property.IProperty;
import com.sap.tc.vcm.base.util.serialization.serializable.SerializableClass;
import com.sap.tc.vcm.base.util.serialization.serializable.SerializableField;
import com.sap.tc.vcm.infrastructure.api.adapter.InfrastructAdapterException;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.storage.adapter.api.base.IStorageOperationContext;
import com.sap.tc.vcm.storage.adapter.api.base.StorageOperationId;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageLogMessage;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageOperationStatus;
import com.sap.tc.vcm.storage.adapter.api.cloning.CloneVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.CloneVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.CloningCharacteristicsRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.CloningCharacteristicsResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.CloningCharacteristicsResponse.CloneMethodDuration;
import com.sap.tc.vcm.storage.adapter.api.cloning.CloningCharacteristicsResponse.OperationAttributes;
import com.sap.tc.vcm.storage.adapter.api.cloning.CloningCharacteristicsResponse.StoragePoolSelection;
import com.sap.tc.vcm.storage.adapter.api.cloning.CloningCharacteristicsResponse.SupportedStorageOperation;
import com.sap.tc.vcm.storage.adapter.api.cloning.CloningValidationResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.DeleteVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.DeleteVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.FinalizeCloneVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.FinalizeCloneVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.GenerateUniqueIdsRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.GenerateUniqueIdsResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.IStorageCloning;
import com.sap.tc.vcm.storage.adapter.api.cloning.PostProcessCloneVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.PostProcessCloneVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.PrepareCloneVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.PrepareCloneVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.RetrieveAvailableTargetPoolsRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.RetrieveAvailableTargetPoolsResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.RetrieveAvailableTargetSystemsRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.RetrieveAvailableTargetSystemsResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.RetrieveAvailableTargetVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.cloning.RetrieveAvailableTargetVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStoragePoolsRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStoragePoolsResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageSystemsRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageSystemsResponse;
import com.sap.tc.vcm.storage.adapter.api.types.MountData;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolume;
import com.sap.tc.vcm.storage.adapter.api.types.VolumeToBeCloned;

public class OpenstackFileStorageCloning implements IStorageCloning {
	
	OpenstackFileCloudStorageController openstackClient = null;
	private OpenstackFileStorageRetrieval storageRetreival;
	private CloningCharacteristicsResponse cloningCharacteristics;
	private IJavaEeLog logger;
	
	public OpenstackFileStorageCloning(OpenstackFileCloudStorageController openstackClient, OpenstackFileStorageRetrieval storageRetrieval,IJavaEeLog logger) {

		this.logger = logger;
		this.storageRetreival = storageRetrieval;
		this.openstackClient = openstackClient;
		cloningCharacteristics = new CloningCharacteristicsResponse();
		cloningCharacteristics.canCloneFromSnapshot=true;
		cloningCharacteristics.isTargetMountConfigurationGeneratedByStorageManager=false;

		Map<SupportedStorageOperation, OperationAttributes> supportedStorageOperations = new HashMap<SupportedStorageOperation, OperationAttributes>();
		OperationAttributes localCloneAttr = new OperationAttributes(CloneMethodDuration.SHORT, StoragePoolSelection.AUTO, false, false);
		supportedStorageOperations.put(SupportedStorageOperation.LOCAL_CLONE, localCloneAttr);
		OperationAttributes localSnapAttr = new OperationAttributes(CloneMethodDuration.SHORT, StoragePoolSelection.AUTO, false, false);
		supportedStorageOperations.put(SupportedStorageOperation.LOCAL_SNAPSHOT, localSnapAttr);
		OperationAttributes remoteCloneAttr = new OperationAttributes(CloneMethodDuration.SHORT, StoragePoolSelection.AUTO, false, false);
		supportedStorageOperations.put(SupportedStorageOperation.REMOTE_CLONE, remoteCloneAttr);
		OperationAttributes remoteSnapAttr = new OperationAttributes(CloneMethodDuration.SHORT, StoragePoolSelection.AUTO, false, false);
		supportedStorageOperations.put(SupportedStorageOperation.REMOTE_SNAPSHOT, remoteSnapAttr);
		cloningCharacteristics.supportedStorageOperations=supportedStorageOperations;

	}
	
	@Override
	public StorageOperationResponse<CloneVolumesResponse> cloneVolumes(CloneVolumesRequest request) {
		
		String msg = "";
		PrepareCloneVolumesRequest prepareRequest = (PrepareCloneVolumesRequest)request.prepareCloneVolumesResult;
		List<VolumeToBeCloned>  volumesToBeCloned = prepareRequest.volumesToBeCloned;
		ArrayList<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();

		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cloneVolumes: request:" + prepareRequest, null);
		msg = "CloneVolumes operation started";
		logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_INFO, "OSFile", System.currentTimeMillis(), msg));
		String operationId = OpenstackAdapterUtil.generateOperationId();
		ArrayList<String> snapshots  = new ArrayList<String>();

		OpenstackFileCloneVolumesContext context = new OpenstackFileCloneVolumesContext();
		context.volumeStatus = new ArrayList<OpenstackFileCloneVolumeStatus>();
		OpenstackFileCloneVolumeStatus status;
		for (VolumeToBeCloned inputVolume : volumesToBeCloned) {
			status = new OpenstackFileCloneVolumeStatus();
	       	status.volumeToBeCloned = inputVolume;
	       	status.customCloningProperties = prepareRequest.customCloningProperties;
	       	context.volumeStatus.add(status);
			try {
			  if (inputVolume.isSourceVolumeSnapshot) {
			     if(openstackClient.getSnapshot(inputVolume.sourceVolumeId) != null) {
			    	 status.sourceSnapshotComplete = true;
			    	 status.sourceSnapshotId = inputVolume.sourceVolumeId;
			    	 if(isRemoteClone(status.volumeToBeCloned)) {
			    		 createTargetSnapshot(status.sourceSnapshotId, status, operationId);
			    		 snapshots.add(status.targetSnapshotId);
    	    			 msg = "Target snapshot" + status.targetSnapshotId + " created";
			    		 logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_INFO, "LVM", System.currentTimeMillis(), msg));
			    	 }
			    	 
			     } else {
	    			 msg = "Snapshot not found: " + inputVolume.sourceVolumeId;
		    		 logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), msg));
					 return StorageAdapterImplHelper.createFailedResponse(logMessages, CloneVolumesResponse.class);  
			     }	
			  } else {
				  ShareSnapshot snapshot = null;
				try {
					String snapshotName = "Snapshot of share " + inputVolume.sourceVolumeId;
					String snapshotDescription = "LVM Snapshot to clone share " + inputVolume.sourceVolumeId;
					snapshot = openstackClient.createShareSnapshot(inputVolume.sourceVolumeId, snapshotName, snapshotDescription);
				} catch (CloudClientException e) {
					
					throw e; 
				}
				  snapshots.add(snapshot.getId());
				  status.sourceSnapshotId = snapshot.getId();
			
			  }
			} catch (CloudClientException e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cloneVolumes:" + e.getMessage(), null,e);
				logMessages.add(0, new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));
				return StorageAdapterImplHelper.createFailedResponse(logMessages, CloneVolumesResponse.class); 
			}
		}
		msg = "CloneVolumes operation ended";
		logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_INFO, "OSFile", System.currentTimeMillis(), msg));

		CloneVolumesResponse payload = new CloneVolumesResponse();
	    context.operationId = operationId;
	    context.customCloningProperties = prepareRequest.customCloningProperties;
		payload.cloneVolumeResult = context;
		StorageOperationResponse<CloneVolumesResponse> response =  new StorageOperationResponse<CloneVolumesResponse>(payload);
		response.setLogMessages(logMessages);
	    response.setPercentCompleted(100);
	    response.setStatus(StorageOperationStatus.COMPLETED);
	    response.setContext(context);
	    return response;
	}

	@Override
	public StorageOperationResponse<DeleteVolumesResponse> deleteVolumes(DeleteVolumesRequest request) {
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "deleteVolumes: request:" + request, null);    
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
		for (StorageVolume volume:request.storageVolumes) {
	    	try {
	    	  Share share = openstackClient.getShare(volume.storageVolumeId);
	    	  Status shareStatus = share.getStatus();
	    	  // snapshotState.equals(Status.AVAILABLE)
	    	  if (!shareStatus.equals(Status.AVAILABLE)) {
	    		  logger.log(IJavaEeLog.SEVERITY_WARNING, this.getClass().getName(), "Volume :" + share.getId() + " is in unexpected state: " + share.getStatus() + ".Operation may fail.", null);  
	    	  }
	    	  openstackClient.deleteShare(volume.storageVolumeId);
	    	} catch (Exception e) {
	    	   logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "deleteVolumes:" + e.getMessage(), null,e);
			   logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));	
	    	}
	    }
	    if (logMessages.isEmpty()) {
	    	DeleteVolumesResponse payload = new DeleteVolumesResponse();
	    	StorageOperationResponse<DeleteVolumesResponse> response  = new StorageOperationResponse<DeleteVolumesResponse>(payload);
	    	response.setPercentCompleted(100);
			response.setStatus(StorageOperationStatus.COMPLETED);
			return response;
	    } else {
	    	return StorageAdapterImplHelper.createFailedResponse(logMessages, DeleteVolumesResponse.class); 
	    }
		
	}

	@Override
	public StorageOperationResponse<FinalizeCloneVolumesResponse> finalizeCloneVolumes(FinalizeCloneVolumesRequest request) {
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "finalizeCloneVolumes: request:" + request.postProcessCloneVolumesResult, null);
		StorageOperationResponse<FinalizeCloneVolumesResponse> response = new StorageOperationResponse<FinalizeCloneVolumesResponse>();
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		FinalizeCloneVolumesResponse payload = new FinalizeCloneVolumesResponse();
		response.setPayload(payload);
		return response;
	}

	@Override
	public StorageOperationResponse<GenerateUniqueIdsResponse> generateUniqueIds(GenerateUniqueIdsRequest request) {
		StorageOperationResponse<GenerateUniqueIdsResponse> response = new StorageOperationResponse<GenerateUniqueIdsResponse>();
		GenerateUniqueIdsResponse generateUniqueIdsResponse = new GenerateUniqueIdsResponse();
		generateUniqueIdsResponse.uniqueIds = request.sapLvmSuggestedUniqueIds;
		response.setPayload(generateUniqueIdsResponse);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;
	}

	@Override
	public CloningCharacteristicsResponse getGlobalCloningCharacteristics(CloningCharacteristicsRequest request) {
		return cloningCharacteristics;
	}

	@Override
	public StorageOperationResponse<PostProcessCloneVolumesResponse> postProcessCloneVolumes(PostProcessCloneVolumesRequest request) {
	
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "postProcessCloneVolumes: request:" +request, null);
	    OpenstackFileCloneVolumesContext context = (OpenstackFileCloneVolumesContext)request.cloneVolumesResult;
		PostProcessCloneVolumesResponse payload = new PostProcessCloneVolumesResponse();
		StorageOperationResponse<PostProcessCloneVolumesResponse> response = new StorageOperationResponse<PostProcessCloneVolumesResponse>();
		StorageOperationId operation = new StorageOperationId();
		operation.id = context.operationId;
		operation.type = "clone"; 
		response.setContext(context);
		response.setId(operation);
		response.setPayload(payload);
		response.setPercentCompleted(0);
		response.setStatus(StorageOperationStatus.EXECUTING);
		return response;
	
	}

	@Override
	public StorageOperationResponse<PrepareCloneVolumesResponse> prepareCloneVolumes(PrepareCloneVolumesRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "prepareCloneVolumes: " + request, null);
		PrepareCloneVolumesResponse payload = new PrepareCloneVolumesResponse();
		payload.prepareCloneVolumeResult=request;
		return new StorageOperationResponse<PrepareCloneVolumesResponse>(payload);
	}
	
	@Override
	public StorageOperationResponse<RetrieveAvailableTargetPoolsResponse> retrieveAvailableTargetPools(RetrieveAvailableTargetPoolsRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableTargetPools: request:" +request, null);
	    GetStoragePoolsRequest getPools = new GetStoragePoolsRequest();
	    StorageOperationResponse<GetStoragePoolsResponse> getPoolsResponse =  storageRetreival.getStoragePools(getPools);
	    if(getPoolsResponse.getStatus().equals(StorageOperationStatus.FAILED)) {
	      return StorageAdapterImplHelper.createFailedResponse(getPoolsResponse.getLogMessages(), RetrieveAvailableTargetPoolsResponse.class);
	    }
	    RetrieveAvailableTargetPoolsResponse payload = new RetrieveAvailableTargetPoolsResponse();
	    payload.availableTargetPools = getPoolsResponse.getPayload().storagePools;
	    logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableTargetPools: found:" + payload.availableTargetPools.size() + " systems: " + payload.availableTargetPools, null);
	    StorageOperationResponse<RetrieveAvailableTargetPoolsResponse> response = new StorageOperationResponse<RetrieveAvailableTargetPoolsResponse>(payload);
	    response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
	    return response;		
	}

	/* (non-Javadoc)
	 * @see com.sap.tc.vcm.storage.adapter.api.cloning.IStorageCloning#retrieveAvailableTargetSystems(com.sap.tc.vcm.storage.adapter.api.cloning.RetrieveAvailableTargetSystemsRequest)
	 */
	@Override

	public StorageOperationResponse<RetrieveAvailableTargetSystemsResponse> retrieveAvailableTargetSystems(RetrieveAvailableTargetSystemsRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableTargetSystems: request:" +request, null);
	    GetStorageSystemsRequest getSystemsRequest  = new GetStorageSystemsRequest();
	    StorageOperationResponse<GetStorageSystemsResponse> getSystemsResponse = storageRetreival.getStorageSystems(getSystemsRequest);
	    if(getSystemsResponse.getStatus().equals(StorageOperationStatus.FAILED)) {
		   return StorageAdapterImplHelper.createFailedResponse(getSystemsResponse.getLogMessages(), RetrieveAvailableTargetSystemsResponse.class);
		}
	    RetrieveAvailableTargetSystemsResponse payload = new RetrieveAvailableTargetSystemsResponse();
	    payload.availableTargetSystems = getSystemsResponse.getPayload().storageSystems;
	    logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableTargetSystems: found:" + payload.availableTargetSystems.size() + " systems: " + payload.availableTargetSystems, null);
	    StorageOperationResponse<RetrieveAvailableTargetSystemsResponse> response = new StorageOperationResponse<RetrieveAvailableTargetSystemsResponse>(payload);
	    response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
	    return response;	    
	}

	/* (non-Javadoc)
	 * @see com.sap.tc.vcm.storage.adapter.api.cloning.IStorageCloning#retrieveAvailableTargetVolumes(com.sap.tc.vcm.storage.adapter.api.cloning.RetrieveAvailableTargetVolumesRequest)
	 */
	@Override
	public StorageOperationResponse<RetrieveAvailableTargetVolumesResponse> retrieveAvailableTargetVolumes(RetrieveAvailableTargetVolumesRequest request) {
		
		//we don't support this feature for shares 
		//but just in case the method ever gets called - return an empty list.
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableTargetVolumes: request:" +request, null);
	    RetrieveAvailableTargetVolumesResponse payload = new RetrieveAvailableTargetVolumesResponse();
		payload.availableTargetVolumes = new ArrayList<StorageVolume>();
		return new StorageOperationResponse<RetrieveAvailableTargetVolumesResponse>(payload); 
	}

	/* (non-Javadoc)
	 * @see com.sap.tc.vcm.storage.adapter.api.cloning.IStorageCloning#validateCloneRequest(com.sap.tc.vcm.storage.adapter.api.cloning.PrepareCloneVolumesRequest)
	 */
	@Override
	public CloningValidationResponse validateCloneRequest(PrepareCloneVolumesRequest request) {
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "validateCloneRequest: " + request, null);	
		// Would be nice to check here if we are  not exceeding the EBS quota 
	
		return new CloningValidationResponse();
		
		
	}
	
	/**
	 * @param operationId
	 * @param context
	 * @return
	 * @throws InfrastructAdapterException
	 */
	public StorageOperationResponse<PostProcessCloneVolumesResponse> cancelVolumes(StorageOperationId operationId, OpenstackFileCloneVolumesContext context)throws InfrastructAdapterException {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelVolumes: operationId: " + operationId  + " volumeStauts: " + context.volumeStatus, null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
		for (OpenstackFileCloneVolumeStatus status:context.volumeStatus) {
	    	if (status.sourceSnapshotId != null){
	    	    try {
	    		   openstackClient.deleteSnapshot(status.sourceSnapshotId);
	    	    } catch (CloudClientException e) {
	    	       logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelVolumes:" + e.getMessage(), null,e);
		   	       logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));	
	    	    }
	    	}
	    	if (status.targetSnapshotId != null){
	    	    try {
	    		   openstackClient.deleteSnapshot(status.targetSnapshotId);
	    	
	    	    } catch (CloudClientException e) {
	    	       logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelVolumes:" + e.getMessage(), null,e);
		   	       logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));	
	    	    }
	    	}
	    	if (status.targetVolumeId != null){
	    	    try {
	    		   openstackClient.deleteShare(status.targetVolumeId);
	    		   //  	    } catch (CloudClientException e) {
	    	    } catch (Exception e) {
	    	       logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelVolumes:" + e.getMessage(), null,e);
		   	       logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));	
	    	    }
	    	}
	    }
		if (logMessages.isEmpty()) {
	    	PostProcessCloneVolumesResponse payload = new PostProcessCloneVolumesResponse();
	    	payload.mountConfiguration = new ArrayList<MountData>();
	    	StorageOperationResponse<PostProcessCloneVolumesResponse> response =   new StorageOperationResponse<PostProcessCloneVolumesResponse>(payload);
	    	response.setPercentCompleted(100);
			response.setStatus(StorageOperationStatus.CANCELLED);
			return response;
	    } else {
	    	return  StorageAdapterImplHelper.createFailedResponse(logMessages, PostProcessCloneVolumesResponse.class); 
	    }
	}
	
	/**
	 * @param operationId
	 * @param context
	 * @return
	 * @throws InfrastructAdapterException
	 */
	public  StorageOperationResponse<PostProcessCloneVolumesResponse> getOperationStatus(StorageOperationId operationId, OpenstackFileCloneVolumesContext context)
			throws InfrastructAdapterException {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " context: " + context, null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
		org.openstack4j.model.manila.ShareSnapshot.Status snapshotState = null;
		Share share = null;
		Status shareState = null;
	    String snapshotId = null;
	    String sourceVolumeId = null;
	    String targetVolumeId = null;
	    int progress = 0;
	    int operationProgress = 0; 
	    boolean pending = false;
	    boolean failed = false;
	    boolean snapshotsReady = true;

	    List<OpenstackFileCloneVolumeStatus> volumeStatus = context.volumeStatus;
	    
	    //STEP 1 : Check sourceSnapshot State
	    for (OpenstackFileCloneVolumeStatus status:volumeStatus) {

	    	try {
	    		sourceVolumeId = status.volumeToBeCloned.sourceVolumeId;
	    	    snapshotId = status.sourceSnapshotId;
	    		//	    	   String sourceSnapshotId = status.sourceSnapshotId;
	    		logger.log(IJavaEeLog.SEVERITY_DEBUG,this.getClass().getName()," //STEP 1 : Check sourceSnapshot State "+   snapshotId ,null);
	    		if (status.sourceSnapshotComplete) continue;//snapshot already completed
	    		//snapshot = openstackClient.getSnapshot(snapshotId);
	    		snapshotState  = openstackClient.getSnapshotStatus(snapshotId);
	    		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " checking snapshot: " + snapshotId + " state: " + snapshotState, null);
	    		if ( snapshotState.equals(Status.CREATING)) {

	    			//   logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + "checking snapshot:" + snapshotId + " progress:" + snapshot.getProgress(), null);
	    			String percent = "0";//snapshot.getProgress(); //progress not tracked in Openstack
	    			//	    		   if (percent.isEmpty()) {
	    			//	    			   percent = "0";
	    			//	    		   } else {
	    			//	    			   percent = percent.substring(0,percent.length() -2);
	    			//	    		   }
	    			progress = Integer.parseInt(percent); 
	    			if (progress < operationProgress) operationProgress = progress;
	    			pending = true;
	    			snapshotsReady = false;
	    			continue;
	    		} else if  (snapshotState.equals(Status.AVAILABLE)) { 
	    			status.sourceSnapshotComplete = true;
	    			logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " completed source snapshot: " + snapshotId, null);
	    			//	    		  if (isRemoteClone(status.volumeToBeCloned)) {
	    			//	    			  createTargetSnapshot(snapshotId, status, operationId.toString());
	    			//	    		   } else {

	    			createTargetShare(context, status, true);
	    			//	    		  }
	    		} else if (snapshotState.equals(Status.ERROR)) {
	    			logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " failed: " + snapshotId, null);
	    			logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), "Failed to Clone volume " + status.volumeToBeCloned.sourceVolumeId +". Snapshot process terminated with ERROR") );	
	    			failed = true;
	    			break;
	    		} else {
	    			throw new IllegalStateException("Failed to Clone volume " + status.volumeToBeCloned.sourceVolumeId +". Snapshot process returned unexpected status: " + snapshotState);
	    		}
	    	} catch (Exception e) {
	    		failed = true;
	    		logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
	    		logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(),  "getOperationStatus:" + e.getMessage()));	
	    	}
	    }
	    //STEP 2: Check Target Snapshot state (for cross-region only) //TODO
	    for (OpenstackFileCloneVolumeStatus status:context.volumeStatus) {
	    	if (isRemoteClone(status.volumeToBeCloned)) {
	    		if (status.targetSnapshotId != null && !status.targetSnapshotComplete) {
	    			try {
	    				snapshotId = status.targetSnapshotId;
	    				//snapshot = openstackClient.getSnapshot(snapshotId);
	    				snapshotState  = openstackClient.getSnapshotStatus(snapshotId);
	    				logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " checking snapshot: " + snapshotId + " state: " + snapshotState, null);
	    				if (snapshotState.equals(Status.CREATING)) {
	    					// logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + "checking snapshot:" + snapshotId + " progress:" + snapshot.getProgress(), null);
	    					//TODO: openstack doesnt have percent complete I think so map percentages based on state 0, 50,100
	    					String percent = "0";//snapshot.getStatus()
	    					if (percent.isEmpty()) {
	    						percent = "0";
	    					} else {
	    						percent = percent.substring(0,percent.length() -2);
	    					}
	    					progress = Integer.parseInt(percent); 
	    					if (progress < operationProgress) operationProgress = progress;
	    					pending = true;
	    					snapshotsReady = false;
	    					continue;
	    				} else if (snapshotState.equals(Status.AVAILABLE)) { 
	    					status.targetSnapshotComplete = true;
	    					logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " completed target snapshot: " + snapshotId, null);
	    					createTargetShare(context, status, true);


	    				} else if (snapshotState.equals(Status.ERROR)) {
	    					logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " failed: " + snapshotId, null);
	    					logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), "Failed to Clone volume " + status.volumeToBeCloned.sourceVolumeId +". Snapshot process terminated with ERROR") );	
	    					failed = true;
	    					break;
	    				} else {
	    					throw new IllegalStateException("Failed to Clone volume " + status.volumeToBeCloned.sourceVolumeId +". Snapshot process returned unexpected status: " + snapshotState);
	    				}
	    			} catch (CloudClientException e) {
	    				failed = true;
	    				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
	    				logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));	
	    				break;
	    			}

	    		}
	    	}
	    }
	    //STEP 3: Check Target Volume state
	    for (OpenstackFileCloneVolumeStatus status:context.volumeStatus) {
	    	if (status.targetVolumeId == null) continue;
	    	try {
	    		share = openstackClient.getShare(status.targetVolumeId);
	    		if (share == null) {
	    			failed = true;
	    			logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), "Volume " + status.targetVolumeId + " not found"));	
	    			break;
	    		}
	    		//    } catch (CloudClientException e) {
	    	} catch (Exception e) {
	    		failed = true;
	    		logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
	    		logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));	
	    		break;
	    	}
	    	shareState = share.getStatus();
	    	logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " checking volume: " + share.getId() + " state: " + shareState, null);
	    	if ((shareState.equals(Status.CREATING))){
	    		pending = true;
	    		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " in progress: " + share.getId(), null);
	    	} else if (shareState.equals(Status.ERROR)) {

	    		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " create volume failed: " + share.getId(), null);
	    		logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), "Failed to Clone volume " + share.getId() + ". Process terminated with ERROR") );	
	    		failed = true;
	    		break;
	    	} else if (shareState.equals(Status.AVAILABLE)) {
	    		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " completed volume: " + share.getId(), null);
	    		status.targetVolumeComplete = true;

	    		//once volume is ready we can delete the temporary snapshot as it is no longer needed. 
	    		if (status.targetSnapshotComplete) {
	    			snapshotId = status.targetSnapshotId;
	    		} else {
	    			snapshotId = status.sourceSnapshotId;
	    		}
	    		try{
	    			boolean forDelete = false;

	    			if (forDelete) {
	    				openstackClient.deleteSnapshot(snapshotId);
	    			}
	    		}

	    		catch (Exception e) {
	    			logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
	    			//here we just log warning as failing to delete the temporary snapshot does not fail the clone operation 
	    			logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_WARNING, "OSFile", System.currentTimeMillis(), "Failed delete temorary snapshot " + share.getSnapshotId() + ". " + e.getMessage()));	
	    		}
	    	} else {
	    		logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), "Unexpected volume state: " + shareState.name()) );	
	    		failed = true;
	    		break;
	    	}}

	    if (failed) {
	    	//cleanup 
	    	cancelVolumes(operationId, context);
	    	return  StorageAdapterImplHelper.createFailedResponse(logMessages, PostProcessCloneVolumesResponse.class); 
	    } else if (pending) {
	    	PostProcessCloneVolumesResponse payload = new PostProcessCloneVolumesResponse();
	    	StorageOperationResponse<PostProcessCloneVolumesResponse> response =  new StorageOperationResponse<PostProcessCloneVolumesResponse>(payload);
	    	response.setPercentCompleted(progress);
	    	response.setId(operationId);
	    	response.setContext(context);
	    	response.setStatus(StorageOperationStatus.EXECUTING);
	    	response.setLogMessages(logMessages);
	    	return response;

	    } else {
	    	//operation completed successfully
	    	PostProcessCloneVolumesResponse payload = new PostProcessCloneVolumesResponse();
	    	payload.mountConfiguration = new ArrayList<MountData>();
	    	for (OpenstackFileCloneVolumeStatus state:context.volumeStatus) {
	    		state.volumeToBeCloned.targetVolumeId = state.targetVolumeId;
	    		sourceVolumeId = state.volumeToBeCloned.sourceVolumeId;
	    		targetVolumeId = state.volumeToBeCloned.targetVolumeId;

	    		Share sourceShare = openstackClient.getShare(sourceVolumeId);
	    		String sourceExport = sourceShare.getExportLocation();
	    		Share targetShare = openstackClient.getShare(targetVolumeId);
	    		String targetExport = targetShare.getExportLocation();

	    		List<MountData> sourceMnts = state.volumeToBeCloned.sourceMountConfiguration;
	    		if(sourceMnts!=null && !sourceMnts.isEmpty()) 
	    		{
	    			for (int i=0; i< sourceMnts.size(); i++) 
	    			{
	    				MountData srcMD = sourceMnts.get(i);
	    				String sourceExportPath = srcMD.exportPath;
	    				if(!sourceExportPath.contains(sourceExport))
	    					continue;
	    				MountData targetMD = state.volumeToBeCloned.targetMountConfiguration.get(i);
	    				String targetExportPath = sourceExportPath.replace(sourceExport, targetExport);
	    				targetMD.exportPath = targetExportPath;
	    				payload.mountConfiguration.add(targetMD);
	    			}
	    		}


	    		try {
   					List<? extends Access> listAccess = openstackClient.listAccess(sourceVolumeId);
	    			for (Access shareAccessMapping : listAccess) {
	    				String accessTo = shareAccessMapping.getAccessTo();
	    				Boolean active = openstackClient.allowAccess(targetVolumeId, accessTo);
	    			}
	    		} catch (Exception e) {
	    			logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cloneVolumes:" + e.getMessage(), null,e);
	    			//	    	       		ArrayList<StorageLogMessage> logMessages = cancel(snapshots);
	    			//	    	       		logMessages.add(0, new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSFile", System.currentTimeMillis(), e.getMessage()));
	    			return StorageAdapterImplHelper.createFailedResponse(logMessages, PostProcessCloneVolumesResponse.class); 
	    		}


	    	}
	    	StorageOperationResponse<PostProcessCloneVolumesResponse> response =  new StorageOperationResponse<PostProcessCloneVolumesResponse>(payload);
	    	response.setId(operationId);
	    	response.setPercentCompleted(100);
	    	response.setLogMessages(logMessages);
	    	response.setStatus(StorageOperationStatus.COMPLETED);
	    	return response;
	    }

	}
		
	private String  createTargetShare(OpenstackFileCloneVolumesContext context, OpenstackFileCloneVolumeStatus status, boolean source) throws  CloudClientException {
		  String snapshotId;
		  if (source) {
			  //snapshotId = status.volumeToBeCloned.sourceVolumeId; TODO: why does AWS do it this way? wrong value is passed here
			  snapshotId=status.sourceSnapshotId;
		  } else {
			  snapshotId = status.volumeToBeCloned.targetVolumeId;
		  }
		  ShareSnapshot snapshot = openstackClient.getSnapshot(snapshotId);
		  String shareType = null;
		  
		  if (status.volumeToBeCloned.isSourceVolumeSnapshot) {
			  shareType = "standard";
		  } else {
	    		  String volumeId = status.volumeToBeCloned.sourceVolumeId;
	    		  Share sourceVolume = openstackClient.getShare(volumeId);
	    		  shareType = sourceVolume.getShareType();

		  }
		  String shareName=status.volumeToBeCloned.targetVolumeName;
		  Share share = openstackClient.createShareFromSnapshot(shareName, snapshot, shareType);
		  status.targetVolumeId = share.getId();
		  logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + context.operationId + " creating volume: " + share.getId(), null);
		  return share.getId();
	}
	
	private void createTargetSnapshot(String snapshotId, OpenstackFileCloneVolumeStatus status,String operationId) throws CloudClientException {
		  String[] keys = {OpenstackConstants.TARGET_VOLUME_TAG, OpenstackConstants.CLONE_OPERATION_TAG};
		  String[] values = new String[2];
		  values[1] = operationId;
		  String targetRegion = status.volumeToBeCloned.targetStorageSystemId.substring(status.volumeToBeCloned.targetStorageSystemId.indexOf(':')+1);
		  ShareSnapshot snapshot = openstackClient.copy(snapshotId, targetRegion , "LVM Snapshot to clone Volume " + status.volumeToBeCloned.sourceVolumeId);
		 //is this used??
		  status.targetSnapshotId = targetRegion + ':' + snapshot.getId();
      
	}
	
	private boolean isRemoteClone(VolumeToBeCloned volume) {
		return false;	
	}
	        
	@SerializableClass
	public static class OpenstackFileCloneVolumesContext implements IStorageOperationContext {

		@SerializableField
		public List<OpenstackFileCloneVolumeStatus> volumeStatus;
		@SerializableField
		public String operationId;
        
		@SerializableField
		public Map<String, IProperty> customCloningProperties;
		
		@Override
		public String toString() {
			return "OpenstackCloneVolumesContext [operationId=" + operationId + ", volumeStatus=" + volumeStatus + "]";
		}
	}
	
	@SerializableClass
	public static class OpenstackFileCloneVolumeStatus implements IStorageOperationContext {

		@SerializableField
		public VolumeToBeCloned volumeToBeCloned;

		@SerializableField
		public String sourceSnapshotId;
		
		@SerializableField
		public boolean sourceSnapshotComplete = false;
		
		@SerializableField
		public String targetSnapshotId;
		
		@SerializableField
		public boolean targetSnapshotComplete = false;
		
		@SerializableField
		public String targetVolumeId;
		
		@SerializableField
		public boolean targetVolumeComplete = false;
	    
		@SerializableField
		public Map<String, IProperty> customCloningProperties;
		
		@Override
		public String toString() {
			return "OpenstackCloneVolumeStatus [sourceVolume=" + volumeToBeCloned+ ", sourceSnapshot=" + sourceSnapshotId  
			+ ", sourceSnapshotComplete=" + sourceSnapshotComplete + ", targetSnapshot="+targetSnapshotId + ", targetSnapshotComplete="
			+ targetSnapshotComplete + ", targetVolume=" + targetVolumeId +", targetVolumeComplete=" +targetVolumeComplete +"]";
		}

	}

}
