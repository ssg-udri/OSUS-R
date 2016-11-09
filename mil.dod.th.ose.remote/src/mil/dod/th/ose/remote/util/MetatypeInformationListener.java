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
package mil.dod.th.ose.remote.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteMetatypeConstants;
import mil.dod.th.ose.shared.OSGiEventConstants;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.MetaTypeProvider;
import org.xml.sax.SAXException;

/**
 * This component listens for when metatype information is available and posts and event containing the PID and bundle
 * id.
 * @author callen
 *
 */
@Component (immediate = true, provide = { MetatypeInformationListener.class })
public class MetatypeInformationListener
{
    /**
     * Used to log messages.
     */
    private LoggingService m_Logging;

    /**
     * The bundle context from the bundle containing this component.
     */
    private BundleContext m_Context;

    /**
     * Used to post the local event that metatype information is available.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Listener that listens for metatype information.
     */
    private MetatypeHandler m_MetaListener;

    /**
     * Listener that listens for metatype information after bundle events are received.
     */
    private MetatypeBundleHandler m_MetaBundleListener;

    /**
     * XML parsing service.
     */
    private MetaTypeXMLParsingService m_XMLParsingService;

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
     * Bind the XML parsing service.
     * 
     * @param xmlService
     *      service used to parse XML documents from bundles
     */
    @Reference
    public void setXMLParsingService(final MetaTypeXMLParsingService xmlService)
    {
        m_XMLParsingService = xmlService;
    }

    /**
     * Activate this component and pass the context to the event handlers that 
     * listen to metatype events.
     * 
     * @param context
     *      context for this bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        //save the context
        m_Context = context;

        //create handlers
        m_MetaListener = new MetatypeHandler();
        m_MetaBundleListener = new MetatypeBundleHandler();

        //check existing bundles
        for (Bundle bundle : m_Context.getBundles())
        {
            final int state = bundle.getState();
            if (state > Bundle.INSTALLED)
            {
                m_MetaBundleListener.checkBundle(bundle.getBundleId());
            }
        }

        try
        {
            //check existing services that have already been registered
            for (ServiceReference<MetaTypeProvider> ref : m_Context.getServiceReferences(MetaTypeProvider.class, null))
            {
                final String pid = (String)ref.getProperty(EventConstants.SERVICE_PID);
                m_MetaListener.notifyService(ref, pid);
            }
        }
        catch (final InvalidSyntaxException ex)
        {
            m_Logging.error(ex, "Error retrieving MetaTypeProvider service references");
        }

        //register handlers
        m_MetaListener.registerForMetatypeEvents();
        m_MetaBundleListener.registerForMetatypeBundleEvents();
    }

    /**
     * Deactivate the component by unregistering the event handlers.
     */
    @Deactivate
    public void deactivate()
    {
        //unregister the event listeners
        m_MetaListener.unregisterServiceRegistration();
        m_MetaBundleListener.unregisterServiceRegistration();
    }

    /**
     * Handles metatype provider and information events. 
     *
     */
    class MetatypeHandler implements EventHandler
    {
        /**
         * The service registration object for the registered event.
         */
        private ServiceRegistration<EventHandler> m_ServiceReg;

        /**
         * Method to register this event handler for metatype events.
         */
        public void registerForMetatypeEvents()
        {
            //dictionary of properties
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String topic = OSGiEventConstants.TOPIC_SERVICE_REGISTERED;
            props.put(EventConstants.EVENT_TOPIC, topic);

            //register the event handler
            m_ServiceReg = m_Context.registerService(EventHandler.class, this, props);
        }

        /**
         * Send event notification of metatype information for the given service.
         * 
         * @param service
         *      reference to the service
         * @param pid
         *      PID of the service
         */
        public void notifyService(final ServiceReference<?> service, final String pid)
        {
            final Map<String, Object> properties = new HashMap<String, Object>();

            //list containing the pid, done as a list for consistency
            final List<String> pids = new ArrayList<String>();
            pids.add(pid);

            properties.put(RemoteMetatypeConstants.EVENT_PROP_PIDS, pids);
            properties.put(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID, service.getBundle().getBundleId());

            //new event
            m_EventAdmin.postEvent(
                new Event(RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE, properties));
        }

        @Override
        public void handleEvent(final Event event)
        {
            //Get the list of service object classes
            final List<String> services = 
                Arrays.asList((String[])event.getProperty(EventConstants.SERVICE_OBJECTCLASS));
            if (services.contains(MetaTypeProvider.class.getName()))
            {
                final String pid = (String)event.getProperty(EventConstants.SERVICE_PID);
                @SuppressWarnings("rawtypes")//raw type because it will vary depending on the service
                final ServiceReference service = (ServiceReference)event.getProperty(EventConstants.SERVICE);

                notifyService(service, pid);
            }
        }
        
        /**
         * Method to unregister the service registration for the registered event.
         */
        public void unregisterServiceRegistration()
        {
            m_ServiceReg.unregister();
        }
    }

    /**
     * Handles bundle installed/updated events and checks if the bundle has XML based metatype information. 
     *
     */
    class MetatypeBundleHandler implements EventHandler
    {
        /**
         * The service registration object for the registered event.
         */
        private ServiceRegistration<EventHandler> m_ServiceReg;

        /**
         * Executor used process the XML document an post an event.
         * 
         */
        private final ExecutorService m_ExecutorService = Executors.newCachedThreadPool();

        /**
         * Method to register this event handler for bundle events.
         */
        public void registerForMetatypeBundleEvents()
        {
            //dictionary of properties
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] topics = {OSGiEventConstants.TOPIC_BUNDLE_RESOLVED, OSGiEventConstants.TOPIC_BUNDLE_STARTED};
            props.put(EventConstants.EVENT_TOPIC, topics);

            //register the event handler
            m_ServiceReg = m_Context.registerService(EventHandler.class, this, props);
        }

        /**
         * Checks the bundle for the given ID to see if metatype data is provided.
         * 
         * @param bundleId
         *      ID of the bundle to check
         */
        public void checkBundle(final long bundleId)
        {
            //send off thread to process XML information and post event
            m_ExecutorService.execute(new HandleBundleEvent(bundleId));
        }

        @Override
        public void handleEvent(final Event event)
        {
            //pull out the bundle id from the event
            final Long bundleId = (Long) event.getProperty(EventConstants.BUNDLE_ID);

            checkBundle(bundleId);
        }
        
        /**
         * Method to unregister the service registration for the registered event.
         */
        public void unregisterServiceRegistration()
        {
            m_ServiceReg.unregister();
        }
    }
    
    /**
     * Inner class that assists with posting an event if a bundle is installed that contains xml data.
     */
    private class HandleBundleEvent implements Runnable
    {
        /**
         * The ID of the bundle which contains XML based metadata.
         */
        private final long m_BundleId;

        /**
         * URLs to XML data within a given bundle.
         */
        private Enumeration<URL> m_UrlsMetaXML;

        /**
         * Constructor that will take the bundle ID.
         * @param bundleId
         *     the ID of the bundle that the URLS are from
         */
        HandleBundleEvent(final long bundleId)
        {
            m_BundleId = bundleId;
        }


        @Override
        public void run()
        {
            //query for XML metatype information
            m_UrlsMetaXML = m_Context.getBundle(m_BundleId).findEntries("OSGI-INF/metatype", "*.xml", true);

            if (m_UrlsMetaXML == null)
            {
                //ignore the event no XML metatype data is available
                return;
            }
            
            final List<String> pids = new ArrayList<String>();
            //if there are URLs returned than there is metatype information in the specified bundle
            while (m_UrlsMetaXML.hasMoreElements())
            {
                //resolve the fact that the URI returned is relative to the bundle
                final URI finalMappingToDestination;
                try
                {
                    finalMappingToDestination = m_UrlsMetaXML.nextElement().toURI().normalize();
                }
                catch (final URISyntaxException e)
                {
                    m_Logging.error(e, "The URL from bundle [%d] cannot be converted to a URI ", m_BundleId);
                    //can't go forward with this iteration
                    continue;
                }
                
                try
                {
                    final String pid = m_XMLParsingService.getPidAttribute(finalMappingToDestination);
                    //check the pid value
                    if (pid != null)
                    {
                        pids.add(pid);
                    }
                }
                catch (final IOException e)
                {
                    m_Logging.error(e, "The information at URI [%s] cannot processed. ", finalMappingToDestination);
                    continue;
                }
                catch (final SAXException e)
                {
                    m_Logging.error(e, "The XML document at [%s] cannot be found or is erroneous.", 
                        finalMappingToDestination);
                    continue;
                }
            }
            //check that pids isn't empty
            if (pids.isEmpty())
            {
                //just return no need to post event
                return;
            }
            
            final Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(RemoteMetatypeConstants.EVENT_PROP_PIDS, pids);
            properties.put(RemoteMetatypeConstants.EVENT_PROP_BUNDLE_ID, m_BundleId);

            //new event
            m_EventAdmin.postEvent(new Event(
                RemoteMetatypeConstants.TOPIC_METATYPE_INFORMATION_AVAILABLE, properties));
        }
    }
}
