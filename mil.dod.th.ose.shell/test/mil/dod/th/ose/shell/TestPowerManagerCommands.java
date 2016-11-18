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
package mil.dod.th.ose.shell;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.pm.WakeLockState;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


/**
 * @author cweisenborn
 */
public class TestPowerManagerCommands
{
    private PowerManagerCommands m_SUT;
    private PowerManager m_PowerManager;
    private PlatformPowerManager m_PlatformPowerManager;
    private ConfigurationAdmin m_ConfigurationAdmin;
    private Configuration m_Configuration;
    private CommandSession m_Session;
    private PrintStream m_PrintStream;
    
    @Before
    public void setup() throws IOException
    {
        m_SUT = new PowerManagerCommands();
        
        m_PowerManager = mock(PowerManager.class);
        m_PlatformPowerManager = mock(PlatformPowerManager.class);
        m_ConfigurationAdmin = mock(ConfigurationAdmin.class);
        m_Configuration = mock(Configuration.class);
        
        m_SUT.setPowerManager(m_PowerManager);
        m_SUT.setPlatformPowerManager(m_PlatformPowerManager);
        m_SUT.setConfigurationAdmin(m_ConfigurationAdmin);
        
        when(m_ConfigurationAdmin.getConfiguration(PlatformPowerManager.class.getName(), 
                null)).thenReturn(m_Configuration);
        
        Set<WakeLock> activeLocks = getMockedWakeLocks(WakeLockState.Active);
        when(m_PowerManager.getWakeLocks(WakeLockState.Active))
            .thenReturn(activeLocks);
        
        Set<WakeLock> inactiveLocks = getMockedWakeLocks(WakeLockState.Inactive);
        when(m_PowerManager.getWakeLocks(WakeLockState.Inactive))
            .thenReturn(inactiveLocks);
        
        Set<WakeLock> scheduledLocks = getMockedWakeLocks(WakeLockState.Scheduled);
        when(m_PowerManager.getWakeLocks(WakeLockState.Scheduled))
            .thenReturn(scheduledLocks);
        
        Set<WakeLock> allLocks = getMockedWakeLocks(WakeLockState.Any);
        when(m_PowerManager.getWakeLocks(WakeLockState.Any))
            .thenReturn(allLocks);
        
        Set<Class<?>> contexts = new HashSet<>();
        contexts.add(ActiveContextClass.class);
        when(m_PowerManager.getWakeLockContexts()).thenReturn(contexts);
        
        m_Session = mock(CommandSession.class);
        m_PrintStream = mock(PrintStream.class);
        
        when(m_Session.getConsole()).thenReturn(m_PrintStream);
    }
    
    /**
     * Verify that correct active locks are returned.
     */
    @Test
    public void testGetActiveWakeLocks()
    {
        Set<WakeLock> activeLocks = m_SUT.getWakeLocks("Active");
        
        assertThat(activeLocks.size(), is(2));
        
        List<String> lockIds = new ArrayList<String>();
        lockIds.add("activeLock1");
        lockIds.add("activeLock2");
        verifyLocks(lockIds, activeLocks);
    }
    
    /**
     * Verify that correct inactive locks are returned.
     */
    @Test
    public void testGetInactiveWakeLocks()
    {
        Set<WakeLock> inactiveLocks = m_SUT.getWakeLocks("Inactive");
        assertThat(inactiveLocks.size(), is(1));
        
        List<String> lockIds = new ArrayList<>();
        lockIds.add("inactiveLock");
        
        verifyLocks(lockIds, inactiveLocks);
    }
    
    /**
     * Verify that correct scheduled locks are returned.
     */
    @Test
    public void testGetScheduledWakeLocks()
    {
        Set<WakeLock> schedLocks = m_SUT.getWakeLocks("Scheduled");
        assertThat(schedLocks.size(), is(1));
        
        List<String> lockIds = new ArrayList<>();
        lockIds.add("schedLock");
        
        verifyLocks(lockIds, schedLocks);
    }
    
    /**
     * Verify that all locks are returned.
     */
    @Test
    public void testGetAnyWakeLocks()
    {
        Set<WakeLock> anyLocks = m_SUT.getWakeLocks("Any");
        assertThat(anyLocks.size(), is(4));
        
        List<String> lockIds = new ArrayList<>();
        lockIds.add("activeLock1");
        lockIds.add("activeLock2");
        lockIds.add("schedLock");
        lockIds.add("inactiveLock");
        
        verifyLocks(lockIds, anyLocks);
    }
    
    /**
     * Verify that only locks for a given factory object are returned.
     */
    @Test
    public void testGetWakeLockForFactoryObject()
    {
        FactoryObject factoryObj = mock(FactoryObject.class);
        Set<WakeLock> locksToReturn = new HashSet<>();
        locksToReturn.add(mockWakeLock("id1", factoryObj));
        locksToReturn.add(mockWakeLock("id2", factoryObj));
        
        when(m_PowerManager.getWakeLocks(factoryObj, WakeLockState.Any)).thenReturn(locksToReturn);
        
        WakeLock lock = m_SUT.getWakeLock(m_Session, factoryObj, "id1");
        assertThat(lock, notNullValue());
        assertThat(lock.getId(), is("id1"));
        assertThat(lock.getSourceObject(), is(factoryObj));
    }
    
    /**
     * Verify that command session outputs the correct error message if no
     * wake locks can be found for a given factory object.
     */
    @Test
    public void testGetWakeLockForFactoryObjNoWakeLocks()
    {
        FactoryObject factoryObj = mock(FactoryObject.class);
        when(factoryObj.getName()).thenReturn("name");
        Set<WakeLock> locksToReturn = new HashSet<>();
        
        when(m_PowerManager.getWakeLocks(factoryObj, WakeLockState.Any)).thenReturn(locksToReturn);
        
        WakeLock lock = m_SUT.getWakeLock(m_Session, factoryObj, "id1");
        assertThat(lock, nullValue());
        
        verify(m_PrintStream).format("Cannot find a WakeLock with the given factory object %s and lock id %s", 
                "name", "id1");        
    }
    
    /**
     * Verify that command session outputs the correct error message if no wake lock can be found 
     * for the given id.
     */
    @Test
    public void testGetWakeLockForFactoryObjectNoMatchingId()
    {
        FactoryObject factoryObj = mock(FactoryObject.class);
        when(factoryObj.getName()).thenReturn("name");
        Set<WakeLock> locksToReturn = new HashSet<>();
        locksToReturn.add(mockWakeLock("id1", factoryObj));
        locksToReturn.add(mockWakeLock("id2", factoryObj));
        
        when(m_PowerManager.getWakeLocks(factoryObj, WakeLockState.Any)).thenReturn(locksToReturn);
        
        WakeLock lock = m_SUT.getWakeLock(m_Session, factoryObj, "wrongid");
        assertThat(lock, nullValue());
        verify(m_PrintStream).format("Cannot find a WakeLock with the given factory object %s and lock id %s", 
                "name", "wrongid");
    }
    
    /**
     * Verify that the correct wake lock is returned.
     */
    @Test
    public void testGetWakeLock()
    {
        Set<WakeLock> locks = getMockedWakeLocks(WakeLockState.Any);
        when(m_PowerManager.getWakeLocks(ActiveContextClass.class, WakeLockState.Any)).thenReturn(locks);
        Set<WakeLock> returnedLocks = m_SUT.getWakeLocksByContext(m_Session,
                ActiveContextClass.class.getName(), "activeLock1");
        verify(m_PowerManager).getWakeLocks(ActiveContextClass.class, WakeLockState.Any);
        
        assertThat(returnedLocks, notNullValue());
        assertThat(returnedLocks.size(), is(1));
        
        Object[] array = returnedLocks.toArray();
        WakeLock lock = (WakeLock)array[0];
        assertThat(lock.getId(), is("activeLock1"));
        verify(m_PrintStream, never()).format(anyString(), anyVararg());
    }
    
    /**
     * Verify that with no contexts a null value is returned.
     */
    @Test
    public void testGetWakeLockByContextNoContexts()
    {
        when(m_PowerManager.getWakeLockContexts()).thenReturn(new HashSet<Class<?>>());
        Set<WakeLock> returnedLocks = m_SUT.getWakeLocksByContext(m_Session, 
                ActiveContextClass.class.getName(), "activeLock1");
        
        assertThat(returnedLocks, notNullValue());
        assertThat(returnedLocks.size(), is(0));
        verify(m_PrintStream).format(anyString(), anyVararg());
    }
    
    /**
     * Verify that no wakelock is returned if context does not match.
     */
    @Test
    public void testGetWakeLockByContextWrongClass()
    {
        Set<WakeLock> locks = getMockedWakeLocks(WakeLockState.Any);
        when(m_PowerManager.getWakeLocks(ActiveContextClass.class, WakeLockState.Any)).thenReturn(locks);
        Set<WakeLock> returnedLocks = m_SUT.getWakeLocksByContext(m_Session, String.class.getName(), "activeLock1");
        verify(m_PowerManager, never()).getWakeLocks(ActiveContextClass.class, WakeLockState.Any);
        
        assertThat(returnedLocks, notNullValue());
        assertThat(returnedLocks.size(), is(0));
        verify(m_PrintStream).format(anyString(), anyVararg());
    }
    
    /**
     * Verify that no wakelock is returned if the id does not match.
     */
    @Test
    public void testGetWakeLockByContextWrongId()
    {
        Set<WakeLock> locks = getMockedWakeLocks(WakeLockState.Any);
        when(m_PowerManager.getWakeLocks(ActiveContextClass.class, WakeLockState.Any)).thenReturn(locks);
        Set<WakeLock> returnedLocks = m_SUT.getWakeLocksByContext(m_Session, 
                ActiveContextClass.class.getName(), "noId");
        verify(m_PowerManager).getWakeLocks(ActiveContextClass.class, WakeLockState.Any);
        
        assertThat(returnedLocks, notNullValue());
        assertThat(returnedLocks.size(), is(0));
        verify(m_PrintStream).format(anyString(), anyVararg());
    }
    
    /**
     * Verify that the correct contexts are returned.
     */
    @Test
    public void testGetWakeLockContexts()
    {
        Set<Class<?>> contexts = m_SUT.getWakeLockContexts();
        assertThat(contexts.size(), is(1));
        
        for (Class<?> context : contexts)
        {
            assertThat(context.getCanonicalName(), is(ActiveContextClass.class.getCanonicalName()));
        }
    }
    
    /**
     * Verify that the info message is correctly printed.
     */
    @Test
    public void testInfo() throws IOException
    {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(PlatformPowerManager.CONFIG_PROP_ENABLED, true);
        when(m_Configuration.getProperties()).thenReturn(dict);
        
        when(m_PlatformPowerManager.getBatteryAmpHoursRem()).thenReturn(10);
        when(m_PlatformPowerManager.getBatteryVoltage()).thenReturn(20);
        
        m_SUT.info(m_Session);
        
        verify(m_PrintStream).format("Overall PlatformPowerManager status: %s%n%n\t"
                + "Enabled: %s%n\tPlatformPowerManager Present: %s%n%n", "Enabled", "true", "Yes");
        
        verify(m_PrintStream).format("Battery Remaining: %s%n%n", "10 amp-hours");
        verify(m_PrintStream).format("Battery Voltage: %s%n%n", "20 mV");
    }
    
    /**
     * Verify that the info message is correctly printed based on configuration not being enabled.
     */
    @Test
    public void testInfoNotEnabled() throws IOException
    {
        when(m_Configuration.getProperties()).thenReturn(null);
        
        when(m_PlatformPowerManager.getBatteryAmpHoursRem()).thenReturn(10);
        when(m_PlatformPowerManager.getBatteryVoltage()).thenReturn(20);
        
        m_SUT.info(m_Session);
        
        verify(m_PrintStream).format("Overall PlatformPowerManager status: %s%n%n\t"
                + "Enabled: %s%n\tPlatformPowerManager Present: %s%n%n", "Enabled", "No Property Exists", "Yes");
        
        verify(m_PrintStream).format("Battery Remaining: %s%n%n", "10 amp-hours");
        verify(m_PrintStream).format("Battery Voltage: %s%n%n", "20 mV");
    }
    
    /**
     * Verify that the info message is correctly printed based on PPM not being present.
     */
    @Test
    public void testInfoPPMNotEnabled() throws IOException
    {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(PlatformPowerManager.CONFIG_PROP_ENABLED, false);
        when(m_Configuration.getProperties()).thenReturn(dict);
        
        m_SUT.setPlatformPowerManager(null);
        
        when(m_PlatformPowerManager.getBatteryAmpHoursRem()).thenReturn(10);
        when(m_PlatformPowerManager.getBatteryVoltage()).thenReturn(20);
        
        m_SUT.info(m_Session);
        
        verify(m_PrintStream).format("Overall PlatformPowerManager status: %s%n%n\t"
                + "Enabled: %s%n\tPlatformPowerManager Present: %s%n%n", "Disabled", "false", "No");
        
        verify(m_PrintStream).format("Battery Remaining: %s%n%n", "N/A");
        verify(m_PrintStream).format("Battery Voltage: %s%n%n", "N/A");
    }
    
    /**
     * Verify that the platform power manager can be enabled via the configuration admin service
     * if there is not an existing configuration.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testEnableCommandNoConfigProperties() throws IOException
    {
        when(m_Configuration.getProperties()).thenReturn(null);
        
        m_SUT.enable(m_Session);
        verify(m_ConfigurationAdmin).getConfiguration(PlatformPowerManager.class.getName(), null);
        
        ArgumentCaptor<Dictionary> mapCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_Configuration).update(mapCaptor.capture());
        
        Dictionary<String, Object> capDict = mapCaptor.getValue();
        assertThat(capDict, notNullValue());
        assertThat((boolean)capDict.get(PlatformPowerManager.CONFIG_PROP_ENABLED), is(true));
    }
    
    /**
     * Verify that the platform power manager can be enabled via the configuration admin service
     * if there is an existing configuration.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testEnableCommandWithConfigProperties() throws IOException
    {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(PlatformPowerManager.CONFIG_PROP_ENABLED, false);
        when(m_Configuration.getProperties()).thenReturn(dict);
        
        m_SUT.enable(m_Session);
        verify(m_ConfigurationAdmin).getConfiguration(PlatformPowerManager.class.getName(), null);
        
        ArgumentCaptor<Dictionary> mapCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_Configuration).update(mapCaptor.capture());
        
        Dictionary<String, Object> capDict = mapCaptor.getValue();
        assertThat(capDict, notNullValue());
        assertThat((boolean)capDict.get(PlatformPowerManager.CONFIG_PROP_ENABLED), is(true));
    }
    
    /**
     * Verify that the configuration for the platform power manager is not updated if 
     * the enabled property is already true.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEnableCommandAlreadyEnabled() throws IOException
    {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(PlatformPowerManager.CONFIG_PROP_ENABLED, true);
        when(m_Configuration.getProperties()).thenReturn(dict);
        
        m_SUT.enable(m_Session);
        verify(m_ConfigurationAdmin).getConfiguration(PlatformPowerManager.class.getName(), null);
        
        verify(m_Configuration, never()).update(Mockito.any(Dictionary.class));
        
        verify(m_PrintStream).format("The PlatformPowerManager is already enabled.%n");
    }
    
    /**
     * Verify that the correct output is printed if PPM is not present.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testEnableCommandPPMMissing() throws IOException
    {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(PlatformPowerManager.CONFIG_PROP_ENABLED, false);
        when(m_Configuration.getProperties()).thenReturn(dict);
        
        m_SUT.setPlatformPowerManager(null);
        
        m_SUT.enable(m_Session);
        verify(m_ConfigurationAdmin).getConfiguration(PlatformPowerManager.class.getName(), null);
        
        verify(m_Configuration).update(Mockito.any(Dictionary.class));
        
        verify(m_PrintStream).format("Warning!! The configuration property has been set to enabled "
                + "but the PlatformPowerManager is not present%n");
    }
    
    /**
     * Verify that correct output is printed if PPM is not present.
     */
    @SuppressWarnings ("unchecked")
    @Test
    public void testDisableCommandWithNoPPM() throws IOException
    {
        when(m_Configuration.getProperties()).thenReturn(null);
        m_SUT.setPlatformPowerManager(null);
        
        m_SUT.disable(m_Session);
        verify(m_ConfigurationAdmin).getConfiguration(PlatformPowerManager.class.getName(), null);
        
        verify(m_ConfigurationAdmin).getConfiguration(PlatformPowerManager.class.getName(), null);
        
        verify(m_Configuration).update(Mockito.any(Dictionary.class));
        
        verify(m_PrintStream).format("Warning!! The configuration property has been set to disabled "
                + "but the PlatformPowerManager is not present%n");
    }
    
    /**
     * Verify that the platform power manager can be disabled via the configuration admin service
     * if no configuration already exists.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testDisableCommandWithNoProperties() throws IOException
    {
        when(m_Configuration.getProperties()).thenReturn(null);
        
        m_SUT.disable(m_Session);
        verify(m_ConfigurationAdmin).getConfiguration(PlatformPowerManager.class.getName(), null);
        
        ArgumentCaptor<Dictionary> mapCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_Configuration).update(mapCaptor.capture());
        
        Dictionary<String, Object> capDict = mapCaptor.getValue();
        assertThat(capDict, notNullValue());
        assertThat((boolean)capDict.get(PlatformPowerManager.CONFIG_PROP_ENABLED), is(false));
    }
    
    /**
     * Verify that the platform power manager can be disabled via the configuration admin service
     * if a configuration exists.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testDisableCommandWithProperties() throws IOException
    {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(PlatformPowerManager.CONFIG_PROP_ENABLED, true);
        when(m_Configuration.getProperties()).thenReturn(dict);
        
        m_SUT.disable(m_Session);
        verify(m_ConfigurationAdmin).getConfiguration(PlatformPowerManager.class.getName(), null);
        
        ArgumentCaptor<Dictionary> mapCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_Configuration).update(mapCaptor.capture());
        
        Dictionary<String, Object> capDict = mapCaptor.getValue();
        assertThat(capDict, notNullValue());
        assertThat((boolean)capDict.get(PlatformPowerManager.CONFIG_PROP_ENABLED), is(false));
    }
    
    /**
     * Verify that the configuration is not updated if the platform power manager is already disabled.
     */
    @SuppressWarnings({"unchecked"})
    @Test
    public void testDisableCommandAlreadyDisabled() throws IOException
    {
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put(PlatformPowerManager.CONFIG_PROP_ENABLED, false);
        when(m_Configuration.getProperties()).thenReturn(dict);
        
        m_SUT.disable(m_Session);
        verify(m_ConfigurationAdmin).getConfiguration(PlatformPowerManager.class.getName(), null);
        
        verify(m_Configuration, never()).update(Mockito.any(Dictionary.class));
        
        verify(m_PrintStream).format("The PlatformPowerManager is already disabled.%n");
    }
    
    /**
     * Verify that the create wake lock command returns the expected wake lock.
     */
    @Test
    public void testCreateWakeLock()
    {
        String lockId = "test";
        WakeLock lock = mockWakeLock(lockId);
        when(m_PowerManager.createWakeLock(m_SUT.getClass(), lockId)).thenReturn(lock);
        
        WakeLock createdLock = m_SUT.createWakeLock(lockId);
        assertThat(createdLock, is(lock));
    }
    
    /**
     * Verify that the new shell wake lock is created.
     */
    @Test
    public void testActivate()
    {
        m_SUT.activate();
        verify(m_PowerManager).createWakeLock(PowerManagerCommands.class, "shellWakeLock");
    }
    
    /**
     * Verify that the shell wake lock is deleted.
     */
    @Test
    public void testDeactivate()
    {
        WakeLock shellLock = mockWakeLock("shellWakeLock");
        when(m_PowerManager.createWakeLock(m_SUT.getClass(), "shellWakeLock")).thenReturn(shellLock);
        m_SUT.activate();
      
        m_SUT.deactivate();
        verify(shellLock, times(1)).delete();
    }
    
    /**
     * Verify the shell wake lock is activated.
     */
    @Test
    public void testStartWL()
    {
        WakeLock shellLock = mockWakeLock("shellWakeLock");
        when(m_PowerManager.createWakeLock(m_SUT.getClass(), "shellWakeLock")).thenReturn(shellLock);
        m_SUT.activate();
      
        m_SUT.startwl();
        verify(shellLock, times(1)).activate();
    }
    
    /**
     * Verify the shell wake lock is deactivated.
     */
    @Test
    public void testStopWL()
    {
        WakeLock shellLock = mockWakeLock("shellWakeLock");
        when(m_PowerManager.createWakeLock(m_SUT.getClass(), "shellWakeLock")).thenReturn(shellLock);
        m_SUT.activate();
      
        m_SUT.stopwl();
        verify(shellLock, times(1)).cancel();
    }
    
    /**
     * Returns a set of WakeLocks based on the given state.
     * @param state
     *  the state in which the desired WakeLocks are to be
     * @return
     *  the set of WakeLocks that match that state.
     */
    private Set<WakeLock> getMockedWakeLocks(WakeLockState state)
    {
        WakeLock activeLock1 = mockWakeLock("activeLock1");
        
        doReturn(ActiveContextClass.class).when(activeLock1).getContext();
        
        WakeLock activeLock2 = mockWakeLock("activeLock2");
        
        WakeLock schedLock = mockWakeLock("schedLock");
        WakeLock inactiveLock = mockWakeLock("inactiveLock");
        
        Set<WakeLock> setToReturn = new HashSet<WakeLock>();
        switch(state)
        {
            case Active:
                setToReturn.add(activeLock1);
                setToReturn.add(activeLock2);
                break;
            case Inactive:
                setToReturn.add(inactiveLock);
                break;
            case Scheduled:
                setToReturn.add(schedLock);
                break;
            case Any:
                setToReturn.add(inactiveLock);
                setToReturn.add(schedLock);
                setToReturn.add(activeLock2);
                setToReturn.add(activeLock1);
                break;
        }
        
        return setToReturn;
    }
    
    /**
     * Creates a mocked WakeLock
     * @param id
     *  the id of the WakeLock 
     * @return
     *  the mocked WakeLock
     */
    private WakeLock mockWakeLock(String id)
    {
        WakeLock lock = mock(WakeLock.class);
        when(lock.getId()).thenReturn(id);
        
        return lock;
    }
    
    /**
     * Creates a mocked WakeLock
     * @param id
     *  the id of the WakeLock 
     * @param obj
     *  the factory object associated with this wakelock
     * @return
     *  the mocked WakeLock
     */
    private WakeLock mockWakeLock(String id, FactoryObject obj)
    {
        WakeLock lock = mockWakeLock(id);
        when(lock.getSourceObject()).thenReturn(obj);
        return lock;
    }
    
    /**
     * Method used to verify that a list of given locks contains the expected ones.
     * @param lockIds
     *  list of lock ids that are expected to be in the list of foundLocks
     * @param foundLocks
     *  list of the given locks
     */
    private void verifyLocks(List<String> lockIds, Set<WakeLock> foundLocks)
    {
        int foundCount = 0;
        for (WakeLock foundLock : foundLocks)
        {
            if (lockIds.contains(foundLock.getId()))
            {
                foundCount++;
            }
        }
        
        assertThat(foundCount, is(lockIds.size()));
    }
    
    /**
     * Class used for an active context.
     */
    private class ActiveContextClass
    {
        
    }
}
