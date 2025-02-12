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

package org.eclipse.mosaic.fed.application.app.api.navigation;

import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.road.IConnection;
import org.eclipse.mosaic.lib.objects.road.INode;
import org.eclipse.mosaic.lib.objects.road.IRoadPosition;
import org.eclipse.mosaic.lib.routing.RoutingParameters;
import org.eclipse.mosaic.lib.routing.RoutingPosition;
import org.eclipse.mosaic.lib.routing.RoutingResponse;

/**
 * Interface to access road routing functionalities for server or road side units.
 * The offered methods, for example, provide route calculation from a provided source to
 * a provided target location.<br>
 * In contrast to {@link NavigationModule}, a {@link RoutingModule} can only calculate routes,
 * but is not able to switch to calculated routes. Thus, the {@link RoutingModule} can be used
 * in server units to emulate central routíng service functionalities.
 */
public interface RoutingModule {

    /**
     * Calculates one or more routes from the position of the vehicle to the given target location.
     *
     * @param sourcePosition    The source position of the required route.
     * @param targetPosition    The target position of the required route.
     * @param routingParameters Properties defining the way routes are calculated (e.g. number of routes, weighting).
     * @return The response including a set of routes towards the target.
     */
    RoutingResponse calculateRoutes(RoutingPosition sourcePosition, RoutingPosition targetPosition, RoutingParameters routingParameters);

    /**
     * Returns the node object identified by the given nodeId.
     *
     * @param nodeId The id of the requested node.
     * @return The node object identified by the given nodeId.
     */
    INode getNode(String nodeId);

    /**
     * Returns data for the specified connection id.
     *
     * @param connection the id of the node
     * @return the {@link IConnection} containing data for the specified connection id.
     */
    IConnection getConnection(String connection);

    /**
     * Returns the node object, which is closest to the given {@link GeoPoint}.
     *
     * @param geoPoint The geographical location to search a node for.
     * @return The node object, which is closest to the given location.
     */
    INode getClosestNode(GeoPoint geoPoint);

    /**
     * Returns the road position, which is closest to the given {@link GeoPoint}.
     *
     * @param geoPoint The geographical location to search a road position for.
     * @return The road position, which is closest to the given location.
     */
    IRoadPosition getClosestRoadPosition(GeoPoint geoPoint);

    /**
     * Returns the road position, which is closest to the given {@link GeoPoint}.
     * If two adjacent edges overlap, the heading will be used as a similarity measure.
     *
     * @param geoPoint The geographical location to search a road position for.
     * @param heading  used as a measure of similarity if multiple edges match
     * @return The road position, which is closest to the given location.
     */
    IRoadPosition getClosestRoadPosition(GeoPoint geoPoint, double heading);
}
