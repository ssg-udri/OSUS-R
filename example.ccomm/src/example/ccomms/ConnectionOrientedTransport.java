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
package example.ccomms;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.transport.BaseTransportPacket;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerContext;
import mil.dod.th.core.ccomm.transport.TransportLayerProxy;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

import org.osgi.service.cm.ConfigurationException;

/**
 * This plug-in shows a very basic implementation of a connection oriented transport layer.
 * 
 * @author jlatham
 */
@Component(factory = TransportLayer.FACTORY)
public class ConnectionOrientedTransport implements TransportLayerProxy
{
    private TransportLayerContext m_Context;
    private boolean m_IsConnected;

    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();

    @Override
    public void updated(Map<String, Object> props) throws ConfigurationException
    {
        // Do nothing        
    }

    @Override
    public void initialize(TransportLayerContext context, Map<String, Object> props) throws FactoryException
    {
        m_Context = context;
        m_CountingLock.setWakeLock(m_Context.createPowerManagerWakeLock(getClass().getSimpleName() + "WakeLock"));
    }

    @Deactivate
    public void deactivateInstance()
    {
        m_CountingLock.deleteWakeLock();
    }

    @Override
    public void connect(Address address) throws CCommException, IllegalStateException
    {
        m_IsConnected = true;
    }

    @Override
    public void disconnect() throws CCommException, IllegalStateException
    {
        m_IsConnected = false;     
    }

    @Override
    public boolean isConnected()
    {
        return m_IsConnected;
    }

    @Override
    public boolean isAvailable(Address address)
    {
        return isConnected();
    }

    @Override
    public void send(ByteBuffer data, Address addr) throws CCommException
    {
        throw new IllegalStateException("This transport layer is connection oriented, "
                + "cannot send to specific address.");
    }

    @Override
    public void send(ByteBuffer data) throws CCommException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            if (m_IsConnected)
            {
                m_Context.beginReceiving();
                m_Context.endReceiving(new BaseTransportPacket(data), null, null);   
            }
            else
            {
                throw new CCommException("Connection oriented transport layer must be connected before sending data.",
                        FormatProblem.OTHER);
            }
        }
    }

    @Override
    public void onShutdown()
    {
        m_IsConnected = false;
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
