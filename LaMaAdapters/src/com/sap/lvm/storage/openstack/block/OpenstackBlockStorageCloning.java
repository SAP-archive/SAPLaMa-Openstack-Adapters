package com.sap.lvm.storage.openstack.block;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.Volume.Status;
import org.openstack4j.model.storage.block.VolumeSnapshot;

import com.sap.lvm.storage.openstack.util.CloudClientException;
import com.sap.lvm.storage.openstack.util.OpenstackAdapterUtil;
import com.sap.lvm.storage.openstack.util.OpenstackConstants.OpenstackVolumeStates;
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
import com.sap.tc.vcm.storage.adapter.api.cloning.GenerateUniqueIdsRequest.UniqueId;
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
import com.sap.tc.vcm.storage.adapter.api.types.StoragePool;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolume;
import com.sap.tc.vcm.storage.adapter.api.types.VolumeToBeCloned;


public class OpenstackBlockStorageCloning implements IStorageCloning {
	
	OpenstackBlockCloudStorageController openstackClient = null;
	private OpenstackBlockStorageRetrieval storageRetreival;
	private CloningCharacteristicsResponse cloningCharacteristics;
	private IJavaEeLog logger;
	
	public OpenstackBlockStorageCloning(OpenstackBlockCloudStorageController openstackClient, OpenstackBlockStorageRetrieval storageRetrieval,IJavaEeLog logger) {
		
		this.logger = logger;
		this.storageRetreival = storageRetrieval;
		this.openstackClient = openstackClient;
		cloningCharacteristics = new CloningCharacteristicsResponse();
        cloningCharacteristics.canCloneFromSnapshot=true;
		cloningCharacteristics.isTargetMountConfigurationGeneratedByStorageManager=true;
		Map<SupportedStorageOperation, OperationAttributes> supportedStorageOperations = new HashMap<SupportedStorageOperation, OperationAttributes>();
		OperationAttributes localCloneAttr = new OperationAttributes(CloneMethodDuration.SHORT, StoragePoolSelection.BOTH, false, false);
		supportedStorageOperations.put(SupportedStorageOperation.LOCAL_CLONE, localCloneAttr);
		OperationAttributes localSnapAttr = new OperationAttributes(CloneMethodDuration.SHORT, StoragePoolSelection.BOTH, false, false);
		supportedStorageOperations.put(SupportedStorageOperation.LOCAL_SNAPSHOT, localSnapAttr);
		OperationAttributes remoteCloneAttr = new OperationAttributes(CloneMethodDuration.SHORT, StoragePoolSelection.BOTH, false, false);
		supportedStorageOperations.put(SupportedStorageOperation.REMOTE_CLONE, remoteCloneAttr);
		OperationAttributes remoteSnapAttr = new OperationAttributes(CloneMethodDuration.SHORT, StoragePoolSelection.BOTH, false, false);
		supportedStorageOperations.put(SupportedStorageOperation.REMOTE_SNAPSHOT, remoteSnapAttr);
		cloningCharacteristics.supportedStorageOperations=supportedStorageOperations;
	}

	@Override
	public StorageOperationResponse<CloneVolumesResponse> cloneVolumes(
			CloneVolumesRequest request) {
		
		PrepareCloneVolumesRequest prepareRequest = (PrepareCloneVolumesRequest)request.prepareCloneVolumesResult;
		List<VolumeToBeCloned>  volumesToBeCloned = prepareRequest.volumesToBeCloned;
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cloneVolumes: request:" + prepareRequest, null);
		String operationId = OpenstackAdapterUtil.generateOperationId();
		ArrayList<String> snapshots  = new ArrayList<String>();
	
		String[] values = new String[2];
		values[1] = operationId;
		String region = null;
		OpenstackBlockCloneVolumesContext context = new OpenstackBlockCloneVolumesContext();
		context.volumeStatus = new ArrayList<OpenstackBlockCloneVolumeStatus>();
		OpenstackBlockCloneVolumeStatus status;
		for (VolumeToBeCloned inputVolume : volumesToBeCloned) {
			status = new OpenstackBlockCloneVolumeStatus();
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
			    	 }
			    	 
			     } else {
			    	 ArrayList<StorageLogMessage> logMessages = cancel(snapshots);
					 logMessages.add(0, new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), "Snapshot not found: " + inputVolume.sourceVolumeId));
					 return StorageAdapterImplHelper.createFailedResponse(logMessages, CloneVolumesResponse.class);  
			     }	
			  } else {
				  region = inputVolume.sourceVolumeId.substring(0,inputVolume.sourceVolumeId.indexOf(':'));
				  VolumeSnapshot snapshot = null;
				try {
					snapshot = openstackClient.createSnapshot(inputVolume.sourceVolumeId, "LVM Snapshot to clone Volume " + inputVolume.sourceVolumeId,true);
				} catch (CloudClientException e) {
					
					throw e; 
				}
				  snapshots.add(region + ':' + snapshot.getId());
				  status.sourceSnapshotId = region + ':' + snapshot.getId();
			
			  }
			} catch (CloudClientException e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cloneVolumes:" + e.getMessage(), null,e);
				ArrayList<StorageLogMessage> logMessages = cancel(snapshots);
				logMessages.add(0, new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));
				return StorageAdapterImplHelper.createFailedResponse(logMessages, CloneVolumesResponse.class); 
			}
		}
	    CloneVolumesResponse payload = new CloneVolumesResponse();
	    context.operationId = operationId;
	    context.customCloningProperties = prepareRequest.customCloningProperties;
		payload.cloneVolumeResult = context;
		StorageOperationResponse<CloneVolumesResponse> response =  new StorageOperationResponse<CloneVolumesResponse>(payload);
	    response.setPercentCompleted(100);
	    response.setStatus(StorageOperationStatus.COMPLETED);
	    response.setContext(context);
	    return response;
	}
	
	private ArrayList<StorageLogMessage> cancel(List<String> snapshots) {
		ArrayList<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
	    for (String snapshot:snapshots) {
	    	try {
	    	   openstackClient.deleteSnapshot(snapshot);
	    	} catch (CloudClientException e) {
	    	   logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancel:" + e.getMessage(), null,e);
			   logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
	    	}
	    }
	    return logMessages;
		
	}

	@Override
	public StorageOperationResponse<DeleteVolumesResponse> deleteVolumes(
			DeleteVolumesRequest request) {
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "deleteVolumes: request:" + request, null);    
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
		Volume openstackVolume;
		for (StorageVolume volume:request.storageVolumes) {
	    	try {
	    	  openstackVolume = openstackClient.getVolume(volume.storageVolumeId);
	    	  if  (OpenstackVolumeStates.inuse.toString().equals(openstackVolume.getStatus())) {
	    		  logger.log(IJavaEeLog.SEVERITY_WARNING, this.getClass().getName(), "Volume :" + openstackVolume.getId() + " is still attached", null); 
	    		  ActionResponse response = openstackClient.detachVolume(volume.storageVolumeId);
	    		  if (!response.isSuccess())
	    			  throw new CloudClientException(response.toString());
	    		  openstackVolume = openstackClient.getVolume(volume.storageVolumeId);
	    		  while(!OpenstackVolumeStates.available.toString().equals(openstackVolume.getStatus())){
	    			  try {
	    				  Thread.sleep(1000);
	    				  openstackVolume = openstackClient.getVolume(volume.storageVolumeId);
	    			  } catch (InterruptedException ie) {
	    				  //$JL-EXC$
	    			  }
	    		  }
	    	  } else if (!OpenstackVolumeStates.available.toString().equals(openstackVolume.getStatus())) {
	    		  logger.log(IJavaEeLog.SEVERITY_WARNING, this.getClass().getName(), "Volume :" + openstackVolume.getId() + " is in unexpected state: " + openstackVolume.getStatus() + ".Operation may fail.", null);  
	    	  }
	    	  openstackClient.deleteVolume(volume.storageVolumeId);
	    	} catch (CloudClientException e) {
	    	   logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "deleteVolumes:" + e.getMessage(), null,e);
			   logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
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
	public StorageOperationResponse<FinalizeCloneVolumesResponse> finalizeCloneVolumes(
			FinalizeCloneVolumesRequest request) {
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "finalizeCloneVolumes: request:" + request.postProcessCloneVolumesResult, null);
		StorageOperationResponse<FinalizeCloneVolumesResponse> response = new StorageOperationResponse<FinalizeCloneVolumesResponse>();
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		FinalizeCloneVolumesResponse payload = new FinalizeCloneVolumesResponse();
		response.setPayload(payload);
		return response;
	}

	@Override
	public StorageOperationResponse<GenerateUniqueIdsResponse> generateUniqueIds(
			GenerateUniqueIdsRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "generateUniqueIds: suggested ids:" + request.sapLvmSuggestedUniqueIds, null);
	    List<UniqueId> sapLvmSuggestedUniqueIds = request.sapLvmSuggestedUniqueIds;
		GenerateUniqueIdsResponse payload = new GenerateUniqueIdsResponse();
		String targetRegion = null;
		for (UniqueId id:sapLvmSuggestedUniqueIds) {
			targetRegion  =  id.targetStorageSystem.storageSystemId.substring(id.targetStorageSystem.storageSystemId.indexOf(':')+1);
			id.identifier = targetRegion + ":" + OpenstackAdapterUtil.generateOperationId();
		}
		payload.uniqueIds=sapLvmSuggestedUniqueIds;
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "generateUniqueIds:  returned ids:" + request.sapLvmSuggestedUniqueIds, null);
		StorageOperationResponse<GenerateUniqueIdsResponse> response = new StorageOperationResponse<GenerateUniqueIdsResponse>(payload);
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;
	}

	@Override
	public CloningCharacteristicsResponse getGlobalCloningCharacteristics(
			CloningCharacteristicsRequest request) {
		return cloningCharacteristics;
	}

	@Override
	public StorageOperationResponse<PostProcessCloneVolumesResponse> postProcessCloneVolumes(
			PostProcessCloneVolumesRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "postProcessCloneVolumes: request:" +request, null);
	    OpenstackBlockCloneVolumesContext context = (OpenstackBlockCloneVolumesContext)request.cloneVolumesResult;
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
	public StorageOperationResponse<PrepareCloneVolumesResponse> prepareCloneVolumes(
			PrepareCloneVolumesRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "prepareCloneVolumes: " + request, null);
		ArrayList<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
		MountData smData;
		MountData tmData;
		for (VolumeToBeCloned volume:request.volumesToBeCloned) {
			if(volume.targetStorageSystemId == null) {
				volume.targetStorageSystemId = volume.sourceStorageSystemId;
			}
			
			if (volume.targetStoragePoolId == null) {
				try {
					
				  String zone =openstackClient.listAvailabilityZones(volume.sourceVolumeId).get(0);
				  volume.targetStoragePoolId = volume.sourceVolumeId.substring(0,volume.sourceVolumeId.indexOf(':')) + ":" + zone;
	
				} catch (Exception e) {
		    	  logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "prepareCloneVolumes:" + e.getMessage(), null,e);
			   	  logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
		    	}
			}
			for (int i = 0; i < volume.sourceMountConfiguration.size();i++) {
				smData = volume.sourceMountConfiguration.get(i);
				tmData = volume.targetMountConfiguration.get(i);
				tmData.fsType = smData.fsType;
				tmData.mountOptions = smData.mountOptions;
				tmData.storageType = smData.storageType;
			}
			//TODO - validate that target availabilityZone is the same as where target host is.
		}
		
		PrepareCloneVolumesResponse payload = new PrepareCloneVolumesResponse();
		payload.prepareCloneVolumeResult = request;
		StorageOperationResponse<PrepareCloneVolumesResponse> response =  new StorageOperationResponse<PrepareCloneVolumesResponse>(payload);
	    if (logMessages.isEmpty()) {
		  response.setPercentCompleted(100);
	      response.setStatus(StorageOperationStatus.COMPLETED);
	    } else {
	      response.setLogMessages(logMessages);
	      response.setStatus(StorageOperationStatus.FAILED);
	    }
	    return response;
	}
	
	
	@Override
	public StorageOperationResponse<RetrieveAvailableTargetPoolsResponse> retrieveAvailableTargetPools(
			RetrieveAvailableTargetPoolsRequest request) {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableTargetPools: request:" +request, null);
	    GetStoragePoolsRequest getPools = new GetStoragePoolsRequest();
	    getPools.storageSystemId = request.targetStorageSystemId;
	    StorageOperationResponse<GetStoragePoolsResponse> getPoolsResponse =  storageRetreival.getStoragePools(getPools);
	    if(getPoolsResponse.getStatus().equals(StorageOperationStatus.FAILED)) {
	      return StorageAdapterImplHelper.createFailedResponse(getPoolsResponse.getLogMessages(), RetrieveAvailableTargetPoolsResponse.class);
	    }
	    RetrieveAvailableTargetPoolsResponse payload = new RetrieveAvailableTargetPoolsResponse();
	    Server instance = null;
	    if (request.maskingProperties != null && request.maskingProperties.targetPhysicalHostnames != null && !request.maskingProperties.targetPhysicalHostnames.isEmpty()) {
		    for (String hostname:request.maskingProperties.targetPhysicalHostnames) {
		    	try {
		    		String region =  openstackClient.getOpenstackId(request.targetStorageSystemId);
		    		try {
					      InetAddress address = InetAddress.getByName(hostname); 
					     
					      instance = openstackClient.findInstanceByInternalIP(region, address.getHostAddress());
					   } catch (UnknownHostException uhe) {
						   logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableStoragePools:" + uhe.getMessage(), null,uhe);
						
						   throw uhe;
					   }
		    	
		    		if (instance == null) continue; else break;
	
		    	} catch (Exception e) {
		    		logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableTargetPools: " + e.getMessage(), null, e);
		    		continue;
		    	}
		    }
		    if (instance == null) {
		    	logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableTargetPools: Target Openstack instance not found - target availability zone must be selected manually", null);
	    		payload.availableTargetPools = getPoolsResponse.getPayload().storagePools;
		    } else {
		        payload.availableTargetPools = new ArrayList<StoragePool>();
			    Iterator<StoragePool> iterator = getPoolsResponse.getPayload().storagePools.iterator();
			    String availabilityZone = null;
			    StoragePool pool = null;
			    while(iterator.hasNext()) {
			      pool =  iterator.next();
			      availabilityZone = pool.name;
			      if (instance.getAvailabilityZone().equals(availabilityZone)) {
			    	  payload.availableTargetPools.add(pool);
			    	  break;
			      }
			    }
		    }
	    } else {
	        payload.availableTargetPools = getPoolsResponse.getPayload().storagePools;
	    }
	    logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveAvailableTargetPools: found:" + payload.availableTargetPools.size() + " pools: " + payload.availableTargetPools, null);
	    StorageOperationResponse<RetrieveAvailableTargetPoolsResponse> response = new StorageOperationResponse<RetrieveAvailableTargetPoolsResponse>(payload);
	    response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
	    return response;
	}

	/* (non-Javadoc)
	 * @see com.sap.tc.vcm.storage.adapter.api.cloning.IStorageCloning#retrieveAvailableTargetSystems(com.sap.tc.vcm.storage.adapter.api.cloning.RetrieveAvailableTargetSystemsRequest)
	 */
	@Override

	public StorageOperationResponse<RetrieveAvailableTargetSystemsResponse> retrieveAvailableTargetSystems(
			RetrieveAvailableTargetSystemsRequest request) {
		
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
	public StorageOperationResponse<RetrieveAvailableTargetVolumesResponse> retrieveAvailableTargetVolumes(
			RetrieveAvailableTargetVolumesRequest request) {
		
		//we don't support this feature for Block volumes 
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
	public CloningValidationResponse validateCloneRequest(
			PrepareCloneVolumesRequest request) {
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "validateCloneRequest: " + request, null);	
		// Would be nice to check here if we are  not exceeding the Storage quota 
	
		return new CloningValidationResponse();
		
		
	}
	
	/**
	 * @param operationId
	 * @param context
	 * @return StorageOperationResponse
	 * @throws InfrastructAdapterException
	 */
	public StorageOperationResponse<PostProcessCloneVolumesResponse> cancelVolumes(
			StorageOperationId operationId, OpenstackBlockCloneVolumesContext context)throws InfrastructAdapterException {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelVolumes: operationId: " + operationId  + " volumeStauts: " + context.volumeStatus, null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
		for (OpenstackBlockCloneVolumeStatus status:context.volumeStatus) {
	    	if (status.sourceSnapshotId != null){
	    	    try {
	    		   openstackClient.deleteSnapshot(status.sourceSnapshotId);
	    	    } catch (CloudClientException e) {
	    	       logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelVolumes:" + e.getMessage(), null,e);
		   	       logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
	    	    }
	    	}
	    	if (status.targetSnapshotId != null){
	    	    try {
	    		   openstackClient.deleteSnapshot(status.targetSnapshotId);
	    	
	    	    } catch (CloudClientException e) {
	    	       logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelVolumes:" + e.getMessage(), null,e);
		   	       logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
	    	    }
	    	}
	    	if (status.targetVolumeId != null){
	    	    try {
	    		   openstackClient.deleteVolume(status.targetVolumeId);
	    		
	    	    } catch (Exception e) {
	    	       logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "cancelVolumes:" + e.getMessage(), null,e);
		   	       logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
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
	 * @return StorageOperationResponse
	 * @throws InfrastructAdapterException
	 */
	public  StorageOperationResponse<PostProcessCloneVolumesResponse> getOperationStatus(
			StorageOperationId operationId, OpenstackBlockCloneVolumesContext context)
			throws InfrastructAdapterException {
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " context: " + context, null);
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();
	    VolumeSnapshot snapshot = null;
		Status snapshotState = null;
	    String snapshotId = null;
	    int progress = 0;
	    int operationProgress = 0; 
	    boolean pending = false;
	    boolean failed = false;
	//    boolean snapshotsReady = true;
	    
	    //STEP 1 : Check sourceSnapshot State
		for (OpenstackBlockCloneVolumeStatus status:context.volumeStatus) {
	    	try {
	    	   snapshotId = status.sourceSnapshotId;
	    	   logger.log(IJavaEeLog.SEVERITY_DEBUG,this.getClass().getName()," //STEP 1 : Check sourceSnapshot State "+   snapshotId ,null);
	    	   if (status.sourceSnapshotComplete) continue;//snapshot already completed
	    	   snapshot = openstackClient.getSnapshotStatus(snapshotId);
	    	   snapshotState  = snapshot.getStatus();
	    	   logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " checking snapshot: " + snapshotId + " state: " + snapshotState, null);
	    	   String snapshotStateString=snapshotState.toString();
	    	   if (snapshotStateString.equals("pending")|| (snapshotStateString.equals("creating"))) {
  		    	 
  		    
	    		   String percent = "0";//snapshot.getProgress(); //progress not tracked in Openstack
//	    		   if (percent.isEmpty()) {
//	    			   percent = "0";
//	    		   } else {
//	    			   percent = percent.substring(0,percent.length() -2);
//	    		   }
	    		   progress = Integer.parseInt(percent); 
	    		   if (progress < operationProgress) operationProgress = progress;
	    		   pending = true;
	    //		   snapshotsReady = false;
	    		   continue;
	    	   } else if  (snapshotState.name().equals("AVAILABLE")) { 
	    		  status.sourceSnapshotComplete = true;
	    		  logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " completed source snapshot: " + snapshotId, null);

//Clone of remote volumes not currently implemented	    		  
//	    		  if (isRemoteClone(status.volumeToBeCloned)) {
//	    			  createTargetSnapshot(snapshotId, status, operationId.toString());
//	    		   } else {
		    		  createTargetVolume(context, status, true);
//	    		  }
	    	   } else if (snapshotState.toString().equals("error")) {
	    		   logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " failed: " + snapshotId, null);
		    	   logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), "Failed to Clone volume " + status.volumeToBeCloned.sourceVolumeId +". Snapshot process terminated with ERROR") );	
	    	       failed = true;
	    		   break;
	    	   } else {
	    		   throw new IllegalStateException("Failed to Clone volume " + status.volumeToBeCloned.sourceVolumeId +". Snapshot process returned unexpected status: " + snapshotState);
	    	   }
	    	} catch (Exception e) {
	    	   failed = true;
	    	   logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
	   	       logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(),  "getOperationStatus:" + e.getMessage()));	
	    	}
	    }
		//STEP 2: Check Target Snapshot state (for cross-region only) 
		for (OpenstackBlockCloneVolumeStatus status:context.volumeStatus) {
		  if (isRemoteClone(status.volumeToBeCloned)) {
			  if (status.targetSnapshotId != null && !status.targetSnapshotComplete) {
				  try {
					snapshotId = status.targetSnapshotId;
				    snapshot = openstackClient.getSnapshotStatus(snapshotId);
		    	    snapshotState  = snapshot.getStatus();
		    	    logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " checking snapshot: " + snapshotId + " state: " + snapshotState, null);
		    	    if (snapshotState.toString().equals("pending")|| (snapshotState.toString().equals("creating"))) {
			    		  // logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + "checking snapshot:" + snapshotId + " progress:" + snapshot.getProgress(), null);
			    		   //TODO: openstack doesn't have percent complete so map percentages based on state 0, 50,100
			    		   String percent = "0";//snapshot.getStatus()
			    		   if (percent.isEmpty()) {
			    			   percent = "0";
			    		   } else {
			    			   percent = percent.substring(0,percent.length() -2);
			    		   }
			    		   progress = Integer.parseInt(percent); 
			    		   if (progress < operationProgress) operationProgress = progress;
			    		   pending = true;
//			    		   snapshotsReady = false;
			    		   continue;
			    	   } else if (snapshotState.equals("completed")) { 
			    		  status.targetSnapshotComplete = true;
			    		  logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " completed target snapshot: " + snapshotId, null);
			    		  createTargetVolume(context, status, true);
			    	   } else if (snapshotState.equals("error")) {
			    		   logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " failed: " + snapshotId, null);
				    	   logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), "Failed to Clone volume " + status.volumeToBeCloned.sourceVolumeId +". Snapshot process terminated with ERROR") );	
			    	       failed = true;
			    		   break;
			    	   } else {
			    		   throw new IllegalStateException("Failed to Clone volume " + status.volumeToBeCloned.sourceVolumeId +". Snapshot process returned unexpected status: " + snapshotState);
			    	   }
				  } catch (CloudClientException e) {
				     failed = true;
				     logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
				   	 logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
				     break;
				  }
				  
			  }
		  }
		}
		//STEP 3: Check Target Volume state
		Status volumeState = null;
		Volume volume = null;
		for (OpenstackBlockCloneVolumeStatus status:context.volumeStatus) {
			if (status.targetVolumeId == null) continue;
			try {
			    volume = openstackClient.getVolume(status.targetVolumeId);
			     if (volume == null) {
	                failed = true;
				    logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), "Volume " + status.targetVolumeId + " not found"));	
			        break;
			     }
	
		    } catch (Exception e) {
		         failed = true;
		         logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
		   	     logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
		         break;
		     }
		     volumeState = volume.getStatus();
	   	    logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " checking volume: " + volume.getId() + " state: " + volumeState, null);
	         if ((volumeState.equals(Status.CREATING))||(volumeState.equals(Status.ATTACHING))){
	    		pending = true;
	    		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " in progress: " + volume.getId(), null);
	    	} else if (volumeState.equals(Status.ERROR)) {
		    	
		         logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " create volume failed: " + volume.getId(), null);
		         logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), "Failed to Clone volume " + volume.getId() + ". Process terminated with ERROR") );	
		         failed = true;
		    	 break;
		     } else if (volumeState.equals(Status.AVAILABLE)) {
		    	 logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + operationId + " completed volume: " + volume.getId(), null);
		    	  status.targetVolumeComplete = true;
		    	  //once volume is ready we can delete the temporary snapshot as it is no longer needed. 
		    	 
		    	
		    	 if (status.targetSnapshotComplete) {
		    		 snapshotId = status.targetSnapshotId;
		    	 } else {
		    		 snapshotId = status.sourceSnapshotId;
		    	 }
		    	 try{
		    		snapshot = (VolumeSnapshot) openstackClient.getSnapshot(snapshotId);
		    		//set to true to delete snapshots when no longer needed 
			    	boolean forDelete = false;
			    
			    		  if (forDelete) {
			    	         openstackClient.deleteSnapshot(snapshotId);
			    		  }
		    		  }
		    		
	
		    	 catch (Exception e) {
		    			logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus:" + e.getMessage(), null,e);
		    			//here we just log warning as failing to delete the temporary snapshot does not fail the clone operation 
		    			logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_WARNING, "OSBlock", System.currentTimeMillis(), "Failed delete temorary snapshot " + volume.getSnapshotId() + ". " + e.getMessage()));	
		    		}
		    	} else {
		    		logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), "Unexpected volume state: " + volumeState.name()) );	
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
	    	String device = null;
	    	String sourceExportPath = null;
	    	for (OpenstackBlockCloneVolumeStatus state:context.volumeStatus) {
	    		state.volumeToBeCloned.targetVolumeId = state.targetVolumeId;
	    		if(state.volumeToBeCloned.sourceMountConfiguration.size() > 0) {
		    		sourceExportPath = state.volumeToBeCloned.sourceMountConfiguration.get(0).exportPath;
		    		for (MountData mountData:state.volumeToBeCloned.targetMountConfiguration) {
		    			 device = sourceExportPath.substring(sourceExportPath.lastIndexOf(':')+1);
		    		     mountData.exportPath = state.targetVolumeId + ":" + device;
		    		     payload.mountConfiguration.add(mountData);
		    		}
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
	
	
	
	
	
	
	private void createTargetVolume(OpenstackBlockCloneVolumesContext context, OpenstackBlockCloneVolumeStatus status, boolean source) throws  CloudClientException {
		  String snapshotId;
		  if (source) {
			  snapshotId=status.sourceSnapshotId;
		  } else {
			  snapshotId = status.volumeToBeCloned.targetVolumeId;
		  }
		  String ebsType = null;
	
		
		  String volumeId = status.volumeToBeCloned.sourceVolumeId;
	      Volume sourceVolume = openstackClient.getVolume(volumeId);
	      ebsType = sourceVolume.getVolumeType();

		
		  String region = openstackClient.getRegion(snapshotId);
		  String name="clone of "+status.volumeToBeCloned.sourceVolumeId;
		  Volume volume = openstackClient.createVolume(snapshotId,status.volumeToBeCloned.targetStoragePoolId,ebsType,name);
			  status.targetVolumeId = region+ ":" +volume.getId();
		  logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getOperationStatus: operationId: " + context.operationId + " creating volume: " + volume.getId(), null);
	}
	
	
	private void createTargetSnapshot(String snapshotId, OpenstackBlockCloneVolumeStatus status,String operationId) throws CloudClientException {
		 
		  String[] values = new String[2];
		  values[1] = operationId;
		  String targetRegion = status.volumeToBeCloned.targetStorageSystemId.substring(status.volumeToBeCloned.targetStorageSystemId.indexOf(':')+1);
		  VolumeSnapshot snapshot = openstackClient.copy(snapshotId, targetRegion , "LVM Snapshot to clone Volume " + status.volumeToBeCloned.sourceVolumeId);
		
		  status.targetSnapshotId = targetRegion + ':' + snapshot.getId();
      
	}
	
	private boolean isRemoteClone(VolumeToBeCloned volume) {
		return false;
	
	}
	        
				
	
	
	
	
	
	@SerializableClass
	public static class OpenstackBlockCloneVolumesContext implements IStorageOperationContext {

		@SerializableField
		public List<OpenstackBlockCloneVolumeStatus> volumeStatus;
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
	public static class OpenstackBlockCloneVolumeStatus implements IStorageOperationContext {

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
