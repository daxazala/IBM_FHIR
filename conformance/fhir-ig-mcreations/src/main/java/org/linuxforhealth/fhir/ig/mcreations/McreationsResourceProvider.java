/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.linuxforhealth.fhir.ig.mcreations;

import org.linuxforhealth.fhir.registry.util.PackageRegistryResourceProvider;

public class McreationsResourceProvider extends PackageRegistryResourceProvider {
    @Override
    public String getPackageId() {
        return "com.mcreations.fhir.experiment.01";
    }
}
