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

package mil.dod.th.core.ccomm.link;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.factory.FactoryObject;

/**
 * Provides the interface for transmitting/receiving a link frame between addressed devices, adding retry and
 * transmission unit size capability. Allows a {@link PhysicalLink} to be used for the physical connection to the 
 * device if necessary.
 * 
 * <p>
 * All functionality of a link layer needed by the consumer should be available through this interface. It should never 
 * be necessary to use the {@link LinkLayerProxy} interface implemented by the plug-in. For example, if RadioLinkLayer 
 * implements {@link LinkLayerProxy}, consumers should not directly access RadioLinkLayer methods, but instead access 
 * this interface.
 * 
 * <p>
 * Instances of a LinkLayer are managed (created, tracked, deleted) by the core. This interface should never be 
 * implemented by a plug-in. Instead, a plug-in implements {@link LinkLayerProxy} to define custom behavior that is 
 * invoked when consumers use this interface. To interact with a link layer, use the {@link 
 * mil.dod.th.core.ccomm.CustomCommsService}.
 */
@ProviderType
public interface LinkLayer extends FactoryObject
{
    /** 
     * Each {@link LinkLayerProxy} implementation must provide a {@link org.osgi.service.component.ComponentFactory} 
     * with the factory attribute set to this constant.
     * 
     * <p>
     * For example:
     * 
     * <pre>
     * {@literal @}Component(factory = LinkLayer.FACTORY)
     * public class MyLinkLayer implements LinkLayerProxy
     * {
     *     ...
     * </pre>
     */
    String FACTORY = "mil.dod.th.core.ccomm.link.LinkLayer";
    
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/ccomm/link/LinkLayer/";
    
    /** 
     * Topic used when the link layer is activated. 
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ} - the {@link LinkLayer} object 
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the layer as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * LinkLayer object as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the layer as a String,
     * may not be included if the layer has no PID
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * Link Layer represents (e.g., LinkLayer)
     * <li> {@link LinkLayer#EVENT_PROP_LINK_STATUS} - the current {@link LinkStatus} status 
     * </ul>
     */
    String TOPIC_ACTIVATED = TOPIC_PREFIX + "ACTIVATED";

    /** 
     * Topic used when the link layer is deactivated. 
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ} - the {@link LinkLayer} object 
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the layer as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * LinkLayer object as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the layer as a String,
     * may not be included if the layer has no PID
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * Link Layer represents (e.g., LinkLayer)
     * <li> {@link LinkLayer#EVENT_PROP_LINK_STATUS} - the current {@link LinkStatus} status
     * </ul>
     */
    String TOPIC_DEACTIVATED = TOPIC_PREFIX + "DEACTIVATED";

    /** 
     * Topic used for when the link layer's status has changed. 
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ} - the {@link LinkLayer} object 
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the layer as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * LinkLayer object as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the layer as a String,
     * may not be included if the layer has no PID
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * Link Layer represents (e.g., LinkLayer)
     * <li> {@link LinkLayer#EVENT_PROP_LINK_STATUS} - the current {@link LinkStatus} status 
     * </ul>
     */
    String TOPIC_STATUS_CHANGED = TOPIC_PREFIX + "STATUS_CHANGED";

    /** 
     * Topic used when the link layer receives data. 
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ} - the {@link LinkLayer} object 
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the layer as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * LinkLayer object as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the layer as a String,
     * may not be included if the layer has no PID
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * Link Layer represents (e.g., LinkLayer)
     * <li> {@link LinkLayer#EVENT_PROP_LINK_STATUS} - the current {@link LinkStatus} status
     * <li> {@link LinkLayer#EVENT_PROP_LINK_FRAME} - the {@link LinkFrame} that was received
     * <li> {@link Address#getEventProperties(String)} - {@link Address#EVENT_PROP_SOURCE_ADDRESS_PREFIX} properties 
     * from which the {@link LinkFrame} originated, may not be present if the layer does not support addressing
     * <li>{@link Address#getEventProperties(String)} - {@link Address#EVENT_PROP_DEST_ADDRESS_PREFIX} properties 
     * describing where the {@link LinkFrame} was received, may not be present if the layer does not support addressing
     * </ul>
     */
    String TOPIC_DATA_RECEIVED = TOPIC_PREFIX + "DATA_RECEIVED";

    /** 
     * Topic used when the link layer sends data. 
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ} - the {@link LinkLayer} object 
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the layer as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * LinkLayer object as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the layer as a String,
     * may not be included if the layer has no PID
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * Link Layer represents (e.g., LinkLayer)
     * <li> {@link LinkLayer#EVENT_PROP_LINK_STATUS} - the current {@link LinkStatus} status
     * <li> {@link LinkLayer#EVENT_PROP_LINK_FRAME} - the {@link LinkFrame} that was sent
     * <li>{@link Address#getEventProperties(String)} - {@link Address#EVENT_PROP_DEST_ADDRESS_PREFIX} properties 
     * describing where the frame was sent, may not be present if the layer does not support addressing
     * </ul>
     */
    String TOPIC_DATA_SENT = TOPIC_PREFIX + "DATA_SENT";

    /** Event property key for the {@link LinkStatus}. */
    String EVENT_PROP_LINK_STATUS = "link.status";

    /** Event property key for the {@link LinkFrame}. */
    String EVENT_PROP_LINK_FRAME = "link.frame";

    /**
     * Activate this link layer. When activated, this protocol will attempt to connect to the currently set
     * {@link PhysicalLink}. Once the connection is established, this protocol will maintain the connection per the
     * protocol specification. If the {@link PhysicalLink} unexpectedly disconnects while activated, this protocol will
     * continuously attempt to reconnect until deactivated.
     */
    void activateLayer();

    /**
     * Deactivate this link layer protocol. When deactivated, this protocol will disconnect the currently set
     * {@link PhysicalLink}. This method is asynchronous, so the caller must use the {@link #TOPIC_DEACTIVATED} to know
     * that the link has become deactivated. It is up to the caller to decide how long to wait.
     */
    void deactivateLayer();

    /**
     * returns the PhysicalLink used.
     * 
     * @return The PhysicalLink that this LinkLayer uses.
     */
    PhysicalLink getPhysicalLink();

    /**
     * Get the link status.
     * 
     * @return The status of the link.
     */
    LinkStatus getLinkStatus();
    
    /**
     * Determine whether the provided Address can be reached using this LinkLayer, regardless of whether it has been 
     * reached.
     * @param address
     *          The address for which to check availability
     * @return The availability of the link
     */
    boolean isAvailable(Address address);

    /**
     * Determine if the link layer is activated.
     * 
     * @return True if this data link protocol is activated, false otherwise.
     */
    boolean isActivated();

    /**
     * Indicates if the link layer is currently performing BIT.
     *
     * @return true if the logical device is performing BIT, false otherwise
     * @see #performBit()
     */
    boolean isPerformingBit();

    /**
     * Send data; if this link is not connected, it will throw an CCommException. Data will be encoded at the frame
     * level. This method will post the {@link #TOPIC_DATA_SENT} event.
     * 
     * @param frame
     *            data to send.
     * @param addr
     *            The destination address.
     * 
     * @throws CCommException
     *             if the physical link is not connected.
     * 
     * @return Number of bytes sent.
     */
    int send(LinkFrame frame, Address addr) throws CCommException;

    /**
     * Request that the link layer perform a built-in test (BIT). The BIT is something performed on the link layer
     * itself to ensure the layer is working properly.
     *
     * @return the status of the link layer after BIT has been performed
     * 
     * @throws CCommException
     *     if a BIT is not supported 
     */
    LinkStatus performBit() throws CCommException;
    
    /**
     * Get the maximum transmission units for the layer as currently allowed by the transmission device. If the MTU is 
     * a fixed value, this will return the same value as {@link 
     * mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities#getMtu()}. If not device can allow different MTUs, 
     * this will return the value of {@link LinkLayerProxy#getDynamicMtu()}.
     * 
     * @return
     *      current MTU of the layer's payload
     */
    int getMtu();
    
    /**
     * Same as {@link FactoryObject#getFactory()}, but returns the link layer specific factory.
     * 
     * @return
     *      factory for the link layer
     */
    @Override
    LinkLayerFactory getFactory();
    
    /**
     * Get the configuration for the link layer.
     * 
     * <p>
     * <b>NOTE: the returned interface uses reflection to retrieve configuration and so values should be cached once 
     * retrieved</b>
     * 
     * @return
     *      configuration attributes for the link layer
     */
    LinkLayerAttributes getConfig();

    /**
     * This enumeration defines the possible states a link may be in: OK or LOST.
     */
    enum LinkStatus
    {
        /** LOST. */
        LOST,

        /** OK. */
        OK;
    }
}
