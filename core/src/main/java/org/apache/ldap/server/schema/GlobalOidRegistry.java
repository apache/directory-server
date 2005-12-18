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
package org.apache.ldap.server.schema;


import java.util.List;

import javax.naming.NamingException;

import org.apache.asn1new.primitives.OID;
import org.apache.commons.lang.StringUtils;
import org.apache.ldap.server.schema.bootstrap.BootstrapOidRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Default OID registry implementation used to resolve a schema object OID 
 * to a name and vice-versa.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GlobalOidRegistry extends AbstractOidRegistry
{ 
    /** The LoggerFactory used by this Interceptor */
    private static Logger log = LoggerFactory.getLogger( GlobalOidRegistry.class );

    /** the underlying bootstrap registry to delegate on misses to */
    private BootstrapOidRegistry bootstrap;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a default OidRegistry by initializing the map and the montior.
     */
    public GlobalOidRegistry( BootstrapOidRegistry bootstrap )
    {
        if ( bootstrap == null )
        {
            throw new NullPointerException( "the bootstrap registry cannot be null" ) ;
        }

        this.bootstrap = bootstrap;
    }

    // ------------------------------------------------------------------------
    // Service Methods
    // ------------------------------------------------------------------------


    /**
     * @see OidRegistry#getOid(String)
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

        if ( bootstrap.hasOid( name ) )
        {
            String oid = bootstrap.getOid( name );
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
        
        if ( ! name.equals( lowerCase ) )
		{
			if ( byName.containsKey( lowerCase ) )
	        {
	            String oid = ( String ) byName.get( lowerCase );

	            // We expect to see this version of the key again so we add it
	            byName.put( name, oid );
                return oid;
	        }
			
			/*
			 * Some LDAP servers (MS Active Directory) tend to use some of the
			 * bootstrap oid names as all caps, like OU. This should resolve that.
			 * Lets stash this in the byName if we find it.
			 */
			
			if ( bootstrap.hasOid( lowerCase) )
			{
	            String oid = bootstrap.getOid( name );

	            // We expect to see this version of the key again so we add it
                byName.put( name, oid );
                return oid;
			}
		}

        String msg = "OID for name '" + name + "' was not found within the OID registry";
        log.error( msg );
        throw new NamingException ( msg );
    }


    /**
     * @see OidRegistry#hasOid(String)
     */
    public boolean hasOid( String name )
    {
    	if ( StringUtils.isEmpty( name ) )
    	{
    		return false;
    	}
    	
        // check first with non-normalized name
        if ( byName.containsKey( name ) || byOid.containsKey( name ) )
        {
            return true;
        }

        // check next with non-normalized name on the bootstrap registry
        if ( bootstrap.hasOid( name ) )
        {
            return true;
        }

        /*
        * As a last resort we check if name is not normalized and if the
        * normalized version used as a key returns an OID.  If the normalized
        * name works add the normalized name as a key with its OID to the
        * byName lookup.  BTW these normalized versions of the key are not
        * returned on a getNameSet.
        */
    	String trimedName = StringUtils.trim( name );
    	
    	if ( StringUtils.isEmpty( trimedName ) )
    	{
    		return false;
    	}
    	
        String lowerCase = StringUtils.lowerCase( trimedName );
        
        if ( ! name.equals( lowerCase ) )
		{
			if ( byName.containsKey( lowerCase ) )
	        {
	            String oid = ( String ) byName.get( lowerCase );

	            // We expect to see this version of the key again so we add it
	            byName.put( name, oid );
                return true;
	        }

			/*
			 * Some LDAP servers (MS Active Directory) tend to use some of the
			 * bootstrap oid names as all caps, like OU. This should resolve that.
			 * Lets stash this in the byName if we find it.
			 */
			if ( bootstrap.hasOid( lowerCase) )
			{
                return true;
			}
		}

        return false;
    }

    /**
     * @see OidRegistry#getNameSet(String)
     */
    public List getNameSet( String oid ) throws NamingException
    {
        List value = super.getNameSet( oid );
        
        if ( null == value )
        {
            return bootstrap.getNameSet( oid );
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
    	
    	sb.append( tabs ).append( "GlobalOidRegistry :\n" );
    	
    	sb.append( super.toString( tabs + "  " ) );
    	
    	sb.append( tabs ).append( bootstrap == null ? "no bootstrap" : bootstrap.toString() );
    	
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

