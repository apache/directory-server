/*
 * $Id: ParserSyntaxChecker.java,v 1.4 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.schema ;
import antlr.Parser ;
import antlr.ANTLRLexer ;
import java.io.StringReader ;
import java.lang.reflect.Constructor ;
import org.apache.avalon.framework.logger.AbstractLogEnabled ;
import java.io.Reader ;


public class ParserSyntaxChecker
    extends AbstractLogEnabled
    implements SyntaxChecker
{
    private final String m_syntaxOid ;
    private Constructor m_lexerConstructor ;
    private Constructor m_parserConstructor ;


    public ParserSyntaxChecker(String a_syntaxOid,
        Class a_lexerClass, Class a_parserClass)
    {
        m_syntaxOid = a_syntaxOid ;
		Constructor [] l_constructors = a_lexerClass.getConstructors() ;
		Constructor l_constructor = null ;

		for(int ii = 0; ii < l_constructors.length ; ii++) {
			l_constructor = l_constructors[ii] ;
			Class [] params = l_constructor.getParameterTypes() ;

			if(1 == params.length && params[0].equals(Reader.class)) {
                m_lexerConstructor = l_constructor ;
                break ;
			}
		}

        l_constructors = a_parserClass.getConstructors() ;
		for(int ii = 0; ii < l_constructors.length ; ii++) {
			l_constructor = l_constructors[ii] ;
			Class [] params = l_constructor.getParameterTypes() ;

			if(1 == params.length && params[0].equals(a_lexerClass)) {
                m_parserConstructor = l_constructor ;
                break ;
			}
		}
    }


    public String getSyntaxOid()
    {
        return m_syntaxOid ;
    }


	public boolean isValidSyntax(String a_value)
    {
        try {
            StringReader l_in = new StringReader(a_value) ;
            Object [] args = new Object[1] ;
            args[0] = l_in ;
			ANTLRLexer l_lexer = (ANTLRLexer)
                m_lexerConstructor.newInstance(args) ;
            args[0] = l_lexer ;
            Parser l_parser = (Parser) m_parserConstructor.newInstance(args) ;
            // Ok we do not know what to do with this yet!
        } catch(Exception e) {
        }

        throw new RuntimeException("N O T   I M P L E M E N T E D   Y E T !") ;
    }
}
