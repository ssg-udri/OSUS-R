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
package mil.dod.th.ose.junit4xmltestrunner;

import java.io.IOException;
import java.io.OutputStream;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import org.osgi.service.log.LogService;

/**
 * Output stream writes data to the OSGi log service by splitting messages up for each new line.
 * 
 * @author dhumeniuk
 *
 */
@Component(provide = LogOutputStream.class)
public class LogOutputStream extends OutputStream
{
    /**
     * String representing a new line separator.
     */
    private static final String LINE_SEP = System.getProperty("line.separator");

    /**
     * OSGi log service reference.
     */
    private LogService m_LogService;
    
    /**
     * Piece of message left over from last write that wasn't terminated with '\n'.
     */
    private String m_LeftOver;

    /**
     * Bind the OSGi log service.
     * 
     * @param logService
     *      OSGi log service
     */
    @Reference
    public void setLogService(final LogService logService)
    {
        m_LogService = logService;
    }
    
    @Override
    public void write(final int dataByte) throws IOException
    {
        write(new byte[] {(byte)dataByte}, 0, 1);
    }
    
    @Override
    public void write(final byte[] buf, final int off, final int len)
    {
        final String str = new String(buf, off, len);
        final boolean endsWithLn = str.endsWith(LINE_SEP);
        final String[] lines = str.split(LINE_SEP);
        
        if (lines.length == 0)
        {
            m_LogService.log(LogService.LOG_INFO, m_LeftOver);
            m_LeftOver = null; // NOPMD assigning to null (using to keep track of an empty message)
        }
        
        for (int i = 0; i < lines.length; i++)
        {
            final StringBuilder message = new StringBuilder(lines[i]);
            if (i == 0 && m_LeftOver != null)
            {
                message.append(m_LeftOver);
            }
            
            if (i == lines.length - 1 && !endsWithLn)
            {
                if (m_LeftOver == null)
                {
                    m_LeftOver = lines[i];
                }
                else
                {
                    m_LeftOver += lines[i];
                }
                break; // wait to emit next time
            }
            
            m_LogService.log(LogService.LOG_INFO, message.toString());
        }
    }
}
