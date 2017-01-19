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
package mil.dod.th.ose.datastream.integration;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileAttributes;
import mil.dod.th.core.datastream.StreamProfileException;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.integration.commons.EventHandlerSyncer;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import example.stream.profile.ExampleStreamProfile;
import junit.framework.TestCase;

/**
 * @author jmiller
 *
 */
public class TestDataStreamService extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext(); 
    
    private String m_PID = "mil.dod.th.ose.datastream.DataStreamService";
    private String m_CONFIG_PROP_MULTICAST_HOST = "multicast.host";
    private String m_CONFIG_PROP_START_PORT = "start.port";
    
    @Override
    public void setUp() throws IOException
    {
        ConfigurationAdmin configAdmin = ServiceUtils.getService(m_Context, ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        final Configuration config = configAdmin.getConfiguration(m_PID, null);

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        
        props.put(m_CONFIG_PROP_MULTICAST_HOST, "225.1.2.3");
        props.put(m_CONFIG_PROP_START_PORT, 5004);
        
        config.update(props);
        
        DataStreamService dataStreamService = ServiceUtils.getService(m_Context, DataStreamService.class, 5000);
        assertThat(dataStreamService, is(notNullValue()));
        deleteAllStreamProfiles(m_Context);
        
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleStreamProfile.class, 5000);        
    }
    
    @Override
    public void tearDown() throws Exception
    {
        deleteAllStreamProfiles(m_Context);
    }
    
    public final void testAdd() throws StreamProfileException
    {
        DataStreamService dataStreamService = ServiceUtils.getService(m_Context, DataStreamService.class);
        assertThat(dataStreamService, is(notNullValue()));
        
        Map<String, Object> props = new HashMap<>();
        props.put(StreamProfileAttributes.CONFIG_PROP_ASSET_NAME, "asset1");
        props.put(StreamProfileAttributes.CONFIG_PROP_BITRATE_KBPS, "10.0");
        props.put(StreamProfileAttributes.CONFIG_PROP_FORMAT, "video/mpeg");
        props.put(StreamProfileAttributes.CONFIG_PROP_DATA_SOURCE, "rtsp://hostname:9999");

        dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(), "profile1", props);
        
        Set<StreamProfile> profiles = dataStreamService.getStreamProfiles();
        assertThat(profiles.size(), is(1));
    }
    
    public final void testDelete() throws StreamProfileException
    {
        DataStreamService dataStreamService = ServiceUtils.getService(m_Context, DataStreamService.class);
        assertThat(dataStreamService, is(notNullValue()));
        
        Map<String, Object> props = new HashMap<>();
        props.put(StreamProfileAttributes.CONFIG_PROP_ASSET_NAME, "asset1");
        props.put(StreamProfileAttributes.CONFIG_PROP_BITRATE_KBPS, "10.0");
        props.put(StreamProfileAttributes.CONFIG_PROP_FORMAT, "video/mpeg");
        props.put(StreamProfileAttributes.CONFIG_PROP_DATA_SOURCE, "rtsp://hostname:9999");

        StreamProfile streamProfile = dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(), 
                "profile1", props);
        
        streamProfile.delete();
        
        //Verify that object has been removed
        assertThat(dataStreamService.getStreamProfiles().size(), is(0));
        
        //Added StreamProfile with same name
        dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(), "profile1", props);
        
        //Verify the new StreamProfile shows up in registry
        assertThat(dataStreamService.getStreamProfiles().size(), is(1));
    }
    
    /**
     * Test that a StreamProfile name is correctly set if the name is unique. If the name is not unique,
     * it should not be set.
     */
    public void testSetStreamProfileName() throws StreamProfileException, FactoryException
    {
        DataStreamService dataStreamService = ServiceUtils.getService(m_Context, DataStreamService.class);
        assertThat(dataStreamService, is(notNullValue()));
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context,
                FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED, String.format("(%s=%s)",
                        FactoryDescriptor.EVENT_PROP_OBJ_NAME, "testSetNameStreamProfile"));
        
        Map<String, Object> props = new HashMap<>();
        props.put(StreamProfileAttributes.CONFIG_PROP_ASSET_NAME, "asset1");
        props.put(StreamProfileAttributes.CONFIG_PROP_BITRATE_KBPS, "10.0");
        props.put(StreamProfileAttributes.CONFIG_PROP_FORMAT, "video/mpeg");
        props.put(StreamProfileAttributes.CONFIG_PROP_DATA_SOURCE, "rtsp://hostname:9999");

        StreamProfile streamProfile = dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(), 
                "testSetNameStreamProfile", props);
        streamProfile.setName("testSetNameStreamProfile");
        
        syncer.waitForEvent(10);
        
        StreamProfile streamProfile2 = dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(),
                "notTestSetNameStreamProfile", props);
        
        try
        {
            streamProfile2.setName("testSetNameStreamProfile");
            fail("expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            // do nothing; this exception is expected.
        }
        
        assertThat(streamProfile.getName(), is("testSetNameStreamProfile"));
        assertThat(streamProfile2.getName(), is(not("testSetNameStreamProfile")));               
    }
    
    public void testSetStreamProfileNameAtCreation() throws StreamProfileException
    {
        DataStreamService dataStreamService = ServiceUtils.getService(m_Context, DataStreamService.class);
        assertThat(dataStreamService, is(notNullValue()));
        
        Map<String, Object> props = new HashMap<>();
        props.put(StreamProfileAttributes.CONFIG_PROP_ASSET_NAME, "asset1");
        props.put(StreamProfileAttributes.CONFIG_PROP_BITRATE_KBPS, "10.0");
        props.put(StreamProfileAttributes.CONFIG_PROP_FORMAT, "video/mpeg");
        props.put(StreamProfileAttributes.CONFIG_PROP_DATA_SOURCE, "rtsp://hostname:9999");

        dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(), 
                "testSetNameStreamProfile", props);
        int size = dataStreamService.getStreamProfiles().size();
        
        try
        {
            dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(),
                    "testSetNameStreamProfile", props);
            fail("expecting exception");
        }
        catch (Exception e)
        {
            //expected exception
            assertThat(e.getCause().getMessage(),
                    containsString("Duplicate name: [testSetNameStreamProfile] is already in use"));
        }
        
        //verify no new StreamProfile was created
        assertThat(dataStreamService.getStreamProfiles().size(), is(size));
    }
    
    public void testStreamProfileEnableDisable() throws StreamProfileException
    {
        DataStreamService dataStreamService = ServiceUtils.getService(m_Context, DataStreamService.class);
        assertThat(dataStreamService, is(notNullValue()));
        
        Map<String, Object> props = new HashMap<>();
        props.put(StreamProfileAttributes.CONFIG_PROP_ASSET_NAME, "asset1");
        props.put(StreamProfileAttributes.CONFIG_PROP_BITRATE_KBPS, "10.0");
        props.put(StreamProfileAttributes.CONFIG_PROP_FORMAT, "video/mpeg");
        props.put(StreamProfileAttributes.CONFIG_PROP_DATA_SOURCE, "rtsp://hostname:9999");

        StreamProfile streamProfile = dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(),
                "profile1", props);
        
        EventHandlerSyncer syncer1 = new EventHandlerSyncer(m_Context,
                DataStreamService.TOPIC_STREAM_PROFILE_STATE_CHANGED, String.format("(%s=%s)",
                        DataStreamService.EVENT_PROP_STREAM_PROFILE_ENABLED, "true"));
        
        streamProfile.setEnabled(true);
        syncer1.waitForEvent(10);
        
        EventHandlerSyncer syncer2 = new EventHandlerSyncer(m_Context,
                DataStreamService.TOPIC_STREAM_PROFILE_STATE_CHANGED, String.format("(%s=%s)",
                        DataStreamService.EVENT_PROP_STREAM_PROFILE_ENABLED, "false"));
        
        streamProfile.setEnabled(false);
        syncer2.waitForEvent(10);
    }

    private void deleteAllStreamProfiles(BundleContext context)
    {
        DataStreamService dataStreamService = ServiceUtils.getService(context, DataStreamService.class);
        assertThat(dataStreamService, is(notNullValue()));
        
        //Delete all stream profile objects
        Set<StreamProfile> streamProfiles = dataStreamService.getStreamProfiles();

        for(StreamProfile profile : streamProfiles)
        {
            profile.delete();
        }
    }
}
