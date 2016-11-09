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
package mil.dod.th.ose.gui.webapp.factory;

import java.util.UUID;

import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.advanced.configuration.UnmodifiablePropertyModel;

/**
 * Base class for all remote representations of factory configuration type objects.
 * @author callen
 *
 */
public abstract class AbstractFactoryBaseModel implements FactoryBaseModel
{
    /**
     * The PID of the factory object.
     */
    private String m_Pid;

    /**
     * The controller ID of the system to which this factory object originates.
     */
    private final int m_ControllerId;

    /**
     * The UUID of the represented factory object.
     */
    private final UUID m_Uuid;
    
    /**
     * The factory configuration PID for this factory object.
     */
    private final String m_FactoryPid;
    
    /**
     * The base factory manager that this factory object belongs to.
     */
    final private FactoryObjMgr m_BaseFactoryMgr; 
    
    /**
     * Reference the configuration wrapper bean.
     */
    private final ConfigurationWrapper m_ConfigWrapper;
    
    /**
     * The name of the represented factory object.
     */
    private  String m_Name;
    
    /**
     * The working name of the represented factory object. 
     */
    private  String m_WorkingName = "";
    
    /**
     * Constructor which sets the common fields for all factory created objects.
     * @param controllerId
     *     the controller ID from which this object originates
     * @param uuid
     *     the UUID of the factory object
     * @param pid
     *     the PID of the factory object
     * @param  factMgr
     *     factory object manager interface
     * @param clazz
     *      the class that this factory base model represents
     * @param configWrapper
     *     reference to the configuration wrapper bean
     */
    public AbstractFactoryBaseModel(final int controllerId, final UUID uuid, final String pid, 
            final String clazz, final FactoryObjMgr factMgr, final ConfigurationWrapper configWrapper) 
    {
        super();
        
        m_Pid = checkGivenPid(pid);
        m_Uuid = uuid;
        m_BaseFactoryMgr = factMgr;
        m_ControllerId = controllerId;
        m_ConfigWrapper = configWrapper;
        m_Name = getDefaultName();
        
        m_FactoryPid = clazz + FactoryDescriptor.PID_SUFFIX;
    }
    
    /**
     * Get the PID of the factory object represented by this model.
     * @return
     *     the PID
     */
    @Override
    public String getPid()
    {
        return m_Pid;
    }
    
    @Override
    public void setPid(final String pid)
    {
        m_Pid = checkGivenPid(pid);
    }

    /**
     * Get the UUID of this factory object.
     * @return
     *     the UUID
     */
    @Override
    public UUID getUuid()
    {
        return m_Uuid;
    }

    /**
     * Get the ID of the controller that this factory object belongs to.
     * @return
     *    the controller id that this object belongs to
     */
    @Override
    public int getControllerId()
    {
        return m_ControllerId;
    }

    /**
     * Get the object's name.
     * @return
     *     the name
     */
    @Override
    public String getName()
    {
        return m_Name;
    }
    
    @Override
    public UnmodifiablePropertyModel getPropertyAsync(final String key)
    {
        return m_ConfigWrapper.getConfigurationPropertyAsync(m_ControllerId, m_Pid, key);
    }
    
    @Override
    public boolean isComplete()
    {
        //check if the name has not been set
        if (getName().equals(getDefaultName()))
        {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String getFactoryPid() 
    {
        return m_FactoryPid;
    }
    
    @Override
    public FactoryObjMgr getFactoryManager() 
    {
        return m_BaseFactoryMgr;
    }
    
    /**
     * Set the object's name.
     * @param name
     *     the name to set for the factory object
     */
    public void setName(final String name)
    {
        m_WorkingName = name;
    }
    
    /**
     * Get the object's working name. Used to hold the name value, if the name is being updated.
     * This should NOT be used for display purposes as this name is not confirmed to be able to be used.
     * @return
     *     the name set previously for the factory object
     */
    public String getWorkingName()
    {
        return m_WorkingName;
    }
    
    /**
     * update the object's name.
     * @param name
     *     the name to set for the factory object
     */
    public void updateName(final String name)
    {
        m_Name = name;
    }
    
    /**
     * Placeholder until actual name is available.
     * @return
     *      default name of the model
     */
    private String getDefaultName()
    {
        return "Unknown (" + m_Uuid.toString() + ")";
    }
    
    /**
     * Method to check if the given PID is null. If PID is null
     * an empty string will be returned. Otherwise, the PID value 
     * passed in will be returned.
     * 
     * @param pid
     *  the PID that is to be checked.
     * @return
     * If PID is null an empty string will be returned. Otherwise, the PID value 
     * passed in will be returned.
     */
    private String checkGivenPid(final String pid)
    {
        if (pid == null)
        {
            return "";
        }
        
        return pid;
    }
}
