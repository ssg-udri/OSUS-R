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
package mil.dod.th.ose.core.types.factory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Ellipse;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.spatial.OrientationOffset;

public class TestSpatialTypesFactory
{
    /**
     * Verify coordinates is created with correct longitude and latitude.
     */
    @Test
    public void testNewCoord()
    {
        Coordinates coords = SpatialTypesFactory.newCoordinates(5.0, 6.0);
        assertThat(coords.getLongitude().getValue(), closeTo(5.0, 0.00001));
        assertThat(coords.getLatitude().getValue(), closeTo(6.0, 0.00001));
    }
    
    /**
     * Verify coordinates is created with correct longitude, latitude and altitude.
     */
    @Test
    public void testNewCoordWithAlt()
    {
        Coordinates coords = SpatialTypesFactory.newCoordinates(5.0, 6.0, 9.0);
        assertThat(coords.getLongitude().getValue(), closeTo(5.0, 0.00001));
        assertThat(coords.getLatitude().getValue(), closeTo(6.0, 0.00001));
        assertThat(coords.getAltitude().getValue(), closeTo(9.0, 0.00001));
    }

    /**
     * Verify ellipse is created with correct values.
     */
    @Test
    public void testNewEllipse()
    {
        Ellipse ellipse = SpatialTypesFactory.newEllipse(1.0, 2.0, 3.0);
        assertThat(ellipse.getSemiMajorAxis().getValue(), closeTo(1.0, 0.00001));
        assertThat(ellipse.getSemiMinorAxis().getValue(), closeTo(2.0, 0.00001));
        assertThat(ellipse.getRotation().getValue(), closeTo(3.0, 0.00001));
    }

    /**
     * Verify coordinates is created with correct longitude, latitude, altitude and ellipse.
     */
    @Test
    public void testNewCoordWithAltAndEllipse()
    {
        Coordinates coords = SpatialTypesFactory.newCoordinates(5.0, 6.0, 9.0, 
                SpatialTypesFactory.newEllipse(1.0, 2.0, 3.0));
        assertThat(coords.getLongitude().getValue(), closeTo(5.0, 0.00001));
        assertThat(coords.getLatitude().getValue(), closeTo(6.0, 0.00001));
        assertThat(coords.getAltitude().getValue(), closeTo(9.0, 0.00001));
        assertThat(coords.getEllipseRegion().getSemiMajorAxis().getValue(), closeTo(1.0, 0.00001));
        assertThat(coords.getEllipseRegion().getSemiMinorAxis().getValue(), closeTo(2.0, 0.00001));
        assertThat(coords.getEllipseRegion().getRotation().getValue(), closeTo(3.0, 0.00001));
    }
    
    /**
     * Verify orientation is created with correct values.
     */
    @Test
    public void testNewOrientation()
    {
        Orientation orientation = SpatialTypesFactory.newOrientation(32.0, 11.5, 7.7);
        assertThat(orientation.getHeading().getValue(), closeTo(32.0, 0.00001));
        assertThat(orientation.getElevation().getValue(), closeTo(11.5, 0.00001));
        assertThat(orientation.getBank().getValue(), closeTo(7.7, 0.00001));
    }
    
    /**
     * Verify orientation offset is created with correct values.
     */
    @Test
    public void testNewOrientationOffset()
    {
        OrientationOffset orientation = SpatialTypesFactory.newOrientationOffset(9.8, 7.6, 5.4);
        assertThat(orientation.getAzimuth().getValue(), closeTo(9.8, 0.00001));
        assertThat(orientation.getElevation().getValue(), closeTo(7.6, 0.00001));
        assertThat(orientation.getBank().getValue(), closeTo(5.4, 0.00001));
    }
    
    /**
     * Verify orientation offset is created with correct values.
     */
    @Test
    public void testNewOrientationOffset_AzimuthElevationOnly()
    {
        OrientationOffset orientation = SpatialTypesFactory.newOrientationOffset(83.2, 104.2);
        assertThat(orientation.getAzimuth().getValue(), closeTo(83.2, 0.00001));
        assertThat(orientation.getElevation().getValue(), closeTo(104.2, 0.00001));
        assertThat(orientation.getBank(), is(nullValue()));
    }
    
    /**
     * Verify orientation offset is created with correct values.
     */
    @Test
    public void testNewOrientationOffset_AzimuthOnly()
    {
        OrientationOffset orientation = SpatialTypesFactory.newOrientationOffset(134.2);
        assertThat(orientation.getAzimuth().getValue(), closeTo(134.2, 0.00001));
        assertThat(orientation.getElevation(), is(nullValue()));
        assertThat(orientation.getBank(), is(nullValue()));
    }
}
