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
package mil.dod.th.ose.checkstyle;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Class will make sure JUnit methods that start with the word test have the proper annotations.
 * 
 * @author dhumeniuk
 *
 */
public class JUnitTestMethodCheck extends Check
{
    @Override
    public int[] getDefaultTokens()
    {
        return new int[]{TokenTypes.METHOD_DEF};
    }

    @Override
    public void visitToken(final DetailAST ast)
    {
        try
        {
            final String methodName = ast.findFirstToken(TokenTypes.IDENT).getText();
            
            if (!methodName.startsWith("test"))
            {
                return; // method doesn't start with "test" okay
            }
            
            final DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
            DetailAST modifierChild = modifiers.findFirstToken(TokenTypes.ANNOTATION);
            boolean foundTestAnnotation = false;
            while (modifierChild != null)
            {
                if (modifierChild.getType() == TokenTypes.ANNOTATION)
                {
                    // found annotation
                    final DetailAST identToken = modifierChild.findFirstToken(TokenTypes.IDENT);
                    final String annotationName = identToken.getText();
                    if ("Test".equals(annotationName) || "Ignore".equals(annotationName))
                    {
                        foundTestAnnotation = true;
                        break;
                    }
                }
                else
                {
                    System.err.format("Found non-annotation modifier (%s) for %s%n", modifierChild, methodName);
                }
                modifierChild = modifierChild.getNextSibling();
            }
            
            if (!foundTestAnnotation)
            {
                log(ast.getLineNo(), "test method without @Test annotation");
            }
        }
        catch (final Exception e)
        {
            e.printStackTrace(); //NOPMD no other option from within a checkstyle rule
            throw e;
        }
    }
}
