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

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import mil.dod.th.core.ccomm.physical.TimeoutException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.linux.gnu_c.FileOperations;
import mil.dod.th.ose.linux.gnu_c.FileOperationsConstants;
import mil.dod.th.ose.test.FinalStaticMocker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("mil.dod.th.ose.linux.gnu_c.FileOperationsConstants")
@PrepareForTest(FileOperations.class)
public class TestSerialPortInputStream
{
    private static final int FO_READ_TIMEOUT = -1;
    private static final int FO_READ_FAILURE = -2;
    private static final int FO_SELECT_FAILURE = -3;
    private static final int FO_INVALID_ARG = -4;
    
    private SerialPortInputStream m_SUT;

    @Before
    public void setUp() throws Exception
    {
        PowerMockito.mockStatic(FileOperations.class);
        
        m_SUT = new SerialPortInputStream(5);
        
        FinalStaticMocker.mockIt(FileOperationsConstants.class, "FO_READ_TIMEOUT", FO_READ_TIMEOUT);
        FinalStaticMocker.mockIt(FileOperationsConstants.class, "FO_READ_FAILURE", FO_READ_FAILURE);
        FinalStaticMocker.mockIt(FileOperationsConstants.class, "FO_SELECT_FAILURE", FO_SELECT_FAILURE);
        FinalStaticMocker.mockIt(FileOperationsConstants.class, "FO_INVALID_ARG", FO_INVALID_ARG);
    }
    
    /**
     * Verify read returns the byte from {@link FileOperations}.
     */
    @Test
    public void testRead() throws IOException, FactoryException
    {
        when(FileOperations.readBuffer(eq(5), Mockito.any(byte[].class), eq(0), eq(1L), eq(0)))
            .thenAnswer(new Answer<Integer>()
            {
                @Override
                public Integer answer(InvocationOnMock invocation) throws Throwable
                {
                    byte[] buf = (byte[])invocation.getArguments()[1];
                    buf[0] = 0x6f;
                    return 1;
                }
            });
        
        assertThat(m_SUT.read(), is(0x6f));
    }
    
    /**
     * Verify read buffer returns the buffer from {@link FileOperations}.
     */
    @Test
    public void testReadBytes() throws IOException, FactoryException
    {
        when(FileOperations.readBuffer(eq(5), Mockito.any(byte[].class), eq(3), eq(10L), eq(0)))
            .thenAnswer(new Answer<Integer>()
            {
                @Override
                public Integer answer(InvocationOnMock invocation) throws Throwable
                {
                    byte[] buf = (byte[])invocation.getArguments()[1];
                    buf[0] = (byte)0xde;
                    buf[1] = (byte)0xad;
                    buf[2] = (byte)0xbe;
                    buf[3] = (byte)0xef;
                    return 5;
                }
            });
        
        byte[] buf = new byte[10];
        int bytesRead = m_SUT.read(buf, 3, 10);
        assertThat(bytesRead, is(5));
        assertThat(buf[0], is((byte)0xde));
        assertThat(buf[1], is((byte)0xad));
        assertThat(buf[2], is((byte)0xbe));
        assertThat(buf[3], is((byte)0xef));
    }
    
    /**
     * Verify read timeout is passed on to {@link FileOperations}.
     */
    @Test
    public void testReadTimeoutValue() throws IOException, FactoryException
    {
        // mock to return value other than 1
        when(FileOperations.readBuffer(anyInt(), Mockito.any(byte[].class), anyInt(), anyLong(), anyInt()))
            .thenReturn(1);
        
        m_SUT.setReadTimeout(123);
        
        m_SUT.read();
        
        PowerMockito.verifyStatic();
        FileOperations.readBuffer(eq(5), Mockito.any(byte[].class), eq(0), eq(1L), eq(123));
        
        // check other read method that accepts a buffer
        byte[] buf = new byte[10];
        m_SUT.read(buf, 0, 10);
        
        PowerMockito.verifyStatic();
        FileOperations.readBuffer(eq(5), Mockito.any(byte[].class), eq(0), eq(10L), eq(123));
    }

    /**
     * Verify read timeout error is handled properly.
     */
    @Test
    public void testReadTimeoutException() throws IOException, FactoryException
    {
        when(FileOperations.readBuffer(anyInt(), Mockito.any(byte[].class), anyInt(), anyInt(), anyInt()))
            .thenReturn(FO_READ_TIMEOUT);
        
        try
        {
            m_SUT.read();
            fail("Expecting timeout");
        }
        catch (TimeoutException e)
        {
            
        }
    }
    
    /**
     * Verify read failure error is handled properly.
     */
    @Test
    public void testReadFailure() throws IOException, FactoryException
    {
        when(FileOperations.readBuffer(anyInt(), Mockito.any(byte[].class), anyInt(), anyInt(), anyInt()))
            .thenReturn(FO_READ_FAILURE);
        
        try
        {
            m_SUT.read();
            fail("Expecting read failure");
        }
        catch (IOException e)
        {
            
        }
    }
    
    /**
     * Verify select failure error is handled properly.
     */
    @Test
    public void testSelectFailure() throws IOException, FactoryException
    {
        when(FileOperations.readBuffer(anyInt(), Mockito.any(byte[].class), anyInt(), anyInt(), anyInt()))
            .thenReturn(FO_SELECT_FAILURE);
        
        try
        {
            m_SUT.read();
            fail("Expecting select failure");
        }
        catch (IOException e)
        {
            
        }
    }
    
    /**
     * Verify {@link IllegalStateException} is thrown if no data read (should either return error/negative or positive 
     * number).
     */
    @Test
    public void testReadStateException() throws IOException
    {
        // mock to return 0 bytes read, should never happen
        when(FileOperations.readBuffer(anyInt(), Mockito.any(byte[].class), anyInt(), anyLong(), anyInt()))
            .thenReturn(0);
        
        try
        {
            m_SUT.read();
            fail("Expecting exception");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
    
    /**
     * Verify invalid arg error is handled properly.
     */
    @Test
    public void testInvalidArg() throws IOException, FactoryException
    {
        when(FileOperations.readBuffer(anyInt(), Mockito.any(byte[].class), anyInt(), anyInt(), anyInt()))
            .thenReturn(FO_INVALID_ARG);
        
        try
        {
            m_SUT.read();
            fail("Expecting invalid arg");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
}
