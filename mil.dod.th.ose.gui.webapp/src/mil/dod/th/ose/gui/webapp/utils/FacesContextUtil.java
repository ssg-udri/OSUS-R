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
package mil.dod.th.ose.gui.webapp.utils;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.faces.context.FacesContext;

import org.primefaces.context.RequestContext;

/**
 * Service that retrieves the FacesContext and RequestContext from the JSF page.
 * 
 * @author cweisenborn
 */
@Startup
@Singleton
public class FacesContextUtil
{      
    /**
     * Method returns the current FacesContext from the JSF page.
     * 
     * @return
     *          The FacesContext of the current JSF page.
     */
    public FacesContext getFacesContext()
    {
        return FacesContext.getCurrentInstance();
    }
    
    /**
     * Method returns the current RequestContext from the JSF page.
     * 
     * @return
     *          The RequestContext of the current JSF page.
     */
    public RequestContext getRequestContext()
    {
        return RequestContext.getCurrentInstance();
    }
}
