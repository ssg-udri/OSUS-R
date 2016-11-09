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
package mil.dod.th.ose.gui.webapp.utils;

/**
 * This class defines navigation constants. In order for these constants to work properly the redirect end-points need
 * to be defined in the faces-config.xml
 * @author callen
 *
 */
public final class NavigationConstants
{
    /**
     * Navigation constant for when a service fails to complete as expected and there is a redirection as the outcome.
     */
    final public static String FAILURE = "fail";
    
    /**
     * Navigation constant for when a service successfully completes a request and there is a redirection as the 
     * outcome.
     */
    final public static String SUCCESS = "success";
    
    /**
     * Defined to prevent instantiation.
     */
    private NavigationConstants()
    {
        //utility class constructor
    }
}
