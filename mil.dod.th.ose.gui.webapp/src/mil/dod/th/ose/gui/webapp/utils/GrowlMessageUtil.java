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
package mil.dod.th.ose.gui.webapp.utils;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;

import mil.dod.th.core.log.Logging;
import mil.dod.th.ose.gui.webapp.utils.push.PushChannelMessageManager;
import mil.dod.th.ose.gui.webapp.utils.push.PushGrowlMessage;

import org.osgi.service.log.LogService;


/**
 * Service used to create growl messages.
 * 
 * @author cweisenborn
 */
@Startup
@Singleton
public class GrowlMessageUtil
{
    /**
     * String representation of the id of the growl tag which handles messages that do not timeout.
     */
    public static final String GROWL_STICKY_ID = "growl-sticky";
    
    /**
     * String representation of the id of the growl tag which handles messages that have a timeout value.
     */
    public static final String GROWL_TIMED_ID = "growl-timed";
    
    /**
     * Push manager service that is used to push messages.
     */
    @Inject
    private PushChannelMessageManager m_PushManager;
    
    /**
     * Sets the push manager service to use.
     * @param manager
     *  the manager service to use.
     */
    public void setPushManager(final PushChannelMessageManager manager)
    {
        m_PushManager = manager;
    }
    
    /**
     * Method that creates a FacesMessage from the given parameters and then updates growl to display that messsage 
     * on the current page via the PrimeFaces Push API. This method will also log the details to the logging 
     * service. Creating a message this way will use the default growl display settings. 
     * Error and warning messages are set to be displayed till closed and information messages are set to 
     * use a default timeout.
     * 
     * @param severity
     *  Message severity level.
     * @param summary
     *  Summary of why the message is being displayed.
     * @param detail
     *  Further details about why the message is being displayed.
     */
    public void createGlobalFacesMessage(final FacesMessage.Severity severity, final String summary, 
            final String detail)
    {
        createGlobalFacesMessage(severity, summary, detail, null);
    }
    
    /**
     * Method that creates a FacesMessage from the given parameters and then updates growl to display that messsage 
     * on the current page via the PrimeFaces Push API. This method will also log the details to the logging 
     * service. Error and warning messages are set to be displayed till closed and information messages are set to 
     * use a default timeout.
     * 
     * @param severity
     *  Message severity level.
     * @param summary
     *  Summary of why the message is being displayed.
     * @param detail
     *  Further details about why the message is being displayed.
     * @param exception
     *  Exception to log
     */
    public void createGlobalFacesMessage(final FacesMessage.Severity severity, final String summary, 
            final String detail, final Exception exception)
    {
        createGlobalFacesMessage(severity, summary, detail, exception, false);
    }
    
    /**
     * Method that creates a FacesMessage from the given parameters and then updates growl to display 
     * that messsage on the current page via the PrimeFaces Push API. This method will also log the details to the 
     * logging service. Creating a message this way will use the default growl display settings. Error and warning 
     * messages are set to be displayed till closed and information messages are set to use a default timeout.
     * 
     * @param severity
     *   Message severity level.
     * @param summary
     *  Summary of why the message is being displayed.
     * @param detail
     *  Further details about why the message is being displayed.
     * @param exception
     *  Exception to log
     * @param sticky
     *  True if the message should remain open until closed or false if the message should close after the 
     *  default timeout period. Warning, Error, Fatal messages will all default to sticky regardless of the
     *  settting passed to this method.
     */
    public void createGlobalFacesMessage(final FacesMessage.Severity severity, final String summary, 
            final String detail, final Exception exception, final boolean sticky)
    {
        //the message to be pushed
        final PushGrowlMessage message;
        
        if (sticky
                || severity.equals(FacesMessage.SEVERITY_ERROR) 
                || severity.equals(FacesMessage.SEVERITY_FATAL) 
                || severity.equals(FacesMessage.SEVERITY_WARN))
        {
            
            message = new PushGrowlMessage(severity, summary, detail, true);
        }
        else
        {
            message = new PushGrowlMessage(severity, summary, detail, false);
        }
        
        //format is channel you want to push to, data that is to be pushed.
        m_PushManager.addMessage(message);
        
        //log the message
        if (exception == null)
        {
            Logging.log(severityToLogLevel(severity), detail);
        }
        else
        {
            Logging.log(severityToLogLevel(severity), exception, detail);
        } 
    }
    
    
    
    /**
     * Method that creates a FacesMessage from the given parameters and then updates growl to display that message on
     * the current page. This method will also log the details to the logging service. Creating a message this way will
     * use the default growl display settings. Error and warning messages are set to be displayed till closed and 
     * information messages are set to use a default timeout that is determined by how many characters are in the 
     * message. 
     * @param severity
     *          Message severity level.
     * @param summary
     *          Summary of why the message is being displayed.
     * @param detail
     *          Further details about why the message is being displayed.
     */
    public void createLocalFacesMessage(final FacesMessage.Severity severity, final String summary, final String detail)
    {
        createLocalFacesMessage(severity, summary, detail, null);
    }
    
    /**
     * Method that creates a FacesMessage from the given parameters and then updates growl to display that message on
     * the current page. In addition to updating growl this method will log the exception and append the details to the
     * log entry. Creating a message this way will use the default growl display settings. Error and warning messages 
     * are set to be displayed till closed and information messages are set to use a default timeout that is determined 
     * by how many characters are in the message.
     * @param severity
     *          Message severity level.
     * @param summary
     *          Summary of why the message is being displayed.
     * @param detail
     *          Further details about why the message is being displayed.
     * @param exception
     *          Exception to log       
     */
    public void createLocalFacesMessage(final FacesMessage.Severity severity, final String summary,
            final String detail, final Exception exception)
    {
        createLocalFacesMessage(severity, summary, detail, exception, false);
    }
    
    /**
     * Method that creates a FacesMessage from the given parameters and then updates growl to display that message on
     * the current page. In addition to updating growl this method will log the exception and append the details to the
     * log entry. 
     * @param severity
     *          Message severity level.
     * @param summary
     *          Summary of why the message is being displayed.
     * @param detail
     *          Further details about why the message is being displayed.
     * @param exception
     *          Exception to log 
     * @param sticky
     *          True if the message should remain open until closed or false if the message should close after the 
     *          default timeout period. Warning, Error, Fatal messages will all default to sticky regardless of the
     *          settting passed to this method.      
     */
    public void createLocalFacesMessage(final FacesMessage.Severity severity, final String summary, final String detail,
            final Exception exception, final boolean sticky)
    {
        // TODO: TH-2059 this currently pushes globally to all sessions, but using the faces context will overwrite any
        // messages previously pushed so push local messages
        createGlobalFacesMessage(severity, summary, detail, exception, sticky);
    }
    
    /**
     * Correlate between the {@link javax.faces.application.FacesMessage} severity levels and 
     * {@link org.osgi.service.log.LogService} logging levels.
     * @param severity
     *     the severity set for the 'growl' message
     * @return
     *     integer value for the corresponding OSGi log service level    
     */
    private int severityToLogLevel(final FacesMessage.Severity severity)
    {
        final int ordinal = severity.getOrdinal();
        if (ordinal == FacesMessage.SEVERITY_WARN.getOrdinal())
        {
            return LogService.LOG_WARNING;
        }
        else if (ordinal == FacesMessage.SEVERITY_ERROR.getOrdinal() || ordinal == FacesMessage.SEVERITY_FATAL.
            getOrdinal())
        {
            return LogService.LOG_ERROR;
        }
        else
        {
            return LogService.LOG_INFO;
        }
    }
}
