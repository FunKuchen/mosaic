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
import org.eclipse.mosaic.app.bachelor.utils.BicycleBehavior;
import org.eclipse.mosaic.app.bachelor.utils.BicycleSpecificCostFunction;
import org.eclipse.mosaic.fed.application.ambassador.SimulationKernel;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.SimplePerceptionConfiguration;
import org.eclipse.mosaic.fed.application.ambassador.simulation.perception.index.objects.VehicleObject;
import org.eclipse.mosaic.fed.application.app.ConfigurableApplication;
import org.eclipse.mosaic.fed.application.app.api.VehicleApplication;
import org.eclipse.mosaic.fed.application.app.api.os.VehicleOperatingSystem;
import org.eclipse.mosaic.lib.enums.VehicleClass;
import org.eclipse.mosaic.lib.geo.GeoPoint;
import org.eclipse.mosaic.lib.math.Vector3d;
import org.eclipse.mosaic.lib.math.VectorUtils;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.lib.routing.CandidateRoute;
import org.eclipse.mosaic.lib.routing.RoutingCostFunction;
import org.eclipse.mosaic.lib.routing.RoutingParameters;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import com.opencsv.CSVWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BicycleRoutingApp extends ConfigurableApplication<CBicycleApplication, VehicleOperatingSystem> implements VehicleApplication {

    private static CSVWriter OUT;

    private CBicycleApplication config;
    private boolean firstUpdate = true;
    private BicycleBehavior behaviorPattern;

    public BicycleRoutingApp() {
        super(CBicycleApplication.class, "CBicycleApplication");
    }

    @Override
    public void onStartup() {
        // Get the configuration from the configuration file
        config = getConfiguration();

        // Create an individual behavior pattern for the unit
        behaviorPattern = new BicycleBehavior(getRandom());

        // Request to change this units parameters according to its individual behavior pattern
        getOs().requestVehicleParametersUpdate()
                .changeMaxSpeed(behaviorPattern.maxSpeed)
                .changeMaxAcceleration(behaviorPattern.acceleration)
                .changeMaxDeceleration(behaviorPattern.deceleration)
                .apply();

        // Enable the perception module
        getOs().getPerceptionModule().enable(new SimplePerceptionConfiguration.Builder(360, 25)
                .build());

        // Create an output stream if there is none yet
        try {
            if (OUT == null) {
                OUT = new CSVWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(
                        SimulationKernel.SimulationKernel.getMainLogDirectory().resolve(config.outputFile).toFile()
                ))));
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        OUT.close();
                    } catch (IOException ignore) {}
                }));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onVehicleUpdated(@Nullable VehicleData previousVehicleData, @Nonnull VehicleData updatedVehicleData) {
        // If the configuration enables saving the output, call saveValues with the vehicles perceived in this step
        if (config.saveOutput) {
            saveValues(getOs().getPerceptionModule().getPerceivedVehicles());
        }

        // We are updating the route here, because in onStartup() the navigation module has not yet received the initial route -> FIXME?
        if (config.calculateRoutes && firstUpdate && updatedVehicleData.getRoadPosition() != null) {
            calculateBicycleRoute();
        }
    }

    /**
     * Calculates a new route for this unit from its current position to the target position of the route using the
     * 'BicycleSpecificCostFunction'.
     */
    public void calculateBicycleRoute() {
        VehicleRoute initialRoute = getOs().getNavigationModule().getCurrentRoute();
        getLog().infoSimTime(this, "Initial route has length {} and connections {}", initialRoute.getLength(), initialRoute.getConnectionIds());

        // Currently we are calculating just one route, as multiple alternatives have caused the calculation to crash
        // -> Known FIXME with graphhopper and turn costs -> Update Graphhopper
        GeoPoint targetPoint = this.getOs().getNavigationModule().getTargetPosition();
        RoutingCostFunction bicycleRoutingCostFunction = new BicycleSpecificCostFunction(behaviorPattern);
        RoutingParameters bicycleParameters = new RoutingParameters()
                // .alternativeRoutes(1)
                .considerTurnCosts(true)
                .costFunction(bicycleRoutingCostFunction)
                .vehicleClass(VehicleClass.Bicycle);

        // Calculate best route  from current position to target position based on cost function
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
     * - Perceived units ID
     * - Distance to perceived unit
     * - Angle to perceived unit
     * - Boolean if perceived unit is on same edge
     * - Boolean if perceived unit is on same lane
     * - Current speed of perceived unit
     * - Edge id that this unit is currently on
     * - Type of edge this unit is currently on
     * - Number of lanes this edge has
     * - If the edge has a bike lane
     *
     * @param perceivedVehicles A list of perceived vehicles in this timestep from the perception module.
     */
    public void saveValues(List<VehicleObject> perceivedVehicles) {
        // If there were no vehicles perceived
        if (perceivedVehicles.isEmpty()) {
            saveEmptyValues();
            return;
        }
        // If there were vehicles perceived
        savePerceivedValues(perceivedVehicles);
    }

    /**
     * Saves a line in the output CSV that only contains information that is available through the current unit.
     */
    private void saveEmptyValues() {
        // If the road position has not been initialized for this unit yet, skip saving values this timestep
        if (getOs().getRoadPosition() == null) {
            return;
        }

        // Save a line with only data for this unit has been set
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
        OUT.writeNext(emptyValues);
    }

    /**
     * Save lines in the output CSV for each perceived vehicle in this timestep.
     *
     * @param perceivedVehicles List of perceived VehicleObjects this timestep.
     */
    private void savePerceivedValues(List<VehicleObject> perceivedVehicles) {
        // Skip saving if the road position of this unit has not been initialized yet
        if (getOs().getRoadPosition() == null) {
            return;
        }

        long timestamp = getOs().getSimulationTime();

        // Create and save row for every perceived vehicle
        for (VehicleObject perceivedVehicle : perceivedVehicles) {
            String[] currentValues = new String[13];
            double distance = this.getOs().getPosition().toVector3d().distanceTo(perceivedVehicle.getPosition());
            Vector3d directionVector = new Vector3d(perceivedVehicle.getPosition()).subtract(this.getOs().getPosition().toVector3d());
            double angle = new Vector3d(directionVector).angle(VectorUtils.getDirectionVectorFromHeading(Objects.requireNonNull(this.getOs().getVehicleData()).getHeading(), new Vector3d()));
            Arrays.fill(currentValues, "");
            currentValues[0] = getOs().getId();
            currentValues[1] = String.valueOf(timestamp);
            currentValues[2] = perceivedVehicle.getId();
            currentValues[3] = perceivedVehicle.getLength() < 2.0 ? "bike" : "car";
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
            OUT.writeNext(currentValues);
        }
    }

    /**
     * Determine whether the current vehicle is on the same edge as the perceived vehicle.
     *
     * @param vehicle The perceived vehicle.
     * @return True if on the same edge, False if not.
     */
    public boolean isOnSameEdge(VehicleObject vehicle) {
        if (Objects.requireNonNull(this.getOs().getVehicleData()).getRoadPosition() != null && vehicle.getEdgeId() != null) {
            return vehicle.getEdgeId().equals(Objects.requireNonNull(this.getOs().getVehicleData()).getRoadPosition().getConnectionId());
        } else {
            return false;
        }
    }

    /**
     * Determine whether current vehicle is on the same lane as the perceived vehicle.
     *
     * @param vehicle The perceived vehicle.
     * @return True if on the same lane, False if not.
     */
    public boolean isOnSameLane(VehicleObject vehicle) {
        if (isOnSameEdge(vehicle)) {
            return vehicle.getLaneIndex() == Objects.requireNonNull(this.getOs().getVehicleData()).getRoadPosition().getLaneIndex();
        } else {
            return false;
        }
    }

    @Override
    public void onShutdown() {
        OUT.flushQuietly();
    }


    @Override
    public void processEvent(Event event) throws Exception {

    }
}
