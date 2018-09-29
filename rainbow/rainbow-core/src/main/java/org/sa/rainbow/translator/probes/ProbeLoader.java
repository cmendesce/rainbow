package org.sa.rainbow.translator.probes;

import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;
import org.sa.rainbow.core.Rainbow;
import org.sa.rainbow.core.models.ProbeDescription;
import org.sa.rainbow.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import static org.sa.rainbow.util.YamlUtil.extractArrays;

public class ProbeLoader implements IProbeLoader {

  private static final Logger logger = Logger.getLogger(ProbeLoader.class);
  private final File probeFile;

  public ProbeLoader(File configFile) {
    this.probeFile = configFile;
  }

  @Override
  public ProbeDescription load() {
    ProbeDescription ed = new ProbeDescription ();
    Map probeMap = null;
    try {
      Object o = Yaml.load (probeFile);
      logger.trace ("Probe Desc Yaml file loaded: " + o.toString ());
      probeMap = (Map )o;
      Map<String, String> varMap = (Map<String, String> )probeMap.get ("vars");
      if (varMap != null)
        for (Map.Entry<String, String> varPair : varMap.entrySet ()) {
          Rainbow.instance ().setProperty (varPair.getKey (), Util.evalTokens (varPair.getValue ()));
        }

      // store probe description info
      Map<String, Map> pbMap = (Map<String, Map> )probeMap.get ("probes");
      if (pbMap != null) {
        for (Map.Entry<String, Map> pbInfo : pbMap.entrySet ()) {
          ProbeDescription.ProbeAttributes pa = new ProbeDescription.ProbeAttributes ();

          // get probe name
          pa.name = pbInfo.getKey ();
          Map<String, Object> attrMap = pbInfo.getValue (); // get attribute map
          // get location, alias, and probe type
          pa.setLocation (Util.evalTokens ((String) attrMap.get ("location")));
          pa.alias = (String) attrMap.get ("alias");
          pa.setKindName ((String) attrMap.get ("type"));
          pa.kind = IProbe.Kind.valueOf (pa.getKindName ().toUpperCase ());
          Map<String, Object> addlInfoMap = (Map<String, Object>) attrMap.get (pa.infoPropName ());
          extractArrays (pa, addlInfoMap);
          ed.probes.add (pa);
        }
        logger.trace (" - Probe collected: " + ed.probes);
      }
      else {
        logger.warn (" - No probes specified");

      }
    }
    catch (FileNotFoundException e) {
      logger.error ("Loading Probe Desc Yaml file failed!", e);
    }

    // acquire "variable" declarations and store as rainbow properties

    return ed;
  }
}
