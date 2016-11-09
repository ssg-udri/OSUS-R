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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static mil.dod.th.ose.test.matchers.Matchers.*;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.core.MetaTypeMocker;
import mil.dod.th.ose.core.MetaTypeProviderBundleMocker;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.metatype.MetaTypeProviderBundle;
import mil.dod.th.ose.test.AttributeDefinitionMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;
import mil.dod.th.ose.test.ServiceRegistrationMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import com.google.common.collect.ImmutableMap;

public class TestFactoryDescriptorImpl
{
    private static final String PRODUCT_TYPE = "some.product.type";
    
    private FactoryDescriptorImpl m_SUT;
    private BaseCapabilities m_Caps;
    private MetaTypeService m_MetaTypeService;
    private Dictionary<String, Object> m_AddProps = new Hashtable<>();
    private MetaTypeProviderBundle m_MetaTypeProviderBundle;
    private BundleContext m_MetaTypeProviderBundleContext;
    
    @Mock private XmlUnmarshalService xmlUS;
    @Mock private FactoryServiceContext<?> serviceContext;
    @Mock private ServiceReference<ComponentFactory> pluginServiceRef;
    @SuppressWarnings("rawtypes")
    @Mock private ServiceRegistration metaServiceReg;
    @Mock private Bundle pluginBundle;
    @Mock private BundleContext coreBundleContext;
    @Mock private FactoryRegistry<?> factoryRegistry;
    @SuppressWarnings("rawtypes")
    @Mock private ServiceRegistration descriptorServiceReg;
    @Mock private ComponentFactory componentFactory;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new FactoryDescriptorImpl();
        m_Caps = new BaseCapabilities().withDescription("blah");
        
        MockitoAnnotations.initMocks(this);
        m_MetaTypeService = MetaTypeMocker.createMockMetaType();
        m_MetaTypeProviderBundle = MetaTypeProviderBundleMocker.mockIt();
        // bind
        m_SUT.setXMLUnmarshalService(xmlUS);
        
        // stub
        when(pluginServiceRef.getProperty(ComponentConstants.COMPONENT_NAME)).thenReturn(PRODUCT_TYPE);
        when(pluginServiceRef.getBundle()).thenReturn(pluginBundle);
        
        doReturn(BaseCapabilities.class).when(serviceContext).getCapabilityType();
        when(serviceContext.getMetaTypeService()).thenReturn(m_MetaTypeService);
        when(serviceContext.getMetaTypeProviderBundle()).thenReturn(m_MetaTypeProviderBundle);
        when(serviceContext.getCoreContext()).thenReturn(coreBundleContext);
        m_AddProps = new Hashtable<>();
        when(serviceContext.getAdditionalFactoryRegistrationProps(m_SUT)).thenReturn(m_AddProps);
        doReturn(factoryRegistry).when(serviceContext).getRegistry();
        
        URL capUrl = new URL("file:/cap.xml");
        when(xmlUS
            .getXmlResource(pluginServiceRef.getBundle(), FactoryDescriptor.CAPABILITIES_XML_FOLDER_NAME, PRODUCT_TYPE))
            .thenReturn(capUrl);
        when(xmlUS.getXmlObject(BaseCapabilities.class, capUrl)).thenReturn(m_Caps);
        
        MetaTypeMocker.registerMetaTypeAttributes(pluginServiceRef.getBundle(),
                PRODUCT_TYPE + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, 
                AttributeDefinitionMocker.mockArrayRequired(), AttributeDefinitionMocker.mockArrayOptional(), 
                AttributeDefinitionMocker.mockArrayAll());
        
        m_MetaTypeProviderBundleContext = m_MetaTypeProviderBundle.getBundle().getBundleContext();
        
        doReturn(FactoryObject.class).when(serviceContext).getBaseType();
        
        when(m_MetaTypeProviderBundleContext.registerService(
                eq(new String[]{ManagedServiceFactory.class.getName(), MetaTypeProvider.class.getName()}), 
                    Mockito.any(FactoryConfigurationService.class),
                    Mockito.any(Dictionary.class))).thenReturn(metaServiceReg);
        
        when(coreBundleContext.registerService(eq(FactoryDescriptor.class), eq(m_SUT), Mockito.any(Dictionary.class)))
            .thenReturn(descriptorServiceReg);
        
        ServiceRegistrationMocker.mockIt(metaServiceReg);
        ServiceRegistrationMocker.mockIt(descriptorServiceReg);
        when(descriptorServiceReg.getReference().getProperty(FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY))
            .thenReturn(PRODUCT_TYPE);
        
        // common activation
        m_SUT.activate(new ImmutableMap.Builder<String, Object>()
                .put(FactoryDescriptorImpl.KEY_SERVICE_CONTEXT, serviceContext)
                .put(FactoryDescriptorImpl.KEY_SERVICE_REFERENCE, pluginServiceRef)
                .put(FactoryDescriptorImpl.KEY_COMPONENT_FACTORY, componentFactory).build());
    }
    
    /**
     * Verify the correct factory PID is returned.
     */
    @Test
    public void testGetPid()
    {
        assertThat(m_SUT.getPid(), is(PRODUCT_TYPE + FactoryDescriptor.PID_SUFFIX));
    }
    
    /**
     * Verify product type is based on the component name for the passed in service reference.
     */
    @Test
    public void testGetProductType() throws Exception
    {
        assertThat(m_SUT.getProductType(), is(PRODUCT_TYPE));
    }
    
    /**
     * Verify product name is based on capabilities.
     */
    @Test
    public void testGetProductName() throws Exception
    {
        m_Caps.setProductName("some name");
        assertThat(m_SUT.getProductName(), is("some name"));
    }
    
    /**
     * Verify product description is based on capabilities.
     */
    @Test
    public void testGetProductDescription() throws Exception
    {
        m_Caps.setDescription("some desc");
        assertThat(m_SUT.getProductDescription(), is("some desc"));
    }
    
    /**
     * Verify capabilities can be retrieved based on activation.
     */
    @Test
    public void testGetCapabilities() throws Exception
    {
        assertThat(m_SUT.getCapabilities(), is(m_Caps));
    }
    
    /**
     * Verify ability to get required attribute definitions for a plug-in.
     */
    @Test
    public void testGetPluginAttributeDefinitionsRequired() throws UnmarshalException
    {
        AttributeDefinition[] retrieveRequired = 
                m_SUT.getPluginAttributeDefinitions(ObjectClassDefinition.REQUIRED);
        assertThat(retrieveRequired.length, is(2));
    }
    
    /**
     * Verify ability to get optional attribute definitions for a plug-in.
     */
    @Test
    public void testGetPluginAttributeDefinitionsOptional() throws UnmarshalException
    {
        AttributeDefinition[] retrieveOptional = 
                m_SUT.getPluginAttributeDefinitions(ObjectClassDefinition.OPTIONAL);        
        assertThat(retrieveOptional.length, is(4));
    }
    
    /**
     * Verify ability to get all attribute definitions for a plug-in.
     */
    @Test
    public void testGetPluginAttributeDefinitionsAll() throws UnmarshalException
    {
        AttributeDefinition[] retrieveAll = m_SUT.getPluginAttributeDefinitions(ObjectClassDefinition.ALL); 
        assertThat(retrieveAll.length, is(6));
    }
    
    /**
     * Verify exception if the plug-in attr definitions are not available.
     */
    @Test
    public void testGetPluginAttributeDefinitionsBadOcd() throws UnmarshalException
    {
        MetaTypeMocker.registerMetaTypeAttributesNoOcdDescription(pluginServiceRef.getBundle(),
                PRODUCT_TYPE + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, new AttributeDefinition[]{});
        
        try
        {
            m_SUT.activate(new ImmutableMap.Builder<String, Object>()
                    .put(FactoryDescriptorImpl.KEY_SERVICE_CONTEXT, serviceContext)
                    .put(FactoryDescriptorImpl.KEY_SERVICE_REFERENCE, pluginServiceRef).build());
            fail("expected exception no OCD description.");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
    
    /**
     * Verify empty definitions are returned correctly.
     */
    @Test
    public void testGetPluginAttributeDefinitionsEmpty() throws UnmarshalException
    {
        // change to return null for attribute defs
        MetaTypeMocker.registerMetaTypeAttributes(pluginServiceRef.getBundle(),
                PRODUCT_TYPE + FactoryObjectInternal.ATTRIBUTES_CLASS_SUFFIX, null, null, null);
        
        AttributeDefinition[] retrieved = m_SUT.getPluginAttributeDefinitions(ObjectClassDefinition.ALL);
        
        assertThat(retrieved.length, is(0));
    }
    
    /*
     * Verify init call will register services and fetch metatype information.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testRegisterServices() throws UnmarshalException
    {
        m_SUT.registerServices();
        
        String[] clazzes = {ManagedServiceFactory.class.getName(), MetaTypeProvider.class.getName()};
        ArgumentCaptor<Dictionary> props = ArgumentCaptor.forClass(Dictionary.class); 
        verify(m_MetaTypeProviderBundleContext).registerService(eq(clazzes), 
                Mockito.any(FactoryConfigurationService.class), props.capture());
        assertThat(props.getValue(), rawDictionaryHasEntry(Constants.SERVICE_PID, PRODUCT_TYPE + "Config"));
        assertThat(props.getValue(), rawDictionaryHasEntry(MetaTypeProvider.METATYPE_FACTORY_PID, 
                PRODUCT_TYPE + "Config"));
    }
    
    /**
     * Verify that the factory service is registered.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testMakeAvailable() throws InterruptedException, UnmarshalException
    {
        m_AddProps.put("some-key", "some-value");
        
        m_SUT.makeAvailable();
        
        ArgumentCaptor<Dictionary> props = ArgumentCaptor.forClass(Dictionary.class);
        verify(coreBundleContext).registerService(eq(FactoryDescriptor.class), eq(m_SUT), props.capture());
        assertThat(props.getValue(), 
                rawDictionaryHasEntry(FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY, PRODUCT_TYPE));
        assertThat(props.getValue(), rawDictionaryHasEntry("some-key", "some-value"));
    }

    /*
     * Verify that the factory service is unregistered.
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testUnavailable() throws InterruptedException, UnmarshalException
    {
        m_SUT.registerServices();
        m_SUT.makeAvailable();
        
        verify(coreBundleContext, timeout(3000)).registerService(eq(FactoryDescriptor.class), eq(m_SUT), 
                Mockito.any(Dictionary.class));

        m_SUT.makeUnavailable();
        verify(descriptorServiceReg).unregister();
        verify(metaServiceReg, never()).unregister(); // should only unregister descriptor registration
    }
    
    /*
     * Verify that the other factory service registrations are cleaned up.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCleanup() throws InterruptedException, UnmarshalException
    {
        m_SUT.registerServices();
        m_SUT.makeAvailable();
        
        verify(coreBundleContext, timeout(3000)).registerService(eq(FactoryDescriptor.class), eq(m_SUT), 
                Mockito.any(Dictionary.class));
        
        m_SUT.cleanup();
        verify(metaServiceReg).unregister();
        verify(descriptorServiceReg).unregister();
    }
    
    /**
     * Verify the create method will call on the {@link ComponentFactory}.
     */
    @Test
    public void testCreate()
    {
        ComponentFactoryMocker.mockSingleComponent(FactoryObjectProxy.class, componentFactory);
        
        m_SUT.create();
        
        verify(componentFactory).newInstance(null);
    }
    
    /**
     * Verify the dispose method will dispose of previously created instance.
     */
    @Test
    public void testDispose()
    {
        List<ComponentInfo<FactoryObjectProxy>> components = 
                ComponentFactoryMocker.mockComponents(FactoryObjectProxy.class, componentFactory, 3);
        
        FactoryObjectProxy obj1 = m_SUT.create();
        FactoryObjectProxy obj2 = m_SUT.create();
        FactoryObjectProxy obj3 = m_SUT.create();
        
        // dispose of object and make sure the property component instance is disposed and not others
        m_SUT.dispose(obj2);
        verify(components.get(0).getInstance(), never()).dispose();
        verify(components.get(1).getInstance()).dispose();
        verify(components.get(2).getInstance(), never()).dispose();
        
        m_SUT.dispose(obj3);
        verify(components.get(0).getInstance(), never()).dispose();
        verify(components.get(1).getInstance()).dispose(); // still holder at 1
        verify(components.get(2).getInstance()).dispose(); // now disposed
        
        // dispose of last one, each should be disposed exactly 1 time
        m_SUT.dispose(obj1);
        verify(components.get(0).getInstance()).dispose();
        verify(components.get(1).getInstance()).dispose();
        verify(components.get(2).getInstance()).dispose();
        
        // dispose object again, shouldn't cause exception or additional calls to dispose
        m_SUT.dispose(obj1);
        verify(components.get(0).getInstance()).dispose();
        verify(components.get(1).getInstance()).dispose();
        verify(components.get(2).getInstance()).dispose();
    }
    
    /**
     * Verify that attributes can be retrieved for a given factory and bundle
     */
    @Test
    public void testGetFactoryObjectAttributes()
    {
        AttributeDefinition[] defs = AttributeDefinitionMocker.mockArrayAll();
        
        MetaTypeMocker.registerMetaTypeAttributes(m_MetaTypeProviderBundle.getBundle(), m_SUT, null, null, defs);
        
        AttributeDefinition[] ans = m_SUT.getAttributeDefinitions(ObjectClassDefinition.ALL);
        
        assertThat(ans, is(defs));        
    }
    
    /**
     * Verify empty list of attributes returned if no meta type information exists.
     */
    @Test
    public void testGetFactoryObjectAttributesNoMetaInformation()
    {
        when(m_MetaTypeService.getMetaTypeInformation(m_MetaTypeProviderBundle.getBundle())).thenReturn(null);
        
        AttributeDefinition[] ans = m_SUT.getAttributeDefinitions(ObjectClassDefinition.ALL);
        
        assertThat(ans.length, is(0));
    }
    
    /**
     * Verify empty list is returned if null returned for attribute definitions.
     */
    @Test
    public void testGetFactoryObjectAttributesNull()
    {
        MetaTypeInformation metaInfo = mock(MetaTypeInformation.class);
        ObjectClassDefinition ocd = mock(ObjectClassDefinition.class);
        
        when(m_MetaTypeService.getMetaTypeInformation(m_MetaTypeProviderBundle.getBundle())).thenReturn(metaInfo);
        when(metaInfo.getObjectClassDefinition(anyString(), anyString())).thenReturn(ocd);
        
        AttributeDefinition[] ans = m_SUT.getAttributeDefinitions(ObjectClassDefinition.ALL);
        
        assertThat(ans.length, is(0));
    }
}
