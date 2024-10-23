/*
 * Copyright (c) 2020 Fraunhofer FOKUS and others. All rights reserved.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contact: mosaic@fokus.fraunhofer.de
 */

package org.eclipse.mosaic.lib.objects.addressing;

import org.eclipse.mosaic.lib.enums.AdHocChannel;
import org.eclipse.mosaic.lib.enums.DestinationType;
import org.eclipse.mosaic.lib.enums.ProtocolType;
import org.eclipse.mosaic.lib.geo.GeoArea;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;

import java.net.Inet4Address;

/**
 * Central API for obtaining {@link MessageRouting} for sending {@link org.eclipse.mosaic.lib.objects.v2x.V2xMessage}s via
 * ad hoc communication.
 */
public class AdHocMessageRoutingBuilder {

    private final SourceAddressContainer sourceAddressContainer;
    private AdHocChannel channel = AdHocChannel.CCH;
    private NetworkAddress destination;

    /**
     * The constructor for {@link AdHocMessageRoutingBuilder}.
     *
     * @param hostName       name of the sending entity
     * @param sourcePosition position of the sending entity
     */
    public AdHocMessageRoutingBuilder(String hostName, GeoPoint sourcePosition) {
        Inet4Address address = IpResolver.getSingleton().lookup(hostName);
        if (address == null) {
            throw new IllegalArgumentException("Given hostname " + hostName + " has no registered IP address");
        }

        this.sourceAddressContainer = new SourceAddressContainer(
                new NetworkAddress(address),
                hostName,
                sourcePosition
        );
    }

    private MessageRouting build(DestinationAddressContainer dac) {
        return new MessageRouting(dac, sourceAddressContainer);
    }

    /**
     * Sets a specific {@link AdHocChannel} for the {@link MessageRouting}.
     *
     * @param adHocChannel specific ad hoc channel {@link AdHocChannel}
     * @return this builder
     */
    public AdHocMessageRoutingBuilder viaChannel(AdHocChannel adHocChannel) {
        this.channel = adHocChannel;
        return this;
    }

    public AdHocMessageRoutingBuilder destination(byte[] ipAddress) {
        this.destination = new NetworkAddress(ipAddress);
        return this;
    }

    public AdHocMessageRoutingBuilder destination(Inet4Address ipAddress) {
        this.destination = new NetworkAddress(ipAddress);
        return this;
    }

    public AdHocMessageRoutingBuilder destination(NetworkAddress ipAddress) {
        this.destination = ipAddress;
        return this;
    }

    public AdHocMessageRoutingBuilder destination(String receiverName) {
        this.destination = new NetworkAddress(IpResolver.getSingleton().nameToIp(receiverName));
        return this;
    }

    public AdHocMessageRoutingBuilder broadcast() {
        this.destination = new NetworkAddress(NetworkAddress.BROADCAST_ADDRESS);
        return this;
    }

    public MessageRouting topological(int hops) {
        return build(new DestinationAddressContainer(
                DestinationType.AD_HOC_TOPOCAST,
                destination,
                channel,
                require8BitTtl(hops),
                null,
                ProtocolType.UDP
        ));
    }

    public MessageRouting topological() {
        return topological(1);
    }

    public MessageRouting singlehop() {
        return topological(1);
    }

    public MessageRouting geographical(GeoArea area) {
        return build(new DestinationAddressContainer(
                DestinationType.AD_HOC_GEOCAST,
                destination,
                channel,
                null,
                area,
                ProtocolType.UDP
        ));
    }

    /**
     * The maximum time to live (TTL).
     */
    private final static int MAXIMUM_TTL = 255;

    private static int require8BitTtl(final int ttl) {
        if (ttl > MAXIMUM_TTL || ttl < 0) {
            throw new IllegalArgumentException("Passed time to live shouldn't exceed 8-bit limit!");
        }
        return ttl;
    }
}
