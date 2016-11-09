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
package edu.udayton.udri.asset.gps;

import java.io.IOException;

import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.ccomm.physical.SerialPort;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.DistanceMeters;
import mil.dod.th.core.types.spatial.Ellipse;
import mil.dod.th.core.types.spatial.HaeMeters;
import mil.dod.th.core.types.spatial.HeadingDegrees;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GGASentence;
import net.sf.marineapi.nmea.sentence.GLLSentence;
import net.sf.marineapi.nmea.sentence.GSASentence;
import net.sf.marineapi.nmea.sentence.GSVSentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.SentenceValidator;
import net.sf.marineapi.nmea.util.Position;

import org.osgi.service.log.LogService;

/**
 * Thread that reads from the GPS device over a physical link.
 * 
 * @author timdale
 */
public class GpsObservationThread implements Runnable //NOCHECKSTYLE: Abstract Coupling is required.
{
    /**
     * Serial port that the GPS device runs on.
     */
    private SerialPort m_SerialPort;
    
    /**
     * AssetContext that this GPS device is meant to give data to.
     */
    private AssetContext m_Context;
  
    /**
     * Constructor for GpsObservation Thread.
     * @param serialPort - serial port that the GPS device runs on.
     * @param context - AssetContext that this GPS device is meant to give data to.
     */
    public GpsObservationThread(final SerialPort serialPort, final AssetContext context)
    {
        m_SerialPort = serialPort;
        m_Context = context;
    }
    
    @Override
    public void run()
    {
        if (m_SerialPort != null) 
        {
            try
            {
                final LineReader m_Reader = new LineReader(m_SerialPort.getInputStream());
                
                while (m_Context.getActiveStatus() == AssetActiveStatus.ACTIVATING 
                        || m_Context.getActiveStatus() == AssetActiveStatus.ACTIVATED)
                {
                    final String loc;
                    try
                    {
                        loc = m_Reader.readLine();
                        Logging.log(LogService.LOG_DEBUG, "READ LINE: " + loc);
                        final Coordinates currentLocation = parseNMEA(loc);
                       
                        if (currentLocation != null)
                        {
                            final Observation obs = new Observation();
                            obs.setAssetLocation(currentLocation);
                            m_Context.persistObservation(obs);
                        }
                    }
                    catch (final IOException e)
                    {
                        Logging.log(LogService.LOG_ERROR, e,
                                "IOException reading from the Serial Port connection for asset [%s].",
                                m_Context.getName());
                        m_Context.setStatus(SummaryStatusEnum.BAD,
                                "IOException reading from Serial Port - GPS device likely disconnected.");
                        m_Context.deactivateAsync();
                        m_Reader.close();
                    }
                    catch (final PersistenceFailedException e)
                    {
                        Logging.log(LogService.LOG_ERROR, e,
                                "PersistenceFailedException reading from the"
                                + " Serial Port connection for asset [%s].",
                                m_Context.getName());
                    }
                    catch (final ValidationFailedException e)
                    {
                        Logging.log(LogService.LOG_ERROR, e,
                                "ValidationFailedException reading from the Serial Port connection for asset [%s].",
                                m_Context.getName());
                    }
                    
                }
                
                //Close reader after asset is no longer active
                m_Reader.close();
            }
            catch (final PhysicalLinkException e) //Deactivates if the physical link was never set.
            {
                Logging.log(LogService.LOG_ERROR, e,
                        "PhysicalLinkException reading from the Serial Port connection for asset [%s].",
                        m_Context.getName());
                m_Context.setStatus(SummaryStatusEnum.BAD,
                        "Throwable thrown reading from Serial Port - GPS device likely not connected properly.");
                m_Context.deactivateAsync();
            }
            catch (final IOException e) //Prevents crashing when GPS device is unplugged while running.
            {
                Logging.log(LogService.LOG_ERROR, e,
                        "Throwable thrown reading from the Serial Port connection for asset [%s].",
                        m_Context.getName());
                m_Context.setStatus(SummaryStatusEnum.BAD,
                        "Throwable thrown reading from Serial Port - GPS device likely disconnected"
                        + "or connected improperly.");
                m_Context.deactivateAsync();
            }
        }
    }
    
    /**
     * Parses a NMEA string using Java Marine Sentences.
     * @param nmea - String that represtents a NMEA sentence.
     * @return Coordinates object - if a NMEA sentence does not provide enough data for a full Coordinates
     *      object a value of 0 will be assumed for the missing parameters.
     */
    private Coordinates parseNMEA(final String nmea)
    {
        final SentenceFactory factory = SentenceFactory.getInstance();
        Coordinates currentLocation = null;
        if (SentenceValidator.isValid(nmea)) 
        {
            final Sentence s = factory.createParser(nmea);
            Logging.log(LogService.LOG_DEBUG, "RAW: " + s.toString());
            Logging.log(LogService.LOG_DEBUG, "SID: " + s.getSentenceId());
              
            final LatitudeWgsDegrees lat = new LatitudeWgsDegrees();
            final LongitudeWgsDegrees lon = new LongitudeWgsDegrees();
            final HaeMeters alt = new HaeMeters();
            
            final HeadingDegrees heading = new HeadingDegrees();
            heading.setValue(0); 
            final DistanceMeters semiMajorAxis = new DistanceMeters();
            semiMajorAxis.setValue(0);
            final DistanceMeters semiMinorAxis = new DistanceMeters();
            semiMinorAxis.setValue(0);
            final Ellipse ellipseRegion = new Ellipse(semiMajorAxis, semiMinorAxis, heading);
          
            try
            {
                switch (s.getSentenceId()) 
                {
                    //You can read more about NMEA sentence types here http://www.gpsinformation.org/dale/nmea.htm#nmea
                    //RMC is speed/velocity and position (recommended minimum)
                    case "RMC": 
                        currentLocation = new Coordinates();
                        final RMCSentence rmc = (RMCSentence)factory.createParser(nmea); 
                        Logging.log(LogService.LOG_DEBUG, "RMC TRANSLATED: " + rmc.getPosition().toString());
                        Logging.log(LogService.LOG_DEBUG, "RMC ALL (TRANSLATED?): " + rmc.toString());
                        final Position pos = rmc.getPosition();
                          
                        lat.setValue(pos.getLatitude());
                        lon.setValue(pos.getLongitude());
                        alt.setValue(pos.getAltitude());
                        heading.setValue(rmc.getCourse());
                          
                        currentLocation.setLatitude(lat);
                        currentLocation.setLongitude(lon);
                        currentLocation.setAltitude(alt);
                          
                        ellipseRegion.setRotation(heading);
                        currentLocation.setEllipseRegion(ellipseRegion);
                        break;
                    //GGA fix information
                    case "GGA":
                        final GGASentence gga = (GGASentence)factory.createParser(nmea);
                        Logging.log(LogService.LOG_DEBUG, "GGA TRANSLATED: " + gga.getPosition().toString());
                        Logging.log(LogService.LOG_DEBUG, "GGA Satellite Count: " + gga.getSatelliteCount());
                        Logging.log(LogService.LOG_DEBUG, "GGA Fix Quality: " + gga.getFixQuality().toString());
                          
                        break;
                    //GSA satellite data    
                    case "GSA":
                        final GSASentence gsa = (GSASentence)factory.createParser(nmea);
                        Logging.log(LogService.LOG_DEBUG, "GSA TRANSLATED: " + gsa.toString());
                        final double gsaHorizontalDOP = gsa.getHorizontalDOP();
                        final double gsaVerticalDOP = gsa.getVerticalDOP();
                        final double gsaPositionDOP = gsa.getPositionDOP();
                          
                        Logging.log(LogService.LOG_DEBUG, "~~Dilution of Precision~~");
                        Logging.log(LogService.LOG_DEBUG, "Horizontal DOP: " + gsaHorizontalDOP);
                        Logging.log(LogService.LOG_DEBUG, "Vertical DOP: " + gsaVerticalDOP);
                        Logging.log(LogService.LOG_DEBUG, "Positional DOP: " + gsaPositionDOP);
                          
                        break;
                    //GSV detailed satellite info
                    case "GSV":
                        final GSVSentence gsv = (GSVSentence)factory.createParser(nmea);
                        Logging.log(LogService.LOG_DEBUG, "GSV TRANSLATED: " + gsv.toString());
                        Logging.log(LogService.LOG_DEBUG, "GSV Satellite Count: " + gsv.getSatelliteCount());
                        try
                        {
                            Logging.log(LogService.LOG_DEBUG, "GSV Satellite Info: " 
                                    + gsv.getSatelliteInfo().toString());
                        }
                        catch (final IndexOutOfBoundsException e)
                        {
                            Logging.log(LogService.LOG_ERROR, 
                                    "NMEA Marine IndexOutOfBoundsException when counting satelites.", null, e);
                        }
                        break;
            
                    //GLL lat/lon data
                    case "GLL":
                        currentLocation = new Coordinates();
                        final GLLSentence gll = (GLLSentence)factory.createParser(nmea);
                        Logging.log(LogService.LOG_DEBUG, "GLL TRANSLATED: " + gll.getPosition().toString());
                          
                        final Position gllPos = gll.getPosition();
                        lat.setValue(gllPos.getLatitude());
                        lon.setValue(gllPos.getLongitude());
                        alt.setValue(gllPos.getAltitude());
                          
                        currentLocation.setLatitude(lat);
                        currentLocation.setLongitude(lon);
                        currentLocation.setAltitude(alt);
                        break;
                    default:
                        Logging.log(LogService.LOG_WARNING, "Unknown or unsupported NMEA sentence type detected.");
                        return null; 
                }
            } 
            catch (final DataNotAvailableException e)
            {
                Logging.log(LogService.LOG_WARNING, "Incomplete NMEA Sentence "
                        + "- Please move to a location with better satelite service.");
                return null;
            }
        }
        return currentLocation;
    }
}
