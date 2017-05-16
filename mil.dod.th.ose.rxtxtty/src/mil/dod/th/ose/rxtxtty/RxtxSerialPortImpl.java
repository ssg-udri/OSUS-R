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
// 
// Code is originally from ARL and was updated to integrate into the THOSE 
// workspace.  Code does not originally have unit testing, but should be added
// when making changes.
//

package mil.dod.th.ose.rxtxtty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.metatype.Configurable;

import com.google.common.base.Preconditions;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkContext;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.PropertyValueNotSupportedException;
import mil.dod.th.core.ccomm.physical.SerialPortAttributes;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.log.LogService;

// Note: There is a SerialPort in mil.dod.th.core.ccomm.physical
// and a SerialPort in gnu.io. We are using both of them (@!#%$@#%!!) so we
// import neither of them, to avoid ambiguity.

/**
 * Implementation of the serial port using RXTX.
 */
@Component(factory = PhysicalLink.FACTORY)
public class RxtxSerialPortImpl implements mil.dod.th.core.ccomm.physical.SerialPortProxy
{
    /**
     * Used to enabled debugging statically to enable {@link TestInputStream} and 
     * {@link TestOutputStream}.
     */
    static final boolean ENABLE_DEBUG = false;
    
    /**
     * Reference to the context that allows the physical link to interact with the rest of the system.
     */
    private PhysicalLinkContext m_Context;
    
    /**
     * Reference to the RXTX port that this class wraps.
     */
    private gnu.io.SerialPort m_RxtxPort;
    
    /**
     * Reference to the configuration properties for the serial port.
     */
    private SerialPortAttributes m_Attributes;

    @Override
    public void updated(final Map<String, Object> props) throws ConfigurationException
    {
        m_Attributes = Configurable.createConfigurable(SerialPortAttributes.class, props);
        if (m_RxtxPort != null)
        {
            try
            {
                applyProperties();
            }
            catch (final PhysicalLinkException e)
            {
                throw new ConfigurationException(null, "Unable to apply properties", e);
            }
        }
    }
    
    @Override
    public void initialize(final PhysicalLinkContext context, final Map<String, Object> props)
    {
        m_Context = context;
        m_Attributes = Configurable.createConfigurable(SerialPortAttributes.class, props);
    }

    @Override
    public void close() throws PhysicalLinkException
    {
        if (m_RxtxPort != null)
        {
            m_RxtxPort.close();
            m_RxtxPort = null; // NOPMD: set to null: used to keep track of open status
        }
    }

    @Override
    public InputStream getInputStream() throws PhysicalLinkException
    {
        checkRxtxPort();
        
        try
        {
            if (ENABLE_DEBUG)
            {
                return new TestInputStream(m_RxtxPort.getInputStream());
            }
            else
            {
                return new RxtxInputStreamWrapper(m_RxtxPort.getInputStream());
            }
        }
        catch (final IOException e)
        {
            throw new PhysicalLinkException("Error getting input stream", e);
        }
    }

    @Override
    public OutputStream getOutputStream() throws PhysicalLinkException
    {
        checkRxtxPort();
        
        try
        {
            if (ENABLE_DEBUG)
            {
                return new TestOutputStream(m_RxtxPort.getOutputStream());
            }
            else
            {
                return m_RxtxPort.getOutputStream();
            }
        }
        catch (final IOException e)
        {
            throw new PhysicalLinkException("Error getting output stream", e);
        }
    }

    @Override
    public boolean isOpen()
    {
        return m_RxtxPort != null;
    }

    @Override
    public void open() throws PhysicalLinkException
    {
        final String thePortName = m_Context.getName();

        final CommPortIdentifier portId;
        try
        {
            portId = CommPortIdentifier.getPortIdentifier(thePortName);
        }
        catch (final NoSuchPortException e)
        {
            final Enumeration<?> portIds = CommPortIdentifier.getPortIdentifiers();
            final StringBuilder validPortString = new StringBuilder();
            while (portIds.hasMoreElements())
            {
                validPortString.append(((CommPortIdentifier)portIds.nextElement()).getName());
                validPortString.append(" ");
            }
            final String message = String.format("Can't find port [%s], valid ports are [%s]",
                    thePortName, validPortString.toString());
            throw new PhysicalLinkException(message, e);
        }
        
        try
        {
            final int timeout = 5000;
            m_RxtxPort = (gnu.io.SerialPort)portId.open("RxtxTtyPlugin", timeout);
        }
        catch (final PortInUseException e)
        {
            throw new PhysicalLinkException("Port already in use", e);
        }

        applyProperties();
    }

    @Override
    public void setDTR(final boolean dtrState) throws IllegalStateException
    {
        checkRxtxPort();
        
        m_RxtxPort.setDTR(dtrState);
        if (ENABLE_DEBUG)
        {
            Logging.log(LogService.LOG_DEBUG, "setDTR to %b", dtrState);
        }
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return new HashSet<Extension<?>>();
    }

    /**
     * Update the properties for an open port.
     * 
     * @throws PhysicalLinkException
     *      if there is a problem applying the properties
     */
    private void applyProperties() throws PhysicalLinkException // NOCHECKSTYLE: NOPMD complexity
                                                                // TD: method from ARL, could be cleaned up
    {
        checkRxtxPort();

        final int baudRate = m_Attributes.baudRate();
        final int dataBits = m_Attributes.dataBits();
        final ParityEnum parity = m_Attributes.parity();
        final StopBitsEnum stopBits = m_Attributes.stopBits();
        final FlowControlEnum flowControl = m_Attributes.flowControl();
        final int timeout = m_Attributes.readTimeoutMs();
        
        int rxtxDataBits = 0;
        int rxtxStopBits = 0;
        int rxtxParity = 0;
        int rxtxFlowControl = 0;

        switch (dataBits)
        {
            case 5: // NOCHECKSTYLE: Simple lookup, magic number ok
                rxtxDataBits = gnu.io.SerialPort.DATABITS_5;
                break;
            case 6: // NOCHECKSTYLE: Simple lookup, magic number ok
                rxtxDataBits = gnu.io.SerialPort.DATABITS_6;
                break;
            case 7: // NOCHECKSTYLE: Simple lookup, magic number ok
                rxtxDataBits = gnu.io.SerialPort.DATABITS_7;
                break;
            case 8: // NOCHECKSTYLE: Simple lookup, magic number ok
                rxtxDataBits = gnu.io.SerialPort.DATABITS_8;
                break;
            default:
                throw new PropertyValueNotSupportedException(SerialPortAttributes.CONFIG_PROP_DATA_BITS, dataBits);
        }

        switch (stopBits)
        {
            case ONE_STOP_BIT:
                rxtxStopBits = gnu.io.SerialPort.STOPBITS_1;
                break;
            case TWO_STOP_BITS:
                rxtxStopBits = gnu.io.SerialPort.STOPBITS_2;
                break;
            case ONE_5_STOP_BITS:
                rxtxStopBits = gnu.io.SerialPort.STOPBITS_1_5;
                break;
            default:
                throw new PropertyValueNotSupportedException(SerialPortAttributes.CONFIG_PROP_STOP_BITS, dataBits);
        }

        switch (parity)
        {
            case NONE:
                rxtxParity = gnu.io.SerialPort.PARITY_NONE;
                break;
            case ODD:
                rxtxParity = gnu.io.SerialPort.PARITY_ODD;
                break;
            case EVEN:
                rxtxParity = gnu.io.SerialPort.PARITY_EVEN;
                break;
            default:
                throw new PropertyValueNotSupportedException(SerialPortAttributes.CONFIG_PROP_PARITY, dataBits);
        }

        switch (flowControl)
        {
            case NONE:
                rxtxFlowControl = gnu.io.SerialPort.FLOWCONTROL_NONE;
                break;
            case HARDWARE:
                rxtxFlowControl = gnu.io.SerialPort.FLOWCONTROL_RTSCTS_IN | gnu.io.SerialPort.FLOWCONTROL_RTSCTS_OUT;
                break;
            case XON_XOFF:
                rxtxFlowControl = gnu.io.SerialPort.FLOWCONTROL_XONXOFF_IN | gnu.io.SerialPort.FLOWCONTROL_XONXOFF_OUT;
                break;
            default:
                throw new PropertyValueNotSupportedException(SerialPortAttributes.CONFIG_PROP_FLOW_CONTROL, dataBits);
        }

        try
        {
            if (timeout < 0)
            {
                m_RxtxPort.disableReceiveTimeout();
                m_RxtxPort.enableReceiveThreshold(1);
            }
            else
            {
                m_RxtxPort.enableReceiveTimeout(timeout);
                m_RxtxPort.disableReceiveThreshold();
            }
            m_RxtxPort.setSerialPortParams(baudRate, rxtxDataBits, rxtxStopBits, rxtxParity);
            m_RxtxPort.setFlowControlMode(rxtxFlowControl);
        }
        catch (final UnsupportedCommOperationException e)
        {
            throw new PhysicalLinkException("Unable to set parameters", e);
        }
    }

    /**
     * Check if the internal port is null, throw exception if so.
     */
    private void checkRxtxPort()
    {
        Preconditions.checkNotNull(m_RxtxPort, "Serial port is not open");
    }
}
