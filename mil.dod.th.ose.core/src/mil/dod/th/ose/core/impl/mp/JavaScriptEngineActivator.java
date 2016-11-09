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
package mil.dod.th.ose.core.impl.mp;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.ose.mp.runtime.MissionProgramRuntime;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;


/**
 * Component makes the JavaScript {@link ScriptEngine} available as an OSGi service when active.
 * 
 * @author dhumeniuk
 *
 */
@Component
public class JavaScriptEngineActivator
{
    /**
     * Name of the {@link ScriptEngine} to request.
     */
    private static final String ENGINE_NAME = "JavaScript";
    /**
     * Registration of the JavaScript engine.
     */
    private ServiceRegistration<ScriptEngine> m_Registration;

    
    private MissionProgramRuntime m_MissionProgramRuntime;
    
    @Reference
    public void setMissionProgramRuntime(final MissionProgramRuntime runtime)
    {
        m_MissionProgramRuntime = runtime;
    }
   
    
    /**
     * Activate this component by creating a JavaScript {@link ScriptEngine} and providing as an OSGi service.
     * 
     * @param context
     *      context of the bundle containing this component 
     */
    @Activate
    public void activate(final BundleContext context)
    {
        // thread context class loader is used by script engine to access the TH API, set it to the runtime's class 
        // loader
        Thread.currentThread().setContextClassLoader(m_MissionProgramRuntime.getClassLoader());
        
        final ScriptEngineManager manager = new ScriptEngineManager(null);
        
        final ScriptEngine engine = manager.getEngineByName(ENGINE_NAME);
        
        final Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("name", ENGINE_NAME);
        m_Registration = context.registerService(ScriptEngine.class, engine, properties);
    }

    /**
     * Deactivate this component by unregistering the {@link ScriptEngine} service.
     */
    @Deactivate
    public void deactivate()
    {
        m_Registration.unregister();
    }
}
