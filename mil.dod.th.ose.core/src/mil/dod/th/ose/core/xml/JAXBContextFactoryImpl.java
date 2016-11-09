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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import aQute.bnd.annotation.component.Component;

/**
 * Implementation class for creating context for XML documents.
 * 
 * @author bachmakm
 *
 */
@Component
public class JAXBContextFactoryImpl implements JAXBContextFactory
{
    /**
     * Map of all the JAXB contexts keyed by class type.
     */
    final private Map<Class<?>, JAXBContext> m_ContextMap = 
            Collections.synchronizedMap(new HashMap<Class<?>, JAXBContext>());
    
    @Override
    public <T> JAXBContext getContext(final Class<T> clazz) throws JAXBException
    {
        JAXBContext context;
        synchronized (m_ContextMap)
        {
            context = m_ContextMap.get(clazz);
            if (context == null)
            {
                context = JAXBContext.newInstance(clazz);
                m_ContextMap.put(clazz, context);
            }
        }
        
        return context;
    }
}
