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
package edu.udayton.udri.asset.novatel.impl.timechanger;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import edu.udayton.udri.asset.novatel.StatusHandler;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.utils.ClientSocketFactory;

/**
 * Class to test the TimeChange interface.
 * 
 * @author nickmarcucci
 *
 */
public class TestTimeChangeImpl
{
    private static final int PORT = 4444;
    
    private TimeChangeImpl m_SUT;
    private ClientSocketFactory m_ClientSocketFactory; 
    private Socket m_ClientSocket;
    private OutputStream m_Stream;
    private StatusHandler m_StatusHandler;
    
    @Before 
    public void init() throws IOException
    {
        m_StatusHandler = mock(StatusHandler.class);
        m_ClientSocketFactory = mock(ClientSocketFactory.class);
        
        m_ClientSocket = mock(Socket.class);
        m_Stream = mock(OutputStream.class);
        
        when(m_ClientSocket.getOutputStream()).thenReturn(m_Stream);
        
        when(m_ClientSocketFactory.createClientSocket(Mockito.anyString(), 
                Mockito.anyInt())).thenReturn(m_ClientSocket);
        m_SUT = new TimeChangeImpl();
        
        m_SUT.setClientSocketFactory(m_ClientSocketFactory);
    }
    
    /**
     * Verify connection can be set up.
     */
    @Test
    public void testConnectTimeService() throws IOException, InterruptedException
    {
        m_SUT.connectTimeService(m_StatusHandler, PORT);
        
        ArgumentCaptor<ComponentStatus> statusCaptor = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_StatusHandler, timeout(1000).atLeast(1)).handleStatusUpdate(statusCaptor.capture());
        
        m_SUT.disconnectTimeService();
        
        verify(m_ClientSocketFactory, atLeast(1)).createClientSocket(Mockito.anyString(), Mockito.anyInt());
        
        assertThat(statusCaptor.getValue(), notNullValue());
        
        ComponentStatus status = statusCaptor.getValue();
        assertThat(status.getComponent().getType(), is(ComponentTypeEnum.SOFTWARE_UNIT));
        assertThat(status.getComponent().getDescription(), is("Time Service"));
        assertThat(status.getStatus().getSummary(), is(SummaryStatusEnum.GOOD));
    }
    
    /**
     * Verify that a connection that is already running throws an illegal state exception.
     */
    @Test
    public void testConnectTimeServiceAlreadyRunning() throws IOException, InterruptedException
    {
        m_SUT.connectTimeService(m_StatusHandler, PORT);
        
        verify(m_ClientSocketFactory, timeout(1000).atLeast(1))
            .createClientSocket(Mockito.anyString(), Mockito.anyInt());
        
        ArgumentCaptor<ComponentStatus> statusCaptor = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_StatusHandler, timeout(1000).atLeast(1)).handleStatusUpdate(statusCaptor.capture());
        
        try
        {
            m_SUT.connectTimeService(m_StatusHandler, PORT);
            fail("Expecting an illegal state exception.");
        }
        catch (IllegalStateException exception)
        {
            //expecting an exception.
        }
        
        m_SUT.disconnectTimeService();
        
        //status should still be good because we are connected to the service
        assertThat(statusCaptor.getValue(), notNullValue());
        
        ComponentStatus status = statusCaptor.getValue();
        
        assertThat(status.getComponent().getType(), is(ComponentTypeEnum.SOFTWARE_UNIT));
        assertThat(status.getComponent().getDescription(), is("Time Service"));
        assertThat(status.getStatus().getSummary(), is(SummaryStatusEnum.GOOD));
    }
    
    /**
     * Verify that when exception occurs on connection that fails
     */
    @Test
    public void testConnectTimeServiceIOException() throws IOException, InterruptedException
    {
        doThrow(new IOException("error")).when(m_ClientSocketFactory).createClientSocket(
                Mockito.anyString(), Mockito.anyInt());
        
        m_SUT.connectTimeService(m_StatusHandler, PORT);
        
        verify(m_ClientSocketFactory, timeout(100).atLeast(1))
            .createClientSocket(Mockito.anyString(), Mockito.anyInt());
        
        m_SUT.disconnectTimeService();
        
        ArgumentCaptor<ComponentStatus> statusCaptor = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_StatusHandler, atLeast(2)).handleStatusUpdate(statusCaptor.capture());

        List<ComponentStatus> status = statusCaptor.getAllValues();
        boolean foundBad = false;
        boolean foundOff = false;
        for (ComponentStatus aStatus : status)
        {
            assertThat(aStatus.getComponent().getType(), is(ComponentTypeEnum.SOFTWARE_UNIT));
            assertThat(aStatus.getComponent().getDescription(), is("Time Service"));

            if (aStatus.getStatus().getSummary() == SummaryStatusEnum.BAD)
            {
                assertThat(aStatus.getStatus().getDescription(), is("error"));
                foundBad = true;
            }
            else if (aStatus.getStatus().getSummary() == SummaryStatusEnum.OFF)
            {
                assertThat(aStatus.getStatus().getDescription(), is("Time service is now off."));
                foundOff = true;
            }
        }
        assertThat(foundBad, is(true));
        assertThat(foundOff, is(true));
    }
    
    /**
     * Verify that disconnect closes the socket
     */
    @Test
    public void testDisconnect() throws IOException, InterruptedException
    {
        m_SUT.connectTimeService(m_StatusHandler, PORT);
        
        verify(m_ClientSocketFactory, timeout(1000))
            .createClientSocket(Mockito.anyString(), eq(PORT));
        m_SUT.disconnectTimeService();
        
        verify(m_ClientSocket).close();
        
        ArgumentCaptor<ComponentStatus> statusCaptor = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_StatusHandler, timeout(1000).atLeast(1)).handleStatusUpdate(statusCaptor.capture());
        
        ComponentStatus status = statusCaptor.getValue();
        
        assertThat(status.getComponent().getType(), is(ComponentTypeEnum.SOFTWARE_UNIT));
        assertThat(status.getComponent().getDescription(), is("Time Service"));
        assertThat(status.getStatus().getSummary(), is(SummaryStatusEnum.OFF));
    }
    
    /**
     * Verify that an exception is thrown when disconnect is called before a connect.
     */
    @Test
    public void testDisconnectIllegalStateException() throws IOException, InterruptedException
    {
        try
        {
            m_SUT.disconnectTimeService();
            fail("Expected illegal state exception");
        }
        catch (IllegalStateException exception)
        {
            //exception is expected.
        }
    }
    
    /**
     * Verify proper exceptions are thrown when trying to disconnect.
     */
    @Test
    public void testDisconnectIOException() throws IOException, InterruptedException
    {
        doThrow(IOException.class).when(m_ClientSocket).close();
        
        try
        {
            m_SUT.connectTimeService(m_StatusHandler, PORT);
            Thread.sleep(1000);
            m_SUT.disconnectTimeService();
            fail("Expected io exception");
        }
        catch (IOException exception)
        {
            //exception is expected.
        }
    }
    
    /**
     * Verify that the time can be changed when called in the proper order.
     */
    @Test
    public void testChangeTime() throws InterruptedException, AssetException, IllegalStateException, IOException
    {
        m_SUT.connectTimeService(m_StatusHandler, PORT);
        
        long time = System.currentTimeMillis();
        Thread.sleep(1000);
        
        m_SUT.changeTime(time);
        
        long secondTime = System.currentTimeMillis();
        m_SUT.changeTime(secondTime);
        
        ArgumentCaptor<byte[]> byteCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_Stream, timeout(1000)).write(byteCaptor.capture());
        
        assertThat(byteCaptor.getValue(), notNullValue());
        
        ByteBuffer buffer = ByteBuffer.wrap(byteCaptor.getValue());
        assertThat(time, is(buffer.getLong()));
    }
    
    /**
     * Verify that a call to time change when not connected throws an AssetException
     */
    @Test
    public void testChangeTimeFailNotConnectedFirst() throws AssetException, InterruptedException, IOException
    {
        try
        {
            m_SUT.changeTime(1000);
            fail("Expecting an asset exception");
        }
        catch (AssetException exception)
        {
            //expecting this exception
        }
    }
    
    /**
     * Verify that when an IOException is thrown in change time it is properly handled.
     */
    @Test
    public void testChangeTimeIOException() throws IOException, InterruptedException, AssetException
    {
        when(m_ClientSocket.getOutputStream()).thenReturn(null);
        m_SUT.connectTimeService(m_StatusHandler, PORT);
        
        long time = System.currentTimeMillis() - 1000;
        m_SUT.changeTime(time);
        
        ArgumentCaptor<ComponentStatus> statusCaptor = ArgumentCaptor.forClass(ComponentStatus.class);
        
        verify(m_StatusHandler, timeout(1000)
                .atLeast(2)).handleStatusUpdate(statusCaptor.capture());
        
        m_SUT.disconnectTimeService();
        
        verify(m_Stream, never()).write(Mockito.any(byte[].class));
        
        List<ComponentStatus> list = statusCaptor.getAllValues();
        
        boolean found = false;
        for(ComponentStatus status : list)
        {
            if (status.getStatus().getSummary() == SummaryStatusEnum.BAD)
            {
                assertThat(status.getComponent().getType(), is(ComponentTypeEnum.SOFTWARE_UNIT));
                assertThat(status.getComponent().getDescription(), is("Time Service"));
                assertThat(status.getStatus().getSummary(), is(SummaryStatusEnum.BAD));
                
                found = true;
            }
        }
        assertThat(found, is(true));
    }
    
    /**
     * Verify that a heartbeat signal will be sent if no data is received to change the system time.
     */
    @Test
    public void testHeartbeatSignal() throws InterruptedException, IllegalStateException, IOException
    {
        m_SUT.connectTimeService(m_StatusHandler, PORT);
        
        //Wait up to 6 seconds to allow the poll for time data to timeout so that heartbeat
        //message will be sent.
        ArgumentCaptor<byte[]> byteCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(m_Stream, timeout(6000).atLeast(1)).write(byteCaptor.capture());
        
        m_SUT.disconnectTimeService();
        
        assertThat(byteCaptor.getValue(), notNullValue());
        
        ByteBuffer buffer = ByteBuffer.wrap(byteCaptor.getValue());
        assertThat(-22L, is(buffer.getLong()));
    }
}
