import com.navis.argo.business.api.GroovyApi
import com.navis.argo.business.atoms.ScopeEnum
import com.navis.argo.business.model.Complex
import com.navis.argo.business.model.Facility
import com.navis.argo.business.model.N4EntityScoper
import com.navis.argo.business.model.Operator
import com.navis.argo.portal.context.ArgoUserContextProvider
import com.navis.framework.business.Roastery
import com.navis.framework.persistence.hibernate.CarinaPersistenceCallback
import com.navis.framework.persistence.hibernate.PersistenceTemplate
import com.navis.framework.portal.UserContext
import com.navis.framework.portal.context.IUserContextProvider
import com.navis.framework.portal.context.PortalApplicationContext
import com.navis.framework.util.TransactionParms
import com.navis.framework.util.message.MessageCollector
import com.navis.framework.util.scope.ScopeCoordinates
import com.navis.road.business.atoms.TruckVisitStatusGroupEnum
import com.navis.road.business.model.Truck
import com.navis.road.business.model.TruckTransaction
import com.navis.road.business.model.TruckVisitDetails
import com.navis.road.business.model.TruckingCompany
import com.navis.road.business.workflow.TransactionAndVisitHolder
import com.navis.security.apiobj.SecuritySessionID
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.jdom.Attribute
import org.jdom.Element

/*
 *
 * @Author <a href="mailto:kpalanisamy@matson.com">Karthikeyan P</a>,12/April/2019
 *
 * Requirements : This groovy is called after completing the stage done xml and send the stage done xml to Now processor, if truck bypass outgate.
 *
 * @Inclusion Location	: Incorporated as a code extension of the type GroovyPlugin
 *
 */

class GateWebservicePostInvokeInTx {

    public void postHandlerInvoke(Map parameter) {
        Element element = (Element) parameter.get("ARGO_WS_ROOT")

        Element stageDoneElement = element.getChild("stage-done") != null ? (Element) element.getChild("stage-done") : null;

        UserContext userContext = (UserContext) parameter.get("ARGO_WS_USER_CONTEXT")
        String userId=userContext.getUserId()
        Serializable userGkey=userContext.getUserGkey()
        SecuritySessionID securitySessionID=userContext.getSecuritySessionId()
        ScopeCoordinates coordinate = userContext.getScopeCoordinate()
        String businessCoords = coordinate.getBusinessCoords()
        String[] topologyArray = businessCoords.split("/")
        String fcyId = topologyArray[2]
        String complexId = topologyArray[1]
        String operatorId = topologyArray[0]

        if (stageDoneElement != null) {

            Element stageIdElement=stageDoneElement.getChild("stage-id")
            String stageIdValue=stageIdElement!=null ? stageIdElement.getValue(): null

            //if(stageIdValue.equals("SecurityOutGate")){
                Element truckElement=stageDoneElement.getChild("truck")
                Attribute idAttribute= truckElement!=null ? truckElement.getAttribute("id"):null
                String IdValue=idAttribute!=null ? idAttribute.getValue():null


                Attribute truckingCoIdAttribute=truckElement!=null ? truckElement.getAttribute("trucking-co-id"): null
                String truckingCoIdValue=idAttribute!=null ? truckingCoIdAttribute.getValue(): null


                Element truckVisitElement = stageDoneElement.getChild("truck-visit")
                Attribute gosTvKeyAttribute = truckVisitElement != null ? truckVisitElement.getAttribute("gos-tv-key") : null
                Long gosTvKeyValue = gosTvKeyAttribute != null ? gosTvKeyAttribute.getValue().toLong() : null

                Truck truck=Truck.findTruckById(IdValue)
                String truckLicenseNbr=truck!=null ? truck.getTruckLicenseNbr(): null
                String batNbr=truck!=null ? truck.getTruckBatNbr() : null
                String tagId=truck!=null ? truck.getTruckAeiTagId(): null

                TruckingCompany truckingCompany=TruckingCompany.findTruckingCompany(truckingCoIdValue)
                Long bzuGkey=truckingCompany!=null ? truckingCompany.bzuGkey : null

                TruckVisitDetails truckVisitDetails=TruckVisitDetails.findTruckVisit(null,null,IdValue,truckingCoIdValue,bzuGkey,null,truckLicenseNbr,batNbr,null,null,null,null,TruckVisitStatusGroupEnum.ACTIVE_COMPLETE,true)
                if(truckVisitDetails!=null){
                    Set truckTransSet=truckVisitDetails.getTvdtlsTruckTrans()
                    Iterator iterator=truckTransSet.iterator()
                    while(iterator.hasNext()){
                        TruckTransaction truckTransaction=(TruckTransaction)iterator.next()
                        TransactionAndVisitHolder inDao = new TransactionAndVisitHolder(truckVisitDetails, truckTransaction);



                        Complex complex =Complex.findComplex(complexId, Operator.findOperator(operatorId))
                        Facility facility = Facility.findFacility(fcyId, Complex.findComplex(complexId, Operator.findOperator(operatorId)))
                        N4EntityScoper entityScoper = (N4EntityScoper) Roastery.getBeanFactory().getBean(N4EntityScoper.BEAN_ID);
                        ScopeCoordinates scopeCoordinates = entityScoper.getScopeCoordinates(ScopeEnum.YARD, facility.getActiveYard().getYrdGkey());
                        ArgoUserContextProvider userContextProvider = (ArgoUserContextProvider) PortalApplicationContext.getBean(IUserContextProvider.BEAN_ID);
                        UserContext dutUserContext = userContextProvider.createUserContext(userGkey,userId, scopeCoordinates);
                        dutUserContext.setSecuritySessionId(securitySessionID);

                        PersistenceTemplate template = new PersistenceTemplate(dutUserContext);
                        MessageCollector mc = template.invoke(new CarinaPersistenceCallback() {
                            protected void doInTransaction() {
                                TransactionParms.getBoundParms().setUserContext(dutUserContext);
                                GroovyApi api=new GroovyApi()
                                if(inDao!=null && truckVisitDetails.getTvdtlsExitLane()==null){
                                    api.getGroovyClassInstance("NOWOutgateProcessor").execute(inDao, api, false);
                                }
                            }
                        });
                    }
                }

           // }


        }
    }

    private final static Logger LOGGER = Logger.getLogger(GateWebservicePostInvokeInTx.class)
}