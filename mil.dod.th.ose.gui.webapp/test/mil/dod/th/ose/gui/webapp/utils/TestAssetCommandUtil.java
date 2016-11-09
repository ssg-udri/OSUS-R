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
package mil.dod.th.ose.gui.webapp.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.lang.reflect.InvocationTargetException; //NOCHECKSTYLE - import needed

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetPositionCommand;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.remote.lexicon.asset.commands.GetPositionCommandGen;

import org.junit.Test;

import com.google.protobuf.Message;

/**
 * Verifies AssetCommandUtil functions work.
 * @author nickmarcucci
 *
 */
public class TestAssetCommandUtil
{
    /**
     * Verify that a command type enum can be converted into the string representation of the 
     * class name.
     */
    @Test
    public void testCommandTypeToClassName()
    {
        String answer = AssetCommandUtil.commandTypeToClassName(CommandTypeEnum.CAPTURE_IMAGE_COMMAND);
        
        assertThat(answer, is("CaptureImageCommand"));
    }
    
    /**
     * Verify given a command type enum the correct command instance is returned.
     */
    @Test
    public void testInstantiateCommandBasedOnType() throws ClassNotFoundException, 
        InstantiationException, IllegalAccessException
    {
        Command command = AssetCommandUtil.instantiateCommandBasedOnType(
                CommandTypeEnum.GET_POSITION_COMMAND);
        assertThat(command, notNullValue());
        assertThat(command, instanceOf(GetPositionCommand.class));
    }
    
    /**
     * Verify a given class name, the correct message builder instance will be returned.
     */
    @Test
    public void testCreateMessageBuilderFromCommandType() throws NoSuchMethodException, 
        SecurityException, ClassNotFoundException, IllegalAccessException, 
        IllegalArgumentException, InvocationTargetException
    {
        Message.Builder builder = AssetCommandUtil.
                createMessageBuilderFromCommandType(GetPositionCommandGen.GetPositionCommand.class);
        
        assertThat(builder, notNullValue());
        assertThat(builder, instanceOf(GetPositionCommandGen.GetPositionCommand.Builder.class));
        //Verify that the base command was set.
        assertThat(((GetPositionCommandGen.GetPositionCommand.Builder)builder).getBase(), notNullValue());
    }
}
