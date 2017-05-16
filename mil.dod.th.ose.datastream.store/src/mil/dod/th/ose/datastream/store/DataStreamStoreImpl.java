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
package mil.dod.th.ose.datastream.store;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import mil.dod.th.core.archiver.ArchiverException;
import mil.dod.th.core.archiver.ArchiverService;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.store.DataStreamStore;
import mil.dod.th.core.datastream.store.DateRange;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.ose.datastream.store.data.StreamArchiveMetadata;

/**
 * Implementation for {@link DataStreamStore} interface.
 * 
 * @see DataStreamStore
 * @author jmiller
 *
 */
@Component(name = DataStreamStoreImpl.PID, designate = DataStreamStoreConfig.class,
    configurationPolicy = ConfigurationPolicy.require)
public class DataStreamStoreImpl implements DataStreamStore, ThreadStatusListener 
{
    /**
     * Persistent identity (PID) for the configuration.
     */
    public final static String PID = "mil.dod.th.ose.datastream.store.DataStreamStore";
    
    /**
     * Time in milliseconds to allow for thread to join.
     */
    private static final long THREAD_JOIN_TIME = 1000;
    
    /**
     * Reference to logging service.
     */
    private LoggingService m_LoggingService;
    
    /**
     * Reference to DataStreamService.
     */
    private DataStreamService m_DataStreamService;
    
    /**
     * Reference to PersistentDataStore.
     */
    private PersistentDataStore m_PersistentDataStore;
    
    /**
     * Reference to ArchiverService.
     */
    private ArchiverService m_ArchiverService;
    
    /**
     * Threads to keep track of enabled archiving processes that have not yet begun.
     */
    private Map<StreamProfile, ClientResponseThread> m_ClientResponseThreads;

    /**
     * Top-level directory of where to store archived streaming data.
     */
    private String m_FileStoreTopDir;
    
    /**
     * Ordering to arrange PersistentData records in chronological order.
     */
    private Ordering<PersistentData> m_RecordOrdering;


    ///////////////////////////////////////////////////////////////////////////
    // OSGi binding methods
    ///////////////////////////////////////////////////////////////////////////
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_LoggingService = logging;
    }
    
    @Reference
    public void setDataStreamService(final DataStreamService dataStreamService)
    {
        m_DataStreamService = dataStreamService;
    }
    
    @Reference
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        m_PersistentDataStore = persistentDataStore;
    }
    
    @Reference
    public void setArchiverService(final ArchiverService archiverService)
    {
        m_ArchiverService = archiverService;
    }


    ///////////////////////////////////////////////////////////////////////////
    // OSGi Declarative Services methods.
    ///////////////////////////////////////////////////////////////////////////
    
    /**
     * The service component activation method.
     * 
     * @param props
     *      configuration properties associated with the component
     */
    @Activate
    public void activate(final Map<String, Object> props)
    {
        m_ClientResponseThreads = new HashMap<>();
        
        final DataStreamStoreConfig config = Configurable.createConfigurable(DataStreamStoreConfig.class, props);
        m_FileStoreTopDir = config.filestoreTopDir();
        
        m_RecordOrdering = new Ordering<PersistentData>()
        {
            @Override
            public int compare(final PersistentData left, final PersistentData right)
            {
                if (left.getTimestamp() < right.getTimestamp())
                {
                    return -1;
                }
                else if (left.getTimestamp() > right.getTimestamp())
                {
                    return 1;
                }
                
                return 0;
            }
        };
    }
    
    /**
     * Deactivate this service and halt any current archiving processes.
     */
    @Deactivate
    public void deactivate()
    {
        for (Entry<StreamProfile, ClientResponseThread> item : m_ClientResponseThreads.entrySet())
        {
            item.getValue().interrupt();
            try
            {
                item.getValue().join(THREAD_JOIN_TIME);
            }
            catch (final InterruptedException e)
            {
                m_LoggingService.error("Error joining client response thread");
            }
            
            try
            {
                m_ArchiverService.stop(item.getKey().getUuid().toString());
            }
            catch (final IllegalArgumentException e)
            {
                m_LoggingService.error("Error stopping archiver for id %s", item.getKey().getUuid().toString());
            }
           
        }
        
        m_ClientResponseThreads.clear();
        
    }
    
    /**
     * Update configuration with new properties.
     * 
     * @param props
     *      map of key-value pairs
     */
    @Modified
    public void modified(final Map<String, Object> props)
    {
        final DataStreamStoreConfig config = Configurable.createConfigurable(DataStreamStoreConfig.class, props);
        m_FileStoreTopDir = config.filestoreTopDir();
        
    }

    @Override
    public void enableArchiving(final StreamProfile streamProfile,
           final boolean useSourceBitrate, final long heartbeatPeriod, final long delay)
                   throws IllegalArgumentException, IllegalStateException 
    {
        final Set<StreamProfile> profiles =  m_DataStreamService.getStreamProfiles();
        
        if (!profiles.contains(streamProfile))
        {
            throw new IllegalArgumentException(String.format("Could not enable archiving for stream profile %s",
                    streamProfile.getName()));
        }
           
        //Check if an entry in m_ClientResponseThreads already exists for streamProfile
        final ClientResponseThread oldThread = m_ClientResponseThreads.get(streamProfile);
        if (oldThread != null)
        {
            if (oldThread.isAlive())
            {
                oldThread.interrupt();
                try
                {
                    oldThread.join(THREAD_JOIN_TIME);
                }
                catch (final InterruptedException e)
                {
                    m_LoggingService.error("enableArchiving error waiting for thread to join");
                }
                
                m_LoggingService.warning("Archiving process for stream profile UUID: %s was previously enabled",
                        streamProfile.getUuid().toString());
            }
        }

        //Start a thread that monitors if a client acknowledgement has been received within the
        //heartbeat period. If so, the countdown resets.  If not, the archiving process begins.
        final ClientResponseThread thread = new ClientResponseThread(
                this, streamProfile, heartbeatPeriod, delay, useSourceBitrate);
        m_ClientResponseThreads.put(streamProfile, thread);
        thread.start();
    }


    @Override
    public void disableArchiving(final StreamProfile streamProfile)
            throws IllegalArgumentException, IllegalStateException 
    {
        final Set<StreamProfile> profiles =  m_DataStreamService.getStreamProfiles();
        
        if (!profiles.contains(streamProfile))
        {
            throw new IllegalArgumentException(String.format("Could not disable archiving for stream profile %s",
                    streamProfile.getName()));
        }
        
        //Check if an client response thread with this streamProfile's ID already exists
        final ClientResponseThread thread = m_ClientResponseThreads.get(streamProfile);
        if (thread == null)
        {
            throw new IllegalStateException("No existing archiving process for StreamProfile UUID: "
                    + streamProfile.getUuid().toString());
        }
        else
        {
            thread.interrupt();
            try
            {
                thread.join(THREAD_JOIN_TIME);
            }
            catch (final InterruptedException e)
            {
                m_LoggingService.error("disableArchiving error waiting for thread to join");
            }
            
            try
            {
                m_ArchiverService.stop(streamProfile.getUuid().toString());
            }
            catch (final IllegalArgumentException e)
            {
                //If archiver is not yet running 
                m_LoggingService.info("Archiving process disabled before archiver started");
            }
            
            if (thread.getArchiveStartTime() > 0)
            {
                
                final boolean useSourceBitrate = thread.isUseSourceBitrate();
                final String filePath = createFilePath(streamProfile, thread.getArchiveStartTime());

                //Create an entry in persistent data store
                try
                {
                    final StreamArchiveMetadata metadata = new StreamArchiveMetadata(
                            new File(filePath).toURI().toURL(), thread.getArchiveStartTime(), 
                            System.currentTimeMillis(), useSourceBitrate);

                    m_PersistentDataStore.persist(DataStreamStoreImpl.class, UUID.randomUUID(), 
                            streamProfile.getUuid().toString(), metadata);
                }
                catch (final MalformedURLException mue)
                {
                    m_LoggingService.error("Error creating URL for file path: %s", filePath);
                }
            }
 
            m_ClientResponseThreads.remove(streamProfile);
        }

    }


    @Override
    public void clientAck(final StreamProfile streamProfile)
    {
        final ClientResponseThread thread = m_ClientResponseThreads.get(streamProfile);
        if (thread != null)
        {
            thread.reset();
        }
    }


    @Override
    public List<DateRange> getArchivePeriods(final StreamProfile streamProfile)
            throws IllegalArgumentException 
    {

        final List<DateRange> dateRanges = new ArrayList<>();
        
        final Collection<PersistentData> records = m_PersistentDataStore.query(
                DataStreamStoreImpl.class, streamProfile.getUuid().toString());
        
        //Arrange records in chronological order        
        final ArrayList<PersistentData> orderedRecords = Lists.newArrayList(records);
        Collections.sort(orderedRecords, m_RecordOrdering);
        
        
        for (PersistentData record : orderedRecords)
        {
            final StreamArchiveMetadata metadata = (StreamArchiveMetadata)record.getEntity();
            dateRanges.add(new DateRange(metadata.getStartTimestamp(), metadata.getStopTimestamp()));            
        }
        
        return dateRanges;
    }


    @Override
    public InputStream getArchiveStream(final StreamProfile streamProfile,
            final DateRange dateRange) throws IllegalArgumentException 
    {
        

        final List<InputStream> inputStreams = new ArrayList<>();
        
        final Collection<PersistentData> records = m_PersistentDataStore.query(
                DataStreamStoreImpl.class, streamProfile.getUuid().toString());
        
        //Arrange records in chronological order        
        final ArrayList<PersistentData> orderedRecords = Lists.newArrayList(records);
        Collections.sort(orderedRecords, m_RecordOrdering);
        
        for (PersistentData record : orderedRecords)
        {
            final StreamArchiveMetadata metadata = (StreamArchiveMetadata)record.getEntity();
            
            if (metadata.getStopTimestamp() < dateRange.getStartTime() 
                    || metadata.getStartTimestamp() > dateRange.getStopTime())
            {
                continue;
            }
            else
            {
                
                final URL filePath = metadata.getFilePath();
                try
                {
                    inputStreams.add(new BufferedInputStream(filePath.openStream()));
                }
                catch (final IOException e)
                {
                    m_LoggingService.error("Error opening file from URL %s", filePath.toString());
                }
            }
        }
        
        return new SequenceInputStream(Collections.enumeration(inputStreams));

    }
    
    ///////////////////////////////////////////////////////////////////////////
    // ThreadStatusListener method
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void notifyObserver(final StreamProfile profile, final ThreadState threadState)
    {
        if (threadState == ThreadState.FINISHED)
        {
            
            final ClientResponseThread thread = m_ClientResponseThreads.get(profile);
            if (thread != null)
            {
                final boolean useSourceBitrate = thread.isUseSourceBitrate();
                final String filePath = createFilePath(profile, thread.getArchiveStartTime());

                try
                {
                    //Start archiver process
                    if (useSourceBitrate)
                    {
                        m_ArchiverService.start(profile.getUuid().toString(), 
                                profile.getConfig().dataSource(), filePath);
                    }
                    else
                    {
                        m_ArchiverService.start(profile.getUuid().toString(), profile.getStreamPort(), filePath);
                    }
                }
                catch (final ArchiverException ae)
                {
                    m_LoggingService.error("Error starting archiver process for Stream Profile uuid %s", 
                            profile.getUuid().toString());
                }
            }
        }
        else if (threadState == ThreadState.INTERRUPTED)
        {
            // Remove thread from map if it exists
            m_ClientResponseThreads.remove(profile);
        }
    }
    
    /**
     * Create a file path based on the top-level file store directory, the stream profile UUID as a String,
     * and the current time.
     * 
     * @param profile
     *      the StreamProfile whose data is being archived
     * @param startTime
     *      start time in milliseconds of the data in the file
     * @return
     *      file path as a String
     */
    private String createFilePath(final StreamProfile profile, final long startTime)
    {
        return String.format("%s/%s/%d", m_FileStoreTopDir, profile.getUuid().toString(), startTime);
    }
    

}
