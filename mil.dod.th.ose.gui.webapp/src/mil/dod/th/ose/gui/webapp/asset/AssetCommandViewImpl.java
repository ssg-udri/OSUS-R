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
package mil.dod.th.ose.gui.webapp.asset;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.UUID;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.gui.webapp.utils.ReflectionsUtil;
import mil.dod.th.ose.gui.webapp.utils.ReflectionsUtilException;

import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;

/**
 * The viewscoped bean that is used to create a tree for displaying and setting values within for an asset
 * command.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "commandBuilder")
@ViewScoped
public class AssetCommandViewImpl implements AssetCommandView
{
    /**
     * String used to represent the growl title for the reflection error thrown when a field is being retrieved.
     */
    private final static String REFLECTIONS_RETRIEVE_ERROR_TITLE = "Error Getting Inner Command Field:";
    
    /**
     * String used to represent the growl message for the reflection error thrown when a field is being retrieved.
     */
    private final static String REFLECTIONS_RETRIEVE_ERROR_MSG = "An exception occured while trying to get a field " 
            + "within a command. The name of the field to be retrieved was: %s. See server log for further details.";
    
    /**
     * String used to represent the growl title for the reflection error thrown when a field is being set.
     */
    private final static String REFLECTIONS_SET_ERROR_TITLE = "Error Setting Inner Command Field:";
    
    /**
     * String used to represent the growl message for the reflection error thrown when a field is being set.
     */
    private final static String REFLECTIONS_SET_ERROR_MSG = "An exception occured while trying to set a field within " 
            + "a command. The name of the field to be set was: %s. See server log for further details.";
    
    /**
     * Reference to the growl message utility service.
     */
    @Inject
    private static GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Method that sets the growl message utility to be used.
     * 
     * @param growlUtil
     *          The growl message utility to be used.
     */
    public void setGrowlMessageUtil(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    @Override
    public TreeNode getTree(final UUID uuid, final Object command)
    {
        final CommandNodeModel root = 
                new CommandNodeModel(uuid.toString(), command.getClass(), true, null, m_GrowlUtil);
        final TreeNode rootTree = new DefaultTreeNode(root, null);
        buildTree(command, rootTree);
  
        return rootTree;
    }
    
    @Override
    public void addField(final CommandNodeModel nodeObject)
    {
        if (nodeObject.getType().equals(List.class))
        {
            addToList(nodeObject);
        }
        else
        {
            addComplexObject(nodeObject);
        }
    }
    
    @Override
    public void removeField(final CommandNodeModel nodeObject)
    {
        if (nodeObject.getContainingObject() instanceof List)
        {
            removeFromList(nodeObject);
        }
        else
        {
            removeComplextObject(nodeObject);
        }
    }

    /**
     * Method that adds an object to the specified list.
     * 
     * @param nodeObject
     *              {@link CommandNodeModel} represents the list object that a value will be added to.
     */
    @SuppressWarnings("unchecked")
    private void addToList(final CommandNodeModel nodeObject)
    {
        final String exceptionTitle = "Error Adding Object To List:";
        final String exceptionMsg = "An exception has occurred while trying to add an object to the list with the field"
                + "name: %s. See the server logs for more detail.";
        
        final Field listField = ReflectionsUtil.retrieveField(nodeObject.getName(), nodeObject.getContainingObject());
        if (listField == null)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, exceptionTitle, String.format(exceptionMsg,
                    nodeObject.getName()), null, true);
            return;
        }

        final ParameterizedType listType = (ParameterizedType)listField.getGenericType();
        final Class<?> listClass = (Class<?>)listType.getActualTypeArguments()[0];
        final List<Object> list;
        try
        {
            list = (List<Object>)nodeObject.getValue();
        }
        catch (final ReflectionsUtilException exception)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, exceptionTitle, 
                    String.format(exceptionMsg, nodeObject.getName()), exception, true);
            return;
        }
        
        if (ReflectionsUtil.isPrimitiveOrWrapper(listClass))
        {
            list.add(returnDefaultPrimitive(listClass));
        }
        else
        {
            Object listObject = null;
            try
            {
                listObject = listClass.newInstance();
            }
            catch (final InstantiationException | IllegalAccessException exception)
            {
                m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, exceptionTitle, 
                        String.format(exceptionMsg, nodeObject.getName()), exception, true);
            }
            list.add(listObject);
        }
    }
    
    /**
     * Method that creates and sets a reference object within the command object.
     * 
     * @param nodeObject
     *          {@link CommandNodeModel} that represents the reference object to be instantiated.
     */
    private void addComplexObject(final CommandNodeModel nodeObject)
    {
        final String exceptionTitle = "Error Adding Field:";
        final String exceptionMsg = "An exception has occurred while trying to add an object of type: %s. See the " 
                + "server log for further details.";
        
        final Object containingObject = nodeObject.getContainingObject();
        final Field field = ReflectionsUtil.retrieveField(nodeObject.getName(), containingObject);
        if (field == null)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, exceptionTitle, String.format(exceptionMsg,
                    nodeObject.getName()), null, true);
            return;
        }

        
        final Object innerObject;
        try
        {
            innerObject = ReflectionsUtil.retrieveInnerObject(containingObject, field.getName(), 
                    nodeObject.getType());
        }
        catch (final ReflectionsUtilException exception)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, exceptionTitle, 
                    String.format(exceptionMsg, nodeObject.getType()), exception, true);
            return;
        }
        try
        {
            if (innerObject == null)
            {
                ReflectionsUtil.setInnerObject(containingObject, field.getName(), field.getType().newInstance());
            }
        }
        catch (final IllegalAccessException | InstantiationException | ReflectionsUtilException exception)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, exceptionTitle, 
                    String.format(exceptionMsg, nodeObject.getType()), exception, true);
        }
    }
    
    /**
     * Method that sets a reference object within the command to null.
     * 
     * @param nodeObject
     *          {@link CommandNodeModel} that represents the reference object to be removed.
     */
    private void removeComplextObject(final CommandNodeModel nodeObject)
    {
        final Object containingObject = nodeObject.getContainingObject();
        final Object innerObject;
        try
        {
            innerObject = ReflectionsUtil.retrieveInnerObject(containingObject, nodeObject.getName(), 
                    nodeObject.getType());
        }
        catch (final ReflectionsUtilException exception)
        {
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, REFLECTIONS_SET_ERROR_TITLE, 
                    String.format(REFLECTIONS_SET_ERROR_MSG, nodeObject.getName()), exception);
            return;
        }

        if (innerObject != null)
        {
            try
            {
                ReflectionsUtil.setInnerObject(containingObject, nodeObject.getName(), null);
            }
            catch (final ReflectionsUtilException exception)
            {
                m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, REFLECTIONS_SET_ERROR_TITLE, 
                        String.format(REFLECTIONS_SET_ERROR_MSG, nodeObject.getName()), exception);
            }
        }
    }
    
    /**
     * Method that removes an object from the specified list.
     * 
     * @param nodeObject
     *          {@link CommandNodeModel} represents the object that a value will be removed from the list.
     */
    private void removeFromList(final CommandNodeModel nodeObject)
    {
        @SuppressWarnings("unchecked")
        final List<Object> list = (List<Object>)nodeObject.getContainingObject();
        final int index = nodeObject.getIndex();
        list.remove(index);
    }
    
    /**
     * Method used to build the tree that will display all the command values on the asset command and control
     * tab.
     * 
     * @param object
     *          Object that contains the fields that the tree is to be built for.
     * @param treeNode
     *          Parent node that the fields will be added to as child nodes.
     */
    private void buildTree(final Object object, final TreeNode treeNode)
    {
        for (final Field field: object.getClass().getDeclaredFields())
        {
            try
            {
                handleField(field, object, treeNode);
            }
            catch (final ReflectionsUtilException exception)
            {
                m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, 
                        REFLECTIONS_RETRIEVE_ERROR_TITLE, String.format(REFLECTIONS_RETRIEVE_ERROR_MSG, 
                                field.getName()), exception);
                return;
            }
        }
        //Check to see if the object has a super class and then find all fields declared by the superclass.
        Class<?> superClass = object.getClass().getSuperclass();
        while (superClass != null && !superClass.equals(Object.class))
        {
            for (final Field field: superClass.getDeclaredFields())
            {
                try
                {
                    handleField(field, object, treeNode);
                }
                catch (final ReflectionsUtilException exception)
                {
                    m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, 
                            REFLECTIONS_RETRIEVE_ERROR_TITLE, String.format(REFLECTIONS_RETRIEVE_ERROR_MSG, 
                                    field.getName()), exception);
                    return;
                }
            }
            superClass = superClass.getSuperclass();
        }
    }
    
    /**
     * Method that determines field type and then calls the appropriate method to handle the field.
     * 
     * @param field
     *      Field to determine how to be handled.
     * @param object
     *      Object that contains the field.
     * @param treeNode
     *      The {@link TreeNode} that will be the parent node.
     * @throws ReflectionsUtilException
     *      Thrown if an error occurs while using reflections.
     */
    private void handleField(final Field field, final Object object, final TreeNode treeNode) 
            throws ReflectionsUtilException
    {
        if (ReflectionsUtil.hasJaxbAnnotation(field))
        {                    
            if (ReflectionsUtil.isPrimitiveOrWrapper(field.getType()) || field.getType().equals(Object.class))
            {
                handleStandardType(field, object, treeNode);
            }
            else if (field.getType().equals(List.class))
            {
                handleList(field, object, treeNode);
            }
            else
            {
                handleReferenceType(field, object, treeNode);
            }
        }
    }

    /**
     * Method that handles a field within the command object that is a list object.
     *  
     * @param field
     *          The {@link Field} that represents the list object field in the containing object.
     * @param containingObject
     *          The object that the list object is to be retrieved from.
     * @param treeNode
     *          The {@link TreeNode} that will be the parent node to the list object node.
     * @throws ReflectionsUtilException
     *          Thrown if the list cannot be retrieved from the containing object.
     */
    @SuppressWarnings("unchecked")
    private void handleList(final Field field, final Object containingObject, final TreeNode treeNode) 
            throws ReflectionsUtilException
    {
        final ParameterizedType listParam = (ParameterizedType)field.getGenericType();
        final Class<?> listClass = (Class<?>)listParam.getActualTypeArguments()[0];
        final List<Object> list = (List<Object>)ReflectionsUtil.retrieveInnerObject(containingObject, field.getName(), 
                field.getType());
        
        final CommandNodeModel listNodeModel = new CommandNodeModel(field.getName(), field.getType(), true, 
                containingObject, m_GrowlUtil);
        final TreeNode listNode = new DefaultTreeNode(listNodeModel, treeNode);
        listNode.setExpanded(true);
        
        for (int i = 0; i < list.size(); i++)
        {
            final Object listObject = list.get(i);
            final boolean isReference = !ReflectionsUtil.isPrimitiveOrWrapper(listClass);
            final CommandNodeModel listItem = new CommandNodeModel(listClass.getSimpleName(), listClass, isReference, 
                    list, m_GrowlUtil);
            listItem.setIndex(i);
            final TreeNode listObjectNode = new DefaultTreeNode(listItem, listNode);
            listObjectNode.setExpanded(true);
            
            if (isReference)
            {
                buildTree(listObject, listObjectNode);
            }
        }
    }

    /**
     * Method that handles a field within the command object that is a reference object and contains inner fields.
     * 
     * @param field
     *          The {@link Field} that represents the reference object field in the containing object.
     * @param containingObject 
     *          The object that the reference object is to be retrieved from.
     * @param treeNode 
     *          The {@link TreeNode} that will be the parent node to the reference object node.
     * @throws ReflectionsUtilException
     *          Thrown if the reference object cannot be retrieved from the containing object.
     */
    private void handleReferenceType(final Field field, final Object containingObject, 
            final TreeNode treeNode) throws ReflectionsUtilException
    {        
        final CommandNodeModel refType = new CommandNodeModel(field.getName(), field.getType(), true, containingObject, 
                m_GrowlUtil);
        final TreeNode reference = new DefaultTreeNode(refType, treeNode);
        reference.setExpanded(true);
        
        final Object innerObject = ReflectionsUtil.retrieveInnerObject(containingObject, field.getName(), 
                field.getType());
        
        if (innerObject != null)
        {
            buildTree(innerObject, reference);
        }
    }

    /**
     * Method that handles a field within the command object that is a primitive or object type.
     * 
     * @param field
     *          The {@link Field} that represents the primitive or object field in the containing object.
     * @param containingObject
     *          The object that the primitive or object type is to be retrieved from. 
     * @param treeNode
     *          The {@link TreeNode} that will be the parent node to the primitive object node.
     * @throws ReflectionsUtilException
     *          Thrown if an error occurs while using reflections.
     */
    private void handleStandardType(final Field field, final Object containingObject, 
            final TreeNode treeNode) throws ReflectionsUtilException
    {
        final CommandNodeModel primType = new CommandNodeModel(field.getName(), field.getType(), false, 
                containingObject, m_GrowlUtil);
        new DefaultTreeNode(primType, treeNode);
    }
    
    /**
     * Method that returns the default value for the primitive class type passed to this method.
     * 
     * @param type
     *          Primitive class type to return the default value for.
     * @return
     *          Default value for the specified primitive type.
     */
    private Object returnDefaultPrimitive(final Class<?> type)
    {
        final String defualtValue = "0";
        if (type.equals(boolean.class))
        {
            return false;
        }
        else if (type.equals(int.class))
        {
            return Integer.valueOf(defualtValue);
        }
        else if (type.equals(double.class))
        {
            return Double.valueOf(defualtValue);
        }
        else if (type.equals(float.class))
        {
            return Float.valueOf(defualtValue);
        }
        else if (type.equals(long.class))
        {
            return Long.valueOf(defualtValue);
        }
        else if (type.equals(short.class))
        {
            return Short.valueOf(defualtValue);
        }
        else if (type.equals(byte.class))
        {
            return Byte.valueOf(defualtValue);
        }
        else if (type.equals(char.class))
        {
            return Character.valueOf('a');
        }
        else
        {
            return null;
        }
    }
}
