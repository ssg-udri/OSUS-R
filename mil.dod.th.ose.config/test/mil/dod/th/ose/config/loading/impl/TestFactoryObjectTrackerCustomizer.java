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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.types.StringMapEntry;
import mil.dod.th.model.config.AddressConfig;
import mil.dod.th.model.config.FactoryObjectConfig;
import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;
import mil.dod.th.ose.test.AttributeDefinitionMocker;
import mil.dod.th.ose.test.EventAdminVerifier;
import mil.dod.th.ose.test.LoggingServiceMocker;

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

public class TestFactoryObjectTrackerCustomizer
{
    private FactoryObjectTrackerCustomizer m_AddrSUT;
    private FactoryObjectTrackerCustomizer m_FactSUT;
    private FactoryObject m_Object;
    private AddressConfig m_AddressConfig;
    private FactoryObjectConfig m_ObjectConfig;
    private LoggingService m_Logging;
    private EventAdmin m_EventAdmin;

    @Before
    public void setUp() throws Exception
    {
        m_Object = mock(FactoryObject.class);
        when(m_Object.getPid()).thenReturn(m_Object.getClass().getName());
        m_AddressConfig = new AddressConfig();
        m_ObjectConfig = new FactoryObjectConfig();
        FactoryDescriptor factory = mock(FactoryDescriptor.class);
        AttributeDefinition[] attrDefs = AttributeDefinitionMocker.mockArrayAll();
        when(factory.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(attrDefs);
        m_Logging = LoggingServiceMocker.createMock();
        m_EventAdmin = mock(EventAdmin.class);
        m_AddrSUT = new FactoryObjectTrackerCustomizerStub(m_AddressConfig);
        m_FactSUT = new FactoryObjectTrackerCustomizerStub(m_ObjectConfig);
        
        when(m_Object.getFactory()).thenReturn(factory);
    }
    
    @Test
    public void testAddingServiceAddressException()
    {
        FactoryDescriptor factory = mock(FactoryDescriptor.class);
        Bundle bundle = mock(Bundle.class);
        BundleContext context = mock(BundleContext.class);
        @SuppressWarnings("unchecked")
        ServiceReference<FactoryDescriptor> reference = mock(ServiceReference.class);
        when(reference.getBundle()).thenReturn(bundle);
        when(bundle.getBundleContext()).thenReturn(context);
        when(context.getService(reference)).thenReturn(factory);
        
        m_AddrSUT.addingService(reference);
        
        verify(m_Logging).log(eq(LogService.LOG_WARNING), Mockito.any(RuntimeException.class), anyString(), 
            anyVararg());

        // Verify that factory object config event is sent when exception is thrown
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_LOADED_EVENT);
    }

    @Test
    public void testAddingServiceFactoryException()
    {
        FactoryDescriptor factory = mock(FactoryDescriptor.class);
        Bundle bundle = mock(Bundle.class);
        BundleContext context = mock(BundleContext.class);
        @SuppressWarnings("unchecked")
        ServiceReference<FactoryDescriptor> reference = mock(ServiceReference.class);
        when(reference.getBundle()).thenReturn(bundle);
        when(bundle.getBundleContext()).thenReturn(context);
        when(context.getService(reference)).thenReturn(factory);
        
        m_FactSUT.addingService(reference);
        
        verify(m_Logging).log(eq(LogService.LOG_WARNING), Mockito.any(RuntimeException.class), anyString(), 
            anyVararg());

        // Verify that factory object config event is sent when exception is thrown
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin,
            ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJ_LOADED_EVENT);
    }

    @Test
    public void testModifiedService()
    {
        // No implementation, verify that no exceptions are thrown
        m_AddrSUT.modifiedService(null, null);
        m_FactSUT.modifiedService(null, null);
    }

    @Test
    public void testRemovedService()
    {
        // No implementation, verify that no exceptions are thrown
        m_AddrSUT.removedService(null, null);
        m_FactSUT.removedService(null, null);
    }

    /**
     * Verify {@link StringMapEntry} can be translated into a map.
     */
    @Test
    public void testTranslateStringMap()
    {
        List<StringMapEntry> props = new ArrayList<StringMapEntry>();
        props.add(new StringMapEntry("id1", "true"));
        props.add(new StringMapEntry("id2", "5"));

        Map<String, Object> expectedValues = new HashMap<String, Object>();
        expectedValues.put("id1", true);
        expectedValues.put("id2", 5);
        
        assertEquals(m_FactSUT.translateStringMap(props, m_Object.getFactory()), expectedValues);
    }
    
    /**
     * Verify {@link StringMapEntry} that does not have a matching id in the {@link 
     * org.osgi.service.metatype.AttributeDefinition} array causes an exception.
     */
    @Test
    public void testTranslateStringMapException()
    {
        List<StringMapEntry> props = new ArrayList<StringMapEntry>();
        props.add(new StringMapEntry("id1", "true"));
        props.add(new StringMapEntry("badId", "5"));

        try
        {
            m_FactSUT.translateStringMap(props, m_Object.getFactory());
            fail("IllegalArgumentException not thrown");
        }
        catch(IllegalArgumentException e)
        {
        }
    }
    
    /**
     * Test that the same object configuration (used during instantiation) is returned.
     */
    @Test
    public void testGetObjectConfig()
    {
        assertEquals(m_AddrSUT.getAddressConfig(), m_AddressConfig);
        assertEquals(m_FactSUT.getObjectConfig(), m_ObjectConfig);
    }
    
    private class FactoryObjectTrackerCustomizerStub extends FactoryObjectTrackerCustomizer
    {
        FactoryObjectTrackerCustomizerStub(AddressConfig addressConfig)
        {
            super(addressConfig, m_Logging, m_EventAdmin);
        }

        FactoryObjectTrackerCustomizerStub(FactoryObjectConfig objectConfig)
        {
            super(objectConfig, m_Logging, m_EventAdmin);
        }

        /**
         * Method must throw exception for {@link 
         * TestFactoryObjectTrackerCustomizer#testAddingServiceFactoryException()}.
         */
        @Override
        void addingFactoryDescriptor(FactoryDescriptor factory) throws FactoryException
        {
            throw new RuntimeException("blah");
        }
    }
}
