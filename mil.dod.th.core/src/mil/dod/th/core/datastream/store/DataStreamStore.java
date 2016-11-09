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
package mil.dod.th.core.datastream.store;

import java.io.InputStream;
import java.util.List;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.datastream.StreamProfile;

/**
 * <p>
 * The DataStreamStore interface defines the necessary methods for consumers to store
 * and retrieve streaming data. The interface uses existing {@link StreamProfile}
 * instances as a way to tag and reference archived data.
 * 
 * @author jmiller
 *
 */
@ProviderType
public interface DataStreamStore
{
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/datastream/store/DataStreamStore/";
    
    /**
     * Topic used for when archiving has been enabled for a stream profile. This does
     * not mean that data is being archived to disk, only that archiving
     * will happen if {@link DataStreamStore#clientAck(StreamProfile)} is not called
     * within the heartbeat period. The following properties will be included in the
     * event as applicable:
     * <ul>
     * <li>{@link #EVENT_PROP_ARCHIVE_ID} - the ID of the stream archive, which
     * matches the String representation of the associated {@link StreamProfile}'s UUID
     * <li>{@link #EVENT_PROP_HEARTBEAT_PERIOD_SECONDS} - the amount of time, in seconds,
     * that the archiving process will wait for a call to {@link #clientAck(StreamProfile)}
     * before starting. 
     * <li>{@link #EVENT_PROP_INITIAL_DELAY_SECONDS} - the amount of time, in seconds, that
     * the archiving process will wait before starting its heartbeat period countdown. 
     * </ul>
     * 
     * @see #clientAck(StreamProfile)
     */
    String TOPIC_ARCHIVING_ENABLED = TOPIC_PREFIX + "ARCHIVING_ENABLED";
    
    /** 
     * Topic used for when archiving has been disabled. The following properties will be
     * included in the event as applicable:
     * <ul>
     * <li>{@link #EVENT_PROP_ARCHIVE_ID} - the ID of the stream archive, which
     * matches the String representation of the associated {@link StreamProfile}'s UUID
     * </ul>
     */ 
    String TOPIC_ARCHIVING_DISABLED = TOPIC_PREFIX + "ARCHIVING_DISABLED";
    
    /** 
     * Topic used for when archiving has started. This event will occur if archiving had
     * previously been enabled for the stream profile and the heartbeat period has lapsed.
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link #EVENT_PROP_ARCHIVE_ID} - the ID of the stream archive, which
     * matches the String representation of the associated {@link StreamProfile}'s UUID
     * <li>{@link #EVENT_PROP_HEARTBEAT_PERIOD_SECONDS} - the amount of time, in seconds,
     * that the archiving process will wait for a call to {@link #clientAck(StreamProfile)}
     * before starting. 
     * </ul>
     * 
     * @see #clientAck(StreamProfile)
     */
    String TOPIC_ARCHIVING_STARTED = TOPIC_PREFIX + "ARCHIVING_STARTED";
    
    /** 
     * Topic used for when archiving has stopped. This event will occur when an active
     * archiving process has been interrupted either by a call to {@link #clientAck(StreamProfile)}
     * or an explicit disable from {@link #disableArchiving(StreamProfile)}. The following
     * properties will be included in the event as applicable:
     * <ul>
     * <li>{@link #EVENT_PROP_ARCHIVE_ID} - the ID of the stream archive, which
     * matches the String representation of the associated {@link StreamProfile}'s UUID
     * <li>{@link #EVENT_PROP_ARCHIVE_ENABLED} - the enabled state of the archiving process. 
     * <li>{@link #EVENT_PROP_HEARTBEAT_PERIOD_SECONDS} - the amount of time, in seconds,
     * that the archiving process will wait for a call to {@link #clientAck(StreamProfile)}
     * before starting. 
     * </ul>
     * 
     * @see #disableArchiving(StreamProfile)
     * @see #clientAck(StreamProfile)
     */
    String TOPIC_ARCHIVING_STOPPED = TOPIC_PREFIX + "ARCHIVING_STOPPED";
    
    /** Event property key for the ID of the stream archive. */
    String EVENT_PROP_ARCHIVE_ID = "archive.id";
    
    /** Event property key for enabled state of the stream archive. */
    String EVENT_PROP_ARCHIVE_ENABLED = "archive.enabled";
    
    /** Event property key for the heartbeat period (in seconds). */
    String EVENT_PROP_HEARTBEAT_PERIOD_SECONDS = "heartbeat.period.seconds";
    
    /** Event property key for the amount of time in seconds for which the heartbeat
     * countdown should be delayed.
     */
    String EVENT_PROP_INITIAL_DELAY_SECONDS = "initial.delay.seconds";
    
    /**
     * Enables archiving of streaming data described by a {@link StreamProfile}. The method allows
     * the consumer to specify whether to archive data at the original source bitrate, or at
     * the target bitrate specified by the stream profile. The consumer also provides a heartbeatPeriod,
     * which is the amount of time an enabled archive will wait for a client acknowledgement before beginning
     * to archive data.
     * 
     * @param streamProfile
     *      The stream profile instance that contains the relevant parameters for the archiving process.
     * @param useSourceBitrate
     *      True if the archived data should be the original resolution data from the asset; false if the
     *      archived data should be at the target bitrate specified in the {@link StreamProfile}.
     * @param heartbeatPeriod
     *      Time in seconds that a client has to call the {@link #clientAck(StreamProfile)} method in order
     *      to maintain the live stream and prevent archiving to disk. If the value is 0, streaming data
     *      will be archived regardless of a client calling {@link #clientAck(StreamProfile)}.
     * @param delay
     *      Initial delay in seconds for when the heartbeat countdown timer should begin.
     * 
     * @throws IllegalArgumentException 
     *      if the stream profile does not exist in the registry
     * @throws IllegalStateException 
     *      if archiving has already been enabled for the specified StreamProfile. 
     * 
     * @see #clientAck(StreamProfile)
     */
    void enableArchiving(StreamProfile streamProfile, boolean useSourceBitrate, long heartbeatPeriod, long delay)
            throws IllegalArgumentException, IllegalStateException;

    /**
     * Disables archiving of streaming data described by a {@link StreamProfile}.
     * 
     * @param streamProfile
     *      The stream profile instance whose UUID is used to reference the archive operation.
     * 
     * @throws IllegalArgumentException
     *      if the stream profile does not exist in the registry.
     * @throws IllegalStateException 
     *      if archiving is not currently enabled for the specified StreamProfile.
     */
    void disableArchiving(StreamProfile streamProfile) throws IllegalArgumentException, IllegalStateException;
    
    /**
     * Called by consumer to acknowledge a connection and prevent streaming data from being archived to disk.
     * When this method is called, the heartbeatPeriod countdown specified in {@link #enableArchiving} is reset.
     *
     * @param streamProfile
     *      The stream profile instance whose UUID is used to reference the archive operation.
     * @throws IllegalArgumentException 
     *      if the stream profile does not exist in the registry.
     * @throws IllegalStateException 
     *      if archiving is not currently enabled for the specified StreamProfile.
     * 
     * @see #enableArchiving(StreamProfile, boolean, long, long)
     */
    void clientAck(StreamProfile streamProfile) throws IllegalArgumentException, IllegalStateException;
    
    /**
     * Returns all available time periods of archived data associated with a stream profile. Any adjacent
     * {@link DateRange}s should be consolidated so that the returned list contains as few elements
     * as possible.
     * 
     * @param streamProfile
     *      The stream profile instance whose UUID is used to reference the archived data.
     * @return
     *      list of {@link DateRange}s that contain archived data, in chronological order
     * @throws IllegalArgumentException
     *      if the stream profile does not exist in the registry
     */
    List<DateRange> getArchivePeriods(StreamProfile streamProfile) throws IllegalArgumentException;
    
    /**
     * Returns an {@link InputStream} reference for archived data from a specified date range. The 
     * data will be archived with no additional headers or framing. If an archived stream spans
     * multiple files or database records, the data stream will be reconstructed by appending
     * together the binary data from sequential files/records.
     * 
     * @param streamProfile
     *      The stream profile instance whose UUID is used to reference the archived data.
     * @param dateRange
     *      The requested start and stop dates for the data stream.
     * @return
     *      Stream from which consumer can retrieve binary data.
     * @throws IllegalArgumentException 
     *      if stream profile does not exist in the registry, or if no data is available for the
     *      requested date range. 
     */
    InputStream getArchiveStream(StreamProfile streamProfile, DateRange dateRange) throws IllegalArgumentException;
    
    
    
    
    
}
