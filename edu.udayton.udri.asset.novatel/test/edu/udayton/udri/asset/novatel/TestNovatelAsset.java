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
package edu.udayton.udri.asset.novatel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.asset.commands.GetPanTiltCommand;
import mil.dod.th.core.asset.commands.GetPositionCommand;
import mil.dod.th.core.asset.commands.GetPositionResponse;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.test.FactoryObjectContextMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationException;

import edu.udayton.udri.asset.novatel.connection.NovatelConnectionMgr;
import edu.udayton.udri.asset.novatel.message.MessageReader;
import edu.udayton.udri.asset.novatel.message.NovatelInsMessage;
import edu.udayton.udri.asset.novatel.message.NovatelMessageException;
import edu.udayton.udri.asset.novatel.message.NovatelMessageParser;
import edu.udayton.udri.asset.novatel.message.NovatelMessageException.FormatProblem;
import edu.udayton.udri.asset.novatel.timechanger.TimeChange;

/**
 * This class tests the functionality of the {@link NovatelAsset}.
 * @author allenchl
 *
 */
public class TestNovatelAsset
{
    private static boolean DEFAULT_SYNC_SYSTEM_TIME_WITH_GPS = true;
    private static int DEFAULT_NOVATEL_HANDLE_MESSAGE_FREQUENCY = 20;
    private static int DEFAULT_TIME_SERVICE_PORT = 4444;
    private int DEFAULT_NOVATEL_BAUD_RATE = 115200;
    private String DEFAULT_NOVATEL_PHYS_PORT = "COM1";

    private NovatelAsset m_SUT;
    private NovatelConnectionMgr m_ConnectionManager;
    private MessageReader m_MessageReader;
    private TimeChange m_TimeChangeService;
    private NovatelMessageParser m_Parser;
    private NovatelStatusService m_NovatelStatusService;
    private ComponentTypeEnum m_TimeServiceIdentifier = ComponentTypeEnum.SOFTWARE_UNIT;
    private String m_TimeDesc = "Time Service";
    private AssetContext m_AssetContext;
    
    //message strings
    private final String m_InsData = "#INSPVAA";
    private final NovatelInsMessage m_Message = new NovatelInsMessage(
            -80.624022245, 
            28.111270808, 
            -20.775880711, 
            148.083596481, 
            0.238876476, 
            -0.782552644, 1);

    @Before
    public void setUp() throws InvalidSyntaxException, ValidationFailedException, IOException, AssetException,
         UnmarshalException, IllegalArgumentException, IllegalStateException, ConfigurationException, FactoryException
    {
        m_SUT = new NovatelAsset();

        m_ConnectionManager = mock(NovatelConnectionMgr.class);
        m_MessageReader = mock(MessageReader.class);
        m_TimeChangeService = mock(TimeChange.class);
        m_Parser = mock(NovatelMessageParser.class);
        m_NovatelStatusService = mock(NovatelStatusService.class);
       
        when(m_TimeChangeService.getComponentStatus()).thenAnswer(
                new Answer<ComponentStatus>()
                {
                    @Override
                    public ComponentStatus answer(InvocationOnMock invocation) throws Throwable
                    {
                        return createComponentStatus(m_TimeServiceIdentifier, m_TimeDesc, 
                                SummaryStatusEnum.OFF, "Off");
                    }
                });
        //set deps
        m_SUT.setNovatelConnectionMgr(m_ConnectionManager);
        m_SUT.setMessageReader(m_MessageReader);
        m_SUT.setTimeChangeService(m_TimeChangeService);
        m_SUT.setNovatelMessageParser(m_Parser);
        m_SUT.setStatusService(m_NovatelStatusService);
        
        
        Map<String, Object> configProps = createConfigProps();
        m_AssetContext = FactoryObjectContextMocker.mockAssetContext("Novatel", configProps);
        
        m_SUT.initialize(m_AssetContext, configProps);
    }

    /**
     * Verify initialization of the asset's status to off for all components during the 
     * Initialize call.
     */
    @Test
    public void testInitializeStatus()
    {        
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_NovatelStatusService, times(2)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        verify(m_TimeChangeService).getComponentStatus();
        
        //inspect
        assertThat(statusCap.getAllValues().size(), is(2));
        Map<ComponentTypeEnum, SummaryStatusEnum> statMap = new HashMap<>();
        for (ComponentStatus stat: statusCap.getAllValues())
        {
            statMap.put(stat.getComponent().getType(), stat.getStatus().getSummary());
        }
        assertThat(statMap.get(ComponentTypeEnum.SOFTWARE_UNIT), is(SummaryStatusEnum.OFF));
        assertThat(statMap.get(ComponentTypeEnum.NAVIGATION), is(SummaryStatusEnum.OFF));
    }
    
    /**
     * Verify that the asset can be activated.
     * Verify BAD status, as the offset is not known
     */
    @Test
    public void testOnActivate() throws AssetException, IOException
    {
        //activate the asset
        m_SUT.onActivate();
        
        //verify dep interaction
        verify(m_ConnectionManager).startProcessing(DEFAULT_NOVATEL_PHYS_PORT, DEFAULT_NOVATEL_BAUD_RATE);
        verify(m_MessageReader).startRetreiving(m_SUT);
        verify(m_TimeChangeService).connectTimeService(eq(m_SUT), eq(4444));
        
        //capture the status
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.BAD));
    }
    
    /**
     * Verify that the asset can be activated even if tiem service is off.
     * Verify off component status.
     */
    @Test
    public void testOnActivateTimeServiceOff() throws AssetException, IOException, IllegalArgumentException, 
        IllegalStateException, FactoryException
    {
        final Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_SYNC_SYSTEM_TIME_WITH_GPS, false);
        m_SUT.updated(configProps);

        //activate the asset
        m_SUT.onActivate();
        
        //verify no time service interaction
        verify(m_TimeChangeService, never()).connectTimeService(eq(m_SUT), eq(4444));
    }
    
    /**
     * Verify that the asset can be activated.
     * Verify GOOD status, as the offset will be mocked to be known
     */
    @Test
    public void testOnActivateOffsetKnown() throws AssetException, IOException
    {
        when(m_Parser.isOffsetKnown()).thenReturn(true);
        
        //activate the asset
        m_SUT.onActivate();
        
        //verify dep interaction
        verify(m_ConnectionManager).startProcessing(DEFAULT_NOVATEL_PHYS_PORT, DEFAULT_NOVATEL_BAUD_RATE);
        verify(m_MessageReader).startRetreiving(m_SUT);
        verify(m_TimeChangeService).connectTimeService(eq(m_SUT), eq(4444));
        
        //capture the status
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.GOOD));
        
    }
    
    /**
     * Verify that the asset can be deactivated.
     */
    @Test
    public void testOnDeactivate() throws AssetException, IOException, IllegalStateException, InterruptedException
    {
        m_SUT.onActivate();
        m_SUT.onDeactivate();
        
        //verify dep interaction
        verify(m_ConnectionManager).stopProcessing();
        verify(m_MessageReader).stopRetrieving();
        verify(m_TimeChangeService).disconnectTimeService();
    }
    
    /**
     * Verify that the asset can be deactivated even if processing mechanisms fail due to them never being started.
     */
    @Test
    public void testOnDeactivateWithExceptions() throws AssetException, 
        IllegalStateException, IOException, InterruptedException
    {
        
        doThrow(new IllegalStateException("badstate")).when(m_ConnectionManager).stopProcessing();
        doThrow(new IllegalStateException("badstate")).when(m_MessageReader).stopRetrieving();
        doThrow(new IllegalStateException("badstate")).when(m_TimeChangeService).disconnectTimeService();
        
        m_SUT.onActivate();
        m_SUT.onDeactivate();
        
        //verify dep interaction still happened
        verify(m_ConnectionManager).stopProcessing();
        verify(m_MessageReader).stopRetrieving();
        verify(m_TimeChangeService).disconnectTimeService();
    }
    
    /**
     * Verify that the asset can be updated.
     */
    @Test
    public void testUpdated() throws AssetException
    {   
        //activate so that stop is called
        m_SUT.onActivate();
        
        m_SUT.updated(createConfigProps());
        
        verify(m_ConnectionManager).stopProcessing();
        verify(m_MessageReader).stopRetrieving();
        //will be twice, once for the activation once again after update
        verify(m_ConnectionManager, times(2)).startProcessing(DEFAULT_NOVATEL_PHYS_PORT, DEFAULT_NOVATEL_BAUD_RATE);
    }
    
    /**
     * Verify that if the asset is requested to capture data that an exception is thrown.
     */
    @Test
    public void testOnCaptureData()
    {
        try
        {
            m_SUT.onCaptureData();
            fail("Expecting Exception");
        }
        catch (UnsupportedOperationException e)
        {
            //expected exception this asset does not support capturing data
        }
    }
    
    /**
     * Verify exception if position information is not available.
     */
    @Test
    public void testPositionCommandException() throws AssetException
    {
        GetPositionCommand command = mock(GetPositionCommand.class);
        try
        {
            m_SUT.onExecuteCommand(command);
            fail("Expecting Exception");
        }
        catch (CommandExecutionException e)
        {
            //expected exception, only get position is supported
        }
    }
    
    /**
     * Verify executing get position. 
     */
    @Test
    public void testGetPosition() throws NovatelMessageException, ValidationFailedException, CommandExecutionException
    {
        //update props so that the message is immediately handled
        Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_AssetContext = FactoryObjectContextMocker.mockAssetContext("Novatel", configProps);
        m_SUT.initialize(m_AssetContext, configProps);
        
        //mock message
        when(m_Parser.parseInsMessage(m_InsData)).thenReturn(m_Message);
        
        //now handle the ins message
        m_SUT.handleDataString(m_InsData);
        
        GetPositionCommand command = mock(GetPositionCommand.class);
        GetPositionResponse response = (GetPositionResponse)m_SUT.onExecuteCommand(command);
        assertThat(response.getLocation().getLatitude().getValue(), is(28.111270808));
        assertThat(response.getLocation().getLongitude().getValue(), is(-80.624022245));
        assertThat(response.getOrientation().getBank().getValue(), is(-0.782552644));
        assertThat(response.getOrientation().getElevation().getValue(), is(0.238876476));
    }
    
    /**
     * Verify exception with a type of command not supported.
     */
    @Test
    public void testCommandException() throws AssetException
    {
        GetPanTiltCommand command = mock(GetPanTiltCommand.class);
        try
        {
            m_SUT.onExecuteCommand(command);
            fail("Expecting Exception");
        }
        catch (CommandExecutionException e)
        {
            //expected exception, only get position is supported
        }
    }
    
    /**
     * Verify that the asset can handle an INSPVA message from the message reader. 
     */
    @Test
    public void testHandleInsPvaMessage() throws AssetException, IOException, 
        NovatelMessageException, IllegalArgumentException, IllegalStateException, FactoryException, 
        ValidationFailedException
    {
        final Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_SUT.updated(configProps);

        m_SUT.onActivate();
        
        //mock message
        when(m_Parser.parseInsMessage(m_InsData)).thenReturn(m_Message);
        
        //now handle the ins message
        m_SUT.handleDataString(m_InsData);
        
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(m_TimeChangeService).changeTime(captor.capture());
        
        assertThat(captor.getValue(), is(1L));
        
        ArgumentCaptor<Observation> obsCap = ArgumentCaptor.forClass(Observation.class);
        verify(m_AssetContext).persistObservation(obsCap.capture());
        
        Observation obs = obsCap.getValue();
        assertThat(obs.getAssetLocation().getLongitude().getValue(), is(-80.624022245d));
        assertThat(obs.getAssetLocation().getLatitude().getValue(), is(28.111270808d));
        assertThat(obs.getAssetOrientation().getElevation().getValue(), is(0.238876476d));
        assertThat(obs.getAssetOrientation().getHeading().getValue(), is(148.083596481d));
        assertThat(obs.getObservedTimestamp(), is(1L));
    }
    
    /**
     * Verify that the asset can handle an INSPVA that contains bad data.
     * Verify bad status update 
     */
    @Test
    public void testHandleInsPvaMessageBad() throws AssetException, IOException, 
        IllegalArgumentException, PersistenceFailedException, NovatelMessageException, ValidationFailedException
    {
        //update props so that the message is immediately handled
        Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_SUT.updated(configProps);
        
        //mock message
        when(m_Parser.parseInsMessage(anyString())).
            thenThrow(new NovatelMessageException("blarg", FormatProblem.PARSING_ERROR));
        
        m_SUT.handleDataString(NovatelConstants.NOVATEL_INSPVA_MESSAGE + "alsdkjfksljdf");
        
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        
        //verify one event for bad status
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.DEGRADED));
        verify(m_TimeChangeService, never()).changeTime(Mockito.anyLong());
    }
    
    /**
     * Verify that the asset can handle an INSPVA that is missing data.
     * Verify bad status update 
     */
    @Test
    public void testHandleInsPvaMessageIncomplete() throws AssetException, IOException, 
        IllegalArgumentException, PersistenceFailedException, NovatelMessageException, ValidationFailedException
    {
        //update props so that the message is immediately handled
        Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_SUT.updated(configProps);
        
        //mock message
        when(m_Parser.parseInsMessage(anyString())).
            thenThrow(new NovatelMessageException("blarg", FormatProblem.INCOMPLETE_INS_MESSAGE));
        
        m_SUT.handleDataString(NovatelConstants.NOVATEL_INSPVA_MESSAGE + "alsdkjfksljdf");
        
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        
        //verify one event for bad status
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.BAD));
    }
    
    /**
     * Verify that the asset can handle an exception with "other" type.
     * Verify unknown status.
     */
    @Test
    public void testHandleInsPvaMessageUnknown() throws AssetException, IOException, 
        IllegalArgumentException, PersistenceFailedException, NovatelMessageException, ValidationFailedException
    {
        //update props so that the message is immediately handled
        Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_SUT.updated(configProps);
        
        //mock message
        when(m_Parser.parseInsMessage(anyString())).
            thenThrow(new NovatelMessageException("blarg", FormatProblem.OTHER));
        
        m_SUT.handleDataString(NovatelConstants.NOVATEL_INSPVA_MESSAGE + "alsdkjfksljdf");
        
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        
        //verify one event for bad status
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.UNKNOWN));
    }
    
    /**
     * Verify that the asset can handle a Time that contains bad data.
     * Verify bad status update. 
     */
    @Test
    public void testHandleTimeMessageBad() throws AssetException, IOException, NovatelMessageException, 
        ValidationFailedException
    {
        //mock message
        doThrow(new NovatelMessageException("blarg", FormatProblem.TIME_RELIABILITY)).
            when(m_Parser).evaluateTimeMessage(anyString());
        
        m_SUT.handleDataString(NovatelConstants.NOVATEL_TIME_MESSAGE + "alsdkjfksljdf");
        
        //verify bad status
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        
        //verify one event for bad status
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.DEGRADED));
        verify(m_TimeChangeService, never()).changeTime(Mockito.anyLong());
    }

    /**
     * Verify that the asset can handle a Time that contains bad data.
     * Verify bad status update.
     */
    @Test
    public void testHandleTimeMessageIncomplete() throws AssetException, IOException, NovatelMessageException, 
        ValidationFailedException
    {
        //mock message
        doThrow(new NovatelMessageException("blarg", FormatProblem.INCOMPLETE_TIME_MESSAGE)).
            when(m_Parser).evaluateTimeMessage(anyString());
        
        m_SUT.handleDataString(NovatelConstants.NOVATEL_TIME_MESSAGE + "alsdkjfksljdf");
        
        //verify bad status
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        
        //verify one event for bad status
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.BAD));
        verify(m_TimeChangeService, never()).changeTime(Mockito.anyLong());
    }
    
    /**
     * Verify that the asset won't hand a time message if the UTC time offset is not known.
     * Verify bad status update.
     */
    @Test
    public void testHandleTimeMessageNoUtcOffset() throws AssetException, IOException, NovatelMessageException, 
        ValidationFailedException
    {
        //mock message
        doThrow(new NovatelMessageException("blarg", FormatProblem.UTC_OFFSET_UNKNOWN)).
            when(m_Parser).evaluateTimeMessage(anyString());
        
        m_SUT.handleDataString(NovatelConstants.NOVATEL_TIME_MESSAGE + "alsdkjfksljdf");
        
        //verify bad status
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        
        //verify one event for bad status
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.BAD));
        verify(m_TimeChangeService, never()).changeTime(Mockito.anyLong());
    }
    
    /**
     * Verify that the asset can handle a TIMEA message. Verify message sent to parser.
     */
    @Test
    public void testHandleTimeMessage() throws AssetException, IOException, NovatelMessageException, 
        ValidationFailedException
    {
        String time = "time";
        
        m_SUT.handleDataString(NovatelConstants.NOVATEL_TIME_MESSAGE + time);
        
        //verify interaction with parser
        verify(m_Parser).evaluateTimeMessage(NovatelConstants.NOVATEL_TIME_MESSAGE + time);
    }
    
    /**
     * Test update observation frequency.
     */
    @Test
    public void testUpdateObsFreq() throws InterruptedException, AssetException, IllegalArgumentException, 
           IllegalStateException, FactoryException, NovatelMessageException, ValidationFailedException
    {        
        final Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_SUT.updated(configProps);

        m_SUT.onActivate();
        
        //mock message
        when(m_Parser.parseInsMessage(m_InsData)).thenReturn(m_Message);
        
        //verify dep interaction
        verify(m_MessageReader).startRetreiving(m_SUT);
        
        for (int i = 0; i < 10; i++)
        {
            m_SUT.handleDataString(m_InsData);
        }
        
        verify(m_AssetContext, times(10)).persistObservation(Mockito.any(Observation.class));
        
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 2);
        m_SUT.updated(configProps);
        
        //verify dep interaction
        verify(m_MessageReader).stopRetrieving();
        verify(m_MessageReader, times(2)).startRetreiving(m_SUT);
        for (int i = 0; i < 10; i++)
        {
            m_SUT.handleDataString(m_InsData);
        }
        
        //ten from before and 5 more, because every other message should be skipped
        verify(m_AssetContext, times(15)).persistObservation(Mockito.any(Observation.class));
    }
    
    /**
     * Test update serial port settings.
     */
    @Test
    public void testUpdateSerialPortSettings() throws InterruptedException, AssetException, IllegalArgumentException, 
           IllegalStateException, FactoryException
    {        
        m_SUT.onActivate();
        
        //verify dep interaction
        verify(m_ConnectionManager).startProcessing(DEFAULT_NOVATEL_PHYS_PORT, DEFAULT_NOVATEL_BAUD_RATE);
        
        final Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_PHYS_PORT, "COM7");
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_BAUD_RATE, 123);
        m_SUT.updated(configProps);
        
        //verify dep interaction
        verify(m_MessageReader, times(1)).stopRetrieving(); // will be once for each property
        verify(m_ConnectionManager).startProcessing("COM7", 123);
    }
    
    /**
     * Verify illegal state exception handling if the reading thread and serial port are not processing and stop has 
     * been called.
     */
    @Test
    public void testIllegalStateHandling() throws AssetException
    {
        m_SUT.onActivate();

        doThrow(IllegalStateException.class).when(m_ConnectionManager).stopProcessing();
        doThrow(IllegalStateException.class).when(m_MessageReader).stopRetrieving();
        
        m_SUT.updated(createConfigProps());

        //verify that both methods are still called
        verify(m_ConnectionManager).stopProcessing();
        verify(m_MessageReader).stopRetrieving();
        //once for activate once on update
        verify(m_MessageReader, times(2)).startRetreiving(m_SUT);
    }
    
    /**
     * Test update with start processing asset exception
     */
    @Test
    public void testUpdateStartProcException() throws InterruptedException, AssetException, IllegalArgumentException, 
           IllegalStateException, FactoryException
    {       
        //activate the asset so that start processing is called
        m_SUT.onActivate();
        
        doThrow(new AssetException("Asset")).when(m_ConnectionManager).
                startProcessing(DEFAULT_NOVATEL_PHYS_PORT, DEFAULT_NOVATEL_BAUD_RATE);

        m_SUT.updated(createConfigProps());

        //verify no further dep interaction, should be once since the asset was activated
        verify(m_MessageReader, times(1)).startRetreiving(m_SUT);
        
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        //once for activation once for status update
        verify(m_NovatelStatusService, times(4)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.BAD));
    }
    
    /**
     * Verify that if the asset is not activated that the reading and processing services are not updated/interacted
     * with.
     */
    @Test
    public void testNotActiveUpdate() throws IllegalStateException, AssetException
    {
        m_SUT.updated(createConfigProps());

        //verify no dep interaction
        verify(m_ConnectionManager, never()).stopProcessing();
        verify(m_MessageReader, never()).stopRetrieving();
        verify(m_ConnectionManager, never()).startProcessing(DEFAULT_NOVATEL_PHYS_PORT, DEFAULT_NOVATEL_BAUD_RATE);
        verify(m_MessageReader, never()).startRetreiving(m_SUT);
    }
    
    /**
     * Verify status update if the utc offset was not known, but then becomes known and a data message is properly
     * parsed.
     */
    @Test
    public void testHandleInsPvaMessageInitialOffsetMissing() throws AssetException, IOException, 
        IllegalArgumentException, IllegalStateException, FactoryException, NovatelMessageException, 
        ValidationFailedException
    {
        //activate to set the status
        final Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_SUT.updated(configProps);
        m_SUT.onActivate();
        
        //capture the status
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.BAD));
        
        //mock behavior
        when(m_ConnectionManager.isProcessing()).thenReturn(true);
        
        //mock message
        when(m_Parser.parseInsMessage(m_InsData)).thenReturn(m_Message);
        
        //now handle the ins message
        m_SUT.handleDataString(m_InsData);
        
        //capture the status
        verify(m_NovatelStatusService, times(4)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.GOOD));
    }
    
    /**
     * Verify bad status if there is a {@link NovatelMessageException}.
     * Verify that the asset's status is not updated multiple times.
     */
    @Test
    public void testHandleInsPvaStatusException() throws AssetException, IOException, 
        FactoryException, NovatelMessageException, ValidationFailedException
    {
        //activate to set the status
        final Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_SUT.updated(configProps);
        m_SUT.onActivate();
        
        //mock message exception
        when(m_Parser.parseInsMessage(m_InsData)).thenThrow(new NovatelMessageException(
                "gere", FormatProblem.INS_STATUS_NOT_GOOD));
        
        //now handle the ins message a few times
        m_SUT.handleDataString(m_InsData);
        m_SUT.handleDataString(m_InsData);
        m_SUT.handleDataString(m_InsData);
        m_SUT.handleDataString(m_InsData);
        m_SUT.handleDataString(m_InsData);
        
        //capture the status
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_NovatelStatusService, times(8)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.DEGRADED));
    }
    
    /**
     * Verify bad status if there is a {@link NovatelMessageException}.
     * Verify status update if this happens after the status was good.
     */
    @Test
    public void testHandleInsPvaMessageGoodToBadStatus() throws AssetException, IOException, 
        FactoryException, NovatelMessageException, ValidationFailedException
    {
        //activate to set the status
        when(m_Parser.isOffsetKnown()).thenReturn(true);
        final Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_SUT.updated(configProps);
        m_SUT.onActivate();
        
        //capture the status
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        ComponentStatus capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.GOOD));
        
        //mock message exception
        when(m_Parser.parseInsMessage(m_InsData)).thenThrow(
                new NovatelMessageException("gere", FormatProblem.INS_STATUS_NOT_GOOD));
        
        //now handle the ins message a few times
        m_SUT.handleDataString(m_InsData);
        
        //capture message
        verify(m_NovatelStatusService, times(4)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        //get the message readers component status
        capStatus = statusCap.getValue();
        assertThat(capStatus.getStatus().getSummary(), is(SummaryStatusEnum.DEGRADED));
    }
    
    /**
     * Test the status returned from the status service is posted as an observation.
     */
    @Test
    public void testStatusFromStatusServicePosts() throws AssetException, IOException, ValidationFailedException, 
        IllegalArgumentException, PersistenceFailedException
    {
        //mock status
        Status status = mock(Status.class);
        when(status.getSummaryStatus()).thenReturn(new OperatingStatus(SummaryStatusEnum.GOOD, "asdf"));
        
        //component status
        ComponentStatus compStat = createComponentStatus(
                ComponentTypeEnum.SOFTWARE_UNIT, "plugin", SummaryStatusEnum.GOOD, "goodness");
        
        when(m_NovatelStatusService.getStatus(Mockito.any(Observation.class), 
                Mockito.any(ComponentStatus.class))).thenReturn(status);
        
        //handle status
        m_SUT.handleStatusUpdate(compStat);
        
        //verify dep interactions
        verify(m_AssetContext).setStatus(status);
    }
    
    /**
     * Verify that the summary status is posted if an error occurs posting the complete status.
     */
    @Test
    public void testHandleStatusUpdateError() throws ValidationFailedException
    {
        final Observation obs = mock(Observation.class);
        final ComponentStatus compStatus = mock(ComponentStatus.class);
        final Status status = mock(Status.class);
        final OperatingStatus opStatus = mock(OperatingStatus.class);
        when(m_AssetContext.getLastStatus()).thenReturn(obs);
        when(m_NovatelStatusService.getStatus(Mockito.any(Observation.class), 
                Mockito.any(ComponentStatus.class))).thenReturn(status);
        when(status.getSummaryStatus()).thenReturn(opStatus);
        doThrow(ValidationFailedException.class).when(m_AssetContext).setStatus(status);
        
        m_SUT.handleStatusUpdate(compStatus);
        
        verify(m_AssetContext).setStatus(status);
        verify(m_AssetContext).setStatus(status.getSummaryStatus().getSummary(), 
                status.getSummaryStatus().getDescription());
    }

    /**
     * Verify the handling of an error status callback.
     */
    @Test
    public void testHandleReadError()
    {
        m_SUT.handleReadError(SummaryStatusEnum.BAD, "this is bad");
        
        //capture the status
        ArgumentCaptor<ComponentStatus> statusCap = ArgumentCaptor.forClass(ComponentStatus.class);
        verify(m_NovatelStatusService, times(3)).getStatus(Mockito.any(Observation.class), statusCap.capture());
        
        assertThat(statusCap.getValue().getStatus().getSummary(), is(SummaryStatusEnum.BAD));
        assertThat(statusCap.getValue().getStatus().getDescription(), is("this is bad"));
    }
    
    /**
     * Verify that if the time service is disabled that the change time method is not invoked.
     */
    @Test
    public void testDisableTimeServiceChangeCall() throws IllegalArgumentException, IllegalStateException, 
        FactoryException, AssetException, NovatelMessageException, ValidationFailedException
    {
        //mock message
        when(m_Parser.parseInsMessage(m_InsData)).thenReturn(m_Message);
        
        final Map<String, Object> configProps = createConfigProps();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_SYNC_SYSTEM_TIME_WITH_GPS, false);
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 1);
        m_SUT.updated(configProps);
        m_SUT.onActivate();
        
        m_SUT.handleDataString(m_InsData);
        
        //interactions
        verify(m_TimeChangeService, never()).changeTime(anyLong());
        
        verify(m_AssetContext).persistObservation(Mockito.any(Observation.class));
    }
    
    /**
     * Component status.
     * @param type
     *      the component type
     * @param componentDesc
     *      the description
     * @param status
     *      the summary status for the component
     * @param description
     *      description of the components status
     */
    private ComponentStatus createComponentStatus(final ComponentTypeEnum type, final String componentDesc, 
            final SummaryStatusEnum status, final String description)
    {
        final ComponentStatus insStatus = new ComponentStatus();
        final ComponentType insCompType = new ComponentType();
        insCompType.setDescription(componentDesc);
        insCompType.setType(type);
        insStatus.setComponent(insCompType);
        final OperatingStatus insOpStatus = new OperatingStatus();
        insOpStatus.setSummary(status);
        insOpStatus.setDescription(description);
        insStatus.setStatus(insOpStatus);
        return insStatus;
    }
    
    /**
     * Method that creates a map of configuration properties for use in testing.
     * @return
     *      Map containing fake configuration property data.
     */
    private Map<String, Object> createConfigProps()
    {
        final Map<String, Object> configProps = new HashMap<String, Object>();
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_HANDLE_MESSAGE_FREQUENCY, 
                DEFAULT_NOVATEL_HANDLE_MESSAGE_FREQUENCY);
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_PHYS_PORT, DEFAULT_NOVATEL_PHYS_PORT);
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_BAUD_RATE, DEFAULT_NOVATEL_BAUD_RATE);
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_SYNC_SYSTEM_TIME_WITH_GPS, 
                DEFAULT_SYNC_SYSTEM_TIME_WITH_GPS);
        configProps.put(NovatelAssetAttributes.CONFIG_PROP_TIME_SERVICE_PORT, DEFAULT_TIME_SERVICE_PORT);
        return configProps;
    }
}
