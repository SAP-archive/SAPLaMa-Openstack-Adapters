package com.sap.lvm.storage.openstack.block; 

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.Volume.Status;

import com.sap.lvm.CloudClientException;
import com.sap.lvm.storage.openstack.util.OpenstackConstants.OpenstackAttachmentStatus;
import com.sap.lvm.storage.openstack.util.OpenstackConstants.OpenstackVolumeStates;
import com.sap.lvm.storage.openstack.util.StorageAdapterImplHelper;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageLogMessage;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageOperationStatus;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.IStorageMappingAndMasking;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.PostDetachVolumeRequest;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.PostDetachVolumeResponse;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.PreAttachVolumeRequest;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.PreAttachVolumeResponse;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.PrepareForMountRequest;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.PrepareForMountResponse;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.UnprepareForMountRequest;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.UnprepareForMountResponse;
import com.sap.tc.vcm.storage.adapter.api.types.MaskingProperties;
import com.sap.tc.vcm.storage.adapter.api.types.MountData;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolumeDetails;



public class OpenstackBlockStorageMappingAndMasking implements IStorageMappingAndMasking {

	private OpenstackBlockCloudStorageController openstackClient = null;
	private IJavaEeLog logger;

	public OpenstackBlockStorageMappingAndMasking(OpenstackBlockCloudStorageController openstackClient, IJavaEeLog logger) {
		this.openstackClient = openstackClient;
		this.logger = logger;
	}


	@Override
	public StorageOperationResponse<PostDetachVolumeResponse> postDetachVolume(
			PostDetachVolumeRequest request) {

		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "postDetachVolume: properties:" + request.maskingProperties + " volumes: " + request.volumes, null);

		MaskingProperties masking  = null;
		String instanceId = null;
		String expectedInstanceId = null;
		Volume volume = null;
		List<StorageLogMessage> logMessages = new ArrayList<StorageLogMessage>();

		for (StorageVolumeDetails volumeDetails:request.volumes.keySet()) {
			try {
				volume = openstackClient.getVolume(volumeDetails.storageVolume.storageVolumeId);
				if (volume == null) {
					logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), "Volume: " + volumeDetails.storageVolume.storageVolumeId + " does not exist"));
					continue;
				}
				//check if volume is in the correct state - attached - if not, fail the operation as state is undefined
				if (OpenstackVolumeStates.inuse.toString().equals(volume.getStatus().toString())) {
					masking = request.maskingProperties.get(volume);
					if (masking!=null)
					expectedInstanceId = masking.hostProperties.get(masking.targetPhysicalHostnames.get(0)).getProperty(MaskingProperties.PROP_KEY_VIRTUAL_RESOURCE_ID);
					instanceId = volume.getAttachments().get(0).getId();
					//check if the volume is attached to the correct instance - if not, do not detach as state is undefined
					//if expectedInstanceId is null then skip this check
					
					if ((expectedInstanceId==null) || (expectedInstanceId.equals(instanceId))){
						ActionResponse response = openstackClient.detachVolume(volumeDetails.storageVolume.storageVolumeId);
						if (!response.isSuccess())
							throw new CloudClientException(response.toString());
					} else {
						logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(),
								"Volume: " + volumeDetails.storageVolume.storageVolumeId + " is attached to instance: " + instanceId + " but expected instance is: " + expectedInstanceId)); 
					}
				} 
				else
				 if (OpenstackVolumeStates.available.toString().equals(volume.getStatus().toString())) 
				{	logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "postDetachVolume: Volume: " + volumeDetails.storageVolume.storageVolumeId + " is already in expected state: " + volume.getStatus() + " for the operation", null);
				
//					logMessages.add(new StorageLogMessage(Severity.INFO, "OSBlock", System.currentTimeMillis(),
//							"Volume: " + volumeDetails.storageVolume.storageVolumeId + " is already in expected state: " + volume.getStatus() + " for the operation"));
				}
				else
				
				{
					logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(),
							"Volunme: " + volumeDetails.storageVolume.storageVolumeId + " is in unexpected state: " + volume.getStatus() + " for the operation"));
				}
			
			} catch (Exception e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "postDetachVolume:" + e.getMessage(), null,e);
				logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
			}
		}

		//if no errors - wait until all volumes reach detached (available) state.
		if (logMessages.isEmpty()) {
			for (StorageVolumeDetails volumeDetails:request.volumes.keySet()) {
				try { 
					while(!openstackClient.getVolume(volumeDetails.storageVolume.storageVolumeId).getStatus().toString().equals(OpenstackVolumeStates.available.toString()))
 {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException ie) {
							//$JL-EXC$
						}
					}

				} catch (Exception e) {

					logMessages.add(new StorageLogMessage(IJavaEeLog.SEVERITY_ERROR, "OSBlock", System.currentTimeMillis(), e.getMessage()));	
				}
			}
		}

		if (logMessages.isEmpty()) {
			PostDetachVolumeResponse payload = new PostDetachVolumeResponse();
			StorageOperationResponse<PostDetachVolumeResponse> response = new StorageOperationResponse<PostDetachVolumeResponse>(payload);
			response.setPercentCompleted(100);
			response.setStatus(StorageOperationStatus.COMPLETED);
			return response;
		} else {
			return StorageAdapterImplHelper.createFailedResponse(logMessages, PostDetachVolumeResponse.class); 
		}
	}

	@Override
	public StorageOperationResponse<PreAttachVolumeResponse> preAttachVolume(
			PreAttachVolumeRequest request) {

		MaskingProperties masking  = null;
		MountData mountData = null;
		List<MountData> mounts;
		String device = null;
		String instanceId = null;
		String expectedInstanceId = null;
		Volume volume = null;
		Status currentState = null;


		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "preAttachVolume: properties:" + request.maskingProperties +" volumes: " + request.volumes, null);


		//prepare for attaching volumes to the target hosts - first they need to be detached from the source hosts
		HashMap <String,String>volumesToAttach=new HashMap<String,String>();
		for (StorageVolumeDetails volumeDetails:request.maskingProperties.keySet()) {
			masking = request.maskingProperties.get(volumeDetails);
			if (masking.sourcePhysicalHostnames != null && masking.sourcePhysicalHostnames.size() > 0) {
				if (masking.targetPhysicalHostnames != null) {
					//if source and target physical hostnames are the same ( block volume can be only attached to 1 instance at a time) no need to detach/re-attach 
					if (sameSourceAndTargetHost(masking)) {
						try {
							volume = openstackClient.getVolume(volumeDetails.storageVolume.storageVolumeId);
							if (volume == null) {
								return StorageAdapterImplHelper.createFailedResponse("Volume: " + volumeDetails.storageVolume.storageVolumeId + " does not exist",PreAttachVolumeResponse.class); 
							}
					
						} catch (Exception e) {
							logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG , this.getClass().getName(), "preAttachVolume:", null, e);
							return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), PreAttachVolumeResponse.class); 
						}
						//check if volume is in the correct state - attached - if not, fail the operation as state is undefined
						if (OpenstackVolumeStates.inuse.toString().equals(volume.getStatus())) {
							masking = request.maskingProperties.get(volume);
							expectedInstanceId = masking.hostProperties.get(masking.targetPhysicalHostnames.get(0)).getProperty(MaskingProperties.PROP_KEY_VIRTUAL_RESOURCE_ID);
							
							//check if the volume is attached to the correct instance - if not, do not detach as state is undefined
							if (expectedInstanceId.equals(instanceId)){
								continue;
							} else {
								return StorageAdapterImplHelper.createFailedResponse("Volume: " + volumeDetails.storageVolume.storageVolumeId + " is attached to instance: " + instanceId + " but expected instance is: " + expectedInstanceId,PreAttachVolumeResponse.class); 
							}
						} else {
							return StorageAdapterImplHelper.createFailedResponse("Volunme: " + volumeDetails.storageVolume.storageVolumeId + " is in unexpected state: " + volume.getStatus() + " for the operation",PreAttachVolumeResponse.class);
						}

					} else {
						// when target and source physical hosts are different - volume must be detached first from the source.
						try {
							volume = openstackClient.getVolume(volumeDetails.storageVolume.storageVolumeId);
							if (volume == null) {
								return StorageAdapterImplHelper.createFailedResponse("Volume: " + volumeDetails.storageVolume.storageVolumeId + " does not exist",PreAttachVolumeResponse.class); 
							}
							if (OpenstackVolumeStates.inuse.toString().equals(volume.getStatus())) {
								masking = request.maskingProperties.get(volume);
								expectedInstanceId = masking.hostProperties.get(masking.sourcePhysicalHostnames.get(0)).getProperty(MaskingProperties.PROP_KEY_VIRTUAL_RESOURCE_ID);
								instanceId = volume.getAttachments().get(0).getId();
								//check if the volume is attached to the correct instance - if not, do not detach as state is undefined
								if (expectedInstanceId.equals(instanceId)){
									ActionResponse response = openstackClient.detachVolume(volumeDetails.storageVolume.storageVolumeId);
									if (!response.isSuccess())
										throw new CloudClientException(response.toString());
								} else {
									return StorageAdapterImplHelper.createFailedResponse("Volume: " + volumeDetails.storageVolume.storageVolumeId + " is attached to instance: " + instanceId + " but expected instance is: " + expectedInstanceId,PreAttachVolumeResponse.class); 
								}
							} else {
								return StorageAdapterImplHelper.createFailedResponse("Volunme: " + volumeDetails.storageVolume.storageVolumeId + " is in unexpected state: " + volume.getStatus() + " for the operation",PreAttachVolumeResponse.class);
							}
					
						} catch (Exception e) {
							logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG , this.getClass().getName(), "preAttachVolume:", null, e);
							return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), PreAttachVolumeResponse.class); 
						}
					}
				}
			}

			//attach volumes to given target hosts
			if (masking.targetPhysicalHostnames != null) {
				try {
					if (sameSourceAndTargetHost(masking)) continue;
					instanceId = masking.hostProperties.get(masking.targetPhysicalHostnames.get(0)).getProperty(MaskingProperties.PROP_KEY_VIRTUAL_RESOURCE_ID);
					mounts = request.volumes.get(volumeDetails);
					if (mounts == null || mounts.isEmpty()) {
						return StorageAdapterImplHelper.createFailedResponse("Invalid Mount Data", PreAttachVolumeResponse.class); 
					}
					mountData = mounts.get(0);
					device = mountData.getRemotePath().substring(mountData.getRemotePath().lastIndexOf(':')+1); 
					volume = openstackClient.getVolume(volumeDetails.storageVolume.storageVolumeId);
					if (volume == null) {
						return StorageAdapterImplHelper.createFailedResponse("Volume: " + volumeDetails.storageVolume.storageVolumeId + " does not exist",PreAttachVolumeResponse.class); 
					}
					
					currentState = volume.getStatus();
				
					//volumes may still be detaching from the source host so we have to wait until detached.
				
					if (currentState.toString().equals(OpenstackAttachmentStatus.attaching.name())) { //wait if volumes are still attaching
						while(!openstackClient.getVolume(volumeDetails.storageVolume.storageVolumeId).getStatus().equals(OpenstackVolumeStates.available.toString())) {
							try {
								Thread.sleep(1000);
								logger.log(IJavaEeLog.SEVERITY_DEBUG ,  "Openstack preAttachVolume:","Waiting for volume "+volumeDetails.storageVolume.storageVolumeId+" to attach to instance:"+instanceId,null);
							} catch (InterruptedException ie) {
								//$JL-EXC$
							}
						}
					}
					if (currentState.equals(OpenstackAttachmentStatus.detached.toString())||currentState.toString().equalsIgnoreCase(OpenstackVolumeStates.available.toString())) {//attach if detached
						volumesToAttach.put(device,volumeDetails.storageVolume.storageVolumeId+"##"+ instanceId );
					

					} else if ((currentState.equals(OpenstackAttachmentStatus.attached.name()))||(currentState.toString().equals(Status.IN_USE.toString()))) {
						if (volume.getAttachments().get(0).getServerId().equals(instanceId)) {
							continue;
						} else {
							return StorageAdapterImplHelper.createFailedResponse("Volume: " + volumeDetails.storageVolume.storageVolumeId + " is attached to instance: " + volume.getAttachments().get(0).getId() + " but expected instance is: " + instanceId , PreAttachVolumeResponse.class);  
						}
					} else {
						//fail in all other states
						return StorageAdapterImplHelper.createFailedResponse("Volume " + volumeDetails.storageVolume.storageVolumeId + " is in invalid state: "+currentState+" for the operation", PreAttachVolumeResponse.class);  
					}
					//  	    } catch (CloudClientException e) {
				} catch (Exception e) {
					logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG , this.getClass().getName(), "preAttachVolume:", null, e);
					return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), PreAttachVolumeResponse.class); 
				}
			} else {
				return StorageAdapterImplHelper.createFailedResponse("Invalid attach request", PreAttachVolumeResponse.class); 
			}

		}
		
		//Openstack quirk: the VM may ignore the specified device name e.g. if you try to attach volume X as device /dev/vdc then if /dev/vdb does not yet
		//exist it will attach the volume to /dev/vdb BUT it reports back that it's attached to /dev/vdc
		//The workaround here is to sort the volumes by device name before attaching 
		
		List<String> keys = new ArrayList<String>(volumesToAttach.keySet());
		Collections.sort(keys);
		for (String deviceName: keys){
			String[]	volumeAndInstance=volumesToAttach.get(deviceName).split("##");
			String volumeid=volumeAndInstance[0];
			String instanceid=volumeAndInstance[1];
			logger.log(IJavaEeLog.SEVERITY_INFO , this.getClass().getName(),"attaching "+deviceName+" with volumeID "+volumeid +" to:"+instanceid , null);

//			if (OpenstackVolumeStates.available.toString().equals(updatedStatus)) 	
				openstackClient.attachVolume(volumeid, instanceid, deviceName);
//			else 
//				logger.log(IJavaEeLog.SEVERITY_INFO , this.getClass().getName(),"cannot attach "+deviceName+" with volumeID "+volumeid +" to:"+instanceid+"; status must be available but found status:"+updatedStatus , null);
			
			int counter=0;
			while((!openstackClient.getVolume(volumeid).getStatus().toString().equals(OpenstackVolumeStates.inuse.toString())&& (counter<60))) {
				try {
					counter++;
					Thread.sleep(1000); 
				} catch (InterruptedException ie) {//$JL-EXC$
				}
			}
		}


		PreAttachVolumeResponse payload = new PreAttachVolumeResponse();
		StorageOperationResponse<PreAttachVolumeResponse> response = new StorageOperationResponse<PreAttachVolumeResponse>(payload);
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;
	}
	
	private boolean sameSourceAndTargetHost(MaskingProperties masking) {
		if (masking != null && masking.sourcePhysicalHostnames != null) {
			  //if target and source host is the same then no need to re-attach
			  for(String source:masking.sourcePhysicalHostnames){
					for (String target:masking.targetPhysicalHostnames) {
						if (source.equals(target)) {
							return true;
						}
					}
			  }
		  }
	      return false;
	}


	@Override
	public StorageOperationResponse<PrepareForMountResponse> prepareForMount(
			PrepareForMountRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public StorageOperationResponse<UnprepareForMountResponse> unprepareForMount(
			UnprepareForMountRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}


