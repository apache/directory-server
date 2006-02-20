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
package org.apache.directory.server.kerberos.shared.io.encoder;


public class EncAsRepPartEncoder extends EncKdcRepPartEncoder implements EncoderFactory
{
    /*
     * EncASRepPart ::=    [APPLICATION 25[25]] EncKDCRepPart
     */
    public static final int APPLICATION_CODE = 25;


    public EncAsRepPartEncoder()
    {
        super( APPLICATION_CODE );
    }


    public Encoder getEncoder()
    {
        return new EncAsRepPartEncoder();
    }
}
