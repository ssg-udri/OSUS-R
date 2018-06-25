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

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import mil.dod.th.core.asset.AssetScanner;
import mil.dod.th.core.ccomm.AddressTranslator;

/**
 * Class will make sure all AssetScanner implementation classes have ProductType annotations.
 * 
 * @author tkuck
 */
public class ProductTypeAnnotationCheck extends AbstractCheck
{
    @Override
    public int[] getDefaultTokens()
    {
        return new int[]{TokenTypes.CLASS_DEF};
    }

    @Override
    public void visitToken(final DetailAST ast)
    {
        try
        {
            // get implements clause block from the class definition
            final DetailAST implementsClauseToken = ast.findFirstToken(TokenTypes.IMPLEMENTS_CLAUSE);
            if (implementsClauseToken != null)
            {
                // get the identifier of the implements clause block
                final DetailAST identToken = implementsClauseToken.findFirstToken(TokenTypes.IDENT);
                if (identToken == null)
                {
                    return;
                }
                final String implementationType = identToken.getText();
                if (implementationType.contains(AssetScanner.class.getSimpleName())
                        || implementationType.contains(AddressTranslator.class.getSimpleName()))
                {
                    checkForAnnotation(ast, identToken, implementationType);
                }
            }
        }
        catch (final Exception e)
        {
            e.printStackTrace(); //NOPMD no other option from within a checkstyle rule
            throw e;
        }
    }
    
    /**
     * Check all modifiers to find out if the correct annotation type is present.
     * @param ast
     *      the class definition
     * @param identToken
     *      the identifier of the implements clause block
     * @param implementationType
     *      the string value of the identToken
     */
    private void checkForAnnotation(final DetailAST ast, final DetailAST identToken, final String implementationType)
    {
        // get modifiers block from class definition
        final DetailAST modifiers = ast.findFirstToken(TokenTypes.MODIFIERS);
        // get annotations block from modifiers block
        DetailAST modifierChild = modifiers.findFirstToken(TokenTypes.ANNOTATION);
        
        while (modifierChild != null)
        {
            // second time through sibling might not be annotation type
            if (modifierChild.getType() == TokenTypes.ANNOTATION
                    && modifierChild.findFirstToken(TokenTypes.IDENT).getText().contains("ProductType"))
            {
                return; // annotation found, no need to continue checking
            }
            modifierChild = modifierChild.getNextSibling();
        }
        log(identToken.getLineNo(), 
                implementationType + " implementation class without @ProductType annotation");
    }

    @Override
    public int[] getAcceptableTokens()
    {
        return getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens()
    {
        return getDefaultTokens();
    }
}
