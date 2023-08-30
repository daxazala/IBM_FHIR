/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.linuxforhealth.fhir.ig.mcreations.test;

import static org.linuxforhealth.fhir.validation.util.FHIRValidationUtil.countErrors;

import java.io.InputStream;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.linuxforhealth.fhir.model.format.Format;
import org.linuxforhealth.fhir.model.parser.FHIRParser;
import org.linuxforhealth.fhir.model.resource.Questionnaire;
import org.linuxforhealth.fhir.model.resource.OperationOutcome.Issue;
import org.linuxforhealth.fhir.validation.FHIRValidator;
import org.linuxforhealth.fhir.model.resource.StructureDefinition;
import org.linuxforhealth.fhir.profile.ProfileSupport;

public class McreationsValidationTest {
    @Test
    public void testMCREATIONSValidation1() throws Exception {

        // get new questionnaire structure definition
        String NEW_QUESTIONNAIRE_PROFILE_URL = "https://fhir-ig-example/StructureDefinition/ExampleQuestionnaire";
        StructureDefinition qProfile = ProfileSupport.getProfile(NEW_QUESTIONNAIRE_PROFILE_URL);
        System.out.println("questionnaire definition----------------------------" + qProfile);

        try (InputStream in = McreationsValidationTest.class.getClassLoader().getResourceAsStream("JSON/Questionnaire-questionnaire-example-with-new-profile.json")) {
            Questionnaire questionnaire = FHIRParser.parser(Format.JSON).parse(in);
            //System.out.println("quest----------------------------" + questionnaire);
            List<Issue> issues = FHIRValidator.validator().validate(questionnaire);
            issues.forEach(System.out::println);
            Assert.assertEquals(countErrors(issues), 0);
        }
    }

//    @Test
//    public void testMCREATIONSValidation2() throws Exception {
//        try (InputStream in = MCREATIONSValidationTest.class.getClassLoader().getResourceAsStream("JSON/mcode-cancer-disease-status-no-subject-not-asserted.json")) {
//            Observation observation = FHIRParser.parser(Format.JSON).parse(in);
//            List<Issue> issues = FHIRValidator.validator().validate(observation);
//            issues.forEach(System.out::println);
//            Assert.assertEquals(countErrors(issues), 0);
//        }
//    }
//
//    @Test
//    public void testMCREATIONSValidation3() throws Exception {
//        try (InputStream in = MCREATIONSValidationTest.class.getClassLoader().getResourceAsStream("XML/mcode-cancer-disease-status-no-subject-asserted.xml")) {
//            Observation observation = FHIRParser.parser(Format.XML).parse(in);
//            List<Issue> issues = FHIRValidator.validator().validate(observation);
//            issues.forEach(System.out::println);
//            Assert.assertEquals(countErrors(issues), 1);
//        }
//    }
//
//    @Test
//    public void testMCREATIONSValidation4() throws Exception {
//        try (InputStream in = MCREATIONSValidationTest.class.getClassLoader().getResourceAsStream("XML/mcode-cancer-disease-status-no-subject-not-asserted.xml")) {
//            Observation observation = FHIRParser.parser(Format.XML).parse(in);
//            List<Issue> issues = FHIRValidator.validator().validate(observation);
//            issues.forEach(System.out::println);
//            Assert.assertEquals(countErrors(issues), 0);
//        }
//    }
}
