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
package edu.udayton.udri.smartphone;

import static org.mockito.Mockito.*;

import mil.dod.th.core.factory.FactoryException;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;

public class TestServerActivator
{
    @InjectMocks private ServerActivator m_SUT;
    @Mock private BundleContext m_Context;
    @Mock private HttpServer m_HttpServer;
    @Mock private SmartphoneSensorSocketServer m_SocketServer;
    
    @Before
    public void setUp() throws FactoryException
    {
        m_SUT = new ServerActivator();
        
        MockitoAnnotations.initMocks(this);
    }
    
    @Test
    public void testStart() throws Exception
    {
        m_SUT.start(m_Context);
    }
    
    @Test
    public void testStop() throws Exception
    {
        m_SUT.stop(m_Context);
        
        verify(m_HttpServer).shutdownNow();
        verify(m_SocketServer).stop();
    }
}
