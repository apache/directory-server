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
 * Test when translation available for current locale -> use locale translation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GermanLanguageTest
{

    @BeforeClass
    public static void setup() throws Exception
    {
        Locale.setDefault( Locale.GERMAN );
    }


    @Test
    public void testErrorTranslationGerman()
    {
        assertEquals( "ERR_1 Das ist Fehler 1", I18n.err( I18n.ERR_1 ) );
        assertEquals( "ERR_2 Das ist Fehler 2 mit 1 Parameter", I18n.err( I18n.ERR_2, 1 ) );
        assertEquals( "ERR_3 Das ist Fehler 3 mit 2 Parameter", I18n.err( I18n.ERR_3, 3, 2 ) );
        assertEquals( "ERR_4 Das ist Fehler 4 mit mehr als 2 Parameter", I18n.err( I18n.ERR_4, 4, "mehr", "als", 2 ) );
    }


    @Test
    public void testMessageTranslationGerman()
    {
        assertEquals( "Das ist Nachricht 1", I18n.msg( "MSG_1" ) );
        assertEquals( "Das ist Nachricht 2 mit 1 Parameter", I18n.msg( "MSG_2", 1 ) );
        assertEquals( "Das ist Nachricht 3 mit 2 Parameter", I18n.msg( "MSG_3", 3, 2 ) );
        assertEquals( "Das ist Nachricht 4 mit mehr als 2 Parameter", I18n.msg( "MSG_4", 4, "mehr", "als", 2 ) );
    }
}
