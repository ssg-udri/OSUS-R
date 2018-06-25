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
package mil.dod.th.ose.metatype.impl;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;

import mil.dod.th.ose.metatype.MetaTypeProviderBundle;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Implements {@link MetaTypeProviderBundle}.
 * 
 * @author dhumeniuk
 *
 */
@Component
public class MetaTypeProviderBundleImpl implements MetaTypeProviderBundle
{
    /**
     * Bundle context associated with the bundle containing this component.
     */
    private BundleContext m_Context;

    /**
     * Activate the component, just save off the context.
     * 
     * @param context
     *      context for the bundle containing this component
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;
    }
    
    @Override
    public Bundle getBundle()
    {
        return m_Context.getBundle();
    }
}
