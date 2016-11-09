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
package edu.udayton.udri.asset.hikvision;

import static org.hamcrest.MatcherAssert.assertThat; //NOCHECKSTYLE
import static org.hamcrest.CoreMatchers.equalTo; //NOCHECKSTYLE
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import mil.dod.th.core.asset.commands.SetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.types.spatial.AzimuthDegrees;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.OrientationOffset;

/**
 * @author Noah
 *
 */
public class TestCommandProcessor
{
    private CommandProcessor m_SUT; 
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this); 
        m_SUT = new CommandProcessor();
    }
    
    @Test
    public void testGetZoom()
    {        
        SetCameraSettingsCommand setCameraSettings = new SetCameraSettingsCommand().withZoom(10f);
        int result = m_SUT.getZoom(setCameraSettings);
        assertThat(result, equalTo(10000));
    }
    
    @Test
    public void testGetAzimuth() throws IOException
    { 
        SetPanTiltCommand setPanTilt = new SetPanTiltCommand().withPanTilt(
                        new OrientationOffset().withAzimuth(new AzimuthDegrees().withValue(20)));
        SetPanTiltCommand setPanTtilt2 = new SetPanTiltCommand().withPanTilt(
                new OrientationOffset().withAzimuth(new AzimuthDegrees().withValue(-20)));
        
        int result = m_SUT.getAzimuth(setPanTilt);
        int result2 = m_SUT.getAzimuth(setPanTtilt2);
        
        assertThat(result, equalTo(20));
        assertThat(result2, equalTo(340)); 
    }
    
    @Test
    public void testGetElevation() throws IOException
    {
        SetPanTiltCommand setPanTilt = new SetPanTiltCommand().withPanTilt(
                new OrientationOffset().withElevation(new ElevationDegrees().withValue(20)));        
        int result = m_SUT.getElevation(setPanTilt);
        
        assertThat(result, equalTo(200));
    }
    
    @Test
    public void testIsAzimuthSet()
    {
        SetPanTiltCommand setPanTilt = new SetPanTiltCommand().withPanTilt(
                new OrientationOffset().withAzimuth(new AzimuthDegrees().withValue(20)));
        boolean result = m_SUT.isAzimuthSet(setPanTilt);
        
        assertThat(result, equalTo(true));
    }
    
    @Test
    public void testIsElevationSet()
    {
        SetPanTiltCommand setPanTilt = new SetPanTiltCommand().withPanTilt(
                new OrientationOffset().withElevation(new ElevationDegrees().withValue(20)));
        boolean result = m_SUT.isElevationSet(setPanTilt);
        
        assertThat(result, equalTo(true));
    }
}
