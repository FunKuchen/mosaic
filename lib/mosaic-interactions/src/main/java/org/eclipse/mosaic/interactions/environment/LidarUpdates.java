/*
 * Copyright (c) 2024 Fraunhofer FOKUS and others. All rights reserved.
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

package org.eclipse.mosaic.interactions.environment;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import org.eclipse.mosaic.lib.objects.UnitData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.sensor.LidarData;
import org.eclipse.mosaic.rti.api.Interaction;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.stream.Collectors;

public class LidarUpdates extends Interaction {
    //TODO what is this?
    private static final long serialVersionUID = 1L;

    /**
     * String identifying the type of this interaction.
     */
    public final static String TYPE_ID = createTypeIdentifier(LidarUpdates.class);

    /**
     * Time at which the next sensor update will be sent.
     */
    private long nextUpdate;


    /**
     * List of {@link VehicleData} identifying vehicles with updated positions.
     */
    private final List<LidarData> updated;



    public LidarUpdates(long time, List<LidarData> updated) {
        super(time);
        this.updated = updated;
    }

    public List<LidarData> getUpdated() {
        return this.updated;
    }

    public long getNextUpdate() {
        return this.nextUpdate;
    }

    public void setNextUpdate(long nextUpdate) {
        this.nextUpdate = nextUpdate;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(5, 17)
                .append(nextUpdate)
                .append(updated)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }

        LidarUpdates other = (LidarUpdates) obj;
        return new EqualsBuilder()
                .append(this.nextUpdate, other.nextUpdate)
                .append(this.updated, other.updated)
                .isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
                .appendSuper(super.toString())
                .append("updated", updated.stream().map(UnitData::getName).collect(Collectors.joining(",")))
                .toString();
    }

}
