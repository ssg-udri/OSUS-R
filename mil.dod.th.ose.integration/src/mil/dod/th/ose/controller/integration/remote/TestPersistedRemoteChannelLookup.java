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
package mil.dod.th.ose.controller.integration.remote;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;

/**
 * @author Dave Humeniuk
 *
 */
public class TestPersistedRemoteChannelLookup
{
    /**
     * Verify channels setup in {@link 
     * mil.dod.th.ose.controller.integration.TestPrepFor2ndRun#testCreateRemoteChannels()}.
     */
    @Test
    public void testChannelsRestored()
    {
        RemoteChannelLookup remoteChannelLookup = IntegrationTestRunner.getService(RemoteChannelLookup.class);
        
        Map<Integer, Set<RemoteChannel>> channels = remoteChannelLookup.getAllChannels();
        
        assertThat(channels.size(), is(6));
        
        // check each channel by adding the entries in a set of strings "systemId:transport:localAddress:remoteAddress"
        Set<String> channelStrings = new HashSet<String>();
        for (int systemId : channels.keySet())
        {
            Set<RemoteChannel> systemChannels = channels.get(systemId);
            for (RemoteChannel channel : systemChannels)
            {
                if (channel instanceof TransportChannel)
                {
                    TransportChannel transportChannel = (TransportChannel)channel;
                    channelStrings.add(String.format("%d:%s:%s:%s", systemId, transportChannel.getTransportLayerName(), 
                        transportChannel.getLocalMessageAddress(), transportChannel.getRemoteMessageAddress()));
                }
            }
        }
  
        //Verify persisted channels exist
        assertThat(channelStrings, hasItem("500:test:local:remote"));
        assertThat(channelStrings, hasItem("500:test:local3:remote3"));
        assertThat(channelStrings, hasItem("600:test2:local-A:remote-B"));
        assertThat(channelStrings, hasItem("600:test:local2:remote2"));
        
        //Verify channels that shouldn't have been persisted do not exist
        assertThat(channelStrings, not(hasItems("700:test3:local-B:remote-C")));
    }
}
