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
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;
import mil.dod.th.ose.controller.integration.api.EventHandlerSyncer;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * @author cweisenborn
 *
 */
public class TestConfigurationAdminEventBridge
{
    private ConfigurationAdmin m_ConfigAdmin;
    private EventHandlerSyncer m_ConfigEventHandler;
    private Configuration m_Config;
    
    @Before
    public void setup()
    {
        m_ConfigAdmin = IntegrationTestRunner.getService(ConfigurationAdmin.class);
        assertThat(m_ConfigAdmin, is(notNullValue()));       
    }
    
    @After
    public void teardown()
    {
        if (m_Config != null)
        {
            try
            {
                m_Config.delete();
            }
            catch (IOException | IllegalStateException e)
            {
                //illegal state will be thrown if the object was already successfully deleted
            }
        }
    }
    
    /**
     * Test the configuration event admin bridge.
     * Verify that configuration events are posted to the event admin service.
     */
    @Test
    public void testConfigurationBridge() throws IOException, InvalidSyntaxException, InterruptedException
    {
        m_Config = m_ConfigAdmin.getConfiguration("some.pid.test.configuration.bridge");

        Dictionary<String, Object> props =  new Hashtable<>();
        
        String value = "test.value";
        String key = "test.key";
        props.put(key, value);

        m_ConfigEventHandler = new EventHandlerSyncer(ConfigurationEventConstants.TOPIC_CONFIGURATION_UPDATED_EVENT, 
                "(" + ConfigurationEventConstants.EVENT_PROP_PID + "=" + m_Config.getPid() + ")");
        
        //Update configuration
        m_Config.update(props);
        
        //Verify that a configuration updated event is posted for the specified configuration.
        m_ConfigEventHandler.waitForEvent(5, 1, 0);
        
        m_ConfigEventHandler = new EventHandlerSyncer(ConfigurationEventConstants.TOPIC_CONFIGURATION_DELETED_EVENT, 
                "(" + ConfigurationEventConstants.EVENT_PROP_PID + "=" + m_Config.getPid() + ")");
        
        //Delete configuration.
        m_Config.delete();
        
        //Verify that a configuration deleted event is posted for the specified configuration.
        m_ConfigEventHandler.waitForEvent(5, 1, 0);
    }
}
