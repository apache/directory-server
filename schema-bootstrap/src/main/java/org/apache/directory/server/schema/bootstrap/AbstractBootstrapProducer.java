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
package org.apache.directory.server.schema.bootstrap;


import java.util.Comparator;

import javax.naming.NamingException;

import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.ComparatorRegistry;
import org.apache.directory.server.schema.registries.MatchingRuleRegistry;
import org.apache.directory.server.schema.registries.NormalizerRegistry;
import org.apache.directory.server.schema.registries.ObjectClassRegistry;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.schema.registries.SyntaxCheckerRegistry;
import org.apache.directory.server.schema.registries.SyntaxRegistry;
import org.apache.directory.shared.ldap.schema.AbstractAttributeType;
import org.apache.directory.shared.ldap.schema.AbstractMatchingRule;
import org.apache.directory.shared.ldap.schema.AbstractSchemaObject;
import org.apache.directory.shared.ldap.schema.AbstractSyntax;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.Syntax;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker;


/**
 * An abstract producer implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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
    protected AbstractBootstrapProducer(ProducerTypeEnum type)
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


    protected static BootstrapSyntax newSyntax( String oid, Registries registries )
    {
        return new BootstrapSyntax( oid, registries.getSyntaxCheckerRegistry() );
    }


    protected static BootstrapAttributeType newAttributeType( String oid, Registries registries )
    {
        return new BootstrapAttributeType( oid, registries );
    }


    protected static BootstrapObjectClass newObjectClass( String oid, Registries registries )
    {
        return new BootstrapObjectClass( oid, registries );
    }

    
    /**
     * A mutable Syntax for the bootstrap phase that uses the
     * syntaxCheckerRegistry to dynamically resolve syntax checkers.
     */
    public static class BootstrapSyntax extends AbstractSyntax
    {
        private static final long serialVersionUID = 1L;
        final SyntaxCheckerRegistry registry;


        public BootstrapSyntax(String oid, SyntaxCheckerRegistry registry)
        {
            super( oid );
            this.registry = registry;
        }
        
        
        public void setSchema( String schema )
        {
            super.setSchema( schema );
        }


        public void setDescription( String description )
        {
            super.setDescription( description );
        }


        public void setHumanReadable( boolean isHumanReadable )
        {
            super.setHumanReadable( isHumanReadable );
        }


        public void setNames( String[] names )
        {
            super.setNames( names );
        }


        public SyntaxChecker getSyntaxChecker() throws NamingException
        {
            return registry.lookup( getOid() );
        }


        public boolean isObsolete()
        {
            return false;
        }
    }

    public static class BootstrapMatchingRule extends AbstractMatchingRule
    {
        private static final long serialVersionUID = 1L;
        final SyntaxRegistry syntaxRegistry;
        final NormalizerRegistry normalizerRegistry;
        final ComparatorRegistry comparatorRegistry;
        String syntaxOid;


        public BootstrapMatchingRule(String oid, Registries registries)
        {
            super( oid );
            this.syntaxRegistry = registries.getSyntaxRegistry();
            this.normalizerRegistry = registries.getNormalizerRegistry();
            this.comparatorRegistry = registries.getComparatorRegistry();
        }


        public void setNames( String[] names )
        {
            super.setNames( names );
        }


        public void setSchema( String schema )
        {
            super.setSchema( schema );
        }


        public void setSyntaxOid( String syntaxOid )
        {
            this.syntaxOid = syntaxOid;
        }


        public void setDescription( String description )
        {
            super.setDescription( description );
        }


        public void setObsolete( boolean isObsolete )
        {
            super.setObsolete( isObsolete );
        }


        // accessors

        public Syntax getSyntax() throws NamingException
        {
            return syntaxRegistry.lookup( syntaxOid );
        }


        public Comparator getComparator() throws NamingException
        {
            return comparatorRegistry.lookup( getOid() );
        }


        public Normalizer getNormalizer() throws NamingException
        {
            return normalizerRegistry.lookup( getOid() );
        }
    }

    /**
     * A concrete mutable attributeType implementation for bootstrapping which
     * uses registries for dynamically resolving dependent objects.
     */
    public static class BootstrapAttributeType extends AbstractAttributeType
    {
        private static final long serialVersionUID = 4050205236738471984L;

        private final SyntaxRegistry syntaxRegistry;
        private final MatchingRuleRegistry matchingRuleRegistry;
        private final AttributeTypeRegistry attributeTypeRegistry;
        private String superiorId;
        
        /** The equality OID for this AttributeType */
        private String equalityId;

        /** The MatchingRule associated with the equalityID */
        private MatchingRule equalityMR;
        
        /** The substring OID for this AttributeType */
        private String substrId;
        
        /** The MatchingRule associated with the substrID */
        private MatchingRule substrMR;
        
        /** The ordering OID for this AttributeType */
        private String orderingId;
        
        /** The MatchingRule associated with the orderingID */
        private MatchingRule orderingMR;

        /** The syntax OID for this attributeType */
        private String syntaxId;
        
        /** The Syntax associated with the syntaxID */
        private Syntax syntax;


        public BootstrapAttributeType(String oid, Registries registries)
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


        public void setSchema( String schema )
        {
            super.setSchema( schema );
        }


        public AttributeType getSuperior() throws NamingException
        {
            if ( superiorId == null )
            {
                return null;
            }

            return this.attributeTypeRegistry.lookup( superiorId );
        }


        public void setNames( String[] names )
        {
            super.setNames( names );
        }


        /**
         * @return The MatchingRule associated with the AttributeType
         */
        public MatchingRule getEquality() throws NamingException
        {
            if ( equalityMR == null )
            {
                if ( equalityId != null )
                {
                    equalityMR = this.matchingRuleRegistry.lookup( equalityId );
                }
                else if ( superiorId != null )
                {
                    equalityMR = getSuperior().getEquality();
                }
            }

            return equalityMR;
        }


        public void setEqualityId( String equalityId )
        {
            this.equalityId = equalityId;
        }


        public MatchingRule getSubstr() throws NamingException
        {
            if ( substrMR == null )
            {
                if ( substrId != null )
                {
                    substrMR = matchingRuleRegistry.lookup( substrId );
                }
                else if ( superiorId != null )
                {
                    substrMR = getSuperior().getSubstr();
                }
            }

            return substrMR;
        }


        public boolean isAncestorOf( AttributeType attributeType ) throws NamingException
        {
            return false;
        }


        public boolean isDescentantOf( AttributeType attributeType ) throws NamingException
        {
            return false;
        }


        public void setSubstrId( String substrId )
        {
            this.substrId = substrId;
        }


        /**
         * @return The Ordering Matching Rule associated with this AttributeType
         */
        public MatchingRule getOrdering() throws NamingException
        {
            if ( orderingMR == null )
            {
                if ( orderingId != null )
                {
                    orderingMR = matchingRuleRegistry.lookup( orderingId );
                }
                else if ( superiorId != null )
                {
                    orderingMR = getSuperior().getOrdering();
                }
            }

            return orderingMR;
        }


        public void setOrderingId( String orderingId )
        {
            this.orderingId = orderingId;
        }


        public void setSyntaxId( String syntaxId )
        {
            this.syntaxId = syntaxId;
        }


        /**
         * @return The Syntax associated with the AttributeType
         */
        public Syntax getSyntax() throws NamingException
        {
            if ( syntax == null )
            {
                if ( syntaxId != null )
                {
                    syntax = syntaxRegistry.lookup( syntaxId );
                }
                else if ( superiorId != null )
                {
                    syntax = getSuperior().getSyntax();
                }
            }

            return syntax;
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


        public void setDescription( String description )
        {
            super.setDescription( description );
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
    public static class BootstrapObjectClass extends AbstractSchemaObject implements ObjectClass
    {
        private static final long serialVersionUID = 1L;
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
        public BootstrapObjectClass(String oid, Registries registries)
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

            for ( int ii = 0; ii < superClassIds.length; ii++ )
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

        public boolean isStructural()
        {
            return type == ObjectClassTypeEnum.STRUCTURAL;
        }

        public boolean isAbstract()
        {
            return type == ObjectClassTypeEnum.ABSTRACT;
        }

        public boolean isAuxiliary()
        {
            return type == ObjectClassTypeEnum.AUXILIARY;
        }

        public void setSchema( String schema )
        {
            super.setSchema( schema );
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

            for ( int ii = 0; ii < mustListIds.length; ii++ )
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

            for ( int ii = 0; ii < mayListIds.length; ii++ )
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
