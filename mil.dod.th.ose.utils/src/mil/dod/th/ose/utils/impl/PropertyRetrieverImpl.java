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
package mil.dod.th.ose.utils.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.ose.utils.PropertyRetriever;

/**
 * Implementation of the service which creates a property object from a given resource.
 * @author callen
 *
 */
@Component
public class PropertyRetrieverImpl implements PropertyRetriever
{
    @Override
    public Properties getPropertiesFromUrl(final URL propertiesURL) throws IOException
    {
        //pull out the information and load into property object
        final Properties properties = new Properties();
        final URLConnection connection = propertiesURL.openConnection();
        try (InputStream stream = connection.getInputStream())
        {
            properties.load(stream);
            return properties;
        }
    }

    @Override
    public Properties getPropertiesFromFile(final String fileDirectory, final String fileName) throws IOException
    {
        final Properties properties = new Properties();
        
        final File configurationFile = new File(fileDirectory, fileName);
        
        try (FileInputStream inputStream = new FileInputStream(configurationFile))
        {
            properties.load(inputStream);
            return properties;
        }
    }
}
