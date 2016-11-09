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
package mil.dod.th.ose.utils;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;



/**
 * Generate a property object from a URL. 
 * This is an OSGi provided service that assists with calls on to a bundle like getEntry(String) 
 * and other similar calls that return the URL of the resource embedded within the bundle. 
 * @author callen
 *
 */
public interface PropertyRetriever
{
    /**
     * Get the properties found at the given URL.
     * @param propertiesURL
     *     the URL where the properties resource is located
     * @return
     *     a properties object
     * @throws IOException
     *     if there is an error with opening the URL connection or other difficulties with obtaining the resources from
     *     the URL given
     */
    Properties getPropertiesFromUrl(final URL propertiesURL) throws IOException;
    
    /**
     * Get the properties found at the given file location.
     * @param fileDirectory
     *  the file directory that contains the properties to be read.
     * @param fileName
     *  the name of the properties file located in the file directory.
     * @return
     *  the properties object
     * @throws IOException
     *  if there is an error with opening the file or other difficulties with obtaining the resource from
     *  the file path given
     */
    Properties getPropertiesFromFile(final String fileDirectory, final String fileName) throws IOException;
}
