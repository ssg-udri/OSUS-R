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
import java.util.List;

import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.junit.Before;
import org.junit.Test;

public class TestFactoryObjectConfigModifierViewImpl
{
    private FactoryObjectConfigModifierViewImpl m_SUT;
    private FactoryBaseModel m_BaseModel;
    private FactoryObjMgr m_FactoryMgr;
    private ConfigurationWrapper m_ConfigWrapper;
    private GrowlMessageUtil m_GrowlUtil;
    
    private final int CONTROLLER_ID = 123;
    private final String FACTORY_CONF_NAME = "factoryConfName";
    private final String PID = "mil.dod.th.ose.pid";
    
    private ConfigPropModelImpl m_Property1;
    private ConfigPropModelImpl m_Property2;
    
    @Before
    public void setup()
    {
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_Property1 = ModelFactory.createPropModel("key1", "value1");
        
        List<String> dfltVals = new ArrayList<>();
        dfltVals.add("otherValue");
        m_Property2 = ModelFactory.createPropModel("key2", "value2");
               
        m_ConfigWrapper = mock(ConfigurationWrapper.class);
        m_FactoryMgr = mock(FactoryObjMgr.class);
        
        m_BaseModel = mockFactoryBaseModel();
        
        m_SUT = new FactoryObjectConfigModifierViewImpl();
        m_SUT.setConfigWrapper(m_ConfigWrapper);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        
        m_SUT.setSelectedFactoryModel(m_BaseModel);
    }
    
    /**
     * Verify that update all props calls the correct methods when object does not have a pid.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateAllPropertiesAsyncNoPid()
    {
        when(m_BaseModel.getPid()).thenReturn("");
        List<ModifiablePropertyModel> properties = new ArrayList<>();
        properties.add(m_Property1);
        when(m_ConfigWrapper.findChangedPropertiesAsync(eq(CONTROLLER_ID), eq(FACTORY_CONF_NAME), anyList()))
            .thenReturn(properties);
        
        m_SUT.updateAllPropertiesAsync();
        verify(m_FactoryMgr).createConfiguration(eq(CONTROLLER_ID), eq(m_BaseModel), anyList());     
    }
    
    /**
     * Verify that update all props calls the correct methods when object has a pid.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateAllPropertiesAsyncWithPid()
    {
        m_SUT.updateAllPropertiesAsync();
        verify(m_ConfigWrapper).setConfigurationValueAsync(eq(CONTROLLER_ID), eq(PID), anyList());
    }
    
    /**
     * Verify that update all props without a factory object set does nothing.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateAllPropertiesAsyncNoFactObj()
    {
        m_SUT = new FactoryObjectConfigModifierViewImpl();
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        
        m_SUT.updateAllPropertiesAsync();
        
        verify(m_ConfigWrapper, never()).findChangedPropertiesAsync(eq(CONTROLLER_ID), 
                eq(FACTORY_CONF_NAME), anyList());
        verify(m_FactoryMgr, never()).createConfiguration(eq(CONTROLLER_ID), eq(m_BaseModel), anyList());
        verify(m_ConfigWrapper, never()).setConfigurationValueAsync(eq(CONTROLLER_ID), eq(PID), anyList());
    }
    
    /**
     * Verify that get properties method returns properties when object does not have a pid.
     */
    @Test
    public void testGetPropertiesWithoutPid()
    {
        when(m_BaseModel.getPid()).thenReturn("");
        
        UnmodifiableConfigMetatypeModel model = mockUnmodifiableConfigMetatypeModel();
        when(m_ConfigWrapper.getConfigurationDefaultsByFactoryPidAsync(CONTROLLER_ID, 
                FACTORY_CONF_NAME)).thenReturn(model);
        
        List<ModifiablePropertyModel> retList = m_SUT.getProperties();
        
        verify(m_ConfigWrapper, never()).getConfigurationByPidAsync(anyInt(), anyString());
        
        assertThat(retList, notNullValue());
        assertThat(retList.size(), is(2));
        
        ModifiablePropertyModel mod1 = retList.get(0);
        assertThat(mod1.getKey(), is(m_Property1.getKey()));
        assertThat(mod1.getName(), is(m_Property1.getName()));
        assertThat(mod1.getDescription(), is(m_Property1.getDescription()));
        assertThat(mod1.getValue(), is(m_Property1.getValue()));
        
        ModifiablePropertyModel mod2 = retList.get(1);
        assertThat(mod2.getKey(), is(m_Property2.getKey()));
        assertThat(mod2.getName(), is(m_Property2.getName()));
        assertThat(mod2.getDescription(), is(m_Property2.getDescription()));
        assertThat(mod2.getValue(), is(m_Property2.getValue()));
    }
    
    /**
     * Verify that get properties method returns properties when object has pid.
     */
    @Test
    public void testGetPropertiesWithPid()
    {
        UnmodifiableConfigMetatypeModel model = mockUnmodifiableConfigMetatypeModel();
        when(m_ConfigWrapper.getConfigurationByPidAsync(
                CONTROLLER_ID, PID)).thenReturn(model);
        
        List<ModifiablePropertyModel> retList = m_SUT.getProperties();
        
        verify(m_ConfigWrapper, never()).getConfigurationDefaultsByFactoryPidAsync(anyInt(), anyString());
        
        assertThat(retList, notNullValue());
        assertThat(retList.size(), is(2));
        
        ModifiablePropertyModel mod1 = retList.get(0);
        assertThat(mod1.getKey(), is(m_Property1.getKey()));
        assertThat(mod1.getName(), is(m_Property1.getName()));
        assertThat(mod1.getDescription(), is(m_Property1.getDescription()));
        assertThat(mod1.getValue(), is(m_Property1.getValue()));
        
        ModifiablePropertyModel mod2 = retList.get(1);
        assertThat(mod2.getKey(), is(m_Property2.getKey()));
        assertThat(mod2.getName(), is(m_Property2.getName()));
        assertThat(mod2.getDescription(), is(m_Property2.getDescription()));
        assertThat(mod2.getValue(), is(m_Property2.getValue()));
    }
    
    /**
     * Verify that if no object is set an empty list is returned.
     */
    @Test
    public void testGetPropertieNoFactoryObject()
    {
        //remake so that factory object is not set
        m_SUT = new FactoryObjectConfigModifierViewImpl();
        m_SUT.setConfigWrapper(m_ConfigWrapper);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        
        List<ModifiablePropertyModel> retList = m_SUT.getProperties();
        assertThat(retList, notNullValue());
        assertThat(retList.size(), is(0));
    }
    
    /**
     * Verify properties can be properly set when factory object has a pid.
     */
    @Test
    public void testSetConfigPropertiesAsyncWithPid()
    {
        UnmodifiableConfigMetatypeModel model = mockUnmodifiableConfigMetatypeModel();
        when(m_ConfigWrapper.getConfigurationByPidAsync(
                CONTROLLER_ID, PID)).thenReturn(null, model);
        
        List<ModifiablePropertyModel> list = m_SUT.getProperties();
        assertThat(list.size(), is(0));
        
        list = m_SUT.getProperties();
        
        //once because call to getProperties and then once for the setConfig method
        verify(m_ConfigWrapper, times(2))
            .getConfigurationByPidAsync(CONTROLLER_ID, PID);        
       
        assertThat(list.size(), is(2));
        
        ModifiablePropertyModel mod1 = list.get(0);
        assertThat(mod1.getKey(), is(m_Property1.getKey()));
        assertThat(mod1.getName(), is(m_Property1.getName()));
        assertThat(mod1.getDescription(), is(m_Property1.getDescription()));
        assertThat(mod1.getValue(), is(m_Property1.getValue()));
        
        ModifiablePropertyModel mod2 = list.get(1);
        assertThat(mod2.getKey(), is(m_Property2.getKey()));
        assertThat(mod2.getName(), is(m_Property2.getName()));
        assertThat(mod2.getDescription(), is(m_Property2.getDescription()));
        assertThat(mod2.getValue(), is(m_Property2.getValue()));
    }
    
    /**
     * Verify properties can be properly set when factory object doesn't have a pid.
     */
    @Test
    public void testSetConfigPropertiesAsyncWithNoPid()
    {
        when(m_BaseModel.getPid()).thenReturn("");
        UnmodifiableConfigMetatypeModel model = mockUnmodifiableConfigMetatypeModel();
        when(m_ConfigWrapper.getConfigurationDefaultsByFactoryPidAsync(
                CONTROLLER_ID, FACTORY_CONF_NAME)).thenReturn(null, model);
        
        List<ModifiablePropertyModel> list = m_SUT.getProperties();
        assertThat(list.size(), is(0));
        
        list = m_SUT.getProperties();
        
        //once because call to getProperties and then once for the setConfig method
        verify(m_ConfigWrapper, times(2))
            .getConfigurationDefaultsByFactoryPidAsync(CONTROLLER_ID, FACTORY_CONF_NAME);
        
        assertThat(list.size(), is(2));
        
        ModifiablePropertyModel mod1 = list.get(0);
        assertThat(mod1.getKey(), is(m_Property1.getKey()));
        assertThat(mod1.getName(), is(m_Property1.getName()));
        assertThat(mod1.getDescription(), is(m_Property1.getDescription()));
        assertThat(mod1.getValue(), is(m_Property1.getValue()));
        
        ModifiablePropertyModel mod2 = list.get(1);
        assertThat(mod2.getKey(), is(m_Property2.getKey()));
        assertThat(mod2.getName(), is(m_Property2.getName()));
        assertThat(mod2.getDescription(), is(m_Property2.getDescription()));
        assertThat(mod2.getValue(), is(m_Property2.getValue()));
    }
    
    /**
     * Verify nothing happens if factory model is not set
     */
    @Test
    public void testSetConfigPropertiesAsyncNoFactObj()
    {
        m_SUT = new FactoryObjectConfigModifierViewImpl();
        
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        
        List<ModifiablePropertyModel> list = m_SUT.getProperties();
        assertThat(list.size(), is(0));
        
        verify(m_ConfigWrapper, never()).getConfigurationDefaultsByFactoryPidAsync(CONTROLLER_ID, FACTORY_CONF_NAME);
        verify(m_ConfigWrapper, never()).getConfigurationByPidAsync(CONTROLLER_ID, PID);       
    }
    
    /**
     * Verify that after updating properties that the value in the properties list 
     * has been updated to contain the changed value.
     */
    @Test
    public void testModifyPropertiesWithoutPid()
    {
        //make sure list is initially empty; 
        List<ModifiablePropertyModel> initialProperties = m_SUT.getProperties();
        assertThat(initialProperties, notNullValue());
        assertThat(initialProperties.size(), is(0));
        
        //mock out configuration wrapper calls; must be mocked after getProperties call
        //so that the list of properties does not contain any items. Mock out wrapper calls
        //here and a subsequent call to getProperties should return the list of properties
        UnmodifiableConfigMetatypeModel model = mockUnmodifiableConfigMetatypeModel();
        
        when(m_ConfigWrapper.getConfigurationByPidAsync(CONTROLLER_ID, PID)).thenReturn(model);
       
        List<ModifiablePropertyModel> list = m_SUT.getProperties();
        
        assertThat(list.size(), is(2));
        
        ModifiablePropertyModel mod1 = list.get(0);
        assertThat(mod1.getKey(), is(m_Property1.getKey()));
        assertThat(mod1.getName(), is(m_Property1.getName()));
        assertThat(mod1.getDescription(), is(m_Property1.getDescription()));
        assertThat(mod1.getValue(), is(m_Property1.getValue()));
        
        ModifiablePropertyModel mod2 = list.get(1);
        assertThat(mod2.getKey(), is(m_Property2.getKey()));
        assertThat(mod2.getName(), is(m_Property2.getName()));
        assertThat(mod2.getDescription(), is(m_Property2.getDescription()));
        assertThat(mod2.getValue(), is(m_Property2.getValue()));
    }
    
    /**
     * Create a mocked factory base model.
     * @return
     *  the mocked object
     */
    private FactoryBaseModel mockFactoryBaseModel()
    {
        FactoryBaseModel model = mock(FactoryBaseModel.class);
        
        when(model.getControllerId()).thenReturn(CONTROLLER_ID);
        when(model.getFactoryPid()).thenReturn(FACTORY_CONF_NAME);
        when(model.getPid()).thenReturn(PID);
        when(model.getFactoryManager()).thenReturn(m_FactoryMgr);
        
        return model;
    }
    
    /**
     * Create a mocked instance of the UnmodifiableConfigMetatypeModel instance.
     * @return
     *  the mocked object
     */
    private UnmodifiableConfigMetatypeModel mockUnmodifiableConfigMetatypeModel()
    {
        UnmodifiableConfigMetatypeModel model = mock(UnmodifiableConfigMetatypeModel.class);
        
        List<UnmodifiablePropertyModel> list = new ArrayList<>();
        list.add(m_Property1);
        list.add(m_Property2);
        
        when(model.getProperties()).thenReturn(list);
        
        return model;
    }
}
