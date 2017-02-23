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
package mil.dod.th.ose.gui.webapp.controller.history;

import java.io.Serializable;

/**
 * Class used to represent a controller that has been previously connected to.
 * 
 * @author cweisenborn
 */
public class ControllerHistory implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    private Integer m_ControllerId;
    private String m_ControllerName;
    private String m_HostName;
    private int m_Port;
    private boolean m_SslEnabled;
    private long m_LastConnected;
    
    public Integer getControllerId()
    {
        return m_ControllerId;
    }
    
    public void setControllerId(final int controllerId)
    {
        m_ControllerId = controllerId;
    }
    
    public String getControllerName()
    {
        return m_ControllerName;
    }
    
    public void setControllerName(final String controllerName)
    {
        m_ControllerName = controllerName;
    }
    
    public String getHostName()
    {
        return m_HostName;
    }
    
    public void setHostName(final String hostName)
    {
        m_HostName = hostName;
    }
    
    public int getPort()
    {
        return m_Port;
    }
    
    public void setPort(final int port)
    {
        m_Port = port;
    }
    
    public boolean isSslEnabled()
    {
        return m_SslEnabled;
    }

    public void setSslEnabled(final boolean sslEnabled)
    {
        m_SslEnabled = sslEnabled;
    }

    public long getLastConnected()
    {
        return m_LastConnected;
    }
    
    public void setLastConnected(final long lastConnected)
    {
        m_LastConnected = lastConnected;
    }
}
