/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.processtask.implementation.report;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.query.JsonQueryExecuterFactory;
import org.cafienne.json.StringValue;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.definition.SubProcessDefinition;
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
            jasperParameters.put(JsonQueryExecuterFactory.JSON_INPUT_STREAM, getDefinition().createDataStream(this));
            jasperParameters.put(JsonQueryExecuterFactory.JSON_DATE_PATTERN, "yyyy-MM-dd");
            jasperParameters.put(JsonQueryExecuterFactory.JSON_NUMBER_PATTERN, "#,##0.##");
            jasperParameters.put(JsonQueryExecuterFactory.JSON_LOCALE, Locale.ENGLISH);
            jasperParameters.put(JRParameter.REPORT_LOCALE, Locale.US);

            getDefinition().getSubReportDefinitions().forEach(subReport -> {
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

            JasperReport jReport = getDefinition().getReportDefinition().createInstance(this);
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
