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

import java.util.Random;

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

    boolean isCommuter;

    double maxTripLength;
    double turnFactor;
    double tlFactor;
    double bikeLaneFactor;

    // A high riskAversion (=10) leads way types having no influence on route finding
    double riskAversion;

    public BicycleBehavior() {
        Random random = new Random();
        isCommuter = random.nextDouble() < 0.3;
        initializeTripLength(random);
        initializeTurnFactor(random);
        initializeTlFactor(random);
        initializeBikeLaneFactor(random);
        initializeRiskAversion(random);
    }

    private void initializeTripLength(Random random) {
        if (isCommuter) {
            maxTripLength = random.nextGaussian(6.0, 1.0); // TODO value for stddev
        } else {
            maxTripLength = random.nextGaussian(3.5, 1.0); // TODO value for stddev
        }
    }

    private void initializeTurnFactor(Random random) {
        turnFactor = random.nextGaussian(1.3, 0.3); // TODO values
    }

    private void initializeTlFactor(Random random) {
        tlFactor = random.nextGaussian(1.5, 0.6); // TODO values
    }

    private void initializeBikeLaneFactor(Random random) {
        bikeLaneFactor = random.nextGaussian(0.7, 0.5); // TODO values
    }

    private void initializeRiskAversion(Random random) {
        double gaussian = random.nextGaussian(5.5, 1.5);
        double clamped = Math.max(1, Math.min(gaussian, 10));
        riskAversion = Math.floor(clamped);
    }
}
