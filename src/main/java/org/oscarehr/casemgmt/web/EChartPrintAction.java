package org.oscarehr.casemgmt.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.actions.DispatchAction;
import org.oscarehr.PMmodule.dao.ClientDao;
import org.oscarehr.casemgmt.dao.CaseManagementNoteDAO;
import org.oscarehr.casemgmt.model.CaseManagementIssue;
import org.oscarehr.casemgmt.model.CaseManagementNote;
import org.oscarehr.casemgmt.print.OscarChartPrinter;
import org.oscarehr.common.dao.AllergyDAO;
import org.oscarehr.common.model.Allergy;
import org.oscarehr.common.model.Demographic;
import org.oscarehr.util.SpringUtils;

import com.lowagie.text.DocumentException;

public class EChartPrintAction extends DispatchAction {

	CaseManagementNoteDAO caseManagementNoteDao = (CaseManagementNoteDAO)SpringUtils.getBean("CaseManagementNoteDAO");
	AllergyDAO allergyDao = (AllergyDAO)SpringUtils.getBean("AllergyDAO");
	static String[] cppIssues = {"MedHistory","OMeds","SocHistory","FamHistory","Reminders","Concerns","RiskFactors"};

	
	public ActionForward unspecified(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		return this.print(mapping, form, request, response);
	}
	
	public ActionForward print(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		String demographicNo = request.getParameter("demographicNo");
		ClientDao clientDao = (ClientDao)SpringUtils.getBean("clientDao");
		Demographic demographic = clientDao.getClientByDemographicNo(Integer.parseInt(demographicNo));
		
		response.setContentType("application/pdf"); // octet-stream			
		response.setHeader("Content-Disposition", "attachment; filename=\""+demographicNo+".pdf\"");			
			
		OscarChartPrinter printer = new OscarChartPrinter(request,response.getOutputStream());
		printer.setDemographic(demographic);
		printer.setNewPage(true);
		printer.printDocHeaderFooter();
		
		printer.printMasterRecord();
		printer.setNewPage(true);
		printer.printAppointmentHistory();
		printer.setNewPage(true);
		
		printCppItem(printer,"Social History","SocHistory",demographic.getDemographicNo());
		printCppItem(printer,"Medical History","MedHistory",demographic.getDemographicNo());
		printCppItem(printer,"Ongoing Concerns","Concerns",demographic.getDemographicNo());
		printCppItem(printer,"Reminders","Reminders",demographic.getDemographicNo());		
		printCppItem(printer,"Family History","FamHistory",demographic.getDemographicNo());		
		printCppItem(printer,"Risk Factors","RiskFactors",demographic.getDemographicNo());
		printCppItem(printer,"Other Medications","OMeds",demographic.getDemographicNo());				
		printer.setNewPage(true);

		@SuppressWarnings("unchecked")
		List<Allergy> allergies = allergyDao.getAllergies(String.valueOf(demographic.getDemographicNo()));
		if(allergies.size()>0) {
			printer.printAllergies(allergies);
		}
		printer.printRx(String.valueOf(demographic.getDemographicNo()));

		printer.printPreventions();
		printer.printTicklers();
		printer.printDiseaseRegistry();
		
		printer.printCurrentAdmissions();
		printer.printPastAdmissions();
		
		printer.printCurrentIssues();
		
		
		List<CaseManagementNote> notes = this.caseManagementNoteDao.getMostRecentNotes(demographic.getDemographicNo());
		notes = filterOutCpp(notes);
		if(notes.size()>0)
			printer.printNotes(notes, true);
		
		printer.finish();
		
		return null;
	}
	
	   public List<CaseManagementNote> filterOutCpp(Collection<CaseManagementNote> notes) {
		   List<CaseManagementNote> filteredNotes = new ArrayList<CaseManagementNote>();
		   for(CaseManagementNote note:notes) {
			   boolean skip=false;
			 for(CaseManagementIssue issue:note.getIssues()) {
				 for(int x=0;x<cppIssues.length;x++) {
					 if(issue.getIssue().getCode().equals(cppIssues[x])) {
						 skip=true;
					 }
				 }
			 }
			 if(!skip) {
				 filteredNotes.add(note);
			 }
		   }
		   return filteredNotes;
	   }
	
	public void printCppItem(OscarChartPrinter printer, String header, String issueCode, int demographicNo) throws DocumentException,IOException {
		   Collection<CaseManagementNote> notes = null;
		   notes = caseManagementNoteDao.findNotesByDemographicAndIssueCode(demographicNo, new String[] {issueCode});			   
		   
		   if(notes.size()>0) {
			   printer.printCPPItem(header, notes);
			   printer.printBlankLine();
		   }
	   }

}