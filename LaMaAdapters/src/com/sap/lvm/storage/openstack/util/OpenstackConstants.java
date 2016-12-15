package com.sap.lvm.storage.openstack.util;
 
@SuppressWarnings("nls")
public class OpenstackConstants {
	
	public static final String OS_REGION = "Region";
	public static final String OS_REGION_DESCRIPTION = "Openstack Region";
	public static final String OS_TENANT= "Tenant Name";
	public static final String OS_TENANT_DESCRIPTION= "Openstack Tenant";
	public static final String OS_DOMAIN = "Domain";
	public static final String OS_DOMAIN_DESCRIPTION = "Openstack Domain";
	public static final String OS_PROJECT = "Project";
	public static final String OS_PROJECT_DESCRIPTION = "Openstack Project";
	
	public static final String SECRET_KEY = "Secret_Key";
	public static final String SECRET_KEY_DESCRIPTION = "Secret Key for Openstack";
	public static final String ACCESS_KEY = "Access_Key";
	public static final String ACCESS_KEY_DESCRIPTION = "Access Key for Openstack";
	public static final String PROXY_PASS = "Proxy_password";
	public static final String PROXY_PASS_DESCRIPTION = "Proxy password";
	public static final String PROXY_USER_NAME = "Proxy_user";
	public static final String PROXY_USER_NAME_DESCRIPTION = "Proxy username";
	public static final String PROXY_PORT = "Proxy_port";
	public static final String PROXY_PORT_DESCRIPTION = "Proxy port";
	public static final String PROXY_HOST = "Proxy_host";
	public static final String PROXY_HOST_DESCRIPTION = "Proxy host";
	
	public static final long POLL_OPERATION_SLEEP = 5000;

	public static final String Openstack_VENDOR = "Openstack";
	public static final String Openstack_VERSION = "1.3.0";
	public static final String Openstack_FACTORY_ID = OpenstackConstants.Openstack_VENDOR + " " + OpenstackConstants.Openstack_VERSION;
	public static final String Openstack_VIRTUALIZATION_ADAPTER = "Openstack";
	public static final String Openstack_CLOUD = "Openstack Cloud";
	
	public static final String OpenstackBlock_ADAPTER_DESCRIPTION = "Openstack Block Storage Adapter for SAP LVM";
	public static final String OpenstackBlock_ADAPTER_NAME = "Openstack Block Storage Adapter";

	public static final String OpenstackFile_ADAPTER_DESCRIPTION = "Openstack File Storage Adapter for SAP LVM";
	public static final String OpenstackFile_ADAPTER_NAME = "Openstack File Storage Adapter";

	public static final String Openstack_MANILA = "FILE_STORAGE";
	public static final String Openstack_CINDER = "BLOCK_STORAGE";
	
	
	public static String INSTANCE_TYPE = "Instance type";
	public static String SUBNET_ID = "Subnet ID";
	public static String REGION_TYPE_ID = "OpenstackRegion";
	public static String REGION_TYPE_NAME = "Openstack Region DataCenter";	
	public static String REGION_TYPE_DESCRIPTION = "Openstack Region DataCenter";
	public static String ACCOUNT_ID = "AccountId";
	public static String ACCOUNT_ID_DESCRIPTION ="Openstack EC2 Account Id";
	public static String EBS_VOLUME_TYPE = "EBS Volume Type";
	public static String EBS_VOLUME_TYPE_DESCRIPTION = "EBS Voulume Type";
	public static String IOPS = "IOPS Performance";
	public static String IOPS_DESCRIPTION = "IOPS Performance";
	
	
	public static final String Openstack_POOL_VOLUMES = "Volumes";
	public static final String Openstack_POOL_SNAPSHOTS = "Snapshots";
	
	public static final String LVM_ID = "LVM_volume_id";
	public static final String TARGET_VOLUME_TAG = "LVM_target_volume_id";
	public static final String CLONE_OPERATION_TAG = "LVM_clone_operation_id";
	public static final String LVM_SERVICEID = "LVM_service_Id";
	public static final String LVM_EXPORT_PATH = "LVM_export_path";
	public static final String LVM_MOUNTPOINT = "LVM_mountpoint";
	
	
	
	
	public static enum OpenstackInstanceStates {
		pending, running, stopping, stopped, available, terminated
	}
	
	public static enum OpenstackVolumeStates {
		creating("creating"), available("available"), inuse("in-use"), deleting("deleting"), error("error");
		
		String value;
		
		OpenstackVolumeStates(String value) {
			this.value = value;
		}
		
		@Override
		public String toString() {
			return this.value;
		}
	}
	
	public static enum OpenstackSnapshotStates {
		pending, completed, error
	}
	
	public static enum OpenstackAttachmentStatus {
		 attaching, attached, detaching, detached
	}
	
	
	public static enum OpenstackCloneSteps {
		backup, create, deregister
	}
	
}
