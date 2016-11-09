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
package mil.dod.th.ose.gui.remotesystemencryption;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.RemoteSystemEncryption;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.ose.gui.api.ControllerEncryptionConstants;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.remotesystemencryption.EncryptionTypeManagerImpl.EventHelperChannelEvent;
import mil.dod.th.ose.gui.remotesystemencryption.EncryptionTypeManagerImpl.EventHelperEncryptionInfoNamespace;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

/**
 * @author nickmarcucci
 *
 */
public class TestEncryptionTypeManagerImpl
{
    private EncryptionTypeManagerImpl m_SUT;
    private BundleContext m_BundleContext;
    private EventHelperEncryptionInfoNamespace m_EncryptionInfoEventHelper;
    private EventHelperChannelEvent m_ChannelEventHelper;
    private EventAdmin m_EventAdmin;

    private MessageFactory m_MessageFactory;
    private MessageWrapper m_MessageWrapper;
    private RemoteSystemEncryption m_RemoteSystemEncryption;
    
    @SuppressWarnings("rawtypes") 
    private ServiceRegistration m_HandlerReg;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        m_SUT = new EncryptionTypeManagerImpl();
        
        m_BundleContext = mock(BundleContext.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_RemoteSystemEncryption = mock(RemoteSystemEncryption.class);
        m_HandlerReg = mock(ServiceRegistration.class);
        
        when(m_BundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        when(m_MessageFactory.createEncryptionInfoMessage(
                EncryptionInfoMessageType.GetEncryptionTypeRequest, null)).thenReturn(m_MessageWrapper);
        
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setRemoteSystemEncryption(m_RemoteSystemEncryption);
        
        m_SUT.activate(m_BundleContext);
        
        //verify event listeners are registered
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_BundleContext, times(2)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        
        m_EncryptionInfoEventHelper = (EventHelperEncryptionInfoNamespace)captor.getAllValues().get(0);
        m_ChannelEventHelper = (EventHelperChannelEvent)captor.getAllValues().get(1);   
    }
    
    @After
    public void teardown()
    {
        m_SUT.deactivate();
    }
    
    /**
     * verify services are unregistered
     */
    @Test
    public void testDeactivate()
    {
        m_SUT.deactivate();
        //once for controller event class, once for encryption event class
        verify(m_HandlerReg, times(2)).unregister();
    }
    
    /**
     * Verify that the remote system encryption service is invoked to cleanup entries when a channel removed 
     * message is received.
     */
    @Test
    public void testRemoveChannelEvent()
    {
        //pretend the channel has been removed
        Event removeChannelEvent = new Event(RemoteChannelLookup.TOPIC_CHANNEL_REMOVED, new HashMap<String, Object>());
        m_ChannelEventHelper.handleEvent(removeChannelEvent);
        
        verify(m_RemoteSystemEncryption).cleanupSystemEncryptionTypes();
    }
    
    /**
     * Verify that encryption type information is requested if the encryption type is not known.
     */
    @Test
    public void tetGetEncryptionTypeAsyncUnknownSystem()
    {
        int controllerId = 123;
        
        assertThat(m_SUT.getEncryptTypeAsnyc(controllerId), is(nullValue()));
        
        verify(m_MessageFactory).createEncryptionInfoMessage(EncryptionInfoMessageType.GetEncryptionTypeRequest, null);
        verify(m_MessageWrapper).queue(controllerId, null);
    }
    
    /**
     * Verify that encryption type is returned if type is known for the requested system.
     */
    @Test
    public void tetGetEncryptionTypeAsyncKnownSystem()
    {
        int controllerId = 123;
        
        when(m_RemoteSystemEncryption.getEncryptType(controllerId)).thenReturn(EncryptType.AES_ECDH_ECDSA);
        
        //mock encryption response for new controller
        Event event = mockEncryptionTypeResponse(controllerId, EncryptType.AES_ECDH_ECDSA);
        m_EncryptionInfoEventHelper.handleEvent(event);
        
        assertThat(m_SUT.getEncryptTypeAsnyc(controllerId), is(EncryptType.AES_ECDH_ECDSA));
    }
    
    /**
     * Verify that an encryption type response for an unknown system generates an encrypt type updated
     * event and the entry is saved to the remote system encryption service.
     */
    @Test
    public void testEncryptionResponseMsgUnknownSystem()
    {
        int controllerId = 123;
        
        when(m_RemoteSystemEncryption.getEncryptType(controllerId)).thenReturn(null);
        
        //mock encryption response for new controller
        Event event = mockEncryptionTypeResponse(controllerId, EncryptType.AES_ECDH_ECDSA);
        m_EncryptionInfoEventHelper.handleEvent(event);
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(captor.capture());
        
      //assert that first encryption type updated event is posted
        String topic = captor.getAllValues().get(0).getTopic();
        String type = (String)captor.getAllValues().get(0).getProperty(ControllerEncryptionConstants.
                EVENT_PROP_ENCRYPTION_TYPE);
        int systemId = (Integer)captor.getAllValues().get(0).getProperty(SharedPropertyConstants.
                EVENT_PROP_CONTROLLER_ID);
        assertThat(topic, is(ControllerEncryptionConstants.TOPIC_CONTROLLER_ENCRYPTION_TYPE_UPDATED));
        assertThat(EncryptType.valueOf(type), is(EncryptType.AES_ECDH_ECDSA));
        assertThat(systemId, is(controllerId));
        
        verify(m_RemoteSystemEncryption).addEncryptionTypeForSystem(controllerId, EncryptType.AES_ECDH_ECDSA);
    }
    
    /**
     * Verify that an encryption type response for an known system that has changed its encryption type
     * generates an encrypt type updated event and the entry is saved to the remote system encryption service.
     */
    @Test
    public void testEncryptionResponseMsgKnownSystemAndChangedType()
    {
        int controllerId = 123;
        
        when(m_RemoteSystemEncryption.getEncryptType(controllerId)).thenReturn(EncryptType.AES_ECDH_ECDSA);
        
        //mock encryption response for new controller
        Event event = mockEncryptionTypeResponse(controllerId, EncryptType.NONE);
        m_EncryptionInfoEventHelper.handleEvent(event);
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(captor.capture());
        
      //assert that first encryption type updated event is posted
        String topic = captor.getAllValues().get(0).getTopic();
        String type = (String)captor.getAllValues().get(0).getProperty(ControllerEncryptionConstants.
                EVENT_PROP_ENCRYPTION_TYPE);
        int systemId = (Integer)captor.getAllValues().get(0).getProperty(SharedPropertyConstants.
                EVENT_PROP_CONTROLLER_ID);
        assertThat(topic, is(ControllerEncryptionConstants.TOPIC_CONTROLLER_ENCRYPTION_TYPE_UPDATED));
        assertThat(EncryptType.valueOf(type), is(EncryptType.NONE));
        assertThat(systemId, is(controllerId));
        
        verify(m_RemoteSystemEncryption).addEncryptionTypeForSystem(controllerId, EncryptType.NONE);
    }
    
    /**
     * Verify that an encryption type response for an known system that has not changed its encryption type
     * does not generate an encrypt type updated event and the entry is not saved to the 
     * remote system encryption service.
     */
    @Test
    public void testEncryptionResponseMsgKnownSystemAndUnchangedType()
    {
        int controllerId = 123;
        
        when(m_RemoteSystemEncryption.getEncryptType(controllerId)).thenReturn(EncryptType.AES_ECDH_ECDSA);
        
        //mock encryption response for new controller
        Event event = mockEncryptionTypeResponse(controllerId, EncryptType.AES_ECDH_ECDSA);
        m_EncryptionInfoEventHelper.handleEvent(event);
        
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
        verify(m_RemoteSystemEncryption, never()).addEncryptionTypeForSystem(controllerId, EncryptType.AES_ECDH_ECDSA);
    }
    
    /**
     * Helper method for mocking a GetLayersResponse from the controller.
     */
    private Event mockEncryptionTypeResponse(final int systemId, EncryptType type)
    {
        GetEncryptionTypeResponseData response = GetEncryptionTypeResponseData.newBuilder().setType(type).build();
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.EncryptionInfo.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            CustomCommsMessageType.GetLayersResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);        
    }  
}
