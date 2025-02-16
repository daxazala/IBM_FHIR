/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.linuxforhealth.fhir.operation.term.cache;

import java.io.InputStream;

import org.linuxforhealth.fhir.cache.CacheKey;
import org.linuxforhealth.fhir.cache.CacheManager;
import org.linuxforhealth.fhir.config.FHIRRequestContext;
import org.linuxforhealth.fhir.core.HTTPReturnPreference;
import org.linuxforhealth.fhir.exception.FHIROperationException;
import org.linuxforhealth.fhir.model.format.Format;
import org.linuxforhealth.fhir.model.parser.FHIRParser;
import org.linuxforhealth.fhir.model.resource.CodeSystem;
import org.linuxforhealth.fhir.model.resource.OperationDefinition;
import org.linuxforhealth.fhir.model.resource.OperationOutcome;
import org.linuxforhealth.fhir.model.resource.Parameters;
import org.linuxforhealth.fhir.model.resource.Resource;
import org.linuxforhealth.fhir.model.type.Code;
import org.linuxforhealth.fhir.model.type.CodeableConcept;
import org.linuxforhealth.fhir.model.type.Coding;
import org.linuxforhealth.fhir.model.type.code.IssueSeverity;
import org.linuxforhealth.fhir.model.type.code.IssueType;
import org.linuxforhealth.fhir.operation.term.AbstractTermOperation;
import org.linuxforhealth.fhir.search.util.SearchHelper;
import org.linuxforhealth.fhir.server.registry.ServerRegistryResourceProvider;
import org.linuxforhealth.fhir.server.spi.operation.FHIROperationContext;
import org.linuxforhealth.fhir.server.spi.operation.FHIROperationUtil;
import org.linuxforhealth.fhir.server.spi.operation.FHIRResourceHelpers;
import org.linuxforhealth.fhir.term.util.CodeSystemSupport;

public class CodeSystemClearCacheOperation extends AbstractTermOperation {

    @Override
    protected OperationDefinition buildOperationDefinition() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("operation-codesystem-clear-cache.json")) {
            return FHIRParser.parser(Format.JSON).parse(in);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    @Override
    protected Parameters doInvoke(
            FHIROperationContext operationContext,
            Class<? extends Resource> resourceType,
            String logicalId,
            String versionId,
            Parameters parameters,
            FHIRResourceHelpers resourceHelper,
            SearchHelper searchHelper) throws FHIROperationException {

        CacheManager.invalidateAll(CodeSystemSupport.ANCESTORS_AND_SELF_CACHE_NAME);
        CacheManager.invalidateAll(CodeSystemSupport.DESCENDANTS_AND_SELF_CACHE_NAME);

        try {
            if (FHIROperationContext.Type.INSTANCE.equals(operationContext.getType()) || parameters.getParameter().size() > 0 ) {
                CodeSystem codeSystem = getResource(operationContext, logicalId, parameters, resourceHelper, CodeSystem.class );
                clearServerRegistryCache(codeSystem);
            }

            OperationOutcome operationOutcome = OperationOutcome.builder().issue(
                OperationOutcome.Issue.builder()
                    .severity(IssueSeverity.INFORMATION)
                    .code(IssueType.INFORMATIONAL)
                    .details(CodeableConcept.builder().coding(
                        Coding.builder().code(Code.of("success")).build()
                     ).build()).build()
                ).build();

            if (FHIRRequestContext.get().getReturnPreference() == HTTPReturnPreference.OPERATION_OUTCOME) {
                return FHIROperationUtil.getOutputParameters(operationOutcome);
            } else {
                return null;
            }
        } catch( Throwable t ) {
            throw new FHIROperationException("Unexpected error occurred while processing request for operation '"
                    + getName() + "': " + getCausedByMessage(t), t);
        }
    }

    private String getCausedByMessage(Throwable throwable) {
        return throwable.getClass().getName() + ": " + throwable.getMessage();
    }

    private void clearServerRegistryCache(CodeSystem resource) {
        String dataStoreId = FHIRRequestContext.get().getDataStoreId();
        String url = resource.getUrl().getValue();
        CacheManager.invalidate(ServerRegistryResourceProvider.REGISTRY_RESOURCE_CACHE_NAME, CacheKey.key(dataStoreId,url));
        if( resource.getVersion() != null ) {
            url = url + "|" + resource.getVersion().getValue();
            CacheManager.invalidate(ServerRegistryResourceProvider.REGISTRY_RESOURCE_CACHE_NAME, CacheKey.key(dataStoreId,url));
        }
    }
}
