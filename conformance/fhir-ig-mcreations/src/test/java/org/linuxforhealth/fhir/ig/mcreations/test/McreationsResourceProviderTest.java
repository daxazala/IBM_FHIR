/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.linuxforhealth.fhir.ig.mcreations.test;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.linuxforhealth.fhir.ig.mcreations.McreationsResourceProvider;
import org.linuxforhealth.fhir.registry.spi.FHIRRegistryResourceProvider;

public class McreationsResourceProviderTest {
    @Test
    public void testMcreationsResourceProvider() {
        FHIRRegistryResourceProvider provider = new McreationsResourceProvider();
        Assert.assertEquals(provider.getRegistryResources().size(), 5);
    }
}
