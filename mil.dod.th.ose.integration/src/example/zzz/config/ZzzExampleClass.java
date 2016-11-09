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
package example.zzz.config;

import java.util.Map;

import mil.dod.th.core.log.LoggingService;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Reference;

/**
 * Example class used to test {@link mil.dod.th.ose.controller.integration.config.TestXmlConfigurations
 * #testOSGiConfiguration_BundleLoadedAfterActivation()}.
 */
@Component(designate = ZzzExampleClassConfig.class, configurationPolicy = ConfigurationPolicy.optional)
public class ZzzExampleClass
{
    /**
     * Config PID will be the FQCN of this component since it uses {@link ZzzExampleClassConfig} as a designate.
     */
    public final static String CONFIG_PID = ZzzExampleClass.class.getName();
    
    private LoggingService m_Logging;
    
    @Reference
    public void setLoggingService(LoggingService logging)
    {
        m_Logging = logging;
    }

    /**
     * Activate the component.
     * 
     * @param props
     *            Configuration Map
     */
    @Activate
    public void activate(final Map<String, Object> props)
    {
        m_Logging.debug("%s activated with properties: %s", getClass().getName(), props);
    }
}
