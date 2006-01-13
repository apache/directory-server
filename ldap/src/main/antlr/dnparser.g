/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

header {
	/*
	 * Keep the semicolon right next to org.apache.ldap.common.name or else there
	 * will be a bug that comes into the foreground in the new antlr release.
	 */
    package org.apache.ldap.common.name;

    import java.util.ArrayList ;
}

class antlrNameParser extends Parser ;

options {
	importVocab = antlrType ;
    defaultErrorHandler = false ;
}
{
    private antlrValueParser m_valueParser = 
        new antlrValueParser(getInputState()) ;

    
    public void setNormalizer(NameComponentNormalizer a_normalizer)
    {
        m_valueParser.setNormalizer(a_normalizer) ;
    }  
}


name returns [ArrayList l_list]
{
    String l_comp0 = null ;
    String l_comp1 = null ;
	l_list = new ArrayList() ;
}
	:	l_comp0=nameComponent
        {
            l_list.add(l_comp0) ;
        }
        ( ( COMMA | SEMI ) l_comp1=nameComponent
            {
                l_list.add(l_comp1) ;
            }
        )* DN_TERMINATOR 
	;


nameComponent returns [String l_comp]
{
    l_comp = null ;
    String l_tav0 = null ;
    String l_tav1 = null ;
    StringBuffer l_buf = new StringBuffer() ;
}
        : l_tav0=attributeTypeAndValue
        {
            l_buf.append(l_tav0) ;
        }
        ( PLUS l_tav1=attributeTypeAndValue 
            {
                l_buf.append('+').append(l_tav1) ;
            }
        )*
        {
            l_comp = l_buf.toString() ;
        }
    ;


attributeTypeAndValue returns [String tav]
{
    tav = null ;
    StringBuffer buf = new StringBuffer() ;
    String lhs = null ;
}
	:	( attr:ATTRIBUTE 
            {
                lhs = attr.getText() ;
				m_valueParser.setOid(false) ;
            }
        | oiddn:OIDDN
            {
                lhs = oiddn.getText().substring( "OID.".length() ) ;
                m_valueParser.setOid(true) ;
            }
        | oid:OID
            {
                lhs = oid.getText() ;
                m_valueParser.setOid(true) ;
            }
        ) EQUAL
		{
            m_valueParser.setLhs(lhs) ;
            buf.append(lhs) ;
            buf.append('=').append(m_valueParser.value()) ;
            tav = buf.toString() ;
		}
	;
