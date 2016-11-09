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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.types.command.CommandTypeEnum;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests all asset get command models.
 * 
 * @author nickmarcucci
 *
 */
public class TestAssetGetCommandModel
{
    private AssetGetCommandModel m_SUT;
    private UUID m_Uuid;
    
    @Before
    public void init()
    {
        m_Uuid = UUID.randomUUID();
    }
    
    /*
     * Verify that retrieved commands are correct.
     */
    @Test
    public void testGetSupportedCommands()
    {
        List<CommandTypeEnum> types = new ArrayList<CommandTypeEnum>();
        types.add(CommandTypeEnum.GET_VERSION_COMMAND);
        
        m_SUT = new AssetGetCommandModel(m_Uuid, types);
        
        List<CommandTypeEnum> answer = m_SUT.getSupportedCommands();
        assertThat(answer.size(), is(1));
        assertThat(answer.get(0), is(CommandTypeEnum.GET_VERSION_COMMAND));
    }
    
    /*
     * Verify that a command response can be retrieved by the given type.
     */
    @Test
    public void testGetCommandGetResponseByType()
    {
        List<CommandTypeEnum> types = new ArrayList<CommandTypeEnum>();
        types.add(CommandTypeEnum.GET_VERSION_COMMAND);
        
        m_SUT = new AssetGetCommandModel(m_Uuid, types);
        
        AssetGetCommandResponse commandResponse = m_SUT.getCommandResponseByType(CommandTypeEnum.GET_VERSION_COMMAND);
        assertThat(commandResponse, notNullValue());
    }
    
    /*
     * Verify that a response can be set for a given type.
     */
    @Test
    public void testTrySetResponseByType()
    {
        List<CommandTypeEnum> types = new ArrayList<CommandTypeEnum>();
        types.add(CommandTypeEnum.GET_VERSION_COMMAND);
        
        m_SUT = new AssetGetCommandModel(m_Uuid, types);
        
        Date date = new Date(1);
        Response response = mock(Response.class);
        boolean answer = m_SUT.trySetResponseByType(CommandTypeEnum.SET_TUNE_SETTINGS_COMMAND, response, date);
        
        assertThat(answer, is(false));
        
        answer = m_SUT.trySetResponseByType(CommandTypeEnum.GET_VERSION_COMMAND, response, date);
        assertThat(answer, is(true));
        
        AssetGetCommandResponse commandResponse = m_SUT.getCommandResponseByType(CommandTypeEnum.GET_VERSION_COMMAND);
        assertThat(commandResponse, notNullValue());
        
        final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss'Z'", Locale.getDefault());
        
        format.setTimeZone(TimeZone.getTimeZone("MM/dd/yyyy HH:mm:ss'Z'"));
        
        assertThat(commandResponse.printTimeMessage(), is(format.format(new Date(1))));
    }
    
    /**
     * Verify that the list of supported commands is updated appropriately.
     */
    @Test
    public void testUpdateSupportedCommands()
    {
        List<CommandTypeEnum> types = new ArrayList<CommandTypeEnum>();
        types.add(CommandTypeEnum.GET_VERSION_COMMAND);
        
        m_SUT = new AssetGetCommandModel(m_Uuid, types);
        
        List<CommandTypeEnum> answer = m_SUT.getSupportedCommands();
        assertThat(answer.size(), is(1));
        assertThat(answer.get(0), is(CommandTypeEnum.GET_VERSION_COMMAND));
        
        types.clear();
        types.add(CommandTypeEnum.GET_PROFILES_COMMAND);
        types.add(CommandTypeEnum.GET_TUNE_SETTINGS_COMMAND);
        
        m_SUT.updateSupportedCommands(types);
        
        answer = m_SUT.getSupportedCommands();
        assertThat(answer.size(), is(2));
        assertThat(answer, containsInAnyOrder(CommandTypeEnum.GET_PROFILES_COMMAND, 
                CommandTypeEnum.GET_TUNE_SETTINGS_COMMAND));
    }
}
