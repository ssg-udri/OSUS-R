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
package mil.dod.th.ose.shared;

/**
 * General string utilities.
 * 
 * @author dhumeniuk
 *
 */
public final class StringUtils
{
    /**
     * Hidden default constructor to prevent instantiation.
     */
    private StringUtils()
    {
        
    }
    
    /**
     * Split up a camel case string by inserting spacing between parts.
     * 
     * @param string
     *      string to split up
     * @return
     *      split up string
     */
    public static String splitCamelCase(final String string) 
    {
        return string.replaceAll("(?<=[A-Z])(?=[A-Z][a-z])|(?<=[^A-Z])(?=[A-Z])|(?<=[A-Za-z])(?=[^A-Za-z])", " ");
    }
}
