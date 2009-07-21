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

package org.apache.directory.studio.dsmlv2;


import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.directory.studio.dsmlv2.abandonRequest.AbandonRequestTest;
import org.apache.directory.studio.dsmlv2.addRequest.AddRequestTest;
import org.apache.directory.studio.dsmlv2.addResponse.AddResponseTest;
import org.apache.directory.studio.dsmlv2.authRequest.AuthRequestTest;
import org.apache.directory.studio.dsmlv2.authResponse.AuthResponseTest;
import org.apache.directory.studio.dsmlv2.batchRequest.BatchRequestTest;
import org.apache.directory.studio.dsmlv2.batchResponse.BatchResponseTest;
import org.apache.directory.studio.dsmlv2.compareRequest.CompareRequestTest;
import org.apache.directory.studio.dsmlv2.compareResponse.CompareResponseTest;
import org.apache.directory.studio.dsmlv2.delRequest.DelRequestTest;
import org.apache.directory.studio.dsmlv2.delResponse.DelResponseTest;
import org.apache.directory.studio.dsmlv2.errorResponse.ErrorResponseTest;
import org.apache.directory.studio.dsmlv2.extendedRequest.ExtendedRequestTest;
import org.apache.directory.studio.dsmlv2.extendedResponse.ExtendedResponseTest;
import org.apache.directory.studio.dsmlv2.modDNRequest.ModifyDNRequestTest;
import org.apache.directory.studio.dsmlv2.modDNResponse.ModifyDNResponseTest;
import org.apache.directory.studio.dsmlv2.modifyRequest.ModifyRequestTest;
import org.apache.directory.studio.dsmlv2.modifyResponse.ModifyResponseTest;
import org.apache.directory.studio.dsmlv2.searchRequest.SearchRequestTest;
import org.apache.directory.studio.dsmlv2.searchResponse.SearchResponseTest;
import org.apache.directory.studio.dsmlv2.searchResponse.searchResultDone.SearchResultDoneTest;
import org.apache.directory.studio.dsmlv2.searchResponse.searchResultEntry.SearchResultEntryTest;
import org.apache.directory.studio.dsmlv2.searchResponse.searchResultReference.SearchResultReferenceTest;


/**
 * This is the complete Test Suite for DSMLv2 Parser (Request and Response)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AllTests
{
    /**
     * Lauches the Test Suite
     * 
     * @return
     *      the test
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Test for org.apache.directory.studio.dsmlv2.addRequest" );
        //$JUnit-BEGIN$
        suite.addTestSuite( AbandonRequestTest.class );
        suite.addTestSuite( AddRequestTest.class );
        suite.addTestSuite( AddResponseTest.class );
        suite.addTestSuite( AuthRequestTest.class );
        suite.addTestSuite( AuthResponseTest.class );
        suite.addTestSuite( BatchRequestTest.class );
        suite.addTestSuite( BatchResponseTest.class );
        suite.addTestSuite( CompareRequestTest.class );
        suite.addTestSuite( CompareResponseTest.class );
        suite.addTestSuite( DelRequestTest.class );
        suite.addTestSuite( DelResponseTest.class );
        suite.addTestSuite( ErrorResponseTest.class );
        suite.addTestSuite( ExtendedRequestTest.class );
        suite.addTestSuite( ExtendedResponseTest.class );
        suite.addTestSuite( ModifyDNRequestTest.class );
        suite.addTestSuite( ModifyDNResponseTest.class );
        suite.addTestSuite( ModifyRequestTest.class );
        suite.addTestSuite( ModifyResponseTest.class );
        suite.addTestSuite( SearchRequestTest.class );
        suite.addTestSuite( SearchResponseTest.class );
        suite.addTestSuite( SearchResultDoneTest.class );
        suite.addTestSuite( SearchResultEntryTest.class );
        suite.addTestSuite( SearchResultReferenceTest.class );
        //$JUnit-END$
        return suite;
    }
}
