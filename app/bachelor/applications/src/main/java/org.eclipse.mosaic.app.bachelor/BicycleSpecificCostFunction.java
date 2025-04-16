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

package org.eclipse.mosaic.app.bachelor;

import org.eclipse.mosaic.lib.routing.EdgeProperties;
import org.eclipse.mosaic.lib.routing.RoutingCostFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * A route cost function which uses driving time on roads for the costs. The driving
 * time on the connections need to to be updated with travel times or speeds from
 * the simulation.
 *
 * @see #setConnectionSpeedMS(String, double)
 * @see #setConnectionTravelTime(String, long)
 */
public class BicycleSpecificCostFunction implements RoutingCostFunction {

    private final Map<String, Double> affectedConnectionSpeeds = new HashMap<>();
    private final Map<String, Long> affectedConnectionTravelTimes = new HashMap<>();
    private final BicycleBehavior behavior;

    private final Map<String, Double> comfortFactors = Map.of(
            "living_street", 0.8,
            "residential", 0.9
    );

    public BicycleSpecificCostFunction(BicycleBehavior bicycleBehavior) {
        behavior = bicycleBehavior;
    }

    @Override
    public double calculateCosts(final EdgeProperties edgeProperties) {
        String connectionId = edgeProperties.getConnectionId();

        switch (edgeProperties.getWayType()) {
            case "residential":
                return edgeProperties.getLength() * comfortFactors.get("residential");
        }
        return RoutingCostFunction.Shortest.calculateCosts(edgeProperties);
    }

    @Override
    public String getCostFunctionName() {
        return "Fastest with affected connection";
    }

    /**
     * Updates the current speed on the connection which is considered by this cost function.
     *
     * @param connectionId                  the id of the connection
     * @param connectionSpeedMeterPerSecond the speed in m/s
     */
    public void setConnectionSpeedMS(String connectionId, double connectionSpeedMeterPerSecond) {
        affectedConnectionSpeeds.put(connectionId, connectionSpeedMeterPerSecond);
    }


    /**
     * Updates the current travel time in seconds on the connection which is considered by this cost function.
     *
     * @param connectionId      the id of the connection
     * @param travelTimeSeconds the speed in m/s
     */
    public void setConnectionTravelTime(String connectionId, long travelTimeSeconds) {
        affectedConnectionTravelTimes.put(connectionId, travelTimeSeconds);
    }

//    /**
//     * Sets a global penalty which is multiplied with the costs of each connection
//     * which has been updated with a speed or travel time.
//     *
//     * @param penalty the new penalty to use during cost calculation
//     */
//    public BicycleSpecificCostFunction setAdditionalPenalty(double penalty) {
//        this.penalty = penalty;
//        return this;
//    }
}