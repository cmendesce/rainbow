package org.sa.rainbow.core.adaptation;

import org.sa.rainbow.core.ComponentModelWrapper;
import org.sa.rainbow.core.IComponentFactory;
import org.sa.rainbow.core.IRainbowPropertyProvider;
import org.sa.rainbow.core.RainbowComponentT;
import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.models.ModelReference;
import org.sa.rainbow.core.ports.IRainbowReportingPort;
import org.sa.rainbow.util.Util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static org.sa.rainbow.core.RainbowConstants.*;

/**
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public class AdaptationExecutorFactory implements IComponentFactory<ComponentModelWrapper<IAdaptationExecutor<?>>> {

  public ComponentModelWrapper<IAdaptationExecutor<?>> create(IRainbowReportingPort reportingPort, String className, String modelReference)
          throws RainbowConnectionException {
    try {
      @SuppressWarnings("unchecked")
      Class<? extends IAdaptationExecutor<?>> cls = (Class<? extends IAdaptationExecutor<?>>) Class.forName(className);
      IAdaptationExecutor<?> adaptationExecutor = cls.newInstance();
      adaptationExecutor.initialize(reportingPort);
      if (modelReference != null) {
        ModelReference model = Util.decomposeModelReference(modelReference);
        adaptationExecutor.setModelToManage(model);
        adaptationExecutor.start();
        return new ComponentModelWrapper<>(model, adaptationExecutor);
      } else {
        reportingPort.error(RainbowComponentT.MASTER,
                MessageFormat.format(
                        "There is no model reference for adapation executor ''{0}''. Need to set the property ''{1}''.",
                        className, modelReference));
      }
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
            | ClassCastException e) {
      reportingPort.error(RainbowComponentT.MASTER, MessageFormat
              .format("Could not start the adaptation executor ''{0}''.", className), e);
    }
    return null;
  }

  @Override
  public List<ComponentModelWrapper<IAdaptationExecutor<?>>> create(IRainbowPropertyProvider rainbowPropertyProvider, IRainbowReportingPort reportingPort) throws RainbowConnectionException {
    int size = rainbowPropertyProvider.getProperty(PROPKEY_ADAPTATION_EXECUTOR_SIZE, 0);
    List<ComponentModelWrapper<IAdaptationExecutor<?>>> components = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      String className = rainbowPropertyProvider.getProperty(PROPKEY_ADAPTATION_EXECUTOR_CLASS + "_" + i);
      String modelReference = rainbowPropertyProvider.getProperty(PROPKEY_ADAPTATION_EXECUTOR_MODEL + "_" + i);
      components.add(create(reportingPort, className, modelReference));
    }
    return components;
  }
}
