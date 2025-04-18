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

package org.eclipse.mosaic.app.bachelor;

import org.eclipse.mosaic.fed.application.app.AbstractApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.VehicleClass;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.lib.routing.CandidateRoute;
import org.eclipse.mosaic.lib.routing.RoutingCostFunction;
import org.eclipse.mosaic.lib.routing.RoutingParameters;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BicycleRoutingApp extends AbstractApplication<VehicleOperatingSystem> implements VehicleApplication {

    private boolean firstUpdate = true;
    private VehicleRoute initialRoute;

    @Override
    public void onVehicleUpdated(@Nullable VehicleData previousVehicleData, @Nonnull VehicleData updatedVehicleData) {
        if (firstUpdate && updatedVehicleData.getRoadPosition() != null)  {
            initialRoute = getOs().getNavigationModule().getCurrentRoute();
            getLog().infoSimTime(this, "Initial route has length {} and connections {}", initialRoute.getLength(), initialRoute.getConnectionIds());
            BicycleBehavior behaviorPattern = new BicycleBehavior();

            GeoPoint currentPoint = this.getOs().getNavigationModule().getCurrentPosition();
            GeoPoint targetPoint = this.getOs().getNavigationModule().getTargetPosition();
            RoutingCostFunction bicycleRoutingCostFunction = new BicycleSpecificCostFunction(behaviorPattern);
            RoutingParameters bicycleParameters = new RoutingParameters()
                    .alternativeRoutes(3)
                    .considerTurnCosts(true)
                    .costFunction(bicycleRoutingCostFunction)
                    .vehicleClass(VehicleClass.Bicycle);
            CandidateRoute bestRoute = this.getOs().getNavigationModule().calculateRoutes(targetPoint, bicycleParameters)
                    .getBestRoute();
            getOs().getNavigationModule().switchRoute(bestRoute);
            getLog().infoSimTime(this, "Switched to route with length {} and connections {}", bestRoute.getLength(), bestRoute.getConnectionIds());
            firstUpdate = false;
        }
    }

    @Override
    public void onStartup() {

    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void processEvent(Event event) throws Exception {

    }
}
