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
package mil.dod.th.ose.remote.observation; 

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.FindObservationByUUIDResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationResponseData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.remote.api.RemoteEventConstants;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

/**
 * This interface describe a service which listens for remote messages that contain observations and stores those
 * observations into the local {@link ObservationStore}. 
 * @author callen
 *
 */
//TD: fix data coupling
@SuppressWarnings("classdataabstractioncoupling")
@Component (designate = RemoteObservationListenerConfig.class, configurationPolicy = ConfigurationPolicy.optional)
public class RemoteObservationListener 
{
    /**
     * Name of the OSGi framework property containing the default behavior for this component.
     */
    public static final String ENABLED_FRAMEWORK_PROPERTY = "mil.dod.th.ose.remote.observationlistener.enabled";
    
    /**
     * Max number of observations the received observation queue can hold at one time.
     */
    private static final int QUEUE_LIMIT = 100;

    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;

    /**
     * The observation store service which allows remote observations to be stored locally.
     */
    private ObservationStore m_ObservationStore;

    /**
     * Observation event handler, this inner helper class listens for received messages that may contain observations.
     */
    private ObservationStoreHandler m_ObservationHandler;

    /**
     * Handler that listens for locally posted remote events that contain observations.
     */
    private ObservationRemoteHandler m_RemoteObsListener;

    /**
     * Service that assists in converting instances of {@link Observation}s from proto messages to JAXB objects.
     */
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     * Reference to the event admin service.  Used for local messages within event admin service.
     */
    private EventAdmin m_EventAdmin;

    /**
     * The bundle context from the bundle containing this component.
     */
    private BundleContext m_Context;
    
    /**
     * Queue used to store observations that still need to be added to the local store.
     */
    private LinkedBlockingQueue<Observation> m_ReceivedObsQueue = new LinkedBlockingQueue<>(QUEUE_LIMIT);
    
    /**
     * Queue used to store protobuf observations that need to be converted to JAXB observations before being stored
     * locally.
     */
    private LinkedBlockingQueue<ObservationGen.Observation> m_ConvertObsQueue = new LinkedBlockingQueue<>();
    
    /**
     * Thread used to process observations received and persist them locally.
     */
    private Thread m_ObsHandlerThread;
    
    /**
     * Thread used to convert protobuf observations to their JAXB equivalent.
     */
    private Thread m_ConvertObsThread;
    
    /**
     * Boolean used to determine if the observation handling thread is running.
     */
    private boolean m_Running;
    
    /**
     * Map with a key that is the UUID of the last observation in a {@link GetObservationResponseData} message and value
     * that is the total number of observations within the message. This map is used to determine when all observations 
     * within the message have been stored locally so an update event can be posted.
     */
    private Map<UUID, Integer> m_LastObsUuidMap = Collections.synchronizedMap(new HashMap<UUID, Integer>());

    /**
     * Set the {@link mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter}.
     * 
     * @param converter
     *     the service responsible for converting between proto and JAXB objects.
     */
    @Reference
    public void setJaxbProtoObjectConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;        
    }
    
    /**
     * Set the {@link EventAdmin} service.
     * @param eventAdmin
     *      the event admin service to use.
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }

    /**
     * Bind the local observation store service.
     * 
     * @param observationStore
     *      local observation store service
     */
    @Reference
    public void setObservationStore(final ObservationStore observationStore)
    {
        m_ObservationStore = observationStore;
    }

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
     * Activate this component and pass the context to the event handler that 
     * listens to remote observation events if this component in enabled.
     * 
     * @param context
     *      context for this bundle
     * @param props
     *      the initial properties
     */
    @Activate
    public void activate(final BundleContext context, final Map<String, Object> props)
    {
        //save the context
        m_Context = context;

        //create handlers
        m_ObservationHandler = new ObservationStoreHandler();
        m_RemoteObsListener = new ObservationRemoteHandler();

        //update properties
        updateProps(props);
    }

    /**
     * Deactivate the component by unregistering the event handlers. Also stops the threads responsible for handling
     * converting observations and validating/storing observations locally.
     * 
     * @throws InterruptedException
     *      thrown if the join for the observation handler thread or observation converter thread is interrupted.
     */
    @Deactivate
    public void deactivate() throws InterruptedException
    {
        //unregister the event listeners for observation messages
        m_ObservationHandler.unregisterServiceRegistration();
        m_RemoteObsListener.unregisterServiceRegistration();
        stopHandlerThreads();
    }

    /**
     * Updates the {@link RemoteObservationListenerConfig} values.
     * 
     * @param props
     *     updated key, value pair property map
     */
    @Modified
    public void modified(final Map<String, Object> props)
    {
        updateProps(props);
    }

    /**
     * Sets the this class's configuration values. 
     * 
     * @param props
     *    key, value pair map of applicable configuration values 
     */
    private void updateProps(final Map<String, Object> props)
    {
        //new property list
        final Map<String, Object> properties = new HashMap<String, Object>(props);

        //property key
        //This is a workaround because the handling of boolean values is not correct through the configurable class.
        //The property should be created when a configuration is made, and use a default from the configuration 
        //defined for this component.
        final String property = "enabled";

        if (props.get(property) == null)
        {
            //framework property
            final String prop = m_Context.getProperty(ENABLED_FRAMEWORK_PROPERTY);

            //if the property from the context is null then use default of true
            final boolean enabled = prop == null ? true : Boolean.parseBoolean(prop);

            properties.put(property, enabled);
            m_Logging.debug("Remote Listener framework property identifies the component is enabled [%s]", prop);
        }

        final RemoteObservationListenerConfig config = Configurable.createConfigurable(
            RemoteObservationListenerConfig.class, properties);
        setEnabled(config.enabled());
    }
    
    /**
     * Method used to start the threads that handle converting and storing observations.
     */
    private void startHandlerThreads()
    {
        if (!m_Running)
        {
            m_Running = true;
            //Create the thread responsible for handling observations received.
            m_ObsHandlerThread = new Thread(new ObsHandler());
            //Create the thread responsible for converting protobuf observations to JAXB observations
            m_ConvertObsThread = new Thread(new ObsConverter());
            m_ObsHandlerThread.start();
            m_ConvertObsThread.start();
        }
    }
    
    /**
     * Method that stops and waits for the threads that handle converting and storing observations to join.
     */
    private void stopHandlerThreads()
    {
        if (m_Running)
        {
            m_Running = false;
            final int wait = 1000;
            m_ObsHandlerThread.interrupt();
            m_ConvertObsThread.interrupt();
            try
            {
                m_ObsHandlerThread.join(wait);
            } 
            catch (final InterruptedException exception)
            {
                m_Logging.log(LogService.LOG_WARNING, exception, "Observation handler thread was "
                        + "interrupted while waiting for it to join.");
            }
            try
            {
                m_ConvertObsThread.join(wait);
            } 
            catch (final InterruptedException exception)
            {
                m_Logging.log(LogService.LOG_WARNING, exception, "Observation converter thread was interrupted while "
                        + "waiting for it to join.");
            }
        }
    }
    
    /**
     * Method used to post and {@link RemoteEventConstants#TOPIC_OBS_STORE_RETRIEVE_COMPLETE} event.
     * 
     * @param count
     *      Integer that represents the number of observations received.
     */
    private void postObsStoreRetrieveCompleteEvent(final int count)
    {
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(RemoteEventConstants.EVENT_PROP_OBS_NUMBER_RETRIEVED, count);
        m_EventAdmin.postEvent(new Event(RemoteEventConstants.TOPIC_OBS_STORE_RETRIEVE_COMPLETE, props));
    }

    /**
     * Enable this component, if the config admin is NOT available the enabled status of this component is based
     * of off a framework property.
     * @param enabled
     *     flag value representing if this component is remotely listening for observations and automatically persisting
     *     all received observation data
     */
    private void setEnabled(final boolean enabled)
    {
        if (enabled)
        {
            //register the handlers
            m_ObservationHandler.registerForObservationStore();
            m_RemoteObsListener.registerForObservationStore();
            startHandlerThreads();
            m_Logging.debug("Remote observation listener is enabled");
        }
        else
        {
            //unregister the event listeners for observation messages
            m_ObservationHandler.unregisterServiceRegistration();
            m_RemoteObsListener.unregisterServiceRegistration();
            stopHandlerThreads();
            m_Logging.debug("Remote observation listener is disabled");
        }
    }

    /**
     * Handles {@link RemoteConstants#TOPIC_MESSAGE_RECEIVED} where the {@link RemoteConstants#EVENT_PROP_MESSAGE_TYPE}
     * is a type of message that contains observation data. 
     *
     */
    class ObservationStoreHandler implements EventHandler
    {
        /**
         * The service registration object for the registered event.
         */
        private ServiceRegistration<EventHandler> m_ServiceReg;

        /**
         * Method to register this event handler for events with the {@link RemoteConstants#TOPIC_MESSAGE_RECEIVED}
         * topic from the ObservationStoreNamespace.
         */
        public void registerForObservationStore()
        {
            if (m_ServiceReg == null)
            {
                //dictionary of properties for the event handler for TOPIC_MESSAGE_RECEIVED events
                final Dictionary<String, Object> props = new Hashtable<String, Object>();
                props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
                final String filterString = String.format("(&(%s=%s)(|(%s=%s)(%s=%s)))", //NOCHECKSTYLE multiple string 
                    RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.ObservationStore.toString(), //literals. LDAP filter
                    RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                    ObservationStoreMessageType.FindObservationByUUIDResponse.toString(),
                    RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                    ObservationStoreMessageType.GetObservationResponse.toString()); 
                props.put(EventConstants.EVENT_FILTER, filterString);
            
                //register the event handler
                m_ServiceReg = m_Context.registerService(EventHandler.class, this, props);
            }
        }

        @Override
        public void handleEvent(final Event event)
        {
            final String messageType = (String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE);
            
            //analyze the type
            final List<ObservationGen.Observation> observations;
            if (messageType.equals(ObservationStoreMessageType.GetObservationResponse.toString()))
            {
                final GetObservationResponseData obsResponse = (GetObservationResponseData)event.getProperty(
                    RemoteConstants.EVENT_PROP_DATA_MESSAGE);

                observations = obsResponse.getObservationNativeList();
                final int obsCount = obsResponse.getObservationNativeCount();
                if (obsCount == 0)
                {
                    postObsStoreRetrieveCompleteEvent(0);
                }
                else
                {
                    final SharedMessages.UUID protoUuid = observations.get(obsCount - 1).getUuid();
                    final UUID lastUuid = SharedMessageUtils.convertProtoUUIDtoUUID(protoUuid);
                    m_LastObsUuidMap.put(lastUuid, obsResponse.getObservationNativeCount()); 
                }
            }
            else
            {
                final FindObservationByUUIDResponseData request = (FindObservationByUUIDResponseData)event.getProperty(
                        RemoteConstants.EVENT_PROP_DATA_MESSAGE);
                observations = request.getObservationNativeList();
            }
            
            //queue the proto observations to be converted to JAXB observations.
            m_ConvertObsQueue.addAll(observations);
        }
        
        /**
         * Method to unregister the service registration for the registered event.
         */
        public void unregisterServiceRegistration()
        {
            if (m_ServiceReg != null)
            {
                m_ServiceReg.unregister();
                m_ServiceReg = null; //NOPMD: explicitly setting to null so it is known if the component is registered
            }
        }
    }

    /**
     * Handles remotely subscribed events where the event contains observation data. 
     *
     */
    class ObservationRemoteHandler implements EventHandler
    {
        /**
         * The service registration object for the registered event.
         */
        private ServiceRegistration<EventHandler> m_ServiceReg;

        /**
         * Method to register this event handler for events with the {@link RemoteConstants#TOPIC_MESSAGE_RECEIVED}
         * topic from the ObservationStoreNamespace.
         */
        public void registerForObservationStore()
        {
            if (m_ServiceReg == null)
            {
                //dictionary of properties for the event handler for TOPIC_MESSAGE_RECEIVED events
                final Dictionary<String, Object> props = new Hashtable<String, Object>();
                final String[] topics = {
                    ObservationStore.TOPIC_OBSERVATION_MERGED_WITH_OBS + RemoteConstants.REMOTE_TOPIC_SUFFIX,
                    ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS + RemoteConstants.REMOTE_TOPIC_SUFFIX};
                props.put(EventConstants.EVENT_TOPIC, topics);

                //register the event handler
                m_ServiceReg = m_Context.registerService(EventHandler.class, this, props);
            }
        }

        @Override
        public void handleEvent(final Event event)
        {
            //get observation, might not be there
            final Observation observation = (Observation)event.getProperty(ObservationStore.EVENT_PROP_OBSERVATION);

            //add the observation to the observation handler queue so that it may be validated and loaded
            if (!m_ReceivedObsQueue.offer(observation))
            {
                m_Logging.info("The remote observations with UUID %s could not be received, the received observation"
                    + "queue is currently full.", observation.getUuid());
            }
        }
        
        /**
         * Method to unregister the service registration for the registered event.
         */
        public void unregisterServiceRegistration()
        {
            if (m_ServiceReg != null)
            {
                m_ServiceReg.unregister();
                m_ServiceReg = null; //NOPMD: explicitly setting to null so it is known if the component is registered
            }
        }
    }
    
    /**
     * Class that handles validating and loading observations to the local observation store from the received 
     * observations queue. Implements the {@link Runnable} interface.
     */
    private class ObsHandler implements Runnable
    {
        @Override
        public void run()
        {
            while (m_Running)
            {
                final Observation observation;
                try
                {
                    observation = m_ReceivedObsQueue.take();
                } 
                catch (final InterruptedException exception)
                {
                    m_Logging.log(LogService.LOG_INFO, "Observation handler thread was interrupted "
                            + "while waiting for an observation to be received.");
                    continue;
                }
                removeIfKnownAndLoad(observation);

                final Integer count = m_LastObsUuidMap.remove(observation.getUuid());
                if (count != null)
                {
                    postObsStoreRetrieveCompleteEvent(count);
                }
            }
        }
        
        /**
         * Removes the observation if it already exists and loads the observation.
         * @param observation
         *     the observation to load
         */
        private void removeIfKnownAndLoad(final Observation observation)
        {
            //make sure that the observation doesn't already exist, if it does this call will remove it
            removeObservationIfKnown(observation.getUuid());

            //persist observation
            try
            {
                m_ObservationStore.persist(observation);
            }
            catch (final PersistenceFailedException | IllegalArgumentException | ValidationFailedException exception)
            {
                m_Logging.error(exception, "The remote observation with UUID %s failed to be persisted.", 
                    observation.getUuid());
            }
        }
        
        /**
         * Check if the given observation already exists within the local datatstore.
         * @param uuid
         *     the UUID of the observation to check for.
         */
        private void removeObservationIfKnown(final UUID uuid)
        {
            //check if the observation already exists
            final Observation observation = m_ObservationStore.find(uuid);
            if (observation != null)
            {
                //already exists, remove existing observation
                m_ObservationStore.remove(observation);
                m_Logging.info("Observation with UUID [%s] was replace by a remotely received observation with the "
                    + "same UUID", uuid);
            }
        }

    }
    
    /**
     * Class that handles converting received protobuf observations to JAXB observations and adds the converted 
     * observations to the received observations queue. Implements the {@link Runnable} interface.
     */
    private class ObsConverter implements Runnable
    {
        @Override
        public void run()
        {
            while (m_Running)
            {
                final ObservationGen.Observation observation;
                try
                {
                    observation = m_ConvertObsQueue.take();
                } 
                catch (final InterruptedException exception)
                {
                    m_Logging.log(LogService.LOG_INFO, "Protobuffer observation converter thread was "
                            + "interrupted while waiting for an observation to be received.");
                    continue;
                }
                final Observation convertedObs;
                try
                {
                    convertedObs = (Observation) m_Converter.convertToJaxb(observation);
                }
                catch (final ObjectConverterException exception)
                {
                    m_Logging.error(exception, "The remote observation with UUID %s failed to be converted.", 
                        observation.getUuid());
                    continue;
                }
                //add the observation to the observation handler queue so that it may be validated and loaded
                if (!m_ReceivedObsQueue.offer(convertedObs))
                {
                    m_Logging.info("The converted remote observation with UUID %s could not be handled, the received "
                         + "observation queue is currently full.", convertedObs.getUuid());
                }
            }
        }
    }
}
