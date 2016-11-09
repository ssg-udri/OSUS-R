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
// DESCRIPTION:
// Contains the SerialPortImpl class implementation.
//
//==============================================================================
package mil.dod.th.ose.linux.physical;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkContext;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.PropertyValueNotSupportedException;
import mil.dod.th.core.ccomm.physical.SerialPortAttributes;
import mil.dod.th.core.ccomm.physical.SerialPortProxy;
import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;
import mil.dod.th.ose.linux.gnu_c.FileOperations;
import mil.dod.th.ose.linux.gnu_c.TerminalIO;
import mil.dod.th.ose.linux.loader.LinuxNativeLibraryLoader;

/**
 * Implementation of the {@link SerialPortProxy}.
 * 
 * @author dhumeniuk
 */
@Component(factory = PhysicalLink.FACTORY)
public class SerialPortProxyImpl implements SerialPortProxy 
{
    /**
     * Value used for {@link #m_Fd} when the port is not open.
     */
    private static final int NOT_OPEN = -1;

    /**
     * Map for all of the baud rate values, key is baud rate as number, value is the mask representing that value.
     */
    private static final Map<Integer, Integer> BAUD_RATE_MASK_LOOKUP = new HashMap<Integer, Integer>();

    /**
     * File descriptor associated with the serial port.
     */
    private int m_Fd = NOT_OPEN;
    
    /**
     * Input stream for the serial port.
     */
    private SerialPortInputStream m_InputStream;
    
    /**
     * Output stream for the serial port.
     */
    private SerialPortOutputStream m_OutputStream;
    
    /**
     * Reference to the physical link context for the specific physical link instance.
     */
    private PhysicalLinkContext m_Context;
    
    /**
     * Baud rate to use for the serial port.
     */
    private int m_BaudRate;
    
    /**
     * Number of bits in each byte of data read from the link.
     */
    private int m_DataBits;
    
    /**
     * Parity to use for the serial port.
     */
    private ParityEnum m_Parity;
    
    /**
     * Number of stop bits in between each byte.
     */
    private StopBitsEnum m_StopBits;
    
    /**
     * Flow control to use for the serial port.
     */
    private FlowControlEnum m_FlowControl;
    
    /**
     * Read timeout in milliseconds for the serial port.
     */
    private int m_ReadTimeoutMs;
    
    /**
     * Logging service.
     */
    private LoggingService m_LogService;
    
    static
    {
        LinuxNativeLibraryLoader.load();
        
        // construct the look up table
        // baud rates rates are requested by number, so not really magic number
        BAUD_RATE_MASK_LOOKUP.put(1200,    TerminalIO.B1200);    // NOCHECKSTYLE: magic number     
        BAUD_RATE_MASK_LOOKUP.put(1800,    TerminalIO.B1800);    // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(2400,    TerminalIO.B2400);    // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(4800,    TerminalIO.B4800);    // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(9600,    TerminalIO.B9600);    // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(19200,   TerminalIO.B19200);   // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(38400,   TerminalIO.B38400);   // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(57600,   TerminalIO.B57600);   // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(115200,  TerminalIO.B115200);  // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(230400,  TerminalIO.B230400);  // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(460800,  TerminalIO.B460800);  // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(500000,  TerminalIO.B500000);  // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(576000,  TerminalIO.B576000);  // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(921600,  TerminalIO.B921600);  // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(1000000, TerminalIO.B1000000); // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(1152000, TerminalIO.B1152000); // NOCHECKSTYLE: magic number
        BAUD_RATE_MASK_LOOKUP.put(1500000, TerminalIO.B1500000); // NOCHECKSTYLE: magic number
    }
    
    @Reference
    public void setLogService(final LoggingService logService)
    {
        m_LogService = logService;
    }

    @Override
    public void initialize(final PhysicalLinkContext context, final Map<String, Object> props)
    {
        m_Context = context;
        final SerialPortProxyImplAttributes attributes = 
                Configurable.createConfigurable(SerialPortProxyImplAttributes.class, props);
        setProperties(attributes);
    }
    
    @Override
    public void updated(final Map<String, Object> props)
    {
        final SerialPortProxyImplAttributes attributes = 
                Configurable.createConfigurable(SerialPortProxyImplAttributes.class, props);
        setProperties(attributes);
        // Apply properties to hardware if port is already open
        if (isOpen())
        {
            try
            {
                applyProperties();
            }
            catch (final PhysicalLinkException ex)
            {
                throw new IllegalArgumentException("Error applying serial port properties", ex);
            }
        }
    }

    @Override
    public void open() throws PhysicalLinkException
    {
        final int returnValue = FileOperations.open(m_Context.getName(), 
            FileOperations.O_RDWR | FileOperations.O_NOCTTY);
        if (returnValue == NOT_OPEN)
        {
            throw new PhysicalLinkException("Unable to open serial port: " + m_Context.getName());
        }
        else
        {
            m_Fd = returnValue;
            m_InputStream = new SerialPortInputStream(m_Fd);
            m_OutputStream = new SerialPortOutputStream(m_Fd);

            try
            {
                applyProperties();
            }
            catch (final Exception ex)
            {
                // Close the serial port when an error occurs
                close();

                throw new PhysicalLinkException("Unable to apply serial port properties", ex);
            }
        }
    }

    @Override
    public void close()
    {
        final int returnValue = FileOperations.close(m_Fd);
        if (returnValue == -1)
        {
            m_LogService.warning("Unable to close serial port: %s", m_Context.getName());
        }
        m_Fd = NOT_OPEN;
        m_InputStream = null; // NOPMD: 
        // Violation: NullAssignment: object needs GC'd but this object will not
        m_OutputStream = null; // NOPMD: 
        // Violation: NullAssignment: object needs GC'd but this object will not
    }

    @Override
    public InputStream getInputStream() throws PhysicalLinkException
    {
        if (m_InputStream == null)
        {
            throw new PhysicalLinkException("InputStream not available");
        }
        
        return m_InputStream;
    }

    @Override
    public OutputStream getOutputStream() throws PhysicalLinkException
    {
        if (m_OutputStream == null)
        {
            throw new PhysicalLinkException("OutputStream not available");
        }
        
        return m_OutputStream;
    }

    @Override
    public Set<Extension<?>> getExtensions()
    {
        return new HashSet<Extension<?>>();
    }

    /**
     * Applies serial port properties to hardware.
     * 
     * @throws PhysicalLinkException
     *             error applying properties
     */
    private void applyProperties() throws PhysicalLinkException
    {
        long c_iflag = 0;
        final long c_oflag = 0;  // output options not used
        long c_cflag = TerminalIO.CLOCAL 
                           | TerminalIO.CREAD 
                           | getBaudRateMask(m_BaudRate)
                           | getDataBitsMask(m_DataBits)
                           | getParityMask(m_Parity)
                           | getStopBitsMask(m_StopBits);
        final long c_lflag = 0;  // not used

        switch (m_FlowControl)
        {
            case XON_XOFF:
                c_iflag |= TerminalIO.IXON | TerminalIO.IXOFF;
                break;

            case HARDWARE:
                c_cflag |= TerminalIO.CRTSCTS;
                break;

            case NONE:
                // nothing to set in this case
                break;

            default:
                assert false : String.format("Unhandled flow control enumeration (%s)", m_FlowControl);
                break;
        }

        final short[] c_cc = new short[TerminalIO.NCCS];
        c_cc[TerminalIO.VMIN] = 1; // wait for at least one character on a read

        final int returnValue = TerminalIO.tcSetAttr(m_Fd, c_iflag, c_oflag, c_cflag, c_lflag, c_cc);
        if (returnValue != TerminalIO.TIO_SUCCESS)
        {
            throw new PhysicalLinkException(String.format("Unable to set terminal attributes, rv=%d", returnValue));
        }
        
        //Update input stream read timeout.
        m_InputStream.setReadTimeout(m_ReadTimeoutMs);
    }

    /**
     * Get a bit mask for the given baud rate value.  Only a small subset of baud rates are 
     * supported, if more needed, add to switch statement and add define to TerminalIO SWIG module.
     * 
     * @param baudRate
     *      baud rate to convert from
     * @return
     *      a mask based on the baud rate to be used for setting terminal settings
     * @throws PropertyValueNotSupportedException
     *      thrown if the baud rate does not have a mask
     */
    private int getBaudRateMask(final int baudRate) throws PropertyValueNotSupportedException
    {
        final Integer mask = BAUD_RATE_MASK_LOOKUP.get(baudRate);
        if (mask == null)
        {
            throw new PropertyValueNotSupportedException(SerialPortAttributes.CONFIG_PROP_BAUD_RATE, baudRate);
        }
        
        return mask;
    }
    
    /**
     * Get the data bit mask for the given data bit value.
     * 
     * @param dataBits
     *      data bit value to convert from
     * @return
     *      a mask based on the data bits to be used for setting terminal settings 
     * @throws PropertyValueNotSupportedException
     *      thrown if the data bit setting is invalid, no mask available
     */
    private int getDataBitsMask(final int dataBits) throws PropertyValueNotSupportedException
    {
        int mask;//NOCHECKSTYLE will get assigned in switch statement
        switch (dataBits)
        {
            case 7:  // NOCHECKSTYLE: Simple lookup, magic number ok
                mask = TerminalIO.CS7;
                break;
                
            case 8:  // NOCHECKSTYLE: Simple lookup, magic number ok
                mask = TerminalIO.CS8;
                break;
                
            default:
                throw new PropertyValueNotSupportedException(SerialPortAttributes.CONFIG_PROP_DATA_BITS, dataBits);
        }
        return mask;
    }
    
    /**
     * Get the parity mask for the given parity value.
     * 
     * @param parity
     *      parity value to convert from
     * @return
     *      a mask based on the parity to be used, for setting terminal settings 
     */
    private int getParityMask(final ParityEnum parity)
    {
        int mask;//NOCHECKSTYLE will get assigned in switch statement
        switch (parity)
        {
            case EVEN:
                mask = TerminalIO.PARENB;
                break;
                
            case ODD:
                mask = TerminalIO.PARENB | TerminalIO.PARODD;
                break;
                
            case NONE:
                // nothing to set in this case
                mask = 0;
                break;
                
            default:
                mask = 0;
                assert false : String.format("Unhandled parity enumeration (%s)", parity);
                break;
        }
        return mask;
    }

    /**
     * Get the stop bit mask for the given stop bit value.
     * 
     * @param stopBits
     *      stop bits value to convert from
     * @return
     *      a mask based on the stop bits to be used, for setting terminal settings
     * @throws PropertyValueNotSupportedException
     *      thrown if the stop bit setting is invalid, no mask available 
     */
    private int getStopBitsMask(final StopBitsEnum stopBits) throws PropertyValueNotSupportedException
    {
        int mask;//NOCHECKSTYLE will get assigned in switch statement
        switch (stopBits)
        {
            case ONE_STOP_BIT:
                mask = 0; // no macro assoicated with 1 stop bit
                break;
                
            case TWO_STOP_BITS:
                mask = TerminalIO.CSTOPB;
                break;
                
            default:
                throw new PropertyValueNotSupportedException(SerialPortAttributes.CONFIG_PROP_STOP_BITS, stopBits);
        }
        return mask;
    }

    @Override
    public boolean isOpen()
    {
        return m_Fd != NOT_OPEN;
    }
    
    @Override
    public void setDTR(final boolean high)
    {
        if (!isOpen())
        {
            throw new IllegalStateException("Must open serial port prior to setting DTR signal");
        }
        
        if (high)
        {
            TerminalIO.ioCtl(m_Fd, TerminalIO.TIOCMBIS, TerminalIO.TIOCM_DTR);
        }
        else
        {
            TerminalIO.ioCtl(m_Fd, TerminalIO.TIOCMBIC, TerminalIO.TIOCM_DTR);
        }
    }
    
    /**
     * Method that sets the local properties needed by the serial port.
     * 
     * @param attributes
     *      Attributes object that contains the configuration properties needed by the serial port.
     */
    private void setProperties(final SerialPortProxyImplAttributes attributes)
    {
        m_BaudRate = attributes.baudRate();
        m_DataBits = attributes.dataBits();
        m_Parity = attributes.parity();
        m_FlowControl = attributes.flowControl();
        m_StopBits = attributes.stopBits();
        final int readTimeoutMs = attributes.readTimeoutMs();
        if (readTimeoutMs < 0)
        {
            m_ReadTimeoutMs = 0;
        }
        else if (readTimeoutMs == 0)
        {
            m_ReadTimeoutMs = 1;
        }
        else
        {
            m_ReadTimeoutMs = readTimeoutMs;
        }
    }
}
