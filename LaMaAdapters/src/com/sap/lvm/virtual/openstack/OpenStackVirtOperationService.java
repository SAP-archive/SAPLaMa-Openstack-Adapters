package com.sap.lvm.virtual.openstack;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.openstack4j.model.compute.Server;

import com.sap.lvm.CloudClientException;
import com.sap.lvm.virtual.openstack.OpenStackConstants.OpenStackCloneSteps;
import com.sap.lvm.virtual.openstack.OpenStackConstants.OpenStackInstanceStates;
import com.sap.nw.lm.aci.engine.api.base.property.IProperty;
import com.sap.nw.lm.aci.engine.api.base.property.IPropertyType.ValueType;
import com.sap.nw.lm.aci.engine.api.base.property.ISimpleProperty;
import com.sap.nw.lm.aci.engine.base.api.i18n.TranslatableString;
import com.sap.tc.vcm.infrastructure.api.adapter.InfrastructAdapterException;
import com.sap.tc.vcm.infrastructure.api.adapter.config.ConfigPropMetaData;
import com.sap.tc.vcm.infrastructure.api.adapter.config.IInfrastructAdapterConfigMetaData.ConfigRequirement;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.virtualization.adapter.api.base.IVirtOpContext;
import com.sap.tc.vcm.virtualization.adapter.api.base.IVirtOpResponsePayload;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpResponse;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpResponse.VirtLogMessage;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpResponse.VirtOperationStatus;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpSyncResponse;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOperationId;
import com.sap.tc.vcm.virtualization.adapter.api.operation.AdditionalOperationMetaDataRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.AdditionalOperationMetaDataResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.IVirtNetworkOperationService;
import com.sap.tc.vcm.virtualization.adapter.api.operation.IVirtOperationService;
import com.sap.tc.vcm.virtualization.adapter.api.operation.NextDialogStepRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.NextDialogStepResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.NextDialogStepResponse.UserInput;
import com.sap.tc.vcm.virtualization.adapter.api.operation.OperationCharacteristicsRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.OperationCharacteristicsResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.RetrieveAvailableTargetEntitiesRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.RetrieveAvailableTargetEntitiesResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.ValidateDialogStepRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.ValidateDialogStepResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.ValidateRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.ValidateResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.VirtDefaultOperation;
import com.sap.tc.vcm.virtualization.adapter.api.operation.VirtOperation;
import com.sap.tc.vcm.virtualization.adapter.api.operation.async.ExecuteOperationRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.async.ExecuteOperationResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.input.DialogContext;
import com.sap.tc.vcm.virtualization.adapter.api.operation.input.IVirtOperationSpecificInput;
import com.sap.tc.vcm.virtualization.adapter.api.operation.input.VirtOperationInput;
import com.sap.tc.vcm.virtualization.adapter.api.operation.input.VirtProvisionInput;
import com.sap.tc.vcm.virtualization.adapter.api.operation.phased.ExecutePhasedOperationRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.phased.ExecutePhasedOperationResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.phased.FinalizePhasedOperationRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.phased.FinalizePhasedOperationResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.phased.PostProcessPhasedOperationRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.phased.PostProcessPhasedOperationResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.phased.PreparePhasedOperationRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.phased.PreparePhasedOperationResponse;
import com.sap.tc.vcm.virtualization.adapter.api.operation.results.VirtProvisionResult;
import com.sap.tc.vcm.virtualization.adapter.api.operation.sync.ExecuteOperationSyncRequest;
import com.sap.tc.vcm.virtualization.adapter.api.operation.sync.ExecuteOperationSyncResponse;
import com.sap.tc.vcm.virtualization.adapter.api.types.LegacyVirtEntityType;
import com.sap.tc.vcm.virtualization.adapter.api.types.TypedEntityId;
import com.sap.tc.vcm.virtualization.adapter.api.types.VirtPropertyKeys.VirtPredefinedOperationProperties;


public class OpenStackVirtOperationService implements IVirtOperationService, IVirtNetworkOperationService {

	OpenStack_CloudController osClient ;
	private IJavaEeLog logger;
	


	public OpenStackVirtOperationService(OpenStack_CloudController osClient, IJavaEeLog logger) {
		this.osClient = osClient;
		this.logger=logger;

	}

	@Override
	public <PAYLOAD extends IVirtOpResponsePayload> VirtOpResponse<PAYLOAD> cancelOperation(VirtOperationId operationId, IVirtOpContext context,
			Class<PAYLOAD> expectedResponseClass) throws InfrastructAdapterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtOpResponse<ExecuteOperationResponse> executeOperation(ExecuteOperationRequest request) {
		ExecuteOperationResponse response = new ExecuteOperationResponse();
		String operation = request.operationInput.operation.operation.name();
		VirtOperationInput virtSingleOpInput = request.operationInput;
		IVirtOperationSpecificInput operationInput = virtSingleOpInput.operationInput;
		DialogContext dialogContext = virtSingleOpInput.dialogContext;
		String entityId = virtSingleOpInput.entityId;
		String entityType = virtSingleOpInput.entityType;
		try {
			if (operation.equals(VirtDefaultOperation.START.name())) {
				// TODO : check if started already

				String result = osClient.activateInstance(entityId);

				return OpenStackUtil.createVirtOperationResponse(entityId, VirtDefaultOperation.START, response);
			} else if (operation.equals(VirtDefaultOperation.STOP.name())) {

				String result = osClient.deactivateInstance(entityId);

				return OpenStackUtil.createVirtOperationResponse(entityId, VirtDefaultOperation.STOP, response);
			} else if (operation.equals(VirtDefaultOperation.DESTROY)) {

				osClient.terminateInstance(entityId);

				return OpenStackUtil.createVirtOperationResponse(entityId, VirtDefaultOperation.DESTROY, response);
			} else if (operation.equals(VirtDefaultOperation.PROVISION.name())) {
				VirtProvisionInput provisionInput = (VirtProvisionInput) operationInput;
				if (entityType.equals(LegacyVirtEntityType.VIRTUAL_HOST_TEMPLATE.name())) {
					String imageId = entityId;
					String instanceType = "",  hostName = "", staticIP = "", instanceID = "",networkID="",securityGroupID="";
					LinkedHashMap<String, IProperty> propertiesMap = dialogContext.dialogStepContexts.get(0).dialogStepProps;
					instanceType = propertiesMap.get(OpenStackConstants.INSTANCE_TYPE).getSimpleProperty().getStringValue();

					networkID = propertiesMap.get(OpenStackConstants.NETWORK_ID).getSimpleProperty().getStringValue();
					if ((networkID != null) && (!networkID.equals(""))
							&& (!networkID.equals("default"))) {
						networkID = osClient.getNetworkIdByName(networkID);
					}
					
					List<String> networkIDList = new ArrayList<String>();
					networkIDList.add(networkID);
					
					if (propertiesMap.containsKey(OpenStackConstants.ADDITIONAL_NETWORK_IDS)) {
						 ISimpleProperty additionalNetworksProp = propertiesMap.get(OpenStackConstants.ADDITIONAL_NETWORK_IDS).getSimpleProperty();
						
						 String additionalNetworksString=null;
						if (additionalNetworksProp.getValueToString()!=null)
						{	 additionalNetworksString= additionalNetworksProp.getStringValue();
					
						String[] addNetworksArray = additionalNetworksString.split(",");
						for (String addNetwork : addNetworksArray) {
							addNetwork = addNetwork.trim();
							if ((addNetwork != null) && (!addNetwork.equals(""))
									&& (!addNetwork.equals("default"))) {
								addNetwork = osClient.getNetworkIdByName(addNetwork);
								if (!networkIDList.contains(addNetwork)){
									networkIDList.add(addNetwork);
								}
							}
							
						}
						
					}
					}

					securityGroupID = propertiesMap.get(OpenStackConstants.SECURITY_ID).getSimpleProperty().getStringValue();


					IProperty property = dialogContext.dialogStepContexts.get(0).dialogStepProps.get(VirtPredefinedOperationProperties.IP_ADDRESS_W_HOSTNAME);
					if (property != null) {
						String prop = property.getSimpleProperty().getStringValue();
						staticIP = prop.substring(0, prop.indexOf("#"));
						hostName = prop.substring(prop.indexOf("#") + 1);
					}
					String availabilityZone="";
					if (propertiesMap.containsKey("%"+OpenStackConstants.AVAILABILITY_ZONE)) {
						ISimpleProperty availabilityZoneProp = propertiesMap.get("%"+OpenStackConstants.AVAILABILITY_ZONE).getSimpleProperty();
				
					
						if (availabilityZoneProp.getValueToString()!=null) 
							availabilityZone=availabilityZoneProp.getStringValue();
					}
				
					instanceID = osClient.createInstance(imageId, staticIP, hostName, instanceType, networkIDList,securityGroupID ,((VirtProvisionInput)operationInput).targetName,availabilityZone);

					VirtProvisionResult result = new VirtProvisionResult();
					result.newEntityId = new TypedEntityId(LegacyVirtEntityType.VIRTUAL_HOST, instanceID); 
					response.operationResult = result;

					VirtOpResponse<ExecuteOperationResponse> virtResponse = new VirtOpResponse<ExecuteOperationResponse>();
					IVirtOpContext context = new OpenStackVirtOpContext(operation, instanceID);
					((OpenStackVirtOpContext) context).setEntityType(LegacyVirtEntityType.VIRTUAL_HOST.name());
					((OpenStackVirtOpContext) context).setEntityName(provisionInput.targetName);
					virtResponse.setContext(context);
					VirtOperationId id = new VirtOperationId();
					id.id = operation;
					id.type = operation;
					virtResponse.setId(id);
					virtResponse.setPayload(response);
					virtResponse.setPercentCompleted(0);
					virtResponse.setStatus(VirtOperationStatus.INITIAL);
					virtResponse.setLogMessages(new ArrayList<VirtLogMessage>());
					return virtResponse;

				} else if (entityType.equals(LegacyVirtEntityType.VIRTUAL_HOST.name())) {
					VirtProvisionResult result = new VirtProvisionResult();
				//	Server srv = osClient.describeInstance(entityId);
					String imageID = osClient.backupInstance(entityId);
					result.newEntityId = new TypedEntityId(LegacyVirtEntityType.VIRTUAL_HOST_TEMPLATE, imageID); 
					response.operationResult = result;
					String staticIP = null, hostName = null;
					IProperty hostNameProperty = dialogContext.dialogStepContexts.get(0).dialogStepProps.get(VirtPredefinedOperationProperties.IP_ADDRESS_W_HOSTNAME);
					if (hostNameProperty != null) {
						String prop = hostNameProperty.getSimpleProperty().getStringValue();
						if (prop != null && !prop.isEmpty()) {
							staticIP = prop.substring(0, prop.indexOf("#"));
							hostName = prop.substring(prop.indexOf("#") + 1);
						}
					}
					LinkedHashMap<String, IProperty> propertiesMap = dialogContext.dialogStepContexts.get(0).dialogStepProps;
					String networkID = propertiesMap.get(OpenStackConstants.NETWORK_ID).getSimpleProperty().getStringValue();
					if ((networkID != null) && (!networkID.equals(""))
							&& (!networkID.equals("default"))) {
						networkID = osClient.getNetworkIdByName(networkID);
					}
					List<String> networkIDList = new ArrayList<String>();
					networkIDList.add(networkID);
					if (propertiesMap.containsKey(OpenStackConstants.ADDITIONAL_NETWORK_IDS)) {
						String additionalNetworksString = propertiesMap.get(OpenStackConstants.ADDITIONAL_NETWORK_IDS).getSimpleProperty().getStringValue();
						String[] addNetworksArray = additionalNetworksString.split(",");
						for (String addNetwork : addNetworksArray) {
							addNetwork = addNetwork.trim();
							if ((addNetwork != null) && (!addNetwork.equals(""))
									&& (!addNetwork.equals("default"))) {
								addNetwork = osClient.getNetworkIdByName(addNetwork);
								if (!networkIDList.contains(addNetwork)){
									networkIDList.add(addNetwork);
								}
							}
							
						}
					}
					String securityGroup = propertiesMap.get("SecurityGroup").getSimpleProperty().getStringValue();
					String instanceType = propertiesMap.get(OpenStackConstants.INSTANCE_TYPE).getSimpleProperty().getStringValue();
					OpenStackVirtOpContext cloneContext = new OpenStackVirtOpContext(OpenStackCloneSteps.backup.name(), imageID, LegacyVirtEntityType.VIRTUAL_HOST_TEMPLATE.name(),
							provisionInput.targetName, hostName, staticIP, instanceType,getNetworksAsString(networkIDList), securityGroup);
					return OpenStackUtil.createVirtOperationResponseWithContext(entityId, VirtDefaultOperation.PROVISION, response, cloneContext);
				}
			}
		}
		catch (CloudClientException e){
			return OpenStackUtil.createFailedResponseWithException(ExecuteOperationResponse.class, e);
		}

		return null;
	}

	@Override
	public VirtOpSyncResponse<ExecuteOperationSyncResponse> executeOperationSync(ExecuteOperationSyncRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtOpResponse<ExecutePhasedOperationResponse> executePhasedOperation(ExecutePhasedOperationRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtOpResponse<FinalizePhasedOperationResponse> finalizePhasedOperation(FinalizePhasedOperationRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtOpSyncResponse<AdditionalOperationMetaDataResponse> getAdditionalOperationMetaData(AdditionalOperationMetaDataRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtOpSyncResponse<NextDialogStepResponse> getNextDialogStep(NextDialogStepRequest request) {
		NextDialogStepResponse response = new NextDialogStepResponse();
		List<UserInput> userInputList = new ArrayList<UserInput>();
		String operation = request.operation.operation.name();
		
		
		
		//IVirtOperationSpecificInput opSpecificInput = request.operationSpecificInput;

		if (operation.equals(VirtDefaultOperation.PROVISION.name())) {

			//get user selected instance type
			NextDialogStepResponse.UserInput instanceTypeInput = new UserInput();
			ConfigPropMetaData instanceTypeMetaData = new ConfigPropMetaData(new TranslatableString(OpenStackConstants.INSTANCE_TYPE, OpenStackVirtualizationManagerAdapterFactory.DOMAIN_INFRASTRUCTURE, "INSTANCE_TYPE"), ValueType.STRING,
					new TranslatableString(OpenStackConstants.OpenStack_INSTANCE_TYPE_PROP_DESCRIPTION, OpenStackVirtualizationManagerAdapterFactory.DOMAIN_INFRASTRUCTURE, "OpenStack_INSTANCE_TYPE_PROP_DESCRIPTION"), true);

			List<TranslatableString> flavorids = new ArrayList<TranslatableString>();
			List<String> flavorNames = new ArrayList<String>();
			try {
				flavorNames = osClient.getFlavorNames();
				
			} catch (CloudClientException e) {
				OpenStackUtil.createFailedSynchResponseWithException(NextDialogStepResponse.class, e);
			}
			if (flavorNames!=null){
				for (String name : flavorNames) {
					flavorids.add(new TranslatableString("%" + name, null));
				}

				instanceTypeMetaData.setIsValueArray(false);
				instanceTypeMetaData.setValueSet(flavorids);
				
				if (flavorids.size() == 1) {
					instanceTypeMetaData.setDefaultValue(flavorids.get(0).getString());
				}
				
				instanceTypeInput.propertyMetaData = instanceTypeMetaData;

				userInputList.add(instanceTypeInput);
			}
			
		
//get user selected network 
			NextDialogStepResponse.UserInput networkIdInput = new UserInput();
			ConfigPropMetaData networkIdMetaData = new ConfigPropMetaData(new TranslatableString(OpenStackConstants.NETWORK_ID, OpenStackVirtualizationManagerAdapterFactory.DOMAIN_INFRASTRUCTURE, "NETWORK_ID"), ValueType.STRING,
					new TranslatableString(OpenStackConstants.OpenStack_NETWORK_ID_PROP_DESCRIPTION, OpenStackVirtualizationManagerAdapterFactory.DOMAIN_INFRASTRUCTURE, "OpenStack_NETWORK_ID_PROP_DESCRIPTION"), true);
			
			NextDialogStepResponse.UserInput additionalNetworkIdInput = null; 
			List<TranslatableString> networkIds = new ArrayList<TranslatableString>();
			List<String> networkNames = new ArrayList<String>();
			try {
				networkNames = osClient.getNetworkNames();
			} catch (CloudClientException e) {
				OpenStackUtil.createFailedSynchResponseWithException(NextDialogStepResponse.class, e);
			}


			if ((networkNames!=null) && (networkNames.size()>0)){
				for (String name : networkNames) {
					networkIds.add(new TranslatableString("%" +name, null));
				}
				networkIdMetaData.setIsValueArray(false);
				networkIdMetaData.setValueSet(networkIds);
				if (networkIds.size() == 1) {
					networkIdMetaData.setDefaultValue(networkIds.get(0).getString());
				}
				
				if (networkIds.size() > 1) {
					// here we are creating an additional MetaData field for addtional Network Ids to be selected
					String possibleValues = "Possible values (comma seperated): ";
					for (String name : networkNames) {
						possibleValues += name + ",";
					}
					possibleValues = possibleValues.substring(0, possibleValues.lastIndexOf(","));
					
					additionalNetworkIdInput = new UserInput();
					ConfigPropMetaData additionalNetworkIdMetaData = new ConfigPropMetaData(new TranslatableString(OpenStackConstants.ADDITIONAL_NETWORK_IDS, OpenStackVirtualizationManagerAdapterFactory.DOMAIN_INFRASTRUCTURE, "ADDITIONAL_NETWORK_IDS"), ValueType.STRING,
							new TranslatableString("%" + possibleValues, null), false);
					additionalNetworkIdMetaData.setIsValueArray(false);
					
					additionalNetworkIdInput.propertyMetaData = additionalNetworkIdMetaData;
					
				}
				networkIdInput.propertyMetaData = networkIdMetaData;
				


				userInputList.add(networkIdInput);
				if (additionalNetworkIdInput != null) {
					userInputList.add(additionalNetworkIdInput);					
				}
			}

			//Security Groups

			NextDialogStepResponse.UserInput securityIdInput = new UserInput();
			ConfigPropMetaData securityIdMetaData = new ConfigPropMetaData(new TranslatableString(OpenStackConstants.SECURITY_ID, OpenStackVirtualizationManagerAdapterFactory.DOMAIN_INFRASTRUCTURE, "SECURITY_ID"), ValueType.STRING,
					new TranslatableString(OpenStackConstants.OpenStack_security_ID_PROP_DESCRIPTION, OpenStackVirtualizationManagerAdapterFactory.DOMAIN_INFRASTRUCTURE, "OpenStack_security_ID_PROP_DESCRIPTION"), true);
			List<TranslatableString> securityIds = new ArrayList<TranslatableString>();
			List<String> securityGroupIds = new ArrayList<String>();
			try {
				securityGroupIds = osClient.getSecurityGroups();
			} catch (CloudClientException e) {
				OpenStackUtil.createFailedSynchResponseWithException(NextDialogStepResponse.class, e);
			}


			if ((networkNames!=null) && (securityGroupIds.size()>0)){
				for (String id : securityGroupIds) {
					securityIds.add(new TranslatableString("%" + id, null));
				}
				
				securityIdMetaData.setIsValueArray(false);
				securityIdMetaData.setValueSet(securityIds);
				
				if (securityIds.size() == 1) {
					securityIdMetaData.setDefaultValue(securityIds.get(0).getString());
				}
				
				securityIdInput.propertyMetaData = securityIdMetaData;

				userInputList.add(securityIdInput);
			}
			
			//get user selected availability zone 

			NextDialogStepResponse.UserInput availabilityZoneInput = new UserInput();
			ConfigPropMetaData availabilityZoneMetaData = new ConfigPropMetaData(new TranslatableString("%"+OpenStackConstants.AVAILABILITY_ZONE,null), ValueType.STRING,
					new TranslatableString("%"+OpenStackConstants.OpenStack_AVAILABILITY_ZONE_PROP_DESCRIPTION,null),false);//, OpenStackVirtualizationManagerAdapterFactory.DOMAIN_INFRASTRUCTURE, "OpenStack_AVAILABILITY_ZONE_PROP_DESCRIPTION"), true);

			List<TranslatableString> availabilityZones = new ArrayList<TranslatableString>();
			List<String> availabilityZoneNames = new ArrayList<String>();
			availabilityZoneNames = osClient.listAvailabilityZones();
			if (availabilityZoneNames!=null){
				for (String name : availabilityZoneNames) {
					availabilityZones.add(new TranslatableString("%" + name, null));
				}

				availabilityZoneMetaData.setIsValueArray(false);
				availabilityZoneMetaData.setValueSet(availabilityZones);
				
				if (availabilityZones.size() == 1) {
					availabilityZoneMetaData.setDefaultValue(availabilityZones.get(0).getString());
				}
				
				availabilityZoneInput.propertyMetaData = availabilityZoneMetaData;

				userInputList.add(availabilityZoneInput);
			}
		}
		response.requiredUserInput = userInputList;
		return new VirtOpSyncResponse<NextDialogStepResponse>(response, new ArrayList<VirtLogMessage>());
	}

	@Override
	public VirtOpSyncResponse<OperationCharacteristicsResponse> getOperationCharacteristics(OperationCharacteristicsRequest request) {
		OperationCharacteristicsResponse payload = new OperationCharacteristicsResponse();
		if (request.operation.operation.name().equals(VirtDefaultOperation.PROVISION.name())) {
			payload.prerequisiteOperation = null;
			payload.isPhasedOperation = false;
			payload.isRequiringRequestDialog = true;
			payload.isSyncOperation = false;
			payload.targetEntityRequirement = ConfigRequirement.Required;
		}else if (request.operation.operation.name().equals(VirtDefaultOperation.DESTROY.name())) {
			payload.prerequisiteOperation = retrievePrerequisiteOperationForDestroyOfActiveVirtualHost(request.entityId);
			payload.isPhasedOperation = false;
			payload.isRequiringRequestDialog = false;
			payload.isSyncOperation = false;
			payload.targetEntityRequirement=ConfigRequirement.None;
		}else {
			payload.prerequisiteOperation = null;
			payload.isPhasedOperation = false;
			payload.isRequiringRequestDialog = false;
			payload.isSyncOperation = false;
			payload.targetEntityRequirement = ConfigRequirement.None;
		}
		return new VirtOpSyncResponse<OperationCharacteristicsResponse>(payload, new ArrayList<VirtLogMessage>());
	}

	private VirtOperation retrievePrerequisiteOperationForDestroyOfActiveVirtualHost(TypedEntityId entityId) {
		VirtOperation prereqOp = null;
		try {
			if(LegacyVirtEntityType.VIRTUAL_HOST.name().equals(entityId.elementType)) {
				List<Server> vms = osClient.getInstances(osClient.getRegion());
				for (Server vm : vms) {
					if(vm.getId().equals(entityId.elementId) &&
							vm.getStatus().name().equalsIgnoreCase(OpenStackConstants.OpenStackInstanceStates.running.name())) {
						prereqOp = new VirtOperation(VirtDefaultOperation.STOP);
					}
				}
			}
		}
		 
		catch (CloudClientException e) {
			logger.logThrowable(IJavaEeLog.SEVERITY_ERROR, "getOperationCharacteristics()", "Error occured when trying to get data for entity with id " + entityId, null, e);
		}
		return prereqOp;
	}


	@Override
	public <PAYLOAD extends IVirtOpResponsePayload> VirtOpResponse<PAYLOAD> getOperationStatus(VirtOperationId operationId, IVirtOpContext context,
			Class<PAYLOAD> expectedResponseClass) throws InfrastructAdapterException {


		VirtOpResponse<PAYLOAD> response = new VirtOpResponse<PAYLOAD>();

		if (expectedResponseClass.getName().equals(ExecuteOperationResponse.class.getName())) {

			ExecuteOperationResponse payLoad = new ExecuteOperationResponse();
			List<VirtLogMessage> logMessages = new ArrayList<VirtLogMessage>();

			OpenStackVirtOpContext OpenStackContext = (OpenStackVirtOpContext) context;

			VirtOperationStatus opState = mapStatus(OpenStackContext.getEntityId(), OpenStackContext.getEntityType());
			if (opState.name().equals(VirtOperationStatus.COMPLETED.name()) && VirtDefaultOperation.PROVISION.name().equals(operationId.type)) {
				org.openstack4j.model.image.Image image = null;
				try {

					if (OpenStackContext.getOperationType().equals(OpenStackCloneSteps.backup.name())) {
						image = osClient.describeImage(OpenStackContext.getEntityId());
						String description = image.getName();
						String staticIP = OpenStackContext.getIp();
						String hostName = OpenStackContext.getHostName();
						String instanceType = OpenStackContext.getInstanceType();
							

						List<String> networklist= new ArrayList<String>();
						networklist.add(description.substring(0, description.indexOf('#')));
						String availabilityZone=description.substring(description.indexOf('#') + 2);
						String instanceID = osClient.createInstance(OpenStackContext.getEntityId(), staticIP, hostName, instanceType, getNetworksAsList(OpenStackContext.getNetworkIds()),
								OpenStackContext.getSecurityGroup(), OpenStackContext.getEntityName(),availabilityZone);
			
					

						OpenStackVirtOpContext cloneContext = new OpenStackVirtOpContext(OpenStackCloneSteps.create.name(), instanceID);
					cloneContext.setEntityType(LegacyVirtEntityType.VIRTUAL_HOST.name());
						cloneContext.setEntityName(OpenStackContext.getEntityName());
						response.setId(operationId);
						response.setStatus(VirtOperationStatus.EXECUTING);
						response.setPayload((PAYLOAD) payLoad);
						response.setLogMessages(logMessages);
						response.setContext(cloneContext);
						response.setPercentCompleted(33);
						return response;
					} else if (OpenStackContext.getOperationType().equals(OpenStackCloneSteps.create.name())) {
						String instanceID = OpenStackContext.getEntityId();
						// TODO: Change method in  controller to return
						// instance
						Server instance = osClient.describeInstance(instanceID);
						String imageID = instance.getImageId();
						//Is this next line needed?
						//osClient.deregisterImage(imageID);
						// should the image be deleted instead - ??
						osClient.deleteImage(imageID);
						OpenStackVirtOpContext cloneContext = new OpenStackVirtOpContext(OpenStackCloneSteps.deregister.name(), instanceID);
						cloneContext.setEntityType(LegacyVirtEntityType.VIRTUAL_HOST.name());
						cloneContext.setEntityName(OpenStackContext.getEntityName());
						response.setId(operationId);
						response.setStatus(VirtOperationStatus.COMPLETED);
						response.setPayload((PAYLOAD) payLoad);
						response.setLogMessages(logMessages);
						response.setContext(cloneContext);
						response.setPercentCompleted(66);
						return response;
					} else {
						VirtProvisionResult result = new VirtProvisionResult();
						result.newEntityId = new TypedEntityId(LegacyVirtEntityType.VIRTUAL_HOST, OpenStackContext.getEntityId());
						payLoad.operationResult = result;						
					}
				} catch (CloudClientException e) {				

					return  (VirtOpResponse<PAYLOAD>) OpenStackUtil.createFailedResponseWithException(payLoad.getClass(), e);
				}

			}
			if (opState.name().equals(VirtOperationStatus.FAILED.name())) {
				VirtLogMessage logMessage = new VirtLogMessage(IJavaEeLog.SEVERITY_ERROR, "OpenStack", System.currentTimeMillis(), "");
				logMessages.add(logMessage);
			}else if (opState.name().equals(VirtOperationStatus.COMPLETED.name())) {
				response.setPercentCompleted(100);
			} else {
				response.setPercentCompleted(33);
			}
			response.setContext(OpenStackContext);
			response.setId(operationId);
			response.setStatus(opState);
			response.setPayload((PAYLOAD) payLoad);
			response.setLogMessages(logMessages);



		}
		return response;
	}

	@Override
	public boolean isCancelable(VirtOperationId operationId, IVirtOpContext context) {
		return false;
	}

	@Override
	public boolean isPausable(VirtOperationId operationId, IVirtOpContext context) {
		return false;
	}

	@Override
	public boolean isResumable(VirtOperationId operationId, IVirtOpContext context) {
		return false;
	}

	@Override
	public <PAYLOAD extends IVirtOpResponsePayload> VirtOpResponse<PAYLOAD> pauseOperation(VirtOperationId operationId, IVirtOpContext context, Class<PAYLOAD> expectedResponseClass)
	throws InfrastructAdapterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtOpResponse<PostProcessPhasedOperationResponse> postProcessPhasedOperation(PostProcessPhasedOperationRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtOpResponse<PreparePhasedOperationResponse> preparePhasedOperation(PreparePhasedOperationRequest request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <PAYLOAD extends IVirtOpResponsePayload> VirtOpResponse<PAYLOAD> resumeOperation(VirtOperationId operationId, IVirtOpContext context,
			Class<PAYLOAD> expectedResponseClass) throws InfrastructAdapterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtOpSyncResponse<RetrieveAvailableTargetEntitiesResponse> retrieveAvailableTargetEntities(RetrieveAvailableTargetEntitiesRequest request) {
		RetrieveAvailableTargetEntitiesResponse response = new RetrieveAvailableTargetEntitiesResponse();
		List<TypedEntityId> targetEntitiyIds = new ArrayList<TypedEntityId>();
		String instanceRegion = osClient.getRegion();
		targetEntitiyIds.add(new TypedEntityId(LegacyVirtEntityType.OS_RESOURCE_POOL, instanceRegion));
		response.targetEntityIds = targetEntitiyIds;
		return new VirtOpSyncResponse<RetrieveAvailableTargetEntitiesResponse>(response, new ArrayList<VirtLogMessage>());
	}

	@Override
	public VirtOpSyncResponse<ValidateResponse> validate(ValidateRequest request) {
		ValidateResponse response = new ValidateResponse();
		response.warningAndErrorMessages = new ArrayList<VirtLogMessage>();
		return new VirtOpSyncResponse<ValidateResponse>(response, new ArrayList<VirtLogMessage>());
	}

	@Override
	public VirtOpSyncResponse<ValidateDialogStepResponse> validateDialogStep(ValidateDialogStepRequest request) {
		ValidateDialogStepResponse response = new ValidateDialogStepResponse();
		response.warningAndErrorMessages = new ArrayList<VirtLogMessage>();
		return new VirtOpSyncResponse<ValidateDialogStepResponse>(response, new ArrayList<VirtLogMessage>());
	}

	private VirtOperationStatus mapStatus(String entityId, String entityType) {
		String instanceState = null;
		try {
			if (entityType.equals(LegacyVirtEntityType.VIRTUAL_HOST_TEMPLATE.name())) {
				instanceState = osClient.getImageState(entityId);
			} else {
				instanceState = osClient.getInstanceState(entityId);
			}
		} catch (CloudClientException e) {
			logger.log(logger.SEVERITY_INFO,"Could not get status for "+entityId, instanceState, null);
			instanceState = OpenStackInstanceStates.pending.name();

		}
		if (instanceState.equalsIgnoreCase(OpenStackConstants.OpenStackInstanceStates.pending.name()) || instanceState.equalsIgnoreCase(OpenStackConstants.OpenStackInstanceStates.stopping.name())|| instanceState.equalsIgnoreCase(OpenStackConstants.OpenStackInstanceStates.build.name()) || instanceState.equals("QUEUED") || instanceState.equals("SAVING")) {
			return VirtOperationStatus.EXECUTING;

		} else if (instanceState.equals(OpenStackConstants.OpenStackInstanceStates.terminated.name()) || instanceState.equalsIgnoreCase(OpenStackConstants.OpenStackInstanceStates.stopped.name())
				|| instanceState.equals("SUSPENDED")|| instanceState.equals("SHUTOFF")|| instanceState.equals("ACTIVE")|| instanceState.equals("ERROR")
				|| instanceState.equals(OpenStackConstants.OpenStackInstanceStates.running.name()) || instanceState.equals(OpenStackConstants.OpenStackInstanceStates.available.name())) {
			return VirtOperationStatus.COMPLETED;

		} else {
			return VirtOperationStatus.FAILED;
		}
	}

	@Override
	public VirtOpSyncResponse<ExecuteOperationSyncResponse> attachSecondaryAddresses(
			String virtualInstanceId, String primaryIPAddress,
			String... virtualHostIPAddresses) {
		VirtOpSyncResponse<ExecuteOperationSyncResponse> virtResponse = new VirtOpSyncResponse<ExecuteOperationSyncResponse>();
		for (String virtualHostIPAddress : virtualHostIPAddresses) {
			osClient.attachSecondaryInterface(virtualHostIPAddress,
					primaryIPAddress, virtualInstanceId);
		}
		virtResponse.setPercentCompleted(100);
		virtResponse.setStatus(VirtOperationStatus.COMPLETED);
		return virtResponse;
	}

	@Override
	public VirtOpSyncResponse<ExecuteOperationSyncResponse> detachSecondaryAddresses(
			String virtualInstanceId, String... virtualHostIPAddresses) {
		VirtOpSyncResponse<ExecuteOperationSyncResponse> virtResponse = new VirtOpSyncResponse<ExecuteOperationSyncResponse>();
		for (String virtualHostIPAddress : virtualHostIPAddresses) {
			osClient.detachSecondaryInterface(virtualHostIPAddress,
					virtualInstanceId);
		}
		virtResponse.setPercentCompleted(100);
		virtResponse.setStatus(VirtOperationStatus.COMPLETED);
		return virtResponse;
	}

	@Override
	public boolean isNetworkManagementEnabled() {

		return true;
	}

	private String getNetworksAsString(List<String> networkIDList) {
		if (networkIDList == null || networkIDList.isEmpty()) 
			return null;
		return networkIDList.toString().replaceAll("\\[|\\]", "").replaceAll(", ",",");
	}
	
	private List<String> getNetworksAsList(String networks) {
		if (networks == null) return null;	
		return new ArrayList<String>(Arrays.asList(networks.split(","))); 
	}

}
