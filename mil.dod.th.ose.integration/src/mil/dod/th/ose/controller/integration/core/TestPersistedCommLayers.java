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
package mil.dod.th.ose.controller.integration.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;

/**
 * @author dhumeniuk
 *
 */
public class TestPersistedCommLayers
{
    @Test
    public void testLayersRestored()
    {
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        
        LinkLayer linkLayer = customCommsService.getLinkLayer("saved-ll");
        assertThat(linkLayer.getPhysicalLink().getName(), is("saved-pl2"));
        
        TransportLayer transportLayer = customCommsService.getTransportLayer("saved-tl");
        assertThat(transportLayer.getLinkLayer(), is(linkLayer));
    }

    /**
     * Verify the recreation and name of the physical link layer created in 
     * {@link mil.dod.th.ose.controller.integration.TestPrepFor2ndRun#createPhysicalLinkStandalone()}.
     */
    @Test
    public void testLayersRestoredCheckName()
    {
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        
        PhysicalLink pLink = customCommsService.requestPhysicalLink("testPersistedPhysLink");
        
        assertThat(pLink, is(notNullValue()));
    }
}
