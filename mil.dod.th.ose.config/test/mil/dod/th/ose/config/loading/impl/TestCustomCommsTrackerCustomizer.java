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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.core.types.StringMapEntry;
import mil.dod.th.model.config.CreatePolicyEnum;
import mil.dod.th.model.config.FactoryObjectConfig;
import mil.dod.th.model.config.FactoryTypeEnum;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.test.AttributeDefinitionMocker;
import mil.dod.th.ose.test.EventAdminVerifier;
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

public class TestCustomCommsTrackerCustomizer
{
    private static final String LL_TEST_PID = "com.test.ccomms.linklayer";
    private static final String TL_TEST_PID = "com.test.ccomms.transport";
    private static final String PL_TEST_NAME = "PhysicalLink1";
    private static final String LL_TEST_NAME = "LinkLayer1";
    private static final String TL_TEST_NAME = "TransportLayer1";

    private CustomCommsService m_CustomCommsService;
    private List<StringMapEntry> m_StringMapList;
    private Map<String, Object> m_Properties;
    private ServiceReference<FactoryDescriptor> m_ServiceReference;
    private LoggingService m_Log;
    private EventAdmin m_EventAdmin;
    private LinkLayer m_Link;
    private BundleContext m_BundleContext;

    @Before
    public void setUp() throws Exception
    {
        m_CustomCommsService = mock(CustomCommsService.class);
        m_Log = LoggingServiceMocker.createMock();
        m_EventAdmin = mock(EventAdmin.class);
        
        //mock links
        m_Link = mock(LinkLayer.class);
        when(m_Link.getName()).thenReturn(LL_TEST_NAME);

        FactoryDescriptor factory = mock(FactoryDescriptor.class);
        AttributeDefinition[] attrDefs = AttributeDefinitionMocker.mockArrayAll();
        when(factory.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(attrDefs);
        m_ServiceReference = mock(FactoryServiceReference.class);
        
        Bundle bundle = mock(Bundle.class);
        m_BundleContext = mock(BundleContext.class);
        when(m_ServiceReference.getBundle()).thenReturn(bundle);
        when(bundle.getBundleContext()).thenReturn(m_BundleContext);
        when(m_BundleContext.getService(m_ServiceReference)).thenReturn(factory);
        
        m_StringMapList = new ArrayList<>();
        m_StringMapList.add(new StringMapEntry("id1", "false"));
        m_StringMapList.add(new StringMapEntry("id2", "2"));
        m_StringMapList.add(new StringMapEntry("id3", "stringvalue"));
        m_StringMapList.add(new StringMapEntry("id4", "2.1"));

        m_Properties = new HashMap<>();
        m_Properties.put("id1", false);
        m_Properties.put("id2", 2);
        m_Properties.put("id3", "stringvalue");
        m_Properties.put("id4", 2.1f);
    }

    @After
    public void tearDown()
    {
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJ_LOADED_EVENT);
    }

    /**
     * Test adding a PhysicalLink that isn't created yet.
     */
    @Test
    public void testAddingPhysicalLinkMissing() throws IllegalArgumentException, CCommException, IllegalStateException,
            FactoryException, IOException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.PHYSICAL_LINK)
            .withProperties(m_StringMapList)
            .withPhysicalLinkType(PhysicalLinkTypeEnum.GPIO)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName(PL_TEST_NAME);

        CustomCommsTrackerCustomizer sut = new CustomCommsTrackerCustomizer(objectConfig, m_CustomCommsService, m_Log,
            m_EventAdmin);

        sut.addingService(m_ServiceReference);

        verify(m_CustomCommsService, times(1)).
            tryCreatePhysicalLink(PhysicalLinkTypeEnum.GPIO, PL_TEST_NAME, m_Properties);
    }

    /**
     * Test adding a LinkLayer that isn't created yet.
     */
    @Test
    public void testAddingLinkLayer() throws IllegalArgumentException, IllegalStateException, FactoryException,
            CCommException, IOException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.LINK_LAYER)
            .withProperties(m_StringMapList)
            .withProductType(LL_TEST_PID)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName(LL_TEST_NAME);
        
        CustomCommsTrackerCustomizer sut = new CustomCommsTrackerCustomizer(objectConfig, m_CustomCommsService, m_Log,
            m_EventAdmin);
        
        sut.addingService(m_ServiceReference);
        
        verify(m_CustomCommsService, times(1)).createLinkLayer(
                eq(LL_TEST_PID), eq(LL_TEST_NAME), eq(m_Properties));
    }

    /**
     * Test adding a TransportLayer that isn't created yet.
     */
    @Test
    public void testAddingTransportLayer() throws IllegalArgumentException, IllegalStateException,
            FactoryException, CCommException, IOException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.TRANSPORT_LAYER)
            .withProperties(m_StringMapList)
            .withProductType(TL_TEST_PID)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName(TL_TEST_NAME);

        when(m_CustomCommsService.getLinkLayer(LL_TEST_NAME)).thenReturn(m_Link);

        CustomCommsTrackerCustomizer sut = new CustomCommsTrackerCustomizer(objectConfig, m_CustomCommsService, m_Log,
            m_EventAdmin);

        sut.addingService(m_ServiceReference);

        verify(m_CustomCommsService, times(1)).createTransportLayer(
                eq(TL_TEST_PID), eq(TL_TEST_NAME), eq(m_Properties));
    }

    /**
     * Verify error when adding a layer causes a {@link CCommException}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testTransportLayerCCommException() throws CCommException, IllegalArgumentException, 
        IllegalStateException, FactoryException, IOException
    {
        FactoryObjectConfig objectConfig = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.TRANSPORT_LAYER)
            .withProperties(m_StringMapList)
            .withProductType(TL_TEST_PID)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName(TL_TEST_NAME);

        when(m_CustomCommsService.createTransportLayer(eq(TL_TEST_PID), 
                eq(TL_TEST_NAME), Mockito.any(Map.class))).thenThrow(new CCommException(FormatProblem.OTHER));
        when(m_CustomCommsService.getLinkLayer(LL_TEST_NAME)).thenReturn(m_Link);

        CustomCommsTrackerCustomizer sut = new CustomCommsTrackerCustomizer(objectConfig, m_CustomCommsService, m_Log,
            m_EventAdmin);

        sut.addingService(m_ServiceReference);

        verify(m_CustomCommsService, times(1)).createTransportLayer(
                eq(TL_TEST_PID), eq(TL_TEST_NAME), eq(m_Properties));

        verify(m_Log).log(eq(LogService.LOG_WARNING), Mockito.any(FactoryException.class), Mockito.anyString(), 
                Mockito.any());
    }
}
