package org.openmrs.module.basicmodule.authorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import luca.data.AttributeQuery;
import luca.data.DataHandler;
import luca.data.XmlDataHandler;
import luca.tmac.basic.ResponseParser;
import luca.tmac.basic.TmacPEP;
import luca.tmac.basic.data.xml.PermissionAttributeXmlName;
import luca.tmac.basic.obligations.Obligation;
import luca.tmac.basic.obligations.ObligationMonitorable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.module.basicmodule.TmacEnforceService;
import org.openmrs.module.basicmodule.dataFinder.OpenmrsSubjectAttributeFinderModule;
import org.wso2.balana.attr.StringAttribute;


public class TmacEnforceServiceImpl implements TmacEnforceService,ObligationMonitorable {
	
	
	private static final Log LOG = LogFactory.getLog(TmacEnforceServiceImpl.class);
	private static final String RESOURCE_PATH = "data.xml";
	// /Users/anitacao/Documents/workspace/Eclipse/openmrs-core/webapp/api/src/main/resources/data.xml

	//private static final long serialVersionUID = -8561161513239681330L;
	private TmacPEP pep;
	private DataHandler dh;
	//private List<String> obligationList;
	private long responseParserId;
	
	
	public TmacEnforceServiceImpl(Object[] parameters) throws FileNotFoundException {
		String path = this.getClass().getClassLoader().getResource(RESOURCE_PATH).toString().substring(5);

		//BufferedReader path2 = new BufferedReader(new FileReader(RESOURCE_PATH));
		System.err.println("SERIOUSLY CHRIS : " + path);
		dh = new XmlDataHandler(path);
		
		pep = new TmacPEP(dh,this);
		
		pep.addAttributeFinderToPDP(new OpenmrsSubjectAttributeFinderModule((dh),parameters)); //actually new TmacPEP here. 

		
	}

	public boolean isAuthorized(String priviledge, User user) throws APIException {
		
		boolean isAuthorized = false;
		
		// isAuthorized = sendRequest(priviledge, user, context); // context would have info about what is being asked: i.e. which patient record does a doctor want to access?
		isAuthorized = sendRequest(priviledge, user);	
		return isAuthorized;
	}
	
	//create and send request to pdp, privilege is a string which contains the action and resource
    public boolean sendRequest(String privilege, User user){
    	
    	System.out.println( "Anita !!!!!!!!!!!!!!!!!! the required privilege is : " + privilege);
    	
    	int i = 0, j =0;
    	for(;i<privilege.length();i++){
    		if(privilege.charAt(i)==' '){
    		j = i;	
    		break;}
    	}
    	//split the privilege string to actionString and reaourceString
    	String actionString = privilege.substring(0, j).toLowerCase();
    	String resourceString = privilege.substring(j+1).toLowerCase();
    	
    	//set all the attributes
    	ArrayList<AttributeQuery> attributeQuery = new ArrayList<AttributeQuery>();
    	String permission = "";
		List<String> permission_ids = null;
		attributeQuery.clear();
		attributeQuery.add(new AttributeQuery(
				PermissionAttributeXmlName.ACTION, actionString,
				StringAttribute.identifier));
//		attributeQuery.add(new AttributeQuery(
//				PermissionAttributeXmlName.RESOURCE_ID, resourceString,
//				StringAttribute.identifier));
		attributeQuery.add(new AttributeQuery(
				PermissionAttributeXmlName.RESOURCE_TYPE, resourceString,   //if we dont want to give all the privileges about patients to the doctor, we can just change resourceString to "patients".
				StringAttribute.identifier));
		permission_ids = dh.getAttribute(
				PermissionAttributeXmlName.PERMISSION_TABLE,
				attributeQuery, PermissionAttributeXmlName.ID);
		
		if (permission_ids == null || permission_ids.size() == 0) {
			
			System.out.println("Anita ! the persission is empty : " + permission_ids.size());
			
			return false;
		}
		permission = permission_ids.get(0);
		
		//the request type is "obtain_permission"
		String userID = user.getId().toString();
		ResponseParser rParser = pep.requestAccess(userID,
				permission, "", "",
				"obtain_permission");

		responseParserId = rParser.getParserId();
		
		List<Obligation> oblList = rParser.getObligation().getList();
		
		//if the decision is permit, return true
		         
		if(rParser.getDecision().equals(ResponseParser.PERMIT_RESPONSE)){		
			return true;
		}
		else
			return false;

		
		
    }
   
    
    public String acceptResponse(){
    	String message = pep.acceptResponse(responseParserId);
    	return message;
    }
   
	public void notifyDeadline(Obligation obl) {
		// TODO Auto-generated method stub
		
	}

	public void notifyFulfillment(Obligation obl) {
		// TODO Auto-generated method stub
		
	}

	public void notifyObligationInsert(Obligation obl) {
		// TODO Auto-generated method stub
		
	}

	public List<String> getObligations() throws APIException {
		// TODO Auto-generated method stub
		return null;
	}



}
