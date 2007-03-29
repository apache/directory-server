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


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.constants.MetaSchemaConstants;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.server.schema.registries.AbstractSchemaLoader;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.syntax.ComparatorDescription;
import org.apache.directory.shared.ldap.schema.syntax.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.Base64;
    
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class that loads schemas from a partition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PartitionSchemaLoader extends AbstractSchemaLoader
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( PartitionSchemaLoader.class );
    
    private final SchemaPartitionDao dao;
    private SchemaEntityFactory factory;
    private Partition partition;
    private AttributeTypeRegistry attrRegistry;
    private final AttributeType mOidAT;
    private final AttributeType mNameAT;
    private final AttributeType cnAT;
    private final AttributeType byteCodeAT;
    private final AttributeType descAT;
    private final AttributeType fqcnAT;

    private static Map<String, LdapDN> staticAttributeTypeDNs = new HashMap<String, LdapDN>();
    private static Map<String, LdapDN> staticMatchingRulesDNs = new HashMap<String, LdapDN>();
    private static Map<String, LdapDN> staticObjectClassesDNs = new HashMap<String, LdapDN>();
    private static Map<String, LdapDN> staticComparatorsDNs = new HashMap<String, LdapDN>();
    private static Map<String, LdapDN> staticNormalizersDNs = new HashMap<String, LdapDN>();
    private static Map<String, LdapDN> staticSyntaxCheckersDNs = new HashMap<String, LdapDN>();
    private static Map<String, LdapDN> staticSyntaxesDNs = new HashMap<String, LdapDN>();
    
    public PartitionSchemaLoader( Partition partition, Registries bootstrapRegistries ) throws NamingException
    {
        this.factory = new SchemaEntityFactory( bootstrapRegistries );
        this.partition = partition;
        this.attrRegistry = bootstrapRegistries.getAttributeTypeRegistry();
        
        this.dao = new SchemaPartitionDao( this.partition, bootstrapRegistries );
        this.mOidAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
        this.mNameAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_NAME_AT );
        this.cnAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( SchemaConstants.CN_AT );
        this.byteCodeAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_BYTECODE_AT );
        this.descAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_DESCRIPTION_AT );
        this.fqcnAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_FQCN_AT );
        
        initStaticDNs( "system" );
        initStaticDNs( "core" );
        initStaticDNs( "apache" );
        initStaticDNs( "apachemeta" );
        initStaticDNs( "other" );
        initStaticDNs( "collective" );
        initStaticDNs( "java" );
        initStaticDNs( "cosine" );
        initStaticDNs( "inetorgperson" );
    }
    
    private void initStaticDNs( String schemaName ) throws NamingException
    {
        
        // Initialize AttributeType Dns
        LdapDN dn = new LdapDN( "ou=attributeTypes,cn=" + schemaName + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        staticAttributeTypeDNs.put( schemaName, dn );

        // Initialize ObjectClasses Dns
        dn = new LdapDN( "ou=objectClasses,cn=" + schemaName + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        staticObjectClassesDNs.put( schemaName, dn );

        // Initialize MatchingRules Dns
        dn = new LdapDN( "ou=matchingRules,cn=" + schemaName + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        staticMatchingRulesDNs.put( schemaName, dn );

        // Initialize Comparators Dns
        dn = new LdapDN( "ou=comparators,cn=" + schemaName + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        staticComparatorsDNs.put( schemaName, dn );
        
        // Initialize Normalizers Dns
        dn = new LdapDN( "ou=normalizers,cn=" + schemaName + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        staticNormalizersDNs.put( schemaName, dn );

        // Initialize SyntaxCheckers Dns
        dn = new LdapDN( "ou=syntaxCheckers,cn=" + schemaName + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        staticSyntaxCheckersDNs.put( schemaName, dn );

        // Initialize Syntaxes Dns
        dn = new LdapDN( "ou=syntaxes,cn=" + schemaName + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        staticSyntaxesDNs.put( schemaName, dn );

    }
    
    /**
     * Utility method to load all enabled schemas into this registry.
     * 
     * @param targetRegistries
     * @throws NamingException
     */
    public void loadEnabled( Registries targetRegistries ) throws NamingException
    {
        /* 
         * We need to load all names and oids into the oid registry regardless of
         * the entity being in an enabled schema.  This is necessary because we 
         * search for values in the schema partition that represent matchingRules
         * and other entities that are not loaded.  While searching these values
         * in disabled schemas normalizers will attempt to equate names with oids
         * and if there is an unrecognized value by a normalizer then the search 
         * will fail.
         * 
         * For example there is a NameOrNumericOidNormalizer that will reduce a 
         * numeric OID or a non-numeric OID to it's numeric form using the OID 
         * registry.  While searching the schema partition for attributeTypes we
         * might find values of matchingRules in the m-ordering, m-equality, and
         * m-substr attributes of metaAttributeType definitions.  Now if an entry
         * references a matchingRule that has not been loaded then the 
         * NameOrNumericOidNormalizer will bomb out when it tries to resolve 
         * names of matchingRules in unloaded schemas to OID values using the 
         * OID registry.  To prevent this we need to load all the OID's in advance
         * regardless of whether they are used or not.
         */
        NamingEnumeration ne = dao.listAllNames();
        while ( ne.hasMore() )
        {
            Attributes attrs = ( ( SearchResult ) ne.next() ).getAttributes();
            String oid = ( String ) AttributeUtils.getAttribute( attrs, mOidAT ).get();
            Attribute names = AttributeUtils.getAttribute( attrs, mNameAT );
            targetRegistries.getOidRegistry().register( oid, oid );
            for ( int ii = 0; ii < names.size(); ii++ )
            {
                targetRegistries.getOidRegistry().register( ( String ) names.get( ii ), oid );
            }
        }
        ne.close();
        
        
        Map<String, Schema> allSchemaMap = getSchemas();
        Set<Schema> enabledSchemaSet = new HashSet<Schema>();

        for ( Schema schema: allSchemaMap.values() )
        {
            if ( ! schema.isDisabled() )
            {
                log.debug( "will attempt to load enabled schema: {}", schema.getSchemaName() );
                    
                enabledSchemaSet.add( schema );
            }
            else
            {
                log.debug( "will NOT attempt to load disabled schema: {}", schema.getSchemaName() );
            }
        }

        loadWithDependencies( enabledSchemaSet, targetRegistries );
    }
    
    
    /**
     * Lists the names of the schemas that depend on the schema name provided.
     * 
     * @param schemaName the name of the schema to find dependents for
     * @return a set of schemas (String names) that depend on the schema
     * @throws NamingException if there are problems searching the schema partition
     */
    public Set<String> listDependentSchemaNames( String schemaName ) throws NamingException
    {
        Set<String> dependees = new HashSet<String>();
        Set<SearchResult> results = dao.listSchemaDependents( schemaName );
        
        if ( results.isEmpty() )
        {
            return dependees;
        }
        
        for ( SearchResult sr: results )
        {
            Attribute cn = AttributeUtils.getAttribute( sr.getAttributes(), cnAT );
            dependees.add( ( String ) cn.get() );
        }
        
        return dependees;
    }

    
    /**
     * Lists the names of the enabled schemas that depend on the schema name 
     * provided.
     * 
     * @param schemaName the name of the schema to find dependents for
     * @return a set of enabled schemas (String names) that depend on the schema
     * @throws NamingException if there are problems searching the schema partition
     */
    public Set<String> listEnabledDependentSchemaNames( String schemaName ) throws NamingException
    {
        Set<String> dependees = new HashSet<String>();
        Set<SearchResult> results = dao.listEnabledSchemaDependents( schemaName );
        
        if ( results.isEmpty() )
        {
            return dependees;
        }
        
        for ( SearchResult sr: results )
        {
            Attribute cn = AttributeUtils.getAttribute( sr.getAttributes(), cnAT );
            dependees.add( ( String ) cn.get() );
        }
        
        return dependees;
    }

    
    public Map<String,Schema> getSchemas() throws NamingException
    {
        return dao.getSchemas();
    }

    
    public Set<String> getSchemaNames() throws NamingException
    {
        return dao.getSchemaNames();
    }
    
    
    public Schema getSchema( String schemaName ) throws NamingException
    {
        return dao.getSchema( schemaName );
    }


    public Schema getSchema( String schemaName, Properties schemaProperties ) throws NamingException
    {
        return getSchema( schemaName );
    }


    public final void loadWithDependencies( Collection<Schema> schemas, Registries targetRegistries ) throws NamingException
    {
        HashMap<String,Schema> notLoaded = new HashMap<String,Schema>();
        Iterator<Schema> list = schemas.iterator();
        
        while ( list.hasNext() )
        {
            Schema schema = list.next();
            notLoaded.put( schema.getSchemaName(), schema );
        }

        list = notLoaded.values().iterator();
        while ( list.hasNext() )
        {
            Schema schema = ( Schema ) list.next();
            loadDepsFirst( schema, new Stack<String>(), notLoaded, schema, targetRegistries, null );
            list = notLoaded.values().iterator();
        }
    }

    /**
     * {@link SchemaLoader#load(Schema, Registries, boolean)}
     */
    public final void load( Schema schema, Registries targetRegistries, boolean isDepLoad ) throws NamingException
    {
        // if we're loading a dependency and it has not been enabled on 
        // disk then enable it on disk before we proceed to load it
        if ( schema.isDisabled() && isDepLoad )
        {
            dao.enableSchema( schema.getSchemaName() );
        }
        
        if ( targetRegistries.getLoadedSchemas().containsKey( schema.getSchemaName() ) )
        {
            log.debug( "schema {} already seems to be loaded", schema.getSchemaName() );
            return;
        }
        
        log.debug( "loading {} schema ...", schema.getSchemaName() );
        
        loadComparators( schema, targetRegistries );
        loadNormalizers( schema, targetRegistries );
        loadSyntaxCheckers( schema, targetRegistries );
        loadSyntaxes( schema, targetRegistries );
        loadMatchingRules( schema, targetRegistries );
        loadAttributeTypes( schema, targetRegistries );
        loadObjectClasses( schema, targetRegistries );
        loadMatchingRuleUses( schema, targetRegistries );
        loadDitContentRules( schema, targetRegistries );
        loadNameForms( schema, targetRegistries );
        
        // order does matter here so some special trickery is needed
        // we cannot load a DSR before the DSRs it depends on are loaded?
        // TODO need ot confirm this ( or we must make the class for this and use deferred 
        // resolution until everything is available?
        
        loadDitStructureRules( schema, targetRegistries );
        
        
        notifyListenerOrRegistries( schema, targetRegistries );
    }

    
    private void loadMatchingRuleUses( Schema schema, Registries targetRegistries )
    {
        // TODO Auto-generated method stub
    }


    private void loadDitStructureRules( Schema schema, Registries targetRegistries ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    private void loadNameForms( Schema schema, Registries targetRegistries ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    private void loadDitContentRules( Schema schema, Registries targetRegistries ) throws NamingException
    {
        // TODO Auto-generated method stub
    }


    private void loadObjectClasses( Schema schema, Registries targetRegistries ) throws NamingException
    {
        /**
         * Sometimes search may return child objectClasses before their superiors have
         * been registered like with attributeTypes.  To prevent this from bombing out
         * the loader we will defer the registration of elements until later.
         */
        LinkedList<ObjectClass> deferred = new LinkedList<ObjectClass>();

        LdapDN dn = staticObjectClassesDNs.get( schema.getSchemaName() );
        
        if ( dn == null )
        {
            dn = new LdapDN( "ou=objectClasses,cn=" + schema.getSchemaName() + ",ou=schema" );
            dn.normalize( this.attrRegistry.getNormalizerMapping() );
            staticObjectClassesDNs.put( schema.getSchemaName(), dn );
        }
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.debug( "{} schema: loading objectClasses", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            ObjectClass oc = factory.getObjectClass( attrs, targetRegistries, schema.getSchemaName() );
            
            try
            {
                targetRegistries.getObjectClassRegistry().register( oc );
            }
            catch ( NamingException ne )
            {
                deferred.add( oc );
            }
        }
        
        log.debug( "Deferred queue size = {}", deferred.size() );
        if ( log.isDebugEnabled() )
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "Deferred queue contains: " );
            
            for ( ObjectClass extra : deferred )
            {
                buf.append( extra.getName() );
                buf.append( '[' );
                buf.append( extra.getOid() );
                buf.append( "]" );
                buf.append( "\n" );
            }
        }
        
        int lastCount = deferred.size();
        while ( ! deferred.isEmpty() )
        {
            log.debug( "Deferred queue size = {}", deferred.size() );
            ObjectClass oc = deferred.removeFirst();
            NamingException lastException = null;
            
            try
            {
                targetRegistries.getObjectClassRegistry().register( oc );
            }
            catch ( NamingException ne )
            {
                deferred.addLast( oc );
                lastException = ne;
            }
            
            // if we shrank the deferred list we're doing good and can continue
            if ( deferred.size() < lastCount )
            {
                lastCount = deferred.size();
            }
            else
            {
                StringBuffer buf = new StringBuffer();
                buf.append( "A cycle must exist somewhere within the objectClasses of the " );
                buf.append( schema.getSchemaName() );
                buf.append( " schema.  We cannot seem to register the following objectClasses:\n" );
                
                for ( ObjectClass extra : deferred )
                {
                    buf.append( extra.getName() );
                    buf.append( '[' );
                    buf.append( extra.getOid() );
                    buf.append( "]" );
                    buf.append( "\n" );
                }
                
                NamingException ne = new NamingException( buf.toString() );
                ne.setRootCause( lastException );
            }
        }
    }


    private void loadAttributeTypes( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LinkedList<AttributeType> deferred = new LinkedList<AttributeType>();
        
        LdapDN dn = staticAttributeTypeDNs.get( schema.getSchemaName() );
        
        if ( dn == null )
        {
            dn = new LdapDN( "ou=attributeTypes,cn=" + schema.getSchemaName() + ",ou=schema" );
            dn.normalize( this.attrRegistry.getNormalizerMapping() );
            staticAttributeTypeDNs.put( schema.getSchemaName(), dn );
        }
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.debug( "{} schema: loading attributeTypes", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            AttributeType at = factory.getAttributeType( attrs, targetRegistries, schema.getSchemaName() );
            try
            {
                targetRegistries.getAttributeTypeRegistry().register( at );
            }
            catch ( NamingException ne )
            {
                deferred.add( at );
            }
        }

        log.debug( "Deferred queue size = {}", deferred.size() );
        if ( log.isDebugEnabled() )
        {
            StringBuffer buf = new StringBuffer();
            buf.append( "Deferred queue contains: " );
            
            for ( AttributeType extra : deferred )
            {
                buf.append( extra.getName() );
                buf.append( '[' );
                buf.append( extra.getOid() );
                buf.append( "]" );
                buf.append( "\n" );
            }
        }
        
        int lastCount = deferred.size();
        while ( ! deferred.isEmpty() )
        {
            log.debug( "Deferred queue size = {}", deferred.size() );
            AttributeType at = deferred.removeFirst();
            NamingException lastException = null;
            
            try
            {
                targetRegistries.getAttributeTypeRegistry().register( at );
            }
            catch ( NamingException ne )
            {
                deferred.addLast( at );
                lastException = ne;
            }
            
            // if we shrank the deferred list we're doing good and can continue
            if ( deferred.size() < lastCount )
            {
                lastCount = deferred.size();
            }
            else
            {
                StringBuffer buf = new StringBuffer();
                buf.append( "A cycle must exist somewhere within the attributeTypes of the " );
                buf.append( schema.getSchemaName() );
                buf.append( " schema.  We cannot seem to register the following attributeTypes:\n" );
                
                for ( AttributeType extra : deferred )
                {
                    buf.append( extra.getName() );
                    buf.append( '[' );
                    buf.append( extra.getOid() );
                    buf.append( "]" );
                    buf.append( "\n" );
                }
                
                NamingException ne = new NamingException( buf.toString() );
                ne.setRootCause( lastException );
            }
        }
    }


    private void loadMatchingRules( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LdapDN dn = staticMatchingRulesDNs.get( schema.getSchemaName() );
        
        if ( dn == null )
        {
            dn = new LdapDN( "ou=matchingRules,cn=" + schema.getSchemaName() + ",ou=schema" );
            dn.normalize( this.attrRegistry.getNormalizerMapping() );
            staticMatchingRulesDNs.put( schema.getSchemaName(), dn );
        }
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.debug( "{} schema: loading matchingRules", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            MatchingRule mrule = factory.getMatchingRule( attrs, targetRegistries, schema.getSchemaName() );
            targetRegistries.getMatchingRuleRegistry().register( mrule );

        }
    }


    private void loadSyntaxes( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LdapDN dn = staticSyntaxesDNs.get( schema.getSchemaName() );
        
        if ( dn == null )
        {
            dn = new LdapDN( "ou=syntaxes,cn=" + schema.getSchemaName() + ",ou=schema" );
            dn.normalize( this.attrRegistry.getNormalizerMapping() );
            staticSyntaxesDNs.put( schema.getSchemaName(), dn );
        }
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.debug( "{} schema: loading syntaxes", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            Syntax syntax = factory.getSyntax( attrs, targetRegistries, schema.getSchemaName() );
            targetRegistries.getSyntaxRegistry().register( syntax );
        }
    }


    private void loadSyntaxCheckers( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LdapDN dn = staticSyntaxCheckersDNs.get( schema.getSchemaName() );
        
        if ( dn == null )
        {
            dn = new LdapDN( "ou=syntaxCheckers,cn=" + schema.getSchemaName() + ",ou=schema" );
            dn.normalize( this.attrRegistry.getNormalizerMapping() );
            staticSyntaxCheckersDNs.put( schema.getSchemaName(), dn );
        }
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.debug( "{} schema: loading syntaxCheckers", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            SyntaxChecker sc = factory.getSyntaxChecker( attrs, targetRegistries );
            SyntaxCheckerDescription syntaxCheckerDescription = 
                getSyntaxCheckerDescription( schema.getSchemaName(), attrs );
            targetRegistries.getSyntaxCheckerRegistry().register( syntaxCheckerDescription, sc );
        }
    }


    private void loadNormalizers( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LdapDN dn = staticNormalizersDNs.get( schema.getSchemaName() );
        
        if ( dn == null )
        {
            dn = new LdapDN( "ou=normalizers,cn=" + schema.getSchemaName() + ",ou=schema" );
            dn.normalize( this.attrRegistry.getNormalizerMapping() );
            staticNormalizersDNs.put( schema.getSchemaName(), dn );
        }
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.debug( "{} schema: loading normalizers", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            Normalizer normalizer = factory.getNormalizer( attrs, targetRegistries );
            NormalizerDescription normalizerDescription = getNormalizerDescription( schema.getSchemaName(), attrs );
            targetRegistries.getNormalizerRegistry().register( normalizerDescription, normalizer );
        }
    }


    private String getOid( Attributes entry ) throws NamingException
    {
        Attribute oid = AttributeUtils.getAttribute( entry, mOidAT );
        if ( oid == null )
        {
            return null;
        }
        return ( String ) oid.get();
    }

    
    private NormalizerDescription getNormalizerDescription( String schemaName, Attributes entry ) throws NamingException
    {
        NormalizerDescription description = new NormalizerDescription();
        description.setNumericOid( getOid( entry ) );
        List<String> values = new ArrayList<String>();
        values.add( schemaName );
        description.addExtension( MetaSchemaConstants.X_SCHEMA, values );
        description.setFqcn( ( String ) AttributeUtils.getAttribute( entry, fqcnAT ).get() );
        
        Attribute desc = AttributeUtils.getAttribute( entry, descAT );
        if ( desc != null && desc.size() > 0 )
        {
            description.setDescription( ( String ) desc.get() );
        }
        
        Attribute bytecode = AttributeUtils.getAttribute( entry, byteCodeAT );
        if ( bytecode != null && bytecode.size() > 0 )
        {
            byte[] bytes = ( byte[] ) bytecode.get();
            description.setBytecode( new String( Base64.encode( bytes ) ) );
        }

        return description;
    }

    
    private void loadComparators( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LdapDN dn = staticComparatorsDNs.get( schema.getSchemaName() );
        
        if ( dn == null )
        {
            dn = new LdapDN( "ou=comparators,cn=" + schema.getSchemaName() + ",ou=schema" );
            dn.normalize( this.attrRegistry.getNormalizerMapping() );
            staticComparatorsDNs.put( schema.getSchemaName(), dn );
        }

        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.debug( "{} schema: loading comparators", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            Comparator comparator = factory.getComparator( attrs, targetRegistries );
            ComparatorDescription comparatorDescription = getComparatorDescription( schema.getSchemaName(), attrs );
            targetRegistries.getComparatorRegistry().register( comparatorDescription, comparator );
        }
    }


    private ComparatorDescription getComparatorDescription( String schemaName, Attributes entry ) throws NamingException
    {
        ComparatorDescription description = new ComparatorDescription();
        description.setNumericOid( getOid( entry ) );
        List<String> values = new ArrayList<String>();
        values.add( schemaName );
        description.addExtension( MetaSchemaConstants.X_SCHEMA, values );
        description.setFqcn( ( String ) AttributeUtils.getAttribute( entry, fqcnAT ).get() );
        
        Attribute desc = AttributeUtils.getAttribute( entry, descAT );
        if ( desc != null && desc.size() > 0 )
        {
            description.setDescription( ( String ) desc.get() );
        }
        
        Attribute bytecode = AttributeUtils.getAttribute( entry, byteCodeAT );
        if ( bytecode != null && bytecode.size() > 0 )
        {
            byte[] bytes = ( byte[] ) bytecode.get();
            description.setBytecode( new String( Base64.encode( bytes ) ) );
        }

        return description;
    }

    
    private SyntaxCheckerDescription getSyntaxCheckerDescription( String schemaName, Attributes entry ) 
        throws NamingException
    {
        SyntaxCheckerDescription description = new SyntaxCheckerDescription();
        description.setNumericOid( getOid( entry ) );
        List<String> values = new ArrayList<String>();
        values.add( schemaName );
        description.addExtension( MetaSchemaConstants.X_SCHEMA, values );
        description.setFqcn( ( String ) AttributeUtils.getAttribute( entry, fqcnAT ).get() );
        
        Attribute desc = AttributeUtils.getAttribute( entry, descAT );
        if ( desc != null && desc.size() > 0 )
        {
            description.setDescription( ( String ) desc.get() );
        }
        
        Attribute bytecode = AttributeUtils.getAttribute( entry, byteCodeAT );
        if ( bytecode != null && bytecode.size() > 0 )
        {
            byte[] bytes = ( byte[] ) bytecode.get();
            description.setBytecode( new String( Base64.encode( bytes ) ) );
        }

        return description;
    }

    
    public void loadWithDependencies( Schema schema, Registries registries ) throws NamingException
    {
        HashMap<String,Schema> notLoaded = new HashMap<String,Schema>();
        notLoaded.put( schema.getSchemaName(), schema );                        
        Properties props = new Properties();
        loadDepsFirst( schema, new Stack<String>(), notLoaded, schema, registries, props );
    }
}
