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
package mil.dod.th.ose.controller.integration.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;

/**
 * Tests loading of configuration information from an .xml file for all remote channel types on a 2nd run of the 
 * controller.
 * 
 * @author cweisenborn
 */
public class TestRemoteChannelConfigs2ndRun
{
    /**
     * Verify that the remote channels defined in the configs.xml are added to the {@link RemoteChannelLookup}.
     */
    @Test
    public void testRemoteChannelConfigLoading()
    {
        RemoteChannelLookup remoteChannelLookup = IntegrationTestRunner.getService(RemoteChannelLookup.class);
        
        assertThat(remoteChannelLookup.getChannels(300).size(), is(1));
        assertThat(remoteChannelLookup.getChannels(2).size(), is(1));
        
        TransportChannel transportChannel = (TransportChannel)remoteChannelLookup.getChannels(300).get(0);
        assertThat(transportChannel.getTransportLayerName(), is("testTransportLayer"));
        assertThat(transportChannel.getLocalMessageAddress(), is("t1:1.1.1"));
        assertThat(transportChannel.getRemoteMessageAddress(), is("t2:1.1.2"));
        
        SocketChannel socketChannel = (SocketChannel)remoteChannelLookup.getChannels(2).get(0);
        assertThat(socketChannel.getHost(), is("localhost"));
        assertThat(socketChannel.getPort(), is(4001));
    }
}
