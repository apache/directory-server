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
package org.apache.directory.server.kerberos.shared.messages.value.flags;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


/**
 * Test for AbstractKerberosFlags
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AbstractKerberosFlagsTest
{

    AbstractKerberosFlags akf;


    @Before
    public void setUp() throws Exception
    {
        akf = new AbstractKerberosFlags()
        {
            private static final long serialVersionUID = 1L;
        };
    }


    @Test
    public void testFlags()
    {
        // MAX_VALUE is not a real ticket flag and will cause an Out Of Bounds Exception,
        // so skip this
        TicketFlag[] ticketFlags = new TicketFlag[TicketFlag.values().length - 1];
        int i = 0;
        for ( TicketFlag tf : TicketFlag.values() )
        {
            if ( !tf.equals( TicketFlag.MAX_VALUE ) )
            {
                ticketFlags[i] = tf;
            }
            i++;
        }

        boolean setFlag = true;
        for ( TicketFlag ticketFlag : ticketFlags )
        {
            // Only set every 2nd ticket flag
            if ( setFlag )
            {
                akf.setFlag( ticketFlag.getOrdinal() );
            }
            setFlag = !setFlag;
        }

        setFlag = true;
        for ( TicketFlag ticketFlag : ticketFlags )
        {
            // Only every 2nd ticket flag is set
            if ( setFlag )
            {
                assertTrue( "TicketFlag: " + ticketFlag.toString(), akf.isFlagSet( ticketFlag ) );
            }
            else
            {
                assertFalse( "TicketFlag: " + ticketFlag.toString(), akf.isFlagSet( ticketFlag ) );
            }
            setFlag = !setFlag;
        }
    }
}
