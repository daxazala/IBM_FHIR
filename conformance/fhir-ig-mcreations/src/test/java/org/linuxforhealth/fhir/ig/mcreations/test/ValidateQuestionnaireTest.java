package org.linuxforhealth.fhir.ig.mcreations.test;

import org.testng.Assert;
import org.testng.annotations.Test;

import org.apache.http.client.HttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import java.io.*;

public class ValidateQuestionnaireTest {
    @Test
    public void testValidateQuestionnaire() throws IOException {
        String certificatesTrustStorePath = System.getenv("TRUST_STORE_PATH");
        System.out.println("certificatesTrustStorePath----------" + certificatesTrustStorePath);
        System.setProperty("javax.net.ssl.trustStore", certificatesTrustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope("localhost", 9443),
                new UsernamePasswordCredentials("fhiruser", "change-password"));

        InputStream inputStream = getClass().getResourceAsStream("/com/mcreations/fhir/experiment/01/package/Questionnaire-questionnaire-example-with-new-profile.json");

        HttpClient httpClient = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
        HttpPost httpPost = new HttpPost("https://localhost:9443/fhir-server/api/v4/Questionnaire/$validate");

        InputStreamEntity entity = new InputStreamEntity(inputStream, ContentType.create("application/fhir+json"));
        httpPost.setEntity(entity);
        System.out.println("entity---------------" + entity);
        HttpResponse response = httpClient.execute(httpPost);
        System.out.println("response---------------" + response);
        String responseBody = EntityUtils.toString(response.getEntity());

        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
    }
}
