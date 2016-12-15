package com.sap.lvm.virtual.openstack;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.openstack4j.model.compute.Address;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.image.Image;

import com.sap.lvm.CloudClientException;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpResponse.VirtLogMessage;
import com.sap.tc.vcm.virtualization.adapter.api.base.VirtOpSyncResponse;
import com.sap.tc.vcm.virtualization.adapter.api.monitoring.IVirtMonitoringService;
import com.sap.tc.vcm.virtualization.adapter.api.monitoring.RetrieveCompleteVirtLandscapeRequest;
import com.sap.tc.vcm.virtualization.adapter.api.monitoring.RetrieveCompleteVirtLandscapeResponse;
import com.sap.tc.vcm.virtualization.adapter.api.monitoring.RetrieveVirtEntitiesRequest;
import com.sap.tc.vcm.virtualization.adapter.api.monitoring.RetrieveVirtEntitiesResponse;
import com.sap.tc.vcm.virtualization.adapter.api.monitoring.VirtGroupingHierarchy;
import com.sap.tc.vcm.virtualization.adapter.api.monitoring.VirtGroupingHierarchyMapping;
import com.sap.tc.vcm.virtualization.adapter.api.operation.VirtDefaultOperation;
import com.sap.tc.vcm.virtualization.adapter.api.types.LegacyVirtEntity;
import com.sap.tc.vcm.virtualization.adapter.api.types.LegacyVirtEntityType;
import com.sap.tc.vcm.virtualization.adapter.api.types.PoolVirtEntity;
import com.sap.tc.vcm.virtualization.adapter.api.types.TypedEntityId;
import com.sap.tc.vcm.virtualization.adapter.api.types.VirtPropertyMetaData;
import com.sap.tc.vcm.virtualization.adapter.api.types.VirtPropertyValues;
import com.sap.tc.vcm.virtualization.adapter.api.types.VirtPropertyValues.VirtualEntityState;



public class OpenStackVirtMonitoringService implements IVirtMonitoringService {
	IJavaEeLog x;
	OpenStack_CloudController openStackClient = null;
	private List<String> regions;

	IJavaEeLog j2eeLog;
	public OpenStackVirtMonitoringService(
			OpenStack_CloudController openStackClient, String region,
			IJavaEeLog iJavaEeLog) {
		j2eeLog=iJavaEeLog;
		this.openStackClient = openStackClient;
		List<String> regionsOpenStack = new ArrayList<String>();
		if (region == null)
			region = "default";
		regionsOpenStack.add(region);
		this.regions = regionsOpenStack;
	}


	@Override
	public Collection<VirtPropertyMetaData> getVirtPropertyMetaData() {
		
		return new ArrayList<VirtPropertyMetaData>();
	}

	
	public VirtOpSyncResponse<RetrieveCompleteVirtLandscapeResponse> retrieveCompleteVirtLandscape (
			RetrieveCompleteVirtLandscapeRequest request)  {
		try {
			Map<String, LegacyVirtEntity> legacy = new Hashtable<String, LegacyVirtEntity>();
			LegacyVirtEntity landscape = new LegacyVirtEntity();
			landscape.type = LegacyVirtEntityType.VIRTUALIZATION_MANAGER;
			landscape.id = LegacyVirtEntity.VIRTUALIZATION_MANAGER_ID;
			landscape.name = OpenStackConstants.OpenStack_CLOUD;
			Properties props = new Properties();
			props.setProperty(VirtPropertyMetaData.COMMON_PROPERTY_STATE,
					VirtualEntityState.Available.name());
			props.setProperty(
					VirtPropertyMetaData.COMMON_PROPERTY_VENDOR_SPECIFIC_TYPE,
					"OPEN_STACK");
			landscape.properties = props;

			for (String region : regions) {
				LegacyVirtEntity pool = new LegacyVirtEntity();
				Properties poolProps = new Properties();
				if (region == null)
					region = "default";
				pool.id = region;
				pool.name = region;
				pool.type = LegacyVirtEntityType.OS_RESOURCE_POOL;
				pool.properties = poolProps;
				pool.properties
						.setProperty(
								VirtPropertyMetaData.COMMON_PROPERTY_VENDOR_SPECIFIC_TYPE,
								OpenStackConstants.OS_REGION);
				pool.properties.setProperty(
						VirtPropertyMetaData.COMMON_PROPERTY_STATE,
						VirtualEntityState.Available.name());

				List<Server> vmRecords = openStackClient.getInstances(region);
				List<org.openstack4j.model.image.Image> vmImages = openStackClient
						.getOpenStackImages(region);
				List<Flavor> flavors = (List<Flavor>) openStackClient
						.getFlavors();

				Map<String, Flavor> flavorMap = new HashMap<String, Flavor>();
				for (Flavor f : flavors)
					flavorMap.put(f.getId(), f);

				if (vmImages != null && !vmImages.isEmpty()) {
					for (Image image : vmImages) {
						HashSet<VirtDefaultOperation> operations = new HashSet<VirtDefaultOperation>();
						operations.add(VirtDefaultOperation.PROVISION);
						LegacyVirtEntity img = new LegacyVirtEntity();
						img.type = LegacyVirtEntityType.VIRTUAL_HOST_TEMPLATE;
						img.id = image.getId();
						img.name = image.getName() + " [" + img.id + "]";						
						img.supportedOperations = operations;
						img.enabledOperations = operations;
						Properties imgProps = new Properties();
						img.properties = imgProps;
						img.properties
								.setProperty(
										VirtPropertyMetaData.COMMON_PROPERTY_VENDOR_SPECIFIC_TYPE,
										"Template");
						
						String imageos = openStackClient.getOSType(image);
						
						if (imageos != null) {
							img.properties.setProperty(
									VirtPropertyMetaData.VS_PROPERTY_OS_TYPE,
									imageos);
							img.properties.setProperty(
									VirtPropertyMetaData.VS_PROPERTY_OS,
									imageos);
						}
						pool.addChild(img);
						legacy.put(img.id, img);
					}
				}


				if (vmRecords != null && !vmRecords.isEmpty()) {
					for (Server vmRecord : vmRecords) {
						HashSet<VirtDefaultOperation> operations = new HashSet<VirtDefaultOperation>();
						LegacyVirtEntity vm = new LegacyVirtEntity();
						Properties vmProperties = new Properties();
						vm.type = LegacyVirtEntityType.VIRTUAL_HOST;
						vm.id = vmRecord.getId();
						vm.name = vmRecord.getName() + " [" + vm.id + "]";
						vm.properties = vmProperties;
						vm.properties.setProperty(
								VirtPropertyMetaData.VS_PROPERTY_IPADDRESS,
								vmRecord.getAccessIPv4() == null ? ""
										: vmRecord.getAccessIPv4());
						String userDefinedHostname=vmRecord.getMetadata().get("hostname");
						if (userDefinedHostname!=null)
							vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_HOSTNAME,userDefinedHostname);
						else
						vm.properties.setProperty(
								VirtPropertyMetaData.VS_PROPERTY_HOSTNAME,
								vmRecord.getHost() == null ? vmRecord
										.getAccessIPv4() : vmRecord.getHost());
						
						Status status = vmRecord.getStatus();
						if (status != null) {
							if ((status.name().equalsIgnoreCase("SHUTOFF"))|| (status.name().equalsIgnoreCase("SUSPENDED"))
									|| status.name().equalsIgnoreCase(OpenStackConstants.OpenStackInstanceStates.stopped.name())) {
								vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_POWER_STATE,VirtPropertyValues.VirtualHostState.Defined.name());
								operations.add(VirtDefaultOperation.START);
								operations.add(VirtDefaultOperation.DESTROY);
								operations.add(VirtDefaultOperation.PROVISION);
							} else if ((status.name().equalsIgnoreCase(OpenStackConstants.OpenStackInstanceStates.running.name()))
									|| ((status.name().equalsIgnoreCase("ACTIVE")))) {vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_POWER_STATE,VirtPropertyValues.VirtualHostState.Active.name());
								operations.add(VirtDefaultOperation.STOP);
								operations.add(VirtDefaultOperation.PROVISION);
								operations.add(VirtDefaultOperation.DESTROY);
							} else if ((status.name().equalsIgnoreCase(OpenStackConstants.OpenStackInstanceStates.pending.name()))
									|| ((status.name().equalsIgnoreCase("SPAWNING")))
									|| ((status.name().equalsIgnoreCase("BUILD")))) { 
									// TODO: add "pending" to VirtualHostState
								vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_POWER_STATE,VirtPropertyValues.VirtualHostState.Defined.name());
								operations.add(VirtDefaultOperation.STOP);
								operations.add(VirtDefaultOperation.DESTROY);
							} else if (status.name().equalsIgnoreCase(OpenStackConstants.OpenStackInstanceStates.Error.name()))

							{
								vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_POWER_STATE,"Error");								
								operations.add(VirtDefaultOperation.DESTROY);
							} else {
								vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_POWER_STATE,	VirtPropertyValues.VirtualHostState.Unknown.name());
								operations.add(VirtDefaultOperation.DESTROY);
							}
						}
						if (vmRecord.getFlavorId() != null) {
							Flavor instanceType = flavorMap.get(vmRecord
									.getFlavorId());
							if (instanceType != null) {
								vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_MEMORY_CAPACITY,"" + instanceType.getRam());
								vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_CPU_COUNT,"" + instanceType.getVcpus());
																				
							}
						}

						String ip = "";
						ArrayList<String> adressList= new ArrayList<String>();
						Map<String, List<? extends Address>> addresses = vmRecord
								.getAddresses().getAddresses();
						// TODO: iterate through keyset, this will only extract the first set e.g. private IPs
			
						if (addresses != null && addresses.isEmpty()) {
							ip = openStackClient.getInstanceAddresses(vmRecord.getId());
						}
						Set<String> keyset = addresses.keySet();
						if ((keyset != null) && (keyset.size() > 0)) {
							String key1 = null;
							Iterator<String> it=keyset.iterator();
						while (it.hasNext()){
							 key1 = it.next();
							// while (keyset.iterator().hasNext())
							// key1 +=","+ keyset.iterator().next();

							List<? extends Address> addressesForKey = addresses
									.get(key1);
							Address addressRecord = addressesForKey.get(0);
							if (addressRecord.getAddr().length()>1)
						
							adressList.add(addressRecord.getAddr());
							for (int i = 1; i < addressesForKey.size(); i++) {
								addressRecord = addressesForKey.get(i);
								if (addressRecord.getAddr().length()>1)
								adressList.add(addressRecord.getAddr());

							}
						}
						}
						String ips="";
						for (String s : adressList)
						{
						    ips += s + ",";
						}
						
						vm.properties.setProperty(
								VirtPropertyMetaData.VS_PROPERTY_IPADDRESS,
								ips == null ? "" : ips);
						// TODO: check this!
						// if no hostname and no user defined hostname then add hostname based on IP
						//the host will not be accessible by default at this hostname but rather this is used 
						//because certain LVM installations will not manage the list of hosts if hostname is not provided
						
						
						
						if (vm.properties.getProperty("VirtPropertyMetaData.VS_PROPERTY_HOSTNAME")==null) {
							
							String firstIP=ips == null ? "" : ips.split(",")[0];
							
							String hostname ="os-"+firstIP.replaceAll("\\.", "-");
							InetAddress addr = null;
							try {
								addr = InetAddress.getByName(firstIP);
								hostname = addr.getHostName();
							} catch (UnknownHostException e) {
						
								j2eeLog.log(IJavaEeLog.SEVERITY_WARNING,
										"Could not get hostname for "+firstIP,"Openstack retrieveCompleteVirtLandscape()",
									 null);
								
							}
							
								
							vm.properties.setProperty(
								VirtPropertyMetaData.VS_PROPERTY_HOSTNAME,hostname);
						}						vm.supportedOperations = OpenStackConstants.OpenStack_SUPPORTED_OPERATIONS;
						vm.enabledOperations = operations;
						vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_POWER_STATE_DETAIL,status.name() == null ? "" : status.name());

						//This code can slow down performance but it is currently needed for on the fly provisioning
				
//						String osType = openStackClient.getOSType(vmRecord);
//						if (osType != null) {
//							vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_OS, osType);
//							vm.properties.setProperty(VirtPropertyMetaData.VS_PROPERTY_OS_TYPE, osType);
//						}													
						vm.properties.setProperty(VirtPropertyMetaData.COMMON_PROPERTY_VENDOR_SPECIFIC_TYPE,"Virtual machine");
						pool.addChild(vm);
						legacy.put(vm.id, vm);
					}
				}
				landscape.addChild(pool);
				legacy.put(pool.id, pool);
			}
			legacy.put(landscape.id, landscape);
			RetrieveCompleteVirtLandscapeResponse payload = new RetrieveCompleteVirtLandscapeResponse();
			payload.legacyEntities = legacy;
			payload.virtualizationManager = landscape;
			VirtOpSyncResponse<RetrieveCompleteVirtLandscapeResponse> responce = new VirtOpSyncResponse<RetrieveCompleteVirtLandscapeResponse>(
					payload, new ArrayList<VirtLogMessage>());
			
			return responce;
		} catch (CloudClientException e) {
		j2eeLog.log(IJavaEeLog.SEVERITY_ERROR, "retrieveCompleteVirtLandscape",
					"OPENSTACK ERROR:" + e,null);
			 return OpenStackUtil.createFailedSynchResponseWithException(RetrieveCompleteVirtLandscapeResponse.class, e);
			
		}
		
	}

	@Override
	public VirtOpSyncResponse<RetrieveVirtEntitiesResponse> retrieveVirtEntities(
			RetrieveVirtEntitiesRequest request) {
		RetrieveVirtEntitiesResponse response = new RetrieveVirtEntitiesResponse();
		RetrieveCompleteVirtLandscapeRequest rec = new RetrieveCompleteVirtLandscapeRequest();
		Map<String, LegacyVirtEntity> resultLegacyVirtEntities = new HashMap<String, LegacyVirtEntity>();
		Map<String, PoolVirtEntity> resultPoolVirtEntities = new HashMap<String, PoolVirtEntity>();
		rec.timeout = 1000;
		Collection<TypedEntityId> entityIds = request.entityIds;
		VirtOpSyncResponse<RetrieveCompleteVirtLandscapeResponse> allEntitiesResponse = retrieveCompleteVirtLandscape(rec);
		Map<String, LegacyVirtEntity> legacyVirtEntities = allEntitiesResponse
				.getPayload().legacyEntities;
		for (TypedEntityId entityId : entityIds) {
			if (legacyVirtEntities.containsKey(entityId.getEntityId())) {
				resultLegacyVirtEntities.put(entityId.getEntityId(),
						legacyVirtEntities.get(entityId.getEntityId()));
			}
		}
		response.legacyEntities = resultLegacyVirtEntities;
		response.poolEntities = resultPoolVirtEntities;
		return new VirtOpSyncResponse<RetrieveVirtEntitiesResponse>(response,
				new ArrayList<VirtLogMessage>());
	}

	@Override
	public VirtGroupingHierarchyMapping getGroupingHierarchyMapping() {
		VirtGroupingHierarchyMapping groupingMap = new VirtGroupingHierarchyMapping();

		Map<VirtGroupingHierarchy, Set<LegacyVirtEntityType>> legacyTypes = new HashMap<VirtGroupingHierarchy, Set<LegacyVirtEntityType>>();
		Set<LegacyVirtEntityType> navigationSet = new HashSet<LegacyVirtEntityType>();
		Set<LegacyVirtEntityType> defaultSet = new HashSet<LegacyVirtEntityType>();
		Set<LegacyVirtEntityType> extendedSet = new HashSet<LegacyVirtEntityType>();

		navigationSet.add(LegacyVirtEntityType.OS_RESOURCE_POOL);
		navigationSet.add(LegacyVirtEntityType.VIRTUALIZATION_MANAGER);

		defaultSet.add(LegacyVirtEntityType.VIRTUAL_HOST);
		defaultSet.add(LegacyVirtEntityType.OS_RESOURCE_POOL);
		defaultSet.add(LegacyVirtEntityType.VIRTUALIZATION_MANAGER);

		extendedSet.add(LegacyVirtEntityType.VIRTUAL_HOST);
		extendedSet.add(LegacyVirtEntityType.VIRTUALIZATION_MANAGER);
		extendedSet.add(LegacyVirtEntityType.OS_RESOURCE_POOL);
		extendedSet.add(LegacyVirtEntityType.VIRTUAL_HOST_TEMPLATE);

		legacyTypes.put(VirtGroupingHierarchy.NAVIGATION_TREE, navigationSet);
		legacyTypes.put(VirtGroupingHierarchy.DEFAULT, defaultSet);
		legacyTypes.put(VirtGroupingHierarchy.EXTENDED, extendedSet);
		groupingMap.legacyTypes = legacyTypes;

		return groupingMap;
	}

}
