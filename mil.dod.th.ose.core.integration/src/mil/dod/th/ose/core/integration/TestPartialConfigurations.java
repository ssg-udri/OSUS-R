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
package mil.dod.th.ose.core.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.util.Set;

import junit.framework.TestCase;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import example.ccomms.exception.ExampleLinkLayerMissingOcdDesc;

/**
 * Verify that if a plug-ins attributes class is not properly annotated as a partial that
 * the factory is not registered.
 * @author allenchl
 *
 */
public class TestPartialConfigurations extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    /**
     * Verify that a link layer factory is not registered as available if the OCD of the attributes class
     * does not have 
     * {@link mil.dod.th.core.ConfigurationConstants#PARTIAL_OBJECT_CLASS_DEFINITION} as the description.
     */
    public void testLinkLayerFactoryNotAvailAsNoOcdDescription() 
        throws InterruptedException, CCommException, IllegalArgumentException, FactoryException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        
        // give plug-in time to try and register
        FactoryUtils.getFactoryDescriptorReference(m_Context, ExampleLinkLayerMissingOcdDesc.class, 5000);
        
        Set<LinkLayerFactory> factories = customCommsService.getLinkLayerFactories();
        for (LinkLayerFactory fact : factories)
        {
            assertThat(fact.getProductType(), is(not(ExampleLinkLayerMissingOcdDesc.class.getName())));
        }
    }
}
