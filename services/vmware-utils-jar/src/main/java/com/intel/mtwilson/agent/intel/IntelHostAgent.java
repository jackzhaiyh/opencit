/*
 * Copyright (C) 2012 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.agent.intel;

import com.intel.mountwilson.as.helper.TrustAgentSecureClient;
import com.intel.mountwilson.manifest.data.IManifest;
import com.intel.mountwilson.manifest.data.PcrManifest;
import com.intel.mountwilson.manifest.helper.TAHelper;
import com.intel.mountwilson.ta.data.hostinfo.HostInfo;
import com.intel.mtwilson.agent.HostAgent;
import com.intel.mtwilson.agent.vmware.VCenterHost;
import com.intel.mtwilson.crypto.X509Util;
import com.intel.mtwilson.datatypes.Aik;
import com.intel.mtwilson.datatypes.InternetAddress;
import com.intel.mtwilson.datatypes.Nonce;
import com.intel.mtwilson.datatypes.Pcr;
import com.intel.mtwilson.datatypes.PcrIndex;
import com.intel.mtwilson.datatypes.TpmQuote;
import com.intel.mtwilson.datatypes.TxtHostRecord;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of VmwareAgent should be created by the VmwareAgentFactory
 * @author jbuhacoff
 */
public class IntelHostAgent implements HostAgent {
    private transient Logger log = LoggerFactory.getLogger(getClass());
    private transient final TrustAgentSecureClient trustAgentClient;
//    private final String hostname;
    private InternetAddress hostAddress;
    private Boolean isTpmAvailable = null;
    private String vendorHostReport = null;
    private String vmmName = null;
    private HashMap<String, ? extends IManifest> manifestMap = null; // XXX TODO needs to change, it's not a clear programming interface
    
    public IntelHostAgent(TrustAgentSecureClient client, InternetAddress hostAddress) throws Exception {
        trustAgentClient = client;
        this.hostAddress = hostAddress;
//        this.hostname = hostname;
    }
    
    
    
    @Override
    public boolean isTpmAvailable() {
//        throw new UnsupportedOperationException("Not supported yet.");
        // bug #538  for now assuming all trust-agent hosts have tpm since we don't have a separate capabilities call
        return true; //  XXX TODO need to have a separate call to trust agent to get host capabilities  ... see bug #540
    }

    @Override
    public boolean isTpmEnabled() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isEkAvailable() {
        return false; // vmware does not make the EK available through its API
    }

    @Override
    public boolean isAikAvailable() { // XXX TODO need to distinguish between "the host system could have an AIK" (maybe isAikSupported) and "the host system actually does have an AIK" (isAikAvailable)
        return true;  // assume we can always get an AIK from a trust agent,  for now
    }

    @Override
    public boolean isAikCaAvailable() { // XXX TODO probably needs to be separated like isAik*  into isAikCaSupported and isAikCaAvailable .    AikCa is synonym for PrivacyCa.
        return true; // assume hosts running trust agent always use a privacy ca,  for now
    }

    @Override
    public boolean isDaaAvailable() {
        return false; // intel trust agent currently does not support DAA
    }

    @Override
    public X509Certificate getAikCertificate() {
        String pem = trustAgentClient.getAIKCertificate();
        try {
            X509Certificate aikCert = X509Util.decodePemCertificate(pem);
            isTpmAvailable = true;
            return aikCert;
        }
        catch(Exception e) {
            log.error("Cannot decode AIK certificate: {}", e.toString());
            log.debug(pem);
            return null;
        }
    }

    @Override
    public X509Certificate getAikCaCertificate() {
        throw new UnsupportedOperationException("Not supported yet.");
//        return null; // XXX TODO throw exception or return null? call should first check isAikCaAvailable  // vmware does not make the Privacy CA Certificate available through its API, if it even uses a Privacy CA
    }

    @Override
    public String getHostInformation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getVendorHostReport()  throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public TpmQuote getTpmQuote(Aik aik, Nonce nonce, Set<PcrIndex> pcr) {
        throw new UnsupportedOperationException("Vmware does not provide TPM Quotes"); // XXX TODO throw exception or return null?
    }

    @Override
    public List<Pcr> getPcrValues() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getModuleManifest() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Pcr> getPcrHistory(PcrIndex number) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public HashMap<String, ? extends IManifest> getManifest(VCenterHost postProcessing) { // XXX TODO VCenterHost must not be part of the api;  this was a kludge specific to the VmwareHostAgent
        // XXX TODO  obtain the manifest map  using existing code in one of the trust agent helper classes
        return manifestMap;
    }

    @Override
    public TxtHostRecord getHostDetails() throws IOException {
        HostInfo hostInfo = trustAgentClient.getHostInfo();
        TxtHostRecord host = new TxtHostRecord();
        host.BIOS_Oem = hostInfo.getBiosOem().trim();
        host.BIOS_Version = hostInfo.getBiosVersion().trim();
        host.VMM_Name = hostInfo.getVmmName().trim();
        host.VMM_Version = hostInfo.getVmmVersion().trim();
        host.VMM_OSName = hostInfo.getOsName().trim();
        host.VMM_OSVersion = hostInfo.getOsVersion().trim();
        // now set some state we need for getHostAttestationReport
        vmmName = host.VMM_Name; // XXX maybe we should maintain the entire TxtHostRecord or something similar
        return host;
    }

    @Override
    public String getHostAttestationReport(String pcrList) throws IOException {
        if( vendorHostReport != null ) { return vendorHostReport; }
        if( vmmName == null ) { getHostDetails(); }
//        throw new UnsupportedOperationException("Not supported yet.");
        // XXX TODO huge kludge, we are relying on the OpenSourceVMMHelper for this, which uses API Client to call Attestation Service, which then creates a TrustAgentSecureClient to get some information from the host but also grabs some from the database and then generates the XML format we need
//        OpenSourceVMMHelper helper = new OpenSourceVMMHelper();
//        return help.getHostAttestationReport(hostAddress);
        try {
            TAHelper helper = new TAHelper();
            HashMap<String, PcrManifest> pcrMap = helper.getQuoteInformationForHost(hostAddress.toString(), trustAgentClient, pcrList);
            vendorHostReport = helper.getHostAttestationReport(hostAddress.toString(), pcrMap, vmmName);
            log.debug("Host attestation report for {}", hostAddress);
            log.debug(vendorHostReport);
            return vendorHostReport;
        }
        catch(Exception e) {
            throw new IOException(e);
        }
    }
    
}
