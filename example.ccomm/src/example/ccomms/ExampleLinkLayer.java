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

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.link.LinkFrame;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.link.LinkLayerContext;
import mil.dod.th.core.ccomm.link.LinkLayerProxy;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

import org.osgi.service.log.LogService;

/**
 * @author dhumeniuk
 *
 */
@Component(factory = LinkLayer.FACTORY)
public class ExampleLinkLayer implements LinkLayerProxy
{
    private LoggingService m_Logging;
    private LinkLayerContext m_Context;

    /**
     * Reference to the counting {@link WakeLock} used by this asset.
     */
    private CountingWakeLock m_CountingLock = new CountingWakeLock();
    
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }

    @Override
    public void initialize(final LinkLayerContext context, final Map<String, Object> props)
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
    public void updated(final Map<String, Object> props)
    {
        // Do nothing
    }
    
    @Override
    public void onActivate()
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            Logging.log(LogService.LOG_INFO, "Example LinkLayer activated");

            try
            {
                m_Context.getPhysicalLink().open();
            }
            catch (PhysicalLinkException e)
            {
                m_Logging.error(e, "Could not activate example link layer.");
            }
        }
    }

    @Override
    public void onDeactivate()
    {
        try (CountingWakeLockHandle wakeHandle = m_CountingLock.activateWithHandle())
        {
            try
            {
                m_Context.getPhysicalLink().close();
            }
            catch (PhysicalLinkException e)
            {
                m_Logging.error(e, "Could not deactivate example link layer.");
            }

            m_Logging.info("ExampleLinkLayer deactivated");
        }
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
        return 1000;
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
