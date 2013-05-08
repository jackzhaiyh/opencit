
package com.intel.mtwilson.as.rest;

import com.intel.mountwilson.as.common.ValidationException;
import com.intel.mountwilson.as.hostmanifestreport.data.HostManifestReportType;
import com.intel.mountwilson.as.hosttrustreport.data.HostsTrustReportType;
import com.intel.mtwilson.as.business.ReportsBO;
import com.intel.mtwilson.as.helper.ASComponentFactory;
import com.intel.mtwilson.datatypes.AttestationReport;
import com.intel.mtwilson.model.*;
import com.intel.mtwilson.security.annotations.*;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * REST Web Service
 * * 
 */

@Stateless
@Path("/hosts/reports")
public class Reports {
    
    /**
     * Sample request:
     * GET http://localhost:8080/AttestationService/resources/hosts/reports/trust?hostNames=HostName1,HostName2,HostName3
     * 
     * Sample output:
<hosts_trust_report>
<Host Host_Name="10.1.71.103" MLE_Info="BIOS:EPSD-55,VMM:RHEL 6.1-Xen:4.1.1" Created_On="2012-01-24T03:25:27.000-08:00" Trust_Status="0" Verified_On="2012-02-13T16:39:31.000-08:00"/>
<Host Host_Name="10.1.71.103" MLE_Info="BIOS:EPSD-55,VMM:RHEL 6.1-Xen:4.1.1" Created_On="2012-01-24T03:25:27.000-08:00" Trust_Status="0" Verified_On="2012-02-13T16:32:31.000-08:00"/>
<Host Host_Name="10.1.71.103" MLE_Info="BIOS:EPSD-55,VMM:RHEL 6.1-Xen:4.1.1" Created_On="2012-01-24T03:25:27.000-08:00" Trust_Status="0" Verified_On="2012-02-13T12:21:37.000-08:00"/>
<Host Host_Name="10.1.71.103" MLE_Info="BIOS:EPSD-55,VMM:RHEL 6.1-Xen:4.1.1" Created_On="2012-01-24T03:25:27.000-08:00" Trust_Status="0" Verified_On="2012-02-10T17:10:32.000-08:00"/>
<Host Host_Name="10.1.71.103" MLE_Info="BIOS:EPSD-55,VMM:RHEL 6.1-Xen:4.1.1" Created_On="2012-01-24T03:25:27.000-08:00" Trust_Status="0" Verified_On="2012-02-10T16:01:45.000-08:00"/>
</hosts_trust_report> 
     * 
     * When there is an error, the service returns JSON like this:
     * {"error_message":"Host not found - Host 10.1.71.103cv not found.","error_code":2000}
     * 
     * @param hostNamesCSV
     * @return an XML document with the trust status of the specified hosts
     */
    @RolesAllowed({"Attestation","Report"})
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @Path("/trust")
    public JAXBElement<HostsTrustReportType> getTrustReport(@QueryParam("hostNames")String hostNamesCSV) { 
        if( hostNamesCSV == null || hostNamesCSV.isEmpty() ) { throw new ValidationException("Missing hostNames parameter"); }
        return new JAXBElement<HostsTrustReportType>(new QName("hosts_trust_report"),HostsTrustReportType.class, reportsBO.getTrustReport(hostnameListFromCSV(hostNamesCSV))); // datatype.Hostname            
    }

    
    /**
     * Sample request:
     * GET http://localhost:8080/AttestationService/resources/hosts/reports/manifest?hostName=HostName1
     * 
     * Sample output:
<host_manifest_report>
<Host Name="10.1.71.103">
<Manifest TrustStatus="0" Name="0" Value="e3a29bd603bf9982113b696cd37af8afc58e2877" Verified_On="2012-02-13T16:39:31.753-08:00"/>
<Manifest TrustStatus="0" Name="19" Value="cdd56ce92ce515414e72d8203a30b0107717cf27" Verified_On="2012-02-13T16:39:31.753-08:00"/>
<Manifest TrustStatus="0" Name="17" Value="014936fb8e273d53823636235b1626ab25f1c514" Verified_On="2012-02-13T16:39:31.753-08:00"/>
<Manifest TrustStatus="0" Name="18" Value="9c65082230f792824eba1c43e3c0fa6255186577" Verified_On="2012-02-13T16:39:31.753-08:00"/>
</Host>
</host_manifest_report>
     * 
     * When there is an error, the service returns JSON like this:
     * {"error_message":"Host not found - Host 10.1.71.103cv not found.","error_code":2000}
     * 
     * @param hostName
     * @return an XML document with the PCR manifest and trust status of each PCR
     */
    @RolesAllowed({"Attestation","Report"})
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @Path("/manifest")
    public JAXBElement<HostManifestReportType> getManifestReport(@QueryParam("hostName")String hostName) {
        return new JAXBElement<HostManifestReportType>(new QName("host_manifest_report"), HostManifestReportType.class,reportsBO.getReportManifest(new Hostname(hostName))); // datatype.Hostname        
    }

    @RolesAllowed({"Attestation","Report","Security"})
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    @Path("/attestation")
    public String getHostAttestationReport(@QueryParam("hostName")String hostName) {
        return reportsBO.getHostAttestationReport(new Hostname(hostName));   
    }
    
    private List<Hostname> hostnameListFromCSV(String hostnameCSV) {
        ArrayList<Hostname> list = new ArrayList<Hostname>();
        String stringArray[] = hostnameCSV.split(",");
        for(String hostname : stringArray) {
            list.add(new Hostname(hostname));
        }
        return list;
    }
    
       /**
     * Sample request:
     * GET http://localhost:8080/AttestationService/resources/hosts/reports/manifest?hostName=HostName1
     * 
      * When there is an error, the service returns JSON like this:
     * {"error_message":"Host not found - Host 10.1.71.103cv not found.","error_code":2000}
     * 
     * @param hostName
     * @return an XML document with the PCR manifest and trust status of each PCR
     */
    @RolesAllowed({"Attestation","Report"})
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/attestationreport")
    public AttestationReport getAttestationReport(@QueryParam("hostName")String hostName,
            @QueryParam("failure_only") @DefaultValue("false") Boolean failureOnly) {
        return reportsBO.getAttestationReport(new Hostname(hostName),failureOnly); // datatype.Hostname        
    }
    
    
    ReportsBO reportsBO = new ASComponentFactory().getReportsBO();
}
