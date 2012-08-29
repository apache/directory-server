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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.IOException;

import jdbm.helper.Serializer;

import org.apache.directory.shared.util.Strings;


/**
 * A {@link Serializer} for Longs
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UuidSerializer implements Serializer
{
    private static final long serialVersionUID = 237756689544852128L;
    public static final UuidSerializer INSTANCE = new UuidSerializer();


    public byte[] serialize( Object o ) throws IOException
    {
        String uuid = ( String ) o;

        return Strings.getBytesUtf8( uuid );
    }


    public Object deserialize( byte[] bytes ) throws IOException
    {
        return Strings.utf8ToString( bytes );
    }
}
