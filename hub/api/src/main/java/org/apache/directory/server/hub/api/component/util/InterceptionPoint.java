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

package org.apache.directory.server.hub.api.component.util;


public enum InterceptionPoint
{
    START,
    PRE_NORM,
    NORM,
    POST_NORM,
    PRE_AUTHN,
    AUTHN,
    POST_AUTHN,
    PRE_REFERRAL,
    REFERRAL,
    POST_REFERRAL,
    PRE_ACI,
    ACI,
    POST_ACI,
    PRE_AUTHZ,
    AUTHZ,
    POST_AUTHZ,
    PRE_EXCEPTION,
    EXCEPTION,
    POST_EXCEPTION,
    PRE_KEYDRV,
    KEYDRV,
    POST_KEYDRV,
    PRE_PASSHASH,
    PASSHASH,
    POST_PASSHASH,
    PRE_SCHEMA,
    SCHEMA,
    POST_SCHEMA,
    PRE_OPERAT,
    OPERAT,
    POST_OPERAT,
    PRE_COLLAT,
    COLLAT,
    POST_COLLAT,
    PRE_SUBENTRY,
    SUBENTRY,
    POST_SUBENTRY,
    PRE_EVENT,
    EVENT,
    POST_EVENT,
    PRE_TRIGGER,
    TRIGGER,
    POST_TRIGGER,
    END
}
