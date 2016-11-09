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
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace.TransportLayerMessageType;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.factory.FactoryObjMgr;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.protobuf.Message;

/**
 * Test implementation of the {@link SelectedCommsHelper} class.
 * @author bachmakm
 *
 */
public class TestSelectedCommsHelper 
{
    private ConfigurationWrapper m_ConfigWrapper;
    private CommsLayerTypesMgr m_TypesMgr;
    private SelectedCommsHelperImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private MessageWrapper m_MessageWrapper;
    private CommsImage m_CommsImageInterface;
    private FactoryObjMgr m_FactoryMgr;
    
    private UUID uuid = UUID.randomUUID();
    private UUID uuid2 = UUID.randomUUID();
    private String pid = "pid";
    
    @Before
    public void setup()
    {
        m_SUT = new SelectedCommsHelperImpl();
        
        m_ConfigWrapper = mock(ConfigurationWrapper.class);
        m_TypesMgr = mock(CommsLayerTypesMgr.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_CommsImageInterface = mock(CommsImage.class);
        m_FactoryMgr = mock(FactoryObjMgr.class);
        
        m_SUT.setMessageFactory(m_MessageFactory);
        
        when(m_MessageFactory.createCustomCommsMessage(Mockito.any(CustomCommsMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createTransportLayerMessage(Mockito.any(TransportLayerMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createLinkLayerMessage(Mockito.any(LinkLayerMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
    }
    
    /**
     * Test the default/initial values set for the selected model at creation.
     * 
     * Verify that all values are returned as documented.
     */
    @Test
    public void testGettersAndSetters()
    {
        assertThat(m_SUT.getSelectedComms(), is(nullValue()));
        
        CommsStackModelImpl model = new CommsStackModelImpl(m_CommsImageInterface);
        model.setPhysical(new CommsLayerBaseModel(0, uuid, pid, "clazz", CommType.PhysicalLink, 
                m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface));
        m_SUT.setSelectedComms(model);
        
        assertThat(m_SUT.getSelectedComms().getPhysical(), is(model.getPhysical()));  
        
        m_SUT.unSetSelectedComms();
        assertThat(m_SUT.getSelectedComms(), is(nullValue()));        
    }
    
    /**
     * Test send link activation functionality.
     * Verify remote message to activate/deactivate link is sent.  
     */
    @Test
    public void testSendLinkActivation()
    {
        CommsLayerLinkModelImpl linky = new CommsLayerLinkModelImpl(123, uuid, pid, pid, 
                m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        linky.setActivated(false);
        m_SUT.sendLinkActivationRequest(linky, 123);
        
        verify(m_MessageFactory).createLinkLayerMessage(eq(LinkLayerMessageType.ActivateRequest), 
                Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(Mockito.anyInt(), (ResponseHandler) eq(null));
        
        linky.setActivated(true);
        m_SUT.sendLinkActivationRequest(linky, 123);
        
        verify(m_MessageFactory).createLinkLayerMessage(eq(LinkLayerMessageType.DeactivateRequest), 
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(2)).queue(Mockito.anyInt(), 
                (ResponseHandler) eq(null));        
    }
    
    /**
     * Test send link perform BIT functionality.
     * Verify remote message to perform a BIT on a link is sent.  
     */
    @Test
    public void testSendLinkPerformBit()
    {
        CommsLayerLinkModelImpl linky = new CommsLayerLinkModelImpl(123, uuid, pid, pid,
                m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        m_SUT.sendLinkPerformBitRequest(linky, 123);
        
        verify(m_MessageFactory).createLinkLayerMessage(eq(LinkLayerMessageType.PerformBITRequest), 
                Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(Mockito.anyInt(), (ResponseHandler) eq(null));        
    }
    
    /**
     * Test remove stack functionality.
     * Verify names of layers to be removed.
     * Verify remote message to remove transport and link layer is sent.  
     */   
    @Test
    public void testRemoveStack()
    {
        CommsStackModelImpl stack = new CommsStackModelImpl(m_CommsImageInterface);
        CommsLayerBaseModel trans = new CommsLayerBaseModel(0, uuid, pid, "clazz", 
                CommType.TransportLayer, m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        CommsLayerLinkModelImpl link = new CommsLayerLinkModelImpl(0, uuid2, pid, pid, 
                m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        
        stack.setTransport(trans);
        stack.setLink(link);
        
        m_SUT.setRemoveStack(stack);
        
        String removeNames = m_SUT.getRemoveStackLayerNames();
        
        assertThat(removeNames.contains(uuid.toString()), is(true));
        assertThat(removeNames.contains(uuid2.toString()), is(true));  
        
        m_SUT.sendRemoveStackRequest(123);
        
        verify(m_MessageFactory).createTransportLayerMessage(eq(TransportLayerMessageType.
                DeleteRequest), Mockito.any(Message.class));
        verify(m_MessageWrapper, times(2)).queue(Mockito.anyInt(), (ResponseHandler) eq(null));
        
        verify(m_MessageFactory).createLinkLayerMessage(eq(LinkLayerMessageType.DeleteRequest), 
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(2)).queue(Mockito.anyInt(), (ResponseHandler) eq(null));
        
        //test null values
        stack.setTransport(null);
        stack.setLink(null);
        
        assertThat( m_SUT.getRemoveStackLayerNames(), is(""));
    }    
    
    /**
     * Verify that with no stack to remove set there are no remote messages produced/sent.
     */
    @Test
    public void testRemoveStackWithRemoveStackNull()
    {
        m_SUT.setRemoveStack(null);
        
        m_SUT.sendRemoveStackRequest(123);
        
        verify(m_MessageFactory, never()).createTransportLayerMessage(eq(TransportLayerMessageType.
                DeleteRequest), Mockito.any(Message.class));
        verify(m_MessageWrapper, never()).queue(Mockito.anyInt(), (ResponseHandler) eq(null));
        
        verify(m_MessageFactory, never()).createLinkLayerMessage(eq(LinkLayerMessageType.DeleteRequest), 
                Mockito.any(Message.class));
        verify(m_MessageWrapper, never()).queue(Mockito.anyInt(), (ResponseHandler) eq(null));
    }
    
    /**
     * Verify that with no stack to remove set an empty string is returned.
     */
    @Test
    public void testGetRemoveStackLayerNamesWithRemoveStackNull()
    {
        m_SUT.setRemoveStack(null);
        
        String removeLayers = m_SUT.getRemoveStackLayerNames();
        
        assertThat(removeLayers, is(""));
    }
    
    /**
     * Verify that with only a link layer model set, only the link layer name is returned.
     */
    @Test
    public void testRemoveStackLayerNamesWithLinkOnly()
    {
        CommsStackModelImpl stack = new CommsStackModelImpl(m_CommsImageInterface);
        CommsLayerLinkModelImpl link = new CommsLayerLinkModelImpl(0, uuid2, pid, pid, 
                m_FactoryMgr, m_TypesMgr, m_ConfigWrapper, m_CommsImageInterface);
        stack.setLink(link);
        
        m_SUT.setRemoveStack(stack);
        
        String removeLayers = m_SUT.getRemoveStackLayerNames();
        
        assertThat(removeLayers.contains(uuid2.toString()), is(true));
    }
}
