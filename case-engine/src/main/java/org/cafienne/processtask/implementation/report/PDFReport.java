/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.report;

import org.cafienne.cmmn.instance.casefile.StringValue;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.query.JsonQueryExecuterFactory;
import org.cafienne.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PDFReport extends SubProcess<PDFReportDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(PDFReport.class);

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

            Map<String, Object> jasperParameters = new HashMap<String, Object>();
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
                    raiseFault(new RuntimeException("Could not compile parameter " + subReportName + " into report definition", e));
                    return;
                } catch (IllegalArgumentException iae) {
                    raiseFault(iae);
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
            raiseFault(new RuntimeException("Error while generating pdf report", e));
        } catch (MissingParameterException mpe) {
            raiseFault(mpe);
        }
    }

    ValueMap getInputParameters() {
        return processTaskActor.getMappedInputParameters();
    }
}
