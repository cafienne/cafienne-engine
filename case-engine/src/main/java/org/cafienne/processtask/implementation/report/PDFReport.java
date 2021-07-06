/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.report;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.query.JsonQueryExecuterFactory;
import org.cafienne.json.StringValue;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PDFReport extends SubProcess<PDFReportDefinition> {

    final static String REPORT_DATA_TAG = "reportData";

    public PDFReport(ProcessTaskActor processTask, PDFReportDefinition definition) {
        super(processTask, definition);
    }

    @Override
    public void start() {
        generateReport();
    }

    @Override
    public void reactivate() {
        generateReport();
    }

    @Override
    public void suspend() {
    }

    @Override
    public void terminate() {
    }

    @Override
    public void resume() {
    }

    private void generateReport() {
        try {
            long start = System.currentTimeMillis();

            Map<String, Object> jasperParameters = new HashMap<>();
            jasperParameters.put(JsonQueryExecuterFactory.JSON_INPUT_STREAM, definition.createDataStream(this));
            jasperParameters.put(JsonQueryExecuterFactory.JSON_DATE_PATTERN, "yyyy-MM-dd");
            jasperParameters.put(JsonQueryExecuterFactory.JSON_NUMBER_PATTERN, "#,##0.##");
            jasperParameters.put(JsonQueryExecuterFactory.JSON_LOCALE, Locale.ENGLISH);
            jasperParameters.put(JRParameter.REPORT_LOCALE, Locale.US);

            definition.getSubReportDefinitions().forEach(subReport -> {
                String subReportName = subReport.getSubReportName();
                try {
                    JasperReport subReportje = subReport.createInstance(this);
                    jasperParameters.put(subReportName, subReportje);
                } catch (JRException e) {
                    raiseFault("Could not compile parameter " + subReportName + " into report definition", e);
                    return;
                } catch (IllegalArgumentException iae) {
                    raiseFault(iae.getMessage(), iae);
                    return;
                }
            });

            JasperReport jReport = definition.getReportDefinition().createInstance(this);
            JasperPrint jPrint = JasperFillManager.fillReport(jReport, jasperParameters);

            ByteArrayOutputStream reportOutput = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jPrint, reportOutput);

            String encodedOutput = Base64.getEncoder().encodeToString(reportOutput.toByteArray());
            setRawOutputParameter(PDFReportDefinition.PDF_REPORT_DATA, new StringValue(encodedOutput));

            processTaskActor.addDebugInfo(() -> "PDF Report - Filling time : " + (System.currentTimeMillis() - start));
            raiseComplete();

        } catch (JRException e) {
            raiseFault("Error while generating pdf report", e);
        } catch (MissingParameterException mpe) {
            raiseFault("Missing parameter", mpe);
        }
    }

    ValueMap getInputParameters() {
        return processTaskActor.getMappedInputParameters();
    }
}
