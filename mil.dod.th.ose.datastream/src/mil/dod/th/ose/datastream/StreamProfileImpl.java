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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfileAttributes;
import mil.dod.th.core.datastream.StreamProfileException;
import mil.dod.th.core.datastream.StreamProfileProxy;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.transcoder.TranscoderService;
import mil.dod.th.ose.core.factory.api.AbstractFactoryObject;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;

/**
 * StreamProfile implementation of a {@link mil.dod.th.core.factory.FactoryObject}.
 * 
 * @author jmiller
 */
@Component(factory = StreamProfileInternal.COMPONENT_FACTORY_REG_ID)
public class StreamProfileImpl extends AbstractFactoryObject implements StreamProfileInternal
{
    /**
     * The {@link StreamProfileProxy} that is associated with this implementation.
     */
    private StreamProfileProxy m_StreamProfileProxy;
    
    /**
     * Boolean to track the enabled state of this stream profile instance.
     */
    private boolean m_Enabled;
    
    /**
     * Client-facing connection point for data stream.
     */
    private URI m_StreamPort;

    /**
     * Logging service instance.
     */
    private LoggingService m_Log;
    
    /**
     * Instance of {@link AssetDirectoryService} associated with this implementation.
     */
    private AssetDirectoryService m_AssetDirectoryService;
    
    /**
     * Reference to the transcoder service.
     */
    private TranscoderService m_TranscoderService;    
    
    /**
     * The {@link StreamProfileFactoryObjectDataManager} for this implementation.
     */
    private StreamProfileFactoryObjectDataManager m_StreamProfileFactoryObjectDataManager;
    
    /**
     * Wake lock used to keep system awake when the stream is enabled.
     */
    private WakeLock m_WakeLock;

    /**
     * Method to set the {@link LoggingService} to use.
     * @param loggingService interface for logging service
     */
    @Reference
    public void setLoggingService(final LoggingService loggingService)
    {
        m_Log = loggingService;
    }
    
    /**
     * Method to set the {@link AssetDirectoryService} to use.
     * @param assetDirectoryService interface for asset directory service
     */
    @Reference
    public void setAssetDirectoryService(final AssetDirectoryService assetDirectoryService)
    {
        m_AssetDirectoryService = assetDirectoryService;
    }
    
    /**
     * Method used to set the {@link TranscoderService}.
     * 
     * @param transcoderService service to bind
     */
    @Reference
    public void setTranscoderService(final TranscoderService transcoderService)
    {
        m_TranscoderService = transcoderService;
    }
    
    @Reference
    public void setFactoryObjectDataManager(final StreamProfileFactoryObjectDataManager manager)
    {
        m_StreamProfileFactoryObjectDataManager = manager;
    }
    
    @Override
    public void initialize(final FactoryRegistry<?> registry, final FactoryObjectProxy proxy, 
            final FactoryInternal factory, final ConfigurationAdmin configAdmin, final EventAdmin eventAdmin, 
            final PowerManagerInternal powerMgr, final UUID uuid, final String name, final String pid,
            final String baseType) 
                    throws IllegalStateException
    {
        super.initialize(registry, proxy, factory, configAdmin, eventAdmin, powerMgr, uuid, name, pid, baseType);
        m_StreamProfileProxy = (StreamProfileProxy)proxy;
        m_Enabled = false;
        m_WakeLock = powerMgr.createWakeLock(m_StreamProfileProxy.getClass(), this, "coreStreamProfile");
        
        try
        {
            m_StreamPort = m_StreamProfileFactoryObjectDataManager.getStreamPort(getUuid());
        }
        catch (final FactoryObjectInformationException e)
        {
            m_Log.error("Unable to retrieve stream port information for stream profile with uuid: %s", 
                    getUuid().toString());
        }
    }

    @Override
    public Asset getAsset()
    {
        return m_AssetDirectoryService.getAssetByName(getConfig().assetName());
    }
    
    @Override
    public double getBitrate()
    {
        return getConfig().bitrateKbps();
    }

    @Override
    public StreamProfileAttributes getConfig()
    {
        return Configurable.createConfigurable(StreamProfileAttributes.class, getProperties());
    }

    @Override
    public URI getDataSource()
    {
        return getConfig().dataSource();
    }

    @Override
    public String getFormat()
    {
        return getConfig().format();
    }

    @Override
    public String getSensorId()
    {
        return getConfig().sensorId();
    }

    @Override
    public URI getStreamPort()
    {
        return m_StreamPort;
    }

    @Override
    public boolean isEnabled()
    {
        return m_Enabled;
    }

    @Override
    public void setEnabled(final boolean enable)
    {
        if (enable)
        {
            if (m_Enabled)
            {
                m_Log.warning("Stream profile with UUID %s is already enabled.", getUuid());
                return;
            }
            
            try 
            {
                m_StreamProfileProxy.onEnabled();

                // Prevent the system from sleeping when the stream profile is enabled
                m_WakeLock.activate();

                m_Enabled = true;
                
                final Map<String, Object> transcoderProps = new HashMap<>();
                transcoderProps.put(TranscoderService.CONFIG_PROP_BITRATE_KBPS, getBitrate());
                transcoderProps.put(TranscoderService.CONFIG_PROP_FORMAT, getFormat());
                
                final Map<String, Object> props = new HashMap<>();
                props.put(DataStreamService.EVENT_PROP_STREAM_PROFILE, this);
                props.put(DataStreamService.EVENT_PROP_STREAM_PROFILE_ENABLED, true);
                postEvent(DataStreamService.TOPIC_STREAM_PROFILE_STATE_CHANGED, props);

                m_TranscoderService.start(getUuid().toString(), getDataSource(), getStreamPort(), transcoderProps);
            } 
            catch (final StreamProfileException spe)
            {
                m_Log.error("Stream profile with UUID %s could not be enabled.", getUuid());
            }
            catch (final Exception e)
            {
                m_Log.error("Exception occurred enabling StreamProfile with UUID: %s", getUuid().toString());
            }
        }
        else
        {
            if (!m_Enabled)
            {
                m_Log.warning("Stream profile with UUID %s is already disabled.", getUuid());
                return;
            }
            
            try
            {
                m_StreamProfileProxy.onDisabled();
                m_Enabled = false;
                
                final Map<String, Object> props = new HashMap<>();
                props.put(DataStreamService.EVENT_PROP_STREAM_PROFILE, this);
                props.put(DataStreamService.EVENT_PROP_STREAM_PROFILE_ENABLED, false);
                postEvent(DataStreamService.TOPIC_STREAM_PROFILE_STATE_CHANGED, props);
                
                m_TranscoderService.stop(getUuid().toString());
            } 
            catch (final StreamProfileException spe)
            {
                m_Log.error("Stream profile with UUID %s could not be disabled.", getUuid());

            }
            finally
            {
                m_WakeLock.cancel();
            }
        }
    }

    @Override
    public void setStreamPort(final URI streamPort)
    {
        m_StreamPort = streamPort;
        
        try
        {
            //Try to retrieve stream port from persistent store if it exists
            m_StreamProfileFactoryObjectDataManager.setStreamPort(getUuid(), streamPort);
        }
        catch (final FactoryObjectInformationException e)
        {
            m_Log.error("Unable to persist stream port information for stream profile with uuid: %s", 
                    getUuid().toString());
        }
    }

    @Override
    public void delete() throws IllegalStateException
    {
        if (isEnabled())
        {
            throw new IllegalStateException(
                    String.format("Stream Profile is enabled, cannot remove Stream Profile [%s]",
                    getName()));
        }

        m_WakeLock.delete();

        super.delete();
    }
}
