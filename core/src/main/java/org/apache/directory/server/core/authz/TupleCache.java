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
package org.apache.directory.server.core.authz;


import java.text.ParseException;
import java.util.*;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.partition.DirectoryPartitionNexus;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.aci.ACIItem;
import org.apache.directory.shared.ldap.aci.ACIItemParser;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DnParser;
import org.apache.directory.shared.ldap.name.LdapName;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
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
    /** the attribute id for prescriptive aci: prescriptiveACI */
    private static final String ACI_ATTR = "prescriptiveACI";
    /** the attribute id for an object class: objectClass */
    private static final String OC_ATTR = "objectClass";
    /** the object class for access control subentries: accessControlSubentry */
    private static final String ACSUBENTRY_OC = "accessControlSubentry";

    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( TupleCache.class );

    /** cloned startup environment properties we use for subentry searching */
    private final Hashtable env;
    /** a map of strings to ACITuple collections */
    private final Map tuples = new HashMap();
    /** a handle on the partition nexus */
    private final DirectoryPartitionNexus nexus;
    /** a normalizing ACIItem parser */
    private final ACIItemParser aciParser;
    /** a normalizing DN parser */
    private final DnParser dnParser;


    /**
     * Creates a ACITuple cache.
     *
     * @param factoryCfg the context factory configuration for the server
     */
    public TupleCache( DirectoryServiceConfiguration factoryCfg ) throws NamingException
    {
        this.nexus = factoryCfg.getPartitionNexus();
        AttributeTypeRegistry registry = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( registry );
        aciParser = new ACIItemParser( ncn );
        dnParser = new DnParser( ncn );
        env = ( Hashtable ) factoryCfg.getEnvironment().clone();
        initialize();
    }


    private void initialize() throws NamingException
    {
        // search all naming contexts for access control subentenries
        // generate ACITuple Arrays for each subentry
        // add that subentry to the hash
        Iterator suffixes = nexus.listSuffixes( true );
        while ( suffixes.hasNext() )
        {
            String suffix = ( String ) suffixes.next();
            Name baseDn = new LdapName( suffix );
            ExprNode filter = new SimpleNode( OC_ATTR, ACSUBENTRY_OC, SimpleNode.EQUALITY );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            NamingEnumeration results = nexus.search( baseDn, env, filter, ctls );
            while ( results.hasMore() )
            {
                SearchResult result = ( SearchResult ) results.next();
                String subentryDn = result.getName();
                Attribute aci = result.getAttributes().get( ACI_ATTR );
                if ( aci == null )
                {
                    log.warn( "Found accessControlSubentry '" + subentryDn + "' without any " + ACI_ATTR );
                    continue;
                }

                Name normName = dnParser.parse( subentryDn );
                subentryAdded( subentryDn, normName, result.getAttributes() );
            }
            results.close();
        }
    }


    private boolean hasPrescriptiveACI( Attributes entry ) throws NamingException
    {
        // only do something if the entry contains prescriptiveACI
        Attribute aci = entry.get( ACI_ATTR );
        if ( aci == null && entry.get( OC_ATTR ).contains( ACSUBENTRY_OC ) )
        {
            // should not be necessary because of schema interceptor but schema checking
            // can be turned off and in this case we must protect against being able to
            // add access control information to anything other than an AC subentry
            throw new LdapSchemaViolationException( "", ResultCodeEnum.OBJECTCLASSVIOLATION );
        }
        else if ( aci == null )
        {
            return false;
        }
        return true;
    }


    public void subentryAdded( String upName, Name normName, Attributes entry ) throws NamingException
    {
        // only do something if the entry contains prescriptiveACI
        Attribute aci = entry.get( ACI_ATTR );
        if ( ! hasPrescriptiveACI( entry ) )
        {
            return;
        }

        List entryTuples = new ArrayList();
        for ( int ii = 0; ii < aci.size(); ii++ )
        {
            ACIItem item = null;

            try
            {
                item = aciParser.parse( ( String ) aci.get( ii ) );
            }
            catch ( ParseException e )
            {
                String msg = "ACIItem parser failure on '"+item+"'. Cannnot add ACITuples to TupleCache.";
                log.warn( msg, e );
            }

            entryTuples.addAll( item.toTuples() );
        }
        tuples.put( normName.toString(), entryTuples );
    }


    public void subentryDeleted( Name normName, Attributes entry ) throws NamingException
    {                                                                                      
        if ( ! hasPrescriptiveACI( entry ) )
        {
            return;
        }

        tuples.remove( normName.toString() );
    }


    public void subentryModified( Name normName, ModificationItem[] mods, Attributes entry ) throws NamingException
    {
        if ( ! hasPrescriptiveACI( entry ) )
        {
            return;
        }

        boolean isAciModified = false;
        for ( int ii = 0; ii < mods.length; ii++ )
        {
            isAciModified |= mods[ii].getAttribute().contains( ACI_ATTR );
        }
        if ( isAciModified )
        {
            subentryDeleted( normName, entry );
            subentryAdded( normName.toString(), normName, entry );
        }
    }


    public void subentryModified( Name normName, int modOp, Attributes mods, Attributes entry ) throws NamingException
    {
        if ( ! hasPrescriptiveACI( entry ) )
        {
            return;
        }

        if ( mods.get( ACI_ATTR ) != null )
        {
            subentryDeleted( normName, entry );
            subentryAdded( normName.toString(), normName, entry );
        }
    }                                                     


    public List getACITuples( String subentryDn )
    {
        List aciTuples = ( List ) tuples.get( subentryDn );
        if ( aciTuples == null )
        {
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableList( aciTuples );
    }


    public void subentryRenamed( Name oldName, Name newName )
    {
        tuples.put( newName.toString(), tuples.remove( oldName.toString() ) );
    }
}
