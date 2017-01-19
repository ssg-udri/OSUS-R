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
package mil.dod.th.ose.rxtxtty.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import junit.framework.TestCase;

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Test class that verifies the RXTX bundle is functioning.
 * 
 * @author cweisenborn
 */
public class TestRxtxBundle extends TestCase
{   
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    /**
     * Verify that the rxtx bundle is installed/active.
     */
    public void testRxtxBundleLoaded()
    {
        for (Bundle bundle : m_Context.getBundles())
        {
            if (bundle.getSymbolicName().startsWith("mil.dod.th.ose.rxtxtty"))
            {
                assertThat(bundle.getState(), is(Bundle.ACTIVE));
                return;
            }
        }
        
        fail("RXTX bundle is missing");
    }
    
    /**
     * Verify that a serial port can be created.
     */
    public void testSerialAvailable() throws IllegalStateException, CCommException, PersistenceFailedException
    {
        CustomCommsService customCommsService = ServiceUtils.getService(m_Context, CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));

        // just create the serial port, this will verify serial port factory is registered but don't open the serial 
        // port since it is unknown whether port is available
        customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.SERIAL_PORT, "/dev/ttyS1");
    }
}
