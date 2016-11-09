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
package mil.dod.th.ose.remote.encryption;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionErrorCode;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoErrorResponseData;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.Message;

/**
 * Test class for the {@link EncryptInfoMessageService} class.
 * 
 * @author cweisenborn
 */
public class TestEncryptInfoMessageService
{
    private EncryptInfoMessageService m_SUT;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private MessageRouterInternal m_MessageRouter;
    private RemoteSettings m_RemoteSettings;
    private RemoteChannel m_RemoteChannel;
    
    @Before
    public void setup()
    {
        m_EventAdmin = mock(EventAdmin.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_RemoteSettings = mock(RemoteSettings.class);
        m_RemoteChannel = mock(RemoteChannel.class);
        
        m_SUT = new EncryptInfoMessageService();
        
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setRemoteSettings(m_RemoteSettings);
        
        m_SUT.activate();
        
        verify(m_MessageRouter).bindMessageService(m_SUT);
    }
    
    /**
     * Verify that the deactivate method unbinds the message service from the message router.
     */
    @Test
    public void testDeactivate()
    {
        m_SUT.deactivate();
        
        verify(m_MessageRouter).unbindMessageService(m_SUT);
    }
    
    /**
     * Verify the namespace is encryption info
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.EncryptionInfo));
    }
    
    /**
     * Verify that encryption info request message is handled appropriately.
     */
    @Test
    public void testEncyptionInfoRequest() throws IOException
    {
        MessageResponseWrapper wrapper = mock(MessageResponseWrapper.class);
        
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.AES_ECDH_ECDSA);
        when(m_MessageFactory.createEncryptionInfoResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                eq(EncryptionInfoMessageType.GetEncryptionTypeResponse), 
                Mockito.any(GetEncryptionTypeResponseData.class))).thenReturn(wrapper);
        when(wrapper.queue(m_RemoteChannel)).thenReturn(true);
                
        EncryptionInfoNamespace namepsaceMessage = EncryptionInfoNamespace.newBuilder().setType(
                EncryptionInfoMessageType.GetEncryptionTypeRequest).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 5, 
                Namespace.EncryptionInfo, 225, namepsaceMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        m_SUT.handleMessage(thMessage, payload, m_RemoteChannel);
        
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(wrapper).queue(m_RemoteChannel);
        verify(m_MessageFactory).createEncryptionInfoResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                eq(EncryptionInfoMessageType.GetEncryptionTypeResponse), messageCaptor.capture());
        
        GetEncryptionTypeResponseData response = (GetEncryptionTypeResponseData)messageCaptor.getValue();
        assertThat(response.getType(), is(EncryptType.AES_ECDH_ECDSA));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event postedEvent = eventCaptor.getValue();
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(EncryptionInfoMessageType.GetEncryptionTypeRequest.toString()));
        assertThat((TerraHarvestPayload)postedEvent.getProperty(RemoteConstants.EVENT_PROP_PAYLOAD), is(payload));
    }
    
    /**
     * Verify that the encryption response message is handled appropriately.
     */
    @Test
    public void testEncryptionInfoResponse() throws IOException
    {        
        GetEncryptionTypeResponseData infoResponse = 
                GetEncryptionTypeResponseData.newBuilder().setType(EncryptType.AES_ECDH_ECDSA).build();
        EncryptionInfoNamespace namepsaceMessage = EncryptionInfoNamespace.newBuilder().setType(
                EncryptionInfoMessageType.GetEncryptionTypeResponse).setData(infoResponse.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 5, 
                Namespace.EncryptionInfo, 225, namepsaceMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        m_SUT.handleMessage(thMessage, payload, m_RemoteChannel);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event postedEvent = eventCaptor.getValue();
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(EncryptionInfoMessageType.GetEncryptionTypeResponse.toString()));
        assertThat((TerraHarvestPayload)postedEvent.getProperty(RemoteConstants.EVENT_PROP_PAYLOAD), is(payload));
        GetEncryptionTypeResponseData eventDataMessage = 
                (GetEncryptionTypeResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
        assertThat(eventDataMessage.getType(), is(EncryptType.AES_ECDH_ECDSA));
    }
    
    /**
     * Verify that the encryption error response message is handled appropriately.
     */
    @Test
    public void testEncryptionInfoErrorResponse() throws IOException
    {
        EncryptionInfoErrorResponseData errorResponse = EncryptionInfoErrorResponseData.newBuilder().setError(
                EncryptionErrorCode.INVALID_ENCRYPTION_LEVEL).setErrorDescription("the system was too lazy to decrypt " 
                        + "your message... sorry").setType(EncryptType.AES_ECDH_ECDSA).build();
        EncryptionInfoNamespace namepsaceMessage = EncryptionInfoNamespace.newBuilder().setType(
                EncryptionInfoMessageType.EncryptionInfoErrorResponse).setData(errorResponse.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(0, 5, 
                Namespace.EncryptionInfo, 225, namepsaceMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        m_SUT.handleMessage(thMessage, payload, m_RemoteChannel);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event postedEvent = eventCaptor.getValue();
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(EncryptionInfoMessageType.EncryptionInfoErrorResponse.toString()));
        assertThat((TerraHarvestPayload)postedEvent.getProperty(RemoteConstants.EVENT_PROP_PAYLOAD), is(payload));
    }
}
