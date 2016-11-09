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
package mil.dod.th.ose.integration.commons;

import static org.junit.Assert.fail;

import java.io.File;

import org.knowhowlab.osgi.testing.assertions.ServiceAssert;
import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObjectProxy;

/**
 * Utilities for working with {@link FactoryDescriptor} and other core factory concepts.
 * 
 * @author dhumeniuk
 *
 */
public class FactoryUtils
{
    public static FactoryDescriptor getFactoryDescriptor(BundleContext context, 
            Class<? extends FactoryObjectProxy> productType)
    {
        try
        {
            return ServiceUtils.getService(context, FactoryDescriptor.class, 
                    "(" + FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY + "=" + productType.getName() + ")");
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public static void assertFactoryDescriptorAvailable(BundleContext context, 
            Class<? extends FactoryObjectProxy> productType, long timeoutMillis)
    {
        String filter = getServiceFilter(productType);
        try
        {           
            ServiceAssert.assertServiceAvailable(context.createFilter(filter), timeoutMillis);
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch (AssertionError e)
        {
            fail(productType + " product type not available");
        }
    }
    
    public static void assertFactoryDescriptorAvailable(BundleContext context, String productType, long timeoutMillis)
    {
        String filter = getServiceFilter(productType);
        try
        {
            
            ServiceAssert.assertServiceAvailable(context.createFilter(filter), timeoutMillis);
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch (AssertionError e)
        {
            fail(productType + " product type not available");
        }
    }

    @SuppressWarnings("unchecked")
    public static ServiceReference<FactoryDescriptor> getFactoryDescriptorReference(BundleContext context, 
            Class<? extends FactoryObjectProxy> productType, long timeoutMillis)
    {
        String filter = getServiceFilter(productType);
        
        try
        {
            return ServiceUtils.getServiceReference(context, context.createFilter(filter), timeoutMillis);
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static ServiceReference<FactoryDescriptor> getFactoryDescriptorReference(BundleContext context, 
            String productType, long timeoutMillis)
    {
        String filter = getServiceFilter(productType);
        
        try
        {
            return ServiceUtils.getServiceReference(context, context.createFilter(filter), timeoutMillis);
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
    
    public static void installPlugin(BundleContext context, File jarFile, String productType) throws BundleException
    {
        Bundle bundle = context.installBundle(jarFile.toURI().toString());
        bundle.start();
        
        FactoryUtils.assertFactoryDescriptorAvailable(context, productType, 5000);
    }
    
    private static String getServiceFilter(Class<? extends FactoryObjectProxy> productType)
    {
        String filter = String.format("(&(%s=%s)(%s=%s))",
                Constants.OBJECTCLASS, FactoryDescriptor.class.getName(),
                FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY, productType.getName());
        return filter;
    }
    
    private static String getServiceFilter(String productType)
    {
        String filter = String.format("(&(%s=%s)(%s=%s))",
                Constants.OBJECTCLASS, FactoryDescriptor.class.getName(),
                FactoryDescriptor.PRODUCT_TYPE_SERVICE_PROPERTY, productType);
        return filter;
    }
}
