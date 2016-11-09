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
package mil.dod.th.ose.rxtxtty;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;

import mil.dod.th.core.ccomm.physical.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for the serial port input stream wrapper.
 * @author allenchl
 *
 */
public class TestRxtxInputStreamWrapper
{
    private RxtxInputStreamWrapper m_SUT;
    private InputStream m_In;
    
    @Before
    public void setup()
    {
        m_In = mock(InputStream.class);
        m_SUT = new RxtxInputStreamWrapper(m_In);
    }
    
    /**
     * Verify exception is throw if 0 is returned.
     */
    @Test
    public void testReturnZero() throws IOException
    {
        byte[] bytes = new byte[3];
        when(m_In.read(bytes, 0, 3)).thenReturn(0);
        
        try
        {
            m_SUT.read(bytes);
            fail("expecting exception, 0 should throw an exception.");
        }
        catch(TimeoutException e)
        {
            //expected
        }
    }
    
    /**
     * Verify that a return value of -1 does not throw an exception.
     */
    @Test
    public void testReturnNegOne() throws IOException
    {
        byte[] bytes = new byte[0];
        when(m_In.read(bytes, 0, 0)).thenReturn(-1);
        
        m_SUT.read(bytes);
        verify(m_In).read(bytes, 0, 0);
    }
    
    /**
     * Verify that a return value of non-zero does not throw an exception.
     */
    @Test
    public void testReturnNotZeroOrNegOne() throws IOException
    {
        byte[] bytes = new byte[55];
        when(m_In.read(bytes, 0, 55)).thenReturn(55);
        
        m_SUT.read(bytes);
        verify(m_In).read(bytes, 0, 55);
    }
    
    /**
     * Verify that a read calls the buffer read of the input stream.
     */
    @Test
    public void testRead() throws IOException
    {
        when(m_In.read(Mockito.any(byte[].class), eq(0), eq(1))).thenReturn(55);
        
        m_SUT.read();
        verify(m_In).read(Mockito.any(byte[].class), eq(0), eq(1));
    }
}
