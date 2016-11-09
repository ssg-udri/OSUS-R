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
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import java.util.UUID;

import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.types.command.CommandTypeEnum;

import org.junit.Before;
import org.junit.Test;

/**
 * Verifies all base methods of an asset command work properly.
 * @author nickmarcucci
 *
 */
public class TestAbstractCommandModel
{
    private AbstractCommandModel m_SUT;
    private UUID m_Uuid;
    
    @Before
    public void init() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        CommandCapabilities caps = mock(CommandCapabilities.class);
        m_Uuid = UUID.randomUUID();
        m_SUT = new AssetSyncableCommandModel(m_Uuid, caps);
    }
    
    /**
     * Verify that the desired display name for a command is returned.
     */
    @Test
    public void testGetCommandDisplayName()
    {
        assertThat(m_SUT.getCommandDisplayName(CommandTypeEnum.SET_PAN_TILT_COMMAND), is("Pan Tilt"));
        
        assertThat(m_SUT.getCommandDisplayName(CommandTypeEnum.GET_PAN_TILT_COMMAND), is("Pan Tilt"));
        
        assertThat(m_SUT.getCommandDisplayName(CommandTypeEnum.DETECT_TARGET_COMMAND), is("Detect Target"));
    }
}
