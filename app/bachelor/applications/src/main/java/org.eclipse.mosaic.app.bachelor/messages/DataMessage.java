/*
 * Copyright (c) 2025 Fraunhofer FOKUS and others. All rights reserved.
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

package org.eclipse.mosaic.app.bachelor.messages;

import org.eclipse.mosaic.lib.objects.v2x.EncodedPayload;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.v2x.V2xMessage;

import java.util.List;
import javax.annotation.Nonnull;

public class DataMessage extends V2xMessage {

    public final List<String[]> bikeData;

    public DataMessage(MessageRouting routing, final List<String[]> bikeData) {
        super(routing);
        this.bikeData = bikeData;
    }

    private EncodedPayload encode(List<String[]> bikeData) {
        if (bikeData == null) {
            return EncodedPayload.EMPTY_PAYLOAD;
        } else {
            return new EncodedPayload((long) bikeData.size() * bikeData.get(0).length);
        }
    }

    @Nonnull
    @Override
    public EncodedPayload getPayload() {
        return encode(bikeData);
    }
}
