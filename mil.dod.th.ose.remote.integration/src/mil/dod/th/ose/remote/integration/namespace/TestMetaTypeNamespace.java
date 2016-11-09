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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import example.metatype.XML.ExampleClass;
import example.metatype.XML.ExampleClassConfig;
import mil.dod.th.core.remote.proto.MetaTypeMessages.AttributeDefinitionType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeDefinitionRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeDefinitionResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeKeysRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetAttributeKeysResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetBundlePidsRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetBundlePidsResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoRequestData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.GetMetaTypeInfoResponseData;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeInfoType;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace.MetaTypeMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.integration.BundleNamespaceUtils;
import mil.dod.th.ose.remote.integration.MessageListener;
import mil.dod.th.ose.remote.integration.SocketHostHelper;
import mil.dod.th.ose.remote.integration.TerraHarvestMessageHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.metatype.AttributeDefinition;

import com.google.protobuf.Message;

/**
 * Test routing of messages to the {@link MetaTypeNamespace}. Bundle service is used to install example asset, but
 * is not the class under test.
 * 
 * @author callen
 */
public class TestMetaTypeNamespace 
{
    private static final String EXAMPLE_METATYPE_BUNDLE_NAME = "mil.dod.th.ose.integration.example.metatype";
    private Socket m_Socket;
    
    @Before
    public void setUp() throws IOException
    {
        m_Socket = SocketHostHelper.connectToController();
    }
    
    @After
    public void tearDown() throws IOException
    {
        m_Socket.close();
    }
    
    /**
     * Test that a list of pids is returned for a particular bundle id.
     * 
     * Verify that pids are returned.
     */
    @Test
    public void testGetBundlePids() throws UnknownHostException, IOException, InterruptedException
    {
        //find the example metatype bundle
        long bundleId = 
                BundleNamespaceUtils.getBundleBySymbolicName(EXAMPLE_METATYPE_BUNDLE_NAME, m_Socket);
        
        //construct request
        GetBundlePidsRequestData request = GetBundlePidsRequestData.newBuilder().
            setBundleId(bundleId).build();
        TerraHarvestMessage message = createMetaTypeMessage(MetaTypeMessageType.GetBundlePidsRequest, request);

        //send message
        message.writeDelimitedTo(m_Socket.getOutputStream());

        //read in response
        TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(m_Socket.getInputStream());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        assertThat(response, is(notNullValue()));
        assertThat(payLoadTest.getNamespace(), is(Namespace.MetaType));
        //parse specific message type
        MetaTypeNamespace namespaceResponse = MetaTypeNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        assertThat(namespaceResponse.getType(), is(MetaTypeMessageType.GetBundlePidsResponse));
        //verify
        GetBundlePidsResponseData responseData = GetBundlePidsResponseData.parseFrom(namespaceResponse.getData());

        assertThat(responseData.getPidsCount(), is(1));
        //verify if there is more that one PID
        assertThat(responseData.getPidsList().contains(ExampleClass.class.getName()), is(true));
    }

    /**
     * Test that GetAttributeDefinitionRequest returns a list of attribute definition types.
     * 
     * Verify the expected number of attributes is returned.
     * 
     * Verify content of the attribute definition type.
     */
    @Test
    public void testGetAttributeDefinitionRequest() throws UnknownHostException, IOException, InterruptedException
    {
        //find the example metatype bundle
        long bundleId = 
                BundleNamespaceUtils.getBundleBySymbolicName(EXAMPLE_METATYPE_BUNDLE_NAME, m_Socket);

        //construct request
        GetAttributeDefinitionRequestData request = GetAttributeDefinitionRequestData.newBuilder().
            setBundleId(bundleId).setPid(ExampleClass.class.getName()).setKey(
                    ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING).build();
        TerraHarvestMessage message = createMetaTypeMessage(MetaTypeMessageType.GetAttributeDefinitionRequest, request);

        //send message
        message.writeDelimitedTo(m_Socket.getOutputStream());

        //read in response
        TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(m_Socket.getInputStream());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        
        assertThat(response, is(notNullValue()));
        assertThat(payLoadTest.getNamespace(), is(Namespace.MetaType));
        //parse specific message type
        MetaTypeNamespace namespaceResponse = MetaTypeNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        assertThat(namespaceResponse.getType(), is(MetaTypeMessageType.GetAttributeDefinitionResponse));
        //verify
        GetAttributeDefinitionResponseData responseData = GetAttributeDefinitionResponseData.parseFrom(
            namespaceResponse.getData());
        assertThat(responseData.getAttributeDefinitionCount(), greaterThan(0));
        assertThat(responseData.getAttributeDefinition(0).getAttributeType(), is(AttributeDefinition.STRING));
        assertThat(responseData.getAttributeDefinition(0).getName(), 
                is("Example Config String Value"));
        assertThat(responseData.getAttributeDefinition(0).getId(), 
                is(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING));
        assertThat(responseData.getAttributeDefinition(0).getDefaultValue(0), is("hello"));
        assertThat(responseData.getAttributeDefinition(0).getRequired(), is(false));
    }
    
    /**
     * Test that GetMetaTypeInfoRequest returns a list of meta type information.
     */
    @Test
    public void testGetMetaTypeInformation() throws UnknownHostException, IOException
    {
        //get connection to controller
        MessageListener listener = new MessageListener(m_Socket);
        
        //construct request message
        GetMetaTypeInfoRequestData request = GetMetaTypeInfoRequestData.newBuilder().build();
        TerraHarvestMessage message = createMetaTypeMessage(MetaTypeMessageType.GetMetaTypeInfoRequest, request);
        
        //send message
        message.writeDelimitedTo(m_Socket.getOutputStream());
        
        Message messageRcvd = listener.waitForMessage(Namespace.MetaType, 
                MetaTypeMessageType.GetMetaTypeInfoResponse, 1200);
        
        MetaTypeNamespace namespace = (MetaTypeNamespace)messageRcvd;
        GetMetaTypeInfoResponseData response = GetMetaTypeInfoResponseData.parseFrom(namespace.getData());
        List<MetaTypeInfoType> metaList = response.getMetaTypeList();
        List<String> pidList = new ArrayList<String>();
        for (MetaTypeInfoType meta: metaList)
        {
            pidList.add(meta.getPid());
        }
        assertThat(pidList, hasItem(ExampleClass.class.getName()));
        
        MetaTypeInfoType exampleConfigFactory = 
                metaList.get(pidList.indexOf(ExampleClass.class.getName()));
        assertThat(exampleConfigFactory.getPid(), is(ExampleClass.class.getName()));
        
        List<AttributeDefinitionType> attrList = exampleConfigFactory.getAttributesList();
        List<String> keyList = new ArrayList<String>();
        for (AttributeDefinitionType attr: attrList)
        {
            keyList.add(attr.getId());
        }
        
        assertThat(keyList, hasItems(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE, 
                ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING, 
                ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_STRING_VALUE_AGAIN,
                ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_REQUIRED_STRING_VALUE));
        
        AttributeDefinitionType attribute = attrList.get(keyList.indexOf(
                ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE));
        assertThat(attribute.getName(), is("Example Config Value"));
        assertThat(attribute.getDefaultValue(0), is("1"));
        assertThat(attribute.getOptionLabelList(), is(Arrays.asList("Label1", "Label2", "Label3", "Label4")));
        assertThat(attribute.getOptionValueList(), is(Arrays.asList("1", "2", "3", "4")));
        assertThat(attribute.getRequired(), is(false));
        
        attribute = attrList.get(keyList.indexOf(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING));
        assertThat(attribute.getName(), is("Example Config String Value"));
        assertThat(attribute.getDefaultValue(0), is("hello"));
        assertThat(attribute.getRequired(), is(false));
        
        attribute = attrList.get(keyList.indexOf(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_STRING_VALUE_AGAIN));
        assertThat(attribute.getName(), is("Another Config String Value"));
        assertThat(attribute.getDefaultValue(0), is("goodbye"));
        assertThat(attribute.getRequired(), is(false));
        
        attribute = attrList.get(keyList.indexOf(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_REQUIRED_STRING_VALUE));
        assertThat(attribute.getName(), is("Required Config String Value"));
        assertThat(attribute.getDefaultValueCount(), is(0));
        assertThat(attribute.getRequired(), is(true));
    }
    
    /**
     * Test that GetAttributeKeysRequest returns a list of attribute keys.
     * 
     * Verify that the list of keys contains that expected values.
     */
    @Test
    public void testGetAttributeKeysRequest() throws UnknownHostException, IOException, InterruptedException
    {
        //find the example metatype bundle
        long bundleId = 
                BundleNamespaceUtils.getBundleBySymbolicName(EXAMPLE_METATYPE_BUNDLE_NAME, m_Socket);

        //construct request
        GetAttributeKeysRequestData request = GetAttributeKeysRequestData.newBuilder().
            setBundleId(bundleId).setPid(ExampleClass.class.getName()).build();
        TerraHarvestMessage message = createMetaTypeMessage(MetaTypeMessageType.GetAttributeKeyRequest, request);

        //send message
        message.writeDelimitedTo(m_Socket.getOutputStream());

        //read in response
        TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(m_Socket.getInputStream());
        TerraHarvestPayload payLoadTest = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());

        assertThat(response, is(notNullValue()));
        assertThat(payLoadTest.getNamespace(), is(Namespace.MetaType));
        //parse specific message type
        MetaTypeNamespace namespaceResponse = MetaTypeNamespace.parseFrom(payLoadTest.getNamespaceMessage());
        assertThat(namespaceResponse.getType(), is(MetaTypeMessageType.GetAttributeKeyResponse));
        //verify
        GetAttributeKeysResponseData responseData = GetAttributeKeysResponseData.parseFrom(namespaceResponse.getData());
        assertThat(responseData.getKeyCount(), is(4));
        assertThat(responseData.getKeyList(), hasItems(ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE, 
                ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_VALUE_STRING, 
                ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_STRING_VALUE_AGAIN,
                ExampleClassConfig.CONFIG_PROP_EXAMPLE_CONFIG_REQUIRED_STRING_VALUE));
    }
    
    public static TerraHarvestMessage createMetaTypeMessage(final MetaTypeMessageType type, final Message message)
    {
        MetaTypeNamespace.Builder metaMessageBuilder = null;
        switch(type)
        {
            case GetBundlePidsRequest:
            {                
                metaMessageBuilder = MetaTypeNamespace.newBuilder().setType(type).setData(message.toByteString());
                break;
            }
            case GetAttributeKeyRequest:
            {
                metaMessageBuilder = MetaTypeNamespace.newBuilder().setType(type).setData(message.toByteString());
                break;
            }
            case GetAttributeDefinitionRequest:
            {
                metaMessageBuilder = MetaTypeNamespace.newBuilder().setType(type).setData(message.toByteString());
                break;
            }
            case GetMetaTypeInfoRequest:
                metaMessageBuilder = MetaTypeNamespace.newBuilder().setType(type).setData(message.toByteString());
                break;
            default:
                break;
        }

        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMsg(Namespace.MetaType,
            metaMessageBuilder);
            
        return thMessage;
    }
}
