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
package edu.udayton.udri.asset.gps;

import java.io.IOException;
import java.io.InputStream;

/**
 * Buffered reader that delimits by new line characters.  Carriage returns will be ignored so the line reader will work
 * for both CR+LF and LF only line delimiting.
 * 
 * @author allenchl
 *
 */
public class LineReader
{
    /**
     * The input stream from which this reader reads from.
     */
    private final InputStream m_In;
    
    /**
     * Constructor that will set the input stream to read from.
     * @param inStream
     *      the input stream from which to read
     */
    public LineReader(final InputStream inStream)
    {
        m_In = inStream;
    }
    
    /**
     * Read the next line from the input stream.
     * @return
     *      string representation of the data read from the input stream
     * @throws IOException
     *      if the read times out or another IOException occurs
     */
    public String readLine() throws IOException
    {
        final StringBuilder line = new StringBuilder();
        while (true)
        {
            final int readValue = m_In.read();
            if (readValue == -1)
            {
                return null;
            }
            final char readChar = (char)readValue;
            
            switch (readChar)
            {
                case '\r':
                    // ignore, nothing to add
                    break;
                case '\n':
                    return line.toString();
                default:
                    line.append(readChar);
                    break;
            }
        }
    }
    
    /**
     * Close the underlying input stream.
     * @throws IOException 
     *      if the resource is unable to be closed
     */
    public void close() throws IOException
    {
        m_In.close();
    }
}