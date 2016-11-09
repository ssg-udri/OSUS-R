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

import java.util.Map;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;

/**
 * A link layer plug-in implements this interface providing any custom behavior of the plug-in that is needed to 
 * interact with the physical device. The core will perform some functions automatically such as tracking the status
 * and underlying {@link mil.dod.th.core.ccomm.physical.PhysicalLink}.
 * 
 * <p>
 * The plug-in implementation must also be sure to call on {@link 
 * LinkLayerContext#postReceiveEvent(Address, Address, LinkFrame)} when receiving data.
 * 
 * @author dhumeniuk
 *
 */
public interface LinkLayerProxy extends FactoryObjectProxy
{
    /**
     * Called to initialize the object and provide the plug-in with the {@link LinkLayerContext} to interact with the
     * rest of the system.
     * 
     * @param context
     *      context specific to the {@link LinkLayer} instance
     * @param props
     *      the link layer's configuration properties, available as a convenience so {@link 
     *      LinkLayerContext#getProperties()} does not have to be called
     * @throws FactoryException
     *      if there is an error initializing the object.
     */
    void initialize(LinkLayerContext context, Map<String, Object> props) throws FactoryException;
    
    /**
     * Activate this link layer. When activated, this protocol will attempt to connect to the currently set
     * {@link mil.dod.th.core.ccomm.physical.PhysicalLink}. Once the connection is established, this protocol will 
     * maintain the connection per the protocol specification. If the {@link 
     * mil.dod.th.core.ccomm.physical.PhysicalLink} unexpectedly disconnects while activated, this protocol will
     * continuously attempt to reconnect until deactivated.
     */
    void onActivate();

    /**
     * Deactivate this link layer protocol. When deactivated, this protocol will disconnect the currently set
     * {@link mil.dod.th.core.ccomm.physical.PhysicalLink}. This method is asynchronous, so the caller must use the 
     * {@link LinkLayer#TOPIC_DEACTIVATED} to know that the link has become deactivated. It is up to the caller to 
     * decide how long to wait.
     */
    void onDeactivate();
    
    /**
     * Determine whether the provided Address can be reached using this LinkLayer, regardless of whether it has been 
     * reached.
     * @param address
     *          The address for which to check availability
     * @return The availability of the link
     */
    boolean isAvailable(Address address);
    
    /**
     * Send data; if this link is not connected, it will throw an CCommException. Data will be encoded at the frame
     * level.
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
     * Override to perform built-in test (BIT) on the link layer.
     * 
     * @return
     *      the status of the layer after performing BIT (must not be null)
     * @throws CCommException
     *      if the layer fails to perform BIT or the operation is not supported
     * @see LinkLayer#performBit()
     */
    LinkStatus onPerformBit() throws CCommException;
    
    /**
     * Get the dynamic maximum transmission unit (MTU) of the {@link LinkLayer}. This method would only be implemented 
     * if the MTU can vary and if {@link mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities#isStaticMtu()} 
     * returns false. If MTU does not vary, the MTU should be defined within the capabilities XML.
     * 
     * @return
     *      the dynamic MTU of the layer
     * @throws UnsupportedOperationException
     *      should be thrown if {@link mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities#isStaticMtu()} 
     *      returns true
     */
    int getDynamicMtu() throws UnsupportedOperationException;
}
