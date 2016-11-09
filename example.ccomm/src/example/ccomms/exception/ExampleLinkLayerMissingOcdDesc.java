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
package example.ccomms.exception;

import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.link.LinkFrame;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.link.LinkLayerContext;
import mil.dod.th.core.ccomm.link.LinkLayerProxy;
import mil.dod.th.core.factory.Extension;


/**
 * @author callen
 *
 */
@Component(factory = LinkLayer.FACTORY)
public class ExampleLinkLayerMissingOcdDesc implements LinkLayerProxy
{    
    @Override
    public void initialize(final LinkLayerContext context, final Map<String, Object> props)
    {
        // Do nothing
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        // Do nothing
    }

    @Override
    public void onActivate()
    {
        
    }

    @Override
    public void onDeactivate()
    {
        // Do nothing
    }

    @Override
    public boolean isAvailable(Address address)
    {
        return false;
    }

    @Override
    public int send(LinkFrame frame, Address addr) throws CCommException
    {
        return 0;
    }

    @Override
    public LinkStatus onPerformBit() throws CCommException
    {
        return null;
    }

    @Override
    public int getDynamicMtu() throws UnsupportedOperationException
    {
        return 0;
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
