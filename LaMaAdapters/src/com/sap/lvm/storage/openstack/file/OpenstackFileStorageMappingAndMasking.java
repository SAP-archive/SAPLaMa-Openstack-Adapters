package com.sap.lvm.storage.openstack.file;

import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse;
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

public class OpenstackFileStorageMappingAndMasking implements IStorageMappingAndMasking {

	private OpenstackFileCloudStorageController openstackClient = null;
	private IJavaEeLog logger;

	public OpenstackFileStorageMappingAndMasking(OpenstackFileCloudStorageController openstackClient, IJavaEeLog logger) {
		this.openstackClient = openstackClient;
		this.logger = logger;
	}

    @Override
    public StorageOperationResponse<PostDetachVolumeResponse> postDetachVolume(PostDetachVolumeRequest request) {

		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "preAttachVolume: properties:" + request.maskingProperties +" volumes: " + request.volumes, null);
        StorageOperationResponse<PostDetachVolumeResponse> response = new StorageOperationResponse<PostDetachVolumeResponse>();
        response.setStatus(StorageOperationStatus.COMPLETED);
        PostDetachVolumeResponse payload = new PostDetachVolumeResponse();
        response.setPayload(payload);
        return response ;

    }

    @Override
    public StorageOperationResponse<PreAttachVolumeResponse> preAttachVolume(PreAttachVolumeRequest request) {
 
		logger.log(IJavaEeLog.SEVERITY_DEBUG, this.getClass().getName(), "postDetachVolume: properties:" + request.maskingProperties + " volumes: " + request.volumes, null);
    	StorageOperationResponse<PreAttachVolumeResponse> response = new StorageOperationResponse<PreAttachVolumeResponse>();
        response.setStatus(StorageOperationStatus.COMPLETED);
        PreAttachVolumeResponse payload = new PreAttachVolumeResponse();
        response.setPayload(payload);
        return response ;
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
