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
package mil.dod.th.ose.remote;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.utils.ServerSocketFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;

/**
 * Test that the component can accept multiple connections from a configured port. Make sure that server sockets created
 * listen to each connected socket on a separate thread.
 * @author Dave Humeniuk
 *
 */
public class TestServerSocketMessageListener
{
    private ServerSocketMessageListener m_SUT;
    private ServerSocketFactory m_Factory;
    private ServerSocket m_ServerSocket;
    private Set<Socket> m_Sockets = new HashSet<Socket>();
    private RemoteChannelLookup m_RemoteChannelLookup;
    
    // used to queue mocked accept method to throw exception
    private Semaphore m_CloseSocketSem = new Semaphore(0);
    private LoggingService m_Logging;

    @Before
    public void setUp() throws Exception
    {
        // basic mocking of SUT and binding services
        m_SUT = new ServerSocketMessageListener();
        m_Factory = mock(ServerSocketFactory.class);
        m_SUT.setServerSocketFactory(m_Factory);
        
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
        
        m_RemoteChannelLookup = mock(RemoteChannelLookup.class);
        m_SUT.setRemoteChannelLookup(m_RemoteChannelLookup);
        
        // mock server socket factory to create the correct server socket
        m_ServerSocket = mock(ServerSocket.class);
        when(m_Factory.createServerSocket(anyInt())).thenReturn(m_ServerSocket);
        
        // mock the server accept method to periodically return a socket
        when(m_ServerSocket.accept()).thenAnswer(new Answer<Socket>()
        {
            @SuppressWarnings("serial")
            @Override
            public Socket answer(InvocationOnMock invocation) throws Throwable
            {
                // simulated sleeps for accepting connections, initial sleep  is short for first connection
                if (m_Sockets.size() == 0)
                {
                    Thread.sleep(100);
                }
                else
                {
                    // semaphore is released when socket is closed, must throw exception when that happens
                    if (m_CloseSocketSem.tryAcquire(600, TimeUnit.MILLISECONDS))
                    {
                        throw new SocketException();
                    }
                }
                
                Socket socket = mock(Socket.class);
                when(socket.getRemoteSocketAddress()).thenReturn(new SocketAddress()
                {
                    @Override
                    public String toString()
                    {
                        return String.format("%d", m_Sockets.size());
                    }
                });
                m_Sockets.add(socket);
                return socket;
            }
        });
        
        // mock close method to trigger accept to behave differently
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                when(m_ServerSocket.isClosed()).thenReturn(true);
                m_CloseSocketSem.release();
                return null;
            }
        }).when(m_ServerSocket).close();
    }

    /**
     * Verify the socket server starts up and accepts multiple connections.  For each accepted connection, verify a 
     * remote socket channel is created.
     */
    @Test
    public void testActivate() throws IOException, InterruptedException
    {
        BundleContext contextNoProp = mock(BundleContext.class);
        
        m_SUT.activate(contextNoProp);
        
        // verify server socket created for the correct port
        verify(m_Factory).createServerSocket(ServerSocketMessageListener.DEFAULT_PORT);
        
        // wait long enough to accept 2 sockets, verify at least that many have been accepted
        Thread.sleep(1500);
        verify(m_ServerSocket, atLeast(2)).accept();
        
        // verify a remote channel has been created for each accepted socket
        verify(m_RemoteChannelLookup, atLeast(2)).newServerSocketChannel(Mockito.any(Socket.class));
        
        // need to force thread to stop
        m_SUT.deactivate();
    }
    
    @Test
    public void testActivateOsgiPortProperty() throws IOException
    {
        BundleContext context = mock(BundleContext.class);
        
        when(context.getProperty(ServerSocketMessageListener.PORT_PROP_KEY)).thenReturn("4500");
        m_SUT.activate(context);
        
        verify(m_Factory).createServerSocket(4500);
    }

    /**
     * Verify socket stops listening on port and all child threads are stopped.
     */
    @Test
    public void testDeactivate() throws IOException, InterruptedException
    {
        BundleContext contextNoProp = mock(BundleContext.class);
        
        m_SUT.activate(contextNoProp);
        
        // wait long enough to accept 2 sockets
        Thread.sleep(1500);
        
        m_SUT.deactivate();
        
        // verify server socket stops listening
        verify(m_ServerSocket).close();
        
        // verify thread is no longer running, since accept is not longer being called
        // should be equal to the number of successful calls + the last failure
        int expectedCalls = m_Sockets.size() + 1;
        verify(m_ServerSocket, times(expectedCalls)).accept();
    }
}
