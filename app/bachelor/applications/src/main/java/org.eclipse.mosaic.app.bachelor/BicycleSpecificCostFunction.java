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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private final Map<String, Double> comfortFactors = Stream.of(new Object[][]{
            {"cycleway", 0.5},
            {"footway", 0.6},
            {"path", 0.6},
            {"pedestrian", 0.7},
            {"living_street", 0.8},
            {"residential", 0.9},
            {"service", 0.9},
            {"tertiary", 1.0},
            {"tertiary_link", 1.0},
            {"unclassified", 1.0},
            {"secondary", 1.2},
            {"primary", 1.5},
            {"trunk", 2.0}
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (Double) data[1]));


    public BicycleSpecificCostFunction(BicycleBehavior bicycleBehavior) {
        behavior = bicycleBehavior;
    }

    @Override
    public double calculateCosts(final EdgeProperties edgeProperties) {

        return switch (edgeProperties.getWayType()) {
            case "cycleway" -> edgeProperties.getLength() * (comfortFactors.get("cycleway") * ((10 - behavior.riskAversion) / 10));
            case "footway" -> edgeProperties.getLength() * (comfortFactors.get("footway") * ((10 - behavior.riskAversion) / 10));
            case "path" -> edgeProperties.getLength() * (comfortFactors.get("path") * ((10 - behavior.riskAversion) / 10));
            case "pedestrian" -> edgeProperties.getLength() * (comfortFactors.get("pedestrian") * ((10 - behavior.riskAversion) / 10));
            case "living_street" -> edgeProperties.getLength() * (comfortFactors.get("living_street") * ((10 - behavior.riskAversion) / 10));
            case "residential" -> edgeProperties.getLength() * (comfortFactors.get("residential") * ((10 - behavior.riskAversion) / 10));
            case "service" -> edgeProperties.getLength() * (comfortFactors.get("service") * ((10 - behavior.riskAversion) / 10));
            case "tertiary" -> edgeProperties.getLength() * (comfortFactors.get("tertiary") * ((10 - behavior.riskAversion) / 10));
            case "tertiary_link" -> edgeProperties.getLength() * (comfortFactors.get("tertiary_link") * ((10 - behavior.riskAversion) / 10));
            case "unclassified" -> edgeProperties.getLength() * (comfortFactors.get("unclassified") * ((10 - behavior.riskAversion) / 10));
            default -> RoutingCostFunction.Shortest.calculateCosts(edgeProperties);
        };
    }

    @Override
    public String getCostFunctionName() {
        return "Individual cyclist cost function";
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
}