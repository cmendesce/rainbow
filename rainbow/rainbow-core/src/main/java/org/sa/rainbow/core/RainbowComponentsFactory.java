package org.sa.rainbow.core;

import org.apache.log4j.Logger;
import org.sa.rainbow.core.adaptation.AdaptationExecutorFactory;
import org.sa.rainbow.core.adaptation.AdaptationManagerFactory;
import org.sa.rainbow.core.adaptation.IAdaptationExecutor;
import org.sa.rainbow.core.adaptation.IAdaptationManager;
import org.sa.rainbow.core.analysis.IRainbowAnalysis;
import org.sa.rainbow.core.analysis.RainbowAnalysisFactory;
import org.sa.rainbow.core.error.RainbowAbortException;
import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.gauges.GaugeManager;
import org.sa.rainbow.core.gauges.GaugeManagerFactory;
import org.sa.rainbow.core.models.ModelsManager;
import org.sa.rainbow.core.models.ModelsManagerFactory;
import org.sa.rainbow.core.ports.IRainbowReportingPort;
import org.sa.rainbow.translator.effectors.EffectorManager;
import org.sa.rainbow.translator.effectors.EffectorManagerFactory;

import java.text.MessageFormat;
import java.util.List;

/**
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public class RainbowComponentsFactory {

  private static final Logger LOGGER = Logger.getLogger (RainbowComponentsFactory.class);
  private final IRainbowPropertyProvider rainbowPropertyProvider;
  private final IRainbowReportingPort reportingPort;

  private IComponentFactory<ModelsManager> modelsManagerFactory;
  private IComponentFactory<IAdaptationManager<?>> adaptationManagerFactory;
  private IComponentFactory<IAdaptationExecutor<?>> adaptationExecutorFactory;
  private IComponentFactory<IRainbowAnalysis> rainbowAnalysisFactory;
  private IComponentFactory<GaugeManager> gaugeManagerFactory;
  private IComponentFactory<EffectorManager> effectorManagerFactory;

  public RainbowComponentsFactory(IRainbowPropertyProvider rainbowPropertyProvider, IRainbowReportingPort reportingPort) {
    this.rainbowPropertyProvider = rainbowPropertyProvider;
    this.reportingPort = reportingPort;
    createFactories();
  }

  public void createFactories() {
    modelsManagerFactory = resolveFactory(getClassName("rainbow.model.factory", ModelsManagerFactory.class));
    adaptationManagerFactory = resolveFactory(getClassName("rainbow.adaptationManager.factory", AdaptationManagerFactory.class));
    adaptationExecutorFactory = resolveFactory(getClassName("rainbow.adaptationExecutor.factory", AdaptationExecutorFactory.class));
    rainbowAnalysisFactory = resolveFactory(getClassName("rainbow.analysis.factory", RainbowAnalysisFactory.class));
    gaugeManagerFactory = resolveFactory(getClassName("rainbow.gauges.factory", GaugeManagerFactory.class));
    effectorManagerFactory = resolveFactory(getClassName("rainbow.effectors.factory", EffectorManagerFactory.class));
  }

  private String getClassName(String key, Class defaultClass) {
    return rainbowPropertyProvider.getProperty(key, defaultClass.getCanonicalName());
  }

  private <C> IComponentFactory<C> resolveFactory(String className) throws RainbowAbortException {
    try {
      LOGGER.info("Constructing component [" + className + "]");
      Class<IComponentFactory<C>> factory = (Class<IComponentFactory<C>>) Class.forName(className);
      return factory.newInstance();
    } catch (Exception e) {
      String message = MessageFormat.format("Impossible to create component [{0}]", className);
      reportingPort.error(RainbowComponentT.MASTER, message);
      throw new RainbowAbortException(message, e);
    }
  }

  public ModelsManager createModelsManager() throws RainbowConnectionException {
    return modelsManagerFactory.create(rainbowPropertyProvider, reportingPort).get(0);
  }

  public List<IAdaptationManager<?>> createAdaptationManagers() throws RainbowConnectionException {
    return adaptationManagerFactory.create(rainbowPropertyProvider, reportingPort);
  }

  public List<IAdaptationExecutor<?>> createAdaptationExecutors() throws RainbowConnectionException {
    return adaptationExecutorFactory.create(rainbowPropertyProvider, reportingPort);
  }

  public GaugeManager createGaugeManager() throws RainbowConnectionException {
    return gaugeManagerFactory.create(rainbowPropertyProvider, reportingPort).get(0);
  }

  public List<IRainbowAnalysis> createAnalysis() throws RainbowConnectionException {
    return rainbowAnalysisFactory.create(rainbowPropertyProvider, reportingPort);
  }

  public List<EffectorManager> createEffectorManagers() throws RainbowConnectionException {
    return effectorManagerFactory.create(rainbowPropertyProvider, reportingPort);
  }
}
