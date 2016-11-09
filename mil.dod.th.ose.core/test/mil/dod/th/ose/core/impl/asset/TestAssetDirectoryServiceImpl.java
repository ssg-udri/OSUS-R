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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetAttributes;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.FactoryObjectDataManagerMocker;
import mil.dod.th.ose.core.impl.asset.data.AssetFactoryObjectDataManager;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class TestAssetDirectoryServiceImpl
{
    private static final String PRODUCT_TYPE = "product-type";
    private ScannerManager m_ScannerManager;
    private AssetDirectoryServiceImpl m_SUT;
    private FactoryInternal m_AssetFactory;
    private Map<String, AssetFactory> m_AssetFactories = new HashMap<>();
    private EventAdmin m_EventAdmin;
    private AssetFactoryObjectDataManager m_AssetFactoryObjectDataManager;
    private FactoryRegistry<AssetInternal> m_Registry;
    private BundleContext m_BundleContext;
    private ComponentInstance m_FactServiceContextInstance;
    
    private FactoryServiceProxy<AssetInternal> m_FactoryServiceProxy;
    
    @Mock private ComponentFactory serviceContextFactory;
    @SuppressWarnings("rawtypes")
    private FactoryServiceContext m_ServiceContext;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Before
    public void setUp() throws Exception
    {
        // Constructor
        m_SUT = new AssetDirectoryServiceImpl();

        MockitoAnnotations.initMocks(this);
        
        m_ScannerManager = mock(ScannerManager.class);
        m_BundleContext = mock(BundleContext.class);
        m_Registry = mock(FactoryRegistry.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_AssetFactoryObjectDataManager = mock(AssetFactoryObjectDataManager.class);
        m_FactoryServiceProxy = mock(FactoryServiceProxy.class);
        
        FactoryObjectDataManagerMocker.createMockFactoryObjectDataManager(m_AssetFactoryObjectDataManager);
              
        m_AssetFactory = mock(FactoryInternal.class);
        when(m_AssetFactory.getProductName()).thenReturn("AssetProxy1");
        when(m_AssetFactory.getProductType()).thenReturn(PRODUCT_TYPE);
        m_AssetFactories.put(PRODUCT_TYPE, m_AssetFactory);
        
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        
        m_SUT.setScannerManager(m_ScannerManager);
        m_SUT.setEventAdmin(m_EventAdmin);     
        m_SUT.setFactoryServiceProxy(m_FactoryServiceProxy);
        m_SUT.setFactoryServiceContextFactory(serviceContextFactory);
        m_FactServiceContextInstance = ComponentFactoryMocker.mockSingleComponent(
                FactoryServiceContext.class, serviceContextFactory).getInstance();
        m_ServiceContext = (FactoryServiceContext)m_FactServiceContextInstance.getInstance();
        when(m_ServiceContext.getFactories()).thenReturn(m_AssetFactories);
        when(m_ServiceContext.getRegistry()).thenReturn(m_Registry);

        m_SUT.activate(m_BundleContext);
    }

    @After
    public void tearDown() throws InvalidSyntaxException
    {
        m_SUT.deactivate();
        verify(m_FactServiceContextInstance).dispose();
    }
    
    /**
     * Verify registry and service context are initialized properly.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivation() throws Exception
    {
        verify(serviceContextFactory).newInstance(null);
        verify(m_ServiceContext).initialize(m_BundleContext, m_FactoryServiceProxy, m_SUT);
    }
    
    /**
     * Verify an invalidate product type name will cause an exception.
     */
    @Test
    public void testCreateAssetWithInvalidProductType() throws AssetException
    {
        try
        {
            m_SUT.createAsset("blah");
            fail("Expecting failure because product type name is invalid");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        
        try
        {
            m_SUT.createAsset("blah", "valid-name");
            fail("Expecting failure because product type name is invalid");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
    
    /**
     * Test that asset is created and dictionary passed in is correct.
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testCreateAsset() throws AssetException, FactoryException, 
        IllegalArgumentException, FactoryObjectInformationException, ClassNotFoundException
    {
        AssetInternal mockAsset = mock(AssetInternal.class);
        when(m_Registry.createNewObject(eq(m_AssetFactory), Mockito.any(String.class), 
                Mockito.any(Map.class))).thenReturn(mockAsset);
        
        Asset asset = m_SUT.createAsset(PRODUCT_TYPE);
        assertThat(asset, is(notNullValue()));
        
        verify(m_Registry).createNewObject(m_AssetFactory, null, new Hashtable<String, Object>());
    }
    
    /**
     * Test handling of exception when trying to create an asset.
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testCreateAssetException() throws AssetException, FactoryException, 
        IllegalArgumentException, FactoryObjectInformationException, ClassNotFoundException
    {
        when(m_Registry.createNewObject(eq(m_AssetFactory), Mockito.any(String.class), 
                Mockito.any(Map.class))).thenThrow(new IllegalArgumentException("test error"));
        
        try
        {
            m_SUT.createAsset(PRODUCT_TYPE);
            fail("Expected exception from createAsset");
        }
        catch (AssetException e)
        {
        }
    }
    
    /**
     * Test adding an asset.
     * 
     * Verify that the base type and uuid are correct in the posted event.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddAsset() throws IllegalArgumentException, AssetException, 
        FactoryException, FactoryObjectInformationException, ClassNotFoundException
    {
        AssetInternal mockAsset1 = mock(AssetInternal.class);
        when(m_Registry.createNewObject(eq(m_AssetFactory), eq("asset"), 
                Mockito.any(Map.class))).thenReturn(mockAsset1);
        
        m_SUT.createAsset(PRODUCT_TYPE, "asset");
        verify(m_Registry).createNewObject(m_AssetFactory, "asset", new Hashtable<String, Object>());
    }
    
    /**
     * Test that asset is created with the given name and properties.
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testCreateAssetWithProps() throws AssetException, FactoryException, 
        IllegalArgumentException, FactoryObjectInformationException, ClassNotFoundException
    {
        AssetInternal mockAsset = mock(AssetInternal.class);
        when(m_Registry.createNewObject(eq(m_AssetFactory), Mockito.any(String.class), 
                Mockito.any(Map.class))).thenReturn(mockAsset);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", "prop1value");
        Asset asset = m_SUT.createAsset(PRODUCT_TYPE, "asset", props);
        assertThat(asset, is(notNullValue()));
        
        verify(m_Registry).createNewObject(m_AssetFactory, "asset", props);
    }
    
    /**
     * Test the service will return the factory descriptors for all registered factories.
     */
    @Test
    public void testGetAssetFactoryDescriptors()
    {
        when(m_ServiceContext.getFactories()).thenReturn(m_AssetFactories);
        
        Set<AssetFactory> expectedFactories = 
                new HashSet<AssetFactory>(m_AssetFactories.values());
        Set<AssetFactory> factories = m_SUT.getAssetFactories();
        assertThat(factories, is(expectedFactories));
    }
    
    /**
     * Verify assets are returned if assets are created.
     */
    @Test
    public void testGetAssets() throws IllegalArgumentException, 
        AssetException, FactoryException, FactoryObjectInformationException
    {
        Set<AssetInternal> set = new HashSet<AssetInternal>();
        set.add(mock(AssetInternal.class));
        when(m_Registry.getObjects()).thenReturn(set);
        
        Set<Asset> allAssets = m_SUT.getAssets();
        assertThat("Null Retrieved from getAssets method", allAssets, is(notNullValue()));
        assertThat(allAssets.size(), is(1));
    }
    
    /**
     * Verify that new assets can be scanned for if a factory implements such behavior.
     */
    @Test
    public void testScanForNewAssets() throws InterruptedException, 
        IllegalArgumentException, AssetException, FactoryException, 
        FactoryObjectInformationException, ClassNotFoundException
    {
        m_SUT.scanForNewAssets();
        verify(m_ScannerManager).scanForAllAssets(m_SUT);
    }
    
    /**
     * Verify ability to scan for assets of a particular type.
     */
    @Test
    public void testScanForNewAssetsWithParam() throws InterruptedException, 
        IllegalArgumentException, AssetException, FactoryException, 
        FactoryObjectInformationException, ClassNotFoundException
    {
        m_SUT.scanForNewAssets(PRODUCT_TYPE);
        verify(m_ScannerManager).scanForAssetsByType(m_SUT, PRODUCT_TYPE);
    }
    
    /**
     * Verify that the scannable asset types are returned from {@link ScannerManager}.
     */
    @Test
    public void testGetScannableAssetTypes()
    {
        Set<String> assetTypes = new HashSet<>();
        assetTypes.add(PRODUCT_TYPE);
        assetTypes.add("other-type");
        when(m_ScannerManager.getScannableAssetTypes()).thenReturn(assetTypes);

        assertThat(m_SUT.getScannableAssetTypes(), is(assetTypes));
    }

   /**
     * Verify the correct assets are returned when a request to get assets by type is made.
     */
    @Test
    public void testGetAssetsByType() throws IllegalArgumentException, 
        AssetException, FactoryException, FactoryObjectInformationException
    {
        Set<AssetInternal> set = new HashSet<AssetInternal>();
        set.add(mock(AssetInternal.class));
        when(m_Registry.getObjectsByProductType(PRODUCT_TYPE)).thenReturn(set);
        
        Set<? extends Asset> assetSet = m_SUT.getAssetsByType(PRODUCT_TYPE);
        assertThat(assetSet.size(), is(1));
    }
    
    /**
     * Verify requesting as asset by UUID throws an exception if the UUID is null.
     * Verify the correct asset is returned if the UUID is not null.
     */
    @Test
    public void testGetAssetByUuid() throws AssetException, 
        IllegalArgumentException, FactoryException, FactoryObjectInformationException
    {
        UUID uuid = UUID.randomUUID();
        AssetInternal internal = mock(AssetInternal.class);
        when(m_Registry.getObjectByUuid(uuid)).thenReturn(internal);
        Asset retrievedAsset = m_SUT.getAssetByUuid(uuid);
        assertThat(retrievedAsset, notNullValue());
    }
    
    /**
     * Verify requesting as asset by name throws an exception if the name is null.
     * Verify the correct asset is returned if the name is not null.
     */
    @Test
    public void testGetAssetByName() throws AssetException
    {
        when(m_Registry.getObjectByName("name")).thenReturn(mock(AssetInternal.class));
        Asset retrievedAsset = m_SUT.getAssetByName("name");
        assertThat(retrievedAsset, notNullValue());
    }
    
    /**
     * Verify that if a request to find out if an asset is available is made that a null value throws an exception.
     * Verify that the expected value is returned if the requested, named, asset is found.
     */
    @Test
    public void testIsAssetAvailable() throws AssetException
    {
        when(m_Registry.isObjectCreated("blah")).thenReturn(false);
        assertThat(m_SUT.isAssetAvailable("blah"), is(false));
        
        when(m_Registry.isObjectCreated("name")).thenReturn(true);
        assertThat(m_SUT.isAssetAvailable("name"), is(true));
    }
    
    /**
     * Verify ability to get an asset's status.
     */
    @Test
    public void testGetAssetStatus() throws IllegalArgumentException, 
        AssetException, FactoryException, FactoryObjectInformationException, ClassNotFoundException
    {
        mockCreateAssetSequence();
        final Asset tempAsset = m_SUT.createAsset(PRODUCT_TYPE);
        assertThat(AssetActiveStatus.DEACTIVATED, is(tempAsset.getActiveStatus()));
    }
    
    /**
     * Verify that all instance of assets must be unique.
     */
    @Test
    public void testObjectsAlreadyInMaps() throws IllegalArgumentException, 
        AssetException, FactoryException, FactoryObjectInformationException
    {
        Set<AssetInternal> set = new HashSet<>();
        set.add(mock(AssetInternal.class));
        when(m_Registry.getObjects()).thenReturn(set);
        final Set<Asset> allAssets = m_SUT.getAssets();
        assertThat(allAssets.size(), is(1));
    }
    
    /**
     * Verify get assets by type returns and unmodifiable set.
     */
    @Test
    public void testGetAssetByTypeReturnsUnmodifiable() throws IllegalArgumentException, AssetException
    {
        Set<AssetInternal> set = new HashSet<>();
        set.add(mock(AssetInternal.class));
        when(m_Registry.getObjectsByProductType(PRODUCT_TYPE)).thenReturn(set);
        Set<Asset> assets = m_SUT.getAssetsByType(PRODUCT_TYPE);

        assertThat(assets.isEmpty(), is(false));
        
        try
        {
            assets.remove(assets.iterator().next());
            fail("Expected exception to be thrown upon modifying the set.");
        }
        catch(Exception e)
        {
            // do nothing
        }
    }
    
    /**
     * Verify that get assets returns an unmodifiable set.
     */
    @Test
    public void testGetAssetReturnsUnmodifiable() throws IllegalArgumentException, AssetException
    {
        Set<AssetInternal> set = new HashSet<>();
        set.add(mock(AssetInternal.class));
        when(m_Registry.getObjects()).thenReturn(set);
        Set<Asset> assets = m_SUT.getAssets();

        assertThat(assets.isEmpty(), is(false));
        
        try
        {
            assets.remove(assets.iterator().next());
            fail("Expected exception to be thrown upon modifying the set.");
        }
        catch(Exception e)
        {
            // do nothing
        }
    }
    
    /**
     * Method which creates a mock instance of an asset and provides stubbing to provide 
     * @return
     *  the mocked asset impl
     */
    @SuppressWarnings("unchecked")
    private AssetInternal mockCreateAssetSequence() throws IllegalArgumentException, FactoryException, 
        FactoryObjectInformationException, ClassNotFoundException
    {
        final AssetInternal mockAsset = mock(AssetInternal.class);
        
        FactoryInternal descriptor = mock(FactoryInternal.class);
        final UUID uuid = UUID.randomUUID();
        when(mockAsset.getUuid()).thenReturn(uuid);
        when(descriptor.getProductType()).thenReturn(PRODUCT_TYPE);
        when(mockAsset.getFactory()).thenReturn(descriptor);
        when(mockAsset.getName()).thenReturn("Ditka");

        AssetAttributes attributes = mock(AssetAttributes.class);
        when(attributes.activateOnStartup()).thenReturn(new Boolean(false));
        
        when(mockAsset.getConfig()).thenReturn(attributes);
        when(mockAsset.getActiveStatus()).thenReturn(AssetActiveStatus.DEACTIVATED);
        
        when(m_Registry.createNewObject(eq(m_AssetFactory), Mockito.any(String.class), 
                Mockito.any(Map.class))).thenReturn(mockAsset);
        when(m_Registry.getObjectByUuid(uuid)).thenReturn(mockAsset);
        
        Set<AssetInternal> objSet = new HashSet<>();
        objSet.add(mockAsset);
        when(m_Registry.getObjects()).thenReturn(objSet);
        
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                m_EventAdmin.postEvent(new Event((String)invocation.getArguments()[0], 
                        (Map<String, Object>)invocation.getArguments()[1]));
                return null;
            }
        }).when(mockAsset).postEvent(Mockito.anyString(), Mockito.anyMap());
        
        return mockAsset;
    }
}