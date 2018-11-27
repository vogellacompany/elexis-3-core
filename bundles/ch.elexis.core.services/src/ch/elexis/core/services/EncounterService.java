package ch.elexis.core.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import ch.elexis.core.ac.AccessControlDefaults;
import ch.elexis.core.common.ElexisEventTopics;
import ch.elexis.core.constants.Preferences;
import ch.elexis.core.model.IBillable;
import ch.elexis.core.model.IBilled;
import ch.elexis.core.model.IContact;
import ch.elexis.core.model.ICoverage;
import ch.elexis.core.model.IEncounter;
import ch.elexis.core.model.IMandator;
import ch.elexis.core.model.IPatient;
import ch.elexis.core.model.ModelPackage;
import ch.elexis.core.model.builder.ICoverageBuilder;
import ch.elexis.core.model.builder.IEncounterBuilder;
import ch.elexis.core.services.IQuery.COMPARATOR;
import ch.elexis.core.services.IQuery.ORDER;
import ch.elexis.core.services.holder.CodeElementServiceHolder;
import ch.elexis.core.services.holder.ConfigServiceHolder;
import ch.elexis.core.services.holder.ContextServiceHolder;
import ch.elexis.core.services.holder.CoreModelServiceHolder;
import ch.rgw.tools.Result;

@Component
public class EncounterService implements IEncounterService {
	
	@Reference
	private IAccessControlService accessControlService;
	
	@Reference
	private IBillingService billingService;
	
	/**
	 * Get the default label for a new {@link ICoverage}. Performs a lookup for
	 * {@link Preferences#USR_DEFCASELABEL} configuration.
	 * 
	 * @return
	 */
	public static String getDefaultCoverageLabel(){
		Optional<IContact> userContact = ContextServiceHolder.get().getActiveUserContact();
		if (userContact.isPresent()) {
			return ConfigServiceHolder.get().get(userContact.get(), Preferences.USR_DEFCASELABEL,
				Preferences.USR_DEFCASELABEL_DEFAULT);
		}
		return Preferences.USR_DEFCASELABEL_DEFAULT;
	}
	
	/**
	 * Get the default reason for a new {@link ICoverage}. Performs a lookup for
	 * {@link Preferences#USR_DEFCASEREASON} configuration.
	 * 
	 * @return
	 */
	public static String getDefaultCoverageReason(){
		Optional<IContact> userContact = ContextServiceHolder.get().getActiveUserContact();
		if (userContact.isPresent()) {
			return ConfigServiceHolder.get().get(userContact.get(), Preferences.USR_DEFCASEREASON,
				Preferences.USR_DEFCASEREASON_DEFAULT);
		}
		return Preferences.USR_DEFCASEREASON_DEFAULT;
	}
	
	/**
	 * Get the default law for a new {@link ICoverage}. Performs a lookup for
	 * {@link Preferences#USR_DEFLAW} configuration.
	 * 
	 * TODO implement BillingSystem
	 * 
	 * @return
	 */
	public static String getDefaultCoverageLaw(){
		Optional<IContact> userContact = ContextServiceHolder.get().getActiveUserContact();
		if (userContact.isPresent()) {
			return ConfigServiceHolder.get().get(userContact.get(), Preferences.USR_DEFLAW,
				"defaultBillingSystem");
		}
		return "defaultBillingSystem";
		//		return CoreHub.userCfg.get(Preferences.USR_DEFLAW,
		//			BillingSystem.getAbrechnungsSysteme()[0]);
	}
	
	@Override
	public boolean isEditable(IEncounter encounter){
		boolean editable = false;
		boolean hasRight =
			accessControlService.request(AccessControlDefaults.ADMIN_KONS_EDIT_IF_BILLED);
		if (hasRight) {
			// user has right to change encounter. in this case, the user
			// may change the text even if the encounter has already been
			// billed, so don't check if it is billed
			editable = isEditableInternal(encounter);
		} else {
			// normal case, check all
			editable = billingService.isEditable(encounter).isOK();
		}
		
		return editable;
	}
	
	public Result<IEncounter> transferToCoverage(IEncounter encounter, ICoverage coverage,
		boolean ignoreEditable){
		Result<IEncounter> editableResult = billingService.isEditable(encounter);
		if (ignoreEditable || editableResult.isOK()) {
			ICoverage encounterCovearage = encounter.getCoverage();
			encounter.setCoverage(coverage);
			if (encounterCovearage != null) {
				ch.elexis.core.services.ICodeElementService codeElementService =
					CodeElementServiceHolder.get();
				HashMap<Object, Object> context = getCodeElementServiceContext(encounter);
				List<IBilled> encounterBilled = encounter.getBilled();
				for (IBilled billed : encounterBilled) {
					IBillable billable = billed.getBillable();
					// TODO update after getFactor and getPoints methods are established
					// tarmed needs to be recharged
					//					if (isTarmed(billed)) {
					//						// make sure verrechenbar is matching for the kons
					//						Optional<ICodeElement> matchingVerrechenbar =
					//							codeElementService.createFromString(billable.getCodeSystemName(),
					//								billable.getCode(), context);
					//						if (matchingVerrechenbar.isPresent()) {
					//							int amount = billed.getZahl();
					//							removeLeistung(billed);
					//							for (int i = 0; i < amount; i++) {
					//								addLeistung((IVerrechenbar) matchingVerrechenbar.get());
					//							}
					//						} else {
					//							MessageEvent.fireInformation("Info",
					//								"Achtung: durch den Fall wechsel wurde die Position "
					//									+ billable.getCode()
					//									+ " automatisch entfernt, da diese im neuen Fall nicht vorhanden ist.");
					//							removeLeistung(billed);
					//						}
					//					} else {
					//						TimeTool date = new TimeTool(billed.getKons().getDatum());
					//						double factor = billable.getFactor(date, f);
					//						billed.set(Verrechnet.SCALE_SELLING, Double.toString(factor));
					//					}
				}
			}
			CoreModelServiceHolder.get().save(encounter);
			ContextServiceHolder.get().postEvent(ElexisEventTopics.EVENT_UPDATE, encounter);
		} else if (!editableResult.isOK()) {
			return editableResult;
		}
		return new Result<IEncounter>(encounter);
	}
	
	private HashMap<Object, Object> getCodeElementServiceContext(IEncounter encounter){
		HashMap<Object, Object> ret = new HashMap<>();
		ret.put(ICodeElementService.ContextKeys.CONSULTATION, encounter);
		ICoverage coverage = encounter.getCoverage();
		if (coverage != null) {
			ret.put(ICodeElementService.ContextKeys.COVERAGE, coverage);
		}
		return ret;
	}
	
	private boolean isEditableInternal(IEncounter encounter){
		ICoverage coverage = encounter.getCoverage();
		if (coverage != null) {
			if (!coverage.isOpen()) {
				return false;
			}
		}
		
		IMandator encounterMandator = encounter.getMandator();
		boolean checkMandant =
			!accessControlService.request(AccessControlDefaults.LSTG_CHARGE_FOR_ALL);
		boolean mandatorOK = true;
		IMandator activeMandator =
			ContextServiceHolder.get().getRootContext().getActiveMandator().orElse(null);
		boolean mandatorLoggedIn = (activeMandator != null);
		
		// if m is null, ignore checks (return true)
		if (encounterMandator != null && activeMandator != null) {
			if (checkMandant && !(encounterMandator.getId().equals(activeMandator.getId()))) {
				mandatorOK = false;
			}
		}
		
		boolean ok = mandatorOK && mandatorLoggedIn;
		if (ok) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Optional<IEncounter> getLastEncounter(IPatient patient, boolean create){
		if (!ContextServiceHolder.get().getActiveMandator().isPresent()) {
			return Optional.empty();
		}
		IMandator activeMandator = ContextServiceHolder.get().getActiveMandator().get();
		IContact userContact = ContextServiceHolder.get().getActiveUserContact().get();
		IQuery<IEncounter> encounterQuery = CoreModelServiceHolder.get().getQuery(IEncounter.class);
		
		// if not configured otherwise load only consultations of active mandant
		if (!ConfigServiceHolder.get().get(userContact, Preferences.USR_DEFLOADCONSALL, false)) {
			encounterQuery.and(ModelPackage.Literals.IENCOUNTER__MANDATOR, COMPARATOR.EQUALS,
				activeMandator);
		}
		
		List<ICoverage> coverages = patient.getCoverages();
		if (coverages == null || coverages.isEmpty()) {
			return create ? createCoverageAndEncounter(patient) : Optional.empty();
		}
		encounterQuery.startGroup();
		boolean termInserted = false;
		for (ICoverage coverage : coverages) {
			if (coverage.isOpen()) {
				encounterQuery.or(ModelPackage.Literals.IENCOUNTER__COVERAGE, COMPARATOR.EQUALS,
					coverage);
				termInserted = true;
			}
		}
		if (!termInserted) {
			return create ? createCoverageAndEncounter(patient) : Optional.empty();
		}
		encounterQuery.andJoinGroups();
		encounterQuery.orderBy(ModelPackage.Literals.IENCOUNTER__DATE, ORDER.DESC);
		List<IEncounter> list = encounterQuery.execute();
		if ((list == null) || list.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(list.get(0));
		}
	}
	
	public Optional<IEncounter> createCoverageAndEncounter(IPatient patient){
		ICoverage coverage = new ICoverageBuilder(CoreModelServiceHolder.get(), patient,
			getDefaultCoverageLabel(), getDefaultCoverageReason(), getDefaultCoverageLaw())
				.buildAndSave();
		Optional<IMandator> activeMandator = ContextServiceHolder.get().getActiveMandator();
		if (activeMandator.isPresent()) {
			return Optional.of(
				new IEncounterBuilder(CoreModelServiceHolder.get(), coverage, activeMandator.get())
					.buildAndSave());
		}
		return Optional.empty();
	}
	
	//	private static final String ENCOUNTER_LAST_QUERY =
	//		" ON BH.FallID = FA.id AND BH.deleted = FA.deleted WHERE FA.PatientID = :patientid and FA.deleted = '0' order by BH.Datum desc, BH.lastupdate desc limit 1";
	
	// @formatter:off
	private static final String ENCOUNTER_LAST_QUERY = "SELECT behandlungen.ID FROM behandlungen, faelle"
			+ " WHERE behandlungen.FallID = faelle.id"
			+ " AND behandlungen.deleted = faelle.deleted"
			+ " AND faelle.deleted = '0'"
			+ " AND faelle.patientID = ?1"
			+ " ORDER BY behandlungen.datum desc, behandlungen.lastupdate desc"
			+ " LIMIT 1";
	// @formatter:on
	
	@Override
	public Optional<IEncounter> getLastEncounter(IPatient patient){
		INativeQuery nativeQuery =
			CoreModelServiceHolder.get().getNativeQuery(ENCOUNTER_LAST_QUERY);
		Iterator<?> result = nativeQuery.executeWithParameters(
			nativeQuery.getIndexedParameterMap(Integer.valueOf(1),
				patient.getId()))
			.iterator();
		if (result.hasNext()) {
			String next = result.next().toString();
			return CoreModelServiceHolder.get().load(next, IEncounter.class);
		}
		return Optional.empty();
	}
}