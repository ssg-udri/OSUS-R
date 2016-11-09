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
package mil.dod.th.ose.gui.webapp.mp;

import java.util.List;

import org.primefaces.model.StreamedContent;

/**
 * Class that handles the calls made to the TemplateProgramManager by the mission setup page. The implementation
 * of this class should be request scoped to make sure that the information displayed on the mission setup page is 
 * always current.
 * 
 * @author cweisenborn
 */
public interface MissionRequest
{

    /**
     * Method that returns a list of all available missions with which the user can currently choose from.
     * 
     * @return
     *          A list of MissionModels which represents all missions available for the user to choose from.
     */
    List<MissionModel> getMissions();

    /**
     * Method that is called to stream the primary image stored for a mission template to the mission setup page as an 
     * image. Primary images are stored as byte arrays in the data store. The FacesContext is used to pass the template 
     * name to this method so that the appropriate template with which the image belongs to can be found.
     * 
     * @return
     *           The StreamedContent that represents the primary image stored for a mission template.
     */
    StreamedContent getStreamPrimaryImage();

    /**
     * Method that is called to stream a secondary image for mission template to the mission setup page as an image. 
     * Secondary images are stored as byte arrays in the data store. The FacesContext is used to pass the template name 
     * and a subscript value to this method so that the appropriate image may be streamed to the page.
     * 
     * @return
     *           The link StreamedContent that represents the secondary image of a mission template.
     */
    StreamedContent getStreamSecondaryImage();

}
