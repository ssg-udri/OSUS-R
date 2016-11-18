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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerAttributes;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.impl.ccomm.transport.TransportLayerInternal;

import org.junit.Before;
import org.junit.Test;

/**
 * This test class reflects the {@link CustomCommsServiceImpl} tests for Transport Layers. The tests for 
 * the other layers, OSGi and system functions are {@link TestCustomCommsServiceImpl_System}, 
 * {@link TestCustomCommsServiceImpl_Physical} and {@link TestCustomCommsServiceImpl_LinkLayer}.
 * 
 * @author callen
 *
 */

public class TestCustomCommsServiceImpl_Transport extends CustomCommsServiceImpl_TestCommon
{
    
    @Before
    public void setUp() throws Exception
    {
        stubServices();
    }
    
    /**
     * Verifies creation of transport layer works as expected. In particular, verifies created transport layer 
     * associations with link layers and physical links.
     */
    @Test
    public void testCreateTransportLayer() throws CCommException, IOException, InterruptedException, 
        FactoryException, IllegalArgumentException, FactoryObjectInformationException, ClassNotFoundException
    {
        String name = "name";
        when(m_TransInternal.getLinkLayer()).thenReturn(m_LinkInternal);
        when(m_LinkInternal.getName()).thenReturn(name);
        
        //checks if get transport back
        String transName = "trans";
        TransportLayer transport = m_SUT.createTransportLayer(TL_PRODUCT_TYPE, transName, name);
        
        //assert
        assertThat(transport, is(notNullValue()));

        //verify
        Map<String, Object> props = new HashMap<>();
        props.put(TransportLayerAttributes.CONFIG_PROP_LINK_LAYER_NAME, name);
        verify(m_TransRegistry).createNewObject(m_TransFactory, transName, props);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Verify return of previously created transport if a new request is received with the 
     * same link layer.
     */
    @Test
    public void testLinkLayerAlreadyOwnedCreateTransport() throws IllegalArgumentException, FactoryException, 
        FactoryObjectInformationException, CCommException, ClassNotFoundException
    {
        String name = "name";
        when(m_TransInternal.getLinkLayer()).thenReturn(m_LinkInternal);
        when(m_LinkInternal.getName()).thenReturn(name);
        
        //checks if get transport back
        String transName = "trans";
        TransportLayer transport = m_SUT.createTransportLayer(TL_PRODUCT_TYPE, transName, name);
        
        //assert
        assertThat(transport, is(notNullValue()));

        //verify
        Map<String, Object> props = new HashMap<>();
        props.put(TransportLayerAttributes.CONFIG_PROP_LINK_LAYER_NAME, name);
        verify(m_TransRegistry).createNewObject(m_TransFactory, transName, props);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
        
        //reg mocking
        Set<TransportLayerInternal> transList = new HashSet<>();
        transList.add(m_TransInternal);
        when(m_TransRegistry.getObjects()).thenReturn(transList);
        
        //request to create another link with the same link layer, but different name
        String transName2 = "nameAlso";
        TransportLayer transport2 = m_SUT.createTransportLayer(TL_PRODUCT_TYPE, transName2, name);
        
        //assert
        assertThat(transport, is(notNullValue()));
        assertThat(transport, is(transport2));

        //verify call to reg only made once
        verify(m_TransRegistry, times(1)).createNewObject(m_TransFactory, transName, props);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Verify that if a transport layer factory is not available, an instance can't be created.
     */
    @Test
    public void testCreateTransportLayerNoFactory() throws IOException, InterruptedException, FactoryException
    {
        when(m_TransportLayerServiceContext.getFactories()).thenReturn(Collections.emptyMap());

        try
        {
            m_SUT.createTransportLayer(TL_PRODUCT_TYPE, null, "jazz");
            fail("Expecting exception");
        }
        catch (CCommException e)
        {
            verify(m_WakeLock, never()).activate();
            verify(m_WakeLock, never()).cancel();
        }
    }
    
    /**
     * Verify that a transport layer can be requested by name.
     */
    @Test
    public void testGetTransportLayer() 
        throws CCommException, IOException, InterruptedException, IllegalArgumentException, IllegalStateException, 
            FactoryException
    {
        String name = "name";
        when(m_TransRegistry.getObjectByName(name)).thenReturn(m_TransInternal);
        
        TransportLayer recvd = m_SUT.getTransportLayer(name);
        
        assertThat(recvd, is(notNullValue()));
        
        //verify reg call
        verify(m_TransRegistry).getObjectByName(name);
    }
    
    /**
     * Verify that a transport layer can be found by name.
     */
    @Test
    public void testFindTransportLayer() 
        throws CCommException, IOException, InterruptedException, IllegalArgumentException, IllegalStateException, 
            FactoryException
    {
        String name = "name";
        when(m_TransRegistry.findObjectByName(name)).thenReturn(m_TransInternal);
        
        TransportLayer recvd = m_SUT.findTransportLayer(name);
        
        assertThat(recvd, is(notNullValue()));
        
        //verify reg call
        verify(m_TransRegistry).findObjectByName(name);
    }

    /**
     * Tests that getTransportLayers returns all created transport layers.
     */
    @Test
    public void testGetTransportLayers() 
        throws CCommException, IOException, InterruptedException, IllegalArgumentException, IllegalStateException, 
            FactoryException, FactoryObjectInformationException
    {
        //reg actions
        Set<TransportLayerInternal> transLinks = new HashSet<>();
        transLinks.add(m_TransInternal);
        when(m_TransRegistry.getObjects()).thenReturn(transLinks);
        
        assertThat(m_SUT.getTransportLayers().size(), is(1));
        
        verify(m_TransRegistry).getObjects();
    }

    /**
     * Verify that a request to know if a layer is created returns the expected results.
     */
    @Test
    public void testIsTransportLayerCreated() throws CCommException, IOException, InterruptedException, 
            FactoryException
    {
        String name = "name";
        when(m_TransRegistry.isObjectCreated(name)).thenReturn(true);
        
        assertThat(m_SUT.isTransportLayerCreated(name), is(true));
    }
    
    /**
     * Test getting the factory types for transport layers.
     */
    @Test
    public void testGetTransportLayerFactoryTypes() throws CCommException, IOException, InterruptedException,
        FactoryException
    {
        Set<TransportLayerFactory> expectedFactories = m_SUT.getTransportLayerFactories();
        assertThat(expectedFactories, hasItem(m_TransFactory));
    }
}
