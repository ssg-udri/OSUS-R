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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.Map;

import mil.dod.th.ose.shared.LogLevel;
import mil.dod.th.ose.test.BundleMocker;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * @author dhumeniuk
 *
 */
public class TestLogFilterImpl
{
    private LogFilterImpl m_SUT;

    @Before
    public void setUp()
    {
        m_SUT = new LogFilterImpl();
    }
    
    /**
     * Verify the component properties are handle correctly during activation (severity is correct, correct bundles 
     * match). 
     */
    @Test
    public void testPropsAfterActivation()
    {
        Map<String, Object> props = new HashMap<>();
        props.put("bundleSymbolicFilter", "mil.dod.th.ose..*");
        props.put("severity", LogLevel.Warning);
        m_SUT.activate(props);
        
        assertThat(m_SUT.getSeverity(), is(LogService.LOG_WARNING));
        
        // mock bundles
        Bundle bundle1 = BundleMocker.mockBundle("mil.dod.th.ose.anything");
        Bundle bundle2 = BundleMocker.mockBundle("org.apache.felix.anything");
        
        assertThat(m_SUT.matches(bundle1), is(true));
        assertThat(m_SUT.matches(bundle2), is(false));
    }
    
    /**
     * Verify the component properties are handle correctly during modification (severity is correct, correct bundles 
     * match). 
     */
    @Test
    public void testPropsAfterModification()
    {
        Map<String, Object> props = new HashMap<>();
        props.put("bundleSymbolicFilter", ".*");
        props.put("severity", LogLevel.Debug);
        m_SUT.modified(props);
        
        assertThat(m_SUT.getSeverity(), is(LogService.LOG_DEBUG));
        
        // mock bundles
        Bundle bundle1 = BundleMocker.mockBundle("mil.dod.th.ose.anything");
        Bundle bundle2 = BundleMocker.mockBundle("org.apache.felix.anything");
        
        assertThat(m_SUT.matches(bundle1), is(true));
        assertThat(m_SUT.matches(bundle2), is(true));
    }
}
