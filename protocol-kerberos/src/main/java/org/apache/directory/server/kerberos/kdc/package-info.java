/**
 * Provides the entry point to an instance of the {@link KerberosServer}
 * (KDC), as well as classes common to the KDC's two services:  the
 * Authentication Service (AS) and the Ticket-Granting Service (TGS).  The
 * AS and TGS service implementations follow the Chain of Responsibility
 * pattern, using MINA's {@link IoHandlerChain} support.  Additionally,
 * there is a third chain for pre-authentication, which is a sub-chain
 * of the Authentication Service.
 * <p/>
 * Classes common to all of the chains provide configuration
 * support, the execution context, chain monitors for logging, and chain
 * "links" ({@link IoHandlerCommand}'s) for selecting checksum and
 * encryption types.
 */

package org.apache.directory.server.kerberos.kdc;
