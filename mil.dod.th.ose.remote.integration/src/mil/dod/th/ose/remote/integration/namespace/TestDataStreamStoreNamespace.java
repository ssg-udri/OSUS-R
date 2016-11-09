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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import mil.dod.th.core.remote.proto.SharedMessages;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.StreamProfile;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DateRange;
import mil.dod.th.ose.remote.integration.DataStreamServiceNamespaceUtils;
import mil.dod.th.ose.remote.integration.DataStreamStoreNamespaceUtils;
import mil.dod.th.ose.remote.integration.SocketHostHelper;

/**
 * Tests the interaction of the remote interface with the
 * {@link mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace}. The class
 * tests that DataStreamStoreMessageService messages are properly sent and appropriate responses are received.
 * 
 * @author jmiller
 *
 */
public class TestDataStreamStoreNamespace
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
     * Verify that an exception is thrown when trying to enable an archiving process for a non-existent
     * stream profile.
     */
    @Test
    public final void testEnableArchivingForNonExistentStreamProfile() throws IOException
    {
        
        UUID randUuid = UUID.randomUUID();
        SharedMessages.UUID randProtoUuid = SharedMessages.UUID.newBuilder()
                .setLeastSignificantBits(randUuid.getLeastSignificantBits())
                .setMostSignificantBits(randUuid.getMostSignificantBits()).build();
        DataStreamStoreNamespaceUtils.enableArchiving(socket, randProtoUuid, 5, true);
    }
    
    /**
     * Verify that an exception is thrown when trying to disable an archiving process for a non-existent
     * stream profile.
     */
    @Test
    public final void testDisableArchivingForNonExistentStreamProfile() throws IOException
    {        
        UUID randUuid = UUID.randomUUID();
        SharedMessages.UUID randProtoUuid = SharedMessages.UUID.newBuilder()
                .setLeastSignificantBits(randUuid.getLeastSignificantBits())
                .setMostSignificantBits(randUuid.getMostSignificantBits()).build();
        DataStreamStoreNamespaceUtils.disableArchiving(socket, randProtoUuid, true);
    }
    
    /**
     * Verify that an exception is thrown when trying to execute a client acknowledgement for a non-existent
     * stream profile.
     */
    @Test
    public final void testClientAckForNonExistentStreamProfile() throws IOException
    {
        UUID randUuid = UUID.randomUUID();
        SharedMessages.UUID randProtoUuid = SharedMessages.UUID.newBuilder()
                .setLeastSignificantBits(randUuid.getLeastSignificantBits())
                .setMostSignificantBits(randUuid.getMostSignificantBits()).build();
        DataStreamStoreNamespaceUtils.clientAck(socket, randProtoUuid, true);
    }
    
    /**
     * Verify that an exception is thrown when trying to retrieve archive periods for a non-existent
     * stream profile.
     */
    @Test
    public final void testGetArchivePeriodsForNonExistentStreamProfile() throws IOException
    {
        UUID randUuid = UUID.randomUUID();
        SharedMessages.UUID randProtoUuid = SharedMessages.UUID.newBuilder()
                .setLeastSignificantBits(randUuid.getLeastSignificantBits())
                .setMostSignificantBits(randUuid.getMostSignificantBits()).build();
        DataStreamStoreNamespaceUtils.getArchivePeriods(socket, randProtoUuid, true);
    }
    
    /**
     * Verify that an archiving process can be enabled for a stream profile
     */
    @Test
    public final void testEnableAndDisableArchiving() throws IOException
    {
        Set<StreamProfile> profiles = DataStreamServiceNamespaceUtils.getStreamProfiles(socket);
        
        //Choose the first profile
        StreamProfile profile = (StreamProfile)profiles.toArray()[0];
        
        //Set archiving process to begin after 3 seconds if no client acknowledgment is received.
        DataStreamStoreNamespaceUtils.enableArchiving(socket, profile.getInfo().getUuid(), 3, false);
        
        //Wait for 10 seconds to ensure that archiving process begins
        try
        {
            Thread.sleep(10000);
        }
        catch (InterruptedException e)
        {
            throw new IOException(e);
        }
        
        //Disable currently running archive process
        DataStreamStoreNamespaceUtils.disableArchiving(socket, profile.getInfo().getUuid(), false);      
    }
    
    /**
     * Verify that a client acknowledgment can be sent for a stream profile to prevent an enabled archiving
     * process from entering archive mode.
     */
    @Test
    public final void testClientAck() throws IOException
    {
        Set<StreamProfile> profiles = DataStreamServiceNamespaceUtils.getStreamProfiles(socket);
        
        //Choose the first profile
        StreamProfile profile = (StreamProfile)profiles.toArray()[0];
        
        //Set archiving process to begin after 5 seconds if no client acknowledgment is received.
        DataStreamStoreNamespaceUtils.enableArchiving(socket, profile.getInfo().getUuid(), 5, false);

        //Send clientAck messages every second for 20 seconds to ensure that archiving process doesn't begin
        for (int i=0; i<20; i++)
        {
            try
            {
                DataStreamStoreNamespaceUtils.clientAck(socket, profile.getInfo().getUuid(), false);
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                throw new IOException(e);
            }
        }
        
        //Disable process without any archiving having taken place.
        DataStreamStoreNamespaceUtils.disableArchiving(socket, profile.getInfo().getUuid(), false);       
    }
    
    @Test
    public final void testGetArchivePeriods() throws IOException
    {
        Set<StreamProfile> profiles = DataStreamServiceNamespaceUtils.getStreamProfiles(socket);
        
        //Choose the first profile
        StreamProfile profile = (StreamProfile)profiles.toArray()[0];
        
        List<DateRange> dateRanges = DataStreamStoreNamespaceUtils.getArchivePeriods(socket, 
                profile.getInfo().getUuid(), false);
        
        for (DateRange range : dateRanges)
        {
            assertThat(range.getStartTime(), is(lessThan(range.getStopTime())));
        }
    }
}
