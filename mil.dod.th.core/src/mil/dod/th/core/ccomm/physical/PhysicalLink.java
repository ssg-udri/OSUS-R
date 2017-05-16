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

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObject;


/**
 * This interface represents the physical link layer of the OSI model.
 * 
 * <p>
 * This is the physical link, that other links should extend, see {@link SerialPort} for an example.  
 * 
 * A physical link is assumed to be used in the following order:
 * <ol>
 * <li>Open the link by calling {@link #open()}</li>
 * <li>Use the input/output streams to read/write data by calling {@link #getInputStream()} and 
 * {@link #getOutputStream()}</li>
 * <li>Close the link to release any resources by calling {@link #close()}</li>
 * </ol>
 * 
 * <p>
 * All functionality of a physical link needed by the consumer should be available through this interface. It should 
 * never be necessary to use the {@link PhysicalLinkProxy} interface implemented by the plug-in. For example, if 
 * SerialPortLink implements {@link PhysicalLinkProxy}, consumers should not directly access SerialPortLink methods, but
 * instead access this interface.
 * 
 * <p>
 * Instances of a PhysicalLink are managed (created, tracked, deleted) by the core. This interface should never be 
 * implemented by a plug-in. Instead, a plug-in implements {@link PhysicalLinkProxy} to define custom behavior that is 
 * invoked when consumers use this interface. To interact with a physical link, use the {@link 
 * mil.dod.th.core.ccomm.CustomCommsService}.
 */
@ProviderType
public interface PhysicalLink extends FactoryObject
{
    /** 
     * Each {@link PhysicalLinkProxy} implementation must provide a {@link org.osgi.service.component.ComponentFactory} 
     * with the factory attribute set to this constant.
     * 
     * <p>
     * For example:
     * 
     * <pre>
     * {@literal @}Component(factory = PhysicalLink.FACTORY)
     * public class MyPhysicalLink implements PhysicalLinkProxy
     * {
     *     ...
     * </pre>
     */
    String FACTORY = "mil.dod.th.core.ccomm.physical.PhysicalLink";
    
    /**
     * Property set on each {@link mil.dod.th.core.factory.FactoryDescriptor} service registered for each physical link 
     * type. Value should be equal to {@link 
     * mil.dod.th.core.ccomm.physical.capability.PhysicalLinkCapabilities#getLinkType()}.toString().
     * 
     * When a factory is registered, the {@link mil.dod.th.core.ccomm.CustomCommsService} will register a service with 
     * the {@link mil.dod.th.core.factory.FactoryDescriptor} interface to allow other bundles to track when the physical
     * link type is available.
     */
    String LINK_TYPE_SERVICE_PROPERTY = FactoryObject.TH_PROP_PREFIX + ".link.type";
    
    /**
     * {@link mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum#SERIAL_PORT} target constant to be used for reference
     * binding.
     */
    String FILTER_SERIAL_PORT = 
        "(" + LINK_TYPE_SERVICE_PROPERTY + "=SerialPort)"; //NOCHECKSTYLE:Parentheses are needed to complete the filter
    
    /**
     * {@link mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum#GPIO} target constant to be used for reference binding.
     */
    String FILTER_GPIO = "(" + LINK_TYPE_SERVICE_PROPERTY + "=GPIO)";
    
    /**
     * {@link mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum#SPI} target constant to be used for reference binding.
     */
    String FILTER_SPI = "(" + LINK_TYPE_SERVICE_PROPERTY + "=SPI)";
    
    /**
     * {@link mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum#I_2_C} target constant to be used for reference binding.
     */
    String FILTER_I2C = "(" + LINK_TYPE_SERVICE_PROPERTY + "=I2C)";

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
     * returns if the PhysicalLink is currently in use.
     * 
     * @return True if in use, false otherwise. 
     */
    boolean isInUse();

    /**
     * Get which link layer owns the physical link.
     * 
     * @return
     *      which link layer owns the physical link or null if not in use or in use by something other than a link layer
     */
    LinkLayer getOwner();
    
    /**
     * Determine if the link is currently open.
     * 
     * @return True if the link is open and false if close. 
     */
    boolean isOpen();
    
    /**
     * Get the input stream associated with the physical link for reading data.  Typically the implementer of the 
     * physical link will also implement an InputStream class.
     * 
     * @return An implementation of an {@link InputStream} for the link
     * 
     * @throws PhysicalLinkException
     *      Implementation of link is unable to produce an input stream to read data. It is possible the link doesn't 
     *      support reading from a stream.
     */
    InputStream getInputStream() throws PhysicalLinkException;

    /**
     * Get the output stream for writing out data to the connected device. Typically the implementer of the physical 
     * link will also implement an OutputStream class.
     * 
     * @return An implementation of an {@link OutputStream} for the port
     * 
     * @throws PhysicalLinkException
     *      Implementation of link is unable to produce an output stream to write data. It is possible the link doesn't 
     *      support writing to a stream.
     */
    OutputStream getOutputStream() throws PhysicalLinkException;

    /**
     * Set the timeout value for all reads.
     * 
     * @param timeoutMS
     *            Time in milliseconds. If the value is negative, timeout is disabled.
     * @throws IllegalArgumentException
     *             no longer thrown, may be removed in a later version
     * @throws FactoryException
     *             if there is an error in setting the read timeout
     */
    void setReadTimeout(int timeoutMS) throws IllegalArgumentException, FactoryException;
    
    /**
     * Set the number of bits per transmission cycle.
     *
     * @param bits
     *            how may bits
     * @throws IllegalArgumentException
     *            if data bits value is less than 1
     * @throws FactoryException
     *            if there is an error in setting the data bits value
     */
    void setDataBits(int bits) throws IllegalArgumentException, FactoryException;
    
    /**
     * Releases exclusive access to the physical link. After this point, the calling module should no longer
     * reference this physical link. The physical link cannot be released while open.
     * 
     * @throws IllegalStateException
     *      if the physical link is not in use, or is still open
     */
    void release() throws IllegalStateException;
    
    /**
     * Same as {@link FactoryObject#getFactory()}, but returns the physical link specific factory.
     * 
     * @return
     *      factory for the physical link
     */
    @Override
    PhysicalLinkFactory getFactory();
    
    /**
     * Get the configuration for the physical link.
     * 
     * <p>
     * <b>NOTE: the returned interface uses reflection to retrieve configuration and so values should be cached once 
     * retrieved</b>
     * 
     * @return
     *      configuration attributes for the physical link
     */
    PhysicalLinkAttributes getConfig();
}
