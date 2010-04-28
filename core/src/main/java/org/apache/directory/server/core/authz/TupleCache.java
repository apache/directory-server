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

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.aci.ACIItem;
import org.apache.directory.shared.ldap.aci.ACIItemParser;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.normalizers.ConcreteNameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A cache for tuple sets which responds to specific events to perform
 * cache house keeping as access control subentries are added, deleted
 * and modified.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class TupleCache
{
    /** the logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( TupleCache.class );

    /** a map of strings to ACITuple collections */
    private final Map<String, List<ACITuple>> tuples = new HashMap<String, List<ACITuple>>();

    /** a handle on the partition nexus */
    private final PartitionNexus nexus;

    /** a normalizing ACIItem parser */
    private final ACIItemParser aciParser;

    /** A storage for the PrescriptiveACI attributeType */
    private AttributeType prescriptiveAciAT;


    /**
     * Creates a ACITuple cache.
     *
     * @param directoryService the context factory configuration for the server
     * @throws NamingException if initialization fails
     */
    public TupleCache( CoreSession session ) throws Exception
    {
        SchemaManager schemaManager = session.getDirectoryService().getSchemaManager();
        this.nexus = session.getDirectoryService().getPartitionNexus();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        aciParser = new ACIItemParser( ncn, schemaManager.getNormalizerMapping() );
        prescriptiveAciAT = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.PRESCRIPTIVE_ACI_AT );
        initialize( session );
    }


    private DN parseNormalized( SchemaManager schemaManager, String name ) throws LdapException
    {
        DN dn = new DN( name );
        dn.normalize( schemaManager.getNormalizerMapping() );
        return dn;
    }


    private void initialize( CoreSession session ) throws Exception
    {
        // search all naming contexts for access control subentenries
        // generate ACITuple Arrays for each subentry
        // add that subentry to the hash
        Set<String> suffixes = nexus.listSuffixes( null );

        for ( String suffix:suffixes )
        {
            DN baseDn = parseNormalized( session.getDirectoryService().getSchemaManager(), suffix );
            ExprNode filter = new EqualityNode<String>( SchemaConstants.OBJECT_CLASS_AT, 
                new StringValue( SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            
            SearchOperationContext searchOperationContext = new SearchOperationContext( session,
                baseDn, filter, ctls );
            searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

            EntryFilteringCursor results = nexus.search( searchOperationContext );

            while ( results.next() )
            {
                Entry result = results.get();
                DN subentryDn = result.getDn().normalize( session.getDirectoryService().getSchemaManager().
                        getNormalizerMapping() );
                EntryAttribute aci = result.get( prescriptiveAciAT );

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
    }


    private boolean hasPrescriptiveACI( Entry entry ) throws LdapException
    {
        // only do something if the entry contains prescriptiveACI
        EntryAttribute aci = entry.get( prescriptiveAciAT );

        if ( aci == null )
        {
            if ( entry.contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC )
                || entry.contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC_OID ) )
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


    public void subentryAdded( DN normName, Entry entry ) throws LdapException
    {
        // only do something if the entry contains prescriptiveACI
        EntryAttribute aciAttr = entry.get( prescriptiveAciAT );

        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        List<ACITuple> entryTuples = new ArrayList<ACITuple>();

        for ( Value<?> value : aciAttr )
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

        tuples.put( normName.getNormName(), entryTuples );
    }


    public void subentryDeleted( DN normName, Entry entry ) throws LdapException
    {
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        tuples.remove( normName.toString() );
    }


    public void subentryModified( DN normName, List<Modification> mods, Entry entry ) throws LdapException
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


    public void subentryModified( DN normName, Entry mods, Entry entry ) throws LdapException
    {
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        if ( mods.get( prescriptiveAciAT ) != null )
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


    public void subentryRenamed( DN oldName, DN newName )
    {
        tuples.put( newName.getNormName(), tuples.remove( oldName.getNormName() ) );
    }
}
