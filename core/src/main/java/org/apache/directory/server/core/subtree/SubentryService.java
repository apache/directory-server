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

import org.apache.directory.server.core.interceptor.BaseInterceptor;


import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.*;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.*;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationParser;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * The Subentry interceptor service which is responsible for filtering
 * out subentries on search operations and injecting operational attributes
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubentryService extends BaseInterceptor
{
    /** the subentry control OID */
    private static final String SUBENTRY_CONTROL = SubentriesControl.CONTROL_OID;

    public static final String AC_AREA = "accessControlSpecificArea";
    public static final String AC_INNERAREA = "accessControlInnerArea";
    //public static final String AC_SUBENTRIES = "accessControlSubentries";

    public static final String SCHEMA_AREA = "subschemaAdminSpecificArea";

    public static final String COLLECTIVE_AREA = "collectiveAttributeSpecificArea";
    public static final String COLLECTIVE_INNERAREA = "collectiveAttributeInnerArea";
    
    public static final String TRIGGER_AREA = "triggerExecutionSpecificArea";
    public static final String TRIGGER_INNERAREA = "triggerExecutionInnerArea";

    public static final String[] SUBENTRY_OPATTRS =
        { 
    	SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT, 
    	SchemaConstants.SUBSCHEMA_SUBENTRY_AT, 
    	SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT, 
    	SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT 
    	};

    private static final Logger LOG = LoggerFactory.getLogger( SubentryService.class );

    /** the hash mapping the DN of a subentry to its SubtreeSpecification/types */
    private final SubentryCache subentryCache = new SubentryCache();
    
    private DirectoryService directoryService;
    private SubtreeSpecificationParser ssParser;
    private SubtreeEvaluator evaluator;
    private PartitionNexus nexus;
    private AttributeTypeRegistry attrRegistry;
    private OidRegistry oidRegistry;
    
    private AttributeType objectClassType;


    public void init( DirectoryService directoryService ) throws NamingException
    {
        super.init( directoryService );
        this.nexus = directoryService.getPartitionNexus();
        this.directoryService = directoryService;
        this.attrRegistry = directoryService.getRegistries().getAttributeTypeRegistry();
        this.oidRegistry = directoryService.getRegistries().getOidRegistry();
        
        // setup various attribute type values
        objectClassType = attrRegistry.lookup( oidRegistry.getOid( SchemaConstants.OBJECT_CLASS_AT ) );
        
        ssParser = new SubtreeSpecificationParser( new NormalizerMappingResolver()
        {
            public Map<String, OidNormalizer> getNormalizerMapping() throws NamingException
            {
                return attrRegistry.getNormalizerMapping();
            }
        }, attrRegistry.getNormalizerMapping() );
        evaluator = new SubtreeEvaluator( directoryService.getRegistries().getOidRegistry(),
                directoryService.getRegistries().getAttributeTypeRegistry() );

        // prepare to find all subentries in all namingContexts
        Iterator<String> suffixes = this.nexus.listSuffixes( null );
        ExprNode filter = new EqualityNode( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.SUBENTRY_OC );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[] { SchemaConstants.SUBTREE_SPECIFICATION_AT, SchemaConstants.OBJECT_CLASS_AT } );

        // search each namingContext for subentries
        while ( suffixes.hasNext() )
        {
            LdapDN suffix = new LdapDN( suffixes.next() );
            //suffix = LdapDN.normalize( suffix, registry.getNormalizerMapping() );
            suffix.normalize( attrRegistry.getNormalizerMapping() );
            NamingEnumeration<SearchResult> subentries = nexus.search( 
                new SearchOperationContext( suffix, directoryService.getEnvironment(), filter, controls ) );
            
            while ( subentries.hasMore() )
            {
                SearchResult result = subentries.next();
                Attributes subentry = result.getAttributes();
                String dn = result.getName();
                String subtree = ( String ) subentry.get( SchemaConstants.SUBTREE_SPECIFICATION_AT ).get();
                SubtreeSpecification ss;

                try
                {
                    ss = ssParser.parse( subtree );
                }
                catch ( Exception e )
                {
                    LOG.warn( "Failed while parsing subtreeSpecification for " + dn );
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
        
        Attribute oc = subentry.get( SchemaConstants.OBJECT_CLASS_AT );
        if ( oc == null )
        {
            throw new LdapSchemaViolationException( "A subentry must have an objectClass attribute", 
                ResultCodeEnum.OBJECT_CLASS_VIOLATION );
        }
        
        if ( AttributeUtils.containsValueCaseIgnore( oc, SchemaConstants.ACCESS_CONTROL_SUBENTRY_OC ) )
        {
            types |= Subentry.ACCESS_CONTROL_SUBENTRY;
        }
        
        if ( AttributeUtils.containsValueCaseIgnore( oc, "subschema" ) )
        {
            types |= Subentry.SCHEMA_SUBENTRY;
        }
        
        if ( AttributeUtils.containsValueCaseIgnore( oc, "collectiveAttributeSubentry" ) )
        {
            types |= Subentry.COLLECTIVE_SUBENTRY;
        }
        
        if ( AttributeUtils.containsValueCaseIgnore( oc, ApacheSchemaConstants.TRIGGER_EXECUTION_SUBENTRY_OC ) )
        {
            types |= Subentry.TRIGGER_SUBENTRY;
        }
        
        return types;
    }


    // -----------------------------------------------------------------------
    // Methods/Code dealing with Subentry Visibility
    // -----------------------------------------------------------------------

    public NamingEnumeration<SearchResult> list( NextInterceptor nextInterceptor, ListOperationContext opContext ) throws NamingException
    {
        NamingEnumeration<SearchResult> result = nextInterceptor.list( opContext );
        Invocation invocation = InvocationStack.getInstance().peek();

        if ( !isSubentryVisible( invocation ) )
        {
            return new SearchResultFilteringEnumeration( result, new SearchControls(), invocation,
                new HideSubentriesFilter(), "List Subentry filter" );
        }

        return result;
    }


    public NamingEnumeration<SearchResult> search( NextInterceptor nextInterceptor, SearchOperationContext opContext ) throws NamingException
    {
        NamingEnumeration<SearchResult> result = nextInterceptor.search( opContext );
        Invocation invocation = InvocationStack.getInstance().peek();
        SearchControls searchCtls = opContext.getSearchControls();

        // object scope searches by default return subentries
        if ( searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE )
        {
            return result;
        }

        // for subtree and one level scope we filter
        if ( !isSubentryVisible( invocation ) )
        {
            return new SearchResultFilteringEnumeration( result, searchCtls, invocation, new HideSubentriesFilter(), "Search Subentry filter hide subentries" );
        }
        else
        {
            return new SearchResultFilteringEnumeration( result, searchCtls, invocation, new HideEntriesFilter(), "Search Subentry filter hide entries");
        }
    }


    /**
     * Checks to see if subentries for the search and list operations should be
     * made visible based on the availability of the search request control
     *
     * @param invocation the invocation object to use for determining subentry visibility
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
        for ( Control reqControl : reqControls )
        {
            // found the subentry request control so we return its value
            if ( reqControl.getID().equals( SUBENTRY_CONTROL ) )
            {
                SubentriesControl subentriesControl = ( SubentriesControl ) reqControl;
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
        Attributes subentryAttrs = new AttributesImpl();
        Iterator<String> list = subentryCache.nameIterator();
        
        while ( list.hasNext() )
        {
            String subentryDnStr = list.next();
            LdapDN subentryDn = new LdapDN( subentryDnStr );
            LdapDN apDn = ( LdapDN ) subentryDn.clone();
            apDn.remove( apDn.size() - 1 );
            Subentry subentry = subentryCache.getSubentry( subentryDnStr );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();

            if ( evaluator.evaluate( ss, apDn, dn, entryAttrs ) )
            {                
                Attribute operational;
                
                if ( subentry.isAccessControlSubentry() )
                {
                    operational = subentryAttrs.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );
                    if ( operational == null )
                    {
                        operational = new AttributeImpl( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );
                        subentryAttrs.put( operational );
                    }
                    operational.add( subentryDn.toString() );
                }
                if ( subentry.isSchemaSubentry() )
                {
                    operational = subentryAttrs.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
                    if ( operational == null )
                    {
                        operational = new AttributeImpl( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
                        subentryAttrs.put( operational );
                    }
                    operational.add( subentryDn.toString() );
                }
                if ( subentry.isCollectiveSubentry() )
                {
                    operational = subentryAttrs.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
                    if ( operational == null )
                    {
                        operational = new AttributeImpl( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
                        subentryAttrs.put( operational );
                    }
                    operational.add( subentryDn.toString() );
                } 
                if ( subentry.isTriggerSubentry() )
                {
                    operational = subentryAttrs.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
                    if ( operational == null )
                    {
                        operational = new AttributeImpl( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
                        subentryAttrs.put( operational );
                    }
                    operational.add( subentryDn.toString() );
                }
            }
        }

        return subentryAttrs;
    }


    public void add( NextInterceptor next, AddOperationContext opContext ) throws NamingException
    {
    	LdapDN name = opContext.getDn();
    	Attributes entry = opContext.getEntry();
    	
        Attribute objectClasses = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC ) )
        {
            // get the name of the administrative point and its administrativeRole attributes
            LdapDN apName = ( LdapDN ) name.clone();
            apName.remove( name.size() - 1 );
            Attributes ap = nexus.lookup( new LookupOperationContext( apName ) );
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
            Attributes operational = getSubentryOperatationalAttributes( name, subentry );

            /* ----------------------------------------------------------------
             * Parse the subtreeSpecification of the subentry and add it to the
             * SubtreeSpecification cache.  If the parse succeeds we continue
             * to add the entry to the DIT.  Thereafter we search out entries
             * to modify the subentry operational attributes of.
             * ----------------------------------------------------------------
             */
            String subtree = ( String ) entry.get( SchemaConstants.SUBTREE_SPECIFICATION_AT ).get();
            SubtreeSpecification ss;
            
            try
            {
                ss = ssParser.parse( subtree );
            }
            catch ( Exception e )
            {
                String msg = "Failed while parsing subtreeSpecification for " + name.getUpName();
                LOG.warn( msg );
                throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
            }
            
            subentryCache.setSubentry( name.getNormName(), ss, getSubentryTypes( entry ) );
            next.add( opContext );

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

            ExprNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT_OID ); // (objectClass=*)
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            NamingEnumeration<SearchResult> subentries = 
                nexus.search( 
                    new SearchOperationContext( baseDn, directoryService.getEnvironment(), filter, controls ) );

            while ( subentries.hasMore() )
            {
                SearchResult result = subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                {
                    nexus.modify( new ModifyOperationContext( dn, getOperationalModsForAdd( candidate, operational )  ));
                }
            }
        }
        else
        {
            Iterator<String> list = subentryCache.nameIterator();
            
            while ( list.hasNext() )
            {
                String subentryDnStr = list.next();
                LdapDN subentryDn = new LdapDN( subentryDnStr );
                LdapDN apDn = ( LdapDN ) subentryDn.clone();
                apDn.remove( apDn.size() - 1 );
                Subentry subentry = subentryCache.getSubentry( subentryDnStr );
                SubtreeSpecification ss = subentry.getSubtreeSpecification();

                if ( evaluator.evaluate( ss, apDn, name, entry ) )
                {
                    Attribute operational;
                    
                    if ( subentry.isAccessControlSubentry() )
                    {
                        operational = entry.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );
                        
                        if ( operational == null )
                        {
                            operational = new AttributeImpl( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );
                            entry.put( operational );
                        }
                        
                        operational.add( subentryDn.toString() );
                    }
                    
                    if ( subentry.isSchemaSubentry() )
                    {
                        operational = entry.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
                        
                        if ( operational == null )
                        {
                            operational = new AttributeImpl( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
                            entry.put( operational );
                        }
                        
                        operational.add( subentryDn.toString() );
                    }
                    
                    if ( subentry.isCollectiveSubentry() )
                    {
                        operational = entry.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
                        
                        if ( operational == null )
                        {
                            operational = new AttributeImpl( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
                            entry.put( operational );
                        }
                        
                        operational.add( subentryDn.toString() );
                    }
                    
                    if ( subentry.isTriggerSubentry() )
                    {
                        operational = entry.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
                        
                        if ( operational == null )
                        {
                            operational = new AttributeImpl( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
                            entry.put( operational );
                        }
                        
                        operational.add( subentryDn.toString() );
                    }
                }
            }

            next.add( opContext );
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry deletion
    // -----------------------------------------------------------------------

    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws NamingException
    {
    	LdapDN name = opContext.getDn();
        Attributes entry = nexus.lookup( new LookupOperationContext( name ) );
        Attribute objectClasses = AttributeUtils.getAttribute( entry, objectClassType );

        if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC ) )
        {
            SubtreeSpecification ss = subentryCache.removeSubentry( name.toNormName() ).getSubtreeSpecification();
            next.delete( opContext );

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

            ExprNode filter = new PresenceNode( oidRegistry.getOid( SchemaConstants.OBJECT_CLASS_AT ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );

            NamingEnumeration<SearchResult> subentries = 
                nexus.search( 
                    new SearchOperationContext( baseDn, directoryService.getEnvironment(), filter, controls ) );
            
            while ( subentries.hasMore() )
            {
                SearchResult result = subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                {
                    nexus.modify( new ModifyOperationContext( dn, getOperationalModsForRemove( name, candidate ) ) );
                }
            }
        }
        else
        {
            next.delete( opContext );
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
        NamingEnumeration<SearchResult> aps = 
            nexus.search( 
                new SearchOperationContext( name, directoryService.getEnvironment(), filter, controls ) );

        if ( aps.hasMore() )
        {
            aps.close();
            return true;
        }

        return false;
    }


    private List<ModificationItemImpl> getModsOnEntryRdnChange( Name oldName, Name newName, Attributes entry )
        throws NamingException
    {
        List<ModificationItemImpl> modList = new ArrayList<ModificationItemImpl>();

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
        Iterator<String> subentries = subentryCache.nameIterator();
        
        while ( subentries.hasNext() )
        {
            String subentryDn = subentries.next();
            Name apDn = new LdapDN( subentryDn );
            apDn.remove( apDn.size() - 1 );
            SubtreeSpecification ss = subentryCache.getSubentry( subentryDn ).getSubtreeSpecification();
            boolean isOldNameSelected = evaluator.evaluate( ss, apDn, oldName, entry );
            boolean isNewNameSelected = evaluator.evaluate( ss, apDn, newName, entry );

            if ( isOldNameSelected == isNewNameSelected )
            {
                continue;
            }

            // need to remove references to the subentry
            if ( isOldNameSelected && !isNewNameSelected )
            {
                for ( String aSUBENTRY_OPATTRS : SUBENTRY_OPATTRS )
                {
                    int op = DirContext.REPLACE_ATTRIBUTE;
                    Attribute opAttr = entry.get( aSUBENTRY_OPATTRS );
                    
                    if ( opAttr != null )
                    {
                        opAttr = ( Attribute ) opAttr.clone();
                        opAttr.remove( subentryDn );

                        if ( opAttr.size() < 1 )
                        {
                            op = DirContext.REMOVE_ATTRIBUTE;
                        }

                        modList.add( new ModificationItemImpl( op, opAttr ) );
                    }
                }
            }
            // need to add references to the subentry
            else if ( isNewNameSelected && !isOldNameSelected )
            {
                for ( String aSUBENTRY_OPATTRS : SUBENTRY_OPATTRS )
                {
                    int op = DirContext.ADD_ATTRIBUTE;
                    Attribute opAttr = new AttributeImpl( aSUBENTRY_OPATTRS );
                    opAttr.add( subentryDn );
                    modList.add( new ModificationItemImpl( op, opAttr ) );
                }
            }
        }

        return modList;
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws NamingException
    {
        LdapDN name = opContext.getDn();
        String newRdn = opContext.getNewRdn();
        
        Attributes entry = nexus.lookup( new LookupOperationContext( name ) );
        Attribute objectClasses = AttributeUtils.getAttribute( entry, objectClassType );

        if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC ) )
        {
            Subentry subentry = subentryCache.getSubentry( name.toNormName() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            LdapDN apName = ( LdapDN ) name.clone();
            apName.remove( apName.size() - 1 );
            LdapDN baseDn = ( LdapDN ) apName.clone();
            baseDn.addAll( ss.getBase() );
            LdapDN newName = ( LdapDN ) name.clone();
            newName.remove( newName.size() - 1 );

            LdapDN rdn = new LdapDN( newRdn );
            newName.addAll( rdn );
            rdn.normalize( attrRegistry.getNormalizerMapping() );
            newName.normalize( attrRegistry.getNormalizerMapping() );

            String newNormName = newName.toNormName();
            subentryCache.setSubentry( newNormName, ss, subentry.getTypes() );
            next.rename( opContext );

            subentry = subentryCache.getSubentry( newNormName );
            ExprNode filter = new PresenceNode( oidRegistry.getOid( SchemaConstants.OBJECT_CLASS_AT ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );
            NamingEnumeration<SearchResult> subentries = 
                nexus.search( 
                    new SearchOperationContext( baseDn, directoryService.getEnvironment(), filter, controls ) );
            
            while ( subentries.hasMore() )
            {
                SearchResult result = subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                {
                    nexus.modify( new ModifyOperationContext( dn, getOperationalModsForReplace( name, newName, subentry, candidate ) ) );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( name ) )
            {
                String msg = "Will not allow rename operation on entries with administrative descendants.";
                LOG.warn( msg );
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
            }
            
            next.rename( opContext );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            LdapDN newName = ( LdapDN ) name.clone();
            newName.remove( newName.size() - 1 );
            newName.add( newRdn );
            newName.normalize( attrRegistry.getNormalizerMapping() );
            List<ModificationItemImpl> mods = getModsOnEntryRdnChange( name, newName, entry );

            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( newName, mods ) );
            }
        }
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext )
        throws NamingException
    {
        LdapDN oriChildName = opContext.getDn();
        LdapDN parent = opContext.getParent();
        String newRn = opContext.getNewRdn();
        
        
        Attributes entry = nexus.lookup( new LookupOperationContext( oriChildName ) );
        Attribute objectClasses = AttributeUtils.getAttribute( entry, objectClassType );

        if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC ) )
        {
            Subentry subentry = subentryCache.getSubentry( oriChildName.toNormName() );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();
            LdapDN apName = ( LdapDN ) oriChildName.clone();
            apName.remove( apName.size() - 1 );
            LdapDN baseDn = ( LdapDN ) apName.clone();
            baseDn.addAll( ss.getBase() );
            LdapDN newName = ( LdapDN ) parent.clone();
            newName.remove( newName.size() - 1 );

            LdapDN rdn = new LdapDN( newRn );
            newName.addAll( rdn );
            rdn.normalize( attrRegistry.getNormalizerMapping() );
            newName.normalize( attrRegistry.getNormalizerMapping() );
            
            String newNormName = newName.toNormName();
            subentryCache.setSubentry( newNormName, ss, subentry.getTypes() );
            next.moveAndRename( opContext );

            subentry = subentryCache.getSubentry( newNormName );

            ExprNode filter = new PresenceNode( oidRegistry.getOid( SchemaConstants.OBJECT_CLASS_AT ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[] { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );
            NamingEnumeration<SearchResult> subentries = 
                nexus.search( 
                    new SearchOperationContext( baseDn, directoryService.getEnvironment(), filter, controls ) );
            
            while ( subentries.hasMore() )
            {
                SearchResult result = subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                {
                    nexus.modify( new ModifyOperationContext( dn, getOperationalModsForReplace( oriChildName, newName, subentry,
                        candidate ) ) );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( oriChildName ) )
            {
                String msg = "Will not allow rename operation on entries with administrative descendants.";
                LOG.warn( msg );
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
            }
            
            next.moveAndRename( opContext );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            LdapDN newName = ( LdapDN ) parent.clone();
            newName.add( newRn );
            newName.normalize( attrRegistry.getNormalizerMapping() );
            List<ModificationItemImpl> mods = getModsOnEntryRdnChange( oriChildName, newName, entry );

            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( newName, mods ) );
            }
        }
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws NamingException
    {
        LdapDN oriChildName = opContext.getDn();
        LdapDN newParentName = opContext.getParent();
        
        Attributes entry = nexus.lookup( new LookupOperationContext( oriChildName ) );
        Attribute objectClasses = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC ) )
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
            next.move( opContext );

            subentry = subentryCache.getSubentry( newNormName );

            ExprNode filter = new PresenceNode( SchemaConstants.OBJECT_CLASS_AT );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );
            NamingEnumeration<SearchResult> subentries = 
                nexus.search( 
                    new SearchOperationContext( baseDn, directoryService.getEnvironment(), filter, controls ) );
            
            while ( subentries.hasMore() )
            {
                SearchResult result = subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ss, apName, dn, candidate ) )
                {
                    nexus.modify( new ModifyOperationContext( dn, getOperationalModsForReplace( oriChildName, newName, subentry,
                        candidate ) ) );
                }
            }
        }
        else
        {
            if ( hasAdministrativeDescendant( oriChildName ) )
            {
                String msg = "Will not allow rename operation on entries with administrative descendants.";
                LOG.warn( msg );
                throw new LdapSchemaViolationException( msg, ResultCodeEnum.NOT_ALLOWED_ON_RDN );
            }
            
            next.move( opContext );

            // calculate the new DN now for use below to modify subentry operational
            // attributes contained within this regular entry with name changes
            LdapDN newName = ( LdapDN ) newParentName.clone();
            newName.add( oriChildName.get( oriChildName.size() - 1 ) );
            List<ModificationItemImpl> mods = getModsOnEntryRdnChange( oriChildName, newName, entry );

            if ( mods.size() > 0 )
            {
                nexus.modify( new ModifyOperationContext( newName, mods ) );
            }
        }
    }


    // -----------------------------------------------------------------------
    // Methods dealing subentry modification
    // -----------------------------------------------------------------------

    
    private int getSubentryTypes( Attributes entry, List<ModificationItemImpl> mods ) throws NamingException
    {
        Attribute ocFinalState = ( Attribute ) entry.get( SchemaConstants.OBJECT_CLASS_AT ).clone();
        for ( ModificationItemImpl mod : mods )
        {
            if ( mod.getAttribute().getID().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT ) )
            {
                switch ( mod.getModificationOp() )
                {
                    case ( DirContext.ADD_ATTRIBUTE ):
                        for ( int jj = 0; jj < mod.getAttribute().size(); jj++ )
                        {
                            ocFinalState.add( mod.getAttribute().get( jj ) );
                        }
                    
                        break;
                    case ( DirContext.REMOVE_ATTRIBUTE ):
                        for ( int jj = 0; jj < mod.getAttribute().size(); jj++ )
                        {
                            ocFinalState.remove( mod.getAttribute().get( jj ) );
                        }
                    
                        break;
                    case ( DirContext.REPLACE_ATTRIBUTE ):
                        ocFinalState = mod.getAttribute();
                        break;
                }
            }
        }
        
        Attributes attrs = new AttributesImpl();
        attrs.put( ocFinalState );
        return getSubentryTypes( attrs );
    }

    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws NamingException
    {
        LdapDN name = opContext.getDn();
        List<ModificationItemImpl> mods = opContext.getModItems();
        
        Attributes entry = nexus.lookup( new LookupOperationContext( name ) );
        Attributes oldEntry = (Attributes) entry.clone();
        Attribute objectClasses = AttributeUtils.getAttribute( entry, objectClassType );
        boolean isSubtreeSpecificationModification = false;
        ModificationItem subtreeMod = null;

        for ( ModificationItem mod : mods )
        {
            if ( SchemaConstants.SUBTREE_SPECIFICATION_AT.equalsIgnoreCase( mod.getAttribute().getID() ) )
            {
                isSubtreeSpecificationModification = true;
                subtreeMod = mod;
            }
        }

        if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC ) && isSubtreeSpecificationModification )
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
                LOG.error( msg, e );
                throw new LdapInvalidAttributeValueException( msg, ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
            }

            subentryCache.setSubentry( name.toNormName(), ssNew, getSubentryTypes( entry, mods ) );
            next.modify( opContext );

            // search for all entries selected by the old SS and remove references to subentry
            LdapDN apName = ( LdapDN ) name.clone();
            apName.remove( apName.size() - 1 );
            LdapDN oldBaseDn = ( LdapDN ) apName.clone();
            oldBaseDn.addAll( ssOld.getBase() );
            ExprNode filter = new PresenceNode( oidRegistry.getOid( SchemaConstants.OBJECT_CLASS_AT ) );
            SearchControls controls = new SearchControls();
            controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            controls.setReturningAttributes( new String[]
                { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES } );
            NamingEnumeration<SearchResult> subentries = 
                nexus.search( 
                    new SearchOperationContext( oldBaseDn, directoryService.getEnvironment(), filter, controls ) );
            
            while ( subentries.hasMore() )
            {
                SearchResult result = subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ssOld, apName, dn, candidate ) )
                {
                    nexus.modify( new ModifyOperationContext( dn, getOperationalModsForRemove( name, candidate ) ) );
                }
            }

            // search for all selected entries by the new SS and add references to subentry
            Subentry subentry = subentryCache.getSubentry( name.toNormName() );
            Attributes operational = getSubentryOperatationalAttributes( name, subentry );
            LdapDN newBaseDn = ( LdapDN ) apName.clone();
            newBaseDn.addAll( ssNew.getBase() );
            subentries = nexus.search( 
                new SearchOperationContext( newBaseDn, directoryService.getEnvironment(), filter, controls ) );
            while ( subentries.hasMore() )
            {
                SearchResult result = ( SearchResult ) subentries.next();
                Attributes candidate = result.getAttributes();
                LdapDN dn = new LdapDN( result.getName() );
                dn.normalize( attrRegistry.getNormalizerMapping() );

                if ( evaluator.evaluate( ssNew, apName, dn, candidate ) )
                {
                    nexus.modify( new ModifyOperationContext( dn, getOperationalModsForAdd( candidate, operational ) )) ;
                }
            }
        }
        else
        {
            next.modify( opContext );
            
            if ( !AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC ) )
            {
	            Attributes newEntry = nexus.lookup( new LookupOperationContext( name ) );
	            
	            List<ModificationItemImpl> subentriesOpAttrMods = getModsOnEntryModification( name, oldEntry, newEntry );
                
	            if ( subentriesOpAttrMods.size() > 0)
	            {
	            	nexus.modify( new ModifyOperationContext( name, subentriesOpAttrMods ) );
	            }
            }
        }
    }


    // -----------------------------------------------------------------------
    // Utility Methods
    // -----------------------------------------------------------------------

    private List<ModificationItemImpl> getOperationalModsForReplace( Name oldName, Name newName, Subentry subentry,
        Attributes entry )
    {
        List<ModificationItemImpl> modList = new ArrayList<ModificationItemImpl>();
        
        Attribute operational;

        if ( subentry.isAccessControlSubentry() )
        {
            operational = ( Attribute ) entry.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT ).clone();
            
            if ( operational == null )
            {
                operational = new AttributeImpl( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }
            
            modList.add( new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, operational ) );
        }
        
        if ( subentry.isSchemaSubentry() )
        {
            operational = ( Attribute ) entry.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).clone();
            
            if ( operational == null )
            {
                operational = new AttributeImpl( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }
            
            modList.add( new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, operational ) );
        }
        
        if ( subentry.isCollectiveSubentry() )
        {
            operational = ( Attribute ) entry.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ).clone();
           
            if ( operational == null )
            {
                operational = new AttributeImpl( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }
            
            modList.add( new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, operational ) );
        }
        
        if ( subentry.isTriggerSubentry() )
        {
            operational = ( Attribute ) entry.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ).clone();
            
            if ( operational == null )
            {
                operational = new AttributeImpl( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
                operational.add( newName.toString() );
            }
            else
            {
                operational.remove( oldName.toString() );
                operational.add( newName.toString() );
            }
            
            modList.add( new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, operational ) );
        } 

        return modList;
    }


    /**
     * Gets the subschema operational attributes to be added to or removed from
     * an entry selected by a subentry's subtreeSpecification.
     *
     * @param name the normalized distinguished name of the subentry (the value of op attrs)
     * @param subentry the subentry to get attributes from
     * @return the set of attributes to be added or removed from entries
     */
    private Attributes getSubentryOperatationalAttributes( Name name, Subentry subentry )
    {
        Attributes operational = new AttributesImpl();
        
        if ( subentry.isAccessControlSubentry() )
        {
            if ( operational.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT ) == null )
            {
                operational.put( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT, name.toString() );
            }
            else
            {
                operational.get( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT ).add( name.toString() );
            }
        }
        if ( subentry.isSchemaSubentry() )
        {
            if ( operational.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ) == null )
            {
                operational.put( SchemaConstants.SUBSCHEMA_SUBENTRY_AT, name.toString() );
            }
            else
            {
                operational.get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).add( name.toString() );
            }
        }
        if ( subentry.isCollectiveSubentry() )
        {
            if ( operational.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ) == null )
            {
                operational.put( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT, name.toString() );
            }
            else
            {
                operational.get( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT ).add( name.toString() );
            }
        }
        if ( subentry.isTriggerSubentry() )
        {
            if ( operational.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ) == null )
            {
                operational.put( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT, name.toString() );
            }
            else
            {
                operational.get( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT ).add( name.toString() );
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
    private List<ModificationItemImpl> getOperationalModsForRemove( LdapDN subentryDn, Attributes candidate )
    {
        List<ModificationItemImpl> modList = new ArrayList<ModificationItemImpl>();
        String dn = subentryDn.toNormName();

        for ( String opAttrId : SUBENTRY_OPATTRS )
        {
            Attribute opAttr = candidate.get( opAttrId );

            if ( opAttr != null && opAttr.contains( dn ) )
            {
                Attribute attr = new AttributeImpl( opAttrId );
                attr.add( dn );
                modList.add( new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE, attr ) );
            }
        }

        return modList;
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
     * @throws NamingException if there are probelms accessing modification items
     */
    public List<ModificationItemImpl> getOperationalModsForAdd( Attributes entry, Attributes operational )
        throws NamingException
    {
        List<ModificationItemImpl> modList = new ArrayList<ModificationItemImpl>();

        NamingEnumeration<String> opAttrIds = operational.getIDs();
        
        while ( opAttrIds.hasMore() )
        {
            int op = DirContext.REPLACE_ATTRIBUTE;
            String opAttrId = opAttrIds.next();
            Attribute result = new AttributeImpl( opAttrId );
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

            modList.add( new ModificationItemImpl( op, result ) );
        }

        return modList;
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
            Attribute objectClasses = result.getAttributes().get( SchemaConstants.OBJECT_CLASS_AT );
            if ( objectClasses != null )
            {
                if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC ) )
                {
                    return false;
                }

                if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC_OID ) )
                {
                    return false;
                }

                for ( int ii = 0; ii < objectClasses.size(); ii++ )
                {
                    String oc = ( String ) objectClasses.get( ii );
                    if ( oc.equalsIgnoreCase( SchemaConstants.SUBENTRY_OC ) )
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
            Attribute objectClasses = result.getAttributes().get( SchemaConstants.OBJECT_CLASS_AT );
            if ( objectClasses != null )
            {
                if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC ) )
                {
                    return true;
                }

                if ( AttributeUtils.containsValueCaseIgnore( objectClasses, SchemaConstants.SUBENTRY_OC_OID ) )
                {
                    return true;
                }

                for ( int ii = 0; ii < objectClasses.size(); ii++ )
                {
                    String oc = ( String ) objectClasses.get( ii );
                    if ( oc.equalsIgnoreCase( SchemaConstants.SUBENTRY_OC ) )
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
    
    
    private List<ModificationItemImpl> getModsOnEntryModification( LdapDN name, Attributes oldEntry, Attributes newEntry )
    throws NamingException
	{
	    List<ModificationItemImpl> modList = new ArrayList<ModificationItemImpl>();
	
	    Iterator<String> subentries = subentryCache.nameIterator();
	    
	    while ( subentries.hasNext() )
	    {
	        String subentryDn = subentries.next();
	        Name apDn = new LdapDN( subentryDn );
	        apDn.remove( apDn.size() - 1 );
	        SubtreeSpecification ss = subentryCache.getSubentry( subentryDn ).getSubtreeSpecification();
	        boolean isOldEntrySelected = evaluator.evaluate( ss, apDn, name, oldEntry );
	        boolean isNewEntrySelected = evaluator.evaluate( ss, apDn, name, newEntry );
	
	        if ( isOldEntrySelected == isNewEntrySelected )
	        {
	            continue;
	        }
	
	        // need to remove references to the subentry
	        if ( isOldEntrySelected && !isNewEntrySelected )
	        {
                for ( String aSUBENTRY_OPATTRS : SUBENTRY_OPATTRS )
                {
                    int op = DirContext.REPLACE_ATTRIBUTE;
                    Attribute opAttr = oldEntry.get( aSUBENTRY_OPATTRS );
                    if ( opAttr != null )
                    {
                        opAttr = ( Attribute ) opAttr.clone();
                        opAttr.remove( subentryDn );

                        if ( opAttr.size() < 1 )
                        {
                            op = DirContext.REMOVE_ATTRIBUTE;
                        }

                        modList.add( new ModificationItemImpl( op, opAttr ) );
                    }
                }
	        }
	        // need to add references to the subentry
	        else if ( isNewEntrySelected && !isOldEntrySelected )
	        {
                for ( String aSUBENTRY_OPATTRS : SUBENTRY_OPATTRS )
                {
                    int op = DirContext.ADD_ATTRIBUTE;
                    Attribute opAttr = new AttributeImpl( aSUBENTRY_OPATTRS );
                    opAttr.add( subentryDn );
                    modList.add( new ModificationItemImpl( op, opAttr ) );
                }
	        }
	    }
	
	    return modList;
	}

}
