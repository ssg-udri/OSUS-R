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
package mil.dod.th.ose.gui.webapp.general;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.xml.bind.annotation.XmlElement;

import mil.dod.th.core.log.Logging;

import org.osgi.service.log.LogService;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 * The Display widget for all Capabilities.
 * @author jgold
 *
 */
@ManagedBean(name = "capsTree")
@RequestScoped
public class CapabilitiesTree 
{
    /**
     * Used to identify an object as being part of Terra Harvest.
     */
    private static final String TH_OBJ_IDENTIFIER = "mil.dod.th.core";
    
    
    /**
     * The capabilities object.
     */
    protected Object m_Caps;
    
    /**
     * Criteria used to determine if the capabilities object exists and is valid.
     * @return true if the object is set and ready for use.
     */
    protected boolean hasCapabilities()
    {
        return true;
    }
    
    /**
     * Build the Capabilities Tree based on the object passed in.
     * @param capsObj a Capabilities object. If null, a single node with an error message is created.
     * @return the root of the tree
     */
    protected TreeNode buildCapabilitiesTree(final Object capsObj)
    {
        m_Caps = capsObj;

        final DefaultTreeNode root = new DefaultTreeNode("root", null);
        
        if (capsObj == null)
        {
            final CapabilityModel capModel = new CapabilityModel();
            capModel.setName("No Capabilities Document Found.");
            capModel.setValue("");
            new DefaultTreeNode(capModel, root);
            return root;
        }
        
        m_Caps = capsObj;
        
        final Class<?> clazz = m_Caps.getClass();

        addChildren(clazz, root, m_Caps);    
        
        return root;
    }
    
    /**
     * Recursively add children to tree node by going through Capabilities class.
     * If a value is deemed of a Terra Harvest type (using class package name)
     * then the attributes of that class are investigated until only non-JAXB attributes are found. 
     * Likewise, if the attribute
     * is a list, then each item is investigated.
     * Nodes are automatically expanded.
     * @param clazz the class
     * @param parent the parent tree node to add new objects
     * @param obj the object to be analyzed
     */
    private void addChildren(final Class<?> clazz, final TreeNode parent, final Object obj)
    {        
        //Get any parent capabilities
        if (clazz.getSuperclass() != null)
        {
            addChildren(clazz.getSuperclass(), parent, obj);
        }
        
        for (Field f : clazz.getDeclaredFields())
        {
            f.setAccessible(true);
            
            //Test to see if this field has JAXB Annotations
            boolean hasAnnos = false;
            for (Annotation anno : f.getAnnotations())
            {
                if (anno.annotationType().getName().startsWith(XmlElement.class.getPackage().getName()))
                {
                    hasAnnos = true;
                    break;
                }
            }
            
            if (hasAnnos)
            {
                try 
                {
                    if (f.get(obj) != null)
                    {
                        processCapabilityElement(f, obj, parent);
                    }
                } 
                catch (final IllegalAccessException e) 
                {
                    Logging.log(LogService.LOG_ERROR, e, 
                            "Error Parsing Capabilities for Type %s", parent.getType());
                }        
            }            
        }
    }    
    
    /**
     * Process the XML element/attribute that is part of the capabilities document.
     * @param field the Field, via reflection.
     * @param obj the obj used to retrieve the values.
     * @param parent the parent tree node.
     * @throws IllegalAccessException thrown if there are issues getting the field value.
     */
    private void processCapabilityElement(final Field field, final Object obj, final TreeNode parent) 
            throws IllegalAccessException
    {
        final Object fieldObj = field.get(obj);
        final CapabilityModel capModel = createCapabilityModel(field, fieldObj);
        if (capModel == null)
        {
            return;
        }
        
        final TreeNode node = new DefaultTreeNode(capModel, parent);
        
        final boolean isThType = field.getGenericType().toString().contains(TH_OBJ_IDENTIFIER);
        if (isThType)
        {
            handleThAttr(field, fieldObj, node);
        }
        
        //If there are no children, remove the node.
        if (node.getChildCount() > 0)
        {
            node.setExpanded(true);
        }
        else if (isThType)
        {
            parent.getChildren().remove(node);
        }
    }
    
    /**
     * Create a model object for the tree table.
     * @param field The field to use
     * @param fieldObj the instance of that field
     * @return a new CapabilityModel, or null if no value is set.
     */
    private CapabilityModel createCapabilityModel(final Field field, final Object fieldObj)
    {
        final boolean isList = fieldObj instanceof List;
        final CapabilityModel capModel = new CapabilityModel();
        final StringBuilder name = new StringBuilder(field.getName());
        if (isList)
        {
            name.append(" (List)");
        }
        capModel.setName(name.toString());
        
        final boolean isThType = field.getGenericType().toString().contains(TH_OBJ_IDENTIFIER);

        if (fieldObj == null)
        {
            return null;
        }
        else if (isThType)
        {
            //If this is an enum, then go ahead and set the value
            if (fieldObj.getClass().isEnum())
            {
                capModel.setValue(fieldObj.toString());
            }
            else
            {
                capModel.setValue("");
            }
        }
        else
        {
            if (isList)
            {
                final StringBuilder val = new StringBuilder();
                for (Object obj : (List<?>)fieldObj)
                {
                    val.append(obj.toString()).append("\n");
                }
                capModel.setValue(val.toString());
            }
            else
            {
                final String str = fieldObj.toString();
                capModel.setValue(str);
            }
        }
        
        return capModel;
    }
    
    /**
     * Create the node for the Th object.
     * @param field the field.
     * @param fieldObj the value of the field for a object.
     * @param node the parent tree node.
     */
    private void handleThAttr(final Field field, final Object fieldObj, final TreeNode node)
    {
        //If its a list, then iterate over that list, showing each object.
        //This won't cover other types that are list fields.  Leveraging the fact that
        //their toString() is sufficient.
        if (fieldObj instanceof List<?>)
        {
            final List<?> listFieldObj = (List<?>)fieldObj;
            final Iterator<?> iter = listFieldObj.iterator();
            final ParameterizedType type = (ParameterizedType) field.getGenericType();
            final Class<?> clazz = (Class<?>)type.getActualTypeArguments()[0];
            while (iter.hasNext())
            {
                final Object listObj = iter.next();
                final CapabilityModel cm2 = new CapabilityModel();
                
                cm2.setName("Item: " + clazz.getSimpleName());
                
                if (listObj.getClass().isEnum())
                {
                    cm2.setValue(listObj.toString());
                    new DefaultTreeNode(cm2, node);
                }
                else
                {
                    cm2.setValue("");
                    final TreeNode node2 = new DefaultTreeNode(cm2, node);
                    
                    addChildren(listObj.getClass(), node2, listObj);
                                    
                    if (node2.getChildCount() > 0)
                    {
                        node2.setExpanded(true);
                    }
                }
            }
        }
        else if (!fieldObj.getClass().isEnum())
        {
            addChildren(fieldObj.getClass(), node, fieldObj);
        }
    }
    
    /**
     * Rendered check for loading the tree.
     * @return True if there is a Capabilities object set.
     */
    public boolean isCapsLoaded()
    {
        return m_Caps != null;
    }

    /**
     * Get the root node. Build out the tree based on the model object every time.
     * @param obj the Capabilities object.
     * @return root node
     */
    public synchronized TreeNode getRoot(final Object obj) 
    {
        return buildCapabilitiesTree(obj);
    }
}
