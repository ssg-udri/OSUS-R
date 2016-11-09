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
package mil.dod.th.ose.core.factory.api.data;

import static org.junit.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.core.FactoryMocker;
import mil.dod.th.ose.core.factory.proto.FactoryObjectInformation.FactoryObjectData;

/**
 * Test the base abstract {@link FactoryObjectDataManager}.
 * @author callen
 *
 */
public class TestBaseFactoryObjectDataManager
{
    private static final String PRODUCT_TYPE = "product-type";
    private BaseFactoryObjectDataManager m_SUT;
    private PersistentDataStore m_PersistentDataStore;
    
    @Before
    public void setUp()
    {
        //the system under test
        m_SUT = new BaseFactoryObjectDataManager()
        {           
            @Override
            protected Class<? extends FactoryObject> getServiceObjectType()
            {
                return LinkLayer.class;
            }
        };
        
        //mock needed services
        m_PersistentDataStore = mock(PersistentDataStore.class);
        
        m_SUT.setPersistentDataStore(m_PersistentDataStore);
    }
    
    /**
     * Verify the behavior of set name if the factory object data exists.
     * - Updated data with the new name should be merged into the data store for the given UUID entry
     */
    @Test
    public void testSetName() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException, InvalidProtocolBufferException, ValidationFailedException
    {
        //the name to use
        String name = "new";
        //the uuid
        UUID uuid = UUID.randomUUID();

        //m_FactData = FactoryObjectData.newBuilder().setName("original").build();
        FactoryObjectData factData = createFactoryObjectData("original", null);
        
        PersistentData persistentData = createPersistentData(uuid, factData.toByteArray());

        //call to set a name
        m_SUT.setName(uuid, name);
        
        //verify
        verify(m_PersistentDataStore).merge(persistentData);
        
        //capture what was set
        ArgumentCaptor<Serializable> messageCap = ArgumentCaptor.forClass(Serializable.class);
        verify(persistentData).setEntity(messageCap.capture());
        
        FactoryObjectData mergedData = FactoryObjectData.parseFrom((byte[])messageCap.getValue());
        assertThat(mergedData.getName(), is(name));
    }
    
    /**
     * Verify the behavior of set name if the factory object data does not exist for the given UUID.
     * - An illegal argument exception should be thrown
     */
    @Test
    public void testSetNameNoData() throws FactoryObjectInformationException
    {
        //no need to mock the data, its not in the data store
        UUID uuid = UUID.randomUUID();
    
        //call to set a name
        try
        {
            m_SUT.setName(uuid, "cantSetMe");
            fail("Expecting exception, UUID is not associated with any data store entries.");
        }
        catch (IllegalArgumentException e)
        {
            //exception expected
        }
    }
    
    /**
     * Verify the behavior of set name if there is an error parsing the data for the given UUID.
     * - A FactoryObjectInformationException should be thrown
     */
    @Test
    public void testSetNameParseException()
    {
        //mock factory object persistent data entry with invalid data
        UUID uuid = UUID.randomUUID();
        createPersistentData(uuid, new byte[]{1,2,3,4,5,6});
    
        //run the method, expecting exception
        try
        {
            m_SUT.setName(uuid, "cantSetMe");
            fail("FactoryObjectInformationExceptionExpected");
        }
        catch (final FactoryObjectInformationException ex)
        {
            //exception expected, caused by bad parse - InvalidProtocolBuffer
            assertThat(ex.getCause().getClass().getName(), is(InvalidProtocolBufferException.class.getName()));
        }
    }
    
    /**
     * Verify the behavior of set name if there is an error persisting the new data
     * - A FactoryObjectInformationException should be thrown
     */
    @Test
    public void testSetNamePersistenceException() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        //mock factory object persistent data entry with valid data
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("name", "pid");
        PersistentData persistentData = createPersistentData(uuid, factData.toByteArray());
    
        //mock exception for persistent data store
        doThrow(new PersistenceFailedException("FAILURE!!!")).when(m_PersistentDataStore).merge(persistentData);
    
        //run the method, expecting exception
        try
        {
            m_SUT.setName(uuid, "cantSetMe");
            fail("FactoryObjectInformationExceptionExpected");
        }
        catch (final FactoryObjectInformationException ex)
        {
            //exception expected, caused by persistence error - PersistenceFailed
            assertThat(ex.getCause().getClass().getName(), is(PersistenceFailedException.class.getName()));
        }      
    }
    
    /**
     * Verify behavior of persistNewObjectData.
     * - given a unique UUID and valid information, a new persisted data entry should be
     *   created in the data store
     */
    @Test
    public void testPersistNewObjectData() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException, InvalidProtocolBufferException
    {
        //the name to use
        String name = "name";
        //the uuid
        UUID uuid = UUID.randomUUID();
        //factory
        FactoryDescriptor factory = FactoryMocker.mockFactoryDescriptor(PRODUCT_TYPE);

        //call to set a name
        m_SUT.persistNewObjectData(uuid, factory, name);
        
        //verify
        ArgumentCaptor<Serializable> entityCap = ArgumentCaptor.forClass(Serializable.class);
        verify(m_PersistentDataStore).
            persist(eq(LinkLayer.class), eq(uuid), eq(PRODUCT_TYPE), entityCap.capture());
        
        FactoryObjectData createdData = FactoryObjectData.parseFrom((byte[])entityCap.getValue());
        assertThat(createdData.getName(), is(name));
    }
    
    /**
     * Verify that exceptions are handled as expected if there is a failure to persist new data
     * - error should be wrapped in a FactoryObjectInformationException and thrown
     */
    @Test
    public void testPersistNewObjectPersistenceException() throws FactoryObjectInformationException, 
        IllegalArgumentException, PersistenceFailedException, InvalidProtocolBufferException
    {
        //the name to use
        String name = "name";
        //the uuid
        UUID uuid = UUID.randomUUID();

        FactoryDescriptor factory = FactoryMocker.mockFactoryDescriptor(PRODUCT_TYPE);

        //exception
        doThrow(new PersistenceFailedException("FAILURE!!")).when(m_PersistentDataStore).
                persist(eq(LinkLayer.class), eq(uuid), eq(PRODUCT_TYPE), (Serializable)any());

        //call to persist new data
        try
        {
            m_SUT.persistNewObjectData(uuid, factory, name);
            fail("Expected factory object information exception - persistence failed.");
        }
        catch (FactoryObjectInformationException e)
        {
            //expected exception
            assertThat(e.getCause().getClass().getName(), is(PersistenceFailedException.class.getName()));
        }
    }
    
    /**
     * Verify case where the given UUID is already being used for persistNewObject.
     * - an IllegalArgumentException should be thrown.
     */
    @Test
    public void testPeristNewObjectUuidInUse() throws FactoryObjectInformationException
    {
        //mock some data
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("storedGuy", null);
        createPersistentData(uuid, factData.toByteArray());
        
        //name to use
        String name = "noUniqueUuidForMe";
        FactoryDescriptor factory = mock(FactoryDescriptor.class);

        //call to persist new data
        try
        {
            m_SUT.persistNewObjectData(uuid, factory, name);
            fail("Expected exception, UUID is not unique.");
        }
        catch (IllegalArgumentException e)
        {
            //expected exception, UUID is already in use
        }
    }
    
    /**
     * Verify the behavior of get name when there is data in the persistent data store for the given UUID.
     * - Method is expected to return the name set in the data
     */
    @Test
    public void testGetName() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException, InvalidProtocolBufferException
    {
        //the name to use
        String name = "getThisName";
        
        //the UUID
        UUID uuid = UUID.randomUUID();
        
        //mock the persistent entry
        FactoryObjectData factData = createFactoryObjectData(name, null);
        createPersistentData(uuid, factData.toByteArray());

        //call to set a name
        String fetchedName = m_SUT.getName(uuid);
        
        //verify
        assertThat(name, is(fetchedName));
    }
    
    /**
     * Verify the behavior of get name when there is no data in the persistent data store for the given UUID.
     * - Method is expected to throw an IllegalArgumentException
     */
    @Test
    public void testGetNameNoData() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException, InvalidProtocolBufferException
    {      
        //the UUID
        UUID uuid = UUID.randomUUID();
        
        //no need to mock data, testing case where non is in the data store

        //call to set a name
        try
        {
            m_SUT.getName(uuid);
            fail("Exception expected, there is no data associate with the uuid");
        }
        catch (final IllegalArgumentException ex)
        {
            //exception expected, no need to check for a specific cause
        }       
    }
    
    /**
     * Verify the behavior of get name if there is an error parsing the factory object data.
     * - Method is expected to throw a FactoryInformationException
     */
    @Test
    public void testGetNameParseException()
    {
        //mock factory object persistent data entry with invalid data
        UUID uuid = UUID.randomUUID();
        createPersistentData(uuid, new byte[]{1,2,3,4,5,6});
    
        //run the method, expecting exception
        try
        {
            m_SUT.getName(uuid);
            fail("FactoryObjectInformationExceptionExpected");
        }
        catch (final FactoryObjectInformationException ex)
        {
            //exception expected, caused by bad parse - InvalidProtocolBuffer
            assertThat(ex.getCause().getClass().getName(), is(InvalidProtocolBufferException.class.getName()));
        }
    }
    
    /**
     * Verify that getPersistentUuid can find and return the UUID based on the given name.
     * - the UUID associated with the given name should be returned
     * - data that cannot be parsed should be ignored
     */
    @Test
    public void testGetPersistentUuid()
    {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
     
        //create data entries
        FactoryObjectData factData1 = createFactoryObjectData("bob", null);
        FactoryObjectData factData2 = createFactoryObjectData("jack", null);
        FactoryObjectData badData = FactoryObjectData.newBuilder().setName("pole").build();
        badData = FactoryObjectData.newBuilder().buildPartial();
        
        //create Persistent Data
        PersistentData pdata1 = createPersistentData(uuid1, factData1.toByteArray());
        PersistentData pdata2 = createPersistentData(uuid2, factData2.toByteArray());
        PersistentData pdata3 = createPersistentData(uuid3, badData.toByteArray());
        
        List<PersistentData> persistentDatas = new ArrayList<PersistentData>();
        persistentDatas.add(pdata1);
        persistentDatas.add(pdata2);
        persistentDatas.add(pdata3);
        
        when(m_PersistentDataStore.query(eq(LinkLayer.class))).thenReturn(persistentDatas);
        
        //find entity
        UUID foundUUID = m_SUT.getPersistentUuid("bob");
        
        assertThat(foundUUID, is(uuid1));       
    }
    
    /**
     * Verify the behavior of getPersistentUuid if the given name is not in the data store
     * - the return value should be null if the name is not found.
     */
    @Test
    public void testGetPersistentUuidNameNotFound()
    {
        //create UUIDs for stored data
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
     
        //create data entries
        FactoryObjectData factData1 = createFactoryObjectData("bob", null);
        FactoryObjectData factData2 = createFactoryObjectData("jack", null);
        
        //create Persistent Data
        PersistentData pdata1 = createPersistentData(uuid1, factData1.toByteArray());
        PersistentData pdata2 = createPersistentData(uuid2, factData2.toByteArray());
        
        List<PersistentData> persistentDatas = new ArrayList<PersistentData>();
        persistentDatas.add(pdata1);
        persistentDatas.add(pdata2);
        
        //mock out the query to search
        when(m_PersistentDataStore.query(eq(LinkLayer.class))).thenReturn(persistentDatas);
        
        //find entity
        UUID foundUUID = m_SUT.getPersistentUuid("noone");
        
        //verify that name was not found in the data store
        assertThat(foundUUID, is(nullValue()));  
    }

    /**
     * Verify that getPersistentUuid handles data that cannot be parsed without interrupting the search
     * - data that cannot be parsed should not cause any exceptions
     */
    @Test
    public void testGetPersistentUuidBadParseEntry()
    {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
     
        //create data entries
        FactoryObjectData factData1 = createFactoryObjectData("bob", null);
        FactoryObjectData factData2 = createFactoryObjectData("jack", null);
        FactoryObjectData badData = FactoryObjectData.newBuilder().setName("pole").build();
        badData = FactoryObjectData.newBuilder().buildPartial();
        
        //create Persistent Data
        PersistentData pdata1 = createPersistentData(uuid1, factData1.toByteArray());
        PersistentData pdata2 = createPersistentData(uuid2, factData2.toByteArray());
        PersistentData pdata3 = createPersistentData(uuid3, badData.toByteArray());
        
        List<PersistentData> persistentDatas = new ArrayList<PersistentData>();
        persistentDatas.add(pdata1);
        persistentDatas.add(pdata2);
        persistentDatas.add(pdata3);
        
        when(m_PersistentDataStore.query(eq(LinkLayer.class))).thenReturn(persistentDatas);
        
        //find entity
        UUID foundUUID = m_SUT.getPersistentUuid("pole");
        
        assertThat(foundUUID, is(nullValue()));       
    }
    
    /**
     * Verify that when remove is called the data store is requested to remove the matching entity.
     */
    @Test
    public void testTryRemove() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException, InvalidProtocolBufferException
    {
        //the uuid to use
        UUID uuid = UUID.randomUUID();
        m_SUT.tryRemove(uuid);
        
        //verify
        verify(m_PersistentDataStore).remove(uuid);
    }
    
    /**
     * Verify that when tryRemove is called and data store throws exception it is caught.
     */
    @Test
    public void testTryRemove_HandleException() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException, InvalidProtocolBufferException
    {
        UUID uuid = UUID.randomUUID();
        
        doThrow(new IllegalArgumentException()).when(m_PersistentDataStore).remove(uuid);
        
        // just make sure no exception is thrown
        m_SUT.tryRemove(uuid);
    }
    
    /**
     * Verify setPid functions as expected.
     * - given a UUID associated with data in the data store the pid should be retrieved accurately
     */
    @Test
    public void testGetPid() throws FactoryObjectInformationException
    {
        //mock factory object information
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("name", "correctPid");
        createPersistentData(uuid, factData.toByteArray());
    
        //retrieve the PID
        String pid = m_SUT.getPid(uuid);
    
        assertThat(pid, is("correctPid"));
    }
    
    /**
     * Verify null is returned if no PID is associated with the data
     * - given a UUID associated with data in the data store null should be returned if no pid is found in the data
     */
    @Test
    public void testGetPidNoConfig() throws FactoryObjectInformationException
    {
        //mock factory object information - leave out PID
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("name", null);
        createPersistentData(uuid, factData.toByteArray());
    
        //retrieve the PID
        String pid = m_SUT.getPid(uuid);
    
        assertThat(pid, is(nullValue()));
    }
    
    /**
     * Verify behavior if there is no data associated with the given UUID.
     * - an illegal argument exception should be thrown
     */
    @Test
    public void testGetPidNoFactData() throws FactoryObjectInformationException
    {
        // no need to mock, testing case with no data entry
        try
        {
            m_SUT.getPid(UUID.randomUUID());
            fail("No valid factory data for given UUID, IllegalArgumentException expected");
        }
        catch (IllegalArgumentException ex)
        {
            //expected exception, no valid factory data for the uuid
        }
    }
    
    /**
     * Verify behavior if there is an error parsing the data.
     * - a FactoryObjectInformationException should be thrown with an appropriate cause
     */
    @Test
    public void testGetPidDataParseException() throws IllegalArgumentException, FactoryObjectInformationException
    {
        //mock factory object information - use a bad entity
        UUID uuid = UUID.randomUUID();
        createPersistentData(uuid, new byte[]{1,2,3,4,5,6});
    
        //retrieve the PID
        //run the method, expecting exception
        try
        {
            m_SUT.getPid(uuid);
            fail("FactoryObjectInformationException Expected");
        }
        catch (final FactoryObjectInformationException ex)
        {
            //exception expected, caused by bad parse - InvalidProtocolBuffer
            assertThat(ex.getCause().getClass().getName(), is(InvalidProtocolBufferException.class.getName()));
        }
    }
    
    /**
     * Verify the expected behavior of setPid() when the factory data exists
     * - data should be updated with the given PID
     */
    @Test
    public void testSetPid() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException, InvalidProtocolBufferException
    {
        //mock factory object data entity
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("name", null);
        PersistentData persistentData = createPersistentData(uuid, factData.toByteArray());
    
        String pid = "setMe";
    
        m_SUT.setPid(uuid, pid);
    
        //verify that the persistent data entry is updated with a new version of the factoryObjectData
        //capture what was set
        ArgumentCaptor<Serializable> messageCap = ArgumentCaptor.forClass(Serializable.class);
        verify(persistentData).setEntity(messageCap.capture());
        
        //parse the updated data from the capture
        FactoryObjectData updatedFactData = FactoryObjectData.parseFrom((byte[])messageCap.getValue());
 
        //verify that the data has no Pid
        assertThat(updatedFactData.getPid(), is(pid));
    }
    
    /**
     * Verify the behavior of setPid() when there is no factory data associated with the UUID
     * - An IllegalArgumentException should be thrown
     */
    @Test
    public void testSetPidNoData() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException
    {
        //no need to mock data for this test
        UUID uuid = UUID.randomUUID();
        String pid = "setMe";
    
        try
        {
            m_SUT.setPid(uuid, pid);
            fail("Expected exception, invalid uuid");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
    }
    
    /**
     * Verify the behavior of setPid() when the factory data for the given UUID cannot be parsed
     * - A FactoryObjectInformationException should be thrown
     */
    @Test
    public void testSetPidParseException()
    {
        //mock factory object persistent data entry with invalid data
        UUID uuid = UUID.randomUUID();
        createPersistentData(uuid, new byte[]{1,2,3,4,5,6});
        
        //run the method, expecting exception
        try
        {
            m_SUT.setPid(uuid, "cantSetMe");
            fail("FactoryObjectInformationExceptionExpected");
        }
        catch (final FactoryObjectInformationException ex)
        {
            //exception expected, caused by bad parse - InvalidProtocolBuffer
            assertThat(ex.getCause().getClass().getName(), is(InvalidProtocolBufferException.class.getName()));
        }
    }
        
    /**
     * Verify the behavior of setPid() if there is an error persisting the new data
     * - A FactoryObjectInformationException should be thrown
     */
    @Test
    public void testSetPidPersistenceException() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        //mock factory object persistent data entry with valid data
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("name", "pid");
        PersistentData persistentData = createPersistentData(uuid, factData.toByteArray());
        
        //mock exception for persistent data store
        doThrow(new PersistenceFailedException("FAILURE!!!")).when(m_PersistentDataStore).merge(persistentData);
        
        //run the method, expecting exception
        try
        {
            m_SUT.setPid(uuid, "cantSetMe");
            fail("FactoryObjectInformationExceptionExpected");
        }
        catch (final FactoryObjectInformationException ex)
        {
            //exception expected, caused by persistence error - PersistenceFailed
            assertThat(ex.getCause().getClass().getName(), is(PersistenceFailedException.class.getName()));
        }      
    }
    
    /**
     * Verify exception if the PID being set is null.
     */
    @Test
    public void testSetPidNull() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException, InvalidProtocolBufferException
    {
        //mock factory object data entity
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("name", null);
        createPersistentData(uuid, factData.toByteArray());
    
        try
        {
            m_SUT.setPid(uuid, null);
            fail("Expected exception");
        }
        catch (IllegalArgumentException e)
        {
            //expected exception PID cannot be set to null
        }
    }
    
    /**
     * Verify expected behavior of clearPid() if the data exists and has a PID value.
     * - PID entry in the entity of the PersistentData for the given object should be cleared
     */
    @Test
    public void testClearPid() throws FactoryObjectInformationException, IllegalArgumentException,
        PersistenceFailedException, InvalidProtocolBufferException
    {
        //mock factory object persistent data entry
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("name", "clearThisPid");
        PersistentData persistentData = createPersistentData(uuid, factData.toByteArray());
   
        //run the method, not expecting any exceptions
        m_SUT.clearPid(uuid);
        
        //verify that the persistent data entry is updated with a new version of the factoryObjectData
        //capture what was set
        ArgumentCaptor<Serializable> messageCap = ArgumentCaptor.forClass(Serializable.class);
        verify(persistentData).setEntity(messageCap.capture());
        
        //parse the updated data from the capture
        FactoryObjectData updatedFactData = FactoryObjectData.parseFrom((byte[])messageCap.getValue());
 
        //verify that the data has no Pid
        assertThat(updatedFactData.hasPid(), is(false));
    }
    
    /**
     * Verify expected behavior of clearPid() if the data exists but does not have a PID value.
     * - There should be no change in the PersistentData entry for the given object
     */
    @Test
    public void testClearPidNoPid() throws FactoryObjectInformationException, IllegalArgumentException,
        PersistenceFailedException
    {
        //mock factory object persistent data entry, don't include a PID
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("name", null);
        PersistentData persistentData = createPersistentData(uuid, factData.toByteArray());
   
        //run the method, not expecting any exceptions
        m_SUT.clearPid(uuid);
       
        //verify that the persistent data entry is never updated, there's no need do a merge of data
        verify(persistentData, times(0)).setEntity(factData.toByteArray());   
    }
    
    /**
     * Verify that an exception is thrown if there is a problem parsing the factory object information for the
     * given UUID.
     * - FactoryObjectInformationException should be thrown, no updates should be made.
     */
    @Test
    public void testClearPidDataParseException() throws FactoryObjectInformationException, IllegalArgumentException,
        PersistenceFailedException
    {
        //mock factory object persistent data entry with invalid data
        UUID uuid = UUID.randomUUID();
        createPersistentData(uuid, new byte[]{1,2,3,4,5,6});
    
        //run the method, expecting exception
        try
        {
            m_SUT.clearPid(uuid);
            fail("FactoryObjectInformationException Expected");
        }
        catch (final FactoryObjectInformationException ex)
        {
            //exception expected, caused by bad parse - InvalidProtocolBuffer
            assertThat(ex.getCause().getClass().getName(), is(InvalidProtocolBufferException.class.getName()));
        }
    }
    
    /**
     * Verify that an exception is thrown if there is a problem merging with the persistent data store.
     * - Factory data is valid but merge cannot be completed. Exception caused by PersistentDataStore error.
     */
    @Test
    public void testClearPidPersistentStoreException() throws FactoryObjectInformationException, 
        IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        //mock factory object persistent data entry with valid data
        UUID uuid = UUID.randomUUID();
        FactoryObjectData factData = createFactoryObjectData("name", "pid");
        PersistentData persistentData = createPersistentData(uuid, factData.toByteArray());
    
        //mock exception for persistent data store
        doThrow(new PersistenceFailedException("FAILURE!!!")).when(m_PersistentDataStore).merge(persistentData);
    
        //run the method, expecting exception
        try
        {
            m_SUT.clearPid(uuid);
            fail("FactoryObjectInformationExceptionExpected");
        }
        catch (final FactoryObjectInformationException ex)
        {
            //exception expected, caused by persistence error - PersistenceFailed
            assertThat(ex.getCause().getClass().getName(), is(PersistenceFailedException.class.getName()));
        }
    }
    
    /**
     * Verify that an exception is thrown if there is no object data for the given uuid
     * - Factory data is not in the persistent store for the uuid, no updates can be made
     */
    @Test
    public void testClearPidNoData() throws FactoryObjectInformationException, IllegalArgumentException, 
        PersistenceFailedException
    {
        //Don't mock the data, its not intended to be in the datastore
        UUID uuid = UUID.randomUUID();
        
        //run the method, expecting an exception
        try
        {
            m_SUT.clearPid(uuid);
            fail("Expected exception for invalid uuid");
        }
        catch (IllegalArgumentException e)
        {
            //Expected exception, no specific cause needs to be checked.
        }
    }
    
    /**
     * Verify the expected behavior of getAllObjectData in a default circumstance
     * - all objects associated with a given factory should be returned
     */
    @Test
    public void testGetAllObjectData()
    {
        //is it worth while to test the PersistentDataStore's query method for our 
        //specific context and description?
    
        FactoryDescriptor factory = FactoryMocker.mockFactoryDescriptor(PRODUCT_TYPE);
        
        //create a mock collection
        PersistentData persistentData1 = createPersistentData(UUID.randomUUID(), new byte[]{1,2,3,4,5,6});
        PersistentData persistentData2 = createPersistentData(UUID.randomUUID(), new byte[]{1,2,3,4,5,6});
        PersistentData persistentData3 = createPersistentData(UUID.randomUUID(), new byte[]{1,2,3,4,5,6});
        
        Collection<PersistentData> factoryCollection = new ArrayList<PersistentData>();
        factoryCollection.add(persistentData1);
        factoryCollection.add(persistentData2);
        factoryCollection.add(persistentData3);
    
        when(m_PersistentDataStore.query(LinkLayer.class, PRODUCT_TYPE)).thenReturn(factoryCollection);
    
        //run get all object data
        Collection<PersistentData> retrievedCollection = m_SUT.getAllObjectData(factory);
        
        assertThat(retrievedCollection, is(factoryCollection));
    }
    
    /**
     * Helper method to create a factory object data entry with the given name and pid.
     * @param name
     *     name to assign to the data object
     * @param pid
     *     pid to assign to the data object, if null then no pid will be assigned
     */
    public FactoryObjectData createFactoryObjectData(final String name, final String pid)
    {
        if (pid == null)
        {
            return FactoryObjectData.newBuilder().setName(name).build();
        }
        else
        {
            return FactoryObjectData.newBuilder().setName(name).setPid(pid).build();
        }
    }
    
    /**
     * Helper method to create a mock persistent data store entry with the given entity stored at the given UUID.
     * @param uuid
     *     UUID to associate with the entry
     * @param entity
     *     entity for the entry
     * @return persistentData
     *     the mocked data entry
     */
    public PersistentData createPersistentData(final UUID uuid, final byte[] entity)
    {
        PersistentData persistentData = mock(PersistentData.class);
        when(persistentData.getEntity()).thenReturn(entity);
        when(persistentData.getUUID()).thenReturn(uuid);
        when(m_PersistentDataStore.find(uuid)).thenReturn(persistentData);
        
        return persistentData;
    }
}
