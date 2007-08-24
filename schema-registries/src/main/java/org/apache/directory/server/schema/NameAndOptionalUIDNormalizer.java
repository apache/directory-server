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
package org.apache.directory.server.schema;


import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * A noirmalizer for UniqueMember
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameAndOptionalUIDNormalizer implements Normalizer
{
    private static final long serialVersionUID = 1L;

    private AttributeTypeRegistry attrRegistry;
    
    
    public NameAndOptionalUIDNormalizer( AttributeTypeRegistry attrRegistry )
    {
        this.attrRegistry = attrRegistry;
    }
    
    
    public NameAndOptionalUIDNormalizer()
    {
    }
 
    
    public void setRegistries( Registries registries )
    {
        this.attrRegistry = registries.getAttributeTypeRegistry();
    }
    

    public Object normalize( Object value ) throws NamingException
    {
        if ( value instanceof String )
        {
            String nameAndUid = (String)value;
            
            if ( nameAndUid.length() == 0 )
            {
                return false;
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
                    return false;
                }
                
                // This is an UID if the '#' is immediatly
                // followed by a BitString, except if the '#' is
                // on the last position
                String uid = nameAndUid.substring( sharpPos + 1 );
                
                if ( sharpPos > 0 )
                {
                    LdapDN dn = new LdapDN( nameAndUid.substring( 0, sharpPos ) );
                    
                    dn.normalize( attrRegistry.getNormalizerMapping() );
                    
                    return dn.getNormName() + '#' + uid;
                }
                else
                {
                    throw new IllegalStateException( "I do not know how to handle NameAndOptionalUID normalization with objects of class: " 
                        + (value == null ? null : value.getClass() ) );
                }
            }
            else
            {
                // No UID, the strValue is a DN
                // Return the normalized DN
                return new LdapDN( nameAndUid ).getNormName();
            }
        }
        else
        {
            throw new IllegalStateException( "I do not know how to handle NameAndOptionalUID normalization with objects of class: " 
                + (value == null ? null : value.getClass() ) );
        }
    }
}
