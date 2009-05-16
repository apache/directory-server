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
package org.apache.directory.shared.ldap.schema.syntaxes;


import org.apache.directory.shared.ldap.schema.AbstractSyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value is a IA5 String according to RFC 4517.
 * 
 * From RFC 4517 :
 * IA5String          = *(%x00-7F)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class Ia5StringSyntaxChecker extends AbstractSyntaxChecker
{
    /** The Syntax OID, according to RFC 4517, par. 3.3.15 */
    private static final String SC_OID = "1.3.6.1.4.1.1466.115.121.1.26";
    

    /**
     * 
     * Creates a new instance of Ia5StringSyntaxChecker.
     *
     */
    public Ia5StringSyntaxChecker()
    {
        super( SC_OID );
    }
    
    /**
     * 
     * Creates a new instance of Ia5StringSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected Ia5StringSyntaxChecker( String oid )
    {
        super( oid );
    }
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(java.lang.Object)
     */
    public boolean isValidSyntax( Object value )
    {
        String strValue = null;

        if ( value == null )
        {
            return true;
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

        return StringTools.isIA5String( strValue );
    }
}
