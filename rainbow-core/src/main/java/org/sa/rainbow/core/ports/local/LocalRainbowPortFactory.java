package org.sa.rainbow.core.ports.local;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNull;
import org.sa.rainbow.core.Identifiable;
import org.sa.rainbow.core.RainbowDelegate;
import org.sa.rainbow.core.RainbowMaster;
import org.sa.rainbow.core.error.RainbowConnectionException;
import org.sa.rainbow.core.gauges.IGauge;
import org.sa.rainbow.core.gauges.IGaugeConfigurationInterface;
import org.sa.rainbow.core.gauges.IGaugeIdentifier;
import org.sa.rainbow.core.gauges.IGaugeQueryInterface;
import org.sa.rainbow.core.gauges.IRainbowGaugeLifecycleBusPort;
import org.sa.rainbow.core.models.IModelsManager;
import org.sa.rainbow.core.ports.IProbeConfigurationPort;
import org.sa.rainbow.core.ports.IProbeReportPort;
import org.sa.rainbow.core.ports.IRainbowConnectionPortFactory;
import org.sa.rainbow.core.ports.IRainbowDelegateConfigurationPort;
import org.sa.rainbow.core.ports.IRainbowManagementPort;
import org.sa.rainbow.core.ports.IRainbowMasterConnectionPort;
import org.sa.rainbow.core.ports.IRainbowModelChangeBusPort;
import org.sa.rainbow.core.ports.IRainbowModelUSBusPort;
import org.sa.rainbow.translator.probes.IProbe;

public class LocalRainbowPortFactory implements IRainbowConnectionPortFactory {

    /**
     * Singleton instance
     */
    private static IRainbowConnectionPortFactory       m_instance;
    Map<String, LocalMasterSideManagementPort>         m_masterPorts             = new HashMap<> ();
    LocalMasterConnectionPort                          m_masterConnectionPort;
    Map<String, LocalDelegateManagementPort>           m_delegatePorts           = new HashMap<> ();
    Map<String, LocalDelegateConnectionPort>           m_delegateConnectionPorts = new HashMap<> ();
    private LocalModelsManagerUSPort                   m_localModelsManagerUSPort;
    private Map<String, LocalModelsManagerClientUSPort> m_mmClientUSPorts         = new HashMap<> ();

    private LocalRainbowPortFactory () {
    };

    @Override
    @NonNull
    public IRainbowManagementPort createMasterSideManagementPort (RainbowMaster rainbowMaster,
            String delegateID,
            Properties connectionProperties) {

        LocalMasterSideManagementPort mdp = m_masterPorts.get (delegateID);
        if (mdp == null) {
            mdp = new LocalMasterSideManagementPort (rainbowMaster, delegateID);
            m_masterPorts.put (delegateID, mdp);
            connectMasterAndDelegate (delegateID);
        }
        return mdp;
    }

    @Override
    @NonNull
    public IRainbowManagementPort createDelegateSideManagementPort (RainbowDelegate delegate, String delegateID) {
        LocalDelegateManagementPort ddp = m_delegatePorts.get (delegateID);
        if (ddp == null) {
            ddp = new LocalDelegateManagementPort (delegate, delegateID);
            m_delegatePorts.put (delegateID, ddp);
            connectMasterAndDelegate (delegateID);
        }
        return ddp;
    }

    private void connectMasterAndDelegate (String delegateID) {
        LocalMasterSideManagementPort mdp = m_masterPorts.get (delegateID);
        LocalDelegateManagementPort ddp = m_delegatePorts.get (delegateID);
        if (mdp != null && ddp != null) {
            mdp.connect (ddp);
            ddp.connect (mdp);
        }
    }

    @Override
    @NonNull
    public IRainbowMasterConnectionPort createMasterSideConnectionPort (final RainbowMaster rainbowMaster) {
        if (m_masterConnectionPort == null) {
            m_masterConnectionPort = new LocalMasterConnectionPort (rainbowMaster);
        }
        return m_masterConnectionPort;
    }

    @Override
    @NonNull
    public IRainbowMasterConnectionPort createDelegateSideConnectionPort (RainbowDelegate delegate) {
        LocalDelegateConnectionPort ldcp = m_delegateConnectionPorts.get (delegate.getId ());
        if (ldcp == null) {
            ldcp = new LocalDelegateConnectionPort (delegate, this);
            ldcp.connect (m_masterConnectionPort);
            m_delegateConnectionPorts.put (delegate.getId (), ldcp);
        }
        return ldcp;
    }

    public static IRainbowConnectionPortFactory getFactory () {
        if (m_instance == null) {
            m_instance = new LocalRainbowPortFactory ();
        }
        return m_instance;
    }

    @Override
    public IRainbowModelUSBusPort createModelsManagerUSPort (IModelsManager m) throws RainbowConnectionException {
        if (m_localModelsManagerUSPort == null) {
            m_localModelsManagerUSPort = new LocalModelsManagerUSPort (m);
            for (LocalModelsManagerClientUSPort p : m_mmClientUSPorts.values ()) {
                p.connect (m_localModelsManagerUSPort);
            }
        }
        return m_localModelsManagerUSPort;
    }

    @Override
    public IRainbowModelUSBusPort createModelsManagerClientUSPort (Identifiable client)
            throws RainbowConnectionException {
        LocalModelsManagerClientUSPort port = m_mmClientUSPorts.get (client.id ());
        if (port == null) {
            port = new LocalModelsManagerClientUSPort (client);
            port.connect (m_localModelsManagerUSPort);
            m_mmClientUSPorts.put (client.id (), port);
        }
        return port;
    }

    @Override
    public IRainbowGaugeLifecycleBusPort createGaugeSideLifecyclePort () throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");
    }

    @Override
    public IRainbowModelChangeBusPort createChangeBusAnnouncePort () throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");

    }

    @Override
    public IRainbowGaugeLifecycleBusPort createManagerGaugeLifecyclePort (IRainbowGaugeLifecycleBusPort manager) {
        throw new UnsupportedOperationException ("NYS");

    }

    @Override
    public IGaugeConfigurationInterface createGaugeConfigurationPortClient (IGaugeIdentifier gauge)
            throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");
    }

    @Override
    public IGaugeQueryInterface createGaugeQueryPortClient (IGaugeIdentifier gauge)
            throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");

    }

    @Override
    public IGaugeConfigurationInterface createGaugeConfigurationPort (IGauge gauge) throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");
    }

    @Override
    public IGaugeQueryInterface createGaugeQueryPort (IGauge gauge) throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");

    }

    @Override
    public IProbeReportPort createProbeReportingPortSender (IProbe probe) throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");

    }

    @Override
    public IProbeConfigurationPort createProbeConfigurationPort (Identifiable probe, IProbeConfigurationPort callback)
            throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");

    }

    @Override
    public IRainbowDelegateConfigurationPort createDelegateConfigurationPort (RainbowDelegate rainbowDelegate)
            throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");

    }

    @Override
    public IRainbowDelegateConfigurationPort createDelegateConfigurationPortClient (String delegateID)
            throws RainbowConnectionException {
        throw new UnsupportedOperationException ("NYS");

    }

}