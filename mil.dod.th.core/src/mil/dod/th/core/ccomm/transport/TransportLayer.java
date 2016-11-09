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

package mil.dod.th.core.ccomm.transport;

import java.nio.ByteBuffer;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.factory.FactoryObject;

/**
 * This interface is for a transport layer. The transport layer takes care of packetization, fragmentation, reassembly,
 * reliability, etc. A transport layer can be connection oriented, meaning the {@link #connect} method must be called
 * before sending a message and {@link #send(ByteBuffer)} or {@link #send(TransportPacket)} must be used. For 
 * connection-less transport layers, the {@link #connect} and {@link #disconnect} methods cannot be called and 
 * either {@link #send(ByteBuffer, Address)} or {@link #send(TransportPacket, Address)} must be used to send data. A 
 * transport layer's capabilities XML dictates whether the layer is connection oriented or not using {@link 
 * mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()}. 
 * 
 * <p>
 * All functionality of a transport layer needed by the consumer should be available through this interface. It should 
 * never be necessary to use the {@link TransportLayerProxy} interface implemented by the plug-in. For example, if 
 * SomeTransportLayer implements {@link TransportLayerProxy}, consumers should not directly access SomeTransportLayer 
 * methods, but instead access this interface.
 * 
 * <p>
 * Instances of a TransportLayer are managed (created, tracked, deleted) by the core. This interface should never be 
 * implemented by a plug-in. Instead, a plug-in implements {@link TransportLayerProxy} to define custom behavior that is
 * invoked when consumers use this interface. To interact with a transport layer, use the {@link 
 * mil.dod.th.core.ccomm.CustomCommsService}.
 */
@ProviderType
public interface TransportLayer extends FactoryObject
{
    /** 
     * Each {@link TransportLayerProxy} implementation must provide a {@link 
     * org.osgi.service.component.ComponentFactory} with the factory attribute set to this constant.
     * 
     * <p>
     * For example:
     * 
     * <pre>
     * {@literal @}Component(factory = TransportLayer.FACTORY)
     * public class MyTransportLayer implements TransportLayerProxy
     * {
     *     ...
     * </pre>
     */
    String FACTORY = "mil.dod.th.core.ccomm.transport.TransportLayer";
    
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/ccomm/transport/TransportLayer/";

    /** Event topic for when transport layer receives data. 
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ} - the {@link TransportLayer} object 
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the layer as a string
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * TransportLayer object as a String
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the layer as a String,
     * may not be included if the layer has no PID
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * TransportLayer represents (e.g., TransportLayer)
     * <li>{@link TransportLayer#EVENT_PROP_PACKET} - the {@link TransportPacket} of data that was received
     * <li> {@link Address#getEventProperties(String)} - {@link Address#EVENT_PROP_SOURCE_ADDRESS_PREFIX} properties 
     * from which the {@link TransportPacket} originated, may not be present if the layer does not support addressing
     * <li>{@link Address#getEventProperties(String)} - {@link Address#EVENT_PROP_DEST_ADDRESS_PREFIX} properties 
     * describing where the {@link TransportPacket} was received, may not be present if the layer does not support 
     * addressing
     * </ul>
     */
    String TOPIC_PACKET_RECEIVED = TOPIC_PREFIX + "PACKET_RECEIVED";

    /** Event property key for the {@link TransportPacket}. */
    String EVENT_PROP_PACKET = "transport.packet";

    /**
     * Get the current Link Layer in use.
     * 
     * @return 
     *      link layer used by this transport layer or null if the transport layer does not use a link layer to data
     */
    LinkLayer getLinkLayer();
    
    /**
     * Connect to an endpoint given the address for data transmission. Method only applies to connection oriented 
     * layers. After this method is called, use {@link #send(ByteBuffer)} or {@link #send(TransportPacket)} to send data
     * to the given endpoint.
     * 
     * @param address
     *      address to connect to
     * @throws CCommException
     *      if unable to connect
     * @throws IllegalStateException
     *      if already connected or attempting to call with a non-connection oriented layer
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void connect(Address address) throws CCommException, IllegalStateException;
    
    /**
     * Disconnect from the currently connected endpoint. Method only applies to connection oriented 
     * layers.
     * 
     * @throws CCommException
     *      if unable to disconnect
     * @throws IllegalStateException
     *      if not currently connected or attempting to call with a non-connection oriented layer
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void disconnect() throws CCommException, IllegalStateException;
    
    /**
     * Determine whether the provided {@link Address} can be reached using this TransportLayer, regardless of whether 
     * it has been reached. Method should return true if 
     * {@link mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()} is true, 
     * {@link #isConnected()} is true, and the provided Address is the connected {@link Address}.
     * 
     * @param address
     *      address for which to check availability
     * @return
     *      availability of the transport
     */
    boolean isAvailable(Address address);
    
    /**
     * This method gives insight into the activity of the Transport Layer. When something is working on receiving a data
     * packet this method will return true.
     * 
     * @return
     *      true if the layer is receiving data.
     */
    boolean isReceiving();

    /**
     * This method gives insight into the activity of the Transport Layer. When something is transmitting this method
     * will return true.
     * 
     * @return
     *      true if the layer is sending data.
     */
    boolean isTransmitting();
    
    /**
     * Whether the transport layer is connected. Method will return false if {@link 
     * mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()} is false.
     * 
     * @return
     *      true if the transport layer is connection oriented and currently connected to an endpoint
     */
    boolean isConnected();

    /**
     * Send a packet of user defined data to the given destination. The passed input buffer is viewed as an entire 
     * packet and will pop out at the other end in a single packet as well.  This method must be thread safe to allow 
     * multiple threads to call this method at the same time. Method should only be used for non-connection oriented 
     * layers.
     * 
     * @param data
     *      data to send
     * @param addr
     *      the address to send the data to.
     * @throws CCommException
     *      if all or part of the data could not be sent.
     * @throws IllegalStateException
     *      if attempting to call with a connection oriented layer.
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void send(ByteBuffer data, Address addr) throws CCommException, IllegalStateException;

    /**
     * Send a packet of user defined data to the given destination. This method must be thread safe to allow multiple 
     * threads to call this method at the same time. Method should only be used for non-connection oriented layers.
     * 
     * @param pkt
     *      the packet to be sent.
     * @param addr
     *      the address to send the data to.
     * @throws CCommException
     *      if all or part of the data could not be sent.
     * @throws IllegalStateException
     *      if attempting to call with a connection oriented layer.
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void send(TransportPacket pkt, Address addr) throws CCommException, IllegalStateException;
    
    /**
     * Send a packet of user defined data to the connected endpoint. The passed input buffer is viewed as an entire 
     * packet and will pop out at the other end in a single packet as well.  This method must be thread safe to allow 
     * multiple threads to call this method at the same time. Method should only be used for connection oriented layers.
     * 
     * @param data
     *      data to send
     * @throws CCommException
     *      if all or part of the data could not be sent.
     * @throws IllegalStateException
     *      if attempting to call with a non-connection oriented layer.
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void send(ByteBuffer data) throws CCommException, IllegalStateException;

    /**
     * Send a packet of user defined data to the connected endpoint. This method must be thread safe to allow multiple 
     * threads to call this method at the same time. Method should only be used for connection oriented layers.
     * 
     * @param pkt
     *      the packet to be sent.
     * @throws CCommException
     *      if all or part of the data could not be sent.
     * @throws IllegalStateException
     *      if attempting to call with a non-connection oriented layer.
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void send(TransportPacket pkt) throws CCommException, IllegalStateException;
    
    /**
     * Cleans up transport layer resources for shutdown.
     */
    void shutdown();
    
    /**
     * Same as {@link FactoryObject#getFactory()}, but returns the transport layer specific factory.
     * 
     * @return
     *      factory for the transport layer
     */
    @Override
    TransportLayerFactory getFactory();
    
    /**
     * Get the configuration for the transport layer.
     * 
     * <p>
     * <b>NOTE: the returned interface uses reflection to retrieve configuration and so values should be cached once 
     * retrieved</b>
     * 
     * @return
     *      configuration attributes for the transport layer
     */
    TransportLayerAttributes getConfig();
}
