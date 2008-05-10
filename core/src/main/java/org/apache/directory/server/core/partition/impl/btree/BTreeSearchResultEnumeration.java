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
package org.apache.directory.server.core.partition.impl.btree;


import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerSearchResult;
import org.apache.directory.server.core.enumeration.SearchResultEnumeration;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.UsageEnum;


/**
 * An enumeration that transforms another underlying enumeration over a set of 
 * IndexRecords into an enumeration over a set of SearchResults.  Note that the
 * SearchResult created may not be complete and other parts of the system may
 * modify it before return.  This enumeration simply creates a new copy of the 
 * entry to return stuffing it with the attributes that were specified.  This is
 * all that it does now but this may change later.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BTreeSearchResultEnumeration implements SearchResultEnumeration
{
    /** Database used to lookup entries from */
    private BTreePartition partition = null;
    /** the attributes to return */
    private final String[] attrIds;
    /** underlying enumeration over IndexRecords */
    private final NamingEnumeration<ForwardIndexEntry> underlying;

    private boolean attrIdsHasStar = false;
    private boolean attrIdsHasPlus = false;
    private Registries registries = null;


    /**
     * using the search parameters supplied to a search call.
     * Creates an enumeration that returns entries packaged within SearchResults
     * 
     * @param attrIds the returned attributes
     * @param underlying the enumeration over IndexRecords
     */
    public BTreeSearchResultEnumeration( String[] attrIds, NamingEnumeration<ForwardIndexEntry> underlying, BTreePartition db,
        Registries registries )
    {
        this.partition = db;
        this.attrIds = attrIds;
        this.underlying = underlying;
        this.attrIdsHasStar = containsStar( attrIds );
        this.attrIdsHasPlus = containsPlus( attrIds );
        this.registries = registries;
    }


    /**
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close() throws NamingException
    {
        underlying.close();
    }


    /**
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore() throws NamingException
    {
        return underlying.hasMore();
    }


    /**
     * @see javax.naming.NamingEnumeration#next()
     */
    public ServerSearchResult next() throws NamingException
    {
        IndexEntry rec = underlying.next();
        ServerEntry entry = rec.getEntry();
        String name = partition.getEntryUpdn( ( Long )rec.getId() );
        LdapDN dn = new LdapDN( name );

        if ( null == entry )
        {
            rec.setObject( partition.lookup( ( Long ) rec.getId() ) );
            rec.setEntry( entry );
            entry = ( ServerEntry ) entry.clone();
        }
        
        LdapDN dn = entry.getDn();

        if ( ( attrIds == null ) || ( attrIdsHasPlus && attrIdsHasStar ) )
        {
            entry = ServerEntryUtils.toServerEntry( ( Attributes ) rec.getObject().clone(), dn, registries );
        }
        else if ( attrIdsHasPlus && attrIdsHasStar )
        {
            entry = ServerEntryUtils.toServerEntry( ( Attributes ) rec.getObject().clone(), dn, registries );
        }
        else if ( attrIdsHasPlus )
        {
            entry = new DefaultServerEntry( registries, dn );

            // add all listed attributes
            for ( String attrId:attrIds )
            {
                if ( attrId.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
                {
                    continue;
                }
                // there is no attribute by that name in the entry so we continue
                if ( null == rec.getObject().get( attrId ) )
                {
                    continue;
                }

            	ServerAttribute attr =  ( ServerAttribute ) rec.getObject().get( attrId ).clone(); 
            	entry.put( attr );
            }

            // add all operational attributes
            for ( EntryAttribute attribute:rec.getObject() )
            {
                AttributeType attrType = ( ( ServerAttribute ) attribute ).getAttributeType();
                
                if ( attrType.getUsage() == UsageEnum.USER_APPLICATIONS )
                {
                    continue;
                }

            	ServerAttribute attr = ( ServerAttribute ) attribute.clone(); 
                entry.put( attr );
            }
        }
        else if ( attrIdsHasStar )
        {
            entry = new DefaultServerEntry( registries, dn );

            // add all listed operational attributes
            for ( String attrId:attrIds )
            {
                if ( attrId.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) )
                {
                    continue;
                }
                // there is no attribute by that name in the entry so we continue
                if ( null == rec.getObject().get( attrId ) )
                {
                    continue;
                }

                // clone attribute to stuff into the new resultant entry
            	ServerAttribute attr = ( ServerAttribute ) rec.getObject().get( attrId ).clone(); 
                entry.put( attr );
            }

            // add all user attributes
            for ( EntryAttribute attribute:rec.getObject() )
            {
                AttributeType attrType = ((ServerAttribute)attribute).getAttributeType();
                
                if ( attrType.getUsage() == UsageEnum.USER_APPLICATIONS )
                {
                    ServerAttribute attr = ( ServerAttribute ) rec.getEntry().get( attrType ).clone(); 
                    entry.put( attr );
                }
            }
        }
        else
        {
        	Set<EntryAttribute> entryAttrs = new HashSet<EntryAttribute>(); 
        	
        	for ( EntryAttribute entryAttribute:entry )
        	{
        		entryAttrs.add( entryAttribute );
        	}
            //entry = new DefaultServerEntry( registries, dn );
            
            for ( String attrId:attrIds )
            {
                if ( SchemaConstants.NO_ATTRIBUTE.equals( attrId ) )
                {
                    break;
                }
                
                EntryAttribute attr = entry.get( registries.getAttributeTypeRegistry().lookup( attrId ) );
                
                // there is no attribute by that name in the entry so we continue
                if ( null == attr )
                {
                    // May be it's because the attributeType is a inherited one?
                    Iterator<AttributeType> descendants = registries.getAttributeTypeRegistry().descendants( attrId );
                    
                    while ( descendants.hasNext() )
                    {
                        AttributeType atype = descendants.next();
                        
                        attr = entry.get( atype );
                        
                        if ( attr != null )
                        {
                            // we may have more than one descendant, like sn and cn
                            // for name, so add all of them
                            //entry.put( (ServerAttribute)attr.clone() );
                        	entryAttrs.remove( attr );
                        }
                    }
                }
                else
                {
                    // clone attribute to stuff into the new resultant entry
                    //entry.put( (ServerAttribute)attr.clone() );
                	entryAttrs.remove( attr );
                }
            }
            
            for ( EntryAttribute entryAttribute:entryAttrs )
            {
            	entry.remove( entryAttribute );
            }
        }

        BTreeSearchResult result = new BTreeSearchResult( (Long)rec.getId(), dn, null, entry );
        result.setRelative( false );
        return result;
    }


    private boolean containsStar( String[] ids )
    {
        if ( ids == null )
        {
            return false;
        }

        for ( int ii = ids.length - 1; ii >= 0; ii-- )
        {
            if ( ids[ii].trim().equals( SchemaConstants.ALL_USER_ATTRIBUTES ) )
            {
                return true;
            }
        }

        return false;
    }


    private boolean containsPlus( String[] ids )
    {
        if ( ids == null )
        {
            return false;
        }

        for ( int ii = ids.length - 1; ii >= 0; ii-- )
        {
            if ( ids[ii].trim().equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
            {
                return true;
            }
        }

        return false;
    }


    /**
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return underlying.hasMoreElements();
    }


    /**
     * @see java.util.Enumeration#nextElement()
     */
    public ServerSearchResult nextElement()
    {
        try
        {
            return next();
        }
        catch ( NamingException e )
        {
            NoSuchElementException nsee = 
                new NoSuchElementException( "Encountered NamingException on underlying enumeration." );
            nsee.initCause( e );
            throw nsee;
        }
    }
}
