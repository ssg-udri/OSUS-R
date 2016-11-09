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
package mil.dod.th.ose.gui.remotesystemencryption;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;

import org.junit.Before;
import org.junit.Test;


/**
 * Test class for the {@link RemoteSystemEncryptionImpl}.
 * @author nick
 *
 */
public class TestRemoteSystemEncryptionImpl
{
    private RemoteSystemEncryptionImpl m_SUT;
    private RemoteChannelLookup m_ChannelLookup;
    
    @Before
    public void setup()
    {
        m_SUT = new RemoteSystemEncryptionImpl();
        
        m_ChannelLookup = mock(RemoteChannelLookup.class);
        
        m_SUT.setRemoteChannelLookup(m_ChannelLookup);
    }
    
    /**
     * Verify that the correct encryption types can be retrieved and set for 
     * a given system id.
     */
    @Test
    public void testGetAddEncryptType()
    {
        EncryptType nullType = m_SUT.getEncryptType(123);
        
        assertThat(nullType, nullValue());
        
        //add an encrypt type for 123 system 
        m_SUT.addEncryptionTypeForSystem(123, EncryptType.AES_ECDH_ECDSA);
        
        //verify correct type can be retrieved 
        assertThat(m_SUT.getEncryptType(123), is(EncryptType.AES_ECDH_ECDSA));
    }
    
    /**
     * Verify that if a system encryption entry contains a system id 
     * that is no longer known by the remote channel lookup then it is 
     * removed from the list of known system encryption types.
     */
    @Test
    public void testCleanupSystemEncryptionTypes()
    {
        //add encryption type
        m_SUT.addEncryptionTypeForSystem(123, EncryptType.AES_ECDH_ECDSA);
        
        assertThat(m_SUT.getEncryptType(123), is(EncryptType.AES_ECDH_ECDSA));
        
        when(m_ChannelLookup.getChannels(123)).thenReturn(new ArrayList<RemoteChannel>());
        
        m_SUT.cleanupSystemEncryptionTypes();
        
        assertThat(m_SUT.getEncryptType(123), nullValue());
    }
    
    /**
     * Verify that if a system is still known by the remote encryption service then 
     * it should not be removed from the list of encryption entries.
     */
    @Test
    public void testCleanupWithStillKnownSystem()
    {
        m_SUT.addEncryptionTypeForSystem(123, EncryptType.AES_ECDH_ECDSA);
        
        assertThat(m_SUT.getEncryptType(123), is(EncryptType.AES_ECDH_ECDSA));
        
        RemoteChannel channel = mock(RemoteChannel.class);
        List<RemoteChannel> list = new ArrayList<RemoteChannel>();
        list.add(channel);
        
        when(m_ChannelLookup.getChannels(123)).thenReturn(list);
        
        m_SUT.cleanupSystemEncryptionTypes();
        
        assertThat(m_SUT.getEncryptType(123), is(EncryptType.AES_ECDH_ECDSA));
    }
}

