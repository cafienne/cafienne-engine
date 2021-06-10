package org.cafienne.cmmn.test.task;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.actorapi.command.StartCase;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.instance.State;
import org.cafienne.akka.actor.serialization.json.StringValue;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.cmmn.test.PingCommand;
import org.cafienne.cmmn.test.TestScript;
import org.cafienne.processtask.actorapi.event.ProcessCompleted;
import org.cafienne.processtask.actorapi.event.ProcessStarted;
import org.junit.Test;

import java.io.*;

public class TestPDFReport {
    private final CaseDefinition definitions = TestScript.getCaseDefinition("testdefinition/task/pdfreport.xml");
    private final TenantUser testUser = TestScript.getTestUser("Anonymous");

    @Test
    public void testReportGeneration() throws IOException {
        String caseInstanceId = "PDFReport";
        TestScript testCase = new TestScript("PDFReport");

        String reportXml = new String(getFileContent("testdefinition/task/report/CustomersReport.jrxml"), "UTF-8");
        String subReportXml = new String(getFileContent("testdefinition/task/report/OrdersReport.jrxml"), "UTF-8");
        String customerData = getCustomerOrders();

        ValueMap inputs = new ValueMap();
        ValueMap request = inputs.with("Request");
        request.putRaw("customerJrXml", reportXml);
        request.putRaw("orderJrXml", subReportXml);
        request.putRaw("jsonData", customerData);

        StartCase startCase = new StartCase(testUser, caseInstanceId, definitions, inputs, null);
        testCase.addStep(startCase, action -> {
            String reportTaskId = testCase.getEventListener().awaitPlanItemState("Generate Report", State.Available).getPlanItemId();

            // TTD: should we also get events from the ProcessActor? Then we can use reportTaskId to figure out of the actor has thrown them.
            //  perhaps just debug events???

            testCase.getEventListener().awaitPlanItemState(reportTaskId, State.Completed);

            // TODO: perhaps this test should test the actual PDF report contents???
            testCase.insertStep(new PingCommand(testUser, caseInstanceId, 0), result -> result.assertCaseFileItem(new Path("Request/pdfReportData")).assertValueType(StringValue.class));

            // Assert that the ProcessActor has published events
            testCase.getEventListener().getEvents().assertEventType(ProcessStarted.class, 1).assertEventType(ProcessCompleted.class, 1);
        });

        testCase.runTest();
    }

    private static String getCustomerOrders() {
        byte[] customerBytes = getFileContent("testdefinition/task/report/Customers.json");
        byte[] orderBytes = getFileContent("testdefinition/task/report/Orders.json");

        JsonNode customersJson = parseJson(new ByteArrayInputStream(customerBytes));
        JsonNode ordersJson = parseJson(new ByteArrayInputStream(orderBytes));

        JsonNode customers = customersJson.path("Northwind").path("Customers");
        JsonNode orders = ordersJson.path("Northwind").path("Orders");

        ObjectMapper mapper = createObjectMapper();
        for (JsonNode customer : customers) {
            ArrayNode customerOrders = mapper.createArrayNode();
            String customerId = customer.path("CustomerID").asText();

            for (JsonNode order : orders) {
                if (order.findValue("CustomerID").asText().equalsIgnoreCase(customerId)) {
                    customerOrders.add(order);
                }
            }
            ((ObjectNode) customer).set("Orders", customerOrders);
        }

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(customersJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while parsing json", e);
        }
    }

    private static JsonNode parseJson(InputStream jsonStream) {
        try {
            return createObjectMapper().readTree(jsonStream);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while parsing json", e);
        } catch (IOException e) {
            throw new RuntimeException("Error while parsing json", e);
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        return mapper;
    }

    /**
     * Reads the content of the specified filename from the classpath and converts it to a byte array.
     *
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    private static byte[] getFileContent(String fileName) {
        InputStream inputStream = TestPDFReport.class.getClassLoader().getResourceAsStream(fileName);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int bite = inputStream.read();
                if (bite == -1) {
                    baos.close();
                    break;
                }
                baos.write(bite);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unable to read file content", e);
        }
    }
}
