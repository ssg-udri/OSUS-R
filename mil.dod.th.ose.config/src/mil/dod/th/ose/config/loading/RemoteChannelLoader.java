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
package mil.dod.th.ose.config.loading;

import java.util.List;

import mil.dod.th.model.config.SocketChannelConfig;
import mil.dod.th.model.config.TransportChannelConfig;

/**
 * OSGi service which loads {@link mil.dod.th.core.remote.RemoteChannel}s based on configurations. Remote channels
 * added through this service are not persisted.
 * 
 * @author cweisenborn
 */
public interface RemoteChannelLoader
{
    /**
     * Load the given {@link mil.dod.th.core.remote.SocketChannel} configurations.
     * 
     * @param socketChannelConfigs
     *      Socket channel configurations to load.
     */
    void processSocketChannels(List<SocketChannelConfig> socketChannelConfigs);
    
    /**
     * Load the given {@link mil.dod.th.core.remote.TransportChannel} configurations.
     * 
     * @param transportChannelConfigs
     *      Transport channel configurations to load.
     */
    void processTransportChannels(List<TransportChannelConfig> transportChannelConfigs);
}
