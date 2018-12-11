package org.sa.rainbow.core;

import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.ports.IRainbowReportingPort;

import java.util.List;

/**
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public interface IComponentFactory<C extends Object> {

  List<C> create(IRainbowPropertyProvider rainbowPropertyProvider,
                 IRainbowReportingPort reportingPort) throws RainbowConnectionException;
}
