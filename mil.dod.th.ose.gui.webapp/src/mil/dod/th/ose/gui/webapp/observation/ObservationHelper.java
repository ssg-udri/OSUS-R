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
package mil.dod.th.ose.gui.webapp.observation;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.RemoteEventConstants;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Class responsible for listening to remote requests to retrieve observations and updates the observation
 * store accordingly.  
 * @author bachmakm
 *
 */
@ManagedBean(name = "obsHelper", eager = true) 
@ApplicationScoped
public class ObservationHelper
{
    /**
     * Service that retrieves the bundle context of this bundle.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;

    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlMessageUtil;

    /**
     * Event handler helper class.  Listens for observation messages from remote interface api. 
     */
    private EventHelperRemoteObservation m_EventHelperRemoteObs;  

    /**
     * Event handler helper class.  Listens for ObservationStore namespace messages.
     */
    private EventHelperObservationStoreNamespace m_EventHelperObsStore;

    /**
     * Inject the EventAdmin service for posting events.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;

    /**
     * Register event listeners.
     */
    @PostConstruct
    public void registerEventHelper()
    {        
        m_EventHelperObsStore = new EventHelperObservationStoreNamespace();
        m_EventHelperObsStore.registerForEvents();

        m_EventHelperRemoteObs = new EventHelperRemoteObservation();
        m_EventHelperRemoteObs.registerForEvents();
    }

    /**
     * Unregister handlers before destruction of the bean.
     */
    @PreDestroy
    public void unregisterHelpers()
    {
        //Unregister for events
        m_EventHelperObsStore.unregisterListener();
        m_EventHelperRemoteObs.unregisterListener();
    }

    /**
     * Set the Growl message utility service.
     * @param growlUtil
     *     the growl message utility service to use
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlMessageUtil = growlUtil;
    } 

    /**
     * Set the {@link BundleContextUtil} utility service.
     * @param bundleUtil
     *     the bundle context utility service to use
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }

    /**
     * Set the {@link EventAdmin} service.
     * @param eventAdmin
     *      the event admin service to use.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }

    /**
     * Handles events from the remote interface api.
     */
    class EventHelperRemoteObservation implements EventHandler 
    {        
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;

        /**
         * Method to register this event handler for the message received topic.
         */
        public void registerForEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();

            final String[] topics = new String[] {
                RemoteEventConstants.TOPIC_OBS_STORE_RETRIEVE_COMPLETE
            };

            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, topics);

            //register the event handler that listens for observation store namespace responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();            
        }

        @Override
        public void handleEvent(final Event event)
        {
            //Post the observation store updated event
            final Map<String, Object> props = null;
            final Event observationsUpdated = new Event(ObservationMgr.TOPIC_OBS_STORE_UPDATED, props);
            m_EventAdmin.postEvent(observationsUpdated);

            //post message
            final String obsNum = event.getProperty(RemoteEventConstants.EVENT_PROP_OBS_NUMBER_RETRIEVED).toString();
            m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Observation Retrieval Complete", 
                    obsNum + " observations were retrieved.");    
        }
    }

    /**
     * Handles events for the observation store namespace. 
     */
    class EventHelperObservationStoreNamespace implements EventHandler
    {        
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;

        /**
         * Method to register this event handler for the message received topic.
         */
        public void registerForEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();

            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);

            //only listen for RemoveObservation response from the ObservationStore namespace.
            final String filterString = String.format("(&(%s=%s)(|(%s=%s)(%s=%s)))", //NOCHECKSTYLE: multiple string literals, LDAP filter
                    RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.ObservationStore.toString(),
                    RemoteConstants.EVENT_PROP_MESSAGE_TYPE,
                    ObservationStoreMessageType.RemoveObservationByUUIDResponse.toString(),
                    RemoteConstants.EVENT_PROP_MESSAGE_TYPE,
                    ObservationStoreMessageType.RemoveObservationResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);

            //register the event handler that listens for observation store namespace responses
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();            
        }

        @Override
        public void handleEvent(final Event event)
        {
            //pull out event props
            final int systemId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);

            //Post the observation store updated event
            final Map<String, Object> props = null;
            final Event observationsUpdated = new Event(ObservationMgr.TOPIC_OBS_STORE_UPDATED, props);
            m_EventAdmin.postEvent(observationsUpdated);

            //post notice
            m_GrowlMessageUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, "Observations Removed",
                String.format("Controller 0x%08x has remotely removed observations for selected assets.", systemId));
        }
    }
}
