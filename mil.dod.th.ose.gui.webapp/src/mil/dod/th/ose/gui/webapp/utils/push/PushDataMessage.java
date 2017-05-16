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
package mil.dod.th.ose.gui.webapp.utils.push;

/**
 * This class contains common methods for other classes that extend this class.
 * 
 * @author nickmarcucci
 *
 */
public abstract class PushDataMessage
{
    /**
     * The {@link PushMessageType} of the message.
     */
    protected PushMessageType m_Type;
    
    /**
    * Sets the type of message.
    * @param type
    *  the type of push message
    */
    public abstract void setType(PushMessageType type);
    
    /**
     * Gets the type of message. This method must be defined in the 
     * class that is to be JSONified. If implemented here, the JSONifier
     * would not include this method in the created string.
     * @return
     *   the message type.
     */
    public abstract String getType();
    
    /**
     * Method used to print the string representation of the message.
     * This method must be defined in the class that is to be JSONified. 
     * If implemented here, the JSONifier would not include this method 
     * in the created string.
     * @return
     *  the string message for this class.
     */
    @Override
    public abstract String toString();
    
    /**
     * Method to retrieve the message type for the message.
     * 
     * @return
     *  a string in the format "MessageType: type" where type
     *  is the message type of this message.
     */
    public String printMessageType()
    {
        return String.format("MessageType: %s", m_Type);
    }
}
