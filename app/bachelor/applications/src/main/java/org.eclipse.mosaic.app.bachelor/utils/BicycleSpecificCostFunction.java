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

package org.eclipse.mosaic.app.bachelor.utils;

import org.eclipse.mosaic.lib.routing.EdgeProperties;
import org.eclipse.mosaic.lib.routing.RoutingCostFunction;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A route cost function which uses driving time on roads for the costs. The driving
 * time on the connections need to to be updated with travel times or speeds from
 * the simulation.
 * */
public class BicycleSpecificCostFunction implements RoutingCostFunction {

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
            {"secondary_link", 1.2},
            {"primary", 1.5},
            {"primary_link", 1.5},
            {"steps", 2.0},
            {"trunk", 2.0}
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (Double) data[1]));

    public BicycleSpecificCostFunction(BicycleBehavior bicycleBehavior) {
        behavior = bicycleBehavior;
    }

    @Override
    public double calculateCosts(final EdgeProperties edgeProperties) {
        // This variable is relevant for all roads. Currently, the database does not include information on all lanes. However, bicycle
        // lanes are often modeled as a lane of a bigger road. Because these bike lanes improve cycling quality on this road, the existance
        // of a bike lane on a bigger road should be a factor.
        double bikeLaneFactor = getBikeLaneFactor(edgeProperties);

        if (!comfortFactors.containsKey(edgeProperties.getWayType())) {
            return RoutingCostFunction.Shortest.calculateCosts(edgeProperties);
        }

        return edgeProperties.getLength() * (1 + comfortFactors.get(edgeProperties.getWayType()) * bikeLaneFactor * behavior.riskAversion);
    }

    private double getBikeLaneFactor(EdgeProperties edgeProperties) {
        double bikeLaneFactor = 1;
        boolean edgeHasBikeLane = edgeProperties.getHasBikeLane();
        if (edgeHasBikeLane) {
            bikeLaneFactor = behavior.bikeLaneFactor;
        }
        return bikeLaneFactor;
    }

    @Override
    public String getCostFunctionName() {
        return "Individual cyclist cost function";
    }
}