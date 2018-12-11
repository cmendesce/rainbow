package org.sa.rainbow.translator.probes;

import org.sa.rainbow.core.AbstractRainbowRunnable;
import org.sa.rainbow.core.RainbowComponentT;

/**
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public class ProbeManager extends AbstractRainbowRunnable {
  /**
   * Default Constructor with name for the thread.
   *
   * @param name Name of the Thread
   */
  public ProbeManager(String name) {
    super(name);
  }

  @Override
  protected void log(String txt) {

  }

  @Override
  protected void runAction() {

  }

  @Override
  public RainbowComponentT getComponentType() {
    return RainbowComponentT.PROBE_MANAGER;
  }

  @Override
  public void dispose() {

  }
}
