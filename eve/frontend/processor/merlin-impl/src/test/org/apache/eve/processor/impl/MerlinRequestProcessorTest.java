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
package org.apache.eve.processor.impl ;


import org.apache.avalon.merlin.unit.AbstractMerlinTestCase ;

import org.apache.eve.processor.RequestProcessor ;


/**
 * Tests the Merlin component within Merlin!
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class MerlinRequestProcessorTest extends AbstractMerlinTestCase
{
    RequestProcessor processor = null ;

    
    public MerlinRequestProcessorTest( String a_name )
    {
        super( a_name ) ;
    }

    
    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( MerlinRequestProcessorTest.class ) ;
    }
    
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception
    {
        super.setUp() ;
        processor = ( RequestProcessor ) 
            resolve( "/server/request-processor" ) ; 
    }

    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown()
    {
        super.tearDown() ;
        processor = null ;
    }
}
