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
package mil.dod.th.ose.datastream.store.data;

import java.io.Serializable;
import java.net.URL;

/**
 * This class defines the serializable data structure that is persisted in 
 * {@link mil.dod.th.core.persistence.PersistentDataStore}
 * as a way to reference binary data stored on the filesystem.
 * 
 * @author jmiller
 *
 */
public class StreamArchiveMetadata implements Serializable
{

    /**
     * The serial version ID for this class.
     */
    private static final long serialVersionUID = -948348635769563323L;
    
    /**
     * The full path of the file that contains the binary streaming data for the given date range.
     */
    private URL m_FilePath;
    
    /**
     * The start time of the binary streaming data in milliseconds.
     */
    private long m_StartTimestamp;
    
    /**
     * The stop time of the binary streaming data in milliseconds.
     */
    private long m_StopTimestamp;
    
    /**
     * Boolean that indicates whether the streaming data is archived at the original bitrate.
     */
    private boolean m_IsOriginalBitrate;

    /**
     * Constructor for {@link StreamArchiveMetadata} object.
     * 
     * @param filePath
     *      The full path of the binary file.
     * @param startTimestamp
     *      The start time of the binary data, in milliseconds.
     * @param stopTimestamp
     *      The stop time of the binary data, in milliseconds.
     * @param isOriginalBitrate
     *      True if the data archived is at the original bitrate; false if it is archived at the bitrate
     *      specified in the associated {@link mil.dod.th.core.datastream.StreamProfile} object.
     */
    public StreamArchiveMetadata(final URL filePath, final long startTimestamp, final long stopTimestamp, 
            final boolean isOriginalBitrate)
    {
        m_FilePath = filePath;
        m_StartTimestamp = startTimestamp;
        m_StopTimestamp = stopTimestamp;
        m_IsOriginalBitrate = isOriginalBitrate;
    }

    public URL getFilePath()
    {
        return m_FilePath;
    }

    public void setFilePath(final URL filePath)
    {
        m_FilePath = filePath;
    }

    public long getStartTimestamp()
    {
        return m_StartTimestamp;
    }

    public void setStartTimestamp(final long startTimestamp)
    {
        m_StartTimestamp = startTimestamp;
    }

    public long getStopTimestamp()
    {
        return m_StopTimestamp;
    }

    public void setStopTimestamp(final long stopTimestamp)
    {
        m_StopTimestamp = stopTimestamp;
    }

    public boolean isOriginalBitrate()
    {
        return m_IsOriginalBitrate;
    }

    public void setIsOriginalBitrate(final boolean isOriginalBitrate)
    {
        m_IsOriginalBitrate = isOriginalBitrate;
    }
    
}
