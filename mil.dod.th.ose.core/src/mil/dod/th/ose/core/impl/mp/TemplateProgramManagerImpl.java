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
package mil.dod.th.ose.core.impl.mp;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.shared.SystemConfigurationConstants;
import mil.dod.th.ose.utils.FileService;
import mil.dod.th.ose.utils.xml.XmlUtils;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

/**
 * Implementation of the {@link TemplateProgramManager}.
 * 
 * @author callen
 *
 */
@Component
public class TemplateProgramManagerImpl implements TemplateProgramManager 
{
    /**
     * Set of all known mission program templates. The key value is the name of the template, and the value is that
     * template's UUID.
     */
    private final Map<String, UUID> m_Templates = Collections.synchronizedMap(new HashMap<String, UUID>());
    
    /**
     * Used to log messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Service for storing programs.
     */
    private PersistentDataStore m_PersistentDataStore;
    
    /**
     * Service for accessing files.
     */
    private FileService m_FileService;
    
    /**
     * Service that validates mission programs, used before the persistence of mission programs.
     */
    private Validator m_MissionValidator;
    
    /**
     * Service for creating and validating XML objects into JAXB objects.
     */
    private XmlUnmarshalService m_UmarshalService;
    
    /**
     * Bind the file service.
     * 
     * @param fileService
     *     the service used to access files
     */
    @Reference
    public void setFileService(final FileService fileService)
    {
        m_FileService = fileService;
    }
    
    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Bind the persistent data store.
     * 
     * @param persistentDataStore
     *      used to store the set of mission programs
     */
    @Reference
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        m_PersistentDataStore = persistentDataStore;
    }
    
    /**
     * Bind the MissionProgramValidator service.
     * 
     * @param missionProgramValidator
     *     Validator service needed to validate MissionProgramInstances.
     */
    @Reference
    public void setMissionProgramValidator(final Validator missionProgramValidator)
    {
        m_MissionValidator = missionProgramValidator;
    }
    
    /**
     * Set the XMLUmarshal service.
     * 
     * @param xmlUnmarshalService
     *     the service used to unmarshal incoming xml documents
     */
    @Reference
    public void setXMLUnmarshalService(final XmlUnmarshalService xmlUnmarshalService)
    {
        m_UmarshalService = xmlUnmarshalService;
    }
    
    /**
     * This activates the component. It loads all the templates from the database, and provides the
     * service of making templates available to the {@link mil.dod.th.core.mp.MissionProgramManager}.
     * 
     * @param context
     *      context of the bundle that contains this component
     */
    @Activate
    public void activate(final BundleContext context)
    {
        // load saved templates
        final Collection<? extends PersistentData> query = m_PersistentDataStore.query(this.getClass());
        //if no templates are found in the database
        if (query.isEmpty())
        {
            m_Logging.info("No templates were found in the data store to restore");
        }
        else
        {
            //restore each entity
            for (PersistentData entity : query)
            {
                final byte[] data = (byte[])entity.getEntity();
                //add the deserialized entities to the list of managed templates
                m_Templates.put(((MissionProgramTemplate)(XmlUtils.fromXML(data, MissionProgramTemplate.class))).
                    getName(), entity.getUUID());
            }            
        }
        
        //look in templates folder for templates
        final File dataDir = new File(context.getProperty(SystemConfigurationConstants.DATA_DIR_PROPERTY));
        final File templateDir = m_FileService.getFile(dataDir, "templates");
        
        loadFromDirectory(templateDir);
        
        //recap, if there are no templates
        if (getMissionTemplateNames().isEmpty())
        {
            m_Logging.info("Templates were not found in template directory nor the datastore");
        }
        else
        {
            m_Logging.info("Mission templates restored: %s", getMissionTemplateNames());
        }
    }
    
    @Override
    public void loadMissionTemplate(final MissionProgramTemplate template) throws IllegalArgumentException, 
            PersistenceFailedException
    {
        //TODO: TH-805, support syncing of templates, need to be able to preserve unique names,
        //but also allow for templates to be update if wanted.
        synchronized (template)
        {
            m_Templates.remove(template.getName());
            // the unique identifier of the template to add
            final UUID templateUUID = UUID.randomUUID();
            persistTemplate(template, templateUUID);
            m_Templates.put(template.getName(), templateUUID);
        }
        m_Logging.info("Template %s was added to the store of mission program templates", template.getName());
    }

    @Override
    public Set<String> getMissionTemplateNames() 
    {
        final Set<String> templateNames = m_Templates.keySet();
        return Collections.unmodifiableSet(templateNames);
    }
    
    @Override
    public MissionProgramTemplate getTemplate(final String name) 
    {
        final MissionProgramTemplate foundTemplate = findTemplate(name);
        if (foundTemplate == null)
        {
            throw new IllegalArgumentException("The mission template " + name + " does not exist");
        }
        else
        {            
            return foundTemplate;
        }
    }

    @Override
    public void removeTemplate(final String name)
    {
        final UUID foundUUID = m_Templates.get(name);
        //remove from the list of managed templates
        if (foundUUID ==  null)
        {
            throw new IllegalArgumentException("Template " + name + " is not currently recognized or has already been"
                 + " removed");
        }
        m_PersistentDataStore.remove(foundUUID);
        m_Templates.remove(name);
        m_Logging.info("Template %s was removed from the Template Program Manager", name);
    }
    
    @Override
    public Set<MissionProgramTemplate> getTemplates()
    {
        final Set<MissionProgramTemplate> templates = new HashSet<MissionProgramTemplate>();
        //iterate through known templates
        synchronized (m_Templates)
        {
            for (String templateName : m_Templates.keySet())
            {
                templates.add(findTemplate(templateName));
            }
        }
        
        //return the list of templates, these are copies
        return templates;
    }
    
    @Override
    public void loadFromFile(final URL url, final boolean overWrite) throws MalformedURLException, UnmarshalException, 
            IllegalArgumentException, PersistenceFailedException, IllegalStateException 
    {
        //create template from the URL
        final MissionProgramTemplate template = m_UmarshalService.getXmlObject(MissionProgramTemplate.class, url);
        
        //if overwriting, remove the old template from the template map and the datastore
        if (overWrite)
        {
            //remove the uuid 
            final UUID toRemove = m_Templates.remove(template.getName());
            if (toRemove != null)
            { 
                //if the uuid was found remove the entity from the datastore
                m_PersistentDataStore.remove(toRemove);
            }
        }
        
        //attempt to load new template to manager
        loadMissionTemplate(template);
    }
    
    @Override
    public void loadFromStream(final InputStream stream, final boolean overWrite) throws UnmarshalException, 
            IllegalArgumentException, PersistenceFailedException, IllegalStateException
    {
        final MissionProgramTemplate template = m_UmarshalService.getXmlObject(MissionProgramTemplate.class, stream);
        
        //if overwriting, remove the old template from the template map and the datastore
        if (overWrite)
        {
            //remove the uuid 
            final UUID toRemove = m_Templates.remove(template.getName());
            if (toRemove != null)
            { 
                //if the uuid was found remove the entity from the datastore
                m_PersistentDataStore.remove(toRemove);
            }
        }
        
        //attempt to load new template to manager
        loadMissionTemplate(template);
    }
    
    /**
     * Persist the template within the {@link PersistentDataStore}. 
     * 
     * @param template
     *     the template to persist
     * @param templateUUID
     *     the unique identifier of the template to persist       
     * @throws PersistenceFailedException
     *     thrown if the data was unable to be persisted 
     * @throws IllegalArgumentException
     *     thrown if the persisted data, description, uuid, or context parameters are invalid when making the call to
     *     persist the template 
     */
    private synchronized void persistTemplate(final MissionProgramTemplate template, final UUID templateUUID) throws 
            IllegalArgumentException, PersistenceFailedException
    {
        try
        {
            m_MissionValidator.validate(template);
        }
        catch (final ValidationFailedException | IllegalStateException ex)
        {
            throw new IllegalArgumentException("Template failed validation!", ex);
        }
        //serialize the validated template
        final byte[] bArray = XmlUtils.toXML(template, false);
        //try to persist the data
        m_PersistentDataStore.persist(this.getClass(), templateUUID, template.getName(), bArray);
    }
    
    /**
     * Finds an individual template within the template manager.
     * 
     * @param name
     *     the name of the template to find
     * @return
     *     mission program template with the matching name passed
     */
    private MissionProgramTemplate findTemplate(final String name)
    {
        UUID foundUUID = null;
        synchronized (m_Templates)
        {
            for (Entry<String, UUID> template : m_Templates.entrySet())
            {
                //if the name matches pull out the uuid
                if (template.getKey().equalsIgnoreCase(name))
                {
                    foundUUID = template.getValue();
                }
            }
        }
        //if the uuid is still null then return null, the null is caught in the receiving method
        if (foundUUID == null)
        {
            return null;
        }
        //use the uuid of the template to query the data store
        final PersistentData persistTemp = m_PersistentDataStore.find(foundUUID);
        final byte[] serTemplate = (byte[])persistTemp.getEntity();
        return (MissionProgramTemplate)XmlUtils.fromXML(serTemplate, MissionProgramTemplate.class);
    }
    
    /**
     * Load templates from the 'templates' directory. 
     * @param directory
     *      the string representation of the path to the template directory
     */
    private void loadFromDirectory(final File directory) 
    {    
        m_Logging.log(LogService.LOG_DEBUG, "The absolute path for the template dir is configured to be: %s", 
            directory.getAbsolutePath());
        
        //check that the directory does in fact exist.
        if (directory.exists())
        {
            for (File file : directory.listFiles())
            {
                try 
                {
                    loadFromFile(file.toURI().toURL(), true);
                } 
                catch (final Exception exception) 
                {
                    m_Logging.error(exception, "The XML template at %s cannot be loaded", file.getAbsolutePath());
                }
            }
        }
    }
}
