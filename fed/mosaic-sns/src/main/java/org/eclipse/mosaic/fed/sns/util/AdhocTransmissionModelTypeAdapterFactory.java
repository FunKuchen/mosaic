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

package org.eclipse.mosaic.fed.sns.util;

import org.eclipse.mosaic.fed.sns.model.AdhocTransmissionModel;
import org.eclipse.mosaic.lib.util.gson.PackageSpecificTypeAdapter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

/**
 * Adapter factory for JSON to configure the two kinds of "simple" or "sophisticated" transmissionModels more comfortably.
 */
public final class AdhocTransmissionModelTypeAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        return new PackageSpecificTypeAdapter<T>(this, gson)
                .searchInPackageOfClass(AdhocTransmissionModel.class)
                .nullSafe();
    }
}
