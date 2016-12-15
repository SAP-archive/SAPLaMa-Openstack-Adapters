package com.sap.lvm.storage.openstack.block;

import java.util.List;
import java.util.Map;

import com.sap.lvm.storage.openstack.util.CloudClientException;
import com.sap.lvm.storage.openstack.block.OpenstackBlockStorageCloning.OpenstackBlockCloneVolumesContext;
import com.sap.lvm.storage.openstack.block.OpenstackBlockStorageSnapshot.OpenstackBlockSnapshotVolumesContext;
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


public class OpenstackBlockStorageManagerAdapter extends AbstractStorageManagerAdapter implements
		IStorageManagerAdapter {
	
	private OpenstackBlockStorageRetrieval retrieval;
	private OpenstackBlockStorageCloning cloning;
	private OpenstackBlockStorageSnapshot snapshot;
	private OpenstackBlockStorageMappingAndMasking mappingAndMasking;
	
	
	public OpenstackBlockStorageManagerAdapter(IStorageManagerAdapterConfig config)throws InfrastructAdapterException {
	  
		OpenstackBlockCloudStorageController openstackClient;  
	  try {
		  Map<String, String> connectionProps = config.getStorageManagerAdditionalConfigProps();
	      Map<String, String> secProps = config.getStorageManagerAdditionalSecConfigProps();

			openstackClient = new OpenstackBlockCloudStorageController(config.getLabel(),config.getUrl(),config.getUser(),config.getPassword(), 
					connectionProps.get(OpenstackConstants.OS_REGION),connectionProps.get(OpenstackConstants.OS_TENANT),
					connectionProps.get(OpenstackConstants.OS_DOMAIN),connectionProps.get(OpenstackConstants.OS_PROJECT),
					connectionProps.get(OpenstackConstants.PROXY_HOST),connectionProps.get(OpenstackConstants.PROXY_PORT),
					connectionProps.get(OpenstackConstants.PROXY_USER_NAME),secProps.get(OpenstackConstants.PROXY_PASS));

		 	  } catch (CloudClientException e) {
	
	    throw new InfrastructAdapterException(e);
	  }
	  retrieval = new OpenstackBlockStorageRetrieval(openstackClient, config.getLogger());
	  cloning = new OpenstackBlockStorageCloning(openstackClient, retrieval,config.getLogger());
	  snapshot = new OpenstackBlockStorageSnapshot(openstackClient,config.getLogger());
	  mappingAndMasking = new OpenstackBlockStorageMappingAndMasking(openstackClient, config.getLogger());
	}

	@Override
	public <PAYLOAD extends IStorageOperationResponsePayload> StorageOperationResponse<PAYLOAD> cancelOperation(
			StorageOperationId operationId, IStorageOperationContext context,
			Class<PAYLOAD> expectedResponseClass)
			throws InfrastructAdapterException {

	    	
	    	if (context instanceof OpenstackBlockSnapshotVolumesContext){
	       return (StorageOperationResponse<PAYLOAD>)snapshot.cancelSnapshots(operationId,(OpenstackBlockSnapshotVolumesContext) context);
	    } else {
	       throw new InfrastructAdapterException("Operation " + operationId.getId() +" is not cancelable" );
	    }
	}

	@Override
	public <PAYLOAD extends IStorageOperationResponsePayload> StorageOperationResponse<PAYLOAD> getOperationStatus(
			StorageOperationId operationId, IStorageOperationContext context,
			Class<PAYLOAD> expectedResponseClass)
			throws InfrastructAdapterException {
				
		
	    if (context instanceof OpenstackBlockCloneVolumesContext ){
	       return  (StorageOperationResponse<PAYLOAD>) cloning.getOperationStatus(operationId, (OpenstackBlockCloneVolumesContext) context);		    	
	    } else if (context instanceof OpenstackBlockSnapshotVolumesContext){
	       return (StorageOperationResponse<PAYLOAD>) snapshot.getOperationStatus(operationId,(OpenstackBlockSnapshotVolumesContext) context);
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
		if (context instanceof OpenstackBlockSnapshotVolumesContext) {
		   return true;	
		} else if (context instanceof OpenstackBlockSnapshotVolumesContext) {
		   return true;
		} else {
		   return false;
		}
		
	}

	@Override
	public boolean isPausable(StorageOperationId operationId,
			IStorageOperationContext context) {
		return false;
	}

	@Override
	public boolean isResumable(StorageOperationId operationId,
			IStorageOperationContext context) {
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
