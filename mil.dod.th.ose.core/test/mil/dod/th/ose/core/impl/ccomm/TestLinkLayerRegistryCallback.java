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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.cm.ConfigurationException;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.impl.ccomm.link.LinkLayerInternal;
import mil.dod.th.ose.core.impl.ccomm.physical.PhysicalLinkInternal;
import mil.dod.th.ose.test.LoggingServiceMocker;

/**
 * Test class for the {@link LinkLayerRegistryCallback}.
 * 
 * @author allenchl
 */
public class TestLinkLayerRegistryCallback
{
    @Mock private CustomCommsServiceImpl m_CcommService;
    @Mock private FactoryRegistry<PhysicalLinkInternal> m_FactPhysInternal;
    @Mock private FactoryServiceContext<LinkLayerInternal> m_CcommFactServCxt;
    @Mock private FactoryRegistry<LinkLayerInternal> m_FactRegistry;
    private LinkLayerRegistryCallback m_SUT;
    private LoggingService m_Log;
    
    @Before
    public void setup()
    {
        // mock
        MockitoAnnotations.initMocks(this);

        // stub
        when(m_CcommFactServCxt.getRegistry()).thenReturn(m_FactRegistry);
        when(m_CcommService.getPhysicalLinkRegistry()).thenReturn(m_FactPhysInternal);

        m_Log = LoggingServiceMocker.createMock();

        // setup
        m_SUT = new LinkLayerRegistryCallback(m_CcommService, m_CcommFactServCxt, m_Log);
    }
    
    /**
     * Verify callback retrieves the physical link and requests ownership before initialization and updating.
     */
    @Test
    public void testPreObjectInitializeUpdated() throws FactoryException, ConfigurationException
    {
        LinkLayerInternal linkInternal = mock(LinkLayerInternal.class);
        String physObjName = "name";
        LinkLayerAttributes attrs = mock(LinkLayerAttributes.class);
        when(linkInternal.getConfig()).thenReturn(attrs);
        when(attrs.physicalLinkName()).thenReturn(physObjName);
        
        //grab phys link from comms
        PhysicalLinkInternal physInternal = mock(PhysicalLinkInternal.class);
        when(m_FactPhysInternal.getObjectByName(physObjName)).thenReturn(physInternal);
        
        when(physInternal.isInUse()).thenReturn(false);
        when(physInternal.getOwner()).thenReturn(null);

        m_SUT.preObjectInitialize(linkInternal);
        
        verify(m_CcommService).requestPhysicalLink(physObjName);
        
        // make sure updated does the same
        m_SUT.preObjectUpdated(linkInternal);
        
        verify(m_CcommService, times(2)).requestPhysicalLink(physObjName);
    }
    
    /**
     * Verify physical link can be null or an empty string
     */
    @Test
    public void testPreObjectEmptyPhysProp() throws Exception
    {
        LinkLayerInternal linkInternal = mock(LinkLayerInternal.class);
        LinkLayerAttributes attrs = mock(LinkLayerAttributes.class);
        when(linkInternal.getConfig()).thenReturn(attrs);
        when(attrs.physicalLinkName()).thenReturn(null);
        
        m_SUT.preObjectInitialize(linkInternal);
        
        m_SUT.preObjectUpdated(linkInternal);
        
        // try with 
        when(attrs.physicalLinkName()).thenReturn("");

        m_SUT.preObjectInitialize(linkInternal);
        
        m_SUT.preObjectUpdated(linkInternal);
    }
    
    /**
     * Verify exception if the owner of a physical link is not the link layer object.
     */
    @Test
    public void testPreObjectPhysWrongOwner() throws ConfigurationException
    {
        LinkLayerInternal linkInternal = mock(LinkLayerInternal.class);
        String physObjName = "name";
        LinkLayerAttributes attrs = mock(LinkLayerAttributes.class);
        when(linkInternal.getConfig()).thenReturn(attrs);
        when(attrs.physicalLinkName()).thenReturn(physObjName);
        
        //grab phys link from comms
        PhysicalLinkInternal physInternal = mock(PhysicalLinkInternal.class);
        when(m_FactPhysInternal.getObjectByName(physObjName)).thenReturn(physInternal);
        
        when(physInternal.isInUse()).thenReturn(true);
        when(physInternal.getOwner()).thenReturn(mock(LinkLayer.class));

        try
        {
            m_SUT.preObjectInitialize(linkInternal);
            fail("Expected as physical link owner is not the link object.");
        }
        catch (FactoryException e)
        {
            //expected as physical link prop is null
        }
        
        try
        {
            m_SUT.preObjectUpdated(linkInternal);
            fail("Expected as physical link owner is not the link object.");
        }
        catch (FactoryException e)
        {
            //expected as physical link prop is null
        }
    }
    
    /**
     * Verify release of physical link when the object is removed.
     */
    @Test
    public void testOnRemovedObject_ActiveLayer()
    {
        LinkLayerInternal linkInternal = mock(LinkLayerInternal.class);
        when(linkInternal.isActivated()).thenReturn(true);
        PhysicalLink phys = mock(PhysicalLink.class);
        when(phys.getName()).thenReturn("bob");
        when(linkInternal.getPhysicalLink()).thenReturn(phys);
        
        m_SUT.onRemovedObject(linkInternal);
        
        verify(phys).release();
        verify(linkInternal).deactivateLayer();
    }
    
    /**
     * Verify release of physical link when the object is removed.
     */
    @Test
    public void testOnRemovedObject_InactiveLayer()
    {
        LinkLayerInternal linkInternal = mock(LinkLayerInternal.class);
        when(linkInternal.isActivated()).thenReturn(false);
        PhysicalLink phys = mock(PhysicalLink.class);
        when(phys.getName()).thenReturn("bob");
        when(linkInternal.getPhysicalLink()).thenReturn(phys);
        
        m_SUT.onRemovedObject(linkInternal);
        
        verify(phys).release();
        verify(linkInternal, never()).deactivateLayer();
    }
    
    /**
     * Verify remove is handled even if no physical link
     */
    @Test
    public void testOnRemovedObject_NoPhysicalLink()
    {
        LinkLayerInternal linkInternal = mock(LinkLayerInternal.class);
        when(linkInternal.isActivated()).thenReturn(true);
        
        m_SUT.onRemovedObject(linkInternal);
        
        verify(linkInternal).deactivateLayer();
    }
    
    @Test
    public void testRetrieveRegistryDependencies()
    {
        assertThat(m_SUT.retrieveRegistryDependencies().get(0).getObjectNameProperty(), 
                is(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME));
        assertThat(m_SUT.retrieveRegistryDependencies().get(0).isRequired(), is(false));
    }
    
    /**
     * Verify catching of exception if physical link is not found when link is removed.
     */
    @Test
    public void testPhysNotFoundOnObjectRemoved()
    {
        PhysicalLink phys = mock(PhysicalLink.class);
        String physName = "name";
        when(phys.getName()).thenReturn(physName);
        
        //mock exception
        doThrow(new IllegalArgumentException()).when(phys).release();

        //link 
        LinkLayerInternal link = mock(LinkLayerInternal.class);
        when(link.getPhysicalLink()).thenReturn(phys);
        
        m_SUT.onRemovedObject(link);
        
        verify(m_Log).error(Mockito.any(IllegalArgumentException.class), 
                eq("Unable to release physical link [%s]"), eq(physName));
    }
    
    /**
     * Verify that registry dep will find phy link when 'find' is called.
     */
    @Test
    public void testRetrieveRegDepFind()
    {
        String name = "name";
        m_SUT.retrieveRegistryDependencies().get(0).findDependency(name);
        
        verify(m_FactPhysInternal).findObjectByName(name);
    }

    /**
     * Verify checking if a new object should be activated at startup.
     */
    @Test
    public void testPostObjectInitialize() throws FactoryException, AssetException, InterruptedException
    {
        LinkLayerInternal mockLayer = mock(LinkLayerInternal.class);
        LinkLayerAttributes attrs = mock(LinkLayerAttributes.class);
        String name = "link1";

        when(mockLayer.getConfig()).thenReturn(attrs);
        when(mockLayer.getName()).thenReturn(name);

        when(attrs.activateOnStartup()).thenReturn(false);
        when(m_FactRegistry.isObjectCreated(name)).thenReturn(false);
        
        m_SUT.postObjectInitialize(mockLayer);

        Thread.sleep(250);
        verify(mockLayer, never()).activateLayer();

        when(attrs.activateOnStartup()).thenReturn(true);
        when(m_FactRegistry.isObjectCreated(name)).thenReturn(true);
        
        m_SUT.postObjectInitialize(mockLayer);

        Thread.sleep(250);
        verify(mockLayer, never()).activateLayer();

        when(attrs.activateOnStartup()).thenReturn(true);
        when(m_FactRegistry.isObjectCreated(name)).thenReturn(false);
        
        m_SUT.postObjectInitialize(mockLayer);

        verify(mockLayer, timeout(250)).activateLayer();
    }
}
