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
package mil.dod.th.ose.gui.webapp.advanced;

import org.primefaces.model.UploadedFile;

/**
 * Class that handles installing and updating bundles on the currently active controller.
 * 
 * @author cweisenborn
 */
public interface BundleImportMgr
{

    /**
     * Set whether the bundle is to be started when updated or installed.
     * 
     * @param startBundle
     *          Boolean value of whether the bundle should be started when updated or installed.
     */
    void setStartBundle(boolean startBundle);

    /**
     * Retrieves the boolean value used to determine if the bundle should be started once updated or installed.
     * 
     * @return
     *          Boolean value used to determine if the bundle should be started.
     */
    boolean isStartBundle();

    /**
     * Sets the bundle file to updated or installed.
     * 
     * @param file
     *          bundle file to be set.
     */
    void setFile(UploadedFile file);

    /**
     * Retrieves the currently uploaded bundle file.
     * 
     * @return
     *          The bundle file to be updated or installed.
     */
    UploadedFile getFile();

    /**
     * Method that handles updating or installing a bundle on the currently active controller. This method will 
     * determine if the bundle already exists on the currently active controller. If it does exist, an update message
     * containing the bundle is sent to the controller. If it does not exist then an installed message containing the
     * bundle is sent to the controller.
     * 
     * @param controllerId
     *          the ID of the controller the bundle is being installed/updated on.
     */
    void handleImportBundle(int controllerId);
}
