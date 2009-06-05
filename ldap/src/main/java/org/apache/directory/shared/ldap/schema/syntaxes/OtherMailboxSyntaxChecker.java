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


import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.AbstractSyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SyntaxChecker which verifies that a value is an OtherMailbox according to 
 * RFC 4517 :
 * 
 * OtherMailbox = mailbox-type DOLLAR mailbox
 * mailbox-type = PrintableString
 * mailbox      = IA5String
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class OtherMailboxSyntaxChecker extends AbstractSyntaxChecker
{
    /**
     * 
     * Creates a new instance of OtherMailboxSyntaxChecker.
     *
     */
    public OtherMailboxSyntaxChecker()
    {
        super( SchemaConstants.OTHER_MAILBOX_SYNTAX );
    }
    
    /**
     * 
     * Creates a new instance of OtherMailboxSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected OtherMailboxSyntaxChecker( String oid )
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
        int dollar = strValue.indexOf( '$' );
        
        if ( dollar == -1 )
        {
            // No '$' => error
            return false;
        }
        
        String mailboxType = strValue.substring( 0, dollar );
        
        String mailbox = ( ( dollar < strValue.length() - 1 ) ? 
            strValue.substring( dollar + 1 ) : "" ); 
        
        // The mailbox should not contains a '$'
        if ( mailbox.indexOf( '$' ) != -1 )
        {
            return false;
        }
            
        // Check that the mailboxType is a PrintableString
        if ( !StringTools.isPrintableString( mailboxType ) )
        {
            return false;
        }
        
        // Check that the mailbox is an IA5String
        return ( StringTools.isIA5String( mailbox ) );
    }
}
