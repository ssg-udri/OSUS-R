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
package mil.dod.th.ose.core.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.util.UUID;

import javax.xml.bind.JAXBException;

import junit.framework.TestCase;

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.asset.commands.SetPanTiltResponse;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.model.commands.AddressedCommand;
import mil.dod.th.model.commands.AddressedResponse;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.google.common.base.CaseFormat;

/**
 * @author allenchl
 *
 */
public class TestAddressedCommandResponse extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    /**
     * Test the creation of an {@link AddressedCommand} with a specific type of command.
     * Verify the created object can be marshalled, unmarshalled and validated.
     */
    public void testAddressedCommand() throws JAXBException, ValidationFailedException
    {
        UUID uuid = UUID.randomUUID();
        int sourceId = 1;
        int destId = 2;
        UUID assetUuid = UUID.randomUUID();
        SetPanTiltCommand command = new SetPanTiltCommand().withPanTilt(
                SpatialTypesFactory.newOrientationOffset(2.0, 3.0));
        AddressedCommand addrCommand = new AddressedCommand(uuid, sourceId, destId, assetUuid, command);
        XmlMarshalService marshalService = ServiceUtils.getService(m_Context, XmlMarshalService.class);
        byte[] data = marshalService.createXmlByteArray(addrCommand, false);

        XmlUnmarshalService unmarshalService = ServiceUtils.getService(m_Context, XmlUnmarshalService.class);
        AddressedCommand unmarshalledAddrCommand = unmarshalService.getXmlObject(AddressedCommand.class, data);
        assertThat(unmarshalledAddrCommand.getAssetUuid(), is(assetUuid));
        assertThat(unmarshalledAddrCommand.getUuid(), is(uuid));
        assertThat(unmarshalledAddrCommand.getSourceId(), is(sourceId));
        assertThat(unmarshalledAddrCommand.getDestId(), is(destId));
        SetPanTiltCommand unMarshCommand = (SetPanTiltCommand)unmarshalledAddrCommand.getCommand();
        assertThat(unMarshCommand, is(command));
        assertThat(unMarshCommand.getPanTilt().getAzimuth().getValue(), is(2.0));
        assertThat(unMarshCommand.getPanTilt().getElevation().getValue(), is(3.0));

        //try validating
        Validator validator = ServiceUtils.getService(m_Context, Validator.class);
        validator.validate(addrCommand);
    }
    
    /**
     * Test the creation of an {@link AddressedResponse} with a specific type of response.
     * Verify the created object can be marshalled, unmarshalled, and validated.
     */
    public void testAddressedResponse() throws JAXBException, ValidationFailedException
    {
        UUID uuid = UUID.randomUUID();
        int sourceId = 1;
        int destId = 2;
        UUID assetUuid = UUID.randomUUID();
        MapEntry entry = new MapEntry("key", 2);
        SetPanTiltResponse response = new SetPanTiltResponse();
        response.withReserved(entry);
        AddressedResponse addrResponse = new AddressedResponse(sourceId, destId, assetUuid, response, uuid);
        XmlMarshalService marshalService = ServiceUtils.getService(m_Context, XmlMarshalService.class);
        byte[] data = marshalService.createXmlByteArray(addrResponse, false);

        XmlUnmarshalService unmarshalService = ServiceUtils.getService(m_Context, XmlUnmarshalService.class);
        AddressedResponse unmarshalledAddrResponse = unmarshalService.getXmlObject(AddressedResponse.class, data);
        assertThat(unmarshalledAddrResponse.getAssetUuid(), is(assetUuid));
        assertThat(unmarshalledAddrResponse.getCommandUuid(), is(uuid));
        assertThat(unmarshalledAddrResponse.getSourceId(), is(sourceId));
        assertThat(unmarshalledAddrResponse.getDestId(), is(destId));
        SetPanTiltResponse unMarshResponse = (SetPanTiltResponse)unmarshalledAddrResponse.getResponse();
        assertThat(unMarshResponse, is(response));
        assertThat(unMarshResponse.getReserved(), hasItem(entry));

        //try validating
        Validator validator = ServiceUtils.getService(m_Context, Validator.class);
        validator.validate(addrResponse);
    }
    
    /**
     * Verify all commands sources are available within a {@link AddressedCommand}.
     */
    public void testCommandsAreAllPresent() throws ValidationFailedException, ClassNotFoundException, 
        InstantiationException, IllegalAccessException
    {
        //some dummy values to insert
        UUID uuid = UUID.randomUUID();
        int sourceId = 1;
        int destId = 2;
        UUID assetUuid = UUID.randomUUID();

        Validator validator = ServiceUtils.getService(m_Context, Validator.class);
        
        for (CommandTypeEnum commandType: CommandTypeEnum.values())
        {
            //capture the class
            final String commandTypeString = 
                    CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, commandType.toString());
            Class<?> clazz = Class.forName("mil.dod.th.core.asset.commands." + commandTypeString);
            Command command = (Command)clazz.newInstance();

            AddressedCommand addrCommand = new AddressedCommand(uuid, sourceId, destId, assetUuid, command);
            //validate
            try
            {
                validator.validate(addrCommand);
            }
            catch (ValidationFailedException e)
            {
                //Expected for certain commands as they have required fields so won't pass validation. If the source
                //for the command cannot be found an IllegalStateException will be thrown, thus causing this test
                //to fail.
            }
            catch (IllegalStateException e)
            {
                fail(String.format(
                        "Command schema was likely not found, ensure that the command %s in included in"
                        + " MasterCommands.xsd %s", command.getClass(), e));
            }
        }
    }

    /**
     * Verify that all response type sources are available within a {@link AddressedResponse}.
     */
    public void testResponsesAreAllPresent() throws ValidationFailedException, ClassNotFoundException, 
        InstantiationException, IllegalAccessException
    {
        //some dummy values to insert
        UUID uuid = UUID.randomUUID();
        int sourceId = 1;
        int destId = 2;
        UUID assetUuid = UUID.randomUUID();

        Validator validator = ServiceUtils.getService(m_Context, Validator.class);
        
        for (CommandResponseEnum responseType: CommandResponseEnum.values())
        {
            //capture the class
            final String responseTypeString = 
                    CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, responseType.toString());
            Class<?> clazz = Class.forName("mil.dod.th.core.asset.commands." + responseTypeString);
            Response response = (Response)clazz.newInstance();
            
            AddressedResponse addrResponse = new AddressedResponse(sourceId, destId, assetUuid, response, uuid);
            try
            {
                validator.validate(addrResponse);
            }
            catch (ValidationFailedException e)
            {
                //Expected for certain responses as they have required fields so won't pass validation. If the source
                //for the response cannot be found an IllegalStateException will be thrown, thus causing this test
                //to fail.
            }
            catch (IllegalStateException e)
            {
                fail(String.format("The response %s is not available to the Addressed Response, "
                        + "check the MasterResponses.xsd to ensure that the response schema is included. %s", 
                            clazz, e));
            }
        }
    }
}