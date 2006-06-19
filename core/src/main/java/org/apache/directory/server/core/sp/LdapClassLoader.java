/*
 *   Copyright 2006 The Apache Software Foundation
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


package org.apache.directory.server.core.sp;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.name.LdapDN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class loader that loads classes from an LDAP DIT.
 * 
 * <p>
 * This loader looks for an configuration entry whose DN is
 * determined by defaultSearchContextsConfig variable. If there is such
 * an entry it gets the search contexts from the entry and searches the 
 * class to be loaded in those contexts.
 * If there is no default search context configuration entry it searches
 * the class in the whole DIT. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$ $Date$
 */
public class LdapClassLoader extends ClassLoader
{
    private static final Logger log = LoggerFactory.getLogger( LdapClassLoader.class );
    public static String defaultSearchContextsConfig = "cn=classLoaderDefaultSearchContext,ou=configuration,ou=system";
    private ServerLdapContext RootDSE;

    public LdapClassLoader( ServerLdapContext RootDSE ) throws NamingException
    {
        this.RootDSE = ( ( ServerLdapContext ) RootDSE.lookup( "" ) );
    }

    private byte[] findClassInDIT( NamingEnumeration searchContexts, String name ) throws ClassNotFoundException
    {
        String currentSearchContextName = null;
        ServerLdapContext currentSearchContext = null;
        NamingEnumeration javaClassEntries = null;
        byte[] classBytes = null;
        
        BranchNode filter = new BranchNode( BranchNode.AND );
        filter.addNode( new SimpleNode( "fullyQualifiedClassName", name, LeafNode.EQUALITY ) );
        filter.addNode( new SimpleNode( "objectClass", "javaClass", LeafNode.EQUALITY ) );
        
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        
        try
        {
            while( searchContexts.hasMore() )
            {
                currentSearchContextName = ( String ) searchContexts.next();
                currentSearchContext = ( ServerLdapContext ) RootDSE.lookup( currentSearchContextName );
                
                javaClassEntries = currentSearchContext.search( LdapDN.EMPTY_LDAPDN, filter, controls );
                if ( javaClassEntries.hasMore() ) // there should be only one!
                {
                    SearchResult javaClassEntry = ( SearchResult ) javaClassEntries.next();
                    Attribute byteCode = javaClassEntry.getAttributes().get( "byteCode" );
                    classBytes = ( byte[] ) byteCode.get();
                    break; // exit on first hit!
                }
            }
        }
        catch ( NamingException e )
        {
            throw new ClassNotFoundException();
        }
        
        return classBytes;
    }
    
    public Class findClass( String name ) throws ClassNotFoundException
    {
        byte[] classBytes = null;

        NamingEnumeration defaultSearchContexts = null;
        NamingEnumeration namingContexts = null;
        
        ServerLdapContext defaultSearchContextsConfigContext = null;
        
        try 
        {   
            try
            {
                defaultSearchContextsConfigContext = 
                    ( ServerLdapContext ) RootDSE.lookup( defaultSearchContextsConfig );
            }
            catch ( NamingException e )
            {
                log.debug( "No configuration data found for class loader default search contexts." );
            }
            
            if ( defaultSearchContextsConfigContext != null )
            {
                defaultSearchContexts = defaultSearchContextsConfigContext
                    .getAttributes( "", new String[] { "classLoaderDefaultSearchContext" } )
                    .get( "classLoaderDefaultSearchContext" ).getAll();
                
                try
                {
                    classBytes = findClassInDIT( defaultSearchContexts, name );
                    
                    log.debug( "Class " + name + " found under default search contexts." );
                }
                catch ( ClassNotFoundException e )
                {
                    log.debug( "Class " + name + " could not be found under default search contexts." );
                }
            }
            
            if ( classBytes == null )
            {
                namingContexts = RootDSE
                    .getAttributes( "", new String[] { "namingContexts" } )
                    .get( "namingContexts" ).getAll();
                
                classBytes = findClassInDIT( namingContexts, name );
            }
        } 
        catch ( NamingException e ) 
        {
            String msg = "Encountered JNDI failure while searching directory for class: " + name;
            log.error( msg, e );
            throw new ClassNotFoundException( msg );
        }
        catch ( ClassNotFoundException e )
        {
            String msg = "Class " + name + " not found in DIT.";
            log.warn( msg );
            throw new ClassNotFoundException( msg );
        }
        finally
        {
            if ( defaultSearchContexts != null ) { try { defaultSearchContexts.close(); } catch( Exception e ) {} };
            if ( namingContexts != null ) { try { namingContexts.close(); } catch( Exception e ) {} };
        }

        return defineClass( name, classBytes, 0, classBytes.length );
    }
}
