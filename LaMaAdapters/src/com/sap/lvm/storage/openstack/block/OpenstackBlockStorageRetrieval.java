package com.sap.lvm.storage.openstack.block;
 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.openstack4j.model.storage.block.BlockLimits;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeAttachment;

import com.sap.lvm.storage.openstack.util.OpenstackAdapterUtil;
import com.sap.lvm.storage.openstack.util.OpenstackConstants;
import com.sap.lvm.storage.openstack.util.StorageAdapterImplHelper;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageOperationStatus;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetClusterStorageSystemsRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetClusterStorageSystemsResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetDeviceVolumeMappingResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStoragePoolsRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStoragePoolsResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageSystemsRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageSystemsResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.IStorageRetrievalExt1;
import com.sap.tc.vcm.storage.adapter.api.retrieval.RetrieveVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.RetrieveVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.types.MountData;
import com.sap.tc.vcm.storage.adapter.api.types.StoragePool;
import com.sap.tc.vcm.storage.adapter.api.types.StorageSystem;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolume;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolumeDetails;



public class OpenstackBlockStorageRetrieval implements IStorageRetrievalExt1 {
	
	private OpenstackBlockCloudStorageController openstackClient = null;
	private String accountId;
	private IJavaEeLog logger;
	
	public OpenstackBlockStorageRetrieval(OpenstackBlockCloudStorageController openstackClient,IJavaEeLog logger) {
	   this.openstackClient = openstackClient;
	   accountId = openstackClient.getAccountId();	
	   this.logger = logger;
	}

	@Override
	public synchronized StorageOperationResponse<GetStoragePoolsResponse> getStoragePools(
			GetStoragePoolsRequest request) {
		
		// storage pools id format = region:availability_zone
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStoragePools: request:" + request, null);
		GetStoragePoolsResponse payload = new GetStoragePoolsResponse();
		ArrayList<StoragePool> storagePools = new ArrayList<StoragePool>();
		List<String> zones = null;
		 BlockLimits blockLimits=openstackClient.getBlockStorageLimits();
		 long totalSpaceGB=blockLimits.getAbsolute().getMaxTotalVolumeGigabytes();
		 long  usedSpaceGB=blockLimits.getAbsolute().getTotalGigabytesUsed();
		 
		if (request.storageSystemId != null) {
			String region = openstackClient.getOpenstackId(request.storageSystemId);
			try {
				zones = openstackClient.listAvailabilityZones(region);
		
			} catch (Exception e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStoragePools:" + e.getMessage(), null,e);
			   	return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), GetStoragePoolsResponse.class); 
			}
		
			for (String zone:zones) {
				storagePools.add(OpenstackAdapterUtil.createStoragePool(zone, region, accountId, totalSpaceGB, usedSpaceGB));
			}
		
		} else {
		  if (request.storagePoolIds == null || request.storagePoolIds.isEmpty()) {
			  return StorageAdapterImplHelper.createFailedResponse("Bad request: both storageSystemId and storagePoolIds are null", GetStoragePoolsResponse.class);  
		  }
		  try {
         
			  String zone = null;
			  String region = null;		  
			  for (String poolId:request.storagePoolIds) {
				if (poolId == null || poolId.indexOf(':') == -1) {
					storagePools.add(null);
					continue;
				}
				zone = openstackClient.getOpenstackId(poolId);
				region = openstackClient.getRegion(poolId);
				 if (zone.equals(OpenstackConstants.Openstack_POOL_SNAPSHOTS)) {
					 storagePools.add(OpenstackAdapterUtil.createStoragePool(OpenstackConstants.Openstack_POOL_SNAPSHOTS, region, accountId,totalSpaceGB, usedSpaceGB));
				 } else {
				    zones =  openstackClient.listAvailabilityZones(region);
				    if (zones.contains(zone)) {
				    	storagePools.add(OpenstackAdapterUtil.createStoragePool(zone, region, accountId,totalSpaceGB, usedSpaceGB));	
					} else {
				       storagePools.add(null);
				    }
				 }
			   }

		  } catch (Exception e) {
			logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStoragePools:" + e.getMessage(), null,e);
			return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), GetStoragePoolsResponse.class); 
		  }
		  
		}
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStoragePools: pools found: " + storagePools.size() + " pools: " + storagePools , null);
		payload.storagePools = storagePools;
		StorageOperationResponse<GetStoragePoolsResponse> response = new StorageOperationResponse<GetStoragePoolsResponse>(payload);
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;
	}

	@Override
	public synchronized StorageOperationResponse<GetStorageSystemsResponse> getStorageSystems(
			GetStorageSystemsRequest request) {
		
		//storage system id format = account_id:region
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStorageSystems: " + request.storageSystemIds , null);
		try {
		List<String> requestedSystems = request.storageSystemIds;
		ArrayList<StorageSystem> systemList = new ArrayList<StorageSystem>();
		if (requestedSystems == null||requestedSystems.isEmpty()) {
			List<String> regions = openstackClient.getRegions();
			logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStorageSystems: zones:" + regions , null);
			for (String region : regions) {
		  	  systemList.add(OpenstackAdapterUtil.createStorageSystem(region,accountId));
			}
		} else {
			List<String> zones = openstackClient.getAvailabilityZones();
			boolean found = false;
			for (int i = 0;i<requestedSystems.size();i++) {
			  if(requestedSystems.get(i) == null) {
				  systemList.add(null);
				  continue;
			  }
		      found = false;
			  for(String zone : zones) {
			     if (requestedSystems.get(i).equals(accountId+':'+zone)) {
		    	    systemList.add(OpenstackAdapterUtil.createStorageSystem(zone, accountId));
		    	    found = true;
		    	    break;
		         }
			  }
			  if (!found) {
			    systemList.add(null);
			  }
			}
		}
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStorageSystems: systems found: " + systemList.size() + " systems: " + systemList , null);
		GetStorageSystemsResponse payload = new GetStorageSystemsResponse();
		payload.storageSystems = systemList;
		StorageOperationResponse<GetStorageSystemsResponse> response = new StorageOperationResponse<GetStorageSystemsResponse>(payload);
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;

		  } catch (Exception e) {
			logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStorageSystems:" + e.getMessage(), null,e);
			return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), GetStorageSystemsResponse.class); 
			
		}
	}

	@Override
	public synchronized StorageOperationResponse<GetStorageVolumesResponse> getStorageVolumes(
			GetStorageVolumesRequest request) {
		//volume id format = region:volume_id or region:volume_id_tag for cloned volumes
		
		StorageOperationResponse<GetStorageVolumesResponse> response = null;
		try {
			List<StorageVolume> internalVolumes = null;
			List<Volume> volumeList = null;
			if (request.storageVolumeIds == null || request.storageVolumeIds.isEmpty()) {
				if (request.storageSystemId == null) {
					if (request.storagePoolId == null) {
				    	return StorageAdapterImplHelper.createFailedResponse("Invalid Reqeust: all parameters are null", GetStorageVolumesResponse.class); 
					} else {
						volumeList = openstackClient.listVolumes(request.storagePoolId);
					}
				} else {
					if (request.storagePoolId == null) {
				    	volumeList = new ArrayList<Volume>();
				    	String region = openstackClient.getOpenstackId(request.storageSystemId);
				    //	for (String zone:openstackClient.listAvailabilityZones(request.storageSystemId)) {
				    		volumeList.addAll(openstackClient.listVolumes(region +":" + "nova"));
				    //	}
					} else {
						volumeList = openstackClient.listVolumes(request.storagePoolId);
					}
				}
				internalVolumes = OpenstackAdapterUtil.transformToStorageVolumeList(volumeList,request.storageSystemId);
					
			} else {
			   internalVolumes = new ArrayList<StorageVolume>();
		       for (String volumeId:request.storageVolumeIds) {
		    	   Volume vol = openstackClient.getVolume(volumeId);
		    	   if (vol == null) {
		    		   internalVolumes.add(null);
		    	   } else {
		    		   internalVolumes.add(OpenstackAdapterUtil.toStorageVolume(vol, accountId +':'+ openstackClient.getRegion(volumeId)));
		    	   }
		       }
			}
			
			GetStorageVolumesResponse payload = new GetStorageVolumesResponse();
			payload.storageVolumes = internalVolumes;
			response = new StorageOperationResponse<GetStorageVolumesResponse>(payload);
			response.setPercentCompleted(100);
			response.setStatus(StorageOperationStatus.COMPLETED);

		} catch (Exception e) {
			
			logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG , this.getClass().getName(), "getStorageVolumes:", null, e);
			return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), GetStorageVolumesResponse.class); 
		}
		
		return response;
	}

	@Override
	public synchronized StorageOperationResponse<RetrieveVolumesResponse> retrieveVolumesFromLvmMountConfiguration(
			RetrieveVolumesRequest request) {
		
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration: mountData:" + request.mountData + " hostnames:" + request.serviceIdToHostname, null);
		
		String device;
		String region;
		String volumeId;
		RetrieveVolumesResponse payload = new RetrieveVolumesResponse();
		payload.retrievedVolumes = new LinkedHashMap<MountData, List<StorageVolumeDetails>>();
		ArrayList<StorageVolumeDetails> list = new ArrayList<StorageVolumeDetails>();
		StorageVolumeDetails details;
		StringTokenizer tokenizer;
		for (MountData mountData:request.mountData) {
		   if (!OpenstackConstants.Openstack_VENDOR.equals(mountData.partnerId)) continue;
		      if (mountData.getStorageType().equals("SAN")) {
	//	         serviceId = mountData.serviceId;
		         if (mountData.exportPath == null || !mountData.exportPath.contains(":")) {
		        	 logger.log(IJavaEeLog.SEVERITY_WARNING, this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration: missing exportPath for mountPoint:" + mountData.mountPoint, null);
		     		 continue;
		        	
		         } else {
			         tokenizer = new StringTokenizer(mountData.exportPath,":");
			         region = tokenizer.nextToken();
			         volumeId = tokenizer.nextToken();
			         device = tokenizer.nextToken();
			         if (device == null || !device.startsWith("/dev")) continue;
			         if (volumeId == null) continue;
		         }
		         list = new ArrayList<StorageVolumeDetails>();
		         details = new StorageVolumeDetails();
		         try {
		            details.storageVolume = OpenstackAdapterUtil.toStorageVolume(openstackClient.getVolume(region+":"+volumeId),accountId+":"+region );
		         } catch (Exception e) {
			        logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG , this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration:", null, e);
			        continue;
		         }
		         details.storageSystem = OpenstackAdapterUtil.createStorageSystem(region, accountId);
		         
		         details.storagePool = OpenstackAdapterUtil.createStoragePool(details.storageVolume.storagePoolId, region, accountId,0, 0);
		         list.add(details); 
		         payload.retrievedVolumes.put(mountData,list);
		         logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration: volume found:" + details, null);
		     }
		}
		
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration: found volumes:" + payload.retrievedVolumes, null);
		payload.customCloningProperties = OpenstackAdapterUtil.getVolumeConfigMetaData();
		StorageOperationResponse<RetrieveVolumesResponse> response = new StorageOperationResponse<RetrieveVolumesResponse>(payload);
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;
	
	}
	
	public synchronized StorageOperationResponse<GetDeviceVolumeMappingResponse> getInstanceDeviceExportPathMapping(Map<String,String> request) {	
		try {
			GetDeviceVolumeMappingResponse payload = new GetDeviceVolumeMappingResponse();
			Map<String,MountData> volumeMapping = new HashMap<String,MountData>();
			String zone = null;
			String instID = request.get("instanceid");
			if (isValidInstanceIDFormat(instID)==false)
				return null;
			List<Volume> volumeList = new ArrayList<Volume>();
	    	//for (String zone:openstackClient.listAvailabilityZones(null)) {
	    		volumeList.addAll(openstackClient.listVolumes(null));
	    	//}
	    	List<String> zones = openstackClient.getAvailabilityZones();
	    	// assuming a single region support
	    	if (zones != null)
	    		zone = zones.get(0);
	    	for (Volume vol:volumeList) {
	    		for (VolumeAttachment va : vol.getAttachments()) {
	    			if (va.getServerId()!= null && va.getServerId().equals(instID)) {
	    				 MountData mt = new MountData();
	  				     mt.setRemotePath(zone + ":" + vol.getId() + ":" + va.getDevice());
	  				     mt.setVendor(OpenstackConstants.Openstack_VENDOR);
	  				     mt.setFsType(MountData.STORAGE_TYPE_DFS);
	  				     volumeMapping.put(va.getDevice(), mt);
	    			}
	    			
	    		}
	    	}
	    	payload.deviceVolumeMap = (HashMap<String, MountData>) volumeMapping;
			StorageOperationResponse<GetDeviceVolumeMappingResponse> response = new StorageOperationResponse<GetDeviceVolumeMappingResponse>(payload);
			response.setPercentCompleted(100);
			response.setStatus(StorageOperationStatus.COMPLETED);
			return response;
		} catch (Exception e) {
			logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG , this.getClass().getName(), "getStorageVolumes:", null, e);
			return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), GetDeviceVolumeMappingResponse.class); 
		}
	}
	
	private boolean isValidInstanceIDFormat(String instID) {
		if (instID.startsWith("i-"))
			return false;
		return true;
	}

	@Override
	public StorageOperationResponse<GetClusterStorageSystemsResponse> getClusterStorageSystems(
			GetClusterStorageSystemsRequest arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
