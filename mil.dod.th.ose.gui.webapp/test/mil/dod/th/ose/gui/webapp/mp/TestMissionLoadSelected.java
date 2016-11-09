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
package mil.dod.th.ose.gui.webapp.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.faces.application.FacesMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.protobuf.Message;

import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadParametersRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadTemplateRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ActiveControllerImpl;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;
import mil.dod.th.ose.gui.webapp.mp.MissionModel.MissionTemplateLocation;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramParametersGen;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.MapEntry;

/**
 * Test loading the selected mission to the active controller. Note that the mocked models are separate from the
 * test code to prevent the methods from being large. Also note that the argument model 'get' method returns an object
 * that is meant to be of the correct type, this is tested in {@link #testSyncedLoadMission()}.
 * @author callen
 *
 */
public class TestMissionLoadSelected 
{
    private MissionLoadSelected m_SUT;
    private GrowlMessageUtil m_GrowlUtil;
    private TemplateProgramManager m_TemplateManager; 
    private JaxbProtoObjectConverter m_Converter;
    private MissionSetUpMgr m_MissionManager;
    private ActiveController m_ActiveController;
    private MissionProgramMgr m_MissionProgMgr;
    
    @Before
    public void setUp()
    {
        //mock services
        m_SUT =  new MissionLoadSelected();
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_TemplateManager = mock(TemplateProgramManager.class);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_MissionManager = mock(MissionSetUpMgrImpl.class);
        m_ActiveController = mock(ActiveControllerImpl.class);
        m_MissionProgMgr = mock(MissionProgramMgrImpl.class);
        
        //set services
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        m_SUT.setTemplateProgramManager(m_TemplateManager);
        m_SUT.setConverter(m_Converter);
        m_SUT.setMissionSetUpMgr(m_MissionManager);
        m_SUT.setMissionProgMgr(m_MissionProgMgr);
        m_SUT.setActiveController(m_ActiveController);
    }
    
    private MissionModel getModelA()
    {
        //mock out mission model
        MissionModel model = mock(MissionModel.class);
        when(model.getName()).thenReturn("Bueller");
        when(model.getLocation()).thenReturn(MissionTemplateLocation.LOCAL);
        //argument list
        List<MissionArgumentModel> args = new ArrayList<MissionArgumentModel>();
        MissionArgumentModel argModelA = new MissionArgumentModel();
        argModelA.setType(MissionVariableTypesEnum.STRING);
        argModelA.setCurrentValue("Money");
        argModelA.setDescription("I like money");
        argModelA.setName("Currency");
        args.add(argModelA);
    
        MissionArgumentModel argModelB = new MissionArgumentModel();
        argModelB.setType(MissionVariableTypesEnum.STRING);
        argModelB.setCurrentValue("Doubloons");
        argModelB.setDescription("I like pirate money");
        argModelB.setName("PirateCurrency");
        args.add(argModelB);
        //mock behavior
        when(model.getArguments()).thenReturn(args);
        
        return model;
    }
    
    private MissionModel getModelB()
    {
        //mock out mission model
        MissionModel model = mock(MissionModel.class);
        when(model.getName()).thenReturn("Bueller");
        when(model.getLocation()).thenReturn(MissionTemplateLocation.SYNCED);
        //argument list
        List<MissionArgumentModel> args = new ArrayList<MissionArgumentModel>();
        MissionArgumentModel argModelA = new MissionArgumentModel();
        argModelA.setType(MissionVariableTypesEnum.STRING);
        argModelA.setCurrentValue("Money");
        argModelA.setDescription("I like money");
        argModelA.setName("Currency");
        args.add(argModelA);
    
        MissionArgumentModel argModelB = new MissionArgumentModel();
        argModelB.setType(MissionVariableTypesEnum.STRING);
        argModelB.setCurrentValue("Doubloons");
        argModelB.setDescription("I like pirate money");
        argModelB.setName("PirateCurrency");
        args.add(argModelB);
        
        MissionArgumentModel argModelC = new MissionArgumentModel();
        argModelC.setType(MissionVariableTypesEnum.INTEGER);
        argModelC.setCurrentValue("321");
        argModelC.setDescription("I like pirate money");
        argModelC.setName("Piracy");        
        args.add(argModelC);
        
        MissionArgumentModel argModelD = new MissionArgumentModel();
        argModelD.setType(MissionVariableTypesEnum.DOUBLE);
        argModelD.setCurrentValue("321.908");
        argModelD.setDescription("I like pirate money");
        argModelD.setName("DOUBLEOONS, get it?");        
        args.add(argModelD);
        
        MissionArgumentModel argModelE = new MissionArgumentModel();
        argModelE.setType(MissionVariableTypesEnum.SHORT);
        argModelE.setCurrentValue("321");
        argModelE.setDescription("I like pirate money");
        argModelE.setName("ShortPirateMoney");        
        args.add(argModelE);
        //mock behavior
        when(model.getArguments()).thenReturn(args);
        
        return model;
    }
    
    /**
     * Fabricated mission program schedule.
     */
    private MissionProgramSchedule getSchedule()
    {
        //mock schedule
        MissionProgramSchedule schedule = new MissionProgramSchedule().
            withActive(false).
            withAtReset(true).
            withImmediately(true).
            withIndefiniteInterval(true);
        return schedule;
    }
    
    /**
     * Fabricated mission program template. Based off of mocked models
     */
    private MissionProgramTemplate getTemplate()
    {
        //template representing the model
        MissionProgramTemplate template = new MissionProgramTemplate().
            withName("Bueller").
            withDescription("Bueller").
            withSource("Take a Day Off!");
        return template;
    }
    
    /**
     * Test loading a local mission.
     * 
     * Verify message sender sends template.
     * 
     * Verify message sender sends parameters.
     */
    @Test
    public void testLocalLoadMission() throws ObjectConverterException
    {
        //schedule
        MissionProgramSchedule schedule = getSchedule();
        //mock mission model
        MissionModel model = getModelA();
        
        //mock mission manager behavior
        when(m_MissionManager.getMission()).thenReturn(model);
        when(m_MissionManager.getSchedule()).thenReturn(schedule);
        when(m_MissionManager.getProgramName()).thenReturn("name");
        
        //mock controller
        ControllerModel controller = mock(ControllerModel.class);
        when(controller.getId()).thenReturn(123);
        //mock active controller
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(controller);
        
        //template representing the model
        MissionProgramTemplate template = getTemplate();
        //mock template program manager behavior
        when(m_TemplateManager.getTemplate(model.getName())).thenReturn(template);
        
        //template proto message
        Message templateGen = MissionProgramTemplateGen.
            MissionProgramTemplate.newBuilder().
            setName("IamATest").
            setSource("TheSource!@@#(*&$^%_@##$").build();
        
        //parameters proto message
        MissionProgramParametersGen.MissionProgramSchedule genSched = 
            MissionProgramParametersGen.MissionProgramSchedule.
            newBuilder().
            setActive(false).
            setAtReset(true).
            setImmediately(true).
            setIndefiniteInterval(true).build();
        Multitype value = Multitype.newBuilder().setStringValue("Money").setType(Type.STRING).build();
        MapEntry entry = MapEntry.newBuilder().setKey("Currency").setValue(value).build();
        MissionProgramParametersGen.MissionProgramParameters parametersGen = 
            MissionProgramParametersGen.MissionProgramParameters.newBuilder().
                addParameters(entry). 
                setSchedule(genSched).
                setProgramName("name").
                setTemplateName("Bueller").build();        
        //mock converters behavior
        when(m_Converter.convertToProto(Mockito.any())).thenReturn(templateGen, parametersGen);
        
        //try to load mock mission, capture string returned
        String outcome = m_SUT.loadMission();
        //verify growl 'success' message
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_INFO), Mockito.anyString(), 
            Mockito.anyString());
        
        //verify template
        ArgumentCaptor<Message> templateCaptor = ArgumentCaptor.
                forClass(Message.class);

        verify(m_MissionProgMgr).queueMessage(eq(123), templateCaptor.capture(), 
                eq(MissionProgrammingMessageType.LoadTemplateRequest));

        LoadTemplateRequestData data = (LoadTemplateRequestData)templateCaptor.getValue();

        assertThat(data.getMission(), is(templateGen));

        //verify parameters
        ArgumentCaptor<Message> paramCaptor = ArgumentCaptor.
                forClass(Message.class);
        verify(m_MissionProgMgr).queueMessage(
                eq(123), paramCaptor.capture(), eq(MissionProgrammingMessageType.LoadParametersRequest));
        
        assertThat(((LoadParametersRequestData)paramCaptor.getValue()).getParameters(), is(parametersGen));
        
        //verify outcome
        assertThat(outcome, is(""));
    }
    
    /**
     * Test loading a template that is supposed to be local, but is not.
     * 
     * Verify message sender does NOT send template.
     * 
     * Verify message sender does NOT send parameters.
     */
    @Test
    public void testLocalLoadMissionTemplateNotFound() throws ObjectConverterException
    {
        //mocked model
        MissionModel model = getModelA();     
        //mock mission manager behavior
        when(m_MissionManager.getMission()).thenReturn(model);
        
        //mock controller
        ControllerModel controller = mock(ControllerModel.class);
        when(controller.getId()).thenReturn(123);
        //mock active controller
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(controller);
        
        //mock template program manager behavior
        when(m_TemplateManager.getTemplate(model.getName())).thenReturn(null);
        
        //try to load mock mission, capture string returned
        try
        {
            m_SUT.loadMission();
            fail("Expecting exception");
        }
        catch (IllegalArgumentException exception)
        {
            //expecting exception
        }
        //verify message sender never sent messages
        verify(m_MissionProgMgr, never()).queueMessage(eq(123), Mockito.any(Message.class), 
                Mockito.any(MissionProgrammingMessageType.class));
    }
    
    /**
     * Test loading a local mission template that can't be converted to a proto message.
     * 
     * Verify message sender NEVER sends template.
     * 
     * Verify message sender NEVER sends parameters.
     */
    @Test
    public void testLocalLoadMissionUnableConvertTemplateToProto() throws ObjectConverterException
    {
        //mock schedule
        MissionProgramSchedule schedule = getSchedule();
        MissionModel model = getModelA();       
        //mock mission manager behavior
        when(m_MissionManager.getMission()).thenReturn(model);
        when(m_MissionManager.getSchedule()).thenReturn(schedule);
        
        //mock controller
        ControllerModel controller = mock(ControllerModel.class);
        when(controller.getId()).thenReturn(123);
        //mock active controller
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(controller);
        
        //template representing the model
        MissionProgramTemplate template = getTemplate();
        //mock template program manager behavior
        when(m_TemplateManager.getTemplate(model.getName())).thenReturn(template);
        
        //mock converter throwing exception
        when(m_Converter.convertToProto(Mockito.any())).thenThrow(new ObjectConverterException(""));
        
        //try to load mock mission, capture string returned
        try
        {
            m_SUT.loadMission();
            fail("Expecting exception");
        }
        catch (ObjectConverterException exception)
        {
            //expecting exception
        }
        //verify message sender never sent
        verify(m_MissionProgMgr, never()).queueMessage(eq(123), Mockito.any(Message.class), 
                Mockito.any(MissionProgrammingMessageType.class));
    }
    
    /**
     * Test loading parameters that can't be converted to a proto message.
     * 
     * Verify message sender sends template.
     * 
     * Verify message sender NEVER sends parameters.
     */
    @Test
    public void testLocalLoadMissionUnableConvertParamsToProto() throws ObjectConverterException
    {
        //schedule
        MissionProgramSchedule schedule = getSchedule();
        MissionModel model = getModelA(); 
        
        //mock mission manager behavior
        when(m_MissionManager.getMission()).thenReturn(model);
        when(m_MissionManager.getSchedule()).thenReturn(schedule);
        when(m_MissionManager.getProgramName()).thenReturn("name");
        
        //mock controller
        ControllerModel controller = mock(ControllerModel.class);
        when(controller.getId()).thenReturn(123);
        //mock active controller
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(controller);
        
        //template representing the model
        MissionProgramTemplate template = getTemplate();
        //mock template program manager behavior
        when(m_TemplateManager.getTemplate(model.getName())).thenReturn(template);
        
        //template proto message
        Message templateGen = MissionProgramTemplateGen.
            MissionProgramTemplate.newBuilder().
            setName("IamATest").
            setSource("TheSource!@@#(*&$^%_@##$").build(); 
        //mock converters behavior
        when(m_Converter.convertToProto(Mockito.any()))
            .thenReturn(templateGen)
            .thenThrow(new ObjectConverterException(""));
        
        //try to load mock mission, capture string returned
        try
        {
            m_SUT.loadMission();
            fail("Expecting exception");
        }
        catch (ObjectConverterException exception)
        {
            //expecting exception
        }
        //verify message sender sent one message for template
        ArgumentCaptor<Message> captor = ArgumentCaptor.
            forClass(Message.class);
        verify(m_MissionProgMgr, times(1)).queueMessage(eq(123), captor.capture(), 
                eq(MissionProgrammingMessageType.LoadTemplateRequest));
        
        //verify template
        assertThat(((LoadTemplateRequestData)captor.getValue()).getMission(), is(templateGen));
    }
    
    /**
     * Test loading synced mission.
     * 
     * Verify message sender sends parameters.
     * 
     * Verify that parameters are of the correct type (ie., integer, double, etc...)
     */
    @Test
    public void testSyncedLoadMission() throws ObjectConverterException
    {
        //mock schedule
        MissionProgramSchedule schedule = getSchedule();
        MissionModel model = getModelB();      
        //mock mission manager behavior
        when(m_MissionManager.getMission()).thenReturn(model);
        when(m_MissionManager.getSchedule()).thenReturn(schedule);
        when(m_MissionManager.getProgramName()).thenReturn("name");
        
        //mock controller
        ControllerModel controller = mock(ControllerModel.class);
        when(controller.getId()).thenReturn(123);
        //mock active controller
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(controller);
        
        MissionProgramParametersGen.MissionProgramSchedule genSched = 
            MissionProgramParametersGen.MissionProgramSchedule.
            newBuilder().setActive(false).setAtReset(true).setImmediately(true).setIndefiniteInterval(true).build();
        Multitype value = Multitype.newBuilder().setStringValue("Money").setType(Type.STRING).build();
        MapEntry entry = MapEntry.newBuilder().setKey("Currency").setValue(value).build();
        MissionProgramParametersGen.MissionProgramParameters parametersGen = 
            MissionProgramParametersGen.MissionProgramParameters.newBuilder().
                addParameters(entry).
                setProgramName("name").
                setSchedule(genSched).
                setTemplateName("Bueller").build();
        //mock converters behavior
        when(m_Converter.convertToProto(Mockito.any())).thenReturn(parametersGen);
        
        //try to load mock mission, capture string returned
        String outcome = m_SUT.loadMission();
        //verify parameter values that are not strings
        ArgumentCaptor<MissionProgramParameters> paramsCap = ArgumentCaptor.forClass(MissionProgramParameters.class);
        verify(m_Converter).convertToProto(paramsCap.capture());
        //see the model for the values set, should be an integer, double and short, verify that 
        //schedule is set as active
        assertThat((Integer)paramsCap.getValue().getParameters().get(2).getValue(), is(321));
        assertThat((Double)paramsCap.getValue().getParameters().get(3).getValue(), is(321.908));
        assertThat((Short)paramsCap.getValue().getParameters().get(4).getValue(), is((short) 321));
        assertThat(paramsCap.getValue().getSchedule().isActive(), is(true));
        
        //verify message sender sent message for params 
        ArgumentCaptor<Message> captor = ArgumentCaptor.
            forClass(Message.class);
        verify(m_MissionProgMgr, times(1)).queueMessage(eq(123), captor.capture(), 
                eq(MissionProgrammingMessageType.LoadParametersRequest));
        //verify growl message to user
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_INFO), Mockito.anyString(), 
                Mockito.anyString());
        
        //verify parameters
        assertThat(((LoadParametersRequestData)captor.getValue()).getParameters(), is(parametersGen));
        
        //verify outcome
        assertThat(outcome, is(""));
    }
    
    /**
     * Test loading synced mission with bad parameters(converter will throw exception).
     * 
     * Verify message sender NEVER sends parameters.
     */
    @Test
    public void testSyncedLoadMissionBadParams() throws ObjectConverterException
    {
        //mock schedule
        MissionProgramSchedule schedule = getSchedule();
        MissionModel model = getModelB();
        
        //mock mission manager behavior
        when(m_MissionManager.getMission()).thenReturn(model);
        when(m_MissionManager.getSchedule()).thenReturn(schedule);
        when(m_MissionManager.getProgramName()).thenReturn("name");
        
        //mock controller
        ControllerModel controller = mock(ControllerModel.class);
        when(controller.getId()).thenReturn(123);
        //mock active controller
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(controller);
        
        //mock parameter proto converter throwing exception
        when(m_Converter.convertToProto(Mockito.any())).thenThrow(new ObjectConverterException(""));
        //try to load mock mission, capture string returned
        try
        {
            m_SUT.loadMission();
            fail("Expecting exception!");
        }
        catch (ObjectConverterException ex)
        {
            //expecting exception
        }
        //verify message sender NEVER sent message for params 
        verify(m_MissionProgMgr, never()).queueMessage(eq(123), Mockito.any(Message.class), 
                Mockito.any(MissionProgrammingMessageType.class));
    }

    /**
     * Test loading synced mission with parameters that do not have a name set.
     * 
     * Verify message sent with created name.
     */
    @Test
    public void testSyncedLoadMissionNoName() throws ObjectConverterException
    {
        //mock schedule
        MissionProgramSchedule schedule = getSchedule();
        MissionModel model = getModelB();
        
        //mock mission manager behavior
        when(m_MissionManager.getMission()).thenReturn(model);
        when(m_MissionManager.getSchedule()).thenReturn(schedule);
        when(m_MissionManager.getProgramName()).thenReturn("");
        
        //mock controller
        ControllerModel controller = mock(ControllerModel.class);
        when(controller.getId()).thenReturn(123);
        //mock active controller
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(controller);
        
        //uuid to test for
        String uuidString = UUID.randomUUID().toString();

        MissionProgramParametersGen.MissionProgramSchedule genSched = 
            MissionProgramParametersGen.MissionProgramSchedule.
                newBuilder().setActive(false).setAtReset(true).setImmediately(true).setIndefiniteInterval(true).build();
        Multitype value = Multitype.newBuilder().setStringValue("Money").setType(Type.STRING).build();
        MapEntry entry = MapEntry.newBuilder().setKey("Currency").setValue(value).build();
        MissionProgramParametersGen.MissionProgramParameters parametersGen = 
             MissionProgramParametersGen.MissionProgramParameters.newBuilder().
                addParameters(entry).
                setProgramName("name" + uuidString).
                setSchedule(genSched).
                setTemplateName("Bueller").build();
        //mock converters behavior
        when(m_Converter.convertToProto(Mockito.any())).thenReturn(parametersGen);

        //try to load mock mission, capture string returned
        String outcome = m_SUT.loadMission();

        //verify parameter values that are not strings
        ArgumentCaptor<MissionProgramParameters> paramsCap = ArgumentCaptor.forClass(MissionProgramParameters.class);
        verify(m_Converter).convertToProto(paramsCap.capture());
        //see the model for the values set, should be an integer, double and short, verify that 
        //schedule is set as active
        assertThat((Integer)paramsCap.getValue().getParameters().get(2).getValue(), is(321));
        assertThat((Double)paramsCap.getValue().getParameters().get(3).getValue(), is(321.908));
        assertThat((Short)paramsCap.getValue().getParameters().get(4).getValue(), is((short) 321));
        assertThat(paramsCap.getValue().getSchedule().isActive(), is(true));
        assertThat(paramsCap.getValue().getProgramName().isEmpty(), is(false));
        assertThat(paramsCap.getValue().getProgramName().contains(model.getName()), is(true));
        //verify message sender sent message for params 
        ArgumentCaptor<Message> captor = ArgumentCaptor.
            forClass(Message.class);
        verify(m_MissionProgMgr, times(1)).queueMessage(eq(123), captor.capture(), 
                eq(MissionProgrammingMessageType.LoadParametersRequest));
        //verify growl message to user, because name was empty should be an additional growl
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_INFO), eq("Mission Program "),
                String.format("A mission program loaded for controller %d was not named, assigned name is %s", 
                        123, Mockito.anyString()));
        verify(m_GrowlUtil, times(2)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_INFO), Mockito.anyString(), 
                Mockito.anyString());
        
        //verify parameters
        assertThat(((LoadParametersRequestData)captor.getValue()).getParameters(), is(parametersGen));
        
        //verify outcome
        assertThat(outcome, is(""));
    }

    /**
     * Test that null is returned when the active controller is not set.
     * 
     */
    @Test
    public void testNoActiveController() throws IllegalArgumentException, ObjectConverterException
    {
        //mock active controller not being set
        when(m_ActiveController.isActiveControllerSet()).thenReturn(false);
        
        //try to load mission
        String outcome = m_SUT.loadMission();
        
        //verify message sender NEVER sends messages
        verify(m_MissionProgMgr, never()).queueMessage(eq(123), Mockito.any(Message.class), 
                Mockito.any(MissionProgrammingMessageType.class));
        
        //verify outcome
        assertThat(outcome, is(nullValue()));
    }
}
