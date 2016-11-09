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
package mil.dod.th.ose.config.loading.impl;

import java.util.List;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.model.config.SocketChannelConfig;
import mil.dod.th.model.config.TransportChannelConfig;
import mil.dod.th.ose.config.loading.RemoteChannelLoader;

/**
 * This service reads in the remote channels denoted in the configs.xml and registers them with the 
 * {@link RemoteChannelLookup} service. Channels added to the remote channel lookup through this service are not 
 * persisted.
 * 
 * @author cweisenborn
 */
@Component
public class RemoteChannelLoaderImpl implements RemoteChannelLoader
{
    private RemoteChannelLookup m_RemoteChannelLookup;
    private LoggingService m_LoggingService;
    
    @Reference
    public void setRemoteChannelLookup(final RemoteChannelLookup remoteChannelLookup)
    {
        m_RemoteChannelLookup = remoteChannelLookup;
    }
    
    @Reference
    public void setLoggingService(final LoggingService loggingService)
    {
        m_LoggingService = loggingService;
    }

    @Override
    public void processSocketChannels(final List<SocketChannelConfig> socketChannelConfigs)
    {
        for (SocketChannelConfig config: socketChannelConfigs)
        {            
            m_LoggingService.debug("Loading socket channel for system id: 0x%08x, hostname: %s, and port: %d", 
                    config.getSystemId(), config.getHost(), config.getPort());
            try
            {
                m_RemoteChannelLookup.syncClientSocketChannel(config.getHost(), config.getPort(), config.getSystemId(), 
                        false);
            }
            catch (final Exception ex)
            {
                m_LoggingService.error(ex, "Invalid socket channel configuration: %s", config);
            }
        }
    }

    @Override
    public void processTransportChannels(final List<TransportChannelConfig> transportChannelConfigs)
    {
        for (TransportChannelConfig config: transportChannelConfigs)
        {
            m_LoggingService.debug("Loading transport channel for system id: 0x%08x, transport layer name: %s, "
                    + "local address: %s, and remote address: %s", config.getSystemId(), 
                    config.getTransportLayerName(), config.getLocalAddress(), config.getRemoteAddress());
            try
            {
                m_RemoteChannelLookup.syncTransportChannel(config.getTransportLayerName(), config.getLocalAddress(), 
                    config.getRemoteAddress(), config.getSystemId(), false);
            }
            catch (final Exception ex)
            {
                m_LoggingService.error(ex, "Invalid transport channel configuration: %s", config);
            }
        }
    }
}
