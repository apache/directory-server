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
package org.apache.directory.server.suites;


import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkSuite;
import org.apache.directory.server.operations.add.AddIT;
import org.apache.directory.server.operations.add.AddingEntriesWithSpecialCharactersInRDNIT;
import org.apache.directory.server.operations.bind.BindIT;
import org.apache.directory.server.operations.bind.MiscBindIT;
import org.apache.directory.server.operations.bind.SaslBindIT;
import org.apache.directory.server.operations.bind.SimpleBindIT;
import org.apache.directory.server.operations.compare.CompareIT;
import org.apache.directory.server.operations.compare.MatchingRuleCompareIT;
import org.apache.directory.server.operations.delete.DeleteIT;
import org.apache.directory.server.operations.extended.ExtendedIT;
import org.apache.directory.server.operations.extended.StoredProcedureIT;
import org.apache.directory.server.operations.modify.IllegalModificationIT;
import org.apache.directory.server.operations.modify.ModifyAddIT;
import org.apache.directory.server.operations.modify.ModifyMultipleChangesIT;
import org.apache.directory.server.operations.modify.ModifyReferralIT;
import org.apache.directory.server.operations.modify.ModifyRemoveIT;
import org.apache.directory.server.operations.modify.ModifyReplaceIT;
import org.apache.directory.server.operations.modifydn.ModifyDnReferralIT;
import org.apache.directory.server.operations.modifydn.ModifyRdnIT;
import org.apache.directory.server.operations.modifydn.MoveIT;
import org.apache.directory.server.operations.search.IndexedNegationSearchIT;
import org.apache.directory.server.operations.search.NegationSearchIT;
import org.apache.directory.server.operations.search.PagedSearchIT;
import org.apache.directory.server.operations.search.PersistentSearchIT;
import org.apache.directory.server.operations.search.ReferralSearchIT;
import org.apache.directory.server.operations.search.SchemaSearchIT;
import org.apache.directory.server.operations.search.SearchIT;
import org.apache.directory.server.operations.search.SearchLimitsIT;
import org.apache.directory.server.ssl.LdapsIT;
import org.apache.directory.server.ssl.StartTlsIT;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Stock (default configuration) server integration test suite.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( FrameworkSuite.class )
@Suite.SuiteClasses ( {
        AddingEntriesWithSpecialCharactersInRDNIT.class,
        AddIT.class,

        BindIT.class,
        MiscBindIT.class,
        SaslBindIT.class,
        SimpleBindIT.class,

        CompareIT.class,
        MatchingRuleCompareIT.class,
        
        DeleteIT.class,
        ExtendedIT.class,
        StoredProcedureIT.class,
        
        IllegalModificationIT.class,
        ModifyAddIT.class,
        ModifyMultipleChangesIT.class,
        ModifyReferralIT.class,
        ModifyRemoveIT.class,
        ModifyReplaceIT.class,

        ModifyDnReferralIT.class,
        ModifyRdnIT.class,
        MoveIT.class,

        IndexedNegationSearchIT.class,
        NegationSearchIT.class,
        PagedSearchIT.class,
        PersistentSearchIT.class,
        ReferralSearchIT.class,
        SchemaSearchIT.class,
        SearchIT.class,
        SearchLimitsIT.class,

        LdapsIT.class,
        StartTlsIT.class
        } )
@CreateDS( 
    name = "SuiteDS",
    partitions =
    {
        @CreatePartition(
            name = "example",
            suffix = "dc=example,dc=com",
            contextEntry = @ContextEntry( 
                entryLdif =
                    "dn: dc=example,dc=com\n" +
                    "dc: example\n" +
                    "objectClass: top\n" +
                    "objectClass: domain\n\n" ),
            indexes = 
            {
                @CreateIndex( attribute = "objectClass" ),
                @CreateIndex( attribute = "dc" ),
                @CreateIndex( attribute = "ou" )
            } )
    } )
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" ), 
        @CreateTransport( protocol = "LDAPS" ) 
    })
public class StockServerISuite
{
}
