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
package mil.dod.th.ose.remote;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

/**
 * Test class for the remote settings implementation. Verifies properties default to the correct values
 * and can be modified and retrieved as expected.
 * 
 * @author cashioka
 */
public class TestRemoteSettingsImpl
{
    private RemoteSettingsImpl m_SUT;
    private Map<String, Object> m_Props;
    private BundleContext m_Context;

    @Before
    public void setUp() throws IOException
    {
        //mock object
        m_Context = mock(BundleContext.class);
        
        //system under test
        m_SUT = new RemoteSettingsImpl();
        
        //empty property map, simulates that there is not previous configuration values for 
        //the component's configuration
        m_Props = new HashMap<String, Object>();
    }

    /**
     * Test activation will read property from framework and translate it to the appropriate encryption type. 
     */
    @Test
    public void testActivateWithProp()
    {
        //mock prop from framework
        when(m_Context.getProperty(RemoteSettingsImpl.ENCRYPTION_FRAMEWORK_PROPERTY)).
            thenReturn(EncryptionMode.AES_ECDH_ECDSA.toString());
        m_Props = new HashMap<String, Object>();
        m_SUT.activate(m_Context, m_Props);
        
        //verify
        assertThat(m_SUT.getEncryptionMode(), is(EncryptionMode.AES_ECDH_ECDSA));
    }
    
    /**
     * Test activation if there is no property from the framework. 
     */
    @Test
    public void testActivateWithOutProp()
    {
        //mock prop from framework
        when(m_Context.getProperty(RemoteSettingsImpl.ENCRYPTION_FRAMEWORK_PROPERTY)).
            thenReturn(null);
        m_SUT.activate(m_Context, m_Props);
        
        //verify
        assertThat(m_SUT.getEncryptionMode(), is(EncryptionMode.NONE));
    }
    
    /**
     * Test activation if there is a previous config property. 
     */
    @Test
    public void testActivateWithPreviousProp()
    {
        m_Props = new HashMap<String, Object>();
        m_Props.put(RemoteSettings.KEY_ENCRYPTION_MODE, EncryptionMode.AES_ECDH_ECDSA);
        m_Props.put(RemoteSettings.KEY_PREVENT_SLEEP_MODE, true);
        m_SUT.activate(m_Context, m_Props);
        
        //verify
        assertThat(m_SUT.getEncryptionMode(), is(EncryptionMode.AES_ECDH_ECDSA));
        assertThat(m_SUT.isPreventSleepModeEnabled(), is(true));
    }
    
    /**
     * Test retrieval of the default log remote messages value
     */
    @Test
    public void testIsLogRemoteMessagingEnabledDefault()
    {
        //default is false
        assertThat(m_SUT.isLogRemoteMessagesEnabled(), is(false));
    }
    
    /**
     * Test retrieval of the default prevent sleep mode value
     */
    @Test
    public void testIsPreventSleepModeEnabledDefault()
    {
        //default is false
        assertThat(m_SUT.isPreventSleepModeEnabled(), is(false));
    }
    
    /**
     * Test retrieval of the default encryption mode value
     */
    @Test
    public void testGetEncryptionMode()
    {
        //default is none
        m_SUT.activate(m_Context, m_Props);
        assertThat(m_SUT.getEncryptionMode(), is(EncryptionMode.NONE));
    }
    
    /**
     * Test retrieval of the default max message size is correct.
     */
    @Test
    public void testGetMaxMsgSize()
    {
        m_SUT.activate(m_Context, m_Props);
        assertThat(String.valueOf(m_SUT.getMaxMessageSize()), is(RemoteSettingsConfig.DFLT_MAX_MSG_SIZE));
    }
    
    /**
     * Test the properties are correctly set when modified
     */
    @Test
    public void testModified()
    {
        m_SUT.activate(m_Context, m_Props);
        m_Props.put("logRemoteMessages", true);
        m_Props.put("encryptionMode", "AES_ECDH_ECDSA");
        m_Props.put("maxMsgSizeInBytes", 1L);
        m_SUT.modified(m_Props);
        
        assertThat(m_SUT.isLogRemoteMessagesEnabled(), is(true));
        assertThat(m_SUT.getEncryptionMode(), is(EncryptionMode.AES_ECDH_ECDSA));
        assertThat(m_SUT.getMaxMessageSize(), is(1L));
    }
}
