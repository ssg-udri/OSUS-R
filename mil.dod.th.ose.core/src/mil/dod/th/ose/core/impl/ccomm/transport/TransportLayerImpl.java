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
package mil.dod.th.ose.core.impl.ccomm.transport;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;

import com.google.common.base.Preconditions;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerAttributes;
import mil.dod.th.core.ccomm.transport.TransportLayerProxy;
import mil.dod.th.core.ccomm.transport.TransportPacket;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.factory.api.AbstractFactoryObject;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * Basic transport layer implementation.
 * @author allenchl
 *
 */
@Component (factory = TransportLayerInternal.COMPONENT_FACTORY_REG_ID)
public class TransportLayerImpl extends AbstractFactoryObject implements TransportLayerInternal
{
    /** 
     * Boolean to hold if the implemented TransportLayer is receiving data. 
     */
    private boolean m_IsRecieving;
    
    /** 
     * Holds reference to the associated link layer. 
     */
    private LinkLayer m_LinkLayer;
    
    /**
     * Boolean to hold if the implemented TransportLayer is transmitting data.
     */
    private boolean m_IsTransmitting;
    
    /**
     * Transport layer proxy.
     */
    private TransportLayerProxy m_TransProxy;
    
    /**
     * Boolean to hold if the implemented TransportLayer is connection oriented.
     */
    private boolean m_IsConnectionOriented;
    
    /**
     * Address that connection oriented transport layer is connected to.
     */
    private Address m_ConnectedAddress;

    /**
     * Reference to internal power management for factory objects.
     */
    private PowerManagerInternal m_PowInternal;

    /**
     * Wake lock used for transport layer operations.
     */
    private WakeLock m_WakeLock;

    /**
     * Wake lock used when the transport layer is receiving data.
     */
    private WakeLock m_RecvWakeLock;

    @Override
    public void initialize(final FactoryRegistry<?> registry, final FactoryObjectProxy proxy, 
            final FactoryInternal factory, final ConfigurationAdmin configAdmin, final EventAdmin eventAdmin,
            final PowerManagerInternal powInternal, final UUID uuid, final String name, 
            final String pid, final String baseType) throws IllegalStateException
    {
        super.initialize(registry, proxy, factory, configAdmin, eventAdmin, powInternal, uuid, name, pid, baseType);
        m_TransProxy = (TransportLayerProxy)proxy;
        m_IsConnectionOriented = factory.getTransportLayerCapabilities().isConnectionOriented();
        m_PowInternal = powInternal;
        m_WakeLock = powInternal.createWakeLock(m_TransProxy.getClass(), this, "coreTransLayer");
        m_RecvWakeLock = powInternal.createWakeLock(m_TransProxy.getClass(), this, "coreTransLayerRecv");
    }
    
    @Override
    public void beginReceiving()
    {
        m_RecvWakeLock.activate();
        m_IsRecieving = true;
    }

    @Override
    public void endReceiving(final TransportPacket pkt, final Address sourceAddress, 
            final Address destAddress)
    {
        postDataReceived(pkt, sourceAddress, destAddress);
        m_IsRecieving = false;

        try
        {
            m_RecvWakeLock.cancel();
        }
        catch (final IllegalStateException ex)
        {
            Logging.log(LogService.LOG_WARNING,
                    "Transport Layer [%s] endReceiving() called without calling beginReceiving() first", getName());
        }
    }

    @Override
    public LinkLayer getLinkLayer()
    {
        return m_LinkLayer;
    }

    @Override
    public boolean isAvailable(final Address address)
    {
        if (m_IsConnectionOriented)
        {
            return m_TransProxy.isConnected() && address.equalProperties(m_ConnectedAddress.getProperties());
        }
        else
        {
            return m_TransProxy.isAvailable(address);
        }
    }

    @Override
    public boolean isReceiving()
    {
        return m_IsRecieving;
    }

    @Override
    public boolean isTransmitting()
    {
        return m_IsTransmitting;
    }

    @Override
    public void send(final ByteBuffer data, final Address addr) throws CCommException, IllegalStateException
    {
        verifyConnectionLess();

        m_IsTransmitting = true;  
        try
        {
            m_WakeLock.activate();

            m_TransProxy.send(data, addr);
        }
        finally
        {
            m_IsTransmitting = false;

            m_WakeLock.cancel();
        }      
    }

    @Override
    public void send(final TransportPacket pkt, final Address addr) throws CCommException, IllegalStateException
    {        
        send(pkt.getPayload(), addr);
    }
    
    @Override
    public void send(final ByteBuffer data) throws CCommException, IllegalStateException
    {
        verifyConnectionOriented();

        m_IsTransmitting = true;
        try
        {
            m_WakeLock.activate();

            m_TransProxy.send(data);
        }
        finally
        {
            m_IsTransmitting = false;

            m_WakeLock.cancel();
        }       
    }

    @Override
    public void send(final TransportPacket pkt) throws CCommException, IllegalStateException
    {
        send(pkt.getPayload());        
    }
    
    @Override
    public void shutdown()
    {
        if (isConnected())
        {
            try
            {
                disconnect();
            }
            catch (final Exception e)
            {
                Logging.log(LogService.LOG_ERROR, e, "Unable to disconnect TransportLayer during shutdown");
            }
        }

        final LinkLayer linkLayer = getLinkLayer();
        if (linkLayer == null)
        {
            Logging.log(LogService.LOG_INFO, "Transport layer %s has no link layer to shutdown", getName());
        }
        else        
        {
            linkLayer.deactivateLayer();
        }
        
        try
        {
            m_WakeLock.activate();

            //shut down proxy
            m_TransProxy.onShutdown();
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public void setLinkLayer(final LinkLayer linkLayer)
    {
        m_LinkLayer = linkLayer;
    }

    @Override
    public TransportLayerAttributes getConfig()
    {
        return Configurable.createConfigurable(TransportLayerAttributes.class, getProperties());
    }
    
    /**
     * Post the {@link #TOPIC_PACKET_RECEIVED} event to the EventAdmin service.
     * 
     * @param pkt
     *      Packet that was received
     * @param sourceAddress
     *      Source address of the packet
     * @param destAddress
     *      Destination address of the packet
     */
    private void postDataReceived(final TransportPacket pkt, final Address sourceAddress, final Address destAddress)
    {
        Preconditions.checkNotNull(pkt, "Cannot transmit a [NULL] packet.");
        
        final Map<String, Object> props = new HashMap<>();
        if (sourceAddress != null)
        {
            props.putAll(sourceAddress.getEventProperties(Address.EVENT_PROP_SOURCE_ADDRESS_PREFIX));
        }
        if (destAddress != null)
        {
            props.putAll(destAddress.getEventProperties(Address.EVENT_PROP_DEST_ADDRESS_PREFIX));
        }
        props.put(EVENT_PROP_PACKET, pkt);
        postEvent(TOPIC_PACKET_RECEIVED, props);

        Logging.log(LogService.LOG_DEBUG, "Posted data received event for transport: %s", getName());
    }

    @Override
    public void connect(final Address address) throws CCommException, IllegalStateException
    {
        verifyConnectionOriented();

        try
        {
            m_WakeLock.activate();

            m_TransProxy.connect(address);
            m_ConnectedAddress = address;
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public void disconnect() throws CCommException, IllegalStateException
    {
        verifyConnectionOriented();

        try
        {
            m_WakeLock.activate();

            m_TransProxy.disconnect();    
            m_ConnectedAddress = null;
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public boolean isConnected()
    {
        if (m_IsConnectionOriented)
        {
            return m_TransProxy.isConnected();
        }
        else
        {
            return false;
        }
    }
    
    @Override
    public void delete() throws IllegalStateException
    {
        final LinkLayer linkLayer = getLinkLayer();
        if (linkLayer != null && linkLayer.isActivated())
        {
            throw new IllegalStateException(String.format("Transport Layer [%s] Link Layer is activated.", getName()));
        }

        shutdown();

        m_PowInternal.deleteWakeLock(m_WakeLock);
        m_PowInternal.deleteWakeLock(m_RecvWakeLock);

        super.delete();
    }

    /**
     * Throws exception if transport layer is not connection-oriented.
     * 
     * @throws IllegalStateException
     *      if not connection-oriented
     */
    private void verifyConnectionOriented() throws IllegalStateException
    {
        if (!m_IsConnectionOriented)
        {
            throw new IllegalStateException("TransportLayer is not connection-oriented");
        }
    }

    /**
     * Throws exception if transport layer is connection-oriented.
     * 
     * @throws IllegalStateException
     *      if connection-oriented
     */
    private void verifyConnectionLess() throws IllegalStateException
    {
        if (m_IsConnectionOriented)
        {
            throw new IllegalStateException("TransportLayer is connection-oriented");
        }
    }
}
