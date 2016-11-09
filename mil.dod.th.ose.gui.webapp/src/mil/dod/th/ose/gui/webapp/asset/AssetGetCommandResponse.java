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
package mil.dod.th.ose.gui.webapp.asset;

import java.util.Date;

import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.ose.gui.webapp.utils.DateTimeConverterUtil;

/**
 * Class that holds the last get command response received.
 * @author nickmarcucci
 *
 */
public class AssetGetCommandResponse
{
    /**
     * The date and time that the get command response was received.
     */
    private final Date m_TimeReceived;
    
    /**
     * The response of the get command.
     */
    private final Response m_ResponseReceived;
    
    /**
     * Constructor.
     * @param response
     *  the response that is to be set
     * @param date
     *  the date of the response that is to be set
     */
    public AssetGetCommandResponse(final Response response, final Date date)
    {
        m_ResponseReceived = response;
        m_TimeReceived = date;
    }
    
    /**
     * Return a printed message of the time the get command 
     * response was received.
     * @return
     *  a formatted date string or a default message if no response
     *  has been received
     */
    public String printTimeMessage()
    {
        if (m_TimeReceived == null)
        {
            return "N/A";
        }
        
        return DateTimeConverterUtil.formatDateFromLong(m_TimeReceived.getTime());
    }
    
    /**
     * Return a printed message of the response received.
     * @return
     *  the string representation of the response or a default message
     *  if no response has been received.
     */
    public String printResponseMessage()
    {
        if (m_ResponseReceived == null)
        {
            return "No Message Response has been received.";
        }
        
        return m_ResponseReceived.toString();
    }
}
