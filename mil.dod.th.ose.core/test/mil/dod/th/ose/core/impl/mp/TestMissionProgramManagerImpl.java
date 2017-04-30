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

package mil.dod.th.ose.core.impl.mp;

import static org.junit.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.EventHandlerHelper;
import mil.dod.th.core.mp.ManagedExecutors;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.MissionScript;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.pm.DevicePowerManager;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.RemoteSystemEncryption;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.core.impl.mp.MissionProgramManagerImpl.MissionProgramManagerEventHandler;
import mil.dod.th.ose.mp.runtime.MissionProgramRuntime;
import mil.dod.th.ose.shared.MapTranslator;
import mil.dod.th.ose.test.EventAdminMocker;
import mil.dod.th.ose.test.EventAdminMocker.EventHandlerRegistrationAnswer;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.utils.Consumer;
import mil.dod.th.ose.utils.xml.XmlUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.MetaTypeService;

public class TestMissionProgramManagerImpl
{
    private MissionProgramManagerImpl m_SUT;
    private LoggingService m_Logging;
    
    @Mock private ScriptEngine m_ScriptEngine;
    @Mock private Bindings m_Bindings;
    @Mock private PersistentDataStore m_PersistentDataStore;
    @Mock private Bundle m_Bundle;
    @Mock private BundleContext m_Context;
    @Mock private EventAdmin m_EventAdmin;
    @Mock private AssetDirectoryService m_AssetDirectoryService;
    @Mock private EventHandlerHelper m_EventHandlerHelper;
    @Mock private CustomCommsService m_CustomCommsService;
    @Mock private AddressManagerService m_AddressManagerService;
    @Mock private ObservationStore m_ObservationStore;
    @Mock private ManagedExecutors m_ManagedExecutors;
    @Mock private TerraHarvestController m_TerraHarvestController;
    @Mock private TerraHarvestSystem m_TerraHarvestSystem;
    @Mock private Validator m_MissionProgramValidator;
    @Mock private TemplateProgramManager m_TemplateManager;
    @Mock private MissionProgramScheduler m_Scheduler;
    @Mock private XmlMarshalService m_XMLMarshalService;
    @Mock private XmlUnmarshalService m_XMLUnmarshalService;
    @Mock private JaxbProtoObjectConverter m_JaxbProtoObjectConverter;
    @Mock private MetaTypeService m_MetaTypeService;
    @Mock private Validator m_Validator;
    @Mock private MissionProgramRuntime m_MissionProgramRuntime;
    
    private EventHandlerRegistrationAnswer m_EventHandlerAnswer;
    private Map<String, Object> m_BindingsMap;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        m_Logging = LoggingServiceMocker.createMock();

        m_SUT = new MissionProgramManagerImpl();
        
        setupStandardMocks();        
        mockBindings();
          
        when(m_Bundle.getEntry("nashornUpgrade.js")).thenReturn(new URL("file:.//resources//nashornUpgrade.js"));
    }
    
    /**
     * 
     */
    private void setupStandardMocks()
    {
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setPersistentDataStore(m_PersistentDataStore);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMissionProgramValidator(m_MissionProgramValidator);
        m_SUT.setTemplateProgramManager(m_TemplateManager);
        m_SUT.setAssetDirectoryService(m_AssetDirectoryService);
        m_SUT.setEventHandlerHelper(m_EventHandlerHelper);
        m_SUT.setCustomCommsService(m_CustomCommsService);
        m_SUT.setAddressManagerService(m_AddressManagerService);
        m_SUT.setObservationStore(m_ObservationStore);
        m_SUT.setManagedExecutors(m_ManagedExecutors);
        m_SUT.setTerraHarvestController(m_TerraHarvestController);
        m_SUT.setTerraHarvestSystem(m_TerraHarvestSystem);
        m_SUT.setMissionScheduler(m_Scheduler);
        m_SUT.setXMLMarshalService(m_XMLMarshalService);
        m_SUT.setXMLUnmarshalService(m_XMLUnmarshalService);
        m_SUT.setJaxbProtoObjectConverter(m_JaxbProtoObjectConverter);
        m_SUT.setMetaTypeService(m_MetaTypeService);
        m_SUT.setValidator(m_Validator);
        m_SUT.setMissionProgramRuntime(m_MissionProgramRuntime);
        
        //because internally the script engine is casted to an additional interface implemented by script
        //engines, need to add the interface to the supported interfaces for this mock object
        m_ScriptEngine = mock(ScriptEngine.class, withSettings().extraInterfaces(Invocable.class));
        MissionScript missionScript = mock(MissionScript.class);
        when(((Invocable)m_ScriptEngine).getInterface(anyObject(), eq(MissionScript.class))).thenReturn(missionScript);
        
        // must set to valid class loader, any will do since running in single class loader environment, if not mocked
        // class loader will be null and nothing can be loaded
        when(m_MissionProgramRuntime.getClassLoader()).thenReturn(getClass().getClassLoader());
        
        //mock the behavior in the scheduler service
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                ProgramImpl prog = (ProgramImpl)invocation.getArguments()[0];
                prog.changeStatus(ProgramStatus.EXECUTING);
                prog.changeStatus(ProgramStatus.EXECUTED);
                //post evet
                Map<String, Object> props = new HashMap<String, Object>();
                props.put(Program.EVENT_PROP_PROGRAM_NAME, prog.getProgramName());
                m_EventHandlerAnswer.getHandler().handleEvent(new Event(Program.TOPIC_PROGRAM_EXECUTED, props));
                return null;
            }
        }).when(m_Scheduler).executeProgram(Mockito.any(ProgramImpl.class));
        
        m_EventHandlerAnswer = EventAdminMocker
                .stubHandlerOfType(m_Context, MissionProgramManagerEventHandler.class, m_EventAdmin);
        
        when(m_Context.getBundle()).thenReturn(m_Bundle);        
    }

    /**
     * Verify that MPM activates correctly.
     */
    @Test
    public void testActivate() 
        throws IllegalArgumentException, MissionProgramException, ScriptException
    {
        // mock
        mockMissionProgramPersistentData();
        mockTemplates();
        
        // replay
        setScriptEngineAndActivate();
        
        // verify copying of base bindings
        verify(m_ScriptEngine).put(MissionProgramManager.BUNDLE_CONTEXT, m_Context);
        verify(m_ScriptEngine).put(MissionProgramManager.ASSET_DIRECTORY_SERVICE, m_AssetDirectoryService);
        verify(m_ScriptEngine).put(MissionProgramManager.CUSTOM_COMMS_SERVICE, m_CustomCommsService);
        verify(m_ScriptEngine).put(MissionProgramManager.ADDRESS_MANAGER_SERVICE, m_AddressManagerService);
        ArgumentCaptor<Object> service = ArgumentCaptor.forClass(Object.class);
        verify(m_ScriptEngine).put(eq(MissionProgramManager.LOGGING_SERVICE), service.capture());
        assertThat(service.getValue(), is(instanceOf(LoggingService.class)));
        verify(m_ScriptEngine).put(MissionProgramManager.EVENT_HANDLER_HELPER, m_EventHandlerHelper);
        verify(m_ScriptEngine).put(MissionProgramManager.OBSERVATION_STORE, m_ObservationStore);
        verify(m_ScriptEngine).put(MissionProgramManager.PERSISTENT_DATA_STORE, m_PersistentDataStore);
        verify(m_ScriptEngine).put(MissionProgramManager.MISSION_PROGRAM_MANAGER, m_SUT);
        verify(m_ScriptEngine).put(MissionProgramManager.MANAGED_EXECUTORS, m_ManagedExecutors);
        verify(m_ScriptEngine).put(MissionProgramManager.TERRA_HARVEST_SYSTEM, m_TerraHarvestSystem);
        verify(m_ScriptEngine).put(MissionProgramManager.TERRA_HARVEST_CONTROLLER, m_TerraHarvestController);
        verify(m_ScriptEngine).put(MissionProgramManager.XML_MARSHAL_SERVICE, m_XMLMarshalService);
        verify(m_ScriptEngine).put(MissionProgramManager.XML_UNMARSHAL_SERVICE, m_XMLUnmarshalService);
        verify(m_ScriptEngine).put(MissionProgramManager.JAXB_PROTO_OBJECT_CONVERTER,    
                m_JaxbProtoObjectConverter);
        verify(m_ScriptEngine).put(MissionProgramManager.META_TYPE_SERVICE, m_MetaTypeService);
        verify(m_ScriptEngine).put(MissionProgramManager.VALIDATOR, m_Validator);
        
        assertThat(m_SUT.getActiveProgramTemplateNames().size(), greaterThan(1));
                
        // verify restoring of programs
        Set<Program> programs =  m_SUT.getActiveProgramsUsingTemplate("saved-program1");
        assertThat(programs.size(), is(1));
        Program program1 = programs.iterator().next();
        assertThat(program1.getSource(), is("test"));
        assertThat(program1.getFactoryObjectDeps(Asset.class).size(), is(2));
        assertThat(program1.getFactoryObjectDeps(Asset.class), hasItem("assetX"));
        assertThat(program1.getFactoryObjectDeps(Asset.class), hasItem("assetZ"));
        assertThat(program1.getFactoryObjectDeps(TransportLayer.class).size(), is(1));
        assertThat(program1.getFactoryObjectDeps(TransportLayer.class), hasItem("tl7"));
        assertThat(program1.getProgramDeps().size(), is(1));
        assertThat(program1.getProgramDeps(), hasItem("saved-program5"));
        //check that values are as expected.
        programs =  m_SUT.getActiveProgramsUsingTemplate("saved-program2");
        assertThat(programs.size(), is(1));
        Program program2 = programs.iterator().next();
        assertThat(program2.getSource(), is("blah"));
        assertThat(program2.getFactoryObjectDeps(PhysicalLink.class).size(), is(1));
        assertThat(program2.getFactoryObjectDeps(PhysicalLink.class), hasItem("pl5"));
        assertThat(program2.getFactoryObjectDeps(LinkLayer.class).size(), is(1));
        assertThat(program2.getFactoryObjectDeps(LinkLayer.class), hasItem("ll1"));
    }
    
    /**
     * Verify that event handler unregisters for deactivation.
     */
    @Test
    public void testDeactivate()
    {
        setScriptEngineAndActivate();
        
        m_SUT.deactivate();
        verify(m_EventHandlerAnswer.getRegistration()).unregister();
    }
    
    /**
     * Test that the script engine will not initialize a script or execute the script until the deps are satisfied.
     * 
     * Verify that that script successfully initializes and executes once the deps (asset dependency) are satisfied.
     *
     */
    @Test
    public void testActivateAutoExecuteProg1AssetMissing() throws ScriptException,
        IllegalArgumentException, MissionProgramException
    {
        // mock
        mockMissionProgramPersistentData();
        mockTemplates();
        
        Asset mockAsset = mock(Asset.class);
        TransportLayer mockTrans = mock(TransportLayer.class);
        // make assetX and tl7 available
        when(m_AssetDirectoryService.isAssetAvailable("assetX")).thenReturn(true);
        when(m_AssetDirectoryService.getAssetByName("assetX")).thenReturn(mockAsset);
        when(m_CustomCommsService.isTransportLayerCreated("tl7")).thenReturn(true);
        when(m_CustomCommsService.getTransportLayer("tl7")).thenReturn(mockTrans);
        when(m_AssetDirectoryService.isAssetAvailable("asset3")).thenReturn(true);
        when(m_AssetDirectoryService.getAssetByName("asset3")).thenReturn(mockAsset);
        
        MissionScript missionMock = mock(MissionScript.class);
        
        when(m_ScriptEngine.eval(eq("test"), Mockito.any(Bindings.class))).thenReturn(new Object());
        when(((Invocable) m_ScriptEngine).getInterface(any(), eq(MissionScript.class))).thenReturn(missionMock);
        
        setScriptEngineAndActivate();
       
        // verify still not executed
        ArgumentCaptor<ScriptContext> context = ArgumentCaptor.forClass(ScriptContext.class);
        verify(m_ScriptEngine, never()).eval(eq("test"), context.capture());
        
        // make assetZ available
        when(m_AssetDirectoryService.isAssetAvailable("assetZ")).thenReturn(true);
        when(m_AssetDirectoryService.getAssetByName("assetZ")).thenReturn(mockAsset);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "assetZ");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        m_EventHandlerAnswer.getHandler().handleEvent(new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, props));
        
        // verify now called
        verify(m_ScriptEngine, times(1)).eval(eq("test"), context.capture());
        assertThat((Integer)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE).get("argA"), is(88));
        assertThat((String)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE).get("argB"), is("arg-b-value"));
    }
    
    /**
     * Test that the script engine will not initialize a script or execute the script until the deps are satisfied.
     * 
     * Verify that that script successfully initializes and executes once the deps (program dependency) are satisfied.
     */
    @Test
    public void testActivateAutoExecuteProg1ProgramMissing() throws ScriptException,
        IllegalArgumentException, MissionProgramException
    {
        // mock
        mockMissionProgramPersistentData();
        mockTemplates();
        
        //mock asset and transport layer
        Asset mockAsset = mock(Asset.class);
        TransportLayer mockTrans = mock(TransportLayer.class);
        // make assetX,Z and transport tl7 available
        when(m_AssetDirectoryService.isAssetAvailable("assetX")).thenReturn(true);
        when(m_AssetDirectoryService.isAssetAvailable("assetZ")).thenReturn(true);
        when(m_CustomCommsService.isTransportLayerCreated("tl7")).thenReturn(true);
        when(m_AssetDirectoryService.getAssetByName("assetX")).thenReturn(mockAsset);
        when(m_CustomCommsService.getTransportLayer("tl7")).thenReturn(mockTrans);
        when(m_AssetDirectoryService.getAssetByName("assetZ")).thenReturn(mockAsset);
        
        MissionScript missionMock = mock(MissionScript.class);
        when(m_ScriptEngine.eval(eq("test"), Mockito.any(Bindings.class))).thenReturn(new Object());
        when(((Invocable) m_ScriptEngine).getInterface(any(), eq(MissionScript.class))).thenReturn(missionMock);

        setScriptEngineAndActivate();
        
        // verify still not executed
        ArgumentCaptor<ScriptContext> context = ArgumentCaptor.forClass(ScriptContext.class);
        verify(m_ScriptEngine, never()).eval(eq("test"), context.capture());
        
        // make asset3 available
        when(m_AssetDirectoryService.isAssetAvailable("asset3")).thenReturn(true);
        when(m_AssetDirectoryService.getAssetByName("asset3")).thenReturn(mockAsset);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "asset3");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Asset");
        m_EventHandlerAnswer.getHandler().handleEvent(new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, props));
        
        // verify script 3 ran as well as script 1 since it depends on it
        verify(m_ScriptEngine, times(1)).eval(eq("something"), context.capture());
        verify(m_ScriptEngine, times(1)).eval(eq("test"), context.capture());
        assertThat((Integer)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE).get("argA"), is(88));
        assertThat((String)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE).get("argB"), is("arg-b-value"));
    }
    
    /**
     * Test that the script engine will not initialize a script or execute the script until the deps are satisfied.
     * 
     * Verify that that script successfully initializes and executes once the deps (Transport Layer) are satisfied.
     */
    @Test
    public void testActivateAutoExecuteProg1TransportLayerMissing() 
        throws ScriptException, IllegalArgumentException, MissionProgramException
    {
        // mock
        mockMissionProgramPersistentData();
        mockTemplates();
        
        //mock asset
        Asset mockAsset = mock(Asset.class);
        // make assetX and assetZ available
        when(m_AssetDirectoryService.isAssetAvailable("assetX")).thenReturn(true);
        when(m_AssetDirectoryService.isAssetAvailable("assetZ")).thenReturn(true);
        when(m_AssetDirectoryService.isAssetAvailable("asset3")).thenReturn(true);
        //mock return values for dep fulfillment
        when(m_AssetDirectoryService.getAssetByName("assetX")).thenReturn(mockAsset);
        when(m_AssetDirectoryService.getAssetByName("asset3")).thenReturn(mockAsset);
        when(m_AssetDirectoryService.getAssetByName("assetZ")).thenReturn(mockAsset);
        
        MissionScript missionMock = mock(MissionScript.class);
        when(m_ScriptEngine.eval(eq("test"), Mockito.any(Bindings.class))).thenReturn(new Object());
        when(((Invocable) m_ScriptEngine).getInterface(any(), eq(MissionScript.class))).thenReturn(missionMock);

        setScriptEngineAndActivate();
        
        // verify still not executed
        ArgumentCaptor<ScriptContext> context = ArgumentCaptor.forClass(ScriptContext.class);
        verify(m_ScriptEngine, never()).eval(eq("test"), context.capture());
        
        //mock transport layer
        TransportLayer mockTrans = mock(TransportLayer.class);
        
        // make transport layer available
        when(m_CustomCommsService.isTransportLayerCreated("tl7")).thenReturn(true);
        when(m_CustomCommsService.getTransportLayer("tl7")).thenReturn(mockTrans);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "tl7");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "TransportLayer");
        m_EventHandlerAnswer.getHandler().handleEvent(new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, props));
        
        // verify finally executed
        verify(m_ScriptEngine, times(1)).eval(eq("test"), context.capture());
        assertThat((Integer)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE).get("argA"), is(88));
        assertThat((String)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE).get("argB"), is("arg-b-value"));
        
        // verify not called again
        verify(m_ScriptEngine, times(1)).eval(eq("test"), context.capture());
    }
    
    /**
     * Test that the script engine will not initialize a script or execute the script until the deps are satisfied.
     * 
     * Verify that that script successfully initializes and executes once the deps (Physical Link) are satisfied.
     */
    @Test
    public void testActivateAutoExecuteProg2PhysicalLinkMissing() throws ScriptException,
        IllegalArgumentException, MissionProgramException
    {
        // mock
        mockMissionProgramPersistentData();
        mockTemplates();
        
        //mock deps
        LinkLayer link = mock(LinkLayer.class);
        PhysicalLink phys = mock(PhysicalLink.class);
        
        // make others available
        when(m_CustomCommsService.isLinkLayerCreated("ll1")).thenReturn(true);
        when(m_CustomCommsService.getLinkLayer("ll1")).thenReturn(link);
        
        MissionScript missionMock = mock(MissionScript.class);
        when(m_ScriptEngine.eval(eq("test"), Mockito.any(Bindings.class))).thenReturn(new Object());
        when(((Invocable) m_ScriptEngine).getInterface(any(), eq(MissionScript.class))).thenReturn(missionMock);
        
        setScriptEngineAndActivate();
        
        // verify still not executed
        ArgumentCaptor<ScriptContext> context = ArgumentCaptor.forClass(ScriptContext.class);
        verify(m_ScriptEngine, never()).eval(eq("blah"), context.capture());
        
        // make physical link available
        List<String> plNames = new ArrayList<String>();
        plNames.add("pl5");
        when(m_CustomCommsService.getPhysicalLinkNames()).thenReturn(plNames);
        when(m_CustomCommsService.requestPhysicalLink("pl5")).thenReturn(phys);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "pl5");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "PhysicalLink");
        m_EventHandlerAnswer.getHandler().handleEvent(new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, props));
        
        // verify now called
        verify(m_ScriptEngine, times(1)).eval(eq("blah"), context.capture());
    }
    
    /**
     * Test that the script engine will not initialize a script or execute the script until the deps are satisfied.
     * 
     * Verify that that script successfully initializes and executes once the deps (Link layer) are satisfied.
     */
    @Test
    public void testActivateAutoExecuteProg2LinkLayerMissing() throws ScriptException,
        IllegalArgumentException, MissionProgramException
    {
        // mock
        mockMissionProgramPersistentData();
        mockTemplates();
        
        //mock deps
        LinkLayer link = mock(LinkLayer.class);
        PhysicalLink phys = mock(PhysicalLink.class);
        
        // make others available
        List<String> plNames = new ArrayList<String>();
        plNames.add("pl5");
        when(m_CustomCommsService.getPhysicalLinkNames()).thenReturn(plNames);
        when(m_CustomCommsService.requestPhysicalLink("pl5")).thenReturn(phys);
        Set<String> isoNames = new HashSet<String>();
        isoNames.add("iso0");
        isoNames.add("iso1");
        
        MissionScript missionMock = mock(MissionScript.class);
        when(m_ScriptEngine.eval(eq("test"), Mockito.any(Bindings.class))).thenReturn(new Object());
        when(((Invocable) m_ScriptEngine).getInterface(any(), eq(MissionScript.class))).thenReturn(missionMock);
        
        setScriptEngineAndActivate();
        
        // verify still not executed
        ArgumentCaptor<ScriptContext> context = ArgumentCaptor.forClass(ScriptContext.class);
        verify(m_ScriptEngine, never()).eval(eq("blah"), context.capture());
        
        // make link layer available
        when(m_CustomCommsService.isLinkLayerCreated("ll1")).thenReturn(true);
        when(m_CustomCommsService.getLinkLayer("ll1")).thenReturn(link);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "ll1");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "LinkLayer");
        m_EventHandlerAnswer.getHandler().handleEvent(new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, props));
        
        // verify now called
        verify(m_ScriptEngine, times(1)).eval(eq("blah"), context.capture());
    }
    
    /**
     *  Verify mpm activates and template is added when asset is added.
     */
    @Test
    public void testActivateAndAddAsset() 
        throws IllegalArgumentException, PersistenceFailedException, MissionProgramException
    {
        // mock
        mockMissionProgramPersistentData();
        mockTemplates();
        
        // replay
        setScriptEngineAndActivate();
        
        MissionProgramTemplate program = new MissionProgramTemplate().withName("blah").withSource("asdf");
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("blah");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        doReturn(program).when(m_TemplateManager).getTemplate("blah");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withParameters().withTemplateName("blah").
            withSchedule(schedule).withProgramName("name");        
        m_SUT.loadParameters(params);
        
        //verify
        verify(m_PersistentDataStore).persist(eq(MissionProgramManagerImpl.class), (UUID)Mockito.any(), 
                eq("blah"), (Serializable)Mockito.any());
        
        assertThat(m_SUT.getActiveProgramTemplateNames(), hasItem("blah"));
    }
    
    /**
     * Verify the program once satisfied is not checked again at activation.
     */
    @Test
    public void testActivationProgramNotCheckedOnceSatisfied()
    {
        // mock templates and programs
        mockDependentMissionTemplates();
        mockDependentMissionPrograms();
        
        // replay
        setScriptEngineAndActivate();
        
        // verify not trying to execute program more than once
        verify(m_Logging, never()).debug(eq("The program [%s] has already executed, will not run"), anyVararg());
        
    }
    
    /**
     * Method used to set the script engine, activate the MPM, and capture the event
     * handler instance.
     */
    private void setScriptEngineAndActivate()
    {
        m_SUT.setScriptEngine(m_ScriptEngine);
        m_SUT.activate(m_Context);
    }
    
    /**
     * Test activate, templates not found at activation.
     * Verify programs not created.
     */
    @Test
    public void testActivateMissingMissionTemplates() 
    {
        // mock
        mockMissionProgramPersistentData();
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(new HashSet<String>());
        
        // replay
        setScriptEngineAndActivate();
        
        //verify no programs were able to be created
        assertThat(m_SUT.getPrograms().size(), is(0));
    }
    
    /**
     * Verifies that the Java 8 upgrade script can be loaded, and it fails if not loaded.
     */
    @Test
    public void testUpgradeScriptLoaded() throws MalformedURLException
    {
        // Test upgrade script load error
        m_SUT = new MissionProgramManagerImpl();        
        setupStandardMocks(); 
        when(m_Bundle.getEntry("nashornUpgrade.js")).thenReturn(null);        
      
        try
        {
            setScriptEngineAndActivate();
            fail();
        }
        catch (IllegalStateException e)
        {
            //Expected exception
            assertThat(e.getClass().equals(IllegalStateException.class), is(true));
        }

        // Test successful loading of upgrade script
        m_SUT = new MissionProgramManagerImpl();        
        setupStandardMocks();         
        when(m_Bundle.getEntry("nashornUpgrade.js")).thenReturn(new URL("file:.//resources//nashornUpgrade.js"));
        mockBindings();
        setScriptEngineAndActivate();
        
        //program to add       
        MissionProgramTemplate program2 = new MissionProgramTemplate().withName("testPass").withSource("testPass");
        MissionProgramSchedule schedule2 = new MissionProgramSchedule().withImmediately(true).
                withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withParameters().withTemplateName("testPass").
                withSchedule(schedule2).withProgramName("testPass");

        //add template
        Set<String> templateNames2 = new HashSet<String>();
        templateNames2.add("testPass");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames2);
        doReturn(program2).when(m_TemplateManager).getTemplate("testPass"); 
        
        Program returnedProg = m_SUT.loadParameters(params);        
        assertThat(returnedProg.getProgramStatus().equals(ProgramStatus.INITIALIZATION_ERROR), is(false));

    }
    
    /**
     * Verify program can be retrieved.
     */
    @Test
    public void testGetProgram() throws IllegalArgumentException, MissionProgramException, PersistenceFailedException
    {
        setScriptEngineAndActivate();
        
        try
        {
            //test that an exception is thrown if the program does not exist.(Is null).
            m_SUT.getProgram("blah");
            fail("Expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        
        // mock
        List<PersistentData> missionPrograms = new ArrayList<PersistentData>();
        doReturn(missionPrograms).when(m_PersistentDataStore).query(MissionProgramManagerImpl.class);
        PersistentData data = mock(PersistentData.class);
        missionPrograms.add(data);
        
        // replay
        MissionProgramTemplate program = new MissionProgramTemplate().withName("test").withSource("asdf");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false);
        MissionProgramParameters params = new MissionProgramParameters().withParameters().withTemplateName("test").
            withSchedule(schedule).withProgramName("name");
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("test");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        doReturn(program).when(m_TemplateManager).getTemplate("test");
        m_SUT.loadParameters(params);
    }
    
    /**
     * Verify program can be added.
     */
    @Test
    public void testAddProgram() 
        throws IllegalArgumentException, PersistenceFailedException, MissionProgramException
    {
        // mock
        setScriptEngineAndActivate();
        assertThat(m_SUT.getActiveProgramTemplateNames().size(), is(0));
        
        // replay        
        MissionProgramTemplate program = new MissionProgramTemplate().withName("blah").withSource("test");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withParameters().withTemplateName("blah").
            withSchedule(schedule).withProgramName("heyssdf");
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("blah");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        doReturn(program).when(m_TemplateManager).getTemplate("blah");
        
        // verify each call once
        Program program1 = m_SUT.loadParameters(params);
        verify(m_PersistentDataStore).persist(eq(MissionProgramManagerImpl.class), (UUID)Mockito.any(),
                eq("blah"), (Serializable)Mockito.any());
        
        // verify
        assertThat(program1, is(notNullValue()));
        assertThat(program1.getTemplateName(), is("blah"));
        assertThat(program1.getSource(), is("test"));
        assertThat(program1.getProgramName(), is("heyssdf"));
        
        assertThat(m_SUT.getActiveProgramTemplateNames().size(), is(1));        
        assertThat(m_SUT.getActiveProgramTemplateNames(), hasItem("blah"));
        
        // mock
        PersistentData foundData = mock(PersistentData.class);
        when(m_PersistentDataStore.find((UUID)Mockito.any())).thenReturn(foundData);

        // replay
        params = new MissionProgramParameters().withParameters().withTemplateName("hello").withSchedule(schedule).
                withProgramName("hey");
        schedule = new MissionProgramSchedule().withImmediately(false).
                withIndefiniteInterval(false);
        MissionProgramTemplate missionProgram = new MissionProgramTemplate().withName("hello").withSource("test");
        //add the name to the mocked list of templates.
        templateNames.add("hello");
        doReturn(missionProgram).when(m_TemplateManager).getTemplate("hello");
        
        ArgumentCaptor<Event> eventCap = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(3)).postEvent(eventCap.capture());
        
        assertThat((String)eventCap.getValue().getProperty(Program.EVENT_PROP_PROGRAM_NAME), is("heyssdf"));
                
        m_SUT.loadParameters(params);
        // verify that the new program was persisted, in addition to the other.
        verify(m_PersistentDataStore, times(1)).persist(eq(MissionProgramManagerImpl.class), (UUID)Mockito.any(),
                eq("hello"), (Serializable)Mockito.any());
        
        //check that the programs are in the set of programs managed.
        assertThat(m_SUT.getActiveProgramTemplateNames(), hasItem("blah"));
        assertThat(m_SUT.getActiveProgramTemplateNames(), hasItem("hello"));
        
        //check that there are ONLY two programs present.
        assertThat(m_SUT.getActiveProgramTemplateNames().size(), is(2));
    }
    
    /**
     * Test loading parameters that do not have a template name defined.
     * 
     * Verify error is thrown, and program is not created.
     */
    @Test
    public void testAddProgramNoTemplateName() 
        throws IllegalArgumentException, PersistenceFailedException, MissionProgramException
    {
        // mock
        setScriptEngineAndActivate();
        assertThat(m_SUT.getActiveProgramTemplateNames().size(), is(0));
        
        // replay
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withParameters().
            withSchedule(schedule).withProgramName("heyssdf");
        
        // verify 
        try
        {
            m_SUT.loadParameters(params);
            fail("The program is expected to fail creation because there is not a template name set.");
        }
        catch (IllegalArgumentException e)
        {
            //expecting exception
        }
        verify(m_PersistentDataStore, never()).persist(eq(MissionProgramManagerImpl.class), (UUID)Mockito.any(),
            anyString(), (Serializable)Mockito.any());
    }
    
    /**
     * Test removing a mission program.
     * Verify if executed that shutdown is called because the program is executed.
     */
    @Test
    public void testRemoveProgramExecuted() 
        throws IllegalArgumentException, PersistenceFailedException, MissionProgramException
    {
        mockMissionProgramPersistentData();
        List<PersistentData> missionPrograms = new ArrayList<PersistentData>();
        doReturn(missionPrograms).when(m_PersistentDataStore).query(MissionProgramManagerImpl.class);
        // mock
        setScriptEngineAndActivate();
        
        // verify
        assertThat(m_SUT.getActiveProgramTemplateNames().size(), is(0));
        
        //program to add       
        MissionProgramTemplate program = new MissionProgramTemplate().withName("blah").withSource("test");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withParameters().withTemplateName("blah").
            withSchedule(schedule).withProgramName("namerrrr");
        
        //add template
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("blah");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        doReturn(program).when(m_TemplateManager).getTemplate("blah");        
        
        //add params and schedule
        Program blahProgram = m_SUT.loadParameters(params);
        
       //need to capture uuid so that it can be used during the removeProgram verification
        ArgumentCaptor<UUID> uuid = ArgumentCaptor.forClass(UUID.class);
        verify(m_PersistentDataStore, times(1)).persist(eq(MissionProgramManagerImpl.class), uuid.capture(),
                anyString(), (Serializable)Mockito.any());
        assertThat(m_SUT.getActiveProgramTemplateNames().size(), is(1));
        
        ((ProgramImpl)blahProgram).changeStatus(ProgramStatus.CANCELED);

        //try to remove 
        m_SUT.removeProgram(blahProgram, uuid.getValue());
        //verify still removed
        assertThat(m_SUT.getActiveProgramsUsingTemplate("blah").size(), is(0));
        
        //verify exception if attempted to remove again
        try
        {
            //try to remove the program again. 
            m_SUT.removeProgram(blahProgram, uuid.getValue());
            fail("Expected exception the program was already removed.");
        }
        catch (IllegalArgumentException e)
        {
            //expecting exception
        }
    }
    
    /**
     * Test the execution of a mission script.
     * 
     * Verify that the mission is initialized before execution.
     * 
     * Verify that if initialization fails that the program does not execute.
     */
    @Test
    public void testExecuteProgram() 
        throws ScriptException, IllegalArgumentException, MissionProgramException, PersistenceFailedException
    {
        // mock
        MissionScript missionMock = mock(MissionScript.class);
        when(m_ScriptEngine.eval(eq("test"), Mockito.any(ScriptContext.class))).thenReturn(new Object());
        when(((Invocable) m_ScriptEngine).getInterface(any(), eq(MissionScript.class))).thenReturn(missionMock);
       
        AssetDirectoryService assetDirectoryService = mock(AssetDirectoryService.class);
        m_SUT.setAssetDirectoryService(assetDirectoryService);
        
        setScriptEngineAndActivate();
        
        //program to add
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("testArg", 282);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("blah").withSource("test");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(true).
            withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withProgramName("Bagel").
            withParameters(MapTranslator.translateMap(map)).withTemplateName("blah").withSchedule(schedule);
        //mocking
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("blah");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        doReturn(program).when(m_TemplateManager).getTemplate("blah");
        
        // replay
        m_SUT.loadParameters(params);
        ArgumentCaptor<ScriptContext> context = ArgumentCaptor.forClass(ScriptContext.class);
        verify(m_ScriptEngine, times(1)).eval(eq("test"), context.capture());
        verify(m_ScriptEngine, times(1)).put(ScriptEngine.FILENAME, "Bagel");
        assertThat((AssetDirectoryService)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE)
                .get(MissionProgramManager.ASSET_DIRECTORY_SERVICE),
                is(assetDirectoryService));
        assertThat((BundleContext)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE)
                .get(MissionProgramManager.BUNDLE_CONTEXT), is(m_Context));
        assertThat((PersistentDataStore)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE)
                .get(MissionProgramManager.PERSISTENT_DATA_STORE),
                is(m_PersistentDataStore));

        ArgumentCaptor<ProgramImpl> prog = ArgumentCaptor.forClass(ProgramImpl.class);
        verify(m_Scheduler).executeProgram(prog.capture());
        assertThat((Integer)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE).get("testArg"), is(282));

        //mock exception
        when(m_ScriptEngine.eval(eq("bad-code"), Mockito.any(ScriptContext.class))).thenThrow(new ScriptException(""));

        //program to add
        MissionProgramTemplate program2 = new MissionProgramTemplate().withName("program2").withSource("bad-code");
        schedule = new MissionProgramSchedule().withImmediately(true).withActive(true).
            withIndefiniteInterval(false);
        params = new MissionProgramParameters().withSchedule(schedule).withProgramName("Fret").
            withParameters(MapTranslator.translateMap(map)).withTemplateName("program2");
        templateNames.add("program2");
        doReturn(program2).when(m_TemplateManager).getTemplate("program2");

        Program badProgram = m_SUT.loadParameters(params);
        verify(m_ScriptEngine).put(ScriptEngine.FILENAME, "Fret");

        //verify that at initialization that the program failed
        verify(m_ScriptEngine, times(1)).eval(eq("bad-code"), context.capture());
        assertThat(badProgram.getProgramStatus(), is(ProgramStatus.INITIALIZATION_ERROR));
    }
    
    /**
     * Verify persisted programs are loaded.
     */
    @Test
    public void testPersistPrograms() 
        throws IllegalArgumentException, PersistenceFailedException, MissionProgramException
    {   
        mockMissionProgramPersistentData();
        mockTemplates();
        // mock
        setScriptEngineAndActivate();
        
        //program to add
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("arg1", "test-value");
        arguments.put("arg2", 5);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("test").withSource("something");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("test").
            withParameters(MapTranslator.translateMap(arguments)).withSchedule(schedule).withProgramName("asdf");
        
        //mocking
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("test");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        doReturn(program).when(m_TemplateManager).getTemplate("test");
        //load params and schedule
        m_SUT.loadParameters(params);
        
        // verify
        ArgumentCaptor<byte[]> serCaptor = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<UUID> uuid = ArgumentCaptor.forClass(UUID.class);        
        verify(m_PersistentDataStore).persist(eq(MissionProgramManagerImpl.class), uuid.capture(), eq("test"), 
                serCaptor.capture());
        assertThat(XmlUtils.fromXML(serCaptor.getValue(), MissionProgramParameters.class), 
                is(instanceOf(MissionProgramParameters.class))); 
    }
    
    /**
     * Verify that an event is properly ignored.
     */
    @Test
    public void testIgnoredEvent()
    {
        setScriptEngineAndActivate();
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "assetZ");
        // Address should be ignored but not throw exception
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "Address"); 
        
        m_EventHandlerAnswer.getHandler().handleEvent(new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, props));
    }
    
    /**
     * Test the binding, saving and validating of mission program parameters that are primitives.
     */
    @Test
    public void testExecArgPrimitives() throws IllegalArgumentException, PersistenceFailedException, 
           MissionProgramException
    {
        // mock
        mockMissionProgramPersistentData();
        mockTemplates();
        
        setScriptEngineAndActivate();
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("Pirates", 92);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("Name").withSource("Source");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false);
        MissionProgramParameters params = new MissionProgramParameters().withSchedule(schedule).
            withParameters(MapTranslator.translateMap(args)).withTemplateName("Name").withProgramName("sed");
        
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("Name");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        doReturn(program).when(m_TemplateManager).getTemplate("Name");
        
        m_SUT.loadParameters(params);
        ArgumentCaptor<byte[]> serializable = ArgumentCaptor.forClass(byte[].class);
        ArgumentCaptor<UUID> uuid = ArgumentCaptor.forClass(UUID.class);
        verify(m_PersistentDataStore).persist(eq(MissionProgramManagerImpl.class), uuid.capture(), eq("Name"), 
            serializable.capture());
        
        //check the params for the persisted object
        List<MapEntry> entry = ((MissionProgramParameters)XmlUtils.fromXML(serializable.getValue(),
                MissionProgramParameters.class)).getParameters();
        assertThat((Integer)entry.get(0).getValue(), is(92));
        
        //test strings
        args.clear();
        args.put("Shoe", "string");
        program = new MissionProgramTemplate().withName("Name1").withSource("Source");
        params = new MissionProgramParameters().withParameters(MapTranslator.translateMap(args)).
                withTemplateName("Name1").withSchedule(schedule).withProgramName("asdf");
        templateNames.add("Name1");
        doReturn(program).when(m_TemplateManager).getTemplate("Name1");
        
        m_SUT.loadParameters(params);
        verify(m_PersistentDataStore, times(1)).persist(eq(MissionProgramManagerImpl.class), uuid.capture(), 
                eq("Name1"), serializable.capture());
        //check the params for the persisted object
        entry = ((MissionProgramParameters)XmlUtils.fromXML(serializable.getValue(), MissionProgramParameters.class))
            .getParameters();
        assertThat((String)entry.get(0).getValue(), is("string"));
        
        //test doubles
        args.clear();
        args.put("Dog", 20.12);
        program = new MissionProgramTemplate().withName("Name2").withSource("Source");
        params = new MissionProgramParameters().withParameters(MapTranslator.translateMap(args)).
                withTemplateName("Name2").withSchedule(schedule).withProgramName("lok");
        templateNames.add("Name2");
        doReturn(program).when(m_TemplateManager).getTemplate("Name2");
        
        m_SUT.loadParameters(params);
        verify(m_PersistentDataStore, times(1)).persist(eq(MissionProgramManagerImpl.class), uuid.capture(), 
                eq("Name2"), serializable.capture());
        //check the autoargs for the persisted object
        entry = ((MissionProgramParameters)XmlUtils.fromXML(serializable.getValue(), MissionProgramParameters.class))
                .getParameters();
        assertThat((Double)entry.get(0).getValue(), is(20.12));

        //test floats
        args.clear();
        args.put("Impossible", (float).007);
        program = new MissionProgramTemplate().withName("Name3").withSource("Source");
        params = new MissionProgramParameters().withParameters(MapTranslator.translateMap(args)).
                withTemplateName("Name3").withSchedule(schedule).withProgramName("as");
        templateNames.add("Name3");
        doReturn(program).when(m_TemplateManager).getTemplate("Name3");
        
        m_SUT.loadParameters(params);
        verify(m_PersistentDataStore, times(1)).persist(eq(MissionProgramManagerImpl.class), uuid.capture(), 
                eq("Name3"), serializable.capture());
        //check the params for the persisted object
        entry = ((MissionProgramParameters)XmlUtils.fromXML(serializable.getValue(), MissionProgramParameters.class))
                .getParameters();
        assertThat((Float)entry.get(0).getValue(), is(.007f));
        
        //test shorts
        args.clear();
        args.put("Shorty", (short)123);
        program = new MissionProgramTemplate().withName("Name4").withSource("Source");
        params = new MissionProgramParameters().withParameters(MapTranslator.translateMap(args)).
                withTemplateName("Name4").withSchedule(schedule).withProgramName("prog4");
        templateNames.add("Name4");
        doReturn(program).when(m_TemplateManager).getTemplate("Name4");
        
        m_SUT.loadParameters(params);
        verify(m_PersistentDataStore, times(1)).persist(eq(MissionProgramManagerImpl.class), uuid.capture(), 
                eq("Name4"), serializable.capture());
        //check the params for the persisted object
        entry = ((MissionProgramParameters)XmlUtils.fromXML(serializable.getValue(), MissionProgramParameters.class))
                .getParameters();
        assertThat((Short)entry.get(0).getValue(), is((short)123));
    }
    
    /**
     * Test a mission program loaded that does not have a valid xml template.
     * Verify exception.
     */
    @Test
    public void testIllegalArgException() throws ValidationFailedException, MissionProgramException, 
        PersistenceFailedException
    {
        //mock exception response of a failed validation
        doThrow(new ValidationFailedException()).when(m_MissionProgramValidator)
            .validate(Mockito.any(MissionProgramTemplate.class));
        
        //program to add
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("arg1", "test-value");
        arguments.put("arg2", 5);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("test").withSource("something");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("test").
            withParameters(MapTranslator.translateMap(arguments)).withSchedule(schedule);
         
        //mocking
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("test");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        doReturn(program).when(m_TemplateManager).getTemplate("test");
        try
        {
            m_SUT.loadParameters(params);
            fail("Expected Exception");
        }
        catch (IllegalArgumentException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test getting active programs for a specific template.
     * 
     * Verify that all mock templates are returned for the getActivePrograms.
     * 
     * Verify the added program is the only program returned for the getActiveProgramsUsingTemplate request.
     * 
     * Validation and persistence exceptions are related to the loading process. The mission program exception
     * is thrown in the event that the initialization process fails or execution fails.
     */
    @Test
    public void testGetActiveProgramsUsingTemplate() throws ValidationFailedException, MissionProgramException, 
        PersistenceFailedException
    {
        //mock data
        mockMissionProgramPersistentData();
        mockTemplates();

        setScriptEngineAndActivate();
        
        //five templates are mocked in the 'mockTemplates'
        assertThat(m_SUT.getActiveProgramTemplateNames().size(), is(6));

        //program to add
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("arg1", "test-value");
        arguments.put("arg2", 5);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("test").withSource("something");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("test").
            withParameters(MapTranslator.translateMap(arguments)).withSchedule(schedule).withProgramName("asl");

        //load program
        doReturn(program).when(m_TemplateManager).getTemplate("test");
        Program mission = m_SUT.loadParameters(params);

        //get active templates using the same template name, named in the program just added
        assertThat(m_SUT.getActiveProgramsUsingTemplate("test"), hasItem(mission));

        //make sure the program is not in the inactive programs
        assertThat(m_SUT.getInactiveProgramsUsingTemplate("test").contains(mission), is(false));

        //check that the newly added program is returned when all programs are requested
        assertThat(m_SUT.getPrograms(), hasItem(mission));
        //five are already created, and one was added during this test
        assertThat(m_SUT.getPrograms().size(), is(7));
    }
    
    /**
     * Test getting inactive programs for a specific template.
     * 
     * Verify that only the added program is returned for the getInactiveProgramsUsingTemplate request.
     */
    @Test
    public void testGetInactiveProgramsUsingTemplate() throws ValidationFailedException, MissionProgramException, 
        PersistenceFailedException, ScriptException
    {
        //mock data
        mockMissionProgramPersistentData();
        mockTemplates();

        setScriptEngineAndActivate();
        
        //five templates are mocked in the 'mockTemplates'
        assertThat(m_SUT.getActiveProgramTemplateNames().size(), is(6));

        //program to add
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("arg1", "test-value");
        arguments.put("arg2", 5);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("test").withSource("something");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("test").
            withParameters(MapTranslator.translateMap(arguments)).withSchedule(schedule).withProgramName("name");

        //test getting the 
        doReturn(program).when(m_TemplateManager).getTemplate("test");
        Program mission = m_SUT.loadParameters(params);

        //verify that script object is not created
        verify(m_ScriptEngine, atMost(5)).eval(eq("something"), Mockito.any(Bindings.class));

        //get inactive templates using the same template name, named in the program just added
        assertThat(m_SUT.getInactiveProgramsUsingTemplate("test"), hasItem(mission));
        assertThat(m_SUT.getActiveProgramsUsingTemplate("test").contains(mission), is(false));

        //check that the inactive program is returned when all programs are requested
        assertThat(m_SUT.getPrograms(), hasItem(mission));
    }
    
    /**
     * Test getting a specific program by name.
     * 
     * Verify that all mock templates are returned for the getPrograms request.
     * 
     * Verify that if getProgram using a name returns the expected result.
     */
    @Test
    public void testGetPrograms() throws ValidationFailedException, MissionProgramException, 
        PersistenceFailedException
    {
        //mock data
        mockMissionProgramPersistentData();
        mockTemplates();

        setScriptEngineAndActivate();
        
        //program to add
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("arg1", "test-value");
        arguments.put("arg2", 5);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("test").withSource("something");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("test").
            withParameters(MapTranslator.translateMap(arguments)).withSchedule(schedule).withProgramName("iokk");

        //test getting the 
        doReturn(program).when(m_TemplateManager).getTemplate("test");
        Program mission = m_SUT.loadParameters(params);

        //five templates are mocked in the 'mockTemplates'
        List<Program> programs = new ArrayList<Program>(m_SUT.getPrograms());

        //grab the program added check that its name is assigned since one is not specified in the
        //params
        int indexOfAddedProg = programs.indexOf(mission);
        Program programFromList = programs.get(indexOfAddedProg);
        assertThat(programFromList.getProgramName().isEmpty(), is(false));

        //verify getting a program by name
        assertThat(m_SUT.getProgram(programFromList.getProgramName()), is(mission));
    }
    
    /**
     * Test loading parameters when the desired name overlaps with another program.
     * 
     * Verify IllegalArgumentException is thrown and that the new program was not created.
     */
    @Test
    public void testLoadParamsException() throws ValidationFailedException, MissionProgramException, 
        PersistenceFailedException
    {
        //mock data
        mockMissionProgramPersistentData();
        mockTemplates();

        setScriptEngineAndActivate();
        
        //program to add
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("arg1", "test-value");
        arguments.put("arg2", 5);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("test").withSource("something");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("test").
            withParameters(MapTranslator.translateMap(arguments)).withProgramName("Jim").withSchedule(schedule);

        //test getting the 
        doReturn(program).when(m_TemplateManager).getTemplate("test");
        m_SUT.loadParameters(params);

        //five templates are mocked in the 'mockTemplates'
        assertThat(m_SUT.getPrograms().size(), is(7));

        //try to load the same parameters
        try
        {
            m_SUT.loadParameters(params);
            fail("Expected exception because the name of the program overlaps with another program!");
        }
        catch (IllegalArgumentException e)
        {
            //expecting exception
        }

        //verify that the program was not recreated
        assertThat(m_SUT.getPrograms().size(), is(7));
    }
    
    /**
     * Test loading parameters initialization process.
     * 
     * Verify inactive program does not get initialized.
     */
    @Test
    public void testLoadParamsInactive() throws ValidationFailedException, MissionProgramException, 
        PersistenceFailedException, ScriptException
    {
        //mock data
        mockMissionProgramPersistentData();
        mockTemplates();

        setScriptEngineAndActivate();
        
        //program to add
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("arg1", "test-value");
        arguments.put("arg2", 5);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("test").withSource("something");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("test").
            withParameters(MapTranslator.translateMap(arguments)).withProgramName("Jim").withSchedule(schedule);

        //load program
        doReturn(program).when(m_TemplateManager).getTemplate("test");
        m_SUT.loadParameters(params);

        //verify not initialized, could throw script exception
        verify(m_ScriptEngine, never()).eval(eq("something"), Mockito.any(Bindings.class));

        //five templates are mocked in the 'mockTemplates'
        assertThat(m_SUT.getPrograms().size(), is(7));
    }
    
    /**
     * Test loading parameters initialization process.
     * 
     * Verify that if the script fails initialization that the program cannot be executed.
     */
    @Test
    public void testLoadParamsInitException() throws ValidationFailedException, MissionProgramException, 
        PersistenceFailedException, ScriptException
    {
        //mock data
        mockMissionProgramPersistentData();
        mockTemplates();

        setScriptEngineAndActivate();
        
        //program to add
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("arg1", "test-value");
        arguments.put("arg2", 5);
        MissionProgramTemplate program = new MissionProgramTemplate().withName("test").withSource("something");
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(false).
            withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("test").
            withParameters(MapTranslator.translateMap(arguments)).withProgramName("Jim").withSchedule(schedule);

        //mock script exception during init
        when(m_ScriptEngine.eval(eq("something"), Mockito.any(Bindings.class))).thenThrow(
            new ScriptException("Exception"));

        //load program
        doReturn(program).when(m_TemplateManager).getTemplate("test");
        m_SUT.loadParameters(params);
       
        //verify not initialized, could throw script exception
        verify(m_ScriptEngine, times(1)).eval(eq("something"), Mockito.any(ScriptContext.class));

        //verify that the program was not recreated
        assertThat(m_SUT.getPrograms().size(), is(7));
    }
    
    /**
    * Test the 'test' execution of a mission script.
    * Verify event posted.
    */
    @Test
    public void testTestExecuteProgram() 
        throws Exception
    {
        // mock mission script object
        MissionScript missionMock = mock(MissionScript.class);
        when(missionMock.test()).thenReturn(TestResult.PASSED);

        //mock program
        ProgramImpl program = mock(ProgramImpl.class);
        when(program.getMissionScript()).thenReturn(missionMock);
        when(program.getProgramName()).thenReturn("Random message!");
      
        AssetDirectoryService assetDirectoryService = mock(AssetDirectoryService.class);
        m_SUT.setAssetDirectoryService(assetDirectoryService);
        setScriptEngineAndActivate();

        //behavior from scheduler
        @SuppressWarnings("unchecked")
        Future<TestResult> future = mock(Future.class);
        when(future.get()).thenReturn(TestResult.PASSED);
       
        doReturn(future).when(m_Scheduler).testProgram(program);
       
        //call the method
        Future<TestResult> result = m_SUT.testProgram(program);

        //verify result
        assertThat(result.get(), is(TestResult.PASSED));
    }
   
    /**
    * Test used to verify that bug with program dependencies is fixed. Verification method for TH-2542
    */
    @Test
    public void testProgramActivationInWhichProgramDependsOnAnotherProgram() throws InterruptedException
    {
        final MockWaitRunnable runner = new MockWaitRunnable();
        final Thread runningThread = new Thread(runner);
       
        //mock the behavior in the scheduler service
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                ProgramImpl prog = (ProgramImpl)invocation.getArguments()[0];
           
                //if statement here to make sure that the program1 simulates
                //a program that takes long to initialize. All other programs 
                //are to change their status immediately.
                if (prog.getProgramName().equals("program1"))
                {
                    runner.setProgram(prog);
                    runningThread.start();
                }
                else
                {
                    prog.changeStatus(ProgramStatus.EXECUTING);
                    prog.changeStatus(ProgramStatus.EXECUTED);
                    
                    //post event
                    Map<String, Object> props = new HashMap<String, Object>();
                    props.put(Program.EVENT_PROP_PROGRAM_NAME, prog.getProgramName());
                    m_EventHandlerAnswer.getHandler().handleEvent(new Event(Program.TOPIC_PROGRAM_EXECUTED, props));
                }
                
                return null;
            }
        }).when(m_Scheduler).executeProgram(Mockito.any(ProgramImpl.class));
   
        mockDependentMissionTemplates();
        mockProgram2DependsProgram1();
   
        setScriptEngineAndActivate();
   
        //wait for long simulation of program changing states to end.
        runningThread.join();
   
        Program program1 = m_SUT.getProgram("program1");
        Program program2 = m_SUT.getProgram("program2");
       
        assertThat(program1.getProgramStatus(), is(ProgramStatus.EXECUTED));
        assertThat(program2.getProgramStatus(), is(ProgramStatus.EXECUTED));
    }
    
    /**
     * Test the 'test' execution of a mission script where the test fails.
     * Verify event posted.
     */
    @Test
    public void testTestExecuteProgramFail() 
        throws Exception
    {
        // mock mission script object
        MissionScript missionMock = mock(MissionScript.class);
        when(missionMock.test()).thenReturn(TestResult.FAILED);

        //mock program
        ProgramImpl program = mock(ProgramImpl.class);
        when(program.getMissionScript()).thenReturn(missionMock);
        when(program.getProgramName()).thenReturn("Random message!");
       
        AssetDirectoryService assetDirectoryService = mock(AssetDirectoryService.class);
        m_SUT.setAssetDirectoryService(assetDirectoryService);
        setScriptEngineAndActivate();

        //behavior from scheduler
        @SuppressWarnings("unchecked")
        Future<TestResult> future = mock(Future.class);
        when(future.get()).thenReturn(TestResult.FAILED);
        
        doReturn(future).when(m_Scheduler).testProgram(program);
        
        //call the method
        Future<TestResult> result = m_SUT.testProgram(program);

        //verify result
        assertThat(result.get(), is(TestResult.FAILED));
    }
    
    /**
     * Verify {@link MessageFactory} are bound correctly.
     */
    @Test
    public void testMessageFactory() throws Exception
    {
        assertOptionalService(MissionProgramManager.MESSAGE_FACTORY, MessageFactory.class, 
                new Consumer<MessageFactory>()
                {
                    @Override
                    public void consume(MessageFactory service)
                    {
                        m_SUT.setMessageFactory(service);
                    }
                }, 
                new Consumer<MessageFactory>()
                {
                    @Override
                    public void consume(MessageFactory service)
                    {
                        m_SUT.unsetMessageFactory(service);
                    }
                });
    }
    
    /**
     * Verify {@link RemoteSystemEncryption} are bound correctly.
     */
    @Test
    public void testRemoteSystemEncryption() throws Exception
    {
        assertOptionalService(MissionProgramManager.REMOTE_SYSTEM_ENCRYPTION, RemoteSystemEncryption.class, 
                new Consumer<RemoteSystemEncryption>()
                {
                    @Override
                    public void consume(RemoteSystemEncryption service)
                    {
                        m_SUT.setRemoteSystemEncryption(service);
                    }
                }, 
                new Consumer<RemoteSystemEncryption>()
                {
                    @Override
                    public void consume(RemoteSystemEncryption service)
                    {
                        m_SUT.unsetRemoteSystemEncryption(service);
                    }
                });
    }
    
    /**
     * Verify {@link RemoteChannelLookup} are bound correctly.
     */
    @Test
    public void testRemoteChannelLookup() throws Exception
    {
        assertOptionalService(MissionProgramManager.REMOTE_CHANNEL_LOOKUKP, RemoteChannelLookup.class, 
                new Consumer<RemoteChannelLookup>()
                {
                    @Override
                    public void consume(RemoteChannelLookup service)
                    {
                        m_SUT.setRemoteChannelLookup(service);
                    }
                }, 
                new Consumer<RemoteChannelLookup>()
                {
                    @Override
                    public void consume(RemoteChannelLookup service)
                    {
                        m_SUT.unsetRemoteChannelLookup(service);
                    }
                });
    }
    
    /**
     * Verify {@link PowerManager} are bound correctly.
     */
    @Test
    public void testPowerManager() throws Exception
    {
        assertOptionalService(MissionProgramManager.POWER_MANAGER, PowerManager.class, 
                new Consumer<PowerManager>()
                {
                    @Override
                    public void consume(PowerManager service)
                    {
                        m_SUT.setPowerManager(service);
                    }
                }, 
                new Consumer<PowerManager>()
                {
                    @Override
                    public void consume(PowerManager service)
                    {
                        m_SUT.unsetPowerManager(service);
                    }
                });
    }
    
    /**
     * Verify {@link DevicePowerManager} are bound correctly.
     */
    @Test
    public void testDevicePowerManager() throws Exception
    {
        assertOptionalService(MissionProgramManager.DEVICE_POWER_MANAGER, DevicePowerManager.class, 
                new Consumer<DevicePowerManager>()
                {
                    @Override
                    public void consume(DevicePowerManager service)
                    {
                        m_SUT.setDevicePowerManager(service);
                    }
                }, 
                new Consumer<DevicePowerManager>()
                {
                    @Override
                    public void consume(DevicePowerManager service)
                    {
                        m_SUT.unsetDevicePowerManager(service);
                    }
                });
    }
    
    /**
     * Test the shutdown execution of a mission script.
     */
    @Test
    public void testShutdownExecution() throws MissionProgramException
    {
        //mock program
        ProgramImpl program = mock(ProgramImpl.class);
        when(program.getProgramName()).thenReturn("shutdownProgramName");
        
        //call the method
        m_SUT.shutdownProgram(program);

        verify(m_Scheduler).shutdownProgram(program);
    }
    
    /**
     * Test the canceling of a mission.
     */
    @Test
    public void testCancel() throws MissionProgramException
    {
        //mock program
        ProgramImpl program = mock(ProgramImpl.class);
        when(program.getProgramName()).thenReturn("cancelProgramName");
        
        //call the method
        m_SUT.cancelProgram(program.getProgramName());

        verify(m_Scheduler).cancelScheduledProgram("cancelProgramName");
    }
    
    /**
     * Templates used for {@link #testActivationProgramNotCheckedOnceSatisfied()}.
     */
    private void mockDependentMissionTemplates()
    {
        // variable metadata
        MissionVariableMetaData varMeta =  new MissionVariableMetaData().withName("program").
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        
        //mission templates
        MissionProgramTemplate programData1 = new MissionProgramTemplate().withSource("test")
                .withVariableMetaData(varMeta).withName("program");
        MissionProgramTemplate programData2 = new MissionProgramTemplate().withSource("test")
                .withName("program-nodeps");
               
        //mock out templates
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("program");
        templateNames.add("program-nodeps");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        
        when(m_TemplateManager.getTemplate("program")).thenReturn(programData1);
        when(m_TemplateManager.getTemplate("program-nodeps")).thenReturn(programData2);
    }
    
    /**
     * Method to mock two programs program1 and program2. Program2 is dependent on 
     * program1.
     */
    private void mockProgram2DependsProgram1()
    {
        List<PersistentData> missionPrograms = new ArrayList<>();
        
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
                withImmediately(true).withAtReset(true).withActive(true);
        
        Map<String, Object> paramArgs = new HashMap<>();
        paramArgs.put("program", "program1");
        
        MissionProgramParameters params1 = new MissionProgramParameters().
                withSchedule(schedule).withProgramName("program1").withTemplateName("program-nodeps");
        MissionProgramParameters params2 = new MissionProgramParameters().withSchedule(schedule).
                withParameters(MapTranslator.translateMap(paramArgs)).withProgramName("program2").
                withTemplateName("program");
        
        PersistentData dataParam1 = mock(PersistentData.class);
        PersistentData dataParam2 = mock(PersistentData.class);
        
        when((byte[])dataParam1.getEntity()).thenReturn(XmlUtils.toXML(params1, true));
        when(dataParam1.getDescription()).thenReturn("program1");
        when((byte[])dataParam2.getEntity()).thenReturn(XmlUtils.toXML(params2, true));
        when(dataParam2.getDescription()).thenReturn("program2");
        
        missionPrograms.add(dataParam1);
        missionPrograms.add(dataParam2);
        
        doReturn(missionPrograms).when(m_PersistentDataStore).query(MissionProgramManagerImpl.class);
        
    }
    
    /*
     * Mission parameters that join with mission templates to create mission programs. 
     */
    private void mockMissionProgramPersistentData()
    {
        //mock the persistent data store for mission programs
        List<PersistentData> missionPrograms = new ArrayList<PersistentData>();
        //these are used by all templates
        Map<String, Object> executeArgs = new HashMap<String, Object>();
        executeArgs.put("argA", 88);
        executeArgs.put("argB", "arg-b-value");
        
        //schedule that all the missions use
        MissionProgramSchedule schedule1 = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(true).withAtReset(true).withActive(true);
        //schedule for mission that does not run after restart
        MissionProgramSchedule schedule2 = new MissionProgramSchedule().withIndefiniteInterval(false).
                withImmediately(true).withAtReset(false).withActive(true);
        
        //create params individually for each template, note that although these values are the same
        //in an actual program they need not be, the key is the name of the variable, the value is the 
        //is the variables value, ie I might have a variable named asset, but with a value of ExampleAsset.
        Map<String, Object> paramArgs = new HashMap<String, Object>();
        paramArgs.putAll(executeArgs);
        paramArgs.put("assetX", "assetX");
        paramArgs.put("assetZ", "assetZ");
        paramArgs.put("tl7", "tl7");
        paramArgs.put("saved-program5", "saved-program5");
        MissionProgramParameters params1 = new MissionProgramParameters().withSchedule(schedule1).
            withProgramName("saved-program1").withParameters(MapTranslator.translateMap(paramArgs)).
                withTemplateName("saved-program1");
        
        paramArgs.clear();
        paramArgs.putAll(executeArgs);
        paramArgs.put("pl5", "pl5");
        paramArgs.put("iso0", "iso0");
        paramArgs.put("iso1", "iso1");
        paramArgs.put("ll1", "ll1");
        MissionProgramParameters params2 = new MissionProgramParameters().withSchedule(schedule1).
            withProgramName("saved-program2").withParameters(MapTranslator.translateMap(paramArgs)).
                withTemplateName("saved-program2");
        
        paramArgs.clear();
        paramArgs.putAll(executeArgs);
        paramArgs.put("asset3", "asset3");
        MissionProgramParameters params3 = new MissionProgramParameters().withSchedule(schedule1).
            withProgramName("saved-program3").withParameters(MapTranslator.translateMap(paramArgs)).
                withTemplateName("saved-program3");
        
        paramArgs.clear();
        paramArgs.putAll(executeArgs);
        paramArgs.put("bogus-program", "bogus-program");
        MissionProgramParameters params4 = new MissionProgramParameters().withSchedule(schedule1).
            withProgramName("saved-program4").withParameters(MapTranslator.translateMap(paramArgs)).
                withTemplateName("saved-program4");
        
        paramArgs.clear();
        paramArgs.putAll(executeArgs);
        paramArgs.put("saved-program3", "saved-program3");
        MissionProgramParameters params5 = new MissionProgramParameters().withSchedule(schedule1).
            withProgramName("saved-program5").withParameters(MapTranslator.translateMap(paramArgs)).
                withTemplateName("saved-program5");
        
        paramArgs.clear();
        paramArgs.putAll(executeArgs);
        paramArgs.put("saved-program3", "saved-program3");
        MissionProgramParameters params6 = new MissionProgramParameters().withSchedule(schedule2).
            withProgramName("saved-program6").withParameters(MapTranslator.translateMap(paramArgs)).
                withTemplateName("saved-program6");
        
        //mock the schedule and parameter values
        PersistentData dataParam1 = mock(PersistentData.class);
        PersistentData dataParam2 = mock(PersistentData.class);
        PersistentData dataParam3 = mock(PersistentData.class);
        PersistentData dataParam4 = mock(PersistentData.class);
        PersistentData dataParam5 = mock(PersistentData.class);
        PersistentData dataParam6 = mock(PersistentData.class);
        
        //mock response from data store when iterating over query results        
        when((byte[])dataParam1.getEntity()).thenReturn(XmlUtils.toXML(params1, true));
        when((byte[])dataParam2.getEntity()).thenReturn(XmlUtils.toXML(params2, true));
        when((byte[])dataParam3.getEntity()).thenReturn(XmlUtils.toXML(params3, true));
        when((byte[])dataParam4.getEntity()).thenReturn(XmlUtils.toXML(params4, true));
        when((byte[])dataParam5.getEntity()).thenReturn(XmlUtils.toXML(params5, true));
        when((byte[])dataParam6.getEntity()).thenReturn(XmlUtils.toXML(params6, true));
        
        //Add the mocked PersistantData to the list
        missionPrograms.add(dataParam1);
        missionPrograms.add(dataParam2);
        missionPrograms.add(dataParam3);
        missionPrograms.add(dataParam4);
        missionPrograms.add(dataParam5);
        missionPrograms.add(dataParam6);
        
        
        //mocked response when data store is queried 
        doReturn(missionPrograms).when(m_PersistentDataStore).query(MissionProgramManagerImpl.class);        
        
        //mock responses for get UUID
        when(dataParam1.getDescription()).thenReturn("saved-program1");
        when(dataParam2.getDescription()).thenReturn("saved-program2");
        when(dataParam3.getDescription()).thenReturn("saved-program3");
        when(dataParam4.getDescription()).thenReturn("saved-program4");
        when(dataParam5.getDescription()).thenReturn("saved-program5");
        when(dataParam6.getDescription()).thenReturn("saved-program6");
    }
    
    /**
     * Create mission programs that depend on each other, used by
     * {@link #testActivationProgramNotCheckedOnceSatisfied()}. 
     * 
     * P1 -> P2 -> P3, all should be executed on activation
     */
    private void mockDependentMissionPrograms()
    {
        //mock the persistent data store for mission programs
        List<PersistentData> missionPrograms = new ArrayList<PersistentData>();
        
        //schedule that all the missions use
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(true).withAtReset(true).withActive(true);
        
        //create params individually for each template, note that although these values are the same
        //in an actual program they need not be, the key is the name of the variable, the value is the 
        //is the variables value, ie I might have a variable named asset, but with a value of ExampleAsset.
        Map<String, Object> paramArgs = new HashMap<String, Object>();
        paramArgs.put("program", "program2");
        MissionProgramParameters params1 = new MissionProgramParameters().withSchedule(schedule).
            withProgramName("program1").withParameters(MapTranslator.translateMap(paramArgs)).
                withTemplateName("program");
        
        paramArgs.put("program", "program3");
        MissionProgramParameters params2 = new MissionProgramParameters().withSchedule(schedule).
            withProgramName("program2").withParameters(MapTranslator.translateMap(paramArgs)).
                withTemplateName("program");
        
        MissionProgramParameters params3 = new MissionProgramParameters().withSchedule(schedule).
            withProgramName("program3").withParameters(MapTranslator.translateMap(paramArgs)).
                withTemplateName("program-nodeps");
        
        
        //mock the schedule and parameter values
        PersistentData dataParam1 = mock(PersistentData.class);
        PersistentData dataParam2 = mock(PersistentData.class);
        PersistentData dataParam3 = mock(PersistentData.class);
        
        //mock response from data store when iterating over query results        
        when((byte[])dataParam1.getEntity()).thenReturn(XmlUtils.toXML(params1, true));
        when((byte[])dataParam2.getEntity()).thenReturn(XmlUtils.toXML(params2, true));
        when((byte[])dataParam3.getEntity()).thenReturn(XmlUtils.toXML(params3, true));
        
        //Add the mocked PersistantData to the list
        missionPrograms.add(dataParam1);
        missionPrograms.add(dataParam2);
        missionPrograms.add(dataParam3);
        
        //mocked response when data store is queried 
        doReturn(missionPrograms).when(m_PersistentDataStore).query(MissionProgramManagerImpl.class);        
        
        //mock responses for get UUID
        when(dataParam1.getDescription()).thenReturn("program1");
        when(dataParam2.getDescription()).thenReturn("program2");
        when(dataParam3.getDescription()).thenReturn("program3");
    }
    
    /**
     * Templates used to test restoring of mission programs.
     */
    private void mockTemplates()
    {
        // variable metadata
        MissionVariableMetaData varMeta =  new MissionVariableMetaData().withName("assetX").withType(
                    MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData varMeta2 =  new MissionVariableMetaData().withName("assetZ").withType(
                    MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData varMeta3 =  new MissionVariableMetaData().withName("tl7").withType(
                    MissionVariableTypesEnum.TRANSPORT_LAYER);
        MissionVariableMetaData varMeta5 =  new MissionVariableMetaData().withName("saved-program5").
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        MissionVariableMetaData varMeta6 =  new MissionVariableMetaData().withName("pl5").withType(
                    MissionVariableTypesEnum.PHYSICAL_LINK);
        MissionVariableMetaData varMeta9 =  new MissionVariableMetaData().withName("asset3").withType(
                    MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData varMeta10 =  new MissionVariableMetaData().withName("bogus-program").
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        MissionVariableMetaData varMeta11 =  new MissionVariableMetaData().withName("ll1").
            withType(MissionVariableTypesEnum.LINK_LAYER);
        MissionVariableMetaData varMeta12 =  new MissionVariableMetaData().withName("saved-program3").
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        
        //mission templates
        MissionProgramTemplate programData1 = new MissionProgramTemplate().withSource("test").
            withVariableMetaData(varMeta, varMeta2, varMeta3, varMeta5 ).withName("saved-program1");
           
        MissionProgramTemplate programData2 = new MissionProgramTemplate().withSource("blah").
            withName("saved-program2").withVariableMetaData(varMeta6, varMeta11);
        
        MissionProgramTemplate programData3 = new MissionProgramTemplate().withSource("something").
            withName("saved-program3").withVariableMetaData(varMeta9);
        
        MissionProgramTemplate programData4 = new MissionProgramTemplate().withSource("blahblah").
            withName("saved-program4").withVariableMetaData(varMeta10);
        
        MissionProgramTemplate programData5 = new MissionProgramTemplate().withSource("huh").
            withName("saved-program5").withVariableMetaData(varMeta12);
        
        MissionProgramTemplate programData6 = new MissionProgramTemplate().withSource("huh2").
                withName("saved-program6").withVariableMetaData(varMeta12);
            
        //mock out templates
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("saved-program1");
        templateNames.add("saved-program2");
        templateNames.add("saved-program3");
        templateNames.add("saved-program4");
        templateNames.add("saved-program5");
        templateNames.add("saved-program6");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);
        
        when(m_TemplateManager.getTemplate("saved-program1")).thenReturn(programData1);
        when(m_TemplateManager.getTemplate("saved-program2")).thenReturn(programData2);
        when(m_TemplateManager.getTemplate("saved-program3")).thenReturn(programData3);
        when(m_TemplateManager.getTemplate("saved-program4")).thenReturn(programData4);
        when(m_TemplateManager.getTemplate("saved-program5")).thenReturn(programData5);
        when(m_TemplateManager.getTemplate("saved-program6")).thenReturn(programData6);
    }

    /**
     * Class used to simulate a long period of time for program status to change to executed.
     * 
     * @author nickmarcucci
     *
     */
    private class MockWaitRunnable implements Runnable
    {
        private ProgramImpl m_Program;
        
        public void setProgram(ProgramImpl program)
        {
            m_Program = program;
        }

        @Override
        public void run()
        {
            try
            {
                //used to 
                Thread.sleep(5000);
            }
            catch (InterruptedException e)
            {
                System.out.println("Thread was interrupted simulating long program initialization!!!!");
                return;
            }
            
            m_Program.changeStatus(ProgramStatus.EXECUTING);
            m_Program.changeStatus(ProgramStatus.EXECUTED);
            
            //post event
            Map<String, Object> props = new HashMap<String, Object>();
            props.put(Program.EVENT_PROP_PROGRAM_NAME, m_Program.getProgramName());
            m_EventHandlerAnswer.getHandler().handleEvent(new Event(Program.TOPIC_PROGRAM_EXECUTED, props));
        }
        
    }

    /**
     * Method asserts the given service class is correctly bound as a variable.
     * 
     * @param variable
     *      name of variable used for the service
     */
    @SuppressWarnings({"unchecked"})
    private <T> void assertOptionalService(String variable, Class<T> serviceClass, Consumer<T> bind, 
            Consumer<T> unbind) throws Exception
    {
        setScriptEngineAndActivate();

        T service = mock(serviceClass);
        bind.consume(service);

        verify(m_ScriptEngine).put(variable, service);

        //mock template manager
        Set<String> templateNames = new HashSet<String>();
        templateNames.add("blah");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(templateNames);

        //mission
        MissionProgramTemplate programModel = new MissionProgramTemplate().withSource("test").withName("blah");
        when(m_TemplateManager.getTemplate("blah")).thenReturn(programModel);
        MissionProgramSchedule schedule = new MissionProgramSchedule().withImmediately(true).
                withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("blah").
                withSchedule(schedule).withProgramName("name");

        //load the params
        Program program = m_SUT.loadParameters(params);

        //check that the binding was processed
        ArgumentCaptor<ScriptContext> context = ArgumentCaptor.forClass(ScriptContext.class);
        verify(m_ScriptEngine, times(1)).eval(eq("test"), context.capture());
        assertThat((T)context.getValue().getBindings(ScriptContext.ENGINE_SCOPE).get(variable), is(service));

        m_SUT.removeProgram(program, null);

        unbind.consume(service);

        //mission
        MissionProgramSchedule schedule2 = new MissionProgramSchedule().withImmediately(true).
                withIndefiniteInterval(false).withActive(true);
        MissionProgramParameters params2 = new MissionProgramParameters().withTemplateName("blah").
                withSchedule(schedule2).withProgramName("name2");

        program = m_SUT.loadParameters(params2);

        //behavior from scheduler
        Future<?> future = mock(Future.class);
        when(future.get()).thenReturn(null);

        //execute program
        program.execute();
        verify(m_ScriptEngine).put(variable, null);
        verify(m_ScriptEngine, times(2)).eval(eq("test"), context.capture());
        assertThat(context.getValue().getBindings(ScriptContext.ENGINE_SCOPE).get(variable), is(nullValue()));
        
        m_SUT.removeProgram(program, null);
    }
    
    @SuppressWarnings("unchecked")
    private void mockBindings()
    {
        m_BindingsMap = new HashMap<>();
        when(m_ScriptEngine.getBindings(ScriptContext.ENGINE_SCOPE)).thenReturn(m_Bindings);
        
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Map<String, Object> bindings = (Map<String, Object>)invocation.getArguments()[0];
                m_BindingsMap.putAll(bindings);
                return null;
            }
        }).when(m_Bindings).putAll(Mockito.anyMap());
        
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                final String key = (String)invocation.getArguments()[0];
                final Object value = invocation.getArguments()[1];
                return m_BindingsMap.put(key, value);
            }
        }).when(m_Bindings).put(Mockito.anyString(), Mockito.any());
        
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                final String key = (String)invocation.getArguments()[0];
                return m_BindingsMap.get(key);
            }
        }).when(m_Bindings).get(Mockito.any());
        
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                final String key = (String)invocation.getArguments()[0];
                final Object value = invocation.getArguments()[1];
                return m_BindingsMap.put(key, value);
            }
        }).when(m_ScriptEngine).put(Mockito.anyString(), Mockito.any());
        
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                m_BindingsMap = new HashMap<>();
                return m_Bindings;
            }
        }).when(m_ScriptEngine).createBindings();
    }
}
