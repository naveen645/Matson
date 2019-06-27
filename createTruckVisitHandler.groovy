package Matson

import com.navis.argo.business.model.CarrierVisit
import com.navis.framework.persistence.HibernateApi
import com.navis.road.RoadField
import com.navis.road.business.atoms.TranStatusEnum
import com.navis.road.business.atoms.TruckVisitStatusEnum
import com.navis.road.business.model.Truck
import com.navis.road.business.model.TruckTransaction
import com.navis.road.business.model.TruckVisitDetails
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.jdom.Element

class PreProcessCreateTruckVisitHandlerCustomGroovyImpl {

    public void execute(Map parameters) {
        LOGGER.setLevel(Level.DEBUG)
        LOGGER.debug("temp::Inside PreProcessCreateTruckVisitHandlerCustomGroovyImpl :: execute(Map parameters)")
        LOGGER.info("parameters" + parameters)

        String tvdtlsTruckLicenseNbr
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String strKey = entry.getKey()
            if (strKey.equalsIgnoreCase("tvdtlsTruckLicenseNbr")) {
                tvdtlsTruckLicenseNbr = entry.getValue()
                List<TruckVisitDetails> visitDetailsList=TruckVisitDetails.findTVActiveByTruckLicenseNbr(tvdtlsTruckLicenseNbr)
                Iterator iterator=visitDetailsList.iterator()
                while(iterator.hasNext()){
                    TruckVisitDetails truckVisitDetails=(TruckVisitDetails)iterator.next()
                    TruckVisitStatusEnum statusEnum= truckVisitDetails.getTvdtlsStatus()
                    if(statusEnum.equals(TruckVisitStatusEnum.OK) ||statusEnum.equals(TruckVisitStatusEnum.TROUBLE)){
                        truckVisitDetails.setTvdtlsStatus(TruckVisitStatusEnum.CLOSED)
                        HibernateApi.getInstance().save(truckVisitDetails)
                        HibernateApi.getInstance().flush()
                    }
                }

            }

        }
    }
    private final static Logger LOGGER = Logger.getLogger(this.class)
}
