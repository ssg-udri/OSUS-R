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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.model.config.SocketChannelConfig;
import mil.dod.th.model.config.TransportChannelConfig;
import mil.dod.th.ose.test.LoggingServiceMocker;

/**
 * Test class for the {@link RemoteChannelLoaderImpl} class.
 * 
 * @author cweisenborn
 */
public class TestRemoteChannelLoaderImpl
{
    private RemoteChannelLoaderImpl m_SUT;
    private RemoteChannelLookup m_RemoteChannelLookup;
    private LoggingService m_LoggingService;
    
    private int m_SystemId1 = 75;
    private int m_SystemId2 = 150;
    private int m_SystemId3 = 225;
    
    @Before
    public void setup()
    {
        m_RemoteChannelLookup = mock(RemoteChannelLookup.class);
        m_LoggingService = LoggingServiceMocker.createMock();
        
        m_SUT = new RemoteChannelLoaderImpl();
        
        m_SUT.setLoggingService(m_LoggingService);
        m_SUT.setRemoteChannelLookup(m_RemoteChannelLookup);
    }
    
    /**
     * Verify that process socket channel configs method calls the remote channel lookup to sync a socket channel for
     * each configuration.
     */
    @Test
    public void testProcessSocketChannelConfigs()
    {
        SocketChannelConfig config1 = new SocketChannelConfig("host1", 1000, m_SystemId1);
        SocketChannelConfig config2 = new SocketChannelConfig("host2", 2000, m_SystemId2);
        SocketChannelConfig config3 = new SocketChannelConfig();
        config3.setHost("host3");
        config3.setSystemId(m_SystemId3);
        
        List<SocketChannelConfig> configsList = new ArrayList<>();
        configsList.add(config1);
        configsList.add(config2);
        configsList.add(config3);
        
        m_SUT.processSocketChannels(configsList);
        
        verify(m_RemoteChannelLookup).syncClientSocketChannel("host1", 1000, m_SystemId1, false);
        verify(m_RemoteChannelLookup).syncClientSocketChannel("host2", 2000, m_SystemId2, false);
        verify(m_RemoteChannelLookup).syncClientSocketChannel("host3", 4000, m_SystemId3, false);
    }
    
    /**
     * Verify that process transport channel configs method calls the remote channel lookup to sync a transport channel 
     * for each configuration.
     */
    @Test
    public void testProcessTransportChannelConfigs()
    {
        TransportChannelConfig config1 = new TransportChannelConfig("t1-1", "t1:1.2.3", "t1:1.2.4", m_SystemId1);
        TransportChannelConfig config2 = new TransportChannelConfig("t2-1", "t2:1.2.5", "t2:1.2.6", m_SystemId2);
        TransportChannelConfig config3 = new TransportChannelConfig("t3-1", "t3:1.2.7", "t3:1.2.8", m_SystemId3);
    
        List<TransportChannelConfig> configsList = new ArrayList<>();
        configsList.add(config1);
        configsList.add(config2);
        configsList.add(config3);
        
        m_SUT.processTransportChannels(configsList);
        
        verify(m_RemoteChannelLookup).syncTransportChannel("t1-1", "t1:1.2.3", "t1:1.2.4", m_SystemId1, false);
        verify(m_RemoteChannelLookup).syncTransportChannel("t2-1", "t2:1.2.5", "t2:1.2.6", m_SystemId2, false);
        verify(m_RemoteChannelLookup).syncTransportChannel("t3-1", "t3:1.2.7", "t3:1.2.8", m_SystemId3, false);
    }
}
