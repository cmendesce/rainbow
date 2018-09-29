package org.sa.rainbow.core.gauges;

import org.apache.log4j.Logger;
import org.ho.yaml.Yaml;
import org.sa.rainbow.core.models.ModelReference;
import org.sa.rainbow.core.util.TypedAttribute;
import org.sa.rainbow.core.util.TypedAttributeWithValue;
import org.sa.rainbow.util.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;

public class GaugeLoader implements IGaugeLoader {

  private static final Logger logger = Logger.getLogger(GaugeLoader.class);
  private final File configFile;

  public GaugeLoader(File configFile) {
    this.configFile = configFile;
  }

  @Override
  public GaugeDescription load() {
    GaugeDescription gd = new GaugeDescription ();

    Map<String, Map<String, Map>> gaugeSpecMap;
    try {

      Object o = Yaml.load (configFile);
      logger.trace ("Gauge Spec Yaml file loaded: " + o.toString ());
      gaugeSpecMap = (Map )o;

      Map<String, Map> typeMap = gaugeSpecMap.get ("gauge-types");
      if (typeMap != null) {
        for (Map.Entry<String, Map> typeSpec : typeMap.entrySet ()) {
          // map type name to Gauge type desc
          String gaugeType = typeSpec.getKey ();
          Map<String, Object> attrMap = typeSpec.getValue (); // get attribute map
          // get comment
          String typeComment = (String) attrMap.get ("comment");
          // populate type description
          GaugeTypeDescription gaugeTypeSpec = new GaugeTypeDescription (gaugeType, typeComment);
          gd.typeSpec.put (gaugeType, gaugeTypeSpec);
          // get mappings of reported values
          Map<String, String> values = (Map<String, String>) attrMap.get ("commands");
          for (Map.Entry<String, String> value : values.entrySet ()) {
            String valName = value.getKey ();
            String signature = value.getValue ();
            gaugeTypeSpec.addCommandSignature (valName, signature);
          }
          // get mappings of setup params
          Map<String, Map> params = (Map<String, Map>) attrMap.get ("setupParams");
          for (Map.Entry<String, Map> param : params.entrySet ()) {
            String pname = param.getKey ();
            Map<String, Object> paramAttr = param.getValue ();
            String ptype = (String) paramAttr.get ("type");
            Object pdefault = paramAttr.get ("default");
            if (!ptype.equals ("String")) {
              pdefault = Util.parseObject (pdefault.toString (), ptype);
            }
            if (pdefault != null && pdefault instanceof String) {
              pdefault = Util.evalTokens ((String) pdefault);
            }
            gaugeTypeSpec.addSetupParam (new TypedAttributeWithValue(pname, ptype, pdefault));
          }
          // get mappings of config params
          params = (Map<String, Map>) attrMap.get ("configParams");
          for (Map.Entry<String, Map> param : params.entrySet ()) {
            String pname = param.getKey ();
            Map<String, Object> paramAttr = param.getValue ();
            String ptype = (String) paramAttr.get ("type");
            Object pdefault = paramAttr.get ("default");
            if (!ptype.equals ("String")) {
              pdefault = Util.parseObject (pdefault.toString (), ptype);
            }
            if (pdefault != null && pdefault instanceof String) {
              pdefault = Util.evalTokens ((String) pdefault);
            }
            gaugeTypeSpec.addConfigParam (new TypedAttributeWithValue (pname, ptype, pdefault));
          }
        }
        logger.trace (" - Gauge Types collected: " + gd.typeSpec.keySet ());
      }
      else
        logger.warn (" - No gauge types specified");
      // store gauge instances
      Map<String, Map> instanceMap = gaugeSpecMap.get ("gauge-instances");
      if (instanceMap != null) {
        for (Map.Entry<String, Map> instSpec : instanceMap.entrySet ()) {
          // map name to Gauge instance
          String gaugeName = instSpec.getKey ();
          Map<String, Object> attrMap = instSpec.getValue (); // get attribute map
          // get type name, model description, comment
          String gaugeType = (String) attrMap.get ("type");
          TypedAttribute modelDesc = TypedAttribute.parsePair ((String) attrMap.get ("model"));
          String instComment = (String) attrMap.get ("comment");
          // populate instance description
          GaugeTypeDescription gaugeTypeSpec = gd.typeSpec.get (gaugeType);
          if (gaugeTypeSpec == null) {
            logger.error (MessageFormat.format (
              "Cannot find gauge type: {0} referred to in gauge ''{1}''.", gaugeType, gaugeName));
            continue;
          }
          GaugeInstanceDescription gaugeInstSpec = gaugeTypeSpec.makeInstance (gaugeName, instComment);
          gaugeInstSpec.setModelDesc (modelDesc);
          gd.instSpec.put (gaugeName, gaugeInstSpec);
          // get commands
          Map<String, String> commandMappings = (Map<String, String>) attrMap.get ("commands");
          for (Map.Entry<String, String> cmd : commandMappings.entrySet ()) {
            String key = cmd.getKey ();
            String[] args = Util.evalCommand (cmd.getValue ());
            gaugeInstSpec.addCommand (Util.evalTokens (key),
              new OperationRepresentation (args[1],
                new ModelReference(modelDesc.getName (), modelDesc.getType ()), args[0],

                Arrays.copyOfRange (args, 2, args.length)));

          }

          // get mappings of setup values and store in setup param info
          Map<String, Object> values = (Map<String, Object>) attrMap.get ("setupValues");
          for (Map.Entry<String, Object> param : values.entrySet ()) {
            String paramName = param.getKey ();
            Object paramValue = param.getValue ();
            if (paramValue != null) { // set new value
              if (paramValue instanceof String) {
                paramValue = Util.evalTokens ((String) paramValue);
              }
              TypedAttributeWithValue setupParam = gaugeInstSpec.findSetupParam (paramName);
              if (setupParam != null) {
                setupParam.setValue (paramValue);
              }
            }
          }
          // get mappings of config values and store in config param info
          values = (Map<String, Object>) attrMap.get ("configValues");
          for (Map.Entry<String, Object> param : values.entrySet ()) {
            String paramName = param.getKey ();
            Object paramValue = param.getValue ();
            if (paramValue != null) { // set new value
              if (paramValue instanceof String) {
                paramValue = Util.evalTokens ((String) paramValue);
              }
              TypedAttributeWithValue configParam = gaugeInstSpec.findConfigParam (paramName);
              if (configParam != null) {
                if (!configParam.getType ().equals ("String")) {
                  paramValue = Util.parseObject (paramValue.toString (), configParam.getType ());
                }
                configParam.setValue (paramValue);
              }
            }
          }
        }
        logger.trace (" - Gauge Instances collected: " + gd.instSpec.keySet ());
      }
      else {
        logger.warn ( " - No gauge instances specified");
      }
    }
    catch (FileNotFoundException e) {
      logger.error ("Loading Gauge Spec Yaml file failed!", e);
    }

    // store gauge type descriptions

    return gd;
  }

}
