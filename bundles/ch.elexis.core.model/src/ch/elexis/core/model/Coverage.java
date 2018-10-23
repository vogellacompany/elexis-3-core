package ch.elexis.core.model;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import ch.elexis.core.jpa.entities.Fall;
import ch.elexis.core.jpa.entities.Kontakt;
import ch.elexis.core.jpa.model.adapter.AbstractIdDeleteModelAdapter;
import ch.elexis.core.jpa.model.adapter.AbstractIdModelAdapter;
import ch.elexis.core.jpa.model.adapter.mixin.ExtInfoHandler;
import ch.elexis.core.jpa.model.adapter.mixin.IdentifiableWithXid;
import ch.elexis.core.model.messages.Messages;
import ch.elexis.core.model.service.holder.CoreModelServiceHolder;
import ch.elexis.core.model.util.internal.ModelUtil;
import ch.elexis.core.utils.CoreUtil;

public class Coverage extends AbstractIdDeleteModelAdapter<Fall>
		implements IdentifiableWithXid, ICoverage {
	
	private ExtInfoHandler extInfoHandler;
	
	public Coverage(Fall entity){
		super(entity);
		extInfoHandler = new ExtInfoHandler(this);
	}
	
	@Override
	public Object getExtInfo(Object key){
		return extInfoHandler.getExtInfo(key);
	}
	
	@Override
	public void setExtInfo(Object key, Object value){
		extInfoHandler.setExtInfo(key, value);
	}
	
	@Override
	public IPatient getPatient(){
		return ModelUtil.getAdapter(getEntity().getPatient(), IPatient.class);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setPatient(IPatient value){
		if (value != null) {
			getEntity().setPatient(((AbstractIdModelAdapter<Kontakt>) value).getEntity());
		} else {
			getEntity().setPatient(null);
		}
	}
	
	@Override
	public String getDescription(){
		return getEntity().getBezeichnung();
	}
	
	@Override
	public void setDescription(String value){
		getEntity().setBezeichnung(value);
	}
	
	@Override
	public String getReason(){
		return getEntity().getGrund();
	}
	
	@Override
	public void setReason(String value){
		getEntity().setGrund(value);
	}
	
	@Override
	public LocalDate getDateFrom(){
		return getEntity().getDatumVon();
	}
	
	@Override
	public void setDateFrom(LocalDate value){
		getEntity().setDatumVon(value);
	}
	
	@Override
	public LocalDate getDateTo(){
		return getEntity().getDatumBis();
	}
	
	@Override
	public void setDateTo(LocalDate value){
		getEntity().setDatumBis(value);
	}
	
	@Override
	public IBillingSystem getBillingSystem(){
		return new BillingSystem(getEntity().getGesetz());
	}
	
	@Override
	public void setBillingSystem(IBillingSystem value){
		getEntity().setGesetz(value.getName());
	}

	@Override
	public IContact getCostBearer(){
		return ModelUtil.getAdapter(getEntity().getKostentrKontakt(), IContact.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setCostBearer(IContact value){
		if (value != null) {
			getEntity().setKostentrKontakt(((AbstractIdModelAdapter<Kontakt>) value).getEntity());
		} else {
			getEntity().setKostentrKontakt(null);
		}
	}

	@Override
	public String getInsuranceNumber(){
		return getEntity().getVersNummer();
	}

	@Override
	public void setInsuranceNumber(String value){
		getEntity().setVersNummer(value);
	}
	
	@Override
	public List<IEncounter> getEncounters(){
		CoreModelServiceHolder.get().refresh(this);
		return getEntity().getConsultations().parallelStream().filter(f -> !f.isDeleted())
			.map(f -> ModelUtil.getAdapter(f, IEncounter.class, true)).collect(Collectors.toList());
	}
	
	@Override
	public boolean isOpen(){
		return getDateTo() == null;
	}
	
	@Override
	public String getLabel(){
		StringBuilder ret = new StringBuilder();
		if (!isOpen()) {
			ret.append(Messages.Fall_CLOSED);
		}
		String ges = getBillingSystem().getName();
		ret.append(ges).append(": ").append(getReason()).append(" - "); //$NON-NLS-1$ //$NON-NLS-2$
		ret.append(getDescription()).append("("); //$NON-NLS-1$
		LocalDate dateTo = getDateTo();
		ret.append(CoreUtil.defaultDateFormat(getDateFrom())).append("-") //$NON-NLS-1$
			.append(dateTo == null ? Messages.Fall_Open : CoreUtil.defaultDateFormat(dateTo))
			.append(")"); //$NON-NLS-1$
		return ret.toString();
	}
}
