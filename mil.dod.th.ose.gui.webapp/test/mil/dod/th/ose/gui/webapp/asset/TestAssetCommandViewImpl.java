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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import javax.faces.application.FacesMessage;

import org.junit.Before;
import org.junit.Test;
import org.primefaces.model.TreeNode;

import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.gui.webapp.utils.ReflectionsUtilException;

/**
 * Test class for the {@link AssetCommandMgrImpl} class.
 * 
 * @author cweisenborn
 */
public class TestAssetCommandViewImpl
{
    private AssetCommandViewImpl m_SUT;
    private GrowlMessageUtil m_GrowlUtil;
    
    @Before
    public void setup()
    {
        //Mocked classes.
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        
        //Instantiate class to be tested.
        m_SUT = new AssetCommandViewImpl();
        
        //Set dependencies.
        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
    }
    
    /**
     * Test the get tree method.
     * Verify that the tree has been built correctly. 
     */
    @Test
    public void testGetTree() throws ReflectionsUtilException
    {
        ExampleClass testCommandObject = new ExampleClass();

        UUID assetUuid = UUID.randomUUID();
        TreeNode root = m_SUT.getTree(assetUuid, testCommandObject);
        
        //Verify tree contains appropriate nodes.
        assertThat(containsNode(root, "boolField", "", 1), is(true));
        assertThat(containsNode(root, "boolWrapperField", testCommandObject.isBoolWrapperField(), 1), is(true));
        assertThat(containsNode(root, "byteField", "", 1), is(true));
        assertThat(containsNode(root, "byteWrapperField", testCommandObject.getByteWrapperField(), 1), is(true));
        assertThat(containsNode(root, "charField", "", 1), is(true));
        assertThat(containsNode(root, "charWrapperField", testCommandObject.getCharWrapperField(), 1), is(true));
        assertThat(containsNode(root, "complexField", testCommandObject.getComplexField(), 1), is(true));
        assertThat(containsNode(root, "complexListField", testCommandObject.getComplexListField(), 1), is(true));
        assertThat(containsNode(root, "doubleField", "", 1), is(true));
        assertThat(containsNode(root, "doubleWrapperField", testCommandObject.getDoubleWrapperField(), 1), is(true));
        assertThat(containsNode(root, "enumField", testCommandObject.getEnumField(), 1), is(true));
        assertThat(containsNode(root, "floatField", "", 1), is(true));
        assertThat(containsNode(root, "floatWrapperField", testCommandObject.getFloatWrapperField(), 1), is(true));
        assertThat(containsNode(root, "intField", "", 1), is(true));
        assertThat(containsNode(root, "intWrapperField", testCommandObject.getIntWrapperField(), 1), is(true));
        assertThat(containsNode(root, "intWrapperListField", testCommandObject.getIntWrapperListField(), 1), is(true));
        assertThat(containsNode(root, "longField", "", 1), is(true));
        assertThat(containsNode(root, "longWrapperField", testCommandObject.getLongWrapperField(), 1), is(true));
        assertThat(containsNode(root, "nullComplexField", testCommandObject.getNullComplexField(), 1), is(true));
        assertThat(containsNode(root, "shortField", "", 1), is(true));
        assertThat(containsNode(root, "shortWrapperField", testCommandObject.getShortWrapperField(), 1), is(true));
        assertThat(containsNode(root, "stringField", testCommandObject.getStringField(), 1), is(true));
        
        ExampleComplexClass listComplexClass = testCommandObject.getComplexListField().get(0);
        assertThat(containsNode(root, "ExampleComplexClass", listComplexClass, 2), is(true));
        assertThat(containsNode(root, "intWrapperField", listComplexClass.getIntWrapperField(), 3), is(true));
        assertThat(containsNode(root, "doubleWrapperField", listComplexClass.getDoubleWrapperField(), 3), is(true));
        
        ExampleComplexClass complexClass = testCommandObject.getComplexField();
        assertThat(containsNode(root, "intWrapperField", complexClass.getIntWrapperField(), 2), is(true));
        assertThat(containsNode(root, "doubleWrapperField", complexClass.getDoubleWrapperField(), 2), is(true));
        
        List<Integer> intList = testCommandObject.getIntWrapperListField();
        assertThat(containsNode(root, "Integer", intList.get(0), 2), is(true));
        assertThat(containsNode(root, "Integer", intList.get(1), 2), is(true));
        assertThat(containsNode(root, "Integer", intList.get(2), 2), is(true));
        
        //Verify that the serialVersionUID field did not get put in the tree.
        assertThat(containsNode(root, "serialVersionUID", testCommandObject.getSerialVersionUID(), 1), is(false));
        assertThat(containsNode(root, "stringTransientField", testCommandObject.getStringTransientField(), 1), 
                is(false));
    }
    
    /**
     * Test the add field method.
     * Verify that the specified field was actually added to the object.
     */
    @Test
    public void testAddField() throws SecurityException
    {
        ExampleClass testCommandObject = new ExampleClass();
        
        //Create fake node for a primitive list field.
        CommandNodeModel listNode = new CommandNodeModel("intWrapperListField", List.class, false, testCommandObject, 
                m_GrowlUtil);
        
        //Verify initial size of the integer list field.
        assertThat(testCommandObject.getIntWrapperListField().size(), is(3));
        //Add another integer to the list.
        m_SUT.addField(listNode);
        //Verify size of integer list has increased by one.
        assertThat(testCommandObject.getIntWrapperListField().size(), is(4));
        
        //Create fake node for a complex list field.
        listNode = new CommandNodeModel("complexListField", List.class, false, testCommandObject, 
                m_GrowlUtil);
        
        //Verify initial size of complex list field.
        assertThat(testCommandObject.getComplexListField().size(), is(1));
        //Add another complex object to the list.
        m_SUT.addField(listNode);
        //Verify size of complex list has increased by one.
        assertThat(testCommandObject.getComplexListField().size(), is(2));
        
        testCommandObject.setComplexField(null);
        
        //Create fake node for a complex field.
        CommandNodeModel complexNode = new CommandNodeModel("complexField", ExampleComplexClass.class, true, 
                testCommandObject, m_GrowlUtil);
        
        //Verify that the complex field is initially null.
        assertThat(testCommandObject.getComplexField(), is(nullValue()));
        //Add object to complex field.
        m_SUT.addField(complexNode);
        //Verify that the complex field is no longer null.
        assertThat(testCommandObject.getComplexField(), is(notNullValue()));
        
        //Test adding to a list field that does not exists.
        listNode = new CommandNodeModel("DNE_ListField", List.class, false, testCommandObject, 
                m_GrowlUtil);
        
        //Add item to list that does not exist.
        m_SUT.addField(listNode);
        
        //Verify that a growl message is displayed since the list field does not exist.
        verify(m_GrowlUtil).createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, "Error Adding Object To List:", 
                "An exception has occurred while trying to add an object to the list with the field" 
                + "name: " + listNode.getName() + ". See the server logs for more detail.", null, true);
        
        //Test adding a complex object that does not exist.
        complexNode = new CommandNodeModel("DNE_ComplexField", ExampleComplexClass.class, true, 
                testCommandObject, m_GrowlUtil);
        
        //Add complex object that does not exist.
        m_SUT.addField(complexNode);
        
        //Verify that a growl message is displayed since the complex field does not exist.
        verify(m_GrowlUtil).createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, "Error Adding Field:", 
                "An exception has occurred while trying to add an object of type: " + complexNode.getName() + ". " +
                        "See the server log for further details.", null, true);
    }
    
    /**
     * Test the remove field method.
     * Verify that the specified list item is removed or the specified field is set to null.
     */
    @Test
    public void testRemoveField()
    {
        ExampleClass testCommandObject = new ExampleClass();
        
        //Create fake node for a value in primitive list field.
        CommandNodeModel listNode = new CommandNodeModel("int", Integer.class, false, 
                testCommandObject.getIntWrapperListField(), m_GrowlUtil);
        listNode.setIndex(1);
        
        Object secondValue = testCommandObject.getIntWrapperListField().get(1);
        //Verify the size of the list before removal of the second value.
        assertThat(testCommandObject.getIntWrapperListField().size(), is(3));
        assertThat(testCommandObject.getIntWrapperListField().contains(secondValue), is(true));
        //Remove the second list value.
        m_SUT.removeField(listNode);
        //Verify the size of the list have decreased by one and that it no longer contains the second value.
        assertThat(testCommandObject.getIntWrapperListField().size(), is(2));
        assertThat(testCommandObject.getIntWrapperListField().contains(secondValue), is(false));
        
        //Create fake node for a complex field.
        CommandNodeModel complexNode = new CommandNodeModel("complexField", ExampleComplexClass.class, true, 
                testCommandObject, m_GrowlUtil);
        
        //Verify that the complex field is initially null.
        assertThat(testCommandObject.getComplexField(), is(notNullValue()));
        //Add object to complex field.
        m_SUT.removeField(complexNode);
        //Verify that the complex field is no longer null.
        assertThat(testCommandObject.getComplexField(), is(nullValue()));
    }
    
    /**
     * Method used to determine if a tree contains a specific node.
     * 
     * @param root
     *          The tree node with which to start searching at.
     * @param nodeName
     *          Name of the node to be found.
     * @param nodeValue
     *          Value the node should contain.
     * @param nodeLevel
     *          Level the node will be at from the root node. (1 or greater)
     * @return
     *          True if the tree contains the specified node and false otherwise.
     */
    private boolean containsNode(TreeNode root, String nodeName, Object nodeValue, int nodeLevel) 
        throws ReflectionsUtilException
    {
        int level = 1;
        return containsNode(root, nodeName, nodeValue, level, nodeLevel);
    }
    
    /**
     * Method used to determine if a tree contains a specific node.
     * 
     * @param node
     *          The tree node with which to start searching at.
     * @param nodeName
     *          Name of the node to be found.
     * @param nodeValue
     *          Value the node should contain.
     * @param currentLevel
     *          Current node level within the tree.
     * @param nodeLevel
     *          Level the node will be at from the root node. (1 or greater)
     * @return
     *          True if the tree contains the specified node and false otherwise. 
     */
    private boolean containsNode(TreeNode node, String nodeName, Object nodeValue, int currentLevel, int nodeLevel) 
        throws ReflectionsUtilException
    {
        for (TreeNode child: node.getChildren())
        {
            if (currentLevel == nodeLevel)
            {
                String name = ((CommandNodeModel)child.getData()).getName();
                Object value = ((CommandNodeModel)child.getData()).getValue();
                //Null check needed since equals method is used.
                if (name.equals(nodeName) && value != null && value.equals(nodeValue))
                {
                    return true;
                }
                //This if will allow for checking if a value does equal null since the equals method is used above.
                else if (name.equals(nodeName) && value == nodeValue)
                {
                    return true;
                }
            }
            
            //Check current level first. If it is the same level as where the node should be found then there is no 
            //reason to search any deeper.
            if (currentLevel != nodeLevel && child.getChildCount() > 0)
            {
                boolean found = containsNode(child, nodeName, nodeValue, currentLevel + 1, nodeLevel);
                if (found)
                {
                    return true;
                }
            }
        }
        return false;
    }
}
