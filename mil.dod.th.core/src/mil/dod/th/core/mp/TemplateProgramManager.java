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
package mil.dod.th.core.mp;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.persistence.PersistenceFailedException;

/**
 * This interface assists in the creation and storage of mission templates. The templates stored within this
 * manager are required to have a unique name. Multiple mission programs may be created using the same template.
 * The Template Program Manager interacts in conjunction with the {@link mil.dod.th.core.mp.MissionProgramManager} to 
 * create new mission programs. 
 * Must be provided as an OSGi service by the core. 
 * 
 * @author callen
 *
 */
@ProviderType
public interface TemplateProgramManager 
{

    /**
     * Loads a new mission template. The template will be validated and required to have a 
     * unique name. The source code is also required, but does not need to be unique.
     * TODO: TH-805, support syncing of templates, need to be able to preserve unique names, but also allow for 
     * templates to be update if wanted.
     * 
     * @param template
     *     the mission program template to add
     * @exception PersistenceFailedException
     *     thrown in the event that the template is not properly persisted to the 
     *     {@link mil.dod.th.core.persistence.PersistentDataStore}
     * @throws IllegalArgumentException
     *     thrown in the event that the template fails validation before being persisted
     */
    void loadMissionTemplate(MissionProgramTemplate template) throws PersistenceFailedException, 
            IllegalArgumentException;
    
    /**
     * Get a set of all known {@link MissionProgramTemplate} names.
     * 
     * @return
     *    a set of names representing all known mission program templates
     */
    Set<String> getMissionTemplateNames();
    
    /**
     * This will return the mission program template that is correlated to the name given. All mission program templates
     * are required to have unique names.
     * 
     * @param name
     *     the name of the template to retrieve
     * @return
     *     the mission program template that is represented by the name passed
     * @exception IllegalArgumentException
     *     thrown in the event that the template requested is not known to the manager   
     */
    MissionProgramTemplate getTemplate(String name) throws IllegalArgumentException;
    
    /**
     * This will remove the template with the given name. It is important to know that multiple missions may depend on
     * a single template and that by removing the template these managed programs within the 
     * {@link MissionProgramManager} will fail if the system is reset.
     * 
     * @param name
     *     the name of the template to remove
     * @exception IllegalArgumentException
     *     if the template specified is already removed, or otherwise unknown 
     */
    void removeTemplate(String name) throws IllegalArgumentException;
    
    /**
     * Get a set of all templates known to this system. The templates returned are all copies, changes made to these
     * copies will not affect the version of the template being stored within this system. 
     *
     * @return
     *     a set of all templates  
     */
    Set<MissionProgramTemplate> getTemplates();
    
    /**
     * Load a template from a URL. Any templates loaded with this method may over-write a template 
     * in the template manager. Overwriting can affect mission programs already created using 
     * that template. These effects may not be known until a system re-start because the 
     * {@link MissionProgramManager} creates mission programs from copies of the templates. Any 
     * NEW mission programs using an updated template will reflect the changes.
     * Denoting true for overwrite will overwrite a template within the manager if it has the same template name.
     * A false value assumes that the incoming XML based template has a name that does not match a template already in 
     * the manager. 
     * TODO: TH-805, support syncing of templates
     * If the template does have the same name the template will not be able to be loaded. 
     * 
     * @param url
     *    the URL where an entity is that contains an XML template to load to the manager
     * @param overWrite
     *    flag that denotes if this new template can overwrite
     * @throws PersistenceFailedException
     *    if the template fails to persist
     * @throws IllegalArgumentException
     *     thrown in the event that the template fails validation before being persisted
     * @throws UnmarshalException
     *    if the unmarshalling service cannot validate and create a JAXB object from the XML
     * @throws MalformedURLException
     *    if the URL to the file is not well-formed
     * @throws IllegalStateException
     *    if the schema to validate the template against cannot be found
     */
    void loadFromFile(URL url, boolean overWrite) throws MalformedURLException, UnmarshalException, 
            PersistenceFailedException, IllegalArgumentException, IllegalStateException;
    
    /**
     * Load a template from an input stream. Any template loaded with this method may over-write a template currently
     * in the template manager. Overwriting can affect mission programs already created using that template. These
     * effects may not be known until the system re-starts because the {@link MissionProgramManager} creates mission
     * programs from copies of the templates. NEW mission programs using an updated template will reflect the changes.
     * Denoting true for overwrite will overwrite a template within the manager if it has the same template name.
     * A false value assumes that the incoming XML based template has a name that does not match a template already in 
     * the manager. 
     * TODO: TH-805, support syncing of templates
     * If the template does have the same name the template will not be able to be loaded.
     * 
     * @param stream
     *      The input stream that contains the XML template to be loaded to the template manager.
     * @param overWrite
     *      The flag that denotes if this new template can overwrite.
     * @throws UnmarshalException
     *      Thrown if the unmarshalling service cannot validate and create a JAXB object from the stream.
     * @throws PersistenceFailedException
     *      Thrown if the template fails to be persisted in the persistent data store.
     * @throws IllegalArgumentException
     *     thrown in the event that the template fails validation before being persisted
     * @throws IllegalStateException
     *    if the schema to validate the template against cannot be found
     */
    void loadFromStream(InputStream stream, boolean overWrite) throws UnmarshalException, PersistenceFailedException, 
            IllegalArgumentException, IllegalStateException;
}
