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
package org.apache.directory.server.core.schema.bootstrap;


import java.util.Comparator;

import javax.naming.NamingException;

import org.apache.directory.server.core.schema.ConcreteNameComponentNormalizer;
import org.apache.directory.server.core.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.shared.ldap.schema.CachingNormalizer;
import org.apache.directory.shared.ldap.schema.ComparableComparator;
import org.apache.directory.shared.ldap.schema.DeepTrimNormalizer;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.DnComparator;
import org.apache.directory.shared.ldap.schema.NormalizingComparator;
import org.apache.directory.shared.ldap.schema.ObjectIdentifierComparator;


/**
 * Document this class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SystemComparatorProducer extends AbstractBootstrapProducer
{
    public SystemComparatorProducer()
    {
        super( ProducerTypeEnum.COMPARATOR_PRODUCER );
    }


    public void produce( BootstrapRegistries registries, ProducerCallback cb ) throws NamingException
    {
        Comparator comparator;

        /*
         * Straight out of RFC 2252: Section 8
         * =======================================
         ( 2.5.13.0 NAME 'objectIdentifierMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        comparator = new ObjectIdentifierComparator();
        cb.schemaObjectProduced( this, "2.5.13.0", comparator );

        /*
         ( 2.5.13.1 NAME 'distinguishedNameMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.12 )
         */
        comparator = new DnComparator( new ConcreteNameComponentNormalizer( registries.getAttributeTypeRegistry() ) );
        cb.schemaObjectProduced( this, "2.5.13.1", comparator );

        /*
         ( 2.5.13.2 NAME 'caseIgnoreMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new NormalizingComparator( new CachingNormalizer( new DeepTrimToLowerNormalizer() ),
            new ComparableComparator() );
        cb.schemaObjectProduced( this, "2.5.13.2", comparator );

        /*
         ( 2.5.13.3 NAME 'caseIgnoreOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new NormalizingComparator( new CachingNormalizer( new DeepTrimToLowerNormalizer() ),
            new ComparableComparator() );
        cb.schemaObjectProduced( this, "2.5.13.3", comparator );

        /*
         ( 2.5.13.4 NAME 'caseIgnoreSubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        comparator = new NormalizingComparator( new CachingNormalizer( new DeepTrimToLowerNormalizer() ),
            new ComparableComparator() );
        cb.schemaObjectProduced( this, "2.5.13.4", comparator );

        /*
         ( 2.5.13.6 NAME 'caseExactOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.6", comparator );

        /*
         ( 2.5.13.8 NAME 'numericStringMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.36 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.8", comparator );

        /*
         ( 2.5.13.10 NAME 'numericStringSubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.10", comparator );

        /*
         ( 2.5.13.11 NAME 'caseIgnoreListMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.41 )
         */
        comparator = new NormalizingComparator( new CachingNormalizer( new DeepTrimToLowerNormalizer() ),
            new ComparableComparator() );
        cb.schemaObjectProduced( this, "2.5.13.11", comparator );

        /*
         ( 2.5.13.14 NAME 'integerMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.14", comparator );

        /*
         ( 2.5.13.14 NAME 'integerOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.15", comparator );

        /*
         ( 2.5.13.16 NAME 'bitStringMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.6 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.16", comparator );

        /*
         ( 2.5.13.17 NAME 'octetStringMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.17", comparator );

        /*
         ( 2.5.13.18 NAME 'octetStringOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.18", comparator );

        /*
         ( 2.5.13.20 NAME 'telephoneNumberMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.50 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.20", comparator );

        /*
         ( 2.5.13.21 NAME 'telephoneNumberSubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.21", comparator );

        /*
         ( 2.5.13.22 NAME 'presentationAddressMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.43 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.22", comparator );

        /*
         ( 2.5.13.23 NAME 'uniqueMemberMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.34 )
         */
        comparator = new NormalizingComparator( new CachingNormalizer( new DeepTrimNormalizer() ),
            new ComparableComparator() );
        cb.schemaObjectProduced( this, "2.5.13.23", comparator );

        /*
         ( 2.5.13.24 NAME 'protocolInformationMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.42 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.24", comparator );

        /*
         ( 2.5.13.27 NAME 'generalizedTimeMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.27", comparator );

        /*
         ( 2.5.13.28 NAME 'generalizedTimeOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.28", comparator );

        /*
         ( 2.5.13.29 NAME 'integerFirstComponentMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.29", comparator );

        /*
         ( 2.5.13.30 NAME 'objectIdentifierFirstComponentMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.30", comparator );

        /*
         * Straight out of RFC 3698: Section 2.6
         * http://www.faqs.org/rfcs/rfc3698.html
         * =======================================
         * ( 2.5.13.31 NAME 'directoryStringFirstComponentMatch'
         *   SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.31", comparator );

        /*
         ( 1.3.6.1.4.1.1466.109.114.1 NAME 'caseExactIA5Match'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        comparator = new NormalizingComparator( new CachingNormalizer( new DeepTrimNormalizer() ),
            new ComparableComparator() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.109.114.1", comparator );

        /*
         ( 1.3.6.1.4.1.1466.109.114.2 NAME 'caseIgnoreIA5Match'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        comparator = new NormalizingComparator( new CachingNormalizer( new DeepTrimToLowerNormalizer() ),
            new ComparableComparator() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.109.114.2", comparator );

        /*
         * MatchingRules from section 2 of http://www.faqs.org/rfcs/rfc3698.html
         * for Additional MatchingRules

         ( 2.5.13.13 NAME 'booleanMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.7 )

         */

        comparator = new ComparableComparator();
        cb.schemaObjectProduced( this, "2.5.13.13", comparator );

    }
}
