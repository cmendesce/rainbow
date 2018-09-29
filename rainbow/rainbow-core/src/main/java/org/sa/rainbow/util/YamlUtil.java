/*
 * The MIT License
 *
 * Copyright 2014 CMU ABLE Group.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
/**
 * Created January 31, 2007.
 */
package org.sa.rainbow.util;

import org.ho.yaml.Yaml;
import org.sa.rainbow.core.Rainbow;
import org.sa.rainbow.core.RainbowConstants;
import org.sa.rainbow.core.gauges.GaugeDescription;
import org.sa.rainbow.core.gauges.GaugeInstanceDescription;
import org.sa.rainbow.core.gauges.GaugeTypeDescription;
import org.sa.rainbow.core.gauges.OperationRepresentation;
import org.sa.rainbow.core.models.*;
import org.sa.rainbow.core.models.UtilityPreferenceDescription.UtilityAttributes;
import org.sa.rainbow.core.util.TypedAttribute;
import org.sa.rainbow.core.util.TypedAttributeWithValue;
import org.sa.rainbow.translator.effectors.IEffector;
import org.sa.rainbow.translator.probes.IProbe;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * This utility class provides methods for parsing specific Yaml files for Rainbow. The class is non-instantiable.
 * 
 * @author Shang-Wen Cheng (zensoul@cs.cmu.edu)
 */
public abstract class YamlUtil {

    /**
     * Retrieves the utility definitions, then
     * <ul>
     * <li>store the weights
     * <li>store the utility functions
     * <li>for each tactic, store respective tactic attribute vectors.
     * 
     * @return UtilityPreferenceDescription the data structure of utility definitions.
     */
//    @SuppressWarnings ("unchecked")
//    public static UtilityPreferenceDescription loadUtilityPrefs () {
//        String utilityPath = Rainbow.instance ().getProperty (RainbowConstants.PROPKEY_UTILITY_PATH);
//        return loadUtilityPrefs (utilityPath);
//    }
    public static UtilityPreferenceDescription loadUtilityPrefs (String utilityPath) {
        UtilityPreferenceDescription prefDesc = new UtilityPreferenceDescription ();

        Map<String, Map<String, Map>> utilityDefMap = null;
        try {
            File defFile = new File (utilityPath);
            if (!defFile.exists ()) {
                defFile = Util.getRelativeToPath (Rainbow.instance ().getTargetPath (), utilityPath);
            }
            Object o = Yaml.load (defFile);
            Util.logger ().trace ("Utiltiy Def Yaml file loaded: " + o.toString ());
            utilityDefMap = (Map )o;
        }
        catch (FileNotFoundException e) {
            Util.logger ().error ("Loading Utiltiy Def Yaml file failed!", e);
            utilityDefMap = new HashMap<> ();
        }
        // store associated model
        Map modelMap = utilityDefMap.get ("model");
        // this is optional for backward compatibility
        if (modelMap != null) {
            prefDesc.associatedModel = new ModelReference ((String) modelMap.get ("name"), (String) modelMap.get ("type"));
        }

        // store weights
        Map<String, Map> weightMap = utilityDefMap.get ("weights");
        if (weightMap != null) {
            for (Map.Entry<String, Map> e : weightMap.entrySet ()) {
                Map<String, Double> kvMap = new HashMap<> ();
                double sum = 0.0;
                for (Object k : e.getValue ().keySet ()) {
                    Object v = e.getValue ().get (k);
                    if (k instanceof String && v instanceof Number) {
                        kvMap.put ((String )k, ((Number )v).doubleValue ());
                        sum += ((Number )v).doubleValue ();
                    }
                }
                if (sum < 1.0 || sum > 1.0) { // issue warning
                    Util.logger ().warn ("Weights for " + e.getKey () + " did NOT sum to 1!");
                }
                prefDesc.weights.put (e.getKey (), kvMap);
            }
            Util.logger ().trace (" - Weights collected: " + prefDesc.weights);
        }
        else {
            Util.logger ().error (MessageFormat.format (" - No Weights exist in ''{0}''", utilityPath));
        }
        // create utility functions
        Map<String, Map> utilMap = utilityDefMap.get ("utilities");
        if (utilMap != null) {
            for (String k : utilMap.keySet ()) {
                Map vMap = utilMap.get (k);
                UtilityAttributes ua = new UtilityAttributes ();
                ua.label = (String )vMap.get ("label");
                ua.mapping = (String )vMap.get ("mapping");
                ua.desc = (String )vMap.get ("description");
                ua.values = (Map<Number, Number> )vMap.get ("utility");
                prefDesc.addAttributes (k, ua);
            }
            Util.logger ().trace (" - Utility functions collected: " + prefDesc.getUtilities ());
        }
        else {
            Util.logger ().error (MessageFormat.format (" - No utilities exist in ''{0}''", utilityPath));
        }

        Map<String, Map> vectorMap = utilityDefMap.get ("vectors");
        if (vectorMap != null) {
            for (String k : vectorMap.keySet ()) {
                prefDesc.attributeVectors.put (k, vectorMap.get (k));
            }
            Util.logger ().trace (" - Utility attribute vectors collected: " + prefDesc.attributeVectors);
        }
        else {
            Util.logger ().error (MessageFormat.format (" - No vectors exist in ''{0}''", utilityPath));
        }

        return prefDesc;
    }

    /**
     * Acquires additional info (key-value pairs) based on the element Kind.
     * 
     * @param attr
     *            the DescriptionAttributes object to populate
     * @param infoMap
     *            the map of key-value info pairs
     */
    public static void extractArrays (DescriptionAttributes attr, Map<String, Object> infoMap) {
        List<String> arrayKeys = new ArrayList<> ();
        if (infoMap == null) return;
        for (Map.Entry<String, Object> pair : infoMap.entrySet ()) {
            if (pair.getKey ().endsWith (".length")) { // store just the key
                arrayKeys.add (pair.getKey ().replace (".length", ""));
            }
            String valStr = String.valueOf (pair.getValue ());
//            attr.getInfo().put (pair.getKey (), Util.evalTokens (valStr));
            attr.putInfo (pair.getKey (), Util.evalTokens (valStr));
        }
        /* Get any key-value pair named "key.length", remove it, find all
         * key.# items, and construct an array out of the list of values
         */
        for (String arrayKey : arrayKeys) {
            int length = Integer.parseInt (attr.getInfo ()./*remove*/get (arrayKey + ".length"));
            String[] valArray = new String[length]; // new array
            for (int i = 0; i < length; ++i) { // store item in array
                String itemKey = arrayKey + Util.DOT + i;
                if (attr.getInfo ().containsKey (itemKey)) {
                    valArray[i] = attr.getInfo ()./*remove*/get (itemKey);
                }
            }
            attr.putArray (arrayKey, valArray);
//            attr.getArrays().put (arrayKey, valArray); // store array
        }
    }

}
