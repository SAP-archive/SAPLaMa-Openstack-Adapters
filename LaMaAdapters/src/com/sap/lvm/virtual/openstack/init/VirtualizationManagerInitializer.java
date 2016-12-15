/*
 * ### SAMPLE CODE ###
 * Copyright (c) 2012 SAP AG
 */
package com.sap.lvm.virtual.openstack.init;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.sap.lvm.virtual.openstack.OpenStackVirtualizationManagerAdapterFactory;
import com.sap.tc.vcm.virtualization.adapter.api.base.IVirtManagerAdapterFactoryRegistry;


/**
 * Servlet implementation class StorageManagerInitializer
 */
public class VirtualizationManagerInitializer extends HttpServlet {
	
	private static final long serialVersionUID = 1L;
    
	

	private static final OpenStackVirtualizationManagerAdapterFactory FACTORY = new OpenStackVirtualizationManagerAdapterFactory();

	private VirtualizationSerializationFactory virtualizationSerializationFactory;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public VirtualizationManagerInitializer() {
        super();
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	@SuppressWarnings("nls")
	public void init() throws ServletException {
		super.init();
		try {
			IVirtManagerAdapterFactoryRegistry factoryRegistry = getVirtFactoryRegistry();
			factoryRegistry.registerAdapterFactory(FACTORY);
			virtualizationSerializationFactory=new VirtualizationSerializationFactory();
			factoryRegistry.registerSerializationFactory(new VirtualizationSerializationFactory());
		} catch (NamingException e) {
			throw new ServletException("NamingException: " + e.getMessage(), e);
		}
		
	}

	/**
	 * @see Servlet#destroy()
	 */
	public void destroy() {
		try {
			IVirtManagerAdapterFactoryRegistry factoryRegistry = getVirtFactoryRegistry();
			factoryRegistry.deregisterAdapterFactory(FACTORY.getFactoryId());
			factoryRegistry.deregisterSerializationFactory(virtualizationSerializationFactory);
		} catch (NamingException e) {
		//TODO: add logging here
		}
		
	}
	
	
	@SuppressWarnings("nls")
	public IVirtManagerAdapterFactoryRegistry getVirtFactoryRegistry() throws NamingException {
		InitialContext	initCtx=new InitialContext();
		
		return (IVirtManagerAdapterFactoryRegistry)initCtx.lookup("/webContainer/applications/sap.com/tc~vcm~virtual~adapter~app/LVMVirtualizationAdapter/AdapterFactoryRegistry");
	}
	
}
