header {
/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
/*
 * Keep the semicolon right next to org.apache.ldap.common.name or else there
 * will be a bug that comes into the foreground in the new antlr release.
 */
package org.apache.ldap.common.name;

import javax.naming.NamingException ;
import org.apache.ldap.common.util.NestableRuntimeException ;
}

class antlrValueParser extends Parser ;

options {
	importVocab = antlrValue ;
    defaultErrorHandler = false ;
}
{
    boolean m_isOid = false ;
    String m_lhs = null ;
    private NameComponentNormalizer m_normalizer ;

    
    void setNormalizer( NameComponentNormalizer a_normalizer )
    {
        m_normalizer = a_normalizer ;
    }


    void setOid( boolean a_isOid )
    {
        m_isOid = a_isOid ;
    }


    void setLhs( String a_lhs )
    {
        m_lhs = a_lhs ;
    }
}


value returns [ String l_val ]
{
    l_val = null ;
    StringBuffer l_buf = new StringBuffer() ;
}
	:	( tok0:SIMPLE_STRING
	{
        //
        // This is where the normalizer comes into play where 
        // normalization is crittical for simple string values.
        //

        String l_value = tok0.getText() ;

        if ( m_normalizer != null ) 
        {
            if ( ! m_normalizer.isDefined( m_lhs ) )
            {
                l_buf.append( l_value );
            }
            else if ( m_isOid )
            {
                l_buf.append( m_normalizer.normalizeByOid( m_lhs, l_value ) ) ;
            } 
            else 
            {
                l_buf.append( m_normalizer.normalizeByName( m_lhs, l_value ) ) ;
            }
        } 
        else 
        {
            l_buf.append( l_value ) ;
        }
	}
    | tok1:HEX_STRING
    {
        // Hex strings are normalized by lower casing the alpha chars
        if ( m_normalizer != null ) 
        {
            l_buf.append( tok1.getText().toLowerCase() ) ;
        } 
        else 
        {
            l_buf.append( tok1.getText() ) ;
        }
    }
    | tok2:QUOTED_STRING
    {
        // Quoted strings must remain constant and shall be evaluated 
        // asis                
        l_buf.append( tok2.getText() ) ;
    }
    | tok3:ESCAPED_CHAR
    {
        // Escaped characters are normalized by lower casing escaped 
        // alpha characters in the escaped pair.
        if ( m_normalizer != null ) 
        {
            l_buf.append( tok3.getText().toLowerCase() ) ;
        } 
        else 
        {
            l_buf.append( tok3.getText() ) ;
        }
    }
    )+
    {
        l_val = l_buf.toString().trim() ;
    } 
	;
    exception 
    catch [ NamingException ne ]
    {
        // Wrap exception in a CascadingRuntimeException to bubble it up.
        throw new NestableRuntimeException( "Failed normalization!", ne ) ;
    }
