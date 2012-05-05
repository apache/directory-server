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

/**
 * <pre>
 * <p>
 * Platform independent server side Preferences implementation based on ApacheDS.
 * The data is backed by the directory using a specific LDAP schema to map
 * preferences to the LDAP/X.500 namespace.  To make sure you're preferences are
 * using the right preferences factory implementation please check to see the
 * following property is set to our implementation:
 * </p>
 * <p>
 * java.util.prefs.PreferencesFactory=org.apache.ldap.server.prefs.ServerPreferencesFactory
 * <p>
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */

package org.apache.directory.server.core.prefs;


