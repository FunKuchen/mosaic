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
 * A route cost function which uses the way type of each edge, the fact that cyclists are more comfortable on bike lanes, and a cyclists
 * individual risk aversion to calculate costs for each edge.
 **/
public class BicycleSpecificCostFunction implements RoutingCostFunction {

    /**
     * BicycleBehavior for the current unit that created this cost function.
     */
    private final BicycleBehavior behavior;

    /**
     * Default cost scaling values for each way type in the scenario.
     */
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

    /**
     * Constructor for the BicycleSpecificCostFunction.
     *
     * @param bicycleBehavior The BicycleBehavior for the unit that creates this cost function.
     */
    public BicycleSpecificCostFunction(BicycleBehavior bicycleBehavior) {
        behavior = bicycleBehavior;
    }

    /**
     * Calculates the cost of traversing each edge based on way type comfort, presence of bike lanes, and this cyclists individual
     * preferences.
     *
     * @param edgeProperties - collection of various attributes the edge provides
     * @return Costs for this edge as a double.
     */
    @Override
    public double calculateCosts(final EdgeProperties edgeProperties) {
        // This variable is relevant for all roads. Currently, the database does not include information on all lanes. However, bicycle
        // lanes are often modeled as a lane of a bigger road. Because these bike lanes improve cycling quality on this road, the existence
        // of a bike lane on a bigger road should be a factor.
        double bikeLaneFactor = getBikeLaneFactor(edgeProperties);

        // If the way type is not in the map initialized above, assign costs based on edge length only.
        if (!comfortFactors.containsKey(edgeProperties.getWayType())) {
            return RoutingCostFunction.Shortest.calculateCosts(edgeProperties);
        }

        // Otherwise calculate the costs like this:
        // - Costs are always at least the length of the edge.
        // - The closer the comfort factor is to 0, the better, as the term that is multiplied with the edge length will be smaller.
        // - If there is a bike lane on the edge, use the bicycles individual bikeLaneFactor as well, which will reduce the costs further.
        // - If the risk aversion is high (=1), the comfort factor and bike lane factor will play a big role in determining the costs of
        //   this edge. Otherwise (=0) the cyclist is not influenced by these factors as much, if at all.
        return edgeProperties.getLength() * (1 + comfortFactors.get(edgeProperties.getWayType()) * bikeLaneFactor * behavior.riskAversion);
    }

    /**
     * Assign the bike lane factor. It is 1.0 if the edge does not have a bike lane, but this units individual factor if there is one.
     *
     * @param edgeProperties - collection of various attributes the edge provides.
     * @return A double value that describes how much the current edges costs are affected by the presence or absence of a bike lane.
     */
    private double getBikeLaneFactor(EdgeProperties edgeProperties) {
        double bikeLaneFactor = 1.0;
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