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
import java.util.Map;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;

/**
 * A transport layer plug-in implements this interface providing any custom behavior of the plug-in that is needed. The 
 * core will perform some functions automatically such as tracking the status and underlying {@link 
 * mil.dod.th.core.ccomm.link.LinkLayer}.
 * 
 * <p>
 * A transport layer plug-in can be connection oriented meaning the layer sends data to a single endpoint during the 
 * duration of a connection. If not connection oriented, the layer can send data to different endpoints based on the 
 * address given to {@link #send(ByteBuffer, Address)}. If the layer is connection oriented, {@link #connect(Address)}
 * will be called before {@link #send(ByteBuffer)} is called. If not connection oriented, {@link #connect(Address)} and 
 * {@link #disconnect()} will never be called. A transport layer's capabilities XML dictates whether the layer is 
 * connection oriented or not using {@link 
 * mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()}. 
 * 
 * <p>
 * The plug-in implementation must also be sure to call on {@link TransportLayerContext#beginReceiving()} and {@link
 * TransportLayerContext#endReceiving(TransportPacket, Address, Address)} when receiving data.
 * 
 * @author dhumeniuk
 *
 */
public interface TransportLayerProxy extends FactoryObjectProxy
{
    /**
     * Called to initialize the object and provide the plug-in with the {@link TransportLayerContext} to interact with 
     * the rest of the system.
     * 
     * @param context
     *      context specific to the {@link TransportLayer} instance
     * @param props
     *      the transport layer's configuration properties, available as a convenience so {@link 
     *      TransportLayerContext#getProperties()} does not have to be called
     * @throws FactoryException
     *      if there is an error initializing the object.
     */
    void initialize(TransportLayerContext context, Map<String, Object> props) throws FactoryException;
    
    /**
     * Connect to an endpoint given the address for data transmission. Core will only call method if layer is connection
     * oriented, but will not check whether layer is connected in case layer is in an inconsistent state.
     * 
     * @param address
     *      address to connect to
     * @throws CCommException
     *      if unable to connect
     * @throws IllegalStateException
     *      if already connected
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void connect(Address address) throws CCommException, IllegalStateException;
    
    /**
     * Disconnect from the currently connected endpoint. Core will only call method if layer is connection oriented, but
     * will not check whether layer is connected in case layer is in an inconsistent state. Anything set up during 
     * {@link #connect(Address)} should be cleaned up.
     * 
     * @throws CCommException
     *      if unable to connect
     * @throws IllegalStateException
     *      if not connected
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void disconnect() throws CCommException, IllegalStateException;

    /**
     * Whether the transport layer is connected. Method will only be called by core if layer is connection oriented.
     * 
     * @return
     *      true if currently connected to an endpoint
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    boolean isConnected();
    
    /**
     * Determine whether the provided Address can be reached using this TransportLayer, regardless of whether it has 
     * been reached. Core will only call method if layer is not connection oriented.
     * 
     * @param address
     *      the address for which to check availability
     * @return
     *      the availability of the transport
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    boolean isAvailable(Address address);
    
    /**
     * Send a packet of user defined data to the given address. The passed input buffer is viewed as an entire packet 
     * and will pop out at the other end in a single packet as well. This method must be thread safe to allow multiple 
     * threads to call this method at the same time. Core will only call method if layer is not connection oriented.
     * 
     * @param data
     *      data to send
     * @param addr
     *      the address to send the data to.
     * @throws CCommException
     *      if all or part of the data could not be sent.
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void send(ByteBuffer data, Address addr) throws CCommException;
    
    /**
     * Send a packet of user defined data to the connected address. The passed input buffer is viewed as an entire 
     * packet and will pop out at the other end in a single packet as well. This method must be thread safe to allow 
     * multiple threads to call this method at the same time. Plug-in must send the data to the previously connected 
     * endpoint as defined when {@link #connect} was called. Core will only call method if layer is connection oriented 
     * and {@link #isConnected()} returns true.
     * 
     * @param data
     *      data to send
     * @throws CCommException
     *      if all or part of the data could not be sent.
     * @see mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities#isConnectionOriented()
     */
    void send(ByteBuffer data) throws CCommException;
    
    /**
     * When {@link TransportLayer#shutdown()} is called, this method is called to notify the plug-in.
     */
    void onShutdown();
}
