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

package mil.dod.th.ose.gui.webapp.comms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.ccomm.transport.TransportLayerAttributes;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.ose.gui.webapp.advanced.configuration.UnmodifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the comms manager implementation. 
 *      @author bachmakm
 */
public class TestCommsStackRequest
{
    private CommsStackRequestImpl m_SUT;
    private ConfigurationWrapper m_ConfigWrapper;
    private CommsMgr m_CommsMgr;
    private CommsLayerTypesMgr m_TypesMgr = mock(CommsLayerTypesMgr.class);
    private CommsImage m_CommsImageInterface = mock(CommsImage.class);
    private FactoryObjMgr m_FactoryMgr = mock(FactoryObjMgr.class);
    
    //these fields are at this level so they can be used in multiple messages
    //system ids
    private int systemId1 = 123;
    
    //pids
    private String pid1 = "PITTER";
    private String pid2 = "PATTER";
    private String pid3 = "LITTLE";
    private String pid4 = "FEET";
    private String pid5 = "PURPLE";
    private String pid6 = "POSIES";
    
    //UUIDs
    private UUID uuid1 = UUID.randomUUID();
    private UUID uuid2 = UUID.randomUUID();
    private UUID uuid3 = UUID.randomUUID();
    private UUID uuid4 = UUID.randomUUID();
    private UUID uuid5 = UUID.randomUUID();
    private UUID uuid6 = UUID.randomUUID();

    @Before
    public void setUp()
    {
        m_SUT = new CommsStackRequestImpl();
        m_ConfigWrapper = mock(ConfigurationWrapper.class);
        m_CommsMgr = mock(CommsMgr.class);   
       
        m_SUT.setCommsMgr(m_CommsMgr);
        m_SUT.setCommsImageInterface(m_CommsImageInterface);

        //Mock possible properties returned over course of getting comms stacks
        UnmodifiablePropertyModel property7 = mock(UnmodifiablePropertyModel.class);
        UnmodifiablePropertyModel property8 = mock(UnmodifiablePropertyModel.class);
        UnmodifiablePropertyModel property9 = mock(UnmodifiablePropertyModel.class);             
        
        /*
         * Mock behavior for getting the child names for a transport or link layer  
         */
        when(m_ConfigWrapper.getConfigurationPropertyAsync(123, pid1, 
                TransportLayerAttributes.CONFIG_PROP_LINK_LAYER_NAME)).
            thenReturn(property7);
        when(property7.getKey()).thenReturn("name");
        when(property7.getValue()).thenReturn("testLink");
        
        when(m_ConfigWrapper.getConfigurationPropertyAsync(123, pid2, 
                LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME)).
            thenReturn(property8);
        when(property8.getKey()).thenReturn("name");
        when(property8.getValue()).thenReturn("testPhys");
        
        when(m_ConfigWrapper.getConfigurationPropertyAsync(123, pid4, 
                LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME)).
            thenReturn(property9);
        when(property9.getKey()).thenReturn("name");
        when(property9.getValue()).thenReturn("testPhys2");
        
        /*
         * Create three fake stacks
         */
        //stack 1
        CommsLayerBaseModel transport = new CommsLayerBaseModel(systemId1, uuid1, pid1, "clazz",
                CommType.TransportLayer, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        transport.updateName("testTransport");
        CommsLayerLinkModelImpl link = new CommsLayerLinkModelImpl(systemId1, uuid2, pid2,
                "clazz", m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        link.updateName("testLink");
        CommsLayerBaseModel physical = new CommsLayerBaseModel(systemId1, uuid3, pid3, "clazz",
                CommType.PhysicalLink, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        physical.updateName("testPhys");
        
        //stack 2
        CommsLayerLinkModelImpl link2 = new CommsLayerLinkModelImpl(systemId1, uuid4, pid4,
                "clazz", m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        link2.updateName("testLink2");
        CommsLayerBaseModel physical2 = new CommsLayerBaseModel(systemId1, uuid5, pid5, "clazz",
                CommType.PhysicalLink, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        physical2.updateName("testPhys2");
        
        //stack 3
        CommsLayerBaseModel physical3 = new CommsLayerBaseModel(systemId1, uuid6, pid6, "clazz",
                CommType.PhysicalLink, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        physical3.updateName("testPhys3");
        
        List<CommsLayerBaseModel> transports = new ArrayList<CommsLayerBaseModel>();
        transports.add(transport);
        
        List<CommsLayerLinkModelImpl> links = new ArrayList<CommsLayerLinkModelImpl>();
        links.add(link);
        links.add(link2);
        
        List<CommsLayerBaseModel> physicals = new ArrayList<CommsLayerBaseModel>();
        physicals.add(physical);
        physicals.add(physical2);
        physicals.add(physical3);
        
        when(m_CommsMgr.getLinksAsync(systemId1)).thenReturn(links);
        when(m_CommsMgr.getTransportsAsync(systemId1)).thenReturn(transports);
        when(m_CommsMgr.getPhysicalsAsync(systemId1)).thenReturn(physicals);             
    }
    
    /**
     * Verify method returns all comms stacks.
     */
    @Test
    public void testGetCommsStacks()
    {
        List<CommsStackModel> stacks = m_SUT.getCommsStacksAsync(systemId1);
        assertThat(stacks.size(), is(3));   
        
        assertThat(stacks.get(0).getTransport().getUuid(), is(uuid1));
        assertThat(stacks.get(0).getLink().getUuid(), is(uuid2));
        assertThat(stacks.get(0).getPhysical().getUuid(), is(uuid3));
        
        assertThat(stacks.get(1).getTransport(), is(nullValue()));
        assertThat(stacks.get(1).getLink().getUuid(), is(uuid4));
        assertThat(stacks.get(1).getPhysical().getUuid(), is(uuid5));
        
        assertThat(stacks.get(2).getTransport(), is(nullValue()));
        assertThat(stacks.get(2).getLink(), is(nullValue()));
        assertThat(stacks.get(2).getPhysical().getUuid(), is(uuid6));
    }
    
    /**
     * Verify that method returns all stacks if selected stack is null.
     * Verify that method returns one stack if one is selected.
     */
    @Test
    public void testGetSelectedCommsStacksAll()
    {
        List<CommsStackModel> selectedStacks = m_SUT.getSelectedCommsStacksAsync(systemId1, null);
        List<CommsStackModel> stacks = m_SUT.getCommsStacksAsync(systemId1);
        
        assertThat(selectedStacks.size(), is(3));
        
        /**
         * Assert that selectedStacks and all stacks are identical since null selected stack was passed in
         */
        assertThat(stacks.get(0).getTransport(), is(selectedStacks.get(0).getTransport()));
        assertThat(stacks.get(0).getLink(), is(selectedStacks.get(0).getLink()));
        assertThat(stacks.get(0).getPhysical(), is(selectedStacks.get(0).getPhysical()));
        
        assertThat(stacks.get(1).getTransport(), is(selectedStacks.get(1).getTransport()));
        assertThat(stacks.get(1).getLink(), is(selectedStacks.get(1).getLink()));
        assertThat(stacks.get(1).getPhysical(), is(selectedStacks.get(1).getPhysical()));
        
        assertThat(stacks.get(2).getTransport(), is(selectedStacks.get(2).getTransport()));
        assertThat(stacks.get(2).getLink(), is(selectedStacks.get(2).getLink()));
        assertThat(stacks.get(2).getPhysical(), is(selectedStacks.get(2).getPhysical()));
    }
    
    /**
     * Test return of of one comms stack.
     * Verify one stack is returned if a particular stack is selected.
     */
    @Test
    public void testGetSelectedCommsStacksOne()
    {
        List<CommsStackModel> stacks = m_SUT.getCommsStacksAsync(systemId1);
        List<CommsStackModel> selectedStacks = m_SUT.getSelectedCommsStacksAsync(systemId1, stacks.get(2));
        
        assertThat(selectedStacks.size(), is(1));
        
        assertThat(stacks.get(2).getTransport(), is(selectedStacks.get(0).getTransport()));
        assertThat(stacks.get(2).getLink(), is(selectedStacks.get(0).getLink()));
        assertThat(stacks.get(2).getPhysical(), is(selectedStacks.get(0).getPhysical()));
        
        //test null return
        when(m_CommsMgr.getLinksAsync(systemId1)).thenReturn(new ArrayList<CommsLayerLinkModelImpl>());
        when(m_CommsMgr.getTransportsAsync(systemId1)).thenReturn(new ArrayList<CommsLayerBaseModel>());
        when(m_CommsMgr.getPhysicalsAsync(systemId1)).thenReturn(new ArrayList<CommsLayerBaseModel>());
        
        selectedStacks = m_SUT.getSelectedCommsStacksAsync(systemId1, stacks.get(2));
    }
    
    /**
     * Test return of top most layers of all stacks.
     * Verify top most layers of all known stacks are returned. 
     */
    @Test
    public void testGetTopMostComms()
    {
        List<CommsStackModel> stacks = m_SUT.getCommsStacksAsync(systemId1);
        List<FactoryBaseModel> topLayers = m_SUT.getTopMostComms(systemId1);
        
        assertThat(stacks.get(0).getTransport(), is(topLayers.get(0)));
        assertThat(stacks.get(1).getLink(), is(topLayers.get(1)));
        assertThat(stacks.get(2).getPhysical(), is(topLayers.get(2)));
    }
    
    /**
     * Test return of entire stack based on given layer.
     * Verify correct return of stack containing given layer. 
     */
    @Test
    public void testGetCommsStackForBase()
    {
        List<CommsStackModel> stacks = m_SUT.getCommsStacksAsync(systemId1);
        
        CommsStackModel model = m_SUT.getCommsStackForBaseModel(systemId1, stacks.get(0).getTransport());
        assertThat(model.getTransport(), is(stacks.get(0).getTransport()));
        assertThat(model.getLink(), is(stacks.get(0).getLink()));
        assertThat(model.getPhysical(), is(stacks.get(0).getPhysical()));
        
        model = m_SUT.getCommsStackForBaseModel(systemId1, stacks.get(1).getLink());
        assertThat(model.getTransport(), is(stacks.get(1).getTransport()));
        assertThat(model.getLink(), is(stacks.get(1).getLink()));
        assertThat(model.getPhysical(), is(stacks.get(1).getPhysical()));
        
        model = m_SUT.getCommsStackForBaseModel(systemId1, stacks.get(2).getPhysical());
        assertThat(model.getTransport(), is(stacks.get(2).getTransport()));
        assertThat(model.getLink(), is(stacks.get(2).getLink()));
        assertThat(model.getPhysical(), is(stacks.get(2).getPhysical()));
        
        //test null return
        when(m_CommsMgr.getLinksAsync(systemId1)).thenReturn(new ArrayList<CommsLayerLinkModelImpl>());
        when(m_CommsMgr.getTransportsAsync(systemId1)).thenReturn(new ArrayList<CommsLayerBaseModel>());
        when(m_CommsMgr.getPhysicalsAsync(systemId1)).thenReturn(new ArrayList<CommsLayerBaseModel>());
        
        assertThat(m_SUT.getCommsStackForBaseModel(systemId1, stacks.get(0).getTransport()), is(nullValue()));
    }
}