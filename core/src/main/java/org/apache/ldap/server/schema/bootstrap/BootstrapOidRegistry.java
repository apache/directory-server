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
package org.apache.ldap.server.schema.bootstrap;


import java.util.List;

import javax.naming.NamingException;

import org.apache.asn1new.primitives.OID;
import org.apache.commons.lang.StringUtils;
import org.apache.ldap.server.schema.AbstractOidRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Default OID registry implementation used to resolve a schema object OID 
 * to a name and vice-versa.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BootstrapOidRegistry extends AbstractOidRegistry
{ 
    /** The LoggerFactory used by this class */
    private static Logger log = LoggerFactory.getLogger( BootstrapOidRegistry.class );
	
    /**
     * @see org.apache.ldap.server.schema.OidRegistry#getOid(java.lang.String)
     */
    public String getOid( String name ) throws NamingException
    {
        if ( StringUtils.isEmpty( name ) )
        {
        	log.error( "The name to be looked at should not be null" );
            throw new NamingException( "name should not be null" );
        }
        
        /* If name is an OID than we return it back since inherently the
         * OID is another name for the object referred to by OID and the
         * caller does not know that the argument is an OID String.
         */
        if ( OID.isOID( name ) )
        {
            return name;
        }

        // If name is mapped to a OID already return OID
        if ( byName.containsKey( name ) )
        {
            String oid = ( String ) byName.get( name );
            return oid;
        }

        /*
         * As a last resort we check if name is not normalized and if the
         * normalized version used as a key returns an OID.  If the normalized
         * name works add the normalized name as a key with its OID to the
         * byName lookup.  BTW these normalized versions of the key are not
         * returned on a getNameSet.
         */
         String lowerCase = StringUtils.lowerCase( StringUtils.trim( name ) );
         
         if ( ! name.equals( lowerCase )
            && byName.containsKey( lowerCase ) )
         {
             String oid = ( String ) byName.get( lowerCase );

             // We expect to see this version of the key again so we add it
             byName.put( name, oid );
             return oid;
         }

         String msg = "OID for name '" + name + "' was not found within the OID registry";
         log.error( msg );
         throw new NamingException ( msg );
    }


    /**
     * @see org.apache.ldap.server.schema.OidRegistry#hasOid(java.lang.String)
     */
    public boolean hasOid( String name )
    {
    	if ( StringUtils.isEmpty( name ) )
    	{
    		return false;
    	}
    	
        if ( byName.containsKey( name ) || byOid.containsKey( name ) )
        {
            return true;
        }

    	String trimedName = StringUtils.trim( name );
    	
    	if ( StringUtils.isEmpty( trimedName ) )
    	{
    		return false;
    	}
    	
        String lowerCase = StringUtils.lowerCase( trimedName );

        return byName.containsKey( lowerCase ) || byOid.containsKey( lowerCase );
    }


    /**
     * @see org.apache.ldap.server.schema.OidRegistry#getNameSet(java.lang.String)
     */
    public List getNameSet( String oid ) throws NamingException
    {
        List value = super.getNameSet( oid );
        
        if ( null == value )
        {
            NamingException fault = new NamingException ( "OID '" + oid
                    + "' was not found within the OID registry" );
            throw fault;
        }
        else
        {
        	return value;
        }
    }
    
    /**
     * A String representation of the class
     */
    public String toString( String tabs )
    {
    	StringBuffer sb = new StringBuffer();
    	
    	sb.append( tabs ).append( "BootstrapOidRegistry : {\n" );
    	
    	sb.append( super.toString( tabs + "  " ) );
    	
    	sb.append( tabs ).append( "}\n" );
    	
    	return sb.toString();
    }

    /**
     * A String representation of the class
     */
    public String toString()
    {
    	return toString( "" );
    }
    
}

