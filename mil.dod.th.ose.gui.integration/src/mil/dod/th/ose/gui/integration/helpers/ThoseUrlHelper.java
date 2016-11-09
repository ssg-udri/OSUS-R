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
package mil.dod.th.ose.gui.integration.helpers;

/**
 * @author Dave Humeniuk
 *
 */
public class ThoseUrlHelper
{
    /**
     * System property that holds the base port for the GUI server.
     */
    public static final String PORTBASE_PROP_NAME = "mil.dod.th.ose.gui.portbase";
    
    /**
     * Default port base to use if system property is not set.
     */
    public static final String DEFAULT_PORTBASE = "8100";
    
    /**
     * Instance port is the portbase + this offset.
     */
    private static final int INSTANCE_PORT_OFFSET = 80;
    
    public static int getInstancePort()
    {
        String portbaseStr = System.getProperty(PORTBASE_PROP_NAME);
        
        if (portbaseStr == null)
        {
            portbaseStr = DEFAULT_PORTBASE;
        }
        
        return Integer.parseInt(portbaseStr) + INSTANCE_PORT_OFFSET;
    }   
}
