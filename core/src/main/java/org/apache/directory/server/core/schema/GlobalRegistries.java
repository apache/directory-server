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
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.bootstrap.BootstrapAttributeTypeRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapComparatorRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapDitContentRuleRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapDitStructureRuleRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapMatchingRuleRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapMatchingRuleUseRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapNameFormRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapNormalizerRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapObjectClassRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapOidRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapRegistries;
import org.apache.directory.server.core.schema.bootstrap.BootstrapSyntaxCheckerRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapSyntaxRegistry;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.Syntax;


/**
 * Document me.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GlobalRegistries implements Registries
{
    private GlobalAttributeTypeRegistry attributeTypeRegistry;
    private GlobalComparatorRegistry comparatorRegistry;
    private GlobalDitContentRuleRegistry ditContentRuleRegistry;
    private GlobalDitStructureRuleRegistry ditStructureRuleRegistry;
    private GlobalMatchingRuleRegistry matchingRuleRegistry;
    private GlobalMatchingRuleUseRegistry matchingRuleUseRegistry;
    private GlobalNameFormRegistry nameFormRegistry;
    private GlobalNormalizerRegistry normalizerRegistry;
    private GlobalObjectClassRegistry objectClassRegistry;
    private GlobalOidRegistry oidRegistry;
    private GlobalSyntaxCheckerRegistry syntaxCheckerRegistry;
    private GlobalSyntaxRegistry syntaxRegistry;


    public GlobalRegistries(BootstrapRegistries bootstrapRegistries)
    {
        oidRegistry = new GlobalOidRegistry( ( BootstrapOidRegistry ) bootstrapRegistries.getOidRegistry() );
        normalizerRegistry = new GlobalNormalizerRegistry( ( BootstrapNormalizerRegistry ) bootstrapRegistries
            .getNormalizerRegistry() );
        comparatorRegistry = new GlobalComparatorRegistry( ( BootstrapComparatorRegistry ) bootstrapRegistries
            .getComparatorRegistry() );
        syntaxCheckerRegistry = new GlobalSyntaxCheckerRegistry( ( BootstrapSyntaxCheckerRegistry ) bootstrapRegistries
            .getSyntaxCheckerRegistry() );
        syntaxRegistry = new GlobalSyntaxRegistry( ( BootstrapSyntaxRegistry ) bootstrapRegistries.getSyntaxRegistry(),
            oidRegistry );
        matchingRuleRegistry = new GlobalMatchingRuleRegistry( ( BootstrapMatchingRuleRegistry ) bootstrapRegistries
            .getMatchingRuleRegistry(), oidRegistry );
        attributeTypeRegistry = new GlobalAttributeTypeRegistry( ( BootstrapAttributeTypeRegistry ) bootstrapRegistries
            .getAttributeTypeRegistry(), oidRegistry );
        objectClassRegistry = new GlobalObjectClassRegistry( ( BootstrapObjectClassRegistry ) bootstrapRegistries
            .getObjectClassRegistry(), oidRegistry );
        ditContentRuleRegistry = new GlobalDitContentRuleRegistry(
            ( BootstrapDitContentRuleRegistry ) bootstrapRegistries.getDitContentRuleRegistry(), oidRegistry );
        ditStructureRuleRegistry = new GlobalDitStructureRuleRegistry(
            ( BootstrapDitStructureRuleRegistry ) bootstrapRegistries.getDitStructureRuleRegistry(), oidRegistry );
        matchingRuleUseRegistry = new GlobalMatchingRuleUseRegistry(
            ( BootstrapMatchingRuleUseRegistry ) bootstrapRegistries.getMatchingRuleUseRegistry(), oidRegistry );
        nameFormRegistry = new GlobalNameFormRegistry( ( BootstrapNameFormRegistry ) bootstrapRegistries
            .getNameFormRegistry(), oidRegistry );
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
        ArrayList errors = new ArrayList();

        Iterator list = objectClassRegistry.list();
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

        list = matchingRuleRegistry.list();
        while ( list.hasNext() )
        {
            MatchingRule mr = ( MatchingRule ) list.next();
            resolve( mr, errors );
        }

        list = syntaxRegistry.list();
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
    private boolean resolve( Syntax syntax, List errors )
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


    private boolean resolve( MatchingRule mr, List errors )
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


    private boolean resolve( AttributeType at, List errors )
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

        if ( hasMatchingRule )
        {
            // ----
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


    private boolean resolve( ObjectClass oc, List errors )
    {
        boolean isSuccess = true;

        if ( oc == null )
        {
            return true;
        }

        ObjectClass[] superiors = new ObjectClass[0];
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

        AttributeType[] mayList = new AttributeType[0];
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

        AttributeType[] mustList = new AttributeType[0];
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
}
