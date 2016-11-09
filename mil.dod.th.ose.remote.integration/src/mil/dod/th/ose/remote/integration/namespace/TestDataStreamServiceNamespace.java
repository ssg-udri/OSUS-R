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
package mil.dod.th.ose.remote.integration.namespace;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import example.stream.profile.ExampleStreamProfile;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.StreamProfile;
import mil.dod.th.ose.remote.integration.DataStreamServiceNamespaceUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.remote.lexicon.datastream.capability.StreamProfileCapabilitiesGen.StreamProfileCapabilities;
import mil.dod.th.remote.lexicon.datastream.types.StreamTypesGen.StreamFormat;

/**
 * Tests the interaction of the remote interface with the 
 * {@link mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace}. The class
 * tests that DataStreamMessageService messages are properly sent and appropriate responses are received.
 * 
 * @author jmiller
 *
 */
public class TestDataStreamServiceNamespace
{
    private Socket socket;
    
    @Before
    public void setUp() throws IOException
    {
        socket = SocketHostHelper.connectToController();
    }
    
    @After
    public void tearDown() throws IOException
    {
        socket.close();
    }
    
    /**
     * Verify that StreamProfile instances on the controller can be retrieved.
     */
    @Test
    public final void testGetStreamProfiles() throws IOException
    {
        Set<StreamProfile> profiles = DataStreamServiceNamespaceUtils.getStreamProfiles(socket);
        
        assertThat(profiles.size(), is(2));
        for (StreamProfile profile : profiles)
        {
            assertThat(profile.getInfo().getProductType(), is(ExampleStreamProfile.class.getName()));
        }
    }
    
    /**
     * Verifies ability of the system to get a stream profile's capabilities remotely.
     */
    @Test
    public final void testGetStreamProfileCapabilities() throws IOException
    {
        StreamProfileCapabilities capabilities = DataStreamServiceNamespaceUtils.
                getStreamProfileCapabilities(socket, ExampleStreamProfile.class.getName());
        
        assertThat(capabilities, is(notNullValue()));
        assertThat(capabilities.hasMaxBitrateKbps(), is(true));
        assertThat(capabilities.hasMinBitrateKbps(), is(true));
        
        List<String> knownFormats = new ArrayList<>();
        knownFormats.add("video/mp4");
        knownFormats.add("video/mpeg");
        
        List<StreamFormat> streamFormats = capabilities.getFormatList();
        for (StreamFormat format : streamFormats)
        {
            assertThat(knownFormats.contains(format.getMimeFormat()), is(true));
        }    
    }
    
    /**
     * Verify that all StreamProfile instances on the controller can be enabled.
     */
    @Test
    public final void testEnableStreamProfiles() throws IOException
    {
        Set<StreamProfile> profiles = DataStreamServiceNamespaceUtils.getStreamProfiles(socket);
        
        for (StreamProfile profile : profiles)
        {
            DataStreamServiceNamespaceUtils.enableStreamProfile(socket, profile.getInfo().getUuid());
        }
        
        //Retrieve profiles again, after they have all been enabled
        Set<StreamProfile> profilesAfter = DataStreamServiceNamespaceUtils.getStreamProfiles(socket);
        
        for (StreamProfile profile : profilesAfter)
        {
            assertThat(profile.getIsEnabled(), is(true));
        }      
    }
    
    /**
     * Verify that all StreamProfile instances on the controller can be disabled.
     */
    @Test
    public final void testDisableStreamProfiles() throws IOException
    {
        Set<StreamProfile> profiles = DataStreamServiceNamespaceUtils.getStreamProfiles(socket);
        
        for (StreamProfile profile : profiles)
        {
            DataStreamServiceNamespaceUtils.disableStreamProfile(socket, profile.getInfo().getUuid());
        }
        
        //Retrieve profiles again, after they have all been disabled
        Set<StreamProfile> profilesAfter = DataStreamServiceNamespaceUtils.getStreamProfiles(socket);
        
        for (StreamProfile profile : profilesAfter)
        {
            assertThat(profile.getIsEnabled(), is(false));
        }       
    }
}
