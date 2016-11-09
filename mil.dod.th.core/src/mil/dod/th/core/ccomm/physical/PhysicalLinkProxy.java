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
package mil.dod.th.core.ccomm.physical;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;

/**
 * A physical link plug-in implements this interface (or sub-interface like {@link SerialPortProxy}) providing any 
 * custom behavior of the plug-in that is needed to interact with the hardware. The core will perform some functions 
 * automatically such as tracking if the physical link is in use.
 * 
 * @author dhumeniuk
 *
 */
public interface PhysicalLinkProxy extends FactoryObjectProxy
{
    /**
     * Called to initialize the object and provide the plug-in with the {@link PhysicalLinkContext} to interact with the
     * rest of the system.
     * 
     * @param context
     *      context specific to the {@link PhysicalLink} instance
     * @param props
     *      the physical link's configuration properties, available as a convenience so {@link 
     *      PhysicalLinkContext#getProperties()} does not have to be called
     * @throws FactoryException
     *      if there is an error initializing the object.
     */
    void initialize(PhysicalLinkContext context, Map<String, Object> props) throws FactoryException;
    
    /**
     * Open the link. This may be a no-op for some links, while others require maybe an underlying OS file to be opened.
     * 
     * @throws PhysicalLinkException
     *      Implementation of port fails to open
     */
    void open() throws PhysicalLinkException;

    /**
     * Close the link.
     * 
     * @throws PhysicalLinkException
     *      Implementation of port fails to close
     */
    void close() throws PhysicalLinkException;
    
    /**
     * Determine if the link is currently open.
     * 
     * @return True if the link is open and false if close. 
     */
    boolean isOpen();
    
    /**
     * Get the input stream associated with the physical link for reading data.  Typically the implementer of the 
     * proxy will also implement an {@link InputStream} class.
     * 
     * @return An implementation of an {@link InputStream} for the link
     * 
     * @throws PhysicalLinkException
     *      Implementation of link is unable to produce an input stream to read data. It is possible the link doesn't 
     *      support reading from a stream.
     */
    InputStream getInputStream() throws PhysicalLinkException;

    /**
     * Get the output stream for writing out data to the connected device. Typically the implementer of the proxy will 
     * also implement an {@link OutputStream} class.
     * 
     * @return An implementation of an {@link OutputStream} for the port
     * 
     * @throws PhysicalLinkException
     *      Implementation of link is unable to produce an output stream to write data. It is possible the link doesn't 
     *      support writing to a stream.
     */
    OutputStream getOutputStream() throws PhysicalLinkException;
}
