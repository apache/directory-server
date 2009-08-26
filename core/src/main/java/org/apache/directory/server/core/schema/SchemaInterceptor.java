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
package org.apache.directory.server.core.schema;


import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.DefaultServerAttribute;
import org.apache.directory.server.core.entry.ServerAttribute;
import org.apache.directory.server.core.entry.ServerBinaryValue;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerStringValue;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
import org.apache.directory.server.core.filtering.EntryFilter;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchingOperationContext;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.cursor.EmptyCursor;
import org.apache.directory.shared.ldap.cursor.SingletonCursor;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientBinaryValue;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.exception.LdapAttributeInUseException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeIdentifierException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.filter.ApproximateNode;
import org.apache.directory.shared.ldap.filter.AssertionNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.filter.LessEqNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.control.CascadeControl;
import org.apache.directory.shared.ldap.name.AttributeTypeAndValue;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.AttributeTypeOptions;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.registries.*;
import org.apache.directory.shared.ldap.schema.syntaxChecker.AcceptAllSyntaxChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link org.apache.directory.server.core.interceptor.Interceptor} that manages and enforces schemas.
 *
 * @todo Better interceptor description required.

 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaInterceptor extends BaseInterceptor
{
    /** The LoggerFactory used by this Interceptor */
    private static Logger LOG = LoggerFactory.getLogger( SchemaInterceptor.class );

    private static final String[] SCHEMA_SUBENTRY_RETURN_ATTRIBUTES = new String[]
        { SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES, SchemaConstants.ALL_USER_ATTRIBUTES };

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /**
     * the root nexus to all database partitions
     */
    private PartitionNexus nexus;

    /**
     * a binary attribute tranforming filter: String -> byte[]
     */
    private BinaryAttributeFilter binaryAttributeFilter;

    private TopFilter topFilter;

    private List<EntryFilter> filters = new ArrayList<EntryFilter>();

    /**
     * the global schema object registries
     */
    private Registries registries;

    /** A global reference to the ObjectClass attributeType */
    private AttributeType OBJECT_CLASS;
    /**
     * the global attributeType registry
     */
    private AttributeTypeRegistry atRegistry;

    /** A normalized form for the SubschemaSubentry DN */
    private String subschemaSubentryDnNorm;

    /** The SubschemaSubentry DN */
    private LdapDN subschemaSubentryDn;

    /**
     * the normalized name for the schema modification attributes
     */
    private LdapDN schemaModificationAttributesDN;

    private SchemaOperationControl schemaManager;

    private SchemaService schemaService;

    // the base DN (normalized) of the schema partition
    private LdapDN schemaBaseDN;

    /** A map used to store all the objectClasses superiors */
    private Map<String, List<ObjectClass>> superiors;

    /** A map used to store all the objectClasses may attributes */
    private Map<String, List<AttributeType>> allMay;

    /** A map used to store all the objectClasses must */
    private Map<String, List<AttributeType>> allMust;

    /** A map used to store all the objectClasses allowed attributes (may + must) */
    private Map<String, List<AttributeType>> allowed;


    /**
     * Initialize the Schema Service
     *
     * @param directoryService the directory service core
     * @throws Exception if there are problems during initialization
     */
    public void init( DirectoryService directoryService ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Initializing SchemaInterceptor..." );
        }

        nexus = directoryService.getPartitionNexus();
        registries = directoryService.getRegistries();
        atRegistry = registries.getAttributeTypeRegistry();
        OBJECT_CLASS = atRegistry.lookup( SchemaConstants.OBJECT_CLASS_AT );
        binaryAttributeFilter = new BinaryAttributeFilter();
        topFilter = new TopFilter();
        filters.add( binaryAttributeFilter );
        filters.add( topFilter );

        schemaBaseDN = new LdapDN( ServerDNConstants.OU_SCHEMA_DN );
        schemaBaseDN.normalize( atRegistry.getNormalizerMapping() );
        schemaService = directoryService.getSchemaService();
        schemaManager = directoryService.getSchemaService().getSchemaControl();

        // stuff for dealing with subentries (garbage for now)
        Value<?> subschemaSubentry = nexus.getRootDSE( null ).get( SchemaConstants.SUBSCHEMA_SUBENTRY_AT ).get();
        subschemaSubentryDn = new LdapDN( subschemaSubentry.getString() );
        subschemaSubentryDn.normalize( atRegistry.getNormalizerMapping() );
        subschemaSubentryDnNorm = subschemaSubentryDn.getNormName();

        schemaModificationAttributesDN = new LdapDN( ServerDNConstants.SCHEMA_TIMESTAMP_ENTRY_DN );
        schemaModificationAttributesDN.normalize( atRegistry.getNormalizerMapping() );

        computeSuperiors();

        if ( IS_DEBUG )
        {
            LOG.debug( "SchemaInterceptor Initialized !" );
        }
    }


    /**
     * Compute the MUST attributes for an objectClass. This method gather all the
     * MUST from all the objectClass and its superors.
     *
     * @param atSeen ???
     * @param objectClass the object class to gather MUST attributes for
     * @throws Exception if there are problems resolving schema entitites
     */
    private void computeMustAttributes( ObjectClass objectClass, Set<String> atSeen ) throws Exception
    {
        List<ObjectClass> parents = superiors.get( objectClass.getOid() );

        List<AttributeType> mustList = new ArrayList<AttributeType>();
        List<AttributeType> allowedList = new ArrayList<AttributeType>();
        Set<String> mustSeen = new HashSet<String>();

        allMust.put( objectClass.getOid(), mustList );
        allowed.put( objectClass.getOid(), allowedList );

        for ( ObjectClass parent : parents )
        {
            AttributeType[] mustParent = parent.getMustList();

            if ( ( mustParent != null ) && ( mustParent.length != 0 ) )
            {
                for ( AttributeType attributeType : mustParent )
                {
                    String oid = attributeType.getOid();

                    if ( !mustSeen.contains( oid ) )
                    {
                        mustSeen.add( oid );
                        mustList.add( attributeType );
                        allowedList.add( attributeType );
                        atSeen.add( attributeType.getOid() );
                    }
                }
            }
        }
    }


    /**
     * Compute the MAY attributes for an objectClass. This method gather all the
     * MAY from all the objectClass and its superors.
     *
     * The allowed attributes is also computed, it's the union of MUST and MAY
     *
     * @param atSeen ???
     * @param objectClass the object class to get all the MAY attributes for
     * @throws Exception with problems accessing registries
     */
    private void computeMayAttributes( ObjectClass objectClass, Set<String> atSeen ) throws Exception
    {
        List<ObjectClass> parents = superiors.get( objectClass.getOid() );

        List<AttributeType> mayList = new ArrayList<AttributeType>();
        Set<String> maySeen = new HashSet<String>();
        List<AttributeType> allowedList = allowed.get( objectClass.getOid() );

        allMay.put( objectClass.getOid(), mayList );

        for ( ObjectClass parent : parents )
        {
            AttributeType[] mustParent = parent.getMustList();

            if ( ( mustParent != null ) && ( mustParent.length != 0 ) )
            {
                for ( AttributeType attributeType : mustParent )
                {
                    String oid = attributeType.getOid();

                    if ( !maySeen.contains( oid ) )
                    {
                        maySeen.add( oid );
                        mayList.add( attributeType );

                        if ( !atSeen.contains( oid ) )
                        {
                            allowedList.add( attributeType );
                        }
                    }
                }
            }
        }
    }


    /**
     * Recursively compute all the superiors of an object class. For instance, considering
     * 'inetOrgPerson', it's direct superior is 'organizationalPerson', which direct superior
     * is 'Person', which direct superior is 'top'.
     *
     * As a result, we will gather all of these three ObjectClasses in 'inetOrgPerson' ObjectClasse
     * superiors.
     */
    private void computeOCSuperiors( ObjectClass objectClass, List<ObjectClass> superiors, Set<String> ocSeen )
        throws Exception
    {
        ObjectClass[] parents = objectClass.getSuperClasses();

        // Loop on all the objectClass superiors
        if ( ( parents != null ) && ( parents.length != 0 ) )
        {
            for ( ObjectClass parent : parents )
            {
                // Top is not added
                if ( SchemaConstants.TOP_OC.equals( parent.getName() ) )
                {
                    continue;
                }

                // For each one, recurse
                computeOCSuperiors( parent, superiors, ocSeen );

                String oid = parent.getOid();

                if ( !ocSeen.contains( oid ) )
                {
                    superiors.add( parent );
                    ocSeen.add( oid );
                }
            }
        }
    }


    /**
     * Compute the superiors and MUST/MAY attributes for a specific
     * ObjectClass
     */
    private void computeSuperior( ObjectClass objectClass ) throws Exception
    {
        List<ObjectClass> ocSuperiors = new ArrayList<ObjectClass>();

        superiors.put( objectClass.getOid(), ocSuperiors );

        computeOCSuperiors( objectClass, ocSuperiors, new HashSet<String>() );

        Set<String> atSeen = new HashSet<String>();
        computeMustAttributes( objectClass, atSeen );
        computeMayAttributes( objectClass, atSeen );

        superiors.put( objectClass.getName(), ocSuperiors );
    }
    

    /**
     * Compute all ObjectClasses superiors, MAY and MUST attributes.
     * @throws Exception
     */
    private void computeSuperiors() throws Exception
    {
        Iterator<ObjectClass> objectClasses = registries.getObjectClassRegistry().iterator();
        superiors = new ConcurrentHashMap<String, List<ObjectClass>>();
        allMust = new ConcurrentHashMap<String, List<AttributeType>>();
        allMay = new ConcurrentHashMap<String, List<AttributeType>>();
        allowed = new ConcurrentHashMap<String, List<AttributeType>>();

        while ( objectClasses.hasNext() )
        {
            ObjectClass objectClass = objectClasses.next();
            computeSuperior( objectClass );
        }
    }


    public EntryFilteringCursor list( NextInterceptor nextInterceptor, ListOperationContext opContext )
        throws Exception
    {
        EntryFilteringCursor cursor = nextInterceptor.list( opContext );
        cursor.addEntryFilter( binaryAttributeFilter );
        return cursor;
    }


    /**
     * Remove all unknown attributes from the searchControls, to avoid an exception.
     *
     * RFC 2251 states that :
     * " Attributes MUST be named at most once in the list, and are returned "
     * " at most once in an entry. "
     * " If there are attribute descriptions in "
     * " the list which are not recognized, they are ignored by the server."
     *
     * @param searchCtls The SearchControls we will filter
     */
    private void filterAttributesToReturn( SearchControls searchCtls )
    {
        String[] attributes = searchCtls.getReturningAttributes();

        if ( ( attributes == null ) || ( attributes.length == 0 ) )
        {
            // We have no attributes, that means "*" (all users attributes)
            searchCtls.setReturningAttributes( SchemaConstants.ALL_USER_ATTRIBUTES_ARRAY );
            return;
        }

        Map<String, String> filteredAttrs = new HashMap<String, String>();
        boolean hasNoAttribute = false;
        boolean hasAttributes = false;

        for ( String attribute : attributes )
        {
            // Skip special attributes
            if ( ( SchemaConstants.ALL_USER_ATTRIBUTES.equals( attribute ) )
                || ( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES.equals( attribute ) )
                || ( SchemaConstants.NO_ATTRIBUTE.equals( attribute ) ) )
            {
                if ( !filteredAttrs.containsKey( attribute ) )
                {
                    filteredAttrs.put( attribute, attribute );
                }

                if ( SchemaConstants.NO_ATTRIBUTE.equals( attribute ) )
                {
                    hasNoAttribute = true;
                }
                else
                {
                    hasAttributes = true;
                }

                continue;
            }

            try
            {
                // Check that the attribute is declared
                if ( registries.getAttributeTypeRegistry().contains( attribute ) )
                {
                    String oid = registries.getOidRegistry().getOid( attribute );

                    // The attribute must be an AttributeType
                    if ( atRegistry.hasAttributeType( oid ) )
                    {
                        if ( !filteredAttrs.containsKey( oid ) )
                        {
                            // Ok, we can add the attribute to the list of filtered attributes
                            filteredAttrs.put( oid, attribute );
                        }
                    }
                }

                hasAttributes = true;
            }
            catch ( Exception ne )
            {
                /* Do nothing, the attribute does not exist */
            }
        }

        // Treat a special case : if we have an attribute and "1.1", then discard "1.1"
        if ( hasAttributes && hasNoAttribute )
        {
            filteredAttrs.remove( SchemaConstants.NO_ATTRIBUTE );
        }

        // If we still have the same attribute number, then we can just get out the method
        if ( filteredAttrs.size() == attributes.length )
        {
            return;
        }

        // Deal with the special case where the attribute list is now empty
        if ( filteredAttrs.size() == 0 )
        {
            // We just have to pass the special 1.1 attribute,
            // as we don't want to return any attribute
            searchCtls.setReturningAttributes( SchemaConstants.NO_ATTRIBUTE_ARRAY );
            return;
        }

        // Some attributes have been removed. let's modify the searchControl
        String[] newAttributesList = new String[filteredAttrs.size()];

        int pos = 0;

        for ( String key : filteredAttrs.keySet() )
        {
            newAttributesList[pos++] = filteredAttrs.get( key );
        }

        searchCtls.setReturningAttributes( newAttributesList );
    }


    private Value<?> convert( String id, Object value ) throws Exception
    {
        AttributeType at = atRegistry.lookup( id );

        if ( at.getSyntax().isHumanReadable() )
        {
            if ( value instanceof byte[] )
            {
                try
                {
                    return new ClientStringValue( new String( ( byte[] ) value, "UTF-8" ) );
                }
                catch ( UnsupportedEncodingException uee )
                {
                    String message = "The value stored in an Human Readable attribute as a byte[] should be convertible to a String";
                    LOG.error( message );
                    throw new NamingException( message );
                }
            }
        }
        else
        {
            if ( value instanceof String )
            {
                try
                {
                    return new ClientBinaryValue( ( ( String ) value ).getBytes( "UTF-8" ) );
                }
                catch ( UnsupportedEncodingException uee )
                {
                    String message = "The value stored in a non Human Readable attribute as a String should be convertible to a byte[]";
                    LOG.error( message );
                    throw new NamingException( message );
                }
            }
        }

        return null;
    }


    /**
     * Check that the filter values are compatible with the AttributeType. Typically,
     * a HumanReadible filter should have a String value. The substring filter should
     * not be used with binary attributes.
     */
    private void checkFilter( ExprNode filter ) throws Exception
    {
        if ( filter == null )
        {
            String message = "A filter should not be null";
            LOG.error( message );
            throw new NamingException( message );
        }

        if ( filter.isLeaf() )
        {
            if ( filter instanceof EqualityNode )
            {
                EqualityNode node = ( ( EqualityNode ) filter );
                Object value = node.getValue();

                Value<?> newValue = convert( node.getAttribute(), value );

                if ( newValue != null )
                {
                    node.setValue( newValue );
                }
            }
            else if ( filter instanceof SubstringNode )
            {
                SubstringNode node = ( ( SubstringNode ) filter );

                if ( !atRegistry.lookup( node.getAttribute() ).getSyntax().isHumanReadable() )
                {
                    String message = "A Substring filter should be used only on Human Readable attributes";
                    LOG.error( message );
                    throw new NamingException( message );
                }
            }
            else if ( filter instanceof PresenceNode )
            {
                // Nothing to do
            }
            else if ( filter instanceof GreaterEqNode )
            {
                GreaterEqNode node = ( ( GreaterEqNode ) filter );
                Object value = node.getValue();

                Value<?> newValue = convert( node.getAttribute(), value );

                if ( newValue != null )
                {
                    node.setValue( newValue );
                }

            }
            else if ( filter instanceof LessEqNode )
            {
                LessEqNode node = ( ( LessEqNode ) filter );
                Object value = node.getValue();

                Value<?> newValue = convert( node.getAttribute(), value );

                if ( newValue != null )
                {
                    node.setValue( newValue );
                }
            }
            else if ( filter instanceof ExtensibleNode )
            {
                ExtensibleNode node = ( ( ExtensibleNode ) filter );

                if ( !atRegistry.lookup( node.getAttribute() ).getSyntax().isHumanReadable() )
                {
                    String message = "A Extensible filter should be used only on Human Readable attributes";
                    LOG.error( message );
                    throw new NamingException( message );
                }
            }
            else if ( filter instanceof ApproximateNode )
            {
                ApproximateNode node = ( ( ApproximateNode ) filter );
                Object value = node.getValue();

                Value<?> newValue = convert( node.getAttribute(), value );

                if ( newValue != null )
                {
                    node.setValue( newValue );
                }
            }
            else if ( filter instanceof AssertionNode )
            {
                // Nothing to do
                return;
            }
            else if ( filter instanceof ScopeNode )
            {
                // Nothing to do
                return;
            }
        }
        else
        {
            // Recursively iterate through all the children.
            for ( ExprNode child : ( ( BranchNode ) filter ).getChildren() )
            {
                checkFilter( child );
            }
        }
    }


    public EntryFilteringCursor search( NextInterceptor nextInterceptor, SearchOperationContext opContext )
        throws Exception
    {
        LdapDN base = opContext.getDn();
        SearchControls searchCtls = opContext.getSearchControls();
        ExprNode filter = opContext.getFilter();

        // We have to eliminate bad attributes from the request, accordingly
        // to RFC 2251, chap. 4.5.1. Basically, all unknown attributes are removed
        // from the list
        if ( searchCtls.getReturningAttributes() != null )
        {
            filterAttributesToReturn( searchCtls );
        }

        // We also have to check the H/R flag for the filter attributes
        checkFilter( filter );

        String baseNormForm = ( base.isNormalized() ? base.getNormName() : base.toNormName() );

        // Deal with the normal case : searching for a normal value (not subSchemaSubEntry)
        if ( !subschemaSubentryDnNorm.equals( baseNormForm ) )
        {
            EntryFilteringCursor cursor = nextInterceptor.search( opContext );

            if ( searchCtls.getReturningAttributes() != null )
            {
                cursor.addEntryFilter( topFilter );
                return cursor;
            }

            for ( EntryFilter ef : filters )
            {
                cursor.addEntryFilter( ef );
            }

            return cursor;
        }

        // The user was searching into the subSchemaSubEntry
        // This kind of search _must_ be limited to OBJECT scope (the subSchemaSubEntry
        // does not have any sub level)
        if ( searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE )
        {
            // The filter can be an equality or a presence, but nothing else
            if ( filter instanceof SimpleNode )
            {
                // We should get the value for the filter.
                // only 'top' and 'subSchema' are valid values
                SimpleNode node = ( SimpleNode ) filter;
                String objectClass;

                objectClass = node.getValue().getString();

                String objectClassOid = null;

                if ( registries.getObjectClassRegistry().hasObjectClass( objectClass ) )
                {
                    objectClassOid = registries.getObjectClassRegistry().lookup( objectClass ).getOid();
                }
                else
                {
                    return new BaseEntryFilteringCursor( new EmptyCursor<ServerEntry>(), opContext );
                }

                String nodeOid = registries.getOidRegistry().getOid( node.getAttribute() );

                // see if node attribute is objectClass
                if ( nodeOid.equals( SchemaConstants.OBJECT_CLASS_AT_OID )
                    && ( objectClassOid.equals( SchemaConstants.TOP_OC_OID ) || objectClassOid
                        .equals( SchemaConstants.SUBSCHEMA_OC_OID ) ) && ( node instanceof EqualityNode ) )
                {
                    // call.setBypass( true );
                    ServerEntry serverEntry = schemaService.getSubschemaEntry( searchCtls.getReturningAttributes() );
                    serverEntry.setDn( base );
                    return new BaseEntryFilteringCursor( new SingletonCursor<ServerEntry>( serverEntry ), opContext );
                }
                else
                {
                    return new BaseEntryFilteringCursor( new EmptyCursor<ServerEntry>(), opContext );
                }
            }
            else if ( filter instanceof PresenceNode )
            {
                PresenceNode node = ( PresenceNode ) filter;

                // see if node attribute is objectClass
                if ( node.getAttribute().equals( SchemaConstants.OBJECT_CLASS_AT_OID ) )
                {
                    // call.setBypass( true );
                    ServerEntry serverEntry = schemaService.getSubschemaEntry( searchCtls.getReturningAttributes() );
                    serverEntry.setDn( base );
                    EntryFilteringCursor cursor = new BaseEntryFilteringCursor( new SingletonCursor<ServerEntry>(
                        serverEntry ), opContext );
                    return cursor;
                }
            }
        }

        // In any case not handled previously, just return an empty result
        return new BaseEntryFilteringCursor( new EmptyCursor<ServerEntry>(), opContext );
    }


    /**
     * Search for an entry, using its DN. Binary attributes and ObjectClass attribute are removed.
     */
    public ClonedServerEntry lookup( NextInterceptor nextInterceptor, LookupOperationContext opContext )
        throws Exception
    {
        ClonedServerEntry result = nextInterceptor.lookup( opContext );

        if ( result == null )
        {
            return null;
        }

        filterBinaryAttributes( result );
        filterObjectClass( result );

        return result;
    }


    private void getSuperiors( ObjectClass oc, Set<String> ocSeen, List<ObjectClass> result ) throws Exception
    {
        for ( ObjectClass parent : oc.getSuperClasses() )
        {
            // Skip 'top'
            if ( SchemaConstants.TOP_OC.equals( parent.getName() ) )
            {
                continue;
            }

            if ( !ocSeen.contains( parent.getOid() ) )
            {
                ocSeen.add( parent.getOid() );
                result.add( parent );
            }

            // Recurse on the parent
            getSuperiors( parent, ocSeen, result );
        }
    }


    /**
     * Checks to see if an attribute is required by as determined from an entry's
     * set of objectClass attribute values.
     *
     * @param attrId the attribute to test if required by a set of objectClass values
     * @param objectClass the objectClass values
     * @return true if the objectClass values require the attribute, false otherwise
     * @throws Exception if the attribute is not recognized
     */
    private boolean isRequired( String attrId, EntryAttribute objectClasses ) throws Exception
    {
        OidRegistry oidRegistry = registries.getOidRegistry();
        ObjectClassRegistry registry = registries.getObjectClassRegistry();

        if ( !oidRegistry.hasOid( attrId ) )
        {
            return false;
        }

        String attrOid = oidRegistry.getOid( attrId );

        for ( Value<?> objectClass : objectClasses )
        {
            ObjectClass ocSpec = registry.lookup( objectClass.getString() );

            for ( AttributeType must : ocSpec.getMustList() )
            {
                if ( must.getOid().equals( attrOid ) )
                {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * A helper method which tells if a schema is disabled
     */
    private  boolean isDisabled( String schemaName )
    {
        Schema schema = registries.getLoadedSchemas().get( schemaName );
        
        return ( schema == null ) || ( schema.isDisabled() );
    }
    
    
    /**
     * A helper method which tells if a schema is enabled
     */
    private boolean isEnabled( String schemaName )
    {
        Schema schema = registries.getLoadedSchemas().get( schemaName );
        
        return ( schema != null ) && ( !schema.isDisabled() );
    }
    
    
    /**
     * Checks to see if removing a set of attributes from an entry completely removes
     * that attribute's values.  If change has zero size then all attributes are
     * presumed to be removed.
     *
     * @param change
     * @param entry
     * @return
     * @throws Exception
     */
    private boolean isCompleteRemoval( ServerAttribute change, ServerEntry entry ) throws Exception
    {
        // if change size is 0 then all values are deleted then we're in trouble
        if ( change.size() == 0 )
        {
            return true;
        }

        // can't do math to figure our if all values are removed since some
        // values in the modify request may not be in the entry.  we need to
        // remove the values from a cloned version of the attribute and see
        // if nothing is left.
        ServerAttribute changedEntryAttr = ( ServerAttribute ) entry.get( change.getUpId() ).clone();

        for ( Value<?> value : change )
        {
            changedEntryAttr.remove( value );
        }

        return changedEntryAttr.size() == 0;
    }


    /**
     * 
     * @param modOp
     * @param changes
     * @param existing
     * @return
     * @throws Exception
     */
    private EntryAttribute getResultantObjectClasses( ModificationOperation modOp, EntryAttribute changes,
        EntryAttribute existing ) throws Exception
    {
        if ( ( changes == null ) && ( existing == null ) )
        {
            return new DefaultServerAttribute( SchemaConstants.OBJECT_CLASS_AT, OBJECT_CLASS );
        }

        if ( changes == null )
        {
            return existing;
        }

        if ( ( existing == null ) && ( modOp == ModificationOperation.ADD_ATTRIBUTE ) )
        {
            return changes;
        }
        else if ( existing == null )
        {
            return new DefaultServerAttribute( SchemaConstants.OBJECT_CLASS_AT, OBJECT_CLASS );
        }

        switch ( modOp )
        {
            case ADD_ATTRIBUTE:
                for ( Value<?> value : changes )
                {
                    existing.add( value );
                }

                return existing;

            case REPLACE_ATTRIBUTE:
                return ( ServerAttribute ) changes.clone();

            case REMOVE_ATTRIBUTE:
                for ( Value<?> value : changes )
                {
                    existing.remove( value );
                }

                return existing;

            default:
                throw new InternalError( "" );
        }
    }


    private boolean getObjectClasses( EntryAttribute objectClasses, List<ObjectClass> result ) throws Exception
    {
        Set<String> ocSeen = new HashSet<String>();
        ObjectClassRegistry registry = registries.getObjectClassRegistry();

        // We must select all the ObjectClasses, except 'top',
        // but including all the inherited ObjectClasses
        boolean hasExtensibleObject = false;

        for ( Value<?> objectClass : objectClasses )
        {
            String objectClassName = objectClass.getString();

            if ( SchemaConstants.TOP_OC.equals( objectClassName ) )
            {
                continue;
            }

            if ( SchemaConstants.EXTENSIBLE_OBJECT_OC.equalsIgnoreCase( objectClassName ) )
            {
                hasExtensibleObject = true;
            }

            ObjectClass oc = registry.lookup( objectClassName );

            // Add all unseen objectClasses to the list, except 'top'
            if ( !ocSeen.contains( oc.getOid() ) )
            {
                ocSeen.add( oc.getOid() );
                result.add( oc );
            }

            // Find all current OC parents
            getSuperiors( oc, ocSeen, result );
        }

        return hasExtensibleObject;
    }


    private Set<String> getAllMust( EntryAttribute objectClasses ) throws Exception
    {
        Set<String> must = new HashSet<String>();

        // Loop on all objectclasses
        for ( Value<?> value : objectClasses )
        {
            String ocName = value.getString();
            ObjectClass oc = registries.getObjectClassRegistry().lookup( ocName );

            AttributeType[] types = oc.getMustList();

            // For each objectClass, loop on all MUST attributeTypes, if any
            if ( ( types != null ) && ( types.length > 0 ) )
            {
                for ( AttributeType type : types )
                {
                    must.add( type.getOid() );
                }
            }
        }

        return must;
    }


    private Set<String> getAllAllowed( EntryAttribute objectClasses, Set<String> must ) throws Exception
    {
        Set<String> allowed = new HashSet<String>( must );

        // Add the 'ObjectClass' attribute ID
        allowed.add( registries.getOidRegistry().getOid( SchemaConstants.OBJECT_CLASS_AT ) );

        // Loop on all objectclasses
        for ( Value<?> objectClass : objectClasses )
        {
            String ocName = objectClass.getString();
            ObjectClass oc = registries.getObjectClassRegistry().lookup( ocName );

            AttributeType[] types = oc.getMayList();

            // For each objectClass, loop on all MAY attributeTypes, if any
            if ( ( types != null ) && ( types.length > 0 ) )
            {
                for ( AttributeType type : types )
                {
                    String oid = type.getOid();

                    allowed.add( oid );
                }
            }
        }

        return allowed;
    }


    /**
     * Given the objectClasses for an entry, this method adds missing ancestors 
     * in the hierarchy except for top which it removes.  This is used for this
     * solution to DIREVE-276.  More information about this solution can be found
     * <a href="http://docs.safehaus.org:8080/x/kBE">here</a>.
     * 
     * @param objectClassAttr the objectClass attribute to modify
     * @throws Exception if there are problems 
     */
    private void alterObjectClasses( EntryAttribute objectClassAttr ) throws Exception
    {
        Set<String> objectClasses = new HashSet<String>();
        Set<String> objectClassesUP = new HashSet<String>();

        // Init the objectClass list with 'top'
        objectClasses.add( SchemaConstants.TOP_OC );
        objectClassesUP.add( SchemaConstants.TOP_OC );

        // Construct the new list of ObjectClasses
        for ( Value<?> ocValue : objectClassAttr )
        {
            String ocName = ocValue.getString();

            if ( !ocName.equalsIgnoreCase( SchemaConstants.TOP_OC ) )
            {
                String ocLowerName = ocName.toLowerCase();

                ObjectClass objectClass = registries.getObjectClassRegistry().lookup( ocLowerName );

                if ( !objectClasses.contains( ocLowerName ) )
                {
                    objectClasses.add( ocLowerName );
                    objectClassesUP.add( ocName );
                }

                List<ObjectClass> ocSuperiors = superiors.get( objectClass.getOid() );

                if ( ocSuperiors != null )
                {
                    for ( ObjectClass oc : ocSuperiors )
                    {
                        if ( !objectClasses.contains( oc.getName().toLowerCase() ) )
                        {
                            objectClasses.add( oc.getName() );
                            objectClassesUP.add( oc.getName() );
                        }
                    }
                }
            }
        }

        // Now, reset the ObjectClass attribute and put the new list into it
        objectClassAttr.clear();

        for ( String attribute : objectClassesUP )
        {
            objectClassAttr.add( attribute );
        }
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext ) throws Exception
    {
        LdapDN oriChildName = opContext.getDn();

        ClonedServerEntry entry = opContext.lookup( oriChildName, ByPassConstants.LOOKUP_BYPASS );

        if ( oriChildName.startsWith( schemaBaseDN ) )
        {
            schemaManager.move( opContext, entry, opContext.hasRequestControl( CascadeControl.CONTROL_OID ) );
        }

        next.moveAndRename( opContext );
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws Exception
    {
        LdapDN oriChildName = opContext.getDn();

        ClonedServerEntry entry = opContext.lookup( oriChildName, ByPassConstants.LOOKUP_BYPASS );

        if ( oriChildName.startsWith( schemaBaseDN ) )
        {
            schemaManager.replace( opContext, entry, opContext.hasRequestControl( CascadeControl.CONTROL_OID ) );
        }

        next.move( opContext );
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws Exception
    {
        LdapDN name = opContext.getDn();
        Rdn newRdn = opContext.getNewRdn();
        boolean deleteOldRn = opContext.getDelOldDn();
        ServerEntry entry = opContext.lookup( name, ByPassConstants.LOOKUP_BYPASS );

        /*
         *  Note: This is only a consistency checks, to the ensure that all
         *  mandatory attributes are available after deleting the old RDN.
         *  The real modification is done in the JdbmStore class.
         *  - TODO: this check is missing in the moveAndRename() method
         */
        if ( deleteOldRn )
        {
            ServerEntry tmpEntry = ( ServerEntry ) entry.clone();
            Rdn oldRDN = name.getRdn();

            // Delete the old RDN means we remove some attributes and values.
            // We must make sure that after this operation all must attributes
            // are still present in the entry.
            for ( AttributeTypeAndValue atav : oldRDN )
            {
                AttributeType type = atRegistry.lookup( atav.getUpType() );
                String value = ( String ) atav.getNormValue().getString();
                tmpEntry.remove( type, value );
            }
            for ( AttributeTypeAndValue atav : newRdn )
            {
                AttributeType type = atRegistry.lookup( atav.getUpType() );
                String value = ( String ) atav.getNormValue().getString();
                if ( !tmpEntry.contains( type, value ) )
                {
                    tmpEntry.add( new DefaultServerAttribute( type, value ) );
                }
            }

            // Substitute the RDN and check if the new entry is correct
            LdapDN newDn = ( LdapDN ) name.clone();
            newDn.remove( name.size() - 1 );
            newDn.add( newRdn );

            tmpEntry.setDn( newDn );
            check( name, tmpEntry );

            // Check that no operational attributes are removed
            for ( AttributeTypeAndValue atav : oldRDN )
            {
                AttributeType attributeType = atRegistry.lookup( atav.getUpType() );

                if ( !attributeType.isCanUserModify() )
                {
                    throw new NoPermissionException( "Cannot modify the attribute '" + atav.getUpType() + "'" );
                }
            }
        }

        if ( name.startsWith( schemaBaseDN ) )
        {
            schemaManager.modifyRn( opContext, entry, opContext.hasRequestControl( CascadeControl.CONTROL_OID ) );
        }

        next.rename( opContext );
    }


    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        ServerEntry entry;
        LdapDN name = opContext.getDn();
        List<Modification> mods = opContext.getModItems();

        // handle operations against the schema subentry in the schema service
        // and never try to look it up in the nexus below
        if ( name.getNormName().equalsIgnoreCase( subschemaSubentryDnNorm ) )
        {
            entry = schemaService.getSubschemaEntry( SCHEMA_SUBENTRY_RETURN_ATTRIBUTES );
            entry.setDn( name );
        }
        else
        {
            // Get the entry we already read at the beginning
            entry = opContext.getEntry();
        }
        
        boolean schemaModification = name.startsWith( schemaBaseDN );
        boolean subSchemaModification = subschemaSubentryDnNorm.equals( name.getNormName() );

        // First, we get the entry from the backend. If it does not exist, then we throw an exception
        ServerEntry targetEntry = (ServerEntry)SchemaUtils.getTargetEntry( mods , entry );

        if ( entry == null )
        {
            LOG.error( "No entry with this name :{}", name );
            throw new LdapNameNotFoundException( "The entry which name is " + name + " is not found." );
        }

        // We will use this temporary entry to check that the modifications
        // can be applied as atomic operations
        ServerEntry tmpEntry = ( ServerEntry ) entry.clone();

        Set<String> modset = new HashSet<String>();
        Modification objectClassMod = null;

        // Check that we don't have two times the same modification.
        // This is somehow useless, as modification operations are supposed to
        // be atomic, so we may have a sucession of Add, DEL, ADD operations
        // for the same attribute, and this will be legal.
        // @TODO : check if we can remove this test.
        for ( Modification mod : mods )
        {
            if ( mod.getAttribute().getId().equalsIgnoreCase( SchemaConstants.OBJECT_CLASS_AT ) )
            {
                objectClassMod = mod;
            }

            // Freak out under some weird cases
            if ( mod.getAttribute().size() == 0 )
            {
                // not ok for add but ok for replace and delete
                if ( mod.getOperation() == ModificationOperation.ADD_ATTRIBUTE )
                {
                    throw new LdapInvalidAttributeValueException( "No value is not a valid value for an attribute.",
                        ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                }
            }

            StringBuffer keybuf = new StringBuffer();
            keybuf.append( mod.getOperation() );
            keybuf.append( mod.getAttribute().getId() );

            for ( Value<?> value : ( ServerAttribute ) mod.getAttribute() )
            {
                keybuf.append( value.getString() );
            }

            if ( !modset.add( keybuf.toString() ) && ( mod.getOperation() == ModificationOperation.ADD_ATTRIBUTE ) )
            {
                throw new LdapAttributeInUseException( "found two copies of the following modification item: " + mod );
            }
        }

        // Get the objectClass attribute.
        EntryAttribute objectClass;

        if ( objectClassMod == null )
        {
            objectClass = entry.get( SchemaConstants.OBJECT_CLASS_AT );

            if ( objectClass == null )
            {
                objectClass = new DefaultServerAttribute( SchemaConstants.OBJECT_CLASS_AT, OBJECT_CLASS );
            }
        }
        else
        {
            objectClass = getResultantObjectClasses( objectClassMod.getOperation(), objectClassMod.getAttribute(),
                tmpEntry.get( SchemaConstants.OBJECT_CLASS_AT ).clone() );
        }

        ObjectClassRegistry ocRegistry = this.registries.getObjectClassRegistry();

        // Now, apply the modifications on the cloned entry before applying it on the
        // real object.
        for ( Modification mod : mods )
        {
            ModificationOperation modOp = mod.getOperation();
            ServerAttribute change = ( ServerAttribute ) mod.getAttribute();

            // TODO/ handle http://issues.apache.org/jira/browse/DIRSERVER-1198
            if ( ( change.getAttributeType() == null ) && !atRegistry.hasAttributeType( change.getUpId() )
                && !objectClass.contains( SchemaConstants.EXTENSIBLE_OBJECT_OC ) )
            {
                throw new LdapInvalidAttributeIdentifierException();
            }

            // We will forbid modification of operational attributes which are not
            // user modifiable.
            AttributeType attributeType = change.getAttributeType();
            
            if ( attributeType == null )
            {
                attributeType = atRegistry.lookup( change.getUpId() );
            }

            if ( !attributeType.isCanUserModify() )
            {
                throw new NoPermissionException( "Cannot modify the attribute '" + change.getUpId() + "'" );
            }

            switch ( modOp )
            {
                case ADD_ATTRIBUTE:
                    EntryAttribute attr = tmpEntry.get( attributeType.getName() );

                    if ( attr != null )
                    {
                        for ( Value<?> value : change )
                        {
                            attr.add( value );
                        }
                    }
                    else
                    {
                        attr = new DefaultServerAttribute( change.getUpId(), attributeType );

                        for ( Value<?> value : change )
                        {
                            attr.add( value );
                        }

                        tmpEntry.put( attr );
                    }

                    break;

                case REMOVE_ATTRIBUTE:
                    if ( tmpEntry.get( change.getId() ) == null )
                    {
                        LOG.error( "Trying to remove an non-existant attribute: " + change.getUpId() );
                        throw new LdapNoSuchAttributeException();
                    }

                    // We may have to remove the attribute or only some values
                    if ( change.size() == 0 )
                    {
                        // No value : we have to remove the entire attribute
                        // Check that we aren't removing a MUST attribute
                        if ( isRequired( change.getUpId(), objectClass ) )
                        {
                            LOG.error( "Trying to remove a required attribute: " + change.getUpId() );
                            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION );
                        }
                        
                        attr = tmpEntry.get( change.getUpId() );
                        if ( attr != null )
                        {
                            tmpEntry.removeAttributes( change.getUpId() );
                        }
                    }
                    else
                    {
                        // for required attributes we need to check if all values are removed
                        // if so then we have a schema violation that must be thrown
                        if ( isRequired( change.getUpId(), objectClass ) && isCompleteRemoval( change, entry ) )
                        {
                            LOG.error( "Trying to remove a required attribute: " + change.getUpId() );
                            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION );
                        }

                        // Now remove the attribute and all its values
                        EntryAttribute modified = tmpEntry.removeAttributes( change.getId() ).get( 0 );

                        // And inject back the values except the ones to remove
                        for ( Value<?> value : change )
                        {
                            if ( modified.contains( value ) )
                            {
                                modified.remove( value );
                            }
                            else if ( !subSchemaModification )
                            {
                                // We are trying to remove an not existing value : error
                                throw new LdapNoSuchAttributeException( "Value " + value + " does not exist in the " + modified + " AT" );
                            }
                        }

                        // ok, done. Last check : if the attribute does not content any more value;
                        // and if it's a MUST one, we should thow an exception
                        if ( ( modified.size() == 0 ) && isRequired( change.getId(), objectClass ) )
                        {
                            LOG.error( "Trying to remove a required attribute: " + change.getUpId() );
                            throw new LdapSchemaViolationException( ResultCodeEnum.OBJECT_CLASS_VIOLATION );
                        }

                        // Put back the attribute in the entry only if it has values left in it
                        if ( modified.size() > 0 )
                        {
                            tmpEntry.put( modified );
                        }
                    }

                    SchemaChecker
                        .preventRdnChangeOnModifyRemove( name, modOp, change, this.registries.getOidRegistry() );
                    SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, modOp, change,
                        objectClass );
                    break;

                case REPLACE_ATTRIBUTE:
                    SchemaChecker.preventRdnChangeOnModifyReplace( name, modOp, change, registries.getOidRegistry() );
                    SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, modOp, change );

                    attr = tmpEntry.get( change.getUpId() );

                    if ( attr != null )
                    {
                        tmpEntry.removeAttributes( change.getUpId() );
                    }

                    attr = new DefaultServerAttribute( change.getUpId(), attributeType );

                    if ( change.size() != 0 )
                    {
                        for ( Value<?> value : change )
                        {
                            attr.add( value );
                        }

                        tmpEntry.put( attr );
                    }

                    break;
            }
        }

        check( name, tmpEntry );

        // let's figure out if we need to add or take away from mods to maintain 
        // the objectClass attribute with it's hierarchy of ancestors 
        if ( objectClassMod != null )
        {
            ServerAttribute alteredObjectClass = ( ServerAttribute ) objectClass.clone();
            alterObjectClasses( alteredObjectClass );

            if ( !alteredObjectClass.equals( objectClass ) )
            {
                ServerAttribute ocMods = ( ServerAttribute ) objectClassMod.getAttribute();

                switch ( objectClassMod.getOperation() )
                {
                    case ADD_ATTRIBUTE:
                        if ( ocMods.contains( SchemaConstants.TOP_OC ) )
                        {
                            ocMods.remove( SchemaConstants.TOP_OC );
                        }

                        for ( Value<?> value : alteredObjectClass )
                        {
                            if ( !objectClass.contains( value ) )
                            {
                                ocMods.add( value );
                            }
                        }

                        break;

                    case REMOVE_ATTRIBUTE:
                        for ( Value<?> value : alteredObjectClass )
                        {
                            if ( !objectClass.contains( value ) )
                            {
                                ocMods.remove( value );
                            }
                        }

                        break;

                    case REPLACE_ATTRIBUTE:
                        for ( Value<?> value : alteredObjectClass )
                        {
                            if ( !objectClass.contains( value ) )
                            {
                                ocMods.add( value );
                            }
                        }

                        break;

                    default:
                }
            }
        }

        if ( schemaModification )
        {
            LOG.debug( "Modification attempt on schema partition {}: \n{}", name, opContext );

            schemaManager.modify( opContext, entry, targetEntry, opContext
                .hasRequestControl( CascadeControl.CONTROL_OID ) );
        }
        else if ( subSchemaModification )
        {
            LOG.debug( "Modification attempt on schema subentry {}: \n{}", name, opContext );

            schemaManager.modifySchemaSubentry( opContext, entry, targetEntry, opContext
                .hasRequestControl( CascadeControl.CONTROL_OID ) );
            return;
        }

        next.modify( opContext );
    }

    
    /**
     * Create a new attribute using the given values
     */
    private EntryAttribute createNewAttribute( ServerAttribute attribute )
    {
        AttributeType attributeType = attribute.getAttributeType();
        
        // Create the new Attribute
        EntryAttribute newAttribute = new DefaultServerAttribute( attribute.getUpId(), attributeType );

        for ( Value<?> value : attribute )
        {
            newAttribute.add( value );
        }

        return newAttribute;
    }
    
    
    /**
     * Modify an entry, applying the given modifications.
     */
    private ServerEntry modifyEntry( LdapDN dn, ServerEntry currentEntry, List<Modification> mods ) throws Exception
    {
        // The first step is to check that the modifications are valid :
        // - the AT are present in the schema
        // - The value is syntaxically correct
        //
        // While doing that, we will apply the modification to a copy of the current entry
        ServerEntry tempEntry = (ServerEntry)currentEntry.clone();
        
        // Now, apply each mod one by one
        for ( Modification mod:mods )
        {
            ServerAttribute attribute = (ServerAttribute)mod.getAttribute();
            AttributeType attributeType = attribute.getAttributeType();
            
            // We don't allow modification of operational attributes
            if ( !attributeType.isCanUserModify() )
            {
                String msg = "Cannot modify the attribute : " + attributeType;
                LOG.error( msg );
                throw new NoPermissionException( msg );
            }
            
            // Check the syntax here
            if ( !attribute.isValid() )
            {
                // The value syntax is incorrect : this is an error
                String msg = "The new Attribute or one of its value is incorrect : " + attributeType;
                LOG.error( msg );
                throw new LdapInvalidAttributeValueException( msg, 
                    ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
            }

            switch ( mod.getOperation() )
            {
                case ADD_ATTRIBUTE :
                    EntryAttribute currentAttribute = tempEntry.get( attributeType );
                    
                    // First check if the added Attribute is already present in the entry
                    // If not, we have to create the entry
                    if ( currentAttribute != null )
                    {
                        for ( Value<?> value : attribute )
                        {
                            // At this point, we know that the attribute's syntax is correct
                            // We just have to check thaat the current attribute does not 
                            // contains the value already
                            if ( currentAttribute.contains( value ))
                            {
                                // This is an error. 
                                String msg = "Cannot add a value which is already present : " +
                                    value;
                                LOG.error( msg );
                                throw new LdapAttributeInUseException( msg );
                            }
                            
                            currentAttribute.add( value );
                        }
                    }
                    else
                    {
                        // We don't check if the attribute is not in the MUST or MAY at this
                        // point, as one of the following modification can change the 
                        // ObjectClasses.
                        EntryAttribute newAttribute = createNewAttribute( attribute );

                        tempEntry.put( newAttribute );
                    }
                    
                    break;
                    
                case REMOVE_ATTRIBUTE :
                    // First check that the removed attribute exists
                    if ( !tempEntry.containsAttribute( attributeType ) )
                    {
                        String msg = "Trying to remove an non-existant attribute: " + attributeType;
                        LOG.error( msg );
                        throw new LdapNoSuchAttributeException( msg );
                    }

                    // We may have to remove the attribute or only some values
                    if ( attribute.size() == 0 )
                    {
                        // No value : we have to remove the entire attribute
                        tempEntry.removeAttributes( attributeType );
                    }
                    else
                    {
                        currentAttribute = tempEntry.get( attributeType );
                        
                        // Now remove all the values
                        for ( Value<?> value:attribute )
                        {
                            // We can only remove existing values.
                            if ( currentAttribute.contains( value ) )
                            {
                                currentAttribute.remove( value );
                            }
                            else
                            {
                                String msg = "Cannot remove an absent value from attribute : " + attributeType;
                                LOG.error( msg );
                                throw new LdapNoSuchAttributeException( msg );
                            }
                        }
                        
                        // If the current attribute is empty, we have to remove
                        // it from the entry
                        if ( currentAttribute.size() == 0 )
                        {
                            tempEntry.removeAttributes( attributeType );
                        }
                    }

                    break;
                
                case REPLACE_ATTRIBUTE :
                    // The replaced attribute might not exist, it will then be a Add
                    // If there is no value, then the attribute will be removed
                    if ( !tempEntry.containsAttribute( attributeType ) )
                    {
                        if ( attribute.size() == 0 )
                        {
                            // Ignore the modification, as the attributeType does not
                            // exists in the entry
                            break;
                        }
                        else
                        {
                            // Create the new Attribute
                            EntryAttribute newAttribute = createNewAttribute( attribute );

                            tempEntry.put( newAttribute );
                        }
                    }
                    else
                    {
                        if ( attribute.size() == 0 )
                        {
                            // Remove the attribute from the entry
                            tempEntry.removeAttributes( attributeType );
                        }
                        else
                        {
                            // Replace the existing values with the new values
                            // This is done by removing the Attribute
                            tempEntry.removeAttributes( attributeType );

                            // Create the new Attribute
                            EntryAttribute newAttribute = createNewAttribute( attribute );

                            tempEntry.put( newAttribute );
                        }
                    }
                    
                    break;
            }
        }
        
        // Ok, we have created the modified entry. We now have to check that it's a valid 
        // entry wrt the schema.
        // We have to check that :
        // - the rdn values are present in the entry
        // - the objectClasses inheritence is correct
        // - all the MUST are present
        // - all the attribute are in MUST and MAY, except fo the extensibleObeject OC
        // is present
        check( dn, tempEntry );
        
        return tempEntry;
    }

    
    /**
     * {@inheritDoc}
     */
    public void modifyNew( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        // First, check that the entry is either a subschemaSubentry or a schema element.
        // This is the case if it's a child of cn=schema or ou=schema
        LdapDN dn = opContext.getDn();
        ServerEntry targetEntry = null;
        
        if ( dn.equals( subschemaSubentryDn ) )
        {
            ServerEntry currentEntry = schemaService.getSubschemaEntry( SCHEMA_SUBENTRY_RETURN_ATTRIBUTES );
            targetEntry = modifyEntry( dn, currentEntry, opContext.getModItems() );
        }
        else
        {
            targetEntry = modifyEntry( dn, opContext.getEntry(), opContext.getModItems() );
        }
        
        if ( dn.startsWith( schemaBaseDN ) )
        {
            LOG.debug( "Modification attempt on schema partition {}: \n{}", dn, opContext );

            schemaManager.modify( opContext, opContext.getEntry(), targetEntry, opContext
                .hasRequestControl( CascadeControl.CONTROL_OID ) );
        }
        else if ( dn.equals( subschemaSubentryDn ) )
        {
            LOG.debug( "Modification attempt on schema subentry {}: \n{}", dn, opContext );

            schemaManager.modifySchemaSubentry( opContext, opContext.getEntry(), targetEntry, opContext
                .hasRequestControl( CascadeControl.CONTROL_OID ) );
            return;
        }

        // We are golden ! Let's apply the change then.
        next.modify( opContext );
    }
    

    /**
     * Filter the attributes by removing the ones which are not allowed
     */
    private void filterAttributeTypes( SearchingOperationContext operation, ClonedServerEntry result )
    {
        if ( operation.getReturningAttributes() == null )
        {
            return;
        }

        for ( AttributeTypeOptions attrOptions : operation.getReturningAttributes() )
        {
            EntryAttribute attribute = result.get( attrOptions.getAttributeType() );

            if ( attrOptions.hasOption() )
            {
                for ( String option : attrOptions.getOptions() )
                {
                    if ( "binary".equalsIgnoreCase( option ) )
                    {
                        continue;
                    }
                    else
                    {
                        try
                        {
                            if ( result.contains( attribute ) )
                            {
                                result.remove( attribute );
                            }
                        }
                        catch ( NamingException ne )
                        {
                            // Do nothings
                        }
                        break;
                    }
                }

            }
        }
    }


    private void filterObjectClass( ServerEntry entry ) throws Exception
    {
        List<ObjectClass> objectClasses = new ArrayList<ObjectClass>();
        EntryAttribute oc = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( oc != null )
        {
            getObjectClasses( oc, objectClasses );

            entry.removeAttributes( SchemaConstants.OBJECT_CLASS_AT );

            ServerAttribute newOc = new DefaultServerAttribute( ( ( ServerAttribute ) oc ).getAttributeType() );

            for ( ObjectClass currentOC : objectClasses )
            {
                newOc.add( currentOC.getName() );
            }

            newOc.add( SchemaConstants.TOP_OC );
            entry.put( newOc );
        }
    }


    private void filterBinaryAttributes( ServerEntry entry ) throws Exception
    {
        /*
         * start converting values of attributes to byte[]s which are not
         * human readable and those that are in the binaries set
         */
        for ( EntryAttribute attribute : entry )
        {
            if ( !( ( ServerAttribute ) attribute ).getAttributeType().getSyntax().isHumanReadable() )
            {
                List<Value<?>> binaries = new ArrayList<Value<?>>();

                for ( Value<?> value : attribute )
                {
                    binaries.add( new ServerBinaryValue( ( ( ServerAttribute ) attribute ).getAttributeType(),
                        value.getBytes() ) );
                }

                attribute.clear();
                attribute.put( binaries );
            }
        }
    }

    /**
     * A special filter over entry attributes which replaces Attribute String values with their respective byte[]
     * representations using schema information and the value held in the JNDI environment property:
     * <code>java.naming.ldap.attributes.binary</code>.
     *
     * @see <a href= "http://java.sun.com/j2se/1.4.2/docs/guide/jndi/jndi-ldap-gl.html#binary">
     *      java.naming.ldap.attributes.binary</a>
     */
    private class BinaryAttributeFilter implements EntryFilter
    {
        public boolean accept( SearchingOperationContext operation, ClonedServerEntry result ) throws Exception
        {
            filterBinaryAttributes( result );
            return true;
        }
    }

    /**
     * Filters objectClass attribute to inject top when not present.
     */
    private class TopFilter implements EntryFilter
    {
        public boolean accept( SearchingOperationContext operation, ClonedServerEntry result ) throws Exception
        {
            filterObjectClass( result );
            filterAttributeTypes( operation, result );
            return true;
        }
    }


    /**
     * Check that all the attributes exist in the schema for this entry.
     * 
     * We also check the syntaxes
     */
    private void check( LdapDN dn, ServerEntry entry ) throws Exception
    {
        // ---------------------------------------------------------------
        // First, make sure all attributes are valid schema defined attributes
        // ---------------------------------------------------------------

        for ( AttributeType attributeType : entry.getAttributeTypes() )
        {
            if ( !atRegistry.hasAttributeType( attributeType.getName() ) )
            {
                throw new LdapInvalidAttributeIdentifierException( attributeType.getName()
                    + " not found in attribute registry!" );
            }
        }

        // We will check some elements :
        // 1) the entry must have all the MUST attributes of all its ObjectClass
        // 2) The SingleValued attributes must be SingleValued
        // 3) No attributes should be used if they are not part of MUST and MAY
        // 3-1) Except if the extensibleObject ObjectClass is used
        // 3-2) or if the AttributeType is COLLECTIVE
        // 4) We also check that for H-R attributes, we have a valid String in the values
        EntryAttribute objectClassAttr = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        // Protect the server against a null objectClassAttr
        // It can be the case if the user forgot to add it to the entry ...
        // In this case, we create an new one, empty
        if ( objectClassAttr == null )
        {
            objectClassAttr = new DefaultServerAttribute( SchemaConstants.OBJECT_CLASS_AT, OBJECT_CLASS );
        }

        List<ObjectClass> ocs = new ArrayList<ObjectClass>();

        alterObjectClasses( objectClassAttr );

        // Now we can process the MUST and MAY attributes
        Set<String> must = getAllMust( objectClassAttr );
        Set<String> allowed = getAllAllowed( objectClassAttr, must );

        boolean hasExtensibleObject = getObjectClasses( objectClassAttr, ocs );

        // As we now have all the ObjectClasses updated, we have
        // to check that we don't have conflicting ObjectClasses
        assertObjectClasses( dn, ocs );

        assertRequiredAttributesPresent( dn, entry, must );
        assertNumberOfAttributeValuesValid( entry );

        if ( !hasExtensibleObject )
        {
            assertAllAttributesAllowed( dn, entry, allowed );
        }

        // Check the attributes values and transform them to String if necessary
        assertHumanReadable( entry );

        // Now check the syntaxes
        assertSyntaxes( entry );
    }


    private void checkOcSuperior( ServerEntry entry ) throws Exception
    {
        ObjectClassRegistry ocRegistry = registries.getObjectClassRegistry();
        
        // handle the m-supObjectClass meta attribute
        EntryAttribute supOC = entry.get( MetaSchemaConstants.M_SUP_OBJECT_CLASS_AT );
        
        if ( supOC != null )
        {
            ObjectClassTypeEnum ocType = ObjectClassTypeEnum.STRUCTURAL;
            
            if ( entry.get( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT ) != null )
            {
                String type = entry.get( MetaSchemaConstants.M_TYPE_OBJECT_CLASS_AT ).getString();
                ocType = ObjectClassTypeEnum.getClassType( type );
            }
            
            // First check that the inheritence scheme is correct.
            // 1) If the ocType is ABSTRACT, it should not have any other SUP not ABSTRACT
            for ( Value<?> sup:supOC )
            {
                try
                {
                    String supName = sup.getString();
                    
                    ObjectClass superior = ocRegistry.lookup( supName );

                    switch ( ocType )
                    {
                        case ABSTRACT :
                            if ( !superior.isAbstract() )
                            {
                                String message = "An ABSTRACT ObjectClass cannot inherit from an objectClass which is not ABSTRACT";
                                LOG.error( message );
                                throw new LdapSchemaViolationException( message, ResultCodeEnum.OBJECT_CLASS_VIOLATION );
                            }
                            
                            break;
    
                        case AUXILIARY :
                            if ( !superior.isAbstract() && ! superior.isAuxiliary() )
                            {
                                String message = "An AUXILiARY ObjectClass cannot inherit from an objectClass which is not ABSTRACT or AUXILIARY";
                                LOG.error( message );
                                throw new LdapSchemaViolationException( message, ResultCodeEnum.OBJECT_CLASS_VIOLATION );
                            }
                            
                            break;

                        case STRUCTURAL :
                            break;
                    }
                }
                catch ( NamingException ne )
                {
                    // The superior OC does not exist : this is an error
                    String message = "Cannot have a superior which does not exist";
                    LOG.error( message );
                    throw new LdapSchemaViolationException( message, ResultCodeEnum.OBJECT_CLASS_VIOLATION );
                }
            }
        }
    }


    /**
     * Check that all the attributes exist in the schema for this entry.
     */
    public void add( NextInterceptor next, AddOperationContext addContext ) throws Exception
    {
        LdapDN name = addContext.getDn();
        ServerEntry entry = addContext.getEntry();

        check( name, entry );

        // Special checks for the MetaSchema branch
        if ( name.startsWith( schemaBaseDN ) )
        {
            schemaManager.add( addContext );
            
            if ( entry.contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.META_SCHEMA_OC ) )
            {
                // This is a schema addition
                // We have to check if the schema is enabled or disabled
                String schema = entry.get( SchemaConstants.CN_AT ).getString();
                
                next.add( addContext );

                if ( isEnabled( schema ) )
                {
                    // Update the OC superiors for each added ObjectClass
                    computeSuperiors();
                }
            }
            else if ( entry.contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.META_OBJECT_CLASS_OC ) )
            {
                // This is an ObjectClass addition
                checkOcSuperior( addContext.getEntry() );

                next.add( addContext );

                // Update the structures now that the schema element has been added
                String schemaName = MetaSchemaUtils.getSchemaName( name );
                
                if ( isEnabled( schemaName ) )
                {
                    String ocName = entry.get( MetaSchemaConstants.M_NAME_AT ).getString();
                    ObjectClass addedOC = registries.getObjectClassRegistry().lookup( ocName );
                    computeSuperior( addedOC );
                }
            }
            else if ( entry.contains( SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.META_ATTRIBUTE_TYPE_OC ) )
            {
                // This is an AttributeType addition
                next.add( addContext );
            }
            else
            {
                next.add( addContext );
            }
            
        }
        else
        {
            next.add( addContext );
        }
    }


    /**
     * Checks to see if an attribute is required by as determined from an entry's
     * set of objectClass attribute values.
     *
     * @return true if the objectClass values require the attribute, false otherwise
     * @throws Exception if the attribute is not recognized
     */
    private void assertAllAttributesAllowed( LdapDN dn, ServerEntry entry, Set<String> allowed ) throws Exception
    {
        // Never check the attributes if the extensibleObject objectClass is
        // declared for this entry
        EntryAttribute objectClass = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( objectClass.contains( SchemaConstants.EXTENSIBLE_OBJECT_OC ) )
        {
            return;
        }

        for ( EntryAttribute attribute : entry )
        {
            String attrOid = ( ( ServerAttribute ) attribute ).getAttributeType().getOid();

            AttributeType attributeType = ( ( ServerAttribute ) attribute ).getAttributeType();

            if ( !attributeType.isCollective() && ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS ) )
            {
                if ( !allowed.contains( attrOid ) )
                {
                    throw new LdapSchemaViolationException( "Attribute " + attribute.getUpId()
                        + " not declared in objectClasses of entry " + dn.getUpName(),
                        ResultCodeEnum.OBJECT_CLASS_VIOLATION );
                }
            }
        }
    }


    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws Exception
    {
        LdapDN name = opContext.getDn();

        if ( name.startsWith( schemaBaseDN ) )
        {
            ClonedServerEntry entry = nexus.lookup( opContext.newLookupContext( name ) );
            schemaManager.delete( opContext, entry, opContext.hasRequestControl( CascadeControl.CONTROL_OID ) );
        }

        next.delete( opContext );
    }


    /**
     * Checks to see number of values of an attribute conforms to the schema
     */
    private void assertNumberOfAttributeValuesValid( Entry entry ) throws InvalidAttributeValueException, Exception
    {
        for ( EntryAttribute attribute : entry )
        {
            assertNumberOfAttributeValuesValid( attribute );
        }
    }


    /**
     * Checks to see numbers of values of attributes conforms to the schema
     */
    private void assertNumberOfAttributeValuesValid( EntryAttribute attribute ) throws InvalidAttributeValueException,
        Exception
    {
        if ( attribute.size() > 1 && ( ( ServerAttribute ) attribute ).getAttributeType().isSingleValue() )
        {
            throw new LdapInvalidAttributeValueException( "More than one value has been provided "
                + "for the single-valued attribute: " + attribute.getUpId(), ResultCodeEnum.CONSTRAINT_VIOLATION );
        }
    }


    /**
     * Checks to see the presence of all required attributes within an entry.
     */
    private void assertRequiredAttributesPresent( LdapDN dn, Entry entry, Set<String> must ) throws Exception
    {
        for ( EntryAttribute attribute : entry )
        {
            must.remove( ( ( ServerAttribute ) attribute ).getAttributeType().getOid() );
        }

        if ( must.size() != 0 )
        {
            throw new LdapSchemaViolationException( "Required attributes " + must + " not found within entry "
                + dn.getUpName(), ResultCodeEnum.OBJECT_CLASS_VIOLATION );
        }
    }


    /**
     * Checck that OC does not conflict :
     * - we can't have more than one STRUCTURAL OC unless they are in the same
     * inheritance tree
     * - we must have at least one STRUCTURAL OC
     */
    private void assertObjectClasses( LdapDN dn, List<ObjectClass> ocs ) throws Exception
    {
        Set<ObjectClass> structuralObjectClasses = new HashSet<ObjectClass>();

        /*
         * Since the number of ocs present in an entry is small it's not 
         * so expensive to take two passes while determining correctness
         * since it will result in clear simple code instead of a deep nasty
         * for loop with nested loops.  Plus after the first pass we can
         * quickly know if there are no structural object classes at all.
         */

        // --------------------------------------------------------------------
        // Extract all structural objectClasses within the entry
        // --------------------------------------------------------------------
        for ( ObjectClass oc : ocs )
        {
            if ( oc.isStructural() )
            {
                structuralObjectClasses.add( oc );
            }
        }

        // --------------------------------------------------------------------
        // Throw an error if no STRUCTURAL objectClass are found.
        // --------------------------------------------------------------------

        if ( structuralObjectClasses.isEmpty() )
        {
            String message = "Entry " + dn + " does not contain a STRUCTURAL ObjectClass";
            LOG.error( message );
            throw new LdapSchemaViolationException( message, ResultCodeEnum.OBJECT_CLASS_VIOLATION );
        }

        // --------------------------------------------------------------------
        // Put all structural object classes into new remaining container and
        // start removing any which are superiors of others in the set.  What
        // is left in the remaining set will be unrelated structural 
        /// objectClasses.  If there is more than one then we have a problem.
        // --------------------------------------------------------------------

        Set<ObjectClass> remaining = new HashSet<ObjectClass>( structuralObjectClasses.size() );
        remaining.addAll( structuralObjectClasses );
        
        for ( ObjectClass oc : structuralObjectClasses )
        {
            if ( oc.getSuperClasses() != null )
            {
                for ( ObjectClass superClass : oc.getSuperClasses() )
                {
                    if ( superClass.isStructural() )
                    {
                        remaining.remove( superClass );
                    }
                }
            }
        }

        // Like the highlander there can only be one :).
        if ( remaining.size() > 1 )
        {
            String message = "Entry " + dn + " contains more than one STRUCTURAL ObjectClass: " + remaining;
            LOG.error( message );
            throw new LdapSchemaViolationException( message, ResultCodeEnum.OBJECT_CLASS_VIOLATION );
        }
    }


    /**
     * Check the entry attributes syntax, using the syntaxCheckers
     */
    private void assertSyntaxes( Entry entry ) throws Exception
    {
        // First, loop on all attributes
        for ( EntryAttribute attribute : entry )
        {
            AttributeType attributeType = ( ( ServerAttribute ) attribute ).getAttributeType();
            SyntaxChecker syntaxChecker = attributeType.getSyntax().getSyntaxChecker();

            if ( syntaxChecker instanceof AcceptAllSyntaxChecker )
            {
                // This is a speedup : no need to check the syntax of any value
                // if all the syntaxes are accepted...
                continue;
            }

            // Then loop on all values
            for ( Value<?> value : attribute )
            {
                try
                {
                    syntaxChecker.assertSyntax( value.get() );
                }
                catch ( Exception ne )
                {
                    String message = "Attribute value '" + value.getString()
                         + "' for attribute '" + attribute.getUpId() + "' is syntactically incorrect";
                    LOG.info( message );

                    throw new LdapInvalidAttributeValueException( message, ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
                }
            }
        }
    }


    /**
     * Check a String attribute to see if there is some byte[] value in it.
     * 
     * If this is the case, try to change it to a String value.
     */
    private boolean checkHumanReadable( EntryAttribute attribute ) throws Exception
    {
        boolean isModified = false;

        // Loop on each values
        for ( Value<?> value : attribute )
        {
            if ( value instanceof ServerStringValue )
            {
                continue;
            }
            else if ( value instanceof ServerBinaryValue )
            {
                // we have a byte[] value. It should be a String UTF-8 encoded
                // Let's transform it
                try
                {
                    String valStr = new String( value.getBytes(), "UTF-8" );
                    attribute.remove( value );
                    attribute.add( valStr );
                    isModified = true;
                }
                catch ( UnsupportedEncodingException uee )
                {
                    throw new NamingException( "The value is not a valid String" );
                }
            }
            else
            {
                throw new NamingException( "The value stored in an Human Readable attribute is not a String" );
            }
        }

        return isModified;
    }


    /**
     * Check a binary attribute to see if there is some String value in it.
     * 
     * If this is the case, try to change it to a binary value.
     */
    private boolean checkNotHumanReadable( EntryAttribute attribute ) throws Exception
    {
        boolean isModified = false;

        // Loop on each values
        for ( Value<?> value : attribute )
        {
            if ( value instanceof ServerBinaryValue )
            {
                continue;
            }
            else if ( value instanceof ServerStringValue )
            {
                // We have a String value. It should be a byte[]
                // Let's transform it
                try
                {
                    byte[] valBytes = value.getString().getBytes( "UTF-8" );

                    attribute.remove( value );
                    attribute.add( valBytes );
                    isModified = true;
                }
                catch ( UnsupportedEncodingException uee )
                {
                    String message = "The value stored in a not Human Readable attribute as a String should be convertible to a byte[]";
                    LOG.error( message );
                    throw new NamingException( message );
                }
            }
            else
            {
                String message = "The value is not valid. It should be a String or a byte[]";
                LOG.error( message );
                throw new NamingException( message );
            }
        }

        return isModified;
    }


    /**
     * Check that all the attribute's values which are Human Readable can be transformed
     * to valid String if they are stored as byte[], and that non Human Readable attributes
     * stored as String can be transformed to byte[]
     */
    private void assertHumanReadable( ServerEntry entry ) throws Exception
    {
        boolean isModified = false;

        ServerEntry clonedEntry = null;

        // Loops on all attributes
        for ( EntryAttribute attribute : entry )
        {
            AttributeType attributeType = ( ( ServerAttribute ) attribute ).getAttributeType();

            // If the attributeType is H-R, check all of its values
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                isModified = checkHumanReadable( attribute );
            }
            else
            {
                isModified = checkNotHumanReadable( attribute );
            }

            // If we have a returned attribute, then we need to store it
            // into a new entry
            if ( isModified )
            {
                if ( clonedEntry == null )
                {
                    clonedEntry = ( ServerEntry ) entry.clone();
                }

                // Switch the attributes
                clonedEntry.put( attribute );

                isModified = false;
            }
        }

        if ( clonedEntry != null )
        {
            entry = clonedEntry;
        }
    }
}
