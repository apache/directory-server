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
package org.apache.directory.server.schema.registries;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.ComparatorRegistry;
import org.apache.directory.shared.ldap.schema.registries.DITContentRuleRegistry;
import org.apache.directory.shared.ldap.schema.registries.DITStructureRuleRegistry;
import org.apache.directory.shared.ldap.schema.registries.MatchingRuleRegistry;
import org.apache.directory.shared.ldap.schema.registries.MatchingRuleUseRegistry;
import org.apache.directory.shared.ldap.schema.registries.NameFormRegistry;
import org.apache.directory.shared.ldap.schema.registries.NormalizerRegistry;
import org.apache.directory.shared.ldap.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoaderListener;
import org.apache.directory.shared.ldap.schema.registries.SchemaObjectRegistry;
import org.apache.directory.shared.ldap.schema.registries.SyntaxCheckerRegistry;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.LdapSyntax;


/**
 * A set of boostrap registries used to fire up the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultRegistries extends Registries
{
    private OidRegistry oidRegistry;
    private Map<String,Schema> loadedByName = new HashMap<String, Schema>();
    private final SchemaLoader schemaLoader;
    private final String name;


    public DefaultRegistries( String name, SchemaLoader schemaLoader, OidRegistry oidRegistry )
    {
        super( oidRegistry );
        
        this.name = name;
        this.schemaLoader = schemaLoader;
        
        this.schemaLoader.setListener( new SchemaLoaderListener() {
            public void schemaLoaded( Schema schema )
            {
                loadedByName.put( schema.getSchemaName(), schema );
            }
        });
    }


    public String getName()
    {
        return name;
    }
    
    
    public AttributeTypeRegistry getAttributeTypeRegistry()
    {
        return attributeTypeRegistry;
    }


    public ComparatorRegistry getComparatorRegistry()
    {
        return comparatorRegistry;
    }


    public DITContentRuleRegistry getDitContentRuleRegistry()
    {
        return ditContentRuleRegistry;
    }


    public DITStructureRuleRegistry getDitStructureRuleRegistry()
    {
        return ditStructureRuleRegistry;
    }


    public MatchingRuleRegistry getMatchingRuleRegistry()
    {
        return matchingRuleRegistry;
    }


    public MatchingRuleUseRegistry getMatchingRuleUseRegistry()
    {
        return matchingRuleUseRegistry;
    }


    public NameFormRegistry getNameFormRegistry()
    {
        return nameFormRegistry;
    }


    public NormalizerRegistry getNormalizerRegistry()
    {
        return normalizerRegistry;
    }


    public ObjectClassRegistry getObjectClassRegistry()
    {
        return objectClassRegistry;
    }


    public OidRegistry getOidRegistry()
    {
        return oidRegistry;
    }


    public SyntaxCheckerRegistry getSyntaxCheckerRegistry()
    {
        return syntaxCheckerRegistry;
    }


    // ------------------------------------------------------------------------
    // Code used to sanity check the resolution of entities in registries
    // ------------------------------------------------------------------------

    /**
     * Attempts to resolve the dependent schema objects of all entities that
     * refer to other objects within the registries.  Null references will be
     * handed appropriately.
     *
     * @return a list of exceptions encountered while resolving entities
     */
    public List<Throwable> checkRefInteg()
    {
        ArrayList<Throwable> errors = new ArrayList<Throwable>();

        Iterator<?> list = objectClassRegistry.iterator();
        while ( list.hasNext() )
        {
            ObjectClass oc = ( ObjectClass ) list.next();
            resolve( oc, errors );
        }

        list = attributeTypeRegistry.iterator();
        while ( list.hasNext() )
        {
            AttributeType at = ( AttributeType ) list.next();
            resolve( at, errors );
        }

        list = matchingRuleRegistry.iterator();
        while ( list.hasNext() )
        {
            MatchingRule mr = ( MatchingRule ) list.next();
            resolve( mr, errors );
        }

        list = ldapSyntaxRegistry.iterator();
        while ( list.hasNext() )
        {
            LdapSyntax syntax = ( LdapSyntax ) list.next();
            resolve( syntax, errors );
        }

        return errors;
    }


    /**
     * Attempts to resolve the SyntaxChecker associated with a Syntax.
     *
     * @param syntax the Syntax to resolve the SyntaxChecker of
     * @param errors the list of errors to add exceptions to
     * @return true if it succeeds, false otherwise
     */
    private boolean resolve( LdapSyntax syntax, List<Throwable> errors )
    {
        if ( syntax == null )
        {
            return true;
        }

        try
        {
            syntax.getSyntaxChecker();
            return true;
        }
        catch ( Exception e )
        {
            errors.add( e );
            return false;
        }
    }


    private boolean resolve( MatchingRule mr, List<Throwable> errors )
    {
        boolean isSuccess = true;

        if ( mr == null )
        {
            return true;
        }

        try
        {
            if ( mr.getLdapComparator() == null )
            {
                String schema = matchingRuleRegistry.getSchemaName( mr.getOid() );
                errors.add( new NullPointerException( "matchingRule " + mr.getName() + " in schema " + schema
                    + " with OID " + mr.getOid() + " has a null comparator" ) );
                isSuccess = false;
            }
        }
        catch ( Exception e )
        {
            errors.add( e );
            isSuccess = false;
        }

        try
        {
            if ( mr.getNormalizer() == null )
            {
                String schema = matchingRuleRegistry.getSchemaName( mr.getOid() );
                errors.add( new NullPointerException( "matchingRule " + mr.getName() + " in schema " + schema
                    + " with OID " + mr.getOid() + " has a null normalizer" ) );
                isSuccess = false;
            }
        }
        catch ( Exception e )
        {
            errors.add( e );
            isSuccess = false;
        }

        try
        {
            isSuccess &= resolve( mr.getSyntax(), errors );

            if ( mr.getSyntax() == null )
            {
                String schema = matchingRuleRegistry.getSchemaName( mr.getOid() );
                errors.add( new NullPointerException( "matchingRule " + mr.getName() + " in schema " + schema
                    + " with OID " + mr.getOid() + " has a null Syntax" ) );
                isSuccess = false;
            }
        }
        catch ( Exception e )
        {
            errors.add( e );
            isSuccess = false;
        }

        return isSuccess;
    }


    private boolean resolve( AttributeType at, List<Throwable> errors )
    {
        boolean isSuccess = true;

        boolean hasMatchingRule = false;

        if ( at == null )
        {
            return true;
        }

        try
        {
            isSuccess &= resolve( at.getSup(), errors );
        }
        catch ( Exception e )
        {
            errors.add( e );
            isSuccess = false;
        }

        try
        {
            isSuccess &= resolve( at.getEquality(), errors );

            if ( at.getEquality() != null )
            {
                hasMatchingRule |= true;
            }
        }
        catch ( Exception e )
        {
            errors.add( e );
            isSuccess = false;
        }

        try
        {
            isSuccess &= resolve( at.getOrdering(), errors );

            if ( at.getOrdering() != null )
            {
                hasMatchingRule |= true;
            }
        }
        catch ( Exception e )
        {
            errors.add( e );
            isSuccess = false;
        }

        try
        {
            isSuccess &= resolve( at.getSubstr(), errors );

            if ( at.getSubstr() != null )
            {
                hasMatchingRule |= true;
            }
        }
        catch ( Exception e )
        {
            errors.add( e );
            isSuccess = false;
        }

        try
        {
            isSuccess &= resolve( at.getSyntax(), errors );

            if ( at.getSyntax() == null )
            {
                String schema = attributeTypeRegistry.getSchemaName( at.getOid() );

                errors.add( new NullPointerException( "attributeType " + at.getName() + " in schema " + schema
                    + " with OID " + at.getOid() + " has a null Syntax" ) );

                isSuccess = false;
            }
        }
        catch ( Exception e )
        {
            errors.add( e );
            isSuccess = false;
        }

        //        try
        //        {
        //            String schema = attributeTypeRegistry.getSchemaName( at.getOid() );
        //            if ( ! hasMatchingRule && at.getSyntax().isHumanReadable() )
        //            {
        //                errors.add( new NullPointerException( "attributeType "
        //                        + at.getName() + " in schema " + schema + " with OID "
        //                        + at.getOid() + " has a no matchingRules defined" ) );
        //                isSuccess = false;
        //            }
        //        }
        //        catch ( NamingException e )
        //        {
        //            errors.add( e );
        //            isSuccess = false;
        //        }

        return isSuccess;
    }


    private boolean resolve( ObjectClass oc, List<Throwable> errors )
    {
        boolean isSuccess = true;

        if ( oc == null )
        {
            return true;
        }

        List<ObjectClass> superiors = oc.getSuperiors();

        if ( ( superiors == null ) || ( superiors.size() == 0 ) )
        {
            isSuccess = false;
        }
        else
        {
            for ( ObjectClass superior : superiors )
            {
                isSuccess &= resolve( superior, errors );
            }
        }

        AttributeType[] mayList = new org.apache.directory.shared.ldap.schema.AttributeType[0];

        try
        {
            mayList = oc.getMayList();
        }
        catch ( Exception e )
        {
            mayList = new AttributeType[0];
            isSuccess = false;
            errors.add( e );
        }

        for ( int ii = 0; ii < mayList.length; ii++ )
        {
            isSuccess &= resolve( mayList[ii], errors );
        }

        AttributeType[] mustList = new org.apache.directory.shared.ldap.schema.AttributeType[0];

        try
        {
            mustList = oc.getMustList();
        }
        catch ( Exception e )
        {
            mustList = new AttributeType[0];
            isSuccess = false;
            errors.add( e );
        }

        for ( int ii = 0; ii < mustList.length; ii++ )
        {
            isSuccess &= resolve( mustList[ii], errors );
        }

        return isSuccess;
    }

    
    /**
     * Alterations to the returned map of schema names to schema objects does not 
     * change the map returned from this method.  The returned map is however mutable.
     */
    public Map<String, Schema> getLoadedSchemas()
    {
        return new HashMap<String,Schema>( loadedByName );
    }


    public void load( String schemaName ) throws Exception
    {
        load( schemaName, new Properties() );
    }


    public void load( String schemaName, Properties schemaProperties ) throws Exception
    {
        Schema schema = schemaLoader.getSchema( schemaName, schemaProperties );
        
        if ( schema.isDisabled() )
        {
            throw new Exception( "Disabled schemas cannot be loaded into registries." );
        }
        
        loadedByName.put( schema.getSchemaName(), schema );
        schemaLoader.load( schema, this, false );
    }
    
    
    public void unload( String schemaName ) throws Exception
    {
        disableSchema( ditStructureRuleRegistry, schemaName );
        disableSchema( ditContentRuleRegistry, schemaName );
        disableSchema( matchingRuleUseRegistry, schemaName );
        disableSchema( nameFormRegistry, schemaName );
        disableSchema( objectClassRegistry, schemaName );
        disableSchema( attributeTypeRegistry, schemaName );
        disableSchema( matchingRuleRegistry, schemaName );
        disableSchema( ldapSyntaxRegistry, schemaName );

        normalizerRegistry.unregisterSchemaElements( schemaName );
        comparatorRegistry.unregisterSchemaElements( schemaName );
        syntaxCheckerRegistry.unregisterSchemaElements( schemaName );
        loadedByName.remove( schemaName );
    }


    private void disableSchema( SchemaObjectRegistry<?> registry, String schemaName ) throws Exception
    {
        Iterator<? extends SchemaObject> objects = registry.iterator();
        List<String> unregistered = new ArrayList<String>();
        while ( objects.hasNext() )
        {
            SchemaObject obj = objects.next();
            if ( obj.getSchema().equalsIgnoreCase( schemaName ) )
            {
                unregistered.add( obj.getOid() );
            }
        }
        
        for ( String oid : unregistered )
        {
            registry.unregister( oid );
        }
    }
    

    public SchemaLoader getSchemaLoader()
    {
        return schemaLoader;
    }


    public Schema getSchema( String schemaName )
    {
        return this.loadedByName.get( schemaName );
    }


    public void addToLoadedSet( Schema schema )
    {
        loadedByName.put( schema.getSchemaName(), schema );
    }


    public void removeFromLoadedSet( String schemaName )
    {
        loadedByName.remove( schemaName );
    }
}
