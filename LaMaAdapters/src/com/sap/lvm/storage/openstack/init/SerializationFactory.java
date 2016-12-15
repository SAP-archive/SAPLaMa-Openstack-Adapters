/*
 * ### SAMPLE CODE ###
 * Copyright (c) 2012 SAP AG
 */
package com.sap.lvm.storage.openstack.init;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sap.lvm.storage.openstack.block.OpenstackBlockStorageCloning.OpenstackBlockCloneVolumeStatus;
import com.sap.lvm.storage.openstack.block.OpenstackBlockStorageCloning.OpenstackBlockCloneVolumesContext;
import com.sap.lvm.storage.openstack.block.OpenstackBlockStorageSnapshot.OpenstackBlockSnapshotVolumesContext;
import com.sap.lvm.storage.openstack.file.OpenstackFileStorageCloning.OpenstackFileCloneVolumeStatus;
import com.sap.lvm.storage.openstack.file.OpenstackFileStorageCloning.OpenstackFileCloneVolumesContext;
import com.sap.lvm.storage.openstack.file.OpenstackFileStorageSnapshot.OpenstackFileSnapshotVolumesContext;
import com.sap.nw.lm.aci.engine.base.exception.EngineException;
import com.sap.tc.vcm.base.util.serialization.factory.ISerializationFactory;
import com.sap.tc.vcm.base.util.serialization.serializable.SerializableClass;
import com.sap.tc.vcm.base.util.serialization.serializable.SerializableField;


@SuppressWarnings("nls")
public class SerializationFactory implements ISerializationFactory {
	
	protected Map<String, Class<?>> classes = new HashMap<String, Class<?>>();
	
	public SerializationFactory() {
		super();
		//OpenStack Block
		addClass(OpenstackBlockSnapshotVolumesContext.class);
		addClass(OpenstackBlockCloneVolumesContext.class);
		addClass(OpenstackBlockCloneVolumeStatus.class);

		//Openstack File    
 		addClass(OpenstackFileSnapshotVolumesContext.class);
		addClass(OpenstackFileCloneVolumesContext.class);
		addClass(OpenstackFileCloneVolumeStatus.class);

	}
	
	protected void addClass(Class<?> clazz) {
		if(!isClassCompliant(clazz)) {
			String message = "Class '" + clazz.getName() + "' has no default constructor!";
			throw new IllegalArgumentException(message);
		}
		
		classes.put(clazz.getName(), clazz);
	}


	@Override
	public Class<?> createClass(String className) throws EngineException {
		return classes.get(className);
	}

	@Override
	public Object createInstance(String className) throws EngineException {
		try {
			return createClass(className).newInstance();
		} catch (Exception e) { //$JL-EXC$
			throw new EngineException("Error occured when instantiating class '" + className + "'", e);
		}
	}

	@Override
	public Collection<Class<?>> getClasses() {
		return classes.values();
	}
	
	private static boolean isClassCompliant(Class<?> cl) {
		if(!isSerializableClassAnnotationPresent(cl))
			return false;
		
		if(!isAnySerializableFieldAnnotationPresent(cl))
			return false;
		
		return hasDefaultConstructor(cl);
	}
	

	private static boolean isSerializableClassAnnotationPresent(Class<?> cl) {
		return cl.isAnnotationPresent(SerializableClass.class);
	}

	private static boolean isAnySerializableFieldAnnotationPresent(Class<?> cl) {
		boolean anySerializableFieldAnnotationPresent=false;
		Field[] fields = cl.getFields();
		if (fields.length==0)
			return true;
		for (Field field : fields) {
			if(field.isAnnotationPresent(SerializableField.class)) {
				anySerializableFieldAnnotationPresent=true;
				break;
			}
		}
		return anySerializableFieldAnnotationPresent;
	}
	
	private static boolean hasDefaultConstructor(Class<?> cl) {
		Constructor<?>[] constructors = cl.getConstructors();
		if(constructors.length==0)
			return true;
		boolean hasDefaultConstructor=false;
		for(Constructor<?> constructor : constructors) {
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			if(parameterTypes.length==0) {
				hasDefaultConstructor=true;
				break;
			}
		}
		return hasDefaultConstructor;
	}

}