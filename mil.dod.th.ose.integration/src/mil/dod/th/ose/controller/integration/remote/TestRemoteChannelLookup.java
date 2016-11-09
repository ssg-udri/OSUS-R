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

import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;

/**
 * @author Dave Humeniuk
 *
 */
public class TestRemoteChannelLookup
{
    /**
     * Verify a channel can be created/synced and then retrieved.
     * 
     * Verify syncing to new system id will remove reference to old one.
     * 
     * TODO: TH-644 - update once a sync client socket method can be called even if endpoint is there
     */
    @Test
    public void testSyncing()
    {
        RemoteChannelLookup remoteChannelLookup = IntegrationTestRunner.getService(RemoteChannelLookup.class);
        
        remoteChannelLookup.syncTransportChannel("test", "local", "remote", 100);
        
        assertThat(remoteChannelLookup.getChannels(100).size(), is(1));
        
        // sync to new id
        remoteChannelLookup.syncTransportChannel("test", "local", "remote", 200);
        
        assertThat(remoteChannelLookup.getChannels(100).size(), is(0));
        assertThat(remoteChannelLookup.getChannels(200).size(), is(1)); 
    }
}
