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
package mil.dod.th.ose.datastream.store.integration;

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
import mil.dod.th.core.datastream.store.DataStreamStore;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.junit.Test;
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
public class TestDataStreamStore extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext(); 

    private String m_DATASTREAM_STORE_PID = "mil.dod.th.ose.datastream.store.DataStreamStore";
    private String m_DATASTREAM_SERVICE_PID = "mil.dod.th.ose.datastream.DataStreamService";
    private String m_CONFIG_PROP_FILESTORE_TOP_DIR = "filestore.top.dir";
    private String m_CONFIG_PROP_MULTICAST_HOST = "multicast.host";
    private String m_CONFIG_PROP_START_PORT = "start.port";

    @Override
    public void setUp() throws IOException
    {
        ConfigurationAdmin configAdmin = ServiceUtils.getService(m_Context, ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        final Configuration dataStreamServiceConfig = configAdmin.getConfiguration(m_DATASTREAM_SERVICE_PID, null);
        final Configuration dataStreamStoreConfig = configAdmin.getConfiguration(m_DATASTREAM_STORE_PID, null);        
        
        final Dictionary<String, Object> dataStreamServiceProps = new Hashtable<>();
        dataStreamServiceProps.put(m_CONFIG_PROP_MULTICAST_HOST, "239.1.2.3");
        dataStreamServiceProps.put(m_CONFIG_PROP_START_PORT, 10000);
        dataStreamServiceConfig.update(dataStreamServiceProps);
        
        final Dictionary<String, Object> dataStreamStoreProps = new Hashtable<>();        
        dataStreamStoreProps.put(m_CONFIG_PROP_FILESTORE_TOP_DIR, "./generated/filestore_top_dir");        
        dataStreamStoreConfig.update(dataStreamStoreProps);
        
        DataStreamService dataStreamService = ServiceUtils.getService(m_Context, DataStreamService.class, 5000);
        assertThat(dataStreamService, is(notNullValue()));
        
        DataStreamStore dataStreamStore = ServiceUtils.getService(m_Context, DataStreamStore.class, 5000);
        assertThat(dataStreamStore, is(notNullValue()));
        deleteAllStreamProfiles(m_Context);
        
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleStreamProfile.class, 5000);
    }
    
    @Override
    public void tearDown() throws Exception
    {
        deleteAllStreamProfiles(m_Context);
    }
        
    @Test
    public void testEnableAndDisableArchiving() throws IllegalArgumentException, StreamProfileException, 
        InterruptedException
    {
        DataStreamService dataStreamService = ServiceUtils.getService(m_Context, DataStreamService.class);
        assertThat(dataStreamService, is(notNullValue()));
        
        Map<String, Object> props = new HashMap<>();
        props.put(StreamProfileAttributes.CONFIG_PROP_ASSET_NAME, "asset1");
        props.put(StreamProfileAttributes.CONFIG_PROP_BITRATE_KBPS, "10.0");
        props.put(StreamProfileAttributes.CONFIG_PROP_FORMAT, "video/mpeg");
        props.put(StreamProfileAttributes.CONFIG_PROP_DATA_SOURCE, "rtsp://hostname:9999");

        StreamProfile profile = dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(), 
                "profile1", props);
        
        DataStreamStore dataStreamStore = ServiceUtils.getService(m_Context, DataStreamStore.class, 5000);
        assertThat(dataStreamStore, is(notNullValue()));
        
        //Begin archiving immediately
        dataStreamStore.enableArchiving(profile, true, 0, 0);
        
        //Wait 5 seconds
        Thread.sleep(5000);
        
        dataStreamStore.disableArchiving(profile);       
    }
    
    @Test
    public void testClientAck() throws IllegalArgumentException, StreamProfileException, InterruptedException
    {
        DataStreamService dataStreamService = ServiceUtils.getService(m_Context, DataStreamService.class);
        assertThat(dataStreamService, is(notNullValue()));
        
        Map<String, Object> props = new HashMap<>();
        props.put(StreamProfileAttributes.CONFIG_PROP_ASSET_NAME, "asset1");
        props.put(StreamProfileAttributes.CONFIG_PROP_BITRATE_KBPS, "10.0");
        props.put(StreamProfileAttributes.CONFIG_PROP_FORMAT, "video/mpeg");
        props.put(StreamProfileAttributes.CONFIG_PROP_DATA_SOURCE, "rtsp://hostname:9999");

        StreamProfile profile = dataStreamService.createStreamProfile(ExampleStreamProfile.class.getName(), 
                "profile1", props);
        
        DataStreamStore dataStreamStore = ServiceUtils.getService(m_Context, DataStreamStore.class, 5000);
        assertThat(dataStreamStore, is(notNullValue()));
        
        //Set archiving to begin if client ack hasn't been received in 5 seconds
        dataStreamStore.enableArchiving(profile, true, 5, 0);
        
        //Wait for 2 seconds, send client ack, repeat several times
        for (int i=0; i<5; i++)
        {
            Thread.sleep(2000);
            dataStreamStore.clientAck(profile);
        }
        
        //Disable archiving before any streaming data was saved to disk
        dataStreamStore.disableArchiving(profile);        
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
