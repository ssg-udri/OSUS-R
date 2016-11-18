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
import mil.dod.th.core.ccomm.transport.BaseTransportPacket;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerContext;
import mil.dod.th.core.ccomm.transport.TransportLayerProxy;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

/**
 * @author dhumeniuk
 *
 */
@Component(factory = TransportLayer.FACTORY)
public class EchoTransport implements TransportLayerProxy
{
    private TransportLayerContext m_Context;

    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();

    @Override
    public void initialize(final TransportLayerContext context, final Map<String, Object> props)
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
    public void send(ByteBuffer data, Address destAddr) throws CCommException
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            m_Context.beginReceiving();
            m_Context.endReceiving(new BaseTransportPacket(data), null, destAddr);
        }
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        // Do nothing
    }
    
    @Override
    public boolean isAvailable(final Address address)
    {
        return true;
    }

    @Override
    public void onShutdown() 
    {
        // Do nothing
    }

    @Override
    public void connect(Address address) throws CCommException, IllegalStateException
    {
        throw new IllegalStateException("This is not a connection oriented transport layer.");     
    }

    @Override
    public void disconnect() throws CCommException, IllegalStateException
    {
        throw new IllegalStateException("This is not a connection oriented transport layer.");
    }

    @Override
    public boolean isConnected()
    {
        return false;
    }

    @Override
    public void send(ByteBuffer data) throws CCommException
    {
        throw new IllegalStateException("This is not a connection oriented transport layer.");
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
