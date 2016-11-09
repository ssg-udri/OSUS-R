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
package mil.dod.th.ose.core.impl.pm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.pm.WakeLockState;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.service.component.ComponentFactory;

public class TestPowerManagerImpl
{
    private PowerManagerImpl m_SUT;
    private ComponentFactory m_Factory;
    private List<ComponentInfo<WakeLock>> m_ComponentList;
    private PlatformPowerManager m_PPM;

    @Before
    public void setUp() throws Exception
    {
        LoggingService logging = mock(LoggingService.class);
        m_PPM = mock(PlatformPowerManager.class);

        // mock component factory
        m_Factory = mock(ComponentFactory.class);
        m_ComponentList = ComponentFactoryMocker.mockComponents(WakeLock.class, m_Factory, 4);

        m_SUT = new PowerManagerImpl();
        m_SUT.setLoggingService(logging);
        m_SUT.setFactory(m_Factory);
        m_SUT.setPlatformPowerManager(m_PPM);
    }

    @After
    public void tearDown() throws Exception
    {
        m_SUT.unsetPlatformPowerManager(m_PPM);
    }

    /**
     * Test method for {@link PowerManagerImpl#createWakeLock(java.lang.Class, java.lang.String)} and
     * {@link PowerManagerImpl#deleteWakeLock(mil.dod.th.core.pm.WakeLock)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDeleteWakeLockByContext()
    {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> propCapture = ArgumentCaptor.forClass(Dictionary.class);

        WakeLock lock1 = m_SUT.createWakeLock(TestContext1.class, "lock1");
        assertNotNull(lock1);

        when(lock1.getId()).thenReturn("lock1");
        doReturn(TestContext1.class).when(lock1).getContext();

        verify(m_Factory).newInstance(propCapture.capture());
        assertEquals(propCapture.getValue().get(WakeLockImpl.COMPONENT_PROP_ID), "lock1");
        assertEquals(propCapture.getValue().get(WakeLockImpl.COMPONENT_PROP_CONTEXT), TestContext1.class);
        assertNull(propCapture.getValue().get(WakeLockImpl.COMPONENT_PROP_SRC_OBJ));
        assertEquals(propCapture.getValue().get(WakeLockImpl.COMPONENT_PROP_POWER_MGR), m_SUT);

        Set<WakeLock> locks = m_SUT.getWakeLocks(TestContext1.class, WakeLockState.Any);
        assertThat(locks.size(), is(1));
        assertThat(locks, contains(lock1));

        m_SUT.deleteWakeLock(lock1);
        assertThat(m_SUT.getWakeLocks(TestContext1.class, WakeLockState.Any).size(), is(0));
        verify(m_ComponentList.get(0).getInstance(), times(1)).dispose();

        // Delete lock that is not active or scheduled
        lock1 = m_SUT.createWakeLock(TestContext1.class, "lock1");
        assertNotNull(lock1);
        when(lock1.getId()).thenReturn("lock1");
        doReturn(TestContext1.class).when(lock1).getContext();
        doThrow(new IllegalStateException()).when(m_PPM).cancelWakeLock(lock1);

        m_SUT.deleteWakeLock(lock1);
        assertThat(m_SUT.getWakeLocks(TestContext1.class, WakeLockState.Any).size(), is(0));
        verify(m_ComponentList.get(0).getInstance(), times(1)).dispose();

        // Delete lock with no PlatformPowerManager
        lock1 = m_SUT.createWakeLock(TestContext1.class, "lock1");
        assertNotNull(lock1);
        when(lock1.getId()).thenReturn("lock1");
        doReturn(TestContext1.class).when(lock1).getContext();

        m_SUT.unsetPlatformPowerManager(m_PPM);
        m_SUT.deleteWakeLock(lock1);
        assertThat(m_SUT.getWakeLocks(TestContext1.class, WakeLockState.Any).size(), is(0));
        verify(m_ComponentList.get(0).getInstance(), times(1)).dispose();

        // Delete lock that's already been deleted
        try
        {
            m_SUT.deleteWakeLock(lock1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        // Delete lock with missing context
        doReturn(TestContext2.class).when(lock1).getContext();
        try
        {
            m_SUT.deleteWakeLock(lock1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    /**
     * Test method for {@link PowerManagerImpl#createWakeLock(Class, FactoryObject, String)} and
     * {@link PowerManagerImpl#deleteWakeLock(mil.dod.th.core.pm.WakeLock)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDeleteWakeLockBySourceObject()
    {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> propCapture = ArgumentCaptor.forClass(Dictionary.class);

        FactoryObject sourceObject = mockFactoryObject();
        

        WakeLock lock1 = m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject, "lock1");
        
        assertNotNull(lock1);

        when(lock1.getId()).thenReturn("lock1");
        when(lock1.getSourceObject()).thenReturn(sourceObject);
        doReturn(FactoryObjectProxy.class).when(lock1).getContext();

        verify(m_Factory).newInstance(propCapture.capture());
        assertEquals(propCapture.getValue().get(WakeLockImpl.COMPONENT_PROP_ID), "lock1");
        assertEquals(propCapture.getValue().get(WakeLockImpl.COMPONENT_PROP_CONTEXT), 
                FactoryObjectProxy.class);
        assertEquals(propCapture.getValue().get(WakeLockImpl.COMPONENT_PROP_SRC_OBJ), sourceObject);
        assertEquals(propCapture.getValue().get(WakeLockImpl.COMPONENT_PROP_POWER_MGR), m_SUT);

        Set<WakeLock> locks = m_SUT.getWakeLocks(sourceObject, WakeLockState.Any);
        assertThat(locks.size(), is(1));
        assertThat(locks, contains(lock1));

        m_SUT.deleteWakeLock(lock1);
        assertThat(m_SUT.getWakeLocks(sourceObject, WakeLockState.Any).size(), is(0));
        verify(m_ComponentList.get(0).getInstance(), times(1)).dispose();

        // Delete lock that is not active or scheduled
        lock1 = m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject, "lock1");
        assertNotNull(lock1);
        when(lock1.getId()).thenReturn("lock1");
        when(lock1.getSourceObject()).thenReturn(sourceObject);
        doReturn(FactoryObjectProxy.class).when(lock1).getContext();
        doThrow(new IllegalStateException()).when(m_PPM).cancelWakeLock(lock1);

        m_SUT.deleteWakeLock(lock1);
        assertThat(m_SUT.getWakeLocks(sourceObject, WakeLockState.Any).size(), is(0));
        verify(m_ComponentList.get(0).getInstance(), times(1)).dispose();

        // Delete lock with no PlatformPowerManager
        lock1 = m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject, "lock1");
        assertNotNull(lock1);
        when(lock1.getId()).thenReturn("lock1");
        when(lock1.getSourceObject()).thenReturn(sourceObject);
        doReturn(FactoryObjectProxy.class).when(lock1).getContext();
        
        m_SUT.unsetPlatformPowerManager(m_PPM);
        m_SUT.deleteWakeLock(lock1);
        assertThat(m_SUT.getWakeLocks(sourceObject, WakeLockState.Any).size(), is(0));
        verify(m_ComponentList.get(0).getInstance(), times(1)).dispose();

        // Delete lock that's already been deleted
        try
        {
            m_SUT.deleteWakeLock(lock1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        // Delete lock with missing source object
        FactoryObject sourceObject2 = mock(FactoryObject.class);
        when(lock1.getSourceObject()).thenReturn(sourceObject2);
        try
        {
            m_SUT.deleteWakeLock(lock1);
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    /**
     * Test method for {@link PowerManagerImpl#createWakeLock(java.lang.Class, java.lang.String)} when a wake lock
     * already exists for a given context or source object and ID.
     */
    @Test
    public void testCreateWakeLockExisting()
    {
        m_SUT.createWakeLock(TestContext1.class, "lock1");

        try
        {
            m_SUT.createWakeLock(TestContext1.class, "lock1");
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }

        FactoryObject sourceObject = mockFactoryObject();
        m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject, "lock1");

        try
        {
            m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject, "lock1");
            fail("Expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }
    
    /**
     * Test method for {@link PowerManagerImpl#deleteWakeLock(mil.dod.th.core.pm.WakeLock)} when the last wake lock
     * has been removed for a context or source object.
     */
    @Test
    public void testDeleteWakeLockRemoveContext()
    {
        WakeLock lock1 = m_SUT.createWakeLock(TestContext1.class, "lock1");
        when(lock1.getId()).thenReturn("lock1");
        doReturn(TestContext1.class).when(lock1).getContext();

        WakeLock lock2 = m_SUT.createWakeLock(TestContext1.class, "lock2");
        when(lock2.getId()).thenReturn("lock2");
        doReturn(TestContext1.class).when(lock2).getContext();

        WakeLock lock3 = m_SUT.createWakeLock(TestContext2.class, "lock3");
        when(lock3.getId()).thenReturn("lock3");
        doReturn(TestContext2.class).when(lock3).getContext();

        FactoryObject sourceObject = mockFactoryObject();
        WakeLock lock4 = m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject, "lock4");
        when(lock4.getId()).thenReturn("lock4");
        when(lock4.getSourceObject()).thenReturn(sourceObject);
        doReturn(FactoryObjectProxy.class).when(lock4).getContext();

        assertThat(m_SUT.getWakeLockContexts(), hasSize(3));

        m_SUT.deleteWakeLock(lock1);

        assertThat(m_SUT.getWakeLockContexts(), hasSize(3));

        m_SUT.deleteWakeLock(lock2);

        assertThat(m_SUT.getWakeLockContexts(), hasSize(2));

        m_SUT.deleteWakeLock(lock4);

        assertThat(m_SUT.getWakeLockContexts(), hasSize(1));
    }

    /**
     * Test method for {@link PowerManagerImpl#getWakeLocks(mil.dod.th.core.pm.WakeLockState)} when the
     * {@link PlatformPowerManager} is available.
     */
    @Test
    public void testGetWakeLocksWakeLockState()
    {
        WakeLock lock1 = m_SUT.createWakeLock(TestContext1.class, "lock1");
        when(lock1.getId()).thenReturn("lock1");
        doReturn(TestContext1.class).when(lock1).getContext();

        WakeLock lock2 = m_SUT.createWakeLock(TestContext1.class, "lock2");
        when(lock2.getId()).thenReturn("lock2");
        doReturn(TestContext1.class).when(lock2).getContext();

        WakeLock lock3 = m_SUT.createWakeLock(TestContext1.class, "lock3");
        when(lock3.getId()).thenReturn("lock3");
        doReturn(TestContext1.class).when(lock3).getContext();

        FactoryObject sourceObject = mockFactoryObject();
        WakeLock lock4 = m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject, "lock4");
        when(lock4.getId()).thenReturn("lock4");
        when(lock4.getSourceObject()).thenReturn(sourceObject);

        Set<WakeLock> activeLocks = new HashSet<WakeLock>();
        activeLocks.add(lock1);
        activeLocks.add(lock4);
        Set<WakeLock> inactiveLocks = new HashSet<WakeLock>();
        inactiveLocks.add(lock2);
        Set<WakeLock> schedLocks = new HashSet<WakeLock>();
        schedLocks.add(lock3);

        when(m_PPM.getActiveWakeLocks()).thenReturn(activeLocks);
        when(m_PPM.getScheduledWakeLocks()).thenReturn(schedLocks);

        Set<WakeLock> returnedLocks = m_SUT.getWakeLocks(WakeLockState.Any);
        assertThat(returnedLocks.size(), is(4));
        assertThat(returnedLocks, hasItem(lock1));
        assertThat(returnedLocks, hasItem(lock2));
        assertThat(returnedLocks, hasItem(lock3));
        assertThat(returnedLocks, hasItem(lock4));

        returnedLocks = m_SUT.getWakeLocks(WakeLockState.Active);
        assertThat(returnedLocks.size(), is(2));
        assertThat(returnedLocks, hasItem(lock1));
        assertThat(returnedLocks, hasItem(lock4));

        returnedLocks = m_SUT.getWakeLocks(WakeLockState.Inactive);
        assertThat(returnedLocks.size(), is(1));
        assertThat(returnedLocks, hasItem(lock2));

        returnedLocks = m_SUT.getWakeLocks(WakeLockState.Scheduled);
        assertThat(returnedLocks.size(), is(1));
        assertThat(returnedLocks, hasItem(lock3));

        // Verify with no PlatformPowerManager
        m_SUT.unsetPlatformPowerManager(m_PPM);

        returnedLocks = m_SUT.getWakeLocks(WakeLockState.Active);
        assertThat(returnedLocks.size(), is(0));

        returnedLocks = m_SUT.getWakeLocks(WakeLockState.Scheduled);
        assertThat(returnedLocks.size(), is(0));
    }

    /**
     * Test method for {@link PowerManagerImpl#getWakeLocks(java.lang.Class, mil.dod.th.core.pm.WakeLockState)} when the
     * {@link PlatformPowerManager} is available.
     * 
     * Each lock should be able to have the same ID since their context is different
     */
    @Test
    public void testGetWakeLocksContextWakeLockState()
    {
        WakeLock lock1 = m_SUT.createWakeLock(TestContext1.class, "lock");
        when(lock1.getId()).thenReturn("lock");
        doReturn(TestContext1.class).when(lock1).getContext();

        WakeLock lock2 = m_SUT.createWakeLock(TestContext2.class, "lock");
        when(lock2.getId()).thenReturn("lock");
        doReturn(TestContext2.class).when(lock2).getContext();

        WakeLock lock3 = m_SUT.createWakeLock(TestContext3.class, "lock");
        when(lock3.getId()).thenReturn("lock");
        doReturn(TestContext3.class).when(lock3).getContext();

        FactoryObject sourceObject = mockFactoryObject();
        WakeLock lock4 = m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject, "lock");
        when(lock4.getId()).thenReturn("lock");
        when(lock4.getSourceObject()).thenReturn(sourceObject);
        doReturn(FactoryObjectProxy.class).when(lock4).getContext();

        Set<WakeLock> activeLocks = new HashSet<WakeLock>();
        activeLocks.add(lock1);
        Set<WakeLock> inactiveLocks = new HashSet<WakeLock>();
        inactiveLocks.add(lock2);
        inactiveLocks.add(lock4);
        Set<WakeLock> schedLocks = new HashSet<WakeLock>();
        schedLocks.add(lock3);

        when(m_PPM.getActiveWakeLocks()).thenReturn(activeLocks);
        when(m_PPM.getScheduledWakeLocks()).thenReturn(schedLocks);

        Set<WakeLock> returnedLocks = m_SUT.getWakeLocks(TestContext1.class, WakeLockState.Any);
        assertThat(returnedLocks, hasItem(lock1));
        assertThat(returnedLocks.size(), is(1));
        
        returnedLocks = m_SUT.getWakeLocks(TestContext1.class, WakeLockState.Active);
        assertThat(returnedLocks, hasItem(lock1));
        assertThat(returnedLocks.size(), is(1));
        
        returnedLocks = m_SUT.getWakeLocks(TestContext2.class, WakeLockState.Inactive);
        assertThat(returnedLocks, hasItem(lock2));
        assertThat(returnedLocks.size(), is(1));
        
        returnedLocks = m_SUT.getWakeLocks(FactoryObjectProxy.class, WakeLockState.Inactive);
        assertThat(returnedLocks, hasItem(lock4));
        assertThat(returnedLocks.size(), is(1));
        
        returnedLocks = m_SUT.getWakeLocks(TestContext3.class, WakeLockState.Scheduled);
        assertThat(returnedLocks, hasItem(lock3));
        assertThat(returnedLocks.size(), is(1));
        
        // Verify with no PlatformPowerManager
        m_SUT.unsetPlatformPowerManager(m_PPM);

        returnedLocks = m_SUT.getWakeLocks(TestContext1.class, WakeLockState.Active);
        assertThat(returnedLocks.size(), is(0));

        returnedLocks = m_SUT.getWakeLocks(TestContext3.class, WakeLockState.Scheduled);
        assertThat(returnedLocks.size(), is(0));
    }
    
    /**
     * Verify there is no exception if finding wake locks by source object, but there are only non-factory object locks.
     */
    @Test
    public void testGetWakeLocksBySourceObject_OnlyContextLocks()
    {
        m_SUT.createWakeLock(TestContext1.class, "lock");
        
        FactoryObject sourceObject = mockFactoryObject();
        
        // make sure non-factory wake locks are ignored
        Set<WakeLock> returnedLocks = m_SUT.getWakeLocks(sourceObject, WakeLockState.Any);
        assertThat(returnedLocks.size(), is(0));
    }

    /**
     * Test method for {@link PowerManagerImpl#getWakeLocks(FactoryObject, WakeLockState)} when the
     * {@link PlatformPowerManager} is available.
     */
    @Test
    public void testGetWakeLocksFactoryObjectWakeLockState()
    {
        FactoryObject sourceObject1 = mockFactoryObject();
        FactoryObject sourceObject2 = mockFactoryObject();
        FactoryObject sourceObject3 = mockFactoryObject();

        // typically each source object will use the same lock ID as they don't about each other and how to make sure
        // each lock ID is different
        WakeLock lock1 = m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject1, "lock");
        when(lock1.getId()).thenReturn("lock");
        when(lock1.getSourceObject()).thenReturn(sourceObject1);

        WakeLock lock2 = m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject2, "lock");
        when(lock2.getId()).thenReturn("lock");
        when(lock2.getSourceObject()).thenReturn(sourceObject2);

        WakeLock lock3 = m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject3, "lock");
        when(lock3.getId()).thenReturn("lock");
        when(lock3.getSourceObject()).thenReturn(sourceObject3);
        
        Set<WakeLock> activeLocks = new HashSet<WakeLock>();
        activeLocks.add(lock1);
        Set<WakeLock> schedLocks = new HashSet<WakeLock>();
        schedLocks.add(lock3);

        when(m_PPM.getActiveWakeLocks()).thenReturn(activeLocks);
        when(m_PPM.getScheduledWakeLocks()).thenReturn(schedLocks);

        Set<WakeLock> returnedLocks = m_SUT.getWakeLocks(sourceObject1, WakeLockState.Any);
        assertThat(returnedLocks.size(), is(1));
        assertThat(returnedLocks, hasItem(lock1));

        returnedLocks = m_SUT.getWakeLocks(sourceObject1, WakeLockState.Active);
        assertThat(returnedLocks.size(), is(1));
        assertThat(returnedLocks, hasItem(lock1));

        returnedLocks = m_SUT.getWakeLocks(sourceObject2, WakeLockState.Inactive);
        assertThat(returnedLocks.size(), is(1));
        assertThat(returnedLocks, hasItem(lock2));

        returnedLocks = m_SUT.getWakeLocks(sourceObject3, WakeLockState.Scheduled);
        assertThat(returnedLocks.size(), is(1));
        assertThat(returnedLocks, hasItem(lock3));

        // Verify with no PlatformPowerManager
        m_SUT.unsetPlatformPowerManager(m_PPM);

        returnedLocks = m_SUT.getWakeLocks(sourceObject1, WakeLockState.Active);
        assertThat(returnedLocks.size(), is(0));

        returnedLocks = m_SUT.getWakeLocks(sourceObject3, WakeLockState.Scheduled);
        assertThat(returnedLocks.size(), is(0));
    }

    /**
     * Test method for {@link PowerManagerImpl#getWakeLockContexts()}.
     */
    @Test
    public void testGetWakeLockContexts()
    {
        m_SUT.createWakeLock(TestContext1.class, "lock1");
        Set<Class<?>> contexts = m_SUT.getWakeLockContexts();
        assertThat(contexts.size(), is(1));
        assertThat(contexts.contains(TestContext1.class), is(true));

        m_SUT.createWakeLock(TestContext2.class, "lock2");
        contexts = m_SUT.getWakeLockContexts();
        assertThat(contexts.size(), is(2));
        assertThat(contexts.contains(TestContext1.class), is(true));
        assertThat(contexts.contains(TestContext2.class), is(true));

        m_SUT.createWakeLock(TestContext3.class, "lock3");
        contexts = m_SUT.getWakeLockContexts();
        assertThat(contexts.size(), is(3));
        assertThat(contexts.contains(TestContext1.class), is(true));
        assertThat(contexts.contains(TestContext2.class), is(true));
        assertThat(contexts.contains(TestContext3.class), is(true));

        FactoryObject sourceObject = mockFactoryObject();
        m_SUT.createWakeLock(FactoryObjectProxy.class, sourceObject, "lock4");
        contexts = m_SUT.getWakeLockContexts();
        assertThat(contexts.size(), is(4));
        assertThat(contexts.contains(TestContext1.class), is(true));
        assertThat(contexts.contains(TestContext2.class), is(true));
        assertThat(contexts.contains(TestContext3.class), is(true));
        assertThat(contexts.contains(FactoryObjectProxy.class), is(true));
    }
    
    /**
     * Method to create a mocked instance of a FactoryObject.
     * @return
     *  the mocked factory object
     */
    private FactoryObject mockFactoryObject()
    {
        FactoryObject object = mock(FactoryObject.class);
        FactoryDescriptor descriptor = mock(FactoryDescriptor.class);
        when(object.getFactory()).thenReturn(descriptor);
        
        return object;
    }

    /**
     * Class used for a wake lock context.
     */
    private class TestContext1
    {
    }

    /**
     * Class used for a wake lock context.
     */
    private class TestContext2
    {
    }

    /**
     * Class used for a wake lock context.
     */
    private class TestContext3
    {
    }
}
