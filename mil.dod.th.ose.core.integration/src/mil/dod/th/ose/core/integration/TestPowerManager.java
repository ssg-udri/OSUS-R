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
package mil.dod.th.ose.core.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogService;

import example.asset.ExampleAsset;
import example.platform.power.ExamplePlatformPowerManager;
import example.platform.power.ExamplePowerManagerMethodLog;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.pm.WakeLockState;
import mil.dod.th.ose.integration.commons.AssetUtils;


/**
 * Tests the PowerManger implementation to ensure proper creation and deletion of WakeLocks and the retrieval 
 * of WakeLocks and WakeLock contexts.
 * 
 * @author jlatham
 *
 */
public class TestPowerManager extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private ExamplePlatformPowerManager m_PlatformPowerManager;
    
    private Calendar m_Calendar;
    
    @Override
    public void setUp()
    {
        m_PlatformPowerManager = (ExamplePlatformPowerManager)ServiceUtils.getService(m_Context,
                PlatformPowerManager.class);
        
        m_Calendar = Calendar.getInstance();
        assertThat(m_Calendar, is(notNullValue()));
    }
    
    @Override
    public void tearDown()
    {
        // Clear out all test locks
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        Set<WakeLock> testLocks = powerManager.getWakeLocks(TestPowerManager.class, WakeLockState.Any);
        int count = 0;
        for(WakeLock lock : testLocks)
        {
            lock.delete();
            Logging.log(LogService.LOG_DEBUG, "***** Deleting lock [%s] for context %s *****", lock.getId(), 
                    TestPowerManager.class);
            count++;
        }
        Logging.log(LogService.LOG_DEBUG, "***** %d Locks removed *****", count);
        
        // Remove entries of method logs
        m_PlatformPowerManager.clearMethodLog();
    }
    
    /** Removes locks created in a different context */    
    private void removeOtherLocks()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        Set<WakeLock> testLocks2 = powerManager.getWakeLocks(ExamplePlatformPowerManager.class, WakeLockState.Any);
        int count = 0;
        for(WakeLock lock : testLocks2)
        {
            lock.delete();
            Logging.log(LogService.LOG_DEBUG, "***** Deleting lock [%s] for context %s *****", lock.getId(), 
                    ExamplePlatformPowerManager.class);
            count++;
        }
        Logging.log(LogService.LOG_DEBUG, "***** %d Locks removed *****", count);
    }
    
    /** 
     * Verifies the WakeLock ID property is correctly set.
     */
    public void testWakeLockId()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create wake lock
        WakeLock testLock = powerManager.createWakeLock(TestPowerManager.class, "TestCreatedLockId");
        
        assertThat(testLock.getId(), is("TestCreatedLockId"));
    }
    
    /**
     * Verifies the WakeLock context is properly set.
     */
    public void testWakeLockContext()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create wake lock
        WakeLock testLock = powerManager.createWakeLock(TestPowerManager.class, "TestContextLockId");
        
        assertThat(testLock.getContext(), is((Object)TestPowerManager.class));
    }
    
    /**
     * Verifies that a WakeLock can be created for a scheduled time
     */
    public void testScheduledWakeLock() throws InterruptedException
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create wake lock
        WakeLock testLock = powerManager.createWakeLock(TestPowerManager.class, "TestScheduledLockId");
        long futureWakeTimeMs = m_Calendar.getTimeInMillis() + (10 * 1000); // current time plus 10 seconds in ms
        Date wakeTime = new Date(futureWakeTimeMs);
        
        // schedule lock for a future time
        testLock.scheduleWakeTime(wakeTime);
        
        // Check logged method call
        ExamplePowerManagerMethodLog[] pDataColl = m_PlatformPowerManager.getMethodLog();
        assertThat(pDataColl.length, is(1));
        ExamplePowerManagerMethodLog log = pDataColl[0];
        outputLogVaules(log);          
        assertThat(log.getWakeLockId(), is("TestScheduledLockId"));
        assertThat(log.getCalledMethod(), is(ExamplePowerManagerMethodLog.MethodCalled.Activate));
        assertThat(log.getStartTimeMs(), is(futureWakeTimeMs));
    }
    
    /**
     * Verifies that a WakeLock can be created given the source {@link mil.dod.th.core.factory.FactoryObject} AND
     * later retrieved by the asset type.
     */
    public void testCreateWakeLockWithFactoryObject() throws IllegalArgumentException, AssetException
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, notNullValue());
        
        // fetch AssetDirectoryService to create factory object
        AssetDirectoryService assetDirService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirService, is(notNullValue()));
        
        // Create test factory object
        Asset sourceObject = assetDirService.createAsset(ExampleAsset.class.getName());
        
        try
        {
            // Create WakeLock (created by asset during activation)
            AssetUtils.activateAsset(m_Context, sourceObject, 5);
            
            // Check that the FactoryObject created lock is present in the WakeLock list retrieved by context.
            Set<WakeLock> currentLocks = powerManager.getWakeLocks(ExampleAsset.class, WakeLockState.Inactive);
            printWakeLocks(currentLocks);
            // make sure there is at least 1, might be more if other ExampleAssets have been activated
            assertThat("FactoryObject wake locks not found", currentLocks.size() >= 1);
        }
        finally
        {
            // can't remove all assets as this class runs after the test prep for 2nd run class (but during the first 
            // run)
            AssetUtils.deleteAsset(m_Context, sourceObject);
        }
    }
    
    /**
     * Verifies that a WakeLock can be created to be active indefinitely
     */
    public void testCreateWakeLockIndefinite()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create wake lock
        WakeLock testLock = powerManager.createWakeLock(TestPowerManager.class, "TestCreatedIndefLock");
        
        // Activate it indefinitely
        testLock.activate();  
        
        // Check logged method call
        ExamplePowerManagerMethodLog[] pDataColl = m_PlatformPowerManager.getMethodLog();
        assertThat(pDataColl.length, is(1));
        ExamplePowerManagerMethodLog log = pDataColl[0];
        outputLogVaules(log);          
        assertThat(log.getWakeLockId(), is("TestCreatedIndefLock"));
        assertThat(log.getCalledMethod(), is(ExamplePowerManagerMethodLog.MethodCalled.Activate));
        assertThat(log.getEndTimeMs(), is(PlatformPowerManager.INDEFINITE));
        
        // Check that lock has been made active
        Set<WakeLock> activeLocks = powerManager.getWakeLocks(WakeLockState.Active);
        assertThat(activeLocks, hasItem(testLock));               
    }
    
    /**
     * Verifies that a WakeLock can be created to be active for a given duration
     */
    public void testCreateWakeLockForDuration()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create wake lock
        WakeLock testLock = powerManager.createWakeLock(TestPowerManager.class, "TestCreatedDurationLock");
        
        // Activate it for a certain duration
        long lockAwakeDuration = 10;
        long endDateInMs = m_Calendar.getTimeInMillis() + (lockAwakeDuration * 1000); // lock duration in ms
        testLock.activate(lockAwakeDuration, TimeUnit.SECONDS);
        
        // Check logged method call
        ExamplePowerManagerMethodLog[] pDataColl = m_PlatformPowerManager.getMethodLog();
        assertThat(pDataColl.length, is(1));
        ExamplePowerManagerMethodLog log = pDataColl[0];
        outputLogVaules(log);      
        assertThat(log.getWakeLockId(), is("TestCreatedDurationLock"));  
        assertThat(log.getCalledMethod(), is(ExamplePowerManagerMethodLog.MethodCalled.Activate));
        assertThat("End time not within range",
                log.getEndTimeMs() > (endDateInMs - 200) && log.getEndTimeMs() < (endDateInMs + 200));
        
        // Check that lock has been made active
        Set<WakeLock> activeLocks = powerManager.getWakeLocks(WakeLockState.Active);
        assertThat(activeLocks, hasItem(testLock));               
    }
    
    /**
     * Verifies that a WakeLock can be created to be active until a given time
     */
    public void testCreateWakeLockUntilTime()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create wake lock
        WakeLock testLock = powerManager.createWakeLock(TestPowerManager.class, "TestCreatedUntilTimeLock");
        
        // Activate it until a certain time in the future
        long endDateInMs = m_Calendar.getTimeInMillis() + (10 * 60 * 1000); //Ten minutes from now in ms
        Date lockAwakeTime = new Date(endDateInMs);
        testLock.activate(lockAwakeTime); 
        
        // Check logged method call
        ExamplePowerManagerMethodLog[] pDataColl = m_PlatformPowerManager.getMethodLog();
        assertThat(pDataColl.length, is(1));
        ExamplePowerManagerMethodLog log = pDataColl[0];
        outputLogVaules(log);      
        assertThat(log.getWakeLockId(), is("TestCreatedUntilTimeLock")); 
        assertThat(log.getCalledMethod(), is(ExamplePowerManagerMethodLog.MethodCalled.Activate));
        assertThat(log.getEndTimeMs(), is(endDateInMs));
        
        // Check that lock has been made active
        Set<WakeLock> activeLocks = powerManager.getWakeLocks(WakeLockState.Active);     
        assertThat(activeLocks, hasItem(testLock));                
    }
    
    /**
     * Verifies that a WakeLock can be deleted
     */
    public void testDeleteWakeLock()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create test lock to delete
        WakeLock testLock = powerManager.createWakeLock(TestPowerManager.class, "TestDeleteLock");
        testLock.activate();
        
        // Check that test lock has been activated
        Set<WakeLock> activeLocks = powerManager.getWakeLocks(TestPowerManager.class, WakeLockState.Active);
        assertThat(activeLocks, hasItem(testLock));     
        
        // Delete created lock
        testLock.delete();
        
        // Check that lock has been deleted
        activeLocks = powerManager.getWakeLocks(TestPowerManager.class, WakeLockState.Any);
        assertThat(activeLocks, not(hasItem(testLock)));      
    }
    
    /**
     * Verifies that all WakeLocks are returned by the PowerManager
     */
    public void testGetWakeLocksAny()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
             
        // Create active wake lock
        WakeLock testLockActive = powerManager.createWakeLock(TestPowerManager.class, "TestAnyWakeLockActive");        
        testLockActive.activate();  
        
        // Create scheduled wake lock
        WakeLock testLockScheduled = powerManager.createWakeLock(TestPowerManager.class, "TestAnyWakeLockSchdeuled");
        Date wakeTime = new Date(m_Calendar.getTimeInMillis() + (10 * 60 * 1000)); // schedule for 10min in future
        testLockScheduled.scheduleWakeTime(wakeTime);
        
        // Create Inactive wake lock
        WakeLock testLockInactive = powerManager.createWakeLock(TestPowerManager.class, "TestAnyWakeLockInactive");
        testLockInactive.activate();
        testLockInactive.cancel();
        
        // Check that all lock are returned
        Set<WakeLock> currentLocks = powerManager.getWakeLocks(WakeLockState.Any);    
        printWakeLocks(currentLocks);
        assertThat(currentLocks, hasItem(testLockActive));
        assertThat(currentLocks, hasItem(testLockScheduled));
        assertThat(currentLocks, hasItem(testLockInactive));            
    }
    
    /**
     * Verifies that only WakeLocks in the Active state are returned by the PowerManager
     */
    public void testGetWakeLocksActive()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create active wake lock
        WakeLock testLockActive = powerManager.createWakeLock(TestPowerManager.class, "TestActiveWakeLockActive");
        testLockActive.activate();  
        
        // Create scheduled wake lock
        WakeLock testLockScheduled = powerManager.createWakeLock(TestPowerManager.class, "TestActiveWakeLockSchdeuled");
        Date wakeTime = new Date(m_Calendar.getTimeInMillis() + (10 * 60 * 1000)); // schedule for 10min in future
        testLockScheduled.scheduleWakeTime(wakeTime);
        
        // Create Inactive wake lock
        WakeLock testLockInactive = powerManager.createWakeLock(TestPowerManager.class, "TestActiveWakeLockInactive");
        testLockInactive.activate();
        testLockInactive.cancel();
        
        // Check that only the active lock is returned
        Set<WakeLock> currentLocks = powerManager.getWakeLocks(WakeLockState.Active);
        assertThat(currentLocks, hasItem(testLockActive));
        assertThat(currentLocks, not(hasItem(testLockScheduled)));
        assertThat(currentLocks, not(hasItem(testLockInactive)));               
    }
    
    /**
     * Verifies that only WakeLocks in the scheduled state are returned by the PowerManager 
     */
    public void testGetWakeLocksScheduled()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create active wake lock
        WakeLock testLockActive = powerManager.createWakeLock(TestPowerManager.class, "TestSchedWakeLockActive");
        testLockActive.activate();  
        
        // Create scheduled wake lock
        WakeLock testLockScheduled = powerManager.createWakeLock(TestPowerManager.class, "TestSchedWakeLockSchdeuled");
        Date wakeTime = new Date(m_Calendar.getTimeInMillis() + (10 * 60 * 1000)); // schedule for 10min in future
        testLockScheduled.scheduleWakeTime(wakeTime);
        
        // Create Inactive wake lock
        WakeLock testLockInactive = powerManager.createWakeLock(TestPowerManager.class, "TestSchedWakeLockInactive");
        testLockInactive.activate();
        testLockInactive.cancel();
        
        // Check that only the scheduled lock is returned
        Set<WakeLock> currentLocks = powerManager.getWakeLocks(WakeLockState.Scheduled);  
        printWakeLocks(currentLocks);
        assertThat(currentLocks, not(hasItem(testLockActive)));
        assertThat(currentLocks, hasItem(testLockScheduled));
        assertThat(currentLocks, not(hasItem(testLockInactive)));             
    }
    
    /**
     * Verifies that only WakeLocks in the Inactive state are returned by the PowerManager
     */
    public void testGetWakeLocksInactive()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create active wake lock
        WakeLock testLockActive = powerManager.createWakeLock(TestPowerManager.class, "TestInactiveWakeLockActive");
        testLockActive.activate();  
        
        // Create scheduled wake lock
        WakeLock testLockScheduled = powerManager.createWakeLock(TestPowerManager.class, 
                "TestInactiveWakeLockSchdeuled");
        Date wakeTime = new Date(m_Calendar.getTimeInMillis() + (10 * 60 * 1000)); // schedule for 10min in future
        testLockScheduled.scheduleWakeTime(wakeTime);
        
        // Create Inactive wake lock
        WakeLock testLockInactive = powerManager.createWakeLock(TestPowerManager.class, "TestInactiveWakeLockInactive");
        testLockInactive.activate();
        testLockInactive.cancel();
        
        // Check that only the inactive lock is returned
        Set<WakeLock> currentLocks = powerManager.getWakeLocks(WakeLockState.Inactive);  
        printWakeLocks(currentLocks);
        assertThat(currentLocks, not(hasItem(testLockActive)));
        assertThat(currentLocks, not(hasItem(testLockScheduled)));
        assertThat(currentLocks, hasItem(testLockInactive));              
    }
    
    /**
     * Verifies that only WakeLocks of a given context are returned by the PowerManager
     */
    public void testGetWakeLocksWithContext()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create active wake lock
        WakeLock testLock1 = powerManager.createWakeLock(TestPowerManager.class, "TestCurrentContext");        
        testLock1.activate();
        
        // Create second active wake lock
        WakeLock testLock2 = powerManager.createWakeLock(ExamplePlatformPowerManager.class, "TestOtherContext");        
        testLock2.activate(); 
               
        // Check that only the lock with context TestPowerManager is returned
        Set<WakeLock> currentLocksInContext = powerManager.getWakeLocks(TestPowerManager.class, WakeLockState.Any);
        printWakeLocks(currentLocksInContext);
        assertThat(currentLocksInContext, hasItem(testLock1));
        assertThat(currentLocksInContext, not(hasItem(testLock2)));   
        
        removeOtherLocks();
    }
    
    /**
     * Verifies that the proper WakeLock contexts are returned from the PowerManager
     */
    public void testGetWakeLockContexts()
    {
        PowerManager powerManager = ServiceUtils.getService(m_Context, PowerManager.class);
        assertThat(powerManager, is(notNullValue()));
        
        // Create active wake lock
        WakeLock testLock1 = powerManager.createWakeLock(TestPowerManager.class, "TestAllCurrentContext");        
        testLock1.activate();
        
        // Create second active wake lock
        WakeLock testLock2 = powerManager.createWakeLock(ExamplePlatformPowerManager.class, "TestAllOtherContext");
        testLock2.activate();         
        
        // Check that both contexts are returned
        Set<Class <?>> currentLockContexts = powerManager.getWakeLockContexts(); 
        printWakeLockContexts(currentLockContexts);
        assertThat(currentLockContexts, hasItem(TestPowerManager.class));
        assertThat(currentLockContexts, hasItem(ExamplePlatformPowerManager.class));       
                
        removeOtherLocks();
    }

    /**
     * Outputs the contents of an {@link ExamplePowerManagerMethodLog} to the console
     * @param log
     *      the {@link ExamplePowerManagerMethodLog} to output  
     */
    private void outputLogVaules(ExamplePowerManagerMethodLog log)
    {
        Logging.log(LogService.LOG_DEBUG, 
                "Logged values from PPM: [Called Method: %s] [WakeLockId: %s] [StartTimeMs: %d] [EndTimeMs: %d]", 
                log.getCalledMethod(), log.getWakeLockId(), log.getStartTimeMs(), log.getEndTimeMs());  
    }
    
    /** Outputs ID of a Set of WakeLocks to the logger */
    private void printWakeLocks(Set<WakeLock> wakeLocks)
    {
        for(WakeLock lock : wakeLocks)
        {
            Logging.log(LogService.LOG_DEBUG, "WakeLock ID: [%s] Context: [%s]", lock.getId(), 
                    lock.getContext().toString());
        }
    }

    /** Outputs set of contexts to the logger */
    private void printWakeLockContexts(Set<Class <?>> contexts)
    {
        for(Class <?> context : contexts)
        {
            Logging.log(LogService.LOG_DEBUG, "WakeLock Context: [%s]", context.toString());
        }
    }
}
