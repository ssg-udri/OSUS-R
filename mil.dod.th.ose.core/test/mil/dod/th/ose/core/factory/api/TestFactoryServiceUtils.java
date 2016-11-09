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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.FactoryMocker;
import mil.dod.th.ose.core.MetaTypeMocker;
import mil.dod.th.ose.core.MetaTypeProviderBundleMocker;
import mil.dod.th.ose.metatype.MetaTypeProviderBundle;
import mil.dod.th.ose.test.AttributeDefinitionMocker;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * @author nickmarcucci
 *
 */
public class TestFactoryServiceUtils
{
    private static final String PRODUCT_TYPE = "product-type";
    private static final String BASE_TYPE = "base-type";
    private FactoryServiceUtils m_SUT;
    private MetaTypeService m_MetaTypeService;
    private MetaTypeProviderBundle m_MetaTypeProviderBundle;
    private FactoryInternal m_Factory;
    private AttributeDefinition[] m_ADs;
    
    @Mock private ConfigurationAdmin configAdmin;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new FactoryServiceUtils();
        
        m_MetaTypeService = MetaTypeMocker.createMockMetaType();
        m_SUT.setMetaTypeService(m_MetaTypeService);
        m_MetaTypeProviderBundle = MetaTypeProviderBundleMocker.mockIt();
        m_SUT.setMetaTypeProviderBundle(m_MetaTypeProviderBundle);
        
        m_Factory = FactoryMocker.mockFactoryInternal(PRODUCT_TYPE);
        
        AttributeDefinition attr1 = 
                AttributeDefinitionMocker.mockIt("attr1", null, null, AttributeDefinition.STRING, "default1");
        AttributeDefinition attr2 = 
                AttributeDefinitionMocker.mockIt("attr2", null, null, AttributeDefinition.STRING, "default2");
        AttributeDefinition attr3 = 
                AttributeDefinitionMocker.mockIt("attr3", null, null, AttributeDefinition.STRING, null);
        when(attr3.getDefaultValue()).thenReturn(null); // return null instead of array containing a null item
        m_ADs = new AttributeDefinition[]{attr1, attr2, attr3};
    }
    
    /**
     * Verify that the correct event properties are returned based on the given 
     * factory object that is passed in. No PID event property should be returned.
     */
    @Test
    public void testGetFactoryObjectBaseEventPropsNullPid()
    {
        UUID uuid = UUID.randomUUID(); 
        
        FactoryObjectInternal objectNoPid = mock(FactoryObjectInternal.class);
        when(objectNoPid.getFactory()).thenReturn(m_Factory);
        when(objectNoPid.getName()).thenReturn("factObjName");
        when(objectNoPid.getUuid()).thenReturn(uuid);
        when(objectNoPid.getBaseType()).thenReturn(BASE_TYPE);
                       
        Map<String, Object> props = FactoryServiceUtils.getFactoryObjectBaseEventProps(objectNoPid);
        
        assertThat(props.size(), is(5));
        assertThat(props.containsKey(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(false));
        assertThat((FactoryObjectInternal)props.get(FactoryDescriptor.EVENT_PROP_OBJ), is(objectNoPid));
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is("factObjName"));
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), is(PRODUCT_TYPE));
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_UUID), is(uuid.toString()));
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
            is(BASE_TYPE));    
    }
    
    /**
     * Verify that the correct event properties are returned based on the given 
     * factory object that is passed in. No PID event property should be returned.
     */
    @Test
    public void testGetFactoryObjectBaseEventPropsPid()
    {
        UUID uuid = UUID.randomUUID();
        
        FactoryObjectInternal objectPid = mock(FactoryObjectInternal.class);
        when(objectPid.getFactory()).thenReturn(m_Factory);
        when(objectPid.getName()).thenReturn("factObjName");
        when(objectPid.getUuid()).thenReturn(uuid);
        when(objectPid.getPid()).thenReturn("pid");
        when(objectPid.getBaseType()).thenReturn(BASE_TYPE);
        
        Map<String, Object> props = FactoryServiceUtils.getFactoryObjectBaseEventProps(objectPid);
        
        assertThat(props.size(), is(6));
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_PID), is("pid"));
        assertThat((FactoryObjectInternal)props.get(FactoryDescriptor.EVENT_PROP_OBJ), is(objectPid));
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is("factObjName"));
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), is(PRODUCT_TYPE));
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_UUID), is(uuid.toString()));
        assertThat((String)props.get(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(BASE_TYPE));
    }
    
    /**
     * Verify instance of metatype service is returned from {@link FactoryServiceUtils#getMetaTypeService()}.
     */
    @Test
    public void testGetMetatypeService()
    {
        assertThat(m_SUT.getMetaTypeService(), is(m_MetaTypeService));
    }
    
    /**
     * Verify ability to get metatype defaults of a factory.
     */
    @Test
    public void testGetMetaTypeDefaults()
    {
        when(m_Factory.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(m_ADs);
        
        Dictionary<String, Object> retrieved = m_SUT.getMetaTypeDefaults(m_Factory);
        
        assertThat(ConfigurationUtils.convertDictionaryPropsToMap(retrieved), hasKey("attr1"));
        assertThat(ConfigurationUtils.convertDictionaryPropsToMap(retrieved), hasKey("attr2"));
        // empty string should not be added
        assertThat(ConfigurationUtils.convertDictionaryPropsToMap(retrieved), not(hasKey("attr3"))); 
        assertThat(retrieved.get("attr1"), is((Object)"default1"));
        assertThat(retrieved.get("attr2"), is((Object)"default2"));
    }
    
    /**
     * Verify method returns properties from config admin.
     */
    @Test
    public void testGetFactoryConfiguration() throws Exception
    {
        Configuration expectedConfig = mock(Configuration.class);
        when(configAdmin.listConfigurations("(service.pid=some-pid)")).thenReturn(new Configuration[] {expectedConfig});
        
        Configuration config = FactoryServiceUtils.getFactoryConfiguration(configAdmin, "some-pid");
        
        assertThat(config, is(expectedConfig));
    }
    
    /**
     * Verify method throws exception if underlying config admin does
     */
    @Test
    public void testGetFactoryConfiguration_ConfigAdminException() throws Exception
    {
        when(configAdmin.listConfigurations("(service.pid=some-pid)")).thenThrow(new IOException());
        
        try
        {
            FactoryServiceUtils.getFactoryConfiguration(configAdmin, "some-pid");
            fail("Expecting wrapped exception as underlying service throws exception");
        }
        catch (FactoryException e)
        {
            
        }
    }
    
    /**
     * Verify empty dictionary is returned if no pid.
     */
    @Test
    public void testGetFactoryConfiguration_NoPid() throws Exception
    {
        Configuration config = FactoryServiceUtils.getFactoryConfiguration(configAdmin, null);
        
        assertThat(config, is(nullValue()));
    }
    
    /**
     * Verify empty dictionary is returned if pid is not found.
     */
    @Test
    public void testGetFactoryConfiguration_PidNotFound() throws Exception
    {
        Configuration config = FactoryServiceUtils.getFactoryConfiguration(configAdmin, "some-pid");
        
        assertThat(config, is(nullValue()));
    }
    
    /**
     * Verify method throws exception if multiple configs are returned.
     */
    @Test
    public void testGetFactoryConfiguration_MultipleConfigs() throws Exception
    {
        Configuration config1 = mock(Configuration.class);
        Configuration config2 = mock(Configuration.class);
        when(configAdmin.listConfigurations("(service.pid=some-pid)"))
            .thenReturn(new Configuration[] {config1, config2});
        
        try
        {
            FactoryServiceUtils.getFactoryConfiguration(configAdmin, "some-pid");
            fail("Expecting exception as multiple configs are returned");
        }
        catch (IllegalStateException e) { }
    }
}
