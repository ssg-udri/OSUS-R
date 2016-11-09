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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.impl.ccomm.link.LinkLayerInternal;
import mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * This test class reflects the {@link CustomCommsServiceImpl} tests for Link Layers. The tests for 
 * the other layers, OSGi and system functions are {@link TestCustomCommsServiceImpl_System}, 
 * {@link TestCustomCommsServiceImpl_Transport} and {@link TestCustomCommsServiceImpl_Physical}.
 * 
 * @author callen
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class TestCustomCommsServiceImpl_LinkLayer extends CustomCommsServiceImpl_TestCommon
{
    @Before
    public void setUp() throws Exception
    {
        stubServices();
    }
    
    /**
     * Verify a link layer can be successfully created.
     */
    @Test
    public void testCreateLinkLayer() throws Exception
    {
        //test to see if you get a LinkLayer
        when(m_PhysInternal.getName()).thenReturn("name");
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);
        
        LinkLayer layer = m_SUT.createLinkLayer(LL_PRODUCT_TYPE, physicalLink.getName());
        
        assertThat(layer, is((LinkLayer)m_LinkInternal));
        
        //verify call to link reg
        ArgumentCaptor<Map> map = ArgumentCaptor.forClass(Map.class);
        verify(m_LinkRegistry).createNewObject(eq(m_LinkFactory), Mockito.anyString(), map.capture());
        
        assertThat((String)map.getValue().get(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME), is("name"));
    }
    
    /**
     * Verify that the physical link property can be null.
     */
    @Test
    public void testCreateLinkLayer_NullPhysicalLinkName() throws CCommException
    {
        LinkLayer layer = m_SUT.createLinkLayer(LL_PRODUCT_TYPE, null);
        
        assertThat(layer.getPhysicalLink(), is(nullValue()));
    }
    
    /**
     * Verify that the physical link property is not required.
     */
    @Test
    public void testCreateLinkLayer_NoPhysicalLinkProperty() throws CCommException
    {
        LinkLayer layer = m_SUT.createLinkLayer(LL_PRODUCT_TYPE, "linkLayer", new HashMap<String, Object>());
        
        assertThat(layer.getPhysicalLink(), is(nullValue()));
    }
    
    /**
     * Make sure create method deals with a physical link that is null for existing link layer.
     */
    @Test
    public void testCreateLinkLayer_WithAndWithoutPhysicalLink() throws CCommException
    {
        Set<LinkLayerInternal> existingLayers = new HashSet<>();
        LinkLayerInternal existingLayer = mock(LinkLayerInternal.class);
        when(existingLayer.getFactory()).thenReturn(m_LinkFactory);
        existingLayers.add(existingLayer);
        when(m_LinkRegistry.getObjects()).thenReturn(existingLayers);
        
        when(m_PhysInternal.getName()).thenReturn("name");
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);
        m_SUT.createLinkLayer(LL_PRODUCT_TYPE, physicalLink.getName());
    }
    
    /**
     * Verify if the physical link property is specified but an empty string or null, an exception is thrown.
     */
    @Test
    public void testCreateLinkLayer_InvalidPhysicalLinkName() throws CCommException
    {
        Map<String, Object> props = new HashMap<>();
        props.put(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, null);
        
        try
        {
            m_SUT.createLinkLayer(LL_PRODUCT_TYPE, "linkLayer", props);
            fail("Expecting exception as property value is null");
        }
        catch (IllegalArgumentException e) { }
        
        // try with empty string
        props.put(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, "");
        try
        {
            m_SUT.createLinkLayer(LL_PRODUCT_TYPE, "linkLayer", props);
            fail("Expecting exception as property value is empty");
        }
        catch (IllegalArgumentException e) { }
    }
    
    /**
     * Verify that a newly created link layer with a specified name is correctly created.
     * Verify multiples can be created as long as the physical link name is different.
     */
    @Test
    public void testCreateLinkLayer_Named() throws Exception 
    {
        //test to see if you get a LinkLayer with the specified name
        String name = "name";
        String linkName = "CreationName";
        
        when(m_PhysInternal.getName()).thenReturn(name);
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);

        LinkLayer layer = m_SUT.createLinkLayer(LL_PRODUCT_TYPE, linkName, physicalLink.getName());
        
        assertThat(layer, is((LinkLayer)m_LinkInternal));
        
        //add mocking behavior for next layer
        when(m_LinkInternal.getPhysicalLink()).thenReturn(m_PhysInternal);
        
        //verify
        Map<String, Object> props = new HashMap<>();
        props.put(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, name);
        verify(m_LinkRegistry).createNewObject(m_LinkFactory, linkName, props);
        
        //reg mock
        Set<LinkLayerInternal> links = new HashSet<>();
        links.add(m_LinkInternal);
        when(m_LinkRegistry.getObjects()).thenReturn(links);
        
        //mock another physical link
        UUID uuidDos = UUID.randomUUID();
        PhysicalLinkInternal physInternal = mock(PhysicalLinkInternal.class);
        when(physInternal.getUuid()).thenReturn(uuidDos);
        when(m_PhysRegistry.createNewObject(eq(m_PhysFactory), Mockito.anyString(), Mockito.any(Map.class))).
            thenReturn(physInternal);
        
        //create phys
        String physName2 = "name2";
        when(physInternal.getName()).thenReturn(physName2);
        PhysicalLink physicalLink2 = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);

        String linkName2 = "OriginalName";
        LinkLayerInternal linkInternal = mock(LinkLayerInternal.class);
        when(linkInternal.getName()).thenReturn(linkName2);
        when(linkInternal.getPhysicalLink()).thenReturn(physicalLink2);
        when(linkInternal.getFactory()).thenReturn(m_LinkFactory);
        when(m_LinkRegistry.createNewObject(eq(m_LinkFactory), eq(linkName2), Mockito.any(Map.class))).
            thenReturn(linkInternal);
        
        //act
        LinkLayer layer2 = m_SUT.createLinkLayer(LL_PRODUCT_TYPE, linkName2, physicalLink2.getName());

        assertThat(layer2, is(notNullValue()));
        
        //add to mock reg list
        links.add(linkInternal);
        
        //verify
        Map<String, Object> props2 = new HashMap<>();
        props2.put(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, physName2);
        verify(m_LinkRegistry).createNewObject(m_LinkFactory, linkName2, props2);
    }
    
    /**
     * Verify a link layer fails to be created if physical link is required, but not provided.
     */
    @Test
    public void testCreateLinkLayer_PhysicalLinkRequired() throws Exception
    {
        m_LinkLayerCapabilities.setPhysicalLinkRequired(true);
        
        try
        {
            m_SUT.createLinkLayer(LL_PRODUCT_TYPE, null);
            fail("Expecting exception as physical link is required, but not provided");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
    
    /**
     * Verify that if another link layer tries to use the same physical link already in use, the creation fails.
     */
    @Test
    public void testCreateLinkLayerException() 
        throws IOException, InterruptedException, CCommException, FactoryException, IllegalArgumentException, 
            FactoryObjectInformationException, ClassNotFoundException
    {
        String name = "name";
        
        when(m_PhysInternal.getName()).thenReturn(name);
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);

        when(m_LinkRegistry.createNewObject(
            eq(m_LinkFactory), Mockito.anyString(), Mockito.any(Map.class))).
                thenThrow(new FactoryException("exception"));
        try
        {
            m_SUT.createLinkLayer(LL_PRODUCT_TYPE, physicalLink.getName());
            fail("Expected due to mocking.");
        }
        catch (CCommException e)
        {
            //expected
        }
    }
    
    /**
     * Verify ability to get a link layer by name.
     */
    @Test
    public void testGetLinkLayer() 
        throws CCommException, IOException, InterruptedException, IllegalArgumentException, 
            IllegalStateException, FactoryException, FactoryObjectInformationException
    {
        m_SUT.getLinkLayer("bob");
        verify(m_LinkRegistry).getObjectByName("bob");
    }
    
    /**
     * Verify that a link layer can be found by its name.
     */
    @Test
    public void testFindLinkLayer() 
        throws InterruptedException, CCommException, IllegalArgumentException, IllegalStateException, 
            FactoryException, IOException, FactoryObjectInformationException 
    {
        //initial behavior without layer
        assertThat(m_SUT.findLinkLayer("noodle"), is(nullValue()));
        
        String name = "name";
        
        when(m_PhysInternal.getName()).thenReturn(name);
        PhysicalLink physicalLink = m_SUT.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT);

        LinkLayer layer = m_SUT.createLinkLayer(LL_PRODUCT_TYPE, name, physicalLink.getName());
        
        //reg mock
        when(m_LinkRegistry.findObjectByName(name)).thenReturn(m_LinkInternal);
        
        LinkLayer recvd = m_SUT.findLinkLayer(name);
        
        assertThat(recvd, is(layer));
    }
    
    /**
     * Verify registry call when get call is made.
     */
    @Test
    public void testGetLinkLayerSingle() 
        throws CCommException, IOException, InterruptedException, IllegalArgumentException, 
            IllegalStateException, FactoryException, FactoryObjectInformationException 
    {
        m_SUT.getLinkLayer("someName");
        verify(m_LinkRegistry).getObjectByName("someName");
    }

    /**
     * Verifies get link layers correctly retrieves all link layers.
     */
    @Test
    public void testGetLinkLayers() 
        throws CCommException, IOException, InterruptedException, IllegalArgumentException, 
            IllegalStateException, FactoryException, FactoryObjectInformationException 
    {
        String link1Name = "name";
        when(m_LinkInternal.getName()).thenReturn(link1Name);
        
        //mock another link layer
        String link2Name = "namers";
        LinkLayerInternal intLink = mock(LinkLayerInternal.class);
        when(intLink.getName()).thenReturn(link2Name);
        
        //reg will return this list
        Set<LinkLayerInternal> links = new HashSet<>();
        links.add(intLink);
        links.add(m_LinkInternal);
        when(m_LinkRegistry.getObjects()).thenReturn(links);
        
        List<LinkLayer> list = m_SUT.getLinkLayers();
        
        assertThat(list.size(), is(2));

        List<String> checkLayerNames = new ArrayList<String>();
        
        for (LinkLayer layer : list)
        {
            checkLayerNames.add(layer.getName());
        }
        assertThat(checkLayerNames, hasItems(link1Name, link2Name));
    }
    
    /**
     * Verify that the a link layer returns the expected results to the request to find out if the layer is created.
     */
    @Test
    public void testIsLinkLayerCreated() throws CCommException, IOException, InterruptedException, FactoryException,
        IllegalArgumentException, FactoryObjectInformationException
    {
        String linkName = "link";
        
        //reg mocking
        when(m_LinkRegistry.isObjectCreated(linkName)).thenReturn(true);
        
        //assert
        assertThat(m_SUT.isLinkLayerCreated(linkName), is(true));
    }

    /**
     * Test getting the factory types for link layers.
     */
    @Test
    public void testGetLinkLayerFactoryTypes() throws CCommException, InterruptedException,
        FactoryException
    {
        Set<LinkLayerFactory> factories = m_SUT.getLinkLayerFactories();
        assertThat(factories, hasItem(m_LinkFactory));
    }
    
    /**
     * Verify that a link layer cannot be created if there are no factories available.
     */
    @Test
    public void testNoFactory() throws FactoryException, CCommException
    {
        when(m_LinkLayerServiceContext.getFactories()).thenReturn(Collections.emptyMap());
        
        //verify unable to create link layer
        try
        {
            // mock a factory object
            m_SUT.createLinkLayer(LL_PRODUCT_TYPE, "blah");
            fail("Expected exception because there should be no factories available.");
        }
        catch (CCommException e)
        {
            //expected exception
        }
    }
}
