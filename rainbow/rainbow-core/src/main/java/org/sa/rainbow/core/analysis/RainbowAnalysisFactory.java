package org.sa.rainbow.core.analysis;

import org.sa.rainbow.core.IComponentFactory;
import org.sa.rainbow.core.IRainbowPropertyProvider;
import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.ports.IRainbowReportingPort;

import java.util.ArrayList;
import java.util.List;

import static java.text.MessageFormat.format;
import static org.sa.rainbow.core.RainbowComponentT.MASTER;
import static org.sa.rainbow.core.RainbowConstants.PROPKEY_ANALYSIS_COMPONENTS;
import static org.sa.rainbow.core.RainbowConstants.PROPKEY_ANALYSIS_COMPONENT_SIZE;

/**
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public class RainbowAnalysisFactory implements IComponentFactory<IRainbowAnalysis> {

  public IRainbowAnalysis create(IRainbowReportingPort reportingPort,
                                 String an, String model) throws RainbowConnectionException {
    IRainbowAnalysis analysis = null;
    if (an != null) {
      an = an.trim();
      try {
        @SuppressWarnings("unchecked")
        Class<? extends IRainbowAnalysis> cls = (Class<? extends IRainbowAnalysis>) Class.forName(an);
        analysis = cls.newInstance();
        analysis.initialize(reportingPort);
        analysis.start();
        if (model != null) {
          analysis.setProperty("model", model);
        }
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
              | ClassCastException e) {
        reportingPort.error(MASTER, format("Could not start the analysis ''{0}''", an), e);
      }
    }
    return analysis;
  }

  @Override
  public List<IRainbowAnalysis> create(IRainbowPropertyProvider rainbowPropertyProvider,
                                       IRainbowReportingPort reportingPort) throws RainbowConnectionException {
    int size = rainbowPropertyProvider.getProperty(PROPKEY_ANALYSIS_COMPONENT_SIZE, 0);
    List<IRainbowAnalysis> analyses = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {

      String className = rainbowPropertyProvider.getProperty(PROPKEY_ANALYSIS_COMPONENTS + "_" + i);
      String modelName = rainbowPropertyProvider.getProperty(PROPKEY_ANALYSIS_COMPONENTS + ".model_" + i);
      reportingPort.info(MASTER, "Starting " + className);
      analyses.add(create(reportingPort, className, modelName));
    }
    return analyses;
  }
}
