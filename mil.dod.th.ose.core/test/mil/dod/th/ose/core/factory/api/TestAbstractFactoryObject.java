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
package mil.dod.th.ose.core.factory.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.factory.Extension;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.ConfigurationAdminMocker;
import mil.dod.th.ose.core.FactoryObjectMocker;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;
import mil.dod.th.ose.test.ExtensionMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.common.collect.ImmutableMap;

/**
 * @author dhumeniuk
 *
 */
public class TestAbstractFactoryObject
{
    private static final String FACTORY_PID = "factory-pid";
    private static final UUID OBJ_UUID = UUID.randomUUID();
    private static final String OBJ_NAME = "FactObj";
    private static final String OBJ_PID = "org.edu.udri.pid";
    private static final String OBJ_BASETYPE = "ObjBaseType";
    
    private AbstractFactoryObject m_SUT;
    private ConfigurationAdmin m_ConfigAdmin;
    private FactoryServiceUtils m_FactoryServiceUtils;
    private EventAdmin m_EventAdmin;
    private PowerManagerInternal m_PowerInternal;
    
    private FactoryRegistry<AbstractFactoryObject> m_Registry;
    private FactoryObjectProxy m_FactoryProxy;
    
    @Mock private FactoryInternal factoryInternal;
    @Mock private Extension<Extension1> m_Extension1;
    @Mock private Extension<Extension2> m_Extension2;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new AbstractFactoryObject()
        {
        };
        
        MockitoAnnotations.initMocks(this);
        
        m_Registry = mock(FactoryRegistry.class);
        m_FactoryProxy = mock(FactoryObjectProxy.class);
        m_ConfigAdmin = ConfigurationAdminMocker.createMockConfigAdmin();
        m_FactoryServiceUtils = mock(FactoryServiceUtils.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_PowerInternal = mock(PowerManagerInternal.class);
        
        // stub
        when(factoryInternal.getProductType()).thenReturn("product-type");
        when(factoryInternal.getPid()).thenReturn(FACTORY_PID);
        
        when(m_FactoryServiceUtils.getMetaTypeDefaults(Mockito.any(FactoryDescriptor.class)))
            .thenReturn(new Hashtable<String, Object>());
        
        Configuration config = FactoryObjectMocker.addMockConfiguration(m_SUT, OBJ_PID);
        mockExtensions();
        
        //initialize the instance so that it can be used for testing
        m_SUT.initialize(m_Registry, m_FactoryProxy, factoryInternal,
                m_ConfigAdmin, m_EventAdmin, m_PowerInternal,
                OBJ_UUID, OBJ_NAME, OBJ_PID, OBJ_BASETYPE);
        
        when(m_Registry.createConfiguration(OBJ_UUID, FACTORY_PID, m_SUT)).thenReturn(config);
    }
    
    /**
     * Verify that uuid, name, pid, and base type are returned correctly when accessed.
     */
    @Test
    public void testBasicProperties()
    {
        assertThat(m_SUT.getName(), is(OBJ_NAME));
        assertThat(m_SUT.getPid(), is(OBJ_PID));
        assertThat(m_SUT.getUuid(), is(OBJ_UUID));
        assertThat(m_SUT.getBaseType(), is(OBJ_BASETYPE));
       
        m_SUT.internalSetName("funky");
       
        assertThat(m_SUT.getName(), is("funky"));
    }
    
    /**
     * Verify empty map is returned if returned configuration object is null. 
     */
    @Test
    public void testGetProperties_NullConfiguration() throws Exception
    {
        when(m_ConfigAdmin.listConfigurations(anyString())).thenReturn(null);
        
        assertThat(m_SUT.getProperties().isEmpty(), is(true));
        
        // make sure property map can be updated for easy updates by consumer
        m_SUT.getProperties().put("new-prop", "new-value");
    }
    
    /**
     * Verify that a configuration property value can be retrieved.
     */
    @Test
    public void testGetProperties() throws IllegalArgumentException, IllegalStateException, 
        IOException, ConfigurationException, FactoryException
    {
        Configuration config = ConfigurationAdminMocker.getConfigByPid(OBJ_PID);
        Dictionary<String, Object> map = new Hashtable<>();
        map.put("key1", "value1");
        when(config.getProperties()).thenReturn(map);
        
        assertThat(m_SUT.getProperties(), hasEntry("key1", (Object)"value1"));
    }
    
    /**
     * Verify that if an error occurs while trying to retrieve a configuration that an 
     * exception is thrown.
     */
    @Test
    public void testGetProperties_FailToRetrieveConfiguration() throws Exception
    {
        doThrow(new IOException()).when(m_ConfigAdmin).listConfigurations(anyString());
        try
        {
            m_SUT.getProperties();
            fail("Expecting exception");
        }
        catch (IllegalStateException e)
        {
        }
    }
    
    /**
     * Verify configuration is created if not there.
     */
    @Test
    public void testSetProperties_NewConfig() throws IllegalArgumentException, 
        IllegalStateException, FactoryException, FactoryObjectInformationException, ClassNotFoundException
    {
        m_SUT.initialize(m_Registry, m_FactoryProxy, factoryInternal, m_ConfigAdmin, 
                m_EventAdmin, m_PowerInternal, OBJ_UUID, OBJ_NAME, null, OBJ_BASETYPE); 
        
        // don't give SUT the PID, but set up new config that has the PID
        Configuration config = ConfigurationAdminMocker.addMockConfiguration(m_SUT, "new-pid");
        when(m_Registry.createConfiguration(Mockito.any(UUID.class), anyString(), 
                Mockito.any(AbstractFactoryObject.class))).thenReturn(config);
        
        
        Map<String, Object> props = new HashMap<>();
        props.put("test-prop", "test-value");
        m_SUT.setProperties(props);
        
        verify(m_Registry).createConfiguration(OBJ_UUID, FACTORY_PID, m_SUT);
    }
    
    /**
     * Verify that exception occurs if configuration object cannot be created.
     */
    @Test
    public void testSetProperties_FailCreateFactoryConfig() throws IllegalArgumentException, 
        IllegalStateException, FactoryException, FactoryObjectInformationException, ClassNotFoundException
    {
        m_SUT.initialize(m_Registry, m_FactoryProxy, factoryInternal, m_ConfigAdmin, 
                m_EventAdmin, m_PowerInternal, OBJ_UUID, OBJ_NAME, null, OBJ_BASETYPE);
        when(m_Registry.createConfiguration(eq(OBJ_UUID), Mockito.anyString(), 
                Mockito.any(AbstractFactoryObject.class))).thenThrow(new FactoryObjectInformationException("Failed"));
        
        Map<String, Object> props = new HashMap<>();
        props.put("test-prop", "test-value");
        try
        {
            m_SUT.setProperties(props);
            fail("expecting exception");
        }
        catch (FactoryException e)
        {
            //expecting exception
        }
    }
    
    /**
     * Verify that set properties retrieves the correct properties.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testSetProperties() throws IllegalArgumentException, 
        IllegalStateException, FactoryException, IOException, ConfigurationException
    {
        Configuration config = ConfigurationAdminMocker.getConfigByPid(OBJ_PID);
       
        Map<String, Object> properties = new Hashtable<String, Object>();
        properties.put("test-prop", "test-value");
        m_SUT.setProperties(properties);

        ArgumentCaptor<Dictionary> dictionaryCap1 = ArgumentCaptor.forClass(Dictionary.class);
        verify(config, times(1)).update(dictionaryCap1.capture());
        assertThat((String)dictionaryCap1.getValue().get("test-prop"), is("test-value"));
        
        // set properties again, verify merge
        properties = new Hashtable<String, Object>();
        properties.put("test-prop", "test-value1");
        properties.put("test-prop2", "test-value2");
        properties.put("test-prop3", "test-value3");
        m_SUT.setProperties(properties);
        
        ArgumentCaptor<Dictionary> dictionaryCap2 = ArgumentCaptor.forClass(Dictionary.class);
        verify(config, times(2)).update(dictionaryCap2.capture());
        Dictionary<String, Object> capturedProperties = dictionaryCap2.getAllValues().get(1);
        
        assertThat((String)capturedProperties.get("test-prop"), is("test-value1"));
        assertThat((String)capturedProperties.get("test-prop2"), is("test-value2"));
        assertThat((String)capturedProperties.get("test-prop3"), is("test-value3"));
    }
    
    /**
     * Verify that set properties will create a new configuration if existing PID results in null config object.
     */
    @Test
    public void testSetProperties_NullConfiguration() throws Exception
    {
        Configuration config = ConfigurationAdminMocker.getConfigByPid(OBJ_PID);
        
        when(m_ConfigAdmin.listConfigurations(anyString())).thenReturn(null);
        when(m_Registry.createConfiguration(
                Mockito.any(UUID.class), anyString(), Mockito.any(FactoryObjectInternal.class))).thenReturn(config);
        
        Map<String, Object> properties = new Hashtable<String, Object>();
        properties.put("test-prop", "test-value");
        m_SUT.setProperties(properties);
        
        verify(m_Registry).createConfiguration(OBJ_UUID, FACTORY_PID, m_SUT);
    }
    
    /**
     * Verify that if empty properties are passed that the config IS still updated. As this would
     * be how a configuration would be reset.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSetProperties_Empty() throws IllegalArgumentException, 
        IllegalStateException, FactoryException, IOException, ConfigurationException
    {
        Configuration config = ConfigurationAdminMocker.getConfigByPid(OBJ_PID);
        
        Map<String, Object> properties = new Hashtable<String, Object>();
        m_SUT.setProperties(properties);
        
        ArgumentCaptor<Dictionary> dictionaryCap1 = ArgumentCaptor.forClass(Dictionary.class);
        verify(config).update(dictionaryCap1.capture());
    }
    
    /**
     * Verify set properties fails if update fails.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSetProperties_UpdateFails() throws IOException, IllegalArgumentException, 
        IllegalStateException, ConfigurationException, FactoryException
    {
        Configuration config = ConfigurationAdminMocker.getConfigByPid(OBJ_PID);    
        Map<String, Object> properties = new Hashtable<String, Object>();
        properties.put("key1", "value1");
        doThrow(new IOException()).when(config).update(Mockito.any(Dictionary.class));
        try
        {
            m_SUT.setProperties(properties);
            fail("Expecting exception");
        }
        catch (FactoryException e)
        {
            //expecting the exception
        }
    }
    
    /**
     * Verify that toString prints out the name of the factory object follow by the factory product name.
     */
    @Test
    public void testToString()
    {
        m_SUT.internalSetName("testName");
        String value = m_SUT.toString();
        assertThat(value, is("testName (" + m_SUT.getFactory().getProductName() + ")"));
    }
    
    /**
     * Test that is the latch is null, that there is no exception thrown if blocking update is called.
     */
    @Test
    public void testBlockingPropsUpdate() throws ConfigurationException
    {
        //will fail if an exception is thrown
        m_SUT.blockingPropsUpdate(new HashMap<String, Object>());
    }
    
    /**
     * Test that the latch countdown will report a timeout if blocking update method is not called to countdown
     * the latch.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLatchCountdown() throws ConfigurationException, IllegalArgumentException, IllegalStateException, 
        FactoryException, IOException
    {
        Configuration config = ConfigurationAdminMocker.getConfigByPid(OBJ_PID);
        doNothing().when(config).update(Mockito.any(Dictionary.class));
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("key", "value");
        //try to set the property, and exception should be thrown
        try
        {
            m_SUT.setProperties(props);
            fail("Expected exception due to time out");
        }
        catch (FactoryException e)
        {
            //expected timeout
        }
    }
    
    /**
     * Verify latch is released even if an exception occurs during an objects update.
     */
    @SuppressWarnings("unchecked")
    @Test (timeout = 2000) //timeout to be sure that the first call exits, and the second can proceed
    public void testLatchCountdownException() throws ConfigurationException, IllegalArgumentException, 
        IllegalStateException, FactoryException, IOException
    {
        doThrow(new ConfigurationException("", "")).when(m_FactoryProxy).updated(Mockito.anyMap());
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("key", "value");
        //try to set the property, and exception should be thrown
        try
        {
            m_SUT.setProperties(props);
            fail("expecting property to fail");
        }
        catch (Exception e)
        {
            //exception expected
        }
        
        //make the call again, this verifies that the latch is released, otherwise this call would fail
        try
        {
            m_SUT.setProperties(props);
            fail("expecting property to fail");
        }
        catch (Exception e)
        {
            //exception expected
        }
        
        //Test will fail if lock is not released as the set properties call will timeout later than this test
        //will timeout. Thus by the test not timing out we are proving that the release is in fact happening and
        //the set properties method is returning out.
    }
    
    /**
     * Verify that wake lock is created.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreatePowerManagerWakeLock()
    {
        WakeLock lock = mock(WakeLock.class);
        when(m_PowerInternal.createWakeLock(Mockito.any(Class.class), Mockito.any(FactoryObject.class), eq("lock.id")))
               .thenReturn(lock);
        WakeLock returned = m_SUT.createPowerManagerWakeLock("lock.id");
        
        verify(m_PowerInternal).createWakeLock(m_FactoryProxy.getClass(), m_SUT, "lock.id");
        assertThat(returned, is(lock));
    }
    
    /**
     * Verify properties are posted properly.
     */
    @Test
    public void testPostEvent()
    {
        m_SUT.postEvent("some-event", new ImmutableMap.Builder<String, Object>().put("key", "value").build());
        
        ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(event.capture());
        
        assertThat(event.getValue().getTopic(), is("some-event"));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_TYPE));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_NAME));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_PID));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_UUID));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE));
        assertThat(event.getValue().getProperty("key"), is((Object)"value"));
    }
    
    /**
     * Verify properties are posted properly if null is passed for additional properties.
     */
    @Test
    public void testPostEventNoAddProps()
    {
        m_SUT.postEvent("some-event", null);
        
        ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(event.capture());
        
        assertThat(event.getValue().getTopic(), is("some-event"));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_TYPE));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_NAME));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_PID));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_UUID));
        assertThat(event.getValue().getPropertyNames(), hasItemInArray(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE));
    }
    
    @Test
    public void testGetExtensions()
    {
        assertThat(m_SUT.getExtension(Extension1.class), is(m_Extension1.getObject()));
        assertThat(m_SUT.getExtension(Extension2.class), is(m_Extension2.getObject()));
    }
    
    /**
     * Verify if an invalid type is passed, an exception is thrown.
     */
    @Test
    public void testGetExtension_InvalidType()
    {
        try
        {
            m_SUT.getExtension(Object.class);
            fail("Expecting exception as type is invalid");
        }
        catch (IllegalArgumentException e) {}
    }
    
    @Test
    public void testGetExtensionTypes()
    {
        Set<Class<?>> expectedSet = new HashSet<>();
        expectedSet.add(m_Extension1.getType());
        expectedSet.add(m_Extension2.getType());
        assertThat(m_SUT.getExtensionTypes(), is(expectedSet));
    }
    
    /**
     * Verify types list is empty if proxy returns null.
     */
    @Test
    public void testGetExtensionTypes_Empty()
    {
        when(m_FactoryProxy.getExtensions()).thenReturn(null);
        m_SUT.initialize(m_Registry, m_FactoryProxy, factoryInternal, m_ConfigAdmin, m_EventAdmin, m_PowerInternal,
                OBJ_UUID, OBJ_NAME, OBJ_PID, OBJ_BASETYPE);
        
        Set<Class<?>> expectedSet = new HashSet<>();
        assertThat(m_SUT.getExtensionTypes(), is(expectedSet));
    }
    
    /**
     * Verify setting the name calls the registry.
     */
    @Test
    public void testSetName() throws Exception
    {
        m_SUT.setName("test");
        
        verify(m_Registry).setName(m_SUT, "test");
    }
    
    /**
     * Verify exception is wrapped.
     */
    @Test
    public void testSetName_RegistryException() throws Exception
    {
        doThrow(new FactoryObjectInformationException("")).when(m_Registry)
            .setName(Mockito.any(AbstractFactoryObject.class), Mockito.anyString());
        
        try
        {
            m_SUT.setName("test");
            fail("expected exception");
        }
        catch(FactoryException e)
        {
        }
    }
    
    /**
     * Verify the registry is called to delete the object.
     */
    @Test
    public void testDelete() throws Exception
    {
        m_SUT.delete();
        verify(m_Registry).delete(m_SUT);
    }
    
    /**
     * Verify that you can check if an object is managed by the directory service.
     */
    @Test
    public void testIsManaged() throws Exception
    {
        Set<AbstractFactoryObject> objects = new HashSet<>();
        objects.add(m_SUT);
        when(m_Registry.getObjects()).thenReturn(objects);
        
        assertThat(m_SUT.isManaged(), is(true));
        
        objects.remove(m_SUT);

        assertThat(m_SUT.isManaged(), is(false));
    }
    
    /**
     * Mock the factory object to have 2 extensions, {@link Extension1} and {@link Extension2}.
     */
    private void mockExtensions()
    {
        ExtensionMocker.stub(m_Extension1, Extension1.class);
        ExtensionMocker.stub(m_Extension2, Extension2.class);
        
        Set<Extension<?>> extensions = new HashSet<>();
        extensions.add(m_Extension1);
        extensions.add(m_Extension2);
        
        when(m_FactoryProxy.getExtensions()).thenReturn(extensions);
    }
    
    /** used to test extensions provided through {@link FactoryObject#getExtension(Class)}. */
    interface Extension1 {}
    interface Extension2 {}
}
