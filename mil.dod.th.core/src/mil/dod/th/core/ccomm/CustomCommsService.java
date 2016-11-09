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
package mil.dod.th.core.ccomm;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

/**
 * Manages {@link PhysicalLink}s, {@link LinkLayer}s, and {@link TransportLayer}s. The custom comms manager
 * does several things. First it manages exclusive access to PhysicalLinks. Second, it manages multiple access to 
 * LinkLayers. Next, it manages creation and access to {@link TransportLayer}s and connecting them to LinkLayers. It 
 * also provides for a central place to configure all PhysicalLinks.
 *
 * <p>
 * This service uses {@link mil.dod.th.core.ccomm.physical.PhysicalLinkProxy}, {@link 
 * mil.dod.th.core.ccomm.link.LinkLayerProxy} and {@link mil.dod.th.core.ccomm.transport.TransportLayerProxy} 
 * implementations from bundle developers. A proxy is considered available once it has been registered as an OSGi {@link
 * org.osgi.service.component.ComponentFactory} service. All previously created communication layers will be restored by
 * this service when the component factory is registered.
 * 
 * <p>
 * This is an OSGi service provided by the core and may be obtained by get an OSGi service reference or using
 * declarative services.
 */
@ProviderType
public interface CustomCommsService
{
    /**
     * Same as {@link #createPhysicalLink(PhysicalLinkTypeEnum, String, Map)}, except the name is set automatically to 
     * {@link mil.dod.th.core.factory.FactoryDescriptor#getProductType()} (UUID is appended if name already used) and an
     * empty property dictionary is used.
     * 
     * @param plType
     *     the type of {@link PhysicalLink} desired
     * @return 
     *     the created {@link PhysicalLink}
     * @throws CCommException
     *     if the desired {@link PhysicalLink} cannot be created
     * @throws IllegalArgumentException
     *     if the given name is blank or a duplicate of the name of another {@link PhysicalLink}
     */
    PhysicalLink createPhysicalLink(PhysicalLinkTypeEnum plType) throws CCommException, 
            IllegalArgumentException;

    /**
     * Same as {@link #createPhysicalLink(PhysicalLinkTypeEnum, String, Map)}, except an empty property dictionary is
     * used.
     * 
     * @param plType
     *     the type of {@link PhysicalLink} desired
     * @param name
     *     the {@link PhysicalLink} name
     * @return
     *     the created {@link PhysicalLink}
     * @throws CCommException
     *     if the desired {@link PhysicalLink} cannot be created
     * @throws IllegalArgumentException
     *     if the given name is blank or a duplicate of the name of another {@link PhysicalLink}.
     */
    PhysicalLink createPhysicalLink(PhysicalLinkTypeEnum plType, String name) throws CCommException, 
            IllegalArgumentException;
    
    /**
     * Returns a new {@link PhysicalLink} with the specified type, name, and properties. A new instance of the given 
     * type is created and added to the service.
     * 
     * @param plType
     *     the type of {@link PhysicalLink} desired
     * @param name
     *     the {@link PhysicalLink} name
     * @param props
     *     the properties intended for the link if the defaults are not desired
     * @return
     *     the created {@link PhysicalLink}
     * @throws CCommException
     *     if the desired {@link PhysicalLink} cannot be created
     * @throws IllegalArgumentException
     *     if the given name is blank or a duplicate of the name of another {@link PhysicalLink}.
     */
    PhysicalLink createPhysicalLink(PhysicalLinkTypeEnum plType, String name, Map<String, Object> props) 
            throws CCommException, IllegalArgumentException;
    
    /**
     * Same as {@link #tryCreatePhysicalLink(PhysicalLinkTypeEnum, String, Map)}, except no properties are specified.
     * 
     * @param plType
     *     the type of {@link PhysicalLink} desired
     * @param name
     *     the {@link PhysicalLink} name
     * @return
     *     UUID of the new or existing {@link PhysicalLink}
     * @throws CCommException
     *     if the desired {@link PhysicalLink} cannot be created
     * @throws IllegalArgumentException
     *     if the given name is blank or exists for a different type of {@link PhysicalLink}
     */
    UUID tryCreatePhysicalLink(PhysicalLinkTypeEnum plType, String name) throws CCommException, 
            IllegalArgumentException;

    /**
     * Creates a {@link PhysicalLink} of the desired type, name and properties. If the desired {@link PhysicalLink} 
     * already exists, the UUID of the existing link will be returned and the properties provided will still be applied.
     * The link can be obtained by calling {@link #requestPhysicalLink(UUID)}.
     * 
     * @param plType
     *     the type of {@link PhysicalLink} desired
     * @param name
     *     the {@link PhysicalLink} name
     * @param properties
     *      properties to use for the new {@link PhysicalLink}
     * @return
     *     UUID of the new or existing {@link PhysicalLink}
     * @throws CCommException
     *     if the desired {@link PhysicalLink} cannot be created
     * @throws IllegalArgumentException
     *     if the given name is blank or exists for a different type of {@link PhysicalLink}
     */
    UUID tryCreatePhysicalLink(PhysicalLinkTypeEnum plType, String name, Map<String, Object> properties)
            throws CCommException, IllegalArgumentException;
    
    /**
     * Delete the {@link PhysicalLink} with the given name.
     * 
     * @param phyLinkName
     *     name of the {@link PhysicalLink} to be removed
     * @throws CCommException
     *     if an error occurs trying to remove the {@link PhysicalLink}
     * @throws IllegalArgumentException
     *     if the {@link PhysicalLink} is null or {@link PhysicalLink#getUuid()} is null
     * @throws IllegalStateException
     *     if the {@link PhysicalLink} is currently in use or open, and therefore cannot be removed
     */
    void deletePhysicalLink(final String phyLinkName) throws CCommException, IllegalArgumentException, 
            IllegalStateException;

    /**
     * Same as {@link #createLinkLayer(String, String, String)}, except the name is set automatically to {@link 
     * mil.dod.th.core.factory.FactoryDescriptor#getProductType()} (UUID is appended if name already used).
     * 
     * @param llType
     *     the class type of the {@link mil.dod.th.core.ccomm.link.LinkLayerProxy} desired as returned by {@link 
     *     LinkLayerFactory#getProductType()}
     * @param physicalLinkName
     *     name of the {@link PhysicalLink} to use or null if no {@link PhysicalLink} required
     * @return 
     *     the created {@link LinkLayer} or an existing one
     * @throws CCommException
     *      if the desired {@link LinkLayer} cannot be created
     * @throws IllegalArgumentException
     *      if the {@link LinkLayer} requires a {@link PhysicalLink}, but a {@link PhysicalLink} name was not provided
     */
    LinkLayer createLinkLayer(String llType, String physicalLinkName) throws CCommException, IllegalArgumentException;
    
    /**
     * Same as {@link #createLinkLayer(String, String, Map)}, except only the 
     * {@link mil.dod.th.core.ccomm.link.LinkLayerAttributes#CONFIG_PROP_PHYSICAL_LINK_NAME} property is specified.
     * 
     * @param llType
     *      the class type of the {@link mil.dod.th.core.ccomm.link.LinkLayerProxy} desired as returned by {@link 
     *      LinkLayerFactory#getProductType()}
     * @param name
     *      the name for the {@link LinkLayer}, if a new layer is created
     * @param physicalLinkName
     *      name of the {@link PhysicalLink} to use or null of no {@link PhysicalLink} required
     * @return 
     *      the created {@link LinkLayer} or an existing one
     * @throws CCommException
     *      if the desired {@link LinkLayer} cannot be created or is pending to be created
     * @throws IllegalArgumentException
     *      if the {@link LinkLayer} requires a {@link PhysicalLink}, but a {@link PhysicalLink} name was not provided
     */
    LinkLayer createLinkLayer(String llType, String name, String physicalLinkName) throws CCommException, 
            IllegalArgumentException;
    
    /**
     * Returns a {@link LinkLayer} with the specified name. If such a {@link LinkLayer} already exists (same type and 
     * {@link PhysicalLink}) it is returned, otherwise a new one is created. If the {@link LinkLayer} does not use a 
     * {@link PhysicalLink}, a new link layer will always be returned. If the name of the {@link LinkLayer} is a 
     * duplicate of the name of another, an exception is thrown and the {@link LinkLayer} will not be created. If a 
     * previously existing {@link LinkLayer} is returned, the name parameter will be ignored, but properties will be 
     * applied.
     * 
     * @param llType
     *      the class type of the {@link mil.dod.th.core.ccomm.link.LinkLayerProxy} desired as returned by {@link 
     *      LinkLayerFactory#getProductType()}
     * @param name
     *      the name for the {@link LinkLayer}, if a new layer is created
     * @param properties
     *      properties to use for the new {@link LinkLayer}. 
     *      {@link mil.dod.th.core.ccomm.link.LinkLayerAttributes#CONFIG_PROP_PHYSICAL_LINK_NAME} should be specified
     *      if the {@link LinkLayer} is to be associated with a {@link PhysicalLink}
     * @return 
     *     the created {@link LinkLayer} or an existing one
     * @throws IllegalArgumentException
     *     if the property map contains an invalid value for 
     *     {@link mil.dod.th.core.ccomm.link.LinkLayerAttributes#CONFIG_PROP_PHYSICAL_LINK_NAME} or the 
     *     {@link LinkLayer} requires a {@link PhysicalLink}, but a {@link PhysicalLink} name was not provided
     * @throws CCommException
     *     if the desired {@link LinkLayer} cannot be created or is pending to be created
     */
    LinkLayer createLinkLayer(String llType, String name, Map<String, Object> properties) 
            throws IllegalArgumentException, CCommException;

    /**
     * Same as {@link #createTransportLayer(String, String, Map)}, except the property
     * {@link mil.dod.th.core.ccomm.transport.TransportLayerAttributes#CONFIG_PROP_LINK_LAYER_NAME} is specified.
     * 
     * @param tlType 
     *     the class type of the {@link mil.dod.th.core.ccomm.transport.TransportLayerProxy} desired as returned by 
     *     {@link TransportLayerFactory#getProductType()}
     * @param name
     *     name of the {@link TransportLayer} to create
     * @param linklayerName
     *     name of the {@link LinkLayer} that should be used or null if no {@link LinkLayer} is to be used
     * @return 
     *     the created {@link TransportLayer} or an existing one
     * @throws CCommException
     *     if the desired {@link TransportLayer} cannot be created or is pending to be created
     */
    TransportLayer createTransportLayer(String tlType, String name, String linklayerName) throws CCommException;
    
    /**
     * Returns a {@link TransportLayer} with the specified {@link LinkLayer}. If such a {@link TransportLayer} already 
     * exists (same type and {@link LinkLayer}) it is returned, otherwise a new one is created. If the name is a 
     * duplicate of the name of another {@link TransportLayer} an exception is thrown and the {@link TransportLayer} 
     * will not be created. If a previously existing {@link TransportLayer} is returned, the name parameter will be 
     * ignored, but properties will be applied.
     * 
     * @param tlType 
     *     the class type of the {@link mil.dod.th.core.ccomm.transport.TransportLayerProxy} desired as returned by 
     *     {@link TransportLayerFactory#getProductType()}
     * @param name
     *     name of the {@link TransportLayer} to create
     * @param properties
     *      properties to use for the new {@link TransportLayer}. {@link 
     *      mil.dod.th.core.ccomm.transport.TransportLayerAttributes#CONFIG_PROP_LINK_LAYER_NAME} 
     *      should be specified if the {@link TransportLayer} works with a {@link LinkLayer}.
     * @return 
     *     the created {@link TransportLayer} or an existing one
     * @throws IllegalArgumentException
     *     if the property dictionary does not contain 
     *     {@link mil.dod.th.core.ccomm.transport.TransportLayerAttributes#CONFIG_PROP_LINK_LAYER_NAME}
     * @throws CCommException
     *     if the desired {@link TransportLayer} cannot be created or is pending to be created
     */
    TransportLayer createTransportLayer(String tlType, String name, Map<String, Object> properties) 
            throws IllegalArgumentException, CCommException;

    /**
     * Get a list of all the names of the {@link PhysicalLink}s.
     * 
     * @return 
     *     a list of all the names of {@link PhysicalLink}s held by the service
     */
    List<String> getPhysicalLinkNames();

    /**
     * Get a LinkLayer, based off name search.
     * 
     * @param name
     *      the name of the {@link LinkLayer}
     * @return The {@link LinkLayer} that corresponds to 
     *      the requested name
     * @throws IllegalArgumentException 
     *      {@link LinkLayer} not in list
     */
    LinkLayer getLinkLayer(String name) throws IllegalArgumentException;

    /**
     * Get a {@link LinkLayer}, based off name search.
     * 
     * @param name
     *      the name of the {@link LinkLayer}
     * @return 
     *      the {@link LinkLayer} that corresponds with this name or null if not found
     */
    LinkLayer findLinkLayer(String name);
    
    /**
     * Get a list of all {@link LinkLayer}s. The list returned is unmodifiable, any needed changes to a {@link 
     * LinkLayer} should be done through other {@link CustomCommsService} calls.
     * 
     * @return
     *     a list of all {@link LinkLayer}s held by the service
     */
    List<LinkLayer> getLinkLayers();
    
    /**
     * Get a {@link TransportLayer} based off a name search.
     * 
     * @param name
     *     the name of the {@link TransportLayer}
     * @return 
     *     the {@link TransportLayer} that corresponds with this name.
     * @throws IllegalArgumentException 
     *     {@link TransportLayer} not in list
     */
    TransportLayer getTransportLayer(String name) throws IllegalArgumentException;
    
    /**
     * Get a {@link TransportLayer} based off a name search.
     * 
     * @param name
     *     the name of the {@link TransportLayer}
     * @return 
     *     the {@link TransportLayer} that corresponds with this name or null if not found
     */
    TransportLayer findTransportLayer(String name);

    /**
     * Return the list of TransportLayers. The list returned is unmodifiable, any needed changes to a {@link 
     * TransportLayer} should be done through other {@link CustomCommsService} calls.
     * 
     * @return 
     *     the list of all the {@link TransportLayer}s held by this service
     */
    List<TransportLayer> getTransportLayers();
    
    /**
     * Prints out detailed debug info to the given {@link PrintStream}.
     * 
     * @param printstream 
     *     the output stream to print debug info
     * 
     */
    void printDeep(PrintStream printstream);

    /**
     * Requests exclusive access to a PhysicalLink.
     * 
     * @param name 
     *     the name of the PhysicalLink being requested
     * @return
     *     the PhysicalLink to use.
     * @throws IllegalStateException
     *     if requested {@link PhysicalLink} is already in use
     * @throws IllegalArgumentException
     *     if {@link PhysicalLink} cannot be found
     */
    PhysicalLink requestPhysicalLink(String name) throws IllegalStateException, IllegalArgumentException;
    
    /**
     * Requests exclusive access to a {@link PhysicalLink}.
     * 
     * @param uuid
     *     the UUID of the {@link PhysicalLink} being requested
     * @return
     *     the {@link PhysicalLink} to use
     * @throws IllegalStateException
     *     if requested {@link PhysicalLink} is already in use
     * @throws IllegalArgumentException
     *     if {@link PhysicalLink} cannot be found
     */
    PhysicalLink requestPhysicalLink(UUID uuid) throws IllegalStateException, IllegalArgumentException;
    
    /**
     * Releases exclusive access to a {@link PhysicalLink}. After this point, the calling module should no longer
     * reference this {@link PhysicalLink}.  {@link PhysicalLink} cannot be open.
     * 
     * @param name
     *      the name of the {@link PhysicalLink} to be released
     * @throws IllegalStateException
     *      if requested {@link PhysicalLink} is not in use, or is still open
     * @throws IllegalArgumentException
     *      if {@link PhysicalLink} cannot be found
     */
    void releasePhysicalLink(String name) throws IllegalStateException, IllegalArgumentException;

    /**
     * Check if the {@link PhysicalLink} is already in use.
     * 
     * @param name
     *      the name of the {@link PhysicalLink} to check
     * @return
     *      true if the {@link PhysicalLink} has already been requested
     * @throws IllegalArgumentException
     *      if the {@link PhysicalLink} cannot be found
     */
    boolean isPhysicalLinkInUse(String name) throws IllegalArgumentException;
    
    /**
     * Check if the {@link PhysicalLink} is open.
     * @see PhysicalLink#isOpen {@link PhysicalLink}'s is open method.
     * 
     * @param name
     *      the name of the {@link PhysicalLink} to check
     * @return
     *      true if the {@link PhysicalLink} is open
     * @throws IllegalArgumentException
     *      if the {@link PhysicalLink} cannot be found
     */
    boolean isPhysicalLinkOpen(String name) throws IllegalArgumentException;
    /**
     * Check if a {@link LinkLayer} exists with the given name.
     * 
     * @param name
     *      name of the {@link LinkLayer}
     * @return
     *      true if created, false if not
     */
    boolean isLinkLayerCreated(String name);
    
    /**
     * Check if a {@link TransportLayer} exists with the given name.
     * 
     * @param name
     *      name of the {@link TransportLayer}
     * @return
     *      true if created, false if not
     */
    boolean isTransportLayerCreated(String name);
    
    /**
     * Get all UUIDs of {@link PhysicalLink}s known to this service.
     * 
     * @return
     *      list of UUIDs of all the {@link PhysicalLink}s
     */
    List<UUID> getPhysicalLinkUuids();

    /** Get the name of the {@link PhysicalLink} with the given UUID.
     * 
     * @param uuid
     *      UUID of the {@link PhysicalLink} to get the name for
     * @return
     *      name of the {@link PhysicalLink}
     * @throws IllegalArgumentException
     *      if a {@link PhysicalLink} cannot be found with the given UUID
     */
    String getPhysicalLinkName(UUID uuid) throws IllegalArgumentException;
    
    /**
     * Get the PID of a {@link PhysicalLink} given its UUID.
     * 
     * @param uuid
     *      UUID of the {@link PhysicalLink}
     * @return
     *      PID of the {@link PhysicalLink}
     * @throws IllegalArgumentException
     *      if a {@link PhysicalLink} cannot be found with the given UUID
     *      
     * @see PhysicalLink#getPid()
     */
    String getPhysicalLinkPid(UUID uuid) throws IllegalArgumentException;

    /**
     * Get the factory for the {@link PhysicalLink} given its UUID.
     * 
     * @param uuid
     *     UUID of the {@link PhysicalLink}
     * @return
     *     factory that created the {@link PhysicalLink}
     * @throws IllegalArgumentException
     *     if a {@link PhysicalLink} cannot be found with the given UUID    
     */
    PhysicalLinkFactory getPhysicalLinkFactory(UUID uuid) throws IllegalArgumentException;

    /**
     * Set the persisted name of the {@link PhysicalLink} with the given UUID.
     * 
     * @param uuid
     *     the UUID of the {@link PhysicalLink} being named
     * @param name
     *     the new name of the link
     * @throws IllegalArgumentException
     *     if the name is null, blank, or a duplicate name of another {@link PhysicalLink} in the registry
     * @throws CCommException
     *     if an error occurs saving the {@link PhysicalLink} name
     */
    void setPhysicalLinkName(UUID uuid, String name) throws IllegalArgumentException, 
            CCommException;
    
    /**
     * Get a set of factories that describe {@link LinkLayer} plug-in instances. This method is useful to clients that 
     * wish to discover what {@link LinkLayer}s this directory can produce.  As {@link LinkLayer} factories come and go
     * via services, this list may not accurately reflect the current state of the directory.
     * 
     * @return 
     *     set of all {@link LinkLayer} factory descriptors
     */
    Set<LinkLayerFactory> getLinkLayerFactories();
    
    /**
     * Get a set of factories that describe {@link PhysicalLink} plug-in instances. This method is useful to clients 
     * that wish to discover what {@link PhysicalLink}s this directory can produce.  As {@link PhysicalLink} factories 
     * come and go via services, this list may not accurately reflect the current state of the directory.
     * 
     * @return 
     *     set of all {@link PhysicalLink} factory descriptors
     */
    Set<PhysicalLinkFactory> getPhysicalLinkFactories();
    
    /**
     * Get a set of factories that describe {@link TransportLayer} plug-in instances. This method is useful to clients 
     * that wish to discover what {@link TransportLayer}s this directory can produce.  As {@link TransportLayer} 
     * factories come and go via services, this list may not accurately reflect the current state of the directory.
     * 
     * @return 
     *     set of all {@link TransportLayer} factory descriptors
     */
    Set<TransportLayerFactory> getTransportLayerFactories();
}
