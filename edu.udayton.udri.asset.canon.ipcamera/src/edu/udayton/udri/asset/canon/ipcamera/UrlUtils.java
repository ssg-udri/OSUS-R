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

package edu.udayton.udri.asset.canon.ipcamera;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import aQute.bnd.annotation.component.Component;

/** 
 * Helper Class.
 * @author Timothy
 */
@Component(provide = UrlUtils.class)
public class UrlUtils
{
    
    /**
     * Creates an open connection to the camera's URL.
     * @param url 
     *     URL to create an open connection to.
     * @return URLConnection
     *     A URLConnection to the given URL.
     * @throws IOException 
     *     Thrown if the connection isn't created
     */
    public URLConnection getConnection(final URL url) throws IOException
    {
        return url.openConnection();
    }
    
    /**
     * Wrapper for creating an InputStream. 
     * @param url
     *     Needed to create an input stream
     * @return InputStream
     *     An Input Stream to the given URL
     * @throws IOException 
     *     Throws if there an error creating the Input Stream
     */
    public InputStream getInputStream(final URL url) throws IOException
    {
        return url.openStream();
    }
    
    /**
     * Wrapper method for creating an InputStream.
     * @param urlConnection
     *     Uses a URLConnection to create an InputStream
     * @return InputStream
     *     An input Stream from the given URLConnection
     * @throws IOException 
     *     Throws if the connection isn't created.
     */
    public InputStream getInputStream(final URLConnection urlConnection) throws IOException
    {
        return urlConnection.getInputStream();
    }
}