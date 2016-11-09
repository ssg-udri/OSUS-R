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

import static org.mockito.Mockito.*;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author dhumeniuk
 *
 */
public class ServiceRegistrationMocker
{
    public static void mockIt(ServiceRegistration<?> serviceReg)
    {
        ServiceReference<?> serviceRef = mock(ServiceReference.class);
        doReturn(serviceRef).when(serviceReg).getReference();
    }   
}