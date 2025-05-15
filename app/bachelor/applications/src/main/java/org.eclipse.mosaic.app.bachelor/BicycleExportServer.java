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

import org.eclipse.mosaic.app.bachelor.config.CBicycleExportServer;
import org.eclipse.mosaic.app.bachelor.messages.DataMessage;
import org.eclipse.mosaic.fed.application.ambassador.SimulationKernel;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.CamBuilder;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedAcknowledgement;
import org.eclipse.mosaic.fed.application.ambassador.simulation.communication.ReceivedV2xMessage;
import org.eclipse.mosaic.fed.application.app.ConfigurableApplication;
import org.eclipse.mosaic.fed.application.app.api.CommunicationApplication;
import org.eclipse.mosaic.fed.application.app.api.os.ServerOperatingSystem;
import org.eclipse.mosaic.interactions.communication.V2xMessageTransmission;
import org.eclipse.mosaic.lib.util.scheduling.Event;

import com.opencsv.CSVWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class BicycleExportServer extends ConfigurableApplication<CBicycleExportServer, ServerOperatingSystem> implements CommunicationApplication {

    private CBicycleExportServer config;
    private CSVWriter writer;

    public BicycleExportServer() {
        super(CBicycleExportServer.class, "CBicycleExportServer");
    }

    @Override
    public void onStartup() {
        getOs().getCellModule().enable();
        config = getConfiguration();
        try {
            FileOutputStream fos = new FileOutputStream(SimulationKernel.SimulationKernel.getMainLogDirectory().resolve(config.outputFile).toFile());
            GZIPOutputStream gos = new GZIPOutputStream(fos);
            OutputStreamWriter osw = new OutputStreamWriter(gos);
            writer = new CSVWriter(osw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onShutdown() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processEvent(Event event) throws Exception {

    }

    @Override
    public void onMessageReceived(ReceivedV2xMessage receivedV2xMessage) {
        if (receivedV2xMessage.getMessage() instanceof DataMessage dataMessage) {
            List<String[]> writeValues = dataMessage.bikeData;
            writer.writeAll(writeValues);
            try {
                writer.flush();
                writeValues.clear();
            } catch (IOException e) {
                //
            }
        }
    }

    @Override
    public void onAcknowledgementReceived(ReceivedAcknowledgement acknowledgement) {

    }

    @Override
    public void onCamBuilding(CamBuilder camBuilder) {

    }

    @Override
    public void onMessageTransmitted(V2xMessageTransmission v2xMessageTransmission) {

    }
}
