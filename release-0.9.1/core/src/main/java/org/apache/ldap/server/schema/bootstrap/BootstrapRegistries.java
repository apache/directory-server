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
package org.apache.ldap.server.schema.bootstrap;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingException;

import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.schema.MatchingRule;
import org.apache.ldap.common.schema.ObjectClass;
import org.apache.ldap.common.schema.Syntax;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.ComparatorRegistry;
import org.apache.ldap.server.schema.DITContentRuleRegistry;
import org.apache.ldap.server.schema.DITStructureRuleRegistry;
import org.apache.ldap.server.schema.MatchingRuleRegistry;
import org.apache.ldap.server.schema.MatchingRuleUseRegistry;
import org.apache.ldap.server.schema.NameFormRegistry;
import org.apache.ldap.server.schema.NormalizerRegistry;
import org.apache.ldap.server.schema.ObjectClassRegistry;
import org.apache.ldap.server.schema.ObjectFactoryRegistry;
import org.apache.ldap.server.schema.OidRegistry;
import org.apache.ldap.server.schema.Registries;
import org.apache.ldap.server.schema.StateFactoryRegistry;
import org.apache.ldap.server.schema.SyntaxCheckerRegistry;
import org.apache.ldap.server.schema.SyntaxRegistry;


/**
 * A set of boostrap registries used to fire up the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapRegistries implements Registries
{
    private BootstrapAttributeTypeRegistry attributeTypeRegistry;
    private BootstrapComparatorRegistry comparatorRegistry;
    private BootstrapDitContentRuleRegistry ditContentRuleRegistry;
    private BootstrapDitStructureRuleRegistry ditStructureRuleRegistry;
    private BootstrapMatchingRuleRegistry matchingRuleRegistry;
    private BootstrapMatchingRuleUseRegistry matchingRuleUseRegistry;
    private BootstrapNameFormRegistry nameFormRegistry;
    private BootstrapNormalizerRegistry normalizerRegistry;
    private BootstrapObjectClassRegistry objectClassRegistry;
    private BootstrapOidRegistry oidRegistry;
    private BootstrapSyntaxCheckerRegistry syntaxCheckerRegistry;
    private BootstrapSyntaxRegistry syntaxRegistry;
    private BootstrapObjectFactoryRegistry objectFactoryRegistry;
    private BootstrapStateFactoryRegistry stateFactoryRegistry;


    public BootstrapRegistries()
    {
        oidRegistry = new BootstrapOidRegistry();
        normalizerRegistry = new BootstrapNormalizerRegistry();
        comparatorRegistry = new BootstrapComparatorRegistry();
        syntaxCheckerRegistry = new BootstrapSyntaxCheckerRegistry();
        syntaxRegistry = new BootstrapSyntaxRegistry( getOidRegistry() );
        matchingRuleRegistry = new BootstrapMatchingRuleRegistry( getOidRegistry() );
        attributeTypeRegistry = new BootstrapAttributeTypeRegistry( getOidRegistry() );
        objectClassRegistry = new BootstrapObjectClassRegistry( getOidRegistry() );
        ditContentRuleRegistry = new BootstrapDitContentRuleRegistry( getOidRegistry() );
        ditStructureRuleRegistry = new BootstrapDitStructureRuleRegistry( getOidRegistry() );
        matchingRuleUseRegistry = new BootstrapMatchingRuleUseRegistry();
        nameFormRegistry = new BootstrapNameFormRegistry( getOidRegistry() );
        objectFactoryRegistry = new BootstrapObjectFactoryRegistry( getOidRegistry() );
        stateFactoryRegistry = new BootstrapStateFactoryRegistry();
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

    public ObjectFactoryRegistry getObjectFactoryRegistry()
    {
        return objectFactoryRegistry;
    }

    public StateFactoryRegistry getStateFactoryRegistry()
    {
        return stateFactoryRegistry;
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
                errors.add( new NullPointerException( "matchingRule "
                        + mr.getName() + " in schema " + schema + " with OID "
                        + mr.getOid() + " has a null comparator" ) );
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
                errors.add( new NullPointerException( "matchingRule "
                        + mr.getName() + " in schema " + schema + " with OID "
                        + mr.getOid() + " has a null normalizer" ) );
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
                errors.add( new NullPointerException( "matchingRule "
                        + mr.getName() + " in schema " + schema + " with OID " + mr.getOid()
                        + " has a null Syntax" ) );
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

                errors.add( new NullPointerException( "attributeType "
                        + at.getName() + " in schema " + schema + " with OID "
                        + at.getOid() + " has a null Syntax" ) );

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


    private boolean resolve( ObjectClass oc, List errors )
    {
        boolean isSuccess = true;

        if ( oc == null )
        {
            return true;
        }

        ObjectClass[] superiors = new org.apache.ldap.common.schema.ObjectClass[0];

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
            isSuccess &= resolve( superiors[ii], errors ) ;
        }

        AttributeType[] mayList = new org.apache.ldap.common.schema.AttributeType[0];

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
            isSuccess &= resolve( mayList[ii], errors ) ;
        }


        AttributeType[] mustList = new org.apache.ldap.common.schema.AttributeType[0];

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
            isSuccess &= resolve( mustList[ii], errors ) ;
        }

        return isSuccess;
    }
}
