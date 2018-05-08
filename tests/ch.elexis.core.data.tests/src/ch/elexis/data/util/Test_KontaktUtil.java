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
package ch.elexis.data.util;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.util.KontaktUtil;
import ch.elexis.data.AbstractPersistentObjectTest;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Mandant;
import ch.elexis.data.Patient;
import ch.elexis.data.User;
import ch.rgw.tools.JdbcLink;
import ch.rgw.tools.StringTool;

public class Test_KontaktUtil extends AbstractPersistentObjectTest {
	
	public Test_KontaktUtil(JdbcLink link){
		super(link);
	}
	
	private static Patient p_Mustermann;
	private static Kontakt k_Mustermann;
	private static Patient p_Baumgartner;
	private static Kontakt k_Baumgartner;
	final static String content_prefix = "I_";
	final static String phone = "033 822 02 02";
	final static String mobile = "079 335 66 33";
	final static String fax = "033 822 02 03";

	// In der View "Patient Detail"
	// "Alle Daten der ausgewählten Kontakte jeweils als Einzeiler in die Zwischenablage kopieren"

	final static String expectedPostanschriftWithPhone =
		"Herr, Dr. med. Andreas Baumgartner, Marktgasse 1, CH - 3800 Interlaken, 033 822 02 02, 079 335 66 33, Fax: 033 822 02 03, baumgartner.praxis@hin.ch, www.baumgartner.ch";
	final String expectedPostanschriftWithoutPhone =
		"Herr, Dr. med. Andreas Baumgartner, Marktgasse 1, CH - 3800 Interlaken, Fax: 033 822 02 03, baumgartner.praxis@hin.ch, www.baumgartner.ch";
	
	
	final static String[] fieldnames = {
		Patient.FLD_NAME, Patient.FLD_NAME1, Patient.FLD_NAME2, Patient.FLD_NAME3,
		Patient.FLD_REMARK, Patient.FLD_NAME3, Patient.FLD_STREET, Patient.FLD_TITLE_SUFFIX,
		"Titel", Patient.FLD_ZIP, Patient.FLD_PLACE, Patient.FLD_PHONE1, Patient.FLD_MOBILEPHONE,
		Patient.FLD_FAX, Patient.FLD_E_MAIL, Patient.FLD_WEBSITE
	};
	
	private static void genBaumgartner(){
		p_Baumgartner = new Patient("Baumgartner", "Andreas", "1.1.1980", "m");
		k_Baumgartner = Kontakt.load(p_Baumgartner.getId());
		k_Baumgartner.set("Titel", "Dr. med.");
		k_Baumgartner.set(Patient.FLD_REMARK, "Sprechstunden nur nach Vereinbarung");
		k_Baumgartner.set(Patient.FLD_NAME3,
			"Facharzt FMH für Gastroenterologie u. Innere Medizin");
		k_Baumgartner.set(Patient.FLD_FAX, fax);
		k_Baumgartner.set(Patient.FLD_MOBILEPHONE, mobile);
		k_Baumgartner.set(Patient.FLD_PHONE1, phone);
		k_Baumgartner.set(Patient.FLD_COUNTRY, "CH");
		k_Baumgartner.set(Patient.FLD_ZIP, "3800");
		k_Baumgartner.set(Patient.FLD_STREET, "Marktgasse 1");
		k_Baumgartner.set(Patient.FLD_PLACE, "Interlaken");
		k_Baumgartner.set(Patient.FLD_E_MAIL, "baumgartner.praxis@hin.ch");
		k_Baumgartner.set(Patient.FLD_WEBSITE, "www.baumgartner.ch");
	}
	private static void genMustermann(){
		p_Mustermann = new Patient("Mustermann", "Max", "1.1.2000", "m");
		k_Mustermann = Kontakt.load(p_Mustermann.getId());
	}
	
	@Before
	public void before(){
		User user = User.load(testUserName);
		// set user and Mandant in system
		ElexisEventDispatcher.getInstance()
			.fire(new ElexisEvent(user, User.class, ElexisEvent.EVENT_SELECTED));
		Mandant m = new Mandant("Mandant", "Erwin", "26.07.1979", "m");
		CoreHub.setMandant(m);
		genBaumgartner();
		genMustermann();
	}
	
	@After
	public void after(){
		if (p_Mustermann != null)
			p_Mustermann.delete();
		if (p_Baumgartner != null)
			p_Baumgartner.delete();
	}
	
	@Test
	public void testGetContactInfo(){
		genMustermann();
		StringBuffer info = KontaktUtil.getContactInfo(p_Mustermann);
		Assert.assertEquals(ch.elexis.core.data.util.Messages.KontakteView_SalutationM
			+ " Max Mustermann, 01.01.2000", info.toString());
	}
	
	@Test
	public void testTidyContactInfo(){
		
		final String familyNameWithApostrophe = "D'Andrea";
		Patient male = new Patient("Mustermann", "Max", "1.1.2000", "m");
		male.set(Patient.FLD_NAME, familyNameWithApostrophe);
		male.set(Kontakt.FLD_NAME3, "Facharzt FMH f.");
		
		male.set(Patient.FLD_ANSCHRIFT, "prof.");
		StringBuffer SelectedContactInfosChangedList = KontaktUtil.tidyContactInfo(male);
		Assert.assertEquals("Facharzt FMH für", male.get(Kontakt.FLD_NAME3));
		Assert.assertNotEquals(0, SelectedContactInfosChangedList.toString().length());
		Assert.assertEquals("Prof.", male.get(Kontakt.FLD_ANSCHRIFT));
	}
	
	@Test
	public void testTidyContactInfoNothingToChange(){
		final String familyNameWithApostrophe = "D'Andrea";
		final String anrede = "Prof. Dr. med. FMH";
		Patient male = new Patient("Mustermann", "Max", "1.1.2000", "m");
		male.set(Patient.FLD_NAME, familyNameWithApostrophe);
		male.set(Kontakt.FLD_NAME3, "Facharzt FMH für");
		
		male.set(Patient.FLD_ANSCHRIFT, anrede);
		StringBuffer SelectedContactInfosChangedList = KontaktUtil.tidyContactInfo(male);
		Assert.assertEquals("Facharzt FMH für", male.get(Kontakt.FLD_NAME3));
		Assert.assertEquals(0, SelectedContactInfosChangedList.toString().length());
		Assert.assertEquals(anrede, male.get(Kontakt.FLD_ANSCHRIFT));
	}
	
	@Test
	public void testPhoneFaxOnelinerWithoutPhone(){
		k_Baumgartner.set(Kontakt.FLD_ANSCHRIFT, null);
		String res = KontaktUtil.getPostAnschriftPhoneFaxEmail(p_Baumgartner, false, false);
		Assert.assertEquals(expectedPostanschriftWithoutPhone, res);
	}
	
	@Test
	public void testPhoneFaxMultilineWithPhone(){
		String res = KontaktUtil.getPostAnschriftPhoneFaxEmail(k_Baumgartner, true, true);
		String multiline = expectedPostanschriftWithPhone.replaceAll(", ", StringTool.lf);
		Assert.assertEquals(multiline, res);
	}
	
	@Test
	public void testWithAnschriftPhoneFaxOnelinerWithPhone(){
		k_Mustermann.setAnschrift(new ch.elexis.data.Anschrift(k_Baumgartner)); // Must be called before calling getAnschrift
		String res = KontaktUtil.getPostAnschriftPhoneFaxEmail(k_Mustermann, false, true);
		String res2 = KontaktUtil.getPostAnschriftPhoneFaxEmail(k_Baumgartner, false, true);
		Assert.assertEquals("Herr, Max Mustermann, Marktgasse 1, CH - 3800 Interlaken", res);
		Assert.assertEquals(expectedPostanschriftWithPhone, res2);
	}
	@Test
	public void testBaumgartnerPhoneFaxOnelinerWithPhone(){
		String res = KontaktUtil.getPostAnschriftPhoneFaxEmail(k_Baumgartner, false, true);
		Assert.assertEquals(expectedPostanschriftWithPhone, res);
	}
	
	@Test
	public void testMustermannrPhoneFaxOnelinerWithPhone(){
		String res = KontaktUtil.getPostAnschriftPhoneFaxEmail(k_Mustermann, false, true);
		Assert.assertEquals("Herr, Max Mustermann", res);
	}
	
	@Test
	public void testPhoneFaxMultilineWithoutPhone(){
		k_Baumgartner.set(Kontakt.FLD_ANSCHRIFT, null);
		String res = KontaktUtil.getPostAnschriftPhoneFaxEmail(k_Baumgartner, true, false);
		String multiline = expectedPostanschriftWithoutPhone.replaceAll(", ", StringTool.lf);
		Assert.assertEquals(multiline, res);
	}
	
	// PD_OHNE_Mobil_einzeilig_an_Zwischenablage
	final String expectedPersonalia =
		"Herr Dr. med. Andreas Baumgartner, Facharzt FMH für Gastroenterologie u. Innere Medizin, 01.01.1980, " +
			"Marktgasse 1, CH-3800 Interlaken, 033 822 02 02, Fax 033 822 02 03, "+
			"baumgartner.praxis@hin.ch, www.baumgartner.ch, Sprechstunden nur nach Vereinbarung";
	
	@Test
	public void testPatientPersonaliaMultiline(){
		String res = KontaktUtil.getPersonalia(p_Baumgartner, true);
		String multiline = expectedPersonalia.replaceAll(", ", StringTool.lf);
		Assert.assertEquals(multiline, res);
	}
	
	@Test
	public void testPatientPersonaliaOneLiner(){
		String res = KontaktUtil.getPersonalia(p_Baumgartner, false);
		String multiline = expectedPersonalia;
		Assert.assertEquals(multiline, res);
	}
	
}
