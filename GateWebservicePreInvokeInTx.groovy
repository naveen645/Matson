import com.navis.framework.persistence.HibernateApi
import com.navis.road.business.atoms.TruckVisitStatusEnum
import com.navis.road.business.model.TruckVisitDetails
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.jdom.Attribute
import org.jdom.Element

/*
 *
 * @Author <a href="mailto:kpalanisamy@matson.com">Karthikeyan P</a>, 12/April/2019
 *
 * Requirements : This groovy is used to find a open TruckVisit With different GosTvKey if exists and closes by createting a new truckVisit.
 *
 * and this groovy handles only at SecurityGate.
 *
 * @Inclusion Location	: Incorporated as a code extension of the type GroovyPlugin
 *
 *
 */

class GateWebservicePreInvokeInTx {
    public void preHandlerInvoke(Map parameter) {
        LOGGER.setLevel(Level.DEBUG)
        LOGGER.info("GateWebservicePreInvokeInTx started execution!!!!!!!!")

        Element element = (Element) parameter.get("ARGO_WS_ROOT")
        Element createTruckVisitElement = element.getChild("create-truck-visit") != null ? (Element) element.getChild("create-truck-visit") : null;
        if (createTruckVisitElement != null) {
            Element gateIdElement = createTruckVisitElement.getChild("gate-id");
            String gateId = gateIdElement != null ? gateIdElement.getValue() : null;

            Element stageIdElement = createTruckVisitElement.getChild("stage-id")
            String stageIdValue = stageIdElement != null ?  stageIdElement.getValue() : null;

            if("SecurityGate".equalsIgnoreCase(stageIdValue) || "Ingate".equalsIgnoreCase(stageIdValue)){

                Element truckElement = createTruckVisitElement.getChild("truck");
                Attribute licenseNbrAttribute = truckElement != null ? truckElement.getAttribute("license-nbr") : null;
                String licenseNbrValue = licenseNbrAttribute != null ? licenseNbrAttribute.getValue() : null;

                Element truckVisitElement = createTruckVisitElement.getChild("truck-visit");
                Attribute truckVisitAttribute = truckVisitElement != null ? truckVisitElement.getAttribute("gos-tv-key") : null;
                String gosTvKeyValue = truckVisitAttribute != null ? truckVisitAttribute.getValue() : null;

                if (licenseNbrValue != null) {
                    List<TruckVisitDetails> truckVisitDetailsList = TruckVisitDetails.findTVActiveByTruckLicenseNbr(licenseNbrValue);
                    if (truckVisitDetailsList != null && !truckVisitDetailsList.isEmpty()) {
                        Iterator iterator = truckVisitDetailsList.iterator()
                        while (iterator.hasNext()) {
                            TruckVisitDetails truckVisitDetails = (TruckVisitDetails) iterator.next()
                           if (truckVisitDetails != null) {
                                String tvdtlsGosTvKey = truckVisitDetails.getTvdtlsGosTvKey() != null ? truckVisitDetails.getTvdtlsGosTvKey().toString() : null;
                                String stageId = truckVisitDetails.getTvdtlsNextStageId()
                               if ("SecurityGate".equalsIgnoreCase(stageIdValue) && "SecurityGate".equalsIgnoreCase(stageId)) {
                                    if (gosTvKeyValue != null && !gosTvKeyValue.equals(tvdtlsGosTvKey)) {
                                        truckVisitDetails.setTvdtlsStatus(TruckVisitStatusEnum.CLOSED)
                                        HibernateApi.getInstance().save(truckVisitDetails)
                                        HibernateApi.getInstance().flush()

                                    }
                               }
                            }
                        }
                    }
                }
            }

        }
    }
    private final static Logger LOGGER = Logger.getLogger(GateWebservicePreInvokeInTx.class)
}
