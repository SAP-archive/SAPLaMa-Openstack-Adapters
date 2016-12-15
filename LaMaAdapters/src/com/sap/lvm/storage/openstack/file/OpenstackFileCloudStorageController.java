package com.sap.lvm.storage.openstack.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.core.transport.Config;
import org.openstack4j.core.transport.ProxyHost;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.compute.ext.AvailabilityZone;
import org.openstack4j.model.identity.v3.Region;
import org.openstack4j.model.manila.Access;
import org.openstack4j.model.manila.Access.Level;
import org.openstack4j.model.manila.Access.State;
import org.openstack4j.model.manila.Access.Type;
import org.openstack4j.model.manila.Share;
import org.openstack4j.model.manila.Share.Status;
import org.openstack4j.model.manila.ShareCreate;
import org.openstack4j.model.manila.ShareSnapshot;
import org.openstack4j.model.manila.ShareSnapshotCreate;
import org.openstack4j.model.manila.actions.AccessOptions;
import org.openstack4j.openstack.OSFactory;

import com.sap.lvm.CloudClientException;
import com.sap.lvm.util.MiscUtil;



public class OpenstackFileCloudStorageController {

	public boolean v3;
	
	static OSClientV3 os;

	private String region = "Default";
	private String domain = "Default";
	private String project = "Default";
	private String backend = "Default";
	private String pool = "Default";
	
	String accessKey;
	String secretKey;
	String endpoint;
	String username;
	String password;
	String tenant;
	String accountId;

	private String proxyHost;
	private int proxyPortint;
	private String proxyPort;
	private String proxyPassword; 
	private String proxyUsername; 

	public OpenstackFileCloudStorageController (
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
		
		v3 = this.endpoint.endsWith("/v3/");

		ClassLoader cl = OpenstackFileCloudStorageController.class.getClassLoader();
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


	public synchronized List<Share> getVolumesByServiceId(String serviceId) throws CloudClientException {
		return listShares(serviceId);
	}

	public synchronized Share createShare(String snapshotId, String availabilityZone, String shareType,  String description ) throws CloudClientException {
		OSClientV3 os=getOs();
		snapshotId = getOpenstackId(snapshotId);
		if (shareType==null)
			shareType="general";

		ShareCreate builder = Builders.share().name("Cloned Share").description(description).snapshotId(snapshotId).availabilityZone(availabilityZone).build();
		Share share = os.share().shares().create(builder);

		return share;
	}

	public synchronized ShareSnapshot createShareSnapshot(String shareId, String name, String description) throws CloudClientException {
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);
		ShareSnapshotCreate builder = Builders.shareSnapshot().name(name).description(description).shareId(shareId).build();
		ShareSnapshot snap = os.share().shareSnapshots().create(builder);
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

	public List<String> listBackends() {
		OSClientV3 os=getOs();
		Set<String> backends = new HashSet<String>();		
		List<? extends Share> shares = os.share().shares().listDetails();
		if (shares.size()>0)
			for (Share share : shares) {
				String shareHost = share.getHost();
				String spId = shareHost.split("@")[1];
				String backend = spId.split("#")[0];
				if (MiscUtil.notNullAndEmpty(backend)) {
					backends.add(backend);
				}
			}
		else
			backends.add(pool);

		return new ArrayList<String>(backends);
	}	
	
	public String getAccountId() {
		return this.accountId; 
	}

	public String getOpenstackId(String id) {
		if (id.contains(":"))
			id=id.split(":")[1];
		return id; 
	}

	public String getBackend(String shareId) {
		
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);
		shareId = shareId.trim();
		Share share = os.share().shares().get(shareId);
		String spId = share.getHost().split("@")[1];
		String backend = spId.split("#")[0];

		return backend; 
	}

	public List<String> listPools (String backend) {
		OSClientV3 os=getOs();
		ArrayList<String> pools=new ArrayList<String>();
		List<? extends Share> shares = os.share().shares().listDetails();
		if (shares.size()>0)
			for (Share share : shares) {
				String shareHost = share.getHost();
				String spId = shareHost.split("@")[1];
				String shareBackend = spId.split("#")[0];
				String sharePool = spId.split("#")[1];
				if (MiscUtil.notNullAndEmpty(backend)) {
					if (MiscUtil.equals(shareBackend, backend)) {
						pools.add(sharePool);
					}
				} else {
					pools.add(sharePool);
				}
			}
		else
			pools.add(pool);
		return pools;
	}
	
	public String getPool(String shareId) {
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);
		Share share = os.share().shares().get(shareId);
		String spId = share.getHost().split("@")[1];
		String pool = spId.split("#")[1];

		return pool;
	}

	public String getRegion(String poolId) {
		return this.region;
	}

	public List<Share> listShares(String storagePoolId) {

		OSClientV3 os=getOs();
		List<Share> listShares = new ArrayList<Share>();
		
		List<? extends Share> shares = os.share().shares().listDetails();
		for (Share share : shares) {
			if (MiscUtil.notNullAndEmpty(storagePoolId)) {
				String spId = share.getHost().split("@")[1];
				spId = spId.replace("#",":");
				if (MiscUtil.equals(spId, storagePoolId)) {
					listShares.add(share);
				}
			} else {
				listShares.add(share);
			}
		}
		return listShares;
	}

	public List<? extends Access> listAccess(String shareId) {
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);		
		List<? extends Access> accessList = os.share().shares().listAccess(shareId);
		return accessList;
	}

	public Access getAccess(String shareId, String shareAccessId) {
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);		
		List<? extends Access> accessList = os.share().shares().listAccess(shareId);
		for (Access shareAccessMapping : accessList) {
			if (MiscUtil.equals(shareAccessMapping.getId(),shareAccessId)) {
				return shareAccessMapping;
			} 
		}
		return null;
	}

	public Boolean allowAccess(String shareId, String accessTo) {
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);		
		Level accessLevel = Level.RW ;
		Type accessType = Type.IP;
		
		AccessOptions accessOptions = new AccessOptions(accessLevel, accessType, accessTo);
		Access resp = os.share().shares().grantAccess(shareId, accessOptions);
		String shareAccessId = resp.getId();
		State state = resp.getState();
		Boolean active = false;
		while (!state.equals(State.ACTIVE)) {
			resp = getAccess(shareId, shareAccessId);
		}
		return active;
	}
	
	public Share getShare(String shareId) {
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);
		Share share = os.share().shares().get(shareId);
		return share;
	}

	public Share getSharebyExport(String exportlocation) {
		OSClientV3 os=getOs();
		exportlocation = exportlocation.trim();
		List<? extends Share> shares = os.share().shares().list();
		for (Share share : shares) {
			if (exportlocation.contains(share.getExportLocation().trim())) {
				return share;
			}
		}
		return null;
	}

	public Share getSharebyName(String shareName) {
		OSClientV3 os=getOs();
		List<? extends Share> shares = os.share().shares().list();
		for (Share share : shares) {
			if (MiscUtil.equals(share.getName(), shareName)) {
				return share;
			}
		}
		return null;
	}

	public Status getShareStatus(String shareId) {
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);
		Status status = os.share().shares().get(shareId).getStatus();
		return status;
	}

	public void deleteShare(String shareId) throws CloudClientException {
		OSClientV3 os=getOs();
		shareId=getOpenstackId(shareId);
		ActionResponse response = os.share().shares().delete(shareId);
		if (!response.isSuccess())		
			throw new CloudClientException("Failed to delete Openstack share:"+response.toString());
	}

	public ShareSnapshot getSnapshot(String snapshotId) {
		OSClientV3 os=getOs();
		snapshotId = getOpenstackId(snapshotId);
		ShareSnapshot snap = os.share().shareSnapshots().get(snapshotId);
		return snap;
	}

	public List<ShareSnapshot> getVCMSnapshots(String shareId) {
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);
		List<ShareSnapshot> VCMshareSnapshots = new ArrayList<ShareSnapshot>();
		Map<String, String> filteringParams = new HashMap<String, String>();
		filteringParams.put("share_id", shareId);
		List<? extends ShareSnapshot> shareSnapshots = os.share().shareSnapshots().list();
		for (ShareSnapshot shareSnapshot : shareSnapshots) {
			if (shareSnapshot.getShareId().equals(shareId)) {
				VCMshareSnapshots.add(os.share().shareSnapshots().get(shareSnapshot.getId()));
			}
		}
		return VCMshareSnapshots;
	}
	public ShareSnapshot getVCMSnapshot(String shareId,String sapshotName) {
		OSClientV3 os=getOs();
		shareId = getOpenstackId(shareId);
		ShareSnapshot VCMshareSnapshot = null;
		Map<String, String> filteringParams = new HashMap<String, String>();
		filteringParams.put("share_id", shareId);
		List<? extends ShareSnapshot> list = os.share().shareSnapshots().list();
		for (ShareSnapshot shareSnapshot : list) {
			if (shareSnapshot.getShareId().equals(shareId) && shareSnapshot.getName().contains(sapshotName)) {
				VCMshareSnapshot = os.share().shareSnapshots().get(shareSnapshot.getId());
				break;
			}
		}
		return VCMshareSnapshot;
	}

	public synchronized org.openstack4j.model.manila.ShareSnapshot.Status getSnapshotStatus(String snapshotId) throws CloudClientException {
		OSClientV3 os=getOs();
		snapshotId = getOpenstackId(snapshotId);
		return os.share().shareSnapshots().get(snapshotId).getStatus();
	
	}

	public void deleteSnapshot(String snapshotId) throws CloudClientException  {
		OSClientV3 os=getOs();
		snapshotId = getOpenstackId(snapshotId);
		ActionResponse response = os.share().shareSnapshots().delete(snapshotId);
		if (!response.isSuccess())		
			throw new CloudClientException("Failed to delete Openstack snapshot:"+response.toString());
	}

	public ShareSnapshot copy(String snapshotId, String backend, String string) {
		//copy volumes across backends not supported
		return null;
	}

	public Share createShareFromSnapshot(String shareName, ShareSnapshot snapshot, String shareType) {

		OSClientV3 os=getOs(); 
		ShareCreate sharePrototype = Builders.share().snapshotId(snapshot.getId()).shareType(shareType).name(shareName).size(snapshot.getSize()).shareProto(snapshot.getShareProto()).build();
		Share share = os.share().shares().create(sharePrototype);
		return share;
	}

	public ShareSnapshot createSnapshot(Share share, String snapshotName) {

		OSClientV3 os=getOs(); 
		ShareSnapshotCreate builder = Builders.shareSnapshot().shareId(share.getId()).name(snapshotName).build();
		ShareSnapshot snapshot = os.share().shareSnapshots().create(builder);
		return snapshot;
	}

	private static Access allowAccess(OSClientV3 os, String accessTo, Type accessType, Share share) {

		AccessOptions AccessOptions = new AccessOptions(Level.RW, accessType, accessTo);
		Access accessMapping = os.share().shares().grantAccess(share.getId(), AccessOptions);
		return accessMapping;
	}
	public static boolean isCloneAvailable(String cloneShareId) {

		Share cloneShare = os.share().shares().get(cloneShareId);
		if (Status.AVAILABLE.equals(cloneShare.getStatus())) {
			allowAccess(os, "0.0.0.0/0", Type.IP, cloneShare);
			return true;
		}
		return false;
	}

	public static boolean isCloneNotAvailable(String cloneShareId) {
		return !isCloneAvailable(cloneShareId);
	}

	public static boolean areClonesAvailable(Set<String> cloneShareSet) {

		for (String cloneShareId : cloneShareSet) {
			if (isCloneNotAvailable(cloneShareId)) {
				return false;
			}
		}
		return true;
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

	public List<String> listAvailabilityZones(String region) {
		OSClientV3 os=getOs();
		ArrayList<String> zoneNames=new ArrayList<String>();
		List<? extends AvailabilityZone> zones = os.compute().zones().list();

		if (zones.size()>0)
			for (AvailabilityZone zone:zones)
				zoneNames.add(zone.getZoneName());
		else
			zoneNames.add("DefaultZone")	;
		return zoneNames;
	}

}