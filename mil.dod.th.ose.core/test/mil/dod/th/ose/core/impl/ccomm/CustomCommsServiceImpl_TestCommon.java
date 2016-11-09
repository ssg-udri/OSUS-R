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
package mil.dod.th.ose.core.impl.ccomm;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;

import com.google.common.collect.ImmutableMap;

import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.core.FactoryObjectDataManagerMocker;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.impl.ccomm.link.LinkLayerInternal;
import mil.dod.th.ose.core.impl.ccomm.link.LinkLayerServiceProxy;
import mil.dod.th.ose.core.impl.ccomm.link.data.LinkLayerFactoryObjectDataManager;
import mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal;
import mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkServiceProxy;
import mil.dod.th.ose.core.impl.ccomm.physical.data.PhysicalLinkFactoryObjectDataManager;
import mil.dod.th.ose.core.impl.ccomm.transport.TransportLayerInternal;
import mil.dod.th.ose.core.impl.ccomm.transport.TransportLayerServiceProxy;
import mil.dod.th.ose.core.impl.ccomm.transport.data.TransportLayerFactoryObjectDataManager;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;
import mil.dod.th.ose.test.LoggingServiceMocker;

/**
 * Common base class for all test classes for {@link CustomCommsServiceImpl}.
 * 
 * @author dhumeniuk
 *
 */
public abstract class CustomCommsServiceImpl_TestCommon
{
    protected static final String PL_PRODUCT_TYPE = "pl-product-type";
    protected static final String LL_PRODUCT_TYPE = "ll-product-type";
    protected static final String TL_PRODUCT_TYPE = "tl-product-type";
    
    protected CustomCommsServiceImpl m_SUT;
    protected BundleContext m_Context;
    protected PhysicalLinkFactoryObjectDataManager m_PhysicalLinkFactoryObjectDataManager;
    protected LinkLayerFactoryObjectDataManager m_LinkLayerFactoryObjectDataManager;
    protected TransportLayerFactoryObjectDataManager m_TransportLayerFactoryObjectDataManager;
    protected FactoryRegistry<PhysicalLinkInternal> m_PhysRegistry;
    protected FactoryRegistry<LinkLayerInternal> m_LinkRegistry;
    protected FactoryRegistry<TransportLayerInternal> m_TransRegistry;
    protected FactoryServiceProxy<PhysicalLinkInternal> m_PhysicalLinkProxy;
    protected FactoryServiceProxy<LinkLayerInternal> m_LinkLayerProxy;
    protected FactoryServiceProxy<TransportLayerInternal> m_TransportLayerProxy;
    
    @Mock protected ComponentFactory serviceContextFactory;
    protected ComponentInstance m_PhysicalLinkComp;
    protected ComponentInstance m_LinkLayerComp;
    protected ComponentInstance m_TransportLayerComp;
    
    @SuppressWarnings("rawtypes")
    protected FactoryServiceContext m_TransportLayerServiceContext;
    @SuppressWarnings("rawtypes")
    protected FactoryServiceContext m_LinkLayerServiceContext;
    @SuppressWarnings("rawtypes")
    protected FactoryServiceContext m_PhysicalLinkServiceContext;
    
    protected FactoryInternal m_PhysFactory;
    protected PhysicalLinkCapabilities m_PhyCaps;
    protected PhysicalLinkInternal m_PhysInternal; 
    protected UUID m_PhysUuidUno;
    
    protected FactoryInternal m_LinkFactory;
    protected LinkLayerInternal m_LinkInternal;
    protected UUID m_LinkUuidUno;
    
    protected FactoryInternal m_TransFactory;
    protected TransportLayerInternal m_TransInternal;
    protected UUID m_TransUuidUno;
    
    protected LinkLayerCapabilities m_LinkLayerCapabilities = new LinkLayerCapabilities();

    /**
     * Mock all needed objects and stub them out.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void stubServices() throws Exception
    {
        m_SUT = new CustomCommsServiceImpl();
        MockitoAnnotations.initMocks(this);
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());

        m_Context = mock(BundleContext.class);
        m_PhysicalLinkFactoryObjectDataManager = mock(PhysicalLinkFactoryObjectDataManager.class);
        m_LinkLayerFactoryObjectDataManager = mock(LinkLayerFactoryObjectDataManager.class);
        m_TransportLayerFactoryObjectDataManager = mock(TransportLayerFactoryObjectDataManager.class);
        
        FactoryObjectDataManagerMocker.createMockFactoryObjectDataManager(m_PhysicalLinkFactoryObjectDataManager);
        FactoryObjectDataManagerMocker.createMockFactoryObjectDataManager(m_LinkLayerFactoryObjectDataManager);
        FactoryObjectDataManagerMocker.createMockFactoryObjectDataManager(m_TransportLayerFactoryObjectDataManager);
        
        m_PhysicalLinkProxy = mock(PhysicalLinkServiceProxy.class);
        m_LinkLayerProxy = mock(LinkLayerServiceProxy.class);
        m_TransportLayerProxy = mock(TransportLayerServiceProxy.class);
        
        m_SUT.setPhysicalLinkFactoryServiceProxy(m_PhysicalLinkProxy);
        m_SUT.setLinkLayerFactoryServiceProxy(m_LinkLayerProxy);
        m_SUT.setTransportLayerFactoryServiceProxy(m_TransportLayerProxy);
        m_SUT.setFactoryServiceContextFactory(serviceContextFactory);
        
        List<ComponentInfo<FactoryServiceContext>> serviceContextComps = 
                ComponentFactoryMocker.mockComponents(FactoryServiceContext.class, serviceContextFactory, 3);
        m_PhysicalLinkServiceContext = serviceContextComps.get(0).getObject();
        m_LinkLayerServiceContext = serviceContextComps.get(1).getObject();
        m_TransportLayerServiceContext = serviceContextComps.get(2).getObject();
        
        m_PhysicalLinkComp = serviceContextComps.get(0).getInstance();
        m_LinkLayerComp = serviceContextComps.get(1).getInstance();
        m_TransportLayerComp = serviceContextComps.get(2).getInstance();
        
        //they are reg'ed in order: phys, link, trans
        m_PhysRegistry = mock(FactoryRegistry.class);
        m_LinkRegistry = mock(FactoryRegistry.class);
        m_TransRegistry = mock(FactoryRegistry.class);
        m_PhysFactory = mock(FactoryInternal.class);
        m_LinkFactory = mock(FactoryInternal.class);
        m_TransFactory = mock(FactoryInternal.class);
        m_PhysInternal = mock(PhysicalLinkInternal.class);
        m_LinkInternal = mock(LinkLayerInternal.class);
        m_TransInternal = mock(TransportLayerInternal.class);
        
        when(m_PhysicalLinkServiceContext.getRegistry()).thenReturn(m_PhysRegistry);
        when(m_LinkLayerServiceContext.getRegistry()).thenReturn(m_LinkRegistry);
        when(m_TransportLayerServiceContext.getRegistry()).thenReturn(m_TransRegistry);
        
        when(m_PhysicalLinkServiceContext.getFactories()).thenReturn(
                ImmutableMap.builder().put(PL_PRODUCT_TYPE, m_PhysFactory).build());
        when(m_LinkLayerServiceContext.getFactories()).thenReturn(
                ImmutableMap.builder().put(LL_PRODUCT_TYPE, m_LinkFactory).build());
        when(m_TransportLayerServiceContext.getFactories()).thenReturn(
                ImmutableMap.builder().put(TL_PRODUCT_TYPE, m_TransFactory).build());
        
        //uuids
        m_PhysUuidUno = UUID.randomUUID();
        m_LinkUuidUno = UUID.randomUUID();
        
        //factory mocking phys
        m_PhyCaps = new PhysicalLinkCapabilities().withLinkType(PhysicalLinkTypeEnum.SERIAL_PORT);
        when(m_PhysFactory.getProductName()).thenReturn("SerialPortProxy1");
        when(m_PhysFactory.getProductType()).thenReturn(PL_PRODUCT_TYPE);
        when(m_PhysFactory.getPhysicalLinkCapabilities()).thenReturn(m_PhyCaps);
        
        //factory mocking link
        when(m_LinkFactory.getProductName()).thenReturn("LinkyLink");
        when(m_LinkFactory.getProductType()).thenReturn(LL_PRODUCT_TYPE);
        when(m_LinkFactory.getLinkLayerCapabilities()).thenReturn(m_LinkLayerCapabilities );
        
        //factory mocking trans
        when(m_TransFactory.getProductName()).thenReturn("TrannyLayer");
        when(m_TransFactory.getProductType()).thenReturn(TL_PRODUCT_TYPE);
        
        //internal setup phys
        when(m_PhysInternal.getUuid()).thenReturn(m_PhysUuidUno);
        when(m_PhysRegistry.createNewObject(eq(m_PhysFactory), Mockito.anyString(), Mockito.any(Map.class))).
            thenReturn(m_PhysInternal);
        
        //internal setup linklayer
        when(m_LinkInternal.getUuid()).thenReturn(m_LinkUuidUno);
        when(m_LinkRegistry.createNewObject(eq(m_LinkFactory), Mockito.anyString(), Mockito.any(Map.class))).
            thenReturn(m_LinkInternal);
        when(m_LinkInternal.getFactory()).thenReturn(m_LinkFactory);
        
        //internal setup trans layer
        when(m_TransInternal.getUuid()).thenReturn(m_TransUuidUno);
        when(m_TransRegistry.createNewObject(eq(m_TransFactory), Mockito.anyString(), Mockito.any(Map.class))).
            thenReturn(m_TransInternal);
        when(m_TransInternal.getFactory()).thenReturn(m_TransFactory);
        
        m_SUT.activate(m_Context);
    }
}
