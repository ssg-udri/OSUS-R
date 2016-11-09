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
package edu.udayton.udri.asset.axis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.OrientationOffset;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;

/**
 * @author allenchl
 *
 */
public class TestCommandProcessorImpl
{
    private CommandProcessorImpl m_SUT = new CommandProcessorImpl();
    private String m_Ip = "ip";
    
    /**
     * Verify expected command URL is returned for:
     * MODE: pan
     */
    @Test
    public void testSetPanOnly() throws CommandExecutionException
    {
        //Command
        SetPanTiltCommand command = new SetPanTiltCommand().withPanTilt(SpatialTypesFactory.newOrientationOffset(3));
        
        String urlString = m_SUT.processSetPanTilt(command, m_Ip);
        
        assertThat(urlString.contains("pan=3"), is(true));
        assertThat(urlString.contains("tilt="), is(false)); //shouldn't contain the tilt.. mode is pan
    }
    
    /**
     * Verify expected command URL is returned for:
     * MODE: tilt
     */
    @Test
    public void testSetTiltOnly() throws CommandExecutionException
    {
        //Command
        SetPanTiltCommand command = new SetPanTiltCommand().withPanTilt(
               new OrientationOffset().withElevation(new ElevationDegrees().withValue(6)));
        
        String urlString = m_SUT.processSetPanTilt(command, m_Ip);
        
        assertThat(urlString.contains("pan="), is(false)); //shouldn't contain the pan.. mode is tilt
        assertThat(urlString.contains("tilt=6"), is(true)); 
    }
    
    /**
     * Verify expected command URL is returned for:
     * MODE: pan/tilt
     */
    @Test
    public void testSetPanTilt() throws CommandExecutionException
    {
        //Command
        SetPanTiltCommand command = 
                new SetPanTiltCommand().withPanTilt(SpatialTypesFactory.newOrientationOffset(77, 88));
        
        String urlString = m_SUT.processSetPanTilt(command, m_Ip);
        
        assertThat(urlString.contains("pan=77"), is(true));
        assertThat(urlString.contains("tilt=88"), is(true));
    }
}
