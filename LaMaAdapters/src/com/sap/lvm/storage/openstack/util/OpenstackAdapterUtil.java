package com.sap.lvm.storage.openstack.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.openstack4j.model.compute.Server;
import org.openstack4j.model.manila.Share;
import org.openstack4j.model.manila.ShareSnapshot;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeSnapshot;

import com.sap.tc.vcm.infrastructure.api.adapter.IInfrastructAdapter.ExternalURL;
import com.sap.tc.vcm.infrastructure.api.adapter.config.ConfigPropMetaData;
import com.sap.tc.vcm.storage.adapter.api.types.StoragePool;
import com.sap.tc.vcm.storage.adapter.api.types.StoragePool.StoragePoolType;
import com.sap.tc.vcm.storage.adapter.api.types.StorageSnapshotVolume;
import com.sap.tc.vcm.storage.adapter.api.types.StorageSystem;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolume;
import com.sap.tc.vcm.storage.adapter.api.types.StorageVolume.VolumeType;

public class OpenstackAdapterUtil {

	public static final String PROP_KEY_PROXY_HOST = "PROXY_HOST";
	public static final String PROP_KEY_PROXY_PORT = "PROXY_PORT";

	public static List<StorageVolume> transformToStorageVolumeList(List<Volume> volumeList, String storageSystemId) {
		List<StorageVolume> internalVolumes = new ArrayList<StorageVolume>();
		for (Volume vol : volumeList) {
			internalVolumes.add(toStorageVolume(vol, storageSystemId));
		}
		return internalVolumes;
	}

	public static List<StorageVolume> transformSharesToStorageVolumeList(List<Share> shareList, String storageSystemId, String storagePoolId) {
		List<StorageVolume> internalVolumes = new ArrayList<StorageVolume>();
		for (Share share : shareList) {
			internalVolumes.add(sharetoStorageVolume(share, storageSystemId, storagePoolId));
		}
		return internalVolumes;
	}


	public static StorageVolume toStorageVolume(Volume vol, String storageSystemId) {

		String region = storageSystemId.substring(storageSystemId.indexOf(':')+1);
		StorageVolume internalVolume = new StorageVolume();

		{
			internalVolume.storageVolumeId = region+":"+vol.getId();
		}
		internalVolume.totalSpaceMB = vol.getSize()*1024;



		/*
		 * Openstack  has user definable volume types , so it's difficult to create generic mapping to internal types
		 */

		internalVolume.volumeType = StorageVolume.VolumeType.SAN;
		internalVolume.name = vol.getName()+"("+vol.getId()+")";
		internalVolume.freeSpaceMB = 0L;
		internalVolume.storagePoolId = region+':'+vol.getZone();
		internalVolume.storageSystemId = storageSystemId;
		internalVolume.lun = null;
		return internalVolume;

	}

	public static StorageVolume sharetoStorageVolume(Share share, String storageSystemId, String storagePoolId) {

		String backend = storageSystemId.substring(storageSystemId.indexOf(':')+1);
		String pool = storagePoolId.substring(storagePoolId.indexOf(':')+1);

		StorageVolume internalVolume = new StorageVolume();
		internalVolume.storageVolumeId = share.getId();
		internalVolume.totalSpaceMB = share.getSize()*1024;
		internalVolume.volumeType = StorageVolume.VolumeType.NAS;
		internalVolume.name = share.getName(); // +"("+share.getId()+")";
		internalVolume.freeSpaceMB = 0L;
		internalVolume.storagePoolId = backend+':'+pool;
		internalVolume.storageSystemId = storageSystemId;
		internalVolume.lun = null;
		return internalVolume;
	}

	public static StorageSnapshotVolume toStorageSnapshot(VolumeSnapshot snap, String storageSystemId) {

		String region = storageSystemId.substring(storageSystemId.indexOf(':')+1);
		StorageSnapshotVolume internalSnapshot = new StorageSnapshotVolume();
		internalSnapshot.storageVolumeId = region+":"+snap.getId();
		internalSnapshot.totalSpaceMB = snap.getSize();
		internalSnapshot.volumeType = VolumeType.SAN;
		internalSnapshot.name = snap.getId();
		internalSnapshot.snapshotName = snap.getDescription();
		internalSnapshot.freeSpaceMB = 0L;
		internalSnapshot.storagePoolId = region+':'+OpenstackConstants.Openstack_POOL_SNAPSHOTS;
		internalSnapshot.storageSystemId = storageSystemId;
		Calendar c = Calendar.getInstance();
		c.setTime(snap.getCreated());
		internalSnapshot.snapshotTimestamp = c.getTimeInMillis();
		internalSnapshot.synchronizedFromVolumeId = snap.getVolumeId();
		return internalSnapshot;
	}
 
	public static StorageSnapshotVolume shareSnapshotToStorageSnapshot(ShareSnapshot snap, String storageSystemId) {

		StorageSnapshotVolume internalSnapshot = new StorageSnapshotVolume();
		internalSnapshot.storageVolumeId = snap.getId();
		internalSnapshot.totalSpaceMB = snap.getSize();
		internalSnapshot.volumeType = VolumeType.NAS;
		internalSnapshot.name = snap.getId();
		internalSnapshot.snapshotName = snap.getName();
		internalSnapshot.freeSpaceMB = 0L;
//		internalSnapshot.storagePoolId = region+':'+OpenstackConstants.Openstack_POOL_SNAPSHOTS;
		internalSnapshot.storagePoolId = "";
		internalSnapshot.storageSystemId = storageSystemId;
		internalSnapshot.synchronizedFromVolumeId = snap.getShareId();
		return internalSnapshot;
	}
	public static List<StorageSnapshotVolume> transformToStorageVolumeMap(List<VolumeSnapshot> snapshotList, String storageSystemId) {

		List<StorageSnapshotVolume> internalSnapshotList = new ArrayList<StorageSnapshotVolume>();
		for (VolumeSnapshot snapshot : snapshotList) {
			internalSnapshotList.add(toStorageSnapshot(snapshot, storageSystemId));

		}
		return internalSnapshotList;
	}

	public static List<StorageSnapshotVolume> transformShareSnapshotToStorageVolumeMap(List<ShareSnapshot> snapshotList, String storageSystemId) {

		List<StorageSnapshotVolume> internalSnapshotList = new ArrayList<StorageSnapshotVolume>();
		for (ShareSnapshot snapshot : snapshotList) {
			internalSnapshotList.add(shareSnapshotToStorageSnapshot(snapshot, storageSystemId));

		}
		return internalSnapshotList;
	}

	public static StorageSystem createStorageSystem(String region, String accountId) {
		StorageSystem system = new StorageSystem();
		system.name = region;
		system.storageSystemId = accountId+':'+region;
		system.vendorName = "Openstack";
		List<ExternalURL> urls = new ArrayList<ExternalURL>();
		ExternalURL url = new ExternalURL();
		url.description = "Openstack Console";
		url.label = "Openstack Console";
		try {
			//TODO: fix this to genric url for horizon UI, if it's ever used
			url.endpoint = new URL("http://www.sap.com/pc/tech/cloud/software/virtualization/index.html");
			urls.add(url);
		} catch (MalformedURLException e) {
			//$JL-EXC$
		}
		system.storageManagementUrls = urls;
		return system;
	}

	public static StorageSystem createFileStorageSystem(String backend, String accountId ) {
		StorageSystem system = new StorageSystem();
		system.name = backend;
		system.storageSystemId = accountId+':'+backend;
		system.vendorName = OpenstackConstants.Openstack_VENDOR;
		system.modelName = OpenstackConstants.Openstack_MANILA;
		List<ExternalURL> urls = new ArrayList<ExternalURL>();
		ExternalURL url = new ExternalURL();
		url.description = "Openstack Console";
		url.label = "Openstack Console";
		try {
			//TODO: fix this to genric url for horizon UI, if it's ever used
			url.endpoint = new URL("http://www.sap.com/pc/tech/cloud/software/virtualization/index.html");
			urls.add(url);
		} catch (MalformedURLException e) {
			//$JL-EXC$
		}
		system.storageManagementUrls = urls;
		return system;
	}

	public static StoragePool createStoragePool(String zone, String region, String accountId,long totalSpaceGB,long usedSpaceGB ) {
		//TODO: come up with something better / fix region
		String parsedZone= zone.contains(":") ? zone.split(":")[1] : zone;
		String parsedRegion= region.contains(":") ? region.split(":")[1] : region;
		
		StoragePool volumePool = new StoragePool(parsedRegion+":"+parsedZone);
		volumePool.name=zone;
		volumePool.storageSystemId =  accountId+":"+parsedRegion;
		volumePool.poolType = StoragePoolType.VolumePool;
		if (totalSpaceGB>0){
			volumePool.totalSpaceMB=totalSpaceGB*1024;
			long usedSpaceMB=usedSpaceGB*1024;
			volumePool.freeSpaceMB= volumePool.totalSpaceMB-usedSpaceMB;
		}
		return volumePool;
	}
	
	public static StoragePool createFileStoragePool(String aggr, String backend, String accountId,long totalSpaceGB,long usedSpaceGB ) {
		//TODO: come up with something better / fix region
		String parsedAggr= aggr.contains(":") ? aggr.split(":")[1] : aggr;
		String parsedBackend= backend.contains(":") ? backend.split(":")[1] : backend;

		StoragePool volumePool = new StoragePool(parsedBackend+":"+parsedAggr);
		volumePool.name=parsedAggr;
		volumePool.storageSystemId =  accountId+":"+parsedBackend;
		volumePool.poolType = StoragePoolType.VolumePool;
		if (totalSpaceGB>0){
			volumePool.totalSpaceMB=totalSpaceGB*1024;
			long usedSpaceMB=usedSpaceGB*1024;
			volumePool.freeSpaceMB= volumePool.totalSpaceMB-usedSpaceMB;
		}
		return volumePool;
	}

	public static String convertShareIdToName(String shareId) {
        return "share_" +shareId.replaceAll("-", "_");
    }

	public static String convertShareNameToId(String shareName) {
        return shareName.replaceFirst("share_", "").replaceAll("_", "-");
    }

	public static  String generateOperationId() {
		return UUID.randomUUID().toString();
	}

	public static List<ConfigPropMetaData> getVolumeConfigMetaData() {

		List<ConfigPropMetaData> configMetaDatas = new ArrayList<ConfigPropMetaData>();

//		TranslatableString key0 = new TranslatableString("EBS_VOLUME_TYPE", I18nTexts.DOMAIN_INFRASTRUCTURE, "EBS_VOLUME_TYPE");
//		TranslatableString description0 = new TranslatableString("EBS_VOLUME_TYPE_DESCRIPTION", I18nTexts.DOMAIN_INFRASTRUCTURE, "EBS_VOLUME_TYPE_DESCRIPTION");
//
//		TranslatableString key1 = new TranslatableString("IOPS", I18nTexts.DOMAIN_INFRASTRUCTURE, "IOPS");
//		TranslatableString description1 = new TranslatableString("IOPS_DESCRIPTION", I18nTexts.DOMAIN_INFRASTRUCTURE, "IOPS_DESCRIPTION");
//
//		ConfigPropMetaData configMetaData0 = new ConfigPropMetaData(key0, ValueType.STRING, description0, false);
//		configMetaDatas.add(configMetaData0);
//
//		ConfigPropMetaData configMetaData1 = new ConfigPropMetaData(key1, ValueType.INT, description1, false);
//		configMetaDatas.add(configMetaData1);

		return configMetaDatas;
	}

	public static String getVolumeId(Server instance, String device) {
		List<String> bdmapping = instance.getOsExtendedVolumesAttached();
		for(String ibdm : bdmapping) {
			//TODO: we should get the device name from the volume ; this is just iterating through volumeIDs
			if(ibdm.equals(device)) {
				return ibdm;
			}
		}		
		return null;
	}

//	public static HttpProxyDataImpl getProxyData(Map<String, String> additionalConfigProps) {
//		String proxyHost = additionalConfigProps.get(PROP_KEY_PROXY_HOST);
//		String proxyPort = additionalConfigProps.get(PROP_KEY_PROXY_PORT);
//		HttpProxyDataImpl httpProxy=null;
//		if (MiscUtil.notNullAndEmpty(proxyHost) && MiscUtil.notNullAndEmpty(proxyPort)) {
//			httpProxy = new HttpProxyDataImpl();
//			httpProxy.setHost(proxyHost);
//			httpProxy.setPort(Integer.valueOf(proxyPort));
//		}
//		return httpProxy;
//	}

}
