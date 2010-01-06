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
package org.apache.directory.shared.ldap.schema.normalizers;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A noirmalizer for UniqueMember
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UniqueMemberNormalizer extends Normalizer
{
    // The serial UID
    private static final long serialVersionUID = 1L;

    /** A reference to the schema manager used to normalize the DN */
    private SchemaManager schemaManager;
    
    
    public UniqueMemberNormalizer()
    {
        super( SchemaConstants.UNIQUE_MEMBER_MATCH_MR_OID );
    }
    

    public Value<?> normalize( Value<?> value ) throws NamingException
    {
        String nameAndUid = value.getString();
            
        if ( nameAndUid.length() == 0 )
        {
            return null;
        }
        
        // Let's see if we have an UID part
        int sharpPos = nameAndUid.lastIndexOf( '#' );
        
        if ( sharpPos != -1 )
        {
            // Now, check that we don't have another '#'
            if ( nameAndUid.indexOf( '#' ) != sharpPos )
            {
                // Yes, we have one : this is not allowed, it should have been
                // escaped.
                return null;
            }
            
            // This is an UID if the '#' is immediately
            // followed by a BitString, except if the '#' is
            // on the last position
            String uid = nameAndUid.substring( sharpPos + 1 );
            
            if ( sharpPos > 0 )
            {
                LdapDN dn = new LdapDN( nameAndUid.substring( 0, sharpPos ) );
                
                dn.normalize( schemaManager.getNormalizerMapping() );
                
                return new ClientStringValue( dn.getNormName() + '#' + uid );
            }
            else
            {
                throw new IllegalStateException( "I do not know how to handle NameAndOptionalUID normalization with objects of class: " 
                    + value.getClass() );
            }
        }
        else
        {
            // No UID, the strValue is a DN
            // Return the normalized DN
            return new ClientStringValue( new LdapDN( nameAndUid ).getNormName() );
        }
    }


    public String normalize( String value ) throws NamingException
    {
        if ( StringTools.isEmpty( value ) )
        {
            return null;
        }
        
        // Let's see if we have an UID part
        int sharpPos = value.lastIndexOf( '#' );
        
        if ( sharpPos != -1 )
        {
            // Now, check that we don't have another '#'
            if ( value.indexOf( '#' ) != sharpPos )
            {
                // Yes, we have one : this is not allowed, it should have been
                // escaped.
                return null;
            }
            
            // This is an UID if the '#' is immediatly
            // followed by a BitString, except if the '#' is
            // on the last position
            String uid = value.substring( sharpPos + 1 );
            
            if ( sharpPos > 0 )
            {
                LdapDN dn = new LdapDN( value.substring( 0, sharpPos ) );
                
                dn.normalize( schemaManager.getNormalizerMapping() );
                
                return dn.getNormName() + '#' + uid;
            }
            else
            {
                throw new IllegalStateException( "I do not know how to handle NameAndOptionalUID normalization with objects of class: " 
                    + value.getClass() );
            }
        }
        else
        {
            // No UID, the strValue is a DN
            // Return the normalized DN
            return new LdapDN( value ).normalize( schemaManager.getNormalizerMapping() ).getNormName();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }
}
