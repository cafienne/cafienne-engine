/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.report;

import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.query.JsonQueryExecuterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PDFReport extends SubProcess<PDFReportDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(PDFReport.class);

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
            ValueMap processInputParameters = processTaskActor.getMappedInputParameters();
            definition.setInputParameters(processInputParameters);

            InputStream jrXmlInputStream = definition.getJasperReportXml();
            InputStream dataInputStream = definition.getJasperReportJsonData();

            Map<String, Object> params = new HashMap<String, Object>();
            params.put(JsonQueryExecuterFactory.JSON_INPUT_STREAM, dataInputStream);
            params.put(JsonQueryExecuterFactory.JSON_DATE_PATTERN, "yyyy-MM-dd");
            params.put(JsonQueryExecuterFactory.JSON_NUMBER_PATTERN, "#,##0.##");
            params.put(JsonQueryExecuterFactory.JSON_LOCALE, Locale.ENGLISH);
            params.put(JRParameter.REPORT_LOCALE, Locale.US);

            definition.getJasperSubReportXmls().forEach((paramName, subReportDef) -> {
                try {
                    params.put(paramName, JasperCompileManager.compileReport(subReportDef));
                } catch (JRException e) {
                    raiseFault(new RuntimeException("Could not compile parameter " + paramName + " into report definition", e));
                    return;
                }
            });

            long start = System.currentTimeMillis();
            JasperReport jReport = JasperCompileManager.compileReport(jrXmlInputStream);
            JasperPrint jPrint = JasperFillManager.fillReport(jReport, params);

            ByteArrayOutputStream reportOutput = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jPrint, reportOutput);

            processTaskActor.addDebugInfo(() -> "PDF Report - Filling time : " + (System.currentTimeMillis() - start));

            String encodedOutput = Base64.getEncoder().encodeToString(reportOutput.toByteArray());
            setRawOutputParameter(PDFReportDefinition.PDF_REPORT_DATA, Value.convert(encodedOutput));
            raiseComplete();

        } catch (JRException e) {
            raiseFault(new RuntimeException("Error while generating pdf report", e));
        }
    }
}
