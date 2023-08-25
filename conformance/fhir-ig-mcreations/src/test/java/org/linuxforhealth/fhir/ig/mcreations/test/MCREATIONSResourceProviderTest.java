/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.linuxforhealth.fhir.ig.mcreations.test;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.linuxforhealth.fhir.ig.mcreations.MCREATIONSResourceProvider;
import org.linuxforhealth.fhir.registry.spi.FHIRRegistryResourceProvider;

public class MCREATIONSResourceProviderTest {
    @Test
    public void testMCREATIONSResourceProvider() {
        FHIRRegistryResourceProvider provider = new MCREATIONSResourceProvider();
        Assert.assertEquals(provider.getRegistryResources().size(), 56);
    }
}
