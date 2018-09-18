package ch.elexis.core.model;

import ch.elexis.core.jpa.entities.Kontakt;
import ch.elexis.core.jpa.entities.ZusatzAdresse;
import ch.elexis.core.jpa.model.adapter.AbstractIdDeleteModelAdapter;
import ch.elexis.core.jpa.model.adapter.AbstractIdModelAdapter;
import ch.elexis.core.jpa.model.adapter.mixin.IdentifiableWithXid;
import ch.elexis.core.model.util.ModelUtil;
import ch.elexis.core.types.AddressType;
import ch.elexis.core.types.Country;

public class Address extends AbstractIdDeleteModelAdapter<ZusatzAdresse>
		implements IdentifiableWithXid, IAddress {
	
	public Address(ZusatzAdresse entity){
		super(entity);
	}
	
	@Override
	public String getStreet1(){
		return getEntity().getStreet1();
	}
	
	@Override
	public void setStreet1(String value){
		getEntity().setStreet1(value);
	}
	
	@Override
	public String getStreet2(){
		return getEntity().getStreet2();
	}
	
	@Override
	public void setStreet2(String value){
		getEntity().setStreet2(value);
	}
	
	@Override
	public String getZip(){
		return getEntity().getZip();
	}
	
	@Override
	public void setZip(String value){
		getEntity().setZip(value);
	}
	
	@Override
	public String getCity(){
		return getEntity().getCity();
	}
	
	@Override
	public void setCity(String value){
		getEntity().setCity(value);
	}
	
	@Override
	public Country getCountry(){
		return getEntity().getCountry();
	}
	
	@Override
	public void setCountry(Country value){
		getEntity().setCountry(value);
		
	}
	
	@Override
	public String getWrittenAddress(){
		return getEntity().getWrittenAddress();
	}
	
	@Override
	public void setWrittenAddress(String value){
		getEntity().setWrittenAddress(value);
	}
	
	@Override
	public AddressType getType(){
		return getEntity().getAddressType();
	}
	
	@Override
	public void setType(AddressType value){
		getEntity().setAddressType(value);
	}
	
	@Override
	public IContact getContact(){
		if (getEntity().getContact() != null) {
			return ModelUtil.getAdapter(getEntity().getContact(), IContact.class);
		}
		return null;
	}
	
	@Override
	public void setContact(IContact value){
		if (value != null) {
			if (value instanceof AbstractIdModelAdapter) {
				getEntity().setContact((Kontakt) ((AbstractIdModelAdapter<?>) value).getEntity());
			}
		} else {
			getEntity().setContact(null);
		}
	}
	
}
