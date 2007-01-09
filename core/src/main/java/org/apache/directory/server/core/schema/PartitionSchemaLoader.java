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


import java.util.LinkedList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;
import org.apache.directory.shared.ldap.util.AttributeUtils;
    
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

    
    public PartitionSchemaLoader( Partition partition, Registries bootstrapRegistries ) throws NamingException
    {
        this.factory = new SchemaEntityFactory( bootstrapRegistries );
        this.partition = partition;
        this.attrRegistry = bootstrapRegistries.getAttributeTypeRegistry();
        
        dao = new SchemaPartitionDao( this.partition, bootstrapRegistries );
        mOidAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_OID_AT );
        mNameAT = bootstrapRegistries.getAttributeTypeRegistry().lookup( MetaSchemaConstants.M_NAME_AT );
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
                log.info( "will attempt to load enabled schema: {}", schema.getSchemaName() );
                enabledSchemaSet.add( schema );
            }
            else
            {
                log.info( "will NOT attempt to load disabled schema: {}", schema.getSchemaName() );
            }
        }

        loadWithDependencies( enabledSchemaSet, targetRegistries );
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
            loadDepsFirst( new Stack<String>(), notLoaded, schema, targetRegistries, null );
            list = notLoaded.values().iterator();
        }
    }


    public final void load( Schema schema, Registries targetRegistries ) throws NamingException
    {
        if ( targetRegistries.getLoadedSchemas().containsKey( schema.getSchemaName() ) )
        {
            log.debug( "schema {} already seems to be loaded", schema.getSchemaName() );
            return;
        }
        
        log.info( "loading {} schema ...", schema.getSchemaName() );
        
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
        LdapDN dn = new LdapDN( "ou=objectClasses,cn=" + schema.getSchemaName() + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.info( "{} schema: loading objectClasses", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            ObjectClass oc = factory.getObjectClass( attrs, targetRegistries );
            
            try
            {
                targetRegistries.getObjectClassRegistry().register( schema.getSchemaName(), oc );
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
                targetRegistries.getObjectClassRegistry().register( schema.getSchemaName(), oc );
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
        LdapDN dn = new LdapDN( "ou=attributeTypes,cn=" + schema.getSchemaName() + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.info( "{} schema: loading attributeTypes", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            AttributeType at = factory.getAttributeType( attrs, targetRegistries );
            try
            {
                targetRegistries.getAttributeTypeRegistry().register( schema.getSchemaName(), at );
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
                targetRegistries.getAttributeTypeRegistry().register( schema.getSchemaName(), at );
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
        LdapDN dn = new LdapDN( "ou=matchingRules,cn=" + schema.getSchemaName() + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.info( "{} schema: loading matchingRules", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            MatchingRule mrule = factory.getMatchingRule( attrs, targetRegistries );
            targetRegistries.getMatchingRuleRegistry().register( schema.getSchemaName(), mrule );

        }
    }


    private void loadSyntaxes( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=syntaxes,cn=" + schema.getSchemaName() + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.info( "{} schema: loading syntaxes", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            Syntax syntax = factory.getSyntax( attrs, targetRegistries );
            targetRegistries.getSyntaxRegistry().register( schema.getSchemaName(), syntax );
        }
    }


    private void loadSyntaxCheckers( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=syntaxCheckers,cn=" + schema.getSchemaName() + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.info( "{} schema: loading syntaxCheckers", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            SyntaxChecker sc = factory.getSyntaxChecker( attrs, targetRegistries );
            targetRegistries.getSyntaxCheckerRegistry().register( schema.getSchemaName(), sc );
        }
    }


    private void loadNormalizers( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=normalizers,cn=" + schema.getSchemaName() + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );
        
        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.info( "{} schema: loading normalizers", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            Normalizer normalizer = factory.getNormalizer( attrs, targetRegistries );
            String oid = ( String ) attrs.get( "m-oid" ).get();
            targetRegistries.getNormalizerRegistry().register( schema.getSchemaName(), oid, normalizer );
        }
    }


    private void loadComparators( Schema schema, Registries targetRegistries ) throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=comparators,cn=" + schema.getSchemaName() + ",ou=schema" );
        dn.normalize( this.attrRegistry.getNormalizerMapping() );

        if ( ! partition.hasEntry( dn ) )
        {
            return;
        }
        
        log.info( "{} schema: loading comparators", schema.getSchemaName() );
        
        NamingEnumeration list = partition.list( dn );
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            LdapDN resultDN = new LdapDN( result.getName() );
            resultDN.normalize( attrRegistry.getNormalizerMapping() );
            Attributes attrs = partition.lookup( resultDN );
            Comparator comparator = factory.getComparator( attrs, targetRegistries );
            String oid = ( String ) attrs.get( "m-oid" ).get();
            targetRegistries.getComparatorRegistry().register( schema.getSchemaName(), oid, comparator );
        }
    }


    public void loadWithDependencies( Schema schema, Registries registries ) throws NamingException
    {
        HashMap<String,Schema> notLoaded = new HashMap<String,Schema>();
        notLoaded.put( schema.getSchemaName(), schema );                        
        Properties props = new Properties();
        loadDepsFirst( new Stack<String>(), notLoaded, schema, registries, props );
    }
}
