/*
 * HL7Handler
 * Upload handler
 * 
 */
package oscar.oscarLab.ca.all.upload.handlers;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.oscarehr.common.dao.Hl7TextInfoDao;
import org.oscarehr.common.model.Hl7TextInfo;
import org.oscarehr.util.DbConnectionFilter;
import org.oscarehr.util.LoggedInInfo;
import org.oscarehr.util.SpringUtils;

import oscar.oscarLab.ca.all.parsers.Factory;
import oscar.oscarLab.ca.all.upload.MessageUploader;
import oscar.oscarLab.ca.all.upload.ProviderLabRouting;
import oscar.oscarLab.ca.all.upload.RouteReportResults;
import oscar.oscarLab.ca.all.util.Utilities;

/**
 * 
 */
public class OLISHL7Handler implements MessageHandler {

	Logger logger = Logger.getLogger(OLISHL7Handler.class);
	Hl7TextInfoDao hl7TextInfoDao = (Hl7TextInfoDao)SpringUtils.getBean("hl7TextInfoDao");
	
	private int lastSegmentId = 0;
	
	public OLISHL7Handler() {
		logger.info("NEW OLISHL7Handler UPLOAD HANDLER instance just instantiated. ");
	}

	public String parse(String serviceName, String fileName, int fileId) {
		return parse(serviceName,fileName,fileId, false);
	}
	public String parse(String serviceName, String fileName, int fileId, boolean routeToCurrentProvider) {		
		int i = 0;
		RouteReportResults results = new RouteReportResults();
		
		String provNo =  LoggedInInfo.loggedInInfo.get().loggedInProvider.getProviderNo();
		try {
			ArrayList<String> messages = Utilities.separateMessages(fileName);
			for (i = 0; i < messages.size(); i++) {
				String msg = messages.get(i);
				if(isDuplicate(msg)) {
					return "success";
				}
				MessageUploader.routeReport(serviceName,"OLIS_HL7", msg.replace("\\E\\", "\\SLASHHACK\\").replace("µ", "\\MUHACK\\").replace("\\H\\", "\\.H\\").replace("\\N\\", "\\.N\\"), fileId, results);
				if (routeToCurrentProvider) {
					ProviderLabRouting routing = new ProviderLabRouting();
					routing.route(results.segmentId, provNo, DbConnectionFilter.getThreadLocalDbConnection(), "HL7");
					this.lastSegmentId = results.segmentId;
				}
			}
			logger.info("Parsed OK");
		} catch (Exception e) {
			MessageUploader.clean(fileId);
			logger.error("Could not upload message", e);
			return null;
		}
		return ("success");
	}
	
	public int getLastSegmentId() {
		return this.lastSegmentId;
	}
	//TODO: check HIN
	//TODO: check # of results
	
	private boolean isDuplicate(String msg) {
		
		
		//OLIS requirements - need to see if this is a duplicate
		oscar.oscarLab.ca.all.parsers.MessageHandler h = Factory.getHandler("OLIS_HL7", msg);
		//if final
//		if(h.getOrderStatus().equals("F")) {
			String acc = h.getAccessionNum();
			
			//CML
			if(acc.length()>5 && acc.charAt(acc.length()-5) == '-') {
				List<Hl7TextInfo> dupResults = hl7TextInfoDao.searchByAccessionNumber(acc.split("-")[0]);
				for(Hl7TextInfo dupResult:dupResults) {
					if(dupResult.getAccessionNumber().indexOf("-")!=-1) {
						//olis
						//if(dupResult.getAccessionNumber().equals(acc)) {
						//	return true;
						//}
					} else {
						//direct
						if(dupResult.getAccessionNumber().substring(3).equals(acc.split("-")[0])) {
							return true;
						}
					}
				}		
			}
			//GDML
			if(acc.length() == 14 && acc.indexOf("-") == -1) {
				List<Hl7TextInfo> dupResults = hl7TextInfoDao.searchByAccessionNumber(acc.substring(6));
				String directAcc = acc.substring(4);
				directAcc = directAcc.substring(0,2) + "-" + directAcc.substring(2);
				for(Hl7TextInfo dupResult:dupResults) {
					//if(dupResult.getAccessionNumber().equals(directAcc)) {
					//	return true;
					//}
					if(dupResult.getAccessionNumber().equals(acc)) {
						return true;
					}
				}		
			}
			//LL
			if(acc.length() > 5 && acc.charAt(4) == '-') {
				List<Hl7TextInfo> dupResults = hl7TextInfoDao.searchByAccessionNumber(acc.substring(6));
				for(Hl7TextInfo dupResult:dupResults) {
					//if(dupResult.getAccessionNumber().equals(acc)) {
					//	return true;
					//}
					if(dupResult.getAccessionNumber().equals(acc.substring(5))) {
						return true;
					}
				}		
			}
	//	}
		return false;	
	}
}