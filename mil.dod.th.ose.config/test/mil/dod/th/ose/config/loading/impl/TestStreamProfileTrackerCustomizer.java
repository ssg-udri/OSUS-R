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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileException;
import mil.dod.th.core.datastream.StreamProfileFactory;
import mil.dod.th.core.datastream.StreamProfileProxy;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.StringMapEntry;
import mil.dod.th.model.config.CreatePolicyEnum;
import mil.dod.th.model.config.FactoryObjectConfig;
import mil.dod.th.model.config.FactoryTypeEnum;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.test.AttributeDefinitionMocker;
import mil.dod.th.ose.test.EventAdminVerifier;
import mil.dod.th.ose.test.FactoryObjectMocker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * @author jmiller
 *
 */
public class TestStreamProfileTrackerCustomizer
{
    
    @Mock private DataStreamService m_DataStreamService;
    @Mock private EventAdmin m_EventAdmin;
    @Mock private LoggingService m_Log;
    @Mock private ServiceReference<FactoryDescriptor> m_ServiceReference;
    private StreamProfile m_StreamProfile;
    private List<StringMapEntry> m_Properties;
    private Map<String, Object> m_ExpectedProps;
    
    
    private String TEST_PID = StreamProfileProxy.class.getName();
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        
        m_StreamProfile = FactoryObjectMocker.mockFactoryObject(StreamProfile.class, TEST_PID);
        
        StreamProfileFactory factory = m_StreamProfile.getFactory();
        AttributeDefinition[] attrDefs = AttributeDefinitionMocker.mockArrayAll();
        when(factory.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(attrDefs);
        
        m_Properties = new ArrayList<>();
        
        m_ExpectedProps = new HashMap<>();
        
        m_ServiceReference = mock(FactoryServiceReference.class);
        
        Bundle bundle = mock(Bundle.class);
        when(m_ServiceReference.getBundle()).thenReturn(bundle);
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundle.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(m_ServiceReference)).thenReturn(factory);
        
    }
    
    @After
    public void tearDown()
    {
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJ_LOADED_EVENT);
    }
    
    /**
     * Test adding a StreamProfile that doesn't exist yet.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingServiceMissing() throws StreamProfileException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.STREAM_PROFILE)
            .withProperties(m_Properties)
            .withProductType(TEST_PID)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName("StreamProfile1");
        
        when(m_DataStreamService.getStreamProfiles()).thenReturn(new HashSet<StreamProfile>());
        when(m_DataStreamService.createStreamProfile(eq(TEST_PID), eq("StreamProfile1"), 
                any(Map.class))).thenReturn(m_StreamProfile);
        
        StreamProfileTrackerCustomizer sut = new StreamProfileTrackerCustomizer(objectConfig,
                m_DataStreamService, m_Log, m_EventAdmin);
        
        sut.addingService(m_ServiceReference);
        
        verify(m_DataStreamService, times(1)).createStreamProfile(eq(TEST_PID), 
                eq("StreamProfile1"), eq(m_ExpectedProps));
    }
    
    /**
     * Verify that if a service is added and the stream profile already exists, it is not recreated.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingServiceExists() throws IllegalArgumentException, StreamProfileException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.STREAM_PROFILE)
            .withProperties(m_Properties)
            .withProductType(TEST_PID)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName("StreamProfile1");
        
        when(m_StreamProfile.getName()).thenReturn("StreamProfile1");
        
        Set<StreamProfile> profiles = new HashSet<>();
        profiles.add(m_StreamProfile);
        when(m_DataStreamService.getStreamProfiles()).thenReturn(profiles);
        
        StreamProfileTrackerCustomizer sut = new StreamProfileTrackerCustomizer(objectConfig,
                m_DataStreamService, m_Log, m_EventAdmin);
        sut.addingService(m_ServiceReference);
        verify(m_DataStreamService, never()).createStreamProfile(eq(TEST_PID), eq("StreamProfile1"), any(Map.class));
    }
    
    /**
     * Verify error handling if a stream profile cannot be created.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingServiceStreamProfileException() throws IllegalArgumentException, StreamProfileException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.STREAM_PROFILE)
            .withProperties(m_Properties)
            .withProductType(TEST_PID)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName("StreamProfile1");
        
        when(m_DataStreamService.getStreamProfiles()).thenReturn(new HashSet<StreamProfile>());
        when(m_DataStreamService.createStreamProfile(eq(TEST_PID), eq("StreamProfile1"), any(Map.class))).
            thenThrow(new StreamProfileException("problem"));
        
        StreamProfileTrackerCustomizer sut = new StreamProfileTrackerCustomizer(objectConfig, m_DataStreamService,
                m_Log, m_EventAdmin);
        sut.addingService(m_ServiceReference);
        
        verify(m_DataStreamService, times(1)).createStreamProfile(eq(TEST_PID), eq("StreamProfile1"), any(Map.class));
        verify(m_Log).log(eq(LogService.LOG_WARNING), any(FactoryException.class), anyString(), any());
    }

}
