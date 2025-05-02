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
import org.eclipse.mosaic.lib.objects.vehicle.VehicleData;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleRoute;
import org.eclipse.mosaic.lib.objects.vehicle.VehicleType;
import org.eclipse.mosaic.lib.routing.CandidateRoute;
import org.eclipse.mosaic.lib.routing.RoutingCostFunction;
import org.eclipse.mosaic.lib.routing.RoutingParameters;
import org.eclipse.mosaic.lib.util.scheduling.Event;
import org.eclipse.mosaic.rti.TIME;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BicycleRoutingApp extends ConfigurableApplication<CBicycleApplication, VehicleOperatingSystem> implements VehicleApplication {

    private CBicycleApplication config;
    private boolean firstUpdate = true;
    private BicycleBehavior behaviorPattern;

    private final List<Integer> perceivedCarsPerStep = new ArrayList<>();
    private final List<Integer> perceivedBikesPerStep = new ArrayList<>();


    private final List<List<String>> values = new ArrayList<>();

    private final double MAX_COMFORT = 10.0;

    public BicycleRoutingApp() {
        super(CBicycleApplication.class, "CBicycleApplication");
    }

    @Override
    public void onStartup() {
        config = getConfiguration();

        // Create an individual behavior pattern for the unit
        behaviorPattern = new BicycleBehavior();

        // Request to change this units parameters according to its individual behavior pattern
        getOs().requestVehicleParametersUpdate()
                .changeMaxSpeed(behaviorPattern.maxSpeed)
                .changeMaxAcceleration(behaviorPattern.acceleration)
                .changeMaxDeceleration(behaviorPattern.deceleration)
                .apply();

        getOs().getPerceptionModule().enable(new SimplePerceptionConfiguration.Builder(360, 20)
                .build());

        getOs().getEventManager().addEvent(new Event(getOs().getSimulationTime(), this));
    }

    @Override
    public void onVehicleUpdated(@Nullable VehicleData previousVehicleData, @Nonnull VehicleData updatedVehicleData) {
        // We are updating the route here, because in onStartup() the navigation module has not yet received the initial route -> FIXME?
        if (config.calculateRoutes && firstUpdate && updatedVehicleData.getRoadPosition() != null) {
            calculateBicycleRoute(updatedVehicleData);
        }
    }

    public void calculateBicycleRoute(@Nonnull VehicleData updatedVehicleData) {
        VehicleRoute initialRoute = getOs().getNavigationModule().getCurrentRoute();
        getLog().infoSimTime(this, "Initial route has length {} and connections {}", initialRoute.getLength(), initialRoute.getConnectionIds());

        // Currently we are calculating just one route, as multiple alternatives have caused the calculation to crash -> FIXME
        GeoPoint targetPoint = this.getOs().getNavigationModule().getTargetPosition();
        RoutingCostFunction bicycleRoutingCostFunction = new BicycleSpecificCostFunction(behaviorPattern);
        RoutingParameters bicycleParameters = new RoutingParameters()
                // .alternativeRoutes(1)
                .considerTurnCosts(true)
                .costFunction(bicycleRoutingCostFunction)
                .vehicleClass(VehicleClass.Bicycle);
        CandidateRoute bestRoute = this.getOs().getNavigationModule().calculateRoutes(targetPoint, bicycleParameters)
                .getBestRoute();

        // Switch the route to the newly calculated one
        getOs().getNavigationModule().switchRoute(bestRoute);
        getLog().infoSimTime(this, "Switched to route with length {} and connections {}", bestRoute.getLength(), bestRoute.getConnectionIds());
        firstUpdate = false;
    }

    // Indirectly given through the size of the distance list
    public void saveNumberOfVehicles(List<VehicleObject> perceivedVehicles) {
        int carCount = 0;
        int bicycleCount = 0;

        Map<String, VehicleUnit> vehicles = UnitSimulator.UnitSimulator.getVehicles();


        for (VehicleObject vehicleObject : perceivedVehicles) {
            VehicleType vehicleType = vehicles.get(vehicleObject.getId()).getInitialVehicleType();
            if (vehicleType.getName().equals("Car")) {carCount += 1;} else if (vehicleType.getName().equals("Bike")) {
                bicycleCount += 1;
            }
        }
        perceivedCarsPerStep.add(carCount);
        perceivedBikesPerStep.add(bicycleCount);
    }

    public void saveValues(List<VehicleObject> perceivedVehicles) {
        if (perceivedVehicles.isEmpty()) {
            List<String> noValues = new ArrayList<>();
            noValues.add("");
            noValues.add(String.valueOf(this.getOs().getSimulationTime()));
            noValues.add("");
            noValues.add("");
            noValues.add("");
            values.add(noValues);
        }
        Map<String, VehicleUnit> vehicles = UnitSimulator.UnitSimulator.getVehicles();

        long timestamp = getOs().getSimulationTime();

        for (VehicleObject perceivedVehicle : perceivedVehicles) {
            List<String> currentValues = new ArrayList<>();
            VehicleType vehicleType = vehicles.get(perceivedVehicle.getId()).getInitialVehicleType();
            double distance = this.getOs().getPosition().toVector3d().distanceTo(perceivedVehicle.getPosition());
            Vector3d directionVector = new Vector3d(perceivedVehicle.getPosition()).subtract(this.getOs().getPosition().toVector3d());
            double angle = new Vector3d(directionVector).angle(VectorUtils.getDirectionVectorFromHeading(Objects.requireNonNull(this.getOs().getVehicleData()).getHeading(), new Vector3d()));
            if (vehicleType.getName().equals("Car")) {
                currentValues.add("car");
            } else if (vehicleType.getName().equals("Bike")) {
                currentValues.add("bike");
            }
            currentValues.add(String.valueOf(timestamp));
            currentValues.add(this.getOs().getId());
            currentValues.add(String.valueOf(distance));
            currentValues.add(String.valueOf(angle));
            values.add(currentValues);
        }
    }


    public double carDistanceComfort(VehicleObject perceivedVehicle) {
        Vector3d directionVector = new Vector3d(perceivedVehicle.getPosition()).subtract(this.getOs().getPosition().toVector3d());
        double distance = this.getOs().getPosition().toVector3d().distanceTo(perceivedVehicle.getPosition());
        double perceptionRange = this.getOs().getPerceptionModule().getConfiguration().getViewingRange();

        return (distance / perceptionRange) * MAX_COMFORT;
    }

    public void calculateDirectionBasedComfort(List<VehicleObject> perceivedVehicles) {
        Map<String, VehicleUnit> vehicles = UnitSimulator.UnitSimulator.getVehicles();
        List<Double> carComfortFactors = new ArrayList<>();
        List<Double> bikeComfortFactors = new ArrayList<>();
        for (VehicleObject vehicleObject : perceivedVehicles) {
            VehicleType vehicleType = vehicles.get(vehicleObject.getId()).getInitialVehicleType();
            if (vehicleType.getName().equals("Car")) {
                carComfortFactors.add(carDirectionComfort(vehicleObject));
            } else if (vehicleType.getName().equals("Bike")) {
//                bikeComfortFactors.add(bikeDirectionComfort(vehicleObject));
            }
        }
    }

    public double carDirectionComfort(VehicleObject perceivedVehicle) {
        // Return 1 if vehicle is very close and directly in front or behind
        // Return 10 if vehicle is very far away
        // Possibly check if all vehicles are to the left or right -> This could be because we are on a bike lane -> adjust comfort factor
        Vector3d directionVector = new Vector3d(perceivedVehicle.getPosition()).subtract(this.getOs().getPosition().toVector3d());
        double distance = this.getOs().getPosition().toVector3d().distanceTo(perceivedVehicle.getPosition());
        double angle = new Vector3d(directionVector).angle(VectorUtils.getDirectionVectorFromHeading(Objects.requireNonNull(this.getOs().getVehicleData()).getHeading(), new Vector3d()));

        double perceptionRange = this.getOs().getPerceptionModule().getConfiguration().getViewingRange();
        // Case perceived vehicle is within 10 degrees in front or behind this unit
        if (angle <= 5.0 || angle >= 175) {
            double directlyInFrontFactor = 0.5;
            return directlyInFrontFactor * (distance / perceptionRange) * MAX_COMFORT;
        } else {
            return (distance / perceptionRange) * MAX_COMFORT;
        }
    }

//    public Double bikeDirectionComfort(VehicleObject vehicleObject) {
//
//    }

    @Override
    public void onShutdown() {
//        getLog().infoSimTime(this, "Perceived Cars per simulation step: {}", this.perceivedCarsPerStep);
//        getLog().infoSimTime(this, "Perceived Bikes per simulation step: {}", this.perceivedBikesPerStep);
//        double averageNumberOfCarsPerStep = (double) perceivedCarsPerStep.stream().mapToInt(Integer::intValue).sum() / perceivedCarsPerStep.size();
//        double averageNumberOfBikesPerStep = (double) perceivedBikesPerStep.stream().mapToInt(Integer::intValue).sum() / perceivedBikesPerStep.size();
//        getLog().infoSimTime(this, "Average number of cars per simulation step: {}", averageNumberOfCarsPerStep);
//        getLog().infoSimTime(this, "Average number of bikes per simulation step: {}", averageNumberOfBikesPerStep);
        try {
            exportValuesToCsv(values);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void exportValuesToCsv(List<List<String>> valueList) throws IOException {
        String filename = "logs/values/values.csv";

        for (List<String> row : valueList) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(filename, true))) {
//                writer.println("Timestep,vType,ID,Distance,Angle");

                String csvLine = row.stream()
                        .map(entry -> "\"" + entry.replace("\"", "\"\"") + "\"")
                        .collect(Collectors.joining(","));
                writer.println(csvLine);
                getLog().info(csvLine);

            } catch (IOException e) {
                throw new IOException(e);
            }
        }
    }

    @Override
    public void processEvent(Event event) throws Exception {
        // Get perceived vehicles around unit to calculate comfort metric
        List<VehicleObject> perceivedVehicles = getOs().getPerceptionModule().getPerceivedVehicles();

        saveValues(perceivedVehicles);

        getOs().getEventManager().addEvent(new Event(getOs().getSimulationTime() + 10 * TIME.SECOND, this));
    }
}
