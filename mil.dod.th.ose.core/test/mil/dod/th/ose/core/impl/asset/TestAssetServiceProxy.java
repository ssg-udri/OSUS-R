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
package mil.dod.th.ose.core.impl.asset;

// - use assertion methods from Hamcrest (instead of JUnit built in assertions as Hamcrest 
// provides more information when an assertion fails). Hamcrest library also contains a lot of
// matchers that should be used when possible (e.g., use assertThat(map, hasEntry(key, value)) 
// instead of assertThat(map.get(key), is(value)) as it will provide a list of entries in the map
// if the desired one is not found)
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.impl.asset.data.AssetFactoryObjectDataManager;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * This unit test is embellished with more comments than normal to explain conventions to be used when 
 * unit testing.
 * 
 * Each Java class should have a single Java unit test class with "Test" prepended to the name of class 
 * being tested. This means interfaces do not need to have unit test as they contain no logic.
 * 
 * Getter and setter methods do not need to be unit tested.
 * 
 * Unit tests should not access any resources or platform specific logic. For instance, if the class needs to read in a 
 * file. The {@link mil.dod.th.ose.utils.FileService} interface should be used and mocked instead.
 */
public class TestAssetServiceProxy
{
    /** 
     * Each unit test class will have a software under test field (named m_SUT) to hold an instance of 
     * the class being tested. 
     */
    private AssetServiceProxy m_SUT;
    
    /**
     * Objects used by class should be mocked so their behavior can be controlled and the unit tests can
     * be run with as little dependencies as possible.
     * 
     * Simple objects or data objects (objects that don't have any methods besides getters/setters) do 
     * not need to be mocked as they don't add any complexity to the test. In these cases, mocking would
     * make things more complicated.
     */
    @Mock private ComponentFactory componentFactory;
    @Mock private FactoryServiceContext<AssetInternal> factoryServiceContext;
    @Mock private FactoryRegistry<AssetInternal> factoryRegistry;
    @Mock private FactoryInternal factory;
    @Mock private AssetDirectoryServiceImpl assetDirectoryService;
    @Mock private AssetFactoryObjectDataManager assetFactoryObjectDataManager;
    
    private ComponentInfo<AssetInternal> m_ComponentInfo;
    
    @Before
    public void setup()
    {
        // this will initialize all annotated mocks (fields with the @Mock annotation)
        MockitoAnnotations.initMocks(this);
        
        // stub mocks to behave in a certain way when their methods are called, be sure to use existing mock libraries
        // in mil.dod.th.ose.test package or add a mock utility class if mocker would be used elsewhere
        m_ComponentInfo = ComponentFactoryMocker.mockSingleComponent(AssetInternal.class, componentFactory);
        
        // create instance of the software under test (class being unit tested)
        m_SUT = new AssetServiceProxy();
        
        // bind any mocked services to the SUT
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.setAssetFactory(componentFactory);
        m_SUT.setAssetFactoryObjectDataManager(assetFactoryObjectDataManager);
    }
    
    /*
     * Each test method is annotated with @Test so the method is run when the test class is run.
     * 
     * Each test method should verify a single class method and particular state. Keep test methods 
     * simple.
     * 
     * The method name format should be "test<MethodUnderTest>_<state>" where _<state> is optional if 
     * the test is checking normal path of the method.
     * 
     * Each test method should have 3 parts: mock, replay and verify
     * 
     * Tests should have a short comment to explain what they do if not obvious by the method name
     */
    @Test
    public void testInitializeProxy() throws FactoryException
    {
        // additional mock stubbing would be added here when the mock behavior needs to be unique for 
        // the specific method
        AssetProxy mockProxy = mock(AssetProxy.class);
        AssetInternal internalObj = mock(AssetInternal.class);
        
        // replay step (call the method being tested, setting up any parameters needed for the method 
        // call)
        Map<String, Object> map = new HashMap<>();
        m_SUT.initializeProxy(internalObj, mockProxy, map);
        
        // verify step
        //
        // - use verify() to make sure a particular method is called on a mocked object with the 
        // necessary parameters. Here we want to make sure the backed proxy class is initialized by the SUT
        verify(mockProxy).initialize(internalObj, map);
    }
    
    /**
     * Verify that an internal object can be created.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateComponentInstance()
    {
        ComponentInstance instance = m_SUT.createFactoryObjectInternal(factory);
        
        verify(componentFactory).newInstance(Mockito.any(Dictionary.class));
        
        assertThat(instance, is(m_ComponentInfo.getInstance()));
    }
    
    /**
     * Verify there are no additional service attr defs.
     */
    @Test
    public void testGetExtendedServiceAttributeDefinitions()
    {
        assertThat(m_SUT.getExtendedServiceAttributeDefinitions(factoryServiceContext, factory, 
                ObjectClassDefinition.ALL).length, is(0));
    }
    
    /**
     * Verify proper capability type is returned.
     */
    @Test
    public void testGetCapType()
    {
        assertThat(m_SUT.getCapabilityType().getName(), is(AssetCapabilities.class.getName()));
    }
    
    /**
     * Verify proper base type is returned.
     */
    @Test
    public void testGetBaseType()
    {
        assertThat(m_SUT.getBaseType().getName(), is(Asset.class.getName()));
    }
    
    /**
     * Verify dictionary with factory type is returned for additional registration properties.
     */
    @Test
    public void testGetAdditionalServiceRegProps()
    {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, AssetFactory.class);
        assertThat(m_SUT.getAdditionalFactoryRegistrationProps(null).equals(props), is(true));
    }
    
    /**
     * Test to verify that when an asset factory removed, all assets are deactivated first.
     */
    @Test
    public void testOnRemoveFactory() throws Exception
    {
        AssetInternal asset1 = mock(AssetInternal.class);
        stubDeactivationAsset(asset1, AssetActiveStatus.ACTIVATED, 5);
        AssetInternal asset2 = mock(AssetInternal.class);
        stubDeactivationAsset(asset2, AssetActiveStatus.DEACTIVATED, 0);
        AssetInternal asset3 = mock(AssetInternal.class);
        // will take 100 seconds but total time should only be 10 seconds as that is the max
        stubDeactivationAsset(asset3, AssetActiveStatus.ACTIVATED, 100); 
        
        Set<AssetInternal> assets = new HashSet<>();
        assets.add(asset1);
        assets.add(asset2);
        assets.add(asset3);
                
        // use of mockito to tell what an injected service should do when called
        when(factoryServiceContext.getRegistry()).thenReturn(factoryRegistry);
        when(factoryServiceContext.getDirectoryService()).thenReturn(assetDirectoryService);
        when(factory.getProductType()).thenReturn("product-type");
        when(factoryRegistry.getObjectsByProductType("product-type")).thenReturn(assets);
        
        // replay
        long timeBefore = System.currentTimeMillis();
        m_SUT.onRemoveFactory(factoryServiceContext, factory);
        long timeDelta = System.currentTimeMillis() - timeBefore;
        
        // waits a total of 10 seconds, so the thread that takes 100 seconds should not keep this running longer
        assertThat((int)timeDelta, is(lessThanOrEqualTo(12 * 1000)));
        
        // verify
        ArgumentCaptor<AssetActivationListener[]> listenersCap = 
                ArgumentCaptor.forClass(AssetActivationListener[].class);
        verify(asset1).internalDeactivate(listenersCap.capture());
        verify(asset2, never()).internalDeactivate(listenersCap.capture());
        verify(asset3).internalDeactivate(listenersCap.capture());
        
        for (AssetActivationListener[] listeners : listenersCap.getAllValues())
        {
            assertThat(listeners[0], is(instanceOf(ActivationListenerBridge.class)));
            assertThat(listeners[1], is(instanceOf(OnAssetDeactivateListener.class)));
        }
    }

    /**
     * Verify that set factory object data manager is returned.
     */
    @Test
    public void testGetDataManager()
    {
        assertThat((AssetFactoryObjectDataManager)m_SUT.getDataManager(), is(assetFactoryObjectDataManager));
    }
    
    /**
     * Verify that callback is created.
     */
    @Test
    public void testCreateCallback()
    {
        assertThat(m_SUT.createCallback(factoryServiceContext).getClass().getName(), 
                is(AssetRegistryCallback.class.getName()));
    }
    
    /**
     * Stub out each asset that can be deactivated.
     * 
     * @param waitSecs
     *      how long to wait in seconds until deactivation is completed
     */
    private void stubDeactivationAsset(final AssetInternal asset, AssetActiveStatus activated, final long waitSecs)
    {
        when(asset.getActiveStatus()).thenReturn(activated);
        
        when(asset.internalDeactivate(Mockito.any(AssetActivationListener[].class)))
            .thenAnswer(new Answer<Thread>()
            {
                @Override
                public Thread answer(final InvocationOnMock invocation) throws Throwable
                {
                    Thread thread = new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            AssetActivationListener[] listeners = 
                                    (AssetActivationListener[])invocation.getArguments()[1];
                            OnAssetDeactivateListener listener = (OnAssetDeactivateListener)listeners[1];
                            
                            try
                            {
                                Thread.sleep(waitSecs * 1000);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }

                            listener.assetDeactivationComplete(asset);
                        }
                    });
                    
                    return thread;
                }
            });
    }
}
