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
package mil.dod.th.ose.datastream;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileException;
import mil.dod.th.core.datastream.StreamProfileFactory;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;
import mil.dod.th.ose.core.factory.api.DirectoryService;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.utils.SingleComponent;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Implementation for {@link DataStreamService} interface. 
 * 
 * @see DataStreamService
 * @author jmiller
 *
 */
@Component(name = DataStreamServiceImpl.PID, designate = DataStreamServiceConfig.class, 
    configurationPolicy = ConfigurationPolicy.require)
public class DataStreamServiceImpl extends DirectoryService implements DataStreamService
{
    
    /**
     * Persistent identity (PID) for the configuration.
     */
    public final static String PID = "mil.dod.th.ose.datastream.DataStreamService";
    
    /**
     * Reference to the service proxy for stream profiles.
     */
    private FactoryServiceProxy<StreamProfileInternal> m_FactoryServiceProxy;
    
    /**
     * Factory service context.
     */
    private FactoryServiceContext<StreamProfileInternal> m_FactoryContext;
    
    /**
     * Component wrapper for {@link FactoryServiceContext}s.
     */
    private SingleComponent<FactoryServiceContext<StreamProfileInternal>> m_FactServiceContextComp;
    
    /**
     * String representation of multicast host for outbound data streams.
     */
    private String m_MulticastHost;
    
    /**
     * Starting number of potentially many multicast connection ports. This service will assign
     * a unique port number to each {@link StreamProfile} that is created.
     */
    private int m_StartPort;
    
    /**
     * Wake lock used for data stream service operations.
     */
    private WakeLock m_WakeLock;

    private BundleContext m_Context;
    private ConfigurationListener m_ConfigListener;
    private PidRemovedListener m_PidRemovedListener;

    ///////////////////////////////////////////////////////////////////////////
    // OSGi binding methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        super.setLoggingService(logging);
    }

    @Override
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        super.setEventAdmin(eventAdmin);
    }

    @Override
    @Reference
    public void setPowerManager(final PowerManager powerManager)
    {
        super.setPowerManager(powerManager);
    }

    /**
     * Method used to set the {@link ComponentFactory} to be used for creating a {@link FactoryServiceContext}.
     * 
     * @param factory
     *  the factory to use
     */
    @Reference(target = "(" + ComponentConstants.COMPONENT_FACTORY + "=" + FactoryServiceContext.FACTORY_NAME + ")")
    public void setFactoryServiceContextFactory(final ComponentFactory factory)
    {
        m_FactServiceContextComp = new SingleComponent<FactoryServiceContext<StreamProfileInternal>>(factory);
    }
    
    /**
     * Bind the service. Each service type will provide a service proxy, restrict to the stream profile one.
     * 
     * @param factoryServiceProxy
     *      service to bind
     */
    @Reference(target = "(" + StreamProfileInternal.SERVICE_TYPE_PAIR + ")")
    public void setFactoryServiceProxy(final FactoryServiceProxy<StreamProfileInternal> factoryServiceProxy)
    {
        m_FactoryServiceProxy = factoryServiceProxy;
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // OSGi Declarative Services activate/deactivate methods.
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The service component activation method.
     * 
     * Registers factories that have been added before activation.
     * 
     * @param props
     *      configuration properties associated with the component
     * @param coreContext
     *      bundle context of the core bundle
     * @throws InvalidSyntaxException
     *      if {@link FactoryServiceContext} uses an invalid filter string
     */
    @Activate
    public void activate(final Map<String, Object> props, final BundleContext coreContext) throws InvalidSyntaxException
    {
        m_Context = coreContext;
        m_ConfigListener = new ConfigurationListener();
        m_ConfigListener.registerEvents();
        m_PidRemovedListener = new PidRemovedListener();
        m_PidRemovedListener.registerEvents();
        
        m_FactoryContext = m_FactServiceContextComp.newInstance(null);
        m_FactoryContext.initialize(coreContext, m_FactoryServiceProxy, this);

        m_WakeLock = m_PowerManager.createWakeLock(getClass(), "coreDataStreamService");

        setMulticastHost("");
        setStartPort(-1);
        
        if (props.get(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST) != null)
        {
            setMulticastHost((String)props.get(DataStreamServiceConfig.CONFIG_PROP_MULTICAST_HOST));
        }
        
        if (props.get(DataStreamServiceConfig.CONFIG_PROP_START_PORT) != null)
        {
            setStartPort((int)props.get(DataStreamServiceConfig.CONFIG_PROP_START_PORT));
        }
        
    }

    /**
     * Deactivate this service and discard registry services.
     */
    @Deactivate
    public void deactivate()
    {
        m_ConfigListener.unregsiterEvents();
        m_PidRemovedListener.unregsiterEvents();
        
        //dispose of factory service component
        m_FactServiceContextComp.tryDispose();

        m_WakeLock.delete();
    }
    
    /**
     * This method is called when the service configuration is updated.
     * 
     * @param props
     *     map of configuration properties
     */
    @Modified
    public void modified(final Map<String, Object> props)
    {
        final DataStreamServiceConfig attributes = 
                Configurable.createConfigurable(DataStreamServiceConfig.class, props);
        
        setMulticastHost(attributes.multicastHost());
        setStartPort(attributes.startPort());
    }


    ///////////////////////////////////////////////////////////////////////////
    // Inherited methods
    ///////////////////////////////////////////////////////////////////////////    
    @Override
    public StreamProfile createStreamProfile(final String productType, final String name, 
            final Map<String, Object> properties) throws StreamProfileException, IllegalArgumentException
    {        

        Preconditions.checkNotNull(productType);
        Preconditions.checkNotNull(properties);

        final FactoryInternal streamProfileFactory = m_FactoryContext.getFactories().get(productType);
        if (streamProfileFactory == null)
        {
            throw new IllegalArgumentException(
                    String.format("No Factory found that can create a Stream Profile of [%s] type.", 
                            productType));
        }

        try
        {
            m_WakeLock.activate();

            final StreamProfileInternal spi;
            synchronized (m_FactoryContext)
            {
                spi = m_FactoryContext.getRegistry()
                        .createNewObject(streamProfileFactory, name, properties);
            }

            //Assign value for stream port
            spi.setStreamPort(getAvailableURI());
            
            // Return the Stream Profile instance
            return spi;
        }
        catch (final Exception e)
        {
            throw new StreamProfileException("Unable to create StreamProfile.", e);
        }
        finally
        {
            m_WakeLock.cancel();
        }
    }

    @Override
    public StreamProfile getStreamProfile(final UUID profileId) throws IllegalArgumentException
    {
        return m_FactoryContext.getRegistry().getObjectByUuid(profileId);
    }

    @Override
    public Set<StreamProfile> getStreamProfiles(final Asset asset)
    {
        final Set<StreamProfile> streamProfiles = Collections.unmodifiableSet(new HashSet<StreamProfile>(
                m_FactoryContext.getRegistry().getObjects()));
        
        return Sets.filter(streamProfiles, new Predicate<StreamProfile>()
        {
            @Override
            public boolean apply(final StreamProfile profile)
            {
                return profile.getAsset().getName().equals(asset.getName());
            }
        });
    }

    @Override
    public Set<StreamProfile> getStreamProfiles()
    {
        return Collections.unmodifiableSet(new HashSet<StreamProfile>(
                m_FactoryContext.getRegistry().getObjects()));
    }    
  
    @Override
    public Set<StreamProfileFactory> getStreamProfileFactories()
    {
        final Set<FactoryInternal> set = new HashSet<>(m_FactoryContext.getFactories().values());
        @SuppressWarnings("unchecked")
        final Set<StreamProfileFactory> toReturn = Collections.unmodifiableSet((Set<StreamProfileFactory>)(Set<?>)set);
        
        return toReturn;
    }

    ////////////////////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////////////////////

    /**
     * Returns a multicast URI to be assigned to newly created {@link StreamProfile}
     * instances.
     * 
     * @throws URISyntaxException
     *      if there is an error creating the URI
     * @return
     *      multicast address and port as a URI
     */
    private URI getAvailableURI() throws URISyntaxException
    {
        //Find highest port number currently in use by a StreamProfile instance
        int maxPortNum = -1;
        for (StreamProfile profile : getStreamProfiles())
        {
            final URI uri = profile.getStreamPort();
            if (uri != null)
            {
                final int portNum = profile.getStreamPort().getPort();
                
                if (portNum > maxPortNum)
                {
                    maxPortNum = portNum;
                }
            }
        }
        
        int newPortNum;//NOCHECKSTYLE will get assigned in if statement
        
        if (maxPortNum < 0)
        {
            newPortNum = m_StartPort;
        }
        else
        {
            newPortNum = maxPortNum + 2;
        }
        
        return new URI(null, null, m_MulticastHost, newPortNum, null, null, null);
    }
    
    /**
     * Private setter for m_MulticastHost.
     * @param multicastHost
     *      String representation of multicast host
     */
    private void setMulticastHost(final String multicastHost)
    {
        m_MulticastHost = multicastHost;
    }
    
    /**
     * Private setter for m_StartPort.
     * @param startPort
     *      starting port number to be allocated
     */
    private void setStartPort(final int startPort)
    {
        m_StartPort = startPort;
    }

    /**
     * Event handler that listens for stream profile updated events and creates a stream profile if one is not 
     * associated with the updated steam profile configuration.
     */
    class ConfigurationListener implements EventHandler
    {
        /**
         * The service registration reference for this handler class.
         */
        private ServiceRegistration<EventHandler> m_ServiceRegistration;
        
        /**
         * Method used to register this handler class for configuration events.
         */
        public void registerEvents()
        {
            final Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(EventConstants.EVENT_TOPIC, StreamProfileConfigListener.TOPIC_STREAM_PROFILE_CONFIG_UPDATED);
            m_ServiceRegistration = m_Context.registerService(EventHandler.class, this, properties);
        }

        @Override
        public void handleEvent(final Event event)
        {
            final String factoryPid = (String)event.getProperty(ConfigurationEventConstants.EVENT_PROP_FACTORY_PID);
            FactoryInternal streamFactory = null;
            if (factoryPid != null)
            {
                for (FactoryInternal factory : m_FactoryContext.getFactories().values())
                {
                    if (factory.getPid().equals(factoryPid))
                    {
                        streamFactory = factory;
                    }
                }
            }
            
            if (streamFactory == null)
            {
                return;
            }
            
            final String configPid = (String)event.getProperty(ConfigurationEventConstants.EVENT_PROP_PID);
            (new Thread(new ConfigHandler(configPid, streamFactory))).start();
        }
        
        /**
         * Method used to unregister this handler class.
         */
        public void unregsiterEvents()
        {
            m_ServiceRegistration.unregister();
        }
    }
    
    /**
     * Runnable that handles creating a stream profile object for the configuration with the specified PID.
     */
    class ConfigHandler implements Runnable
    {
        private FactoryInternal m_StreamProfileFactory;
        private String m_Pid;
        
        /**
         * Constructor that accepts the PID of the configuration the new stream profile object is to be associated
         * with and the product type that will be used to determine what kind of stream profile to create.
         * 
         * @param pid
         *      PID of the updated configuration.
         * @param streamProfileFactory
         *      The factory for stream profile configuration that has been updated.
         */
        ConfigHandler(final String pid, final FactoryInternal streamProfileFactory)
        {
            m_Pid = pid;
            m_StreamProfileFactory = streamProfileFactory;
        }
        
        @Override
        public void run()
        {
            synchronized (m_FactoryContext)
            {
                for (StreamProfile profile : getStreamProfiles())
                {
                    if (profile.getPid().equals(m_Pid))
                    {
                        return;
                    }
                }
            }
            
            final StreamProfileInternal spi;
            try
            {
                m_WakeLock.activate();
                spi = m_FactoryContext.getRegistry().createNewObjectForConfig(m_StreamProfileFactory, null, m_Pid);
            }
            catch (final IllegalArgumentException | FactoryException | FactoryObjectInformationException | IOException 
                    | InvalidSyntaxException ex)
            {
                m_Logging.error(ex, "Unable to automatically create a stream profile object for "
                        + "the configuration with PID: %s", m_Pid);
                return;
            }
            finally
            {
                m_WakeLock.cancel();
            }
            
            try
            {
                //Assign value for stream port
                spi.setStreamPort(getAvailableURI());
            }
            catch (final URISyntaxException ex)
            {
                m_Logging.error(ex, "Unable to set stream port for the stream profile: %s", spi.getName());
            }
        }
    }
    
    /**
     * Event handler that listens for stream profile object PID removed events, which correlates with a configuration 
     * being removed, and deletes the stream profile object that was associated with that PID.
     */
    class PidRemovedListener implements EventHandler
    {
        /**
         * The service registration reference for this handler class.
         */
        private ServiceRegistration<EventHandler> m_ServiceRegistration;
        
        /**
         * Method used to register this handler class for PID removed events.
         */
        public void registerEvents()
        {
            final Dictionary<String, Object> properties = new Hashtable<String, Object>();
            properties.put(EventConstants.EVENT_TOPIC, FactoryDescriptor.TOPIC_FACTORY_OBJ_PID_REMOVED);
            final String filterString = String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, 
                    StreamProfile.class.getSimpleName());
            properties.put(EventConstants.EVENT_FILTER, filterString);
            m_ServiceRegistration = m_Context.registerService(EventHandler.class, this, properties);
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final StreamProfileInternal spi = 
                    (StreamProfileInternal)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ);
            spi.delete();
        }
        
        /**
         * Method used to unregister this handler class.
         */
        public void unregsiterEvents()
        {
            m_ServiceRegistration.unregister();
        }
    }
}
