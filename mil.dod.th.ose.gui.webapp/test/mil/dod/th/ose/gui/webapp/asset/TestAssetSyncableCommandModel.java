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
package mil.dod.th.ose.gui.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetProfilesCommand;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.AzimuthDegrees;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.OrientationOffset;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the {@link AssetSyncableCommandModel} class.
 * 
 * @author cweisenborn
 */
public class TestAssetSyncableCommandModel
{
    private AssetSyncableCommandModel m_SUT;
    private UUID m_Uuid;
    private CommandCapabilities m_Capabilities;
    
    @Before
    public void setup() throws Exception
    {
        m_Capabilities = mock(CommandCapabilities.class);
        
        List<CommandTypeEnum> enumList = Arrays.asList(CommandTypeEnum.values());
        when(m_Capabilities.getSupportedCommands()).thenReturn(enumList);
        
        m_Uuid = UUID.randomUUID();
        m_SUT = new AssetSyncableCommandModel(m_Uuid, m_Capabilities);
    }
    
    /**
     * Test the get UUID method.
     * Verify that the appropriate UUID is returned.
     */
    @Test
    public void testGetUuid()
    {
        assertThat(m_SUT.getUuid(), is(m_Uuid));
    }
    
    /**
     * Verify that the appropriate set of command types is returned.
     */
    @Test
    public void testRetrieveSupportedCommands()
    {
        List<CommandTypeEnum> commands = m_SUT.getSupportedCommands();
        assertThat(commands, containsInAnyOrder(CommandTypeEnum.CAPTURE_IMAGE_COMMAND, 
                CommandTypeEnum.DETECT_TARGET_COMMAND, CommandTypeEnum.SET_TUNE_SETTINGS_COMMAND, 
                CommandTypeEnum.SET_POINTING_LOCATION_COMMAND, CommandTypeEnum.SET_PAN_TILT_COMMAND, 
                CommandTypeEnum.SET_CAMERA_SETTINGS_COMMAND, CommandTypeEnum.CONFIGURE_PROFILE_COMMAND,
                CommandTypeEnum.SET_MODE_COMMAND, CommandTypeEnum.CREATE_ACTION_LIST_COMMAND, 
                CommandTypeEnum.TARGET_REFINEMENT_COMMAND, CommandTypeEnum.START_RECORDING_COMMAND,
                CommandTypeEnum.STOP_RECORDING_COMMAND));
    }
    
    /**
     * Verify that the get command by type method returns a command of the appropriate type. Any further calls should 
     * return the previously instantiated command.
     */
    @Test
    public void testGetCommandByType()
    {
        Command command = m_SUT.getCommandByType(CommandTypeEnum.SET_PAN_TILT_COMMAND);
        assertThat(command.getClass(), is((Object)SetPanTiltCommand.class));
        
        //Verify that it is a newly instantiated command and that the fields are not set.
        SetPanTiltCommand panTilt = (SetPanTiltCommand)command;
        assertThat(panTilt.isSetPanTilt(), is(false));
        
        //Set the fields.
        panTilt.setPanTilt(SpatialTypesFactory.newOrientationOffset(5.2, 12.1));
        
        command = m_SUT.getCommandByType(CommandTypeEnum.SET_PAN_TILT_COMMAND);
        assertThat(command.getClass(), is((Object)SetPanTiltCommand.class));
        
        //Verify that it is a newly instantiated command is not returned and that the values match the previously 
        //received command.
        panTilt = (SetPanTiltCommand)command;
        OrientationOffset ptValues = panTilt.getPanTilt();
        assertThat(panTilt.isSetPanTilt(), is(true));
        assertThat(ptValues.isSetElevation(), is(true));
        assertThat(ptValues.isSetAzimuth(), is(true));
        assertThat(ptValues.getAzimuth().getValue(), is(5.2));
        assertThat(ptValues.getElevation().getValue(), is(12.1));
    }
    
    /**
     * Verify that the get command by sync type method returns a command of the appropriate type. Any further calls 
     * should return the previously instantiated command.
     */
    @Test
    public void testGetCommandBySyncType()
    {
        Command command = m_SUT.getCommandBySyncType(CommandTypeEnum.GET_PAN_TILT_COMMAND);
        assertThat(command.getClass(), is((Object)SetPanTiltCommand.class));
        
        //Verify that it is a newly instantiated command and that the fields are not set.
        SetPanTiltCommand panTilt = (SetPanTiltCommand)command;
        assertThat(panTilt.isSetPanTilt(), is(false));
        
        //Set the fields.
        AzimuthDegrees pan = new AzimuthDegrees().withValue(5.2);
        ElevationDegrees tilt = new ElevationDegrees().withValue(12.1);
        panTilt.setPanTilt(new OrientationOffset().withAzimuth(pan).withElevation(tilt));
       
        command = m_SUT.getCommandBySyncType(CommandTypeEnum.GET_PAN_TILT_COMMAND);
        assertThat(command.getClass(), is((Object)SetPanTiltCommand.class));
        
        //Verify that it is a newly instantiated command is not returned and that the values match the previously 
        //received command.
        panTilt = (SetPanTiltCommand)command;
        OrientationOffset ptValues = panTilt.getPanTilt();
        assertThat(panTilt.isSetPanTilt(), is(true));
        assertThat(ptValues.isSetElevation(), is(true));
        assertThat(ptValues.isSetAzimuth(), is(true));
        assertThat(ptValues.getAzimuth(), is(pan));
        assertThat(ptValues.getElevation(), is(tilt));
    }
    
    /**
     * Verify that the appropriate boolean value is returned for a command that is able to sync and for a command that
     * is not able to sync.
     */
    @Test
    public void testCanSync()
    {
        //Pan tilt should be syncable therefore the canSync method should return true.
        assertThat(m_SUT.canSync(CommandTypeEnum.SET_PAN_TILT_COMMAND), is(true));
        
        //Detect target is a one way command and should not be syncable therefore the canSync method should return
        //false.
        assertThat(m_SUT.canSync(CommandTypeEnum.DETECT_TARGET_COMMAND), is(false));
    }
    
    /**
     * Verify that the appropriate sync display name is returned for a command.
     */
    @Test
    public void testGetSyncActionName()
    {
        assertThat(m_SUT.getSyncActionName(CommandTypeEnum.SET_PAN_TILT_COMMAND), is("Sync Pan Tilt"));
    }
    
    /**
     * Verify that the appropriate get command enumeration is returned for the specified set command enumeration or 
     * that null is returned a command with no associated get command.
     */
    @Test
    public void testGetCommandSyncTypeByType()
    {
        //Verify set command type returns appropriate get command type.
        assertThat(m_SUT.getCommandSyncTypeByType(CommandTypeEnum.SET_PAN_TILT_COMMAND), 
                is(CommandTypeEnum.GET_PAN_TILT_COMMAND));
        
        //Verify one way command returns no associated get command.
        assertThat(m_SUT.getCommandSyncTypeByType(CommandTypeEnum.DETECT_TARGET_COMMAND), is(nullValue()));
        
        //Verify that set tune settings command has no associated get command. This is a special case that
        //is handled by the constructor.
        assertThat(m_SUT.getCommandSyncTypeByType(CommandTypeEnum.SET_TUNE_SETTINGS_COMMAND), is(nullValue()));
    }
    
    /**
     * Verify that the appropriate set command enumeration is returned for the specified get command enumeration.
     * Verify null is returned if no set command is associated.
     */
    @Test
    public void testGetCommandTypeBySyncType()
    {
        //Verify get command type returns appropriate set command type.
        assertThat(m_SUT.getCommandTypeBySyncType(CommandTypeEnum.GET_PAN_TILT_COMMAND), 
                is(CommandTypeEnum.SET_PAN_TILT_COMMAND));
        
        //Verify null is returned for a command that has no set command.
        assertThat(m_SUT.getCommandTypeBySyncType(CommandTypeEnum.DETECT_TARGET_COMMAND), is(nullValue()));
    }
  
    /**
     * Verify that retrieving a command instance returns the proper type.
     */
    @Test
    public void testGetCommandInstanceByType() throws ClassNotFoundException, 
        InstantiationException, IllegalAccessException
    {
        Command command = m_SUT.getCommandInstanceByType(CommandTypeEnum.GET_PROFILES_COMMAND);
        
        assertThat(command, instanceOf(GetProfilesCommand.class));
    }
    
    /**
     * Verify that the update capabilities method updates the model appropriately.
     */
    @Test
    public void testUpdateCapabilities() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        //Get the pan tilt command and set fields for later verification.
        Command command = m_SUT.getCommandByType(CommandTypeEnum.SET_PAN_TILT_COMMAND);
        assertThat(command.getClass(), is((Object)SetPanTiltCommand.class));
        SetPanTiltCommand panTilt = (SetPanTiltCommand)command;
        panTilt.setPanTilt(SpatialTypesFactory.newOrientationOffset(5.2, 12.1));
        
        //Verify that the correct set of initial commands is returned.
        List<CommandTypeEnum> commands = m_SUT.getSupportedCommands();
        assertThat(commands, containsInAnyOrder(CommandTypeEnum.CAPTURE_IMAGE_COMMAND, 
                CommandTypeEnum.DETECT_TARGET_COMMAND, CommandTypeEnum.SET_TUNE_SETTINGS_COMMAND, 
                CommandTypeEnum.SET_POINTING_LOCATION_COMMAND, CommandTypeEnum.SET_PAN_TILT_COMMAND, 
                CommandTypeEnum.SET_CAMERA_SETTINGS_COMMAND, CommandTypeEnum.CONFIGURE_PROFILE_COMMAND,
                CommandTypeEnum.SET_MODE_COMMAND, CommandTypeEnum.CREATE_ACTION_LIST_COMMAND, 
                CommandTypeEnum.TARGET_REFINEMENT_COMMAND, CommandTypeEnum.START_RECORDING_COMMAND,
                CommandTypeEnum.STOP_RECORDING_COMMAND));
        
        List<CommandTypeEnum> supportedCommands = new ArrayList<CommandTypeEnum>();
        supportedCommands.add(CommandTypeEnum.GET_PAN_TILT_COMMAND);
        supportedCommands.add(CommandTypeEnum.SET_PAN_TILT_COMMAND);
        supportedCommands.add(CommandTypeEnum.GET_VERSION_COMMAND);
        
        final CommandCapabilities caps = mock(CommandCapabilities.class);
        when(caps.getSupportedCommands()).thenReturn(supportedCommands);
        
        //Update the capabilities for the model
        m_SUT.updateCapabilities(caps);
        
        //Verify that no longer supported commands are removed.
        commands = m_SUT.getSupportedCommands();
        assertThat(commands, containsInAnyOrder(CommandTypeEnum.SET_PAN_TILT_COMMAND));
        
        //Verify that the set pan tilt command still contains the values set earlier. Updating the capabilities should
        //not erase stored values for commands that are still supported.
        command = m_SUT.getCommandByType(CommandTypeEnum.SET_PAN_TILT_COMMAND);
        assertThat(command.getClass(), is((Object)SetPanTiltCommand.class));
        panTilt = (SetPanTiltCommand)command;
        OrientationOffset ptValues = panTilt.getPanTilt();
        assertThat(panTilt.isSetPanTilt(), is(true));
        assertThat(ptValues.isSetElevation(), is(true));
        assertThat(ptValues.isSetAzimuth(), is(true));
        assertThat(ptValues.getAzimuth().getValue(), is(5.2));
        assertThat(ptValues.getElevation().getValue(), is(12.1));
        
        supportedCommands.clear();
        supportedCommands.add(CommandTypeEnum.GET_PAN_TILT_COMMAND);
        supportedCommands.add(CommandTypeEnum.GET_VERSION_COMMAND);
        supportedCommands.add(CommandTypeEnum.DETECT_TARGET_COMMAND);
        supportedCommands.add(CommandTypeEnum.GET_PROFILES_COMMAND);
        supportedCommands.add(CommandTypeEnum.TARGET_REFINEMENT_COMMAND);
        
        //Update the capabilities again, this time adding as well as removing.
        m_SUT.updateCapabilities(caps);
        
        //Verify add commands are present and removed commands are not.
        commands = m_SUT.getSupportedCommands();
        assertThat(commands, containsInAnyOrder(CommandTypeEnum.DETECT_TARGET_COMMAND, 
                CommandTypeEnum.TARGET_REFINEMENT_COMMAND));
    }
}
