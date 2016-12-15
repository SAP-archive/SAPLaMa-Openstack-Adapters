package com.sap.lvm.virtual.openstack;

import java.util.HashSet;
import java.util.Set;

import com.sap.tc.vcm.virtualization.adapter.api.operation.VirtDefaultOperation;

@SuppressWarnings("nls")
public class OpenStackConstants {
	
	public static final String OS_REGION = "Region";
	public static final String OS_REGION_DESCRIPTION = "Openstack Region";
	public static final String OS_TENANT= "Tenant Name";
	public static final String OS_TENANT_DESCRIPTION= "Openstack Tenant";
	public static final String OS_DOMAIN = "Domain";
	public static final String OS_DOMAIN_DESCRIPTION = "Openstack Domain";
	public static final String OS_PROJECT = "Project";
	public static final String OS_PROJECT_DESCRIPTION = "Openstack Project";
	
	public static final String SECRET_KEY = "Secret_Key";
	public static final String SECRET_KEY_DESCRIPTION = "Secret Key for OpenStack";
	public static final String ACCESS_KEY = "Access_Key";
	public static final String ACCESS_KEY_DESCRIPTION = "Access Key for OpenStack";
	public static final String PROXY_PASS = "Proxy_password";
	public static final String PROXY_PASS_DESCRIPTION = "Proxy password";
	public static final String PROXY_USER_NAME = "Proxy_user";
	public static final String PROXY_USER_NAME_DESCRIPTION = "Proxy username";
	public static final String PROXY_PORT = "Proxy_port";
	public static final String PROXY_PORT_DESCRIPTION = "Proxy port";
	public static final String PROXY_HOST = "Proxy_host";
	public static final String PROXY_HOST_DESCRIPTION = "Proxy host";
	

	public static final String OpenStack_VENDOR = "Openstack";
	public static final String OpenStack_VERSION = "1.3.0";
	public static final String OpenStack_FACTORY_ID = OpenStackConstants.OpenStack_VENDOR + " " + OpenStackConstants.OpenStack_VERSION;
	public static final String OpenStack_VIRTUALIZATION_ADAPTER = "OpenStack";
	public static final String OpenStack_CLOUD = "OpenStack Cloud";
	public static final String USERNAME = "Username for OpenStack";
	public static final String USERNAME_DESCRIPTION = "Username for OpenStack";
	public static final String PASSWORD = "Password for OpenStack";
	public static final String PASSWORD_DESCRIPTION = "Password for OpenStack";
	public static final String ENDPOINT = "Endpoint";
	public static final String ENDPOINT_DESCRIPTION = "Endpoint (e.g. http://host:port/V3)";
	public static final String NETWORK_ID = "Network ID";
	public static final String ADDITIONAL_NETWORK_IDS = "Additional Network Ids";
	public static final String OpenStack_NETWORK_ID_PROP_DESCRIPTION = "The network of the instance that should be started";
	public static final String SECURITY_ID = "SecurityGroup";
	public static final String OpenStack_security_ID_PROP_DESCRIPTION = "The security group of the instance that should be started";
	
	public static String INSTANCE_TYPE = "Instance type";
	public static String AVAILABILITY_ZONE = "Availlability zone";
	public static String OpenStack_INSTANCE_TYPE_PROP_DESCRIPTION = "The type of the instance that should be started";
	public static String OpenStack_AVAILABILITY_ZONE_PROP_DESCRIPTION = "The availability zone where the instance should be started";
	public static String SUBNET_ID = "Subnet ID";
	public static String REGION_TYPE_ID = "OpenStackRegion";
	public static String REGION_TYPE_NAME = "OpenStack Region DataCenter";	
	public static String REGION_TYPE_DESCRIPTION = "OpenStack Region DataCenter";
	
	
	// Custom Properties
	public static String CUSTOM_PROPERTY_BASE_IMAGE_REF="base_image_ref";
	/*
	 * linux or windows
	 */
	public static String CUSTOM_PROPERTY_OS_TYPE="os_type";
	public static String CUSTOM_PROPERTY_OS_DISTRO = "os_distro";
	public static String CUSTOM_PROPERTY_OS_VERSION = "os_version";
	

	
	public static enum OpenStackInstanceStates {
		build, pending, running, stopping, stopped, available, terminated, Error
	}
	
	/**
	 * Supported operations.
	 */
	public static Set<VirtDefaultOperation> OpenStack_SUPPORTED_OPERATIONS = null;
	
	/**
	 * Static block initializes the supported operations collection and the
	 * constant type identifier "serviceReference"
	 */
	static {
		OpenStack_SUPPORTED_OPERATIONS = new HashSet<VirtDefaultOperation>();
		OpenStack_SUPPORTED_OPERATIONS.add(VirtDefaultOperation.START);
		OpenStack_SUPPORTED_OPERATIONS.add(VirtDefaultOperation.STOP);
		OpenStack_SUPPORTED_OPERATIONS.add(VirtDefaultOperation.CLONE);
		OpenStack_SUPPORTED_OPERATIONS.add(VirtDefaultOperation.PROVISION);
		OpenStack_SUPPORTED_OPERATIONS.add(VirtDefaultOperation.DESTROY);
		
	}
	
	public static enum OpenStackCloneSteps {
		backup, create, deregister
	}
}
