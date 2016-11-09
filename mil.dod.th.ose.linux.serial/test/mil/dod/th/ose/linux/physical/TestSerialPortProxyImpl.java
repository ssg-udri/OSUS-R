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

package mil.dod.th.ose.linux.physical;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.ccomm.physical.PhysicalLinkContext;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.SerialPortAttributes;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.ccomm.FlowControlEnum;
import mil.dod.th.core.types.ccomm.ParityEnum;
import mil.dod.th.core.types.ccomm.StopBitsEnum;
import mil.dod.th.ose.linux.gnu_c.FileOperations;
import mil.dod.th.ose.linux.gnu_c.FileOperationsConstants;
import mil.dod.th.ose.linux.gnu_c.TerminalIO;
import mil.dod.th.ose.linux.gnu_c.TerminalIOConstants;
import mil.dod.th.ose.linux.loader.LinuxNativeLibraryLoader;
import mil.dod.th.ose.test.FactoryObjectContextMocker;
import mil.dod.th.ose.test.FinalStaticMocker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * @author dhumeniuk
 *
 */
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor({"mil.dod.th.ose.linux.gnu_c.FileOperationsConstants",
        "mil.dod.th.ose.linux.gnu_c.TerminalIOConstants"})
@PrepareForTest({SerialPortProxyImpl.class, FileOperations.class, TerminalIO.class, LinuxNativeLibraryLoader.class})
public class TestSerialPortProxyImpl
{
    private static final String PORT_A_NAME = "/dev/ttyS1";
    
    // flag bit values are not available to Java unit tests, make up values that will be mocked to be returned
    
    // dummy control flag bits
    private static final int CRTSCTS = 0x1; // HW flow control
    private static final int CLOCAL = 0x2;
    private static final int CREAD = 0x4;
    private static final int CS7 = 0x8; // 7 data bits
    private static final int CS8 = 0x10; // 8 data bits
    private static final int CSTOPB = 0x20; // 2 stop bits
    private static final int B9600 = 0x40; // 9600 baud
    private static final int B57600 = 0x80; // 57600 baud
    private static final int PARENB = 0x100; // parity enabled
    private static final int PARODD = 0x200; // odd parity
    
    // dummy input flag bits
    private static final int IXON = 0x1; // SW flow (X ON)
    private static final int IXOFF = 0x2; // SW flow (X OFF)
    
    // file op dummy flags
    private static final int O_RDWR = 0x1; // open file for read and write
    private static final int O_NOCTTY = 0x2;
    
    private SerialPortProxyImpl m_SUT;
    private PhysicalLinkContext m_Context;
    private Map<String, Object> m_Defaults = new HashMap<String, Object>();
    private SerialPortInputStream m_InputStream;
    private LoggingService m_LogService;

    @Before
    public void setUp() throws Exception
    {
        m_LogService = mock(LoggingService.class);
        m_InputStream = mock(SerialPortInputStream.class);
        PowerMockito.mockStatic(FileOperations.class, TerminalIOConstants.class, TerminalIO.class, 
                LinuxNativeLibraryLoader.class);
        PowerMockito.whenNew(SerialPortInputStream.class).withArguments(anyInt()).thenReturn(m_InputStream);
        
        PowerMockito.doNothing().when(LinuxNativeLibraryLoader.class, "load");
        
        m_SUT = new SerialPortProxyImpl();
        
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "CRTSCTS", CRTSCTS);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "CLOCAL", CLOCAL);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "CREAD", CREAD);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "CS7", CS7);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "CS8", CS8);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "CSTOPB", CSTOPB);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "B9600", B9600);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "B57600", B57600);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "IXON", IXON);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "IXOFF", IXOFF);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "PARENB", PARENB);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "PARODD", PARODD);
        FinalStaticMocker.mockIt(TerminalIOConstants.class, "NCCS", 5);
        
        FinalStaticMocker.mockIt(FileOperationsConstants.class, "O_RDWR", O_RDWR);
        FinalStaticMocker.mockIt(FileOperationsConstants.class, "O_NOCTTY", O_NOCTTY);
        
        // override using native baud rate constant so tests can be run outside of Linux
        Map<Integer, Integer> baudRates = new HashMap<Integer, Integer>();
        baudRates.put(9600, B9600);
        baudRates.put(57600, B57600);
        Whitebox.setInternalState(SerialPortProxyImpl.class, "BAUD_RATE_MASK_LOOKUP", baudRates);
        
        m_Defaults.put(SerialPortAttributes.CONFIG_PROP_DATA_BITS, 8);
        m_Defaults.put(SerialPortAttributes.CONFIG_PROP_BAUD_RATE, 9600);
        m_Defaults.put(SerialPortAttributes.CONFIG_PROP_PARITY, ParityEnum.NONE);
        m_Defaults.put(SerialPortAttributes.CONFIG_PROP_STOP_BITS, StopBitsEnum.ONE_STOP_BIT);
        m_Defaults.put(SerialPortAttributes.CONFIG_PROP_FLOW_CONTROL, FlowControlEnum.NONE);
        m_Defaults.put(SerialPortAttributes.CONFIG_PROP_READ_TIMEOUT_MS, 0);
        
        m_Context = FactoryObjectContextMocker.mockPhysicalLinkContext(PORT_A_NAME, m_Defaults);
        m_SUT.setLogService(m_LogService);
        m_SUT.initialize(m_Context, m_Defaults);
    }

    /**
     * Verify {@link FileOperations} is called on to open native file. 
     */
    @Test
    public void testOpen() 
        throws PhysicalLinkException
    {
        m_SUT.initialize(m_Context, m_Defaults);
        m_SUT.open();
        
        PowerMockito.verifyStatic();
        FileOperations.open(PORT_A_NAME, O_NOCTTY | O_RDWR);
    }
    
    @Test
    public void testOpenException()
    {
        when(FileOperations.open(anyString(), anyInt())).thenReturn(-1);
        
        try
        {
            m_SUT.open();
            fail("Expecting PhysicalLinkException");
        }
        catch (PhysicalLinkException e) { }
    }
    
    /**
     * Verify closing will call {@link FileOperations#close}.
     */
    @Test
    public void testClose() throws PhysicalLinkException
    {
        m_SUT.close();
        
        PowerMockito.verifyStatic();
        FileOperations.close(anyInt());
    }
            
    /**
     * Verify if {@link FileOperations} reports an error on close, an exception is thrown
     */
    @Test
    public void testCloseException()
    {               
        when(FileOperations.close(anyInt())).thenReturn(-1);
        m_SUT.close();
        
        verify(m_LogService).warning("Unable to close serial port: %s", PORT_A_NAME);
        PowerMockito.verifyStatic();
        FileOperations.close(anyInt());
    }
    
    /**
     * Verify exception if serial port is not open and input stream is accessed.
     * Verify ability to get the input stream.
     */
    @Test
    public void testGetInputStream() 
        throws PhysicalLinkException, IllegalArgumentException
    {
        try
        {
            m_SUT.getInputStream();
            fail("Expected PhysicalLinkException");
        }
        catch (PhysicalLinkException e) { }
        
        m_SUT.open();
        InputStream is = m_SUT.getInputStream();
        assertThat(is, is((InputStream)m_InputStream));
    }

    /**
     * Verify that serial port must be open in order to access the output stream.
     */
    @Test
    public void testGetOutputStream()
        throws PhysicalLinkException
    {
        try
        {
            m_SUT.getOutputStream();
            fail("Expected PhysicalLinkException");
        }
        catch (PhysicalLinkException e) { }
        
        m_SUT.open();
        OutputStream os = m_SUT.getOutputStream();
        assertThat(os, is(notNullValue()));
    }
    
    /**
     * Verify ability to set DTR of serial port.
     */
    @Test
    public void testSetDTR() 
        throws PhysicalLinkException
    {
        try
        {
            m_SUT.setDTR(false);
            fail("Expected exception");
        }
        catch (IllegalStateException e)
        {
            
        }
        
        m_SUT.open();
        
        m_SUT.setDTR(true);
        
        m_SUT.setDTR(false);
        
        m_SUT.close();
        
        try
        {
            m_SUT.setDTR(true);
            fail("Expected exception");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
    
    /**
     * Verify the input stream timeout will be set when opening the serial port.
     */
    @Test
    public void testSetTimeout() throws Exception
    {
        int timeout = 500;
        
        // set each port to the same setting so they can talk with each other
        m_Context = mockProperties(m_SUT, PORT_A_NAME, 57600, 8, ParityEnum.NONE, StopBitsEnum.ONE_STOP_BIT, 
            FlowControlEnum.NONE, timeout);
        
        m_SUT.open();
        
        verify(m_InputStream).setReadTimeout(timeout);
    }
    
    /**
     * Verify that if a negative timeout is specified the input stream timeout is set to 0. Linux does not except
     * negative numbers for input stream timeout.
     */
    @Test
    public void testSetTimeoutNegative() throws Exception
    {
        int timeout = -75;
        
        // set each port to the same setting so they can talk with each other
        m_Context = mockProperties(m_SUT, PORT_A_NAME, 57600, 8, ParityEnum.NONE, StopBitsEnum.ONE_STOP_BIT, 
            FlowControlEnum.NONE, timeout);
        
        m_SUT.open();
        
        verify(m_InputStream).setReadTimeout(0);
    }
    
    /**
     * Verify that if a 0 timeout is specified the input stream timeout is set to 1 millisecond. 0 on linux sets the
     * input stream to read indefinitely whereas 0 for OSUS signifies to timeout immediately.
     */
    @Test
    public void testSetTimeoutZero() throws Exception
    {
        int timeout = 0;
        
        // set each port to the same setting so they can talk with each other
        m_Context = mockProperties(m_SUT, PORT_A_NAME, 57600, 8, ParityEnum.NONE, StopBitsEnum.ONE_STOP_BIT, 
            FlowControlEnum.NONE, timeout);
        
        m_SUT.open();
        
        verify(m_InputStream).setReadTimeout(1);
    }
    
    /**
     * Verify ability to set properties, that the property flag values are sent to native call.
     */
    @Test
    public void testSetProperties() throws PhysicalLinkException
    {   
        ArgumentCaptor<Long> actualCFlag = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> actualIFlag = ArgumentCaptor.forClass(Long.class);
        
        // set each port to the same setting so they can talk with each other
        m_Context = mockProperties(m_SUT, PORT_A_NAME, 57600, 8, ParityEnum.NONE, StopBitsEnum.ONE_STOP_BIT, 
            FlowControlEnum.NONE, 0);
        
        m_SUT.open();
        
        PowerMockito.verifyStatic();
        int fd = 0;
        long c_iflag = 0;
        long c_oflag = 0;
        long c_cflag = B57600 | CLOCAL | CREAD | CS8; // CLOCAL and CREAD always apply
        long c_lflag = 0;
        // make sure waiting for at least one character
        short[] c_cc = new short[]{1, 0, 0, 0, 0}; 
        TerminalIO.tcSetAttr(eq(fd), actualIFlag.capture(), eq(c_oflag), actualCFlag.capture(), eq(c_lflag), eq(c_cc));
        assertThat(actualCFlag.getValue(), is(c_cflag));
        assertThat(actualIFlag.getValue(), is(c_iflag));
        
        // reduce to 7 data bits and use 2 stop bits
        m_Context = mockProperties(m_SUT, PORT_A_NAME, 9600, 7, ParityEnum.ODD, StopBitsEnum.TWO_STOP_BITS, 
            FlowControlEnum.XON_XOFF, 0);
        
        PowerMockito.verifyStatic(times(2));
        c_iflag = IXOFF | IXON;
        c_cflag = B9600 | CLOCAL | CREAD | CS7 | CSTOPB | PARODD | PARENB; // CLOCAL and CREAD always apply
        TerminalIO.tcSetAttr(eq(fd), actualIFlag.capture(), eq(c_oflag), actualCFlag.capture(), eq(c_lflag), eq(c_cc));
        assertThat(actualCFlag.getValue(), is(c_cflag));
        assertThat(actualIFlag.getValue(), is(c_iflag));
        
        // reduce to 7 data bits and use 2 stop bits
        m_Context = mockProperties(m_SUT, PORT_A_NAME, 9600, 8, ParityEnum.EVEN, StopBitsEnum.ONE_STOP_BIT, 
            FlowControlEnum.HARDWARE, 0);
        
        PowerMockito.verifyStatic(times(3));
        c_iflag = 0;
        c_cflag = B9600 | CLOCAL | CREAD | CS8 | PARENB | CRTSCTS; // CLOCAL and CREAD always apply
        TerminalIO.tcSetAttr(eq(fd), actualIFlag.capture(), eq(c_oflag), actualCFlag.capture(), eq(c_lflag), eq(c_cc));
        assertThat(actualCFlag.getValue(), is(c_cflag));
        assertThat(actualIFlag.getValue(), is(c_iflag));
    }
    
    private PhysicalLinkContext mockProperties(final SerialPortProxyImpl port, final String name, final int baudRate, 
        final int dataBits, final ParityEnum parity, final StopBitsEnum stopBits, 
        final FlowControlEnum flowControl, final int readTimeout)
    {
        final Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put(SerialPortAttributes.CONFIG_PROP_DATA_BITS, dataBits);
        defaults.put(SerialPortAttributes.CONFIG_PROP_BAUD_RATE, baudRate);
        defaults.put(SerialPortAttributes.CONFIG_PROP_PARITY, parity.toString());
        defaults.put(SerialPortAttributes.CONFIG_PROP_STOP_BITS, stopBits.toString());
        defaults.put(SerialPortAttributes.CONFIG_PROP_FLOW_CONTROL, flowControl.toString());
        defaults.put(SerialPortAttributes.CONFIG_PROP_READ_TIMEOUT_MS, readTimeout);
        
        final PhysicalLinkContext context = FactoryObjectContextMocker.mockPhysicalLinkContext(name, defaults);
        port.initialize(context, defaults);
        port.updated(defaults);
        return context;
    }
}
