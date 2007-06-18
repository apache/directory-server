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
package org.apache.directory.shared.ldap.schema.syntax;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a name is valid for an ObjectClass
 * or an AttributeType<br/><br/>
 * 
 * &lt;m-name&gt; = &lt;keystring&gt; <br/>
 * &lt;keystring&gt; = &lt;leadkeychar&gt; *&lt;keychar&gt;<br/>
 * &lt;leadkeychar&gt; = &lt;ALPHA&gt;<br/>
 * &lt;keychar&gt; = &lt;ALPHA&gt; / &lt;DIGIT&gt; / &lt;HYPHEN&gt; / &lt;SEMI&gt;<br/>
 * &lt;ALPHA&gt;   = %x41-5A / %x61-7A   ; "A"-"Z" / "a"-"z"<br/>
 * &lt;DIGIT&gt;   = %x30 / &lt;LDIGIT       ; "0"-"9"<br/>
 * &lt;LDIGIT&gt;  = %x31-39             ; "1"-"9"<br/>
 * &lt;HYPHEN&gt;  = %x2D ; hyphen ("-")<br/>
 * &lt;SEMI&gt;    = %x3B ; semicolon (";")<br/>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ObjectNameSyntaxChecker extends AbstractSyntaxChecker
{
    /** The Syntax OID, defined specifically for ApacheDS */
    private static final String SC_OID = "1.3.6.1.4.1.18060.0.4.0.0.6";
    
    private static final String REGEXP = "^([a-zA-Z][a-zA-Z0-9-;]*)$";
    
    private static final Pattern PATTERN =  Pattern.compile( REGEXP );
    
    /**
     * 
     * Creates a new instance of ObjectNameSyntaxChecker.
     *
     */
    public ObjectNameSyntaxChecker()
    {
        super( SC_OID );
    }
    
    /**
     * 
     * Creates a new instance of ObjectNameSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected ObjectNameSyntaxChecker( String oid )
    {
        super( oid );
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(java.lang.Object)
     */
    public boolean isValidSyntax( Object value )
    {
        String strValue;

        if ( value == null )
        {
            return false;
        }
        
        if ( value instanceof String )
        {
            strValue = ( String ) value;
        }
        else if ( value instanceof byte[] )
        {
            strValue = StringTools.utf8ToString( ( byte[] ) value ); 
        }
        else
        {
            strValue = value.toString();
        }

        if ( strValue.length() == 0 )
        {
            return false;
        }

        // Search for the '$' separator
        Matcher match = PATTERN.matcher ( strValue );
        
        return match.matches();    
    }
}
