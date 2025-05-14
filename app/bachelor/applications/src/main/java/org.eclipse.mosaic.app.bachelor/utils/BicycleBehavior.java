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
     *  About 30% of trips were commute trips (home to work or work to home). The average trip distance was 2.2 miles (3.5 km) for
     *  non-commute trips (e.g. shopping, errands, etc.) and 3.7 miles (6 km) for commute trips. Average speed (including stops) was
     *  10 miles/h (16.1 km/h) for non-commute trips and 11.8 miles/h (19 km/h) for commute trips. In comparison, the sample of Zurich,
     *  Switerland trips in Menghini et al. (2010) averaged 0.62 miles (1 km) at 6.3 miles/h (10.1 km/h).
     * A little more than half (53%) of recorded miles were ridden on facilities with bicycle infrastructure, including bike lanes (29%),
     * off-street paths (13%), and bike boulevards (11%). Bike boulevards are residential streets with traffic calming features to reduce
     * auto speeds and volumes (e.g. speed humps, one-way restrictions on autos, chicanes, etc.), while giving bicycles increased priority
     * at intersections, e.g. by eliminating stop signs in the boulevard direction and “flipping” them to the cross-direction traffic and
     * adding crossing aids, including signals, at busy intersections. Observed paths were on average somewhat longer than the shortest
     * network paths: by 12% for non-commute trips and 11% for commute trips. Further descriptive analysis is available in a separate
     * report (Dill and Gliebe, 2008).
     * Values in this have been based on this paper:
     * <a href="https://doi.org/10.1016/j.tra.2012.07.005">...</a>
     */
    private enum speedCategory {
        SLOW, MEDIUM, FAST
    }

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

//    private void initializeTripLength(Random random) {
//        if (isCommuter) {
//            maxTripLength = random.nextGaussian(6.0, 1.0); // TODO value for stddev
//        } else {
//            maxTripLength = random.nextGaussian(3.5, 1.0); // TODO value for stddev
//        }
//    }
//
//    private void initializeTurnFactor(Random random) {
//        turnFactor = random.nextGaussian(1.3, 0.3); // TODO values
//    }
//
//    private void initializeTlFactor(Random random) {
//        tlFactor = random.nextGaussian(1.5, 0.6); // TODO values
//    }

    private void initializeBikeLaneFactor(RandomNumberGenerator random) {
        double gaussian = random.nextGaussian(0.6, 0.2); // TODO values
        bikeLaneFactor = Math.max(0, Math.min(gaussian, 1));
    }

    private void initializeRiskAversion(RandomNumberGenerator random) {
        double gaussian = random.nextGaussian(0.6, 0.2); // TODO values
        riskAversion = Math.max(0, Math.min(gaussian, 1));
    }

    /**
     * Numbers for this distribution are guesstimated from this paper.
     * <a href="https://www.sciencedirect.com/science/article/pii/S0140366423001342">...</a>
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
