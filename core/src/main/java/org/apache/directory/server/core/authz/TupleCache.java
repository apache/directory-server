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
package org.apache.directory.server.core.authz;


import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DnFactory;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.aci.ACIItem;
import org.apache.directory.shared.ldap.aci.ACIItemParser;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.*;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.shared.ldap.model.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.model.filter.EqualityNode;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.message.AliasDerefMode;
import org.apache.directory.shared.ldap.model.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A cache for tuple sets which responds to specific events to perform
 * cache house keeping as access control subentries are added, deleted
 * and modified.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TupleCache
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( TupleCache.class );

    /** a map of strings to ACITuple collections */
    private final Map<String, List<ACITuple>> tuples = new HashMap<String, List<ACITuple>>();

    /** the Dn factory */
    private final DnFactory dnFactory;

    /** a handle on the partition nexus */
    private final PartitionNexus nexus;

    /** a normalizing ACIItem parser */
    private final ACIItemParser aciParser;

    /** A storage for the PrescriptiveACI attributeType */
    private AttributeType PRESCRIPTIVE_ACI_AT;

    /** A storage for the ObjectClass attributeType */
    private static AttributeType OBJECT_CLASS_AT;


    /**
     * Creates a ACITuple cache.
     *
     * @param session the session with the directory core services
     * @throws LdapException if initialization fails
     */
    public TupleCache( CoreSession session ) throws LdapException
    {
        SchemaManager schemaManager = session.getDirectoryService().getSchemaManager();
        this.dnFactory = session.getDirectoryService().getDnFactory();
        this.nexus = session.getDirectoryService().getPartitionNexus();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        aciParser = new ACIItemParser( ncn, schemaManager );
        PRESCRIPTIVE_ACI_AT = schemaManager.getAttributeType( SchemaConstants.PRESCRIPTIVE_ACI_AT );
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        initialize( session );
    }


    private Dn parseNormalized( String name ) throws LdapException
    {
        Dn dn = dnFactory.create( name );
        return dn;
    }


    private void initialize( CoreSession session ) throws LdapException
    {
        // search all naming contexts for access control subentenries
        // generate ACITuple Arrays for each subentry
        // add that subentry to the hash
        Set<String> suffixes = nexus.listSuffixes();

        for ( String suffix:suffixes )
        {
            Dn baseDn = parseNormalized( suffix );
            ExprNode filter = new EqualityNode<String>( OBJECT_CLASS_AT,
                new StringValue( SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            SearchOperationContext searchOperationContext = new SearchOperationContext( session,
                baseDn, filter, ctls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor results = nexus.search( searchOperationContext );

            try
            {
                while ( results.next() )
                {
                    Entry result = results.get();
                    Dn subentryDn = result.getDn().normalize( session.getDirectoryService().getSchemaManager() );
                    EntryAttribute aci = result.get( PRESCRIPTIVE_ACI_AT );

                    if ( aci == null )
                    {
                        LOG.warn( "Found accessControlSubentry '" + subentryDn + "' without any "
                            + SchemaConstants.PRESCRIPTIVE_ACI_AT );
                        continue;
                    }

                    subentryAdded( subentryDn, result );
                }

                results.close();
            }
            catch ( Exception e )
            {
                throw new LdapOperationErrorException( e.getMessage() );
            }
        }
    }


    /**
     * Check if the Entry contains a prescriptiveACI
     */
    private boolean hasPrescriptiveACI( Entry entry ) throws LdapException
    {
        // only do something if the entry contains prescriptiveACI
        EntryAttribute aci = entry.get( PRESCRIPTIVE_ACI_AT );

        if ( aci == null )
        {
            if ( entry.contains( OBJECT_CLASS_AT, SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) )
            {
                // should not be necessary because of schema interceptor but schema checking
                // can be turned off and in this case we must protect against being able to
                // add access control information to anything other than an AC subentry
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION, "" );
            }
            else
            {
                return false;
            }
        }

        return true;
    }


    public void subentryAdded( Dn dn, Entry entry ) throws LdapException
    {
        // only do something if the entry contains a prescriptiveACI
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        // Get the prescriptiveACI
        EntryAttribute prescriptiveAci = entry.get( PRESCRIPTIVE_ACI_AT );

        List<ACITuple> entryTuples = new ArrayList<ACITuple>();

        // Loop on all the ACI, parse each of them and
        // store the associated tuples into the cache
        for ( Value<?> value : prescriptiveAci )
        {
            String aci = value.getString();
            ACIItem item = null;

            try
            {
                item = aciParser.parse( aci );
                entryTuples.addAll( item.toTuples() );
            }
            catch ( ParseException e )
            {
                String msg = I18n.err( I18n.ERR_28, item );
                LOG.error( msg, e );

                // do not process this ACI Item because it will be null
                // continue on to process the next ACI item in the entry
            }
        }

        tuples.put( dn.getNormName(), entryTuples );
    }


    public void subentryDeleted( Dn normName, Entry entry ) throws LdapException
    {
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        tuples.remove( normName.toString() );
    }


    public void subentryModified( Dn normName, List<Modification> mods, Entry entry ) throws LdapException
    {
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        for ( Modification mod : mods )
        {
            if ( mod.getAttribute().instanceOf( SchemaConstants.PRESCRIPTIVE_ACI_AT ) )
            {
                subentryDeleted( normName, entry );
                subentryAdded( normName, entry );
            }
        }
    }


    public void subentryModified( Dn normName, Entry mods, Entry entry ) throws LdapException
    {
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        if ( mods.get( PRESCRIPTIVE_ACI_AT ) != null )
        {
            subentryDeleted( normName, entry );
            subentryAdded( normName, entry );
        }
    }


    @SuppressWarnings("unchecked")
    public List<ACITuple> getACITuples( String subentryDn )
    {
        List aciTuples = tuples.get( subentryDn );
        if ( aciTuples == null )
        {
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableList( aciTuples );
    }


    public void subentryRenamed( Dn oldName, Dn newName )
    {
        tuples.put( newName.getNormName(), tuples.remove( oldName.getNormName() ) );
    }
}
