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
package mil.dod.th.ose.remote.osgi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.MarshalException;
import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolStringList;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.EventMessages.SendEventData;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.MapTypes.ComplexTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.api.RemoteEventAdmin;
import mil.dod.th.ose.remote.api.RemoteEventRegistration;
import mil.dod.th.ose.remote.proto.PersistEventRegistration.PersistentEventRegistrationMessage;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.remote.util.RemotePropertyConverter;
import mil.dod.th.ose.shared.EventUtils;
import mil.dod.th.ose.shared.ExceptionLoggingThreadPool;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * This class is responsible for managing OSGi EventAdmin messages received through the remote interface
 * and sending proper responses according to different incoming event admin request messages.  
 * @author bachmakm
 *
 */
//Remote event admin is provided but not other service because this would create a cycle, instead this class 
//registers with the message router
@Component(immediate = true, provide = {RemoteEventAdmin.class}) //NOCHECKSTYLE: High class complexity fanout. 
//This is true because of the  amount of pivotal interactions this service facilitates.
public class EventAdminMessageService implements MessageService, RemoteEventAdmin
{
    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;

    /**
     * Reference to the event admin service.  Used for local messages within event admin service.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;

    /**
     * Running counter, value is the last id used for the last registered event handler. 
     */
    private int m_LastRegId;

    /**
     * Context for the bundle containing this component.
     */
    private BundleContext m_Context;

    /**
     * Map of all registrations, key is the registration id.
     */
    private final Map<Integer, RemoteEventRegistration> m_Registrations = 
            new HashMap<Integer, RemoteEventRegistration>();

    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;

    /**
     * Service that assists with converting the event properties to and from protocol buffer equivalents.
     */
    private RemotePropertyConverter m_RemotePropertyConverter;
    
    /**
     * Persistent datastore service.
     */
    private PersistentDataStore m_Datastore;

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }

    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      service used to post events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }

    /**
     * Bind to the service for creating remote messages.
     * 
     * @param messageFactory
     *      service that create messages
     */
    @Reference
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }

    /**
     * Bind a message router to register.
     * 
     * @param messageRouter
     *      router that handles incoming messages
     */
    @Reference
    public void setMessageRouter(final MessageRouterInternal messageRouter)
    {
        m_MessageRouter = messageRouter;
    }
    
    /**
     * Bind a remote converter service.
     * @param conversionService
     *     the conversion service to use
     */
    @Reference
    public void setRemotePropertyConverter(final RemotePropertyConverter conversionService)
    {
        m_RemotePropertyConverter = conversionService;
    }

    /**
     * Set the persistent data store service.
     * @param datastore
     *     the datastore to use
     */
    @Reference
    public void setPersistentDataStore(final PersistentDataStore datastore)
    {
        m_Datastore = datastore;
    }

    /**
     * Activate this component, just save the context for later use and bind this service to the message router.
     * 
     * @param context
     *      context for this bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;
        
        m_MessageRouter.bindMessageService(this);
        
        //get any persisted registrations
        final Collection<PersistentData> datas = m_Datastore.query(getClass());
        //iterate through them and re-register
        for (PersistentData data : datas)
        {
            final byte[] bytes = (byte[])data.getEntity();
            //create message
            final PersistentEventRegistrationMessage message;
            try
            {
                message = PersistentEventRegistrationMessage.parseFrom(bytes);
            }
            catch (final IOException e)
            {
                m_Logging.error(e, "Unable to pull a remote event registration with ID %s from the datastore.", 
                        data.getDescription());
                //just continue with the next entity
                continue;
            }
            //re-register for the events
            final int regId = Integer.parseInt(data.getDescription());
            //check if registration id just pulled out is higher than other regIDs
            if (regId > m_LastRegId)
            {
                m_LastRegId = regId;
            }
            final EventRegistrationRequestData dataMsg = message.getRegMessage();
            registerListenerLocally(regId, dataMsg, message.getSystemId(), message.getEncryptionType());
        }
    }
    
    /**
     * Deactivate the service, clean out all event registrations and unbind the service from the message router.
     */
    @Deactivate
    public void deactivate()
    {
        m_MessageRouter.unbindMessageService(this);
        
        cleanupAllRegistrations();
    }
    
    @Override
    public Namespace getNamespace()
    {
        return Namespace.EventAdmin;
    }

    @Override
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload,
        final RemoteChannel channel) throws IOException, ObjectConverterException, UnmarshalException
    {
        //parse event message
        final EventAdminNamespace eventMessage = EventAdminNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;
        
        switch (eventMessage.getType())
        {
            case SendEvent:
                //Send event message type was received.  Respond accordingly.
                dataMessage = sendEvent(message, eventMessage);
                break;
            case EventRegistrationRequest:
                //Register for event message type was received.  Respond accordingly.  
                dataMessage = registerForEvent(message, eventMessage, channel);
                break;
            case EventRegistrationResponse:
                dataMessage = EventRegistrationResponseData.parseFrom(eventMessage.getData());
                break;
            case UnregisterEventRequest:
                //Unregister for event message type was received.  Respond accordingly.
                dataMessage = unregister(message, eventMessage, channel);
                break;
            case UnregisterEventResponse:
                dataMessage = null;
                break;
            case CleanupRequest:
                //Clean up message type was received.  Respond accordingly.
                dataMessage = null; // NOPMD, assigning null, explicitly setting no data
                cleanup(message, channel);
                break;
            case CleanupResponse:
                dataMessage = null;
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the EventAdminMessageService namespace.", eventMessage.getType()));
        }
        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, eventMessage, 
                eventMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }
    
    @Override
    public Map<Integer, RemoteEventRegistration> getRemoteEventRegistrations()
    {
        //return registrations
        return Collections.unmodifiableMap(m_Registrations);
    }
    
    @Override
    public void addRemoteEventRegistration(final int systemId, final EventRegistrationRequestData eventRegMessage)
    {
        m_Logging.debug("Adding event registration from RemoteEventAdmin: %s", eventRegMessage);
        registerListenerLocally(++m_LastRegId, eventRegMessage, systemId, EncryptType.NONE);
    }
    
    /**
     * Method that locally posts an event based on a received remote event message and its
     * corresponding properties and topic. 
     * @param request
     *      entire remote message for the request 
     * @param message
     *      message containing the properties and topic of a remote event
     * @throws InvalidProtocolBufferException 
     *      if message cannot be properly parsed
     * @return
     *      the data message for this request 
     * @throws ObjectConverterException
     *      if object cannot be converted between JAXB and protocol buffer format
     * @throws UnmarshalException
     *      if unable to parse XML as JAXB object
     */
    private Message sendEvent(final TerraHarvestMessage request, final EventAdminNamespace message) 
            throws InvalidProtocolBufferException, ObjectConverterException, UnmarshalException
    {
        final SendEventData regRequest = SendEventData.parseFrom(message.getData());
        final Map<String, Object> props = m_RemotePropertyConverter.complexTypesMapToMap(regRequest.getPropertyList());
        
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, request.getSourceId());
        m_EventAdmin.postEvent(new Event(RemoteInterfaceUtilities.getRemoteEventTopic(regRequest.getTopic()), props));
        
        m_Logging.debug("Received remote event [%s]", regRequest.getTopic());
        
        return regRequest;
    }

    /**
     * Method that registers for notifications of certain events based on the remote 
     * event topic and filter contained within the incoming message.  
     * 
     * @param request
     *      entire remote message for the request
     * @param message
     *      message containing the string filter and topic of a remote event
     * @param channel
     *      channel to use for sending a response
     * @throws IOException 
     *      if message cannot be parsed or if response message cannot be sent
     * @return
     *      the data message for this request 
     */
    private Message registerForEvent(final TerraHarvestMessage request, final EventAdminNamespace message, 
            final RemoteChannel channel) throws IOException 
    {
        //parse the message
        final EventRegistrationRequestData requestData = 
                EventRegistrationRequestData.parseFrom(message.getData());

        Integer regId = registrationExists(requestData, request.getSourceId(), request.getEncryptType());
        if (regId != -1)
        {
            m_Logging.debug("Use existing registration %d", regId);
        }
        else
        {
            registerListenerLocally(++m_LastRegId, requestData, request.getSourceId(), request.getEncryptType());
            regId = m_LastRegId;

            try
            {
                persistRegistration(requestData, m_LastRegId, request.getSourceId(), request.getEncryptType());
            }
            catch (final PersistenceFailedException exception) 
            {
                // send appropriate response
                final String errorDesc = "Failed persisting the remote registration, the registration will not be "
                        + "known if the system restarts.";
                m_MessageFactory.createBaseErrorMessage(request, ErrorCode.PERSIST_ERROR,
                        errorDesc + exception.getMessage()).queue(channel);
                m_Logging.error(exception, errorDesc);
                return requestData;
            }
        }
        
        //construct response message
        final EventRegistrationResponseData data = EventRegistrationResponseData.newBuilder().
                setId(regId).build();
        m_MessageFactory.createEventAdminResponseMessage(request, EventAdminMessageType.EventRegistrationResponse, 
                data).queue(channel);
        
        m_Logging.info("Registered to listen to events remotely with given request: %s", requestData.toString());
        
        return requestData;
    }

    /**
     * Method that is responsible for unregistering remote event notifications via a registration ID
     * contained in the event message.
     * 
     * @param request
     *      entire remote message for the request
     * @param message
     *      message containing the registration ID of the event to be unregistered 
     * @param channel
     *      channel to use for sending a response
     * @throws IOException 
     *      if message cannot be parsed or if response message cannot be sent
     * @return
     *      the data message for this request 
     */
    private Message unregister(final TerraHarvestMessage request, final EventAdminNamespace message, 
            final RemoteChannel channel) throws IOException 
    {
        final UnregisterEventRequestData requestData = UnregisterEventRequestData.parseFrom(message.getData());
        //TD: id might not be valid, should check input before using it
        final RemoteEventRegistration eventReg = m_Registrations.remove(requestData.getId());  
        final ServiceRegistration<EventHandler> serviceReg = eventReg.getServiceRegistration();
        serviceReg.unregister();
        
        //remove for datastore
        removePersistedRegistration(requestData.getId());
        
        //construct response message      
        m_MessageFactory.createEventAdminResponseMessage(request, EventAdminMessageType.UnregisterEventResponse, 
                null).queue(channel);
        
        return requestData;
    }

    /**
     * Method responsible for cleaning up event registrations by unregistering all event registrations.
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @throws IOException 
     *      if remote message sender cannot send response message
     */
    private void cleanup(final TerraHarvestMessage request, final RemoteChannel channel) throws IOException
    {
        final List<Integer> registrationsToRemove = new ArrayList<Integer>();
        //clean up registrations
        for (int regId : m_Registrations.keySet())
        {
            final RemoteEventRegistration eventReg = m_Registrations.get(regId);
            if (eventReg.getSystemId() == request.getSourceId())
            {
                // only remove if request is from the same source as the previous registration
                final ServiceRegistration<EventHandler> serviceReg = eventReg.getServiceRegistration();
                serviceReg.unregister();
                registrationsToRemove.add(regId);
            }
        }
        
        // clean up map, only for those unregistered above
        for (int regId : registrationsToRemove)
        {
            m_Registrations.remove(regId);
            removePersistedRegistration(regId);
        }
        
        //construct response message
        m_MessageFactory.createEventAdminResponseMessage(request, EventAdminMessageType.CleanupResponse, null).
            queue(channel);
    }
    
    /**
     * Clean-up all registrations. This only cleans up the registration from memory and does no remove the registrations
     * from the datatstore.
     */
    private void cleanupAllRegistrations()
    {
        for (RemoteEventRegistration eventReg : m_Registrations.values())
        {
            final ServiceRegistration<EventHandler> serviceReg = eventReg.getServiceRegistration();
            serviceReg.unregister();
        }
        m_Logging.debug("The number of registrations that should have been removed %d", m_Registrations.size());
        m_Registrations.clear();
    }

    /**
     * Persist a new event registration.
     * @param newRegistration
     *     the new registration to store
     * @param regId
     *     the current assigned registration id used for the remote event registration
     * @param systemId
     *     the system id of the system requesting the remote events
     * @param encryptionType
     *     the type of encryption to be applied to messages that are created in response to the remote 
     *     event registration
     * @throws PersistenceFailedException
     *     if the persist failed
     * @throws IllegalArgumentException
     *     if any argument is null
     */
    private synchronized void persistRegistration(final EventRegistrationRequestData newRegistration, final int regId, 
        final int systemId, final EncryptType encryptionType) throws IllegalArgumentException, 
            PersistenceFailedException
    {
        //create message that will be persisted
        final PersistentEventRegistrationMessage persistMessage = PersistentEventRegistrationMessage.newBuilder().
                setSystemId(systemId).
                setRegMessage(newRegistration).
                setEncryptionType(encryptionType).build();
        
        m_Datastore.persist(
                this.getClass(), UUID.randomUUID(), String.valueOf(regId), persistMessage.toByteArray());
    }
    
    /**
     * Remove a persisted registration from the datastore.
     * @param regId
     *     the registration id of the remote event registration to remove
     */
    private synchronized void removePersistedRegistration(final int regId)
    {
        m_Datastore.removeMatching(getClass(), String.valueOf(regId));
    }
    
    /**
     * Register an event handler based of the information in a remote event registration request.
     * @param regId
     *     the registration ID to use
     * @param message
     *     the remote event registration message that contains the information of what to listen for
     * @param systemId
     *     the id of the system which is requesting to receive remote notification of events
     * @param encryptionType
     *     the type of encryption to apply to event messages that are generated in response to the registration
     */
    private void registerListenerLocally(final int regId, final EventRegistrationRequestData message, 
            final int systemId, final EncryptType encryptionType)
    {
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        // set topic property
        final String[] topics = new String[message.getTopicCount()];
        properties.put(EventConstants.EVENT_TOPIC, message.getTopicList().toArray(topics));
        
        // set filter property
        final String ignoreRemoteEventFilter = String.format("(!(%s=*))", RemoteConstants.REMOTE_EVENT_PROP);
        // if requesting a certain filter combine with base filter of ignoring remote events
        if (message.hasFilter())
        {
            properties.put(EventConstants.EVENT_FILTER, String.format("(&%s%s)",
                    ignoreRemoteEventFilter,
                    message.getFilter()));
        }
        else
        {
            properties.put(EventConstants.EVENT_FILTER, ignoreRemoteEventFilter);
        }
        
        // register event handler for the given channel
        final ServiceRegistration<EventHandler> reg = m_Context.registerService(EventHandler.class, 
                new EventHandlerImpl(systemId, encryptionType, message), properties);
        m_Registrations.put(regId, new RemoteEventRegistration(systemId, reg, message));
    }

    /**
     * Check to see if an event registration already exists for a system ID.
     * @param message
     *     the remote event registration message that contains the information of what to listen for
     * @param systemId
     *     the id of the system which is requesting to receive remote notification of events
     * @param encryptionType
     *     the type of encryption to apply to event messages that are generated in response to the registration
     * @return true if event registration already exists, false otherwise
     */
    private int registrationExists(final EventRegistrationRequestData message, final int systemId,
            final EncryptType encryptionType)
    {
        for (Integer existingRegId : m_Registrations.keySet())
        {
            final RemoteEventRegistration existingEventReg = m_Registrations.get(existingRegId);
            final EventRegistrationRequestData existingMessage = existingEventReg.getEventRegistrationRequestData();
            if (systemId == existingEventReg.getSystemId()
                    && message.getCanQueueEvent() == existingMessage.getCanQueueEvent()
                    && message.getObjectFormat().equals(existingMessage.getObjectFormat())
                    && ((message.hasFilter() && message.getFilter().equals(existingMessage.getFilter()))
                            || (!message.hasFilter() && !existingMessage.hasFilter()))
                    && topicListsEqual(message.getTopicList(), existingMessage.getTopicList()))
            {
                return existingRegId;
            }
        }

        return -1;
    }
    
    /**
     * Helper function used to compare topic lists and determine whether they are equal.
     * @param list1
     *      first list to compare
     * @param list2
     *      second list to compare
     * @return
     *      true if the lists are equal, false otherwise
     */
    private boolean topicListsEqual(final ProtocolStringList list1, final ProtocolStringList list2)
    {
        if (list1.size() != list2.size())
        {
            return false;
        }
        
        final List<String> sortedList1 = new ArrayList<>(list1);
        sortedList1.sort(String::compareTo);

        final List<String> sortedList2 = new ArrayList<>(list2);
        sortedList2.sort(String::compareTo);
        
        for (int i = 0; i < sortedList1.size(); ++i)
        {
            if (!sortedList1.get(i).equals(sortedList2.get(i)))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Handles local events and sends them to a remote listener server.
     * If there is not a channel available to send to event through the event will be lost.
     * 
     * @author bachmakm
     *
     */
    class EventHandlerImpl implements EventHandler
    {
        /** Maximum number of times to post errors when sending the event fails. */
        private final static int MAX_ERROR_LOGGING = 3;
        
        /** Blocking queue used by the executor to store tasks in FIFO order. */
        final private BlockingQueue<Runnable> m_WorkQueue = new LinkedBlockingQueue<Runnable>();
        
        /** Executor used to queue and handle multiple sendEvent messages to be sent back to a controller. */
        final private ExecutorService m_ExecutorService = new ExceptionLoggingThreadPool(m_Logging, 1, 1, 0L, 
                TimeUnit.SECONDS, m_WorkQueue);
        
        /** System id for identifying the TerraHarvestSystem that registered to listen to an event. */
        final private int m_RegisteredSystemId;
        
        /** The encryption type to applied to messages that result from this registration. */
        final private EncryptType m_EncryptionType;
        
        /**
         * Number of times this handler could not send an event since last successful transmission to the desired 
         * remote system.
         * */
        private int m_FailedAttemptsSinceLastSuccess;

        /**
         * Format to use when converting JAXB lexicon objects.
         */
        private RemoteTypesGen.LexiconFormat.Enum m_LexiconFormat;
        
        private boolean m_CanQueueEvents;
        
        /**
         * Construct an event handler.
         * 
         * @param registeredSystemId
         *      the system id of the controller registering for the event handled by this class
         * @param encryptionType
         *      the type of encryption to applied to messages that are sent because of this registration
         * @param requestData
         *      Request Data used to get Lexicon Format and the canQueuEvents Flag
         */
        EventHandlerImpl(final int registeredSystemId, final EncryptType encryptionType, final 
                EventRegistrationRequestData requestData)
        {
            m_RegisteredSystemId = registeredSystemId;
            m_EncryptionType = encryptionType;
            m_LexiconFormat = requestData.getObjectFormat();
            m_CanQueueEvents = requestData.getCanQueueEvent();
        }
 
        @Override
        public void handleEvent(final Event event)
        {
            m_ExecutorService.execute(new Runnable()//NOCHECKSTYLE Need long inner class to support Queuing
            {
                @Override
                public void run()
                {
                    final SendEventData.Builder sendEventMsg = SendEventData.newBuilder().
                        setTopic(event.getTopic());
                    
                    final Map<String, Object> props = EventUtils.getEventProps(event);
                    
                    //remove the topic
                    props.remove(EventConstants.EVENT_TOPIC);
                    
                    //get remote properties
                    final List<ComplexTypesMapEntry> eventProps;
                    try
                    {
                        eventProps = m_RemotePropertyConverter.mapToComplexTypesMap(props, m_LexiconFormat);
                    }
                    catch (final ObjectConverterException | MarshalException e)
                    {
                        m_Logging.error(e, 
                            "Unable to send event to system [0x%08x] because a property could not be converted.", 
                            m_RegisteredSystemId);
                        return;
                    }
                    //prop
                    final ComplexTypesMapEntry eventProp = ComplexTypesMapEntry.newBuilder().
                        setKey(RemoteConstants.REMOTE_EVENT_PROP).
                        setMulti(SharedMessageUtils.convertObjectToMultitype(true)).build();
                    //add to entries
                    eventProps.add(eventProp);
                    //add the rest of the properties
                    sendEventMsg.addAllProperty(eventProps).build();
                    
                    //try to send the message
                    try
                    {
                        if (m_CanQueueEvents)
                        {
                            final boolean queued = m_MessageFactory.createEventAdminMessage(
                                    EventAdminMessageType.SendEvent, sendEventMsg.build()).
                                    queue(m_RegisteredSystemId, m_EncryptionType, null);
                            m_Logging.debug("Tried to queue event %s to remote service, result:%b", 
                                    event.getTopic(), queued);
                            //set failure count to 0 
                            m_FailedAttemptsSinceLastSuccess = 0;
                        }
                        else
                        {
                            final boolean sent = m_MessageFactory.createEventAdminMessage(
                                    EventAdminMessageType.SendEvent, sendEventMsg.build()).
                                    trySend(m_RegisteredSystemId, m_EncryptionType);
                            m_Logging.debug("Tried to send event %s to remote service, result:%b", 
                                    event.getTopic(), sent);
                            //set failure count to 0 
                            m_FailedAttemptsSinceLastSuccess = 0;
                        }
                    }
                    catch (final IllegalArgumentException e)
                    {
                        //increment failure counter and check the number of times error have be logged
                        m_FailedAttemptsSinceLastSuccess++;
                        if (m_FailedAttemptsSinceLastSuccess <= MAX_ERROR_LOGGING)
                        {
                            m_Logging.warning("Unable to send remote event to remote system [0x%08x]", 
                                    m_RegisteredSystemId);
                        }
                    }
                }
            });
        }
        
        /**
         * Get the number of failed attempts that this handler has attempted to send events to the remote system.
         * @return
         *     the number of failed attempts
         */
        public int getFailedAttemptCount()
        {
            return m_FailedAttemptsSinceLastSuccess;
        }
    }
}
