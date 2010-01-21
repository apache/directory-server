/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.i18n;


import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 * Test when missing error code.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MissingErrorCodeTest
{
    @Test
    public void testErrorTranslationMissing()
    {
        assertEquals( "MissingErrorCode ()", I18n.err( "MissingErrorCode" ) );
        assertEquals( "MissingErrorCode (1)", I18n.err( "MissingErrorCode", 1 ) );
        assertEquals( "MissingErrorCode (3,2)", I18n.err( "MissingErrorCode", 3, 2 ) );
        assertEquals( "MissingErrorCode (4,more,than,2)", I18n.err( "MissingErrorCode", 4, "more", "than", 2 ) );
    }


    @Test
    public void testMessageTranslationMissing()
    {
        assertEquals( "MissingMessage ()", I18n.err( "MissingMessage" ) );
        assertEquals( "MissingMessage (1)", I18n.err( "MissingMessage", 1 ) );
        assertEquals( "MissingMessage (3,2)", I18n.err( "MissingMessage", 3, 2 ) );
        assertEquals( "MissingMessage (4,more,than,2)", I18n.err( "MissingMessage", 4, "more", "than", 2 ) );
    }
}
