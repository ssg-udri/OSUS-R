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
import java.util.List;
import java.util.Map;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.asset.AssetProxy;
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
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

public class TestAssetTrackerCustomizer
{
    private String TEST_PID = AssetProxy.class.getName();
    private AssetDirectoryService m_AssetDirService;
    private List<StringMapEntry> m_Properties;
    private Map<String, Object> m_ExpectedProps;
    private ServiceReference<FactoryDescriptor> m_ServiceReference;
    private LoggingService m_Log;
    private EventAdmin m_EventAdmin;
    private Asset m_Asset;

    @Before
    public void setUp() throws Exception
    {
        m_AssetDirService = mock(AssetDirectoryService.class);
        m_Log = LoggingServiceMocker.createMock();
        m_EventAdmin = mock(EventAdmin.class);
        m_Asset = FactoryObjectMocker.mockFactoryObject(Asset.class, TEST_PID);

        AssetFactory factory = m_Asset.getFactory();
        AttributeDefinition[] attrDefs = AttributeDefinitionMocker.mockArrayAll();
        when(factory.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(attrDefs);

        m_Properties = new ArrayList<>();
        m_Properties.add(new StringMapEntry("id1", "false"));
        m_Properties.add(new StringMapEntry("id2", "2"));
        m_Properties.add(new StringMapEntry("id3", "stringvalue"));
        m_Properties.add(new StringMapEntry("id4", "2.1"));

        m_ExpectedProps = new HashMap<>();
        m_ExpectedProps.put("id1", false);
        m_ExpectedProps.put("id2", 2);
        m_ExpectedProps.put("id3", "stringvalue");
        m_ExpectedProps.put("id4", 2.1f);

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
     * Test adding an Asset that isn't created yet.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingServiceMissing() throws IllegalArgumentException, AssetException, IllegalStateException,
            FactoryException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.ASSET)
            .withProperties(m_Properties)
            .withProductType(TEST_PID)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName("Asset1");

        when(m_AssetDirService.isAssetAvailable("Asset1")).thenReturn(false);

        when(m_AssetDirService.createAsset(eq(TEST_PID), eq("Asset1"), any(Map.class))).thenReturn(m_Asset);

        AssetTrackerCustomizer sut = new AssetTrackerCustomizer(objectConfig, m_AssetDirService, m_Log, m_EventAdmin);

        sut.addingService(m_ServiceReference);

        verify(m_AssetDirService, times(1)).createAsset(eq(TEST_PID), eq("Asset1"), eq(m_ExpectedProps));
    }
    
    /**
     * Verify that if a service is added and asset already exists that it not recreated.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingServiceExists() throws IllegalArgumentException, AssetException, 
        IllegalStateException, FactoryException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.ASSET)
            .withProperties(m_Properties)
            .withProductType(TEST_PID)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName("Asset1");

        when(m_AssetDirService.isAssetAvailable("Asset1")).thenReturn(true);

        when(m_AssetDirService.getAssetByName("Asset1")).thenReturn(m_Asset);

        AssetTrackerCustomizer sut = new AssetTrackerCustomizer(objectConfig, m_AssetDirService, m_Log, m_EventAdmin);

        sut.addingService(m_ServiceReference);

        verify(m_AssetDirService, never()).createAsset(eq(TEST_PID), eq("Asset1"), any(Map.class));
    }
    
    /**
     *  Verify error handling if an asset cannot be created.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddingServiceAssetException() throws IllegalArgumentException, AssetException, 
        IllegalStateException, FactoryException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.ASSET)
            .withProperties(m_Properties)
            .withProductType(TEST_PID)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName("Asset1");
        
        when(m_AssetDirService.isAssetAvailable("Asset1")).thenReturn(false);

        when(m_AssetDirService.createAsset(eq(TEST_PID), eq("Asset1"), any(Map.class))).thenThrow(
                new AssetException("problem"));

        AssetTrackerCustomizer sut = new AssetTrackerCustomizer(objectConfig, m_AssetDirService, m_Log, m_EventAdmin);
        
        sut.addingService(m_ServiceReference);

        verify(m_AssetDirService, times(1)).createAsset(eq(TEST_PID), eq("Asset1"), any(Map.class));
        verify(m_Log).log(eq(LogService.LOG_WARNING), Mockito.any(FactoryException.class), Mockito.anyString(), 
                Mockito.any());
    }
}
