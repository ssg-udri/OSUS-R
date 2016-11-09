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

import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerAttributes;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.impl.ccomm.transport.TransportLayerInternal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.cm.ConfigurationException;

/**
 * Test class for the {@link TransportLayerRegistryCallback}.
 * @author allenchl
 *
 */
public class TestTransportLayerRegistryCallback
{
    private TransportLayerRegistryCallback m_SUT;
    private CustomCommsService m_CcommService;
    
    @Before
    public void setup()
    {
        m_CcommService = mock(CustomCommsService.class);
        m_SUT = new TransportLayerRegistryCallback(m_CcommService);
    }
    
    /**
     * Verify fetching link layer from comms service in pre processing methods.
     */
    @Test
    public void testPreObject_WithLink() throws FactoryException, ConfigurationException
    {
        String linkName = "name";
        
        LinkLayer link = mock(LinkLayer.class);
        when(m_CcommService.findLinkLayer(linkName)).thenReturn(link);
        
        TransportLayerInternal trans = mock(TransportLayerInternal.class);
        TransportLayerAttributes attrs = mock(TransportLayerAttributes.class);
        when(trans.getConfig()).thenReturn(attrs);
        when(attrs.linkLayerName()).thenReturn(linkName);
        
        //act
        m_SUT.preObjectInitialize(trans);
        
        verify(trans).setLinkLayer(link);
        
        // update call should do the same
        m_SUT.preObjectUpdated(trans);
        
        verify(trans, times(2)).setLinkLayer(link);
    }
    
    
    /**
     * Verify attempted fetching link layer from comms service in pre processing methods.
     * Verify exception if link is not returned from comms service.
     */
    @Test
    public void testPreObject_WithLinkNotFound() throws FactoryException, ConfigurationException
    {
        String linkName = "name";
        
        when(m_CcommService.findLinkLayer(linkName)).thenReturn(null);
        
        TransportLayerInternal trans = mock(TransportLayerInternal.class);
        TransportLayerAttributes attrs = mock(TransportLayerAttributes.class);
        when(trans.getConfig()).thenReturn(attrs);
        when(attrs.linkLayerName()).thenReturn(linkName);
        
        //act
        try
        {
            m_SUT.preObjectInitialize(trans);
            fail("expected exception due to link layer not being found by comms.");
        }
        catch (FactoryException e)
        {
            //expected
        }
        
        try
        {
            m_SUT.preObjectUpdated(trans);
            fail("expected exception due to link layer not being found by comms.");
        }
        catch (FactoryException e)
        {
            //expected
        }
        
        verify(trans, never()).setLinkLayer(Mockito.any(LinkLayer.class));
    }
    
    /**
     * Verify nothing happens if the link name property is empty or null.
     */
    @Test
    public void testPreObject_WithoutLink() throws FactoryException, ConfigurationException
    {
        TransportLayerInternal trans = mock(TransportLayerInternal.class);
        TransportLayerAttributes attrs = mock(TransportLayerAttributes.class);
        when(trans.getConfig()).thenReturn(attrs);
        when(attrs.linkLayerName()).thenReturn("");
        
        //act
        m_SUT.preObjectInitialize(trans);
        m_SUT.preObjectUpdated(trans);
        
        // allow null too
        when(attrs.linkLayerName()).thenReturn(null);
        
        //act
        m_SUT.preObjectInitialize(trans);
        m_SUT.preObjectUpdated(trans);
    }
    
    /**
     * Verify nothing happens when a transport later is removed.
     */
    @Test
    public void testOnRemovedObject()
    {
        TransportLayerInternal trans = mock(TransportLayerInternal.class);
        
        m_SUT.onRemovedObject(trans);
        
        trans.shutdown();
    }
    
    /**
     * Verify that when retrieving registry deps that the link layer property is passed as a dep.
     */
    @Test
    public void testRetrieveRegistryDependencies()
    {
        assertThat(m_SUT.retrieveRegistryDependencies().get(0).getObjectNameProperty(), 
                is(TransportLayerAttributes.CONFIG_PROP_LINK_LAYER_NAME));
        assertThat(m_SUT.retrieveRegistryDependencies().get(0).isRequired(), is(false));
        
        String madeUpLinkName = "name";
        m_SUT.retrieveRegistryDependencies().get(0).findDependency(madeUpLinkName);
        
        verify(m_CcommService).findLinkLayer(madeUpLinkName);
    }
}
