package com.sap.lvm.virtual.openstack;


import java.util.List;
import java.util.Properties;

//import com.sap.nw.lm.aci.engine.base.api.util.AcSystemPropUtil;
import com.sap.lvm.CloudClientException;
import com.sap.lvm.storage.openstack.util.OpenstackConstants;
import com.sap.tc.vcm.infrastructure.api.adapter.request.IJavaEeLog;
import com.sap.tc.vcm.virtualization.adapter.api.IVirtualizationManagerAdapter;
import com.sap.tc.vcm.virtualization.adapter.api.config.IVirtManagerAdapterConfig;
import com.sap.tc.vcm.virtualization.adapter.api.monitoring.IVirtMonitoringService;
import com.sap.tc.vcm.virtualization.adapter.api.operation.IVirtNetworkOperationService;
import com.sap.tc.vcm.virtualization.adapter.api.operation.IVirtOperationService;


public class OpenStackVirtualizationManagerAdapter implements IVirtualizationManagerAdapter {

	private OpenStackVirtOperationService operationService;
	private OpenStackVirtMonitoringService monitoringService;
	private IJavaEeLog logger; 
	
	public OpenStackVirtualizationManagerAdapter(IVirtManagerAdapterConfig config) throws CloudClientException {
		logger = config.getLogger();
	
		Properties connectionProps = config.getVirtManagerAdditionalConfigProps();
		Properties secProps = config.getVirtManagerAdditionalSecConfigProps();
		
		OpenStack_CloudController osClient = new OpenStack_CloudController(config.getUrl(),config.getUser(),config.getPassword(), 
				connectionProps.getProperty(OpenstackConstants.OS_REGION),connectionProps.getProperty(OpenstackConstants.OS_TENANT),
				connectionProps.getProperty(OpenstackConstants.OS_DOMAIN),connectionProps.getProperty(OpenstackConstants.OS_PROJECT),
				connectionProps.getProperty(OpenstackConstants.PROXY_HOST),connectionProps.getProperty(OpenstackConstants.PROXY_PORT),
				connectionProps.getProperty(OpenstackConstants.PROXY_USER_NAME),secProps.getProperty(OpenstackConstants.PROXY_PASS));

		operationService = new OpenStackVirtOperationService(osClient,logger);
		monitoringService = new OpenStackVirtMonitoringService(osClient, connectionProps.getProperty(OpenStackConstants.OS_REGION),config.getLogger());
	}

	@Override
	public IVirtMonitoringService getVirtMonitoringService() {
		return this.monitoringService;
	}

	@Override
	public IVirtOperationService getVirtOperationService() {
		return this.operationService;
	}

	@Override
	public List<LogMessage> validate() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<ExternalURL> getExternalUrls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ExternalCustomTab> getExternalCustomTabs() {
		// TODO Auto-generated method stub
		return null;
	}

	public IVirtNetworkOperationService getVirtNetworkOperationService() {
		return this.operationService;
	}

}
