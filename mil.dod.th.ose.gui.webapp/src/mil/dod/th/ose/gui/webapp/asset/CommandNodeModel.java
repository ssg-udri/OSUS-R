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

import java.lang.reflect.Method;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil;
import mil.dod.th.ose.gui.webapp.utils.ArgumentValidatorUtil.ValidationEnum;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.gui.webapp.utils.ReflectionsUtil;
import mil.dod.th.ose.gui.webapp.utils.ReflectionsUtilException;

/**
 * class that is used to store information that is needed for displaying and setting values
 * in a tree table.
 * 
 * @author cweisenborn
 */
public class CommandNodeModel
{
    /**
     * Reference to the name of the field within the command object.
     */
    private String m_Name;
    
    /**
     * Class type of the field within the command object.
     */
    private  Class<?> m_Type;
    
    /**
     * The object that this field is contained within. This could be the main command object or an inner reference
     * object. This object is needed to be able to get or set the value.
     */
    private final Object m_ContainingObject;
    
    /**
     * Boolean value that is set when the object is created. Used to determine if this object represents a reference
     * object.
     */
    private final boolean m_IsReference;
    
    /**
     * Integer values that represents the position of the item within the containing list object.
     */
    private Integer m_Index;
    
    /**
     * Reference the growl messages utility.
     */
    private final GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Constructor method used to create a command node model object.
     * 
     * @param name
     *          Name of the object the node represents.
     * @param type
     *          Class type of the object the node represents.
     * @param isReference
     *          Boolean that represents whether the node pertains to a reference object.
     * @param object
     *          The actual object the node represents.
     * @param growlUtil
     *          Reference to the growl utility. Allows for post growl messages.
     */
    public CommandNodeModel(final String name, final Class<?> type, final boolean isReference, final Object object, 
            final GrowlMessageUtil growlUtil)
    {
        m_Name = name;
        m_Type = type;
        m_IsReference = isReference;
        m_ContainingObject = object;
        m_GrowlUtil = growlUtil;
    }
    
    /**
     * Method used to retrieve the name of the object the node represents.
     * 
     * @return
     *          String that represents the name of the object the node represents.
     */
    public String getName()
    {
        return m_Name;
    }
    
    /**
     * Method that returns the class type of the object the node represents.
     * 
     * @return
     *          Class type of the object the node represents.
     */
    public Class<?> getType()
    {
        return m_Type;
    }
    
    /**
     * Method used to determine if the object is a reference field.
     * 
     * @return True if this object represents a reference field within the command object. Otherwise false
     * is returned meaning this object represents a primitive field.
     */
    public Boolean isReference()
    {
        return m_IsReference;
    }
    
    /**
     * Sets the field that stores the name of the object the node represents.
     * 
     * @param name 
     *          The name to be set.
     */
    public void setName(final String name)
    {
        this.m_Name = name;
    }
    
    /**
     * Sets the field that stores the class type of object the node represents.
     * 
     * @param type 
     *          The class type to be set.
     */
    public void setType(final Class<?> type)
    {
        this.m_Type = type;
    }
    
    /**
     * Method that returns the containing object.
     * 
     * @return
     *          Object that represents the containing object.
     */
    public Object getContainingObject()
    {
        return m_ContainingObject;
    }
    
    /**
     * Method that retrieves the index position of the object within the list that contains this object. Returns 
     * null if this item is not contained within a list.
     * 
     * @return
     *          Integer that represents the index position of this item within the containing list.
     */
    public Integer getIndex()
    {
        return m_Index;
    }
    
    /**
     * Method that sets the index position of the object within the list that this object is contained.
     * 
     * @param index
     *          Integer value that represents the index position of this item.
     */
    public void setIndex(final Integer index)
    {
        m_Index = index;
    }
    
    /**
     * Method used to set the value within the command object.
     * 
     * @param value
     *          Value to be set for the field within the command object.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setValue(final Object value)
    {
        final String errorTitle = "Could not set value for command:";
        final String errorMsg = "Error! Could not set value: %s for field: %s! See server log for further details.";
        
        Object valueToBeSet = null;
        if (value != null && !value.equals(""))
        {
            if (m_Type.isEnum())
            {
                valueToBeSet = Enum.valueOf((Class<Enum>)m_Type, (String)value);
            }
            else
            {
                valueToBeSet = ArgumentValidatorUtil.convertToActualType((String)value, m_Type);
            }
        }
        
        if (m_ContainingObject instanceof List) 
        {                                
            ((List<Object>)m_ContainingObject).set(m_Index, valueToBeSet);
        }
        else
        { 
            final Method setMethod = ReflectionsUtil.retrieveSetMethod(m_ContainingObject, m_Name);
            if (setMethod.getParameterTypes().length != 1)
            {
                m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, errorTitle, errorMsg);
                return;
            }
            final Class<?> paramType = setMethod.getParameterTypes()[0];
            if (paramType.isPrimitive() && !m_Type.isPrimitive() && valueToBeSet == null)
            {
                try
                {
                    ReflectionsUtil.unsetField(m_ContainingObject, m_Name);
                }
                catch (final ReflectionsUtilException ex)
                {
                    m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, errorTitle, errorMsg, ex);
                }
            }
            else
            {
                try
                {
                    ReflectionsUtil.setInnerObject(m_ContainingObject, m_Name, valueToBeSet);
                }
                catch (final ReflectionsUtilException exception)
                {
                    m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, errorTitle, errorMsg, exception);
                }
            }
        }
    }
    
    /**
     * Method that retrieves the value of the field from within the command object.
     * 
     * @return
     *          Value of the field within the field that this object represents within the command object.
     * @throws ReflectionsUtilException 
     *          Thrown if an error occurs while using reflections.
     */
    @SuppressWarnings("unchecked")
    public Object getValue() throws ReflectionsUtilException
    {        
        if (m_ContainingObject instanceof List)
        {
            return ((List<Object>)m_ContainingObject).get(m_Index);
        }
        
        final Method getMethod = ReflectionsUtil.retrieveGetMethod(m_ContainingObject, m_Name, m_Type);
        if (getMethod.getReturnType().isPrimitive() && !m_Type.isPrimitive())
        {
            final boolean isFieldSet = ReflectionsUtil.isFieldSet(m_ContainingObject, m_Name);
            if (!isFieldSet)
            {
                return "";
            }
        }

        return ReflectionsUtil.retrieveInnerObject(m_ContainingObject, m_Name, m_Type);
    }
    
    /**
     * Method used to determine if the object this node stores can be added to the containing object or if an object
     * can be added to the list.
     * 
     * @return
     *          Boolean that represents whether the object this node stores and be added to the containing object or if
     *          an object can be added to the list stored in this node. True if an object can be added and false 
     *          otherwise.
     * @throws ReflectionsUtilException
     *          Thrown if an error occurs while using reflections to retrieve the nodes value.
     */
    public boolean isAddSupported() throws ReflectionsUtilException
    {
        if (m_Type.equals(List.class) && m_IsReference)
        {
            return true;
        }
        else if (m_IsReference && getValue() == null)
        {
            return true;
        }
        return false;
    }
    
    /**
     * Method used to determine if the deleting the object this node stores is possible.
     * 
     * @return
     *          Boolean that represents whether the object this node stores can be deleted or not. True if the object
     *          can be deleted and false otherwise.
     * @throws ReflectionsUtilException
     *          Thrown if an error occurs while using reflections to retrieve the nodes value.
     */
    public boolean isDeleteSupported() throws ReflectionsUtilException
    {
        if (m_ContainingObject instanceof List)
        {
            return true;
        }
        else if (!m_Type.equals(List.class) && m_IsReference && getValue() != null)
        {
            return true;
        }
        return false;
    }
    
    /**
     * Method used to validate the value being set for the command field. This validation only happens for primitive
     * types and their wrapper classes.
     * 
     * @param context
     *          The current faces context, method interface is defined by JSF so parameter is required even if not used
     * @param component
     *          The JSF component calling the validate method, , method interface is defined by JSF so parameter is 
     *          required even if not used
     * @param value
     *          The value being validated.
     * @throws ValidatorException
     *          Thrown value passed is not valid.
     */
    public void validateValue(final FacesContext context, final UIComponent component, final Object value) 
            throws ValidatorException
    {
        if (!m_Type.isPrimitive() && (value == null || value.equals("")))
        {
            return;
        }
        
        final ValidationEnum result = 
                ArgumentValidatorUtil.isValidValue((String)value, null, null, m_Type);
        if (result != ValidationEnum.PASSED)
        {
            final FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, result.toString(), null);
            throw new ValidatorException(msg);
        }
    }
}
