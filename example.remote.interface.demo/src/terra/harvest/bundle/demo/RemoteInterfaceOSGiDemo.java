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
package terra.harvest.bundle.demo;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.MissionProgramMessages;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplatesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadTemplateRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.VariableMetaData;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;



/**
 * This class demonstrates the OSUS Remote Interface (RI).
 * This file assumes the user understands or is familiar with the following:
 * - basic examples of the ConfigAdmin and MissionProgramManager from the "RemoteInterfaceDemo" project
 * - OSGi
 * - OSUS software development kit (SDK)
 * - BND tools
 * - Declarative Services
 * 
 * The SDK generated the project this file resides in. The ANT script builds a .jar file. This .jar is a
 * functioning bundle that may be added to an OSGi system.
 * 
 * The purpose of this demo is to demonstrate
 * - Message sending using the RI's {@link MessageFactory}
 * - Event Handling, OSGi style asynchronous response handler
 * - Response Handling, an OSUS wrapper meant to simplify handling asynchronous responses
 * 
 * To run this test, compile this file to a bundle using the ANT build script. Load the bundle on a 
 * controller software system. When the bundle is started, OSGi calls the activate method, which is the
 * entry point for this demo.
 * 
 * @author Fred Webber
 */
@Component
public class RemoteInterfaceOSGiDemo
{
//-------------------------------------------------------------------------------------------------
//  OSGi Overhead
//-------------------------------------------------------------------------------------------------
    /** 
     * This tag is used for output. 
     */
    private static final String PRINT_TAG = "RIdemo >> ";
    
    /** 
     * This tag is used for output in the ConfigAdmin. 
     */
    private static final String PRINT_TAG_CA = "ConfigAdmin >> ";
    
    /** 
     * This tag is used for output in the MissionProgramming. 
     */
    private static final String PRINT_TAG_MP = "MissionProgramming >> ";
    
    /** 
     * Non-sensical variable, for demonstrating response handling. 
     */
    private String m_ResponseText = "";

    /** 
     * The ID of the target OSUS Controller Software (OSUS-R).
     * TODO: TH-803 update with demo refactor
     * The ID of a controller may be changed at the "those!" prompt (that is, when OSGi prints "FrameworkEvent STARTED")
     * by typing "setid #" where '#' is an integer of your choice.  
     */
    private final int m_TargetSystemID = 0;

    /** 
     * Service for creating and sending a message through the remote interface. 
     */
    private MessageFactory m_MessageFactory;

    /** 
     * Context for the bundle containing this component.
     */
    private BundleContext m_Context;

    /** 
     * A mechanism for handling OSGi events. 
     */
    private DemoEventHandler m_EventHandler;
    
    /** 
     * The service for connecting to remote controller software. 
     */
    private RemoteChannelLookup m_RemoteChannelLookup;
    
    /**
     * OSGi calls this method to provide the message factory service.
     * @param messageFactory
     *      the message factory service
     */
    @Reference
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        this.m_MessageFactory = messageFactory;
    }
    
    /**
     * Set the {@link RemoteChannelLookup} service.
     * @param lookup
     *     the remote channel lookup service to use
     */
    @Reference
    public void setRemoteChannelLookup(final RemoteChannelLookup lookup)
    {
        m_RemoteChannelLookup = lookup;
    }

    /**
     * This is the entry point for the demo. Once the bundle is started, this method is called.
     * @param context
     *      the bundle context
     * @throws FactoryException
     *      if the bundle cannot be started
     * @throws IOException
     *      if an endpoint cannot be reached
     */
    @Activate
    public void activate(final BundleContext context)  throws FactoryException, IOException
    {
        System.out.println(PRINT_TAG + "activating... connecting to Controller # " + this.m_TargetSystemID);
        
        // Connect to the target controller. Port 4000 is the default listening port for all controllers.
        this.m_RemoteChannelLookup.syncClientSocketChannel("localhost", 4000, this.m_TargetSystemID);
        System.out.println(PRINT_TAG + "Connected! Registering EventHandler...");
        
        this.m_Context = context;
        this.m_EventHandler = new DemoEventHandler();
        this.m_EventHandler.registerAllEvents(this.m_Context);
        System.out.println(PRINT_TAG + "all events registered, starting Config Admin demo");
        
        this.runConfigAdminDemo();
        
        System.out.println(PRINT_TAG + "Config Admin complete, beginning Mission Programming demo");
        this.runMissionProgrammingDemo();
        
        System.out.println(PRINT_TAG + "Mission Programming complete.");        
        System.out.println(PRINT_TAG + "Demo complete");
    }

    /**
     * When the bundle is stopped, the deactivate method unregisters the event listener.
     */
    @Deactivate
    public void deactivate()
    {
        m_EventHandler.unregisterListener();
    }
        
    /**
     * The ConfigurationAdmin shows how to manipulate properties on remote controller software.
     * @throws IOException 
     *      if an endpoint cannot be reached.
     */
    public void runConfigAdminDemo() throws IOException
    {
        // The PID is the identifier for a configuration via the OSGi-provided ConfigurationAdmin service
        final String logWriterPid = "mil.dod.th.ose.logging.LogWriter";
        final String logWriterProp = "logLevel";
        
        //---------------------------------------------------------------------
        //              REQUEST THE CONFIGURATION INFO
        // Users may request configuration info to see which configurations are
        // present on an instance of controller software.
        //---------------------------------------------------------------------
        // Build the request
        final GetConfigurationInfoRequestData requestForInfo = GetConfigurationInfoRequestData.
                newBuilder().                                       // Start a new builder
                setFilter("(service.Pid=" + logWriterPid + ")").    // Get the LogWriter bundle
                setIncludeProperties(false).                        // Don't include configuration property information
                build();                                            // Finalize construction
        
        
        // Create a response handler using an anonymous class
        // The response handler can be null when appropriate
        final ResponseHandler infoRequestHandler = new ResponseHandler()
        {
            @Override
            public void handleResponse(final TerraHarvestMessage msg, final TerraHarvestPayload payload,
                    final Message namespaceMsg, final Message dataMsg)
            {
                System.out.println(PRINT_TAG_CA 
                        + "The response to the GetConfigurationInfoRequest has a payload with data type");
                System.out.println(PRINT_TAG_CA + " '" + ((ConfigAdminNamespace)namespaceMsg).getType() + "'");

                // Get the data from the response
                System.out.println(PRINT_TAG_CA + "The configuration list: [" 
                        + ((GetConfigurationInfoResponseData)dataMsg).getConfigurationsList() + "]");
            }
        };
        
        // Queue the message to the other controller
        this.m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.GetConfigurationInfoRequest,
                requestForInfo).queue(this.m_TargetSystemID, EncryptType.NONE, infoRequestHandler);
        
        // The response is handled by configAdminEventHandler.handleEvent and by the ResponseHandler


        //---------------------------------------------------------------------
        //              UPDATE OR CREATE A REMOTE CONFIGURATION 
        // A remote configuration must be created before it can be used (such
        // as reading values from it). Typically, one would be pre-loaded on
        // the controller software. But in the situation that one is not, the
        // user can create one remotely using a SetPropertyRequest.
        //
        // To create a remote configuration, send a message setting any
        // parameter. This example creates a parameter called 'logLevel' of
        // type INT32. The parameter value is "2". The log levels are
        // 1: Error
        // 2: Warning
        // 3: Info
        // 4: Debug
        // Setting a log level gets all the levels above it. So, setting a log
        // level of '2' also includes '1'.
        //
        // A ConfigAdminMessageType.SetPropertyRequest creates the
        // configuration if a configuration does not exist; otherwise, it
        // updates the configuration. If a configuration does not already
        // exist, then a ConfigAdminMessageType.SetPropertyRequest must be the
        // first call to the remote system.
        //---------------------------------------------------------------------

        // Build a parameter to send
        final Multitype mtype = Multitype.
                newBuilder().                   // Start the builder
                setType(Type.INT32).            // Designate it as type INT32
                setInt32Value(2).               // Assign a value to the variable
                build();                        // Finalize construction

        //Build the property to be set
        final SimpleTypesMapEntry prop = SimpleTypesMapEntry.newBuilder().setKey(logWriterProp).setValue(mtype).build();
        
        // Build a message using the above payload
        final SetPropertyRequestData setPropertyRequest = SetPropertyRequestData.
                newBuilder().
                setPid(logWriterPid).
                addProperties(prop).
                build();
               
        
        // Create the response handler 
        final ResponseHandler setPropertyHandler = new ResponseHandler()
        {
            @Override
            public void handleResponse(final TerraHarvestMessage msg, final TerraHarvestPayload payload, 
                    final Message namespaceMsg, final Message dataMsg)
            {
                // The response should be a non-null response with a namespace type Namespace.ConfigAdmin
                System.out.println(PRINT_TAG_CA + "Response to namespace is " + payload.getNamespace());

                // The message namespace should be of type ConfigAdminMessageType.SetPropertyResponse
                System.out.println(PRINT_TAG_CA + "Namespace response type is "
                        + ((ConfigAdminNamespace)namespaceMsg).getType());
            }
        };
        
        // Construct and send a TerraHarvestMessage
        this.m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.SetPropertyRequest,
                setPropertyRequest).queue(this.m_TargetSystemID, EncryptType.NONE, setPropertyHandler);

        // The response will be handled by configAdminEventHandler.handleEvent and by the remote handler above
        

        //---------------------------------------------------------------------
        //              REQUEST A PARAMETER
        // Request the value of a specific parameter belonging to a known
        // configuration from a specific remote controller software.
        // This example requests the current log level from the LogWriter.
        //---------------------------------------------------------------------
        // Build the request
        final GetPropertyRequestData requestProperty = GetPropertyRequestData.
                newBuilder().
                setPid(logWriterPid).
                setKey(logWriterProp).
                build();
        
        // Create the response handler 
        final ResponseHandler requestPropHandler = new ResponseHandler()
        {
            @Override
            public void handleResponse(final TerraHarvestMessage msg, final TerraHarvestPayload payload, 
                    final Message namespaceMsg, final Message dataMsg)
            {
                // Get the type and value of the property
                // Getting the type is important because a property may be overridden by a different type of data.
                // For example, one analyst might remotely assign a parameter called "ID" parameter as a string, while
                // another analyst may habitually assign a parameter with the same name ("ID") as an integer value.

                // Get the parameter data type and value
                final Multitype value = ((GetPropertyResponseData)dataMsg).getValue();
                System.out.println(PRINT_TAG_CA + "The property type is " + value.getType());
                // Because this is a demo and we know the type is a String, we'll skip using any conditional logic
                System.out.println(PRINT_TAG_CA + "The value is " + value.getStringValue());
            }
        };

        // Construct and send a TerraHarvestMessage
        this.m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.GetPropertyRequest,
                requestProperty).queue(this.m_TargetSystemID, EncryptType.NONE, requestPropHandler);

        // The response will be handled by configAdminEventHandler.handleEvent and the above handler
        

        //---------------------------------------------------------------------
        //              REQUEST THE LIST OF PARAMETER KEYS
        // This is useful when you need to see what parameter keys are on the
        // remote device. If you use an invalid PID, a non-null response is
        // returned with an empty array of data.
        // Currently, the two keys in the configuration should be "logLevel"
        // and "service.pid". All configurations have a "service.pid" property,
        // equal to the value used in setPid (below).
        //---------------------------------------------------------------------
        // Build the request
        final GetPropertyKeysRequestData requestPropertyKeys = GetPropertyKeysRequestData
                .newBuilder()
                .setPid(logWriterPid)
                .build();
        
        // Develop the response handler 
        final ResponseHandler propKeysHandler = new ResponseHandler()
        {
            @Override
            public void handleResponse(final TerraHarvestMessage msg, final TerraHarvestPayload payload, 
                    final Message namespaceMsg, final Message dataMsg)
            {
                final GetPropertyKeysResponseData keysData = (GetPropertyKeysResponseData)dataMsg;
                System.out.println(PRINT_TAG_CA + "Keys are " + keysData.getKeyList() + " with "
                        + keysData.getKeyCount() + " keys");
            }
        };
        
        // Construct and send a TerraHarvestMessage
        this.m_MessageFactory.createConfigAdminMessage(ConfigAdminMessageType.GetPropertyKeysRequest,
                requestPropertyKeys).queue(this.m_TargetSystemID, EncryptType.NONE, propKeysHandler);

        // The response will be handled by configAdminEventHandler.handleEvent and the above handler

    } // end of the configuration admin demo
    
    
    /**
     * This method demonstrates getting information from the below handler back into the containing class.
     * It is non-sensical, and serves to demonstrate getting information back to this class from the handler.
     * @param response
     *      the response to a load mission request
     */
    public void setLoadMissionResponseToStringText(final String response)
    { 
        this.m_ResponseText = response;
        System.out.println(PRINT_TAG_MP + "Last program loaded was " + this.m_ResponseText);
    }
    
    /**
     * This response handler receives the response to the LoadMission request when passed as the handler. 
     */
    private class LoadMissionHandler implements ResponseHandler
    {

        /** 
         * This holds the containing class, for access back. 
         */
        private RemoteInterfaceOSGiDemo m_Parent;
        
        /**
         * The constructor for this handler.
         * @param parent
         *      the containing class
         */
        public LoadMissionHandler(final RemoteInterfaceOSGiDemo parent)
        {
            this.m_Parent = parent;
        }

        @Override
        public void handleResponse(final TerraHarvestMessage msg, final TerraHarvestPayload payload, 
                final Message namespaceMsg, final Message dataMsg)
        {
            // Note it is also possible to get error messages back, this does not handle that case
            // Demonstrate a call back to the class that started the message sequence
            this.m_Parent.setLoadMissionResponseToStringText(namespaceMsg.toString());
        }
    }
    
    /**
     * This response handler receives the response to the LoadTemplate request when passed as the handler.
     */
    private class ReceiveTemplatesHandler implements ResponseHandler
    {

        @Override
        public void handleResponse(final TerraHarvestMessage msg, final TerraHarvestPayload payload, 
                final Message namespaceMsg, final Message dataMsg)
        {
            // Read in the response
            final GetTemplatesResponseData templateNames  = (GetTemplatesResponseData)dataMsg;

            System.out.println(PRINT_TAG_MP + "Templates are:");
            for (MissionProgramTemplateGen.MissionProgramTemplate template : templateNames.getTemplateList())
            {
                System.out.println("\t" + template);
            }               
        }
    }
    
    /**
     * This demonstrates using templates with the mission programming namespace. Mission program management enables the
     * execution of scripts on controller software. Scripts can use or exploit assets, custom comms, or other
     * capabilities. Templates are pre-configured controller software configurations and scripts. 
     * 
     * This part of the demo employs a more complicated pair of response handlers, LoadMissionHandler and 
     * ReceiveTemplatesHandler.
     * 
     * @throws IOException
     *      When an endpoint cannot be reached
     */
    public void runMissionProgrammingDemo() throws IOException
    {

        //---------------------------------------------------------------------
        //              GET STORED TEMPLATE NAMES
        // See what templates, if any, are on the destination controller
        // device. The response contains a list of known templates on the
        // controller device. 
        //---------------------------------------------------------------------
        
        // Construct and send a TerraHarvestMessage
        this.m_MessageFactory.createMissionProgrammingMessage(MissionProgrammingMessageType.GetTemplatesRequest,
                null).queue(this.m_TargetSystemID, EncryptType.NONE, new ReceiveTemplatesHandler());
        
        // The response will be handled by configAdminEventHandler.handleEvent and the ReceiveTemplatesHandler

      
        //---------------------------------------------------------------------
        //              LOAD TEMPLATES
        // Missions can be instantiated by loading a template and providing a
        // set of parameters.  
        // Multiple missions may be created from the same template.
        // Different instances might use different parameters; for example,
        // one mission may reference one asset, but another mission constructed
        // from the same template may reference a different asset (such as
        // 'left' and 'right' cameras).
        //---------------------------------------------------------------------
        
        // Each template has a script. Scripts may contain variables that must be defined; see the script in the
        // template to determine what variables must be defined.
        // To define these variables, build a VariableMetaData instance.
        final MissionProgramTemplateGen.MissionVariableMetaData varData = 
                MissionProgramTemplateGen.MissionVariableMetaData.newBuilder().
                setBase(VariableMetaData.newBuilder().
                    setName("variable").
                    setDescription("I describe a variable").
                    setDefaultValue("My Asset").
                    setHumanReadableName("The variable")).
                setType(MissionProgramTemplateGen.MissionVariableTypes.Enum.ASSET).
                build();
        // Build a mission from a template
        final MissionProgramTemplateGen.MissionProgramTemplate template = 
                MissionProgramTemplateGen.MissionProgramTemplate.
                newBuilder().
                setName("TEST_TEMPLATE").
                setSource("If one wanted to change the script, this is where the change would be made").
                setDescription("This describes the template").
                addVariableMetaData(varData).
                build();
        // Build a template message
        final LoadTemplateRequestData loadTemplateRequest = MissionProgramMessages.LoadTemplateRequestData.
                newBuilder().
                setMission(template).
                build();
        
        
        // Construct and send a TerraHarvestMessage
        this.m_MessageFactory.createMissionProgrammingMessage(
                MissionProgrammingMessageType.LoadTemplateRequest, loadTemplateRequest).
                queue(this.m_TargetSystemID, EncryptType.NONE, new LoadMissionHandler(this));

        // The response will be handled by configAdminEventHandler.handleEvent and the LoadMissionHandler

    } // End of the mission programming demo
    
    
//-------------------------------------------------------------------------------------------------
//                                      EventHandler Demo
// Event handlers are provided by OSGi. Event Handlers are an option for developers, though the
// OSUS Remote Interface (RI) provides more tightly integrated, efficient functionality.
//-------------------------------------------------------------------------------------------------
    /**
     * This is an Event Handler, to use the OSGi framework.
     */
    public class DemoEventHandler implements EventHandler
    {

        /**
         * Service registration for the listener service. Saved for unregistering the service when the instance is 
         * destroyed.
         */
        private ServiceRegistration<EventHandler> m_Registration;
        
        /**
         * Method to register this event handler to listen for message received events.
         * @param context
         *      the bundle context
         */
        public void registerAllEvents(final BundleContext context)
        {
            // register to listen for all messages in the topic "TOPIC_MESSAGE_RECEIVED"
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            
            // register the event handler
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        /* 
         * See org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
         */
        @Override
        public void handleEvent(final Event event)
        {
            // This method demonstrates the asynchronous event handling using OSGi events
            System.out.println("EventHandler >> Received message: " + event);
        }
        
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
        
    }
}
