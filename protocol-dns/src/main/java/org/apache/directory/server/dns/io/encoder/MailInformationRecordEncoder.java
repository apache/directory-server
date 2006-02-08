/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.server.dns.io.encoder;

/**
 * 3.3.7. MINFO RDATA format (EXPERIMENTAL)
 * 
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                    RMAILBX                    /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     /                    EMAILBX                    /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 * 
 * where:
 * 
 * RMAILBX         A <domain-name> which specifies a mailbox which is
 *                 responsible for the mailing list or mailbox.  If this
 *                 domain name names the root, the owner of the MINFO RR is
 *                 responsible for itself.  Note that many existing mailing
 *                 lists use a mailbox X-request for the RMAILBX field of
 *                 mailing list X, e.g., Msgroup-request for Msgroup.  This
 *                 field provides a more general mechanism.
 * 
 * EMAILBX         A <domain-name> which specifies a mailbox which is to
 *                 receive error messages related to the mailing list or
 *                 mailbox specified by the owner of the MINFO RR (similar
 *                 to the ERRORS-TO: field which has been proposed).  If
 *                 this domain name names the root, errors should be
 *                 returned to the sender of the message.
 * 
 * MINFO records cause no additional section processing.  Although these
 * records can be associated with a simple mailbox, they are usually used
 * with a mailing list.
 */
public class MailInformationRecordEncoder
{
}
