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
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetDirectoryService.ScanResults;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.AssetScanner;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.ProductType;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.test.EventAdminSyncer;
import mil.dod.th.ose.test.EventAdminVerifier;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class TestScannerManager
{
    private static final String ASSET_NAME = "some name";
    private ScannerManager m_SUT;
    private AssetDirectoryService m_ADS;
    private EventAdmin m_EventAdmin;
    private LoggingService m_Logging;
    private AssetScanner m_Scanner1;
    private AssetScanner m_Scanner2;
    private Map<String, Object> m_Props = new HashMap<>();
    private PowerManager m_PowerManager;
    private WakeLock m_WakeLock;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new ScannerManager();
        m_ADS = mock(AssetDirectoryService.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_Logging = LoggingServiceMocker.createMock();
        m_PowerManager = mock(PowerManager.class);
        m_WakeLock = mock(WakeLock.class);
        when(m_PowerManager.createWakeLock(m_SUT.getClass(), "coreAssetScanner")).thenReturn(m_WakeLock);

        m_SUT.setLoggingService(m_Logging);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setPowerManager(m_PowerManager);
        m_Scanner1 = new ScannerStub1();
        m_Scanner2 = new ScannerStub2();
        m_SUT.setAssetScanner(m_Scanner1);
        m_SUT.setAssetScanner(m_Scanner2);

        m_SUT.activate();
    }

    @After
    public void tearDown()
    {
        m_SUT.unsetAssetScanner(m_Scanner1);
        m_SUT.unsetAssetScanner(m_Scanner2);

        m_SUT.deactivate();
        verify(m_WakeLock, times(1)).delete();
    }

    /**
     * Verify the scanning of all asset scanners.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testScanForAllAssets() throws InterruptedException, IllegalArgumentException, AssetException
    {
        final Semaphore waitSemaphore = new Semaphore(0);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Event event = (Event)invocation.getArguments()[0];
                if (event.getTopic().equals(AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE))
                {
                    for (String key : event.getPropertyNames())
                    {
                        if (key.equals(FactoryDescriptor.EVENT_PROP_OBJ_TYPE))
                        {
                            return null;
                        }
                    }
                    waitSemaphore.release();  // found event without asset type property set
                }
                return null;
            }
        }).when(m_EventAdmin).postEvent(Mockito.any(Event.class));
        
        m_SUT.scanForAllAssets(m_ADS);
        
        verify(m_WakeLock).activate();
        assertThat(waitSemaphore.tryAcquire(1, TimeUnit.SECONDS), is(true));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(eventCaptor.capture());
        
        verify(m_ADS).createAsset(AssetType2.class.getName(), ASSET_NAME, m_Props);
        verify(m_ADS).createAsset(AssetType1.class.getName(), null, Collections.EMPTY_MAP);
        
        for (String assetType : m_SUT.getScannableAssetTypes())
        {
            EventAdminVerifier.assertEventByTopicAssetType(eventCaptor, 
                    AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS, assetType);

            EventAdminVerifier.assertEventByTopicAssetType(eventCaptor, 
                    AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE, assetType);
        }
        
        // should get null to signify all have completed
        EventAdminVerifier.assertEventByTopicNoAssetType(eventCaptor, 
                AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE);

        Thread.sleep(1000);
        verify(m_WakeLock).cancel();
    }

    /**
     * Verify the scanning of a specific asset type.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testScanForAssetsByType() throws IllegalArgumentException, AssetException, InterruptedException
    {
        EventAdminSyncer syncer = new EventAdminSyncer(m_EventAdmin, 
                AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE, 2);
        
        m_SUT.scanForAssetsByType(m_ADS, AssetType1.class.getName());

        verify(m_WakeLock).activate();
        
        syncer.waitFor(3, TimeUnit.SECONDS);
        
        verify(m_ADS).createAsset(AssetType1.class.getName(), null, Collections.EMPTY_MAP);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(eventCaptor.capture());

        EventAdminVerifier.assertEventByTopicAssetType(eventCaptor, AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS, 
                AssetType1.class.getName());

        Thread.sleep(1000);

        EventAdminVerifier.assertEventByTopicAssetType(eventCaptor, 
                AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE, AssetType1.class.getName());
    
        // should get null to signify all have completed
        EventAdminVerifier.assertEventByTopicNoAssetType(eventCaptor, 
                AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE);
        verify(m_WakeLock).cancel();
        
        // remove scanner and verify that exception is thrown
        m_SUT.unsetAssetScanner(m_Scanner1);
        try
        {
            m_SUT.scanForAssetsByType(m_ADS, AssetType1.class.getName());
            fail("Expecting exception since product type is not registered with the service");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Verify the handling of a scanner that exceeds the 15 second limit.
     */
    @Test
    public void testScanTimeout() throws InterruptedException
    {
        m_SUT.setAssetScanner(new ScannerStubTimeout());

        EventAdminSyncer syncer = new EventAdminSyncer(m_EventAdmin, 
                AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE);
        m_SUT.scanForAssetsByType(m_ADS, AssetTypeTimeout.class.getName());

        verify(m_WakeLock).activate();
        
        syncer.waitFor(16, TimeUnit.SECONDS);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(eventCaptor.capture());

        EventAdminVerifier.assertEventByTopicAssetType(eventCaptor, AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS, 
                AssetTypeTimeout.class.getName());

        // should get null to signify all have completed
        EventAdminVerifier.assertEventByTopicNoAssetType(eventCaptor, 
                AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE);

        Thread.sleep(1000);
        verify(m_WakeLock).cancel();
    }

    /**
     * Verify the handling of a scanner that exceeds the 10 second limit.
     */
    @Test
    public void testScanException() throws InterruptedException
    {
        m_SUT.setAssetScanner(new ScannerStubException());

        EventAdminSyncer syncer = new EventAdminSyncer(m_EventAdmin, 
                AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE, 2);
        m_SUT.scanForAssetsByType(m_ADS, AssetTypeException.class.getName());
        
        verify(m_WakeLock).activate();

        syncer.waitFor(16, TimeUnit.SECONDS);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(eventCaptor.capture());

        EventAdminVerifier.assertEventByTopicAssetType(eventCaptor, AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS, 
                AssetTypeException.class.getName());

        // should get null to signify all have completed
        EventAdminVerifier.assertEventByTopicNoAssetType(eventCaptor, 
                AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE);

        Thread.sleep(1000);
        verify(m_WakeLock).cancel();
    }

    /**
     * Verify that all registered scannable asset types are returned.
     */
    @Test
    public void testGetScannableAssetTypes()
    {
        assertThat(m_SUT.getScannableAssetTypes(), hasItems(AssetType1.class.getName(), AssetType2.class.getName()));
    }

    /**
     * Verify that duplicate scanners (same product type) are ignored.
     */
    @Test
    public void testSetDuplicateScanner()
    {
        // Try to add scanner again
        m_SUT.setAssetScanner(m_Scanner1);
        verify(m_Logging).error(anyString(), eq(AssetType1.class.getName()));
    }

    private interface AssetType1 extends AssetProxy
    {
    }

    @ProductType(AssetType1.class)
    private class ScannerStub1 implements AssetScanner
    {
        @Override
        public void scanForNewAssets(ScanResults results, Set<Asset> existing) throws AssetException
        {
            if (existing == null || existing.isEmpty())
            {
                results.found(null, null);
            }
        }
    }

    private interface AssetType2 extends AssetProxy
    {
    }

    @ProductType(AssetType2.class)
    private class ScannerStub2 implements AssetScanner
    {
        @Override
        public void scanForNewAssets(ScanResults results, Set<Asset> existing) throws AssetException
        {
            if (existing == null || existing.isEmpty())
            {
                m_Props.put("value1", 25);
                m_Props.put("value2", "some string");
                results.found(ASSET_NAME, m_Props);
            }
        }
    }

    private interface AssetTypeTimeout extends AssetProxy
    {
    }

    @ProductType(AssetTypeTimeout.class)
    private class ScannerStubTimeout implements AssetScanner
    {
        @Override
        public void scanForNewAssets(ScanResults results, Set<Asset> existing) throws AssetException
        {
            try
            {
                Thread.sleep(20000);
                results.found(null, null);
            }
            catch (InterruptedException e)
            {
            }
        }
    }

    private interface AssetTypeException extends AssetProxy
    {
    }

    @ProductType(AssetTypeException.class)
    private class ScannerStubException implements AssetScanner
    {
        @Override
        public void scanForNewAssets(ScanResults results, Set<Asset> existing) throws AssetException
        {
            throw new AssetException("test scanning exception");
        }
    }
}
