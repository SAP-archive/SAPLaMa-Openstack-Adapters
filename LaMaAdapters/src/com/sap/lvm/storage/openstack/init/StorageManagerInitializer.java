/*
 * ### SAMPLE CODE ###
 * Copyright (c) 2012 SAP AG
 */
package com.sap.lvm.storage.openstack.init;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import com.sap.lvm.storage.openstack.block.OpenstackBlockStorageAdapterFactory;
import com.sap.lvm.storage.openstack.file.OpenstackFileStorageAdapterFactory;
import com.sap.tc.vcm.storage.adapter.api.base.registry.IStorageManagerAdapterFactoryRegistry;


/**
 * Servlet implementation class StorageManagerInitializer
 */
public class StorageManagerInitializer extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final OpenstackBlockStorageAdapterFactory BLOCKFACTORY = new OpenstackBlockStorageAdapterFactory();
	private static final OpenstackFileStorageAdapterFactory  FILEFACTORY = new OpenstackFileStorageAdapterFactory();

	private SerializationFactory serializationFactory;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public StorageManagerInitializer() {
		super();
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init() throws ServletException {
		super.init();
		try {
			IStorageManagerAdapterFactoryRegistry factoryRegistry = getStorageFactoryRegistry();
			serializationFactory=new SerializationFactory();

			factoryRegistry.registerAdapterFactory(BLOCKFACTORY);
			factoryRegistry.registerAdapterFactory(FILEFACTORY);
			try {
				factoryRegistry.registerSerializationFactory(serializationFactory);
			} catch (NoSuchMethodError e) {
				throw new ServletException("Minimum required version is SAP LVM 2.1 SP5. Please update your SAP LVM application from the SAP Service Marketplace!", e);
			}

		} catch (NamingException e) {
			throw new ServletException("NamingException: " + e.getMessage(), e);
		}                                                                                           

	}

	/**
	 * @see Servlet#destroy()
	 */
	public void destroy() {
		try {
			IStorageManagerAdapterFactoryRegistry factoryRegistry = getStorageFactoryRegistry();
			factoryRegistry.deregisterAdapterFactory(BLOCKFACTORY.getFactoryId());
			factoryRegistry.deregisterAdapterFactory(FILEFACTORY.getFactoryId());
			factoryRegistry.deregisterSerializationFactory(serializationFactory);
		} catch (NamingException e) {
			//TODO: add some logging here


		}

	}


	@SuppressWarnings("nls")
	public IStorageManagerAdapterFactoryRegistry getStorageFactoryRegistry() throws NamingException {
		InitialContext	initCtx=new InitialContext();

		return (IStorageManagerAdapterFactoryRegistry)initCtx.lookup("/webContainer/applications/sap.com/tc~vcm~storage~adapter~app/LVMStorageAdapter/AdapterFactoryRegistry");
	}

}
