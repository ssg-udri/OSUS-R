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
import static org.mockito.Mockito.*;

import java.util.UUID;

import mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.ose.gui.webapp.comms.CommsMgrImpl.RemoteCreateLinkLayerHandler;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.types.ccomm.CustomCommTypesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.protobuf.Message;

/**
 * Test implementation of {@link AddCommsMessageController}.
 * @author allenchl
 *
 */
public class TestAddCommsMessageControllerImpl 
{
    private AddCommsMessageControllerImpl m_SUT;
    private CommsMgr m_CommsMgr;
    private MessageFactory m_MessageFactory;
    private MessageWrapper m_MessageWrapper;
    
    @Before
    public void setup()
    {
        m_SUT = new AddCommsMessageControllerImpl();
        
        //mock services
        m_CommsMgr = mock(CommsMgr.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        //set services
        m_SUT.setCommsMgr(m_CommsMgr);
        m_SUT.setMessageFactory(m_MessageFactory);
        
        when(m_MessageFactory.createCustomCommsMessage(eq(CustomCommsMessageType.CreateLinkLayerRequest),
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createCustomCommsMessage(eq(CustomCommsMessageType.CreatePhysicalLinkRequest),
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
    }
    
    /**
     * Verify that when presented a model and controller ID, that requests to create comms layers are sent.
     */
    @Test
    public void testSubmitNewCommsStackRequest()
    {
        //physical link uuid
        UUID uuid_phys = UUID.randomUUID();
        
        //mock link layer handler
        RemoteCreateLinkLayerHandler handler = mock(RemoteCreateLinkLayerHandler.class);
        
        //comms service  behavior
        when(m_CommsMgr.getPhysicalUuidByName("fizzyPhys", 0)).thenReturn(uuid_phys);
        when(m_CommsMgr.createLinkLayerHandler(Mockito.any(CreateTransportLayerRequestData.Builder.class))).
            thenReturn(handler);
        
        m_SUT.submitNewCommsStackRequest(0, getCompleteModel(PhysicalLinkTypeEnum.I_2_C));
        
        //capture the message sent and built message for the transport layer
        ArgumentCaptor<CreateLinkLayerRequestData> linkMessageCap = 
                ArgumentCaptor.forClass(CreateLinkLayerRequestData.class);
        verify(m_MessageFactory).createCustomCommsMessage(
                eq(CustomCommsMessageType.CreateLinkLayerRequest), linkMessageCap.capture());
        ArgumentCaptor<CreateTransportLayerRequestData.Builder> transMessageCap = 
                ArgumentCaptor.forClass(CreateTransportLayerRequestData.Builder.class);
        verify(m_CommsMgr).createLinkLayerHandler(transMessageCap.capture());
        verify(m_MessageWrapper).queue(0, handler);
        
        //inspect transport layer message builder that was captured
        CreateTransportLayerRequestData.Builder transCapBuilder = transMessageCap.getValue();
        assertThat(transCapBuilder.getTransportLayerName(), is("transyTrans"));
        assertThat(transCapBuilder.getTransportLayerProductType(), is("example.trans.type"));
        
        //inspect link layer captured message
        CreateLinkLayerRequestData linkRequest = linkMessageCap.getValue();
        assertThat(linkRequest.getLinkLayerProductType(), is("example.link.type"));
        assertThat(linkRequest.getLinkLayerName(), is("linkyLink"));
        assertThat(linkRequest.getPhysicalLinkUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid_phys)));
    }

    /**
     * Verify that presented a model to force create a physical and controller ID, 
     * that the request to create the physical link is sent.
     */
    @Test
    public void testSubmitNewCommsStackRequestForPhysicalLink()
    {
        final PhysicalLinkCapabilities physCaps = mock(PhysicalLinkCapabilities.class);
        when(physCaps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.SERIAL_PORT);
        //get a model and set force phys to true
        CommsStackCreationModel model = getCompleteModel(PhysicalLinkTypeEnum.SERIAL_PORT);
        model.setForceAdd(true);
        
        m_SUT.submitNewCommsStackRequest(0, model);
        
        //capture the message sent
        ArgumentCaptor<CreatePhysicalLinkRequestData> physMessageCap = 
                ArgumentCaptor.forClass(CreatePhysicalLinkRequestData.class);
        verify(m_MessageFactory).createCustomCommsMessage(
                eq(CustomCommsMessageType.CreatePhysicalLinkRequest), physMessageCap.capture());

        verify(m_MessageWrapper).queue(0, null);
        
        //inspect physical link layer captured message
        CreatePhysicalLinkRequestData plinkRequest = physMessageCap.getValue();
        assertThat(plinkRequest.getPhysicalLinkType(), 
            is(CustomCommTypesGen.PhysicalLinkType.Enum.SERIAL_PORT));
        assertThat(plinkRequest.getPhysicalLinkName(), is("tobyPhys"));
    }
    
    /**
     * Verify that presented a model to force create a physical and controller ID, 
     * that the request to create the physical link is sent.
     */
    @Test
    public void testSubmitNewCommsStackRequestForPhysicalLinkGPIO()
    {
        final PhysicalLinkCapabilities physCaps = mock(PhysicalLinkCapabilities.class);
        when(physCaps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.GPIO);
        //get a model and set force phys to true
        CommsStackCreationModel model = getCompleteModel(PhysicalLinkTypeEnum.GPIO);
        model.setForceAdd(true);
        
        m_SUT.submitNewCommsStackRequest(0, model);
        
        //capture the message sent
        ArgumentCaptor<CreatePhysicalLinkRequestData> physMessageCap = 
                ArgumentCaptor.forClass(CreatePhysicalLinkRequestData.class);
        verify(m_MessageFactory).createCustomCommsMessage(
                eq(CustomCommsMessageType.CreatePhysicalLinkRequest), physMessageCap.capture());

        verify(m_MessageWrapper).queue(0, null);
        
        //inspect physical link layer captured message
        CreatePhysicalLinkRequestData plinkRequest = physMessageCap.getValue();
        assertThat(plinkRequest.getPhysicalLinkType(), 
            is(CustomCommTypesGen.PhysicalLinkType.Enum.GPIO));
        assertThat(plinkRequest.getPhysicalLinkName(), is("tobyPhys"));
    }
    
    /**
     * Verify that presented a model to force create a physical and controller ID, 
     * that the request to create the physical link is sent.
     */
    @Test
    public void testSubmitNewCommsStackRequestForPhysicalLinkSPI()
    {
        final PhysicalLinkCapabilities physCaps = mock(PhysicalLinkCapabilities.class);
        when(physCaps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.SPI);
        //get a model and set force phys to true
        CommsStackCreationModel model = getCompleteModel(PhysicalLinkTypeEnum.SPI);
        model.setForceAdd(true);
        
        m_SUT.submitNewCommsStackRequest(0, model);
        
        //capture the message sent
        ArgumentCaptor<CreatePhysicalLinkRequestData> physMessageCap = 
                ArgumentCaptor.forClass(CreatePhysicalLinkRequestData.class);
        verify(m_MessageFactory).createCustomCommsMessage(
                eq(CustomCommsMessageType.CreatePhysicalLinkRequest), physMessageCap.capture());

        verify(m_MessageWrapper).queue(0, null);
        
        //inspect physical link layer captured message
        CreatePhysicalLinkRequestData plinkRequest = physMessageCap.getValue();
        assertThat(plinkRequest.getPhysicalLinkType(), 
            is(CustomCommTypesGen.PhysicalLinkType.Enum.SPI));
        assertThat(plinkRequest.getPhysicalLinkName(), is("tobyPhys"));
    }
    
    /**
     * Verify that presented a model to force create a physical and controller ID, 
     * that the request to create the physical link is sent.
     */
    @Test
    public void testSubmitNewCommsStackRequestForPhysicalLinkI2C()
    {
        final PhysicalLinkCapabilities physCaps = mock(PhysicalLinkCapabilities.class);
        when(physCaps.getLinkType()).thenReturn(PhysicalLinkTypeEnum.I_2_C);
        //get a model and set force phys to true
        CommsStackCreationModel model = getCompleteModel(PhysicalLinkTypeEnum.I_2_C);
        model.setForceAdd(true);
        
        m_SUT.submitNewCommsStackRequest(0, model);
        
        //capture the message sent
        ArgumentCaptor<CreatePhysicalLinkRequestData> physMessageCap = 
                ArgumentCaptor.forClass(CreatePhysicalLinkRequestData.class);
        verify(m_MessageFactory).createCustomCommsMessage(
                eq(CustomCommsMessageType.CreatePhysicalLinkRequest), physMessageCap.capture());

        verify(m_MessageWrapper).queue(0, null);
        
        //inspect physical link layer captured message
        CreatePhysicalLinkRequestData plinkRequest = physMessageCap.getValue();
        assertThat(plinkRequest.getPhysicalLinkType(), 
            is(CustomCommTypesGen.PhysicalLinkType.Enum.I_2_C));
        assertThat(plinkRequest.getPhysicalLinkName(), is("tobyPhys"));
    }
    
    /**
     * Verify that when presented a model and controller ID, that a request to create a link layer
     * does not pull in transport layer information.
     */
    @Test
    public void testSubmitNewCommsStackRequestLinkLayer()
    {
        //physical link uuid
        UUID uuid_phys = UUID.randomUUID();
        //get a model and un-set trans info 
        CommsStackCreationModel model = getCompleteModel(PhysicalLinkTypeEnum.I_2_C);
        model.setNewTransportName(null);
        model.setSelectedTransportLayerType(null);
        
        //comms service  behavior
        when(m_CommsMgr.getPhysicalUuidByName("fizzyPhys", 0)).thenReturn(uuid_phys);
        
        m_SUT.submitNewCommsStackRequest(0, model);
        
        //capture the message sent and built message for the transport layer
        ArgumentCaptor<CreateLinkLayerRequestData> linkMessageCap = 
                ArgumentCaptor.forClass(CreateLinkLayerRequestData.class);
        verify(m_MessageFactory).createCustomCommsMessage(
                eq(CustomCommsMessageType.CreateLinkLayerRequest), linkMessageCap.capture());
        
        //verify  no link handler is created for trans
        verify(m_CommsMgr, never()).createLinkLayerHandler(Mockito.any(CreateTransportLayerRequestData.Builder.class));
        
        //capture handler value, should be null
        ArgumentCaptor<ResponseHandler> handlerCap = ArgumentCaptor.forClass(ResponseHandler.class);
        verify(m_MessageWrapper).queue(eq(0), handlerCap.capture());
        assertThat(handlerCap.getValue(), is(nullValue()));
        
        //inspect link layer captured message
        CreateLinkLayerRequestData linkRequest = linkMessageCap.getValue();
        assertThat(linkRequest.getLinkLayerProductType(), is("example.link.type"));
        assertThat(linkRequest.getLinkLayerName(), is("linkyLink"));
        assertThat(linkRequest.getPhysicalLinkUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(uuid_phys)));
    }
    
    /**
     * Helper method for setting all values for a comms creation model.
     */
    private CommsStackCreationModel getCompleteModel(final PhysicalLinkTypeEnum type)
    {
        CommsStackCreationModel model = new CommsStackCreationModel();
        model.setNewLinkName("linkyLink");
        model.setNewTransportName("transyTrans");
        model.setForceAdd(false);
        model.setSelectedPhysicalType(type);
        model.setSelectedPhysicalLink("fizzyPhys");
        model.setSelectedTransportLayerType("example.trans.type");
        model.setSelectedLinkLayerType("example.link.type");
        model.setNewPhysicalName("tobyPhys");
        
        return model;
    }
}
