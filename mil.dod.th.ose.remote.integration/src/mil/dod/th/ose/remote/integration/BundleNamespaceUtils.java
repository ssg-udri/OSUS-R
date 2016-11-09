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
package mil.dod.th.ose.remote.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.io.FileUtils;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.BundleMessages.BundleInfoType;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundlesResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.InstallRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.InstallResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.StartRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.StopRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.UninstallRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.shared.OSGiEventConstants;

/**
 * @author allenchl
 *
 */
public final class BundleNamespaceUtils
{
    /**
     * Hidden constructor.
     */
    private BundleNamespaceUtils()
    {
    }
    
    /**
     * The the id of the bundle with the given symbolic name.
     * @param symbolicName
     *     the symbolic name of the bundle
     * @param socket
     *     the socket connection to use
     * @return
     *     the bundle ID
     */
    public static long getBundleBySymbolicName(final String symbolicName, final Socket socket) 
        throws UnknownHostException, IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        // get bundles and retrieve id for the asset bundle installed
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder().setBundleSymbolicName(true).build();
        TerraHarvestMessage message = createBundleMessage(request, BundleMessageType.GetBundleInfoRequest);

        // send message
        message.writeDelimitedTo(socket.getOutputStream());

        long testBundleId = -1L;

        // read in response
        Message messageRcvd = 
             listener.waitForMessage(Namespace.Bundle, BundleMessageType.GetBundleInfoResponse, 1000);

        BundleNamespace namespaceResponse = (BundleNamespace)messageRcvd;
        GetBundleInfoResponseData bundleResponse 
            = GetBundleInfoResponseData.parseFrom(namespaceResponse.getData());

        // find the test bundle
        for (BundleInfoType bundle : bundleResponse.getInfoDataList())
        {
            if (bundle.getBundleSymbolicName().equals(symbolicName))
            {
                testBundleId = bundle.getBundleId();
            }
        }
        assertThat("Bundle was not found", testBundleId, greaterThan(-1L));
        return testBundleId;
    }
    
    /**
     * Get the current state of the bundle, using constants such as {@link org.osgi.framework.Bundle#ACTIVE}.
     */
    public static int getBundleState(final long bundleId, final Socket socket) 
        throws UnknownHostException, IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        // get bundles and retrieve id for the asset bundle installed
        GetBundleInfoRequestData request = GetBundleInfoRequestData.newBuilder()
                .setBundleId(bundleId)
                .setBundleState(true).build();
        TerraHarvestMessage message = createBundleMessage(request, BundleMessageType.GetBundleInfoRequest);

        // send message
        message.writeDelimitedTo(socket.getOutputStream());

        // read in response
        Message messageRcvd = 
             listener.waitForMessage(Namespace.Bundle, BundleMessageType.GetBundleInfoResponse, 1000);

        BundleNamespace namespaceResponse = (BundleNamespace)messageRcvd;
        GetBundleInfoResponseData bundleResponse = GetBundleInfoResponseData.parseFrom(namespaceResponse.getData());

        assertThat(String.format("Bundle [%d] is not installed", bundleId), bundleResponse.getInfoDataCount(), is(1));
        return bundleResponse.getInfoDataList().get(0).getBundleState();
    }

    /**
     * Install the bundle at the given file.
     * @param jarFile
     *     the jar file to install
     * @param bundleLocation
     *      the bundle location 
     * @param socket
     *     the socket connection to use
     * @return
     *     the bundleID of the installed bundle
     */
    public static long installBundle(final File jarFile, final String bundleLocation, final boolean startAtInstall, 
            final Socket socket) throws IOException, InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        byte[] buf = FileUtils.readFileToByteArray(jarFile);
        
        //construct request
        InstallRequestData request = InstallRequestData.newBuilder().
            setBundleLocation(bundleLocation).
            setBundleFile(ByteString.copyFrom(buf)).
            setStartAtInstall(startAtInstall).
            build();
        TerraHarvestMessage message = createBundleMessage(request, BundleMessageType.InstallRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());

        // read in response
        Message messageRcvd = 
             listener.waitForMessage(Namespace.Bundle, BundleMessageType.InstallResponse, 1000);

        BundleNamespace namespaceResponse = (BundleNamespace)messageRcvd;
        InstallResponseData responseData = InstallResponseData.parseFrom(namespaceResponse.getData());
        return responseData.getBundleId();
    }

    /**
     * Uninstall the bundle with the given bundle ID.
     * @param bundleId
     *     the ID of the bundle to uninstall
     * @param socket
     *     the socket connection to use
     */
    public static void uninstallBundle(final long bundleId, final Socket socket) throws IOException, 
        InterruptedException
    {
        MessageListener listener = new MessageListener(socket);
        
        //construct request
        UninstallRequestData request = UninstallRequestData.newBuilder().
            setBundleId(bundleId).build();
        TerraHarvestMessage message = createBundleMessage(request, BundleMessageType.UninstallRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());
        
        // read in response
        listener.waitForMessage(Namespace.Bundle, BundleMessageType.UninstallResponse, 1000);
    }
    
    /**
     * Start a bundle with the given bundle ID.
     * @param bundleId
     *      start the bundle with the given ID
     * @param socket
     *      the socket to use for communication to the controller
     */
    public static void startBundle(final long bundleId, final Socket socket) throws IOException, InterruptedException
    {
        final MessageListener listener = new MessageListener(socket);
                
        //register for the bundle started event
        final int regId = 
                RemoteEventRegistration.regRemoteEventMessages(socket, OSGiEventConstants.TOPIC_BUNDLE_STARTED);
        
        try
        {
            StartRequestData request = StartRequestData.newBuilder().setBundleId(bundleId).build();
            TerraHarvestMessage message = createBundleMessage(request, BundleMessageType.StartRequest);

            // send message
            message.writeDelimitedTo(socket.getOutputStream());

            listener.waitForMessages(1800, new MessageMatchers.BasicMessageMatcher(Namespace.Bundle,
                    BundleMessageType.StartResponse), new MessageMatchers.EventMessageMatcher(
                            OSGiEventConstants.TOPIC_BUNDLE_STARTED));
        }
        finally
        {
            MessageListener.unregisterEvent(regId, socket);
        }
    }
    
    /**
     * Stop a bundle with the given bundle ID.
     * @param bundleId
     *      start the bundle with the given ID
     * @param socket
     *      the socket to use for communication to the controller
     */
    public static void stopBundle(final long bundleId, final Socket socket) throws IOException, InterruptedException
    {
        final MessageListener listener = new MessageListener(socket);
        
        //register for the bundle started event
        final int regId = 
                RemoteEventRegistration.regRemoteEventMessages(socket, OSGiEventConstants.TOPIC_BUNDLE_STOPPED);
        
        try
        {
            StopRequestData request = StopRequestData.newBuilder().setBundleId(bundleId).build();
            TerraHarvestMessage message = createBundleMessage(request, BundleMessageType.StopRequest);

            // send message
            message.writeDelimitedTo(socket.getOutputStream());

            listener.waitForMessages(1200, new MessageMatchers.BasicMessageMatcher(Namespace.Bundle,
                    BundleMessageType.StopResponse), new MessageMatchers.EventMessageMatcher(
                            OSGiEventConstants.TOPIC_BUNDLE_STOPPED));
        }
        finally
        {
            MessageListener.unregisterEvent(regId, socket);
        }
    }
    
    /**
     * Get data for all bundles.
     */
    public static GetBundlesResponseData getBundles(final Socket socket) throws IOException
    {
        final MessageListener listener = new MessageListener(socket);
        
        TerraHarvestMessage message = 
                BundleNamespaceUtils.createBundleMessage(null, BundleMessageType.GetBundlesRequest);

        //send message
        message.writeDelimitedTo(socket.getOutputStream());
        
        BundleNamespace namespaceResponse = 
                (BundleNamespace)listener.waitForMessage(Namespace.Bundle, BundleMessageType.GetBundlesResponse, 1000);

        return GetBundlesResponseData.parseFrom(namespaceResponse.getData());
    }
    
    /**
     * Construct a TerraHarvestMessage wrapping a bundle message.
     * @param bundleMessage
     *     bundle message
     * @param type
     *     the type of the message
     */
    public static TerraHarvestMessage createBundleMessage(final Message bundleMessage, final BundleMessageType type)
    {
        BundleNamespace.Builder namespaceBuilder = BundleNamespace.newBuilder().setType(type);
        if (type == BundleMessageType.GetBundlesRequest)
        {  
        }
        else
        {
            namespaceBuilder.setData(bundleMessage.toByteString());
        }

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.Bundle,
            namespaceBuilder);
            
        return thMessage;
    }
}
