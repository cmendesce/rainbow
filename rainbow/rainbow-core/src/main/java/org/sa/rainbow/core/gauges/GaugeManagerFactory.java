package org.sa.rainbow.core.gauges;

import org.sa.rainbow.core.IComponentFactory;
import org.sa.rainbow.core.IRainbowPropertyProvider;
import org.sa.rainbow.core.RainbowConstants;
import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.ports.IRainbowReportingPort;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public class GaugeManagerFactory implements IComponentFactory<GaugeManager> {

  @Override
  public List<GaugeManager> create(IRainbowPropertyProvider rainbowPropertyProvider, IRainbowReportingPort reportingPort)
          throws RainbowConnectionException {

      String factoryClass = rainbowPropertyProvider.getProperty(RainbowConstants.PROPKEY_GAUGES_FACTORY);
      IGaugeLoader loader = null;//(IGaugeLoader) Class.forName(factoryClass);
      GaugeManager gaugeManager = new GaugeManager (loader.load());
      gaugeManager.initialize(reportingPort);
      gaugeManager.start();
      return asList(gaugeManager);



  }
}
