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
package edu.udayton.udri.asset.novatel.impl.connection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.SerialPort;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

import org.junit.Before;
import org.junit.Test;

import edu.udayton.udri.asset.novatel.NovatelConstants;

/**
 * Test class for the {@link NovatelConnectionMgrImpl} class.
 * 
 * @author cweisenborn
 */
public class TestNovatelConnectionMgrImpl
{
    private NovatelConnectionMgrImpl m_SUT;
    private CustomCommsService m_CustomCommService;
    private SerialPort m_SerialPort;

    private String m_PhysPort = "COM1";
    private int m_BaudRate = 115200;
    private UUID m_UUID = UUID.randomUUID();
    
    @Before
    public void setup()
    {
        m_CustomCommService = mock(CustomCommsService.class);
        m_SerialPort = mock(SerialPort.class);
        
        m_SUT = new NovatelConnectionMgrImpl();
        
        m_SUT.setCustomCommsService(m_CustomCommService);
    }
    
    /**
     * Test the start processing method.
     * Verify that read thread is initialized and that data is posted to the event admin.
     */
    @Test
    public void testStartProcessingAndReadMessage() throws IllegalArgumentException, CCommException, 
        PhysicalLinkException, AssetException, InterruptedException, IOException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        final String novatelTimeString = "#TIMEA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;VALID," 
                + "-3.432839221e-09,6.603391298e-08,-16.00000000000,2013,11,6,15,40,51000,VALID*10581099";
        final String novatelPVAString = "#INSPVAA,COM1,0,31.0,FINESTEERING,1264,144088.000,00040000,5615,1541'1264," 
                + "144088.002284950,51.116827527,-114.037738908,401.191547167,354.846489850,108.429407241," 
                + "-10.837482850,1.116219952,-3.476059035,7.372686190,INS_ALIGNMENT_COMPLETE*af719fd9";

        // the input stream
        InputStream inStream = new InputStream()
        {
            private char[] chars = (novatelPVAString + "\r\n" + novatelTimeString + "\r\n").toCharArray();
            private int m_Index = 0;
            private boolean read = false;

            @Override
            public int read() throws IOException
            {
                if (m_Index < chars.length)
                {
                    int toReturn = chars[m_Index];
                    m_Index++;
                    return toReturn;
                }
                read = true;
                return -1;
            }
            
            @Override
            public int available() throws IOException
            {
                if (!read)
                {
                    return chars.length;
                }
                return 0;
            }
        };
        when(m_SerialPort.getInputStream()).thenReturn(inStream);
        
        m_SUT.startProcessing(m_PhysPort, m_BaudRate);
        
        String message = m_SUT.readMessage();
        assertThat(message, is(novatelPVAString));
        message = m_SUT.readMessage();
        assertThat(message, is(novatelTimeString));
        message = m_SUT.readMessage();
        assertThat(message, is(nullValue()));
    }
    
    /**
     * Verify that the start processing method throws an illegal state if start has already been called.
     */
    @Test
    public void testStartProcessingFailStartAgain() throws IllegalArgumentException, CCommException, 
        PhysicalLinkException, AssetException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getInputStream()).thenReturn(mock(InputStream.class));
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        
        m_SUT.startProcessing(m_PhysPort, m_BaudRate);
        
        //simulating a working port
        when(m_SerialPort.isOpen()).thenReturn(true);
        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("Expected exception because the port is already connected,");
        }
        catch (final IllegalStateException exception)
        {
            //expected exception
        }
    }
    
    /**
     * Verify that the stop processing method throws an illegal state if start has already been called.
     */
    @Test
    public void testStartProcessingFailStopAgain() throws IllegalArgumentException, CCommException, 
        PhysicalLinkException, AssetException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getInputStream()).thenReturn(mock(InputStream.class));
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        
        m_SUT.startProcessing(m_PhysPort, m_BaudRate);
        
        //simulating a working port
        when(m_SerialPort.isOpen()).thenReturn(true);
        
        m_SUT.stopProcessing();

        try
        {
            m_SUT.stopProcessing();
            fail("Expected exception because the connection is already stopped,");
        }
        catch (final IllegalStateException exception)
        {
            //expected exception
        }
    }
    
    /**
     * Verify that the start processing method throws an asset exception if input stream cannot be retrieved from the
     * serial port.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testStartProcessingFailGetInputStream() throws IllegalArgumentException, CCommException, 
        PhysicalLinkException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        when(m_SerialPort.getInputStream()).thenThrow(PhysicalLinkException.class);
        
        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("Read thread should throw a physical link exception that will be caught and throw as an asset " 
                + "exception.");
        }
        catch (final AssetException exception)
        {
            //expected exception
        }
        verify(m_SerialPort).close();
        verify(m_SerialPort).release();
    }
    
    /**
     * Verify the start processing method throws an asset exception if the serial port fails to open or to be able
     * to set the baud rate.
     */
    @Test
    public void testStartProcessingFailOpenSerial() throws IllegalArgumentException, CCommException, 
        PhysicalLinkException, IllegalStateException, FactoryException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        
        //Verify that handling of the open method if it throws an exception.
        doThrow(PhysicalLinkException.class).when(m_SerialPort).open();
        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("Serial port open method should throw a physical link exception that will be caught and wrapped in an" 
                    + " asset exception.");
        }
        catch (final AssetException exception)
        {
            //expected exception
        }
        verify(m_SerialPort).close();
        verify(m_SerialPort).release();
        
        //Verify that handling of the set property method if it throws an exception.
        doThrow(PhysicalLinkException.class).when(m_SerialPort).setSerialPortProperties(m_BaudRate, 
                NovatelConstants.DEFAULT_NOVATEL_DATA_BITS, NovatelConstants.DEFAULT_NOVATEL_PARITY, 
                NovatelConstants.DEFAULT_NOVATEL_STOP_BITS, NovatelConstants.DEFAULT_NOVATEL_FLOW_CONTROL);
        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("Seral port set property method should throw a factory exception that will be caught and wrapped in an"
                    + " asset exception.");
        }
        catch (final AssetException exception)
        {
            //expected exception
        }
        verify(m_SerialPort, times(2)).release();
    }
    
    /**
     * Verify the start processing method throws an asset exception if the custom comms service returns null for the 
     * serial port after being created. 
     */
    @Test
    public void testStartProcessingSerialPortPropsFail() throws FactoryException, IllegalArgumentException, 
        CCommException, PhysicalLinkException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        doThrow(FactoryException.class).when(m_SerialPort).setReadTimeout(1000);

        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("Should throw asset exception if serial port fails to set read timeout.");
        }
        catch (final AssetException exception)
        {
            //expected exception
        }
        //verify cleanup
        verify(m_SerialPort).release();
        
        doThrow(IllegalArgumentException.class).when(m_SerialPort).setReadTimeout(1000);
        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("Should throw asset exception if invalid read tiemout is passed to the serial port.");
        }
        catch (final AssetException exception)
        {
            //expected exception
        }
        //verify cleanup
        verify(m_SerialPort, times(2)).release();
        
        doThrow(PhysicalLinkException.class).when(m_SerialPort).setSerialPortProperties(m_BaudRate, 
                NovatelConstants.DEFAULT_NOVATEL_DATA_BITS, 
                NovatelConstants.DEFAULT_NOVATEL_PARITY, 
                NovatelConstants.DEFAULT_NOVATEL_STOP_BITS, 
                NovatelConstants.DEFAULT_NOVATEL_FLOW_CONTROL);
        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("Should throw asset exception if serial port properties cannot be set.");
        }
        catch (final AssetException exception)
        {
            //expected exception
        }
        
        //verify cleanup
        verify(m_SerialPort, times(3)).release();
    }
    
    /**
     * Verify the start processing method throws an asset exception if the serial port cannot be created or retrieved
     * from the custom comms service.
     */
    @Test
    public void testStartProcessingFailCreateLink() throws IllegalArgumentException, CCommException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        
        doThrow(IllegalStateException.class).when(m_CustomCommService).requestPhysicalLink(m_UUID);
        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("CustomCommsService request physical link method should throw an illegal state exception that will be "
                    + "caught and wrapped in an asset exception.");
        }
        catch (final AssetException exception)
        {
            //expected exception
        }
        
        doThrow(CCommException.class).when(m_CustomCommService).tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, 
                m_PhysPort);
        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("CustomCommsService try create physical link method should throw an ccomm exception that will be "
                    + "caught and wrapped in an asset exception.");
        }
        catch (final AssetException exception)
        {
            //expected exception
        }
    }
    
    /**
     * Verify the start processing method throws an asset exception if the serial port cannot be found.
     */
    @Test
    public void testStartProcessingNoLinkFound() throws IllegalArgumentException, CCommException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        
        doThrow(IllegalArgumentException.class).when(m_CustomCommService).requestPhysicalLink(m_UUID);
        try
        {
            m_SUT.startProcessing(m_PhysPort, m_BaudRate);
            fail("Expected exception");
        }
        catch (final AssetException exception)
        {
            //expected exception
        }
    }

    /**
     * Verify that the stop processing method calls appropriate methods to close and release the serial port.
     */
    @Test
    public void testStopProcessing() throws IllegalArgumentException, CCommException, PhysicalLinkException, 
        AssetException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getInputStream()).thenReturn(mock(InputStream.class));
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        
        //need to call start processing to set up serial port and read thread
        m_SUT.startProcessing(m_PhysPort, m_BaudRate);
        when(m_SerialPort.isOpen()).thenReturn(true);
        
        m_SUT.stopProcessing();
        
        verify(m_SerialPort).close();
        verify(m_SerialPort).release();
    }
    
    /**
     * Verify error handling if IOException is thrown during close.
     */
    @Test
    public void testStopProcessingCloseIOException() throws IllegalArgumentException, CCommException, 
        PhysicalLinkException, AssetException
    {
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getInputStream()).thenReturn(mock(InputStream.class));
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        
        //need to call start processing to set up serial port and read thread
        m_SUT.startProcessing(m_PhysPort, m_BaudRate);
        when(m_SerialPort.isOpen()).thenReturn(true);
        
        //exception from serial port
        doThrow(new PhysicalLinkException("exception")).when(m_SerialPort).close();
        m_SUT.stopProcessing();
        
        //verify release still called
        verify(m_SerialPort).release();
    }
    
    /**
     * Test the is processing method.
     * Verify the correct boolean value is returned for the various states.
     */
    @Test
    public void testIsProcessing() throws IllegalArgumentException, CCommException, AssetException, 
        PhysicalLinkException
    {
        //Not activated therefore should return false.
        assertThat(m_SUT.isProcessing(), is(false));
        
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getInputStream()).thenReturn(mock(InputStream.class));
        when(m_SerialPort.isOpen()).thenReturn(false);
        m_SUT.startProcessing(m_PhysPort, m_BaudRate);
        
        //Activated but serial is not open therefore should return false.
        assertThat(m_SUT.isProcessing(), is(false));
        
        when(m_SerialPort.isOpen()).thenReturn(true);
        
        //Activated but serial is open therefore should return true.
        assertThat(m_SUT.isProcessing(), is(true));
    }
    
    /**
     * Verify asset exception if the read is null.
     */
    @Test
    public void testNullReaderAssetException() throws PhysicalLinkException, IOException
    {
        try
        {
            m_SUT.readMessage();
            fail("Expected exception.");
        }
        catch (AssetException e)
        {
            //expected exceptionS
        }
    }
    
    /**
     * Test the reconnect method with open port, verify it is closed and released.
     */
    @Test
    public void testReconnect() throws IllegalArgumentException, CCommException, AssetException, 
        PhysicalLinkException, IOException
    {
        //Not activated therefore should return false.
        assertThat(m_SUT.isProcessing(), is(false));
        
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getInputStream()).thenReturn(mock(InputStream.class));
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        m_SUT.startProcessing(m_PhysPort, m_BaudRate);
        
        //report it as open so that close is called
        when(m_SerialPort.isOpen()).thenReturn(true);
        
        //reconnect should close the serial port
        m_SUT.reconnect();
        verify(m_SerialPort).close();
        verify(m_SerialPort).release();
    }
    
    /**
     * Test the reconnect method without open port, verify it is not closed and released.
     */
    @Test
    public void testReconnectWithoutOpen() throws IllegalArgumentException, CCommException, AssetException, 
        PhysicalLinkException, IOException
    {
        //Not activated therefore should return false.
        assertThat(m_SUT.isProcessing(), is(false));
        
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getInputStream()).thenReturn(mock(InputStream.class));
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        m_SUT.startProcessing(m_PhysPort, m_BaudRate);
        
        //reconnect should not close the serial port
        m_SUT.reconnect();
        verify(m_SerialPort, never()).close();
        verify(m_SerialPort, never()).release();
    }
    
    /**
     * Test the reconnect method with open port, verify it is closed and released.
     * Verify it can be re-acquired.
     */
    @Test
    public void testReconnectWithOpenCloseOpen() throws IllegalArgumentException, CCommException, AssetException, 
        PhysicalLinkException, IOException
    {
        InputStream inStream = new InputStream()
        {
            private char[] chars = ("Asd" + "\r\n" + "As" + "\r\n").toCharArray();
            private int m_Index = 0;
            private boolean read = false;

            @Override
            public int read() throws IOException
            {
                if (m_Index < chars.length)
                {
                    int toReturn = chars[m_Index];
                    m_Index++;
                    return toReturn;
                }
                read = true;
                return -1;
            }
            
            @Override
            public int available() throws IOException
            {
                if (!read)
                {
                    return chars.length;
                }
                return 0;
            }
        };
        
        //Not activated therefore should return false.
        assertThat(m_SUT.isProcessing(), is(false));
        
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, m_PhysPort)).
            thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getInputStream()).thenReturn(mock(InputStream.class), inStream);
        when(m_SerialPort.getName()).thenReturn(m_PhysPort);
        m_SUT.startProcessing(m_PhysPort, m_BaudRate);
        
        when(m_SerialPort.isOpen()).thenReturn(true, true, false);

        //reconnect should close the serial port
        m_SUT.reconnect();
        verify(m_SerialPort).close();
        verify(m_SerialPort).release();
        
        String message = m_SUT.readMessage();
        assertThat(message, is("Asd"));
    }
    
    /**
     * Test the Illegal state exception if reconnect is called and the baudrate and/or port are null.
     */
    @Test
    public void testNullPortBaudRateReconnect() throws AssetException, IOException, IllegalArgumentException, 
        CCommException, PhysicalLinkException
    {
        //try with nothing set... both are null
        try
        {
            m_SUT.reconnect();
            fail("Expecting exception.");
        }
        catch (IllegalStateException e)
        {
            //expected exception
        }
        //start processing call, but with name of the port being null
        when(m_CustomCommService.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, null)).thenReturn(m_UUID);
        when(m_CustomCommService.requestPhysicalLink(m_UUID)).thenReturn(m_SerialPort);
        when(m_SerialPort.getInputStream()).thenReturn(mock(InputStream.class));
        m_SUT.startProcessing(null, m_BaudRate);
        
        //try with port being null
        try
        {
            m_SUT.reconnect();
            fail("Expecting exception.");
        }
        catch (IllegalStateException e)
        {
            //expected exception
        }
    }
}
