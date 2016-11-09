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
package mil.dod.th.core.types.factory;

import mil.dod.th.core.types.spatial.AzimuthDegrees;
import mil.dod.th.core.types.spatial.BankDegrees;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.DistanceMeters;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.Ellipse;
import mil.dod.th.core.types.spatial.HaeMeters;
import mil.dod.th.core.types.spatial.HeadingDegrees;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.spatial.OrientationOffset;

/**
 * Contains helper methods to make it easier to create spatial types.
 * 
 * @author dhumeniuk
 *
 */
//classes referenced are simple data type and classes sole purpose is to decouple other classes using spatial types
@SuppressWarnings("classdataabstractioncoupling")
public final class SpatialTypesFactory
{
    /**
     * Hidden constructor to prevent instantiation.
     */
    private SpatialTypesFactory()
    {
        
    }
    
    /**
     * Create coordinates object with given values.  Altitude will not be filled out.
     * 
     * @param longWgsDegrees
     *      longitude value
     * @param latWgsDegrees
     *      latitude value
     * @return
     *      coordinates with given values
     */
    public static Coordinates newCoordinates(final double longWgsDegrees, final double latWgsDegrees)
    {
        return new Coordinates()
             .withLongitude(new LongitudeWgsDegrees().withValue(longWgsDegrees))
             .withLatitude(new LatitudeWgsDegrees().withValue(latWgsDegrees));
    }
    
    /**
     * Create coordinates object with given values.
     * 
     * @param longWgsDegrees
     *      longitude value
     * @param latWgsDegrees
     *      latitude value
     * @param haeMeters
     *      altitude value
     * @return
     *      coordinates with given values
     */
    public static Coordinates newCoordinates(final double longWgsDegrees, final double latWgsDegrees, 
            final double haeMeters)
    {
        return new Coordinates()
             .withLongitude(new LongitudeWgsDegrees().withValue(longWgsDegrees))
             .withLatitude(new LatitudeWgsDegrees().withValue(latWgsDegrees))
             .withAltitude(new HaeMeters().withValue(haeMeters));
    }
    
    /**
     * Create coordinates with basic values for error ellipse.
     * 
     * @param longWgsDegrees
     *      longitude value
     * @param latWgsDegrees
     *      latitude value
     * @param haeMeters
     *      altitude value
     * @param ellipse
     *      error ellipse
     * @return
     *      coordinates with given values
     */
    public static Coordinates newCoordinates(final double longWgsDegrees, final double latWgsDegrees, 
            final double haeMeters,
            final Ellipse ellipse)
    {
        return new Coordinates()
             .withLongitude(new LongitudeWgsDegrees().withValue(longWgsDegrees))
             .withLatitude(new LatitudeWgsDegrees().withValue(latWgsDegrees))
             .withAltitude(new HaeMeters().withValue(haeMeters))
             .withEllipseRegion(ellipse);
    }
    
    /**
     * Create Ellipse object with given values.
     * 
     * @param semiMajorAxisMeters
     *      semi-major axis value
     * @param semiMinorAxisMeters
     *      semi-minor axis value
     * @param rotationDegrees
     *      rotation of the ellipse
     * @return
     *      ellipse with given values
     */
    public static Ellipse newEllipse(final double semiMajorAxisMeters, final double semiMinorAxisMeters, 
            final double rotationDegrees)
    {
        return new Ellipse()
            .withSemiMajorAxis(new DistanceMeters().withValue(semiMajorAxisMeters))
            .withSemiMinorAxis(new DistanceMeters().withValue(semiMinorAxisMeters))
            .withRotation(new HeadingDegrees().withValue(rotationDegrees));
    }

    /**
     * Create orientation object with given values. 
     * 
     * @param headingDegrees
     *      heading of the orientation
     * @param elevationDegrees
     *      elevation of the orientation
     * @param bankDegrees
     *      bank of the orientation
     * @return
     *      orientation with given values
     */
    public static Orientation newOrientation(final double headingDegrees, final double elevationDegrees,
            final double bankDegrees)
    {
        return new Orientation()
             .withHeading(new HeadingDegrees().withValue(headingDegrees))
             .withElevation(new ElevationDegrees().withValue(elevationDegrees))
             .withBank(new BankDegrees().withValue(bankDegrees));
    }

    /**
     * Create orientation offset with given values.
     * 
     * @param azimuthDegrees
     *      azimuth of the orientation
     * @param elevationDegrees
     *      elevation of the orientation
     * @param bankDegrees
     *      bank of the orientation
     * @return
     *      orientation with given values
     */
    public static OrientationOffset newOrientationOffset(final double azimuthDegrees, final double elevationDegrees,
             final double bankDegrees)
    {
        return new OrientationOffset()
            .withAzimuth(new AzimuthDegrees().withValue(azimuthDegrees))
            .withElevation(new ElevationDegrees().withValue(elevationDegrees))
            .withBank(new BankDegrees().withValue(bankDegrees));
    }

    /**
     * Create orientation offset without setting bank.
     * 
     * @param azimuthDegrees
     *      azimuth of the orientation
     * @param elevationDegrees
     *      elevation of the orientation
     * @return
     *      orientation with given values
     */
    public static OrientationOffset newOrientationOffset(final double azimuthDegrees, final double elevationDegrees)
    {
        return new OrientationOffset()
            .withAzimuth(new AzimuthDegrees().withValue(azimuthDegrees))
            .withElevation(new ElevationDegrees().withValue(elevationDegrees));
    }
    
    /**
     * Create orientation offset with azimuth value only.
     * 
     * @param azimuthDegrees
     *      azimuth of the orientation
     * @return
     *      orientation with given values
     */
    public static OrientationOffset newOrientationOffset(final double azimuthDegrees)
    {
        return new OrientationOffset().withAzimuth(new AzimuthDegrees().withValue(azimuthDegrees));
    }
}
