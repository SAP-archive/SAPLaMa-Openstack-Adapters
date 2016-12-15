package com.sap.lvm.storage.openstack.block;

import java.util.ArrayList;
import java.util.List;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.ProxyHost;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.identity.v3.Region;
import org.openstack4j.model.storage.block.BlockLimits;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.model.storage.block.builder.VolumeBuilder;
import org.openstack4j.openstack.OSFactory;
import org.openstack4j.openstack.storage.block.domain.AvailabilityZone;

import com.sap.lvm.storage.openstack.util.CloudClientException;
import com.sap.lvm.storage.openstack.util.OpenstackConstants.OpenstackVolumeStates;
import com.sap.lvm.util.MiscUtil;




public class OpenstackBlockCloudStorageController {
	
    public boolean v3;
	static OSClientV3 os;

	private String tenant = "Default";
	private String region = "Default";
	private String domain = "Default";
	private String project = "Default";

	private String proxyHost;
	private int proxyPortint;
	private String proxyPort;
	private String proxyPassword; 
	private String proxyUsername;  

	private String backend = "Default";
	private String pool = "Default";
	String accessKey;
	String secretKey;
	String endpoint;
	String username;
	String password;
	String accountId;

 	public OpenstackBlockCloudStorageController (
			String accountId,String endpoint, String username, String password, 
			String region, String tenant,  
			String domain, String project,  
			String proxyHost, String proxyPort, String proxyUsername, String proxyPassword) throws CloudClientException {

			
			this.region = region;
			this.accountId=accountId;
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

			v3 = this.endpoint.endsWith("/v3");
			
			ClassLoader cl = OpenstackBlockCloudStorageController.class.getClassLoader();
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


 	public synchronized List<Volume> getVolumesByServiceId(String serviceId) throws CloudClientException {
 		return listVolumes(serviceId);
	}

	public synchronized Volume createVolume(String snapshotId, String availabilityZone, String volumeType,  String description ) throws CloudClientException {


		OSClientV3 os=getOs();
		snapshotId = getOpenstackId(snapshotId);
		availabilityZone = getOpenstackId(availabilityZone);
	
		VolumeBuilder volBuilder = Builders.volume()
		.name("Cloned Volume")
		.description(description)
		.snapshot(snapshotId)
		.zone(availabilityZone);
		if ((volumeType!=null) && (!volumeType.equals("None")))
			volBuilder.volumeType(volumeType);  //in openstack this is just a label with no inherent semantic value and must be created by user in advance

		Volume v = os.blockStorage().volumes()
		.create(volBuilder.build());
		return v;

	}

	public synchronized VolumeSnapshot getSnapshotStatus(String snapshotId) throws CloudClientException {

		snapshotId = getOpenstackId(snapshotId);

		OSClientV3 os=getOs();

		VolumeSnapshot snapshot = os.blockStorage().snapshots().get(snapshotId);
		return snapshot;
	}

	public synchronized VolumeSnapshot createSnapshot(String volumeId, String description, boolean force) throws CloudClientException {

		volumeId = getOpenstackId(volumeId);
		OSClientV3 os=getOs();
		if (volumeId.contains(":"))
			volumeId=volumeId.split(":")[1];
		VolumeSnapshot snap = os.blockStorage().snapshots()
		.create(Builders.volumeSnapshot()
				.name("Backup Volume")
				.description(description)
				.volume(volumeId).force(force)  
				.build());

		return snap;


	}

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


	public synchronized List<String> getRegions() throws CloudClientException {

		OSClientV3 os=getOs();
		ArrayList<String> regionNames=new ArrayList<String>();
		List<? extends Region> regions = os.identity().regions().list();
		
		if (regions.size()>0)
			for (Region region:regions)
				regionNames.add(region.getId());
		else
			regionNames.add("DefaultZone")	;
		return regionNames;
	}
	
	public String getAccountId() {

		return this.accountId; 
	}

	public String getOpenstackId(String storageSystemId) {
		if (storageSystemId.contains(":"))
			storageSystemId=storageSystemId.split(":")[1];
		return storageSystemId; 
	}

	public List<String> listAvailabilityZones(String region) {
	//	OSClient os=getOs();
		ArrayList<String> zoneNames=new ArrayList<String>();
//		List<? extends AvailabilityZone> zones = os.compute().zones().list();
//		//zoneItr=zones.iterator();
//		if (zones.size()>0)
//			for (AvailabilityZone zone:zones)
//				zoneNames.add(zone.getZoneName());
//		else
			zoneNames.add("nova")	;
		return zoneNames;
	}

	
	public List<String> getAvailabilityZones() {
		OSClientV3 os=getOs();
		ArrayList<String> zones=new ArrayList<String>();
		List<? extends AvailabilityZone> availabilityZones = os.blockStorage().zones().list();
		if (availabilityZones.size()>0)
		for (AvailabilityZone availabilityZone : availabilityZones) {
			zones.add(availabilityZone.getZoneName());
		}
		else
			zones.add("nova")	;
		return zones;
	}
	
	public String getRegion(String poolId) {

		return this.region;
	}

	public List<Volume> listVolumes(String storagePoolId) {
		// TODO listVolumesBy region and/or zone? 
		OSClientV3 os=getOs();
		List<? extends Volume> volumes = os.blockStorage().volumes().list();


		return (List<Volume>) volumes;
	}
	public BlockLimits getBlockStorageLimits(){
		OSClientV3 os=getOs();
		return os.blockStorage().getLimits();


	}
	public Volume getVolume(String volumeId) {
		OSClientV3 os=getOs();
		//TODO: make this more elegant
		if (volumeId.contains(":"))
			volumeId=volumeId.split(":")[1];
		Volume volume = os.blockStorage().volumes().get(volumeId);

		return volume;
	}

	public ActionResponse detachVolume(String storageVolumeId) {
		OSClientV3 os=getOs();
		if (storageVolumeId.contains(":"))
			storageVolumeId=storageVolumeId.split(":")[1];
		Volume volume=os.blockStorage().volumes().get(storageVolumeId);
		String serverId=volume.getAttachments().get(0).getServerId(); 
		ActionResponse response = os.compute().servers().detachVolume(serverId, storageVolumeId);
		return response;

	}

	public synchronized void  attachVolume(String storageVolumeId, String instanceId,
			String device) {
	
		OSClientV3 os=getOs();
		storageVolumeId=getOpenstackId(storageVolumeId);
		String updatedStatus=getVolume(storageVolumeId).getStatus().toString();
		//we have to check here if the volume is available before attaching 
		//due to a race condition LVM could try to mount the same volume twice and end up throwing an error here on the second try

		if (OpenstackVolumeStates.available.toString().equals(updatedStatus)) 				
			os.compute().servers().attachVolume(instanceId, storageVolumeId, device);
	
	}


	public void deleteVolume(String storageVolumeId) {
		OSClientV3 os=getOs();
		storageVolumeId=getOpenstackId(storageVolumeId);
		os.blockStorage().volumes().delete(storageVolumeId);
	}

	public VolumeSnapshot getSnapshot(String snapshotId) {
		OSClientV3 os=getOs();
		VolumeSnapshot snap = os.blockStorage().snapshots().get(snapshotId);
		return snap;
	}

	public void deleteSnapshot(String snapshotId) throws CloudClientException  {
	
		OSClientV3 os=getOs();
		ActionResponse result = os.blockStorage().snapshots().delete(snapshotId);
		
		if (!result.isSuccess())		
				throw new CloudClientException("Failed to delete Openstack snapshot:"+result.toString());
	}

	public Server findInstanceByInternalIP(String region, String hostAddress) {
		// not universally supported in Openstack 
		return null;
	}


	public boolean supportsIOPS(String ebsType) {	
		return false;
	}


	public VolumeSnapshot copy(String snapshotId, String targetRegion,
			String string) {
		//copy volumes across regions not supported 
		return null;
	}

}
