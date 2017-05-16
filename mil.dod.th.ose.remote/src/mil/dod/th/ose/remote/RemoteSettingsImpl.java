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
package mil.dod.th.ose.remote;

import java.util.HashMap;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.ose.remote.api.RemoteSettings;

import org.osgi.framework.BundleContext;

/**
 * Implementation of {@link RemoteSettings}.
 */
@Component(name = RemoteSettings.PID, designate = RemoteSettingsConfig.class, 
    configurationPolicy = ConfigurationPolicy.optional, immediate = true)
public class RemoteSettingsImpl implements RemoteSettings  // TODO: TH-700 - shouldn't need to be immediate, but helps 
{                                                          //keep this component active
    /**
     * Name of the OSGi framework property containing the default behavior for this component.
     */
    public static final String ENCRYPTION_FRAMEWORK_PROPERTY = "mil.dod.th.ose.remote.encryption.level";
    
    /**
     * True if we want to log remote messages.
     */
    private boolean m_IsLogRemoteMessagesEnabled;
    
    /**
     * Encryption mode.
     */
    private EncryptionMode m_EncryptionMode;
    
    /**
     * Message size, in bytes.
     */
    private long m_MaxMsgSizeInBytes;

    /**
     * True if the power management sleep mode should be prevented.
     */
    private boolean m_IsPreventSleepModeEnabled;

    /**
     * The bundle context from the bundle containing this component.
     */
    private BundleContext m_Context;
    
    /**
     * Activate this component.
     * 
     * @param context
     *      context for this bundle
     * @param props
     *      properties to use for this component
     */
    @Activate
    public void activate(final BundleContext context, final Map<String, Object> props)
    {
        m_Context = context;
        updateProps(props);
    }

    /**
     * Called when the properties of this component are modified.
     * 
     * @param props
     *      new properties
     */
    @Modified
    public void modified(final Map<String, Object> props)
    {
        updateProps(props);
    }
    
    @Override
    public boolean isLogRemoteMessagesEnabled()
    {
        return m_IsLogRemoteMessagesEnabled;
    }
    
    @Override
    public EncryptionMode getEncryptionMode()
    {
        return m_EncryptionMode;
    }
    
    /**
     * Update the properties of this component.
     * 
     * @param props
     *      new properties
     */
    private void updateProps(final Map<String, Object> props)
    {
        //new map of properties the original is read only
        final Map<String, Object> newProperties = new HashMap<String, Object>(props);
        
        //framework property
        final String frameworkProp = m_Context.getProperty(ENCRYPTION_FRAMEWORK_PROPERTY);
        
        //check if there is already a configuration property, if not use framework, if framework is null
        //use default of NONE
        if (props.get(RemoteSettings.KEY_ENCRYPTION_MODE) == null && frameworkProp != null)
        {
            newProperties.put(RemoteSettings.KEY_ENCRYPTION_MODE, EncryptionMode.valueOf(frameworkProp));
        }
        
        final RemoteSettingsConfig config = Configurable.createConfigurable(RemoteSettingsConfig.class, newProperties);
        
        m_IsLogRemoteMessagesEnabled = config.logRemoteMessages();
        m_EncryptionMode = config.encryptionMode();
        m_MaxMsgSizeInBytes = config.maxMsgSizeInBytes();
        m_IsPreventSleepModeEnabled = config.preventSleepMode();
    }

    @Override
    public long getMaxMessageSize() 
    {
        return m_MaxMsgSizeInBytes;
    }

    @Override
    public boolean isPreventSleepModeEnabled()
    {
        return m_IsPreventSleepModeEnabled;
    }
}
