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
package mil.dod.th.ose.remote.osgi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BundleMessages.BundleErrorCode;
import mil.dod.th.core.remote.proto.BundleMessages.BundleInfoType;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespaceErrorData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundleInfoResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.GetBundlesResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.InstallRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.InstallResponseData;
import mil.dod.th.core.remote.proto.BundleMessages.StartRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.StopRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.UninstallRequestData;
import mil.dod.th.core.remote.proto.BundleMessages.UpdateRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * Message service for the {@link BundleNamespace}.
 * @author callen
 *
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) //NOCHECKSTYLE:Class Fan-Out Complexity is high  this is due
//to number of messages for the bundle namespace.it is required that all messages for the bundle namespace
//are handled by the service.
public class BundleManagementMessageService implements MessageService
{
    /**
     * Logging service use to log information.
     */
    private LoggingService m_Logging;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Reference to the event admin service, used to post events that a message was received.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Context for the bundle containing this component.
     */
    private BundleContext m_Context;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Bind to the service for creating remote messages.
     * 
     * @param messageFactory
     *      service that create messages
     */
    @Reference
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      service used to post events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Bind a message router to register.
     * 
     * @param messageRouter
     *      router that handles incoming messages
     */
    @Reference
    public void setMessageRouter(final MessageRouterInternal messageRouter)
    {
        m_MessageRouter = messageRouter;
    }
    
    /**
     * Activate this component, just save the context for later use and bind this service to the message router.
     * 
     * @param context
     *      context for this bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;
        
        m_MessageRouter.bindMessageService(this);
    }
    
    /**
     * Deactivate component by unbinding the service from the message router.
     */
    @Deactivate
    public void deactivate()
    {
        m_MessageRouter.unbindMessageService(this);
    }

    @Override
    public Namespace getNamespace()
    {
        return Namespace.Bundle;
    }

    @Override //NOCHECKSTYLE: Reached maximum cyclomatic complexity, need to cover all messages.
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload,
        final RemoteChannel channel) throws IOException
    {
        //parse bundle message
        final BundleNamespace bunMessage = BundleNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;
        
        switch (bunMessage.getType())
        {
            case StartRequest:
                dataMessage = startBundle(bunMessage, message, channel);
                break;
            case StartResponse:
                dataMessage = null;
                break;
            case StopRequest:
                dataMessage = stopBundle(bunMessage, message, channel);
                break;
            case StopResponse:
                dataMessage = null;
                break;
            case GetBundlesRequest:
                dataMessage = null;
                getBundles(message, channel);
                break;
            case GetBundlesResponse:
                dataMessage = GetBundlesResponseData.parseFrom(bunMessage.getData());
                break;
            case GetBundleInfoRequest:
                dataMessage = getBundleInfo(bunMessage, message, channel);
                break;
            case GetBundleInfoResponse:
                dataMessage = GetBundleInfoResponseData.parseFrom(bunMessage.getData());
                break;
            case InstallRequest:
                dataMessage = installBundle(bunMessage, message, channel);
                break;
            case InstallResponse:
                dataMessage = InstallResponseData.parseFrom(bunMessage.getData());
                break;
            case UpdateRequest:
                dataMessage = updateBundle(bunMessage, message, channel);
                break;
            case UpdateResponse:
                dataMessage = null;
                break;
            case UninstallRequest:
                dataMessage = uninstallBundle(bunMessage, message, channel);
                break;
            case UninstallResponse:
                dataMessage = null;
                break;
            case BundleNamespaceError:
                dataMessage = BundleNamespaceErrorData.parseFrom(bunMessage.getData());
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the BundleManagementMessageService namespace.", bunMessage.getType()));
        }

        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, bunMessage, 
                bunMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }

    /**
     * Handle the start bundle request.
     * @param message
     *    the message that contains the request to start a specified bundle
     * @param thMessage
     *    the terra harvest message that the request was contained in.
     * @param channel
     *    the channel that was used to send request
     * @return
     *    the parsed request message
     * @throws IOException
     *    thrown if the request message cannot be parsed
     */
    public Message startBundle(final BundleNamespace message, final TerraHarvestMessage thMessage,
        final RemoteChannel channel) throws IOException
    {
        //parse request
        final StartRequestData request = StartRequestData.parseFrom(message.getData());
        
        //get the specified bundle
        final Bundle bundle = m_Context.getBundle(request.getBundleId());
        if (bundle == null)
        {
            m_MessageFactory.createBaseErrorMessage(thMessage, ErrorCode.INVALID_VALUE, 
                String.format("The bundle %d requested to start does not exist!", request.getBundleId())).
                    queue(channel);
        }
        else
        {
            //try to start bundle
            try
            {
                bundle.start();
            }
            catch (final BundleException exception)
            {
                //send error
                queueBundleErrorMessage(thMessage, BundleErrorCode.OSGiBundleException, 
                    String.format("The bundle %d could not be started: %s", request.getBundleId(),
                            exception.getMessage()), channel);
                m_Logging.error(exception, "A request to start a bundle, with an id of %d failed",
                    request.getBundleId());
                return request;
            }
            //send response
            m_MessageFactory.createBundleResponseMessage(thMessage, BundleMessageType.StartResponse, null).
                queue(channel);
        }
        
        return request;
    }

    /**
     * Handle the stop bundle request.
     * @param message
     *    the message that contains the request to stop a specified bundle
     * @param thMessage
     *    the terra harvest message that the request was originally contained in
     * @param channel
     *    the channel that was used to send request
     * @return
     *    the parsed request message
     * @throws IOException
     *    thrown if the request message cannot be parsed
     */
    public Message stopBundle(final BundleNamespace message, final TerraHarvestMessage thMessage, 
        final RemoteChannel channel) throws IOException
    {
        //parse request
        final StopRequestData request = StopRequestData.parseFrom(message.getData());
        
        //get the specified bundle
        final Bundle bundle = m_Context.getBundle(request.getBundleId());
        if (bundle == null)
        {
            m_MessageFactory.createBaseErrorMessage(thMessage, ErrorCode.INVALID_VALUE, 
                String.format("The bundle %d requested to stop does not exist!", request.getBundleId())).queue(channel);
        }
        else
        {
            //try to stop bundle
            try
            {
                bundle.stop();
            }
            catch (final BundleException exception)
            {
                //send error
                queueBundleErrorMessage(thMessage, BundleErrorCode.OSGiBundleException, 
                    String.format("The bundle %d could not be stopped : %s", request.getBundleId(),
                        exception.getMessage()), channel);
                m_Logging.error(exception, "A request to stop a bundle, with an id of %d failed",
                        request.getBundleId());
                return request;
            }
            //send response
            m_MessageFactory.createBundleResponseMessage(thMessage, BundleMessageType.StopResponse, null).
                queue(channel);
        }
        
        return request;
    }

    /**
     * Handle the get bundles request.
     * @param thMessage
     *    the terra harvest message that the request was originally contained in
     * @param channel
     *    the channel that was used to send request
     */
    public void getBundles(final TerraHarvestMessage thMessage, final RemoteChannel channel)
    {
        final GetBundlesResponseData.Builder responseBuilder = GetBundlesResponseData.newBuilder();
        
        //List of bundles
        final Bundle[] bundles = m_Context.getBundles();
        //add bundles if any were returned
        for (Bundle bundle : bundles)
        {
            responseBuilder.addBundleId(bundle.getBundleId());
        }
        //send response
        m_MessageFactory.createBundleResponseMessage(thMessage, BundleMessageType.GetBundlesResponse,
            responseBuilder.build()).queue(channel);
    }

    /**
     * Handle the get bundle information request.
     * @param message
     *    the message that contains the request to get information on a specific bundle
     * @param thMessage
     *    the terra harvest message that the request was originally contained in
     * @param channel
     *    the channel that was used to send request 
     * @return
     *    the parsed request message
     * @throws IOException
     *    thrown if the request message cannot be parsed
     */
    public Message getBundleInfo(final BundleNamespace message, 
        final TerraHarvestMessage thMessage, final RemoteChannel channel) throws IOException 
    {                                             
        final GetBundleInfoRequestData request = GetBundleInfoRequestData.parseFrom(message.getData());
        
        //response builder
        final GetBundleInfoResponseData.Builder responseBuilder = GetBundleInfoResponseData.newBuilder();
        
        if (request.hasBundleId())
        {
            final long bundleId = request.getBundleId();
            final Bundle bundle = m_Context.getBundle(bundleId);
            
            if (bundle == null)
            {
                m_MessageFactory.createBaseErrorMessage(thMessage, ErrorCode.INVALID_VALUE, 
                    String.format("The bundle %d was not found!", bundleId)).queue(channel);
                return request;
            }
            
            final BundleInfoType bType = createBundleInfoType(request, bundle);
            responseBuilder.addInfoData(bType);
        }
        else
        {
            final Bundle[] allKnownBundles = m_Context.getBundles();
            
            for (Bundle knownBundle : allKnownBundles)
            {
                final BundleInfoType bType = createBundleInfoType(request, knownBundle);
                responseBuilder.addInfoData(bType);
            }
        }
        
        //send the response
        m_MessageFactory.createBundleResponseMessage(thMessage, 
                BundleMessageType.GetBundleInfoResponse, responseBuilder.build()).queue(channel);
        
        
        return request;
    }

    /**
     * Handle install bundle request.
     * @param message
     *    the message that contains the request to install a bundle
     * @param thMessage
     *    the terra harvest message that the request was originally contained in
     * @param channel
     *    the channel that was used to send request
     * @return
     *    the parsed request message
     * @throws IOException
     *    thrown if the request message cannot be parsed
     */
    public Message installBundle(final BundleNamespace message, final TerraHarvestMessage thMessage,
        final RemoteChannel channel) throws IOException
    {
        final InstallRequestData request = InstallRequestData.parseFrom(message.getData());
        
        //bundle object 
        final Bundle bundle;
        try
        {
            bundle = m_Context.installBundle(request.getBundleLocation(), new ByteArrayInputStream(
                request.getBundleFile().toByteArray()));
        }
        catch (final BundleException exception)
        {
            //send error
            queueBundleErrorMessage(thMessage, BundleErrorCode.OSGiBundleException, 
                "The bundle could not be successfully installed." + exception.getMessage(), channel);
            m_Logging.error(exception,  "Request to install a bundle at %s failed.", request.getBundleLocation());
            return request;
        }
        if (request.getStartAtInstall())
        {
            try
            {
                bundle.start();
            }
            catch (final BundleException exception)
            {
                //send error
                queueBundleErrorMessage(thMessage, BundleErrorCode.OSGiBundleException, 
                    "The bundle was installed but could not be successfully started." + exception.getMessage(), 
                        channel);
                m_Logging.error(exception,  "Request to install, and then start bundle at %s failed.", 
                    request.getBundleLocation());
                return request;
            }
        }
        //send response
        final InstallResponseData response = InstallResponseData.newBuilder().setBundleId(bundle.getBundleId()).build();
        m_MessageFactory.createBundleResponseMessage(thMessage, BundleMessageType.InstallResponse, response).
            queue(channel);
        
        return request;
    }

    /**
     * Handle update bundle request.
     * @param message
     *    the message that contains the request to update a specific bundle
     * @param thMessage
     *    the terra harvest message that the request was originally contained in
     * @param channel
     *    the channel that was used to send request
     * @return
     *    the parsed request message
     * @throws IOException
     *    thrown if the request message cannot be parsed
     */
    public Message updateBundle(final BundleNamespace message, final TerraHarvestMessage thMessage,
        final RemoteChannel channel) throws IOException
    {
        final UpdateRequestData request = UpdateRequestData.parseFrom(message.getData());
        //get the bundle
        final Bundle bundle = m_Context.getBundle(request.getBundleId());
        if (bundle == null)
        {
            m_MessageFactory.createBaseErrorMessage(thMessage, ErrorCode.INVALID_VALUE,
                    "The bundle could not be found.").queue(channel);
        }
        else
        {
            try
            {
                bundle.update(new ByteArrayInputStream(request.getBundleFile().toByteArray()));
            }
            catch (final BundleException exception)
            {
                //send error
                queueBundleErrorMessage(thMessage, BundleErrorCode.OSGiBundleException, 
                    "The bundle could not be successfully updated." + exception.getMessage(), channel);
                m_Logging.error(exception,  "Request to update bundle %d failed.", request.getBundleId());
                return request;
            }
            //send response
            m_MessageFactory.createBundleResponseMessage(thMessage, BundleMessageType.UpdateResponse, null).
                queue(channel);
        }
        return request;
    }

    /**
     * Handle uninstall request.
     * @param message
     *    the message that contains the request to uninstall a specific bundle
     * @param thMessage
     *    the terra harvest message that the request was originally contained in
     * @param channel
     *    the channel that was used to send request
     * @return
     *    the parsed request message
     * @throws IOException
     *    thrown if the request message cannot be parsed
     */
    public Message uninstallBundle(final BundleNamespace message, final TerraHarvestMessage thMessage,
        final RemoteChannel channel) throws IOException
    {
        final UninstallRequestData request = UninstallRequestData.parseFrom(message.getData());
        //get the bundle
        final Bundle bundle = m_Context.getBundle(request.getBundleId());
        if (bundle == null)
        {
            m_MessageFactory.createBaseErrorMessage(thMessage, ErrorCode.INVALID_VALUE,
                    "The bundle to uninstall could not be found.").queue(channel);
        }
        else
        {
            try
            {
                bundle.uninstall();
            }
            catch (final BundleException exception)
            {
                //send error
                queueBundleErrorMessage(thMessage, BundleErrorCode.OSGiBundleException, 
                    "The bundle could not be uninstalled." + exception.getMessage(), channel);
                m_Logging.error(exception, "Request to uninstall bundle %d failed.", request.getBundleId());
                return request;
            }
            //send response
            m_MessageFactory.createBundleResponseMessage(thMessage, BundleMessageType.UninstallResponse, null).
                queue(channel);
        }
        return request;
    }
    
    /**
     * This method will return the value if the dictionary has the desired element.
     * 
     * @param headers
     *     the dictionary of headers
     * @param key
     *     the key to check for
     * @return
     *     value to which the key mapped to or an empty string
     */
    private String getItem(final Dictionary<String, String> headers, final String key)
    {
        if (headers.get(key) != null)
        {
            return headers.get(key);
        }
        
        return "";
    }
    
    /**
     * Send an error message. This method will append the error information to the message and send the response.
     * @param thMessage
     *     the terra harvest request message
     * @param error
     *     bundle namespace error code that triggered this error response
     * @param errorDesc
     *     the description of the error that occurred
     * @param channel
     *     the channel through which the original request was transmitted over
     */
    private void queueBundleErrorMessage(final TerraHarvestMessage thMessage, final BundleErrorCode error,
        final String errorDesc, final RemoteChannel channel)
    {
        //construct the error message
        final BundleNamespaceErrorData errorMessage = BundleNamespaceErrorData.newBuilder().
            setError(error).
            setErrorDescription(errorDesc).
            build();
        //send message
        m_MessageFactory.createBundleResponseMessage(thMessage, BundleMessageType.BundleNamespaceError,
            errorMessage).queue(channel);
    }
    
    /**
     * Creates a {@link BundleInfoType} based on the request's required values and the bundle. 
     * @param request
     *  the info request that specifies which information to pull from the found bundle.
     * @param bundle
     *  the bundle that information is being retrieved for.
     * @return
     *  the {@link BundleInfoType} which holds all required information from the passed in bundle.
     */
    private BundleInfoType createBundleInfoType(final GetBundleInfoRequestData request, final Bundle bundle) //NOCHECKSTYLE 
                                               // n-complexity reached. Only if the return requested fields.
    {                                          //Therefore need to check and append data for only what was requested.
        final BundleInfoType.Builder infoBuilder = BundleInfoType.newBuilder();
        
        final Dictionary<String, String> headerDetails = bundle.getHeaders();
        
        infoBuilder.setBundleId(bundle.getBundleId());
        
        //add the correct requested fields
        if (request.getBundleState())
        {
            infoBuilder.setBundleState(bundle.getState());
        }
        
        if (request.getBundleSymbolicName())
        {
            infoBuilder.setBundleSymbolicName(getItem(headerDetails, Constants.BUNDLE_SYMBOLICNAME));
        }
        
        if (request.getBundleDescription())
        {
            infoBuilder.setBundleDescription(getItem(headerDetails, Constants.BUNDLE_DESCRIPTION));
        }
        
        if (request.getBundleVendor())
        {
            infoBuilder.setBundleVendor(getItem(headerDetails, Constants.BUNDLE_VENDOR));
        }
        
        if (request.getBundleVersion())
        {
            infoBuilder.setBundleVersion(getItem(headerDetails, Constants.BUNDLE_VERSION));
        }
        
        if (request.getPackageImports())
        {
            infoBuilder.addAllPackageImport(parseImportExportHeaders(getItem(headerDetails, Constants.IMPORT_PACKAGE)));
            
        }
        
        if (request.getPackageExports())
        {
            infoBuilder.addAllPackageExport(parseImportExportHeaders(getItem(headerDetails, Constants.EXPORT_PACKAGE)));
        }
        
        if (request.getBundleLocation())
        {
            infoBuilder.setBundleLocation(bundle.getLocation());
        }
        
        if (request.getBundleName())
        {
            infoBuilder.setBundleName(getItem(headerDetails, Constants.BUNDLE_NAME));
        }
        
        if (request.getBundleLastModified())
        {
            infoBuilder.setBundleLastModified(bundle.getLastModified());
        }
        
        return infoBuilder.build();
    }
    
    /**
     * Function which parses a passed in import/export header. Function looks
     * for ',' that are not inside of '"' and breaks the string at that point.
     * @param headerValue
     *  the string value of the import or export header
     * @return
     *  if the string is empty then this function will return a list of size 1
     *  that has the empty string as its only entry. if there are packages in the
     *  string, then the list will contain the packages found.
     */
    public List<String> parseImportExportHeaders(final String headerValue)
    {
        final List<String> answers = new ArrayList<String>();
        
        boolean foundBeginningQuote = false;
        int beginningPos = 0;
        
        for (int i = 0; i < headerValue.length(); i++)
        {
            if (headerValue.charAt(i) == ',' && !foundBeginningQuote)
            {
                final String aPackage = headerValue.substring(beginningPos, i);
                beginningPos = i + 1;
                answers.add(aPackage);
            }
            else if (headerValue.charAt(i) == '"' && !foundBeginningQuote)
            {
                foundBeginningQuote = true;
            }
            else if (headerValue.charAt(i) == '"' && foundBeginningQuote)
            {
                foundBeginningQuote = false;
            }
        }
        
        //add an empty string if no packages are present.
        if (beginningPos != headerValue.length())
        {
            //make sure not to drop the last package 
            answers.add(headerValue.substring(beginningPos, headerValue.length()));
        }
        
        return answers;
    }
}
