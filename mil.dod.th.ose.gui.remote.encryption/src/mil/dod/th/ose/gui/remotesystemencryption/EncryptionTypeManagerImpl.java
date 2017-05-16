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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.RemoteSystemEncryption;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.ose.gui.api.ControllerEncryptionConstants;
import mil.dod.th.ose.gui.api.EncryptionTypeManager;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Implementation of the {@link EncryptionTypeManager} service.
 * 
 * @author nickmarcucci
 *
 */
@Component
public class EncryptionTypeManagerImpl implements EncryptionTypeManager
{
    /**
     * Variable used to maintain the bundle context associated with the bundle.
     */
    private BundleContext m_BundleContext;
    
    /**
     * Helper class for listening to channel removed events. 
     */
    private EventHelperChannelEvent m_ChannelEventHelper;
    
    /**
     * Helper class for listening to {@link GetEncryptionTypeResponseData} messages from a system.
     */
    private EventHelperEncryptionInfoNamespace m_EncryptionInfoEventHelper;
    
    /**
     * Reference the EventAdmin service for posting events.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Inject the {@link MessageFactory} service for creating remote messages.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Reference to {@link RemoteSystemEncryption} service which keeps track of known systems 
     * and their encryption types.
     */
    private RemoteSystemEncryption m_RemoteSystemEncryptService;
    
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
     * Set the {@link MessageFactory} service to use.
     * @param mFactory
     *  the message factory that is to be used
     */
    @Reference
    public void setMessageFactory(final MessageFactory mFactory)
    {
        m_MessageFactory = mFactory;
    }
    
    /**
     * Set the {@link RemoteSystemEncryption} service to use.
     * @param encryptMgr
     *  the encryption manager
     */
    @Reference
    public void setRemoteSystemEncryption(final RemoteSystemEncryption encryptMgr)
    {
        m_RemoteSystemEncryptService = encryptMgr;
    }
    
    /**
     * This method stores the bundle context from the core.  
     * @param context
     *      bundle context of the core
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_BundleContext = context;
        
        //register 
        m_EncryptionInfoEventHelper = new EventHelperEncryptionInfoNamespace();
        m_EncryptionInfoEventHelper.registerForEncryptionInfoEvents();
        
        m_ChannelEventHelper = new EventHelperChannelEvent();
        m_ChannelEventHelper.registerChannelRemovedEvents();
    }
    
    /**
     * Unregisters listeners if bundle has been deactivated. 
     */
    @Deactivate
    public void deactivate()
    {
        m_EncryptionInfoEventHelper.unregisterListener();
        m_ChannelEventHelper.unregisterListener();
    }
    
    @Override
    public synchronized EncryptType getEncryptTypeAsnyc(final int controllerId)
    {
        final EncryptType encryptType = m_RemoteSystemEncryptService.getEncryptType(controllerId);
        if (encryptType == null)
        {
            m_MessageFactory.createEncryptionInfoMessage(
                    EncryptionInfoMessageType.GetEncryptionTypeRequest, null).queue(controllerId, null);
            return null;
        }
        return encryptType;
    }
    
    /**
     * Helper method for persisting a controller's encryption info and posting an encryption info updated event.  
     * @param systemId
     *      ID for which the controller encryption type has been updated.
     * @param type
     *      Encryption type of the specified controller.
     */
    private synchronized void updateEncryptionInfoForController(final int systemId, final EncryptType type)
    {
        //add type to map so it's immediately available for local use
        m_RemoteSystemEncryptService.addEncryptionTypeForSystem(systemId, type);
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, systemId);
        props.put(ControllerEncryptionConstants.EVENT_PROP_ENCRYPTION_TYPE, type.toString());
        m_EventAdmin.postEvent(new Event(ControllerEncryptionConstants.TOPIC_CONTROLLER_ENCRYPTION_TYPE_UPDATED, 
                props));
    }
    
    /**
     * Handles events for the encryption info namespace. 
     */
    class EventHelperEncryptionInfoNamespace implements EventHandler 
    {      
        /**
         * Service registration for the listener. Saved for unregistering the events when the encryption service is 
         * deactivated.
         */
        private ServiceRegistration<EventHandler> m_Registration;

        /**
         * Method to register this event handler for the message received topic.
         */
        public void registerForEncryptionInfoEvents()
        {
            // register to listen to the Asset events
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(&(%s=%s)(%s=%s))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.EncryptionInfo.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
                    EncryptionInfoMessageType.GetEncryptionTypeResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for EncryptionInfo namespace responses
            m_Registration = m_BundleContext.registerService(EventHandler.class, this, props);
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

            final GetEncryptionTypeResponseData response = (GetEncryptionTypeResponseData)
                    event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);

            final EncryptType recvType = response.getType();
            
            final EncryptType knownType = m_RemoteSystemEncryptService.getEncryptType(systemId);

            //if the system is already known 
            if (knownType == null) 
            {
                updateEncryptionInfoForController(systemId, recvType);
            }
            else
            {
                //if the message encrypt type is different from what the known type is
                if (!recvType.equals(knownType))
                {
                    updateEncryptionInfoForController(systemId, recvType);
                }
            }
        }
    }
    
    /**
     * Class to handle channel removed events.  Ensures that obsolete controllers are removed
     * from the data store if a controller is no longer associated with a channel.
     */
    class EventHelperChannelEvent implements EventHandler
    {
        /**
         * Service registration for the listener. Saved for unregistering the events when the encryption service is
         * being destroyed.
         */
        private ServiceRegistration<EventHandler> m_Registration;

        /**
         * Method to register this event handler for the channel removed event.
         */
        public void registerChannelRemovedEvents()
        {
            // register to listen for channel removed events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] remoteChannelTopics = {RemoteChannelLookup.TOPIC_CHANNEL_REMOVED };
            
            props.put(EventConstants.EVENT_TOPIC, remoteChannelTopics);

            //register the event handler that listens for channels being removed.
            m_Registration = m_BundleContext.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            m_RemoteSystemEncryptService.cleanupSystemEncryptionTypes();
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
