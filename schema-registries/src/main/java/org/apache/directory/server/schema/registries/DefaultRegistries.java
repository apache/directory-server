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

import javax.naming.NamingException;

import org.apache.directory.server.schema.bootstrap.Schema;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.Syntax;


/**
 * A set of boostrap registries used to fire up the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultRegistries implements Registries
{
    private DefaultAttributeTypeRegistry attributeTypeRegistry;
    private DefaultComparatorRegistry comparatorRegistry;
    private DefaultDitContentRuleRegistry ditContentRuleRegistry;
    private DefaultDitStructureRuleRegistry ditStructureRuleRegistry;
    private DefaultMatchingRuleRegistry matchingRuleRegistry;
    private DefaultMatchingRuleUseRegistry matchingRuleUseRegistry;
    private DefaultNameFormRegistry nameFormRegistry;
    private DefaultNormalizerRegistry normalizerRegistry;
    private DefaultObjectClassRegistry objectClassRegistry;
    private OidRegistry oidRegistry;
    private DefaultSyntaxCheckerRegistry syntaxCheckerRegistry;
    private DefaultSyntaxRegistry syntaxRegistry;
    private Map<String,Schema> byName = new HashMap<String, Schema>();
    private final SchemaLoader schemaLoader;
    private final String name;


    public DefaultRegistries( String name, SchemaLoader schemaLoader, OidRegistry registry )
    {
        this.name = name;
        this.schemaLoader = schemaLoader;
        this.schemaLoader.setListener( new SchemaLoaderListener() {
            public void schemaLoaded( Schema schema )
            {
                byName.put( schema.getSchemaName(), schema );
            }
        });
        oidRegistry = registry;
        normalizerRegistry = new DefaultNormalizerRegistry();
        comparatorRegistry = new DefaultComparatorRegistry();
        syntaxCheckerRegistry = new DefaultSyntaxCheckerRegistry();
        syntaxRegistry = new DefaultSyntaxRegistry( getOidRegistry() );
        matchingRuleRegistry = new DefaultMatchingRuleRegistry( getOidRegistry() );
        attributeTypeRegistry = new DefaultAttributeTypeRegistry( getOidRegistry() );
        objectClassRegistry = new DefaultObjectClassRegistry( getOidRegistry() );
        ditContentRuleRegistry = new DefaultDitContentRuleRegistry( getOidRegistry() );
        ditStructureRuleRegistry = new DefaultDitStructureRuleRegistry( getOidRegistry() );
        matchingRuleUseRegistry = new DefaultMatchingRuleUseRegistry();
        nameFormRegistry = new DefaultNameFormRegistry( getOidRegistry() );
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


    public SyntaxRegistry getSyntaxRegistry()
    {
        return syntaxRegistry;
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
    public List checkRefInteg()
    {
        ArrayList<Throwable> errors = new ArrayList<Throwable>();

        Iterator list = objectClassRegistry.iterator();
        while ( list.hasNext() )
        {
            ObjectClass oc = ( ObjectClass ) list.next();
            resolve( oc, errors );
        }

        list = attributeTypeRegistry.list();
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

        list = syntaxRegistry.iterator();
        while ( list.hasNext() )
        {
            Syntax syntax = ( Syntax ) list.next();
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
    private boolean resolve( Syntax syntax, List<Throwable> errors )
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
        catch ( NamingException e )
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
            if ( mr.getComparator() == null )
            {
                String schema = matchingRuleRegistry.getSchemaName( mr.getOid() );
                errors.add( new NullPointerException( "matchingRule " + mr.getName() + " in schema " + schema
                    + " with OID " + mr.getOid() + " has a null comparator" ) );
                isSuccess = false;
            }
        }
        catch ( NamingException e )
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
        catch ( NamingException e )
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
        catch ( NamingException e )
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
            isSuccess &= resolve( at.getSuperior(), errors );
        }
        catch ( NamingException e )
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
        catch ( NamingException e )
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
        catch ( NamingException e )
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
        catch ( NamingException e )
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
        catch ( NamingException e )
        {
            errors.add( e );

            isSuccess = false;
        }

        //        try
        //        {
        //            String schema = attributeTypeRegistry.getSchemaName( at.getOid() );
        //            if ( ! hasMatchingRule && at.getSyntax().isHumanReadible() )
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

        ObjectClass[] superiors = new org.apache.directory.shared.ldap.schema.ObjectClass[0];

        try
        {
            superiors = oc.getSuperClasses();
        }
        catch ( NamingException e )
        {
            superiors = new ObjectClass[0];
            isSuccess = false;
            errors.add( e );
        }

        for ( int ii = 0; ii < superiors.length; ii++ )
        {
            isSuccess &= resolve( superiors[ii], errors );
        }

        AttributeType[] mayList = new org.apache.directory.shared.ldap.schema.AttributeType[0];

        try
        {
            mayList = oc.getMayList();
        }
        catch ( NamingException e )
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
        catch ( NamingException e )
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
        return new HashMap<String,Schema>( byName );
    }


    public void load( String schemaName ) throws NamingException
    {
        load( schemaName, new Properties() );
    }


    public void load( String schemaName, Properties schemaProperties ) throws NamingException
    {
        Schema schema = schemaLoader.getSchema( schemaName, schemaProperties );
        if ( schema.isDisabled() )
        {
            throw new NamingException( "Disabled schemas cannot be loaded into registries." );
        }
        
        byName.put( schema.getSchemaName(), schema );
        schemaLoader.load( schema, this );
    }


    public SchemaLoader setSchemaLoader()
    {
        return schemaLoader;
    }


    public Schema getSchema( String schemaName )
    {
        return this.byName.get( schemaName );
    }
}
