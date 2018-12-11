package org.sa.rainbow.translator.effectors;

import org.sa.rainbow.core.IComponentFactory;
import org.sa.rainbow.core.IRainbowPropertyProvider;
import org.sa.rainbow.core.RainbowComponentT;
import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.ports.IRainbowReportingPort;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static org.sa.rainbow.core.RainbowConstants.PROPKEY_EFFECTOR_MANAGER_COMPONENT;
import static org.sa.rainbow.core.RainbowConstants.PROPKEY_EFFECTOR_MANAGER_COMPONENT_SIZE;

/**
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public class EffectorManagerFactory implements IComponentFactory<EffectorManager> {

  private EffectorManager create(IRainbowReportingPort reportingPort,
                                String em) throws RainbowConnectionException {
    try {
      @SuppressWarnings("unchecked")
      Class<? extends EffectorManager> cls = (Class<? extends EffectorManager>) Class.forName(em);
      EffectorManager effectorManager = cls.newInstance();
//        effMan.setEffectors (effectorDesc ());
      effectorManager.initialize(reportingPort);
      effectorManager.start();
      return effectorManager;
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
            | ClassCastException e) {
      reportingPort.error(RainbowComponentT.MASTER,
              MessageFormat.format("Could not start effector manager ''{0}''", em), e);
      return null;
    }
  }

  @Override
  public List<EffectorManager> create(IRainbowPropertyProvider rainbowPropertyProvider, IRainbowReportingPort reportingPort) throws RainbowConnectionException {
    int size = rainbowPropertyProvider.getProperty(PROPKEY_EFFECTOR_MANAGER_COMPONENT_SIZE, 0);
    List<EffectorManager> components = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      String className = rainbowPropertyProvider.getProperty(PROPKEY_EFFECTOR_MANAGER_COMPONENT + "_" + i);
      components.add(create(reportingPort, className.trim()));
    }
    return components;
  }
}
