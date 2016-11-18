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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkContext;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.PhysicalLinkProxy;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.log.LoggingService;


/**
 * @author dhumeniuk
 *
 */
@Component(factory = PhysicalLink.FACTORY)
public class ExamplePhysicalLink implements PhysicalLinkProxy
{
    private LoggingService m_Logging;
    private boolean m_Open = false;
    
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    @Override
    public void initialize(final PhysicalLinkContext context, final Map<String, Object> props)
    {
        // Do nothing
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        // Do nothing
    }
    
    @Override
    public void open()
    {
        m_Logging.info("Example PhysicalLink opened");
        m_Open = true;
    }

    @Override
    public void close()
    {
        m_Logging.info("Example PhysicalLink closed");
        m_Open = false;
    }

    @Override
    public boolean isOpen()
    {
        return m_Open;
    }

    @Override
    public InputStream getInputStream() throws PhysicalLinkException
    {
        return null;
    }

    @Override
    public OutputStream getOutputStream() throws PhysicalLinkException
    {
        return null;
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
