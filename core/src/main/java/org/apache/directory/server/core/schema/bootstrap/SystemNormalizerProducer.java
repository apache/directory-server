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


import javax.naming.NamingException;

import org.apache.directory.server.core.schema.DnNormalizer;
import org.apache.directory.server.core.schema.bootstrap.ProducerTypeEnum;
import org.apache.directory.shared.ldap.schema.CachingNormalizer;
import org.apache.directory.shared.ldap.schema.DeepTrimNormalizer;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectIdentifierNormalizer;


/**
 * A bootstrap producer which creates and announces newly created Normalizers
 * for various matchingRules in the core schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SystemNormalizerProducer extends AbstractBootstrapProducer
{
    public SystemNormalizerProducer()
    {
        super( ProducerTypeEnum.NORMALIZER_PRODUCER );
    }


    public void produce( BootstrapRegistries registries, ProducerCallback cb ) throws NamingException
    {
        Normalizer normalizer;

        /*
         * Straight out of RFC 2252: Section 8
         * =======================================

         ( 2.5.13.1 NAME 'distinguishedNameMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.12 )
         */
        normalizer = new CachingNormalizer( new DnNormalizer( registries.getAttributeTypeRegistry() ) );
        cb.schemaObjectProduced( this, "2.5.13.1", normalizer );

        /*
         ( 1.3.6.1.4.1.1466.109.114.2 NAME 'caseIgnoreIA5Match'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        normalizer = new CachingNormalizer( new DeepTrimToLowerNormalizer() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.109.114.2", normalizer );

        /*
         ( 2.5.13.11 NAME 'caseIgnoreListMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.41 )
         */
        normalizer = new CachingNormalizer( new DeepTrimToLowerNormalizer() );
        cb.schemaObjectProduced( this, "2.5.13.11", normalizer );

        /*
         ( 2.5.13.2 NAME 'caseIgnoreMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new CachingNormalizer( new DeepTrimToLowerNormalizer() );
        cb.schemaObjectProduced( this, "2.5.13.2", normalizer );

        /*
         ( 2.5.13.3 NAME 'caseIgnoreOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new CachingNormalizer( new DeepTrimToLowerNormalizer() );
        cb.schemaObjectProduced( this, "2.5.13.3", normalizer );

        /*
         ( 2.5.13.4 NAME 'caseIgnoreSubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        normalizer = new CachingNormalizer( new DeepTrimToLowerNormalizer() );
        cb.schemaObjectProduced( this, "2.5.13.4", normalizer );

        /*
         ( 2.5.13.6 NAME 'caseExactOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.6", normalizer );

        /*
         ( 2.5.13.0 NAME 'objectIdentifierMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        normalizer = new ObjectIdentifierNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.0", normalizer );

        /*
         ( 2.5.13.8 NAME 'numericStringMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.36 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.8", normalizer );

        /*
         ( 2.5.13.10 NAME 'numericStringSubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.10", normalizer );

        /*
         ( 2.5.13.14 NAME 'integerMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.14", normalizer );

        /*
         ( 2.5.13.14 NAME 'integerOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.15", normalizer );

        /*
         ( 2.5.13.16 NAME 'bitStringMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.6 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.16", normalizer );

        /*
         ( 2.5.13.17 NAME 'octetStringMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.17", normalizer );

        /*
         ( 2.5.13.18 NAME 'octetStringOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.40 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.18", normalizer );

        /*
         ( 2.5.13.20 NAME 'telephoneNumberMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.50 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.20", normalizer );

        /*
         ( 2.5.13.21 NAME 'telephoneNumberSubstringsMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.58 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.21", normalizer );

        /*
         ( 2.5.13.22 NAME 'presentationAddressMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.43 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.22", normalizer );

        /*
         ( 2.5.13.23 NAME 'uniqueMemberMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.34 )
         */
        normalizer = new CachingNormalizer( new DeepTrimNormalizer() );
        cb.schemaObjectProduced( this, "2.5.13.23", normalizer );

        /*
         ( 2.5.13.24 NAME 'protocolInformationMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.42 )
         */
        normalizer = new CachingNormalizer( new DeepTrimNormalizer() );
        cb.schemaObjectProduced( this, "2.5.13.24", normalizer );

        /*
         ( 2.5.13.27 NAME 'generalizedTimeMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        normalizer = new CachingNormalizer( new DeepTrimNormalizer() );
        cb.schemaObjectProduced( this, "2.5.13.27", normalizer );

        /*
         ( 2.5.13.28 NAME 'generalizedTimeOrderingMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.24 )
         */
        normalizer = new CachingNormalizer( new DeepTrimNormalizer() );
        cb.schemaObjectProduced( this, "2.5.13.28", normalizer );

        /*
         ( 2.5.13.29 NAME 'integerFirstComponentMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.29", normalizer );

        /*
         ( 2.5.13.30 NAME 'objectIdentifierFirstComponentMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.38 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.30", normalizer );

        /*
         * Straight out of RFC 3698: Section 2.6
         * http://www.faqs.org/rfcs/rfc3698.html
         * =======================================
         * ( 2.5.13.31 NAME 'directoryStringFirstComponentMatch'
         *   SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 )
         */
        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.31", normalizer );

        /*
         ( 1.3.6.1.4.1.1466.109.114.1 NAME 'caseExactIA5Match'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.26 )
         */
        normalizer = new CachingNormalizer( new DeepTrimNormalizer() );
        cb.schemaObjectProduced( this, "1.3.6.1.4.1.1466.109.114.1", normalizer );

        /*
         * MatchingRules from section 2 of http://www.faqs.org/rfcs/rfc3698.html
         * for Additional MatchingRules

         ( 2.5.13.13 NAME 'booleanMatch'
         SYNTAX 1.3.6.1.4.1.1466.115.121.1.7 )

         */

        normalizer = new NoOpNormalizer();
        cb.schemaObjectProduced( this, "2.5.13.13", normalizer );
    }
}
