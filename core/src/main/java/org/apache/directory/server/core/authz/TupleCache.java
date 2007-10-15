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

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.aci.ACIItem;
import org.apache.directory.shared.ldap.aci.ACIItemParser;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.DerefAliasesEnum;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import java.text.ParseException;
import java.util.*;


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
    private final Map<String,List<ACITuple>> tuples = new HashMap<String,List<ACITuple>>();
    
    /** a handle on the partition nexus */
    private final PartitionNexus nexus;
    
    /** a normalizing ACIItem parser */
    private final ACIItemParser aciParser;

    /** A starage for the PrescriptiveACI attributeType */
    private AttributeType prescriptiveAciAT;
    
    /**
     * The OIDs normalizer map
     */
    private Map<String, OidNormalizer> normalizerMap;

    /**
     * Creates a ACITuple cache.
     *
     * @param directoryService the context factory configuration for the server
     * @throws NamingException if initialization fails
     */
    public TupleCache( DirectoryService directoryService ) throws NamingException
    {
    	normalizerMap = directoryService.getRegistries().getAttributeTypeRegistry().getNormalizerMapping();
        this.nexus = directoryService.getPartitionNexus();
        AttributeTypeRegistry attributeTypeRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
        OidRegistry oidRegistry = directoryService.getRegistries().getOidRegistry();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( attributeTypeRegistry, oidRegistry );
        aciParser = new ACIItemParser( ncn, normalizerMap );
        prescriptiveAciAT = attributeTypeRegistry.lookup( SchemaConstants.PRESCRIPTIVE_ACI_AT );
        initialize();
    }

    
    private LdapDN parseNormalized( String name ) throws NamingException
    {
        LdapDN dn = new LdapDN( name );
        dn.normalize( normalizerMap );
        return dn;
    }


    private void initialize() throws NamingException
    {
        // search all naming contexts for access control subentenries
        // generate ACITuple Arrays for each subentry
        // add that subentry to the hash
        Iterator<String> suffixes = nexus.listSuffixes( null );
        
        while ( suffixes.hasNext() )
        {
            String suffix = suffixes.next();
            LdapDN baseDn = parseNormalized( suffix );
            ExprNode filter = new EqualityNode( SchemaConstants.OBJECT_CLASS_AT,
                    SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC );
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            NamingEnumeration<SearchResult> results = nexus.search( new SearchOperationContext( baseDn,
                    DerefAliasesEnum.NEVER_DEREF_ALIASES, filter, ctls ) );
            
            while ( results.hasMore() )
            {
                SearchResult result = results.next();
                String subentryDn = result.getName();
                Attribute aci = AttributeUtils.getAttribute( result.getAttributes(), prescriptiveAciAT );
                
                if ( aci == null )
                {
                    LOG.warn( "Found accessControlSubentry '" + subentryDn
                            + "' without any " + SchemaConstants.PRESCRIPTIVE_ACI_AT );
                    continue;
                }

                LdapDN normName = parseNormalized( subentryDn );
                subentryAdded( normName, result.getAttributes() );
            }
            
            results.close();
        }
    }


    private boolean hasPrescriptiveACI( Attributes entry ) throws NamingException
    {
        // only do something if the entry contains prescriptiveACI
        Attribute aci = AttributeUtils.getAttribute( entry, prescriptiveAciAT );

        if ( aci == null )
        {
            if ( AttributeUtils.containsValueCaseIgnore( entry.get( SchemaConstants.OBJECT_CLASS_AT ), SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) ||
                 AttributeUtils.containsValueCaseIgnore( entry.get( SchemaConstants.OBJECT_CLASS_AT ), SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC_OID ))
            {
                // should not be necessary because of schema interceptor but schema checking
                // can be turned off and in this case we must protect against being able to
                // add access control information to anything other than an AC subentry
                throw new LdapSchemaViolationException( "", ResultCodeEnum.OBJECT_CLASS_VIOLATION );
            }
            else
            {
                return false;
            }
        }
        
        return true;
    }


    public void subentryAdded( LdapDN normName, Attributes entry ) throws NamingException
    {
        // only do something if the entry contains prescriptiveACI
        Attribute aci = AttributeUtils.getAttribute( entry, prescriptiveAciAT );
        
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        List<ACITuple> entryTuples = new ArrayList<ACITuple>();
        
        for ( int ii = 0; ii < aci.size(); ii++ )
        {
            ACIItem item = null;
            String aciStr = ( String ) aci.get( ii ); 

            try
            {
                item = aciParser.parse( aciStr );
                entryTuples.addAll( item.toTuples() );
            }
            catch ( ParseException e )
            {
                String msg = "ACIItem parser failure on \n'" + item + "'\ndue to syntax error. " +
                        "Cannnot add ACITuples to TupleCache.\n" +
                        "Check that the syntax of the ACI item is correct. \nUntil this error " +
                        "is fixed your security settings will not be as expected.";
                LOG.error( msg, e );
                
                // do not process this ACI Item because it will be null
                // continue on to process the next ACI item in the entry
            }
        }
        
        tuples.put( normName.toNormName(), entryTuples );
    }


    public void subentryDeleted( Name normName, Attributes entry ) throws NamingException
    {
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        tuples.remove( normName.toString() );
    }


    public void subentryModified( LdapDN normName, List<ModificationItemImpl> mods, Attributes entry ) throws NamingException
    {
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        for ( ModificationItemImpl mod : mods )
        {
            String attrID = mod.getAttribute().getID();
            if ( attrID.equalsIgnoreCase( SchemaConstants.PRESCRIPTIVE_ACI_AT ) ||
                    attrID.equalsIgnoreCase( SchemaConstants.PRESCRIPTIVE_ACI_AT_OID ) )
            {
                subentryDeleted( normName, entry );
                subentryAdded( normName, entry );
            }
        }
    }


    public void subentryModified( LdapDN normName, Attributes mods, Attributes entry ) throws NamingException
    {
        if ( !hasPrescriptiveACI( entry ) )
        {
            return;
        }

        if ( AttributeUtils.getAttribute( mods, prescriptiveAciAT ) != null )
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


    public void subentryRenamed( Name oldName, Name newName )
    {
        tuples.put( newName.toString(), tuples.remove( oldName.toString() ) );
    }
}
