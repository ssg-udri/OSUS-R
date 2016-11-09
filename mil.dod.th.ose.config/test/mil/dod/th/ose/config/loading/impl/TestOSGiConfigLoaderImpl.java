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
package mil.dod.th.ose.config.loading.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteMetatypeConstants;
import mil.dod.th.core.types.StringMapEntry;
import mil.dod.th.model.config.PidConfig;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.test.EventAdminMocker;
import mil.dod.th.ose.test.EventAdminVerifier;
import mil.dod.th.ose.test.EventAdminMocker.EventHandlerRegistrationAnswer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Test class for the {@link OSGiConfigLoaderImpl} class.
 * 
 * @author cweisenborn
 */
public class TestOSGiConfigLoaderImpl
{
    private static final String CONFIG_OVERRIDE = "test.config.1";
    private static final String CONFIG_NO_OVERRIDE = "test.config.2";
    private static final String FACTORY_CONFIG_PID = "test.factory.config";
    
    private OSGiConfigLoaderImpl m_SUT;
    private ConfigurationAdmin m_ConfigAdmin;
    private EventAdmin m_EventAdmin;
    private ServiceRegistration<?> m_EventRegistration;
    private MetaTypeService m_MetaTypeService;
    private Configuration m_ConfigNoOverride;
    private Configuration m_ConfigOverride;
    private Configuration m_FactoryConfig;
    private BundleContext m_Context;
    private LoggingService m_Logging;
    private ObjectClassDefinition m_TestConfig1;
    private boolean m_AllValidConfigs;
    
    /**
     * Setup dependencies.
     */
    @Before
    public void setup() throws IOException
    {
        m_ConfigAdmin = mock(ConfigurationAdmin.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_MetaTypeService = mock(MetaTypeService.class);
        m_ConfigOverride = mock(Configuration.class);
        m_ConfigNoOverride = mock(Configuration.class);
        m_FactoryConfig = mock(Configuration.class);
        m_Context = mock(BundleContext.class);
        m_Logging = mock(LoggingService.class);

        EventHandlerRegistrationAnswer evtHandlerStub =
                EventAdminMocker.stubHandlerOfType(m_Context, EventHandler.class, m_EventAdmin);
        m_EventRegistration = evtHandlerStub.getRegistration();
        
        when(m_ConfigAdmin.getConfiguration(CONFIG_OVERRIDE, null)).thenReturn(m_ConfigOverride);
        when(m_ConfigAdmin.getConfiguration(CONFIG_NO_OVERRIDE, null)).thenReturn(m_ConfigNoOverride);
        when(m_ConfigAdmin.createFactoryConfiguration(FACTORY_CONFIG_PID, null)).thenReturn(m_FactoryConfig);
        
        mockMetaTypeData();
        
        m_SUT = new OSGiConfigLoaderImpl();
        
        m_SUT.setConfigAdmin(m_ConfigAdmin);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMetaTypeService(m_MetaTypeService);
        m_SUT.setLogService(m_Logging);

        m_SUT.activate(m_Context);

        // Default to all configurations being valid for the test
        m_AllValidConfigs = true;
    }
    
    @After
    public void tearDown() throws Exception
    {
        m_SUT.deactivate();

        if (m_AllValidConfigs)
        {
            // Verify that the factory objects loading complete event is always sent
            EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
                ConfigLoadingConstants.TOPIC_CONFIG_OSGI_COMPLETE_EVENT);
        }

        // Verify that the event handler has been unregistered
        verify(m_EventRegistration).unregister();
    }

    /**
     * Verify that the process method calls the appropriate configuration admin methods during a first run.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testProcessFirstRun() throws IOException
    {
        final List<PidConfig> configs = createConfigList();
        final Dictionary<String, Object> configAdminProps1 = new Hashtable<String, Object>();
        configAdminProps1.put("existingProp", "existing-value");
        when(m_ConfigOverride.getProperties()).thenReturn(configAdminProps1);
        final Dictionary<String, Object> configAdminProps2 = new Hashtable<String, Object>();
        configAdminProps2.put("existingProp", "existing-value");
        when(m_ConfigNoOverride.getProperties()).thenReturn(configAdminProps2);
        
        m_SUT.process(configs, true);
        
        verify(m_ConfigAdmin, times(1)).getConfiguration(CONFIG_NO_OVERRIDE, null);
        verify(m_ConfigAdmin, times(1)).getConfiguration(CONFIG_OVERRIDE, null);
        verify(m_ConfigAdmin).createFactoryConfiguration(FACTORY_CONFIG_PID, null);
        
        final ArgumentCaptor<Dictionary> propsCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ConfigOverride, times(1)).update(propsCaptor.capture());
        verify(m_ConfigNoOverride).update(propsCaptor.capture());
        verify(m_FactoryConfig).update(propsCaptor.capture());
        
        Dictionary<String, Object> propList = propsCaptor.getAllValues().get(0);
        assertThat(propList.get("someInt"), is((Object)10));
        assertThat(propList.get("someString"), is((Object)"value"));
        assertThat(propList.get("someShort"), is((Object)(short)89));
        assertThat(propList.get("existingProp"), is((Object)"existing-value"));
        
        propList = propsCaptor.getAllValues().get(1);
        assertThat(propList.get("someFloat"), is((Object)12.5f));
        assertThat(propList.get("someByte"), is((Object)(byte)127));
        assertThat(propList.get("someChar"), is((Object)'A'));
        assertThat(propList.get("existingProp"), is((Object)"existing-value"));
        
        propList = propsCaptor.getAllValues().get(2);
        assertThat(propList.get("someDouble"), is((Object)15.55));
        assertThat(propList.get("someBool"), is((Object)true));
        assertThat(propList.get("someLong"), is((Object)1234567890L));
    }
    
    /**
     * Verify that the process method does not call configuration admin methods after a first run.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessSecondRun() throws IOException
    {
        final List<PidConfig> configs = createConfigList();

        m_SUT.process(configs, false);
        
        verify(m_ConfigAdmin, never()).getConfiguration(CONFIG_NO_OVERRIDE, null);
        verify(m_ConfigAdmin, never()).getConfiguration(CONFIG_OVERRIDE, null);
        verify(m_ConfigAdmin, never()).createFactoryConfiguration(FACTORY_CONFIG_PID, null);
        
        verify(m_ConfigOverride, never()).update(Mockito.any(Dictionary.class));
        verify(m_ConfigNoOverride, never()).update(Mockito.any(Dictionary.class));
        verify(m_FactoryConfig, never()).update(Mockito.any(Dictionary.class));
    }
    
    /**
     * Verify that properties get set correctly if not properties are set initially.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testProcessNoInitialProps() throws IOException
    {
        final List<PidConfig> configs = createConfigList();
        
        m_SUT.process(configs, true);
        
        // just test one config to make sure correct properties are set
        final ArgumentCaptor<Dictionary> propsCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ConfigOverride).update(propsCaptor.capture());
        
        // should be exactly 3 properties, no more
        Dictionary<String, Object> propList = propsCaptor.getValue();
        assertThat(propList.get("someInt"), is((Object)10));
        assertThat(propList.get("someString"), is((Object)"value"));
        assertThat(propList.get("someShort"), is((Object)(short)89));
        assertThat(propList.size(), is(3));
    }
    
    /**
     * Verify that an IOException is handled appropriately when thrown while trying to create/update a standard
     * OSGi configuration.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testProcessConfigIOException() throws IOException
    {
        when(m_ConfigAdmin.getConfiguration(CONFIG_OVERRIDE, null)).thenThrow(IOException.class);
        
        final List<PidConfig> configs = createConfigList();
        
        m_SUT.process(configs, true);
        
        final ArgumentCaptor<Dictionary> propsCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ConfigOverride, never()).update(propsCaptor.capture());
        verify(m_ConfigNoOverride).update(propsCaptor.capture());
        verify(m_FactoryConfig).update(propsCaptor.capture());
        
        Dictionary<String, Object> propList = propsCaptor.getAllValues().get(0);
        assertThat(propList.get("someFloat"), is((Object)12.5F));
        assertThat(propList.get("someByte"), is((Object)(byte)127));
        assertThat(propList.get("someChar"), is((Object)'A'));
        
        propList = propsCaptor.getAllValues().get(1);
        assertThat(propList.get("someDouble"), is((Object)15.55));
        assertThat(propList.get("someBool"), is((Object)true));
        assertThat(propList.get("someLong"), is((Object)1234567890L));
        
        verify(m_Logging).error(Mockito.any(IOException.class), eq("An error occurred trying to create "
                + "configuration with PID: %s"), eq(CONFIG_OVERRIDE));
    }
    
    /**
     * Verify that an IOException is handled appropriately when thrown while trying to create a factory configuration.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testProcessFactoryIOException() throws IOException
    {
        when(m_ConfigAdmin.createFactoryConfiguration(FACTORY_CONFIG_PID, null)).thenThrow(IOException.class);
        
        final List<PidConfig> configs = createConfigList();
        
        m_SUT.process(configs, true);
        
        @SuppressWarnings("rawtypes")
        final ArgumentCaptor<Dictionary> propsCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ConfigOverride).update(propsCaptor.capture());
        verify(m_ConfigNoOverride).update(propsCaptor.capture());
        verify(m_FactoryConfig, never()).update(propsCaptor.capture());
        
        Dictionary<String, Object> propList = propsCaptor.getAllValues().get(0);
        assertThat(propList.get("someInt"), is((Object)10));
        assertThat(propList.get("someString"), is((Object)"value"));
        assertThat(propList.get("someShort"), is((Object)(short)89));
        
        propList = propsCaptor.getAllValues().get(1);
        assertThat(propList.get("someFloat"), is((Object)12.5F));
        assertThat(propList.get("someByte"), is((Object)(byte)127));
        assertThat(propList.get("someChar"), is((Object)'A'));
        
        verify(m_Logging).error(Mockito.any(IOException.class), eq("An error occurred trying to create factory "
                + "configuration from factory with PID: %s"), eq(FACTORY_CONFIG_PID));
    }
    
    /**
     * Verify that an illegal argument exception is thrown if no attribute definitions can be found for a property of
     * a configuration.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testProcessNoDefinition() throws IOException
    {
        final List<PidConfig> configs = createConfigList();
        configs.get(0).getProperties().add(new StringMapEntry("badKey", "doesnt_matter"));
        
        m_SUT.process(configs, true);
        
        @SuppressWarnings("rawtypes")
        final ArgumentCaptor<Dictionary> propsCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ConfigOverride, never()).update(propsCaptor.capture());
        verify(m_ConfigNoOverride).update(propsCaptor.capture());
        verify(m_FactoryConfig).update(propsCaptor.capture());
        
        Dictionary<String, Object> propList = propsCaptor.getAllValues().get(0);
        assertThat(propList.get("someFloat"), is((Object)12.5F));
        assertThat(propList.get("someByte"), is((Object)(byte)127));
        assertThat(propList.get("someChar"), is((Object)'A'));
        
        propList = propsCaptor.getAllValues().get(1);
        assertThat(propList.get("someDouble"), is((Object)15.55));
        assertThat(propList.get("someBool"), is((Object)true));
        assertThat(propList.get("someLong"), is((Object)1234567890L));
        
        verify(m_Logging).error(Mockito.any(IllegalArgumentException.class), eq("An error occurred trying to "
                + "create configuration with PID: %s"), eq(CONFIG_OVERRIDE));
    }
    
    /**
     * Verify a configuration with missing metatype data is processed later after receiving the metatype available
     * event.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testProcessNoMetaTypeData() throws IOException
    {
        final List<StringMapEntry> configProps = new ArrayList<StringMapEntry>();
        configProps.add(new StringMapEntry("someInt", "32"));
        configProps.add(new StringMapEntry("someString", "value"));
        configProps.add(new StringMapEntry("someShort", "16"));

        final PidConfig configMissing = new PidConfig(null, "some.nonexistent.config", configProps);

        final List<PidConfig> configs = createConfigList();

        configs.add(configMissing);
        
        m_SUT.process(configs, true);
        
        // Verify configs that were processed immediately
        final ArgumentCaptor<Dictionary> propsCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ConfigOverride).update(propsCaptor.capture());
        verify(m_ConfigNoOverride).update(propsCaptor.capture());
        verify(m_FactoryConfig).update(propsCaptor.capture());
        
        assertThat(propsCaptor.getAllValues().size(), is(3));
        
        Dictionary<String, Object> propList = propsCaptor.getAllValues().get(0);
        assertThat(propList.get("someInt"), is((Object)10));
        assertThat(propList.get("someString"), is((Object)"value"));
        assertThat(propList.get("someShort"), is((Object)(short)89));
        
        propList = propsCaptor.getAllValues().get(1);
        assertThat(propList.get("someFloat"), is((Object)12.5F));
        assertThat(propList.get("someByte"), is((Object)(byte)127));
        assertThat(propList.get("someChar"), is((Object)'A'));
        
        propList = propsCaptor.getAllValues().get(2);
        assertThat(propList.get("someDouble"), is((Object)15.55));
        assertThat(propList.get("someBool"), is((Object)true));
        assertThat(propList.get("someLong"), is((Object)1234567890L));
        
        // Post metatype available event and verify the missing config
        List<String> pidList = new ArrayList<>();
        pidList.add("some.nonexistent.config");
        Map<String, Object> props = new HashMap<>();
        props.put("service.pids", pidList);
        Event metaTypeAvailEvent = new Event(RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE, props);
        m_EventAdmin.postEvent(metaTypeAvailEvent);
    }
    
    /**
     * Verify no attribute definitions are checked.
     */
    @Test
    public void testEmptyAttributesDefinitions()
    {
        m_AllValidConfigs = false;

        final List<PidConfig> configs = createConfigList();
        configs.add(new PidConfig(null, "some.nonexistent.config", new ArrayList<StringMapEntry>()));
        
        // override default to have no attribute definitions
        when(m_TestConfig1.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(null);
        
        m_SUT.process(configs, true);
    }
    
    /**
     * Method that creates a list of pid configs to be used for testing purposes.
     */
    private List<PidConfig> createConfigList()
    {
        final List<StringMapEntry> configProps1 = new ArrayList<StringMapEntry>();
        configProps1.add(new StringMapEntry("someInt", "10"));
        configProps1.add(new StringMapEntry("someString", "value"));
        configProps1.add(new StringMapEntry("someShort", "89"));
        final PidConfig configOverride = new PidConfig(null, CONFIG_OVERRIDE, configProps1);
        
        final List<StringMapEntry> configProps2 = new ArrayList<StringMapEntry>();
        configProps2.add(new StringMapEntry("someFloat", "12.5"));
        configProps2.add(new StringMapEntry("someByte", "127"));
        configProps2.add(new StringMapEntry("someChar", "A"));
        final PidConfig configNoOverride = new PidConfig(null, CONFIG_NO_OVERRIDE, configProps2);
        
        final List<StringMapEntry> factoryConfigProps = new ArrayList<StringMapEntry>();
        factoryConfigProps.add(new StringMapEntry("someDouble", "15.55"));
        factoryConfigProps.add(new StringMapEntry("someBool", "true"));
        factoryConfigProps.add(new StringMapEntry("someLong", "1234567890"));
        final PidConfig factoryConfigNoOverride = new PidConfig(FACTORY_CONFIG_PID, null, 
                factoryConfigProps);
        
        final List<PidConfig> configs = new ArrayList<PidConfig>();
        configs.add(configOverride);
        configs.add(configNoOverride);
        configs.add(factoryConfigNoOverride);
        
        return configs;
    }
    
    /**
     * Method that mocks the needed bundle and meta type information.
     */
    private void mockMetaTypeData()
    {
        final Bundle bundle = mock(Bundle.class);
        final Bundle bundle2 = mock(Bundle.class);
        when(m_Context.getBundles()).thenReturn(new Bundle[]{bundle, bundle2});
        
        final MetaTypeInformation metaInfo = mock(MetaTypeInformation.class);
        when(m_MetaTypeService.getMetaTypeInformation(bundle)).thenReturn(metaInfo);
        when(m_MetaTypeService.getMetaTypeInformation(bundle2)).thenReturn(null);
        
        when(metaInfo.getPids()).thenReturn(new String[]{CONFIG_NO_OVERRIDE, CONFIG_OVERRIDE});
        when(metaInfo.getFactoryPids()).thenReturn(new String[]{FACTORY_CONFIG_PID});
        
        final AttributeDefinition intAttr = mock(AttributeDefinition.class);
        when(intAttr.getID()). thenReturn("someInt");
        when(intAttr.getType()).thenReturn(AttributeDefinition.INTEGER);
        final AttributeDefinition stringAttr = mock(AttributeDefinition.class);
        when(stringAttr.getID()).thenReturn("someString");
        when(stringAttr.getType()).thenReturn(AttributeDefinition.STRING);
        final AttributeDefinition shortAttr = mock(AttributeDefinition.class);
        when(shortAttr.getID()).thenReturn("someShort");
        when(shortAttr.getType()).thenReturn(AttributeDefinition.SHORT);
        final AttributeDefinition floatAttr = mock(AttributeDefinition.class);
        when(floatAttr.getID()).thenReturn("someFloat");
        when(floatAttr.getType()).thenReturn(AttributeDefinition.FLOAT);
        final AttributeDefinition byteAttr = mock(AttributeDefinition.class);
        when(byteAttr.getID()).thenReturn("someByte");
        when(byteAttr.getType()).thenReturn(AttributeDefinition.BYTE);
        final AttributeDefinition charAttr = mock(AttributeDefinition.class);
        when(charAttr.getID()).thenReturn("someChar");
        when(charAttr.getType()).thenReturn(AttributeDefinition.CHARACTER);
        final AttributeDefinition doubleAttr = mock(AttributeDefinition.class);
        when(doubleAttr.getID()).thenReturn("someDouble");
        when(doubleAttr.getType()).thenReturn(AttributeDefinition.DOUBLE);
        final AttributeDefinition boolAttr = mock(AttributeDefinition.class);
        when(boolAttr.getID()).thenReturn("someBool");
        when(boolAttr.getType()).thenReturn(AttributeDefinition.BOOLEAN);
        final AttributeDefinition longAttr = mock(AttributeDefinition.class);
        when(longAttr.getID()).thenReturn("someLong");
        when(longAttr.getType()).thenReturn(AttributeDefinition.LONG);
        
        m_TestConfig1 = mock(ObjectClassDefinition.class);
        when(m_TestConfig1.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(
                new AttributeDefinition[]{intAttr, stringAttr, shortAttr});
        
        final ObjectClassDefinition testConfig2 = mock(ObjectClassDefinition.class);
        when(testConfig2.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(
                new AttributeDefinition[]{floatAttr, byteAttr, charAttr});
        
        final ObjectClassDefinition testFactoryConfig = mock(ObjectClassDefinition.class);
        when(testFactoryConfig.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(
                new AttributeDefinition[]{doubleAttr, boolAttr, longAttr});
        
        when(metaInfo.getObjectClassDefinition(CONFIG_OVERRIDE, null)).thenReturn(m_TestConfig1);
        when(metaInfo.getObjectClassDefinition(CONFIG_NO_OVERRIDE, null)).thenReturn(testConfig2);
        when(metaInfo.getObjectClassDefinition(FACTORY_CONFIG_PID, null)).thenReturn(testFactoryConfig);
    }
}
