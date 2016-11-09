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
package mil.dod.th.ose.gui.webapp.observation;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import mil.dod.th.core.log.Logging;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;

import org.osgi.service.log.LogService;

/**
 * Class determines the appropriate image to display for a given related observation's observation type.
 * @author nickmarcucci
 *
 */
@ManagedBean (name = "obsImageService")
@RequestScoped
public class ObservationImageService
{
    /**
     * Constant for PNG string.
     */
    private static final String PNG_EXTENSION = ".png";
    
    /**
     * Constant for default image.
     */
    private static final String DEFAULT_IMAGE = "unknown" + PNG_EXTENSION;
    
   /**
    * Method to retrieve an image path for a related observation identified by the 
    * given index. Path returned points to the mini version of the image.
    * @param observation
    *   the gui observation model which holds the related observation type in question
    * @param index
    *   the index to find the related observation type at
    * @return
    *   the path of the image that is to be displayed for the related observation's observation type
    */
    public String tryGetMiniObservationImage(final GuiObservation observation, final int index)
    {
        final String path = "thoseIcons/observations/mini-icons/";
        
        if (observation != null && observation.getRelatedObservationModels() != null 
                && (index >= 0 && index < observation.getRelatedObservationModels().size()))
        {
            
            final RelatedObservationIdentity model = observation.getRelatedObservationModels().get(index);
            
            if (model != null && model.getObservationSubType() != null)
            {
                return path + model.getObservationSubType().toString().toLowerCase() + PNG_EXTENSION;
            }
        }
        else 
        {
            Logging.log(LogService.LOG_ERROR, "Invalid index, invalid gui observation, " 
                    + "or invalid list of related observations has prevented mini-icon from being retrieved.");
        }
        
        return path + DEFAULT_IMAGE;
    }
    
    /**
     * Method to retrieve an image path for a given observation type. 
     * Path returned points to the full version of the image.
     * @param type
     *  the type of the given observation
     * @return
     *  the path to the image that corresponds with the observation type.
     */
    public String tryGetObservationImage(final ObservationSubTypeEnum type)
    {
        final String path = "thoseIcons/observations/";
        if (type == null)
        {
            Logging.log(LogService.LOG_ERROR, 
                    "Invalid observation type when trying to find observation image.");
        }
        else
        {
            return path + type.toString().toLowerCase() + PNG_EXTENSION;
        }
        
        return path + DEFAULT_IMAGE;
    }
}
