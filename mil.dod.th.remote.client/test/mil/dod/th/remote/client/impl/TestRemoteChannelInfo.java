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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.InputStream;
import java.io.OutputStream;

import mil.dod.th.remote.client.ChannelStateCallback;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestRemoteChannelInfo
{
    private static final int IN_ID = 1;
    private static final int OUT_ID = 2;
    
    @Mock private InputStream m_InputStream;
    @Mock private OutputStream m_OutputStream;
    @Mock private ChannelStateCallback m_Callback;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testChannelWithInStream()
    {
        // replay
        RemoteChannelInfo sut = new RemoteChannelInfo(IN_ID, m_InputStream, m_Callback);

        // verify
        assertThat(sut.getChannelId(), is(IN_ID));
        assertThat(sut.getInStream(), is(m_InputStream));
        assertThat(sut.getOutStream(), nullValue());
        assertThat(sut.getCallback(), is(m_Callback));
    }

    @Test
    public void testChannelWithOutStream()
    {
        // replay
        RemoteChannelInfo sut = new RemoteChannelInfo(OUT_ID, m_OutputStream, m_Callback);

        // verify
        assertThat(sut.getChannelId(), is(OUT_ID));
        assertThat(sut.getInStream(), nullValue());
        assertThat(sut.getOutStream(), is(m_OutputStream));
        assertThat(sut.getCallback(), is(m_Callback));
    }
}
