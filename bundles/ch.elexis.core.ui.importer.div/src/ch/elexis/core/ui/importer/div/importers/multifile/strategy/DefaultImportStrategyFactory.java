package ch.elexis.core.ui.importer.div.importers.multifile.strategy;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.importer.div.importers.ILabContactResolver;
import ch.elexis.core.importer.div.importers.multifile.strategy.BasicFileImportStrategyFactory;
import ch.elexis.core.importer.div.importers.multifile.strategy.DefaultHL7ImportStrategy;
import ch.elexis.core.importer.div.importers.multifile.strategy.IFileImportStrategy;
import ch.elexis.core.importer.div.importers.multifile.strategy.IFileImportStrategyFactory;
import ch.elexis.core.services.IVirtualFilesystemService.IVirtualFilesystemHandle;
import ch.rgw.io.FileTool;

/**
 * Contains strategies for HL7Files ({@link DefaultHL7ImportStrategy}) and their matching PDFFiles (
 * {@link DefaultPDFImportStrategy})<br>
 * Matching of HL7File to PDF takes place via the filename (need to be identical apart from file
 * suffix).
 * 
 * @author lucia
 * 		
 */
public class DefaultImportStrategyFactory extends BasicFileImportStrategyFactory {
	private static final Logger log = LoggerFactory.getLogger(DefaultImportStrategyFactory.class);
	
	private boolean moveAfterImport;
	
	private ILabContactResolver labContactResolver;
	
	@Override
	public Map<IVirtualFilesystemHandle, IFileImportStrategy> createImportStrategyMap(IVirtualFilesystemHandle hl7File){
		Map<IVirtualFilesystemHandle, IFileImportStrategy> ret = super.createImportStrategyMap(hl7File);
		
		List<IVirtualFilesystemHandle> matchingFiles;
		try {
			matchingFiles = getMatchingFiles(hl7File);
		} catch (IOException e) {
			// masquerade, as this exception is already catched upstream
			throw new IllegalStateException(e);
		}
		// matching files for this hl7 file, probably pdf
		if (!matchingFiles.isEmpty()) {
			DefaultPDFImportStrategy pdfImportStrategy = new DefaultPDFImportStrategy();
			for (IVirtualFilesystemHandle mFile : matchingFiles) {
				String type = FileTool.getExtension(mFile.getName()).toLowerCase();
				if ("pdf".equals(type)) {
					log.debug("... adding [" + mFile.getName() + "] with DefaultPDFImportStrategy");
					ret.put(mFile, pdfImportStrategy);
				}
			}
		}
			
		ret.values().forEach(strategy -> strategy.setMoveAfterImport(moveAfterImport)
			.setLabContactResolver(labContactResolver));
		return ret;
	}
	
	/**
	 * Specify if imported files should be moved to archiv and error directory inside the import
	 * directory. Default is false.
	 * 
	 * @param value
	 * @return
	 */
	@Override
	public IFileImportStrategyFactory setMoveAfterImport(boolean value){
		this.moveAfterImport = value;
		return this;
	}
	
	/**
	 * Add the {@link ILabContactResolver} that should be used on import.
	 * 
	 * @param resolver
	 * @return
	 */
	@Override
	public IFileImportStrategyFactory setLabContactResolver(ILabContactResolver resolver){
		this.labContactResolver = resolver;
		return this;
	}
}
