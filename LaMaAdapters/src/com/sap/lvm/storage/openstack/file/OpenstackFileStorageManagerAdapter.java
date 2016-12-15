package com.sap.lvm.storage.openstack.file;

import java.util.List;
import java.util.Map;

import com.sap.lvm.CloudClientException;
import com.sap.lvm.storage.openstack.file.OpenstackFileStorageCloning.OpenstackFileCloneVolumesContext;
import com.sap.lvm.storage.openstack.file.OpenstackFileStorageSnapshot.OpenstackFileSnapshotVolumesContext;
import com.sap.lvm.storage.openstack.util.OpenstackConstants;
import com.sap.tc.vcm.infrastructure.api.adapter.InfrastructAdapterException;
import com.sap.tc.vcm.storage.adapter.api.AbstractStorageManagerAdapter;
import com.sap.tc.vcm.storage.adapter.api.IStorageManagerAdapter;
import com.sap.tc.vcm.storage.adapter.api.base.IStorageManagerAdapterConfig;
import com.sap.tc.vcm.storage.adapter.api.base.IStorageOperationContext;
import com.sap.tc.vcm.storage.adapter.api.base.StorageOperationId;
import com.sap.tc.vcm.storage.adapter.api.base.response.IStorageOperationResponsePayload;
import com.sap.tc.vcm.storage.adapter.api.base.response.StorageOperationResponse;
import com.sap.tc.vcm.storage.adapter.api.cloning.IStorageCloning;
import com.sap.tc.vcm.storage.adapter.api.mappingmasking.IStorageMappingAndMasking;
import com.sap.tc.vcm.storage.adapter.api.retrieval.IStorageRetrieval;
import com.sap.tc.vcm.storage.adapter.api.snapshot.IStorageSnapshot;
import com.sap.tc.vcm.storage.adapter.api.validation.IStorageValidation;

public class OpenstackFileStorageManagerAdapter extends AbstractStorageManagerAdapter implements IStorageManagerAdapter {

	private OpenstackFileStorageRetrieval retrieval;
	private OpenstackFileStorageCloning cloning;
	private OpenstackFileStorageSnapshot snapshot;
	private OpenstackFileStorageMappingAndMasking mappingAndMasking;

	public OpenstackFileStorageManagerAdapter(IStorageManagerAdapterConfig config)throws InfrastructAdapterException {

		OpenstackFileCloudStorageController openstackClient;  
		try {
			Map<String, String> connectionProps = config.getStorageManagerAdditionalConfigProps();
			Map<String, String> secProps = config.getStorageManagerAdditionalSecConfigProps();

			openstackClient = new OpenstackFileCloudStorageController(config.getLabel(),config.getUrl(),config.getUser(),config.getPassword(), 
					connectionProps.get(OpenstackConstants.OS_REGION),connectionProps.get(OpenstackConstants.OS_TENANT),
					connectionProps.get(OpenstackConstants.OS_DOMAIN),connectionProps.get(OpenstackConstants.OS_PROJECT),
					connectionProps.get(OpenstackConstants.PROXY_HOST),connectionProps.get(OpenstackConstants.PROXY_PORT),
					connectionProps.get(OpenstackConstants.PROXY_USER_NAME),secProps.get(OpenstackConstants.PROXY_PASS));

		} catch (CloudClientException e) {
			throw new InfrastructAdapterException(e);
		}
		retrieval = new OpenstackFileStorageRetrieval(openstackClient, config.getLogger());
		cloning = new OpenstackFileStorageCloning(openstackClient, retrieval,config.getLogger());
		snapshot = new OpenstackFileStorageSnapshot(openstackClient,config.getLogger());
		mappingAndMasking = new OpenstackFileStorageMappingAndMasking(openstackClient, config.getLogger());
	}

	@Override
	public <PAYLOAD extends IStorageOperationResponsePayload> StorageOperationResponse<PAYLOAD> cancelOperation(
			StorageOperationId operationId, IStorageOperationContext context,
			Class<PAYLOAD> expectedResponseClass)
			throws InfrastructAdapterException {

		if (context instanceof OpenstackFileSnapshotVolumesContext){
			return (StorageOperationResponse<PAYLOAD>)snapshot.cancelSnapshots(operationId,(OpenstackFileSnapshotVolumesContext) context);
		} else {
			throw new InfrastructAdapterException("Operation " + operationId.getId() +" is not cancelable" );
		}
	}

	@Override
	public <PAYLOAD extends IStorageOperationResponsePayload> StorageOperationResponse<PAYLOAD> getOperationStatus(
			StorageOperationId operationId, IStorageOperationContext context,
			Class<PAYLOAD> expectedResponseClass)
			throws InfrastructAdapterException {

		if (context instanceof OpenstackFileCloneVolumesContext ){
			return  (StorageOperationResponse<PAYLOAD>) cloning.getOperationStatus(operationId, (OpenstackFileCloneVolumesContext) context);		    	
		} else if (context instanceof OpenstackFileSnapshotVolumesContext){
			return (StorageOperationResponse<PAYLOAD>) snapshot.getOperationStatus(operationId,(OpenstackFileSnapshotVolumesContext) context);
		} else {
			throw new InfrastructAdapterException("Unknown operation " + operationId.getId());
		}

	}

	@Override
	public IStorageCloning getStorageCloning() {
		return this.cloning;
	}

	@Override
	public IStorageMappingAndMasking getStorageMappingAndMasking() {
		return this.mappingAndMasking;
	}

	@Override
	public IStorageRetrieval getStorageRetrieval() {
		return this.retrieval;
	}

	@Override
	public IStorageSnapshot getStorageSnapshot() {
		return this.snapshot;
	}

	@Override
	public boolean isCancelable(StorageOperationId operationId,
			IStorageOperationContext context) {
		if (context instanceof OpenstackFileSnapshotVolumesContext) {
			return true;	
		} else if (context instanceof OpenstackFileSnapshotVolumesContext) {
			return true;
		} else {
			return false;
		}

	}

	@Override
	public boolean isPausable(StorageOperationId operationId, IStorageOperationContext context) {
		return false;
	}

	@Override
	public boolean isResumable(StorageOperationId operationId, IStorageOperationContext context) {
		return false;
	}

	@Override
	public <PAYLOAD extends IStorageOperationResponsePayload> StorageOperationResponse<PAYLOAD> pauseOperation(
			StorageOperationId operationId, IStorageOperationContext context,
			Class<PAYLOAD> expectedResponseClass)
			throws InfrastructAdapterException {
		return null;
	}

	@Override
	public <PAYLOAD extends IStorageOperationResponsePayload> StorageOperationResponse<PAYLOAD> resumeOperation(
			StorageOperationId operationId, IStorageOperationContext context,
			Class<PAYLOAD> expectedResponseClass)
			throws InfrastructAdapterException {
		return null;
	}

	@Override
	public List<LogMessage> validate() {
		return null;
	}

	@Override
	public List<ExternalURL> getExternalUrls() {
		return null;
	}

	@Override
	public List<ExternalCustomTab> getExternalCustomTabs() {
		return null;
	}

	@Override
	public IStorageValidation getStorageValidation() {
		// TODO Auto-generated method stub
		return null;
	}

}