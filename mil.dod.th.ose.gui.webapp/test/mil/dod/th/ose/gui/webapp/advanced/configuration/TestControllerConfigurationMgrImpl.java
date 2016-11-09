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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ControllerConfigurationMgrImpl.UpdateEventHandler;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

/**
 * Test class for the {@link ControllerConfigurationMgrImpl} class.
 * 
 * @author cweisenborn
 */
public class TestControllerConfigurationMgrImpl
{
    private ControllerConfigurationMgrImpl m_SUT;
    private SystemConfigurationMgr m_SysConfigMgr;
    private ConfigurationWrapper m_ConfigWrapper;
    private EventAdmin m_EventAdmin;
    private BundleContextUtil m_BundleUtil;
    private UpdateEventHandler m_UpdateEventHandler;
    
    @SuppressWarnings("rawtypes")
    private ServiceRegistration m_Registration;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        m_SysConfigMgr = mock(SystemConfigurationMgr.class);
        m_ConfigWrapper = mock(ConfigurationWrapper.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_BundleUtil = mock(BundleContextUtil.class);
        m_Registration = mock(ServiceRegistration.class);
        BundleContext context = mock(BundleContext.class);
        
        when(m_BundleUtil.getBundleContext()).thenReturn(context);
        when(context.registerService(eq(EventHandler.class), Mockito.any(UpdateEventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_Registration);
        
        m_SUT = new ControllerConfigurationMgrImpl();
        
        //set dependencies
        m_SUT.setConfigAdminMgr(m_SysConfigMgr);
        m_SUT.setConfigWrapper(m_ConfigWrapper);
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setEventAdmin(m_EventAdmin);
         
        m_SUT.registerListener();
        
        ArgumentCaptor<UpdateEventHandler> captor = ArgumentCaptor.forClass(UpdateEventHandler.class);
        verify(context).registerService(eq(EventHandler.class), captor.capture(), Mockito.any(Dictionary.class));
        m_UpdateEventHandler = captor.getValue();
    }
    
    /**
     * Test the unregister listener pre-destroy method.
     * Verify that the service registrations unregister method is called.
     */
    @Test
    public void testUnregisterListener()
    {
        m_SUT.unregisterListener();
        
        verify(m_Registration).unregister();
    }
    
    /**
     * Test the get remove config pid method.
     * Verify that the correct pid is returned.
     */
    @Test
    public void testGetRemoveConfigPid()
    {
        m_SUT.setRemoveConfigPid("some pid");
        assertThat(m_SUT.getRemoveConfigPid(), is("some pid"));
    }
    
    /**
     * Test the get factory configurations method.
     * Verify that the appropriate list of configuration models is returned.
     */
    @Test
    public void testGetFactoryConfigurations()
    {
        Map<String, ConfigAdminModel> configs = new HashMap<String, ConfigAdminModel>();
        List<ConfigAdminModel> conifgList = new ArrayList<ConfigAdminModel>(configs.values());
        
        when(m_SysConfigMgr.getFactoryConfigurationsByFactoryPidAsync(25, "factory pid")).thenReturn(configs);
        
        assertThat(m_SUT.getFactoryConfigurationsAsync(25, "factory pid"), is(conifgList));
        
        when(m_SysConfigMgr.getFactoryConfigurationsByFactoryPidAsync(25, "factory pid")).thenReturn(null);
        
        assertThat(m_SUT.getFactoryConfigurationsAsync(25, "factory pid").isEmpty(), is(true));
    }
    
    /**
     * Test the set configuration values method.
     * Verify that only changed values are set.
     */
    @Test
    public void testSetConfigurationValues()
    {     
        //Set up the display model to be sent to the method.
        ModifiableConfigMetatypeModel displayModel = new ModifiableConfigMetatypeModel("some PID");
        ConfigPropModelImpl displayProp1 = ModelFactory.createPropModel("test1", "value1");
        ConfigPropModelImpl displayProp2 = ModelFactory.createPropModel("test2", "value2");
        ConfigPropModelImpl displayProp3 = ModelFactory.createPropModel("test3", "value3");
        ConfigPropModelImpl displayProp4 = ModelFactory.createPropModel("test4", "value4");
        
        displayModel.getProperties().add(displayProp1);
        displayModel.getProperties().add(displayProp2);
        displayModel.getProperties().add(displayProp3);
        displayModel.getProperties().add(displayProp4);

        //Replay a configuration being returned from the view so that changed values may be set.
        m_SUT.setConfigurationValuesAsync(25, displayModel);
        
        //Verify that the configuration wrappers setConfigurationValue method is called for each property.
        verify(m_ConfigWrapper).setConfigurationValueAsync(eq(25), eq("some PID"), eq(displayModel.getProperties()));
    }

    /**
     * Test the get config model by PID method.
     * Verify that null is returned if the model does not exist.
     * Verify the correct information is returned for the specified model that does exist.
     */
    @Test
    public void testGetConfigModelByPid()
    {
        createConfigModelInformation();
        
        //Verify that retrieving a non-existent configuration model returns null.
        assertThat(m_SUT.getConfigModelByPidAsync(25, "non-existent pid"), is(nullValue()));
        
        //Retrieve the config display model and verify contents.
        ModifiableConfigMetatypeModel displayModel = m_SUT.getConfigModelByPidAsync(25, "some PID");
        assertThat(displayModel, is(notNullValue()));
        assertThat(displayModel.getPid(), is("some PID"));
        assertThat(displayModel.getProperties().size(), is(2));
        
        List<String> defaultValues = new ArrayList<String>();
        defaultValues.add("default");
        
        //Verify the first property in the first display model.
        ModifiablePropertyModel displayProp = displayModel.getProperties().get(0);
        assertThat(displayProp.getKey(), is("key1"));
        assertThat(displayProp.getName(), is("key1"));
        assertThat(displayProp.getType(), is((Object)String.class));
        assertThat(displayProp.getValue(), is((Object)"value1"));
        assertThat(displayProp.getDescription(), is("descrip"));
        assertThat(displayProp.getDefaultValues(), is(defaultValues));
        
        //Verify the second property in the first display model.
        displayProp = displayModel.getProperties().get(1);
        assertThat(displayProp.getKey(), is("key2"));
        assertThat(displayProp.getName(), is("key2"));
        assertThat(displayProp.getType(), is((Object)String.class));
        assertThat(displayProp.getValue(), is((Object)"value2"));
        assertThat(displayProp.getDescription(), is("descrip"));
        assertThat(displayProp.getDefaultValues(), is(defaultValues));
        
        //Verify that if the model is requested again that it equals the first model that was returned.
        assertThat(m_SUT.getConfigModelByPidAsync(25, "some PID"), is(displayModel));
        //Data for the model should only be retrieved from the config wrapper once. Verify that it is only called once.
        verify(m_ConfigWrapper).getConfigurationByPidAsync(25, "some PID");
    }
    
    /**
     * Verify that a bundle location can be properly retrieved. 
     */
    @Test
    public void testGetConfigurationBundleLocationAsync()
    {
        //Set up the display model to be sent to the method.
        ConfigAdminModel model = mock(ConfigAdminModel.class);
        when(model.getBundleLocation()).thenReturn("bLocation");
        when(m_SysConfigMgr.getConfigurationByPidAsync(25, "some PID")).thenReturn(model);
        
        String location = m_SUT.getConfigBundleLocationAsync(111, "some PID");
        
        assertThat(location, nullValue());
        
        location = m_SUT.getConfigBundleLocationAsync(25, "some PID");
        assertThat(location, is("bLocation"));
    }
    
    /**
     * Test the update event handler.
     * Verify that the handler handles events appropriately.
     */
    @Test
    public void testUpdateEventHandler()
    {
        //Mock initial config info.
        createConfigModelInformation();
        
        ModifiableConfigMetatypeModel model = m_SUT.getConfigModelByPidAsync(25, "some PID");
        assertThat(model.getPid(), is("some PID"));
        List<ModifiablePropertyModel> propList = model.getProperties();
        assertThat(propList.size(), is(2));
        assertThat(propList.get(0).getKey(), is("key1"));
        assertThat(propList.get(1).getKey(), is("key2"));
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, 25);
        props.put(ConfigurationEventConstants.EVENT_PROP_PID, "some PID");
        Event updateEvent = new Event(SystemConfigurationMgr.TOPIC_CONFIG_MODEL_UPDATED, props);
        
        //Add a value on to the configuration info.
        createUpdatedConfigModelInformation();
        
        m_UpdateEventHandler.handleEvent(updateEvent);
        
        model = m_SUT.getConfigModelByPidAsync(25, "some PID");
        assertThat(model.getPid(), is("some PID"));
        propList = model.getProperties();
        assertThat(propList.size(), is(3));
        assertThat(propList.get(0).getKey(), is("key1"));
        assertThat(propList.get(1).getKey(), is("key2"));
        assertThat(propList.get(2).getKey(), is("key3"));
        
        //Mock information for configuration does not exist.
        when(m_ConfigWrapper.getConfigurationByPidAsync(25, "some PID")).thenReturn(null);
        
        m_UpdateEventHandler.handleEvent(updateEvent);
        assertThat(m_SUT.getConfigModelByPidAsync(25, "some PID"), is(nullValue()));
    }
    
    /**
     * Test that if a config/meta model updated event with no PID specified is handled correctly.
     * Verify all models are updated instead of just one when no PID is specified by the event.
     */
    @Test
    public void testUpdateEventHandleUpdateAll()
    {
        //Mock initial info.
        createConfigModelInformation();
        
        //Verify initial values.
        ModifiableConfigMetatypeModel model = m_SUT.getConfigModelByPidAsync(25, "some PID");
        assertThat(model.getPid(), is("some PID"));
        List<ModifiablePropertyModel> propList = model.getProperties();
        assertThat(propList.size(), is(2));
        assertThat(propList.get(0).getKey(), is("key1"));
        assertThat(propList.get(1).getKey(), is("key2"));
        model = m_SUT.getConfigModelByPidAsync(25, "some PID 2");
        assertThat(model.getPid(), is("some PID 2"));
        propList = model.getProperties();
        assertThat(propList.size(), is(2));
        assertThat(propList.get(0).getKey(), is("key1"));
        assertThat(propList.get(1).getKey(), is("key2"));
        
        //Mock updated info.
        createUpdatedConfigModelInformation();
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, 25);
        Event updateEvent = new Event(SystemConfigurationMgr.TOPIC_CONFIG_MODEL_UPDATED, props);
        
        m_UpdateEventHandler.handleEvent(updateEvent);
        
        //Verify updated values.
        model = m_SUT.getConfigModelByPidAsync(25, "some PID");
        assertThat(model.getPid(), is("some PID"));
        propList = model.getProperties();
        assertThat(propList.size(), is(3));
        assertThat(propList.get(0).getKey(), is("key1"));
        assertThat(propList.get(1).getKey(), is("key2"));
        assertThat(propList.get(2).getKey(), is("key3"));
        model = m_SUT.getConfigModelByPidAsync(25, "some PID 2");
        assertThat(model.getPid(), is("some PID 2"));
        propList = model.getProperties();
        assertThat(propList.size(), is(3));
        assertThat(propList.get(0).getKey(), is("key1"));
        assertThat(propList.get(1).getKey(), is("key2"));
        assertThat(propList.get(2).getKey(), is("key3"));
    }
    
    /**
     * Method that creates mocked configuration model information.
     */
    private void createConfigModelInformation()
    {
        UnmodifiableConfigMetatypeModel config = new UnmodifiableConfigMetatypeModel("some PID");
        ConfigPropModelImpl prop1 = ModelFactory.createPropModel("key1", "value1");
        ConfigPropModelImpl prop2 = ModelFactory.createPropModel("key2", "value2");
        config.getProperties().add(prop1);
        config.getProperties().add(prop2);
        
        UnmodifiableConfigMetatypeModel config2 = new UnmodifiableConfigMetatypeModel("some PID 2");
        config2.getProperties().add(prop1);
        config2.getProperties().add(prop2);
        
        //Mock information returned from meta type and configuration system managers.
        when(m_ConfigWrapper.getConfigurationByPidAsync(25, "some PID")).thenReturn(config);
        when(m_ConfigWrapper.getConfigurationByPidAsync(25, "some PID 2")).thenReturn(config2);
    }
    
    /**
     * Method that creates updated mocked configuration model information.
     */
    private void createUpdatedConfigModelInformation()
    {
        // Mock updated info.
        UnmodifiableConfigMetatypeModel config = new UnmodifiableConfigMetatypeModel("some PID");
        ConfigPropModelImpl prop1 = ModelFactory.createPropModel("key1", "value1");
        ConfigPropModelImpl prop2 = ModelFactory.createPropModel("key2", "value2");
        ConfigPropModelImpl prop3 = ModelFactory.createPropModel("key3", "value3");
        config.getProperties().add(prop1);
        config.getProperties().add(prop2);
        config.getProperties().add(prop3);
        UnmodifiableConfigMetatypeModel config2 = new UnmodifiableConfigMetatypeModel("some PID 2");
        config2.getProperties().add(prop1);
        config2.getProperties().add(prop2);
        config2.getProperties().add(prop3);
        
        //Mock information returned from meta type and configuration system managers.
        when(m_ConfigWrapper.getConfigurationByPidAsync(25, "some PID")).thenReturn(config);
        when(m_ConfigWrapper.getConfigurationByPidAsync(25, "some PID 2")).thenReturn(config2);
    }
}
