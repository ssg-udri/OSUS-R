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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;

import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Test class for the {@link ConfigurationWrapperImpl} class.
 * 
 * @author cweisenborn
 */
public class TestConfigurationWrapperImpl
{
    private ConfigurationWrapperImpl m_SUT;
    private SystemConfigurationMgr m_SysConfigMgr;
    private SystemMetaTypeMgr m_SysMetaMgr;
    private GrowlMessageUtil m_GrowlUtil;
    
    @Before
    public void setup()
    {
        m_SysConfigMgr = mock(SystemConfigurationMgr.class);
        m_SysMetaMgr = mock(SystemMetaTypeMgr.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
              
        m_SUT = new ConfigurationWrapperImpl();
        
        m_SUT.setConfigAdminMgr(m_SysConfigMgr);
        m_SUT.setMetatypeMgr(m_SysMetaMgr);
        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
        
        setupValues();
    }
    
    /**
     * Test the GetConfigurationByPid method.
     * Verify the model returned is correct.
     */
    @Test
    public void testGetConfigurationByPid()
    {
        //Verify the contents of the model being returned.
        UnmodifiableConfigMetatypeModel returnModel = m_SUT.getConfigurationByPidAsync(25, "some PID");
        assertThat(returnModel.getPid(), is("some PID"));
        assertThat(returnModel.getProperties().size(), is(1));
        UnmodifiablePropertyModel property = returnModel.getProperties().get(0);
        assertThat(property.getKey(), is("name"));
        assertThat(property.getName(), is("name"));
        assertThat(property.getDefaultValues().get(0), is("Bob"));
        assertThat(property.getCardinality(), is(1));
        assertThat(property.getType(), is((Object)String.class));
        assertThat(property.getValue(), is((Object)"some name"));
        
        //Verify contents of factory configuration is returned correctly.
        returnModel = m_SUT.getConfigurationByPidAsync(25, "some factory config");
        assertThat(returnModel.getPid(), is("some factory config"));
        assertThat(returnModel.getProperties().size(), is(1));
        property = returnModel.getProperties().get(0);
        assertThat(property.getKey(), is("name"));
        assertThat(property.getName(), is("name"));
        assertThat(property.getDefaultValues().get(0), is("Bob"));
        assertThat(property.getCardinality(), is(1));
        assertThat(property.getType(), is((Object)String.class));
        assertThat(property.getValue(), is((Object)"some name"));
        
        //Verify non-existent pid returns null.
        returnModel = m_SUT.getConfigurationByPidAsync(25, "doesn't exist");
        assertThat(returnModel, is(nullValue()));
    }
    
    /**
     * Verify that meta type defaults can be retrieved using 
     */
    @Test
    public void testGetConfigurationDefaultsByFactoryPid()
    {
        UnmodifiableConfigMetatypeModel model = m_SUT.getConfigurationDefaultsByFactoryPidAsync(
                25, "some factory config");
        
        assertThat(model.getProperties().size(), is(1));
        
        UnmodifiablePropertyModel propModel = model.getProperties().get(0);
        
        assertThat(model, notNullValue());
        
        assertThat(propModel.getKey(), is("name"));
        assertThat(propModel.getName(), is("name"));
        assertThat(propModel.getType(), is((Object)String.class));
        assertThat(propModel.getValue(), is((Object)"Bob"));
        
        model = m_SUT.getConfigurationDefaultsByFactoryPidAsync(25, "not factory pid");
        assertThat(model, nullValue());
    }
    
    /**
     * Test the GetConfigurationProperty.
     * Verify the model returned is correct.
     */
    @Test
    public void testGetConfigurationProperty()
    {
        //Verify the contents of the model being returned.
        UnmodifiablePropertyModel returnModel = m_SUT.getConfigurationPropertyAsync(25, "some PID", "name");
        assertThat(returnModel.getKey(), is("name"));
        assertThat(returnModel.getName(), is("name"));
        assertThat(returnModel.getDefaultValues().get(0), is("Bob"));
        assertThat(returnModel.getCardinality(), is(1));
        assertThat(returnModel.getType(), is((Object)String.class));
        assertThat(returnModel.getValue(), is((Object)"some name"));
        
        //Verify that null is returned if no property is found.
        assertThat(m_SUT.getConfigurationPropertyAsync(25, "blah", "don't care"), is(nullValue()));
    }
    
    /**
     * Test the setConfigurationValue method.
     * Verify that the setConfigurationValue method in the system configuration manager is called with appropriate
     * parameters.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSetConfigurationValue()
    {
        final String key = "name";

        //Replay setting values.
        List<ModifiablePropertyModel> properties = new ArrayList<ModifiablePropertyModel>();
        properties.add(ModelFactory.createPropModel("name", "doesn't matter"));
        m_SUT.setConfigurationValueAsync(25, "some PID", properties);
        properties.set(0, ModelFactory.createPropModel(key, "some name"));
        m_SUT.setConfigurationValueAsync(25, "some PID", properties);
        properties.set(0, ModelFactory.createPropModel(key, "doesn't matter 2"));
        m_SUT.setConfigurationValueAsync(25, "some PID2", properties);
        properties.set(0, ModelFactory.createPropModel(key, "test"));
        m_SUT.setConfigurationValueAsync(25, "some PID2", properties);
        properties.set(0, ModelFactory.createPropModel(key, "doesn't matter 3"));
        m_SUT.setConfigurationValueAsync(25, "some PID3", properties);
        
        ArgumentCaptor<String> pidCaptor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        //Verify three values were set.
        verify(m_SysConfigMgr, times(3)).setConfigurationValueAsync(eq(25), pidCaptor.capture(), listCaptor.capture());
        verify(m_GrowlUtil, times(2)).createLocalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                eq("No Property Altered:"), eq("Please alter a property before saving."));
        
        //Verify that the value the were set are correct.
        String pid = pidCaptor.getAllValues().get(0);
        List<ModifiablePropertyModel> value = listCaptor.getAllValues().get(0);
        assertThat(pid, is("some PID"));
        assertThat(value.get(0).getValue(), is((Object)"doesn't matter"));
        
        pid = pidCaptor.getAllValues().get(1);
        value = listCaptor.getAllValues().get(1);
        assertThat(pid, is("some PID2"));
        assertThat(value.get(0).getValue(), is((Object)"doesn't matter 2"));
        
        pid = pidCaptor.getAllValues().get(2);
        value = listCaptor.getAllValues().get(2);
        assertThat(pid, is("some PID3"));
        assertThat(value.get(0).getValue(), is((Object)"doesn't matter 3"));
        
        //Exception should be thrown if no meta type information exists for the specified value being set.
        try
        {
            properties.set(0, ModelFactory.createPropModel(key, "blah"));
            m_SUT.setConfigurationValueAsync(25, "non-existent", properties);
            fail("Illegal argument exception expected!");
        }
        catch (IllegalArgumentException exception)
        {
            //expected exception
        }
    }
    
    /**
     * Verify that the correct list of changed properties is returned.
     */
    @Test
    public void testFindChangedProperties()
    {
        List<ModifiablePropertyModel> properties = new ArrayList<ModifiablePropertyModel>();
        properties.add(ModelFactory.createPropModel("name", "Bill"));
        properties.add(ModelFactory.createPropModel("otherkey", "JimBob"));
        
        List<ModifiablePropertyModel> changed = m_SUT.findChangedPropertiesAsync(25, "some factory config", properties);
        
        assertThat(changed.size(), is(2));
        assertThat(changed.get(0).getKey(), is("name"));
        assertThat(changed.get(1).getKey(), is("otherkey"));
        
        assertThat((String)changed.get(0).getValue(), is("Bill"));
        assertThat((String)changed.get(1).getValue(), is("JimBob"));
        
        properties.clear();
        properties.add(ModelFactory.createPropModel("name", "some name"));
        
        changed = m_SUT.findChangedPropertiesAsync(25,  "some factory config", properties);
        assertThat(changed.size(), is(0));
        
        try
        {
            changed = m_SUT.findChangedPropertiesAsync(25, "nonexistentpid", properties);
            fail("Expecting no meta type information currently available exception.");
        }
        catch (IllegalArgumentException exception)
        {
            //expecting an illegal argument exception
        }
    }
    
    private void setupValues()
    {
        ConfigAdminModel configModel = new ConfigAdminModel();
        configModel.setPid("some PID");
        ConfigAdminPropertyModel propModel = new ConfigAdminPropertyModel();
        propModel.setKey("name");
        propModel.setType(String.class);
        propModel.setValue("some name");
        configModel.getProperties().add(propModel);
        
        MetaTypeModel metaModel = new MetaTypeModel("some PID", 5L);
        AttributeModel attributeModel = ModelFactory.createAttributeModel("name", AttributeDefinition.STRING, "Bob");
        metaModel.getAttributes().add(attributeModel);
        
        ConfigAdminModel configModel2 = new ConfigAdminModel();
        configModel2.setPid("some PID2");
        ConfigAdminPropertyModel propModel2 = new ConfigAdminPropertyModel();
        propModel2.setKey("test");
        propModel2.setType(String.class);
        propModel2.setValue("doesn't matter");
        configModel2.getProperties().add(propModel2);
        
        MetaTypeModel metaModel2 = new MetaTypeModel("some PID2", 5L);
        AttributeModel attributeModel2 = ModelFactory.createAttributeModel("name", AttributeDefinition.STRING, "test");
        metaModel2.getAttributes().add(attributeModel2);
        
        MetaTypeModel metaModel3 = new MetaTypeModel("some PID3", 5L);
        AttributeModel attributeModel3 = ModelFactory.createAttributeModel("name", AttributeDefinition.STRING, "blah");
        metaModel3.getAttributes().add(attributeModel3);
        
        ConfigAdminModel factoryConfigModel = new ConfigAdminModel();
        factoryConfigModel.setPid("some factory config");
        factoryConfigModel.setFactoryPid("some factory");
        ConfigAdminPropertyModel factoryConfigPropModel = new ConfigAdminPropertyModel();
        factoryConfigPropModel.setKey("name");
        factoryConfigPropModel.setType(String.class);
        factoryConfigPropModel.setValue("some name");
        factoryConfigModel.getProperties().add(factoryConfigPropModel);
        
        MetaTypeModel factoryMetaModel = new MetaTypeModel("some factory", 5L);
        factoryMetaModel.getAttributes().add(attributeModel);
        List<MetaTypeModel> factoryMetaList = new ArrayList<MetaTypeModel>();
        factoryMetaList.add(factoryMetaModel);
               
        when(m_SysConfigMgr.getConfigurationByPidAsync(25, "some PID")).thenReturn(configModel);
        when(m_SysMetaMgr.getConfigInformationAsync(25, "some PID")).thenReturn(metaModel);
        
        when(m_SysConfigMgr.getConfigurationByPidAsync(25, "some PID2")).thenReturn(configModel2);
        when(m_SysMetaMgr.getConfigInformationAsync(25, "some PID2")).thenReturn(metaModel2);
        
        when(m_SysConfigMgr.getConfigurationByPidAsync(25, "some PID3")).thenReturn(null);
        when(m_SysMetaMgr.getConfigInformationAsync(25, "some PID3")).thenReturn(metaModel3);
        
        when(m_SysConfigMgr.getConfigurationByPidAsync(25, "some factory config")).thenReturn(factoryConfigModel);
        when(m_SysMetaMgr.getFactoriesListAsync(25)).thenReturn(factoryMetaList);
    }
}
