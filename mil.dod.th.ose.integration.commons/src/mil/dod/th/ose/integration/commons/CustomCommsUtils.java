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

import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.log.Logging;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * @author  dhumeniuk
 */
public class CustomCommsUtils
{
    /**
     * Delete all comms layers.
     */
    public static void deleteAllLayers(BundleContext context)
    {
        CustomCommsService customCommsService = ServiceUtils.getService(context, CustomCommsService.class);
        
        for (TransportLayer transport : customCommsService.getTransportLayers())
        {
            try
            {
                transport.delete();
            }
            catch (Exception e)
            {
                Logging.log(LogService.LOG_WARNING, e, "Unable to delete transport layer [%s]", transport);
            }
        }
        
        for (LinkLayer link : customCommsService.getLinkLayers())
        {
            try
            {
                link.delete();
            }
            catch (Exception e)
            {
                Logging.log(LogService.LOG_WARNING, e, "Unable to delete link layer [%s]", link);
            }
        }
        
        for (String physicalLinkName : customCommsService.getPhysicalLinkNames())
        {
            try
            {
                customCommsService.releasePhysicalLink(physicalLinkName);
            }
            catch (IllegalStateException | IllegalArgumentException e)
            {
                //just want to clean up
            }
            try
            {
                customCommsService.deletePhysicalLink(physicalLinkName);
            }
            catch (IllegalStateException | IllegalArgumentException | CCommException e)
            {
                //just want to clean up
            }
        }
    }
}