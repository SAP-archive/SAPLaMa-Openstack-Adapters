package com.sap.lvm.virtual.openstack;

import java.util.StringTokenizer;

import com.sap.tc.vcm.base.util.serialization.serializable.SerializableClass;
import com.sap.tc.vcm.base.util.serialization.serializable.SerializableField;
import com.sap.tc.vcm.virtualization.adapter.api.base.IVirtOpContext;

@SerializableClass
public class OpenStackVirtOpContext implements IVirtOpContext{

	
	@SerializableField
	private String entityId;
	@SerializableField
	private String operationType; 
	@SerializableField
	private String hostName;
	@SerializableField
	private String ip;
	@SerializableField
	private String entityType;
	@SerializableField
	private String entityName;
	@SerializableField
	private String instanceType;
	@SerializableField
	private String networkIds;
	@SerializableField
	private String securityGroup;
	
	
	
	public OpenStackVirtOpContext() {
		super();
	}
	
	public OpenStackVirtOpContext(String operationType, String entityId){
		super();
		this.operationType = operationType;
		this.entityId = entityId;
		
	}

	public OpenStackVirtOpContext(String operationType, String entityId, String entityType, String entityName, String hostName, String ip) {
		super();
		this.operationType = operationType;
		this.entityId = entityId;
		this.hostName = hostName;
		this.ip = ip;
		this.entityName = entityName;
		this.entityType = entityType;
	}

	public OpenStackVirtOpContext(String operationType, String entityId, String entityType, String entityName, String hostName, String ip, String instanceType, String networkIds, String securityGroup) {
		super();
		this.operationType = operationType;
		this.entityId = entityId;
		this.hostName = hostName;
		this.ip = ip;
		this.entityName = entityName;
		this.entityType = entityType;
		this.instanceType = instanceType;
		this.networkIds = networkIds;
		this.securityGroup = securityGroup;
		
	}
	

	public String getOperationType() {
		
		return operationType;
	}

	public String getEntityId() {
		return entityId;
	}
	
	

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public String serialize() {
		return this.operationType + "@" + this.entityId + "@" + this.entityType + "@" + this.entityName + "@" + this.hostName + "@" + this.ip + "@" + this.instanceType + "@" + this.networkIds + "@" + this.securityGroup;
	}
	
	public static OpenStackVirtOpContext deserialize(String serialized){
		
	
		String operationType, entityId, entityType, entityName, hostName = null, ip = null, networkIds = null, sourceInstanceId = null, securityGroup = null;
		//List<String> networkList = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(serialized, "@");
		operationType = tokenizer.nextToken();
		entityId = tokenizer.nextToken();
		entityType = tokenizer.nextToken();
		entityName = tokenizer.nextToken();
		if (tokenizer.hasMoreElements()) {
			hostName = tokenizer.nextToken();
			ip = tokenizer.nextToken();
		}
		if (tokenizer.hasMoreElements()) {
			sourceInstanceId = tokenizer.nextToken();
			networkIds = tokenizer.nextToken();
			securityGroup = tokenizer.nextToken();
	}
		return new OpenStackVirtOpContext(operationType, entityId, entityType, entityName, hostName, ip, sourceInstanceId, networkIds, securityGroup);
	}
	
	/**
	 * @see java.lang.Object#equals(Object)()
	 */
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof OpenStackVirtOpContext) {
			OpenStackVirtOpContext ref = (OpenStackVirtOpContext) obj;
			return ref.getEntityId().equals(this.getEntityId());
		} else {
			return false;
		}
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.getEntityId().hashCode();
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getIp() {
		return ip;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getEntityType() {
		return entityType;
	}
	public void setInstanceType(String instanceType) {
		this.instanceType = instanceType;
	}

	public String getInstanceType() {
		return instanceType;
	}
	
	public void setNetworkIds(String networkIds) {
		this.networkIds = networkIds;
	}

	public String getNetworkIds() {
		return networkIds;
	}
	
	public void setSecurityGroup(String securityGroup) {
		this.securityGroup = securityGroup;
	}

	public String getSecurityGroup() {
		return securityGroup;
	}
}
