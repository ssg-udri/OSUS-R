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
package mil.dod.th.ose.shell;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteSystemEncryption;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestRemoteServiceCommands
{
    private RemoteServiceCommands m_SUT;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new RemoteServiceCommands();

        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
    }
    
    /**
     * Verify that the remote channel lookup service command will retrieve the correct service.
     */
    @Test
    public void testRemoteChannelLookup()
    {
        RemoteChannelLookup remoteChannelLookup = mock(RemoteChannelLookup.class);
        
        m_SUT.setRemoteChannelLookup(remoteChannelLookup);
        
        assertThat("Command 'rmtChnLkp' returns the RemoteChannelLookup service", m_SUT.rmtChnLkp(), 
            is(remoteChannelLookup));
    }
    
    /**
     * Verify that the data converter directory service command will retrieve the correct service.
     */
    @Test
    public void testDataConverterDirectory()
    {
        JaxbProtoObjectConverter jaxbProtoObjectConverter = mock(JaxbProtoObjectConverter.class);
        
        m_SUT.setJaxbProtoObjectConverter(jaxbProtoObjectConverter);
        
        assertThat("Command 'jxbPrtObjCnvrtr' returns the JaxbProtoObjectConverter", m_SUT.jxbPrtObjCnvrtr(), 
            is(jaxbProtoObjectConverter));
    }
    
    /**
     * Verify that the message factory command will retrieve the correct service.
     */
    @Test
    public void testMessageFactory()
    {
        MessageFactory messageFactory = mock(MessageFactory.class);
        
        m_SUT.setMessageFactory(messageFactory);
        
        assertThat("Command 'msgFty' returns the MessageFactory service", m_SUT.msgFty(), is(messageFactory));
    }
    
    /**
     * Verify that the remote system encryption service command will retrieve the correct service or return null 
     * if the service is not available.
     */
    @Test
    public void testRemoteSystemEncryption()
    {
        // service is set to null as this would be true if the component was activated and the optional 
        //service was not available
        assertThat("Command 'rmtSysEnct' returns the null because the service is not set", m_SUT.rmtSysEnct(), 
                is(nullValue()));
        
        //now mock the service
        RemoteSystemEncryption remoteSystemEncryption = mock(RemoteSystemEncryption.class);
        
        m_SUT.setRemoteSystemEncryption(remoteSystemEncryption);
        
        assertThat("Command 'rmtSysEnct' returns the null because the service is not set", m_SUT.rmtSysEnct(), 
                is(remoteSystemEncryption));
    }   
}
