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
package edu.udayton.udri.asset.hikvision;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import aQute.bnd.annotation.component.Component;

/**
 * Processing the URL into a HttpURLConnection.
 * @author Noah
 */
@Component(provide = UrlUtil.class)
public class UrlUtil
{
    /**
     * Method for returning the url.
     * @param url
     *     The URL you want to open the connection
     * @return A HttpURLConnection open connection
     * @throws IOException 
     *     Did not open connection.
     */
    public HttpURLConnection getConnection(final URL url) throws IOException 
    {
        return (HttpURLConnection) url.openConnection();  
    }
}
