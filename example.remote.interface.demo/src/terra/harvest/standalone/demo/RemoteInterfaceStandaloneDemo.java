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
package terra.harvest.standalone.demo;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigurationInfoType;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.MissionProgramMessages;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplatesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadTemplateRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.VariableMetaData;

/**
* The purpose of this demo is to introduce the user to the remote interface. The remote interface allows the user to
* communicate with controller software. For the rest of this demo, a 'controller device' is the hardware that the
* 'controller software' works on. The remote interface communicates between the user's software and OSUS
* controller software (OSUS-R) running on a controller device.
* 
* This demo includes routing of messages to the {@link ConfigAdminNamespace} and the
* {@link MissionProgrammingNamespace}.
* 
* To run this demo, download the appropriate pre-compiled controller software from the repository and start it before
* running the main method here. On my computer, I run the "start_controller.bat" since I am using Windows. The
* "start_controller.bat" file is located in "unzippedFolderLocation\controller-app-generic\bin" where
* "unzippedFolderLocation" is where you unzipped the sample controller software. The controller software is ready when
* you see a line saying "FrameworkEvent STARTED".
* 
* The remote interface is a way to communicate with controller software, whether running on the same controller device
* or a different one. In this demo, this program does not need to run on controller software; it is independent. It
* does, however, communicate with the controller software, such as the pre-compiled controller software.
* 
* The primary pattern in this demo is to:
* 1) <b>generate a request</b>. Typically, a message is created using a Builder from its static ".newBuilder()"
* method.<br>
* 2) <b>send the request</b> to the designated controller software;<br>
* 3) <b>receive the response</b> from the designated controller software.<br>
* 4) <b>parse the response</b>.<br>
* 
* @author Fred Webber
* @author Dave Humeniuk
* 
*/

public class RemoteInterfaceStandaloneDemo
{
    private int uniqueID = 0;
    private Socket sock;

    public static void main(String[] args) throws UnknownHostException, IOException
    {
        System.out.println("Creating new demo instance...");
        RemoteInterfaceStandaloneDemo demo = new RemoteInterfaceStandaloneDemo();
        System.out.println("Connecting to controller...");
        demo.connect();
        System.out.println("Running ConfigAdmin demo...");
        demo.runConfigurationDemo();
        System.out.println("Running MissionProgramming demo...");
        demo.runMissionProgrammingDemo();
        System.out.println("Cleaning up...");
        demo.disconnect();
        System.out.println("Finished running RemoteInterfaceDemo!");
    }

    /**
     * Get a connection to the controller software !!! This will throw an exception if you haven't already started the
     * demo controller software. !!!
     */
    public void connect() throws UnknownHostException, IOException
    {
        this.sock = this.connectToController();
    }

    /**
     * Disconnect from the controller software
     */
    public void disconnect() throws IOException
    {
        this.sock.close();
    }

    /**
     * Run the Configuration Management Demo.
     * 
     * Configuration management allows remote setting and getting of controller software parameters. This allows the
     * user to configure any OSGi managed service (that is, any class implementing an interface containing
     * "ManagedService" in its name). This includes assets, comms layers, and other components.
     * 
     * If the demo runs successfully, the output should look similar to this: Creating new demo instance... Connecting
     * to controller... Running ConfigAdmin demo... The type of the response is GetConfigurationInfoResponse The data is
     * [pid: "mil.dod.th.ose.logging.LogWriter" bundleLocation:
     * "file:/C:/Users/user/Desktop/controller-app-generic/bundle/mil.dod.th.ose.remoteinterface.jar" ] Response
     * namespace is ConfigAdmin Namespace response type is SetPropertyResponse Response for the name request: sourceId:
     * 0 destId: 0 namespace: ConfigAdmin messageId: 2 message: "\b\004\022\006\n\004\b\003\030\002"
     * 
     * The message contained inside the response is type: GetPropertyResponse data: "\n\004\b\003\030\002"
     * 
     * The type of the requested property is INT32 The value of the requested property is Keys are [service.pid,
     * logLevel] with 2 keys Running MissionProgramming demo... Uncomment the next line to see the stored templates. The
     * response is of namespace MissionProgramming and has error code NO_ERROR. The response message type is
     * LoadTemplateResponse Cleaning up... Finished running RemoteInterfaceDemo!
     */
    public void runConfigurationDemo() throws IOException
    {

        String logWriterPid = "mil.dod.th.ose.logging.LogWriter";
        
        // ---------------------------------------------------------------------
        // REQUEST THE CONFIGURATION INFO
        // Users may request configuration info to see which configurations are
        // present on an instance of controller software.
        // ---------------------------------------------------------------------
        // Build the request
        GetConfigurationInfoRequestData requestForInfo = 
                GetConfigurationInfoRequestData.newBuilder() // Start a new builder
                    .setFilter("(service.pid=" + logWriterPid + ")") // Get the LogWriter bundle
                    .setIncludeProperties(false) //Don't include configuration properties
                    .build(); // Finalize construction

        // Construct and send a TerraHarvestMessage
        ConfigAdminNamespace.Builder getConfigBuilder = ConfigAdminNamespace.newBuilder().setType(
                ConfigAdminMessageType.GetConfigurationInfoRequest).setData(requestForInfo.toByteString());
        TerraHarvestMessage messageForInfoRequest = this.createTerraHarvestMsg(Namespace.ConfigAdmin, getConfigBuilder);
        messageForInfoRequest.writeDelimitedTo(sock.getOutputStream());

        // Read in the response
        // The response should be non-null and contain a message of the ConfigAdmin namespace.
        TerraHarvestMessage responseToInfoRequest = TerraHarvestMessage.parseDelimitedFrom(sock.getInputStream());

        // Read in the payload information from the response
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(responseToInfoRequest.getTerraHarvestPayload());
        // Parse the message namespace type
        ConfigAdminNamespace namespaceOfInfoResponse = ConfigAdminNamespace.parseFrom(payload.getNamespaceMessage());
        System.out.println("The type of the response is " + namespaceOfInfoResponse.getType());

        // Get the data from the response.
        GetConfigurationInfoResponseData responseData = 
                GetConfigurationInfoResponseData.parseFrom(namespaceOfInfoResponse.getData());
        List<ConfigurationInfoType> data = responseData.getConfigurationsList();
        System.out.println("The data is " + data);

        // ---------------------------------------------------------------------
        // UPDATE OR CREATE A REMOTE CONFIGURATION
        // To update a remote configuration, send a message setting any
        // parameter. This example updates a parameter called 'logMBSizeLimit' of
        // type INT32. The parameter value is "2".
        //
        // A ConfigAdminMessageType.SetPropertyRequest creates the
        // configuration if a configuration does not exist; otherwise, it
        // updates the configuration. If a configuration does not already
        // exist, then a ConfigAdminMessageType.SetPropertyRequest must be the
        // first call to the remote system.
        // ---------------------------------------------------------------------

        String logWriterProp = "logMBSizeLimit";

        // Build a parameter to send
        Multitype mtype = Multitype.newBuilder() // Start the builder
                .setType(Type.INT32) // Designate it as type INT32
                .setInt32Value(2) // Assign a value to the variable
                .build(); // Finalize construction

        //Build the property to be set
        SimpleTypesMapEntry prop = SimpleTypesMapEntry.newBuilder().setKey(logWriterProp).setValue(mtype).build();
        
        // Build a message using the above payload
        SetPropertyRequestData request = 
                SetPropertyRequestData.newBuilder().setPid(logWriterPid).addProperties(prop).build();

        // Construct and send a TerraHarvestMessage
        ConfigAdminNamespace.Builder configMessageBuilder = ConfigAdminNamespace.newBuilder().setType(
                ConfigAdminMessageType.SetPropertyRequest).setData(request.toByteString());
        TerraHarvestMessage message = this.createTerraHarvestMsg(Namespace.ConfigAdmin, configMessageBuilder);
        message.writeDelimitedTo(sock.getOutputStream());

        // The response should be a non-null response with a namespace type Namespace.ConfigAdmin.
        TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(sock.getInputStream());
        // get the payload information
        TerraHarvestPayload payloadForResponse = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());

        System.out.println("Response namespace is " + payloadForResponse.getNamespace());

        // The message namespace should be of type ConfigAdminMessageType.SetPropertyResponse.
        ConfigAdminNamespace namespaceResponse = 
                ConfigAdminNamespace.parseFrom(payloadForResponse.getNamespaceMessage());
        System.out.println("Namespace response type is " + namespaceResponse.getType());

        // ---------------------------------------------------------------------
        // REQUEST A PARAMETER
        // Request the value of a specific parameter belonging to a known
        // configuration from a specific remote controller software.
        // This example requests the current log level from the LogWriter.
        // ---------------------------------------------------------------------
        // Build the request
        GetPropertyRequestData requestName = GetPropertyRequestData.newBuilder().setPid(logWriterPid).setKey(
                logWriterProp).build();

        // Construct and send the request
        ConfigAdminNamespace.Builder getPropertyBuilder = ConfigAdminNamespace.newBuilder().setType(
                ConfigAdminMessageType.GetPropertyRequest).setData(requestName.toByteString());
        TerraHarvestMessage messageRequestingName = this.createTerraHarvestMsg(Namespace.ConfigAdmin,
                getPropertyBuilder);
        messageRequestingName.writeDelimitedTo(sock.getOutputStream());

        // Read in the response. The response should be non-null, of namespace type ConfigAdmin, and have a message
        // containing the response for the requested property.
        TerraHarvestMessage responseForNameRequest = TerraHarvestMessage.parseDelimitedFrom(sock.getInputStream());
        TerraHarvestPayload payloadForNameRequest = 
                TerraHarvestPayload.parseFrom(responseForNameRequest.getTerraHarvestPayload());

        System.out.println("Response for the name request:\n" + responseForNameRequest);

        // Parse specific message type. It should be a 'GetPropertyResponse'
        ConfigAdminNamespace namespaceResponseForNameRequest = 
                ConfigAdminNamespace.parseFrom(payloadForNameRequest.getNamespaceMessage());
        System.out.println("The message contained inside the response is\n" + namespaceResponseForNameRequest);

        // Get the type and value of the property
        // Getting the type is important because a property may be overridden by a different type of data.
        // For example, one analyst might remotely assign a parameter called "ID" parameter as a string, while another
        // analyst may habitually assign a parameter with the same name ("ID") as an integer value.
        GetPropertyResponseData propData = GetPropertyResponseData.parseFrom(namespaceResponseForNameRequest.getData());
        // Get the parameter data type and value
        Multitype value = propData.getValue();
        System.out.println("The type of the requested property is " + value.getType());
        // Because this is a demo and we know the type is a String, we'll skip using any conditional logic
        System.out.println("The value of the requested property is " + value.getInt32Value());

        // ---------------------------------------------------------------------
        // REQUEST THE LIST OF PARAMETER KEYS
        // This is useful when you need to see what parameter keys are on the
        // remote device. If you use an invalid PID, a non-null response is
        // returned with an empty array of data.
        // Currently, the two keys in the configuration should be "logLevel"
        // and "service.pid". All configurations have a "service.pid" property,
        // equal to the value used in setPid (below).
        // ---------------------------------------------------------------------
        // Build the request
        GetPropertyKeysRequestData requestPropertyKeys = 
                GetPropertyKeysRequestData.newBuilder().setPid(logWriterPid).build();

        // Construct and send the TerraHarvestMessage
        ConfigAdminNamespace.Builder getPropKeysBuilder = ConfigAdminNamespace.newBuilder().setType(
                ConfigAdminMessageType.GetPropertyKeysRequest).setData(requestPropertyKeys.toByteString());
        TerraHarvestMessage messageRequestingKeys = this.createTerraHarvestMsg(Namespace.ConfigAdmin,
                getPropKeysBuilder);
        messageRequestingKeys.writeDelimitedTo(sock.getOutputStream());

        // Read in response and parse message
        TerraHarvestMessage responseToKeyRequest = TerraHarvestMessage.parseDelimitedFrom(sock.getInputStream());
        TerraHarvestPayload payloadToKeyRequest = 
                TerraHarvestPayload.parseFrom(responseToKeyRequest.getTerraHarvestPayload());

        ConfigAdminNamespace namespaceResponseToKeyRequest = 
                ConfigAdminNamespace.parseFrom(payloadToKeyRequest.getNamespaceMessage());
        GetPropertyKeysResponseData keysData = 
                GetPropertyKeysResponseData.parseFrom(namespaceResponseToKeyRequest.getData());
        System.out.println("Keys are " + keysData.getKeyList() + " with " + keysData.getKeyCount() + " keys");

    } // end of the configuration admin demo

    /**
     * This demonstrates using templates with the mission programming namespace. Mission program management enables the
     * execution of scripts on controller software. Scripts can use or exploit assets, custom comms, or other
     * capabilities. Templates are pre-configured controller software configurations and scripts.
     */
    public void runMissionProgrammingDemo() throws IOException
    {

        // ---------------------------------------------------------------------
        // GET STORED TEMPLATE NAMES
        // See what templates, if any, are on the destination controller
        // device. The response contains a list of known templates on the
        // controller device.
        // ---------------------------------------------------------------------
        // Construct the request
        MissionProgrammingNamespace.Builder getTemplateNamesBuilder = MissionProgrammingNamespace.newBuilder().setType(
                MissionProgrammingMessageType.GetTemplatesRequest);
        TerraHarvestMessage requestStoredTemplates = this.createTerraHarvestMsg(Namespace.MissionProgramming,
                getTemplateNamesBuilder);
        requestStoredTemplates.writeDelimitedTo(sock.getOutputStream());

        // Read in the response
        TerraHarvestMessage responseStoredTemplates = TerraHarvestMessage.parseDelimitedFrom(sock.getInputStream());
        TerraHarvestPayload payloadStoreTemplates = 
                TerraHarvestPayload.parseFrom(responseStoredTemplates.getTerraHarvestPayload());
        MissionProgrammingNamespace mpResponseMessage = 
                MissionProgrammingNamespace.parseFrom(payloadStoreTemplates.getNamespaceMessage());
        GetTemplatesResponseData templateNames = GetTemplatesResponseData.parseFrom(mpResponseMessage.getData());
        System.out.println(templateNames);

        // ---------------------------------------------------------------------
        // LOAD TEMPLATES
        // Missions can be instantiated by loading a template and providing a
        // set of parameters.
        // Multiple missions may be created from the same template.
        // Different instances might use different parameters; for example,
        // one mission may reference one asset, but another mission constructed
        // from the same template may reference a different asset (such as
        // 'left' and 'right' cameras).
        // ---------------------------------------------------------------------

        // Each template has a script. Scripts may contain variables that must be defined; see the script in the
        // template to determine what variables must be defined.
        // To define these variables, build a VariableMetaData instance.
        MissionProgramTemplateGen.MissionVariableMetaData varData = 
                MissionProgramTemplateGen.MissionVariableMetaData.newBuilder().
                setBase(VariableMetaData.newBuilder().
                    setName("variable").
                    setDescription("I describe a variable").
                    setDefaultValue("My Asset").
                    setHumanReadableName("The variable")).
                setType(MissionProgramTemplateGen.MissionVariableTypes.Enum.ASSET).
                build();
        // Build a mission from a template
        MissionProgramTemplateGen.MissionProgramTemplate template = 
                MissionProgramTemplateGen.MissionProgramTemplate.newBuilder().
                    setName("TEST_TEMPLATE").
                    setSource("If one wanted to change the script, this is where the change would be made").
                    setDescription("This describes the template").
                    addVariableMetaData(varData).build();
        // Build a template message
        LoadTemplateRequestData programMessage = MissionProgramMessages.LoadTemplateRequestData.newBuilder().setMission(
                template).build();

        // Construct and send the TerraHarvestMessage
        MissionProgrammingNamespace.Builder builderLoadTemplate = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.LoadTemplateRequest).
                setData(programMessage.toByteString());
        TerraHarvestMessage messageLoadTemplate = this.createTerraHarvestMsg(Namespace.MissionProgramming,
                builderLoadTemplate);
        messageLoadTemplate.writeDelimitedTo(sock.getOutputStream());

        // The response should be a non-null message of namespace "Namespace.MissionProgramming".
        // The error code (like a status code) should be reported as "No error".
        TerraHarvestMessage responseLoadTemplate = TerraHarvestMessage.parseDelimitedFrom(sock.getInputStream());
        TerraHarvestPayload payloadForTemplate = 
                TerraHarvestPayload.parseFrom(responseLoadTemplate.getTerraHarvestPayload());

        System.out.println("The response is of namespace " + payloadForTemplate.getNamespace());

        // The type of the response message should be a "MissionProgrammingMessageType.LoadTemplateResponse".
        MissionProgrammingNamespace namespaceResponseLoadTemplate = 
                MissionProgrammingNamespace.parseFrom(payloadForTemplate.getNamespaceMessage());
        System.out.println("The response message type is " + namespaceResponseLoadTemplate.getType());

    } // End of the mission programming demo

    /**
     * Create a socket to a controller software running on the local machine
     * 
     * @return the instantiated socket
     */
    private Socket connectToController() throws UnknownHostException, IOException
    {
        Socket socket = new Socket("localhost", 4000);
        socket.setSoTimeout(2000);
        return socket;
    }

    /**
     * Create a builder for a {@link TerraHarvestMessage} that fills in the source and destination id.
     */
    private TerraHarvestMessage createTerraHarvestMsg(Namespace namespace, Message.Builder namespaceBuilder)
    {
        final TerraHarvestPayload payLoad = 
                TerraHarvestPayload.newBuilder().setNamespace(namespace).setNamespaceMessage(
                namespaceBuilder.build().toByteString()).build();
        return TerraHarvestMessage.newBuilder()
            .setVersion(RemoteConstants.SPEC_VERSION)
            .setSourceId(1)
            // send message back to source
            .setDestId(0)
            .setMessageId(this.uniqueID++)
            .setTerraHarvestPayload(payLoad.toByteString()).build();
    }
}
