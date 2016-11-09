//==============================================================================
// This software is part of the Open Standard for Unattended Sensors (OSUS)
// reference implementation (OSUS-R).
//
// To the extent possible under law, the author(s) have dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty.
//
// You should have received a copy of the CC0 Public Domain Dedication along
// with this software. If not, see
// <http://creativecommons.org/publicdomain/zero/1.0/>.
//==============================================================================

package mil.dod.th.core.factory;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.capability.BaseCapabilities;

import org.osgi.service.metatype.AttributeDefinition;

/**
 * <p>
 * This interface contains operations that allow clients to determine the capabilities of a specific product type. This 
 * interface allows clients to determine what a factory plug-in shall produce at runtime. (e.g., a User Interface client
 * may need to display a name for the product).
 *
 * <p>
 * For each factory plug-in registered with the core, a service will be registered with OSGi providing this interface 
 * with a service property of {@link #PRODUCT_TYPE_SERVICE_PROPERTY} set to the product type produced by the plug-in.
 * 
 * <p>
 * The {@link #getProductType()} operation returns only one type; therefore, implementations of this interface should
 * only produce one type of product.
 * 
 */
@ProviderType
public interface FactoryDescriptor
{
    /**
     * Property set for each registered factory service for the name of the product that the factory produces. Should
     * equal the value returned by {@link #getProductType()}.
     * 
     * When a factory is registered the appropriate service (e.g., {@link mil.dod.th.core.asset.AssetDirectoryService})
     * will register the factory as this interface to allow other bundles to track when a certain asset or comms is
     * available.
     */
    String PRODUCT_TYPE_SERVICE_PROPERTY = FactoryObject.TH_PROP_PREFIX + ".product.type";
    
    /**
     * Property set for each registered factory service for the type of the factory service. Should be a type like
     * {@link mil.dod.th.core.asset.AssetFactory}.
     * 
     * When a factory is registered the appropriate service (e.g., {@link mil.dod.th.core.asset.AssetDirectoryService})
     * will register the factory as this interface to allow other bundles to track when a certain asset or comms is
     * available.
     */
    String FACTORY_TYPE_SERVICE_PROPERTY = FactoryObject.TH_PROP_PREFIX + ".factory.type";

    /**
     * String appended to the value returned by {@link #getProductType()} to get the PID for the factory.
     */
    String PID_SUFFIX = "Config";

    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/factory/FactoryDescriptor/";
    
    /** Topic used for when an object has been created. */
    String TOPIC_FACTORY_OBJ_CREATED = TOPIC_PREFIX + "FACTORY_OBJ_CREATED";

    /** Topic used for when an object has been deleted. */
    String TOPIC_FACTORY_OBJ_DELETED = TOPIC_PREFIX + "FACTORY_OBJ_DELETED";
    
    /** Topic used for when an object's name has changed. */
    String TOPIC_FACTORY_OBJ_NAME_UPDATED = TOPIC_PREFIX + "FACTORY_OBJ_NAME_UPDATED";
    
    /** Topic used for when a configuration is created and persisted for a factory object.
     * 
     * <ul>
     * <li>{@link #EVENT_PROP_OBJ} - the factory object 
     * <li>{@link #EVENT_PROP_OBJ_NAME} - the name of the factory object for which this event 
     * applies.
     * <li>{@link #EVENT_PROP_OBJ_UUID} - the uuid of the factory object that has had a 
     * configuration created for it.
     * <li>{@link #EVENT_PROP_OBJ_PID} - the pid of the newly created configuration which is associated with
     * the factory object
     * <li>{@link #EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this factory object represents.
     * For instance, this property will be set to Asset, PhysicalLink, LinkLayer, etc depending on the 
     * type of object it represents.
     * </ul>
     * 
     */
    String TOPIC_FACTORY_OBJ_PID_CREATED = TOPIC_PREFIX + "FACTORY_OBJ_PID_CREATED";
    
    /** Topic used for when a configuration is deleted for a factory object.
     * 
     * <ul>
     * <li>{@link #EVENT_PROP_OBJ} - the factory object 
     * <li>{@link #EVENT_PROP_OBJ_NAME} - the name of the factory object for which this event 
     * applies.
     * <li>{@link #EVENT_PROP_OBJ_UUID} - the uuid of the factory object that has had a 
     * configuration created for it.
     * <li>{@link #EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this factory object represents.
     * For instance, this property will be set to Asset, PhysicalLink, LinkLayer, etc depending on the 
     * type of object it represents.
     * </ul>
     * 
     */
    String TOPIC_FACTORY_OBJ_PID_REMOVED = TOPIC_PREFIX + "FACTORY_OBJ_PID_REMOVED";
    
    /** Event property key for the object instance. */
    String EVENT_PROP_OBJ = "obj";

    /** Event property key for the object name ({@link String}). */
    String EVENT_PROP_OBJ_NAME = "obj.name";

    /** Event property key for the object UUID as a string ({@link java.util.UUID#toString()}). */
    String EVENT_PROP_OBJ_UUID = "obj.uuid";

    /** Event property key for the object PID ({@link String}). */
    String EVENT_PROP_OBJ_PID = "obj.pid";

    /** 
     * Event property key for the object's product type as returned by {@link #getProductType()}. 
     */
    String EVENT_PROP_OBJ_TYPE = "obj.type";

    /** Event property key for the base object simple name (e.g., Asset, PhysicalLink) ({@link String}). */ 
    String EVENT_PROP_OBJ_BASE_TYPE = "obj.base-type";
    
    /** 
     * Folder within the bundle that contains any capabilities XML file. Applies to {@link 
     * mil.dod.th.core.controller.TerraHarvestController#getCapabilities()} as well.
     * 
     * @see BaseCapabilities
     */
    String CAPABILITIES_XML_FOLDER_NAME = "capabilities-xml";

    /**
     * Get a human readable description of the product that this factory produces. Convenience method for {@link 
     * BaseCapabilities#getDescription()}.
     * 
     * @return The description of the product.
     */
    String getProductDescription();

    /**
     * Get a human readable name of the product that this factory produces. Convenience method for {@link 
     * BaseCapabilities#getProductName()}.
     * 
     * @return The name of the product.
     */
    String getProductName();

    /**
     * Get a unique product type string for the plug-in in fully qualified class name format (e.g., 
     * com.mycompany.asset.MyAsset).
     * 
     * <p>
     * Each plug-in registered with the core must have a unique product type. The product type is equivalent to the 
     * fully qualified class name of the plug-in's {@link FactoryObjectProxy} implementation (by default). 
     * 
     * <p>
     * The core will use the component name of the {@link org.osgi.service.component.ComponentFactory} that produces 
     * instances of {@link FactoryObjectProxy} for the plug-in. The component name is the fully qualified class name by 
     * default. The component name can be overridden by setting the name attribute of the {@link 
     * aQute.bnd.annotation.component.Component} annotation.
     * 
     * <p>
     * For example, the following proxy definition would result in a product type string of 
     * "com.mycompany.asset.MyAsset". 
     * 
     * <pre>
     * package com.mycompany.asset;
     * 
     * {@literal @}Component(factory = Asset.FACTORY)
     * public class MyAsset implements AssetProxy
     * {
     *     ...
     * </pre>
     * 
     * @return The type of product that is created by a factory.
     */
    String getProductType();
    
    /**
     * Request the product's capabilities.
     * 
     * <p>
     * The capabilities are defined in an XML file located in the {@value #CAPABILITIES_XML_FOLDER_NAME} folder at the 
     * root of the bundle containing the plug-in. Due to the potential of having multiple product types in a single 
     * bundle, a single XML file name is based on the class with which it is associated.  For example, the file 
     * 'example.asset.ExampleAsset.xml' would be the capabilities XML file associated with the class ExampleAsset in 
     * package example.asset.      
     * 
     * @return the product's capabilities
     */
    BaseCapabilities getCapabilities();
    
    /**
     * Retrieve all attribute definitions for this product.
     * 
     * @return 
     *      definitions of available configuration properties
     */
    AttributeDefinition[] getAttributeDefinitions();
    
    /**
     * Retrieve attribute definitions for this product.
     * 
     * @param filter
     *      whether the attribute definitions are required, optional, or all
     * @return 
     *      definitions of available configuration properties
     */
    AttributeDefinition[] getAttributeDefinitions(int filter);
}
