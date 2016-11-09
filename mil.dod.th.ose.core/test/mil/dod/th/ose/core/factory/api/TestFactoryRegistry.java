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
package mil.dod.th.ose.core.factory.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.ose.core.ConfigurationAdminMocker;
import mil.dod.th.ose.core.factory.api.FactoryRegistry.DependencyState;
import mil.dod.th.ose.core.factory.api.FactoryRegistry.FactoryRegistryEventHandler;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.test.NewInstanceAnswer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 * This class tests the generic functionality of the {@link FactoryRegistry}.
 * @author matt
 *
 */
public class TestFactoryRegistry
{
    private static final String FACTORY_PID = "factory-pid";

    private FactoryRegistry<FactoryObjectInternal> m_SUT;
    private FactoryObjectDataManager m_FactoryObjectDataManager;
    private FactoryServiceUtils m_Utils;
    private DirectoryService m_FactoryService;
    private ConfigurationAdmin m_ConfigAdmin;
    private EventAdmin m_EventAdmin;
    private PowerManagerInternal m_PowerInternal;
    private FactoryRegistryCallback<FactoryObjectInternal> m_Callback;
    private FactoryServiceProxy<FactoryObjectInternal> m_ServiceProxy;
    private FactoryInternal m_FactoryDescriptor;
    private BundleContext m_BundleContext;
    private ServiceRegistration<EventHandler> m_EventHandlerRegistration;
    private EventHandler m_EventHandler;

    @SuppressWarnings({"unchecked"})
    @Before
    public void setUp()
    {
        m_FactoryService = mock(DirectoryService.class);
        m_FactoryObjectDataManager = mock(FactoryObjectDataManager.class);
        m_Utils = mock(FactoryServiceUtils.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_PowerInternal = mock(PowerManagerInternal.class);
        m_Callback = mock(FactoryRegistryCallback.class);
        m_ServiceProxy = mock(FactoryServiceProxy.class);
        m_FactoryDescriptor = mock(FactoryInternal.class);
        m_ConfigAdmin = ConfigurationAdminMocker.createMockConfigAdmin();
        m_BundleContext = mock(BundleContext.class);
        m_EventHandlerRegistration = mock(ServiceRegistration.class);

        m_SUT = new FactoryRegistry<FactoryObjectInternal>();

        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setFactoryServiceUtils(m_Utils);
        m_SUT.setConfigurationAdmin(m_ConfigAdmin);
        m_SUT.setPowerManagerInternal(m_PowerInternal);
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());

        when(m_BundleContext.registerService(eq(EventHandler.class),
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class))).thenReturn(m_EventHandlerRegistration);

        doReturn(FactoryObject.class).when(m_ServiceProxy).getBaseType();
        when(m_ServiceProxy.getDataManager()).thenReturn(m_FactoryObjectDataManager);

        when(m_Utils.getMetaTypeDefaults(m_FactoryDescriptor)).thenReturn(new Hashtable<String, Object>());
        when(m_FactoryDescriptor.getPid()).thenReturn(FACTORY_PID);

        m_SUT.activate(m_BundleContext);

        m_SUT.initialize(m_FactoryService, m_ServiceProxy, m_Callback);

        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext).registerService(eq(EventHandler.class), captor.capture(),
                Mockito.any(Dictionary.class));

        m_EventHandler = captor.getValue();
    }

    /**
     * Verify registering for factory obj created and updated events during activation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivation()
    {
        m_SUT.activate(m_BundleContext);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Dictionary> dictCap = ArgumentCaptor.forClass(Dictionary.class);
        //twice because activation happens in setup too
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class),
                Mockito.any(FactoryRegistryEventHandler.class), dictCap.capture());

        assertThat(Arrays.asList((String[])dictCap.getValue().get(EventConstants.EVENT_TOPIC)),
            hasItems(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED,
                FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED));

    }
    /**
     * Verify that the deactivate method unregisters the established event handlers and notifies the
     * callback of shutdown.
     */
    @Test
    public void testDeactivate()
    {
        m_SUT.deactivate();

        verify(m_EventHandlerRegistration).unregister();
    }

    /**
     * Verify that we can get a list of uuids from the objects in the registry
     */
    @Test
    public void testGetUuids() throws IllegalArgumentException, FactoryException, FactoryObjectInformationException
    {
        UUID testUuid = UUID.randomUUID();
        UUID testUuid2 = UUID.randomUUID();

        // mock an object
        ComponentInstance instance1 = mockComponentInstance(testUuid, "obj1", "pid1");
        ComponentInstance instance2 = mockComponentInstance(testUuid2, "obj2", "pid2");
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(
                instance1, instance2);
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);

        FactoryObjectProxy testObjProxy2 = mock(FactoryObjectProxy.class);

        when(m_FactoryDescriptor.create()).thenReturn(testObjProxy, testObjProxy2);

        // replay
        m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);
        m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid2);

        List<UUID> uuidList = m_SUT.getUuids();

        assertThat(uuidList.size(), is(2));
        assertThat(uuidList, hasItems(testUuid, testUuid2));
    }

    /**
     * Verify that an object is properly deleted.
     */
    @Test
    public void testDelete() throws Exception
    {
        UUID testUuid = UUID.randomUUID();
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);
        Configuration config = m_ConfigAdmin.createFactoryConfiguration(FACTORY_PID, null);
        String pid = config.getPid();

        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", pid);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        when(m_FactoryDescriptor.create()).thenReturn(testObjProxy);

        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(pid);
        when(m_ConfigAdmin.getConfiguration(anyString(), anyString())).thenReturn(config);

        // replay
        m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);

        FactoryObjectInternal internal = (FactoryObjectInternal)instance.getInstance();
        m_SUT.delete(internal);

        verify(m_Callback).onRemovedObject(internal);
        verify(config).delete();
    }

    /**
     * Verify deletion of configuration does not throw exception
     */
    @Test
    public void testDelete_ConfigAdminException() throws Exception
    {
        UUID testUuid = UUID.randomUUID();
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);
        Configuration config = m_ConfigAdmin.createFactoryConfiguration(FACTORY_PID, null);
        String pid = config.getPid();

        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", pid);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        when(m_FactoryDescriptor.create()).thenReturn(testObjProxy);

        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(pid);

        // replay
        m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);

        doThrow(new IOException()).when(config).delete();

        FactoryObjectInternal internal = (FactoryObjectInternal)instance.getInstance();
        m_SUT.delete(internal);
    }

    /**
     * Verify deletion of configuration does not throw exception if config is null
     */
    @Test
    public void testDelete_NullConfig() throws Exception
    {
        UUID testUuid = UUID.randomUUID();
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);
        Configuration config = m_ConfigAdmin.createFactoryConfiguration(FACTORY_PID, null);
        String pid = config.getPid();

        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", pid);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        when(m_FactoryDescriptor.create()).thenReturn(testObjProxy);

        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(pid);

        // replay
        m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);

        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(null);

        FactoryObjectInternal internal = (FactoryObjectInternal)instance.getInstance();
        m_SUT.delete(internal);
    }

    /**
     * Test that data isn't re-persisted if the name is already associated with the uuid
     */
    @Test
    public void testPersistObjectDataNoRename() throws IllegalArgumentException, FactoryObjectInformationException
    {
        String name = "NoChangeNeeded";
        UUID uuid = UUID.randomUUID();

        //when the name is checked for associate it with the given uuid
        when(this.m_FactoryObjectDataManager.getPersistentUuid(name)).thenReturn(uuid);

        m_SUT.persistNewObjectData(name, uuid, m_FactoryDescriptor, false);

        //verify name was not persisted because it is already stored with the correct uuid in the datastore
        verify(m_FactoryObjectDataManager, Mockito.never()).persistNewObjectData(uuid, m_FactoryDescriptor, name);
    }

    /**
     * Test the updating of a name triggers the entity to merge.
     * Verify name update and merging call.
     */
    @Test
    public void testSetNameNewEntry() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException
    {
        // mock a factory object
        UUID testUuid = UUID.randomUUID();
        FactoryObjectInternal testObjInternal = createMockFactObj(testUuid, "DefaultName", null);
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);

        // set up fake object factory
        FactoryInternal factory = mock(FactoryInternal.class);
        when(factory.create()).thenReturn(testObjProxy);

        ComponentInstance instance = mockComponentInstance(testUuid, "name", "pid");
        when(m_ServiceProxy.createFactoryObjectInternal(factory)).thenReturn(instance);

        when(m_Utils.getMetaTypeDefaults(factory)).thenReturn(new Hashtable<String, Object>());

        // replay
        m_SUT.createOrRestoreObject(factory, testUuid);

        //update the name
        m_SUT.setName(testObjInternal, "name");

        //verify new entry created with correct name
        verify(m_FactoryObjectDataManager).setName(testUuid, "name");
    }

    /**
     * Test that attempting to set a name when the name is already associated with the object does not re-persist
     * the data. It already exists!
     */
    @Test
    public void testPersistObjectDataNameAlreadyKnown() throws IllegalArgumentException,
           FactoryObjectInformationException, FactoryException
    {
        // mock an object
        UUID testUuid = UUID.randomUUID();

        ComponentInstance instance = mockComponentInstance(testUuid, "SomeName", null);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);
        FactoryObjectProxy testObjectProxy = mock(FactoryObjectProxy.class);

        when(this.m_FactoryObjectDataManager.getPersistentUuid("SomeName")).thenReturn(testUuid);

        // mock factory return (supports getting the PID)
        when(m_FactoryDescriptor.create()).thenReturn(testObjectProxy);

        // replay
        m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);
        m_SUT.setName((FactoryObjectInternal)instance.getInstance(), "SomeName");

        //verify that set name isn't called since the name is the same as mocked up above
        verify(this.m_FactoryObjectDataManager, never()).setName(Mockito.eq(testUuid), eq("SomeName"));
    }

    /**
     * Test that attempting get an object by UUID when the UUID is not associated with an object known to the factory
     * registry.
     */
    @Test
    public void testGetObjectByProductType() throws Exception
    {
        FactoryInternal factory1 = mock(FactoryInternal.class);
        FactoryInternal factory2 = mock(FactoryInternal.class);
        List<FactoryObjectInternal> factory1objects = stubFactoryObject(factory1, "factory-1-type", 2);
        stubFactoryObject(factory2, "factory-2-type", 2);

        // UUIDs passed here don't actually matter, UUID stubbed out already
        m_SUT.createOrRestoreObject(factory1, null);
        m_SUT.createOrRestoreObject(factory2, null);
        m_SUT.createOrRestoreObject(factory1, null);
        m_SUT.createOrRestoreObject(factory2, null);

        Set<FactoryObjectInternal> objects = m_SUT.getObjectsByProductType("factory-1-type");

        assertThat(objects, hasItems(factory1objects.get(0), factory1objects.get(1)));
    }

    private List<FactoryObjectInternal> stubFactoryObject(FactoryInternal factory, String productType, int count)
    {
        NewInstanceAnswer<FactoryObjectInternal> answer =
                new NewInstanceAnswer<FactoryObjectInternal>(FactoryObjectInternal.class, count);

        when(factory.getProductType()).thenReturn(productType);
        when(m_Utils.getMetaTypeDefaults(factory)).thenReturn(new Hashtable<String, Object>());
        when(m_ServiceProxy.createFactoryObjectInternal(factory)).thenAnswer(answer);

        for (FactoryObjectInternal object : answer.getObjects())
        {
            when(object.getFactory()).thenReturn(factory);
            when(object.getUuid()).thenReturn(UUID.randomUUID());
        }

        return Arrays.asList(answer.getObjects());
    }

    /**
     * Test that attempting get an object by UUID when the UUID is not associated with an object known to the factory
     * registry.
     */
    @Test
    public void testGetObjectByUuidNoKnown() throws IllegalArgumentException, FactoryException,
        FactoryObjectInformationException
    {
        //UUIDs for objects
        UUID testUuid = UUID.randomUUID();
        UUID testUuid2 = UUID.randomUUID();
        UUID notKnownUuid = UUID.randomUUID();

        //mock behavior for creating object one and two
        FactoryObjectProxy factoryObjectProxy = mock(FactoryObjectProxy.class);
        FactoryObjectProxy factoryObjectProxy2 = mock(FactoryObjectProxy.class);

        ComponentInstance instance = mockComponentInstance(testUuid);
        ComponentInstance instance2 = mockComponentInstance(testUuid2);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance, instance2);
        when(m_FactoryDescriptor.create()).thenReturn(factoryObjectProxy, factoryObjectProxy2);

        // replay
        FactoryObject objOne = m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);
        FactoryObject objTwo = m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid2);

        //verify UUIDs for object one and two
        assertThat(m_SUT.getObjectByUuid(testUuid), is(objOne));
        assertThat(m_SUT.getObjectByUuid(testUuid2), is(objTwo));

        //verify exception for unknown UUID
        try
        {
            m_SUT.getObjectByUuid(notKnownUuid);
            fail("Expecting Exception");
        }
        catch (IllegalArgumentException e)
        {
            //expecting exception
        }
    }

    /**
     *  Tests that the name of an object is set and persisted if it is a valid name.
     *  A name that is blank, null, or a duplicate is considered invalid.
     */
    @Test
    public void testPersistObjectData() throws IllegalArgumentException, FactoryException,
        FactoryObjectInformationException
    {
        // mock an object
        UUID testUuid = UUID.randomUUID();
        ComponentInstance instance = mockComponentInstance(testUuid, "DefaultName", null);
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);

        // mock another object
        UUID testUuid2 = UUID.randomUUID();
        ComponentInstance instance2 = mockComponentInstance(testUuid2, "DuplicateName", null);
        FactoryObjectProxy testObjProxy2 = mock(FactoryObjectProxy.class);

        // mock factory return (supports getting the PID)
        when(m_FactoryDescriptor.create()).thenReturn(testObjProxy, testObjProxy2);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance, instance2);

        // replay
        m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);
        m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid2);

        try
        {
            m_SUT.setName((FactoryObjectInternal)instance.getInstance(), null);
            fail("Expecting IllegalArgumentException, name should not be set to null.");
        }
        catch (final IllegalArgumentException e)
        {
            // name cannot be null
        }

        try
        {
            m_SUT.setName((FactoryObjectInternal)instance.getInstance(), "   ");
            fail("Expecting IllegalArgumentException, name cannot be blank.");
        }
        catch (final IllegalArgumentException e)
        {
            // name cannot be blank
        }

        //factory object manager behavior
        when(m_FactoryObjectDataManager.getPersistentUuid("DuplicateName")).thenReturn(testUuid2);

        try
        {
            m_SUT.setName((FactoryObjectInternal)instance.getInstance(), "DuplicateName");
            fail("Expecting IllegalArgumentException, duplicate name found.");
        }
        catch (final IllegalArgumentException e)
        {
            // name cannot be duplicate
        }

        // test that an IllegalArgumentException is not thrown if the name already exists but
        // but uniquely belongs to the object being named.
        m_SUT.setName((FactoryObjectInternal)instance.getInstance(), "DefaultName");
        verify((FactoryObjectInternal)instance.getInstance()).internalSetName("DefaultName");
        // name will not be persisted because it is already associated with that uuid in the data store

        // test that a valid, non-duplicate name is set and persisted.
        // this updates a pre-existing entry in the data store
        m_SUT.setName((FactoryObjectInternal)instance.getInstance(), "NewName");
    }

    /**
     * Test that attempting to set a name with autoRename == true
     */
    @Test
    public void testPersistObjectDataAutoRename() throws IllegalArgumentException, FactoryObjectInformationException,
        FactoryException
    {
        // mock an object
        UUID testUuid = UUID.randomUUID();
        ComponentInstance instance = mockComponentInstance(testUuid, "DefaultName", null);
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);

        // mock another object
        UUID testUuid2 = UUID.randomUUID();
        ComponentInstance instance2 = mockComponentInstance(testUuid, "DuplicateName", null);
        FactoryObjectProxy testObjProxy2 = mock(FactoryObjectProxy.class);

        when(this.m_FactoryObjectDataManager.getPersistentUuid("DuplicateName")).thenReturn(testUuid2);

        // mock factory return (supports getting the PID)
        when(m_FactoryDescriptor.create()).thenReturn(testObjProxy, testObjProxy2);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance, instance2);

        // replay
        m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);

        m_SUT.persistNewObjectData("DuplicateName", testUuid, m_FactoryDescriptor, true);

        verify(this.m_FactoryObjectDataManager).persistNewObjectData(eq(testUuid), eq(m_FactoryDescriptor),
                eq("DuplicateName" + testUuid.toString()));
    }

    /**
     * Test exception if unable to update a configuration.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateConfigurationIoException() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException, IOException
    {
        Configuration config = mock(Configuration.class);
        // mock config admin to be returned
        doReturn(config).when(m_ConfigAdmin).createFactoryConfiguration(anyString(), anyString());

        // mock a factory object
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "DefaultName", null);
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);

        // set up fake object factory
        when(m_FactoryDescriptor.getProductType()).thenReturn("product type");
        when(m_FactoryDescriptor.create()).thenReturn(testObjProxy);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        //mock exception when updating the configuration
        doThrow(new IOException("Error")).when(config).update(Mockito.any(Dictionary.class));

        //dictionary of props that will trigger a configuration to be made
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("someKey", "we like the values");

        // create object
        try
        {
            m_SUT.createNewObject(m_FactoryDescriptor, "someName", props);
            fail("Expected Exception when updating the created configuration");
        }
        catch (FactoryException e)
        {
            //expected exception
        }
    }

    /**
     * Test exception if unable to create a configuration.
     */
    @Test
    public void testCreateConfigurationIoException() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException, IOException
    {
        // mock config admin to be returned
        doThrow(new IOException()).when(m_ConfigAdmin).createFactoryConfiguration(anyString(), anyString());

        // mock a factory object
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "DefaultName", null);
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);
        UUID testUuid = UUID.randomUUID();

        // set up fake object factory
        when(m_FactoryDescriptor.create()).thenReturn(testObjProxy);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        // replay
        FactoryObject obj = m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);

        //expecting exception
        try
        {
            m_SUT.createConfiguration(testUuid, "fact_pid",(FactoryObjectInternal)obj);
            fail("Expecting Exception, config should throw IOexception.");
        }
        catch (FactoryException e)
        {
            //expecting exception
        }
    }


    /**
     * Test unsetting the PID for an already created object.
     */
    @Test
    public void testCreateWithConfigUnsetPid() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException
    {
        // mock a factory object
        UUID testUuid = UUID.randomUUID();
        ComponentInstance instance = mockComponentInstance(testUuid, "DefaultName", "some_pid");
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);

        // set up fake object factory
        when(m_FactoryDescriptor.create()).thenReturn(testObjProxy);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        // replay
        FactoryObject obj = m_SUT.createOrRestoreObject(m_FactoryDescriptor, testUuid);

        //verify PID is set
        assertThat(obj.getPid(), is("some_pid"));

        //unset Pid
        m_SUT.unAssignPidForObj((FactoryObjectInternal)obj);

        //verify pid unset
        verify((FactoryObjectInternal)instance.getInstance()).setPid(null);
        verify(m_FactoryObjectDataManager).clearPid(testUuid);
        verify(m_FactoryService).postFactoryObjectEvent(eq(FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_REMOVED),
                eq((FactoryObjectInternal)obj));
    }

    /**
     * Verify the ability to query the data manager for all factory objects for a given factory.
     */
    @Test
    public void testRestoreAllObjects() throws IllegalArgumentException, FactoryException,
        FactoryObjectInformationException
    {
        //UUIDs for objects
        UUID testUuid = UUID.randomUUID();
        UUID testUuid2 = UUID.randomUUID();

        //mock behavior for creating object one and two
        when(m_FactoryDescriptor.getProductType()).thenReturn("product type");
        ComponentInstance instance = mockComponentInstance(testUuid);
        FactoryObjectProxy factoryObjectProxy = mock(FactoryObjectProxy.class);
        ComponentInstance instance2 = mockComponentInstance(testUuid2);
        FactoryObjectProxy factoryObjectProxy2 = mock(FactoryObjectProxy.class);
        when(m_FactoryDescriptor.create()).thenReturn(factoryObjectProxy, factoryObjectProxy2);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance, instance2);

        //data manager behavior
        PersistentData dataOne = mock(PersistentData.class);
        when(dataOne.getUUID()).thenReturn(testUuid);
        PersistentData dataTwo = mock(PersistentData.class);
        when(dataTwo.getUUID()).thenReturn(testUuid2);
        //collection of data to return
        List<PersistentData> factDatas = new ArrayList<PersistentData>();
        factDatas.add(dataOne);
        factDatas.add(dataTwo);

        when(m_FactoryObjectDataManager.getAllObjectData(m_FactoryDescriptor)).thenReturn(factDatas);

        //restore the objects
        m_SUT.restoreAllObjects(m_FactoryDescriptor);

        //get the created Object for inspection
        Set<FactoryObjectInternal> objects = m_SUT.getObjects();
        assertThat(objects.size(), is(2));

        Iterator<FactoryObjectInternal> objIterator = objects.iterator();
        FactoryObjectInternal objOne = objIterator.next();
        FactoryObjectInternal objTwo = objIterator.next();

        //verify objects have been created
        verify(m_FactoryService).postFactoryObjectEvent(eq(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED),
                eq(objOne));
        verify(m_FactoryService).postFactoryObjectEvent(eq(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED),
                eq(objTwo));

        //verify objects have been initialized with restore
        if (objOne.getUuid() == testUuid)
        {
            verify(objOne).initialize(eq(m_SUT),
                    eq(factoryObjectProxy),
                    eq(m_FactoryDescriptor), eq(m_ConfigAdmin),
                    eq(m_EventAdmin), eq(m_PowerInternal),
                    eq(testUuid), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class));
            verify(objTwo).initialize(eq(m_SUT),
                    eq(factoryObjectProxy2),
                    eq(m_FactoryDescriptor),
                    eq(m_ConfigAdmin), eq(m_EventAdmin), eq(m_PowerInternal), eq(testUuid2),
                    Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class));
        }
        else
        {
            verify(objOne).initialize(eq(m_SUT),
                    eq(factoryObjectProxy2),
                    eq(m_FactoryDescriptor), eq(m_ConfigAdmin),
                    eq(m_EventAdmin), eq(m_PowerInternal),
                    eq(testUuid2), Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class));
            verify(objTwo).initialize(eq(m_SUT),
                    eq(factoryObjectProxy),
                    eq(m_FactoryDescriptor),
                    eq(m_ConfigAdmin), eq(m_EventAdmin), eq(m_PowerInternal),
                    eq(testUuid),
                    Mockito.any(String.class), Mockito.any(String.class), Mockito.any(String.class));
        }

        //check UUIDs are the expected UUIDs from the persistent data mocked out above
        UUID objOneUuid = objOne.getUuid();
        UUID objTwoUuid = objTwo.getUuid();
        assertThat(objOneUuid, is(not(objTwoUuid)));
        assertThat(objOneUuid, isOneOf(testUuid, testUuid2));
        assertThat(objTwoUuid, isOneOf(testUuid, testUuid2));
    }

    /**
     * Verify restored objects that have a missing dependency property are removed from config admin and data manager
     * as they will never be satisfied.
     */
    @Test
    public void testRestoreAllObjects_MissingDepProperty() throws Exception
    {
        //mock behavior for creating object
        FactoryObjectInternal factoryObject = stubFactoryObject(m_FactoryDescriptor, "product-type", 1).get(0);
        FactoryObjectProxy factoryObjectProxy = mock(FactoryObjectProxy.class);
        when(m_FactoryDescriptor.create()).thenReturn(factoryObjectProxy);
        UUID uuid = factoryObject.getUuid();

        Configuration config = mock(Configuration.class);
        doReturn(config).when(m_ConfigAdmin).createFactoryConfiguration(anyString(), anyString());
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn("some-pid");
        when(m_ConfigAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Dictionary<String, Object> props = new Hashtable<>();
        when(config.getProperties()).thenReturn(props);

        //data manager behavior
        PersistentData dataOne = mock(PersistentData.class);
        when(dataOne.getUUID()).thenReturn(uuid);
        //collection of data to return
        List<PersistentData> factDatas = new ArrayList<PersistentData>();
        factDatas.add(dataOne);

        when(m_FactoryObjectDataManager.getAllObjectData(m_FactoryDescriptor)).thenReturn(factDatas);

        RegistryDependency dep = mock(RegistryDependency.class);
        when(dep.getObjectNameProperty()).thenReturn("some-prop");
        when(dep.isRequired()).thenReturn(true);

        when(m_Callback.retrieveRegistryDependencies()).thenReturn(Lists.newArrayList(dep));
        m_SUT.initialize(m_FactoryService, m_ServiceProxy, m_Callback);

        //restore the objects
        m_SUT.restoreAllObjects(m_FactoryDescriptor);

        // verify object not in registry
        assertThat(m_SUT.getObjects(), not(hasItem(factoryObject)));

        // make sure data manager and configuration is cleaned up
        verify(m_FactoryObjectDataManager).tryRemove(uuid);
        verify(config).delete();
    }

    /**
     * Test that when an object is created thru the registry that a the service helper and factory is added to the
     * dictionary.  Also verify that factory object has made call to initialize properties.
     */
    @Test
    public void testCreateProperties() throws FactoryException, ConfigurationException, IllegalArgumentException,
        FactoryObjectInformationException
    {
        when(m_FactoryDescriptor.getProductType()).thenReturn("product type");
        FactoryObjectProxy factoryObjectProxy = mock(FactoryObjectProxy.class);
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", "samplepid");

        when(m_FactoryDescriptor.create()).thenReturn(factoryObjectProxy);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        Dictionary<String, Object> metaDefaults = new Hashtable<String, Object>();
        metaDefaults.put("toaster", "bread");
        when(m_Utils.getMetaTypeDefaults(m_FactoryDescriptor)).thenReturn(metaDefaults);

        Map<String, Object> properties = new HashMap<>();

        // replay
        m_SUT.createNewObject(m_FactoryDescriptor, "samplepid", properties);

        // verify dictionary passed to factory is correct
        verify(m_FactoryDescriptor).create();

        //verify object has been created
        FactoryObjectInternal fObjInternal = (FactoryObjectInternal)instance.getInstance();
        verify(m_FactoryService).postFactoryObjectEvent(eq(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED),
                eq(fObjInternal));

        //verify factory object has been initialized
        verify(fObjInternal).initialize(eq(m_SUT),
                eq(factoryObjectProxy), eq(m_FactoryDescriptor),
                eq(m_ConfigAdmin), eq(m_EventAdmin), eq(m_PowerInternal), Mockito.any(UUID.class),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        verify(m_ServiceProxy).initializeProxy(fObjInternal, factoryObjectProxy, new HashMap<String, Object>());
    }

    /**
     * Verify that a created object can be removed.
     */
    @Test
    public void testRemoveObject() throws IllegalArgumentException, FactoryException, FactoryObjectInformationException
    {
        when(m_FactoryDescriptor.getProductType()).thenReturn("product type");
        FactoryObjectProxy factoryObjectProxy = mock(FactoryObjectProxy.class);
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", "samplepid");

        when(m_FactoryDescriptor.create()).thenReturn(factoryObjectProxy);
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        Map<String, Object> properties = new HashMap<>();

        // replay
        FactoryObjectInternal createdObj = m_SUT.createNewObject(m_FactoryDescriptor, "samplepid", properties);
        m_SUT.removeObject(createdObj.getUuid());

        verify(m_Callback).onRemovedObject(createdObj);
    }

    /**
     * Verify that an object that has already been removed or was never in registry doesn't cause an exception when
     * attempting to remove.
     */
    @Test
    public void testRemoveObject_AlreadyRemoved() throws Exception
    {
        m_SUT.removeObject(UUID.randomUUID());
    }

    /**
     * Verify that a new object is properly created when objects are passed in.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateNewObject() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException, ConfigurationException, IOException
    {
        // mock
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", null);
        FactoryObjectInternal fInternal = (FactoryObjectInternal)instance.getInstance();
        FactoryObjectProxy proxy = mock(FactoryObjectProxy.class);

        when(m_FactoryDescriptor.create()).thenReturn(proxy);
        when(m_FactoryDescriptor.getPid()).thenReturn("some.pid");
        when(m_FactoryObjectDataManager.getPersistentUuid("name")).thenReturn(null);
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(null);
        when(m_FactoryObjectDataManager.getName(Mockito.any(UUID.class))).thenReturn("name");
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        // replay
        m_SUT.createNewObject(m_FactoryDescriptor, "name",
                new ImmutableMap.Builder<String, Object>().put("key", "value").build());

        // verify
        verify(m_FactoryObjectDataManager).persistNewObjectData(Mockito.any(UUID.class),
                eq(m_FactoryDescriptor), eq("name"));

        verify(m_ServiceProxy).createFactoryObjectInternal(m_FactoryDescriptor);
        verify(m_FactoryDescriptor).create();

        InOrder inOrder = inOrder(m_Callback, m_ServiceProxy, fInternal);
        inOrder.verify(fInternal).initialize(eq(m_SUT), eq(proxy), eq(m_FactoryDescriptor),
                eq(m_ConfigAdmin), eq(m_EventAdmin), eq(m_PowerInternal),
                Mockito.any(UUID.class), eq("name"), Mockito.anyString(), Mockito.anyString());

        inOrder.verify(m_Callback).preObjectInitialize(fInternal);

        inOrder.verify(m_ServiceProxy).initializeProxy(eq(fInternal), eq(proxy), Mockito.anyMap());

        inOrder.verify(m_Callback).postObjectInitialize(fInternal);

        verify(m_ConfigAdmin).createFactoryConfiguration("some.pid", null);
    }


    /**
     * Verify that there is an attempt to remove the factory object if an exception occurs when setting the PID or
     * creating/restoring the object.
     */
    @Test
    public void testCreateNewObjectForConfigSetPidException() throws IllegalArgumentException,
        FactoryObjectInformationException, IOException, InvalidSyntaxException
    {
        // mock
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", null);
        FactoryObjectProxy proxy = mock(FactoryObjectProxy.class);
        Configuration config = mock(Configuration.class);

        String filter = String.format("(%s=%s)", Constants.SERVICE_PID, "config.pid");

        when(m_FactoryDescriptor.create()).thenReturn(proxy);
        when(m_FactoryDescriptor.getPid()).thenReturn("some.pid");
        when(m_FactoryDescriptor.getProductType()).thenReturn("some.super.AwesomeProduct");
        doThrow(Exception.class).when(m_FactoryObjectDataManager).setPid(Mockito.any(UUID.class), eq("config.pid"));
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(null);
        when(m_FactoryObjectDataManager.getName(Mockito.any(UUID.class))).thenReturn("name");
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);
        when(config.getPid()).thenReturn("config.pid");
        when(m_ConfigAdmin.listConfigurations(filter)).thenReturn(new Configuration[]{config});

        // replay
        try
        {
            m_SUT.createNewObjectForConfig(m_FactoryDescriptor, "", "config.pid");
            fail("Expecting exception when the factory object data manager tries to set the PID for the created "
                    + "object.");
        }
        catch (final Exception ex)
        {
            //expected exception
        }

        // verify
        verify(m_FactoryObjectDataManager).persistNewObjectData(Mockito.any(UUID.class),
                eq(m_FactoryDescriptor), eq("AwesomeProduct"));

        verify(m_FactoryObjectDataManager).setPid(Mockito.any(UUID.class), eq("config.pid"));
        verify(m_ConfigAdmin).listConfigurations(filter);
        verify(m_ConfigAdmin, never()).createFactoryConfiguration("some.pid", null);
        verify(m_FactoryObjectDataManager).tryRemove(Mockito.any(UUID.class));
    }

    /**
     * Verify that a an illegal argument exception is thrown if the specified configuration cannot be found.
     */
    @Test
    public void testCreateNewObjectForConfigNoConfigFound() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException, IOException, InvalidSyntaxException
    {
        // mock
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", null);
        FactoryObjectProxy proxy = mock(FactoryObjectProxy.class);

        String filter = String.format("(%s=%s)", Constants.SERVICE_PID, "config.pid");

        when(m_FactoryDescriptor.create()).thenReturn(proxy);
        when(m_FactoryDescriptor.getPid()).thenReturn("some.pid");
        when(m_FactoryObjectDataManager.getPersistentUuid("name")).thenReturn(null);
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(null);
        when(m_FactoryObjectDataManager.getName(Mockito.any(UUID.class))).thenReturn("name");
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);
        when(m_ConfigAdmin.listConfigurations(filter)).thenReturn(null);

        // replay
        try
        {
            m_SUT.createNewObjectForConfig(m_FactoryDescriptor, "name", "config.pid");
            fail("Expecting illegal argument exception since the specified configuration does not exist.");
        }
        catch (final IllegalArgumentException ex)
        {

        }

        // verify
        verify(m_ConfigAdmin).listConfigurations(filter);
        verify(m_FactoryObjectDataManager, never()).persistNewObjectData(Mockito.any(UUID.class),
                eq(m_FactoryDescriptor), eq("name"));
        verify(m_FactoryObjectDataManager, never()).tryRemove(Mockito.any(UUID.class));
        verify(m_FactoryObjectDataManager, never()).setPid(Mockito.any(UUID.class), eq("config.pid"));
        verify(m_ConfigAdmin, never()).createFactoryConfiguration("some.pid", null);
    }

    /**
     * Verify that a an illegal argument exception is thrown if the specified configuration is already associated with
     * an object.
     */
    @Test
    public void testCreateNewObjectForConfigAlreadyAssociated() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException, IOException, InvalidSyntaxException
    {
        // mock
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", null);
        FactoryObjectProxy proxy = mock(FactoryObjectProxy.class);
        Configuration config = mock(Configuration.class);
        FactoryObjectInternal object = mock(FactoryObjectInternal.class);

        String filter = String.format("(%s=%s)", Constants.SERVICE_PID, "config.pid");

        when(m_FactoryDescriptor.create()).thenReturn(proxy);
        when(m_FactoryDescriptor.getPid()).thenReturn("some.pid");
        when(m_FactoryObjectDataManager.getPersistentUuid("name")).thenReturn(null);
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(null);
        when(m_FactoryObjectDataManager.getName(Mockito.any(UUID.class))).thenReturn("name");
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);
        when(instance.getInstance()).thenReturn(object);
        when(object.getPid()).thenReturn("config.pid");
        when(config.getPid()).thenReturn("config.pid");
        when(m_ConfigAdmin.listConfigurations(filter)).thenReturn(new Configuration[]{config});

        m_SUT.createNewObjectForConfig(m_FactoryDescriptor, "asset1", "config.pid");

        // replay
        try
        {
            m_SUT.createNewObjectForConfig(m_FactoryDescriptor, "asset2", "config.pid");
            fail("Expecting illegal argument exception since the specified configuration is already associated with"
                    + "another object.");
        }
        catch (final IllegalArgumentException ex)
        {

        }

        // verify
        verify(m_ConfigAdmin, times(2)).listConfigurations(filter);
        verify(m_FactoryObjectDataManager).persistNewObjectData(Mockito.any(UUID.class),
                eq(m_FactoryDescriptor), eq("asset1"));
        verify(m_FactoryObjectDataManager, never()).persistNewObjectData(Mockito.any(UUID.class),
                eq(m_FactoryDescriptor), eq("asset2"));
        verify(m_FactoryObjectDataManager, never()).tryRemove(Mockito.any(UUID.class));
        verify(m_FactoryObjectDataManager, times(1)).setPid(Mockito.any(UUID.class), eq("config.pid"));
        verify(m_ConfigAdmin, never()).createFactoryConfiguration("some.pid", null);
    }

    /**
     * Verify exception if null configuration PID is passed into call to createNewObjectWithConfig.
     */
    @Test
    public void testCreateNewObjectForConfigNullPid() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException, IOException, InvalidSyntaxException
    {
        try
        {
            m_SUT.createNewObjectForConfig(m_FactoryDescriptor, "name", null);
            fail("expecting exception");
        }
        catch (NullPointerException e)
        {
            //expecting exception
        }
    }

    /**
     * Verify that a new object is properly created and associated with the specified configuration.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateNewObjectForConfig() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException, ConfigurationException, IOException, InvalidSyntaxException
    {
        // mock
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", null);
        FactoryObjectInternal fInternal = (FactoryObjectInternal)instance.getInstance();
        FactoryObjectProxy proxy = mock(FactoryObjectProxy.class);
        Configuration config = mock(Configuration.class);

        String filter = String.format("(%s=%s)", Constants.SERVICE_PID, "config.pid");

        when(m_FactoryDescriptor.create()).thenReturn(proxy);
        when(m_FactoryDescriptor.getPid()).thenReturn("some.pid");
        when(m_FactoryObjectDataManager.getPersistentUuid("name")).thenReturn(null);
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(null);
        when(m_FactoryObjectDataManager.getName(Mockito.any(UUID.class))).thenReturn("name");
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);
        when(config.getPid()).thenReturn("config.pid");
        when(m_ConfigAdmin.listConfigurations(filter)).thenReturn(new Configuration[]{config});

        // replay
        m_SUT.createNewObjectForConfig(m_FactoryDescriptor, "name", "config.pid");

        // verify
        verify(m_FactoryObjectDataManager).persistNewObjectData(Mockito.any(UUID.class),
                eq(m_FactoryDescriptor), eq("name"));

        verify(m_ServiceProxy).createFactoryObjectInternal(m_FactoryDescriptor);
        verify(m_FactoryDescriptor).create();

        InOrder inOrder = inOrder(m_Callback, m_ServiceProxy, fInternal);
        inOrder.verify(fInternal).initialize(eq(m_SUT), eq(proxy), eq(m_FactoryDescriptor),
                eq(m_ConfigAdmin), eq(m_EventAdmin), eq(m_PowerInternal),
                Mockito.any(UUID.class), eq("name"), Mockito.anyString(), Mockito.anyString());

        inOrder.verify(m_Callback).preObjectInitialize(fInternal);

        inOrder.verify(m_ServiceProxy).initializeProxy(eq(fInternal), eq(proxy), Mockito.anyMap());

        inOrder.verify(m_Callback).postObjectInitialize(fInternal);

        verify(m_FactoryObjectDataManager).setPid(Mockito.any(UUID.class), eq("config.pid"));
        verify(m_ConfigAdmin).listConfigurations(filter);
        verify(m_ConfigAdmin, never()).createFactoryConfiguration("some.pid", null);
    }

    /**
     * Verify exception if null properties are passed into call to createNewObject
     */
    @Test
    public void testCreateNewObjectNullProps() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException
    {
        try
        {
            m_SUT.createNewObject(m_FactoryDescriptor, "name", null);
            fail("expecting exception");
        }
        catch (NullPointerException e)
        {
            //expecting exception
        }
    }

    /**
     * Verify if no name is passed in that an object is created with the name being the
     * name of the proxy class.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateNewObjectNullName() throws IllegalArgumentException,
        FactoryObjectInformationException, FactoryException, ConfigurationException
    {
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), "name", null);
        FactoryObjectInternal fInternal = (FactoryObjectInternal)instance.getInstance();
        FactoryObjectProxy proxy = mock(FactoryObjectProxy.class);

        when(m_FactoryDescriptor.create()).thenReturn(proxy);
        when(m_FactoryDescriptor.getProductType()).thenReturn("some.package.SomeClass");
        when(m_FactoryObjectDataManager.getPersistentUuid("name")).thenReturn(null);
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(null);
        when(m_FactoryObjectDataManager.getName(Mockito.any(UUID.class))).
            thenReturn(FactoryObjectProxy.class.getSimpleName());
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);

        m_SUT.createNewObject(m_FactoryDescriptor, null, new Hashtable<String, Object>());

        verify(m_FactoryObjectDataManager).persistNewObjectData(Mockito.any(UUID.class),
                eq(m_FactoryDescriptor), eq("SomeClass"));

        verify(m_ServiceProxy).createFactoryObjectInternal(m_FactoryDescriptor);
        verify(m_FactoryDescriptor).create();

        InOrder inOrder = inOrder(m_Callback, m_ServiceProxy, fInternal);
        inOrder.verify(fInternal).initialize(eq(m_SUT), eq(proxy), eq(m_FactoryDescriptor),
                eq(m_ConfigAdmin), eq(m_EventAdmin), eq(m_PowerInternal),
                Mockito.any(UUID.class), eq(FactoryObjectProxy.class.getSimpleName()), Mockito.anyString(),
                Mockito.anyString());

        inOrder.verify(m_Callback).preObjectInitialize(fInternal);

        inOrder.verify(m_ServiceProxy).initializeProxy(eq(fInternal), eq(proxy), Mockito.anyMap());

        inOrder.verify(m_Callback).postObjectInitialize(fInternal);
    }

    /**
     * Verify if there is an exception while creating object, the factory object data and configuration are cleaned up.
     */
    @Test
    public void testCreateNewObject_MissingDepProperty() throws Exception
    {
        Configuration config = mock(Configuration.class);
        doReturn(config).when(m_ConfigAdmin).createFactoryConfiguration(anyString(), anyString());
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn("some-pid");
        when(m_ConfigAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});

        // make exception occur after configuration and data manager objects have been created
        when(m_FactoryObjectDataManager.getName(Mockito.any(UUID.class))).thenThrow(new IllegalStateException());

        Map<String, Object> props = new HashMap<>();
        props.put("some-props", "some-value");
        try
        {
            m_SUT.createNewObject(m_FactoryDescriptor, "blah", props);
            fail("Expecting exception as inner call is mocked to throw exception");
        }
        catch (IllegalStateException e)
        {

        }

        ArgumentCaptor<UUID> uuidCapture = ArgumentCaptor.forClass(UUID.class);
        verify(m_FactoryObjectDataManager).persistNewObjectData(uuidCapture.capture(), eq(m_FactoryDescriptor),
                eq("blah"));

        // make sure persisted data is removed since creation failed
        verify(m_FactoryObjectDataManager).tryRemove(uuidCapture.getValue());
        verify(config).delete();
    }

    /**
     * Verify that retrieve deps is called during construction.
     */
    @Test
    public void testRetrieveDeps()
    {
        UUID uuid = UUID.randomUUID();

        RegistryDependency foundDep1 = new RegistryDependency("dep1", true)
        {
            @Override
            public Object findDependency(String objectName)
            {
                //return something other than null
                return "And Jelly";
            }
        };
        RegistryDependency foundDep2 = new RegistryDependency("dep2", true)
        {
            @Override
            public Object findDependency(String objectName)
            {
                //return something other than null
                return "And Jelly";
            }
        };
        RegistryDependency missingDep1 = new RegistryDependency("dep1", true)
        {
            @Override
            public Object findDependency(String objectName)
            {
                return null;
            }
        };
        RegistryDependency missingDep2 = new RegistryDependency("dep2", true)
        {
            @Override
            public Object findDependency(String objectName)
            {
                return null;
            }
        };

        // have call back always say dep is missing
        when(m_Callback.retrieveRegistryDependencies()).thenReturn(Lists.newArrayList(missingDep1, missingDep2));
        m_SUT.initialize(m_FactoryService, m_ServiceProxy, m_Callback);

        final Dictionary<String, Object> props = new Hashtable<>();

        // should be unknown because one prop is missing, the other empty (both should be ignored)
        props.put("dep1", "");
        assertThat(m_SUT.isSatisfied(uuid, props), is(DependencyState.INVALID));

        // should be unknown or unsatisfied (based on which dep is checked first) because dep1 is empty, even though
        // dep2 is unsatisfied
        props.put("dep2", "some name");
        assertThat(m_SUT.isSatisfied(uuid, props), anyOf(is(DependencyState.UNSATISFIED), is(DependencyState.INVALID)));

        // now have call back have one missing, one available
        when(m_Callback.retrieveRegistryDependencies()).thenReturn(Lists.newArrayList(foundDep1, missingDep2));
        m_SUT.initialize(m_FactoryService, m_ServiceProxy, m_Callback);

        //set both deps, 1 is missing
        props.put("dep1", "some name");
        assertThat(m_SUT.isSatisfied(uuid, props), is(DependencyState.UNSATISFIED));

        // now have call back always say dep is there
        when(m_Callback.retrieveRegistryDependencies()).thenReturn(Lists.newArrayList(foundDep1, foundDep2));
        m_SUT.initialize(m_FactoryService, m_ServiceProxy, m_Callback);

        //assert that the reg is now satisfied
        assertThat(m_SUT.isSatisfied(uuid, props), is(DependencyState.SATISFIED));
    }

    /**
     * Verify that if a dep is not required and missing, deps are considered satisfied.
     */
    @Test
    public void testRetrieveDeps_MissingDepNotRequried()
    {
        RegistryDependency dep = mock(RegistryDependency.class);
        when(dep.getObjectNameProperty()).thenReturn("dep");
        when(dep.isRequired()).thenReturn(false);

        // have call back always say dep is missing
        when(m_Callback.retrieveRegistryDependencies()).thenReturn(Lists.newArrayList(dep));
        m_SUT.initialize(m_FactoryService, m_ServiceProxy, m_Callback);

        final Dictionary<String, Object> props = new Hashtable<>();

        assertThat(m_SUT.isSatisfied(UUID.randomUUID(), props), is(DependencyState.SATISFIED));
    }

    /**
     * Verify call to call back interface when handle updated is called.
     */
    @Test
    public void testHandleUpdated() throws FactoryException, ConfigurationException
    {
        FactoryObjectInternal physInt = mock(FactoryObjectInternal.class);

        m_SUT.handleUpdated(physInt);

        verify(m_Callback).preObjectUpdated(physInt);
    }

    /**
     * Test the setting of a name triggers the entity to merge.
     * Verify name update and merging call.
     */
    @Test
    public void testSetName() throws IllegalArgumentException,
        FactoryException, FactoryObjectInformationException
    {
        // mock a factory object
        UUID testUuid = UUID.randomUUID();
        createMockFactObj(testUuid, "DefaultName", null);
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);

        // set up fake object factory
        FactoryInternal factory = mock(FactoryInternal.class);
        when(factory.create()).thenReturn(testObjProxy);

        ComponentInstance instance = mockComponentInstance(testUuid, "name", "pid");
        when(m_ServiceProxy.createFactoryObjectInternal(factory)).thenReturn(instance);

        when(m_Utils.getMetaTypeDefaults(factory)).thenReturn(new Hashtable<String, Object>());

        // replay
        m_SUT.createOrRestoreObject(factory, testUuid);

        //update the name
        m_SUT.setName(testUuid, "name");

        //verify new entry created with correct name
        verify(m_FactoryObjectDataManager).setName(testUuid, "name");
    }

    /**
     * Verify ability to find an object by name.
     */
    @Test
    public void testFindObjectByName() throws IllegalArgumentException, FactoryObjectInformationException,
        FactoryException
    {
        // mock a factory object
        UUID testUuid = UUID.randomUUID();
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);

        // set up fake object factory
        FactoryInternal factory = mock(FactoryInternal.class);
        when(factory.create()).thenReturn(testObjProxy);

        ComponentInstance instance = mockComponentInstance(testUuid, "name", "pid");
        when(m_ServiceProxy.createFactoryObjectInternal(factory)).thenReturn(instance);

        when(m_Utils.getMetaTypeDefaults(factory)).thenReturn(new Hashtable<String, Object>());

        // replay
        FactoryObjectInternal obj = m_SUT.createOrRestoreObject(factory, testUuid);

        assertThat(m_SUT.findObjectByName("name"), is(obj));
    }

    /**
     * Verify null returned if an object with the given name cannot be found.
     */
    @Test
    public void testFindObjectByNameNoObject()
    {
        assertThat(m_SUT.findObjectByName("lops"), is(nullValue()));
    }

    /**
     * Verify false is returned if an object is not known by the registry.
     */
    @Test
    public void testIsObjectCreated()
    {
        assertThat(m_SUT.isObjectCreated("cookie"), is(false));
    }

    /**
     * Verify true is returned if an object is known by the registry.
     */
    @Test
    public void testIsObjectCreatedTrue() throws IllegalArgumentException, FactoryObjectInformationException,
        FactoryException
    {
        // mock a factory object
        UUID testUuid = UUID.randomUUID();
        FactoryObjectProxy testObjProxy = mock(FactoryObjectProxy.class);

        // set up fake object factory
        FactoryInternal factory = mock(FactoryInternal.class);
        when(factory.create()).thenReturn(testObjProxy);

        ComponentInstance instance = mockComponentInstance(testUuid, "name", "pid");
        when(m_ServiceProxy.createFactoryObjectInternal(factory)).thenReturn(instance);

        when(m_Utils.getMetaTypeDefaults(factory)).thenReturn(new Hashtable<String, Object>());

        // replay
        m_SUT.createOrRestoreObject(factory, testUuid);

        assertThat(m_SUT.isObjectCreated("name"), is(true));
    }

    /**
     * Verify an unsatisfied object is added to pending objects.
     */
    @Test
    public void testPendingObjectHandleEvent() throws Exception
    {
        String someProp = "hgjdksla";
        List<RegistryDependency> deps = new ArrayList<>();
        RegistryDependency dep = mock(RegistryDependency.class);
        when(dep.getObjectNameProperty()).thenReturn(someProp);
        when(dep.isRequired()).thenReturn(true);
        deps.add(dep);
        when(m_Callback.retrieveRegistryDependencies()).thenReturn(deps);
        m_SUT.initialize(m_FactoryService, m_ServiceProxy, m_Callback);

        String pid = "pid";
        String name = "name";
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), name, pid);
        FactoryObjectProxy proxy = mock(FactoryObjectProxy.class);

        when(m_FactoryDescriptor.create()).thenReturn(proxy);
        when(m_FactoryDescriptor.getProductType()).thenReturn("product type");
        when(m_FactoryObjectDataManager.getPersistentUuid("name")).thenReturn(null);
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(pid);
        when(m_FactoryObjectDataManager.getName(Mockito.any(UUID.class))).
            thenReturn(FactoryObjectProxy.class.getSimpleName());
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);
        Configuration config = mock(Configuration.class);
        doReturn(config).when(m_ConfigAdmin).createFactoryConfiguration(anyString(), anyString());
        when(m_ConfigAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] { config });
        when(config.getPid()).thenReturn(pid);

        Map<String, Object> propDeps = new HashMap<>();
        propDeps.put(someProp, "missing");
        when(config.getProperties()).thenReturn(new Hashtable<>(propDeps));

        try
        {
            m_SUT.createNewObject(m_FactoryDescriptor, name, propDeps);
            fail("Expected as the deps are not satisfied.");
        }
        catch (FactoryException e)
        {
            //expected exception
        }

        // mock dep to be found when event handler is called
        when(dep.findDependency(anyString())).thenReturn(new Object());

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        m_EventHandler.handleEvent(new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, props));

        //verify
        assertThat(m_SUT.isObjectCreated(name), is(true));
    }

    /**
     * Verify an unsatisfied object is added to pending objects.
     */
    @Test
    public void testPendingObjectHandleEventMultipleDeps() throws Exception
    {
        String someProp = "hgjdksla";
        String someProp2 = "hgjdksla";
        List<RegistryDependency> deps = new ArrayList<>();
        deps.add(new RegistryDependency(someProp, true)
        {
            //first time return null will be checked during creation,
            //second time return value, this will be first handle event check
            private int m_InvocCount = 0;
            @Override
            public Object findDependency(String objectName)
            {
                m_InvocCount ++;
                switch (m_InvocCount)
                {
                    case 1:
                        return null;
                    default:
                        return "jklsdflksjdflkj";
                }
            }
        });
        deps.add(new RegistryDependency(someProp, true)
        {
            //first time return null too, so that we can verify the above dep will
            //not trigger the object to be created - not this will not be checked the first time because the
            //dep above will be checked first
            //second time return value verifying that object will be created only once ALL
            //deps are satisfied
            private int m_InvocCount = 0;
            @Override
            public Object findDependency(String objectName)
            {
                m_InvocCount ++;
                switch (m_InvocCount)
                {
                    case 1:
                        return null;
                    default:
                        return "jklsdflksjdflkj";
                }
            }
        });
        when(m_Callback.retrieveRegistryDependencies()).thenReturn(deps);
        m_SUT.initialize(m_FactoryService, m_ServiceProxy, m_Callback);

        String pid = "pid";
        String name = "name";
        ComponentInstance instance = mockComponentInstance(UUID.randomUUID(), name, pid);
        FactoryObjectProxy proxy = mock(FactoryObjectProxy.class);

        when(m_FactoryDescriptor.create()).thenReturn(proxy);
        when(m_FactoryDescriptor.getProductType()).thenReturn("product type");
        when(m_FactoryObjectDataManager.getPersistentUuid("name")).thenReturn(null);
        when(m_FactoryObjectDataManager.getPid(Mockito.any(UUID.class))).thenReturn(pid);
        when(m_FactoryObjectDataManager.getName(Mockito.any(UUID.class))).
            thenReturn(FactoryObjectProxy.class.getSimpleName());
        when(m_ServiceProxy.createFactoryObjectInternal(m_FactoryDescriptor)).thenReturn(instance);
        Configuration config = mock(Configuration.class);
        doReturn(config).when(m_ConfigAdmin).createFactoryConfiguration(anyString(), anyString());
        when(m_ConfigAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] { config });
        when(config.getPid()).thenReturn(pid);

        Map<String, Object> propDeps = new HashMap<>();
        propDeps.put(someProp, "peanut");
        propDeps.put(someProp2, "butter");
        when(config.getProperties()).thenReturn(new Hashtable<>(propDeps));

        try
        {
            m_SUT.createNewObject(m_FactoryDescriptor, name, propDeps);
            fail("Expected as the deps are not satisfied.");
        }
        catch (FactoryException e)
        {
            //expected exception
        }
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(someProp, "somethingelse");

        m_EventHandler.handleEvent(new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, props));

        //verify still not satisfied
        assertThat(m_SUT.isObjectCreated(name), is(false));

        props.put(someProp2, "somethingelse");
        m_EventHandler.handleEvent(new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, props));

        //verify
        assertThat(m_SUT.isObjectCreated(name), is(true));
    }

    /**
     * Method will create a mock FactoryObjectInternal object with the given parameters and return a
     * mocked ComponentInstace object that will return the created mocked internal object.
     * @param uuid
     *  the uuid that the FactoryObjectInternal should have
     * @return
     *  a component instance that returns a FactoryObjectInternal with the given parameters
     */
    private ComponentInstance mockComponentInstance(UUID uuid) throws IllegalArgumentException,
        FactoryObjectInformationException
    {
        return mockComponentInstance(uuid, null, null);
    }

    /**
     * Method will create a mock FactoryObjectInternal object with the given parameters and return a
     * mocked ComponentInstace object that will return the created mocked internal object.
     * @param uuid
     *  the uuid that the FactoryObjectInternal should have
     * @param name
     *  the name of the FactoryObjectInternal should have
     * @param pid
     *  the pid of the FactoryObjectInternal should have
     * @return
     *  a component instance that returns a FactoryObjectInternal with the given parameters
     */
    private ComponentInstance mockComponentInstance(UUID uuid, String name, String pid) throws
        IllegalArgumentException, FactoryObjectInformationException
    {
        ComponentInstance instance = mock(ComponentInstance.class);

        FactoryObjectInternal fObj = createMockFactObj(uuid, name, pid);
        when(instance.getInstance()).thenReturn(fObj);

        return instance;
    }

    /**
     * Create mock factory object information.
     */
    private FactoryObjectInternal createMockFactObj(final UUID uuid, final String name,
            final String pid) throws IllegalArgumentException, FactoryObjectInformationException
    {
        FactoryObjectInternal testObj = mock(FactoryObjectInternal.class);
        when(testObj.getUuid()).thenReturn(uuid);
        when(testObj.getName()).thenReturn(name);
        when(testObj.getFactory()).thenReturn(m_FactoryDescriptor);
        when(testObj.getProxy()).thenReturn(mock(FactoryObjectProxy.class));
        if (pid != null)
        {
            when(testObj.getPid()).thenReturn(pid);
        }

        return testObj;
    }
}
