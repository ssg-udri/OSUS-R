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
package mil.dod.th.ose.gui.api;

/**
 * Class that contains event property constants that are shared amongst various events within the GUI.
 * 
 * @author cweisenborn
 */
public final class SharedPropertyConstants
{
    /** Event property key for the controller ID. */
    public static final String EVENT_PROP_CONTROLLER_ID = "controller.id";
    
    /**
     * Defined to prevent instantiation.
     */
    private SharedPropertyConstants()
    {
        
    }
}
