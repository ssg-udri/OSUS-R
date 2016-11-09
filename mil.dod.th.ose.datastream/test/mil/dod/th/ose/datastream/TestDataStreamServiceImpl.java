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
package mil.dod.th.ose.datastream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileException;
import mil.dod.th.core.datastream.StreamProfileFactory;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.datastream.DataStreamServiceImpl.ConfigurationListener;
import mil.dod.th.ose.datastream.DataStreamServiceImpl.PidRemovedListener;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * @author jmiller
 *
 */
@RunWith(PowerMockRunner.class)
public class TestDataStreamServiceImpl
{
    private static final String PRODUCT_TYPE = "product-type";
    final private String m_ProductName = "StreamProfileProxy1";
    private DataStreamServiceImpl m_SUT;
    @Mock private FactoryInternal m_StreamProfileFactory;
    @Mock private Map<String, StreamProfileFactory> m_StreamProfileFactories;
    @Mock private FactoryRegistry<StreamProfileInternal> m_Registry;
    @Mock private BundleContext m_BundleContext;
    @Mock private ComponentInstance m_FactoryServiceContextInstance;
    @Mock private FactoryServiceProxy<StreamProfileInternal> m_FactoryServiceProxy;
    @Mock private ComponentFactory m_ServiceContextFactory;
    @Mock private EventAdmin m_EventAdmin;
    @Mock private ServiceRegistration<EventHandler> m_ServiceReg;
    @Mock private LoggingService m_Logging;
    
    @SuppressWarnings("rawtypes")
    @Mock private FactoryServiceContext m_ServiceContext;
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        m_Logging = LoggingServiceMocker.createMock();
        
        m_SUT = new DataStreamServiceImpl();
        
        m_StreamProfileFactories = new HashMap<>();
        
        when(m_StreamProfileFactory.getProductName()).thenReturn(m_ProductName);
        when(m_StreamProfileFactory.getProductType()).thenReturn(PRODUCT_TYPE);
        m_StreamProfileFactories.put(PRODUCT_TYPE, m_StreamProfileFactory);
        
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setFactoryServiceProxy(m_FactoryServiceProxy);
        m_SUT.setFactoryServiceContextFactory(m_ServiceContextFactory);
        m_FactoryServiceContextInstance = ComponentFactoryMocker.mockSingleComponent(
                FactoryServiceContext.class, m_ServiceContextFactory).getInstance();
        m_ServiceContext = (FactoryServiceContext)m_FactoryServiceContextInstance.getInstance();
        when(m_ServiceContext.getFactories()).thenReturn(m_StreamProfileFactories);
        when(m_ServiceContext.getRegistry()).thenReturn(m_Registry);
        when(m_BundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_ServiceReg);
    }
    
    @After
    public void tearDown()
    {
        m_SUT.deactivate();
        verify(m_FactoryServiceContextInstance).dispose();
    }
    
    /**
     * Verify registry and service context are initialized properly.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivation() throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 5004);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        verify(m_ServiceContextFactory).newInstance(null);
        verify(m_ServiceContext).initialize(m_BundleContext, m_FactoryServiceProxy, m_SUT);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testActivationWithoutConfigProps() throws InvalidSyntaxException
    {
        m_SUT.activate(new HashMap<String, Object>(), m_BundleContext);
        verify(m_ServiceContextFactory).newInstance(null);
        verify(m_ServiceContext).initialize(m_BundleContext, m_FactoryServiceProxy, m_SUT);
        
        assertThat((String)Whitebox.getInternalState(m_SUT, "m_MulticastHost"), is(""));
        assertThat((int)Whitebox.getInternalState(m_SUT, "m_StartPort"), is(-1));        
    }
    
    /**
     * Verify an invalid product type will cause an exception.
     */
    @Test
    public void testCreateStreamProfileWithInvalidProductType() throws StreamProfileException, InvalidSyntaxException
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 5004);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        try
        {
            m_SUT.createStreamProfile("filler", "name", new HashMap<String, Object>());
            fail("Expecting failure because product type name is invalid");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
    
    /**
    * Verify that stream profile is created and dictionary passed in is correct.
    */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testCreateStreamProfile() throws StreamProfileException, FactoryException, 
        FactoryObjectInformationException, InvalidSyntaxException
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 5004);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        StreamProfileInternal mockStreamProfile = mock(StreamProfileInternal.class);
        when(m_Registry.createNewObject(eq(m_StreamProfileFactory), Mockito.any(String.class),
                Mockito.any(Map.class))).thenReturn(mockStreamProfile);
        
        StreamProfile streamProfile = m_SUT.createStreamProfile(PRODUCT_TYPE, m_ProductName, 
                new HashMap<String, Object>());
        assertThat(streamProfile, is(notNullValue()));
        
        verify(m_Registry).createNewObject(m_StreamProfileFactory, m_ProductName, new HashMap<String, Object>());
        
        Set<StreamProfile> profileSet = new HashSet<>();
        profileSet.add(streamProfile);
        when(m_SUT.getStreamProfiles()).thenReturn(profileSet);        
        
        //create second stream profile
        StreamProfileInternal mockStreamProfile2 = mock(StreamProfileInternal.class);
        when(m_Registry.createNewObject(eq(m_StreamProfileFactory), Mockito.any(String.class),
                Mockito.any(Map.class))).thenReturn(mockStreamProfile2);
        
        StreamProfile streamProfile2 = m_SUT.createStreamProfile(PRODUCT_TYPE, "StreamProfileProxy2", 
                new HashMap<String, Object>());
        assertThat(streamProfile2, is(notNullValue()));
        
        verify(m_Registry).createNewObject(m_StreamProfileFactory, "StreamProfileProxy2", 
                new HashMap<String, Object>());
    }
    
    /**
     * Test handling of exception when trying to create a stream profile.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateStreamProfileException() throws FactoryObjectInformationException,
        FactoryException, IllegalArgumentException, InvalidSyntaxException
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 5004);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        when(m_Registry.createNewObject(eq(m_StreamProfileFactory), Mockito.any(String.class),
                Mockito.any(Map.class))).thenThrow(new IllegalArgumentException("test error"));
  
        try
        {
            m_SUT.createStreamProfile(PRODUCT_TYPE, m_ProductName, new HashMap<String, Object>());
            fail("Expected exception from createStreamProfile");
            
        }
        catch (StreamProfileException e)
        {
            
        }
    }
    
    /**
     * Verify that the service will return the factory descriptors for all registered factories. 
     */
    @Test
    public void testGetStreamProfileFactoryDescriptors() throws InvalidSyntaxException
    {      
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 5004);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        when(m_ServiceContext.getFactories()).thenReturn(m_StreamProfileFactories);
        
        Set<StreamProfileFactory> expectedFactories = 
                new HashSet<StreamProfileFactory>(m_StreamProfileFactories.values());
        Set<StreamProfileFactory> factories = m_SUT.getStreamProfileFactories();
        assertThat(factories, is(expectedFactories));
    }
    
    /**
     * Verify that stream profiles are returned if they exist in the registry.
     */
    @Test
    public void testGetStreamProfiles() throws InvalidSyntaxException
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 5004);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        Set<StreamProfileInternal> set = new HashSet<StreamProfileInternal>();
        set.add(mock(StreamProfileInternal.class));
        when(m_Registry.getObjects()).thenReturn(set);
        
        Set<StreamProfile> allStreamProfiles = m_SUT.getStreamProfiles();
        assertThat("Null retrieved from getStreamProfiles method", allStreamProfiles, is(notNullValue()));
        assertThat(allStreamProfiles.size(), is(1));
    }
    
    /**
     * Verify that requesting a stream profile by UUID throws an exception if the UUID is null.
     * Verify the correct stream profile is returned if the UUID is not null.
     */
    @Test
    public void testGetStreamProfileByUuid() throws InvalidSyntaxException
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 5004);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);        
        
        when(m_Registry.getObjectByUuid((UUID)isNull())).thenThrow(new IllegalArgumentException("UUID cannot be null"));
        
        try
        {
            m_SUT.getStreamProfile(null);
            fail("Expected exception from getStreamProfile");
        }
        catch (IllegalArgumentException e)
        {
           
        }
        
        StreamProfileInternal internal = mock(StreamProfileInternal.class);
        UUID uuid = UUID.randomUUID();        
        when(m_Registry.getObjectByUuid(uuid)).thenReturn(internal);
        StreamProfile retrievedStreamProfile = m_SUT.getStreamProfile(uuid);
        assertThat(retrievedStreamProfile, notNullValue());
    }
    
    /**
     * Verify the correct stream profiles are returned when requesting all stream profiles associated
     * with a particular asset.
     */
    @Test
    public void testGetStreamProfilesByAsset() throws InvalidSyntaxException
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 5004);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        final String assetName1 = "TestAsset1";
        final String assetName2 = "TestAsset2";
        
        StreamProfileInternal internal1 = mock(StreamProfileInternal.class);
        StreamProfileInternal internal2 = mock(StreamProfileInternal.class);
        StreamProfileInternal internal3 = mock(StreamProfileInternal.class);
        StreamProfileInternal internal4 = mock(StreamProfileInternal.class);
        StreamProfileInternal internal5 = mock(StreamProfileInternal.class);
        
        Asset asset1 = mock(Asset.class);
        Asset asset2 = mock(Asset.class);
        when(asset1.getName()).thenReturn(assetName1);
        when(asset2.getName()).thenReturn(assetName2);
        
        when(internal1.getAsset()).thenReturn(asset1);
        when(internal2.getAsset()).thenReturn(asset1);
        when(internal3.getAsset()).thenReturn(asset2);
        when(internal4.getAsset()).thenReturn(asset2);
        when(internal5.getAsset()).thenReturn(asset2);
        
        Set<StreamProfileInternal> set = new HashSet<StreamProfileInternal>();
        set.add(internal1);
        set.add(internal2);
        set.add(internal3);
        set.add(internal4);
        set.add(internal5);
        when(m_Registry.getObjects()).thenReturn(set);
        
        Set<StreamProfile> streamProfileSet1 = m_SUT.getStreamProfiles(asset1);
        assertThat(streamProfileSet1.size(), is(2));
        
        Set<StreamProfile> streamProfileSet2 = m_SUT.getStreamProfiles(asset2);
        assertThat(streamProfileSet2.size(), is(3));            
    }
    
    @Test
    public void testModifyConfiguration() throws InvalidSyntaxException
    {        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 5004);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        Map<String, Object> newProps = new HashMap<String, Object>();
        newProps.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, "226.1.2.3");
        newProps.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, 6004);
        
        m_SUT.modified(newProps);
        
        assertThat((String)Whitebox.getInternalState(m_SUT, "m_MulticastHost"), is("226.1.2.3"));
        assertThat((int)Whitebox.getInternalState(m_SUT, "m_StartPort"), is(6004));        
    }
    
    /**
     * Verify that a factory object is created for the configuration specified by the event.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConfigurationListener() throws InvalidSyntaxException, IllegalArgumentException, FactoryException, 
        FactoryObjectInformationException, IOException, InterruptedException
    {
        String factoryPid = "factory.pid";
        String host = "225.1.2.3";
        int port = 5004;
        StreamProfileInternal spi = mock(StreamProfileInternal.class);
        FactoryInternal spf = mock(FactoryInternal.class);
        m_StreamProfileFactories.put("some-type", spf);
        
        when(m_ServiceContext.getFactories()).thenReturn(m_StreamProfileFactories);
        when(m_StreamProfileFactory.getPid()).thenReturn(factoryPid);
        when(spf.getPid()).thenReturn("not.the.factory.you.are.looking.for");
        when(m_Registry.createNewObjectForConfig(m_StreamProfileFactory, null, "config.pid")).thenReturn(spi);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, host);
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, port);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        ArgumentCaptor<EventHandler> handlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class), handlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        ConfigurationListener configListener = (ConfigurationListener)handlerCaptor.getAllValues().get(0);
        
        Map<String, Object> topicProps = new HashMap<>();
        topicProps.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, factoryPid);
        topicProps.put(ConfigurationEventConstants.EVENT_PROP_PID, "config.pid");
        Event configAddedEvent = new Event(StreamProfileConfigListener.TOPIC_STREAM_PROFILE_CONFIG_UPDATED, topicProps);
        
        configListener.handleEvent(configAddedEvent);
        
        //Sleep needed to allow thread time create the object.
        Thread.sleep(500);
        
        verify(m_Registry).createNewObjectForConfig(m_StreamProfileFactory, null, "config.pid");
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(spi).setStreamPort(uriCaptor.capture());
        URI uri = uriCaptor.getValue();
        assertThat(uri.getHost(), is(host));
        assertThat(uri.getPort(), is(port));
    }
    
    /**
     * Verify there is no attempt to create a factory object if no factory exists.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConfigurationListenerNoFactory() throws InvalidSyntaxException, IllegalArgumentException, 
        FactoryException, FactoryObjectInformationException, IOException, InterruptedException
    {
        String factoryPid = "factory.pid";
        String host = "225.1.2.3";
        int port = 5004;
        
        when(m_ServiceContext.getFactories()).thenReturn(new HashMap<String, FactoryInternal>());
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, host);
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, port);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        ArgumentCaptor<EventHandler> handlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class), handlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        ConfigurationListener configListener = (ConfigurationListener)handlerCaptor.getAllValues().get(0);
        
        Map<String, Object> topicProps = new HashMap<>();
        topicProps.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, factoryPid);
        topicProps.put(ConfigurationEventConstants.EVENT_PROP_PID, "config.pid");
        Event configAddedEvent = new Event(StreamProfileConfigListener.TOPIC_STREAM_PROFILE_CONFIG_UPDATED, topicProps);
        
        configListener.handleEvent(configAddedEvent);
        
        //Sleep needed to allow thread time create the object.
        Thread.sleep(500);
        
        verify(m_Registry, never()).createNewObjectForConfig(m_StreamProfileFactory, null, "config.pid");
    }
    
    /**
     * Verify there is no attempt to create a factory object if no factory PID is set for the event received.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConfigurationListenerNullFactoryPid() throws InvalidSyntaxException, IllegalArgumentException, 
        FactoryException, FactoryObjectInformationException, IOException, InterruptedException
    {
        String host = "225.1.2.3";
        int port = 5004;
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, host);
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, port);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        ArgumentCaptor<EventHandler> handlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class), handlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        ConfigurationListener configListener = (ConfigurationListener)handlerCaptor.getAllValues().get(0);
        
        Map<String, Object> topicProps = new HashMap<>();
        topicProps.put(ConfigurationEventConstants.EVENT_PROP_PID, "config.pid");
        Event configAddedEvent = new Event(StreamProfileConfigListener.TOPIC_STREAM_PROFILE_CONFIG_UPDATED, topicProps);
        
        configListener.handleEvent(configAddedEvent);
        
        //Sleep needed to allow thread time create the object.
        Thread.sleep(500);
        
        verify(m_Registry, never()).createNewObjectForConfig(m_StreamProfileFactory, null, "config.pid");
        verify(m_ServiceContext, never()).getFactories();
    }
    
    /**
     * Verify that an exception is logged if an error occurs while attempting to create a new stream profile object
     * for the specified configuration.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConfigurationListenerCreateException() throws InvalidSyntaxException, IllegalArgumentException, 
        FactoryException, FactoryObjectInformationException, IOException, InterruptedException
    {
        String factoryPid = "factory.pid";
        String host = "225.1.2.3";
        int port = 5004;
        FactoryInternal spf = mock(FactoryInternal.class);
        m_StreamProfileFactories.put("some-type", spf);
        
        when(m_ServiceContext.getFactories()).thenReturn(m_StreamProfileFactories);
        when(m_StreamProfileFactory.getPid()).thenReturn(factoryPid);
        when(spf.getPid()).thenReturn("not.the.factory.you.are.looking.for");
        when(m_Registry.createNewObjectForConfig(m_StreamProfileFactory, null, "config.pid"))
            .thenThrow(FactoryException.class);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, host);
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, port);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        ArgumentCaptor<EventHandler> handlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class), handlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        ConfigurationListener configListener = (ConfigurationListener)handlerCaptor.getAllValues().get(0);
        
        Map<String, Object> topicProps = new HashMap<>();
        topicProps.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, factoryPid);
        topicProps.put(ConfigurationEventConstants.EVENT_PROP_PID, "config.pid");
        Event configAddedEvent = new Event(StreamProfileConfigListener.TOPIC_STREAM_PROFILE_CONFIG_UPDATED, topicProps);
        
        configListener.handleEvent(configAddedEvent);
        
        //Sleep needed to allow thread time create the object.
        Thread.sleep(500);
        
        verify(m_Registry).createNewObjectForConfig(m_StreamProfileFactory, null, "config.pid");
        verify(m_Logging).error(Mockito.any(FactoryException.class), eq("Unable to automatically create a stream "
                + "profile object for the configuration with PID: %s"), eq("config.pid"));
    }
    
    /**
     * Verify that an exception is logged if an error occurs while attempting to set the stream port for the newly
     * create stream profile object.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConfigurationListenerInvalidStreamPort() throws InvalidSyntaxException, IllegalArgumentException, 
        FactoryException, FactoryObjectInformationException, IOException, InterruptedException
    {
        String factoryPid = "factory.pid";
        String host = "225.1.2.3";
        int port = 5004;
        String spiName = "stream_profile_1";
        StreamProfileInternal spi = mock(StreamProfileInternal.class);
        
        when(m_ServiceContext.getFactories()).thenReturn(m_StreamProfileFactories);
        when(m_StreamProfileFactory.getPid()).thenReturn(factoryPid);
        when(m_Registry.createNewObjectForConfig(m_StreamProfileFactory, null, "config.pid")).thenReturn(spi);
        when(spi.getName()).thenReturn(spiName);
        doThrow(URISyntaxException.class).when(spi).setStreamPort(Mockito.any(URI.class));
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, host);
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, port);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        ArgumentCaptor<EventHandler> handlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class), handlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        ConfigurationListener configListener = (ConfigurationListener)handlerCaptor.getAllValues().get(0);
        
        Map<String, Object> topicProps = new HashMap<>();
        topicProps.put(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID, factoryPid);
        topicProps.put(ConfigurationEventConstants.EVENT_PROP_PID, "config.pid");
        Event configAddedEvent = new Event(StreamProfileConfigListener.TOPIC_STREAM_PROFILE_CONFIG_UPDATED, topicProps);
        
        configListener.handleEvent(configAddedEvent);
        
        //Sleep needed to allow thread time create the object.
        Thread.sleep(500);
        
        verify(m_Registry).createNewObjectForConfig(m_StreamProfileFactory, null, "config.pid");
        verify(spi).setStreamPort(Mockito.any(URI.class));
        verify(m_Logging).error(Mockito.any(URISyntaxException.class), eq("Unable to set stream port for the stream "
                + "profile: %s"), eq(spiName));
    }
    
    /**
     * Verify that the listener removes the stream profile factory object that just had its PID removed.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testPidRemovedListener() throws InvalidSyntaxException
    {
        String host = "225.1.2.3";
        int port = 5004;
        StreamProfileInternal spi = mock(StreamProfileInternal.class);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST, host);
        props.put(DataStreamServiceConfig.CONFIG_PROP_START_PORT, port);
        
        m_SUT.activate(Collections.unmodifiableMap(props), m_BundleContext);
        
        ArgumentCaptor<EventHandler> handlerCaptor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class), handlerCaptor.capture(), 
                Mockito.any(Dictionary.class));
        
        PidRemovedListener listener = (PidRemovedListener)handlerCaptor.getValue();
        
        Map<String, Object> topicProps = new HashMap<>();
        topicProps.put(FactoryDescriptor.EVENT_PROP_OBJ, spi);
        Event pidRemovedEvent = new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_REMOVED, topicProps);
        
        listener.handleEvent(pidRemovedEvent);
        
        verify(spi).delete();
    }
}
