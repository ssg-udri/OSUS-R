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
package mil.dod.th.remote.client.impl;

import java.io.InputStream;
import java.io.OutputStream;

import mil.dod.th.remote.client.ChannelStateCallback;

/**
 * Represents a remote channel and its associated information.
 * 
 * @author dlandoll
 */
public class RemoteChannelInfo
{
    private final int m_ChannelId;
    private InputStream m_InStream;
    private OutputStream m_OutStream;
    private final ChannelStateCallback m_Callback;

    /**
     * Create a remote channel info object for an input stream.
     * 
     * @param channelId
     *      channel ID
     * @param inStream
     *      input stream
     * @param callback
     *      callback reference
     */
    public RemoteChannelInfo(final int channelId, final InputStream inStream, final ChannelStateCallback callback)
    {
        m_ChannelId = channelId;
        m_InStream = inStream;
        m_Callback = callback;
    }

    /**
     * Create a remote channel info object for an output stream.
     * 
     * @param channelId
     *      channel ID
     * @param outStream
     *      output stream
     * @param callback
     *      callback reference
     */
    public RemoteChannelInfo(final int channelId, final OutputStream outStream, final ChannelStateCallback callback)
    {
        m_ChannelId = channelId;
        m_OutStream = outStream;
        m_Callback = callback;
    }

    /**
     * Id of the remote system that messages will be sent to/received from.
     * 
     * @return
     *      id of the remote system
     */
    public int getChannelId()
    {
        return m_ChannelId;
    }

    public InputStream getInStream()
    {
        return m_InStream;
    }

    public OutputStream getOutStream()
    {
        return m_OutStream;
    }

    public ChannelStateCallback getCallback()
    {
        return m_Callback;
    }
}
