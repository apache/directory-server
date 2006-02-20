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
	 * Keep the semicolon right next to org.apache.directory.shared.ldap.name or else there
	 * will be a bug that comes into the foreground in the new antlr release.
	 */
    package org.apache.directory.shared.ldap.name;

    import java.util.ArrayList;
    import org.apache.directory.shared.ldap.util.StringTools;
}

class antlrNameParser extends Parser;

options {
	importVocab = antlrType;
    defaultErrorHandler = false;
}
{
    private antlrValueParser valueParser = new antlrValueParser( getInputState() );

    
    public void setNormalizer(NameComponentNormalizer a_normalizer)
    {
        valueParser.setNormalizer(a_normalizer);
    }  
}


name returns [ArrayList list]
{
    String comp0 = null;
    String comp1 = null;
	list = new ArrayList();
}
	:	comp0=nameComponent
        {
            list.add( comp0 );
        }
        ( ( COMMA | SEMI ) comp1=nameComponent
            {
                list.add( comp1);
            }
        )* DN_TERMINATOR 
	;


nameComponent returns [String comp]
{
    comp = null;
    String tav0 = null;
    String tav1 = null;
    StringBuffer buf = new StringBuffer();
}
        : tav0=attributeTypeAndValue
        {
            buf.append( tav0 );
        }
        ( PLUS tav1=attributeTypeAndValue 
            {
                buf.append('+').append( tav1 );
            }
        )*
        {
            comp = buf.toString();
        }
   ;


attributeTypeAndValue returns [String tav]
{
    tav = null;
    StringBuffer buf = new StringBuffer();
    String lhs = null;
}
	:	( attr:ATTRIBUTE 
            {
                lhs = attr.getText();
				valueParser.setOid( false );
            }
        | oiddn:OIDDN
            {
                lhs = oiddn.getText().substring( "OID.".length() );
                valueParser.setOid( true );
            }
        | oid:OID
            {
                lhs = oid.getText();
                valueParser.setOid( true );
            }
        ) EQUAL
		{
		    if ( valueParser.isNormalizing() )
		    {
		    	lhs = StringTools.lowerCase( lhs );
		    }
		    
            valueParser.setLhs( lhs );
            buf.append( lhs );
            buf.append( '=' ).append( valueParser.value() );
            tav = buf.toString();
		}
	;
