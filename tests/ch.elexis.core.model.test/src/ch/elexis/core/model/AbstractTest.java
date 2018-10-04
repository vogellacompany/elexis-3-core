package ch.elexis.core.model;

import java.time.LocalDate;

import org.junit.After;
import org.junit.Before;

import ch.elexis.core.model.builder.IArticleBuilder;
import ch.elexis.core.model.builder.IContactBuilder;
import ch.elexis.core.model.builder.ICoverageBuilder;
import ch.elexis.core.services.IModelService;
import ch.elexis.core.types.ArticleTyp;
import ch.elexis.core.types.Gender;
import ch.elexis.core.utils.OsgiServiceUtil;

public abstract class AbstractTest {
	
	IModelService modelService;
	
	IPerson person;
	IPatient patient;
	ICoverage coverage;
	IArticle localArticle;
	
	@Before
	public void before(){
		modelService = OsgiServiceUtil.getService(IModelService.class).get();
	}
	
	@After
	public void after(){
		OsgiServiceUtil.ungetService(modelService);
		modelService = null;
	}
	
	public void createPerson(){
		LocalDate dob = LocalDate.of(2016, 9, 1);
		person = new IContactBuilder.PersonBuilder(modelService, "TestPerson", "TestPerson", dob,
			Gender.FEMALE).buildAndSave();
	}
	
	public void createPatient(){
		LocalDate dob = LocalDate.of(2016, 9, 1);
		patient = (IPatient) new IContactBuilder.PatientBuilder(modelService, "TestPatient",
			"TestPatient", dob, Gender.MALE).buildAndSave();
	}
	
	public void createCoverage(){
		coverage = new ICoverageBuilder(modelService, patient, "testCoverage", "testReason",
			"testBillingSystem").buildAndSave();
	}
	
	public void createLocalArticle(){
		localArticle =
			new IArticleBuilder(modelService, "test article", "123456789", ArticleTyp.EIGENARTIKEL)
				.build();
		localArticle.setGtin("0000001111111");
		localArticle.setPackageSize(12);
		localArticle.setSellingSize(12);
		modelService.save(localArticle);
	}
	
	public void removePerson(){
		modelService.remove(person);
	}
	
	public void removePatient(){
		modelService.remove(patient);
	}
	
	public void removeCoverage(){
		modelService.remove(coverage);
	}
	
	public void removeLocalArticle(){
		modelService.remove(localArticle);
	}
}
