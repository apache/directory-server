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
package org.apache.eve.encoder.impl ;


import org.apache.avalon.merlin.unit.AbstractMerlinTestCase ;

import org.apache.eve.encoder.EncoderManager ;


/**
 * Tests the Merlin component within Merlin!
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class MerlinEncoderManagerTest extends AbstractMerlinTestCase
{
    EncoderManager encman = null ;

    
    public MerlinEncoderManagerTest( String a_name )
    {
        super( a_name ) ;
    }

    
    public static void main( String[] args )
    {
        junit.textui.TestRunner.run( MerlinEncoderManagerTest.class ) ;
    }
    
    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception
    {
        super.setUp() ;
        encman = ( EncoderManager ) 
            resolve( "/server/encoder-manager" ) ; 
    }

    
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown()
    {
        super.tearDown() ;
        encman = null ;
    }


    public void testDummy()
    {
        encman.dummy() ;
    }
}
