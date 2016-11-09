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
package mil.dod.th.ose.core.impl.ccomm.link;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.link.LinkLayerProxy;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.impl.ccomm.CustomCommsServiceImpl;
import mil.dod.th.ose.core.impl.ccomm.LinkLayerRegistryCallback;
import mil.dod.th.ose.core.impl.ccomm.link.data.LinkLayerFactoryObjectDataManager;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Test class for the {@link LinkLayerServiceProxy}.
 * @author allenchl
 *
 */
public class TestLinkLayerServiceProxy
{
    private static final String PRODUCT_TYPE = "product-type";
    
    private LinkLayerServiceProxy m_SUT;
    private ComponentFactory m_ComponentFactory;
    private ComponentInfo<LinkLayerInternal> m_ComponentInfo;

    @Mock private LinkLayerFactoryObjectDataManager linkLayerFactoryObjectDataManager;
    @Mock private CustomCommsServiceImpl customCommService;
    @Mock private FactoryServiceContext<LinkLayerInternal> factoryServiceContext;
        
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        m_ComponentFactory = mock(ComponentFactory.class);
        m_ComponentInfo = ComponentFactoryMocker.
                mockSingleComponent(LinkLayerInternal.class, m_ComponentFactory);
        
        m_SUT = new LinkLayerServiceProxy();
        
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.setLinkLayerFactory(m_ComponentFactory);
        m_SUT.setLinkLayerFactoryObjectDataManager(linkLayerFactoryObjectDataManager);
    }
    
    /**
     * Verify event handler is unregistered on deactivation.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testDeactivate()
    {
        ServiceRegistration sReg = mock(ServiceRegistration.class);
        BundleContext bContext = mock(BundleContext.class);
        
        when(bContext.registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class))).thenReturn(sReg);

        m_SUT.activate(bContext);
        
        m_SUT.deactivate();
        
        verify(sReg).unregister();
    }
    
    /**
     * Verify proxy is initialized correctly.
     */
    @Test
    public void testInitializeProxy() throws FactoryException
    {
        LinkLayerProxy mockProxy = mock(LinkLayerProxy.class);
        Map<String, Object> map = new HashMap<>();
        LinkLayerInternal internalObj = mock(LinkLayerInternal.class);
        
        m_SUT.initializeProxy(internalObj, mockProxy, map);
        
        verify(mockProxy).initialize(internalObj, map);
    }
    
    /**
     * Verify that an internal object can be created.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateComponentInstance()
    {
        ComponentInstance instance = m_SUT.createFactoryObjectInternal(
                Mockito.any(FactoryInternal.class));
        
        verify(m_ComponentFactory).newInstance(Mockito.any(Dictionary.class));
        
        assertThat(instance, is(m_ComponentInfo.getInstance()));
    }
    
    /**
     * Verify there are no additional service attr defs.
     */
    @Test
    public void testGetExtendedServiceAttributeDefinitions()
    {
        FactoryInternal factory = mock(FactoryInternal.class);
        assertThat(m_SUT.getExtendedServiceAttributeDefinitions(factoryServiceContext, factory, 
                ObjectClassDefinition.ALL).length, is(0));
    }
    
    /**
     * Verify property capability type is returned.
     */
    @Test
    public void testGetCapType()
    {
        assertThat(m_SUT.getCapabilityType().getName(), is(LinkLayerCapabilities.class.getName()));
    }
    
    /**
     * Verify proper base type is returned.
     */
    @Test
    public void testGetBaseType()
    {
        assertThat(m_SUT.getBaseType().getName(), is(LinkLayer.class.getName()));
    }
    
    /**
     * Verify dictionary with factory type is returned for additional registration properties.
     */
    @Test
    public void testGetAdditionalServiceRegProps()
    {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, LinkLayerFactory.class);
        assertThat(m_SUT.getAdditionalFactoryRegistrationProps(null).equals(props), is(true));
    }
    
    /**
     * Verify that set factory object data manager is returned.
     */
    @Test
    public void testGetDataManager()
    {
        assertThat((LinkLayerFactoryObjectDataManager)m_SUT.getDataManager(), is(linkLayerFactoryObjectDataManager));
    }
    
    /**
     * Verify that callback is created.
     */
    @Test
    public void testCreateCallback()
    {
        assertThat(m_SUT.createCallback(factoryServiceContext).getClass().getName(), 
                is(LinkLayerRegistryCallback.class.getName()));
    }
    
    /**
     * Verify event reg service is unregistered.
     * Verify link layers that are active are deactivated.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testOnRemoveFactory()
    {
        //mock behavior
        BundleContext bContext = mock(BundleContext.class);
        FactoryRegistry reg = mock(FactoryRegistry.class);
        FactoryServiceContext context = mock(FactoryServiceContext.class);
        when(context.getRegistry()).thenReturn(reg);
        
        FactoryInternal factory = mock(FactoryInternal.class);
        when(factory.getProductType()).thenReturn(PRODUCT_TYPE);

        Set<LinkLayer> links = new HashSet<>();
        LinkLayer link1 = mock(LinkLayerInternal.class);
        when(link1.isActivated()).thenReturn(true);
        LinkLayer link2 = mock(LinkLayerInternal.class);
        when(link2.isActivated()).thenReturn(false);
        links.add(link1);
        links.add(link2);
        when(reg.getObjectsByProductType(PRODUCT_TYPE)).thenReturn(links);
        
        m_SUT.activate(bContext);
        
        ArgumentCaptor<EventHandler> handlerCap = ArgumentCaptor.forClass(EventHandler.class);
        verify(bContext).registerService(eq(EventHandler.class), handlerCap.capture(), 
                Mockito.any(Dictionary.class));
        final EventHandler handler = handlerCap.getValue();
       
        //will make the unit test run faster
        Thread handlerThread = new Thread(new Runnable()
        {            
            @Override
            public void run()
            {
                handler.handleEvent(new Event(LinkLayer.TOPIC_DEACTIVATED, new HashMap<String, Object>()));
                handler.handleEvent(new Event(LinkLayer.TOPIC_DEACTIVATED, new HashMap<String, Object>()));
            }
        });

        handlerThread.start();
        m_SUT.onRemoveFactory(context, factory);
        
        //verify deactivated if isActivated returned true
        verify(link1).deactivateLayer();
        verify(link2, never()).deactivateLayer();
    }
}
