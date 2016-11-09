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

import mil.dod.th.core.observation.types.Observation;

import org.primefaces.model.StreamedContent;

/**
 * Session scoped bean that handles displaying and downloading media files stored in observations. 
 * 
 * @author cweisenborn
 */
public interface ViewObsMgr
{

    /**
     * Method that sets the observation to be viewed.
     * 
     * @param observation
     *          The {@link Observation} that is to be viewed. 
     */
    void setObservation(Observation observation);

    /**
     * Method that returns the stored observation.
     * 
     * @return
     *      {@link Observation} that has been stored.
     */
    Observation getObservation();

    /**
     * Method that returns the stored media object.
     * 
     * @return
     *      {@link StreamedContent} that represents the media file.
     */
    StreamedContent getMedia();
    
    /**
     * Whether the viewer is able to determine the resolution.
     * 
     * @return
     *      true if able to determine the resolution, false if not
     */
    boolean hasResolution();

    /**
     * Returns the display height of the image.
     * 
     * @return
     *      Integer that represents the height of the image for display purposes.  Will return -1 if {@link 
     *      #hasResolution()} returns false.
     */
    int getHeight();

    /**
     * Returns the display width of the image.
     * 
     * @return
     *      Integer that represents the width of the image for display purposes.  Will return -1 if {@link 
     *      #hasResolution()} returns false.
     */
    int getWidth();

    /**
     * Method used to download the media file stored in the observation.
     * 
     * @return
     *      {@link StreamedContent} that represents the media file.
     */
    StreamedContent downloadMedia();

}
