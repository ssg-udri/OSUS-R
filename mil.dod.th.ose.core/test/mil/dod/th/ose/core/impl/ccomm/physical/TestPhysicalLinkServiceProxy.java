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
package mil.dod.th.ose.core.impl.ccomm.physical;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.ccomm.physical.Gpio;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.ccomm.physical.PhysicalLinkProxy;
import mil.dod.th.core.ccomm.physical.SerialPort;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.core.MetaTypeMocker;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.impl.ccomm.PhysicalLinkRegistryCallback;
import mil.dod.th.ose.core.impl.ccomm.physical.data.PhysicalLinkFactoryObjectDataManager;
import mil.dod.th.ose.test.AttributeDefinitionMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
  * Test class for the {@link PhysicalLinkServiceProxy}.
 * @author allenchl
 *
 */
public class TestPhysicalLinkServiceProxy
{
    private static final String PRODUCT_TYPE = "product-type";
    private PhysicalLinkServiceProxy m_SUT;
    private ComponentFactory m_ComponentFactory;
    private ComponentInfo<PhysicalLinkInternal> m_ComponentInfo;
    private MetaTypeService m_MetaService;
    
    @Mock private PhysicalLinkFactoryObjectDataManager physicalLinkFactoryObjectDataManager;
    @Mock private FactoryInternal factory;
    @Mock private FactoryServiceContext<PhysicalLinkInternal> factoryServiceContext;
    @Mock private FactoryRegistry<PhysicalLinkInternal> registry;
    @Mock private Bundle apiBundle;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        m_ComponentFactory = mock(ComponentFactory.class);
        m_ComponentInfo = ComponentFactoryMocker.
                mockSingleComponent(PhysicalLinkInternal.class, m_ComponentFactory);
        m_MetaService = MetaTypeMocker.createMockMetaType();
        
        m_SUT = new PhysicalLinkServiceProxy();
        
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.setPhysicalLinkFactory(m_ComponentFactory);
        m_SUT.setSerialPortFactory(m_ComponentFactory);
        m_SUT.setMetaTypeService(m_MetaService);
        m_SUT.setPhysicalLinkFactoryObjectDataManager(physicalLinkFactoryObjectDataManager);
        
        when(factoryServiceContext.getRegistry()).thenReturn(registry);
        when(factoryServiceContext.getApiBundle()).thenReturn(apiBundle);
        
        when(factory.getProductType()).thenReturn(PRODUCT_TYPE);
    }
    
    /**
     * Verify proxy is initialized correctly.
     */
    @Test
    public void testInitializeProxy() throws FactoryException
    {
        PhysicalLinkProxy mockProxy = mock(PhysicalLinkProxy.class);
        Map<String, Object> map = new HashMap<>();
        PhysicalLinkInternal internalObj = mock(PhysicalLinkInternal.class);
        
        m_SUT.initializeProxy(internalObj, mockProxy, map);
        
        verify(mockProxy).initialize(internalObj, map);
    }
    
    /**
     * Verify that an internal serial port type object can be created.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateComponentInstanceSerialPort()
    {
        //mock factory descriptor
        FactoryInternal desc = mock(FactoryInternal.class);
        PhysicalLinkCapabilities caps = mock(PhysicalLinkCapabilities.class);
        when(desc.getCapabilities()).thenReturn(caps);
        when(caps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.SERIAL_PORT);
        
        ComponentInstance instance = m_SUT.createFactoryObjectInternal(desc);
        
        verify(m_ComponentFactory).newInstance(Mockito.any(Dictionary.class));
        
        assertThat(instance, is(m_ComponentInfo.getInstance()));
    }
    
    /**
     * Verify that an internal object can be created.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateComponentInstanceRegPhys()
    {
        //mock factory descriptor
        FactoryInternal desc = mock(FactoryInternal.class);
        PhysicalLinkCapabilities caps = mock(PhysicalLinkCapabilities.class);
        when(desc.getCapabilities()).thenReturn(caps);
        when(caps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.GPIO);
        
        ComponentInstance instance = m_SUT.createFactoryObjectInternal(desc);
        
        verify(m_ComponentFactory).newInstance(Mockito.any(Dictionary.class));
        
        assertThat(instance, is(m_ComponentInfo.getInstance()));
    }
    
    /**
     * Verify custom attrs retrieved from metatype service based upon phys link type.
     */
    @Test
    public void testGetSerialPortServiceAttributeDefinitions()
    {
        FactoryInternal fact = mock(FactoryInternal.class);
        PhysicalLinkCapabilities physCaps = mock(PhysicalLinkCapabilities.class);
        
        when(fact.getCapabilities()).thenReturn(physCaps);
        when(physCaps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.SERIAL_PORT);
        
        String pid = SerialPort.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX;
        
        AttributeDefinition[] expectedDefs = AttributeDefinitionMocker.mockArrayAll();
        MetaTypeMocker.registerMetaTypeAttributes(apiBundle, pid, null, null, expectedDefs);
        
        AttributeDefinition[] retrieved = m_SUT.getExtendedServiceAttributeDefinitions(factoryServiceContext, fact,
                ObjectClassDefinition.ALL);
                
        assertThat(retrieved, is(expectedDefs));
    }
    
    /**
     * Verify custom attrs retrieved from metatype service based upon phys link type.
     */
    @Test
    public void testGetGpioServiceAttributeDefinitions()
    {
        FactoryInternal fact = mock(FactoryInternal.class);
        PhysicalLinkCapabilities physCaps = mock(PhysicalLinkCapabilities.class);
        
        when(fact.getCapabilities()).thenReturn(physCaps);
        when(physCaps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.GPIO);
        
        String pid = Gpio.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX;
        
        AttributeDefinition[] expectedDefs = AttributeDefinitionMocker.mockArrayAll();
        MetaTypeMocker.registerMetaTypeAttributes(apiBundle, pid, null, null, expectedDefs);
        
        AttributeDefinition[] retrieved = m_SUT.getExtendedServiceAttributeDefinitions(factoryServiceContext, fact,
                ObjectClassDefinition.ALL);
        
        assertThat(retrieved, is(expectedDefs));
    }
    
    /**
     * Verify dictionary with factory type is returned for additional registration properties.
     */
    @Test
    public void testGetAdditionalServiceRegProps()
    {
        //mock factory descriptor
        FactoryInternal desc = mock(FactoryInternal.class);
        PhysicalLinkCapabilities caps = mock(PhysicalLinkCapabilities.class);
        when(desc.getPhysicalLinkCapabilities()).thenReturn(caps);
        when(caps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.SERIAL_PORT);

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, PhysicalLinkFactory.class);
        props.put(PhysicalLink.LINK_TYPE_SERVICE_PROPERTY, PhysicalLinkTypeEnum.SERIAL_PORT.value());
        assertThat(m_SUT.getAdditionalFactoryRegistrationProps(desc).equals(props), is(true));
    }
    
    /**
     * Verify no extra attributes if not GPIO or serial port.
     */
    @Test
    public void testGetExtendedServiceAttributeDefinitionsEmpty()
    {
        FactoryInternal fact = mock(FactoryInternal.class);
        PhysicalLinkCapabilities physCaps = mock(PhysicalLinkCapabilities.class);
        
        when(fact.getCapabilities()).thenReturn(physCaps);
        when(physCaps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.SPI);
        
        AttributeDefinition[] retrieved = m_SUT.getExtendedServiceAttributeDefinitions(factoryServiceContext, fact,
                ObjectClassDefinition.ALL);
        
        assertThat(retrieved.length, is(0));
    }
    
    /**
     * Verify empty attributes returned if null attributes defs returned by OCD.
     */
    @Test
    public void testNullDefsFromOcd()
    {
        FactoryInternal fact = mock(FactoryInternal.class);
        PhysicalLinkCapabilities physCaps = mock(PhysicalLinkCapabilities.class);
        
        when(fact.getCapabilities()).thenReturn(physCaps);
        when(physCaps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.GPIO);
        
        String pid = Gpio.class.getName() + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX;
        
        MetaTypeMocker.registerMetaTypeAttributes(apiBundle, pid, null, null, null); // return null for ADs
        
        AttributeDefinition[] retrieved = m_SUT.getExtendedServiceAttributeDefinitions(factoryServiceContext, fact,
                ObjectClassDefinition.ALL);
        
        assertThat(retrieved.length, is(0));
    }
    
    /**
     * Verify property capability type is returned.
     */
    @Test
    public void testGetCapType()
    {
        assertThat(m_SUT.getCapabilityType().getName(), is(PhysicalLinkCapabilities.class.getName()));
    }
    
    /**
     * Verify proper base type is returned.
     */
    @Test
    public void testGetBaseType()
    {
        assertThat(m_SUT.getBaseType().getName(), is(PhysicalLink.class.getName()));
    }
    
    /**
     * Verify that set factory object data manager is returned.
     */
    @Test
    public void testGetDataManager()
    {
        assertThat((PhysicalLinkFactoryObjectDataManager)m_SUT.getDataManager(), 
                is(physicalLinkFactoryObjectDataManager));
    }
    
    /**
     * Verify that callback is created.
     */
    @Test
    public void testCreateCallback()
    {
        assertThat(m_SUT.createCallback(factoryServiceContext).getClass().getName(), 
                is(PhysicalLinkRegistryCallback.class.getName()));
    }
    
    /**
     * Verify physical links are closed, if open.
     */
    @Test
    public void testOnRemoveFactory() throws PhysicalLinkException
    {
        PhysicalLinkInternal phys1 = mock(PhysicalLinkInternal.class);
        when(phys1.isOpen()).thenReturn(true);
        PhysicalLinkInternal phys2 = mock(PhysicalLinkInternal.class);
        when(phys2.isOpen()).thenReturn(false);
        
        Set<PhysicalLinkInternal> links = new HashSet<>();
        links.add(phys1);
        links.add(phys2);
        when(registry.getObjectsByProductType(PRODUCT_TYPE)).thenReturn(links);
        
        m_SUT.onRemoveFactory(factoryServiceContext, factory);
        
        verify(phys1).close();
        verify(phys2, never()).close();
    }
    
    /**
     * Verify if an exception thrown while closing a physical link that other links are still closed.
     */
    @Test
    public void testOnRemoveFactory_Exception() throws PhysicalLinkException
    {
        PhysicalLinkInternal phys1 = mock(PhysicalLinkInternal.class);
        when(phys1.isOpen()).thenReturn(true);
        PhysicalLinkInternal phys2 = mock(PhysicalLinkInternal.class);
        when(phys2.isOpen()).thenReturn(true);
        doThrow(new PhysicalLinkException("exception")).when(phys2).close();
        
        Set<PhysicalLinkInternal> links = new HashSet<>();
        links.add(phys1);
        links.add(phys2);
        when(registry.getObjectsByProductType(PRODUCT_TYPE)).thenReturn(links);
        
        m_SUT.onRemoveFactory(factoryServiceContext, factory);
        
        verify(phys1).close();
        verify(phys2).close();
    }
}
