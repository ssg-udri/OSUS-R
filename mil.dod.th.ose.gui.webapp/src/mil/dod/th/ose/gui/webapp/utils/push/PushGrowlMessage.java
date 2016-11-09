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

import javax.faces.application.FacesMessage;

/**
 * This class represents a GrowlMessage object when it is passed by PrimeFaces Push
 * to a web browser for display. 
 * 
 * @author nickmarcucci
 *
 */
public class PushGrowlMessage extends PushDataMessage
{
    /**
     * The summary text for the growl message being sent 
     * to the browser.
     */
    private String m_Summary;
    
    /**
     * The detail text for the growl message being sent to the browser.
     */
    private String m_Detail;
    
    /**
     * The severity level for the growl message being sent to the browser.
     */
    private String m_Severity;
    
    /**
     * Flag to indicated which GrowlMessage display to use. If true, the GrowlMessage
     * is a sticky message. Otherwise, it is a timed GrowlMessage.
     */
    private boolean m_Sticky;
    
    /**
     * Constructor for creating a PushGrowlMessage.
     * 
     * @param severity
     *  the severity that the Growl message should have.
     * @param summary
     *  the text that belongs in the summary field for the Growl message.
     * @param detail
     *  the text that belongs in the detail field for the Growl message.
     * @param sticky
     *  indicates whether message should be dsplayed as a sticky message or
     *  a timed message. Set to true if sticky message is desired.
     */
    public PushGrowlMessage(final FacesMessage.Severity severity, final String summary, 
            final String detail, final boolean sticky)
    {
        super();
        m_Summary = summary;
        
        m_Detail = detail;
        
        m_Severity = convertSeverity(severity);
        
        m_Sticky = sticky;
        
        m_Type = PushMessageType.GROWL_MESSAGE;
    }
    
    
    /**
     * Function to set the summary field for the Growl message.
     * 
     * @param summary
     *  the text that is to be displayed in the summary field of the 
     *  growl message.
     */
    public void setSummary(final String summary)
    {
        m_Summary = summary;
    }
    
    /**
     * Function to set the detail field for the Growl message.
     * 
     * @param detail
     *  the text that is to be displayed in the detail field of the 
     *  growl message.
     */
    public void setDetail(final String detail)
    {
        m_Detail = detail;
    }
    
    /**
     * Function to set the severity for the Growl message.
     * 
     * @param severity
     *  the severity level that the Growl message is supposed to be.
     */
    public void setSeverity(final FacesMessage.Severity severity)
    {
        m_Severity = convertSeverity(severity);
    }
    
    /**
     * Function to indicate whether message should be displayed as a sticky message 
     * or a timed message. If sticky message is desired set flag to true.
     * @param sticky
     *  Indicates whether or not a message should be displayed as a sticky growl
     *  message or a timed growl message. If value is set to true then the message
     *  will be displayed as a sticky message.
     */
    public void setSticky(final boolean sticky)
    {
        m_Sticky = sticky;
    }
    
    /**
     * Function to return the value of the summary field for a Growl message.
     * 
     * @return
     *  the string text for the summary field of a Growl message.
     */
    public String getSummary()
    {
        return m_Summary;
    }
    
    /**
     * Function to return the value of the detail field for a Growl message.
     * 
     * @return
     *  the string text for the detail field of a Growl message.
     */
    public String getDetail()
    {
        return m_Detail;
    }
    
    /**
     * Function to return the value of the severity field for a Growl message.
     * @return
     *  the string representation of the severity level of the Growl message. Possible
     *  values include info, warn, error, fatal.
     */
    public String getSeverity()
    {
        return m_Severity;
    }
    
    /**
     * Function to return the value indicating whether the Growl message should be 
     * sticky or timed.
     * @return
     *  a boolean value which indicates the desired Growl message display method.
     *  if true, display a sticky message.
     */
    public boolean getSticky() //NOPMD: must be get sticky otherwise field will not be JSONified by PrimeFaces
    {
        return m_Sticky;
    }
    
    /**
     * Converts a faces message severity and converts it to a string representation.
     * @param severity
     *  the severity that the message should be set to.
     * @return
     *  the string representation of a FacesMessage severity
     */
    private String convertSeverity(final FacesMessage.Severity severity)
    {
        final String answer; 
        
        if (severity.equals(FacesMessage.SEVERITY_WARN))
        {
            answer = "warn";
        }
        else if (severity.equals(FacesMessage.SEVERITY_ERROR))
        {
            answer = "error";
        }
        else if (severity.equals(FacesMessage.SEVERITY_FATAL))
        {
            answer = "fatal";
        }
        else
        {
            answer = "info";
        }
        
        return answer;
    }

    @Override
    public void setType(final PushMessageType type)
    {
      //this function is needed by the JSONifier so that object is properly converted
    }

    @Override
    public String getType()
    {
        return m_Type.toString();
    }
    
    @Override
    public String toString()
    {
        final String toString = String.format(
                "%s { summary: '%s' detail: '%s' severity: '%s' sticky: '%s' }",
                printMessageType(), m_Summary, m_Detail, m_Severity, m_Sticky);
        
        return toString;
    }
}
