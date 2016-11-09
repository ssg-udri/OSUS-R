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
package mil.dod.th.ose.gui.webapp.mp;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds all information pertaining to a mission including any parameters to be set.
 * 
 * @author cweisenborn
 */
public class MissionModel
{
    /**
     * String representation of the name of the mission.
     */
    private String m_Name;
    
    /**
     * String representation of the description of the mission.
     */
    private String m_Description;
    
    /**
     * Enumeration that represents where the mission template is stored (either in the local store or on a remote
     * device).
     */
    private MissionTemplateLocation m_TemplateLocation;
    
    /**
     * String representation of the Javascript source code of the mission.
     */
    private String m_Source;
    
    /**
     * {@link List} containing integer values which correspond to the subscript id of each secondary image stored with
     * the template.
     */
    private List<Integer> m_SecondaryImageIds;
    
    /**
     *Boolean value that represents if the mission is ran over an interval.
     */
    private boolean m_WithInterval;
    
    /**
     * Boolean value that represents whether the mission is image capture capable.
     */
    private boolean m_WithImageCapture;
    
    /**
     * Boolean value that represents whether the mission is triggered by a sensor.
     */
    private boolean m_WithSensorTrigger;
    
    /**
     * Boolean value that represents whether the mission is triggered by a timer.
     */
    private boolean m_WithTimerTrigger;
    
    /**
     * {@link List} that stores all parameters that are passed to the mission.
     */
    private List<MissionArgumentModel> m_Arguments;
    
    /**
     * Method that returns the mission name.
     * 
     * @return
     *          The missions name as a string.
     */
    public String getName()
    {
        return m_Name;
    }
    
    /**
     * Method that sets the mission name.
     * 
     * @param name
     *          The name to be set.
     */
    public void setName(final String name)
    {
        m_Name = name;
    }
    
    /**
     * Method that returns the mission description.
     * 
     * @return
     *          The mission description as a string.
     */
    public String getDescription()
    {
        return m_Description;
    }
    
    /**
     * Method that sets the mission description.
     * 
     * @param description
     *          The mission description to be set.
     */
    public void setDescription(final String description)
    {
        m_Description = description;
    }
    
    /**
     * Method that returns the location where the mission is stored.
     * 
     * @return
     *          The location of the mission as a string.
     */
    public MissionTemplateLocation getLocation()
    {
        return m_TemplateLocation;
    }
    
    /**
     * Method that sets the location of the mission.
     * 
     * @param location
     *          The mission location to be set.
     */
    public void setLocation(final MissionTemplateLocation location)
    {
        m_TemplateLocation = location;
    }
    
    /**
     * Method that returns the mission source code as a string.
     * 
     * @return
     *          String representation of the Javascript source code of the mission.
     */
    public String getSource()
    {
        return m_Source;
    }
    
    /**
     * Method that sets the mission source code.
     * 
     * @param source
     *          String that represents the source code for mission.
     */
    public void setSource(final String source)
    {
        m_Source = source;
    }
    
    /**
     * Method that returns a list of integers that are the subscript values of the secondary images stored with the 
     * mission template in the TemplateProgramManager.
     *  
     * @return
     *          List of integers that contains the subscript values of the secondary images stored with the mission
     *          template.
     */
    public List<Integer> getSecondaryImageIds()
    {
        if (m_SecondaryImageIds == null)
        {
            m_SecondaryImageIds = new ArrayList<Integer>();
        }
        return m_SecondaryImageIds;
    }
    
    /**
     * Method that sets the secondary image IDs for the mission.
     * 
     * @param secondaryImages
     *          List that contains the integer values that are the subscript values of the secondary images stored with
     *          the mission template in the TemplateProgramManager.
     */
    public void setSecondaryImageIds(final List<Integer> secondaryImages)
    {
        m_SecondaryImageIds = secondaryImages;
    } 
    
    /**
     * Method that returns of boolean value used to determine if the mission is ran over an interval.
     * 
     * @return
     *          Boolean value of whether the mission is ran over an interval.
     */
    public boolean isWithInterval()
    {
        return m_WithInterval;
    }
    
    /**
     * Method used to the WithInterval value.
     * 
     * @param withInterval
     *          Boolean that represents whether the mission is run over an interval.
     */
    public void setWithInterval(final boolean withInterval)
    {
        m_WithInterval = withInterval;
    }
    
    /**
     * Method that returns a boolean used to determine if the mission is image capture capable.
     * 
     * @return
     *          Boolean value that represents whether the mission is image capture capable.
     */
    public boolean isWithImageCapture()
    {
        return m_WithImageCapture;
    }
    
    /**
     * Method that sets whether the mission is image capture capable.
     * 
     * @param withImageCapture
     *          Boolean value that represents whether the mission is image capture capable.
     */
    public void setWithImageCapture(final boolean withImageCapture)
    {
        m_WithImageCapture = withImageCapture;
    }
    
    /**
     * Method that returns a boolean used to determine if the mission is triggered by a sensor.
     * 
     * @return
     *          Boolean value that represents whether mission is triggered by a sensor.
     */
    public boolean isWithSensorTrigger()
    {
        return m_WithSensorTrigger;
    }
    
    /**
     * Method that sets whether the mission is triggered by a sensor.
     * 
     * @param withSensorTrigger
     *          Boolean value that represents whether mission is triggered by a sensor.
     */
    public void setWithSensorTrigger(final boolean withSensorTrigger)
    {
        m_WithSensorTrigger = withSensorTrigger;
    }
    
    /**
     * Method that returns a boolean used to determine if the mission is triggered by a timer.
     * 
     * @return
     *          Boolean value that represents whether the mission is triggered by a timer.
     */
    public boolean isWithTimerTrigger()
    {
        return m_WithTimerTrigger;
    }
    
    /**
     * Method that sets whether mission is triggered by a timer.
     * 
     * @param withTimerTrigger
     *          Boolean value that represents whether the mission is triggered by a timer.
     */
    public void setWithTimerTrigger(final boolean withTimerTrigger)
    {
        m_WithTimerTrigger = withTimerTrigger;
    }
    
    /**
     * Method that returns a list of all parameters that are passed to the running mission.
     * 
     * @return
     *          List of {@link MissionArgumentModel}s that represents all variables of the mission.
     */
    public List<MissionArgumentModel> getArguments()
    {
        if (m_Arguments == null)
        {
            m_Arguments = new ArrayList<MissionArgumentModel>();
        }
        return m_Arguments;
    }
    
    /**
     * Enumeration that represents where the mission template is located. The template can either reside locally or
     * on an external controller.
     * 
     * @author cweisenborn
     */
    public enum MissionTemplateLocation
    {
        /**
         * Enumeration used to represent that the mission template is stored in the local TemplateProgramManager.
         */
        LOCAL("Local"),
        
        /**
         * Enumeration used to represent that the mission template is stored in a TemplateProgramManager on 
         * an external controller.
         */
        SYNCED("Synced");
        
        /**
         * String representation of the currently set enumeration.
         */
        private String m_Value;
        
        /**
         * Method that sets the string representation of the enumeration.
         * 
         * @param valueStr
         *          String representation of the enumertion.
         */
        MissionTemplateLocation(final String valueStr) 
        {
            m_Value = valueStr;
        }
        
        /**
         * Method that returns value variable which is a string representation of the currently set 
         * MissionTemplateLocation enumeration.
         * 
         * @return
         *          String representation of the currently set enumeration.
         */
        public String value() 
        {
            return m_Value;
        }
    }
}
