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


import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.SetupMode;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Mode;
import org.apache.directory.server.integ.SiSuite;
import org.apache.directory.server.operations.add.AddIT;
import org.apache.directory.server.operations.add.AddingEntriesWithSpecialCharactersInRDNIT;
import org.apache.directory.server.operations.bind.BindIT;
import org.apache.directory.server.operations.bind.SimpleBindIT;
import org.apache.directory.server.operations.compare.CompareIT;
import org.apache.directory.server.operations.compare.MatchingRuleCompareIT;
import org.apache.directory.server.operations.delete.DeleteIT;
import org.apache.directory.server.operations.modify.IllegalModificationIT;
import org.apache.directory.server.operations.modify.ModifyAddIT;
import org.apache.directory.server.operations.modify.ModifyReferralIT;
import org.apache.directory.server.operations.modify.ModifyRemoveIT;
import org.apache.directory.server.operations.modify.ModifyReplaceIT;
import org.apache.directory.server.operations.modifydn.ModifyDnReferralIT;
import org.apache.directory.server.operations.modifydn.ModifyRdnIT;
import org.apache.directory.server.operations.modifydn.MoveIT;
import org.apache.directory.server.operations.search.NegationSearchIT;
import org.apache.directory.server.operations.search.SchemaSearchIT;
import org.apache.directory.server.operations.search.SearchIT;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Stock (default configuration) server integration test suite.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiSuite.class )
@Suite.SuiteClasses ( {
        AddingEntriesWithSpecialCharactersInRDNIT.class,
        AddIT.class,
        CompareIT.class,
        MatchingRuleCompareIT.class,
        DeleteIT.class,
        IllegalModificationIT.class,
        ModifyAddIT.class,
        ModifyReferralIT.class,
        ModifyRemoveIT.class,
        ModifyReplaceIT.class,
        ModifyRdnIT.class,
        BindIT.class,
        SimpleBindIT.class,
        MoveIT.class,
        SearchIT.class,
        NegationSearchIT.class,
        SchemaSearchIT.class,
        ModifyDnReferralIT.class
        } )
@CleanupLevel ( Level.SUITE )
@Mode ( SetupMode.ROLLBACK )
public class StockServerISuite
{
}
