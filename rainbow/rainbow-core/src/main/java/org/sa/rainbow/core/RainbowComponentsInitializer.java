package org.sa.rainbow.core;

import org.sa.rainbow.core.error.RainbowException;

/**
 * @author Carlos Mendes (cmendesce@gmail.com)
 */
public interface RainbowComponentsInitializer {

  /**
   * Initializes all of the components of the Rainbow master: adaptation managers, effector managers, analyses,
   * executors
   *
   * @throws RainbowException
   */
  void initialize () throws RainbowException;
}
