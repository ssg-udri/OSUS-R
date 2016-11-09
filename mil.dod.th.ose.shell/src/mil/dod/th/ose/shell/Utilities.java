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
package mil.dod.th.ose.shell;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.factory.FactoryDescriptor;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Utility class usable by all THOSE shell commands.
 * 
 * @author dhumeniuk
 *
 */
public final class Utilities
{
    /**
     * Private constructor to prevent instantiation.
     */
    private Utilities()
    {
        
    }
    
    /**
     * Print out all services references for the given interface class.
     * 
     * @param context
     *            Bundle context
     * @param out
     *            Where to emit output
     * @param clazz
     *            Interface class to lookup or null for all
     * @param showBundle
     *            Whether to show which bundle registered the service
     * @param showProperties
     *            Whether to show service registration properties
     */
    public static void printServiceImplementations(final BundleContext context, final PrintStream out, 
            final Class<?> clazz, final boolean showBundle, final boolean showProperties)
    {
        try
        {
            final List<ServiceReference<?>> services = new ArrayList<>();

            // Get all registered factories and filter based on the given clazz
            final ServiceReference<?>[] refs = context.getServiceReferences(FactoryDescriptor.class.getName(), null);
            if (refs != null)
            {
                for (ServiceReference<?> ref : refs)
                {
                    if (clazz == null || clazz.equals(ref.getProperty(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY)))
                    {
                        services.add(ref);
                    }
                }
            }

            if (services.isEmpty())
            {
                out.println("None");
            }
            else
            {
                for (ServiceReference<?> service : services)
                {
                    if (showBundle)
                    {
                        out.print(service.getBundle().getSymbolicName() + ": ");
                    }

                    out.println(service.getProperty(FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY));

                    if (showProperties)
                    {
                        for (String key : service.getPropertyKeys())
                        {
                            out.println("   " + key + "=" + service.getProperty(key));
                        }
                    }
                }
            }
        }
        catch (final InvalidSyntaxException e)
        {
            e.printStackTrace(out);
        }
    }
}
