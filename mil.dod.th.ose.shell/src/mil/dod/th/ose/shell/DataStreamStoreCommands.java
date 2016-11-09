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
package mil.dod.th.ose.shell;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.store.DataStreamStore;
import mil.dod.th.core.datastream.store.DateRange;

import org.apache.felix.service.command.Descriptor;


/**
 * Shell commands for DataStreamStore.
 * 
 * @author jmiller
 *
 */
@Component(provide = DataStreamStoreCommands.class, properties = { "osgi.command.scope=thstream",
    "osgi.command.function=enableArchiving|disableArchiving|clientAck|getArchivePeriods" })
public class DataStreamStoreCommands
{
    
    /**
     * Reference to DataStreamService.
     */
    private DataStreamService m_DataStreamService;
    
    /**
     * Reference to DataStreamStore.
     */
    private DataStreamStore m_DataStreamStore;
    
    /**
     * Bind the DataStreamService instance.
     * 
     * @param dataStreamService
     *      the m_DataStreamService to set
     */
    @Reference
    public void setDataStreamService(final DataStreamService dataStreamService)
    {
        m_DataStreamService = dataStreamService;
    }
    
    /**
     * Bind the DataStreamStore instance.
     * 
     * @param dataStreamStore
     *      the m_DataStreamStore to set
     */
    @Reference
    public void setDataStreamStore(final DataStreamStore dataStreamStore)
    {
        m_DataStreamStore = dataStreamStore;
    }
    
    /**
     * Enables an archiving process for a given stream profile.
     * 
     * @param profileName
     *      the name of the StreamProfile instance
     * @param useSourceBitrate
     *      true to use original source bitrate; false to use the output of the transcoder
     * @param heartbeatPeriod
     *      time in seconds that a client must call clientAck() before data begins saving to disk
     * @param delay
     *      time in seconds before the heartbeatPeriod countdown initially begins
     */
    @Descriptor("Enables an archiving process for a given stream profile.")
    public void enableArchiving(@Descriptor("StreamProfile name") final String profileName, 
            @Descriptor("Use source bitrate") final boolean useSourceBitrate, 
            @Descriptor("Heartbeat period, in seconds") final long heartbeatPeriod,
            @Descriptor("Initial delay, in seconds") final long delay)
    {
        final Set<StreamProfile> profiles = m_DataStreamService.getStreamProfiles();
        
        for (StreamProfile profile : profiles)
        {
            if (profile.getName().equalsIgnoreCase(profileName))
            {
                m_DataStreamStore.enableArchiving(profile, useSourceBitrate, heartbeatPeriod, delay);
                return;
            }
        }
    }
    

    /**
     * Disables an archiving process for a given stream profile.
     * 
     * @param profileName
     *      the name of the StreamProfile instance
     */
    @Descriptor("Disables an archiving process for a given stream profile.")
    public void disableArchiving(@Descriptor("StreamProfile name") final String profileName)
    {
        final Set<StreamProfile> profiles = m_DataStreamService.getStreamProfiles();
        
        for (StreamProfile profile : profiles)
        {
            if (profile.getName().equalsIgnoreCase(profileName))
            {
                m_DataStreamStore.disableArchiving(profile);
                return;
            }
        }
    }
    
    /**
     * Prevent data from being archived for an enabled archiving process.
     * 
     * @param profileName
     *      the name of the StreamProfile instance
     */
    @Descriptor("Prevent data from being archived for an enabled archiving process.")
    public void clientAck(@Descriptor("StreamProfile name") final String profileName)
    {
        final Set<StreamProfile> profiles = m_DataStreamService.getStreamProfiles();

        for (StreamProfile profile : profiles)
        {
            if (profile.getName().equalsIgnoreCase(profileName))
            {
                m_DataStreamStore.clientAck(profile);
                return;
            }
        }
    }
    
    /**
     * Retrieve time periods of archived data available for a given Stream Profile.
     * 
     * @param profileName
     *      the name of the StreamProfile instance
     * @return
     *      list of Date object pairs ordered as [StartDate, StopDate]
     */
    @Descriptor("Retrieve time periods of archived data available for a given Stream Profile")
    public List<Date> getArchivePeriods(@Descriptor("StreamProfile name") final String profileName)
    {
        final Set<StreamProfile> profiles = m_DataStreamService.getStreamProfiles();

        // Return a list of Date objects of the format:
        // [StartDate1, StopDate1, StartDate2, StopDate2,...,StartDateN, StopDateN]
        final List<Date> periods = new ArrayList<>();
        
        for (StreamProfile profile : profiles)
        {
            if (profile.getName().equalsIgnoreCase(profileName))
            {
                final List<DateRange> dateRanges = m_DataStreamStore.getArchivePeriods(profile);
                for (DateRange range : dateRanges)
                {
                    periods.add(new Date(range.getStartTime()));
                    periods.add(new Date(range.getStopTime()));
                }
            }
        }
        
        return periods;
    }

}
