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

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerContext;
import mil.dod.th.core.ccomm.transport.TransportLayerProxy;
import mil.dod.th.core.factory.Extension;

/**
 * @author thanuscin
 *
 */
@Component(factory = TransportLayer.FACTORY)
public class QueueTransport implements TransportLayerProxy
{
    @Override
    public void initialize(final TransportLayerContext context, final Map<String, Object> props)
    {
    }
    
    @Override
    public void send(ByteBuffer data, Address addr)
    {
        // Do nothing
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        // Do nothing
    }
    
    @Override
    public boolean isAvailable(final Address address)
    {
        //Used when checking whether to Queue or Send an Event
        return false;
    }

    @Override
    public void onShutdown() 
    {
        //Do nothing
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
    
    @Override
    public void connect(Address address) throws CCommException, IllegalStateException
    {        
        //do nothing
    }

    @Override
    public void disconnect() throws CCommException, IllegalStateException
    {
        //do nothing
    }

    @Override
    public boolean isConnected()
    {
        return false;
    }

    @Override
    public void send(ByteBuffer data) throws CCommException
    {
        //do nothing
    }
}
