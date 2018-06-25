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
package mil.dod.th.ose.junit4xmltestrunner;

import static org.hamcrest.MatcherAssert.assertThat; // NOCHECKSTYLE: avoid static import (used in context of testing)
import static org.hamcrest.Matchers.*; // NOCHECKSTYLE: avoid static import (used in context of testing)

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.log.Logging;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.apache.commons.io.IOUtils;

/**
 * Interface to be implemented by a integration test bundle.  Implementation class should be defined
 * in the Integration-Start-Event-Class property in the manifest file.  This interface allows the test
 * runner to wait to start testing until the integration test bundle gives the okay (until all needed services
 * are ready).
 * 
 * @author dhumeniuk
 *
 */
public abstract class IntegrationTestService
{
    /**
     * Component context for this component. 
     */
    private static ComponentContext m_Context;
    
    /**
     * Map of all the services required by test service.  Key is the name of the service reference.
     */
    final private Map<String, ServiceReferenceInfo> m_ServiceReferences = new HashMap<String, ServiceReferenceInfo>();
    
    /**
     * Tests returned by {@link #getTargetTestClasses()} will be executed first, followed by {@link 
     * #getSharedTestClasses()}.
     * 
     * @return
     *      list of all shared and target test classes
     */
    public List<Class<?>> getStandardTestClasses()
    {
        final List<Class<?>> testClasses = new ArrayList<Class<?>>(getTargetTestClasses());
        testClasses.addAll(getSharedTestClasses());
        
        return testClasses;
    }
    
    /**
     * Same as {@link #getStandardTestClasses()} but also includes {@link #getOptionalTestClasses()}.
     * 
     * @return
     *      a list of a all test classes
     */
    public List<Class<?>> getAllTestClasses()
    {
        final List<Class<?>> testClasses = new ArrayList<>();
        testClasses.addAll(getStandardTestClasses());
        testClasses.addAll(getOptionalTestClasses());
        
        return testClasses;
    }
    
    /**
     * Get the list of test classes that should be executed on all targets.  Tests will be executed in the order of the 
     * list.
     * 
     * @return
     *      the list of classes
     */
    public abstract List<Class<?>> getSharedTestClasses();
    
    /**
     * Get the list of test classes that should only be executed on the target.  Tests will be executed in the order of
     * the list.
     * 
     * @return
     *      the list of classes
     */
    public abstract List<Class<?>> getTargetTestClasses();
    
    /**
     * Get the list of test classes that should only be executed expicitely.
     * 
     * @return
     *      the list of classes
     */
    public abstract List<Class<?>> getOptionalTestClasses();

    /**
     * Activate this component.
     * 
     * @param context
     *      Component context for this component
     * @throws InvalidSyntaxException
     *      If defined service references have invalid filter syntaxes
     */
    public void activate(final ComponentContext context) throws InvalidSyntaxException
    {
        m_Context = context;
        
        addServiceReferences();
        
        for (ServiceReferenceInfo info : m_ServiceReferences.values())
        {
            final ServiceTracker<?, ?> tracker = info.getTracker();
            tracker.open();
        }
    }
    
    /**
     * Called before creating service trackers for all service references.  Extending class must call {@link 
     * #addReference(Class)} or {@link #addReference(String, Class, String)} for each needed service.
     * 
     * @throws InvalidSyntaxException 
     *      if a filter has invalid syntax
     */
    public abstract void addServiceReferences() throws InvalidSyntaxException;

    /**
     * Wait for each tracked service.  Assertion fails if any service is not available within the timeout.
     * 
     * @param timeoutMS
     *      How long to wait for each service, will spin through each service waiting on them one by one
     * @throws InterruptedException
     *      If interrupted while waiting for a service
     */
    public void waitForServices(final long timeoutMS) throws InterruptedException
    {
        for (String key : m_ServiceReferences.keySet())
        {
            final ServiceTracker<?, ?> tracker = m_ServiceReferences.get(key).getTracker();
            final Object service = tracker.waitForService(timeoutMS);
            if (service == null && m_ServiceReferences.get(key).isOptional())
            {
                Logging.log(LogService.LOG_INFO, "Service [%s] not available, but optional", key);
                return;
            }
            assertThat("Service is not available for " + key, service, is(notNullValue()));
        }
    }
    
    /**
     * Get the service where the service name is {@link Class#getName()} as add using some call to {@link 
     * #addReference(Class)} or {@link #addReference(String, Class, String)}.
     * 
     * @param <T>
     *      type of service to get
     * @param serviceClass
     *      type of service to get
     * @return
     *      service of the desired type
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(final Class<T> serviceClass)
    {
        return (T)getService(serviceClass.getName());
    }

    /**
     * Get the service object given the service name.  Name is the key from the {@link #addReference(Class)}
     * call. Uses the {@link ServiceTracker} to get the service.
     * 
     * @param serviceName
     *      Name of the service, key in map
     * @return
     *      Service object with the given name
     */
    public Object getService(final String serviceName)
    {
        final ServiceTracker<?, ?> tracker = m_ServiceReferences.get(serviceName).getTracker();
        return tracker.getService();
    }
    
    /**
     * Get the service object given the service name.  Name is the key from the {@link #addReference(Class)}
     * call. Uses the {@link ServiceTracker} to get the service.
     * 
     * @param serviceName
     *      Name of the service, key in map
     * @param timeoutMS
     *      How many milliseconds to wait for service to become available
     * @return
     *      Service object with the given name
     * @throws InterruptedException
     *      if interrupted while waiting
     */
    public Object getService(final String serviceName, final long timeoutMS) throws InterruptedException
    {
        final ServiceTracker<?, ?> tracker = m_ServiceReferences.get(serviceName).getTracker();
        tracker.waitForService(timeoutMS);
        return tracker.getService();
    }
    
    /**
     * Get the bundle that registered the service with the given name.  See {@link #getService(String)}.
     * 
     * @param serviceClass
     *      Interface of the service, key in map
     * @return
     *      Bundle containing the registered service
     */
    public Bundle getServiceBundle(final Class<?> serviceClass)
    {
        final ServiceTracker<?, ?> tracker = m_ServiceReferences.get(serviceClass.getName()).getTracker(); 
        return tracker.getServiceReference().getBundle();
    }
    
    /**
     * Add a reference using the class name for the reference name.
     * 
     * @param clazz
     *      Interface class for desired service
     */
    protected void addReference(final Class<?> clazz)
    {
        try
        {
            addReference(clazz.getName(), clazz, null, false);
        }
        catch (final InvalidSyntaxException e)
        {
            throw new IllegalStateException(
                    "Filter is invalid, but should not be set", e); // NOCHECKSTYLE: repeated string (message only)
        }
    }
    
    /**
     * Create a service reference that is filtered by the provided services properties.
     * 
     * @param name
     *      Name of reference
     * @param clazz
     *      Interface class for desired service
     * @param filter
     *      LDAP filter string to filter the service based on its properties
     * @throws InvalidSyntaxException
     *      if the filter string is not in a valid format 
     */
    protected void addReference(final String name, final Class<?> clazz, final String filter) 
            throws InvalidSyntaxException
    {
        addReference(name, clazz, filter, false);
    }
    
    /**
     * Create an optional service reference for the class name is the reference name.
     * 
     * @param clazz
     *      Interface class for desired service
     */
    protected void addOptionalReference(final Class<?> clazz)
    {
        try
        {
            addReference(clazz.getName(), clazz, null, true);
        }
        catch (final InvalidSyntaxException e)
        {
            throw new IllegalStateException("Filter is invalid, but should not be set", e);
        }
    }
    
    /**
     * Create a service reference that is filtered by the provided services properties.  See other overloaded methods, 
     * typically this method does not need to be called directly.
     * 
     * @param <S>
     *      Type of service to add
     * @param name
     *      Name of reference
     * @param clazz
     *      Interface class for desired service
     * @param filter
     *      LDAP filter string to filter the service based on its properties
     * @param optional
     *      whether the reference is optional, will be ignored if not found
     * @throws InvalidSyntaxException
     *      if the filter string is not in a valid format 
     */
    protected <S> void addReference(final String name, final Class<S> clazz, final String filter, 
            final boolean optional) throws InvalidSyntaxException
    {
        // customizer can be null, keep it that way if filter is not provided
        ServiceTrackerCustomizer<S, Object> customizer = null;
        if (filter != null)
        {
            customizer = new FilterServiceTracker<S>(filter);
        }
        final ServiceTracker<S, Object> tracker = 
                new ServiceTracker<S, Object>(m_Context.getBundleContext(), clazz.getName(), customizer);
        
        m_ServiceReferences.put(name, new ServiceReferenceInfo(clazz, optional, tracker));
        
        
    }
    
    /**
     * Get the name of the target the integration tests are for.  Each provider of this service must override this 
     * method.
     * 
     * @return
     *      target name
     */
    public abstract String getTargetName();

    /**
     * Read a resource from the bundle providing this service.
     * 
     * @param location
     *      location within the bundle to read from
     * @return
     *      string representing the contents of the resource at the given location
     * @throws IOException
     *      if there is a failure to read the resource
     */
    public static String readResourceAsString(final String location) throws IOException
    {
        final URL url = m_Context.getBundleContext().getBundle().getResource(location);
        if (url == null)
        {
            throw new FileNotFoundException(location);
        }
        final StringWriter writer = new StringWriter();
        final InputStream input = url.openStream();
        IOUtils.copy(input, writer);
        return writer.toString();
    }
    
    /**
     * Read a resource from the bundle providing this service.
     * 
     * @param location
     *      location within the bundle to read from
     * @return
     *      byte array representing the contents of the resource at the given location
     * @throws IOException
     *      if there is a failure to read the resource
     */
    public static byte[] readResource(final String location) throws IOException
    {
        final URL url = m_Context.getBundleContext().getBundle().getResource(location);
        if (url == null)
        {
            throw new FileNotFoundException(location);
        }
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final InputStream input = url.openStream();
        IOUtils.copy(input, output);
        return output.toByteArray();
    }
    
    /**
     * {@link ServiceTrackerCustomizer} that uses a LDAP filter string.
     * 
     * @author dhumeniuk
     *
     *@param <S>
     *       Tracker types
     */
    public class FilterServiceTracker<S> implements ServiceTrackerCustomizer<S, Object>
    {
        /**
         * Filter to match service references.
         */
        final private Filter m_Filter;

        /**
         * Default constructor.
         * 
         * @param filterStr
         *      LDAP filter string to use for service filtering
         * @throws InvalidSyntaxException
         *      if the filter string is not a valid LDAP syntax
         */
        public FilterServiceTracker(final String filterStr) throws InvalidSyntaxException
        {
            m_Filter = m_Context.getBundleContext().createFilter(filterStr);
        }

        @Override
        public Object addingService(final ServiceReference<S> reference)
        {
            if (m_Filter.match(reference))
            {
                return reference.getBundle().getBundleContext().getService(reference);
            }
            return null; 
        }

        @Override
        public void modifiedService(final ServiceReference<S> reference, final Object service)
        {
            // nothing needs to be done when modified
        }

        @Override
        public void removedService(final ServiceReference<S> reference, final Object service)
        {
            // nothing needs to be done when removed
        }     
    }
}
