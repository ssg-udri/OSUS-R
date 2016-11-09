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

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressFactory;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.AddressProxy;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.model.config.AddressConfig;
import mil.dod.th.model.config.CreatePolicyEnum;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
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

public class TestAddressTrackerCustomizer
{
    private static final String TEST_PID = AddressProxy.class.getName();
    private static final String TEST_DESC = "test:desc";

    private AddressManagerService m_AddressManagerService;
    private ServiceReference<FactoryDescriptor> m_ServiceReference;
    private LoggingService m_Log;
    private EventAdmin m_EventAdmin;
    private Address m_Address;

    @Before
    public void setUp() throws Exception
    {
        m_AddressManagerService = mock(AddressManagerService.class);
        m_Log = LoggingServiceMocker.createMock();
        m_EventAdmin = mock(EventAdmin.class);
        m_Address = FactoryObjectMocker.mockFactoryObject(Address.class, TEST_PID);
        m_ServiceReference = mock(FactoryServiceReference.class);

        Bundle bundle = mock(Bundle.class);
        when(m_ServiceReference.getBundle()).thenReturn(bundle);
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundle.getBundleContext()).thenReturn(bundleContext);

        AddressFactory factory = m_Address.getFactory();
        when(bundleContext.getService(m_ServiceReference)).thenReturn(factory);
    }

    @After
    public void tearDown()
    {
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_LOADED_EVENT);
    }

    @Test
    public void testAddingService() throws IllegalArgumentException, IllegalStateException, FactoryException
    {
        AddressConfig config = new AddressConfig()
            .withAddressDescription(TEST_DESC)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN);

        when(m_AddressManagerService.getOrCreateAddress(TEST_DESC)).thenReturn(m_Address);

        AddressTrackerCustomizer sut =
                new AddressTrackerCustomizer(config, m_AddressManagerService, m_Log, m_EventAdmin);

        sut.addingService(m_ServiceReference);

        verify(m_AddressManagerService).getOrCreateAddress(TEST_DESC);
    }
    
    /**
     *  Verify error handling if an address cannot be created.
     */
    @Test
    public void testAddingServiceException() throws IllegalArgumentException, IllegalStateException, FactoryException
    {
        AddressConfig config = new AddressConfig()
            .withAddressDescription(TEST_DESC)
            .withCreatePolicy(CreatePolicyEnum.FIRST_RUN);
        
        when(m_AddressManagerService.getOrCreateAddress(TEST_DESC)).thenThrow(
                new CCommException("problem", FormatProblem.ADDRESS_MISMATCH));

        AddressTrackerCustomizer sut =
                new AddressTrackerCustomizer(config, m_AddressManagerService, m_Log, m_EventAdmin);
        
        sut.addingService(m_ServiceReference);

        verify(m_AddressManagerService).getOrCreateAddress(TEST_DESC);
        verify(m_Log).log(eq(LogService.LOG_WARNING), Mockito.any(FactoryException.class), Mockito.anyString(), 
                Mockito.any());
    }
}
