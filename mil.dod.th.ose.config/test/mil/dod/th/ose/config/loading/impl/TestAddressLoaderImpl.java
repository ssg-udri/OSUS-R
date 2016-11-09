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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.model.config.AddressConfig;
import mil.dod.th.model.config.CreatePolicyEnum;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.test.EventAdminMocker;
import mil.dod.th.ose.test.EventAdminVerifier;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.test.EventAdminMocker.EventHandlerRegistrationAnswer;

/**
 * Test class for the {@link AddressLoaderImpl}.
 * @author allenchl
 *
 */
public class TestAddressLoaderImpl
{
    private AddressManagerService m_AddrMan;
    private LoggingService m_Log;
    private BundleContext m_Context;
    private EventAdmin m_EventAdmin;
    private int m_OpenTrackerCount;
    private ServiceRegistration<?> m_EventRegistration;
    private Event m_AddressEvent;
    private boolean m_AllValidAddresses;
    
    private AddressLoaderImpl m_SUT;
    
    //addresses
    private String m_Addr1 = "testAddr:1";
    private String m_Addr2 = "testAddr:2";
    private String m_Addr3 = "testAddr:3";
    private String m_Addr4 = "invalidAddr";
    
    @Before
    public void setup() throws Exception
    {
        m_SUT = new AddressLoaderImpl();
        m_Context = mock(BundleContext.class);
        m_EventAdmin = mock(EventAdmin.class);
        when(m_Context.createFilter(anyString())).thenReturn(mock(Filter.class));

        EventHandlerRegistrationAnswer evtHandlerStub =
            EventAdminMocker.stubHandlerOfType(m_Context, EventHandler.class, m_EventAdmin);
        m_EventRegistration = evtHandlerStub.getRegistration();
        m_AddressEvent = new Event(ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_LOADED_EVENT,
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

        m_AddrMan = mock(AddressManagerService.class);
        m_Log = LoggingServiceMocker.createMock();
        
        //set deps
        m_SUT.setAddressManager(m_AddrMan);
        m_SUT.setLoggingService(m_Log);
        m_SUT.setEventAdmin(m_EventAdmin);

        m_SUT.activate(m_Context);
        
        // default to all valid addresses being processed
        m_AllValidAddresses = true;
    }
    
    @After
    public void tearDown() throws Exception
    {
        m_SUT.deactivate();

        if (m_AllValidAddresses)
        {
            // Verify that the factory objects loading complete event is always sent
            EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
                ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_OBJS_COMPLETE_EVENT);
        }

        // Verify that the event handler has been unregistered
        verify(m_EventRegistration).unregister();

        // Verify that all service trackers have been closed
        assertTrue("Service tracker(s) not closed", m_OpenTrackerCount == 0);
    }

    /**
     * Test that service trackers are created for all {@link mil.dod.th.model.config.AddressConfig} objects on a
     * first run.
     */
    @Test
    public void testProcessFirstRun() throws InvalidSyntaxException
    {
        List<AddressConfig> configs = createAddressConfigs(false);
        m_SUT.process(configs, true);
        
        // Post factory object loaded events
        m_EventAdmin.postEvent(m_AddressEvent);
        m_EventAdmin.postEvent(m_AddressEvent);
        m_EventAdmin.postEvent(m_AddressEvent);

        verify(m_Context, times(3)).addServiceListener(any(ServiceListener.class), anyString());
    }
    
    /**
     * Test that service trackers are created for all valid {@link mil.dod.th.model.config.AddressConfig} objects on a
     * first run, but not any invalid configs.
     */
    @Test
    public void testProcessFirstRunWithInvalidConfig() throws InvalidSyntaxException
    {
        // This test includes invalid address config
        m_AllValidAddresses = false;

        List<AddressConfig> configs = createAddressConfigs(true);
        m_SUT.process(configs, true);
        
        // Post factory object loaded events
        m_EventAdmin.postEvent(m_AddressEvent);
        m_EventAdmin.postEvent(m_AddressEvent);
        m_EventAdmin.postEvent(m_AddressEvent);

        verify(m_Context, times(3)).addServiceListener(any(ServiceListener.class), anyString());
    }
    
    /**
     * Test that service trackers are created only for {@link mil.dod.th.model.config.AddressConfig} objects with
     * {@link mil.dod.th.model.config.CreatePolicyEnum#IF_MISSING}.
     */
    @Test
    public void testProcessNotFirstRun() throws InvalidSyntaxException
    {
        List<AddressConfig> configs = createAddressConfigs(false);
        m_SUT.process(configs, false);
        
        // Post factory object loaded events
        m_EventAdmin.postEvent(m_AddressEvent);

        verify(m_Context, times(1)).addServiceListener(any(ServiceListener.class), anyString());
    }
    
    /**
     * Test that no service trackers are created if an empty {@link mil.dod.th.model.config.AddressConfig} list is
     * provided and first run is true.
     */
    @Test
    public void testProcessEmptyFirstRun() throws InvalidSyntaxException
    {
        List<AddressConfig> configs = new ArrayList<AddressConfig>();
        m_SUT.process(configs, true);

        verify(m_Context, never()).addServiceListener(any(ServiceListener.class), anyString());
    }
    
    /**
     * Test that no service trackers are created if an empty {@link mil.dod.th.model.config.AddressConfig} list is
     * provided and first run is false.
     */
    @Test
    public void testProcessEmptyNotFirstRun() throws InvalidSyntaxException
    {
        List<AddressConfig> configs = new ArrayList<AddressConfig>();
        m_SUT.process(configs, false);
        
        verify(m_Context, never()).addServiceListener(any(ServiceListener.class), anyString());
    }
    
    /**
     * Create address configs.
     */
    private List<AddressConfig> createAddressConfigs(boolean enableInvalidConfig)
    {
        final List<AddressConfig> configs = new ArrayList<AddressConfig>();
        configs.add(new AddressConfig(CreatePolicyEnum.FIRST_RUN, m_Addr1));
        configs.add(new AddressConfig(CreatePolicyEnum.IF_MISSING, m_Addr2));
        configs.add(new AddressConfig(CreatePolicyEnum.FIRST_RUN, m_Addr3));

        if (enableInvalidConfig)
        {
            configs.add(new AddressConfig(CreatePolicyEnum.FIRST_RUN, m_Addr4));
        }

        return configs;
    }
}
