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
package mil.dod.th.ose.time.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import mil.dod.th.ose.time.service.util.ChangeSystemTimeWrapper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Tests thread which listens to a socket and alters the system time.
 * 
 * @author nickmarcucci
 *
 */
public class TestTimeChangeServiceThread
{
    private TimeChangeServiceThread m_SUT;
    private ServerSocket m_ServerSocket;
    private ChangeSystemTimeWrapper m_SysTimeWrapper; 
    
    @Before
    public void init() throws SocketException
    {
        m_ServerSocket = mock(ServerSocket.class);
        
        m_SysTimeWrapper = mock(ChangeSystemTimeWrapper.class);
        m_SUT = new TimeChangeServiceThread(m_ServerSocket, m_SysTimeWrapper);
        
        verify(m_ServerSocket).setSoTimeout(eq(10000));
    }
    
    /**
     * Verify that thread can be successfully run and a call to set the system time 
     * is made.
     */
    @Test
    public void testRun() throws IOException, InterruptedException
    {
        Socket client = mock(Socket.class);
        when(m_ServerSocket.accept()).thenReturn(client);
        
        long time = System.currentTimeMillis();
        
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(time);
        
        InputStream stream = new ByteArrayInputStream(buffer.array());
        when(client.getInputStream()).thenReturn(stream);
        
        //kick off runner
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        //give thread time to run
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(m_SysTimeWrapper, timeout(1000)).setSystemTime(captor.capture());
        
        m_SUT.stopRun();
        thread.join(5000);
        
        verify(client, atLeast(1)).setSoTimeout(eq(10000));
        
        assertThat(captor.getValue().longValue(), is(time));
    }
    
    /**
     * Verify that a heartbeat message does not cause the system time to be set.
     */
    @Test
    public void testRunHeartbeatMessage() throws IOException, InterruptedException
    {
        Socket client = mock(Socket.class);
        when(m_ServerSocket.accept()).thenReturn(client);
        
        long heartbeat = -22L;
        
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(heartbeat);
        
        InputStream stream = new ByteArrayInputStream(buffer.array());
        when(client.getInputStream()).thenReturn(stream);
        
        //kick off runner
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        //give thread time to run
        Thread.sleep(50);
        
        m_SUT.stopRun();
        thread.join(5000);
        
        verify(m_SysTimeWrapper, never()).setSystemTime(Mockito.anyLong());
    }
    
    /**
     * Verify if no input received socket timeout occurs and the system time is never set.
     */
    @Test
    public void testRunReadTimeout() throws IOException, InterruptedException
    {
        Socket client = mock(Socket.class);
        when(m_ServerSocket.accept()).thenReturn(client);
        
        InputStream stream = mock(InputStream.class);
        when(client.getInputStream()).thenReturn(stream);
        
        doThrow(SocketException.class).
            when(stream).read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt());
       
        //kick off runner
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        //give thread time to run
        Thread.sleep(100);
        
        m_SUT.stopRun();
        thread.join(5000);
        
        verify(m_SysTimeWrapper, never()).setSystemTime(Mockito.anyLong());        
    }
    
    /**
     * Verify if input stream closed, then the client is closed as well.
     */
    @Test
    public void testRunReadEndofInputStream() throws IOException, InterruptedException
    {
        Socket client = mock(Socket.class);
        when(m_ServerSocket.accept()).thenReturn(client);
        
        InputStream stream = mock(InputStream.class);
        when(client.getInputStream()).thenReturn(stream);
        
        when(stream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenReturn(-1);
        
        //kick off runner
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        verify(stream, timeout(1000).atLeast(1)).close();
        
        m_SUT.stopRun();
        
        thread.join(5000);
        
        verify(m_SysTimeWrapper, never()).setSystemTime(Mockito.anyLong());
        verify(client, atLeast(1)).close();
    }
    
    /**
     * Verify that an exception with opening a socket's input stream 
     * cause the code to go back to listen for a new client and closes 
     * the client connection.
     */
    @Test
    public void testRunInputStreamException() throws IOException, InterruptedException
    {
        Socket client = mock(Socket.class);
        when(m_ServerSocket.accept()).thenReturn(client);
        
        doThrow(IOException.class).when(client).getInputStream();
        
        //kick off runner
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        //give thread time to run
        verify(m_ServerSocket, timeout(1000).atLeast(2)).accept();
        verify(client, atLeast(1)).close();
        verify(m_SysTimeWrapper, never()).setSystemTime(Mockito.anyLong());
        
        m_SUT.stopRun();
        
        thread.join(5000);
    }
    
    /**
     * Verify that an exception with the server socket receiving client connections
     * is handled properly.
     */
    @Test
    public void testRunServerAcceptException() throws IOException, InterruptedException
    {
        doThrow(IOException.class).when(m_ServerSocket).accept();
        
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        verify(m_ServerSocket, timeout(2000).atLeast(2)).accept();
        
        m_SUT.stopRun();
        thread.join(5000);
        
        verify(m_SysTimeWrapper, never()).setSystemTime(Mockito.anyLong());
    }
    
    /**
     * Verify server socket and client connections are closed properly.
     */
    @Test
    public void testStopRun() throws IOException, InterruptedException
    {
        Socket client = mock(Socket.class);
        when(m_ServerSocket.accept()).thenReturn(client);
        
        long time = System.currentTimeMillis();
        
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(time);
        
        InputStream stream = new ByteArrayInputStream(buffer.array());
        when(client.getInputStream()).thenReturn(stream);
        
        //kick off runner
        Thread thread = new Thread(m_SUT);
        thread.start();
        
        Thread.sleep(50);
        
        m_SUT.stopRun();
        
        thread.join(5000);
        
        verify(client, atLeast(1)).close();
        verify(m_ServerSocket).close();
    }
    
    /**
     * Verify stop called and an exception is properly handled.
     */
    @Test
    public void testStopRunThrowException() throws IOException
    {
        doThrow(IOException.class).when(m_ServerSocket).close();
        
        m_SUT.stopRun();
        verify(m_ServerSocket).close();       
    }   
}
