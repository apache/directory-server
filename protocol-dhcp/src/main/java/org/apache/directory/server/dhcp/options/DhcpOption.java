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

package org.apache.directory.server.dhcp.options;


import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.dhcp.options.dhcp.BootfileName;
import org.apache.directory.server.dhcp.options.dhcp.ClientIdentifier;
import org.apache.directory.server.dhcp.options.dhcp.DhcpMessageType;
import org.apache.directory.server.dhcp.options.dhcp.IpAddressLeaseTime;
import org.apache.directory.server.dhcp.options.dhcp.MaximumDhcpMessageSize;
import org.apache.directory.server.dhcp.options.dhcp.OptionOverload;
import org.apache.directory.server.dhcp.options.dhcp.ParameterRequestList;
import org.apache.directory.server.dhcp.options.dhcp.RebindingTimeValue;
import org.apache.directory.server.dhcp.options.dhcp.RenewalTimeValue;
import org.apache.directory.server.dhcp.options.dhcp.RequestedIpAddress;
import org.apache.directory.server.dhcp.options.dhcp.ServerIdentifier;
import org.apache.directory.server.dhcp.options.dhcp.TftpServerName;
import org.apache.directory.server.dhcp.options.dhcp.UnrecognizedOption;
import org.apache.directory.server.dhcp.options.dhcp.VendorClassIdentifier;
import org.apache.directory.server.dhcp.options.misc.DefaultFingerServers;
import org.apache.directory.server.dhcp.options.misc.DefaultIrcServers;
import org.apache.directory.server.dhcp.options.misc.DefaultWwwServers;
import org.apache.directory.server.dhcp.options.misc.MobileIpHomeAgents;
import org.apache.directory.server.dhcp.options.misc.NbddServers;
import org.apache.directory.server.dhcp.options.misc.NetbiosNameServers;
import org.apache.directory.server.dhcp.options.misc.NetbiosNodeType;
import org.apache.directory.server.dhcp.options.misc.NetbiosScope;
import org.apache.directory.server.dhcp.options.misc.NisDomain;
import org.apache.directory.server.dhcp.options.misc.NisPlusDomain;
import org.apache.directory.server.dhcp.options.misc.NisPlusServers;
import org.apache.directory.server.dhcp.options.misc.NisServers;
import org.apache.directory.server.dhcp.options.misc.NntpServers;
import org.apache.directory.server.dhcp.options.misc.NtpServers;
import org.apache.directory.server.dhcp.options.misc.Pop3Servers;
import org.apache.directory.server.dhcp.options.misc.SmtpServers;
import org.apache.directory.server.dhcp.options.misc.StdaServers;
import org.apache.directory.server.dhcp.options.misc.StreetTalkServers;
import org.apache.directory.server.dhcp.options.misc.VendorSpecificInformation;
import org.apache.directory.server.dhcp.options.misc.XWindowDisplayManagers;
import org.apache.directory.server.dhcp.options.misc.XWindowFontServers;
import org.apache.directory.server.dhcp.options.perhost.DefaultIpTimeToLive;
import org.apache.directory.server.dhcp.options.perhost.IpForwarding;
import org.apache.directory.server.dhcp.options.perhost.MaximumDatagramSize;
import org.apache.directory.server.dhcp.options.perhost.NonLocalSourceRouting;
import org.apache.directory.server.dhcp.options.perhost.PathMtuAgingTimeout;
import org.apache.directory.server.dhcp.options.perhost.PathMtuPlateauTable;
import org.apache.directory.server.dhcp.options.perhost.PolicyFilter;
import org.apache.directory.server.dhcp.options.perinterface.AllSubnetsAreLocal;
import org.apache.directory.server.dhcp.options.perinterface.BroadcastAddress;
import org.apache.directory.server.dhcp.options.perinterface.InterfaceMtu;
import org.apache.directory.server.dhcp.options.perinterface.MaskSupplier;
import org.apache.directory.server.dhcp.options.perinterface.PerformMaskDiscovery;
import org.apache.directory.server.dhcp.options.perinterface.PerformRouterDiscovery;
import org.apache.directory.server.dhcp.options.perinterface.RouterSolicitationAddress;
import org.apache.directory.server.dhcp.options.perinterface.StaticRoute;
import org.apache.directory.server.dhcp.options.tcp.TcpDefaultTimeToLive;
import org.apache.directory.server.dhcp.options.tcp.TcpKeepaliveGarbage;
import org.apache.directory.server.dhcp.options.tcp.TcpKeepaliveInterval;
import org.apache.directory.server.dhcp.options.vendor.BootFileSize;
import org.apache.directory.server.dhcp.options.vendor.CookieServers;
import org.apache.directory.server.dhcp.options.vendor.DomainName;
import org.apache.directory.server.dhcp.options.vendor.DomainNameServers;
import org.apache.directory.server.dhcp.options.vendor.ExtensionsPath;
import org.apache.directory.server.dhcp.options.vendor.HostName;
import org.apache.directory.server.dhcp.options.vendor.ImpressServers;
import org.apache.directory.server.dhcp.options.vendor.LogServers;
import org.apache.directory.server.dhcp.options.vendor.LprServers;
import org.apache.directory.server.dhcp.options.vendor.MeritDumpFile;
import org.apache.directory.server.dhcp.options.vendor.NameServers;
import org.apache.directory.server.dhcp.options.vendor.ResourceLocationServers;
import org.apache.directory.server.dhcp.options.vendor.RootPath;
import org.apache.directory.server.dhcp.options.vendor.Routers;
import org.apache.directory.server.dhcp.options.vendor.SubnetMask;
import org.apache.directory.server.dhcp.options.vendor.SwapServer;
import org.apache.directory.server.dhcp.options.vendor.TimeOffset;
import org.apache.directory.server.dhcp.options.vendor.TimeServers;
import org.apache.directory.server.i18n.I18n;


/**
 * The Dynamic Host Configuration Protocol (DHCP) provides a framework
 * for passing configuration information to hosts on a TCP/IP network.  
 * Configuration parameters and other control information are carried in
 * tagged data items that are stored in the 'options' field of the DHCP
 * message.  The data items themselves are also called "options."
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class DhcpOption
{
    /**
     * An array of concrete implementations of DhcpOption.
     */
    private static final Class<?>[] OPTION_CLASSES =
        { 
            BootfileName.class, 
            ClientIdentifier.class, 
            DhcpMessageType.class, 
            IpAddressLeaseTime.class,
            MaximumDhcpMessageSize.class, 
            org.apache.directory.server.dhcp.options.dhcp.Message.class,
            OptionOverload.class, 
            ParameterRequestList.class, 
            RebindingTimeValue.class, 
            RenewalTimeValue.class,
            RequestedIpAddress.class, 
            ServerIdentifier.class, 
            TftpServerName.class, 
            VendorClassIdentifier.class,
            ClientIdentifier.class, 
            DhcpMessageType.class, 
            IpAddressLeaseTime.class, 
            MaximumDhcpMessageSize.class,
            OptionOverload.class, 
            ParameterRequestList.class, 
            RebindingTimeValue.class, 
            RenewalTimeValue.class,
            RequestedIpAddress.class, 
            ServerIdentifier.class, 
            TftpServerName.class, 
            UnrecognizedOption.class,
            VendorClassIdentifier.class, 
            DefaultFingerServers.class, 
            DefaultIrcServers.class, 
            DefaultWwwServers.class,
            MobileIpHomeAgents.class, 
            NbddServers.class, 
            NetbiosNameServers.class, 
            NetbiosNodeType.class,
            NetbiosScope.class, 
            NisDomain.class, 
            NisPlusDomain.class, 
            NisPlusServers.class, 
            NisServers.class,
            NntpServers.class, 
            NtpServers.class, 
            Pop3Servers.class, 
            SmtpServers.class, 
            StdaServers.class,
            StreetTalkServers.class, 
            VendorSpecificInformation.class, 
            XWindowDisplayManagers.class,
            XWindowFontServers.class, 
            DefaultIpTimeToLive.class, 
            IpForwarding.class, 
            MaximumDatagramSize.class,
            NonLocalSourceRouting.class, 
            PathMtuAgingTimeout.class, 
            PathMtuPlateauTable.class, 
            PolicyFilter.class,
            AllSubnetsAreLocal.class, 
            BroadcastAddress.class, 
            InterfaceMtu.class, 
            MaskSupplier.class,
            PerformMaskDiscovery.class, 
            PerformRouterDiscovery.class, 
            RouterSolicitationAddress.class,
            StaticRoute.class, 
            TcpDefaultTimeToLive.class, 
            TcpKeepaliveGarbage.class, 
            TcpKeepaliveInterval.class,
            BootFileSize.class, 
            CookieServers.class, 
            DomainName.class, 
            DomainNameServers.class, 
            ExtensionsPath.class,
            HostName.class, 
            ImpressServers.class, 
            LogServers.class, 
            LprServers.class, 
            MeritDumpFile.class,
            NameServers.class, 
            ResourceLocationServers.class, 
            RootPath.class, Routers.class, 
            SubnetMask.class,
            SwapServer.class, 
            TimeOffset.class, 
            TimeServers.class, };

    /**
     * A map of concrete implementations of DhcpOption indexed by tag code.
     */
    private static final Map<Integer, Class<?>> OPTION_CLASS_BY_CODE;

    /**
     * A map of tag codes indexed by OptionClass subclass.
     */
    private static final Map<Class<?>, Integer> CODE_BY_CLASS;

    static
    {
        try
        {
            // initialize the tag-to-class and class-to-tag map
            Map<Integer, Class<?>> classByCode = new HashMap<>();
            Map<Class<?>, Integer> codeByClass = new HashMap<>();
            
            for ( int i = 0; i < OPTION_CLASSES.length; i++ )
            {
                Class<?> dhcpOptionClass = OPTION_CLASSES[i];

                if ( !DhcpOption.class.isAssignableFrom( dhcpOptionClass ) )
                {
                    throw new RuntimeException( I18n.err( I18n.ERR_639, dhcpOptionClass ) );
                }

                DhcpOption dhcpOption = ( DhcpOption ) dhcpOptionClass.newInstance();

                int tagInt = dhcpOption.getTag();
                classByCode.put( tagInt, dhcpOptionClass );
                codeByClass.put( dhcpOptionClass, tagInt );
            }

            OPTION_CLASS_BY_CODE = Collections.unmodifiableMap( classByCode );
            CODE_BY_CLASS = Collections.unmodifiableMap( codeByClass );
        }
        catch ( Exception e )
        {
            throw new RuntimeException( I18n.err( I18n.ERR_640 ), e );
        }
    }


    public static Class<?> getClassByTag( int tag )
    {
        return OPTION_CLASS_BY_CODE.get( tag );
    }


    public static int getTagByClass( Class<?> c )
    {
        return CODE_BY_CLASS.get( c );
    }

    /**
     * The default data array used for simple (unparsed) options.
     */
    private byte[] data;


    /**
     * Get the option's code tag.
     * 
     * @return byte
     */
    public abstract byte getTag();


    /**
     * Set the data (wire format) from a byte array. The default implementation
     * just records the data as a byte array. Subclasses may parse the data into
     * something more meaningful.
     * 
     * @param data
     */
    public void setData( byte[] data )
    {
        this.data = data;
    }


    /**
     * Get the data (wire format) into a byte array. Subclasses must provide an
     * implementation which serializes the parsed data back into a byte array if
     * they override {@link #setData(byte[])}.
     * 
     * @return byte[]
     */
    public byte[] getData()
    {
        return data;
    }


    public final void writeTo( ByteBuffer out )
    {
        out.put( getTag() );

        // FIXME: handle continuation, i.e. options longer than 128 bytes?
        byte[] data = getData();

        if ( data.length > 255 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_641 ) );
        }

        out.put( ( byte ) data.length );
        out.put( data );
    }
}
