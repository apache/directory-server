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
package org.apache.directory.server.core.schema;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.DirectoryPartitionNexus;
import org.apache.directory.shared.ldap.exception.LdapAttributeInUseException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeIdentifierException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.LdapName;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.DateUtils;
import org.apache.directory.shared.ldap.util.SingletonEnumeration;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link org.apache.directory.server.core.interceptor.Interceptor} that manages and enforces schemas.
 *
 * @todo Better interceptor description required.
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SchemaService extends BaseInterceptor
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final String BINARY_KEY = "java.naming.ldap.attributes.binary";

    /** The LoggerFactory used by this Interceptor */
    private static Logger log = LoggerFactory.getLogger( SchemaService.class );

    /**
     * the root nexus to all database partitions
     */
    private DirectoryPartitionNexus nexus;

    /**
     * a binary attribute tranforming filter: String -> byte[]
     */
    private BinaryAttributeFilter binaryAttributeFilter;
    
    private TopFilter topFilter;
    
    private List filters = new ArrayList();

    /**
     * the global schema object registries
     */
    private GlobalRegistries globalRegistries;
    
    private Set binaries;

    /**
     * subschemaSubentry attribute's value from Root DSE
     */
    private String subschemaSubentryDn;

    /**
     * The time when the server started up.
     */
    private String startUpTimeStamp;

    /**
     * Creates a schema service interceptor.
     */
    public SchemaService()
    {
        startUpTimeStamp = DateUtils.getGeneralizedTime();
    }


    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
        this.nexus = factoryCfg.getPartitionNexus();
        this.globalRegistries = factoryCfg.getGlobalRegistries();
        binaryAttributeFilter = new BinaryAttributeFilter();
        topFilter = new TopFilter();
        filters.add( binaryAttributeFilter );
        filters.add( topFilter );
        binaries = ( Set ) factoryCfg.getEnvironment().get( BINARY_KEY );

        // stuff for dealing with subentries (garbage for now)
        String subschemaSubentry = ( String ) nexus.getRootDSE().get( "subschemaSubentry" ).get();
        subschemaSubentryDn = new LdapName( subschemaSubentry ).toString().toLowerCase();
    }


    /**
     * @return Returns the binaries.
     */
    public boolean isBinary( String id )
    {
        return binaries.contains( StringTools.lowerCase( StringTools.trim( id ) ) );
    }


    public void destroy()
    {
    }


    public NamingEnumeration list( NextInterceptor nextInterceptor, Name base ) throws NamingException
    {
        NamingEnumeration e = nextInterceptor.list( base );
        Invocation invocation = InvocationStack.getInstance().peek();
        return new SearchResultFilteringEnumeration( e, new SearchControls(), invocation, binaryAttributeFilter );
    }


    public NamingEnumeration search( NextInterceptor nextInterceptor,
                                     Name base, Map env, ExprNode filter,
                                     SearchControls searchCtls ) throws NamingException
    {
        // check to make sure the DN searched for is a subentry
        if ( !subschemaSubentryDn.equals( base.toString() ) )
        {
            NamingEnumeration e = nextInterceptor.search( base, env, filter, searchCtls );
            if ( searchCtls.getReturningAttributes() != null )
            {
                return e;
            }

            Invocation invocation = InvocationStack.getInstance().peek();
            return new SearchResultFilteringEnumeration( e, searchCtls, invocation, filters );
        }

        if ( searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE &&
                filter instanceof SimpleNode )
        {
            SimpleNode node = ( SimpleNode ) filter;

            if ( node.getAttribute().equalsIgnoreCase( "objectClass" ) &&
                    node.getValue().equalsIgnoreCase( "subschema" ) &&
                    node.getAssertionType() == SimpleNode.EQUALITY
            )
            {
                // call.setBypass( true );
                Attributes attrs = getSubschemaEntry( searchCtls.getReturningAttributes() );
                SearchResult result = new SearchResult( base.toString(), null, attrs );
                return new SingletonEnumeration( result );
            }
        }
        else if ( searchCtls.getSearchScope() == SearchControls.OBJECT_SCOPE &&
                    filter instanceof PresenceNode )
        {
            PresenceNode node = ( PresenceNode ) filter;

            if ( node.getAttribute().equalsIgnoreCase( "objectClass" ) )
            {
                // call.setBypass( true );
                Attributes attrs = getSubschemaEntry( searchCtls.getReturningAttributes() );
                SearchResult result = new SearchResult( base.toString(), null, attrs );
                return new SingletonEnumeration( result );
            }
        }

        NamingEnumeration e = nextInterceptor.search( base, env, filter, searchCtls );
        if ( searchCtls.getReturningAttributes() != null )
        {
            return e;
        }

        Invocation invocation = InvocationStack.getInstance().peek();
        return new SearchResultFilteringEnumeration( e, searchCtls, invocation, filters );
    }


    private Attributes getSubschemaEntry( String[] ids ) throws NamingException
    {
        if ( ids == null )
        {
            ids = EMPTY_STRING_ARRAY;
        }

        Set set = new HashSet();
        LockableAttributesImpl attrs = new LockableAttributesImpl();
        LockableAttributeImpl attr;

        for ( int ii = 0; ii < ids.length; ii++ )
        {
            set.add( ids[ii].toLowerCase() );
        }

        // Check whether the set contains a plus, and use it below to include all
        // operational attributes.  Due to RFC 3673, and issue DIREVE-228 in JIRA
        boolean returnAllOperationalAttributes = set.contains( "+" );

        if ( returnAllOperationalAttributes || set.contains( "objectclasses" ) )
        {
            attr = new LockableAttributeImpl( "objectClasses" );
            Iterator list = globalRegistries.getObjectClassRegistry().list();
            while ( list.hasNext() )
            {
                ObjectClass oc = ( ObjectClass ) list.next();
                attr.add( SchemaUtils.render( oc ).toString() );
            }
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "attributetypes" ) )
        {
            attr = new LockableAttributeImpl( "attributeTypes" );
            Iterator list = globalRegistries.getAttributeTypeRegistry().list();
            while ( list.hasNext() )
            {
                AttributeType at = ( AttributeType ) list.next();
                attr.add( SchemaUtils.render( at ).toString() );
            }
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "matchingrules" ) )
        {
            attr = new LockableAttributeImpl( "matchingRules" );
            Iterator list = globalRegistries.getMatchingRuleRegistry().list();
            while ( list.hasNext() )
            {
                MatchingRule mr = ( MatchingRule ) list.next();
                attr.add( SchemaUtils.render( mr ).toString() );
            }
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "matchingruleuse" ) )
        {
            attr = new LockableAttributeImpl( "matchingRuleUse" );
            Iterator list = globalRegistries.getMatchingRuleUseRegistry().list();
            while ( list.hasNext() )
            {
                MatchingRuleUse mru = ( MatchingRuleUse ) list.next();
                attr.add( SchemaUtils.render( mru ).toString() );
            }
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "ldapsyntaxes" ) )
        {
            attr = new LockableAttributeImpl( "ldapSyntaxes" );
            Iterator list = globalRegistries.getSyntaxRegistry().list();
            while ( list.hasNext() )
            {
                Syntax syntax = ( Syntax ) list.next();
                attr.add( SchemaUtils.render( syntax ).toString() );
            }
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "ditcontentrules" ) )
        {
            attr = new LockableAttributeImpl( "dITContentRules" );
            Iterator list = globalRegistries.getDitContentRuleRegistry().list();
            while ( list.hasNext() )
            {
                DITContentRule dcr = ( DITContentRule ) list.next();
                attr.add( SchemaUtils.render( dcr ).toString() );
            }
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "ditstructurerules" ) )
        {
            attr = new LockableAttributeImpl( "dITStructureRules" );
            Iterator list = globalRegistries.getDitStructureRuleRegistry().list();
            while ( list.hasNext() )
            {
                DITStructureRule dsr = ( DITStructureRule ) list.next();
                attr.add( SchemaUtils.render( dsr ).toString() );
            }
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "nameforms" ) )
        {
            attr = new LockableAttributeImpl( "nameForms" );
            Iterator list = globalRegistries.getNameFormRegistry().list();
            while ( list.hasNext() )
            {
                NameForm nf = ( NameForm ) list.next();
                attr.add( SchemaUtils.render( nf ).toString() );
            }
            attrs.put( attr );
        }

        // timeestamps are hacks for now until the schema is actually updateable these
        // use the servers startup time stamp for both modify and create timestamps


        if ( returnAllOperationalAttributes || set.contains( "createtimestamp" ) )
        {
            attr = new LockableAttributeImpl( "createTimestamp" );
            attr.add( startUpTimeStamp );
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "modifytimestamp" ) )
        {
            attr = new LockableAttributeImpl( "modifyTimestamp" );
            attr.add( startUpTimeStamp );
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "creatorsname" ) )
        {
            attr = new LockableAttributeImpl( "creatorsName" );
            attr.add( DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attrs.put( attr );
        }

        if ( returnAllOperationalAttributes || set.contains( "modifiersname" ) )
        {
            attr = new LockableAttributeImpl( "modifiersName" );
            attr.add( DirectoryPartitionNexus.ADMIN_PRINCIPAL );
            attrs.put( attr );
        }

        int minSetSize = 0;
        if ( set.contains( "+" ) )
        {
            minSetSize++;
        }
        if ( set.contains( "*" ) )
        {
            minSetSize++;
        }
        if ( set.contains( "ref" ) )
        {
            minSetSize++;
        }

        // add the objectClass attribute
        if ( set.contains( "*" ) || set.contains( "objectclass" ) || set.size() == minSetSize )
        {
            attr = new LockableAttributeImpl( "objectClass" );
            attr.add( "top" );
            attr.add( "subschema" );
            attrs.put( attr );
        }

        // add the cn attribute as required for the RDN
        if ( set.contains( "*" ) || set.contains( "cn" ) || set.contains( "commonname" ) || set.size() == minSetSize )
        {
            attrs.put( "cn", "schema" );
        }

        return attrs;
    }


    public Attributes lookup( NextInterceptor nextInterceptor, Name name ) throws NamingException
    {
        Attributes result = nextInterceptor.lookup( name );
        filterBinaryAttributes( result );
        filterTop( result );
        return result;
    }


    public Attributes lookup( NextInterceptor nextInterceptor, Name name, String[] attrIds ) throws NamingException
    {
        Attributes result = nextInterceptor.lookup( name, attrIds );
        if ( result == null )
        {
            return null;
        }

        filterBinaryAttributes( result );
        filterTop( result );
        return result;
    }


    /**
     * Checks to see if an attribute is required by as determined from an entry's
     * set of objectClass attribute values.
     *
     * @param attrId the attribute to test if required by a set of objectClass values
     * @param objectClass the objectClass values
     * @return true if the objectClass values require the attribute, false otherwise
     * @throws NamingException if the attribute is not recognized
     */
    private boolean isRequired( String attrId, Attribute objectClass ) throws NamingException
    {
        OidRegistry oidRegistry = globalRegistries.getOidRegistry();
        ObjectClassRegistry registry = globalRegistries.getObjectClassRegistry();

        if ( ! oidRegistry.hasOid( attrId ) )
        {
            return false;
        }

        String attrOid = oidRegistry.getOid( attrId );
        for ( int ii = 0; ii < objectClass.size(); ii++ )
        {
            ObjectClass ocSpec = registry.lookup( ( String ) objectClass.get( ii ) );
            AttributeType[] mustList = ocSpec.getMustList();
            for ( int jj = 0; jj < mustList.length; jj++ )
            {
                if ( mustList[jj].getOid().equals( attrOid ) )
                {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * Checks to see if removing a set of attributes from an entry completely removes
     * that attribute's values.  If change has zero size then all attributes are
     * presumed to be removed.
     *
     * @param change
     * @param entry
     * @return
     * @throws NamingException
     */
    private boolean isCompleteRemoval( Attribute change, Attributes entry ) throws NamingException
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
        Attribute changedEntryAttr = ( Attribute ) entry.get( change.getID() ).clone();
        for ( int jj = 0; jj < change.size(); jj++ )
        {
            changedEntryAttr.remove( change.get( jj ) );
        }

        return changedEntryAttr.size() == 0;
    }


    Attribute getResultantObjectClasses( int modOp, Attribute changes, Attribute existing ) throws NamingException
    {
        if ( changes == null && existing == null )
        {
            return new LockableAttributeImpl( "objectClass" );
        }

        if ( changes == null )
        {
            return existing;
        }

        if ( existing == null && modOp == DirContext.ADD_ATTRIBUTE )
        {
            return changes;
        }
        else if ( existing == null )
        {
            return new LockableAttributeImpl( "objectClasses" );
        }

        switch( modOp )
        {
            case( DirContext.ADD_ATTRIBUTE ):
                return AttributeUtils.getUnion( existing, changes );
            case( DirContext.REPLACE_ATTRIBUTE ):
                return ( Attribute ) changes.clone();
            case( DirContext.REMOVE_ATTRIBUTE ):
                return AttributeUtils.getDifference( existing, changes );
            default:
                throw new InternalError( "" );
        }
    }
    
    
    /**
     * Given the objectClasses for an entry, this method adds missing ancestors 
     * in the hierarchy except for top which it removes.  This is used for this
     * solution to DIREVE-276.  More information about this solution can be found
     * <a href="http://docs.safehaus.org:8080/x/kBE">here</a>.
     * 
     * @param objectClassAttr the objectClass attribute to modify
     * @throws NamingException if there are problems 
     */
    public static void alterObjectClasses( Attribute objectClassAttr, ObjectClassRegistry registry ) throws NamingException
    {
        if ( ! objectClassAttr.getID().equalsIgnoreCase( "objectClass" ) )
        {
            throw new LdapNamingException( "Expecting an objectClass attribute but got " + objectClassAttr.getID(), 
                ResultCodeEnum.OPERATIONSERROR );
        }
        
        Set objectClasses = new HashSet();
        for ( int ii = 0; ii < objectClassAttr.size(); ii++ )
        {
            String val = ( String ) objectClassAttr.get( ii );
            if ( ! val.equalsIgnoreCase( "top" ) )
            {
                objectClasses.add( val.toLowerCase() );
            }
        }
        
        for ( int ii = 0; ii < objectClassAttr.size(); ii++ )
        {
            String val = ( String ) objectClassAttr.get( ii );
            if ( val.equalsIgnoreCase( "top" ) )
            {
                objectClassAttr.remove( val );
            }
            
            ObjectClass objectClass = registry.lookup( val );
            
            // cannot use Collections.addAll(Collection, Object[]) since it's 1.5
            ObjectClass top = registry.lookup( "top" );
            ObjectClass[] superiors = objectClass.getSuperClasses();
            for ( int jj = 0; jj < superiors.length; jj++ )
            {
                if ( superiors[jj] != top && ! objectClasses.contains( superiors[jj].getName().toLowerCase() ) )
                {
                    objectClassAttr.add( superiors[jj].getName() );
                }
            }
        }
    }


    public void modify( NextInterceptor next, Name name, int modOp, Attributes mods ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Attribute objectClass = getResultantObjectClasses( modOp, mods.get( "objectClass"), entry.get( "objectClass" ) );
        ObjectClassRegistry ocRegistry = this.globalRegistries.getObjectClassRegistry();
        AttributeTypeRegistry atRegistry = this.globalRegistries.getAttributeTypeRegistry();

        NamingEnumeration changes = mods.getIDs();
        while ( changes.hasMore() )
        {
            String id = ( String ) changes.next();
            Attribute change = mods.get( id );

            if ( ! atRegistry.hasAttributeType( change.getID() ) && ! objectClass.contains( "extensibleObject" ) )
            {
                throw new LdapInvalidAttributeIdentifierException( "unrecognized attributeID " + change.getID() );
            }

            if ( modOp == DirContext.REMOVE_ATTRIBUTE && entry.get( change.getID() ) == null )
            {
                throw new LdapNoSuchAttributeException();
            }

            // for required attributes we need to check if all values are removed
            // if so then we have a schema violation that must be thrown
            if ( modOp == DirContext.REMOVE_ATTRIBUTE &&
                 isRequired( change.getID(), objectClass ) &&
                 isCompleteRemoval( change, entry ) )
            {
                throw new LdapSchemaViolationException( ResultCodeEnum.OBJECTCLASSVIOLATION );
            }
        }

        if ( modOp == DirContext.REMOVE_ATTRIBUTE )
        {
            SchemaChecker.preventRdnChangeOnModifyRemove( name, modOp, mods );
            SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, modOp, mods, objectClass );
        }

        if ( modOp == DirContext.REPLACE_ATTRIBUTE )
        {
            SchemaChecker.preventRdnChangeOnModifyReplace( name, modOp, mods );
            SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, modOp, mods );
        }

        // let's figure out if we need to add or take away from mods to maintain 
        // the objectClass attribute with it's hierarchy of ancestors 
        if ( mods.get( "objectClass" ) != null )
        {
            Attribute alteredObjectClass = ( Attribute ) objectClass.clone();
            alterObjectClasses( alteredObjectClass, ocRegistry );
            
            if ( ! alteredObjectClass.equals( objectClass ) )
            {
                Attribute ocMods = mods.get( "objectClass" );
                switch( modOp )
                {
                    case( DirContext.ADD_ATTRIBUTE ):
                        if ( ocMods.contains( "top" ) )
                        {
                            ocMods.remove( "top" );
                        }
                        for ( int ii = 0; ii < alteredObjectClass.size(); ii++ )
                        {
                            if ( ! objectClass.contains( alteredObjectClass.get( ii ) ) )
                            {
                                ocMods.add( alteredObjectClass.get( ii ) );
                            }
                        }
                        break;
                    case( DirContext.REMOVE_ATTRIBUTE ):
                        for ( int ii = 0; ii < alteredObjectClass.size(); ii++ )
                        {
                            if ( ! objectClass.contains( alteredObjectClass.get( ii ) ) )
                            {
                                ocMods.remove( alteredObjectClass.get( ii ) );
                            }
                        }
                        break;
                    case( DirContext.REPLACE_ATTRIBUTE ):
                        for ( int ii = 0; ii < alteredObjectClass.size(); ii++ )
                        {
                            if ( ! objectClass.contains( alteredObjectClass.get( ii ) ) )
                            {
                                ocMods.add( alteredObjectClass.get( ii ) );
                            }
                        }
                        break;
                    default:
                }
            }
        }
        
        next.modify( name, modOp, mods );
    }


    public void modify( NextInterceptor next, Name name, ModificationItem[] mods ) throws NamingException
    {
        Attributes entry = nexus.lookup( name );
        Set modset = new HashSet();
        ModificationItem objectClassMod = null;
                    
        for ( int ii = 0; ii < mods.length; ii++ )
        {
            if ( mods[ii].getAttribute().getID().equalsIgnoreCase( "objectclass" ) )
            {
                objectClassMod = mods[ii];
            }
            
            StringBuffer keybuf = new StringBuffer();
            keybuf.append( mods[ii].getModificationOp() );
            keybuf.append( mods[ii].getAttribute().getID() );
            for ( int jj = 0; jj < mods[ii].getAttribute().size(); jj++ )
            {
                keybuf.append( mods[ii].getAttribute().get( jj ) );
            }
            if ( ! modset.add( keybuf.toString() ) && mods[ii].getModificationOp() == DirContext.ADD_ATTRIBUTE )
            {
                throw new LdapAttributeInUseException( "found two copies of the following modification item: " 
                    + mods[ii] );
            }
        }
        Attribute objectClass;

        if ( objectClassMod == null )
        {
            objectClass = entry.get( "objectClass" );
        }
        else
        {
            objectClass = getResultantObjectClasses( objectClassMod.getModificationOp(),
                    objectClassMod.getAttribute(), entry.get( "objectClass" ) );
        }

        ObjectClassRegistry ocRegistry = this.globalRegistries.getObjectClassRegistry();
        AttributeTypeRegistry atRegistry = this.globalRegistries.getAttributeTypeRegistry();

        for ( int ii = 0; ii < mods.length; ii++ )
        {
            int modOp = mods[ii].getModificationOp();
            Attribute change = mods[ii].getAttribute();

            if ( ! atRegistry.hasAttributeType( change.getID() ) && ! objectClass.contains( "extensibleObject" ) )
            {
                throw new LdapInvalidAttributeIdentifierException();
            }

            if ( modOp == DirContext.REMOVE_ATTRIBUTE && entry.get( change.getID() ) == null )
            {
                throw new LdapNoSuchAttributeException();
            }

            if ( modOp == DirContext.REMOVE_ATTRIBUTE )
            {
                // for required attributes we need to check if all values are removed
                // if so then we have a schema violation that must be thrown
                if ( isRequired( change.getID(), objectClass ) && isCompleteRemoval( change, entry ) )
                {
                    throw new LdapSchemaViolationException( ResultCodeEnum.OBJECTCLASSVIOLATION );
                }
                SchemaChecker.preventRdnChangeOnModifyRemove( name, modOp, change );
                SchemaChecker.preventStructuralClassRemovalOnModifyRemove( ocRegistry, name, modOp, change, objectClass );
            }

            if ( modOp == DirContext.REPLACE_ATTRIBUTE )
            {
                SchemaChecker.preventRdnChangeOnModifyReplace( name, modOp, change );
                SchemaChecker.preventStructuralClassRemovalOnModifyReplace( ocRegistry, name, modOp, change );
            }
        }

        // let's figure out if we need to add or take away from mods to maintain 
        // the objectClass attribute with it's hierarchy of ancestors 
        if ( objectClassMod != null )
        {
            Attribute alteredObjectClass = ( Attribute ) objectClass.clone();
            alterObjectClasses( alteredObjectClass, ocRegistry );
            
            if ( ! alteredObjectClass.equals( objectClass ) )
            {
                Attribute ocMods = objectClassMod.getAttribute();
                switch( objectClassMod.getModificationOp() )
                {
                    case( DirContext.ADD_ATTRIBUTE ):
                        if ( ocMods.contains( "top" ) )
                        {
                            ocMods.remove( "top" );
                        }
                        for ( int ii = 0; ii < alteredObjectClass.size(); ii++ )
                        {
                            if ( ! objectClass.contains( alteredObjectClass.get( ii ) ) )
                            {
                                ocMods.add( alteredObjectClass.get( ii ) );
                            }
                        }
                        break;
                    case( DirContext.REMOVE_ATTRIBUTE ):
                        for ( int ii = 0; ii < alteredObjectClass.size(); ii++ )
                        {
                            if ( ! objectClass.contains( alteredObjectClass.get( ii ) ) )
                            {
                                ocMods.remove( alteredObjectClass.get( ii ) );
                            }
                        }
                        break;
                    case( DirContext.REPLACE_ATTRIBUTE ):
                        for ( int ii = 0; ii < alteredObjectClass.size(); ii++ )
                        {
                            if ( ! objectClass.contains( alteredObjectClass.get( ii ) ) )
                            {
                                ocMods.add( alteredObjectClass.get( ii ) );
                            }
                        }
                        break;
                    default:
                }
            }
        }

        next.modify( name, mods );
    }

    
    private void filterTop( Attributes entry ) throws NamingException
    {
        // add top if objectClass is included and missing top
        Attribute oc = entry.get( "objectClass" );
        if ( oc != null )
        {
            if ( ! oc.contains( "top" ) )
            {
                oc.add( "top" );
            }
        }
    }


    private void filterBinaryAttributes( Attributes entry ) throws NamingException
    {
        long t0 = -1;
        
        if ( log.isDebugEnabled() )
        {
            t0 = System.currentTimeMillis();
            log.debug( "Filtering entry " + AttributeUtils.toString( entry ) );  
        }
        
        /*
         * start converting values of attributes to byte[]s which are not
         * human readable and those that are in the binaries set
         */
        NamingEnumeration list = entry.getIDs();
        while ( list.hasMore() )
        {
            String id = ( String ) list.next();
            AttributeType type = null;
            boolean asBinary = false;

            if ( globalRegistries.getAttributeTypeRegistry().hasAttributeType( id ) )
            {
                type = globalRegistries.getAttributeTypeRegistry().lookup( id );
            }

            if ( type != null )
            {
                asBinary = !type.getSyntax().isHumanReadible();
                asBinary = asBinary || binaries.contains( type );
            }

            if ( asBinary )
            {
                Attribute attribute = entry.get( id );
                Attribute binary = new LockableAttributeImpl( id );
                for ( int i = 0; i < attribute.size(); i++ )
                {
                    Object value = attribute.get( i );
                    if ( value instanceof String )
                    {
                        binary.add( i, StringTools.getBytesUtf8( ( String ) value ) );
                    }
                    else
                    {
                        binary.add( i, value );
                    }
                }

                entry.remove( id );
                entry.put( binary );
            }
        }
        
        if ( log.isDebugEnabled() )
        {
            long t1 = System.currentTimeMillis();
            log.debug( "Time to filter entry = " + (t1 - t0) + " ms" );
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
    private class BinaryAttributeFilter implements SearchResultFilter
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls ) throws NamingException
        {
            filterBinaryAttributes( result.getAttributes() );
            return true;
        }
    }

    /**
     * Filters objectClass attribute to inject top when not present.
     */
    private class TopFilter implements SearchResultFilter
    {
        public boolean accept( Invocation invocation, SearchResult result, SearchControls controls ) throws NamingException
        {
            filterTop( result.getAttributes() );
            return true;
        }
    }

    /**
     * Check that all the attributes exist in the schema for this entry.
     */
    public void add( NextInterceptor next, String upName, Name normName, Attributes attrs ) throws NamingException
    {
        AttributeTypeRegistry atRegistry = this.globalRegistries.getAttributeTypeRegistry();
        NamingEnumeration attrEnum = attrs.getIDs();
        while ( attrEnum.hasMoreElements() )
        {
            String name = (String)attrEnum.nextElement();
            if ( ! atRegistry.hasAttributeType( name ) )
            {
                throw new LdapInvalidAttributeIdentifierException( name + " not found in attribute registry!" );
            }
        }
        
        alterObjectClasses( attrs.get( "objectClass" ), this.globalRegistries.getObjectClassRegistry() );
        next.add( upName, normName, attrs );
    }
}
