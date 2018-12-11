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
package org.sa.rainbow.core;

import org.apache.log4j.Logger;
import org.sa.rainbow.core.adaptation.IAdaptationExecutor;
import org.sa.rainbow.core.adaptation.IAdaptationManager;
import org.sa.rainbow.core.adaptation.IEvaluable;
import org.sa.rainbow.core.analysis.IRainbowAnalysis;
import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.error.RainbowException;
import org.sa.rainbow.core.gauges.*;
import org.sa.rainbow.core.globals.ExitState;
import org.sa.rainbow.core.models.EffectorDescription;
import org.sa.rainbow.core.models.EffectorDescription.EffectorAttributes;
import org.sa.rainbow.core.models.ModelsManager;
import org.sa.rainbow.core.models.ProbeDescription;
import org.sa.rainbow.core.models.ProbeDescription.ProbeAttributes;
import org.sa.rainbow.core.ports.*;
import org.sa.rainbow.core.ports.IMasterConnectionPort.ReportType;
import org.sa.rainbow.core.util.TypedAttributeWithValue;
import org.sa.rainbow.translator.effectors.EffectorLoader;
import org.sa.rainbow.translator.effectors.EffectorManager;
import org.sa.rainbow.translator.effectors.IEffectorExecutionPort.Outcome;
import org.sa.rainbow.translator.effectors.IEffectorLoader;
import org.sa.rainbow.translator.probes.IProbeLoader;
import org.sa.rainbow.translator.probes.ProbeLoader;
import org.sa.rainbow.util.Beacon;
import org.sa.rainbow.util.RainbowConfigurationChecker;
import org.sa.rainbow.util.RainbowConfigurationChecker.Problem;
import org.sa.rainbow.util.RainbowConfigurationChecker.ProblemT;
import org.sa.rainbow.util.Util;

import java.io.File;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;

public class RainbowMaster extends AbstractRainbowRunnable implements IMasterCommandPort, IRainbowMaster {

    private IRainbowEnvironment m_rainbowEnvironment = Rainbow.instance ();

    static final Logger LOGGER = Logger.getLogger (Rainbow.class.getCanonicalName ());

    final Map<String, IDelegateManagementPort> m_delegates = new HashMap<> ();
    final Map<String, IDelegateConfigurationPort> m_delegateConfigurtationPorts = new HashMap<> ();
    final Map<String, Properties> m_delegateInfo = new HashMap<> ();

    IMasterConnectionPort       m_delegateConnection;
    private final Map<String, Beacon> m_heartbeats = new HashMap<> ();

    private ModelsManager m_modelsManager;

    private ProbeDescription    m_probeDesc;
    private EffectorDescription m_effectorDesc;
    private GaugeDescription    m_gaugeDesc;
//    private UtilityPreferenceDescription     m_prefDesc;

    private GaugeManager m_gaugeManager;


    private Collection<IRainbowAnalysis> m_analyses = Collections.emptySet ();


    private List<IAdaptationManager<?>> m_adaptationManagers = Collections.emptyList ();

    private List<IAdaptationExecutor<?>> m_adaptationExecutors = Collections.emptyList ();


    private Collection<EffectorManager> m_effectorManagers = Collections.emptySet ();

    private final Map<String, Beacon> m_terminatedDelegates = Collections
            .synchronizedMap (new HashMap<String, Beacon> ());

    private final Set<String> m_nonCompliantDelegates = Collections.<String> synchronizedSet (new HashSet<String> ());


    private Boolean m_initialized = Boolean.FALSE;

    private IGaugeLoader    m_gaugeLoader;
    private IEffectorLoader m_effectorLoader;
    private IProbeLoader    m_probeLoader;

    private RainbowComponentsInitializer m_initializer;

    public void setRainbowEnvironment (IRainbowEnvironment env) {
        m_rainbowEnvironment = env;
    }

    public RainbowMaster (IGaugeLoader gaugeLoader, IEffectorLoader effectorLoader, IProbeLoader probeLoader) {
        super ("Rainbow Master");
        m_rainbowEnvironment.setMaster (this);

        m_gaugeLoader = gaugeLoader;
        if (m_gaugeLoader == null) {
            File gaugeSpec = Util.getRelativeToPath (Rainbow.instance ().getTargetPath (),
                    Rainbow.instance ().getProperty (RainbowConstants.PROPKEY_GAUGES_PATH));
            m_gaugeLoader = new GaugeLoader(gaugeSpec);
        }

        m_effectorLoader = effectorLoader;
        if (m_effectorLoader == null) {
            String effectorPath = Rainbow.instance ().getProperty (RainbowConstants.PROPKEY_EFFECTORS_PATH);
            if (effectorPath == null) {
                Util.logger ().error (MessageFormat.format ("No property defined for ''{0}''. No effectors loaded.",
                        RainbowConstants.PROPKEY_EFFECTORS_PATH));
            }
            File effectorFile = Util.getRelativeToPath (Rainbow.instance ().getTargetPath (), effectorPath);
            m_effectorLoader = new EffectorLoader(effectorFile);
        }

        m_probeLoader = probeLoader;
        if (m_probeLoader == null) {
            String probePath = Rainbow.instance ().getProperty (RainbowConstants.PROPKEY_PROBES_PATH);
            if (probePath == null) {
                Util.logger ().error (MessageFormat.format ("No property defined for ''{0}''. No probes loaded.",
                        RainbowConstants.PROPKEY_PROBES_PATH));
            }
            File probeFile = Util.getRelativeToPath (Rainbow.instance ().getTargetPath (), probePath);
            m_probeLoader = new ProbeLoader(probeFile);
        }
    }

    public RainbowMaster () {
        this(null, null, null);
    }

    public void initialize () throws RainbowException {
        synchronized (m_initialized) {
            readConfiguration ();
            initializeConnections ();
            super.initialize (m_reportingPort);
            initializeRainbowComponents ();
            RainbowConfigurationChecker checker = new RainbowConfigurationChecker (this);
            checker.checkRainbowConfiguration ();
            for (Problem p : checker.getProblems ()) {
                if (p.problem == ProblemT.ERROR) {
                    m_reportingPort.error (getComponentType (), p.msg);
                }
                else {
                    m_reportingPort.warn (getComponentType (), p.msg);
                }
            }
            m_initialized = true;
        }
    }

    private void readConfiguration () {
        probeDesc ();
        effectorDesc ();
        gaugeDesc ();
//        preferenceDesc (); // This has been moved to a model
    }

    /**
     * Initializes all of the components of the Rainbow master: adaptation managers, effector managers, analyses,
     * executors
     * 
     * @throws RainbowException
     */
    private void initializeRainbowComponents () throws RainbowException {

        RainbowComponentsFactory factory = new RainbowComponentsFactory(m_rainbowEnvironment, m_reportingPort);

        m_modelsManager = factory.createModelsManager();
        m_gaugeManager = factory.createGaugeManager();
        m_effectorManagers = factory.createEffectorManagers();
        m_analyses = factory.createAnalysis();
        m_adaptationManagers = factory.createAdaptationManagers();
        m_adaptationExecutors = factory.createAdaptationExecutors();
    }

    private void initializeConnections () throws RainbowConnectionException {
        m_delegateConnection = RainbowPortFactory.createDelegateConnectionPort (this);
        m_reportingPort = RainbowPortFactory.createMasterReportingPort ();
    }

    @Override
    public ModelsManager modelsManager () {
        return m_modelsManager;
    }

    /**
     * Connects a new delegate and sends the appropriate configuration information to the delegate
     * 
     * @param delegateID
     * @param connectionProperties
     */

    public IDelegateManagementPort connectDelegate (String delegateID, Properties connectionProperties) {
        LOGGER.debug (MessageFormat.format ("Master received connection request from: {0} at {1}", delegateID,
                connectionProperties.getProperty (RainbowConstants.PROPKEY_DEPLOYMENT_LOCATION, "Unknown Location")));
        try {
            m_delegateInfo.put (delegateID, connectionProperties);
            IDelegateManagementPort delegatePort = RainbowPortFactory.createMasterDeploymentPort (this, delegateID,
                    connectionProperties);
            // Check to see if there is already a registered delegate running on the machine
            m_delegates.put (delegateID, delegatePort);
            IDelegateConfigurationPort delegateConfigurationPort = RainbowPortFactory
                    .createDelegateConfigurationPortClient (delegateID);
            m_delegateConfigurtationPorts.put (delegateID, delegateConfigurationPort);
            // Add a second to the heartbeat to allow for communication time
            // TODO: Must be a better way to do this...
            Beacon beacon = new Beacon (
                    Long.parseLong (m_rainbowEnvironment.getProperty (RainbowConstants.PROPKEY_DELEGATE_BEACONPERIOD, "1000"))
                    + 1000);
            synchronized (m_heartbeats) {
                m_heartbeats.put (delegatePort.getDelegateId (), beacon);
            }
            m_nonCompliantDelegates.add (delegatePort.getDelegateId ());
            beacon.mark ();
            LOGGER.info (MessageFormat.format ("Master created management connection with delegate {0}", delegateID));
            requestDelegateConfiguration(delegateID);
            return delegatePort;
        } catch (NumberFormatException | RainbowConnectionException e) {
            LOGGER.error (MessageFormat.format (
                    "Rainbow master could not create the management interface to the delegate {0}", delegateID));
            m_delegateConnection.disconnectDelegate (delegateID);
        }
        return null;
    }

    /**
     * Called by a delegate port to request information be sent to it
     * 
     * @param delegateID
     */
    public void requestDelegateConfiguration (String delegateID) {
        IDelegateConfigurationPort delegate = m_delegateConfigurtationPorts.get (delegateID);
        if (delegate != null) {
            Properties properties = filterPropertiesForDelegate(delegateID);
            List<ProbeAttributes> probes = filterProbesForDelegate(delegateID);
            List<EffectorAttributes> effectors = filterEffectorsForDelegate(delegateID);
            List<GaugeInstanceDescription> gauges = filterGaugesForDelegate(delegateID);
            LOGGER.info (MessageFormat.format ("Sending configuration information to {0}. Probes: {1}; Effectors: {2}; Gauges: {3}", delegateID, probes.size(), effectors.size(), gauges.size()));
            delegate.sendConfigurationInformation (properties, probes, effectors, gauges);
        }
        else {
            LOGGER.error (
                MessageFormat.format ("Received configuration request from unknown delegate {0}.", delegateID));
        }
    }


    private List<GaugeInstanceDescription> filterGaugesForDelegate (String delegateID) {
        if (gaugeDesc ().instSpec == null) return Collections.emptyList ();

        Properties delegateInfo = m_delegateInfo.get (delegateID);
        String deploymentInfo;
        if (delegateInfo == null
                || (deploymentInfo = delegateInfo.getProperty (RainbowConstants.PROPKEY_DEPLOYMENT_LOCATION)) == null) {
            LOGGER.error ("There is no location information associated with " + delegateID);
            return Collections.emptyList ();
        }
        return filterGaugesForLocation (deploymentInfo);
    }


    List<GaugeInstanceDescription> filterGaugesForLocation (String deploymentInfo) {
        List<GaugeInstanceDescription> gauges = new LinkedList<> ();
        for (GaugeInstanceDescription gid : gaugeDesc ().instSpec.values ()) {
            TypedAttributeWithValue targetIP = gid.findSetupParam ("targetIP");
            if (deploymentInfo.equals (targetIP.getValue ())) {
                gauges.add (gid);
            }
        }
        return gauges;
    }


    private List<EffectorAttributes> filterEffectorsForDelegate (String delegateID) {
        if (effectorDesc ().effectors == null)
            return Collections.emptyList ();
        else {
            Properties delegateInfo = m_delegateInfo.get (delegateID);
            String deploymentInfo;

            if (delegateInfo == null || (deploymentInfo = delegateInfo
                    .getProperty (RainbowConstants.PROPKEY_DEPLOYMENT_LOCATION)) == null) {
                LOGGER.error ("There is no location information associated with " + delegateID);
                return Collections.emptyList ();
            }
            return filterEffectorsForLocation (deploymentInfo);
        }
    }


    List<EffectorAttributes> filterEffectorsForLocation (String deploymentInfo) {
        List<EffectorAttributes> effectors = new LinkedList<> ();
        for (EffectorAttributes probe : effectorDesc ().effectors) {
            if (probe.getLocation ().equals (deploymentInfo)) {
                effectors.add (probe);
            }
        }
        return effectors;
    }

    /**
     * Called when a delegate sends a heartbeat message
     * 
     * @param delegateID
     *            The IP of the delegate
     */
    public void processHeartbeat (String delegateID) {
        IDelegateManagementPort delegate = m_delegates.get (delegateID);
        if (delegate != null) {
            Beacon hb;
            synchronized (m_heartbeats) {
                hb = m_heartbeats.get (delegate.getDelegateId ());
            }
            if (hb == null) {
                LOGGER.error (MessageFormat.format ("Received heartbeat from unknown delegate at {0}.", delegateID));
            }
            else {
                Properties properties = m_delegateInfo.get (delegateID);
                String loc = "???";
                if (properties != null) {
                    loc = properties.getProperty (RainbowConstants.PROPKEY_DEPLOYMENT_LOCATION);
                }

                m_reportingPort.info (RainbowComponentT.MASTER,
                        MessageFormat.format ("Heartbeat from {0}@{1}", delegateID, loc));
                hb.mark ();
                m_nonCompliantDelegates.remove (delegate.getDelegateId ());
            }
        }
        else {
            LOGGER.error (MessageFormat.format ("Received heartbeat from unknown delegate at {0}.", delegateID));
        }
    }

    /**
     * Filters the properties to only report those properties that are relevant to the delegate
     * 
     * @param delegateID
     * @return
     */

    private Properties filterPropertiesForDelegate (String delegateID) {
        return m_rainbowEnvironment.allProperties ();
    }


    private List<ProbeAttributes> filterProbesForDelegate (String delegateID) {
        if (probeDesc ().probes == null)
            return Collections.emptyList ();
        else {
            Properties delegateInfo = m_delegateInfo.get (delegateID);
            String deploymentInfo;
            if (delegateInfo == null || (deploymentInfo = delegateInfo
                    .getProperty (RainbowConstants.PROPKEY_DEPLOYMENT_LOCATION)) == null) {
                LOGGER.error ("There is no location information associated with " + delegateID);
                return Collections.emptyList ();
            }
            return filterProbesForLocation (deploymentInfo);
        }
    }


    List<ProbeAttributes> filterProbesForLocation (String deploymentInfo) {
        List<ProbeAttributes> probes = new LinkedList<> ();
        for (ProbeAttributes probe : probeDesc ().probes) {
            if (probe.getLocation ().equals (deploymentInfo)) {
                probes.add (probe);
            }
        }
        return probes;
    }

    @Override
    public void stop () {
        for (IRainbowAnalysis a : m_analyses) {
            a.stop ();
        }
        for (EffectorManager e : m_effectorManagers) {
            e.stop ();
        }
        for (IAdaptationManager<?> a : m_adaptationManagers) {
            a.stop ();
        }
        for (IAdaptationExecutor<?> a : m_adaptationExecutors) {
            a.stop ();
        }
        super.stop ();
    }

    @Override
    public void restart () {
        for (IRainbowAnalysis a : m_analyses) {
            a.restart ();
        }
        for (EffectorManager e : m_effectorManagers) {
            e.restart ();
        }
        for (IAdaptationManager<?> a : m_adaptationManagers) {
            a.restart ();
        }
        for (IAdaptationExecutor<?> a : m_adaptationExecutors) {
            a.restart ();
        }
        super.restart ();
    }

    @Override
    public void dispose () {

    }

    @Override
    protected void log (String txt) {
        LOGGER.info (MessageFormat.format ("RM: {0}", txt));
    }

    @Override
    protected void runAction () {
        checkTerminations ();
        checkHeartbeats ();
    }

    private void checkTerminations () {
        synchronized (m_terminatedDelegates) {
            if (!m_terminatedDelegates.isEmpty ()) {
                for (Iterator<Entry<String, Beacon>> iterator = m_terminatedDelegates.entrySet ().iterator (); iterator
                        .hasNext ();) {
                    Entry<String, Beacon> e = iterator.next ();
                    if (e.getValue ().periodElapsed ()) {
                        m_reportingPort.warn (getComponentType (),
                                "Did not hear back from terminated delegate " + e.getKey () + ". Flushing anyway.");
                        flushDelegate (e.getKey ());
                        iterator.remove ();
                    }
                }
            }
        }
    }

    private void checkHeartbeats () {
        try {
            synchronized (m_heartbeats) {
                Set<Entry<String, Beacon>> entrySet = m_heartbeats.entrySet ();
                for (Iterator<Entry<String, Beacon>> iterator = entrySet.iterator (); iterator.hasNext (); ) {
                    Entry<String, Beacon> entry = iterator.next ();
                    if (entry.getValue ().periodElapsed ()) {
                        Properties properties = m_delegateInfo.get (entry.getKey ());
                        String loc = "???";
                        if (properties != null) {
                            loc = properties.getProperty (RainbowConstants.PROPKEY_DEPLOYMENT_LOCATION);
                        }
                        if (!m_nonCompliantDelegates.contains (entry.getKey ())) {
                            m_nonCompliantDelegates.add (entry.getKey ());
                            m_reportingPort.error (RainbowComponentT.MASTER,
                                    MessageFormat.format ("No Heartbeat from {0}@{1}", entry.getKey ()
                                            , loc));

                            LOGGER.error (MessageFormat.format (
                                    "Delegate {0} has not given a heartbeat withing the right time", entry.getKey ()));
                        }
                        if (entry.getValue ().isExpired ()) {
                            m_reportingPort.error (RainbowComponentT.MASTER,
                                    MessageFormat.format (
                                            "Delegate {0}@{1} has not sent a heartbeat in a while. " +
                                                    "Forgetting about it",
                                                    entry.getKey (), loc));
                            deregisterDelegate (entry.getKey (), loc);
                            iterator.remove ();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            m_reportingPort.error (RainbowComponentT.MASTER, "Failed in checking heartbeats", t);
        }
    }

    @Override
    public boolean allDelegatesOK () {
        return m_nonCompliantDelegates.isEmpty ();
    }

    private void deregisterDelegate (String did, String loc) {
        flushDelegate (did);
    }

    public void disconnectDelegate (String id) {
        LOGGER.info (MessageFormat.format ("RM: Disconnecting delegate: {0}", id));
        synchronized (m_terminatedDelegates) {
            m_terminatedDelegates.remove (id);
        }
        flushDelegate (id);
    }

    void flushDelegate (String id) {
        synchronized (m_heartbeats) {
            m_heartbeats.remove (id);
        }
//        IDelegateManagementPort deploymentPort = m_delegates.remove (id);
//        deploymentPort.dispose ();
        m_delegateInfo.remove (id);
//        IDelegateConfigurationPort port = m_delegateConfigurtationPorts.remove (id);
//        port.dispose ();
    }

    @Override
    public void terminate () {
        for (Entry<String, IDelegateManagementPort> entry : m_delegates.entrySet ()) {
            disconnectDelegate (entry.getKey ());
            entry.getValue ().terminateDelegate ();
            entry.getValue ().dispose ();
        }
        m_delegateConnection.dispose ();

        m_reportingPort.dispose ();
        // TODO: Terminate threads
//        try {
//            Thread.sleep (4000);
//        }
//        catch (InterruptedException e) {
//        }
        super.terminate ();
        while (!isTerminated ()) {
            try {
                Thread.sleep (500);
            }
            catch (InterruptedException e) {
            }
        }
    }

// Methods below this point are used for testing purposes, and so are package protected.

    Map<? extends String, ? extends Beacon> getHeartbeatInfo () {
        return m_heartbeats;
    }

    @Override
    public ProbeDescription probeDesc () {
        synchronized (m_initialized) {
            if (m_probeDesc == null) {
                m_probeDesc = m_probeLoader.load();
            }
            return m_probeDesc;
        }
    }

    @Override
    public EffectorDescription effectorDesc () {
        synchronized (m_initialized) {
            if (m_effectorDesc == null) {
                m_effectorDesc = m_effectorLoader.load ();
            }
            return m_effectorDesc;
        }
    }

    @Override
    public GaugeDescription gaugeDesc () {
        synchronized (m_initialized) {
            if (m_gaugeDesc == null) {
                m_gaugeDesc = m_gaugeLoader.load ();
            }
            return m_gaugeDesc;
        }
    }

//    @Override
//    public UtilityPreferenceDescription preferenceDesc () {
//        if (m_prefDesc == null) {
//            m_prefDesc = YamlUtil.loadUtilityPrefs ();
//        }
//        return m_prefDesc;
//    }

    public void report (String delegateID, ReportType type, RainbowComponentT compT, String msg) {
        String log = MessageFormat.format ("Delegate: {0}[{1}]: {2}", delegateID, compT.name (), msg);
        switch (type) {
        case INFO:
            LOGGER.info (log);
            break;
        case WARNING:
            LOGGER.warn (log);
            break;
        case ERROR:
            LOGGER.error (log);
            break;
        case FATAL:
            LOGGER.fatal (log);
            break;
        default:
            break;
        }
    }


    @SuppressWarnings ("unchecked")
    public <S extends IEvaluable> IAdaptationManager<S> adaptationManagerForModel (String modelRef) {
        for (IAdaptationManager adaptationManager : m_adaptationManagers) {
            if (adaptationManager.getManagedModel().toString().equals(modelRef)) {
                return adaptationManager;
            }
        }
        return null;
    }

//    public EffectorManager effectorManager () {
//        return m_effectorManager;
//    }


    @SuppressWarnings ("unchecked")
    public <S> IAdaptationExecutor<S> strategyExecutor (String modelRef) {
        for (IAdaptationExecutor adaptationExecutor : m_adaptationExecutors) {
            if (adaptationExecutor.getManagedModel().toString().equals(modelRef)) {
                return adaptationExecutor;
            }
        }
        return null;
    }


    @Override
    public RainbowComponentT getComponentType () {
        return RainbowComponentT.MASTER;
    }

    @Override
    public void startProbes () {
        log ("Starting probes");
        for (IDelegateManagementPort delegate : m_delegates.values ()) {
            log("Starting probes from " + delegate.getDelegateId() + " delegate.");
            delegate.startProbes ();
        }
    }

    @Override
    public void killProbes () {
        for (IDelegateManagementPort delegate : m_delegates.values ()) {
            delegate.killProbes ();
        }
    }

    @Override
    public void enableAdaptation (boolean enabled) {
        for (IAdaptationManager<?> am : m_adaptationManagers) {
            log("Enabling adaptation manager " + am.getClass().getCanonicalName());
            am.setEnabled (enabled);
        }
    }

    public boolean isAdaptationEnabled () {
        boolean enabled = true;
        for (IAdaptationManager<?> am : m_adaptationManagers) {
            enabled &= am.isEnabled ();
        }
        return enabled;
    }

    @Override
    public Outcome testEffector (String target, String effName, List<String> args) {
        for (EffectorManager em : m_effectorManagers) {
            Outcome outcome = em.executeEffector (effName, target, args.toArray (new String[0]));
            if (outcome != Outcome.UNKNOWN) return outcome;
        }
        return Outcome.UNKNOWN;
    }

    @Override
    public void sleep () {
        m_rainbowEnvironment.signalTerminate ();
    }

    @Override
    public void terminate (ExitState exitState) {
        m_rainbowEnvironment.signalTerminate (exitState);
    }

    @Override
    public void restartDelegates () {
        for (IDelegateManagementPort delegate : m_delegates.values ()) {
            delegate.startDelegate ();
        }
    }

    @Override
    public void sleepDelegates () {
        for (IDelegateManagementPort delegate : m_delegates.values ()) {
            delegate.pauseDelegate ();
        }
    }

    @Override
    public void destroyDelegates () {
        Set<String> delegatesBeforeClosing = new HashSet<> (m_delegates.keySet ());
        for (String key : delegatesBeforeClosing) {
            Beacon b = new Beacon (10000);
            b.mark ();
            synchronized (m_terminatedDelegates) {
                m_terminatedDelegates.put (key, b);
                m_delegates.get (key).terminateDelegate ();
            }
        }
    }

    @Override
    public void killDelegate (String ipOfDelegate) {
        IDelegateManagementPort port = m_delegates.get (ipOfDelegate);
        String did = ipOfDelegate;
        if (port == null) {
            for (Entry<String, Properties> e : m_delegateInfo.entrySet ()) {
                if (ipOfDelegate.equals (e.getValue ().getProperty (RainbowConstants.PROPKEY_DEPLOYMENT_LOCATION))) {
                    port = m_delegates.get (e.getKey ());
                    did = e.getKey ();
                    break;
                }
            }
            if (port != null) {
                Beacon b = new Beacon (10000);
                b.mark ();
                synchronized (m_terminatedDelegates) {
                    m_terminatedDelegates.put (did, b);
                }
                port.terminateDelegate ();
            }
        }
    }


    @Override
    public List<String> getExpectedDelegateLocations () {
        List<String> ret = new LinkedList<> ();
        Properties allProperties = m_rainbowEnvironment.allProperties ();
        for (Map.Entry<?, ?> o : allProperties.entrySet ()) {
            String k = (String )o.getKey ();
            if (k.startsWith ("customize.system.")) {
                String location = (String )o.getValue ();
                if (!filterEffectorsForLocation (location).isEmpty () || !filterGaugesForLocation (location).isEmpty ()
                        || !filterProbesForLocation (location).isEmpty ()) {
                    ret.add (location);
                }
            }
        }
        Collections.sort (ret);
        return ret;
    }

}
