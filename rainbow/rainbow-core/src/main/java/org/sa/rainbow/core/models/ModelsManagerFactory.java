package org.sa.rainbow.core.models;

import org.sa.rainbow.core.IComponentFactory;
import org.sa.rainbow.core.IRainbowPropertyProvider;
import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.ports.IRainbowReportingPort;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public class ModelsManagerFactory implements IComponentFactory<ModelsManager> {

  @Override
  public List<ModelsManager> create(IRainbowPropertyProvider rainbowPropertyProvider, IRainbowReportingPort reportingPort) throws RainbowConnectionException {
    ModelsManager modelsManager = new ModelsManager ();
    modelsManager.initialize(reportingPort);
    modelsManager.start();
    return asList(modelsManager);
  }
}
