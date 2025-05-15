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

import org.eclipse.mosaic.app.bachelor.config.CBicycleApplication;
import org.eclipse.mosaic.app.bachelor.messages.DataMessage;
import org.eclipse.mosaic.app.bachelor.utils.BicycleBehavior;
import org.eclipse.mosaic.app.bachelor.utils.BicycleSpecificCostFunction;
import org.eclipse.mosaic.fed.application.ambassador.UnitSimulator;
import org.eclipse.mosaic.fed.application.ambassador.simulation.VehicleUnit;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.SimplePerceptionConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.index.objects.VehicleObject;
import org.eclipse.mosaic.fed.application.app.ConfigurableApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.VehicleClass;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.math.Vector3d;
import org.eclipse.mosaic.lib.math.VectorUtils;
import org.eclipse.mosaic.lib.objects.v2x.MessageRouting;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleType;
import org.eclipse.mosaic.lib.routing.CandidateRoute;
import org.eclipse.mosaic.lib.routing.RoutingCostFunction;
import org.eclipse.mosaic.lib.routing.RoutingParameters;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BicycleRoutingApp extends ConfigurableApplication<CBicycleApplication, VehicleOperatingSystem> implements VehicleApplication {

    private CBicycleApplication config;
    private boolean firstUpdate = true;
    private BicycleBehavior behaviorPattern;

    private final List<String[]> values = new ArrayList<>();

    public BicycleRoutingApp() {
        super(CBicycleApplication.class, "CBicycleApplication");
    }

    @Override
    public void onStartup() {
        config = getConfiguration();
        // Create an individual behavior pattern for the unit
        behaviorPattern = new BicycleBehavior(getRandom());

        // Request to change this units parameters according to its individual behavior pattern
        getOs().requestVehicleParametersUpdate()
                .changeMaxSpeed(behaviorPattern.maxSpeed)
                .changeMaxAcceleration(behaviorPattern.acceleration)
                .changeMaxDeceleration(behaviorPattern.deceleration)
                .apply();

        getOs().getPerceptionModule().enable(new SimplePerceptionConfiguration.Builder(360, 25)
                .build());

        getOs().getCellModule().enable();
    }

    @Override
    public void onVehicleUpdated(@Nullable VehicleData previousVehicleData, @Nonnull VehicleData updatedVehicleData) {

        if (config.saveOutput) {
            saveValues(getOs().getPerceptionModule().getPerceivedVehicles());
        }

        // We are updating the route here, because in onStartup() the navigation module has not yet received the initial route -> FIXME?
        if (config.calculateRoutes && firstUpdate && updatedVehicleData.getRoadPosition() != null) {
            calculateBicycleRoute(updatedVehicleData);
        }
    }

    public void calculateBicycleRoute(@Nonnull VehicleData updatedVehicleData) {
        VehicleRoute initialRoute = getOs().getNavigationModule().getCurrentRoute();
        getLog().infoSimTime(this, "Initial route has length {} and connections {}", initialRoute.getLength(), initialRoute.getConnectionIds());

        // Currently we are calculating just one route, as multiple alternatives have caused the calculation to crash -> Known FIXME with
        // graphhopper and turn costs -> Update Graphhopper
        GeoPoint targetPoint = this.getOs().getNavigationModule().getTargetPosition();
        RoutingCostFunction bicycleRoutingCostFunction = new BicycleSpecificCostFunction(behaviorPattern);
        RoutingParameters bicycleParameters = new RoutingParameters()
                // .alternativeRoutes(1)
                .considerTurnCosts(true)
                .costFunction(bicycleRoutingCostFunction)
                .vehicleClass(VehicleClass.Bicycle);

        // Calculate best route based on cost function
        CandidateRoute bestRoute = this.getOs().getNavigationModule().calculateRoutes(targetPoint, bicycleParameters)
                .getBestRoute();

        // Switch the route to the newly calculated one
        getOs().getNavigationModule().switchRoute(bestRoute);
        getLog().infoSimTime(this, "Switched to route with length {} and connections {}", bestRoute.getLength(), bestRoute.getConnectionIds());
        firstUpdate = false;
    }

    /**
     * Save necessary values for evaluation for this timestep.
     * Saved data:
     * - This bicycles ID
     * - Simulation Time
     * - perceived units ID
     * - Distance to perceived unit
     * - Angle to perceived unit
     * - Boolean if perceived unit is on same edge
     * - Boolean if perceived unit is on same lane
     * - Current speed of perceived unit
     * - Edge id that this unit is currently on
     * - Type of edge this unit is currently on
     * - Number of lanes this edge has
     * - If the edge has a bike lane
     * @param perceivedVehicles A list of perceived vehicles in this timestep from the perception module.
     */
    public void saveValues(List<VehicleObject> perceivedVehicles) {
        if (perceivedVehicles.isEmpty()) {
            saveEmptyValues();
            return;
        }
        savePerceivedValues(perceivedVehicles);
    }

    private void saveEmptyValues() {
        if (getOs().getRoadPosition() == null) {
            return;
        }

        String[] emptyValues = new String[13];
        emptyValues[0] = getOs().getId();
        emptyValues[1] = String.valueOf(getOs().getSimulationTime());
        for (int i = 2; i < emptyValues.length; i++) {
            emptyValues[i] = "";
        }
        if (getOs().getRoadPosition() != null) {
            emptyValues[9] = getOs().getRoadPosition().getConnection().getId();
            emptyValues[10] = getOs().getRoadPosition().getConnection().getWay().getType();
            emptyValues[11] = String.valueOf(getOs().getRoadPosition().getConnection().getLanes());
            emptyValues[12] = String.valueOf(getOs().getRoadPosition().getConnection().getHasBikeLane());
        }
        values.add(emptyValues);
    }

    private void savePerceivedValues(List<VehicleObject> perceivedVehicles) {
        if (getOs().getRoadPosition() == null) {
            return;
        }

        Map<String, VehicleUnit> vehicles = UnitSimulator.UnitSimulator.getVehicles();
        long timestamp = getOs().getSimulationTime();

        for (VehicleObject perceivedVehicle: perceivedVehicles) {
            String[] currentValues = new String[13];
            VehicleType vehicleType = vehicles.get(perceivedVehicle.getId()).getInitialVehicleType();
            double distance = this.getOs().getPosition().toVector3d().distanceTo(perceivedVehicle.getPosition());
            Vector3d directionVector = new Vector3d(perceivedVehicle.getPosition()).subtract(this.getOs().getPosition().toVector3d());
            double angle = new Vector3d(directionVector).angle(VectorUtils.getDirectionVectorFromHeading(Objects.requireNonNull(this.getOs().getVehicleData()).getHeading(), new Vector3d()));
            Arrays.fill(currentValues, "");
            currentValues[0] = getOs().getId();
            currentValues[1] = String.valueOf(timestamp);
            currentValues[2] = perceivedVehicle.getId();
            currentValues[3] = vehicleType.getName().equalsIgnoreCase("Car") ? "car"
                    : vehicleType.getName().equalsIgnoreCase("Bike") ? "bike" : "";
            currentValues[4] = String.valueOf(distance);
            currentValues[5] = String.valueOf(angle);
            currentValues[6] = String.valueOf(isOnSameEdge(perceivedVehicle));
            currentValues[7] = String.valueOf(isOnSameLane(perceivedVehicle));
            currentValues[8] = String.valueOf(perceivedVehicle.getSpeed());

            if (getOs().getRoadPosition() != null) {
                currentValues[9] = getOs().getRoadPosition().getConnection().getId();
                currentValues[10] = getOs().getRoadPosition().getConnection().getWay().getType();
                currentValues[11] = String.valueOf(getOs().getRoadPosition().getConnection().getLanes());
                currentValues[12] = String.valueOf(getOs().getRoadPosition().getConnection().getHasBikeLane());
            }
            values.add(currentValues);
        }
    }

    public boolean isOnSameEdge(VehicleObject vehicle) {
        if (Objects.requireNonNull(this.getOs().getVehicleData()).getRoadPosition() != null && vehicle.getEdgeId() != null) {
            return vehicle.getEdgeId().equals(Objects.requireNonNull(this.getOs().getVehicleData()).getRoadPosition().getConnectionId());
        } else {
            return false;
        }
    }

    public boolean isOnSameLane(VehicleObject vehicle) {
        if (isOnSameEdge(vehicle)) {
            return vehicle.getLaneIndex() == Objects.requireNonNull(this.getOs().getVehicleData()).getRoadPosition().getLaneIndex();
        } else {
            return false;
        }
    }

    @Override
    public void onShutdown() {
        // When this unit leaves the simulation, send all collected data to the output server
        MessageRouting routing = getOs().getCellModule().createMessageRouting().destination("server_0").topological().build();
        DataMessage message = new DataMessage(routing, values);
        getOs().getCellModule().sendV2xMessage(message);
    }


    @Override
    public void processEvent(Event event) throws Exception {
        // Get perceived vehicles around unit to calculate comfort metric
    }
}
