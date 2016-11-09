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
package mil.dod.th.ose.datastream;

import java.net.URI;

import mil.dod.th.core.datastream.StreamProfileContext;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;

/**
 * Contains methods available internally to the {@link mil.dod.th.core.datastream.StreamProfile} interface
 * but not to external consumers.
 * 
 * @author jmiller
 *
 */
public interface StreamProfileInternal extends StreamProfileContext, FactoryObjectInternal
{
    /**
     * ID of the component factory for this class.
     */
    String COMPONENT_FACTORY_REG_ID = "mil.dod.th.ose.core.impl.datastream.StreamProfileInternal";
    
    /**
     * Key value pair to denote an stream profile service type component.
     */
    String SERVICE_TYPE_PAIR = FactoryObjectInternal.SERVICE_TYPE_KEY + "=StreamProfile";
    
    @Override
    FactoryInternal getFactory();
    
    /**
     * Set the stream port URI.
     * 
     * @param streamPort the client-facing URI for the output stream, assigned by 
     * {@link mil.dod.th.core.datastream.DataStreamService}
     */
    void setStreamPort(URI streamPort);

    /**
     *  Get the URI for the data source.
     *  
     * @return URI for the profile's data stream.
     */
    URI getDataSource();

}
