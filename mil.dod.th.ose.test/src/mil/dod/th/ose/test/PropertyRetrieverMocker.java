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
package mil.dod.th.ose.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import mil.dod.th.ose.utils.PropertyRetriever;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author dhumeniuk
 *
 */
public class PropertyRetrieverMocker
{
    /**
     * Create a mocked instance that will return the given properties.
     */
    public static PropertyRetriever mockIt(BundleContext context, String propertyFilename, Properties properties) 
        throws IOException
    {
        return mockIt(context, new String[] {propertyFilename}, new Properties[] {properties});
    }
        
    /**
     * Create a mocked instance that will return the given <b>array</b> of properties instead of one properties map.
     * 
     * This assumes a 1 to 1 mapping for the filename and properties array, e.g., index 0 file is for index 0 properties
     * map.
     */
    public static PropertyRetriever mockIt(BundleContext context, String[] propertyFilename, Properties[] properties) 
        throws IOException
    {
        PropertyRetriever retriever = mock(PropertyRetriever.class);
        
        //mock getting the resources
        Bundle bundle = mock(Bundle.class);
        when(context.getBundle()).thenReturn(bundle);
        
        for (int i=0; i < propertyFilename.length; i++)
        {
            URL url = new URL("file:///file" + propertyFilename[i]);
            when(bundle.getResource(propertyFilename[i])).thenReturn(url);
            when(retriever.getPropertiesFromUrl(url)).thenReturn(properties[i]);
        }
        
        return retriever;
    }
    
    /**
     * Mock the retriever to return the given properties based on the URL.
     */
    public static PropertyRetriever mockIt(URL url, Properties properties) throws IOException
    {
        PropertyRetriever retriever = mock(PropertyRetriever.class);
        
        when(retriever.getPropertiesFromUrl(url)).thenReturn(properties);
        
        return retriever;
    }
}
