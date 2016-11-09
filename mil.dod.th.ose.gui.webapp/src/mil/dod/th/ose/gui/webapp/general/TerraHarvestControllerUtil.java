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
package mil.dod.th.ose.gui.webapp.general;

import java.util.List;
import java.util.Map;

/**
 * This class enables the retrieval of system specific information as it pertains 
 * to the platform running the web gui. This information includes things like 
 * system name, id, and version.
 * @author nickmarcucci
 *
 */
public interface TerraHarvestControllerUtil
{
    /**
     * Function to hold the value of input box for the system name of the current system.
     * 
     * @param name
     *  the name of the current system.
     */
    void setSystemName(final String name);
    
    /**
     * Function to retrieve value for the system name of the current system that is displayed
     * to the user.
     * @return
     *  the human readable name for the current system.
     */
    String getSystemName();
    
    /**
     * Function to hold the value of the input box for the unique identifier of the current 
     * system.
     * @param systemId
     *  the unique identifier for the current system.
     */
    void setSystemId(final int systemId);
    
    /**
     * Function to retrieve value for the unique identifier of the current system.
     * @return
     *  the unique identifier for the current system.
     */
    int getSystemId();
    
    /**
     * Function to return the current version of the current system.
     * @return
     *  the version of the current system.
     */
    String getSystemVersion();
    
    /**
     * Function to return the build information of the system.
     * @return
     *  the build information of the current system.
     */
    Map<String, String> getSystemBuildInformation();
    
    /**
     * Function to return the build information keys belonging to the system.
     * @return
     *  the build information keys of the current system.
     */
    List<String> getSystemBuildInformationKeys();
    
    /**
     * Function used to alter the system name and id.
     */
    void setSystemNameAndId();
}
