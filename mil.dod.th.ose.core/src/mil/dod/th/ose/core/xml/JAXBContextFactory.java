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
package mil.dod.th.ose.core.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * Interface for creating instances of a JAXBContext object.
 * 
 * @author bachmakm
 *
 */
public interface JAXBContextFactory
{
    /**
     * Used to get an instance of a JAXBContext object based on specific class.  The same context will be returned each
     * time for the same class as only one is needed.
     * 
     * @param <T>
     *      object type for which JAXBContext needs to be created.  
     * @param clazz
     *      the class containing JAXB annotations
     * @return
     *      JAXB Context
     * @throws JAXBException
     *      if instance of JAXBContext cannot be created.   
     */
    <T> JAXBContext getContext(Class<T> clazz) throws JAXBException;

}
