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
package mil.dod.th.ose.logging;

import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.metatype.Configurable;

import org.osgi.framework.Bundle;

/**
 * Component created for each log filter configuration created.
 * 
 * @author dhumeniuk
 *
 */
@Component(designateFactory = LogFilterConfig.class)
public class LogFilterImpl implements LogFilter
{
    /**
     * Filter for the bundle.
     */
    private String m_BundleSymbolicFilter;
    
    /**
     * Log severity as defined by the {@link org.osgi.service.log.LogService}.
     */
    private int m_Severity;

    /**
     * Activate the component.
     * 
     * @param props
     *      component properties
     */
    @Activate
    public void activate(final Map<String, Object> props)
    {
        update(props);
    }
    
    /**
     * Called when component properties are modified.
     * 
     * @param props
     *      component properties
     */
    @Modified
    public void modified(final Map<String, Object> props)
    {
        update(props);
    }
    
    @Override
    public boolean matches(final Bundle bundle)
    {
        return bundle.getSymbolicName().matches(m_BundleSymbolicFilter);
    }

    @Override
    public int getSeverity()
    {
        return m_Severity;
    }

    /**
     * Update the internal fields based on the properties.
     * 
     * @param props
     *      new properties to use
     */
    private void update(final Map<String, Object> props)
    {
        final LogFilterConfig config = Configurable.createConfigurable(LogFilterConfig.class, props);
        m_BundleSymbolicFilter = config.bundleSymbolicFilter();
        m_Severity = LogUtil.convertNativeToOsgiLevel(config.severity());
    }
}
