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
package org.apache.directory.server.core.sp;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.AndNode;
import org.apache.directory.shared.ldap.model.filter.BranchNode;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class loader that loads classes from an LDAP DIT.
 * 
 * <p>
 * This loader looks for an configuration entry whose Dn is
 * determined by defaultSearchContextsConfig variable. If there is such
 * an entry it gets the search contexts from the entry and searches the 
 * class to be loaded in those contexts.
 * If there is no default search context configuration entry it searches
 * the class in the whole DIT. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapClassLoader extends ClassLoader
{
    private static final Logger LOG = LoggerFactory.getLogger( LdapClassLoader.class );
    public static final String DEFAULT_SEARCH_CONTEXTS_CONFIG = "cn=classLoaderDefaultSearchContext,ou=configuration,ou=system";
    
    private Dn defaultSearchDn;
    private DirectoryService directoryService;

    /** A storage for the ObjectClass attributeType */
    private AttributeType OBJECT_CLASS_AT;

    
    public LdapClassLoader( DirectoryService directoryService ) throws LdapException
    {
        super( LdapClassLoader.class.getClassLoader() );
        this.directoryService = directoryService;
        defaultSearchDn = directoryService.getDnFactory().create( DEFAULT_SEARCH_CONTEXTS_CONFIG );
        
        OBJECT_CLASS_AT = directoryService.getSchemaManager().getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
    }

    
    private byte[] findClassInDIT( List<Dn> searchContexts, String name ) throws ClassNotFoundException
    {
        // Set up the search filter
        BranchNode filter = new AndNode( );
        AttributeType fqjcnAt = directoryService.getSchemaManager().getAttributeType( "fullyQualifiedJavaClassName" );
        filter.addNode( new EqualityNode<String>( fqjcnAt, new StringValue( name ) ) );
        filter.addNode( new EqualityNode<String>( OBJECT_CLASS_AT,
            new StringValue( ApacheSchemaConstants.JAVA_CLASS_OC ) ) );
        
        try
        {
            for ( Dn base : searchContexts )
            {
                EntryFilteringCursor cursor = null;
                try
                {
                    cursor = directoryService.getAdminSession()
                        .search( base, SearchScope.SUBTREE, filter, AliasDerefMode.DEREF_ALWAYS, null );
                    
                    cursor.beforeFirst();
                    if ( cursor.next() ) // there should be only one!
                    {
                        LOG.debug( "Class {} found under {} search context.", name, base );
                        Entry classEntry = cursor.get();

                        if ( cursor.next() )
                        {
                            Entry other = cursor.get();
                            LOG.warn( "More than one class found on classpath at locations: {} \n\tand {}", 
                                classEntry, other );
                        }

                        return classEntry.get( "javaClassByteCode" ).getBytes();
                    }
                }
                finally
                {
                    if ( cursor != null )
                    {
                        cursor.close();
                    }
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_69, name ), e );
        }

        throw new ClassNotFoundException();
    }
    
    
    public Class<?> findClass( String name ) throws ClassNotFoundException
    {
        byte[] classBytes = null;

        try 
        {   
            // TODO we should cache this information and register with the event
            // service to get notified if this changes so we can update the cached
            // copy - there's absolutely no reason why we should be performing this
            // lookup every time!!!
            
            Entry configEntry = null;
            
            try
            {
                configEntry = directoryService.getAdminSession().lookup( defaultSearchDn );
            }
            catch ( LdapException e )
            {
                LOG.debug( "No configuration data found for class loader default search contexts." );
            }
            
            if ( configEntry != null )
            {
                List<Dn> searchContexts = new ArrayList<Dn>();
                Attribute attr = configEntry.get( "classLoaderDefaultSearchContext" );
                
                for ( Value<?> val : attr )
                {
                    Dn dn = directoryService.getDnFactory().create( val.getString() );
                    searchContexts.add( dn );
                }
                
                try
                {
                    classBytes = findClassInDIT( searchContexts, name );
                    
                    LOG.debug( "Class " + name + " found under default search contexts." );
                }
                catch ( ClassNotFoundException e )
                {
                    LOG.debug( "Class " + name + " could not be found under default search contexts." );
                }
            }
            
            if ( classBytes == null )
            {
                List<Dn> namingContexts = new ArrayList<Dn>();
                
                Set<String> suffixes = directoryService.getPartitionNexus().listSuffixes();

                for ( String suffix:suffixes )
                {
                    Dn dn = directoryService.getDnFactory().create( suffix );
                    namingContexts.add( dn );
                }
                
                classBytes = findClassInDIT( namingContexts, name );
            }
        } 
        catch ( ClassNotFoundException e )
        {
            String msg = I18n.err( I18n.ERR_293, name );
            LOG.debug( msg );
            throw new ClassNotFoundException( msg );
        }
        catch ( Exception e ) 
        {
            String msg = I18n.err( I18n.ERR_70, name );
            LOG.error( msg, e );
            throw new ClassNotFoundException( msg );
        }
        
        return defineClass( name, classBytes, 0, classBytes.length );
    }
}
