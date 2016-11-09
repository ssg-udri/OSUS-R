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
package edu.udayton.udri.asset.axis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.asset.commands.GetPanTiltCommand;
import mil.dod.th.core.asset.commands.GetPanTiltResponse;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.asset.commands.SetPanTiltResponse;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.test.FactoryObjectContextMocker;
import mil.dod.th.ose.utils.UrlService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * Test the axis asset.
 * @author allenchl
 *
 */
public class TestAxisAsset
{
    private AxisAsset m_SUT;
    private CommandProcessor m_CommandProcessor;
    private UrlService m_UrlService;
    private AssetContext m_Context;
    private URLConnection m_Connection;
    private String m_Ip = "ip";
    
    //URL string to return
    private String m_UrlString = "something";
    
    @Before
    public void setUp() throws FactoryException
    {
        m_SUT = new AxisAsset();
        m_CommandProcessor = mock(CommandProcessor.class);
        m_UrlService = mock(UrlService.class);
        m_Connection = mock(URLConnection.class);
        
        Map<String, Object> configProps = new HashMap<String, Object>();
        configProps.put(AxisAssetAttributes.CONFIG_PROP_IP_ADDRESS, m_Ip);
        
        m_Context = FactoryObjectContextMocker.mockAssetContext("Axis", configProps);
        
        //set deps
        m_SUT.setCommandProcessor(m_CommandProcessor);
        m_SUT.setUrlService(m_UrlService);
        
        //Initialize asset.
        m_SUT.initialize(m_Context, configProps);
    }
    
    /**
     * Verify that the asset can be activated.
     */
    @Test
    public void testOnActivate() throws AssetException, IOException
    {
        //activate the asset
        m_SUT.onActivate();
        
        ArgumentCaptor<SummaryStatusEnum> statusCaptor = ArgumentCaptor.forClass(SummaryStatusEnum.class);
        ArgumentCaptor<String> descCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(m_Context).setStatus(statusCaptor.capture(), descCaptor.capture());
        
        assertThat(statusCaptor.getValue(), is(SummaryStatusEnum.GOOD));
        assertThat(descCaptor.getValue(), is("Asset Activated"));
    }
    
    /**
     * Verify that the asset can be deactivated.
     */
    @Test
    public void testOnDeactivate() throws AssetException, IOException, IllegalStateException, InterruptedException
    {
        m_SUT.onDeactivate();
        
        ArgumentCaptor<SummaryStatusEnum> statusCaptor = ArgumentCaptor.forClass(SummaryStatusEnum.class);
        ArgumentCaptor<String> descCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(m_Context).setStatus(statusCaptor.capture(), descCaptor.capture());

        assertThat(statusCaptor.getValue(), is(SummaryStatusEnum.OFF));
        assertThat(descCaptor.getValue(), is("Asset Deactivated"));
    }
    
    /**
     * Verify set pan/tilt command interactions.
     */
    @Test
    public void testSetPanTilt() throws AssetException, IOException, IllegalStateException, InterruptedException
    {
        //Command
        SetPanTiltCommand command = new SetPanTiltCommand().withPanTilt(
                SpatialTypesFactory.newOrientationOffset(0f, 0f));
        
        //mock behavior
        when(m_CommandProcessor.processSetPanTilt(command, m_Ip)).thenReturn(m_UrlString);
        when(m_UrlService.constructUrlConnection(m_UrlString)).thenReturn(m_Connection);
        when(m_Connection.getDate()).thenReturn(1L);
        
        //process the command
        SetPanTiltResponse response = (SetPanTiltResponse)m_SUT.onExecuteCommand(command);
        
        verify(m_CommandProcessor).processSetPanTilt(command, m_Ip);
        verify(m_UrlService).constructUrlConnection(m_UrlString);
        assertThat(response, is(notNullValue()));
    }
    
    /**
     * Verify get pan/tilt command interactions.
     */
    @Test
    public void testGetPanTilt() throws AssetException, IOException, IllegalStateException, InterruptedException
    {
        final char[] responseBytes = 
                new char[]{'p', 'a', 'n', '=', '4', '.', '0', '\n', 't', 'i', 'l', 't', '=', '8','.', '1'};
        
        InputStream stream = new InputStream()
        {
            private int m_Counter;
            @Override
            public int read() throws IOException
            {
                int toReturn = -1;
                if (m_Counter < responseBytes.length)
                {
                    toReturn = responseBytes[m_Counter];
                    m_Counter++;
                }
                return toReturn;
            }
        };
        
        when(m_UrlService.constructUrlConnection(m_UrlString)).thenReturn(m_Connection);
        when(m_Connection.getInputStream()).thenReturn(stream);
        
        //mock behavior
        when(m_CommandProcessor.processGetPanTilt(m_Ip)).thenReturn(m_UrlString);
        
        //process the command
        GetPanTiltResponse response = (GetPanTiltResponse)m_SUT.onExecuteCommand(new GetPanTiltCommand());

        assertThat(response.getPanTilt().getAzimuth().getValue(), closeTo(4.0, 0.00001));
        assertThat(response.getPanTilt().getElevation().getValue(), closeTo(8.1, 0.00001));
    }
    
    /**
     * Verify get pan/tilt command exception handling.
     */
    @Test
    public void testGetPanTiltException() throws AssetException, IOException, IllegalStateException, 
        InterruptedException
    {
        when(m_UrlService.constructUrlConnection(m_UrlString)).thenThrow(new IOException("hut"));
        
        //mock behavior
        when(m_CommandProcessor.processGetPanTilt(m_Ip)).thenReturn(m_UrlString);
        
        //process the command
        try
        {
            m_SUT.onExecuteCommand(new GetPanTiltCommand());
            fail("expecting exception");
        }
        catch (CommandExecutionException e)
        {
            //expecting exception
        }
    }
    
    /**
     * Verify capture data interactions.
     */
    @Test
    public void testCaptureData() throws AssetException, IOException, IllegalStateException, InterruptedException
    {
        final byte[] responseBytes = new byte[]{1,2,3,4,5};
        
        InputStream stream = new InputStream()
        {
            private int m_Counter;
            @Override
            public int read() throws IOException
            {
                int toReturn = -1;
                if (m_Counter < responseBytes.length)
                {
                    toReturn = responseBytes[m_Counter];
                    m_Counter++;
                }
                return toReturn;
            }
        };
        when(m_UrlService.constructUrlConnection(m_UrlString)).thenReturn(m_Connection);
        when(m_Connection.getInputStream()).thenReturn(stream);
        
        //mock behavior
        when(m_CommandProcessor.processStillImageRequest(m_Ip)).thenReturn(m_UrlString);
        
        //request data capture
        Observation response = m_SUT.onCaptureData();
        
        assertThat(response.getDigitalMedia().getValue(), is(responseBytes));
    }
    
    /**
     * Verify the onPerformBit throws an unsupported operation exception.
     */
    @Test
    public void testOnPerformBit() throws AssetException
    {
        try
        {
            m_SUT.onPerformBit();
            fail("Expecting an exception, operation is not supported.");
        }
        catch (final UnsupportedOperationException e)
        {
            //Do nothing, expected exception.
        }
    }
    
    /**
     * Verify the onStartRecording throws an unsupported operation exception.
     */
    @Test
    public void testOnStartRecording() throws AssetException
    {
        try
        {
            m_SUT.onPerformBit();
            fail("Expecting an exception, operation is not supported.");
        }
        catch (final UnsupportedOperationException e)
        {
            //Do nothing, expected exception.
        }
    }
    
    /**
     * Verify the onStopRecording throws an unsupported operation exception.
     */
    @Test
    public void testOnStopRecording() throws AssetException
    {
        try
        {
            m_SUT.onPerformBit();
            fail("Expecting an exception, operation is not supported.");
        }
        catch (final UnsupportedOperationException e)
        {
            //Do nothing, expected exception.
        }
    }
}
