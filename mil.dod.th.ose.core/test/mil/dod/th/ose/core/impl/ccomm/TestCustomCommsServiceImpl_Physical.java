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
package mil.dod.th.ose.core.impl.ccomm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;

import org.junit.Before;
import org.junit.Test;

/**
 * This test class reflects the {@link CustomCommsServiceImpl} tests for Physical Layers. The tests for 
 * the other layers, OSGi and system functions are {@link TestCustomCommsServiceImpl_System}, 
 * {@link TestCustomCommsServiceImpl_Transport} and {@link TestCustomCommsServiceImpl_LinkLayer}.
 * 
 * @author callen
 *
 */
public class TestCustomCommsServiceImpl_Physical extends CustomCommsServiceImpl_TestCommon
{
    @Before
    public void setUp() throws Exception
    {
        stubServices();
    }
    
    /**
     * Verify that a physical link can be created when the physical link factory is known to the service.
     */
    @Test
    public void testCreatePhysicalLinkFactoryKnown() throws CCommException, IOException, 
        InterruptedException, FactoryException, FactoryObjectInformationException, 
        IllegalArgumentException, ClassNotFoundException
    {
        String name = "Some_Name";
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name);

        assertThat(physicalLink, is(notNullValue()));
        verify(m_PhysRegistry).createNewObject(m_PhysFactory, name, new Hashtable<String, Object>());
    }
    
    /**
     * Verify that a physical link can be created with properties and that the properties are passed to the registry,
     * and not set afterward.
     */
    @Test
    public void testCreatePhysicalLinkFactoryKnownWithProps() throws CCommException, IOException, 
        InterruptedException, FactoryException, FactoryObjectInformationException, 
        IllegalArgumentException
    {
        String name = "Some_Name";
        Map<String, Object> props = new HashMap<>();
        props.put("key", "value");
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name, props);

        assertThat(physicalLink, is(notNullValue()));
        verify(m_PhysRegistry).createNewObject(m_PhysFactory, name, props);
        verify(m_PhysInternal, never()).setProperties(props);
    }
    
    /**
     * Verify that if a physical link with the specified name already exists that the UUID for that link is returned.
     */
    @Test
    public void testCreatePhysicalLinkKnown() throws CCommException, IOException, 
        InterruptedException, FactoryException, FactoryObjectInformationException, IllegalArgumentException, 
        ClassNotFoundException
    {
        //minor setup
        String name = "Some_Name";
        when(m_PhysRegistry.getObjectByName(name)).thenReturn(m_PhysInternal);
        
        // Verify physical link can be created 
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name);

        //reg mocking
        when(m_PhysRegistry.isObjectCreated(name)).thenReturn(true);
        
        Map<String, Object> props = new HashMap<>();
        props.put("String", "Value");
        //replay that if tryCreate is called that the UUID of the previously created link is returned, with props set
        UUID physicalLinkUuid = 
                m_SUT.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name, props);
        
        //verify same UUID
        assertThat(physicalLink.getUuid(), is(physicalLinkUuid));
        assertThat(physicalLinkUuid, is(m_PhysUuidUno));
        
        //verify create ONLY called once since reg is mocked to return the same link
        verify(m_PhysRegistry, times(1)).createNewObject(m_PhysFactory, name, new Hashtable<String, Object>());
        
        //verify set props called
        verify(m_PhysInternal, times(1)).setProperties(props);
    }
    
    /**
     * Verify that if a physical link with the given name is not already created that it will be created
     */
    @Test
    public void testCreatePhysicalLinkUnknown() throws CCommException, IOException, 
        InterruptedException, FactoryException, FactoryObjectInformationException, IllegalArgumentException
    {
        //minor setup
        String name = "Some_Name";
        when(m_PhysInternal.getName()).thenReturn(name);
        when(m_PhysRegistry.getObjectNames()).thenReturn(new ArrayList<String>());
        when(m_PhysRegistry.getObjectByName(name)).thenReturn(m_PhysInternal);
        
        //try to create
        m_SUT.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name);

        verify(m_PhysRegistry, times(1)).createNewObject(m_PhysFactory, name, new Hashtable<String, Object>());
    }
    
    /**
     * Verify that if a physical link with the given name is not already created that it will be created with 
     * properties.
     */
    @Test
    public void testTryCreatePhysicalLinkUnknownWithProps() throws CCommException, IOException, 
        InterruptedException, FactoryException, FactoryObjectInformationException, IllegalArgumentException, 
            ClassNotFoundException
    {
        //minor setup
        String name = "Some_Name";
        when(m_PhysInternal.getName()).thenReturn(name);
        when(m_PhysRegistry.getObjectNames()).thenReturn(new ArrayList<String>());
        when(m_PhysRegistry.getObjectByName(name)).thenReturn(m_PhysInternal);
        Map<String, Object> props = new HashMap<>();
        props.put("key", "value");
        
        //try to create
        m_SUT.tryCreatePhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name, props);

        verify(m_PhysRegistry, times(1)).createNewObject(m_PhysFactory, name, props);
    }
    
    /**
     * Verify physical link cannot be created when factory is unknown to the service. 
     */
    @Test
    public void testCreatePhysicalLinkFactoryUnKnown() throws InterruptedException
    {
        when(m_PhysicalLinkServiceContext.getFactories()).thenReturn(Collections.emptyMap());
        
        try
        {
            m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);
            fail("Expecting exception, no factories available");
        }
        catch (CCommException e)
        {
        }
    }

    /**
     * Verify a physical link can be removed from the service.
     */
    @Test
    public void testDeletePhysicalLink() throws Exception
    {
        String name = "name";
        
        when(m_PhysRegistry.findObjectByName(name)).thenReturn(m_PhysInternal);

        m_SUT.deletePhysicalLink(name);
        
        verify(m_PhysInternal).delete();
    }
    
    /**
     * Verify multiple physical links can be created.
     */
    @Test
    public void testGetPhysicalLinks() throws CCommException, IOException, InterruptedException, FactoryException, 
        FactoryObjectInformationException, IllegalArgumentException, ClassNotFoundException
    {
        String name = "name";
        String name2 = "name2";
        
        //act
        m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name);
        m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name2);
        
        //verify
        verify(m_PhysRegistry, times(1)).createNewObject(m_PhysFactory, name, new Hashtable<String, Object>());
        verify(m_PhysRegistry, times(1)).createNewObject(m_PhysFactory, name2, new Hashtable<String, Object>());
    }
    
    /**
     * Verify that they can be retrieved from the service as a list of names.
     */
    @Test
    public void testGetPhysicalUuids()
    {
        m_SUT.getPhysicalLinkNames();
        
        //verify
        verify(m_PhysRegistry).getObjectNames();
    }
    
    /**
     * Verify that uuids of all the physical links known to the service can be retrieved
     */
    @Test
    public void testGetPhysicalLinkUuids() throws IllegalArgumentException, CCommException, 
        FactoryObjectInformationException 
    {
        m_SUT.getPhysicalLinkUuids();
        
        //verify
        verify(m_PhysRegistry).getUuids();
    }
    
    /**
     * Verify that a physical link name can be retrieved with the uuid of the physical link
     */
    @Test
    public void testGetPhysicalLinkName() throws IllegalArgumentException, CCommException, 
        FactoryObjectInformationException
    {
        String name = "name";
        
        PhysicalLink phys = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name);

        //mocking
        when(m_PhysRegistry.getObjectByUuid(phys.getUuid())).thenReturn(m_PhysInternal);
        when(m_PhysInternal.getName()).thenReturn(name);
        
        String nameRetvd = m_SUT.getPhysicalLinkName(phys.getUuid());
        
        //assert
        assertThat(nameRetvd, is(name));
    }
    
    /**
     * Verify that a physical link PID can be retrieved with the UUID of a physical link without
     * a configuration.
     */
    @Test
    public void testGetPhysicalLinkPidNoConfiguration() throws IllegalArgumentException, CCommException, 
        FactoryObjectInformationException  
    {
        String name = "name";
        String pid = "pid";
        
        PhysicalLink phys = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name);

        //mocking
        when(m_PhysRegistry.getObjectByUuid(phys.getUuid())).thenReturn(m_PhysInternal);
        when(m_PhysInternal.getPid()).thenReturn(pid);
        
        String pidRtrvd = m_SUT.getPhysicalLinkPid(phys.getUuid());
        
        //assert
        assertThat(pidRtrvd, is(pid));
    }
    
    /**
     * Verify that a physical link factory can be retrieved with the UUID of a physical link
     */
    @Test
    public void testGetPhysicalLinkFactory() throws IOException, InterruptedException, CCommException
    {
        //mocking
        when(m_PhysRegistry.getObjectByUuid(m_PhysUuidUno)).thenReturn(m_PhysInternal);
        when(m_PhysInternal.getFactory()).thenReturn(m_PhysFactory);
        
        // assert
        assertThat(m_SUT.getPhysicalLinkFactory(m_PhysUuidUno), is((PhysicalLinkFactory)m_PhysFactory));
    }
    
    /**
     * Verify physical link is called to release.
     */
    @Test
    public void testReleasePhysicalLink()
    {
        String name = "name";

        //mocking
        when(m_PhysRegistry.getObjectByName(name)).thenReturn(m_PhysInternal);

        //act
        m_SUT.releasePhysicalLink(name);
        
        verify(m_PhysInternal).release();
    }
    
    /**
     * Verify that a physical link can be requested using a uuid.
     */
    @Test
    public void testRequestPhysicalLinkUuid() throws FactoryException, IllegalArgumentException, 
        FactoryObjectInformationException, CCommException
    {
        String name = "name";
        
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name);
        
        //mocking
        when(m_PhysRegistry.getObjectByUuid(m_PhysUuidUno)).thenReturn(m_PhysInternal);
        when(m_PhysInternal.isInUse()).thenReturn(false);
        
        //request
        PhysicalLink recvd = m_SUT.requestPhysicalLink(physicalLink.getUuid());
        
        // verify the received physical link is the same as the created and that its in use flag is set
        assertThat(recvd, is(notNullValue()));
        assertThat(physicalLink.getUuid(), is(recvd.getUuid()));
        
        //verify in use flag setting
        verify(m_PhysInternal).setInUse(true);
    }
    
    /**
     * Verify exception if a physical link is requested using a uuid and the layer is in use.
     */
    @Test
    public void testRequestPhysicalLinkUuidInUse() throws FactoryException, IllegalArgumentException, 
        FactoryObjectInformationException, CCommException
    {
        //mocking
        when(m_PhysRegistry.getObjectByUuid(m_PhysUuidUno)).thenReturn(m_PhysInternal);
        when(m_PhysInternal.isInUse()).thenReturn(true);
        
        //request
        try
        {
            m_SUT.requestPhysicalLink(m_PhysUuidUno);
            fail("Expected exception due to mocking.");
        }
        catch (IllegalStateException e)
        {
            //expected exception
        }
    }
    
    /**
     * Verify that a physical link can be requested successfully.
     */
    @Test
    public void testRequestPhysicalLink() throws IllegalArgumentException, CCommException, 
        FactoryObjectInformationException 
    {
        String name = "name";
        
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, name);
        
        //mocking
        when(m_PhysRegistry.getObjectByName(name)).thenReturn(m_PhysInternal);
        when(m_PhysInternal.isInUse()).thenReturn(false);
        
        //act
        PhysicalLink recvd = m_SUT.requestPhysicalLink(name);
        
        //assertions
        assertThat(recvd, is(notNullValue()));
        assertThat(physicalLink.getName(), is(recvd.getName()));
        
        //verification
        verify(m_PhysInternal).setInUse(true);
    }
    
    /**
     * Verify that if is in use is called that physical link internal obj if fetched from the registry.
     */
    @Test 
    public void testRequestPhysicalLinkInUse() throws IllegalArgumentException, CCommException, 
        FactoryObjectInformationException
    {
        String name = "platter";
        
        //mock
        when(m_PhysRegistry.getObjectByName(name)).thenReturn(m_PhysInternal);
        when(m_PhysInternal.isInUse()).thenReturn(true);
        
        //assert
        assertThat(m_SUT.isPhysicalLinkInUse(name), is(true));
    }
    
    /**
     * Verify that a physical link can be opened and closed properly and that the correct values for whether it is 
     * open or not are received.
     */
    @Test 
    public void testPhysicalLinkIsOpen() throws InterruptedException, IllegalArgumentException, 
        CCommException, FactoryObjectInformationException, PhysicalLinkException 
    {
        String name = "platter";
        
        //mock
        when(m_PhysRegistry.getObjectByName(name)).thenReturn(m_PhysInternal);
        when(m_PhysInternal.isOpen()).thenReturn(true);
        
        //assert
        assertThat(m_SUT.isPhysicalLinkOpen(name), is(true));
    }

    /**
     * Test setting a physical link layer's name.
     * Verify call to registry.
     */
    @Test
    public void testSetPhysicalLinkName() throws IOException, CCommException, InterruptedException, 
        IllegalArgumentException, FactoryException, FactoryObjectInformationException
    {
        // update name again to same name, should not be error
        m_SUT.setPhysicalLinkName(m_PhysUuidUno, "test");

        //verify set name called on obj
        verify(m_PhysRegistry).setName(m_PhysUuidUno, "test");
    }

    /**
     * Test getting the factory types for physical link layers.
     */
    @Test
    public void testGetPhysicalLinkFactoryTypes() throws CCommException, IOException, InterruptedException,
        FactoryException
    {
        Set<PhysicalLinkFactory> expectedFactories = m_SUT.getPhysicalLinkFactories();
        assertThat(expectedFactories, hasItem(m_PhysFactory));
    }
}
