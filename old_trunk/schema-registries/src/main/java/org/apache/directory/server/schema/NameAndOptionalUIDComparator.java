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


import java.util.Comparator;

import javax.naming.Name;
import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A comparator for the uniqueMember match
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameAndOptionalUIDComparator implements Comparator
{
    // @TODO you'll need this to fix the way normalization is done
    private AttributeTypeRegistry attrRegistry;
    
    
    public NameAndOptionalUIDComparator( AttributeTypeRegistry attrRegistry )
    {
        this.attrRegistry = attrRegistry;
    }
    
    
    public NameAndOptionalUIDComparator()
    {
    }

    
    public void setRegistries( Registries registries )
    {
        attrRegistry = registries.getAttributeTypeRegistry();
    }
    
    /**
     * Comparing two uniqueMember is a matter of following this algorithm:
     * - if they are only DN, then the values should be equal
     * - otherwise, both element should contain the same DN and 
     *   * if they both have an UID, they should be equals.
     */
    public int compare( Object obj0, Object obj1 )
    {
        String dnstr0 = null;
        String dnstr1 = null;
        
        if ( ( obj0 instanceof String ) && ( obj1 instanceof String) )
        {
        	dnstr0 = (String)obj0;
        	dnstr1 = (String)obj1;
        	
        	int dash0 = dnstr0.lastIndexOf( '#' );
        	int dash1 = dnstr1.lastIndexOf( '#' );
        	
        	if ( ( dash0 == -1 ) && ( dash1 == -1 ) )
        	{
        		// no UID part
        		try
        		{
        			return getDn( dnstr0 ).compareTo( getDn ( dnstr1 ) );
        		}
        		catch ( NamingException ne )
        		{
        			return -1;
        		}
        	}
        	else
        	{
                // Now, check that we don't have another '#'
                if ( dnstr0.indexOf( '#' ) != dash0 )
                {
                    // Yes, we have one : this is not allowed, it should have been
                    // escaped.
                    return -1;
                }
        		
                if ( dnstr1.indexOf( '#' ) != dash0 )
                {
                    // Yes, we have one : this is not allowed, it should have been
                    // escaped.
                    return 1;
                }
                
                LdapDN dn0 = null;
                LdapDN dn1 = null;
                
                // This is an UID if the '#' is immediatly
                // followed by a BitString, except if the '#' is
                // on the last position
                String uid0 = dnstr0.substring( dash0 + 1 );
                
                if ( dash0 > 0 )
                {
                	try
                	{
                		dn0 = new LdapDN( dnstr0.substring( 0, dash0 ) );
                	}
                	catch ( NamingException ne )
                	{
                		return -1;
                	}
                }
                else
                {
                    return -1;
                }
                
                // This is an UID if the '#' is immediatly
                // followed by a BitString, except if the '#' is
                // on the last position
                String uid1 = dnstr1.substring( dash1 + 1 );
                
                if ( dash1 > 0 )
                {
                	try
                    {
                		dn1 = new LdapDN( dnstr0.substring( 0, dash1 ) );
                	}
                	catch ( NamingException ne )
                	{
                		return 1;
                	}
                }
                else
                {
                    return 1;
                }
                
                int dnComp = dn0.compareTo( dn1 );
                
                if ( dnComp != 0 )
                {
                	return dnComp;
                }
                
                return uid0.compareTo( uid1 );
        	}
        }
        else
        {
        	return -1;
        }
    }


    public LdapDN getDn( Object obj ) throws NamingException
    {
        LdapDN dn = null;
        
        if ( obj instanceof LdapDN )
        {
            dn = (LdapDN)obj;
            
            dn = ( dn.isNormalized() ? dn : LdapDN.normalize( dn, attrRegistry.getNormalizerMapping() ) );
        }
        else if ( obj instanceof Name )
        {
            dn = new LdapDN( ( Name ) obj );
            dn.normalize( attrRegistry.getNormalizerMapping() );
        }
        else if ( obj instanceof String )
        {
            dn = new LdapDN( ( String ) obj );
            dn.normalize( attrRegistry.getNormalizerMapping() );
        }
        else
        {
            throw new IllegalStateException( "I do not know how to handle dn comparisons with objects of class: " 
                + (obj == null ? null : obj.getClass() ) );
        }
        
        return dn;
    }
}
