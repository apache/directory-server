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
package org.apache.eve.schema.bootstrap;


import org.apache.eve.schema.*;
import org.apache.ldap.common.schema.*;

import java.util.Map;
import java.util.Iterator;
import java.util.Comparator;
import javax.naming.NamingException;


/**
 * Class which handles bootstrap schema class file loading.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class BootstrapSchemaLoader
{
    public final void populate( BootstrapSchema schema, BootstrapRegistries registries )
        throws NamingException
    {
        populate( schema, registries.getNormalizerRegistry() );
        populate( schema, registries.getComparatorRegistry() );
        populate( schema, registries.getSyntaxCheckerRegistry() );
        populate( schema, registries.getSyntaxRegistry(), registries.getSyntaxCheckerRegistry() );
        populate( schema, registries.getMatchingRuleRegistry(),
            registries.getSyntaxRegistry(), registries.getNormalizerRegistry(),
            registries.getComparatorRegistry() );
    }


    // ------------------------------------------------------------------------
    // Utility Methods
    // ------------------------------------------------------------------------


    private void populate( BootstrapSchema schema, 
                           MatchingRuleRegistry matchingRuleRegistry,
                           SyntaxRegistry syntaxRegistry,
                           NormalizerRegistry normalizerRegistry,
                           ComparatorRegistry comparatorRegistry )
        throws NamingException
    {
        MatchingRuleFactory factory;
        factory = ( MatchingRuleFactory ) getFactory( schema, "MatchingRuleFactory" );

        Map matchingRules = factory.getMatchingRules(syntaxRegistry,
            normalizerRegistry, comparatorRegistry );
        Iterator list = matchingRules.values().iterator();
        while ( list.hasNext() )
        {
            MatchingRule matchingRule = ( MatchingRule ) list.next();
            matchingRuleRegistry.register( schema.getSchemaName(), matchingRule );
        }
    }


    private void populate( BootstrapSchema schema,
                           SyntaxRegistry syntaxRegistry,
                           SyntaxCheckerRegistry syntaxCheckerRegistry )
        throws NamingException
    {
        SyntaxFactory factory;
        factory = ( SyntaxFactory ) getFactory( schema, "SyntaxFactory" );

        Map syntaxes = factory.getSyntaxes( syntaxCheckerRegistry );
        Iterator list = syntaxes.values().iterator();
        while ( list.hasNext() )
        {
            Syntax syntax = ( Syntax ) list.next();
            syntaxRegistry.register( schema.getSchemaName(), syntax );
        }
    }


    private void populate( BootstrapSchema schema, SyntaxCheckerRegistry registry )
        throws NamingException
    {
        SyntaxCheckerFactory factory;
        factory = ( SyntaxCheckerFactory ) getFactory( schema, "SyntaxCheckerFactory" );

        Map syntaxCheckers = factory.getSyntaxCheckers();
        Iterator oidList = syntaxCheckers.keySet().iterator();
        while ( oidList.hasNext() )
        {
            String oid = ( String ) oidList.next();
            SyntaxChecker syntaxChecker = ( SyntaxChecker ) syntaxCheckers.get( oid );
            registry.register( schema.getSchemaName(), oid, syntaxChecker );
        }
    }


    /**
     * Attempts first to try to load the target class for the NormalizerFactory,
     * then tries for the default if the target load fails.
     *
     * @param schema the bootstrap schema
     * @param registry the registry to populate
     * @throws NamingException if there are failures loading classes
     */
    private void populate( BootstrapSchema schema, NormalizerRegistry registry )
        throws NamingException
    {
        NormalizerFactory factory;
        factory = ( NormalizerFactory ) getFactory( schema, "NormalizerFactory" );

        Map normalizers = factory.getNormalizers();
        Iterator oidList = normalizers.keySet().iterator();
        while ( oidList.hasNext() )
        {
            String oid = ( String ) oidList.next();
            Normalizer normalizer = ( Normalizer ) normalizers.get( oid );
            registry.register( schema.getSchemaName(), oid, normalizer );
        }
    }


    /**
     * Attempts first to try to load the target class for the ComparatorFactory,
     * then tries for the default if the target load fails.
     *
     * @param schema the bootstrap schema
     * @param registry the registry to populate
     * @throws NamingException if there are failures loading classes
     */
    private void populate( BootstrapSchema schema, ComparatorRegistry registry )
        throws NamingException
    {
        ComparatorFactory factory;
        factory = ( ComparatorFactory ) getFactory( schema, "ComparatorFactory" );

        Map comparators = factory.getComparators();
        Iterator oidList = comparators.keySet().iterator();
        while ( oidList.hasNext() )
        {
            String oid = ( String ) oidList.next();
            Comparator comparator = ( Comparator ) comparators.get( oid );
            registry.register( schema.getSchemaName(), oid, comparator );
        }
    }


    /**
     * Attempts first to try to load the target class for the Factory,
     * then tries for the default if the target load fails.
     *
     * @param schema the bootstrap schema
     * @param factoryBase the factory base name
     * @throws NamingException if there are failures loading classes
     */
    private Object getFactory( BootstrapSchema schema, String factoryBase )
        throws NamingException
    {
        Class clazz = null;
        boolean failedTargetLoad = false;
        String defaultClassName;
        String targetClassName = schema.getBaseClassName() + factoryBase;

        try
        {
            clazz = Class.forName( targetClassName );
        }
        catch ( ClassNotFoundException e )
        {
            failedTargetLoad = true;
            // @todo instead of trace report target class load failure to monitor
            e.printStackTrace();
        }

        if ( failedTargetLoad )
        {
            defaultClassName = schema.getDefaultBaseClassName() + factoryBase;

            try
            {
                clazz = Class.forName( defaultClassName );
            }
            catch ( ClassNotFoundException e )
            {
                NamingException ne = new NamingException( "Failed to load " +
                    factoryBase + " for " + schema.getSchemaName()
                    + " schema using following classes: "  + targetClassName
                    + ", " + defaultClassName );
                ne.setRootCause( e );
                throw ne;
            }
        }

        try
        {
            return clazz.newInstance();
        }
        catch ( IllegalAccessException e )
        {
            NamingException ne = new NamingException( "Failed to create " + clazz );
            ne.setRootCause( e );
            throw ne;
        }
        catch ( InstantiationException e )
        {
            NamingException ne = new NamingException( "Failed to create " + clazz );
            ne.setRootCause( e );
            throw ne;
        }
    }
}
