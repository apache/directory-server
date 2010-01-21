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

import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Test when no translation available for current locale -> use default translation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UnknownLanguageTest
{

    @BeforeClass
    public static void setup() throws Exception
    {
        Locale.setDefault( Locale.TRADITIONAL_CHINESE );
    }


    @Test
    public void testErrorTranslationUnknownLocale()
    {
        assertEquals( "ERR_1 This is error 1", I18n.err( I18n.ERR_1 ) );
        assertEquals( "ERR_2 This is error 2 with 1 parameter", I18n.err( I18n.ERR_2, 1 ) );
        assertEquals( "ERR_3 This is error 3 with 2 parameters", I18n.err( I18n.ERR_3, 3, 2 ) );
        assertEquals( "ERR_4 This is error 4 with more than 2 parameters", I18n.err( I18n.ERR_4, 4, "more", "than", 2 ) );
    }


    @Test
    public void testMessageTranslationUnknownLocale()
    {
        assertEquals( "This is message 1", I18n.msg( "MSG_1" ) );
        assertEquals( "This is message 2 with 1 parameter", I18n.msg( "MSG_2", 1 ) );
        assertEquals( "This is message 3 with 2 parameters", I18n.msg( "MSG_3", 3, 2 ) );
        assertEquals( "This is message 4 with more than 2 parameters", I18n.msg( "MSG_4", 4, "more", "than", 2 ) );
    }
}
