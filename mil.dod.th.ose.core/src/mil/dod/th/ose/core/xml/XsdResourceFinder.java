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
package mil.dod.th.ose.core.xml;

import java.net.URL;

import org.osgi.framework.BundleContext;

/**
 * This class contains resources/methods commonly used by OSUS xml services.
 * @author allenchl
 *
 */
public final class XsdResourceFinder
{
    /**
     * Hidden constructor to prevent instantiation.
     */
    private XsdResourceFinder()
    {
        //hidden on purpose
    }
    
    /**
     * Find the XSD resource given the class type.  Method assumes an XSD file exists for the class and it is in the 
     * correct place given the package name.  For a class like a.b.Class, XSD file should be in a/b/Class.xsd
     * 
     * @param bundleContext
     *      The context of the bundle which is able to find the given class
     * @param clazz
     *      Class type to find the XSD for
     * @return
     *      URL of the path to the XSD
     * @throws IllegalStateException
     *      if the class doesn't have an associated XSD file
     */
    public static URL getXsdResource(final BundleContext bundleContext, final Class<?> clazz) 
            throws IllegalStateException 
    {          
        // XSD will be found in the schemas folder for all packages in mil.dod.th, where the rest of the package name
        // defines folders within the schemas folder including class name with .xsd added to the end
        // e.g., mil.dod.th.core.type.SomeType would be found in schemas/core/type/SomeType.xsd
        final String parentPath = clazz.getPackage().getName().replace("mil.dod.th", "schemas").replace('.', '/');
        final String xsdLocalPath = String.format("%s/%s.xsd", parentPath, clazz.getSimpleName());
        
        final URL resource = bundleContext.getBundle().getResource(xsdLocalPath);
        if (resource == null)
        {
            throw new IllegalStateException(
                    String.format("Unable to find the XSD for the %s, %s not in bundle %s", clazz, xsdLocalPath,
                            bundleContext.getBundle()));
        }
        return resource;      
    }
}
