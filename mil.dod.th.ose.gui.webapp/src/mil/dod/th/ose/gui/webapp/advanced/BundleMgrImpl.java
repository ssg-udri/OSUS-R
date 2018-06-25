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
package mil.dod.th.ose.gui.webapp.advanced;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BundleMessages;
import mil.dod.th.core.remote.proto.BundleMessages.BundleInfoType;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.remote.RemoteEvents;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.shared.OSGiEventConstants;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of the {@link BundleMgr} interface.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "bundleManager") 
@ApplicationScoped                                 
public class BundleMgrImpl implements BundleMgr
{
    /**
     * Map that contains the id of controllers as keys and a list of associated bundles as the value.
     */
    private final Map<Integer, Map<Long, BundleModel>> m_ControllerBundles = 
            Collections.synchronizedMap(new HashMap<Integer, Map<Long, BundleModel>>());
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    /**
     * Reference to the event admin service.
     */
    @Inject@OSGiService
    private EventAdmin m_EventAdmin;
    
    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Service that retrieves the bundle context of this bundle.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Event handler used to handle bundle namespace message events.
     */
    private BundleMessageEventHelper m_BundleEventHandler;
    
    /**
     * Event handler used to handle controller events.
     */
    private ControllerEventHelper m_ControllerEventHandler;
    
    /**
     * Event handler used to handle bundle events.
     */
    private RemoteBundleEventHelper m_RemoteBundleEventHandler;
    
    /**
     * Method called after the constructor method but before the bean is made available.
     */
    @PostConstruct
    public void setup()
    {
        m_BundleEventHandler = new BundleMessageEventHelper();
        m_BundleEventHandler.registerBundleEvents();
        
        m_ControllerEventHandler = new ControllerEventHelper();
        m_ControllerEventHandler.registerControllerEvents();
        
        m_RemoteBundleEventHandler = new RemoteBundleEventHelper();
        m_RemoteBundleEventHandler.registerRemoteBundleEvents();
    }
    
    /**
     * Unregister handler before destruction of the bean.
     */
    @PreDestroy
    public void unregisterEventHelper()
    {
        m_BundleEventHandler.unregisterListener();
        m_ControllerEventHandler.unregisterListener();
        m_RemoteBundleEventHandler.unregisterListener();
    }
    
    /**
     * Method that sets the MessageFactory service to use.
     * 
     * @param messageFactory
     *          MessageFactory service to set.
     */
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    /**
     * Sets the event admin service to use.
     * 
     * @param eventAdmin
     *          The event admin service to be set.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Sets the growl message utility to use.
     * 
     * @param growlUtil
     *          The growl message utility to be set.
     */
    public void setGrowlUtil(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    /**
     * Sets the bundle context utility to use.
     * 
     * @param bundleUtil
     *          The bundle context utility to be set.
     */
    public void setBundleUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }

    @Override
    public synchronized List<BundleModel> getBundlesAsync(final int controllerId)
    {
        if (!m_ControllerBundles.containsKey(controllerId))
        {
            m_ControllerBundles.put(controllerId, Collections.synchronizedMap(new HashMap<Long, BundleModel>()));
            
            sendBundleInfoRequest(null, controllerId);
            
            registerRemoteBundleEvents(controllerId);
        }
        return new ArrayList<BundleModel>(m_ControllerBundles.get(controllerId).values());
    }
    
    @Override
    public BundleModel retrieveBundleByLocationAsync(final int controllerId, final String bundleLocation)
    {
        BundleModel returnBundle = null;
        for (BundleModel bundle: getBundlesAsync(controllerId))
        {
            if (bundle.getLocation().equals(bundleLocation))
            {
                returnBundle = bundle;
            }
        }
        return returnBundle;
    }
    
    /**
     * Method that registers bundle events with the specified controllers event administration service.
     *  
     * @param controllerId
     *          ID of the controller to register the events with.
     */
    private void registerRemoteBundleEvents(final int controllerId)
    { 
        //List of events to register for.
        final List<String> topics = new ArrayList<String>();
        topics.add(OSGiEventConstants.TOPIC_BUNDLE_ALL_EVENTS);
       
        //Send registration message.
        RemoteEvents.sendEventRegistration(m_MessageFactory, topics, null, true, controllerId, null);
    }
    
    /**
     * Method used to create a message to request information on a specific bundle.
     * 
     * @param bundleId
     *          ID of the bundle that information is being requested for.
     * @param controllerId
     *          ID of the controller where the bundle is located.
     */
    private void sendBundleInfoRequest(final Long bundleId, final int controllerId)
    {
        final BundleMessages.GetBundleInfoRequestData.Builder requestInfoMsg = 
                BundleMessages.GetBundleInfoRequestData.newBuilder();
        requestInfoMsg.setBundleDescription(true);
        requestInfoMsg.setBundleLastModified(true);
        requestInfoMsg.setBundleLocation(true);
        requestInfoMsg.setBundleName(true);
        requestInfoMsg.setBundleState(true);
        requestInfoMsg.setBundleSymbolicName(true);
        requestInfoMsg.setBundleVendor(true);
        requestInfoMsg.setBundleVersion(true);
        requestInfoMsg.setPackageImports(true);
        requestInfoMsg.setPackageExports(true);
        
        if (bundleId != null)
        {
            requestInfoMsg.setBundleId(bundleId);
        }
        
        m_MessageFactory.createBundleMessage(BundleMessageType.GetBundleInfoRequest, requestInfoMsg.build()).
            queue(controllerId, null);
    }
    
    /**
     * Function will post the specified eventTopic to the {@link EventAdmin} service with the 
     * bundleId and bundleLocation as properties if specified.
     * @param bundleId
     *  the id of the bundle that this event is being generated for
     * @param bundleLocation
     *  the location of the bundle that this event is being generated for
     * @param controllerId
     *  the id of the controller to which the bundle that is generating this event belongs to
     * @param eventTopic
     *  the event topic that is to be posted
     */
    private void postBundleEvent(final Long bundleId, final String bundleLocation, 
            final int controllerId, final String eventTopic)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        
        props.put(EventConstants.BUNDLE_ID, bundleId);
        props.put(BundleMgr.EVENT_PROP_BUNDLE_LOCATION, bundleLocation);
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, controllerId);
        
        m_EventAdmin.postEvent(new Event(eventTopic, props));
    }
    
    /**
     * Handles bundle events and performs actions based on events received.
     */
    class BundleMessageEventHelper implements EventHandler
    {    
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method to register this event handler for the message received topic and filter by the bundle namespace.
         */
        public void registerBundleEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for message received events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(&(%s=%s)(|(%s=%s)(%s=%s)))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Bundle.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BundleMessageType.GetBundleInfoResponse.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BundleMessageType.BundleNamespaceError.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for messages received.
            m_Registration = context.registerService(EventHandler.class, this, props);    
        }

        /* (non-Javadoc)
         * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
         */
        @Override
        public void handleEvent(final Event event)
        {
            final String messageType = (String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE);
            final int controllerId = getControllerId(event);
            
            if (messageType.equals(BundleMessageType.GetBundleInfoResponse.toString()))
            { 
                final BundleMessages.GetBundleInfoResponseData responseMsg = 
                        (BundleMessages.GetBundleInfoResponseData)event.getProperty(
                                RemoteConstants.EVENT_PROP_DATA_MESSAGE);
                handleBundleInfoResponse(responseMsg, controllerId);
            }
            else if (messageType.equals(BundleMessageType.BundleNamespaceError.toString()))
            {
                final BundleMessages.BundleNamespaceErrorData errorMsg = 
                        (BundleMessages.BundleNamespaceErrorData)event.getProperty(
                                RemoteConstants.EVENT_PROP_DATA_MESSAGE);
                
                m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, errorMsg.getError().toString(), 
                        errorMsg.getErrorDescription());
            }
        }
        
        /**
         * Method that is called to handle a bundle info response message.
         * 
         * @param bundle
         *          Bundle model that contains the bundle information found in the bundle info response message.
         * @param controllerId
         *          ID of the controller the bundle is located on.
         */
        private void handleBundleInfoResponse(final BundleMessages.GetBundleInfoResponseData bundle, 
                final int controllerId)
        {
            final List<BundleInfoType> bundleInfoList = bundle.getInfoDataList();
            
            for (BundleInfoType bundleInfo : bundleInfoList)
            {
                final long bundleId = bundleInfo.getBundleId();
                if (m_ControllerBundles.containsKey(controllerId) 
                        && !m_ControllerBundles.get(controllerId).containsKey(bundleId))
                {
                    m_ControllerBundles.get(controllerId).put(bundleId, new BundleModel(bundleInfo));
                }
                else
                {
                    m_ControllerBundles.get(controllerId).get(bundleId).updateBundle(bundleInfo);
                } 
            }
            if (bundleInfoList.size() == 1)
            {
                postBundleEvent(bundleInfoList.get(0).getBundleId(), bundleInfoList.get(0).getBundleLocation(), 
                    controllerId, BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED);
            }
            else
            {
                postBundleEvent(null, null, controllerId, BundleMgr.TOPIC_BUNDLE_INFO_RECEIVED);
            }
        }
        
        /**
         * Method that returns the ID of the controller where message originated from.
         * 
         * @param event
         *          Event that contains the TerraHarvestMessage to pull the source ID from.
         * @return
         *          The ID of the controller where the message originated from.
         */
        private int getControllerId(final Event event)
        {
            final TerraHarvestMessage thMessage = 
                    (TerraHarvestMessage)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE);
            return thMessage.getSourceId();
        }

        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }
    
    /**
     * Handles controller events and performs actions based on events received.
     */
    class ControllerEventHelper implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method to register this event handler for the message received topic and filter by the bundle namespace.
         */
        public void registerControllerEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for controller removed events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, ControllerMgr.TOPIC_CONTROLLER_REMOVED);
            
            //register the event handler that listens for controllers being removed.
            m_Registration = context.registerService(EventHandler.class, this, props);    
        }

        /* (non-Javadoc)
         * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
         */
        @Override
        public void handleEvent(final Event event)
        {
            final int controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);
            m_ControllerBundles.remove(controllerId);
        }
        
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }
    
    /**
     * Handles bundle events and performs actions based on the type of bundle event received.
     */
    class RemoteBundleEventHelper implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method to register this event handler to listen for local bundle events.
         */
        public void registerRemoteBundleEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for controller removed events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final List<String> topics = new ArrayList<String>();
            topics.add(OSGiEventConstants.TOPIC_BUNDLE_STARTED + RemoteConstants.REMOTE_TOPIC_SUFFIX);
            topics.add(OSGiEventConstants.TOPIC_BUNDLE_STOPPED + RemoteConstants.REMOTE_TOPIC_SUFFIX);
            topics.add(OSGiEventConstants.TOPIC_BUNDLE_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX);
            topics.add(OSGiEventConstants.TOPIC_BUNDLE_INSTALLED + RemoteConstants.REMOTE_TOPIC_SUFFIX);
            topics.add(OSGiEventConstants.TOPIC_BUNDLE_UNINSTALLED + RemoteConstants.REMOTE_TOPIC_SUFFIX);
            props.put(EventConstants.EVENT_TOPIC, topics);
            
            //register the event handler that listens for controllers being removed.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        /* (non-Javadoc)
         * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
         */
        @Override
        public void handleEvent(final Event event)
        {
            final String topic = event.getTopic();
            final long bundleId = (Long)event.getProperty(EventConstants.BUNDLE_ID);
            final int controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);

            if (topic.equals(OSGiEventConstants.TOPIC_BUNDLE_STARTED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                if (m_ControllerBundles.containsKey(controllerId) 
                        && m_ControllerBundles.get(controllerId).containsKey(bundleId))
                {
                    m_ControllerBundles.get(controllerId).get(bundleId).setState(Bundle.ACTIVE);
                    
                    postBundleEvent(bundleId,  m_ControllerBundles.get(controllerId).
                            get(bundleId).getLocation(), controllerId, BundleMgr.TOPIC_BUNDLE_STATUS_UPDATED);
                }
            }
            else if (topic.equals(OSGiEventConstants.TOPIC_BUNDLE_STOPPED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                if (m_ControllerBundles.containsKey(controllerId) 
                        && m_ControllerBundles.get(controllerId).containsKey(bundleId))
                {
                    m_ControllerBundles.get(controllerId).get(bundleId).setState(Bundle.RESOLVED);

                    postBundleEvent(bundleId,  m_ControllerBundles.get(controllerId).
                        get(bundleId).getLocation(), controllerId, BundleMgr.TOPIC_BUNDLE_STATUS_UPDATED);
                }
            }
            else if (topic.equals(OSGiEventConstants.TOPIC_BUNDLE_UNINSTALLED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                if (m_ControllerBundles.containsKey(controllerId) 
                        && m_ControllerBundles.get(controllerId).containsKey(bundleId))
                {
                    //Create and post an event with details on the bundle that was removed from the specified 
                    //controller.
                    postBundleEvent(bundleId, m_ControllerBundles.get(controllerId).
                            get(bundleId).getLocation(), controllerId, BundleMgr.TOPIC_BUNDLE_INFO_REMOVED);
                    
                    //Once event is posted, remove unneeded bundle information.
                    m_ControllerBundles.get(controllerId).remove(bundleId);
                }
            }
            else if (topic.equals(OSGiEventConstants.TOPIC_BUNDLE_INSTALLED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                sendBundleInfoRequest(bundleId, controllerId);
            }
            else if (topic.equals(OSGiEventConstants.TOPIC_BUNDLE_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX))
            {
                sendBundleInfoRequest(bundleId, controllerId);
            }
        }
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }
}
