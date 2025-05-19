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

package org.eclipse.mosaic.app.bachelor.utils;

import org.eclipse.mosaic.lib.math.RandomNumberGenerator;

public class BicycleBehavior {


    /**
     * This enum divides cyclists into three categories, as done in the paper by
     * <a href="https://www.sciencedirect.com/science/article/pii/S0140366423001342">Karakaya et. al.</a> on bicycle behavior in SimRa
     * data.
     */
    private enum speedCategory {
        SLOW, MEDIUM, FAST
    }

    // TODO: These variables have been left out of the implementation for now, but could be starting points for a more detailed implementation
//    boolean isCommuter;

//    double maxTripLength;
//    double turnFactor;
//    double tlFactor;

    private speedCategory cyclistCategory;
    public double maxSpeed;
    public double acceleration;
    public double deceleration;

    protected double bikeLaneFactor;
    // A high riskAversion (=1) leads way types having no influence on route finding
    protected double riskAversion;

    public BicycleBehavior(RandomNumberGenerator random) {
//        isCommuter = random.nextDouble() < 0.3;

//        initializeTripLength(random);
//        initializeTurnFactor(random);
//        initializeTlFactor(random);
        initializeBikeLaneFactor(random);
        initializeRiskAversion(random);

        initializeCyclistCategory(random);
        initializeMaxSpeed(random);
        initializeAcceleration(random);
        initializeDeceleration(random);
    }

    /**
     * Set a random bike lane factor.
     *
     * @param random Random number generator.
     */
    private void initializeBikeLaneFactor(RandomNumberGenerator random) {
        double gaussian = random.nextGaussian(0.6, 0.2); // TODO values
        bikeLaneFactor = Math.max(0, Math.min(gaussian, 1));
    }

    /**
     * Set a random risk aversion.
     *
     * @param random Random number generator.
     */
    private void initializeRiskAversion(RandomNumberGenerator random) {
        double gaussian = random.nextGaussian(0.6, 0.2); // TODO values
        riskAversion = Math.max(0, Math.min(gaussian, 1));
    }

    /**
     * Set a cyclist speed category based on a randomly generated max speed value.
     * Numbers for this distribution are guesstimated from this paper.
     * <a href="https://www.sciencedirect.com/science/article/pii/S0140366423001342">(Karakaya et. al.)</a>
     *
     * @param random A RandomNumberGenerator object
     */
    private void initializeCyclistCategory(RandomNumberGenerator random) {
        double averageSpeed = random.nextGaussian(15.768, 0.89);
        if (0.0 < averageSpeed && averageSpeed <= 13.5) {
            cyclistCategory = speedCategory.SLOW;
        } else if (13.5 < averageSpeed && averageSpeed <= 17.9) {
            cyclistCategory = speedCategory.MEDIUM;
        } else if (17.9 < averageSpeed) {
            cyclistCategory = speedCategory.FAST;
        } else {
            // This case should never happen with the set gaussian distribution
            throw new IllegalArgumentException("Calculated max speed has a value smaller than 0");
        }
    }

    /**
     * Initialize the actual max speed of the unit based on the cyclist speed category.
     *
     * @param random Random number generator.
     */
    private void initializeMaxSpeed(RandomNumberGenerator random) {
        switch (cyclistCategory) {
            //TODO values in gaussian are arbitrary, but kind of based on visuals from paper cited above. Best would be to
            // implement distributions mentioned above each case
            case SLOW:
                // Best fitting distribution: Johnson's Su-distribution JSU(Vmax; Gamma*)
                maxSpeed = random.nextGaussian(5.0, 1.0);
            case MEDIUM:
                // Best fitting distribution: Exponentially Modified Gaussian EMG(x;K)
                maxSpeed = random.nextGaussian(6.3, 0.6);
            case FAST:
                // Best fitting distribution: Non-central t-distribution NCT(Vmax;Gamma*)
                maxSpeed = random.nextGaussian(8.0, 1.0);
        }
    }

    //TODO values in gaussian are arbitrary, but kind of based on visuals from paper cited above. Best would be to
    // implement distributions mentioned above each case

    /**
     * Initialize the maximum acceleration of the unit based on the cyclist speed category.
     *
     * @param random Random number generator.
     */
    private void initializeAcceleration(RandomNumberGenerator random) {
        switch (cyclistCategory) {
            case SLOW:
                // Burr Type III distribution Burr3(Amax; c,d)
                acceleration = random.nextGaussian(0.6, 0.3);
            case MEDIUM:
                //Mielke Beta-Kappa distribution Mbk(Amax; c,d)
                acceleration = random.nextGaussian(0.9, 0.3);
            case FAST:
                // Burr Type III distribution Burr3(Amax; c,d)
                acceleration = random.nextGaussian(1.1, 0.4);
        }
    }

    //TODO values in gaussian are arbitrary, but kind of based on visuals from paper cited above. Best would be to
    // implement distributions mentioned above each case

    /**
     * Initialize the maximum deceleration of the unit based on the cyclist speed category.
     *
     * @param random Random number generator.
     */
    private void initializeDeceleration(RandomNumberGenerator random) {
        switch (cyclistCategory) {
            case SLOW:
                // Best fitting distribution: Johnson's Su-distribution JSU(dmax;a,b)
                deceleration = random.nextGaussian(0.6, 0.3);
            case MEDIUM:
                // Best fitting distribution: Johnson's Su-distribution JSU(dmax; a,b)
                deceleration = random.nextGaussian(0.9, 0.4);
            case FAST:
                // Best fitting distribution: Student's t-distribution t(Dmax;v)
                deceleration = random.nextGaussian(1.1, 0.5);
        }
    }
}
