package com.sap.lvm.virtual.openstack;
/*
 * Implements basic CRUD operations for Openstack resources 
 * e.g. for instances: createInstance,getInstances,terminateInstance, activate/deactivate
 */
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.ProxyHost;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.InterfaceAttachment;
import org.openstack4j.model.compute.InterfaceAttachment.FixedIp;
import org.openstack4j.model.compute.SecGroupExtension;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.Server.Status;
import org.openstack4j.model.compute.ServerCreate;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.openstack4j.model.compute.ext.AvailabilityZone;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.openstack.OSFactory;

import com.sap.lvm.CloudClientException;
import com.sap.lvm.util.MiscUtil;


/**
 * @author I803573
 *
 */

public class OpenStack_CloudController {
	public boolean v3;
	OSClientV3 os;
	private String region = "default";
	private String domain = "default";
	private String project = "defaultproject";

	String accessKey;
	String secretKey;
	String endpoint;
	String username;
	String password;
	String tenant;

	private String proxyHost;
	private int proxyPortint;
	private String proxyPort;
	
	private Map<String, String> flavorIdMap = new HashMap<String, String>();
	private Map<String, String> networkIdMap = new HashMap<String, String>();
	private Map<String, String> securityGroupIdMap= new HashMap<String, String>();




	/**
	 * Constructor for OpenStack_CloudController, called by OpenStackVirtualizationManagerAdapter and testConnection
	 * 
	 * @param endpoint
	 * @param region
	 * @param username
	 * @param password
	 * @param tenant
	 * @param proxyHost
	 * @param proxyPort
	 * @param proxyUsername
	 * @param proxyPassword
	 * @throws CloudClientException
	 */
	public OpenStack_CloudController (
			String endpoint, String username, String password, 
			String region, String tenant, 
			String domain, String project, 
			String proxyHost, String proxyPort, String proxyUsername, String proxyPassword) throws CloudClientException {

		this.region = region;
		if (endpoint != null)
			this.endpoint = endpoint;
		if (username != null)
			this.username = username;
		if (password != null)
			this.password = password;

		if (tenant != null)
			this.tenant = tenant;
		if (domain != null)
			this.domain = domain;
		if (project != null)
			this.project = project;

		if (proxyHost != null)
			this.proxyHost = proxyHost;
		if (proxyHost != null)
			this.proxyHost = proxyHost;
		if (proxyPort != null)
			this.proxyPort = proxyPort;

		v3 = this.endpoint.endsWith("/v3/");

		ClassLoader cl = OpenStack_CloudController.class.getClassLoader();
		ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(cl);

		try{

			if (MiscUtil.notNullAndEmpty(proxyHost) && (proxyPort!=null))	{
				int proxyPortint=Integer.parseInt(proxyPort);
				this.proxyPortint=proxyPortint;

				if (v3) {

					Identifier domainIdentifier = Identifier.byName(domain);
					Identifier projectIdentifier = Identifier.byName(project);

					os = OSFactory.builderV3()
							.endpoint(this.endpoint)
							.credentials(username, password, domainIdentifier)
							.withConfig(Config.newConfig().withProxy(ProxyHost.of("http://"+this.proxyHost, this.proxyPortint)))
							.scopeToProject(projectIdentifier, domainIdentifier)
							.authenticate();

				} else {
					// not supported
				}
			}
			else
				if (v3) {
					Identifier domainIdentifier = Identifier.byName(domain);
					Identifier projectIdentifier = Identifier.byName(project);

					os = OSFactory.builderV3()
							.endpoint(this.endpoint)
							.credentials(username, password, domainIdentifier)
							.withConfig(Config.newConfig())
							.scopeToProject(projectIdentifier, domainIdentifier)
							.authenticate();
				} else {
					// not supported
				}
		} catch (RuntimeException e) {
			Thread.currentThread().setContextClassLoader(oldCl);
			throw new CloudClientException("Failed to get Openstack client",e);
		}
		Thread.currentThread().setContextClassLoader(oldCl);
	}

	/**
	 * Gets all servers , currently only 1 region is supported so region param is ignored
	 * 
	 * @param region
	 * @return  List of Openstack images
	 * @throws CloudClientException
	 */
	public synchronized List<Image> getOpenStackImages(String region)
	throws CloudClientException {

		OSClientV3 os = getOs();
		try {

			List<? extends Image> imageList = os.images().list();

			if (imageList == null) {
				return new ArrayList<Image>();
			}

			return (List<Image>) imageList;
		} catch (RuntimeException e) {
			throw new CloudClientException("Failed to get Openstack images",e);
		}
	}

	/**
	 * Gets all servers , currently only 1 region is supported so region param is ignored
	 * 
	 * @param region
	 * @return List of OpenStack servers
	 * @throws CloudClientException
	 */
	public synchronized List<Server> getInstances(String region)
	throws CloudClientException {

		try {

			OSClientV3 os = getOs();
			List<? extends Server> describeInstancesResult = null;

			describeInstancesResult = os.compute().servers().listAll(true);

			List<Server> instanceList = new ArrayList<Server>();
			instanceList.addAll(describeInstancesResult);
			return instanceList;
		} catch (RuntimeException e) {
			throw new CloudClientException("Failed to list Openstack images",e);
		}
	}
	
/**
 * Starts instance specified by instanceID
 * 
 * @param instanceID
 * @return action response
 * @throws CloudClientException
 */
	public  String activateInstance(String instanceID) throws CloudClientException
	{	
	//TODO: this only works for instances that are in state SHUTOFF
	//if the state is SUSPENDED we should call Action.RESUME instead 
		ActionResponse resp;
	try {
	
		OSClientV3 os = getOs();
		resp=os.compute().servers().action(instanceID, Action.START);
		if (!resp.isSuccess())
			throw new CloudClientException(new Exception("Activate failed for "+instanceID));

	} catch (RuntimeException e) {
		throw new CloudClientException("Failed to activate Openstack instance",e);
	}
	return resp.toString();
	}


	/**
	 * Stops instance specified by instanceID (i.e. transitions to state "SHUTOFF"
	 * 
	 * @param instanceID
	 * @return action response
	 * @throws CloudClientException
	 */
	public  String deactivateInstance(String instanceID)
	throws CloudClientException {
		
		ActionResponse resp;
		try {
			OSClientV3 os = getOs();

			resp=os.compute().servers().action(instanceID, Action.STOP);
			if (!resp.isSuccess())
				throw new CloudClientException(new Exception("activate failed for "+instanceID+":"+resp));
		} catch (RuntimeException e) {
			throw new CloudClientException(e);
		}
		return resp.toString();
	}

	/**
	 * Deletes Server specified by instanceId
	 * 
	 * @param instanceID
	 * @throws CloudClientException
	 */
	public synchronized void terminateInstance(String instanceID)
	throws CloudClientException {
		try {
			OSClientV3 os = getOs();
			os.compute().servers().delete(instanceID);
		} catch (RuntimeException e) {
			throw new CloudClientException(e);
		}
	}

	/**
	 * Gets server specified by instanceId
	 * 
	 * @param instanceId
	 * @return server object corresponding to specified instanceId
	 * @throws CloudClientException
	 */
	public Server getInstance(String instanceId) throws CloudClientException {
		Server instance = null;
		try {
			OSClientV3 os = getOs();
			instance =os.compute().servers().get(instanceId);

		} catch (RuntimeException e) {
			throw new CloudClientException(e);
		}

		return instance;
	}


	/**
	 * Returns an Instance state.
	 * The terms "status" and "state" are used interchangeably here. 
	 *
	 * @param  instanceID  the ID of the VM to be monitored
	 * @return the instance status name
	 * 
	 */


	public  String getInstanceState(String instanceID)
	throws CloudClientException {
		try {

			Status state = getInstance(instanceID).getStatus();
			if (state == null) {
				throw new CloudClientException("Failed to get Openstack instance state for "+instanceID);
			}
			return state.name();
		} catch (RuntimeException e) {
			throw new CloudClientException("Failed to get Openstack instance state",e);
		}
	}
	/**
	 * Returns the specified Image state. 
	 * used to determine when an image is "ready" i.e. "ACTIVE"
	 *
	 * @param  imageID  the ID of the image to be monitored
	 * @return the image status name
	 * 
	 */
	public synchronized String getImageState(String imageID)
	throws CloudClientException {
		Image image = getOs().images().get(imageID);
		org.openstack4j.model.image.Image.Status status = image.getStatus();
		return status.name();
	}

	/**
	 * Used by provision instances process. If the parameter passed to the executeOperation method is a serverID instead of an imageID
	 * we first "backup" the server i.e. create an image
	 * 
	 * @param instanceID
	 * @return imageID of the image created from the specified instance
	 * @throws CloudClientException
	 */
	public synchronized String backupInstance(String instanceID)
	throws CloudClientException {
		OSClientV3 os = getOs();
		try {
			Server instance = getInstance(instanceID);



			if (instance == null ) {
				throw new CloudClientException("No instances for instance ID: " + instanceID);
			}

			// create image from instance
			String imageId = os.compute().servers().createSnapshot(instanceID, "Backup of "+instanceID);	
			return imageId;

		} catch (RuntimeException e) {
			throw new CloudClientException(e);
		}
	}
/**
 * Equivalent to getInstance()
 * @param instanceID
 * @return Openstack server object
 * @throws CloudClientException
 */
	public synchronized Server describeInstance(String instanceID)
	throws CloudClientException {
		return getInstance(instanceID);
	}

	/**
	 * 
	 * @param imageId
	 * @return openstack image specified by imageId
	 * @throws CloudClientException
	 */
	public synchronized Image describeImage(String imageId)
	throws CloudClientException {

		Image image = getOs().images().get(imageId);
		return image;
	}
	
	/**
	 * User selects a network during the server provision roadmap, which has to be mapped 
	 * to a networkid before sending this parameter to Openstack API
	 * @param name
	 * @return Maps friendly network name to networkId
	 */
	public String getNetworkIdByName(String name) {
		return this.networkIdMap.get(name);
	}

	/**
	 * Called to create new VM
	 * @param imageID
	 * @param staticIP
	 * @param hostName
	 * @param instanceType
	 * @param networkIDList
	 * @param securityGroup
	 * @param instanceName
	 * @return new instanceID
	 * @throws CloudClientException
	 */
	public synchronized String createInstance(String imageID, String staticIP,
			String hostName, String instanceType, List<String> networkIDList,
			String securityGroup, String instanceName,String availabilityZone)
	throws CloudClientException {

		OSClientV3 os = getOs();

		
		String flavorId = flavorIdMap.get(instanceType);	

		
		try {

			// Create a Server Model Object
			ServerCreate serverCreate = null;
			
			ServerCreateBuilder serverCreateBuilder = Builders.server()
			.name(instanceName)
			.flavor(flavorId).image(imageID);

			if ((securityGroup != null) && (!securityGroup.equals(""))
					&& (!securityGroup.equals("default")))
				serverCreateBuilder.addSecurityGroup(securityGroup);
			
			for (String networkID : networkIDList) {
				String subnet = os.networking().network().get(networkID).getSubnets().get(0);
			//we could add an input field for IP address in the provision input params instead of this
			//for now let's see if the instanceName is resolvable to an IP address and send it to Openstack
			//otherwise we may get random DHCP IP address which does not match 
			if ((staticIP==null)||(staticIP.equals("")))
			{
				InetAddress inetAddress;
				try {
					inetAddress = InetAddress.getByName(instanceName);
					if (inetAddress!=null){ 
						staticIP=inetAddress.getHostAddress();
						}
				} catch (Exception e) {
					//$JL-EXC$
					// if the instanceName does not resolve to an IP address then we quietly rely on DHCP to assign random IP 
				
				}
			
			}
//note if we create a port then the security group of the port over rides 
//the security group of the instance so we need to set it here with securityGroupID
				if ((staticIP!=null) && (!staticIP.equals(""))){
					Port  port = null;
					port = os.networking().port().create(
							Builders.port().name("port0").networkId(networkID).fixedIp(
									staticIP, subnet).securityGroup(securityGroupIdMap.get(securityGroup)).build());
					serverCreateBuilder.addNetworkPort(port.getId());
					
					
				}
				
			
			else
			if (networkIDList.size() > 0)
				serverCreateBuilder.networks(networkIDList);

}

			
			if ((availabilityZone != null) && (!availabilityZone.equals(""))
					&& (!availabilityZone.equals("default")))
				serverCreateBuilder.availabilityZone(availabilityZone);

			serverCreate = serverCreateBuilder.build();

			// Boot the Server
			Server server;

			server = os.compute().servers().boot(serverCreate);
			//TODO: Track down why we (sometimes) need a delay here.
			//The process completes properly with or without this delay but monitoring returns "Failed" if we do not add this 1 sec sleep
			//Maybe bug on Openstack side? i.e. returns a server which is not ready to be asked for status
			Thread.sleep(3000);
			return server.getId();
		} catch (Exception e) {
			throw new CloudClientException(e);
		}

	}
//This would be used to encode user-data or cloud-init scripts to new VMs 
//Currently not supported
//	public static String encode64(String input){
//		byte[] encodedBytes = Base64.encodeBase64(input.getBytes("UTF-8"));
//		return new String(encodedBytes);
//		
//	}
	/**
	 * May or may not be used during provisioning of VMs depending on Openstack setup
	 * @return list of subnet Ids
	 * @throws CloudClientException
	 */
	public synchronized List<String> getSubnetIDs() throws CloudClientException {
		try {
			List<? extends Subnet> subnets = os.networking().subnet().list();

			List<String> subnetIds = new ArrayList<String>();

			for (Subnet subnet : subnets) {
				if (subnet.getId() != null) {
					subnetIds.add(subnet.getId());
				}
			}
			return subnetIds;
		} catch (RuntimeException e) {
			throw new CloudClientException("Failed to get Openstack subnet IDs",e);
		}
	}

	/**
	 * Only 1 region supported as of now
	 * @return region
	 */
	public synchronized String getRegion() {
		return this.region;
	}
	
	/**
	 * Used by provisioning roadmap
	 * Enables user to select which network(s) to launch server on
	 * @return list of networks 
	 * @throws CloudClientException
	 */
	private synchronized List<? extends Network> getNetworks() throws CloudClientException {
		
		OSClientV3 os = getOs();
		List<? extends Network> networks;

		try {
			networks = os.networking().network().list();
		} catch (RuntimeException e) {
			throw new CloudClientException("Failed to get Openstack network IDs",e);
		}
		
		return networks;
		
	}

	/**
	 * Used by roadmap to present networks list for use during provisioning
	 * @return list of network names 
	 * @throws CloudClientException
	 */

	public synchronized List<String> getNetworkNames() throws CloudClientException {
		List<String> networkNames = new ArrayList<String>();
		List<? extends Network> networks = getNetworks();
		for (Network network : networks) {
			if (network.getId() != null) {
				networkNames.add(network.getName());
				networkIdMap.put(network.getName(), network.getId());
			}
		}
		if (networkNames.size() == 0)
			networkNames.add("default");
		
		return networkNames;
		
	}

	/**
	 * Called by most methods that interact with Openstack
	 * Previous client could be invalid so create a new one 
	 * Configure proxy if needed
	 * @return  Openstack client
	 */
	public OSClientV3 getOs() {

		Identifier domainIdentifier = Identifier.byName(domain);
		Identifier projectIdentifier = Identifier.byName(project);

		if (MiscUtil.notNullAndEmpty(this.proxyHost) && (this.proxyPort!=null))
		{	
			os = OSFactory.builderV3()
				.endpoint(this.endpoint)
				.credentials(username, password, domainIdentifier)
				.withConfig(Config.newConfig().withProxy(ProxyHost.of("http://"+this.proxyHost, this.proxyPortint)))
				.scopeToProject(projectIdentifier, domainIdentifier)
				.authenticate();

		}
		else
			os = OSFactory.builderV3()
				.endpoint(this.endpoint)
				.credentials(username, password, domainIdentifier)
				.withConfig(Config.newConfig())
				.scopeToProject(projectIdentifier, domainIdentifier)
				.authenticate();

		return os;
	}


/**
 * Get list of possible "T-shirt" sizes (CPU/disk/RAM) for new VMs during provisioning
 * @return list of flavors a.k.a. t-shirt sizes
 * @throws CloudClientException
 */
	public synchronized List<? extends Flavor> getFlavors()
	throws CloudClientException {
		List<? extends Flavor> flavors;
		OSClientV3 os = getOs();
		try {
			flavors = os.compute().flavors().list();

		} catch (RuntimeException e) {

			throw new CloudClientException("Failed to get OpenStack flavors ",e);
		}
		return flavors;
	}
	/**
	 * @return list of flavor names along with useful informaton i.e. CPUs, Disk & RAM size
	 * @throws CloudClientException
	 */
	public synchronized List<String> getFlavorNames() throws CloudClientException {
		List<String> flavorNames = new ArrayList<String>();
		List<? extends Flavor> flavors = getFlavors();
		for (Flavor flavor : flavors) {
			String name = flavor.getName();
			int cpu = flavor.getVcpus();
			int ram = flavor.getRam();
			int disk = flavor.getDisk();
			
			String flavorName = name + " (" + cpu + "VCPU /" + disk + "GB Disk /" + ram + "MB Ram )";
			String flavorId = flavor.getId();
			flavorIdMap.put(flavorName, flavorId);				
			flavorNames.add(flavorName);	
		}
		return flavorNames;
	}

	/**
	 * Used by roadmap to present security groups list for user selection
	 * @return list of security groups names 
	 * @throws CloudClientException
	 */
	
	public synchronized List<String> getSecurityGroups()
	throws CloudClientException {
		List<String> securityIds = new ArrayList<String>();
		OSClientV3 os = getOs();
		try {
			List<? extends SecGroupExtension> securityGroups;

			securityGroups = os.compute().securityGroups().list();		

			for (SecGroupExtension securityGroup : securityGroups) {
				if (securityGroup.getName() != null) {
					String name = securityGroup.getName();
					String id = securityGroup.getId();
					securityGroupIdMap.put(name, id);		
					securityIds.add(name);
				}
			}

		} catch (RuntimeException e) {

			throw new CloudClientException("Failed to list Openstack security groups.",e);
		}
		return securityIds;
	}

/**
 * Activates secondary IP by first finding the corresponding port of the interface where the primaryIPAddress is already attached 
 * and adding the virtualHostIPAddress as a secondary IP to the same interface.
 * 
 * @param virtualHostIPAddress
 * @param primaryIPAddress
 * @param virtualInstanceId
 */
	public void attachSecondaryInterface(String virtualHostIPAddress,
			String primaryIPAddress, String virtualInstanceId) {


		OSClientV3 os = getOs();

		// find the target interface by instance id and ip address
		List<? extends InterfaceAttachment> interfaces = os.compute().servers().interfaces().list(virtualInstanceId); 

		if (interfaces.isEmpty())
			throw new RuntimeException("Interface with ip address "
					+ primaryIPAddress + " not found");

		for (InterfaceAttachment interf : interfaces) {

			// TODO: put this in try catch block
			List<? extends FixedIp> fixedips = interf.getFixedIps();
			for (FixedIp fixedIP : fixedips) {
				String ip = fixedIP.getIpAddress();
				if (ip.equals(virtualHostIPAddress))
					return ;//virtualHostIPAddress already attached

			}
		}
		for (InterfaceAttachment interf : interfaces) {

			// TODO: put this in try catch block
			List<? extends FixedIp> fixedips = interf.getFixedIps();
			for (FixedIp fixedIP : fixedips) {
				String ip = fixedIP.getIpAddress();
				if (ip.equals(primaryIPAddress)) {
					String portid = interf.getPortId();

					Port port = os.networking().port().get(portid);

					String subnetId = port.getFixedIps().iterator().next().getSubnetId();

					// try to attach to target interface
					Port updatedPort = os.networking().port().update(
							port.toBuilder().fixedIp(virtualHostIPAddress,
									subnetId).name(
											"secondary-" + virtualHostIPAddress)
											.build());

				}
			}
		}

	}
/**
 * Detaches secondary IP from interface, called during unprepare
 * @param virtualHostIPAddress
 * @param virtualInstanceId
 */
	public void detachSecondaryInterface(String virtualHostIPAddress,
			String virtualInstanceId) {

		OSClientV3 os = getOs();

		// find the target interface by instance id and ip address
		List<? extends InterfaceAttachment> interfaces = os.compute().servers().interfaces().list(virtualInstanceId);

		if (interfaces.isEmpty())
			throw new RuntimeException("Interface with ip address "
					+ virtualHostIPAddress + " not found");

		for (InterfaceAttachment interf : interfaces) {
			// TODO: check if the secondary ip is already detached from the interface
			// TODO: put this in try catch block
			List<? extends FixedIp> fixedips = interf.getFixedIps();
			for (FixedIp fixedIP : fixedips) {
				String ip = fixedIP.getIpAddress();
				if (ip.equals(virtualHostIPAddress)) {
					String portid = interf.getPortId();

					Port port = os.networking().port().get(portid);
					String subnetId = port.getFixedIps().iterator().next()
					.getSubnetId();

					// try to detach to target interface
					Port updatedPort = os.networking().port().update(
							port.toBuilder().removeFixedIp(virtualHostIPAddress, subnetId) 

							.build());

				}
			}
		}


	}
	
	/**
	 * Retrieves OS Type for this vmRecord
	 * @param vmRecord
	 * @return null if os_type not maintained
	 */
	public String getOSType (Server vmRecord) {
		String osType = null;
		String imageId = vmRecord.getImageId();
		try {
			Image image = describeImage(imageId);
			osType = getOSType(image);
		} catch (CloudClientException e) {
			// $JL-EXC$ disable exception caught --> OSType cannot be retrieved
		}
		return osType;
	}
	
	/**
	 * Retrieves the OS Type for the Image.
	 * Usually this has to be maintained as custom property on the base image: os_type
	 * @param image 
	 * @return null if os_type not maintained
	 */
	public String getOSType(Image image) {
		String osType = null;
		if (image.getProperties() != null ) {
			if(image.getProperties().containsKey(OpenStackConstants.CUSTOM_PROPERTY_OS_TYPE)) {
				osType = image.getProperties().get(OpenStackConstants.CUSTOM_PROPERTY_OS_TYPE);
			} else {
				// Find base Image and check the OS Type there
				if(image.getProperties().containsKey(OpenStackConstants.CUSTOM_PROPERTY_BASE_IMAGE_REF)) {
					try {
						Image baseImage = describeImage(image.getProperties().get(OpenStackConstants.CUSTOM_PROPERTY_BASE_IMAGE_REF));
						if (baseImage.getProperties() != null ) {
							if (baseImage.getProperties().containsKey(OpenStackConstants.CUSTOM_PROPERTY_OS_TYPE)) {
								osType = baseImage.getProperties().get(OpenStackConstants.CUSTOM_PROPERTY_OS_TYPE);
							}							
						}
					} catch (CloudClientException e) {
						// $JL-EXC$ disable exception caught --> OSType cannot be retrieved
					}
				}
			}		
		}
		
		return osType;
	}
	
	/**
	 * @return list of availability zones 
	 */
	public List<String> listAvailabilityZones() {
		OSClientV3 os=getOs();
		ArrayList<String> zoneNames=new ArrayList<String>();
		List<? extends AvailabilityZone> zones = os.compute().zones().list();
		//zoneItr=zones.iterator();
		if (zones.size()>0)
			for (AvailabilityZone zone:zones)
				zoneNames.add(zone.getZoneName());
		else
			zoneNames.add("DefaultZone")	;
		return zoneNames;
	}
	
	public String getInstanceAddresses(String instanceId) throws CloudClientException {
		OSClientV3 os = getOs();
		List<? extends InterfaceAttachment> interfaces = os.compute().servers().interfaces().list(instanceId);
		if (interfaces.isEmpty()==false) {
				for (InterfaceAttachment interf : interfaces) {
					// TODO: put this in try catch block
					List<? extends FixedIp> fixedips = interf.getFixedIps();
					for (FixedIp fixedIP : fixedips) {
						String ip = fixedIP.getIpAddress();
						return ip;
					}
				}
		}
		return null;
		
	}
	
	public void deleteImage(String imageId) throws CloudClientException {
		ActionResponse resp;
		try {
			OSClientV3 os = getOs();
			resp = os.images().delete(imageId);
			if (!resp.isSuccess())
				throw new CloudClientException(new Exception("Failed to delete image " + imageId));


		} catch (RuntimeException e) {
			throw new CloudClientException(e);
		}

	}
}
