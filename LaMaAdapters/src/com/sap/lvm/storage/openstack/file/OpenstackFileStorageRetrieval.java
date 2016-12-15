package com.sap.lvm.storage.openstack.file;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.openstack4j.model.manila.Share;

import com.sap.lvm.storage.openstack.util.OpenstackAdapterUtil;
import com.sap.lvm.storage.openstack.util.OpenstackConstants;
import com.sap.lvm.storage.openstack.util.StorageAdapterImplHelper;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse.StorageOperationStatus;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStoragePoolsRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStoragePoolsResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageSystemsRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageSystemsResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.GetStorageVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.retrieval.IStorageRetrieval;
import com.sap.tc.vcm.storage.adapter.api.retrieval.RetrieveVolumesRequest;
import com.sap.tc.vcm.storage.adapter.api.retrieval.RetrieveVolumesResponse;
import com.sap.tc.vcm.storage.adapter.api.types.MountData;
import com.sap.tc.vcm.storage.adapter.api.types.StoragePool;
import com.sap.tc.vcm.storage.adapter.api.types.StorageSystem;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolume;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolumeDetails;

public class OpenstackFileStorageRetrieval implements IStorageRetrieval {

	private OpenstackFileCloudStorageController openstackClient = null;
	private String accountId;
	private IJavaEeLog logger;

	public OpenstackFileStorageRetrieval(OpenstackFileCloudStorageController openstackClient,IJavaEeLog logger) {
		this.openstackClient = openstackClient;
		accountId = openstackClient.getAccountId();	
		this.logger = logger;
	}

	@Override
	public synchronized StorageOperationResponse<GetStoragePoolsResponse> getStoragePools(GetStoragePoolsRequest request) {

		// storage pools id format = backend:aggregate
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStoragePools: request:" + request, null);
		GetStoragePoolsResponse payload = new GetStoragePoolsResponse();
		ArrayList<StoragePool> storagePools = new ArrayList<StoragePool>();
		List<String> pools = null;
		long totalSpaceGB= 0;
		long  usedSpaceGB= 0;

		String storageSystemId = request.storageSystemId;
		List<String> storagePoolIds = request.storagePoolIds;

		if (storageSystemId != null) {
			String backend = openstackClient.getOpenstackId(storageSystemId);
			try {
				pools = openstackClient.listPools(backend);
			} catch (Exception e) {
				logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStoragePools:" + e.getMessage(), null,e);
				return StorageAdapterImplHelper.createFailedResponse(e.getMessage(), GetStoragePoolsResponse.class); 
			}
			StoragePool volumePool = null;
			for (String pool:pools) {
				volumePool = OpenstackAdapterUtil.createFileStoragePool(pool, backend, accountId, totalSpaceGB, usedSpaceGB);
				storagePools.add(volumePool);
			}
		} else {
			if (storagePoolIds == null || storagePoolIds.isEmpty()) {
				return StorageAdapterImplHelper.createFailedResponse("Bad request: both storageSystemId and storagePoolIds are null", GetStoragePoolsResponse.class);  
			}
			try {
				StoragePool volumePool = null;
				String pool = null;
				String backend = null;		  
				for (String poolId:storagePoolIds) {
					if (poolId == null || poolId.indexOf(':') == -1) {
						storagePools.add(null);
						continue;
					}
					pool = openstackClient.getOpenstackId(poolId);
					backend = openstackClient.getOpenstackId(storageSystemId);
					if (pool.equals(OpenstackConstants.Openstack_POOL_SNAPSHOTS)) {
						volumePool = OpenstackAdapterUtil.createFileStoragePool(OpenstackConstants.Openstack_POOL_SNAPSHOTS, backend, accountId, totalSpaceGB, usedSpaceGB);
						storagePools.add(volumePool);
					} else {
						pools =  openstackClient.listPools(backend);
						if (pools.contains(pool)) {
							volumePool = OpenstackAdapterUtil.createFileStoragePool(pool, backend, accountId, totalSpaceGB, usedSpaceGB);
							storagePools.add(volumePool);	
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
	public synchronized StorageOperationResponse<GetStorageSystemsResponse> getStorageSystems(GetStorageSystemsRequest request) {

		//storage system id format = account_id:backend
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStorageSystems: " + request.storageSystemIds , null);
		try {
			List<String> requestedSystems = request.storageSystemIds;

			ArrayList<StorageSystem> systemList = new ArrayList<StorageSystem>();
			if (requestedSystems == null||requestedSystems.isEmpty()) {
				List<String> backEnds = openstackClient.listBackends();
				logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStorageSystems: backends:" + backEnds , null);
				for (String backend:backEnds) {
					systemList.add(OpenstackAdapterUtil.createFileStorageSystem(backend, accountId ));
				}
			} else {
				List<String> backends = openstackClient.listBackends();
				boolean found = false;
				for (String requestedSystem : requestedSystems) {
					if(requestedSystem == null) {
						systemList.add(null);
						continue;
					}
					found = false;
					for(String backend:backends) {
						if (requestedSystem.equals(backend)) {
							systemList.add(OpenstackAdapterUtil.createFileStorageSystem(backend, accountId));
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
	public synchronized StorageOperationResponse<GetStorageVolumesResponse> getStorageVolumes(GetStorageVolumesRequest request) {

		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "getStorageVolumes: request:" +request.storagePoolId + " " + request.storageSystemId + " " + request.storageVolumeIds, null);
		StorageOperationResponse<GetStorageVolumesResponse> response = null;
		try {
			List<StorageVolume> internalVolumes = null;
			List<Share> volumeList = null;
			String storageSystemId = request.storageSystemId;
			String storagePoolId = request.storagePoolId;
			List<String> storageVolumeIds = request.storageVolumeIds;

			if (storageVolumeIds == null || storageVolumeIds.isEmpty()) {
				if (storageSystemId == null) {
					if (storagePoolId == null) {
						return StorageAdapterImplHelper.createFailedResponse("Invalid Reqeust: all parameters are null", GetStorageVolumesResponse.class); 
					} else {
						volumeList = openstackClient.listShares(storagePoolId);
					}
				} else {
					String backend = openstackClient.getOpenstackId(storageSystemId);
					if (storagePoolId == null) {
						internalVolumes = new ArrayList<StorageVolume>();
						List<String> listPools = openstackClient.listPools(backend);
						for (String pool:listPools) {
							storagePoolId = backend+":"+pool;
							List<Share> listShares = openstackClient.listShares(storagePoolId);
							for (Share share : listShares) {
								internalVolumes.add(OpenstackAdapterUtil.sharetoStorageVolume(share, storageSystemId, storagePoolId));
							}
						}
					} else {
						volumeList = openstackClient.listShares(storagePoolId);
					}
				}
			} else {
				internalVolumes = new ArrayList<StorageVolume>();
				for (String volumeId:storageVolumeIds) {
					Share share = openstackClient.getShare(volumeId);
					String backend = storageSystemId=storageSystemId.split(":")[1];

					if (share == null) {
						internalVolumes.add(null);
					} else {
						internalVolumes.add(OpenstackAdapterUtil.sharetoStorageVolume(share, storageSystemId, storagePoolId));
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
	public synchronized StorageOperationResponse<RetrieveVolumesResponse> retrieveVolumesFromLvmMountConfiguration(RetrieveVolumesRequest request) {

		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration: mountData:" + request.mountData + " hostnames:" + request.serviceIdToHostname, null);

		String backend = null;
		RetrieveVolumesResponse payload = new RetrieveVolumesResponse();
		payload.retrievedVolumes = new LinkedHashMap<MountData, List<StorageVolumeDetails>>();
		ArrayList<StorageVolumeDetails> list = new ArrayList<StorageVolumeDetails>();
		StorageVolumeDetails details;

		for (MountData mountData:request.mountData) {
			if (mountData.getStorageType().equals("NAS")) {

				if (mountData.exportPath == null || !mountData.exportPath.contains(":")) {
					logger.log(IJavaEeLog.SEVERITY_WARNING, this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration: missing exportPath for mountPoint:" + mountData.mountPoint, null);
					continue;
				}
				list = new ArrayList<StorageVolumeDetails>();
				details = new StorageVolumeDetails();
				try {
					String exportLocation = mountData.exportPath;
					Share share = openstackClient.getSharebyExport(exportLocation);
					backend = openstackClient.getBackend(share.getId());

					String pool = openstackClient.getPool(share.getId());
					details.storageVolume = OpenstackAdapterUtil.sharetoStorageVolume(share,accountId+":"+backend, backend+":"+pool );
				} catch (Exception e) {
					logger.traceThrowable(IJavaEeLog.SEVERITY_DEBUG , this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration:", null, e);
					continue;
				}
				details.storageSystem = OpenstackAdapterUtil.createFileStorageSystem(backend, accountId );

				details.storagePool = OpenstackAdapterUtil.createFileStoragePool(details.storageVolume.storagePoolId, backend, accountId, 0, 0);
				list.add(details); 
				payload.retrievedVolumes.put(mountData,list);
				logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration: volume found:" + details, null);
			}
		}

		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "retrieveVolumesFromLvmMountConfiguration: found volumes:" + payload.retrievedVolumes, null);
		payload.customCloningProperties = OpenstackAdapterUtil.getVolumeConfigMetaData();
		StorageOperationResponse response = new StorageOperationResponse(payload);
		response.setPercentCompleted(100);
		response.setStatus(StorageOperationStatus.COMPLETED);
		return response;

	}

}
