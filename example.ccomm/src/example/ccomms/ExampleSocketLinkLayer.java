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

import java.util.Map;
import java.util.Set;

import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.link.LinkFrame;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.link.LinkLayerContext;
import mil.dod.th.core.ccomm.link.LinkLayerProxy;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.log.LoggingService;

/**
 * This link layer simulates a link layer that does not use a physical link, but uses a socket instead.
 * 
 * @author dhumeniuk
 *
 */
@Component(factory = LinkLayer.FACTORY)
public class ExampleSocketLinkLayer implements LinkLayerProxy
{
    private LoggingService m_Logging;
    
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }

    @Override
    public void initialize(final LinkLayerContext context, final Map<String, Object> props)
    {
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        // Do nothing
    }
    
    @Override
    public void onActivate()
    {
        Logging.log(LogService.LOG_INFO, "Example Socket LinkLayer activated");
    }
    
    @Override
    public void onDeactivate()
    {
        m_Logging.info("Example Socket LinkLayer deactivated");
    }
    
    @Override
    public boolean isAvailable(final Address address)
    {
        return true;
    }

    @Override
    public int send(LinkFrame frame, Address addr) throws CCommException
    {
        return 0;
    }

    @Override
    public LinkStatus onPerformBit() throws CCommException
    {
        return LinkStatus.OK;
    }

    @Override
    public int getDynamicMtu() throws UnsupportedOperationException 
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
