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

package example.ccomms.serial;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkContext;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.SerialPortProxy;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.log.Logging;

import org.osgi.service.log.LogService;

/**
 * Example implementation of the {@link SerialPortProxy}.
 * 
 * @author dlandoll
 */
@Component(factory = PhysicalLink.FACTORY)
public class ExampleSerialPort implements SerialPortProxy
{
    /**
     * Indicates whether the serial port is open or not.
     */
    private boolean m_OpenFlag;
    
    private PhysicalLinkContext m_Context;

    @Override
    public void initialize(final PhysicalLinkContext context, final Map<String, Object> props)
    {
        // Do nothing
        m_Context = context;
    }

    @Override
    public void updated(final Map<String, Object> props)
    {
        // Do nothing
    }

    @Override
    public void open() throws PhysicalLinkException
    {
        Logging.log(LogService.LOG_DEBUG, "Opening Physical Link %s", m_Context.getName());
        m_OpenFlag = true;
    }

    @Override
    public void close() throws PhysicalLinkException
    {
        m_OpenFlag = false;
    }

    @Override
    public InputStream getInputStream() throws PhysicalLinkException
    {
        throw new PhysicalLinkException("InputStream not available");
    }

    @Override
    public OutputStream getOutputStream() throws PhysicalLinkException
    {
        throw new PhysicalLinkException("OutputStream not available");
    }

    @Override
    public boolean isOpen()
    {
        return m_OpenFlag;
    }

    @Override
    public void setDTR(boolean high) throws IllegalStateException
    {
        // Do nothing
    }
    
    @Override
    public Set<Extension<?>> getExtensions()
    {
        return null;
    }
}
