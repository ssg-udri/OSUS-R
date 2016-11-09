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
package mil.dod.th.ose.core;

import static org.mockito.Mockito.*;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import mil.dod.th.ose.metatype.MetaTypeProviderBundle;

/**
 * Mocker for {@link MetaTypeProviderBundle}.
 * 
 * @author dhumeniuk
 *
 */
public class MetaTypeProviderBundleMocker
{
    /**
     * Base mocking of the {@link MetaTypeProviderBundle} service.
     */
    public static MetaTypeProviderBundle mockIt()
    {
        MetaTypeProviderBundle metaTypeProviderBundle = mock(MetaTypeProviderBundle.class);
        Bundle bundle = mock(Bundle.class);
        BundleContext bundleContext = mock(BundleContext.class);
        
        when(metaTypeProviderBundle.getBundle()).thenReturn(bundle);
        when(bundle.getBundleContext()).thenReturn(bundleContext);
        
        return metaTypeProviderBundle;
    }

}
