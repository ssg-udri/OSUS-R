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
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.model.config.AddressConfig;
import mil.dod.th.model.config.Configurations;
import mil.dod.th.model.config.EventConfig;
import mil.dod.th.model.config.FactoryObjectConfig;
import mil.dod.th.model.config.PidConfig;
import mil.dod.th.model.config.SocketChannelConfig;
import mil.dod.th.model.config.TransportChannelConfig;
import mil.dod.th.ose.config.loading.AddressLoader;
import mil.dod.th.ose.config.loading.FactoryObjectLoader;
import mil.dod.th.ose.config.loading.OSGiConfigLoader;
import mil.dod.th.ose.config.loading.RemoteChannelLoader;
import mil.dod.th.ose.config.loading.RemoteEventLoader;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.test.EventAdminVerifier;
import mil.dod.th.ose.utils.FileService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;

/**
 * @author nickmarcucci
 *
 */
public class TestConfigurationMgr
{
    private ConfigurationMgr m_SUT;
    private LoggingService m_Log;
    private XmlUnmarshalService m_Unmarshaller;
    private BundleContext m_BundleContext;
    private ConfigurationAdmin m_ConfigAdmin;
    private EventAdmin m_EventAdmin;
    private FactoryObjectLoader m_FactoryObjLoader;
    private OSGiConfigLoader m_OsgiConfigLoader;
    private AddressLoader m_AddrConfigLoader;
    private Configuration m_OSGiConfiguration;
    private Configurations m_XmlConfiguration;
    private FileService m_FileService;
    private RemoteEventLoader m_EventConfigLoader;
    private RemoteChannelLoader m_ChannelConfigLoader;
    
    /**
     * Test setup.
     */
    @Before
    public void setUp() throws UnmarshalException
    {
        m_SUT = new ConfigurationMgr();
        
        m_BundleContext = mock(BundleContext.class);
        m_Unmarshaller = mock(XmlUnmarshalService.class);
        m_Log = mock(LoggingService.class);
        m_ConfigAdmin = mock(ConfigurationAdmin.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_FactoryObjLoader = mock(FactoryObjectLoader.class);
        m_OsgiConfigLoader = mock(OSGiConfigLoader.class);
        m_AddrConfigLoader = mock(AddressLoader.class);
        m_OSGiConfiguration = mock(Configuration.class);
        m_XmlConfiguration = mock(Configurations.class);
        m_FileService = mock(FileService.class);
        m_EventConfigLoader =  mock(RemoteEventLoader.class);
        m_ChannelConfigLoader = mock(RemoteChannelLoader.class);
        
        m_SUT.setConfigAdmin(m_ConfigAdmin);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setFactoryObjectLoader(m_FactoryObjLoader);
        m_SUT.setOSGiConfigLoader(m_OsgiConfigLoader);
        m_SUT.setXmlUnmarshaller(m_Unmarshaller);
        m_SUT.setLoggingService(m_Log);
        m_SUT.setAddressLoader(m_AddrConfigLoader);
        m_SUT.setFileService(m_FileService);
        m_SUT.setRemoteEventLoader(m_EventConfigLoader);
        m_SUT.setRemoteChannelLoader(m_ChannelConfigLoader);
        
        when(m_FileService.doesFileExist(Mockito.any(File.class))).thenReturn(true);
        
        when(m_BundleContext.getProperty(Mockito.anyString())).thenReturn("resources");
        when(m_Unmarshaller.getXmlObject(
                eq(Configurations.class), Mockito.any(URL.class))).thenReturn(m_XmlConfiguration);
    }
    
    /**
     * Verify that on activation of the manager that the configurations are read in, framework listener registered and
     * OSGi configuration updated. On deactivation, verify that the frame listener is unregistered.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testFrameworkEventStarted_FirstRunUpdated() throws IOException, UnmarshalException
    {
        when(m_ConfigAdmin.getConfiguration(ConfigurationMgr.class.getName())).thenReturn(m_OSGiConfiguration);
        when(m_OSGiConfiguration.getProperties()).thenReturn(null);
        
        m_SUT.activate(m_BundleContext);
        
        ArgumentCaptor<Dictionary> dCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_OSGiConfiguration).update(dCaptor.capture());
        
        assertThat(dCaptor.getValue().size(), is(1));
        assertThat((Boolean)dCaptor.getValue().get(ConfigurationMgrConfig.FIRST_RUN_PROPERTY), is(false));
    }
    
    /**
     * Verify that on framework started event with configuration property already set that correct firstRun flag is
     * passed on.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testFrameworkEventStarted_FirstRunAlreadySet() throws IOException, UnmarshalException
    {
        when(m_ConfigAdmin.getConfiguration(ConfigurationMgr.class.getName())).thenReturn(m_OSGiConfiguration);
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(ConfigurationMgrConfig.FIRST_RUN_PROPERTY, false);
        when(m_OSGiConfiguration.getProperties()).thenReturn(properties);
        
        List<FactoryObjectConfig> fConfigs = new ArrayList<>();
        FactoryObjectConfig config = mock(FactoryObjectConfig.class);
        fConfigs.add(config);
        
        List<PidConfig> pConfigs = new ArrayList<>();
        PidConfig pconfig = mock(PidConfig.class);
        pConfigs.add(pconfig);
        
        List<AddressConfig> aConfigs = new ArrayList<>();
        AddressConfig aconfig = mock(AddressConfig.class);
        aConfigs.add(aconfig);
        
        List<EventConfig> eConfigs = new ArrayList<>();
        EventConfig eConfig = mock(EventConfig.class);
        eConfigs.add(eConfig);
        
        List<SocketChannelConfig> sConfigs = new ArrayList<>();
        SocketChannelConfig sConfig = mock(SocketChannelConfig.class);
        sConfigs.add(sConfig);
        
        List<TransportChannelConfig> tConfigs = new ArrayList<>();
        TransportChannelConfig tConfig = mock(TransportChannelConfig.class);
        tConfigs.add(tConfig);
        
        when(m_XmlConfiguration.getFactoryObjects()).thenReturn(fConfigs);
        when(m_XmlConfiguration.getOsgiConfigs()).thenReturn(pConfigs);
        when(m_XmlConfiguration.getAddresses()).thenReturn(aConfigs);
        when(m_XmlConfiguration.getEventRegs()).thenReturn(eConfigs);
        when(m_XmlConfiguration.getSocketChannels()).thenReturn(sConfigs);
        when(m_XmlConfiguration.getTransportChannels()).thenReturn(tConfigs);
        
        m_SUT.activate(m_BundleContext);
        
        ArgumentCaptor<List> fCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_FactoryObjLoader).process(fCaptor.capture(), eq(false));
        
        assertThat(fCaptor.getValue().size(), is(1));
        assertThat((FactoryObjectConfig)fCaptor.getValue().get(0), is(config));
        
        ArgumentCaptor<List> pCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_OsgiConfigLoader).process(pCaptor.capture(), eq(false));
        
        assertThat(pCaptor.getValue().size(), is(1));
        assertThat((PidConfig)pCaptor.getValue().get(0), is(pconfig));
        
        ArgumentCaptor<List> aCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_AddrConfigLoader).process(aCaptor.capture(), eq(false));
        
        assertThat(aCaptor.getValue().size(), is(1));
        assertThat((AddressConfig)aCaptor.getValue().get(0), is(aconfig));
        
        ArgumentCaptor<List> eCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_EventConfigLoader).process(eCaptor.capture());
        assertThat(eCaptor.getValue().size(), is(1));
        assertThat((EventConfig)eCaptor.getValue().get(0), is(eConfig));
        
        ArgumentCaptor<List> sCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_ChannelConfigLoader).processSocketChannels(sCaptor.capture());
        assertThat(sCaptor.getValue().size(), is(1));
        assertThat((SocketChannelConfig)sCaptor.getValue().get(0), is(sConfig));
        
        ArgumentCaptor<List> tCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_ChannelConfigLoader).processTransportChannels(tCaptor.capture());
        assertThat(tCaptor.getValue().size(), is(1));
        assertThat((TransportChannelConfig)tCaptor.getValue().get(0), is(tConfig));
        
        verify(m_OSGiConfiguration, never()).update(Mockito.any(Dictionary.class));

        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT);
    }

    /**
     * Verify that on framework started event with an empty configuration file does not invoke any of the process
     * methods.
     */
    @Test
    public void testFrameworkEventStarted_EmptyConfiguration() throws IOException, UnmarshalException
    {
        when(m_ConfigAdmin.getConfiguration(ConfigurationMgr.class.getName())).thenReturn(m_OSGiConfiguration);
        when(m_OSGiConfiguration.getProperties()).thenReturn(null);
        
        List<FactoryObjectConfig> fConfigs = new ArrayList<>();
        List<PidConfig> pConfigs = new ArrayList<>();
        List<AddressConfig> aConfigs = new ArrayList<>();
        List<EventConfig> eConfigs = new ArrayList<>();
        List<SocketChannelConfig> sConfigs = new ArrayList<>();
        List<TransportChannelConfig> tConfigs = new ArrayList<>();
        
        when(m_XmlConfiguration.getFactoryObjects()).thenReturn(fConfigs);
        when(m_XmlConfiguration.getOsgiConfigs()).thenReturn(pConfigs);
        when(m_XmlConfiguration.getAddresses()).thenReturn(aConfigs);
        when(m_XmlConfiguration.getEventRegs()).thenReturn(eConfigs);
        when(m_XmlConfiguration.getSocketChannels()).thenReturn(sConfigs);
        when(m_XmlConfiguration.getTransportChannels()).thenReturn(tConfigs);
        
        m_SUT.activate(m_BundleContext);
        
        verify(m_FactoryObjLoader).process(eq(fConfigs), Mockito.anyBoolean());
        verify(m_OsgiConfigLoader).process(eq(pConfigs), Mockito.anyBoolean());
        verify(m_AddrConfigLoader).process(eq(aConfigs), Mockito.anyBoolean());
        verify(m_EventConfigLoader).process(eq(eConfigs));
        verify(m_ChannelConfigLoader).processSocketChannels(eq(sConfigs));
        verify(m_ChannelConfigLoader).processTransportChannels(eq(tConfigs));

        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT);
    }
    
    /**
     * Verify that on framework started event if the configuration file cannot be found no process methods are invoked
     * or update of the configuration admin properties.
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testFrameworkEventStarted_NoConfigsFile() throws IOException, UnmarshalException
    {
        when(m_BundleContext.getProperty(Mockito.anyString())).thenReturn("whoa/what/itsnotthere/");
        when(m_FileService.doesFileExist(Mockito.any(File.class))).thenReturn(false);
        when(m_ConfigAdmin.getConfiguration(ConfigurationMgr.class.getName())).thenReturn(m_OSGiConfiguration);
        when(m_OSGiConfiguration.getProperties()).thenReturn(null);
        
        m_SUT.activate(m_BundleContext);
        
        verify(m_FactoryObjLoader, never()).process(anyList(), anyBoolean());
        verify(m_OsgiConfigLoader, never()).process(anyList(), anyBoolean());
        verify(m_AddrConfigLoader, never()).process(anyList(), anyBoolean());
        verify(m_EventConfigLoader, never()).process(anyList());
        verify(m_ChannelConfigLoader, never()).processSocketChannels(anyList());
        verify(m_ChannelConfigLoader, never()).processTransportChannels(anyList());

        EventAdminVerifier.assertNoEventByTopic(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT);
    }

    /**
     * Verify that the framework started event is handled properly and configurations are processed when a configs.xml
     * file is found during activation.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testFrameworkEventStarted_ConfigsProcessed() throws UnmarshalException, IOException
    {
        when(m_ConfigAdmin.getConfiguration(ConfigurationMgr.class.getName())).thenReturn(m_OSGiConfiguration);
        when(m_OSGiConfiguration.getProperties()).thenReturn(null);
        
        List<FactoryObjectConfig> fConfigs = new ArrayList<>();
        FactoryObjectConfig config = mock(FactoryObjectConfig.class);
        fConfigs.add(config);
        
        List<PidConfig> pConfigs = new ArrayList<>();
        PidConfig pconfig = mock(PidConfig.class);
        pConfigs.add(pconfig);
        
        List<AddressConfig> aConfigs = new ArrayList<>();
        AddressConfig aconfig = mock(AddressConfig.class);
        aConfigs.add(aconfig);
        
        List<EventConfig> eConfigs = new ArrayList<>();
        EventConfig eConfig = mock(EventConfig.class);
        eConfigs.add(eConfig);
        
        List<SocketChannelConfig> sConfigs = new ArrayList<>();
        SocketChannelConfig sConfig = mock(SocketChannelConfig.class);
        sConfigs.add(sConfig);
        
        List<TransportChannelConfig> tConfigs = new ArrayList<>();
        TransportChannelConfig tConfig = mock(TransportChannelConfig.class);
        tConfigs.add(tConfig);
        
        when(m_XmlConfiguration.getFactoryObjects()).thenReturn(fConfigs);
        when(m_XmlConfiguration.getOsgiConfigs()).thenReturn(pConfigs);
        when(m_XmlConfiguration.getAddresses()).thenReturn(aConfigs);
        when(m_XmlConfiguration.getEventRegs()).thenReturn(eConfigs);
        when(m_XmlConfiguration.getSocketChannels()).thenReturn(sConfigs);
        when(m_XmlConfiguration.getTransportChannels()).thenReturn(tConfigs);
        
        m_SUT.activate(m_BundleContext);
        
        ArgumentCaptor<List> fCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_FactoryObjLoader).process(fCaptor.capture(), eq(true));
        
        assertThat(fCaptor.getValue().size(), is(1));
        assertThat((FactoryObjectConfig)fCaptor.getValue().get(0), is(config));
        
        ArgumentCaptor<List> pCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_OsgiConfigLoader).process(pCaptor.capture(), eq(true));
        
        assertThat(pCaptor.getValue().size(), is(1));
        assertThat((PidConfig)pCaptor.getValue().get(0), is(pconfig));
        
        ArgumentCaptor<List> aCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_AddrConfigLoader).process(aCaptor.capture(), eq(true));
        
        assertThat(aCaptor.getValue().size(), is(1));
        assertThat((AddressConfig)aCaptor.getValue().get(0), is(aconfig));
        
        ArgumentCaptor<List> eCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_EventConfigLoader).process(eCaptor.capture());
        assertThat(eCaptor.getValue().size(), is(1));
        assertThat((EventConfig)eCaptor.getValue().get(0), is(eConfig));
        
        ArgumentCaptor<List> sCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_ChannelConfigLoader).processSocketChannels(sCaptor.capture());
        assertThat(sCaptor.getValue().size(), is(1));
        assertThat((SocketChannelConfig)sCaptor.getValue().get(0), is(sConfig));
        
        ArgumentCaptor<List> tCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_ChannelConfigLoader).processTransportChannels(tCaptor.capture());
        assertThat(tCaptor.getValue().size(), is(1));
        assertThat((TransportChannelConfig)tCaptor.getValue().get(0), is(tConfig));

        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT);
    }
    
    /**
     * Verify that on framework started event with an error loading the configuration file, it does not invoke any of 
     * the process methods.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFrameworkEventStarted_ErrorLoadingConfig() throws IOException, UnmarshalException
    {
        when(m_ConfigAdmin.getConfiguration(ConfigurationMgr.class.getName())).thenThrow(new IOException("mock"));
        
        m_SUT.activate(m_BundleContext);
        
        verify(m_FactoryObjLoader, never()).process(anyList(), anyBoolean());
        verify(m_OsgiConfigLoader, never()).process(anyList(), anyBoolean());
        verify(m_AddrConfigLoader, never()).process(anyList(), anyBoolean());
        verify(m_EventConfigLoader, never()).process(anyList());
        verify(m_ChannelConfigLoader, never()).processSocketChannels(anyList());
        verify(m_ChannelConfigLoader, never()).processTransportChannels(anyList());

        EventAdminVerifier.assertNoEventByTopic(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT);
    }
}
