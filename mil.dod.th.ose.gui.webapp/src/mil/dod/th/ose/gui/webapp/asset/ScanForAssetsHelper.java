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

/**
 * Interface used to assist with scanning for assets.
 * 
 * @author cweisenborn
 */
public interface ScanForAssetsHelper
{
    /**
     * Request the controller with the given ID to scan for any connected assets.
     * @param controllerId
     *      the system id of the controller to request to scan for assets
     */
    void requestScanForAssets(int controllerId);
}
