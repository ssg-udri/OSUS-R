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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.core.types.StringMapEntry;
import mil.dod.th.model.config.CreatePolicyEnum;
import mil.dod.th.model.config.FactoryObjectConfig;
import mil.dod.th.model.config.FactoryTypeEnum;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.test.EventAdminMocker;
import mil.dod.th.ose.test.EventAdminVerifier;
import mil.dod.th.ose.test.EventAdminMocker.EventHandlerRegistrationAnswer;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

public class TestFactoryObjectLoaderImpl
{
    private FactoryObjectLoaderImpl m_SUT;
    private BundleContext m_Context;
    private EventAdmin m_EventAdmin;
    private int m_OpenTrackerCount;
    private ServiceRegistration<?> m_EventRegistration;
    private Event m_FactoryObjectEvent;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new FactoryObjectLoaderImpl();
        m_Context = mock(BundleContext.class);
        m_EventAdmin = mock(EventAdmin.class);
        when(m_Context.createFilter(anyString())).thenReturn(mock(Filter.class));

        EventHandlerRegistrationAnswer evtHandlerStub =
            EventAdminMocker.stubHandlerOfType(m_Context, EventHandler.class, m_EventAdmin);
        m_EventRegistration = evtHandlerStub.getRegistration();
        m_FactoryObjectEvent = new Event(ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJ_LOADED_EVENT,
            new HashMap<String, Object>());

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                m_OpenTrackerCount++;
                return null;
            }
        }).when(m_Context).addServiceListener(any(ServiceListener.class), anyString());

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                m_OpenTrackerCount--;
                return null;
            }
        }).when(m_Context).removeServiceListener(any(ServiceListener.class));

        m_OpenTrackerCount = 0;

        AssetDirectoryService assetDir = mock(AssetDirectoryService.class);
        CustomCommsService customComms = mock(CustomCommsService.class);
        DataStreamService dataStream = mock(DataStreamService.class);

        m_SUT.setAssetDirectoryService(assetDir);
        m_SUT.setCustomCommsService(customComms);
        m_SUT.setDataStreamService(dataStream);
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.setEventAdmin(m_EventAdmin);

        m_SUT.activate(m_Context);
    }

    @After
    public void tearDown() throws Exception
    {
        m_SUT.deactivate();

        // Verify that the factory objects loading complete event is always sent
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJS_COMPLETE_EVENT);

        // Verify that the event handler has been unregistered
        verify(m_EventRegistration).unregister();

        // Verify that all service trackers have been closed
        assertTrue("Service tracker(s) not closed", m_OpenTrackerCount == 0);
    }

    /**
     * Test that service trackers are created for all {@link mil.dod.th.model.config.FactoryObjectConfig} objects on a
     * first run.
     */
    @Test
    public void testProcessFirstRun() throws InvalidSyntaxException
    {
        // First run
        m_SUT.process(createConfigList(), true);

        // Post factory object loaded events
        m_EventAdmin.postEvent(m_FactoryObjectEvent);
        m_EventAdmin.postEvent(m_FactoryObjectEvent);
        m_EventAdmin.postEvent(m_FactoryObjectEvent);

        verify(m_Context, times(3)).addServiceListener(any(ServiceListener.class), anyString());
    }

    /**
     * Test that service trackers are created only for {@link mil.dod.th.model.config.FactoryObjectConfig} objects with
     * {@link mil.dod.th.model.config.CreatePolicyEnum#IF_MISSING}.
     */
    @Test
    public void testProcessNotFirstRun() throws InvalidSyntaxException
    {
        // Not a first run
        m_SUT.process(createConfigList(), false);

        // Post factory object loaded event
        m_EventAdmin.postEvent(m_FactoryObjectEvent);
        m_EventAdmin.postEvent(m_FactoryObjectEvent);

        verify(m_Context, times(2)).addServiceListener(any(ServiceListener.class), anyString());
    }

    /**
     * Test that service trackers are created for all {@link mil.dod.th.model.config.FactoryObjectConfig} objects when
     * the data streaming service is not yet available, but is set later after initial processing.
     */
    @Test
    public void testProcessPendingDataStreams() throws InvalidSyntaxException
    {
        // Data stream service is not available yet
        m_SUT.unsetDataStreamService(null);

        // First run
        m_SUT.process(createConfigList(), true);

        // Post factory object loaded events
        m_EventAdmin.postEvent(m_FactoryObjectEvent);
        m_EventAdmin.postEvent(m_FactoryObjectEvent);

        // Data stream service is now available
        m_SUT.setDataStreamService(mock(DataStreamService.class));

        m_EventAdmin.postEvent(m_FactoryObjectEvent);

        verify(m_Context, times(3)).addServiceListener(any(ServiceListener.class), anyString());
    }

    /**
     * Test that no service trackers are created if an empty {@link mil.dod.th.model.config.FactoryObjectConfig} list is
     * provided and first run is true.
     */
    @Test
    public void testProcessEmptyFirstRun() throws InvalidSyntaxException
    {
        List<FactoryObjectConfig> factoryObjectConfigs = new ArrayList<FactoryObjectConfig>();

        m_SUT.process(factoryObjectConfigs, true);

        verify(m_Context, never()).addServiceListener(any(ServiceListener.class), anyString());
    }
    
    /**
     * Test that no service trackers are created if an empty {@link mil.dod.th.model.config.FactoryObjectConfig} list is
     * provided and first run is false.
     */
    @Test
    public void testProcessEmptyNotFirstRun() throws InvalidSyntaxException
    {
        List<FactoryObjectConfig> factoryObjectConfigs = new ArrayList<FactoryObjectConfig>();

        m_SUT.process(factoryObjectConfigs, false);

        verify(m_Context, never()).addServiceListener(any(ServiceListener.class), anyString());
    }
    
    /**
    * Verify that all types of physical links add service trackers correctly
    */
    @Test
    public void testProcessWithPhysicalLinks() throws InvalidSyntaxException
    {
        List<FactoryObjectConfig> factoryObjectConfigs = new ArrayList<FactoryObjectConfig>();
        
        for (PhysicalLinkTypeEnum type : PhysicalLinkTypeEnum.values())
        {
            FactoryObjectConfig physLink = new FactoryObjectConfig()
                .withFactoryType(FactoryTypeEnum.PHYSICAL_LINK)
                .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
                .withPhysicalLinkType(type)
                .withName( type.value() + "Link");
        
            factoryObjectConfigs.add(physLink);
        }
        
        // test with one that has null for physical link type
        FactoryObjectConfig physLink = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.PHYSICAL_LINK)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withPhysicalLinkType(null)
            .withName("Invalid-Link");
        factoryObjectConfigs.add(physLink);

        m_SUT.process(factoryObjectConfigs, true);

        // Post factory object loaded events
        m_EventAdmin.postEvent(m_FactoryObjectEvent);
        m_EventAdmin.postEvent(m_FactoryObjectEvent);
        m_EventAdmin.postEvent(m_FactoryObjectEvent);
        m_EventAdmin.postEvent(m_FactoryObjectEvent);
        m_EventAdmin.postEvent(m_FactoryObjectEvent);

        verify(m_Context, times(4)).addServiceListener(any(ServiceListener.class), anyString());
    }

    private List<FactoryObjectConfig> createConfigList()
    {
        List<StringMapEntry> properties = new ArrayList<StringMapEntry>();

        FactoryObjectConfig obj1 = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.LINK_LAYER)
            .withProperties(properties)
            .withProductType("com.test.product1")
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN)
            .withName("Product1");
        FactoryObjectConfig obj2 = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.ASSET)
            .withProperties(properties)
            .withProductType("com.test.product2")
            .withCreatePolicy(CreatePolicyEnum.IF_MISSING)
            .withName("Product2");
        FactoryObjectConfig obj3 = new FactoryObjectConfig()
            .withFactoryType(FactoryTypeEnum.STREAM_PROFILE)
            .withProperties(properties)
            .withProductType("com.test.product3")
            .withCreatePolicy(CreatePolicyEnum.IF_MISSING)
            .withName("Product3");

        List<FactoryObjectConfig> factoryObjectConfigs = new ArrayList<FactoryObjectConfig>();
        factoryObjectConfigs.add(obj1);
        factoryObjectConfigs.add(obj2);
        factoryObjectConfigs.add(obj3);

        return factoryObjectConfigs;
    }
}
