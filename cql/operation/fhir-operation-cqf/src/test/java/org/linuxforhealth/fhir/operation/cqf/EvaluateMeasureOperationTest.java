/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.linuxforhealth.fhir.operation.cqf;

import static org.linuxforhealth.fhir.cql.helpers.ModelHelper.bundle;
import static org.linuxforhealth.fhir.cql.helpers.ModelHelper.canonical;
import static org.linuxforhealth.fhir.cql.helpers.ModelHelper.coding;
import static org.linuxforhealth.fhir.cql.helpers.ModelHelper.concept;
import static org.linuxforhealth.fhir.cql.helpers.ModelHelper.fhirstring;
import static org.linuxforhealth.fhir.cql.helpers.ModelHelper.valueset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.mockito.MockedStatic;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.linuxforhealth.fhir.cql.helpers.ParameterMap;
import org.linuxforhealth.fhir.exception.FHIROperationException;
import org.linuxforhealth.fhir.model.resource.Encounter;
import org.linuxforhealth.fhir.model.resource.Library;
import org.linuxforhealth.fhir.model.resource.Measure;
import org.linuxforhealth.fhir.model.resource.MeasureReport;
import org.linuxforhealth.fhir.model.resource.Parameters;
import org.linuxforhealth.fhir.model.resource.Patient;
import org.linuxforhealth.fhir.model.resource.Procedure;
import org.linuxforhealth.fhir.model.resource.ValueSet;
import org.linuxforhealth.fhir.model.type.Coding;
import org.linuxforhealth.fhir.model.type.Date;
import org.linuxforhealth.fhir.model.type.DateTime;
import org.linuxforhealth.fhir.model.type.Period;
import org.linuxforhealth.fhir.model.type.Reference;
import org.linuxforhealth.fhir.model.type.code.EncounterStatus;
import org.linuxforhealth.fhir.model.type.code.MeasureReportType;
import org.linuxforhealth.fhir.model.type.code.ProcedureStatus;
import org.linuxforhealth.fhir.registry.FHIRRegistry;
import org.linuxforhealth.fhir.search.util.SearchHelper;
import org.linuxforhealth.fhir.server.spi.operation.FHIROperationContext;
import org.linuxforhealth.fhir.server.spi.operation.FHIRResourceHelpers;

public class EvaluateMeasureOperationTest {

    private EvaluateMeasureOperation operation;
    private SearchHelper searchHelper;

    @BeforeClass
    public void initializeSearchUtil() {
        searchHelper = new SearchHelper();
    }

    @BeforeMethod
    public void setup() {
        operation = new EvaluateMeasureOperation();
    }

    @Test
    public void testDoEvaluationEXM74() throws Exception {
        Patient patient = (Patient) TestHelper.getTestResource("Patient.json");

        String codesystem = "http://snomed.ct/info";
        String encounterCode = "office-visit";
        Coding reason = coding(codesystem, encounterCode);
        Encounter encounter = Encounter.builder()
                .reasonCode(concept(reason))
                .status(EncounterStatus.FINISHED)
                .clazz(reason)
                .period(Period.builder().start(DateTime.now()).end(DateTime.now()).build())
                .build();

        String procedureCode = "fluoride-application";
        Coding type = coding(codesystem, procedureCode);
        Procedure procedure = Procedure.builder().subject(Reference.builder().reference(fhirstring("Patient/"
                + patient.getId())).build()).code(concept(type)).status(ProcedureStatus.COMPLETED).performed(DateTime.of("2019-03-14")).build();

        List<Measure> measures = TestHelper.getBundleResources("EXM74-10.2.000-request.json", Measure.class);
        assertEquals( measures.size(), 1 );
        Measure measure = measures.get(0);
        String measureURL = canonical(measure.getUrl(), measure.getVersion()).getValue();

        List<Library> fhirLibraries = TestHelper.getBundleResources("EXM74-10.2.000-request.json", Library.class);

        List<String> names = fhirLibraries.stream().map( l -> l.getName().getValue()).collect(Collectors.toList());
        System.out.println(names);

        LocalDate periodStart = LocalDate.of(2000, 1, 1);
        LocalDate periodEnd = periodStart.plus(1, ChronoUnit.YEARS);

        Parameters.Parameter pPeriodStart = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_START)).value(Date.of(periodStart)).build();
        Parameters.Parameter pPeriodEnd = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_END)).value(Date.of(periodEnd)).build();
        Parameters.Parameter pReportType = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_REPORT_TYPE)).value(MeasureReportType.INDIVIDUAL).build();
        Parameters.Parameter pMeasure = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_MEASURE)).value(fhirstring(measureURL)).build();
        Parameters.Parameter pSubject = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_SUBJECT)).value(fhirstring("Patient/" + patient.getId())).build();

        Parameters parameters = Parameters.builder().parameter(pPeriodStart, pPeriodEnd, pReportType, pMeasure, pSubject).build();

        FHIRResourceHelpers resourceHelper = mock(FHIRResourceHelpers.class);
        when(resourceHelper.doRead(eq("Patient"), eq(patient.getId()))).thenAnswer(x -> TestHelper.asResult(patient));

        when(resourceHelper.doSearch(eq("Encounter"), anyString(), anyString(), any(), anyString())).thenReturn( bundle(encounter) );
        when(resourceHelper.doSearch(eq("Procedure"), anyString(), anyString(), any(), anyString())).thenReturn( bundle(procedure) );

        try (MockedStatic<FHIRRegistry> staticRegistry = mockStatic(FHIRRegistry.class)) {
            FHIRRegistry mockRegistry = spy(FHIRRegistry.class);
            staticRegistry.when(FHIRRegistry::getInstance).thenReturn(mockRegistry);


            when(mockRegistry.getResource("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.101.12.1001", ValueSet.class)).thenReturn( valueset(codesystem, encounterCode) );
            when(mockRegistry.getResource("http://cts.nlm.nih.gov/fhir/ValueSet/2.16.840.1.113883.3.464.1003.125.12.1002", ValueSet.class)).thenReturn( valueset(codesystem, procedureCode) );

            when(mockRegistry.getResource(measureURL, Measure.class)).thenReturn( measure );
            fhirLibraries.stream().forEach( l -> when(mockRegistry.getResource( canonical(l.getUrl(), l.getVersion()).getValue(), Library.class )).thenReturn(l) );

            Parameters result = operation.doInvoke(FHIROperationContext.createResourceTypeOperationContext("evaluate-measure"),
                Measure.class, null, null, parameters, resourceHelper, searchHelper);
            assertNotNull(result);

            ParameterMap resultMap = new ParameterMap(result);
            MeasureReport report = (MeasureReport) resultMap.getSingletonParameter(EvaluateMeasureOperation.PARAM_OUT_RETURN).getResource();
            assertEquals( report.getMeasure().getValue(), measureURL );

            ZoneOffset zoneOffset = OffsetDateTime.now().getOffset();
            ZonedDateTime expectedStart = OffsetDateTime.of(periodStart, LocalTime.MIN, zoneOffset).toZonedDateTime();
            ZonedDateTime expectedEnd= OffsetDateTime.of(periodEnd, LocalTime.MAX, zoneOffset).toZonedDateTime();

            Period expectedPeriod = Period.builder().start(DateTime.of(expectedStart)).end(DateTime.of(expectedEnd)).build();
            assertEquals( report.getPeriod(), expectedPeriod );

            assertEquals( report.getGroup().size(), 1 );
            MeasureReport.Group group = report.getGroup().get(0);
            assertEquals( group.getPopulation().size(), 3 );
        }
    }

    @Test
    public void testDoEvaluationInvalidIDReferenceNoPrefix() throws Exception {
        Patient patient = (Patient) TestHelper.getTestResource("Patient.json");

        List<Measure> measures = TestHelper.getBundleResources("EXM74-10.2.000-request.json", Measure.class);
        assertEquals( measures.size(), 1 );
        Measure measure = measures.get(0);
        String measureURL = canonical(measure.getUrl(), measure.getVersion()).getValue();

        List<Library> fhirLibraries = TestHelper.getBundleResources("EXM74-10.2.000-request.json", Library.class);

        List<String> names = fhirLibraries.stream().map( l -> l.getName().getValue()).collect(Collectors.toList());
        System.out.println(names);

        LocalDate periodStart = LocalDate.of(2000, 1, 1);
        LocalDate periodEnd = periodStart.plus(1, ChronoUnit.YEARS);

        Parameters.Parameter pPeriodStart = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_START)).value(Date.of(periodStart)).build();
        Parameters.Parameter pPeriodEnd = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_END)).value(Date.of(periodEnd)).build();
        Parameters.Parameter pReportType = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_REPORT_TYPE)).value(MeasureReportType.INDIVIDUAL).build();
        Parameters.Parameter pMeasure = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_MEASURE)).value(fhirstring("NOT VALID")).build();
        Parameters.Parameter pSubject = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_SUBJECT)).value(fhirstring("Patient/" + patient.getId())).build();

        Parameters parameters = Parameters.builder().parameter(pPeriodStart, pPeriodEnd, pReportType, pMeasure, pSubject).build();

        FHIRResourceHelpers resourceHelper = mock(FHIRResourceHelpers.class);
        when(resourceHelper.doRead(eq("Patient"), eq(patient.getId()))).thenAnswer(x -> TestHelper.asResult(patient));

        try (MockedStatic<FHIRRegistry> staticRegistry = mockStatic(FHIRRegistry.class)) {
            FHIRRegistry mockRegistry = spy(FHIRRegistry.class);
            staticRegistry.when(FHIRRegistry::getInstance).thenReturn(mockRegistry);

            when(mockRegistry.getResource(measureURL, Measure.class)).thenReturn( measure );
            fhirLibraries.stream().forEach( l -> when(mockRegistry.getResource( canonical(l.getUrl(), l.getVersion()).getValue(), Library.class )).thenReturn(l) );

            operation.doInvoke(FHIROperationContext.createResourceTypeOperationContext("evaluate-measure"),
                    Measure.class, null, null, parameters, resourceHelper, searchHelper);
            fail("Operation was expected to fail");
        } catch( FHIROperationException fex ) {
            assertEquals(fex.getMessage(), "Unexpected error while reading Measure/NOT VALID");
        }
    }

    @Test
    public void testDoEvaluationInvalidIDReferenceWithPrefix() throws Exception {
        Patient patient = (Patient) TestHelper.getTestResource("Patient.json");

        List<Measure> measures = TestHelper.getBundleResources("EXM74-10.2.000-request.json", Measure.class);
        assertEquals( measures.size(), 1 );
        Measure measure = measures.get(0);
        String measureURL = canonical(measure.getUrl(), measure.getVersion()).getValue();

        List<Library> fhirLibraries = TestHelper.getBundleResources("EXM74-10.2.000-request.json", Library.class);

        List<String> names = fhirLibraries.stream().map( l -> l.getName().getValue()).collect(Collectors.toList());
        System.out.println(names);

        LocalDate periodStart = LocalDate.of(2000, 1, 1);
        LocalDate periodEnd = periodStart.plus(1, ChronoUnit.YEARS);

        Parameters.Parameter pPeriodStart = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_START)).value(Date.of(periodStart)).build();
        Parameters.Parameter pPeriodEnd = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_END)).value(Date.of(periodEnd)).build();
        Parameters.Parameter pReportType = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_REPORT_TYPE)).value(MeasureReportType.INDIVIDUAL).build();
        Parameters.Parameter pMeasure = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_MEASURE)).value(fhirstring("Measure/NOT_VALID")).build();
        Parameters.Parameter pSubject = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_SUBJECT)).value(fhirstring("Patient/" + patient.getId())).build();

        Parameters parameters = Parameters.builder().parameter(pPeriodStart, pPeriodEnd, pReportType, pMeasure, pSubject).build();

        FHIRResourceHelpers resourceHelper = mock(FHIRResourceHelpers.class);
        when(resourceHelper.doRead(eq("Patient"), eq(patient.getId()))).thenAnswer(x -> TestHelper.asResult(patient));
        when(resourceHelper.doRead(eq("Measure"), eq("NOT_VALID"))).thenAnswer(x -> TestHelper.asResult(null));

        try (MockedStatic<FHIRRegistry> staticRegistry = mockStatic(FHIRRegistry.class)) {
            FHIRRegistry mockRegistry = spy(FHIRRegistry.class);
            staticRegistry.when(FHIRRegistry::getInstance).thenReturn(mockRegistry);

            when(mockRegistry.getResource(measureURL, Measure.class)).thenReturn( measure );
            fhirLibraries.stream().forEach( l -> when(mockRegistry.getResource( canonical(l.getUrl(), l.getVersion()).getValue(), Library.class )).thenReturn(l) );

            operation.doInvoke(FHIROperationContext.createResourceTypeOperationContext("evaluate-measure"),
                    Measure.class, null, null, parameters, resourceHelper, searchHelper);
            fail("Operation was expected to fail");
        } catch( FHIROperationException fex ) {
            assertEquals(fex.getMessage(), "Failed to resolve Measure resource \"NOT_VALID\"");
        }
    }

    @Test
    public void testDoEvaluationInvalidURLReference() throws Exception {
        Patient patient = (Patient) TestHelper.getTestResource("Patient.json");

        List<Measure> measures = TestHelper.getBundleResources("EXM74-10.2.000-request.json", Measure.class);
        assertEquals( measures.size(), 1 );
        Measure measure = measures.get(0);
        String measureURL = canonical(measure.getUrl(), measure.getVersion()).getValue();

        List<Library> fhirLibraries = TestHelper.getBundleResources("EXM74-10.2.000-request.json", Library.class);

        List<String> names = fhirLibraries.stream().map( l -> l.getName().getValue()).collect(Collectors.toList());
        System.out.println(names);

        LocalDate periodStart = LocalDate.of(2000, 1, 1);
        LocalDate periodEnd = periodStart.plus(1, ChronoUnit.YEARS);

        Parameters.Parameter pPeriodStart = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_START)).value(Date.of(periodStart)).build();
        Parameters.Parameter pPeriodEnd = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_END)).value(Date.of(periodEnd)).build();
        Parameters.Parameter pReportType = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_REPORT_TYPE)).value(MeasureReportType.INDIVIDUAL).build();
        Parameters.Parameter pMeasure = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_MEASURE)).value(fhirstring("https://iamnothere.com/Measure/NOT_VALID|1.0")).build();
        Parameters.Parameter pSubject = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_SUBJECT)).value(fhirstring("Patient/" + patient.getId())).build();

        Parameters parameters = Parameters.builder().parameter(pPeriodStart, pPeriodEnd, pReportType, pMeasure, pSubject).build();

        FHIRResourceHelpers resourceHelper = mock(FHIRResourceHelpers.class);
        when(resourceHelper.doRead(eq("Patient"), eq(patient.getId()))).thenAnswer(x -> TestHelper.asResult(patient));

        try (MockedStatic<FHIRRegistry> staticRegistry = mockStatic(FHIRRegistry.class)) {
            FHIRRegistry mockRegistry = spy(FHIRRegistry.class);
            staticRegistry.when(FHIRRegistry::getInstance).thenReturn(mockRegistry);

            when(mockRegistry.getResource(measureURL, Measure.class)).thenReturn( measure );
            fhirLibraries.stream().forEach( l -> when(mockRegistry.getResource( canonical(l.getUrl(), l.getVersion()).getValue(), Library.class )).thenReturn(l) );

            operation.doInvoke(FHIROperationContext.createResourceTypeOperationContext("evaluate-measure"),
                    Measure.class, null, null, parameters, resourceHelper, searchHelper);
            fail("Operation was expected to fail");
        } catch( FHIROperationException fex ) {
            assertEquals(fex.getMessage(), "Failed to resolve Measure resource \"https://iamnothere.com/Measure/NOT_VALID|1.0\"");
        }
    }

    /**
     * Evaluate a Measure that does not define a library reference
     */
    @Test
    public void testDoEvaluationMeasureNoLibraries() throws Exception {
        Patient patient = (Patient) TestHelper.getTestResource("Patient.json");

        List<Measure> measures = TestHelper.getBundleResources("EXM74-10.2.000-request.json", Measure.class);
        assertEquals( measures.size(), 1 );
        Measure measure = measures.get(0);
        measure = measure.toBuilder().library(Collections.emptyList()).build();
        String measureURL = canonical(measure.getUrl(), measure.getVersion()).getValue();

        LocalDate periodStart = LocalDate.of(2000, 1, 1);
        LocalDate periodEnd = periodStart.plus(1, ChronoUnit.YEARS);

        Parameters.Parameter pPeriodStart = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_START)).value(Date.of(periodStart)).build();
        Parameters.Parameter pPeriodEnd = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_PERIOD_END)).value(Date.of(periodEnd)).build();
        Parameters.Parameter pReportType = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_REPORT_TYPE)).value(MeasureReportType.INDIVIDUAL).build();
        Parameters.Parameter pMeasure = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_MEASURE)).value(measureURL).build();
        Parameters.Parameter pSubject = Parameters.Parameter.builder().name(fhirstring(EvaluateMeasureOperation.PARAM_IN_SUBJECT)).value(fhirstring("Patient/" + patient.getId())).build();

        Parameters parameters = Parameters.builder().parameter(pPeriodStart, pPeriodEnd, pReportType, pMeasure, pSubject).build();

        FHIRResourceHelpers resourceHelper = mock(FHIRResourceHelpers.class);
        when(resourceHelper.doRead(eq("Patient"), eq(patient.getId()))).thenAnswer(x -> TestHelper.asResult(patient));

        try (MockedStatic<FHIRRegistry> staticRegistry = mockStatic(FHIRRegistry.class)) {
            FHIRRegistry mockRegistry = spy(FHIRRegistry.class);
            staticRegistry.when(FHIRRegistry::getInstance).thenReturn(mockRegistry);

            when(mockRegistry.getResource(measureURL, Measure.class)).thenReturn( measure );

            operation.doInvoke(FHIROperationContext.createResourceTypeOperationContext("evaluate-measure"),
                    Measure.class, null, null, parameters, resourceHelper, searchHelper);
            fail("Operation was expected to fail");
        } catch( FHIROperationException fex ) {
            assertTrue(fex.getMessage().startsWith("Measures utilizing CQL SHALL reference one and only one CQL library"), fex.getMessage());
        }
    }
}
