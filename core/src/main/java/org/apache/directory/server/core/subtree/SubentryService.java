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
package org.apache.directory.server.core.subtree;


import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.OidRegistry;

import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.SubentriesControl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.directory.*;
import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.Name;
import java.util.*;


/**
 * The Subentry interceptor service which is responsible for filtering
 * out subentries on search operations and injecting operational attributes
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubentryService extends BaseInterceptor
{
    /** the subentry control OID */
    private static final String SUBENTRY_CONTROL = SubentriesControl.CONTROL_OID;
    /** the objectClass value for a subentry */
    private static final String SUBENTRY_OBJECTCLASS = "subentry";
    /** the objectClass OID for a subentry */
    private static final String SUBENTRY_OBJECTCLASS_OID = "2.5.17.0";

    public static final String AC_AREA = "accessControlSpecificArea";
    public static final String AC_INNERAREA = "accessControlInnerArea";
    public static final String AC_SUBENTRIES = "accessControlSubentries";

    public static final String SCHEMA_AREA = "subschemaAdminSpecificArea";
    public static final String SCHEMA_SUBENTRY = "subschemaSubentry";

    public static final String COLLECTIVE_AREA = "collectiveAttributeSpecificArea";
    public static final String COLLECTIVE_INNERAREA = "collectiveAttributeInnerArea";
    public static final String COLLECTIVE_ATTRIBUTE_SUBENTRIES = "collectiveAttributeSubentries";
    
    public static final String TRIGGER_AREA = "triggerExecutionSpecificArea";
    public static final String TRIGGER_INNERAREA = "triggerExecutionInnerArea";
    public static final String TRIGGER_SUBENTRIES = "triggerExecutionSubentries";

    public static final String[] SUBENTRY_OPATTRS =
        { AC_SUBENTRIES, SCHEMA_SUBENTRY, COLLECTIVE_ATTRIBUTE_SUBENTRIES, TRIGGER_SUBENTRIES };

    private static final Logger log = LoggerFactory.getLogger( SubentryService.class );

    /** the hash mapping the DN of a subentry to its SubtreeSpecification/types */
    private final SubentryCache subentryCache = new SubentryCache();
    
    private DirectoryServiceConfiguration factoryCfg;
    private SubtreeSpecificationParser ssParser;
    private SubtreeEvaluator evaluator;
    private PartitionNexus nexus;
    private AttributeTypeRegistry attrRegistry;
    private OidRegistry oidRegistry;
    
    private AttributeType objectClassType;


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        super.init( factoryCfg, cfg );
        this.nexus = factoryCfg.getPartitionNexus();
        this.factoryCfg = factoryCfg;
        this.attrRegistry = factoryCfg.getGlobalRegistries().getAttributeTypeRegistry();
        this.oidRegistry = factoryCfg.getGlobalRegistries().getOidRegistry();
        
        // setup various attribute type values
        objectClassType = attrRegistry.lookup( oidRegistry.getOid( "objectClass" ) );
        
        ssParser = new SubtreeSpecificationParser( new NormalizerMappingResolver()
        {
            public Map getNormalizerMapping() throws NamingException
            {
                return attrRegistry.getNormalizerMapping();
            }
        }, attrRegistry.getNormalizerMapping() );
        evaluator = new SubtreeEvaluator( factoryCfg.getGlobalRegistries().getOidRegistry() );

        // prepare to find all subentries in all namingContexts
        Iterator suffixes = this.nexus.listSuffixes();
        ExprNode filter = new SimpleNode( "objectclass", "subentry", LeafNode.EQUALITY );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[] { "subtreeSpecification", "objectClass" } );

        // search each namingContext for subentries
        while ( suffixes.hasNext() )
        {
            LdapDN suffix = new LdapDN( ( String ) suffixes.next() );
            //suffix = LdapDN.normalize( suffix, registry.getNormalizerMapping() );
            suffix.normalize( attrRegistry.getNormalizerMapping() );
            NamingEnumeration subentries = nexus.search( suffix, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes subentry = result.getAttributes();
                String dn = result.getName();
                String subtree = ( String ) subentry.get( "subtreeSpecification" ).get();
                SubtreeSpecification ss;

                try
                {
                    ss = ssParser.parse( subtree );
                }
                catch ( Exception e )
                {
                    log.warn( "Failed while parsing subtreeSpecification for " + dn );
                    continue;
                }

                LdapDN dnName = new LdapDN( dn );
                //dnName = LdapDN.normalize( dnName, registry.getNormalizerMapping() );
                dnName.normalize( attrRegistry.getNormalizerMapping() );
                subentryCache.setSubentry( dnName.toString(), ss, getSubentryTypes( subentry ) );
            }
        }
    }
    
    
    private int getSubentryTypes( Attributes subentry ) throws NamingException
    {
        int types = 0;
        
        Attribute oc = subentry.get( "objectClass" );
        if ( oc == null )
        {
            throw new LdapSchemaViolationException( "A subentry must have an objectClass attribute", 
                ResultCodeEnum.OBJECT_CLASS_VIOLATION );
        }
        
        if ( oc.contains( "accessControlSubentry" ) )
        {
            types |= Subentry.ACCESS_CONTROL_SUBENTRY;
        }
        
        if ( oc.contains( "subschema" ) )
        {
            types |= Subentry.SCHEMA_SUBENTRY;
        }
        
        if ( oc.contains( "collectiveAttributeSubentry" ) )
        {
            types |= Subentry.COLLECTIVE_SUBENTRY;
        }
        
        if ( oc.contains( "triggerExecutionSubentry" ) )
        {
            types |= Subentry.TRIGGER_SUBENTRY;
        }
        
        return types;
    }


    // -----------------------------------------------------------------------
    // Methods/Code dealing with Subentry Visibility
    // -----------------------------------------------------------------------

    public NamingEnumeration list( NextInterceptor nextInterceptor, LdapDN base ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( base );
        Invocation invocation = InvocationStack.getInstance().peek();

        if ( !isSubentryVisible( invocation ) )
        {
            return new SearchResultFilteringEnumeration( e, new SearchControls(), invocation,
                new HideSubentriesFilter() );
        }

        return e;
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor, LdapDN base, Map env, ExprNode filter,
        SearchControls searchCtls ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.search( base, env, filter, searchCtls );
        Invocation invocation = InvocationStack.getInstance().peek();

        // object scope searches by default return subentries
        if ( searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE )
        {
            return e;
        }

        // for subtree and one level scope we filter
        if ( !isSubentryVisible( invocation ) )
        {
            return new SearchResultFilteringEnumeration( e, searchCtls, invocation, new HideSubentriesFilter() );
        }
        else
        {
            return new SearchResultFilteringEnumeration( e, searchCtls, invocation, new HideEntriesFilter() );
        }
    }


    /**
     * Checks to see if subentries for the search and list operations should be
     * made visible based on the availability of the search request control
     *
     * @param invocation
     * @return true if subentries should be visible, false otherwise
     * @throws NamingException if there are problems accessing request controls
     */
    private boolean isSubentryVisible( Invocation invocation ) throws NamingException
    {
        Control[] reqControls = ( ( LdapContext ) invocation.getCaller() ).getRequestControls();

        if ( reqControls == null || reqControls.length <= 0 )
        {
            return false;
        }

        // check all request controls to see if subentry control is present
        for ( int ii = 0; ii < reqControls.length; ii++ )
        {
            // found the subentry request control so we return its value
            if ( reqControls[ii].getID().equals( SUBENTRY_CONTROL ) )
            {
                SubentriesControl subentriesControl = ( SubentriesControl ) reqControls[ii];
                return subentriesControl.isVisible();
            }
        }

        return false;
    }


    // -----------------------------------------------------------------------
    // Methods dealing with entry and subentry addition
    // -----------------------------------------------------------------------

    /**
     * Evaluates the set of subentry subtrees upon an entry and returns the
     * operational subentry attributes that will be added to the entry if
     * added at the dn specified.
     *
     * @param dn the normalized distinguished name of the entry
     * @param entryAttrs the entry attributes are generated for
     * @return the set of subentry op attrs for an entry
     * @throws NamingException if there are problems accessing entry information
     */
    public Attributes getSubentryAttributes( Name dn, Attributes entryAttrs ) throws NamingException
    {
        Attributes subentryAttrs = new LockableAttributesImpl();
        Attribute objectClasses = entryAttrs.get( "objectClass" );
        Iterator list = subentryCache.nameIterator();
        while ( list.hasNext() )
        {
            String subentryDnStr = ( String ) list.next();
            LdapDN subentryDn = new LdapDN( subentryDnStr );
            LdapDN apDn = ( LdapDN ) subentryDn.clone();
            apDn.remove( apDn.size() - 1 );
            Subentry subentry = subentryCache.getSubentry( subentryDnStr );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();

            if ( evaluator.evaluate( ss, apDn, dn, objectClasses ) )
            {                
                Attribute operational;
                
                if ( subentry.isAccessControlSubentry() )
                {
                    operational = subentryAttrs.get( AC_SUBENTRIES );
                    if ( operational == null )
                    {
                        operational = new LockableAttributeImpl( AC_SUBENTRIES );
                        subentryAttrs.put( operational );
                    }
                    operational.add( subentryDn.toString() );
                }
                if ( subentry.isSchemaSubentry() )
                {
                    operational = subentryAttrs.get( SCHEMA_SUBENTRY );
                    if ( operational == null )
                    {
                        operational = new LockableAttributeImpl( SCHEMA_SUBENTRY );
                        subentryAttrs.put( operational );
                    }
                    operational.add( subentryDn.toString() );
                }
                if ( subentry.isCollectiveSubentry() )
                {
                    operational = subentryAttrs.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES );
                    if ( operational == null )
                    {
                        operational = new LockableAttributeImpl( COLLECTIVE_ATTRIBUTE_SUBENTRIES );
                        subentryAttrs.put( operational );
                    }
                    operational.add( subentryDn.toString() );
                } 
                if ( subentry.isTriggerSubentry() )
                {
                    operational = subentryAttrs.get( TRIGGER_SUBENTRIES );
                    if ( operational == null )
                    {
                        operational = new LockableAttributeImpl( TRIGGER_SUBENTRIES );
                        subentryAttrs.put( operational );
                    }
                    operational.add( subentryDn.toString() );
                }
            }
        }

        return subentryAttrs;
    }


    public void add( NextInterceptor next, LdapDN normName, Attributes entry ) throws NamingException
    {
        Attribute objectClasses = entry.get( "objectClass" );

        if ( objectClasses.contains( "subentry" ) )
        {
            // get the name of the administrative point and its administrativeRole attributes
            LdapDN apName = ( LdapDN ) normName.clone();
            apName.remove( normName.size() - 1 );
            Attributes ap = nexus.lookup( apName );
            Attribute administrativeRole = ap.get( "administrativeRole" );

            // check that administrativeRole has something valid in it for us
            if ( administrativeRole == null || administrativeRole.size() <= 0 )
            {
                throw new LdapNoSuchAttributeException( "Administration point " + apName
                    + " does not contain an administrativeRole attribute! An"
                    + " administrativeRole attribute in the administrative point is"
                    + " required to add a subordinate subentry." );
            }

            /* ----------------------------------------------------------------
             * Build the set of operational attributes to be injected into
             * entries that are contained within the subtree repesented by this
             * new subentry.  In the process we make sure the proper roles are
             * supported by the administrative point to allow the addition of
             * this new subentry.
             * ----------------------------------------------------------------
             */
            Subentry subentry = new Subentry();
            subentry.setTypes( getSubentryTypes( entry ) );
            Attributes operational = getSubentryOperatationalAttributes( normName, subentry );

            /* ----------------------------------------------------------------
             * Parse the subtreeSpecification of the subentry and add it to the
             * SubtreeSpecification cache.  If the parse succeeds we continue
             * to add the entry to the DIT.  Thereafter we search out entries
             * to modify the subentry operational attributes of.
             * ----------------------------------------------------------------
             */
            String subtree = ( String ) entry.get( "subtreeSpecification" ).get();
            SubtreeSpecification ss;
            try
            {
                ss = ssParser.parse( subtree );
            }
            catch ( Exception e )
            {
                String msg = "Failed while parsing subtreeSpecification for " + normName.getUpName();
                log.warn( msg );
                throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
            }
            subentryCache.setSubentry( normName.toString(), ss, getSubentryTypes( entry ) );
            next.add(normName, entry );

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * while testing each entry returned for inclusion within the
             * subtree of the subentry's subtreeSpecification.  All included
             * entries will have their operational attributes merged with the
             * operational attributes calculated above.
             * ----------------------------------------------------------------
             */
            LdapDN baseDn = ( LdapDN ) apName.clone();
            baseDn.addAll( ss.getBase() );

            ExprNode filter = new PresenceNode( "2.5.4.0" ); // (objectClass=*)
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { "+", "*" } );

            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForAdd( candidate, operational ) );
                }
            }
        }
        else
        {
            Iterator list = subentryCache.nameIterator();
            while ( list.hasNext() )
            {
                String subentryDnStr = ( String ) list.next();
                LdapDN subentryDn = new LdapDN( subentryDnStr );
                LdapDN apDn = ( LdapDN ) subentryDn.clone();
                apDn.remove( apDn.size() - 1 );
                Subentry subentry = subentryCache.getSubentry( subentryDnStr );
                SubtreeSpecification ss = subentry.getSubtreeSpecification();

                if ( evaluator.evaluate( ss, apDn, normName, objectClasses ) )
                {
                    Attribute operational;
                    
                    if ( subentry.isAccessControlSubentry() )
                    {
                        operational = entry.get( AC_SUBENTRIES );
                        if ( operational == null )
                        {
                            operational = new LockableAttributeImpl( AC_SUBENTRIES );
                            entry.put( operational );
                        }
                        operational.add( subentryDn.toString() );
                    }
                    if ( subentry.isSchemaSubentry() )
                    {
                        operational = entry.get( SCHEMA_SUBENTRY );
                        if ( operational == null )
                        {
                            operational = new LockableAttributeImpl( SCHEMA_SUBENTRY );
                            entry.put( operational );
                        }
                        operational.add( subentryDn.toString() );
                    }
                    if ( subentry.isCollectiveSubentry() )
                    {
                        operational = entry.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES );
                        if ( operational == null )
                        {
                            operational = new LockableAttributeImpl( COLLECTIVE_ATTRIBUTE_SUBENTRIES );
                            entry.put( operational );
                        }
                        operational.add( subentryDn.toString() );
                    }
                    if ( subentry.isTriggerSubentry() )
                    {
                        operational = entry.get( TRIGGER_SUBENTRIES );
                        if ( operational == null )
                        {
                            operational = new LockableAttributeImpl( TRIGGER_SUBENTRIES );
                            entry.put( operational );
                        }
                        operational.add( subentryDn.toString() );
                    }
                }
            }

            next.add(normName, entry );
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry deletion
    // -----------------------------------------------------------------------

    public void delete( NextInterceptor next, LdapDN name ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Attribute objectClasses = ServerUtils.getAttribute( objectClassType, entry );

        if ( objectClasses.contains( "subentry" ) )
        {
            SubtreeSpecification ss = subentryCache.removeSubentry( name.toNormName() ).getSubtreeSpecification();
            next.delete( name );

            /* ----------------------------------------------------------------
             * Find the baseDn for the subentry and use that to search the tree
             * for all entries included by the subtreeSpecification.  Then we
             * check the entry for subentry operational attribute that contain
             * the DN of the subentry.  These are the subentry operational
             * attributes we remove from the entry in a modify operation.
             * ----------------------------------------------------------------
             */
            LdapDN apName = ( LdapDN ) name.clone();
            apName.remove( name.size() - 1 );
            LdapDN baseDn = ( LdapDN ) apName.clone();
            baseDn.addAll( ss.getBase() );

            ExprNode filter = new PresenceNode( oidRegistry.getOid( "objectclass" ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { "+", "*" } );

            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, ServerUtils.getAttribute( objectClassType, candidate ) ) )
                {
                    nexus.modify( dn, getOperationalModsForRemove( name, candidate ) );
                }
            }
        }
        else
        {
            next.delete( name );
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry name changes
    // -----------------------------------------------------------------------

    /**
     * Checks to see if an entry being renamed has a descendant that is an
     * administrative point.
     *
     * @param name the name of the entry which is used as the search base
     * @return true if name is an administrative point or one of its descendants
     * are, false otherwise
     * @throws NamingException if there are errors while searching the directory
     */
    private boolean hasAdministrativeDescendant( LdapDN name ) throws NamingException
    {
        ExprNode filter = new PresenceNode( "administrativeRole" );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        NamingEnumeration aps = nexus.search( name, factoryCfg.getEnvironment(), filter, controls );
        if ( aps.hasMore() )
        {
            aps.close();
            return true;
        }

        return false;
    }


    private ModificationItem[] getModsOnEntryRdnChange( Name oldName, Name newName, Attributes entry )
        throws NamingException
    {
        Attribute objectClasses = entry.get( "objectClass" );
        List modList = new ArrayList();

        /*
         * There are two different situations warranting action.  Firt if
         * an ss evalutating to true with the old name no longer evalutates
         * to true with the new name.  This would be caused by specific chop
         * exclusions that effect the new name but did not effect the old
         * name. In this case we must remove subentry operational attribute
         * values associated with the dn of that subentry.
         *
         * In the second case an ss selects the entry with the new name when
         * it did not previously with the old name.  Again this situation
         * would be caused by chop exclusions. In this case we must add subentry
         * operational attribute values with the dn of this subentry.
         */
        Iterator subentries = subentryCache.nameIterator();
        while ( subentries.hasNext() )
        {
            String subentryDn = ( String ) subentries.next();
            Name apDn = new LdapDN( subentryDn );
            apDn.remove( apDn.size() - 1 );
            SubtreeSpecification ss = subentryCache.getSubentry( subentryDn ).getSubtreeSpecification();
            boolean isOldNameSelected = evaluator.evaluate( ss, apDn, oldName, objectClasses );
            boolean isNewNameSelected = evaluator.evaluate( ss, apDn, newName, objectClasses );

            if ( isOldNameSelected == isNewNameSelected )
            {
                continue;
            }

            // need to remove references to the subentry
            if ( isOldNameSelected && !isNewNameSelected )
            {
                for ( int ii = 0; ii < SUBENTRY_OPATTRS.length; ii++ )
                {
                    int op = DirContext.REPLACE_ATTRIBUTE;
                    Attribute opAttr = entry.get( SUBENTRY_OPATTRS[ii] );
                    if ( opAttr != null )
                    {
                        opAttr = ( Attribute ) opAttr.clone();
                        opAttr.remove( subentryDn );

                        if ( opAttr.size() < 1 )
                        {
                            op = DirContext.REMOVE_ATTRIBUTE;
                        }

                        modList.add( new ModificationItem( op, opAttr ) );
                    }
                }
            }
            // need to add references to the subentry
            else if ( isNewNameSelected && !isOldNameSelected )
            {
                for ( int ii = 0; ii < SUBENTRY_OPATTRS.length; ii++ )
                {
                    int op = DirContext.ADD_ATTRIBUTE;
                    Attribute opAttr = new LockableAttributeImpl( SUBENTRY_OPATTRS[ii] );
                    opAttr.add( subentryDn );
                    modList.add( new ModificationItem( op, opAttr ) );
                }
            }
        }

        ModificationItem[] mods = new ModificationItem[modList.size()];
        mods = ( ModificationItem[] ) modList.toArray( mods );
        return mods;
    }


    public void modifyRn( NextInterceptor next, LdapDN name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Attribute objectClasses = ServerUtils.getAttribute( objectClassType, entry );

        if ( objectClasses.contains( "subentry" ) )
        {
            Subentry subentry = subentryCache.getSubentry( name.toNormName() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            LdapDN apName = ( LdapDN ) name.clone();
            apName.remove( apName.size() - 1 );
            LdapDN baseDn = ( LdapDN ) apName.clone();
            baseDn.addAll( ss.getBase() );
            LdapDN newName = ( LdapDN ) name.clone();
            newName.remove( newName.size() - 1 );

            LdapDN rdn = new LdapDN( newRn );
            newName.addAll( rdn );
            rdn.normalize( attrRegistry.getNormalizerMapping() );
            newName.normalize( attrRegistry.getNormalizerMapping() );

            String newNormName = newName.toNormName();
            subentryCache.setSubentry( newNormName, ss, subentry.getTypes() );
            next.modifyRn( name, newRn, deleteOldRn );

            subentry = subentryCache.getSubentry( newNormName );
            ExprNode filter = new PresenceNode( oidRegistry.getOid( "objectclass" ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { "+", "*" } );
            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, ServerUtils.getAttribute( objectClassType, candidate ) ) )
                {
                    nexus.modify( dn, getOperationalModsForReplace( name, newName, subentry, candidate ) );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( name ) )
            {
                String msg = "Will not allow rename operation on entries with administrative descendants.";
                log.warn( msg );
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
            }
            next.modifyRn( name, newRn, deleteOldRn );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            LdapDN newName = ( LdapDN ) name.clone();
            newName.remove( newName.size() - 1 );
            newName.add( newRn );
            newName.normalize( attrRegistry.getNormalizerMapping() );
            ModificationItem[] mods = getModsOnEntryRdnChange( name, newName, entry );

            if ( mods.length > 0 )
            {
                nexus.modify( newName, mods );
            }
        }
    }


    public void move( NextInterceptor next, LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn )
        throws NamingException
    {
        Attributes entry = nexus.lookup( oriChildName );
        Attribute objectClasses = ServerUtils.getAttribute( objectClassType, entry );

        if ( objectClasses.contains( "subentry" ) )
        {
            Subentry subentry = subentryCache.getSubentry( oriChildName.toNormName() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            LdapDN apName = ( LdapDN ) oriChildName.clone();
            apName.remove( apName.size() - 1 );
            LdapDN baseDn = ( LdapDN ) apName.clone();
            baseDn.addAll( ss.getBase() );
            LdapDN newName = ( LdapDN ) newParentName.clone();
            newName.remove( newName.size() - 1 );

            LdapDN rdn = new LdapDN( newRn );
            newName.addAll( rdn );
            rdn.normalize( attrRegistry.getNormalizerMapping() );
            newName.normalize( attrRegistry.getNormalizerMapping() );
            
            String newNormName = newName.toNormName();
            subentryCache.setSubentry( newNormName, ss, subentry.getTypes() );
            next.move( oriChildName, newParentName, newRn, deleteOldRn );

            subentry = subentryCache.getSubentry( newNormName );

            ExprNode filter = new PresenceNode( oidRegistry.getOid( "objectclass" ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { "+", "*" } );
            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, ServerUtils.getAttribute( objectClassType, candidate ) ) )
                {
                    nexus.modify( dn, getOperationalModsForReplace( oriChildName, newName, subentry,
                        candidate ) );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( oriChildName ) )
            {
                String msg = "Will not allow rename operation on entries with administrative descendants.";
                log.warn( msg );
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
            }
            next.move( oriChildName, newParentName, newRn, deleteOldRn );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            LdapDN newName = ( LdapDN ) newParentName.clone();
            newName.add( newRn );
            newName.normalize( attrRegistry.getNormalizerMapping() );
            ModificationItem[] mods = getModsOnEntryRdnChange( oriChildName, newName, entry );

            if ( mods.length > 0 )
            {
                nexus.modify( newName, mods );
            }
        }
    }


    public void move( NextInterceptor next, LdapDN oriChildName, LdapDN newParentName ) throws NamingException
    {
        Attributes entry = nexus.lookup( oriChildName );
        Attribute objectClasses = entry.get( "objectClass" );

        if ( objectClasses.contains( "subentry" ) )
        {
            Subentry subentry = subentryCache.getSubentry( oriChildName.toString() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            LdapDN apName = ( LdapDN ) oriChildName.clone();
            apName.remove( apName.size() - 1 );
            LdapDN baseDn = ( LdapDN ) apName.clone();
            baseDn.addAll( ss.getBase() );
            LdapDN newName = ( LdapDN ) newParentName.clone();
            newName.remove( newName.size() - 1 );
            newName.add( newParentName.get( newParentName.size() - 1 ) );

            String newNormName = newName.toNormName();
            subentryCache.setSubentry( newNormName, ss, subentry.getTypes() );
            next.move( oriChildName, newParentName );

            subentry = subentryCache.getSubentry( newNormName );

            ExprNode filter = new PresenceNode( "objectclass" );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { "+", "*" } );
            NamingEnumeration subentries = nexus.search( baseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForReplace( oriChildName, newName, subentry,
                        candidate ) );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( oriChildName ) )
            {
                String msg = "Will not allow rename operation on entries with administrative descendants.";
                log.warn( msg );
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
            }
            next.move( oriChildName, newParentName );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            LdapDN newName = ( LdapDN ) newParentName.clone();
            newName.add( oriChildName.get( oriChildName.size() - 1 ) );
            ModificationItem[] mods = getModsOnEntryRdnChange( oriChildName, newName, entry );

            if ( mods.length > 0 )
            {
                nexus.modify( newName, mods );
            }
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry modification
    // -----------------------------------------------------------------------

    
    private int getSubentryTypes( Attributes subentry, int modOp, Attributes mods ) throws NamingException
    {
        if ( mods.get( "objectClass" ) == null )
        {
            return getSubentryTypes( subentry );
        }
        
        // ok user is modifying (replacing) the objectClasses of a subentry so
        // endstate of objectClasses is contained in the mods attibutes
        if ( modOp == DirContext.REPLACE_ATTRIBUTE )
        {
            return getSubentryTypes( mods );
        }
        
        Attribute ocChanges = mods.get( "objectClass" );
        Attribute ocFinalState = ( Attribute ) subentry.get( "objectClass" ).clone();
        if ( modOp == DirContext.ADD_ATTRIBUTE )
        {
            for ( int ii = 0; ii < ocChanges.size(); ii++ )
            {
                ocFinalState.add( ocChanges.get( ii ) );
            }
        }
        else // remove operation
        {
            for ( int ii = 0; ii < ocChanges.size(); ii++ )
            {
                ocFinalState.remove( ocChanges.get( ii ) );
            }
        }
        
        Attributes attrs = new LockableAttributesImpl();
        attrs.put( ocFinalState );
        return getSubentryTypes( attrs );
    }
    

    private int getSubentryTypes( Attributes entry, ModificationItem[] mods ) throws NamingException
    {
        Attribute ocFinalState = ( Attribute ) entry.get( "objectClass" ).clone();
        for ( int ii = 0; ii < mods.length; ii++ )
        {
            if ( mods[ii].getAttribute().getID().equalsIgnoreCase( "objectClass" ) )
            {
                switch ( mods[ii].getModificationOp() )
                {
                    case( DirContext.ADD_ATTRIBUTE ):
                        for ( int jj = 0; jj < mods[ii].getAttribute().size(); jj++ )
                        {
                            ocFinalState.add( mods[ii].getAttribute().get( jj ) );
                        }
                        break;
                    case( DirContext.REMOVE_ATTRIBUTE ):
                        for ( int jj = 0; jj < mods[ii].getAttribute().size(); jj++ )
                        {
                            ocFinalState.remove( mods[ii].getAttribute().get( jj ) );
                        }
                        break;
                    case( DirContext.REPLACE_ATTRIBUTE ):
                        ocFinalState = mods[ii].getAttribute();
                        break;
                }
            }
        }
        
        Attributes attrs = new LockableAttributesImpl();
        attrs.put( ocFinalState );
        return getSubentryTypes( attrs );
    }


    public void modify( NextInterceptor next, LdapDN name, int modOp, Attributes mods ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Attribute objectClasses = ServerUtils.getAttribute( objectClassType, entry );

        if ( objectClasses.contains( "subentry" ) && mods.get( "subtreeSpecification" ) != null )
        {
            SubtreeSpecification ssOld = subentryCache.removeSubentry( name.toNormName() ).getSubtreeSpecification();
            SubtreeSpecification ssNew;

            try
            {
                ssNew = ssParser.parse( ( String ) mods.get( "subtreeSpecification" ).get() );
            }
            catch ( Exception e )
            {
                String msg = "failed to parse the new subtreeSpecification";
                log.error( msg, e );
                throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
            }

            subentryCache.setSubentry( name.toNormName(), ssNew, getSubentryTypes( entry, modOp, mods ) );
            next.modify( name, modOp, mods );

            // search for all entries selected by the old SS and remove references to subentry
            LdapDN apName = ( LdapDN ) name.clone();
            apName.remove( apName.size() - 1 );
            LdapDN oldBaseDn = ( LdapDN ) apName.clone();
            oldBaseDn.addAll( ssOld.getBase() );
            ExprNode filter = new PresenceNode( oidRegistry.getOid( "objectClass" ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { "+", "*" } );
            NamingEnumeration subentries = nexus.search( oldBaseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ssOld, apName, dn, ServerUtils.getAttribute( objectClassType, candidate ) ) )
                {
                    nexus.modify( dn, getOperationalModsForRemove( name, candidate ) );
                }
            }

            // search for all selected entries by the new SS and add references to subentry
            Subentry subentry = subentryCache.getSubentry( name.toNormName() );
            Attributes operational = getSubentryOperatationalAttributes( name, subentry );
            LdapDN newBaseDn = ( LdapDN ) apName.clone();
            newBaseDn.addAll( ssNew.getBase() );
            subentries = nexus.search( newBaseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ssNew, apName, dn, ServerUtils.getAttribute( objectClassType, candidate ) ) )
                {
                    nexus.modify( dn, getOperationalModsForAdd( candidate, operational ) );
                }
            }
        }
        else
        {
            next.modify( name, modOp, mods );
        }
    }


    public void modify( NextInterceptor next, LdapDN name, ModificationItem[] mods ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Attribute objectClasses = ServerUtils.getAttribute( objectClassType, entry );
        boolean isSubtreeSpecificationModification = false;
        ModificationItem subtreeMod = null;

        for ( int ii = 0; ii < mods.length; ii++ )
        {
            if ( "subtreeSpecification".equalsIgnoreCase( mods[ii].getAttribute().getID() ) )
            {
                isSubtreeSpecificationModification = true;
                subtreeMod = mods[ii];
            }
        }

        if ( objectClasses.contains( "subentry" ) && isSubtreeSpecificationModification )
        {
            SubtreeSpecification ssOld = subentryCache.removeSubentry( name.toString() ).getSubtreeSpecification();
            SubtreeSpecification ssNew;

            try
            {
                ssNew = ssParser.parse( ( String ) subtreeMod.getAttribute().get() );
            }
            catch ( Exception e )
            {
                String msg = "failed to parse the new subtreeSpecification";
                log.error( msg, e );
                throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
            }

            subentryCache.setSubentry( name.toNormName(), ssNew, getSubentryTypes( entry, mods ) );
            next.modify( name, mods );

            // search for all entries selected by the old SS and remove references to subentry
            LdapDN apName = ( LdapDN ) name.clone();
            apName.remove( apName.size() - 1 );
            LdapDN oldBaseDn = ( LdapDN ) apName.clone();
            oldBaseDn.addAll( ssOld.getBase() );
            ExprNode filter = new PresenceNode( oidRegistry.getOid( "objectClass" ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { "+", "*" } );
            NamingEnumeration subentries = nexus.search( oldBaseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ssOld, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForRemove( name, candidate ) );
                }
            }

            // search for all selected entries by the new SS and add references to subentry
            Subentry subentry = subentryCache.getSubentry( name.toNormName() );
            Attributes operational = getSubentryOperatationalAttributes( name, subentry );
            LdapDN newBaseDn = ( LdapDN ) apName.clone();
            newBaseDn.addAll( ssNew.getBase() );
            subentries = nexus.search( newBaseDn, factoryCfg.getEnvironment(), filter, controls );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ssNew, apName, dn, candidate.get( "objectClass" ) ) )
                {
                    nexus.modify( dn, getOperationalModsForAdd( candidate, operational ) );
                }
            }
        }
        else
        {
            next.modify( name, mods );
        }
    }


    // -----------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------

    private ModificationItem[] getOperationalModsForReplace( Name oldName, Name newName, Subentry subentry,
        Attributes entry ) throws NamingException
    {
        List modList = new ArrayList();
        
        Attribute operational;

        if ( subentry.isAccessControlSubentry() )
        {
            operational = ( Attribute ) entry.get( AC_SUBENTRIES ).clone();
            if ( operational == null )
            {
                operational = new LockableAttributeImpl( AC_SUBENTRIES );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }
            modList.add( new ModificationItem( DirContext.REPLACE_ATTRIBUTE, operational ) );
        }
        if ( subentry.isSchemaSubentry() )
        {
            operational = ( Attribute ) entry.get( SCHEMA_SUBENTRY ).clone();
            if ( operational == null )
            {
                operational = new LockableAttributeImpl( SCHEMA_SUBENTRY );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }
            modList.add( new ModificationItem( DirContext.REPLACE_ATTRIBUTE, operational ) );
        }
        if ( subentry.isCollectiveSubentry() )
        {
            operational = ( Attribute ) entry.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES ).clone();
            if ( operational == null )
            {
                operational = new LockableAttributeImpl( COLLECTIVE_ATTRIBUTE_SUBENTRIES );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }
            modList.add( new ModificationItem( DirContext.REPLACE_ATTRIBUTE, operational ) );
        }
        if ( subentry.isTriggerSubentry() )
        {
            operational = ( Attribute ) entry.get( TRIGGER_SUBENTRIES ).clone();
            if ( operational == null )
            {
                operational = new LockableAttributeImpl( TRIGGER_SUBENTRIES );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }
            modList.add( new ModificationItem( DirContext.REPLACE_ATTRIBUTE, operational ) );
        } 

        ModificationItem[] mods = new ModificationItem[modList.size()];
        return ( ModificationItem[] ) modList.toArray( mods );
    }


    /**
     * Gets the subschema operational attributes to be added to or removed from
     * an entry selected by a subentry's subtreeSpecification.
     *
     * @param name the normalized distinguished name of the subentry (the value of op attrs)
     * @param administrativeRole the roles the administrative point participates in
     * @return the set of attributes to be added or removed from entries
     * @throws NamingException if there are problems accessing attributes
     */
    private Attributes getSubentryOperatationalAttributes( Name name, Subentry subentry )
        throws NamingException
    {
        Attributes operational = new LockableAttributesImpl();
        
        if ( subentry.isAccessControlSubentry() )
        {
            if ( operational.get( AC_SUBENTRIES ) == null )
            {
                operational.put( AC_SUBENTRIES, name.toString() );
            }
            else
            {
                operational.get( AC_SUBENTRIES ).add( name.toString() );
            }
        }
        if ( subentry.isSchemaSubentry() )
        {
            if ( operational.get( SCHEMA_SUBENTRY ) == null )
            {
                operational.put( SCHEMA_SUBENTRY, name.toString() );
            }
            else
            {
                operational.get( SCHEMA_SUBENTRY ).add( name.toString() );
            }
        }
        if ( subentry.isCollectiveSubentry() )
        {
            if ( operational.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES ) == null )
            {
                operational.put( COLLECTIVE_ATTRIBUTE_SUBENTRIES, name.toString() );
            }
            else
            {
                operational.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES ).add( name.toString() );
            }
        }
        if ( subentry.isTriggerSubentry() )
        {
            if ( operational.get( TRIGGER_SUBENTRIES ) == null )
            {
                operational.put( TRIGGER_SUBENTRIES, name.toString() );
            }
            else
            {
                operational.get( TRIGGER_SUBENTRIES ).add( name.toString() );
            }
        }

        return operational;
    }


    /**
     * Calculates the subentry operational attributes to remove from a candidate
     * entry selected by a subtreeSpecification.  When we remove a subentry we
     * must remove the operational attributes in the entries that were once selected
     * by the subtree specification of that subentry.  To do so we must perform
     * a modify operation with the set of modifications to perform.  This method
     * calculates those modifications.
     *
     * @param subentryDn the distinguished name of the subentry
     * @param candidate the candidate entry to removed from the
     * @return the set of modifications required to remove an entry's reference to
     * a subentry
     */
    private ModificationItem[] getOperationalModsForRemove( LdapDN subentryDn, Attributes candidate )
    {
        List modList = new ArrayList();
        String dn = subentryDn.toNormName();

        for ( int ii = 0; ii < SUBENTRY_OPATTRS.length; ii++ )
        {
            String opAttrId = SUBENTRY_OPATTRS[ii];
            Attribute opAttr = candidate.get( opAttrId );

            if ( opAttr != null && opAttr.contains( dn ) )
            {
                Attribute attr = new LockableAttributeImpl( SUBENTRY_OPATTRS[ii] );
                attr.add( dn );
                modList.add( new ModificationItem( DirContext.REMOVE_ATTRIBUTE, attr ) );
            }
        }

        ModificationItem[] mods = new ModificationItem[modList.size()];
        return ( ModificationItem[] ) modList.toArray( mods );
    }


    /**
     * Calculates the subentry operational attributes to add or replace from
     * a candidate entry selected by a subtree specification.  When a subentry
     * is added or it's specification is modified some entries must have new
     * operational attributes added to it to point back to the associated
     * subentry.  To do so a modify operation must be performed on entries
     * selected by the subtree specification.  This method calculates the
     * modify operation to be performed on the entry.
     *
     * @param entry the entry being modified
     * @param operational the set of operational attributes supported by the AP
     * of the subentry
     * @return the set of modifications needed to update the entry
     */
    public ModificationItem[] getOperationalModsForAdd( Attributes entry, Attributes operational )
        throws NamingException
    {
        List modList = new ArrayList();

        NamingEnumeration opAttrIds = operational.getIDs();
        while ( opAttrIds.hasMore() )
        {
            int op = DirContext.REPLACE_ATTRIBUTE;
            String opAttrId = ( String ) opAttrIds.next();
            Attribute result = new LockableAttributeImpl( opAttrId );
            Attribute opAttrAdditions = operational.get( opAttrId );
            Attribute opAttrInEntry = entry.get( opAttrId );

            for ( int ii = 0; ii < opAttrAdditions.size(); ii++ )
            {
                result.add( opAttrAdditions.get( ii ) );
            }

            if ( opAttrInEntry != null && opAttrInEntry.size() > 0 )
            {
                for ( int ii = 0; ii < opAttrInEntry.size(); ii++ )
                {
                    result.add( opAttrInEntry.get( ii ) );
                }
            }
            else
            {
                op = DirContext.ADD_ATTRIBUTE;
            }

            modList.add( new ModificationItem( op, result ) );
        }

        ModificationItem[] mods = new ModificationItem[modList.size()];
        mods = ( ModificationItem[] ) modList.toArray( mods );
        return mods;
    }

    /**
     * SearchResultFilter used to filter out subentries based on objectClass values.
     */
    public class HideSubentriesFilter implements SearchResultFilter
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
            throws NamingException
        {
            String dn = result.getName();

            // see if we can get a match without normalization
            if ( subentryCache.hasSubentry( dn ) )
            {
                return false;
            }

            // see if we can use objectclass if present
            Attribute objectClasses = result.getAttributes().get( "objectClass" );
            if ( objectClasses != null )
            {
                if ( objectClasses.contains( SUBENTRY_OBJECTCLASS ) )
                {
                    return false;
                }

                if ( objectClasses.contains( SUBENTRY_OBJECTCLASS_OID ) )
                {
                    return false;
                }

                for ( int ii = 0; ii < objectClasses.size(); ii++ )
                {
                    String oc = ( String ) objectClasses.get( ii );
                    if ( oc.equalsIgnoreCase( SUBENTRY_OBJECTCLASS ) )
                    {
                        return false;
                    }
                }

                return true;
            }

            if ( !result.isRelative() )
            {
                LdapDN ndn = new LdapDN( dn );
                ndn.normalize( attrRegistry.getNormalizerMapping() );
                String normalizedDn = ndn.toString();
                return !subentryCache.hasSubentry( normalizedDn );
            }

            LdapDN name = new LdapDN( invocation.getCaller().getNameInNamespace() );
            name.normalize( attrRegistry.getNormalizerMapping() );

            LdapDN rest = new LdapDN( result.getName() );
            rest.normalize( attrRegistry.getNormalizerMapping() );
            name.addAll( rest );
            return !subentryCache.hasSubentry( name.toString() );
        }
    }

    /**
     * SearchResultFilter used to filter out normal entries but shows subentries based on 
     * objectClass values.
     */
    public class HideEntriesFilter implements SearchResultFilter
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
            throws NamingException
        {
            String dn = result.getName();

            // see if we can get a match without normalization
            if ( subentryCache.hasSubentry( dn ) )
            {
                return true;
            }

            // see if we can use objectclass if present
            Attribute objectClasses = result.getAttributes().get( "objectClass" );
            if ( objectClasses != null )
            {
                if ( objectClasses.contains( SUBENTRY_OBJECTCLASS ) )
                {
                    return true;
                }

                if ( objectClasses.contains( SUBENTRY_OBJECTCLASS_OID ) )
                {
                    return true;
                }

                for ( int ii = 0; ii < objectClasses.size(); ii++ )
                {
                    String oc = ( String ) objectClasses.get( ii );
                    if ( oc.equalsIgnoreCase( SUBENTRY_OBJECTCLASS ) )
                    {
                        return true;
                    }
                }

                return false;
            }

            if ( !result.isRelative() )
            {
                LdapDN ndn = new LdapDN( dn );
                ndn.normalize( attrRegistry.getNormalizerMapping() );
                return subentryCache.hasSubentry( ndn.toNormName() );
            }

            LdapDN name = new LdapDN( invocation.getCaller().getNameInNamespace() );
            name.normalize( attrRegistry.getNormalizerMapping() );

            LdapDN rest = new LdapDN( result.getName() );
            rest.normalize( attrRegistry.getNormalizerMapping() );
            name.addAll( rest );
            return subentryCache.hasSubentry( name.toNormName() );
        }
    }
}
