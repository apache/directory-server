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

package org.apache.directory.server.dns.io.encoder;


/**
 * 3.1 KX RDATA format
 * 
 *    The KX DNS record has the following RDATA format:
 * 
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                  PREFERENCE                   |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                   EXCHANGER                   /
 *     /                                               /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 *    where:
 * 
 *    PREFERENCE      A 16 bit non-negative integer which specifies the
 *                    preference given to this RR among other KX records
 *                    at the same owner.  Lower values are preferred.
 * 
 *    EXCHANGER       A <domain-name> which specifies a host willing to
 *                    act as a mail exchange for the owner name.
 * 
 *    KX records MUST cause type A additional section processing for the
 *    host specified by EXCHANGER.  In the event that the host processing
 *    the DNS transaction supports IPv6, KX records MUST also cause type
 *    AAAA additional section processing.
 * 
 *    The KX RDATA field MUST NOT be compressed.
 */
public class KeyExchangeRecordEncoder
{
}
