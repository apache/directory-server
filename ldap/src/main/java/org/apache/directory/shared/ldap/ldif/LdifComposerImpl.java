/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

package org.apache.directory.shared.ldap.ldif;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;

import org.apache.directory.shared.ldap.util.Base64;


/**
 * An LDAP Data Interchange Format (LDIF) composer.
 * 
 * <pre>
 *  &lt;ldif-file&gt; ::= &quot;version:&quot; &lt;fill&gt; &lt;number&gt; &lt;seps&gt; &lt;dn-spec&gt; &lt;sep&gt; 
 *  &lt;ldif-content-change&gt;
 *  
 *  &lt;ldif-content-change&gt; ::= 
 *    &lt;number&gt; &lt;oid&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; 
 *    &lt;ldif-attrval-record-e&gt; | 
 *    &lt;alpha&gt; &lt;chars-e&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; 
 *    &lt;ldif-attrval-record-e&gt; | 
 *    &quot;control:&quot; &lt;fill&gt; &lt;number&gt; &lt;oid&gt; &lt;spaces-e&gt; &lt;criticality&gt; 
 *    &lt;value-spec-e&gt; &lt;sep&gt; &lt;controls-e&gt; 
 *        &quot;changetype:&quot; &lt;fill&gt; &lt;changerecord-type&gt; &lt;ldif-change-record-e&gt; |
 *    &quot;changetype:&quot; &lt;fill&gt; &lt;changerecord-type&gt; &lt;ldif-change-record-e&gt;
 *                              
 *  &lt;ldif-attrval-record-e&gt; ::= &lt;seps&gt; &lt;dn-spec&gt; &lt;sep&gt; &lt;attributeType&gt; 
 *    &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; 
 *    &lt;ldif-attrval-record-e&gt; | e
 *                              
 *  &lt;ldif-change-record-e&gt; ::= &lt;seps&gt; &lt;dn-spec&gt; &lt;sep&gt; &lt;controls-e&gt; 
 *    &quot;changetype:&quot; &lt;fill&gt; &lt;changerecord-type&gt; &lt;ldif-change-record-e&gt; | e
 *                              
 *  &lt;dn-spec&gt; ::= &quot;dn:&quot; &lt;fill&gt; &lt;safe-string&gt; | &quot;dn::&quot; &lt;fill&gt; &lt;base64-string&gt;
 *                              
 *  &lt;controls-e&gt; ::= &quot;control:&quot; &lt;fill&gt; &lt;number&gt; &lt;oid&gt; &lt;spaces-e&gt; &lt;criticality&gt; 
 *    &lt;value-spec-e&gt; &lt;sep&gt; &lt;controls-e&gt; | e
 *                              
 *  &lt;criticality&gt; ::= &quot;true&quot; | &quot;false&quot; | e
 *                              
 *  &lt;oid&gt; ::= '.' &lt;number&gt; &lt;oid&gt; | e
 *                              
 *  &lt;attrval-specs-e&gt; ::= &lt;number&gt; &lt;oid&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; 
 *  &lt;attrval-specs-e&gt; | 
 *    &lt;alpha&gt; &lt;chars-e&gt; &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; | e
 *                              
 *  &lt;value-spec-e&gt; ::= &lt;value-spec&gt; | e
 *  
 *  &lt;value-spec&gt; ::= ':' &lt;fill&gt; &lt;safe-string-e&gt; | 
 *    &quot;::&quot; &lt;fill&gt; &lt;base64-chars&gt; | 
 *    &quot;:&lt;&quot; &lt;fill&gt; &lt;url&gt;
 *  
 *  &lt;attributeType&gt; ::= &lt;number&gt; &lt;oid&gt; | &lt;alpha&gt; &lt;chars-e&gt;
 *  
 *  &lt;options-e&gt; ::= ';' &lt;char&gt; &lt;chars-e&gt; &lt;options-e&gt; |e
 *                              
 *  &lt;chars-e&gt; ::= &lt;char&gt; &lt;chars-e&gt; |  e
 *  
 *  &lt;changerecord-type&gt; ::= &quot;add&quot; &lt;sep&gt; &lt;attributeType&gt; 
 *  &lt;options-e&gt; &lt;value-spec&gt; &lt;sep&gt; &lt;attrval-specs-e&gt; | 
 *    &quot;delete&quot; &lt;sep&gt; | 
 *    &quot;modify&quot; &lt;sep&gt; &lt;mod-type&gt; &lt;fill&gt; &lt;attributeType&gt; &lt;options-e&gt; 
 *    &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;sep&gt; '-' &lt;sep&gt; &lt;mod-specs-e&gt; | 
 *    &quot;moddn&quot; &lt;sep&gt; &lt;newrdn&gt; &lt;sep&gt; &quot;deleteoldrdn:&quot; &lt;fill&gt; 
 *    &lt;0-1&gt; &lt;sep&gt; &lt;newsuperior-e&gt; &lt;sep&gt; |
 *    &quot;modrdn&quot; &lt;sep&gt; &lt;newrdn&gt; &lt;sep&gt; &quot;deleteoldrdn:&quot; &lt;fill&gt; 
 *    &lt;0-1&gt; &lt;sep&gt; &lt;newsuperior-e&gt; &lt;sep&gt;
 *  
 *  &lt;newrdn&gt; ::= ':' &lt;fill&gt; &lt;safe-string&gt; | &quot;::&quot; &lt;fill&gt; &lt;base64-chars&gt;
 *  
 *  &lt;newsuperior-e&gt; ::= &quot;newsuperior&quot; &lt;newrdn&gt; | e
 *  
 *  &lt;mod-specs-e&gt; ::= &lt;mod-type&gt; &lt;fill&gt; &lt;attributeType&gt; &lt;options-e&gt; 
 *    &lt;sep&gt; &lt;attrval-specs-e&gt; &lt;sep&gt; '-' &lt;sep&gt; &lt;mod-specs-e&gt; | e
 *  
 *  &lt;mod-type&gt; ::= &quot;add:&quot; | &quot;delete:&quot; | &quot;replace:&quot;
 *  
 *  &lt;url&gt; ::= &lt;a Uniform Resource Locator, as defined in [6]&gt;
 *  
 *  
 *  
 *  LEXICAL
 *  -------
 *  
 *  &lt;fill&gt;           ::= ' ' &lt;fill&gt; | e
 *  &lt;char&gt;           ::= &lt;alpha&gt; | &lt;digit&gt; | '-'
 *  &lt;number&gt;         ::= &lt;digit&gt; &lt;digits&gt;
 *  &lt;0-1&gt;            ::= '0' | '1'
 *  &lt;digits&gt;         ::= &lt;digit&gt; &lt;digits&gt; | e
 *  &lt;digit&gt;          ::= '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9'
 *  &lt;seps&gt;           ::= &lt;sep&gt; &lt;seps-e&gt; 
 *  &lt;seps-e&gt;         ::= &lt;sep&gt; &lt;seps-e&gt; | e
 *  &lt;sep&gt;            ::= 0x0D 0x0A | 0x0A
 *  &lt;spaces&gt;         ::= ' ' &lt;spaces-e&gt;
 *  &lt;spaces-e&gt;       ::= ' ' &lt;spaces-e&gt; | e
 *  &lt;safe-string-e&gt;  ::= &lt;safe-string&gt; | e
 *  &lt;safe-string&gt;    ::= &lt;safe-init-char&gt; &lt;safe-chars&gt;
 *  &lt;safe-init-char&gt; ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x1F] | [0x21-0x39] | 0x3B | [0x3D-0x7F]
 *  &lt;safe-chars&gt;     ::= &lt;safe-char&gt; &lt;safe-chars&gt; | e
 *  &lt;safe-char&gt;      ::= [0x01-0x09] | 0x0B | 0x0C | [0x0E-0x7F]
 *  &lt;base64-string&gt;  ::= &lt;base64-char&gt; &lt;base64-chars&gt;
 *  &lt;base64-chars&gt;   ::= &lt;base64-char&gt; &lt;base64-chars&gt; | e
 *  &lt;base64-char&gt;    ::= 0x2B | 0x2F | [0x30-0x39] | 0x3D | [0x41-9x5A] | [0x61-0x7A]
 *  &lt;alpha&gt;          ::= [0x41-0x5A] | [0x61-0x7A]
 *  
 *  COMMENTS
 *  --------
 *  - The ldap-oid VN is not correct in the RFC-2849. It has been changed from 1*DIGIT 0*1(&quot;.&quot; 1*DIGIT) to
 *  DIGIT+ (&quot;.&quot; DIGIT+)*
 *  - The mod-spec lacks a sep between *attrval-spec and &quot;-&quot;.
 *  - The BASE64-UTF8-STRING should be BASE64-CHAR BASE64-STRING
 *  - The ValueSpec rule must accept multilines values. In this case, we have a LF followed by a 
 *  single space before the continued value.
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class LdifComposerImpl implements LdifComposer
{
    /**
     * Generates an LDIF from a multi map.
     * 
     * @param attrHash the multi map of single and multivalued attributes.
     * @return the LDIF as a String.
     */
    public String compose( Map<String,?> attrHash )
    {
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter( sw );

        for ( String key:attrHash.keySet() )
        {
            Collection<?> valueCol = ( Collection<?> ) attrHash.get( key );

            if ( valueCol.isEmpty() )
            {
                continue;
            }
            else if ( valueCol.size() == 1 )
            {
                out.print( key );
                out.print( ':' );
                Object val = valueCol.iterator().next();

                if ( val.getClass().isArray() )
                {
                    out.print( ": " );
                    out.println( base64encode( ( byte[] ) val ) );
                }
                else
                {
                    out.print( ' ' );
                    out.println( val );
                }
                continue;
            }

            for ( Object val:valueCol )
            {
                out.print( key );
                out.print( ':' );

                if ( val.getClass().isArray() )
                {
                    out.print( ": " );
                    out.println( base64encode( ( byte[] ) val ) );
                }
                else
                {
                    out.print( ' ' );
                    out.println( val );
                }
            }
        }

        return sw.getBuffer().toString();
    }


    /**
     * Encodes an binary data into a base64 String.
     * 
     * @param byteArray
     *            the value of a binary attribute.
     * @return the encoded binary data as a char array.
     */
    public char[] base64encode( byte[] byteArray )
    {
        return Base64.encode( byteArray );
    }
}
