package org.sa.rainbow.translator.effectors;

import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;
import org.sa.rainbow.core.Rainbow;
import org.sa.rainbow.core.gauges.OperationRepresentation;
import org.sa.rainbow.core.models.EffectorDescription;
import org.sa.rainbow.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import static org.sa.rainbow.util.YamlUtil.extractArrays;

/**
 * Loads effectors from yaml file defined in rainbow.properties by the key ${customize.effectors.path}
 * @author Carlos Mendes
 */
public class EffectorLoader implements IEffectorLoader {

  private static final Logger logger = Logger.getLogger(EffectorLoader.class);
  private final File effectorFile;

  public EffectorLoader(File effectorFile) {
    this.effectorFile = effectorFile;
  }

  /**
   * Loads the effectors from yaml file
   * @return All declared effectors
   */
  public EffectorDescription load() {
    EffectorDescription ed = new EffectorDescription ();

    Map effectorMap;
    try {

      if (!effectorFile.exists ()) {
        logger.error ("Effector file does not exist");
        return ed;
      }
      Object o = Yaml.load (effectorFile);
      logger.trace ("Effector Desc Yaml file loaded: " + o.toString ());
      effectorMap = (Map )o;
      // acquire "variable" declarations and store as rainbow properties
      Map<String, String> varMap = (Map<String, String> )effectorMap.get ("vars");
      if (varMap != null)
        for (Map.Entry<String, String> varPair : varMap.entrySet ()) {
          Rainbow.instance ().setProperty (varPair.getKey (), Util.evalTokens (varPair.getValue ()));
        }

      // store effector type info
      Map<String, Map> effTypeMap = (Map<String, Map> )effectorMap.get ("effector-types");
      if (effTypeMap != null) {
        for (Map.Entry<String, Map> etInfo : effTypeMap.entrySet ()) {
          EffectorDescription.EffectorAttributes ea = new EffectorDescription.EffectorAttributes ();
          ea.name = etInfo.getKey ();
          Map<String, Object> attrMap = etInfo.getValue ();
          ea.setKindName ((String )attrMap.get ("type"));
          ea.setLocation (Util.evalTokens ((String )attrMap.get ("location"))); // the default location
          String commandSignature = Util.evalTokens ((String )attrMap.get ("command"));
          if (commandSignature != null) {
            ea.setCommandPattern (OperationRepresentation.parseCommandSignature (commandSignature));
          }
          Map<String, Object> addlInfoMap = (Map<String, Object> )attrMap.get (ea.infoPropName ());
          extractArrays (ea, addlInfoMap);
          ed.effectorTypes.put (ea.name, ea);
        }
      }

      // store effector description info
      Map<String, Map> effMap = (Map<String, Map> )effectorMap.get ("effectors");
      for (Map.Entry<String, Map> effInfo : effMap.entrySet ()) {
        EffectorDescription.EffectorAttributes ea = new EffectorDescription.EffectorAttributes ();

        // get effector name
        ea.name = effInfo.getKey ();
        Map<String, Object> attrMap = effInfo.getValue (); // get attribute map
        ea.setKindName ((String )attrMap.get ("type"));
        if (ea.getKindName () != null) {
          ea.setKind (IEffector.Kind.valueOf (ea.getKindName ().toUpperCase ()));
        }

        String effectorType = (String )attrMap.get ("effector-type");
        if (effectorType != null) {
          ea.effectorType = ed.effectorTypes.get (effectorType);
        }

        // get location and effector type
        ea.setLocation (Util.evalTokens ((String )attrMap.get ("location")));
        String commandSignature = Util.evalTokens ((String )attrMap.get ("command"));
        if (commandSignature != null) {
          ea.setCommandPattern (OperationRepresentation.parseCommandSignature (commandSignature));
        }

        Map<String, Object> addlInfoMap = (Map<String, Object> )attrMap.get (ea.infoPropName ());
        extractArrays (ea, addlInfoMap);
        ed.effectors.add (ea);
      }
      logger.trace (" - Effectors collected: " + ed.effectors);
    }
    catch (FileNotFoundException e) {
      logger.error ("Loading Effector Desc Yaml file failed!", e);
    }

    return ed;
  }
}
