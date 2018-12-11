package org.sa.rainbow.core.adaptation;

import org.apache.log4j.Logger;
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
 * Create the adaptation managers from the Rainbow properties file
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public class AdaptationManagerFactory implements IComponentFactory<IAdaptationManager<?>> {

  private static final Logger LOGGER = Logger.getLogger (AdaptationManagerFactory.class);

  public IAdaptationManager<?> create(IRainbowReportingPort reportingPort,
                                      String className, String modelReference) throws RainbowConnectionException {
      IAdaptationManager<?> adaptationManager = null;

      if (className != null) {
        try {
          @SuppressWarnings ("unchecked")
          Class<? extends IAdaptationManager<?>> cls = (Class<? extends IAdaptationManager<?>> )Class
                  .forName (className.trim ());
          adaptationManager = cls.newInstance();
          if (modelReference != null) {
            ModelReference model = Util.decomposeModelReference (modelReference);
            adaptationManager.initialize(reportingPort);
            adaptationManager.setModelToManage(model);
            adaptationManager.start();
            LOGGER.info("Initializing adaptation manager " + className);
          }
          else {
            reportingPort.error (RainbowComponentT.MASTER,
                    MessageFormat.format (
                            "There is no model reference for adapation manager ''{0}''. Need to set the property ''{1}''.",
                            className, modelReference));
          }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | ClassCastException e) {
          reportingPort.error (RainbowComponentT.MASTER, MessageFormat
                  .format ("Could not start the adaptation manager ''{0}''.", className), e);
        }
      }
    return adaptationManager;
  }

  @Override
  public List<IAdaptationManager<?>> create(IRainbowPropertyProvider rainbowPropertyProvider, IRainbowReportingPort reportingPort) throws RainbowConnectionException {
    int size = rainbowPropertyProvider.getProperty(PROPKEY_ADAPTATION_MANAGER_SIZE, 0);
    List<IAdaptationManager<?>> components = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      String className = rainbowPropertyProvider.getProperty(PROPKEY_ADAPTATION_MANAGER_CLASS + "_" + i);
      String modelReference = rainbowPropertyProvider.getProperty(PROPKEY_ADAPTATION_MANAGER_MODEL + "_" + i);
      components.add(create(reportingPort, className, modelReference));
    }
    return components;
  }
}
