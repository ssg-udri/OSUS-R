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
package edu.udayton.udri.asset.novatel.impl.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import edu.udayton.udri.asset.novatel.connection.NovatelConnectionMgr;
import edu.udayton.udri.asset.novatel.message.MessageReceiver;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;

/**
 * Test the {@link edu.udayton.udri.asset.novatel.message.MessageReader}.
 * @author allenchl
 *
 */
public class TestMessageReaderImpl
{
    private final String m_InsData = "#INSPVAA,COM1,0,48.5,FINESTEERING,1702,47.848,00000000,ddae,7053;"
            + "1702,221725.999755000,28.111270808,-80.624022245,-20.775880711,-63.661527253,39.651102732,"
            + "-0.000502600,-0.782552644,0.238876476,148.083596481,INS_SOLUTION_GOOD*b1967880";
    
    private MessageReaderImpl m_SUT;
    private NovatelConnectionMgr m_ConnectionManager;
    private MessageReceiver m_Receiver;
    
    /**
     * Setup for testing.
     */
    @Before
    public void setup()
    {
        m_ConnectionManager = mock(NovatelConnectionMgr.class);
        m_Receiver = mock(MessageReceiver.class);
        
        m_SUT = new MessageReaderImpl();
        m_SUT.setNovatelConnectionMgr(m_ConnectionManager);
    }
    
    /**
     * Cleanup, make sure that the read thread is stopped.
     */
    @After
    public void teardown()
    {
        try
        {
            m_SUT.stopRetrieving();
        }
        catch(IllegalStateException e)
        {
            //might not of been started
        }
    }
    
    /**
     * Test start read.
     */
    @Test
    public void testStartRead() throws InterruptedException, AssetException, IOException, ValidationFailedException
    {
        when(m_ConnectionManager.readMessage()).thenReturn(m_InsData);
        
        m_SUT.startRetreiving(m_Receiver);
        
        //verify
        verify(m_ConnectionManager, timeout(500).atLeast(1)).readMessage();
        verify(m_Receiver, timeout(500).atLeast(1)).handleDataString(m_InsData);
    }
    
    /**
     * Test start read illegal state if read has already been called.
     */
    @Test
    public void testStartReadIllegalState() throws InterruptedException, AssetException, 
        IOException
    {
        when(m_ConnectionManager.readMessage()).thenReturn(m_InsData);
        
        m_SUT.startRetreiving(m_Receiver);
        
        //try connecting again
        try
        {
            m_SUT.startRetreiving(m_Receiver);
            fail("Can't call start if thread is already started.");
        }
        catch (IllegalStateException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test start with null data string. Verify no exceptions
     */
    @Test
    public void testStartReadNullMessage() throws InterruptedException, AssetException, 
        IOException, ValidationFailedException
    {
        when(m_ConnectionManager.readMessage()).thenReturn(null);
        
        m_SUT.startRetreiving(m_Receiver);
        
        verify(m_Receiver, never()).handleDataString(null);
    }
    
    /**
     * Test stop read.
     */
    @Test
    public void testStopRead() throws InterruptedException, AssetException, 
        IOException, ValidationFailedException
    {
        when(m_ConnectionManager.readMessage()).thenReturn(m_InsData);
        
        m_SUT.startRetreiving(m_Receiver);
        
        //allow the thread to start
        Thread.sleep(10);
        
        //stop reading
        m_SUT.stopRetrieving();
        
        //verify messages were read, this will all reset the mock verifier
        verify(m_ConnectionManager, atLeast(1)).readMessage();
        verify(m_Receiver, atLeast(1)).handleDataString(m_InsData);
        
        //allow the thread to stop
        Thread.sleep(50);
        
        //verify no further interactions
        verifyNoMoreInteractions(m_ConnectionManager);
        verifyNoMoreInteractions(m_Receiver);
    }
    
    /**
     * Test stop read with illegal state because the thread was never started.
     */
    @Test
    public void testStopReadIllegalState() throws InterruptedException, AssetException, IOException
    {
        when(m_ConnectionManager.readMessage()).thenReturn(m_InsData);
        
        m_SUT.startRetreiving(m_Receiver);
        //allow the thread to start
        Thread.sleep(10);
        //stop reading
        m_SUT.stopRetrieving();
        
        //try stopping again
        try
        {
            m_SUT.stopRetrieving();
            fail("Can't call stop if thread is already stopped.");
        }
        catch (IllegalStateException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test reconnect call. Verify error status. Reconnect call. Reinstatement status.
     */
    @Test
    public void testExceptionWithReconnect() throws InterruptedException, AssetException, IOException
    {
        when(m_ConnectionManager.readMessage()).thenThrow(new AssetException("blarg"));
        
        m_SUT.startRetreiving(m_Receiver);
        
        ArgumentCaptor<SummaryStatusEnum> statusCaptor = ArgumentCaptor.forClass(SummaryStatusEnum.class);
        ArgumentCaptor<String> desc = ArgumentCaptor.forClass(String.class);
        verify(m_Receiver, timeout(1300).atLeast(2)).handleReadError(statusCaptor.capture(), desc.capture());
        List<SummaryStatusEnum> list = statusCaptor.getAllValues();
        List<String> listDesc = desc.getAllValues();
        
        //verify status changes
        assertThat(list.size(), greaterThan(1));
        
        //verify reconnect attempt
        verify(m_ConnectionManager, atLeastOnce()).reconnect();
        
        //verify receiver error string
        assertThat(list, hasItem(SummaryStatusEnum.BAD));
        assertThat(listDesc, hasItem("blarg"));
    }
    
    /**
     * Test reconnect call. Verify error status. Reconnect call. Additional error status.
     */
    @Test
    public void testExceptionWithReconnectException() throws InterruptedException, AssetException, IOException
    {
        when(m_ConnectionManager.readMessage()).thenThrow(new AssetException("blarg"));
        doThrow(new IllegalStateException("blarg TOOO")).when(m_ConnectionManager).reconnect();
        
        m_SUT.startRetreiving(m_Receiver);

        ArgumentCaptor<SummaryStatusEnum> statusCaptor = ArgumentCaptor.forClass(SummaryStatusEnum.class);
        ArgumentCaptor<String> desc = ArgumentCaptor.forClass(String.class);
        verify(m_Receiver, timeout(1300).atLeast(2)).handleReadError(statusCaptor.capture(), desc.capture());
        List<SummaryStatusEnum> list = statusCaptor.getAllValues();
        List<String> listDesc = desc.getAllValues();
        
        //verify status changes
        assertThat(list.size(), greaterThan(1));
        
        //verify reconnect attempt
        verify(m_ConnectionManager, atLeastOnce()).reconnect();
        
        //verify receiver error string
        assertThat(list, hasItem(SummaryStatusEnum.BAD));
        assertThat(listDesc, hasItem("blarg"));
    }
}   
