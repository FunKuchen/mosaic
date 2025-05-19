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

package org.eclipse.mosaic.app.bachelor.config;

import java.io.Serializable;

public class CBicycleApplication implements Serializable {
    /**
     * Whether to calculate a new route in the 'BicycleRoutingApp'.
     */
    public boolean calculateRoutes;
    /**
     * Whether to save the output in a CSV file.
     */
    public boolean saveOutput;
    /**
     * Name of the CSV output file. Should end with .csv.gz
     */
    public String outputFile;

    /**
     * Constructor for this configuration class with default values.
     */
    public CBicycleApplication() {
        calculateRoutes = false;
        saveOutput = false;
        outputFile = "BicycleRoutingAppOutput.csv.gz";
    }
}
