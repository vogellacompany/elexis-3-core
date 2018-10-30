package ch.elexis.core.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import ch.elexis.core.model.ICodeElement;
import ch.elexis.core.services.ICodeElementService.CodeElementTyp;

/**
 * Interface that must be implemented, for each code element system, by bundles that provide a
 * {@link ICodeElement} implementation to the system.
 * 
 * @author thomas
 *
 */
public interface ICodeElementServiceContribution {
	
	public String getSystem();
	
	public CodeElementTyp getTyp();
	
	/**
	 * Load a {@link ICodeElement} instance using its String representation, generated using the
	 * {@link ICodeElementService#storeToString(ICodeElement)} method.<br/>
	 * <br/>
	 * The returned {@link ICodeElement} will have the same SystemName and Code property, the rest
	 * can differ from the {@link ICodeElement} used to generate the String. New imports of the
	 * {@link ICodeElement} dataset, or some value in the context can also change the returned
	 * object.
	 * 
	 * @param storeToString
	 * @param context
	 * @return
	 */
	public Optional<ICodeElement> loadFromCode(String code, Map<Object, Object> context);
	
	/**
	 * Get all {@link ICodeElement} instances (for the given context) of the
	 * {@link ICodeElementServiceContribution}.
	 * 
	 * @param context
	 * @return
	 */
	public List<ICodeElement> getElements(Map<Object, Object> context);
}
