/*******************************************************************************
 * Copyright (c) 2018,  and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    N. Giger - initial implementation
 *******************************************************************************/

package ch.elexis.core.data.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.constants.StringConstants;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.Person;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

public class KontaktUtil {
	private static Logger log = LoggerFactory.getLogger(KontaktUtil.class);

	@SuppressWarnings("static-access")
	public static StringBuffer getContactInfo(Kontakt k) {
		StringBuffer SelectedContactInfosText = new StringBuffer();
		log.debug("copySelectedContactInfosToClipboardAction for {} {}", k.getId(), k.getLabel());
		
		//The following code is adopted from Kontakt.createStdAnschrift for a different purpose/layout:
		//ggf. hier zu Person.getPersonalia() eine abgewandelte Fassung hinzufügen und von hier aus aufrufen.
		
		//This highly similar (but still different) code has been adopted from my addition
		//to PatientenListeView.java CopySelectedPatInfosToClipboard... 
	
		if (k.istPerson()) {
			// Here, we need to look at the Person variant of a Kontakt to obtain their sexy
			// Kontakt cannot simply be cast to Person - if we try, we'll throw an error, and the remainder of this action will be ignored.
			// Person p = (Person) sel[i]; //THIS WILL NOT WORK.
			// So obtain the corresponding Person for a Kontakt via the ID:
			Person p = Person.load(k.getId());
	
			String salutation;
			// TODO default salutation might be configurable (or a "Sex missing!" Info might appear) js 
			if (p.getGeschlecht().equals(Person.MALE)) {							
				salutation = Messages.KontakteView_SalutationM;
			} else  //We do not use any default salutation for unknown sex to avoid errors!
			if (p.getGeschlecht().equals(Person.FEMALE)) {							
				salutation = Messages.KontakteView_SalutationF;
			} else { salutation = ""; //$NON-NLS-1$
			}
			
			if (!StringTool.isNothing(salutation)) {	//salutation should currently never be empty, but paranoia...
				SelectedContactInfosText.append(salutation);
				SelectedContactInfosText.append(StringTool.space);
			}
				
			String titel = p.get(p.TITLE);
			if (!StringTool.isNothing(titel)) {
				SelectedContactInfosText.append(titel).append(StringTool.space);
			}
			//A comma between Family Name and Given Name would be generally helpful to reliably tell them apart:
			//SelectedContactInfosText.append(k.getName()+","+StringTool.space+k.getVorname());
			//But Jürg Hamacher prefers this in his letters without a comma in between:
			//SelectedContactInfosText.append(p.getName()+StringTool.space+p.getVorname());
			//Whereas I use the above variant for PatientenListeView.java;
			//I put the Vorname first in KontakteView. And I only use a spacer, if the first field is not empty!
			//SelectedContactInfosText.append(p.getVorname()+StringTool.space+p.getName());
			if (!StringTool.isNothing(p.getVorname())) {
				SelectedContactInfosText.append(p.getVorname()+StringTool.space);
			}
			if (!StringTool.isNothing(p.getName())) {
				SelectedContactInfosText.append(p.getName());
			}
			
			//Also, in KontakteView, I copy the content of fields "Bemerkung" and "Zusatz" as well.
			//"Zusatz" is mapped to "Bezeichnung3" in Person.java.
			String thisPersonFLD_REMARK = p.get(p.FLD_REMARK);
			if (!StringTool.isNothing(thisPersonFLD_REMARK)) {
				SelectedContactInfosText.append(",").append(StringTool.space).append(thisPersonFLD_REMARK);
			}
			String thisPersonFLD_NAME3 = p.get(p.FLD_NAME3);
			if (!StringTool.isNothing(thisPersonFLD_NAME3)) {
				SelectedContactInfosText.append(",").append(StringTool.space).append(thisPersonFLD_NAME3);
			}						
	
			String thisPatientBIRTHDATE = (String) p.get(p.BIRTHDATE);
			if (!StringTool.isNothing(thisPatientBIRTHDATE)) {
			//This would add the term "geb." (born on the) before the date of birth:
			//	SelectedContactInfosText.append(","+StringTool.space+"geb."+StringTool.space+new TimeTool(thisPatientBIRTHDATE).toString(TimeTool.DATE_GER));
			//But Jürg Hamacher prefers the patient information in his letters without that term:
			SelectedContactInfosText.append(","+StringTool.space+new TimeTool(thisPatientBIRTHDATE).toString(TimeTool.DATE_GER));
			}
		} else {	//if (k.istPerson())... else...
			String thisAddressFLD_NAME1 = (String) k.get(k.FLD_NAME1);
			String thisAddressFLD_NAME2 = (String) k.get(k.FLD_NAME2);
			String thisAddressFLD_NAME3 = (String) k.get(k.FLD_NAME3);
			if (!StringTool.isNothing(thisAddressFLD_NAME1)) {
				SelectedContactInfosText.append(thisAddressFLD_NAME1);
				if (!StringTool.isNothing(thisAddressFLD_NAME2+thisAddressFLD_NAME3)) {
					SelectedContactInfosText.append(StringTool.space);
				}
			}
			if (!StringTool.isNothing(thisAddressFLD_NAME2)) {
				SelectedContactInfosText.append(thisAddressFLD_NAME2);
			}
			if (!StringTool.isNothing(thisAddressFLD_NAME3)) {
				SelectedContactInfosText.append(thisAddressFLD_NAME3);
			}
			if (!StringTool.isNothing(thisAddressFLD_NAME3)) {
				SelectedContactInfosText.append(StringTool.space);
			}
		}
	
		String thisAddressFLD_STREET = (String) k.get(k.FLD_STREET);
		if (!StringTool.isNothing(thisAddressFLD_STREET)) {
			SelectedContactInfosText.append(","+StringTool.space+thisAddressFLD_STREET);
		}
	
		String thisAddressFLD_COUNTRY = (String) k.get(k.FLD_COUNTRY);
		if (!StringTool.isNothing(thisAddressFLD_COUNTRY)) {
			SelectedContactInfosText.append(","+StringTool.space+thisAddressFLD_COUNTRY+"-");
		}
			
		String thisAddressFLD_ZIP = (String) k.get(k.FLD_ZIP);
		if (!StringTool.isNothing(thisAddressFLD_ZIP)) {
				if (StringTool.isNothing(thisAddressFLD_COUNTRY)) {
						SelectedContactInfosText.append(","+StringTool.space);
					};
			SelectedContactInfosText.append(thisAddressFLD_ZIP);
		};
						
		String thisAddressFLD_PLACE = (String) k.get(k.FLD_PLACE);
		if (!StringTool.isNothing(thisAddressFLD_PLACE)) {
			if (StringTool.isNothing(thisAddressFLD_COUNTRY) && StringTool.isNothing(thisAddressFLD_ZIP)) {
				SelectedContactInfosText.append(",");
			};
			SelectedContactInfosText.append(StringTool.space+thisAddressFLD_PLACE);
		}
	
		String thisAddressFLD_PHONE1 = (String) k.get(k.FLD_PHONE1);
		if (!StringTool.isNothing(thisAddressFLD_PHONE1)) {
				SelectedContactInfosText.append(","+StringTool.space+StringTool.space+thisAddressFLD_PHONE1);
		}
			
		String thisAddressFLD_PHONE2 = (String) k.get(k.FLD_PHONE2);
		if (!StringTool.isNothing(thisAddressFLD_PHONE2)) {
			SelectedContactInfosText.append(","+StringTool.space+StringTool.space+thisAddressFLD_PHONE2);
		}
			
		String thisAddressFLD_MOBILEPHONE = (String) k.get(k.FLD_MOBILEPHONE);
		if (!StringTool.isNothing(thisAddressFLD_MOBILEPHONE)) {
			//With a colon after the label:
			//SelectedContactInfosText.append(","+StringTool.space+k.FLD_MOBILEPHONE+":"+StringTool.space+thisAddressFLD_MOBILEPHONE);
			//Without a colon after the label:
			SelectedContactInfosText.append(","+StringTool.space+k.FLD_MOBILEPHONE+StringTool.space+thisAddressFLD_MOBILEPHONE);
		}
			
		String thisAddressFLD_FAX = (String) k.get(k.FLD_FAX);
		if (!StringTool.isNothing(thisAddressFLD_FAX)) {
			//With a colon after the label:
			//SelectedContactInfosText.append(","+StringTool.space+k.FLD_FAX+":"+StringTool.space+thisAddressFLD_FAX);
			//Without a colon after the label:
			SelectedContactInfosText.append(","+StringTool.space+k.FLD_FAX+StringTool.space+thisAddressFLD_FAX);
		}
			
		String thisAddressFLD_E_MAIL = (String) k.get(k.FLD_E_MAIL);
		if (!StringTool.isNothing(thisAddressFLD_E_MAIL)) {
			SelectedContactInfosText.append(","+StringTool.space+thisAddressFLD_E_MAIL);
		}
		return SelectedContactInfosText;
	}

	public static StringBuffer tidyContactInfo(Kontakt k) {
		StringBuffer SelectedContactInfosChangedList = new StringBuffer();
		// To check whether any changes were made,
		// synthesize a string with all affected address fields before and after the processing,
		// and compare them. That's probably faster to do - and not much more of a processing effort,
		// but much less programming hassle - than monitoring the individual steps.
		StringBuffer SelectedContactInfosTextBefore = new StringBuffer();
		StringBuffer SelectedContactInfosTextAfter = new StringBuffer();	
		/*
		 * Tidy all fields of the address
		 */
		
		//Maybe we should also tidy the PostAnschrift?
		//But that would probably be too complicated -
		//step 1: check whether it's made from the other available fields ONLY,
		//        if the slightest suspicion exists that user mods are included - don't touch,
		//        or get very intelligent first.
		
		if (k.istPatient() && !k.istPerson()) {				
			log.debug("KontakteView tidySelectedAddressesAction: corrected: FLD_IS_PATIENT w/o FLD_IS_PERSON: FLD_IS_PERSON set to StringContstants.ONE");
			k.set(Kontakt.FLD_IS_PERSON,StringConstants.ONE);						
		};
		
		//The following field identifiers are derived from Kontakt.java
		//Is there any way to evaluate all field definitions that exist over there,
		//and program a loop that would process all fields automatically?

		//Copy the complete content of k before the processing into a single string
		//so we can easily see later on if any changes were applied that would warrant a manual review of the postaddresse content.
		//Please note: if FLD_IS_PERSON, additional content will be added below.
		
		//The fields are resorted so that a relatively fast review of exported before/after sets is possible.
		//We want to get output even for empty fields; so no check for isNothing. The .append() will work w/o error even for empty fields (tested).
		//We want to be able to compare changed field lengths due to trailing spaces visually. Thus, addition of brackets arround each field.

		//Necessary (looking at the data) probably only for the Postadresse FLD_ANSCHRIFT field and the Bemerkung FLD_REMARK field,
		//but used as a precaution for all fields,
		//we want to replace newlines and carriage returns by |,
		//so that different numbers of lines per field in the Before and After field collections
		//would neither introduce vertical nor horizontal jitter in the exported fields table.
		//Otherwise, they would become very irregular and differences much more difficult to spot manually or by means of formulas within a spreasheet.
		
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_SHORT_LABEL).replace("\n","|").replace("\r","|")+"]\t");};
		
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_NAME1).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_NAME2).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_NAME3).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_REMARK).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_PHONE1).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_PHONE2).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_STREET).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_ZIP).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_PLACE).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_COUNTRY).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_E_MAIL).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_WEBSITE).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_MOBILEPHONE).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_FAX).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextBefore.append("["+k.get(Kontakt.FLD_ANSCHRIFT).replace("\n","|").replace("\r","|")+"]\t");};
		
		//The same processing as for Person.TITLE (see below) is applied to the Kontakt.FLD_ANSCHRIFT field
		//no matter whether we deal with a person or an organization or whatever.
		//This field contains the Postanschrift,
		//where we may also assume that titles may occur (even for an organization, e.g. "Dr. Müller Pharma"),
		//and can be processed rather safely. 
		//This must be done BEFORE the trim() and replace() processing to remove excess spaces.
		//Enforce certain uppercase/lowercase conventions
		
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("prof.","Prof."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("dr.","Dr."));
		
		//We use these ones instead (and I am too tired to make up better regexes):
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Dr.Med.","Dr.med."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Dr.Jur.","Dr.jur."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Dr.Rer.","Dr.rer."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Dr.Nat.","Dr.nat."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Dr. Med.","Dr. med."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Dr. Jur.","Dr. jur."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Dr. Rer.","Dr. rer."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Dr. Nat.","Dr. nat."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Innere med.","Innere Med."));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Anthroposoph. med.","Anthroposoph. Med."));
		

		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Prof.","Prof. "));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Dr.","Dr. "));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("med.","med. "));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("jur.","jur. "));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("rer.","rer. "));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("nat.","nat. "));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("h.c.","h.c. "));
		//remove spaces within "h. c." to "h.c."
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("h. c.","h.c."));
		
		//The same processing as for Kontakt.FLD_NAME3 is applied to the Kontakt.FLD_ANSCHRIFT field.
		//Replace Facharzt f. xyz or Facharzt FMH f. xyz by Facharzt für xyz or Facharzt FMH für xyz
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Facharzt f.","Facharzt für"));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Facharzt FMH f.","Facharzt FMH für"));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Fachärztin f.","Fachärztin für"));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).replace("Fachärztin FMH f.","Fachärztin FMH für"));		
	
		
		//replace three spaces in a row by one space
		//replace two spaces in a row by one space
		//remove leading and trailing spaces for each field that may contain free text input
		
		//Please note: replace 3->1 spaces must be placed left of replace 2->1 spaces, so that both can become active.
		//The processing functions are apparently executed from left to right.
		//If they were placed the other way round, and we have some "abc   xyz" string (with 3 consecutive spaces),
		//only two of these would be replaced by one, so that in the end, we get "abc  xyz", and the second replace would remain unused.
		//I've tried it out. And it makes a difference especially with respect to modified professional titles,
		//where existing single spaces are temporarily expanded and a cleanup is definitely needed in the same processing cycle.
		
		k.set(Kontakt.FLD_E_MAIL,k.get(Kontakt.FLD_E_MAIL).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_WEBSITE,k.get(Kontakt.FLD_WEBSITE).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_MOBILEPHONE,k.get(Kontakt.FLD_MOBILEPHONE).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_FAX,k.get(Kontakt.FLD_FAX).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_SHORT_LABEL,k.get(Kontakt.FLD_SHORT_LABEL).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_ANSCHRIFT,k.get(Kontakt.FLD_ANSCHRIFT).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_COUNTRY,k.get(Kontakt.FLD_COUNTRY).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_PLACE,k.get(Kontakt.FLD_PLACE).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_ZIP,k.get(Kontakt.FLD_ZIP).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_STREET,k.get(Kontakt.FLD_STREET).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_PHONE2,k.get(Kontakt.FLD_PHONE2).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_PHONE1,k.get(Kontakt.FLD_PHONE1).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_REMARK,k.get(Kontakt.FLD_REMARK).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_NAME3,k.get(Kontakt.FLD_NAME3).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_NAME2,k.get(Kontakt.FLD_NAME2).trim().replace("   "," ").replace("  "," "));
		k.set(Kontakt.FLD_NAME1,k.get(Kontakt.FLD_NAME1).trim().replace("   "," ").replace("  "," "));
						
		//Replace Facharzt f. xyz or Facharzt FMH f. xyz by Facharzt für xyz or Facharzt FMH für xyz
		k.set(Kontakt.FLD_NAME3,k.get(Kontakt.FLD_NAME3).replace("Facharzt f.","Facharzt für"));
		k.set(Kontakt.FLD_NAME3,k.get(Kontakt.FLD_NAME3).replace("Facharzt FMH f.","Facharzt FMH für"));
		k.set(Kontakt.FLD_NAME3,k.get(Kontakt.FLD_NAME3).replace("Fachärztin f.","Fachärztin für"));
		k.set(Kontakt.FLD_NAME3,k.get(Kontakt.FLD_NAME3).replace("Fachärztin FMH f.","Fachärztin FMH für"));		

		
		if (k.istPerson()) {
			//Please note that k.set(,k.get()) works in this section with Person.TITLE etc.
			//But p.set(,p.get)) does NOT work.
			//Gerry's comment says: A person is a contact with additional fields,
			//so well, it might be correct that the person is accessed via k...
			//and p.get() throws a no such method error.
			
			//Copy additional content of k (for KONTAKT - PERSON) before the processing into a single string
			//so we can easily see later on if any changes were applied that would warrant a manual review of the postaddresse content.
			//Please note: The fields of KONTAKT have already been added above.

			//The fields are resorted so that a relatively fast review of exported before/after sets is possible.
			//We want to get output even for empty fields; so no check for isNothing. The .append() will work w/o error even for empty fields (tested).
			//We want to be able to compare changed field lengths due to trailing spaces visually. Thus, addition of brackets arround each field.

			{SelectedContactInfosTextBefore.append("["+k.get(Person.TITLE).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextBefore.append("["+k.get(Person.FIRSTNAME).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextBefore.append("["+k.get(Person.NAME).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextBefore.append("["+k.get(Person.SEX).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextBefore.append("["+k.get(Person.BIRTHDATE).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextBefore.append("["+k.get(Person.MOBILE).replace("\n","|").replace("\r","|")+"]\t");};

			//Normalize a title like "Dr.med.", "Dr.Med." etc. to "Dr. med."
			//Or "Prof.Dr.med." to "Prof. Dr. med.",
			//But "h.c." shall remain "h.c."
			
			//Enforce certain uppercase/lowercase conventions						
			k.set(Person.TITLE,k.get(Person.TITLE).replace("prof.","Prof."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("dr.","Dr."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("Med.","med."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("Jur.","jur."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("Rer.","rer."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("Nat.","nat."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("H.c.","h.c."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("H.C.","h.c."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("h.C.","h.c."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("H. c.","h.c."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("H. C.","h.c."));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("h. C.","h.c."));
										
			//Add a space after dots after Dr., Prof. and med.
			//Adding spaces generally after each dot might produce too many unwanted changes.
			//This will produce one trailing space, which will be removed in the next step.
			
			//Very funny. replaceAll searches for regexp, where "." matches any character,
			//but Eclipse thinks that in the first argument, "\." is an invalid special character...
			//And yes. Tested. replaceAll("Prof.","Prof. ")) would really change "Profxmed." to "Prof. med."
			//So I'll search for ASCII character \x2E (hex) instead. Nope: "Prof\x2E": Invalid escape sequence again.
			//I'll use replace() instead of replaceAll) - both will replace all occurences; only the latter will use regexp.
			//But so I can't construct something like "replaceAll("Prof.([:alnum])",... to insert a space only, where more text follows.
			k.set(Person.TITLE,k.get(Person.TITLE).replace("Prof.","Prof. "));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("Dr.","Dr. "));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("med.","med. "));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("jur.","jur. "));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("rer.","rer. "));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("nat.","nat. "));
			k.set(Person.TITLE,k.get(Person.TITLE).replace("h.c.","h.c. "));
			//remove spaces within "h. c." to "h.c."
			k.set(Person.TITLE,k.get(Person.TITLE).replace("h. c.","h.c."));
			
			//The following field identifiers are derived from Person.java
			//Is there any way to evaluate all field definitions that exist over there,
			//and program a loop that would process all fields automatically?

			k.set(Person.TITLE,k.get(Person.TITLE).trim().replace("   "," ").replace("  "," "));
			k.set(Person.MOBILE,k.get(Person.MOBILE).trim().replace("   "," ").replace("  "," "));
			k.set(Person.SEX,k.get(Person.SEX).trim().replace("   "," ").replace("  "," "));
			k.set(Person.BIRTHDATE,k.get(Person.BIRTHDATE).trim().replace("   "," ").replace("  "," "));
			k.set(Person.FIRSTNAME,k.get(Person.FIRSTNAME).trim().replace("   "," ").replace("  "," "));
			k.set(Person.NAME,k.get(Person.NAME).trim().replace("   "," ").replace("  "," "));
		}

		//Copy the complete content of k before the processing into a single string
		//so we can easily see later on if any changes were applied that would warrant a manual review of the postaddresse content.
		//Please note: if FLD_IS_PERSON, additional content will be added below.
		
		//The fields are resorted so that a relatively fast review of exported before/after sets is possible.
		//Necessary (looking at the data) probably only for the Postadresse FLD_ANSCHRIFT field and the Bemerkung FLD_REMARK field,
		//but used as a precaution for all fields,
		//we want to replace newlines and carriage returns by |,
		//so that different numbers of lines per field in the Before and After field collections
		//would neither introduce vertical nor horizontal jitter in the exported fields table.
		//Otherwise, they would become very irregular and differences much more difficult to spot manually or by means of formulas within a spreasheet.

		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_SHORT_LABEL).replace("\n","|").replace("\r","|")+"]\t");};

		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_NAME1).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_NAME2).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_NAME3).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_REMARK).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_PHONE1).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_PHONE2).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_STREET).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_ZIP).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_PLACE).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_COUNTRY).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_E_MAIL).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_WEBSITE).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_MOBILEPHONE).replace("\n","|").replace("\r","|")+"]\t");};
		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_FAX).replace("\n","|").replace("\r","|")+"]\t");};

		{SelectedContactInfosTextAfter.append("["+k.get(Kontakt.FLD_ANSCHRIFT).replace("\n","|").replace("\r","|")+"]\t");};

		//Copy additional content of k (for KONTAKT - PERSON) after the processing into a single string
		//so we can easily see later on if any changes were applied that would warrant a manual review of the postaddresse content.
		//Please note: The fields of KONTAKT have already been added above.

		if (k.istPerson()) {
			//Person p = Person.load(k.getId()); //This has been done above. Will the results of this action persist outside that block? Hopefully.

			//The fields are resorted so that a relatively fast review of exported before/after sets is possible.

			{SelectedContactInfosTextAfter.append("["+k.get(Person.TITLE).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextAfter.append("["+k.get(Person.FIRSTNAME).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextAfter.append("["+k.get(Person.NAME).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextAfter.append("["+k.get(Person.SEX).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextAfter.append("["+k.get(Person.BIRTHDATE).replace("\n","|").replace("\r","|")+"]\t");};
			{SelectedContactInfosTextAfter.append("["+k.get(Person.MOBILE).replace("\n","|").replace("\r","|")+"]\t");};
		}

		//If anything has changed, then add the current address to the list of changed addresses.
		//Actually, I'm adding both the content before and after the processing; without any trailing tab.
		//We will output that list later on - to the clipboard - so that the Postadresse for any changed addresses can be reviewed.
		if ( !SelectedContactInfosTextAfter.toString().equals(SelectedContactInfosTextBefore.toString()) ) {
			log.debug("Before: ["+SelectedContactInfosTextBefore+"]");
			log.debug("After:  ["+SelectedContactInfosTextAfter+"]");
			SelectedContactInfosTextBefore.delete(SelectedContactInfosTextBefore.length(),SelectedContactInfosTextBefore.length());
			SelectedContactInfosTextAfter.delete(SelectedContactInfosTextAfter.length(),SelectedContactInfosTextAfter.length());
			SelectedContactInfosChangedList.append("Before:\t"+SelectedContactInfosTextBefore+"\n");
			SelectedContactInfosChangedList.append("After:\t"+SelectedContactInfosTextAfter+"\n");
		}
		return SelectedContactInfosChangedList;
	}

	/**
	 * Return all personal information. Defined by Jürg Hamacher/Jörg Sigle
	 *  Used in view Patienten
	 * @param multiline
	 * @return A string containing the personalia
	 */
	public static String getPersonalia(Patient k, boolean multiline) {
		StringBuffer selectedPatInfosText = new StringBuffer();
		String delimiter = System.getProperty("line.separator");
	
		if (k.istPerson()) {
			String salutation;
			if (k.getGeschlecht().equals(Person.MALE)) {
				salutation = ch.elexis.core.data.util.Messages.KontakteView_SalutationM;
				salutation = salutation + ( multiline ? delimiter : StringTool.space);
			} else // We do not use any default salutation f
					// unknown sex to
			// avoid errors!
			if (k.getGeschlecht().equals(Person.FEMALE)) {
				salutation = ch.elexis.core.data.util.Messages.KontakteView_SalutationF;
				salutation = salutation + ( multiline ? delimiter : StringTool.space);
			} else {
				salutation = ""; //$NON-NLS-1$
			}
			selectedPatInfosText.append(salutation);
	
			String titel = k.get(Person.TITLE); // $NON-NLS-1$
			if (!StringTool.isNothing(titel)) {
				selectedPatInfosText.append(titel).append(StringTool.space);
			}
			if (!StringTool.isNothing(k.getVorname())) {
				selectedPatInfosText.append(k.getVorname());
			}
			if (!StringTool.isNothing(k.getName())) {
				selectedPatInfosText.append(StringTool.space);
				selectedPatInfosText.append(k.getName());
			}

			if (!StringTool.isNothing(k.get(Kontakt.FLD_NAME3))) {
				selectedPatInfosText.append(delimiter);
				selectedPatInfosText.append(k.get(Kontakt.FLD_NAME3));
			}
				
			String thisPatientBIRTHDATE = k.get(Person.BIRTHDATE);
			if (!StringTool.isNothing(thisPatientBIRTHDATE)) {
				selectedPatInfosText.append(delimiter
						+ new TimeTool(thisPatientBIRTHDATE).toString(TimeTool.DATE_GER));
			}
	
			String thisAddressFLD_STREET = k.get(Kontakt.FLD_STREET);
			if (!StringTool.isNothing(thisAddressFLD_STREET)) {
				selectedPatInfosText.append(delimiter + thisAddressFLD_STREET);
			}
	
			String thisAddressFLD_COUNTRY = k.get(Kontakt.FLD_COUNTRY);
			if (!StringTool.isNothing(thisAddressFLD_COUNTRY)) {
				selectedPatInfosText.append(delimiter + thisAddressFLD_COUNTRY + "-");
			}
	
			String thisAddressFLD_ZIP = k.get(Kontakt.FLD_ZIP);
			if (!StringTool.isNothing(thisAddressFLD_ZIP)) {
				if (StringTool.isNothing(thisAddressFLD_COUNTRY)) {
					selectedPatInfosText.append(delimiter);
				}
				;
				selectedPatInfosText.append(thisAddressFLD_ZIP);
			}
			;
	
			String thisAddressFLD_PLACE = k.get(Kontakt.FLD_PLACE);
			if (!StringTool.isNothing(thisAddressFLD_PLACE)) {
				if (StringTool.isNothing(thisAddressFLD_COUNTRY)
						&& StringTool.isNothing(thisAddressFLD_ZIP)) {
					selectedPatInfosText.append(",");
				}
				;
				selectedPatInfosText.append(StringTool.space + thisAddressFLD_PLACE);
			}
	
			String thisAddressFLD_PHONE1 = k.get(Kontakt.FLD_PHONE1);
			if (!StringTool.isNothing(thisAddressFLD_PHONE1)) {
				selectedPatInfosText
						.append(delimiter + thisAddressFLD_PHONE1);
			}
	
			String thisAddressFLD_PHONE2 = k.get(Kontakt.FLD_PHONE2);
			if (!StringTool.isNothing(thisAddressFLD_PHONE2)) {
				selectedPatInfosText
						.append(delimiter + thisAddressFLD_PHONE2);
			}
	
			// Skip Kontakt.FLD_MOBILEPHONE see #9505
	
			String thisAddressFLD_FAX = k.get(Kontakt.FLD_FAX);
			if (!StringTool.isNothing(thisAddressFLD_FAX)) {
				// With a colon after the label:
				// selectedPatInfosText.append(","+StringTool.space+k.FLD_FAX+":"+StringTool.space+thisAddressFLD_FAX);
				// Without a colon after the label:
				selectedPatInfosText.append(delimiter + Kontakt.FLD_FAX + StringTool.space
						+ thisAddressFLD_FAX);
			}
			String thisAddressFLD_E_MAIL = k.get(Kontakt.FLD_E_MAIL);
			if (!StringTool.isNothing(thisAddressFLD_E_MAIL)) {
				selectedPatInfosText.append(delimiter + thisAddressFLD_E_MAIL);
			}
	
			String thisAddressFLD_WEBSITE = k.get(Kontakt.FLD_WEBSITE);
			if (!StringTool.isNothing(thisAddressFLD_WEBSITE)) {
				selectedPatInfosText.append(delimiter + thisAddressFLD_WEBSITE);
			}
			String thisAddressFLD_REMARK = k.get(Kontakt.FLD_REMARK);
			if (!StringTool.isNothing(thisAddressFLD_REMARK)) {
				selectedPatInfosText.append(delimiter + thisAddressFLD_REMARK);
			}
	
		} else {
			selectedPatInfosText.append(
					"Fehler: Bei diesem Patienten ist das Flag \"Person\" nicht gesetzt! Bitte korrigieren!\n");
		}
		String result = selectedPatInfosText.toString();
		result = result.replaceAll("[\\r\\n]\\n", StringTool.lf); //$NON-NLS-1$
		if (!multiline)  {
			result = result.replaceAll("\\n", "," + StringTool.space); //$NON-NLS-1$
		}
		return result;
	}
	/**
	 * Synthesize the address lines to output from the entries in Kontakt k. added to implement the
	 * output format desired for the copyAddressToClipboard() buttons.
	 * 
	 * @param multiline
	 *            or single line output
	 * @param include_mobile
	 *            controls whether the phone numbers shall be
	 * 
	 * @return string containing the needed information
	 */
	
	/*
	 * getPostAnschrift() does NOT use the System.getProperty("line.separator"); which I use below
	 * after the Fax number (and also in the calling code, before a possibly succeeding addresses.
	 * getPostAnschrift() instead replaces all line separators by either \\n or space at the end of
	 * its run; and I keep that code, +-multiline support herein as well to maintain similar usage
	 * of both methods.
	 * 
	 * On a Win2K system, `that has the following effects when pasting the address(es) into various
	 * targets: notepad: All elements of an address in one line, box characters instead of the
	 * newline (or cr?) character. textpad: New line after each line within an address and between
	 * addresses. winword 97: "new paragraph" after each line within an address, and before a
	 * succeeding address. openoffice 2.0.3: "new line" after each line within an address;
	 * "new paragraph" after the Fax number and before a succeeding address.
	 */
	public static String getPostAnschriftPhoneFaxEmail(Kontakt k, boolean multiline, boolean include_mobile){
		
		StringBuffer thisAddress = new StringBuffer();
		String tab = "\t";
		
		// getPostAnschrift() already returns a line separator after the address;
		// processing of the multiline flag is implemented further below as well,
		// so it suffices if we call getPostAnschrift(true) and not pass the flag there.
		// this also ensures that new-lines inserted in getPostAnschrift() and below,
		// will finally be processed the same way, no matter what we might change below.
		//
		// Wenn die in 2.1.7 eingeführte Funktion zum Putzen von Kontakt-Daten benutzt wurde,
		// dann fehlt der Postanschrift der früher vorhandene trailende LineSeparator.
		// Damit die Telefonnummer in dem Fall nicht direkt am Ort klebt,
		// muss man ihn hier wieder ergänzen. Aber vorher ausschliessen, dass
		// PostAnschrift nicht leer ist, oder dass doch schon ein lineSeparator dran hängt.
		String anschrift = k.getPostAnschrift(true).trim().replaceAll(StringTool.lf, tab);
		if (!multiline) {
			// In this case we must replace "Herr," by "Herr"
			anschrift = anschrift.replaceFirst(tab, StringTool.space);
		}
		thisAddress.append(anschrift);
		
		String thisAddressFLD_PHONE1 = (String) k.get(Kontakt.FLD_PHONE1);
		if (!StringTool.isNothing(thisAddressFLD_PHONE1)) {
			thisAddress.append(tab).append(thisAddressFLD_PHONE1);
		}
		
		String thisAddressFLD_PHONE2 = (String) k.get(Kontakt.FLD_PHONE2);
		if (!StringTool.isNothing(thisAddressFLD_PHONE2)) {
			thisAddress.append(tab).append(thisAddressFLD_PHONE2);
		}
			
		if (include_mobile) {
			String thisAddressFLD_MOBILEPHONE = (String) k.get(Kontakt.FLD_MOBILEPHONE);
			if (!StringTool.isNothing(thisAddressFLD_MOBILEPHONE)) {
				thisAddress.append(tab).append(thisAddressFLD_MOBILEPHONE);
			}
		}
		
		String thisAddressFLD_FAX = (String) k.get(Kontakt.FLD_FAX);
		if (!StringTool.isNothing(thisAddressFLD_FAX)) {
			thisAddress.append(tab).append("Fax:" + StringTool.space + thisAddressFLD_FAX);
		}
		String thisAddressFLD_E_MAIL = (String) k.get(Kontakt.FLD_E_MAIL);
		if (!StringTool.isNothing(thisAddressFLD_E_MAIL)) {
			thisAddress.append(tab).append(thisAddressFLD_E_MAIL);
		}
		String thisAddressFLD_WEBSITE = (String) k.get(Kontakt.FLD_WEBSITE);
		if (!StringTool.isNothing(thisAddressFLD_WEBSITE)) {
			thisAddress.append(tab).append(thisAddressFLD_WEBSITE);
		}
		
		String an = thisAddress.toString();
		String lf = System.getProperty("line.separator");
		return multiline == true ?  an.replaceAll(tab, lf) : an.replaceAll(tab, "," + StringTool.space); //$NON-NLS-1$
	}
	}
