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


import javax.naming.NamingException;

import org.apache.ldap.common.schema.*;
import org.apache.eve.schema.*;


/**
 * An abstract producer implementation.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractBootstrapProducer implements BootstrapProducer
{
    /** a reused empty String array */
    protected static final String[] EMPTY = new String[0];
    /** the producer type */
    private final ProducerTypeEnum type;


    /**
     * Creates a producer of a specific type.
     *
     * @param type the producer type
     */
    protected AbstractBootstrapProducer( ProducerTypeEnum type )
    {
        this.type = type;
    }


    /**
     * @see BootstrapProducer#getType()
     */
    public ProducerTypeEnum getType()
    {
        return type;
    }


    protected static BootstrapSyntax
        newSyntax( String oid, BootstrapRegistries registries )
    {
        return new BootstrapSyntax( oid, registries.getSyntaxCheckerRegistry() );
    }



    protected static BootstrapAttributeType
        newAttributeType( String oid, BootstrapRegistries registries )
    {
        return new BootstrapAttributeType( oid, registries );
    }



    protected static BootstrapObjectClass
        newObjectClass( String oid, BootstrapRegistries registries )
    {
        return new BootstrapObjectClass( oid, registries );
    }



    /**
     * A mutable Syntax for the bootstrap phase that uses the
     * syntaxCheckerRegistry to dynamically resolve syntax checkers.
     */
    public static class BootstrapSyntax extends AbstractSyntax
    {
        final SyntaxCheckerRegistry registry;


        protected BootstrapSyntax( String oid, SyntaxCheckerRegistry registry )
        {
            super( oid );
            this.registry = registry;
        }


        public void setDescription( String description )
        {
            super.setDescription( description );
        }


        public void setHumanReadible( boolean isHumanReadible )
        {
            super.setHumanReadible( isHumanReadible );
        }


        public void setNames( String[] names )
        {
            super.setNames( names );
        }


        public SyntaxChecker getSyntaxChecker( ) throws NamingException
        {
            return registry.lookup( getOid() );
        }


        public boolean isObsolete()
        {
            return false;
        }
    }


    /**
     * A concrete mutable attributeType implementation for bootstrapping which
     * uses registries for dynamically resolving dependent objects.
     */
    public static class BootstrapAttributeType extends AbstractAttributeType
    {
        private final SyntaxRegistry syntaxRegistry;
        private final MatchingRuleRegistry matchingRuleRegistry;
        private final AttributeTypeRegistry attributeTypeRegistry;
        private String superiorId;
        private String equalityId;
        private String substrId;
        private String orderingId;
        private String syntaxId;


        protected BootstrapAttributeType( String oid, BootstrapRegistries registries )
        {
            super( oid );

            syntaxRegistry = registries.getSyntaxRegistry();
            matchingRuleRegistry = registries.getMatchingRuleRegistry();
            attributeTypeRegistry = registries.getAttributeTypeRegistry();
        }

        public void setSuperiorId( String superiorId )
        {
            this.superiorId = superiorId;
        }

        public AttributeType getSuperior() throws NamingException
        {
            return this.attributeTypeRegistry.lookup( superiorId );
        }

        public void setNames( String[] names )
        {
            super.setNames( names );
        }

        public MatchingRule getEquality() throws NamingException
        {
            return this.matchingRuleRegistry.lookup( equalityId );
        }

        public void setEqualityId( String equalityId )
        {
            this.equalityId = equalityId;
        }

        public MatchingRule getSubstr() throws NamingException
        {
            return this.matchingRuleRegistry.lookup( substrId ) ;
        }

        public void setSubstrId( String substrId )
        {
            this.substrId = substrId;
        }

        public MatchingRule getOrdering() throws NamingException
        {
            return this.matchingRuleRegistry.lookup( orderingId );
        }

        public void setOrderingId( String orderingId )
        {
            this.orderingId = orderingId;
        }

        public void setSyntaxId( String syntaxId )
        {
            this.syntaxId = syntaxId;
        }

        public Syntax getSyntax() throws NamingException
        {
            return this.syntaxRegistry.lookup( syntaxId );
        }

        public void setSingleValue( boolean singleValue )
        {
            super.setSingleValue( singleValue );
        }

        public void setCollective( boolean collective )
        {
            super.setCollective( collective );
        }

        public void setCanUserModify( boolean canUserModify )
        {
            super.setCanUserModify( canUserModify );
        }

        public void setObsolete( boolean obsolete )
        {
            super.setObsolete( obsolete );
        }

        public void setUsage( UsageEnum usage )
        {
            super.setUsage( usage );
        }

        public void setLength( int length )
        {
            super.setLength( length );
        }
    }


    /**
     * A concrete mutable objectClass implementation for bootstrapping which
     * uses registries for dynamically resolving dependent objects.
     */
    public static class BootstrapObjectClass extends AbstractSchemaObject
        implements ObjectClass
    {
        private final ObjectClassRegistry objectClassRegistry;
        private final AttributeTypeRegistry attributeTypeRegistry;

        private String[] superClassIds = EMPTY;
        private ObjectClass[] superClasses;
        private ObjectClassTypeEnum type = ObjectClassTypeEnum.STRUCTURAL;

        private String[] mayListIds = EMPTY;
        private AttributeType[] mayList;

        private String[] mustListIds = EMPTY;
        private AttributeType[] mustList;


        /**
         * Creates a mutable ObjectClass for the bootstrap process.
         *
         * @param oid the OID of the new objectClass
         * @param registries the bootstrap registries to use for resolving dependent objects
         */
        protected BootstrapObjectClass( String oid, BootstrapRegistries registries )
        {
            super( oid );

            objectClassRegistry = registries.getObjectClassRegistry();
            attributeTypeRegistry = registries.getAttributeTypeRegistry();
        }


        // --------------------------------------------------------------------
        // ObjectClass Accessors
        // --------------------------------------------------------------------


        public ObjectClass[] getSuperClasses() throws NamingException
        {
            if ( superClasses == null )
            {
                superClasses = new ObjectClass[superClassIds.length];
            }

            for( int ii = 0; ii < superClassIds.length; ii++ )
            {
                superClasses[ii] = objectClassRegistry.lookup( superClassIds[ii] );
            }

            return superClasses;
        }


        public void setSuperClassIds( String[] superClassIds )
        {
            this.superClassIds = superClassIds;
        }


        public ObjectClassTypeEnum getType()
        {
            return type;
        }


        public void setType( ObjectClassTypeEnum type )
        {
            this.type = type;
        }


        public AttributeType[] getMustList() throws NamingException
        {
            if ( mustList == null )
            {
                mustList = new AttributeType[mustListIds.length];
            }

            for( int ii = 0; ii < mustListIds.length; ii++ )
            {
                mustList[ii] = attributeTypeRegistry.lookup( mustListIds[ii] );
            }

            return mustList;
        }


        public void setMustListIds( String[] mustListIds )
        {
            this.mustListIds = mustListIds;
        }


        public AttributeType[] getMayList() throws NamingException
        {
            if ( mayList == null )
            {
                mayList = new AttributeType[mayListIds.length];
            }

            for( int ii = 0; ii < mayListIds.length; ii++ )
            {
                mayList[ii] = attributeTypeRegistry.lookup( mayListIds[ii] );
            }

            return mayList;
        }


        public void setMayListIds( String[] mayListIds )
        {
            this.mayListIds = mayListIds;
        }


        // --------------------------------------------------------------------
        // SchemaObject Mutators
        // --------------------------------------------------------------------


        public void setObsolete( boolean obsolete )
        {
            super.setObsolete( obsolete );
        }

        public void setNames( String[] names )
        {
            super.setNames( names );
        }

        public void setDescription( String description )
        {
            super.setDescription( description );
        }


    }
}
