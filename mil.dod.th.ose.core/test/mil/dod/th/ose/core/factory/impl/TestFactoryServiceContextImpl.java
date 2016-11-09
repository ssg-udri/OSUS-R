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
package mil.dod.th.ose.core.factory.impl;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.ose.core.factory.api.DirectoryService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryRegistryCallback;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.FactoryServiceUtils;
import mil.dod.th.ose.metatype.MetaTypeProviderBundle;
import mil.dod.th.ose.test.AttributeDefinitionMocker;
import mil.dod.th.ose.test.BundleContextMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.test.ServiceListenerAdaptor;
import mil.dod.th.ose.utils.BundleService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

public class TestFactoryServiceContextImpl
{
    private static final String PRODUCT_TYPE = "product-type";
    
    private FactoryServiceContextImpl<FactoryObjectInternal> m_SUT;
    @Mock private FactoryServiceUtils m_FactoryServiceUtils;
    @Mock private FactoryServiceProxy<FactoryObjectInternal> m_FactoryServiceProxy;
    @Mock private BundleContext m_CoreBundleContext;
    @Mock private FactoryRegistry<FactoryObjectInternal> m_FactoryRegistry;
    @Mock private ComponentFactory m_FactoryRegistryFactory;
    @Mock private ComponentInstance m_FactoryRegistryInstance;
    @Mock private ComponentFactory m_FactoryInternalFactory;
    @Mock private MetaTypeService m_MetaTypeService;
    @Mock private MetaTypeProviderBundle m_MetaTypeProviderBundle;
    @Mock private ComponentFactory m_ProxyComponentFactory;
    @Mock private DirectoryService m_DirectoryService;
    @Mock private FactoryRegistryCallback<FactoryObjectInternal> m_Callback;
    
    @Mock private FactoryObjectInternal m_FactoryObject1;
    @Mock private FactoryObjectInternal m_FactoryObject2;
    @Mock private FactoryObjectProxy m_ObjectProxy1;
    @Mock private FactoryObjectProxy m_ObjectProxy2;
    
    private UUID m_ObjectUuid1;
    private UUID m_ObjectUuid2;
    
    private ServiceListenerAdaptor m_ServiceListenerAdpator;

    @Before
    public void setup() throws Exception
    {
        m_SUT = new FactoryServiceContextImpl<FactoryObjectInternal>();
        m_ObjectUuid1 = UUID.randomUUID();
        m_ObjectUuid2 = UUID.randomUUID();
        
        // mock
        MockitoAnnotations.initMocks(this);
        
        // stub
        m_ServiceListenerAdpator = BundleContextMocker.spyServiceListener(m_CoreBundleContext);
        BundleContextMocker.stubFilter(m_CoreBundleContext);
        
        doReturn(FactoryObject.class).when(m_FactoryServiceProxy).getBaseType();
        when(m_FactoryRegistryFactory.newInstance(null)).thenReturn(m_FactoryRegistryInstance);
        when(m_FactoryRegistryInstance.getInstance()).thenReturn(m_FactoryRegistry);
        
        when(m_FactoryObject1.getProxy()).thenReturn(m_ObjectProxy1);
        when(m_FactoryObject2.getProxy()).thenReturn(m_ObjectProxy2);
        when(m_FactoryObject1.getUuid()).thenReturn(m_ObjectUuid1);
        when(m_FactoryObject2.getUuid()).thenReturn(m_ObjectUuid2);
        
        Set<FactoryObjectInternal> objectSet = new HashSet<FactoryObjectInternal>();
        objectSet.add(m_FactoryObject1);
        objectSet.add(m_FactoryObject2);
        when(m_FactoryRegistry.getObjectsByProductType(PRODUCT_TYPE)).thenReturn(objectSet);
        
        when(m_FactoryServiceProxy.createCallback(m_SUT)).thenReturn(m_Callback);
        
        // bind
        m_SUT.setFactoryRegistry(m_FactoryRegistryFactory);
        m_SUT.setFactoryInternalFactory(m_FactoryInternalFactory);
        m_SUT.setMetaTypeService(m_MetaTypeService);
        m_SUT.setMetaTypeProviderBundle(m_MetaTypeProviderBundle);
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        
        m_SUT.initialize(m_CoreBundleContext, m_FactoryServiceProxy, m_DirectoryService);
    }
    
    /**
     * Verify activation will initialize factory registry
     */
    @Test
    public void testActivate()
    {
        verify(m_FactoryRegistry).initialize(m_DirectoryService, m_FactoryServiceProxy, m_Callback);
    }
    
    /**
     * Verify that factory registry factory is disposed of upon deactivation.
     */
    @Test
    public void testDeactivate()
    {
        m_SUT.deactivate();
        verify(m_FactoryRegistryInstance).dispose();
    }
    
    @Test
    public void testGetRegistry()
    {
        assertThat(m_SUT.getRegistry(), is(m_FactoryRegistry));
    }

    @Test
    public void testGetDirectoryService()
    {
        assertThat(m_SUT.getDirectoryService(), is(m_DirectoryService));
    }
    
    /**
     * Verify ability to get service.
     */
    @Test
    public void testGetMetaTypeProviderBundle()
    {
        assertThat(m_SUT.getMetaTypeProviderBundle(), is(m_MetaTypeProviderBundle));
    }
    
    /**
     * Verify ability to get service.
     */
    @Test
    public void testGetMetaTypeService()
    {
        assertThat(m_SUT.getMetaTypeService(), is(m_MetaTypeService));
    }
    
    /**
     * Verify type returned matches that of the proxy.
     */
    @Test
    public void testGetCapabilitiesType()
    {
        doReturn(BaseCapabilities.class).when(m_FactoryServiceProxy).getCapabilityType();
        
        assertThat(m_SUT.getCapabilityType().getName(), is(BaseCapabilities.class.getName()));
    }
    
    /**
     * Verify type returned matches that of the proxy.
     */
    @Test
    public void testGetBaseType()
    {
        doReturn(FactoryObject.class).when(m_FactoryServiceProxy).getBaseType();
        
        assertThat(m_SUT.getBaseType().getName(), is(FactoryObject.class.getName()));
    }
    
    /**
     * Verify value returned matches that of the proxy.
     */
    @Test
    public void testGetAdditionalFactoryRegProps()
    {
        FactoryInternal factory = mock(FactoryInternal.class);
        
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("key1", "value1");
        when(m_FactoryServiceProxy.getAdditionalFactoryRegistrationProps(factory))
            .thenReturn(props);
        
        assertThat(m_SUT.getAdditionalFactoryRegistrationProps(factory), is(props));
    }
    
    /**
     * Verify value returned matches that of the proxy.
     */
    @Test
    public void testGetExtendedServiceAttributeDefs()
    {
        FactoryInternal factory = mock(FactoryInternal.class);
        AttributeDefinition[] mockArray = AttributeDefinitionMocker.mockArrayAll();
        
        when(m_FactoryServiceProxy.getExtendedServiceAttributeDefinitions(m_SUT, factory, ObjectClassDefinition.ALL))
            .thenReturn(mockArray);
        
        assertThat(m_SUT.getExtendedServiceAttributeDefinitions(factory, ObjectClassDefinition.ALL), is(mockArray));
    }
    
    /**
     * Verify plug-in factory is registered properly.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testAddPluginFactory() throws InvalidSyntaxException
    {
        ComponentInfo<FactoryInternal> component = 
                ComponentFactoryMocker.mockSingleComponent(FactoryInternal.class, m_FactoryInternalFactory);
        FactoryInternal factory = component.getObject();
        
        ServiceReference proxyServiceRef = mock(ServiceReference.class);
        when(proxyServiceRef.getProperty(ComponentConstants.COMPONENT_NAME)).thenReturn(PRODUCT_TYPE);
        
        // replay
        m_ServiceListenerAdpator.addService(proxyServiceRef, m_ProxyComponentFactory);
        
        // verify the filter is based on the factory annotation (which is the class name of the base type)
        verify(m_CoreBundleContext)
            .createFilter("(" + ComponentConstants.COMPONENT_FACTORY + "=" + FactoryObject.class.getName() + ")");
        
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(FactoryInternal.KEY_COMPONENT_FACTORY, m_ProxyComponentFactory);
        props.put(FactoryInternal.KEY_SERVICE_CONTEXT, m_SUT);
        props.put(FactoryInternal.KEY_SERVICE_REFERENCE, proxyServiceRef);
        verify(m_FactoryInternalFactory).newInstance(props);
        
        verify(factory, timeout(100)).registerServices();
        
        verify(m_FactoryRegistry, timeout(100)).restoreAllObjects(factory);
        
        verify(factory, timeout(100)).makeAvailable();
        
        assertThat(m_SUT.getFactories(), hasEntry(PRODUCT_TYPE, factory));
    }

    /**
     * Verify if factory is already in map, it will not be registered again.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testAddPluginFactory_AlreadyRegistered() throws Exception
    {
        ComponentFactory otherFactoryInternalFactory = mock(ComponentFactory.class);
        ComponentFactory otherProxyComponentFactory = mock(ComponentFactory.class);
        
        // replay initial registration
        testAddPluginFactory();
        
        assertThat(m_SUT.getFactories().size(), is(1));
        
        // mock new component with the same product type
        ComponentInfo<FactoryInternal> component = 
                ComponentFactoryMocker.mockSingleComponent(FactoryInternal.class, otherFactoryInternalFactory);
        FactoryInternal factory = component.getObject();
        
        ServiceReference proxyServiceRef = mock(ServiceReference.class);
        when(proxyServiceRef.getProperty(ComponentConstants.COMPONENT_NAME)).thenReturn(PRODUCT_TYPE);
        when(factory.getProductType()).thenReturn(PRODUCT_TYPE);

        doThrow(new FactoryException("blah")).when(m_FactoryServiceProxy)
            .beforeAddFactory(Mockito.any(FactoryServiceContext.class), Mockito.any(FactoryInternal.class));
        
        // replay
        m_ServiceListenerAdpator.addService(proxyServiceRef, otherProxyComponentFactory);
        
        // should still be holding at 1
        assertThat(m_SUT.getFactories().size(), is(1));
        
        // should not attempt to create another factory instance since product type is duplicated
        verify(otherFactoryInternalFactory, never()).newInstance(Mockito.any(Dictionary.class));
    }
    
    /**
     * Verify if an exception is thrown before reaching thread, factory registration is cleaned up.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testAddPluginFactory_ExceptionBeforeThread() throws Exception
    {
        ComponentInfo<FactoryInternal> component = 
                ComponentFactoryMocker.mockSingleComponent(FactoryInternal.class, m_FactoryInternalFactory);
        FactoryInternal factory = component.getObject();
        
        ServiceReference proxyServiceRef = mock(ServiceReference.class);
        when(proxyServiceRef.getProperty(ComponentConstants.COMPONENT_NAME)).thenReturn(PRODUCT_TYPE);
        when(factory.getProductType()).thenReturn(PRODUCT_TYPE);

        doThrow(new FactoryException("blah")).when(m_FactoryServiceProxy)
            .beforeAddFactory(Mockito.any(FactoryServiceContext.class), Mockito.any(FactoryInternal.class));
        
        // replay
        m_ServiceListenerAdpator.addService(proxyServiceRef, m_ProxyComponentFactory);
        
        verify(component.getInstance()).dispose();
        verify(m_CoreBundleContext).ungetService(proxyServiceRef);
        
        assertThat(m_SUT.getFactories().isEmpty(), is(true));
    }
    
    /**
     * Verify if an exception in thread is thrown, factory registration is cleaned up.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testAddPluginFactory_ThreadException() throws InvalidSyntaxException
    {
        ComponentInfo<FactoryInternal> component = 
                ComponentFactoryMocker.mockSingleComponent(FactoryInternal.class, m_FactoryInternalFactory);
        FactoryInternal factory = component.getObject();
        
        ServiceReference proxyServiceRef = mock(ServiceReference.class);
        when(proxyServiceRef.getProperty(ComponentConstants.COMPONENT_NAME)).thenReturn(PRODUCT_TYPE);
        when(factory.getProductType()).thenReturn(PRODUCT_TYPE);

        doThrow(RuntimeException.class).when(factory).makeAvailable();
        
        // replay
        m_ServiceListenerAdpator.addService(proxyServiceRef, m_ProxyComponentFactory);
        
        verify(factory, timeout(100)).registerServices();
        verify(factory, timeout(100)).makeUnavailable();
        
        verify(factory, timeout(100)).cleanup();
        
        verify(component.getInstance(), timeout(100)).dispose();
        
        verify(factory, timeout(100)).dispose(m_ObjectProxy1);
        verify(factory, timeout(100)).dispose(m_ObjectProxy2);
        verify(m_FactoryRegistry, timeout(100)).removeObject(m_ObjectUuid1);
        verify(m_FactoryRegistry, timeout(100)).removeObject(m_ObjectUuid2);
        
        verify(m_CoreBundleContext, timeout(100)).ungetService(proxyServiceRef);
        
        assertThat(m_SUT.getFactories().isEmpty(), is(true));
    }
    
    /**
     * Verify plug-in factory is cleaned up properly.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRemovePluginFactory() throws InvalidSyntaxException
    {
        ComponentInfo<FactoryInternal> component = 
                ComponentFactoryMocker.mockSingleComponent(FactoryInternal.class, m_FactoryInternalFactory);
        FactoryInternal factory = component.getObject();
        
        ServiceReference proxyServiceRef = mock(ServiceReference.class);
        when(proxyServiceRef.getProperty(ComponentConstants.COMPONENT_NAME)).thenReturn(PRODUCT_TYPE);
        when(factory.getProductType()).thenReturn(PRODUCT_TYPE);
        
        // have first object throw exception to ensure objects later in list are still disposed
        doThrow(new IllegalStateException()).when(factory).dispose(m_ObjectProxy1);
        
        // replay
        m_ServiceListenerAdpator.addService(proxyServiceRef, m_ProxyComponentFactory);
        m_ServiceListenerAdpator.removeService(m_ProxyComponentFactory);
        
        verify(factory).makeUnavailable();
        
        verify(factory).cleanup();
        
        verify(m_FactoryServiceProxy).onRemoveFactory(m_SUT, factory);
        
        verify(component.getInstance()).dispose();
        
        verify(factory).dispose(m_ObjectProxy1);
        verify(factory).dispose(m_ObjectProxy2);
        verify(m_FactoryRegistry).removeObject(m_ObjectUuid1);
        verify(m_FactoryRegistry).removeObject(m_ObjectUuid2);
        
        verify(m_CoreBundleContext).ungetService(proxyServiceRef);
        
        assertThat(m_SUT.getFactories().isEmpty(), is(true));
    }
    
    /**
     * Verify a bundle will be retrieved for the bundle that contains API classes.
     */
    @Test
    public void testGetApiBundle()
    {
        BundleService bundleService = mock(BundleService.class);
        m_SUT.setBundleService(bundleService);
        Bundle apiBundle = mock(Bundle.class);
        
        when(bundleService.getBundle(FactoryObject.class)).thenReturn(apiBundle);
        
        assertThat(m_SUT.getApiBundle(), is(apiBundle));
    }
}
