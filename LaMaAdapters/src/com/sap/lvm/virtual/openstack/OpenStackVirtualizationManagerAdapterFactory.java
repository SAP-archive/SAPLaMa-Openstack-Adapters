package com.sap.lvm.virtual.openstack;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sap.lvm.CloudClientException;
import com.sap.lvm.storage.openstack.util.OpenstackConstants;
import com.sap.nw.lm.aci.engine.api.base.property.IPropertyType.ValueType;
import com.sap.nw.lm.aci.engine.base.api.i18n.TranslatableString;
import com.sap.tc.vcm.infrastructure.api.adapter.IInfrastructAdapter.ExternalCustomTab;
import com.sap.tc.vcm.infrastructure.api.adapter.IInfrastructAdapter.ExternalURL;
import com.sap.tc.vcm.infrastructure.api.adapter.InfrastructAdapterException;
import com.sap.tc.vcm.infrastructure.api.adapter.config.ConfigPropMetaData;
import com.sap.tc.vcm.infrastructure.api.adapter.config.IInfrastructAdapterConfigMetaData.ConfigRequirement;
import com.sap.tc.vcm.virtualization.adapter.api.IVirtualizationManagerAdapter;
import com.sap.tc.vcm.virtualization.adapter.api.IVirtualizationManagerAdapterFactory;
import com.sap.tc.vcm.virtualization.adapter.api.config.IVirtManagerAdapterConfig;
import com.sap.tc.vcm.virtualization.adapter.api.config.VirtManagerAdapterConfigMetaData;


public class OpenStackVirtualizationManagerAdapterFactory implements IVirtualizationManagerAdapterFactory {
static public String DOMAIN_INFRASTRUCTURE="InfrastructureTexts";
	@Override
	public IVirtualizationManagerAdapter createAdapterInstance(IVirtManagerAdapterConfig config) throws InfrastructAdapterException {
		try {
			return new OpenStackVirtualizationManagerAdapter(config);
		} catch (CloudClientException e) {
	
			throw new InfrastructAdapterException ("failed to create openstack client",e);
		}
	}

	@Override
	public VirtManagerAdapterConfigMetaData getAdapterConfigMetaData() {
		VirtManagerAdapterConfigMetaData adapterConfigData = new VirtManagerAdapterConfigMetaData();
	
		List<ConfigPropMetaData> configMetaDatas = new ArrayList<ConfigPropMetaData>();
		
		TranslatableString key1 = new TranslatableString(OpenstackConstants.OS_TENANT, DOMAIN_INFRASTRUCTURE, "OS_TENANT");
		TranslatableString description1 = new TranslatableString(OpenstackConstants.OS_TENANT_DESCRIPTION, DOMAIN_INFRASTRUCTURE, "OS_TENANT_DESCRIPTION");

		TranslatableString key2 = new TranslatableString(OpenstackConstants.OS_REGION, DOMAIN_INFRASTRUCTURE, "OS_REGION");
		TranslatableString description2 = new TranslatableString(OpenstackConstants.OS_REGION_DESCRIPTION, DOMAIN_INFRASTRUCTURE, "OS_REGION_DESCRIPTION");
		
		TranslatableString key3 = new TranslatableString(OpenstackConstants.OS_DOMAIN, DOMAIN_INFRASTRUCTURE, "OS_DOMAIN");
		TranslatableString description3 = new TranslatableString(OpenstackConstants.OS_DOMAIN_DESCRIPTION, DOMAIN_INFRASTRUCTURE, "OS_DOMAIN_DESCRIPTION");
		
		TranslatableString key4 = new TranslatableString(OpenstackConstants.OS_PROJECT, DOMAIN_INFRASTRUCTURE, "OS_PROJECT");
		TranslatableString description4 = new TranslatableString(OpenstackConstants.OS_PROJECT_DESCRIPTION, DOMAIN_INFRASTRUCTURE, "OS_PROJECT_DESCRIPTION");
		
		TranslatableString key5 = new TranslatableString(OpenstackConstants.PROXY_HOST, DOMAIN_INFRASTRUCTURE, "PROXY_HOST");
		TranslatableString description5 = new TranslatableString(OpenstackConstants.PROXY_HOST_DESCRIPTION, DOMAIN_INFRASTRUCTURE, "PROXY_HOST_DESCRIPTION");
		
		TranslatableString key6 = new TranslatableString(OpenstackConstants.PROXY_PORT, DOMAIN_INFRASTRUCTURE, "PROXY_PORT");
		TranslatableString description6 = new TranslatableString(OpenstackConstants.PROXY_PORT_DESCRIPTION, DOMAIN_INFRASTRUCTURE, "PROXY_PORT_DESCRIPTION");
		
		TranslatableString key7 = new TranslatableString(OpenstackConstants.PROXY_USER_NAME, DOMAIN_INFRASTRUCTURE, "PROXY_USER_NAME");
		TranslatableString description7 = new TranslatableString(OpenstackConstants.PROXY_USER_NAME_DESCRIPTION, DOMAIN_INFRASTRUCTURE, "PROXY_USER_NAME_DESCRIPTION");
		
		TranslatableString key8 = new TranslatableString(OpenstackConstants.PROXY_PASS, DOMAIN_INFRASTRUCTURE, "PROXY_PASS");
		TranslatableString description8 = new TranslatableString(OpenstackConstants.PROXY_PASS_DESCRIPTION, DOMAIN_INFRASTRUCTURE, "PROXY_PASS_DESCRIPTION");
		
		//tenant
		ConfigPropMetaData configMetaData1 = new ConfigPropMetaData(key1, ValueType.STRING, description1, true);
		configMetaDatas.add(configMetaData1);
		
		//REGION
		ConfigPropMetaData configMetaData2 = new ConfigPropMetaData(key2, ValueType.STRING, description2, true);
		configMetaDatas.add(configMetaData2);

		//DOMAIN
		ConfigPropMetaData configMetaData3 = new ConfigPropMetaData(key3, ValueType.STRING, description3, true);
		configMetaDatas.add(configMetaData3);

		//PROJECT
		ConfigPropMetaData configMetaData4 = new ConfigPropMetaData(key4, ValueType.STRING, description4, true);
		configMetaDatas.add(configMetaData4);

		//proxy host
		ConfigPropMetaData configMetaData5 = new ConfigPropMetaData(key5, ValueType.STRING, description5, false, false, true);
		configMetaDatas.add(configMetaData5);
		
		//proxy port
		ConfigPropMetaData configMetaData6 = new ConfigPropMetaData(key6, ValueType.STRING, description6);
		configMetaDatas.add(configMetaData6);
		
		//proxy user
		ConfigPropMetaData configMetaData7 = new ConfigPropMetaData(key7, ValueType.STRING, description7);
		configMetaDatas.add(configMetaData7);
		
		//proxy pass
		ConfigPropMetaData configMetaData8 = new ConfigPropMetaData(key8, ValueType.STRING, description8, false, true);
		configMetaDatas.add(configMetaData8);

		adapterConfigData.setUrlRequired(ConfigRequirement.Required);
		adapterConfigData.setUserRequired(ConfigRequirement.Required);
		adapterConfigData.setPasswordRequired(ConfigRequirement.Required);
		adapterConfigData.setVirtManagerConfigPropMetaData(configMetaDatas);
		
		return adapterConfigData;
	}

	@Override
	public String getDescription() {
		return OpenStackConstants.OpenStack_CLOUD;
	}

	@Override
	public String getFactoryId() {
		return OpenStackConstants.OpenStack_FACTORY_ID;
	}

	@Override
	public String getProduct() {
		return OpenStackConstants.OpenStack_VIRTUALIZATION_ADAPTER;
	}

	@Override
	public String getVendor() {
		return OpenStackConstants.OpenStack_VENDOR;
	}

	@Override
	public String getVersion() {
		return OpenStackConstants.OpenStack_VERSION;
	}

	@Override
	public String testConnection(IVirtManagerAdapterConfig config) throws InfrastructAdapterException {
		Properties connectionProps = config.getVirtManagerAdditionalConfigProps();
		Properties secProps = config.getVirtManagerAdditionalSecConfigProps();
		
		try {
			OpenStack_CloudController controller = new OpenStack_CloudController(config.getUrl(),config.getUser(),config.getPassword(), 
					connectionProps.getProperty(OpenstackConstants.OS_REGION),connectionProps.getProperty(OpenstackConstants.OS_TENANT),
					connectionProps.getProperty(OpenstackConstants.OS_DOMAIN),connectionProps.getProperty(OpenstackConstants.OS_PROJECT),
					connectionProps.getProperty(OpenstackConstants.PROXY_HOST),connectionProps.getProperty(OpenstackConstants.PROXY_PORT),
					connectionProps.getProperty(OpenstackConstants.PROXY_USER_NAME),secProps.getProperty(OpenstackConstants.PROXY_PASS));

			controller.getFlavors();
		} catch (CloudClientException e) {
			throw new InfrastructAdapterException("Failed to connect to OpenStack", e);
		}
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
	
}
