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
package org.apache.directory.mitosis.service.protocol;


public class Constants
{
    public static final int LOGIN = 0x00;
    public static final int LOGIN_ACK = 0x01;
    public static final int GET_UPDATE_VECTOR = 0x02;
    public static final int GET_UPDATE_VECTOR_ACK = 0x03;
    public static final int LOG_ENTRY = 0x04;
    public static final int LOG_ENTRY_ACK = 0x05;
    public static final int BEGIN_LOG_ENTRIES = 0x06;
    public static final int BEGIN_LOG_ENTRIES_ACK = 0x07;
    public static final int END_LOG_ENTRIES = 0x08;
    public static final int END_LOG_ENTRIES_ACK = 0x09;

    public static final int OK = 0;
    public static final int NOT_OK = -1;


    private Constants()
    {
    }
}
