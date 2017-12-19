/*******************************************************************************
 * Copyright (c) 2007-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/

package ch.elexis.core.ui.views.rechnung;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.exceptions.ElexisException;
import ch.elexis.core.ui.commands.Handler;
import ch.elexis.core.ui.commands.MahnlaufCommand;
import ch.elexis.core.ui.constants.UiResourceConstants;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.core.ui.locks.AllOrNoneLockRequestingAction;
import ch.elexis.core.ui.locks.AllOrNoneLockRequestingRestrictedAction;
import ch.elexis.core.ui.locks.LockRequestingAction;
import ch.elexis.core.ui.text.ITextPlugin.ICallback;
import ch.elexis.core.ui.text.TextContainer;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.core.ui.views.FallDetailView;
import ch.elexis.core.ui.views.rechnung.invoice.InvoiceActions;
import ch.elexis.data.AccountTransaction;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;
import ch.elexis.data.Rechnung;
import ch.elexis.data.RnStatus;
import ch.elexis.data.Zahlung;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.Money;
import ch.rgw.tools.Tree;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * Collection of bill-related actions
 * 
 * @author gerry
 * 
 */
public class RnActions {
	/**
	 * @deprecated please replace with {@link InvoiceActions}
	 */
	Action rnExportAction, increaseLevelAction, addPaymentAction, addExpenseAction, changeStatusAction, stornoAction, addAccountExcessAction;
	Action editCaseAction, delRnAction, reactivateRnAction, patDetailAction;
	Action expandAllAction, collapseAllAction, reloadAction, mahnWizardAction;
	Action printListeAction, exportListAction, rnFilterAction;
	
	RnActions(final RechnungsListeView view){
		
		printListeAction = new Action(Messages.RnActions_printListAction) { //$NON-NLS-1$
				{
					setImageDescriptor(Images.IMG_PRINTER.getImageDescriptor());
					setToolTipText(Messages.RnActions_printListTooltip); //$NON-NLS-1$
				}
				@Override
				public void run(){
					Object[] sel = view.cv.getSelection();
					new RnListeDruckDialog(view.getViewSite().getShell(), sel).open();
				}
			};
			exportListAction = new Action(Messages.RnActions_exportListAction) {
						{
							setToolTipText(Messages.RnActions_exportListTooltip);
						}
						@Override
						public void run(){
							Object[] sel = view.cv.getSelection();
							new RnListeExportDialog(view.getViewSite().getShell(), sel).open();
						}
				};			mahnWizardAction = new Action(Messages.RnActions_remindersAction) { //$NON-NLS-1$
				{
					setToolTipText(Messages.RnActions_remindersTooltip); //$NON-NLS-1$
					setImageDescriptor(Images.IMG_WIZARD.getImageDescriptor());
				}
				
				@Override
				public void run(){
					if (!MessageDialog.openConfirm(view.getViewSite().getShell(),
						Messages.RnActions_reminderConfirmCaption, //$NON-NLS-1$
						Messages.RnActions_reminderConfirmMessage)) { //$NON-NLS-1$
						return;
					}
					Handler.execute(view.getViewSite(), MahnlaufCommand.ID, null);
					view.cfp.clearValues();
					view.cfp.cbStat
						.setText(RnControlFieldProvider.stats[RnControlFieldProvider.stats.length - 5]);
					view.cfp.fireChangedEvent();
				}
			};
		rnExportAction = new Action(Messages.RechnungsListeView_printAction) { //$NON-NLS-1$
				{
					setToolTipText(Messages.RechnungsListeView_printToolTip); //$NON-NLS-1$
					setImageDescriptor(Images.IMG_GOFURTHER.getImageDescriptor());
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					new RnOutputDialog(view.getViewSite().getShell(), list).open();
				}
			};
		
		patDetailAction = new Action(Messages.RnActions_patientDetailsAction) { //$NON-NLS-1$
				@Override
				public void run(){
					IWorkbenchPage rnPage =
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						/* PatientDetailView fdv=(PatientDetailView) */rnPage
							.showView(UiResourceConstants.PatientDetailView2_ID);
					} catch (Exception ex) {
						ExHandler.handle(ex);
					}
				}
				
			};
		editCaseAction = new Action(Messages.RnActions_edirCaseAction) { //$NON-NLS-1$
			
				@Override
				public void run(){
					IWorkbenchPage rnPage =
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						rnPage.showView(FallDetailView.ID);
					} catch (Exception ex) {
						ExHandler.handle(ex);
					}
				}
				
			};
		delRnAction =
			new AllOrNoneLockRequestingAction<Rechnung>(Messages.RnActions_deleteBillAction) {
				
				@Override
				public List<Rechnung> getTargetedObjects(){
					return view.createList();
				}
				
				@Override
				public void doRun(List<Rechnung> lockedElements){
					for (Rechnung rn : lockedElements) {
						rn.storno(true);
					}
				}
			};
		reactivateRnAction = new AllOrNoneLockRequestingAction<Rechnung>(Messages.RnActions_reactivateBillAction) {
			
				@Override
				public List<Rechnung> getTargetedObjects(){
					return view.createList();
				}

				@Override
				public void doRun(List<Rechnung> lockedElements){
					for (Rechnung rn : lockedElements) {
						rn.setStatus(RnStatus.OFFEN);
					}
				}
			};
		expandAllAction = new Action(Messages.RnActions_expandAllAction) { //$NON-NLS-1$
				@Override
				public void run(){
					view.cv.getViewerWidget().getControl().setRedraw(false);
					((TreeViewer) view.cv.getViewerWidget()).expandAll();
					view.cv.getViewerWidget().getControl().setRedraw(true);
				}
			};
		collapseAllAction = new Action(Messages.RnActions_collapseAllAction) { //$NON-NLS-1$
				@Override
				public void run(){
					view.cv.getViewerWidget().getControl().setRedraw(false);
					((TreeViewer) view.cv.getViewerWidget()).collapseAll();
					view.cv.getViewerWidget().getControl().setRedraw(true);
				}
			};
		reloadAction = new Action(Messages.RnActions_reloadAction) { //$NON-NLS-1$
				{
					setToolTipText(Messages.RnActions_reloadTooltip); //$NON-NLS-1$
					setImageDescriptor(Images.IMG_REFRESH.getImageDescriptor());
				}
				
				@Override
				public void run(){
					view.cfp.fireChangedEvent();
				}
			};
		
		addPaymentAction = new Action(Messages.RnActions_addBookingAction) { //$NON-NLS-1$
				{
					setToolTipText(Messages.RnActions_addBookingTooltip); //$NON-NLS-1$
					setImageDescriptor(Images.IMG_ADDITEM.getImageDescriptor());
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					if (list.size() > 0) {
						Rechnung actRn = list.get(0);
						try {
							if (new RnDialogs.BuchungHinzuDialog(view.getViewSite().getShell(),
								actRn).open() == Dialog.OK) {
								ElexisEventDispatcher.update(actRn);
							}
						} catch (ElexisException e) {
							SWTHelper.showError("Zahlung hinzufügen ist nicht möglich",
								e.getLocalizedMessage());
						}
					}
				}
			};
		
		addExpenseAction = new Action(Messages.RnActions_addFineAction) { //$NON-NLS-1$
				{
					setImageDescriptor(Images.IMG_REMOVEITEM.getImageDescriptor());
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					if (!list.isEmpty()) {
						try {
							if (list.size() == 1) {
								Rechnung actRn = list.get(0);
								if (new RnDialogs.GebuehrHinzuDialog(view.getViewSite().getShell(),
									actRn).open() == Dialog.OK) {
									ElexisEventDispatcher.update(actRn);
								}
							} else {
								if (new RnDialogs.MultiGebuehrHinzuDialog(view.getViewSite()
									.getShell(), list).open() == Dialog.OK) {
									for (Rechnung rn : list) {
										ElexisEventDispatcher.update(rn);
									}
								}
							}
						} catch (ElexisException e) {
							SWTHelper.showError("Zahlung hinzufügen ist nicht möglich",
								e.getLocalizedMessage());
						}
					}
				}
			};
		
		changeStatusAction =
			new AllOrNoneLockRequestingRestrictedAction<Rechnung>(AccessControlDefaults.ADMIN_CHANGE_BILLSTATUS_MANUALLY,
				Messages.RnActions_changeStateAction) { //$NON-NLS-1$
				{
					setToolTipText(Messages.RnActions_changeStateTooltip); //$NON-NLS-1$
					setImageDescriptor(Images.IMG_EDIT.getImageDescriptor());
				}

				@Override
				public List<Rechnung> getTargetedObjects(){
					return view.createList();
				}

				@Override
				public void doRun(List<Rechnung> list){
					if (list.size() == 1) {
						Rechnung actRn = list.get(0);
						if (new RnDialogs.StatusAendernDialog(view.getViewSite().getShell(),
							actRn).open() == Dialog.OK) {
							ElexisEventDispatcher.update(actRn);
						}
					} else {
						if (new RnDialogs.MultiStatusAendernDialog(view.getViewSite()
							.getShell(), list).open() == Dialog.OK) {
							for (Rechnung rn : list) {
								ElexisEventDispatcher.update(rn);
							}
						}
					}
				}
			};
		stornoAction = new LockRequestingAction<Rechnung>(Messages.RnActions_stornoAction) {
				{
					setImageDescriptor(Images.IMG_DELETE.getImageDescriptor());
					setToolTipText(Messages.RnActions_stornoActionTooltip);
				}

				@Override
				public Rechnung getTargetedObject(){
					List<Rechnung> list = view.createList();
					if (!list.isEmpty()) {
						return list.get(0);
					}
					return null;
				}

				@Override
				public void doRun(Rechnung actRn){
					if (new RnDialogs.StornoDialog(view.getViewSite().getShell(), actRn).open() == Dialog.OK) {
						ElexisEventDispatcher.update(actRn);
					}
				}
			};
		increaseLevelAction = new Action(Messages.RnActions_increaseReminderLevelAction) { //$NON-NLS-1$
				{
					setToolTipText(Messages.RnActions_increadeReminderLevelTooltip); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					if (list.size() > 0) {
						for (Rechnung actRn : list) {
							switch (actRn.getStatus()) {
							case RnStatus.OFFEN_UND_GEDRUCKT:
								actRn.setStatus(RnStatus.MAHNUNG_1);
								break;
							case RnStatus.MAHNUNG_1_GEDRUCKT:
								actRn.setStatus(RnStatus.MAHNUNG_2);
								break;
							case RnStatus.MAHNUNG_2_GEDRUCKT:
								actRn.setStatus(RnStatus.MAHNUNG_3);
								break;
							default:
								SWTHelper.showInfo(Messages.RnActions_changeStateErrorCaption, //$NON-NLS-1$
									Messages.RnActions_changeStateErrorMessage); //$NON-NLS-1$
							}
						}
					}
					
				}
			};
		addAccountExcessAction = new Action(Messages.RnActions_addAccountGood) { //$NON-NLS-1$
				{
					setToolTipText(Messages.RnActions_addAccountGoodTooltip); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					List<Rechnung> list = view.createList();
					if (list.size() > 0) {
						Rechnung actRn = list.get(0);
						
						// Allfaelliges Guthaben des Patienten der Rechnung als
						// Anzahlung hinzufuegen
						Fall fall = actRn.getFall();
						Patient patient = fall.getPatient();
						Money prepayment = patient.getAccountExcess();
						if (prepayment.getCents() > 0) {
							// make sure prepayment is not bigger than amount of
							// bill
							Money amount;
							if (prepayment.getCents() > actRn.getBetrag().getCents()) {
								amount = new Money(actRn.getBetrag());
							} else {
								amount = new Money(prepayment);
							}
							
							if (SWTHelper
								.askYesNo(
									Messages.RnActions_transferMoneyCaption, //$NON-NLS-1$
									"Das Konto von Patient \""
										+ patient.getLabel()
										+ "\" weist ein positives Kontoguthaben auf. Wollen Sie den Betrag von "
										+ amount.toString() + " dieser Rechnung \"" + actRn.getNr()
										+ ": " + fall.getLabel() + "\" zuweisen?")) {
								
								// remove amount from account and transfer it to the
								// bill
								Money accountAmount = new Money(amount);
								accountAmount.negate();
								new AccountTransaction(patient, null, accountAmount, null,
									"Anzahlung von Kontoguthaben auf Rechnung " + actRn.getNr());
								actRn.addZahlung(amount, "Anzahlung von Kontoguthaben", null);
							}
						}
					}
				}
			};
		rnFilterAction = new Action(Messages.RnActions_filterListAction, Action.AS_CHECK_BOX) { //$NON-NLS-1$
				{
					setImageDescriptor(Images.IMG_FILTER.getImageDescriptor());
					setToolTipText(Messages.RnActions_filterLIstTooltip); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					if (isChecked()) {
						RnFilterDialog rfd = new RnFilterDialog(view.getViewSite().getShell());
						if (rfd.open() == Dialog.OK) {
							view.cntp.setConstraints(rfd.ret);
							view.cfp.fireChangedEvent();
						}
					} else {
						view.cntp.setConstraints(null);
						view.cfp.fireChangedEvent();
					}
					
				}
			};
	}
	//201512211341js: Info: This dialog starts the generation of output ONLY AFTER [OK] has been pressed.
	static class RnListeExportDialog extends TitleAreaDialog implements ICallback {
		ArrayList<Rechnung> rnn;
		private TextContainer text;

		//201512211459js: Siehe auch RechnungsDrucker.java - nach dortigem Vorbild modelliert.
		//Zur Kontrolle es Ausgabeverzeichnisses, mit permanentem Speichern.
		//ToDo: Durchgängig auf externe Konstanten umstellen, wie dort gezeigt, u.a. bei Hub.LocalCfg Zugriffen.
		private Button bSaveFileAs;
		String RnListExportDirname = CoreHub.localCfg.get("rechnung/RnListExportDirname", null);
		Text tDirName;

		public RnListeExportDialog(final Shell shell, final Object[] tree){
			super(shell);
			rnn = new ArrayList<Rechnung>(tree.length);
			for (Object o : tree) {
				if (o instanceof Tree) {
					Tree tr = (Tree) o;
					if (tr.contents instanceof Rechnung) {
						tr = tr.getParent();
					}
					if (tr.contents instanceof Fall) {
						tr = tr.getParent();
					}
					if (tr.contents instanceof Patient) {
						for (Tree tFall : (Tree[]) tr.getChildren().toArray(new Tree[0])) {
							Fall fall = (Fall) tFall.contents;
							for (Tree tRn : (Tree[]) tFall.getChildren().toArray(new Tree[0])) {
								Rechnung rn = (Rechnung) tRn.contents;
								//201512211302js: Rechnungen sollten nicht doppelt im Verarbeitungsergebnis auftreten,
								//nur weil aufgeklappt und dann bis zu 3x etwas vom gleichen Patienten/Fall/Rechnung markiert war.
								if (!rnn.contains(rn)) {		//deshalb prüfen, ob die rechnung schon drin ist, bevor sie hinzugefügt wird.
									rnn.add(rn);
								}
							}
						}
					}
				}
			}
		}

		//ToDo: We probably don't need an overwriting close() method here, because we don't use the text plugin. !!!");
		//20151013js: After copying RnListePrint to RnListeExport, removed most content from this close method.
		//201512210059js: Improved exported fields / content, to reseble what's available in View Rechnungsdetails
		//and meet the requirements for the exported table.
		@Override
		public boolean close(){
			//Call the original overwritten close method?
			boolean ret = super.close();
			return ret;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Control createDialogArea(final Composite parent){
			Composite ret = new Composite(parent, SWT.NONE);
			ret.setLayout(new FillLayout());
			ret.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));

			//201512211432js: Siehe auch Rechnungsdrucker.java public class RechnungsDrucker.createSettingsControl()
			//TODO: Auf Konstante umstellen, dann braucht's allerdings den Austausch weiterer Module bei Installation!!!

			Group cSaveCopy = new Group(ret, SWT.NONE);
			//ToDo: Umstellen auf externe Konstante!
			cSaveCopy.setText("Export als Tabelle in Textdatei: RnListExport-yyyyMMddhhmmss.txt, ColSep=TAB, LineSep=CR, \"um alle Felder\", Multiline-Inhalte in Feldern");
			cSaveCopy.setLayout(new GridLayout(2, false));
			bSaveFileAs = new Button(cSaveCopy, SWT.CHECK);
			//ToDo: Umstellen auf externe Konstante!
			bSaveFileAs.setText("Textdatei erstellen");
			bSaveFileAs.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
			//ToDo: Umstellen auf externe Konstante! - auch noch viel weiter unten
			bSaveFileAs.setSelection(CoreHub.localCfg.get("rechnung/RnListExportDirname_bSaveFileAs", true));
			bSaveFileAs.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					CoreHub.localCfg.set("rechnung/RnListExportDirname_bSaveFileAs", bSaveFileAs.getSelection());
				}
			});
			Button bSelectFile = new Button(cSaveCopy, SWT.PUSH);
			bSelectFile.setText(Messages.RnActions_exportListDirName);
			bSelectFile.setLayoutData(SWTHelper.getFillGridData(2, false, 1, false));
			bSelectFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e){
					DirectoryDialog ddlg = new DirectoryDialog(parent.getShell());
					RnListExportDirname = ddlg.open();
					if (RnListExportDirname == null) {
						SWTHelper.alert(Messages.RnActions_exportListDirNameMissingCaption,
								Messages.RnActions_exportListDirNameMissingText);
					} else {
						//ToDo: Umstellen auf externe Konstante!
						CoreHub.localCfg.set("rechnung/RnListExportDirname", RnListExportDirname);
						tDirName.setText(RnListExportDirname);
					}
				}
			});
			tDirName = new Text(cSaveCopy, SWT.BORDER | SWT.READ_ONLY);
			tDirName.setText(CoreHub.localCfg.get("rechnung/RnListExportDirname", "")); //$NON-NLS-1$
			tDirName.setLayoutData(SWTHelper.getFillGridData(2, true, 1, false));
			return ret;
		}

		@Override
		public void create() {
			super.create();
			getShell().setText(Messages.RnActions_billsList);
			setTitle(Messages.RnActions_exportListCaption);
			setMessage(Messages.RnActions_exportListMessage);
			getShell().setSize(900, 700);
			SWTHelper.center(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),getShell());
		}

		@Override
		protected void okPressed(){
			super.okPressed();
			if (CoreHub.localCfg.get("rechnung/RnListExportDirname_bSaveFileAs", true)) CSVWriteTable();
		}

		public void save(){
		}

		public boolean saveAs(){
			return false;
		}

		OutputStreamWriter CSVWriter;

		//201512210516js: Zur Ausgabe eines Strings mit gestrippten enthaltenen und hinzugefügten umgebenden Anführungszeichen für den CSV-Ouptut
		private void CSVWriteStringSanitized(String s) {
				String a=s;
				a.replaceAll("['\"]", "_");		//Enthaltene Anführungszeichen wegersetzen
				a.trim();

				try {
					CSVWriter.write('"'+a+'"');	//mit umgebenden Anführungszeichen ausgeben	//Grmblfix. In Excel 2003 als ANSI interpretiert -> Umlautfehler.
				}
				catch ( IOException e)
				{
				}
			}

		//201512211312js: Zur Ausgabe eines Spaltentrenners
		private void CSVWriteColSep() {
			try {
				CSVWriter.write("\t");	//mit umgebenden Anführungszeichen ausgeben
			}
			catch ( IOException e)
			{
			}
		}
		//201512211312js: Zur Ausgabe eines Spaltentrenners
		private void CSVWriteLineSep() {
			try {
				CSVWriter.write("\n");	//mit umgebenden Anführungszeichen ausgeben
			}
			catch ( IOException e)
			{
			}
		}
		//201510xxjs, 201512211312js: Produce the export table containing information about the selected bills
		public void  CSVWriteTable() {
			System.out.println("\njs ch.elexis.views.rechnung.RnActions.java: RnListeExportDialog: CSVWriteTable(): begin");
			String RnListExportFileName = new SimpleDateFormat("'RnListExport-'yyyyMMddHHmmss'.txt'").format(new Date()); //kleines hh liefert 12h-Format...

			try {
				//Java speichert intern als UTF-16 und gibt in Elexis standardmässig UTF-8 aus.
				//Excel (zumindest 2003) interpretiert standardmässig wohl als Windows/ANSI und liefert dann kaputte Umlaute.
				//Das gilt für Excel 2003 via drag&drop. Beim Datei-Öffnen erscheint der Dialog mit Optinen zum Import, auch zum Zeichensatz -
				//nur wird auf diesem Weg die Datei völlig zerhackt, weil etwas mit Tabs, Anführungszeichen, Newlines etc. gar nicht funktioniert.
				//Also mache ich hier mal eine Umsetzung nach iso-8859-1.
				//Wenn das NICHT nötig wäre, hätte hier gereicht: FileWriter CSVWriter; CSVWriter= new FileWriter( Dateiname );
				//Tatsächlich liefert Excel aus einer so erzeugten Datei nun korrekte Umlatue; allerdings werden wohl andere Sonderzeichen verloren gehen.
				//N.B.: Auch beim EINlesen sollte man sogleich eine Formatumsetzung auf diesem Wege mit einplanen.
				CSVWriter = new OutputStreamWriter(new FileOutputStream( RnListExportDirname+"/"+RnListExportFileName),"Cp1252");

				//201512211328js: Output Table Headers
				CSVWriteStringSanitized("Aktion?"); CSVWriteColSep();				//201512210402js: Leere Spalte zum Eintragen der gewünschten Aktion.
				CSVWriteStringSanitized("Re.Nr"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.DatumRn"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.DatumVon"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.DatumBis"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.Garant"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.Total"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.Offen"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.StatusLastUpdate"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.Status"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.StatusIsActive"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.StatusText"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.StatusChanges"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.Rejecteds"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.Outputs"); CSVWriteColSep();
				CSVWriteStringSanitized("Re.Payments"); CSVWriteColSep();
				CSVWriteStringSanitized("Fall.AbrSystem"); CSVWriteColSep();
				CSVWriteStringSanitized("Fall.Bezeichnung"); CSVWriteColSep();
				CSVWriteStringSanitized("Fall.Grund"); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.Nr"); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.Name"); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.Vorname"); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.GebDat"); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.LztKonsDat"); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.Balance"); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.GetAccountExcess"); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.BillSummary.Total."); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.BillSummary.Paid"); CSVWriteColSep();
				CSVWriteStringSanitized("Pat.BillSummary.Open");
				CSVWriteLineSep();

				//201512211340js: Produce one line for every rn in rnn
				int i;
				for (i = 0; i < rnn.size(); i++) {
					Rechnung rn = rnn.get(i);
					Fall fall = rn.getFall();
					Patient p = fall.getPatient();

					//201512210402js: Leere Spalte zum Eintragen der gewünschten Aktion.
					//Wenn die Aktion ganz vorne steht, reicht es später einmal, diese einzulesen, um zu wissen, ob man den Rest der Zeile verwerfen kann :-)

					System.out.print("");
					CSVWriteColSep();

					//201512210348js: Erst alles zur betroffenen Rechnung...

					CSVWriteStringSanitized(rn.getNr());
					CSVWriteColSep();
					CSVWriteStringSanitized(rn.getDatumRn());
					CSVWriteColSep();
					CSVWriteStringSanitized(rn.getDatumVon());
					CSVWriteColSep();
					CSVWriteStringSanitized(rn.getDatumBis());
					CSVWriteColSep();

					//Siehe für die Quellen von Rechnungsempfaenger und Status-/-Changes auch RechnungsBlatt.java
					//System.out.print("ToDo:RnEmpfaenger");
					CSVWriteStringSanitized(fall.getGarant().getLabel());
					CSVWriteColSep();
					CSVWriteStringSanitized(rn.getBetrag().toString());
					CSVWriteColSep();
					CSVWriteStringSanitized(rn.getOffenerBetrag().toString());
					CSVWriteColSep();

					{
						long luTime=rn.getLastUpdate();
						Date date=new Date(luTime);
						//ToDo: Support other date formats based upon location or configured settings
				        SimpleDateFormat df2 = new SimpleDateFormat("dd.MM.yyyy");
				        String dateText = df2.format(date);
				        CSVWriteStringSanitized(dateText.toString());
						CSVWriteColSep();

						int st=rn.getStatus();
						CSVWriteStringSanitized(Integer.toString(st));
						CSVWriteColSep();
						if (RnStatus.isActive(st)) {
							CSVWriteStringSanitized("True");
						}
						else {
							CSVWriteStringSanitized("False");
						}
						CSVWriteColSep();
						CSVWriteStringSanitized(RnStatus.getStatusText(st));
						CSVWriteColSep();
					}

					// 201512210310js: New: produce 4 fields, each with multiline content.
					{
						List<String> statuschgs = rn.getTrace(Rechnung.STATUS_CHANGED);
						//Kann leer sein, oder Liefert Ergebnisse wie:
						//[tt.mm.yyyy, hh:mm:ss: s, tt.mm.yy, hh:mm:ss: s, tt.mm.yy, hh:mm:ss: s]
						String a=statuschgs.toString();
						if (a!=null && a.length()>1) {
							//Die Uhrzeiten rauswerfen:
							a=a.replaceAll(", [0-9][0-9]:[0-9][0-9]:[0-9][0-9]", "");
							//", " durch "\n" ersetzen (Man könnte auch noch prüfen, ob danach eine Zahl/ein Datum kommt - die dann aber behalten werden muss.)
							a=a.replaceAll(", ", "\n");
							//Führende und Trailende [] bei der Ausgabe (!) rauswerfen
							CSVWriteStringSanitized(a.substring(1,a.length()-1));
						}
						CSVWriteColSep();
					}

					{
						if (rn.getStatus() == RnStatus.FEHLERHAFT) {
							List<String> rejects = rn.getTrace(Rechnung.REJECTED);
							String a=rejects.toString();
							if (a!=null && a.length()>1) {
								//Die Uhrzeiten rauswerfen:
								a=a.replaceAll(", [0-9][0-9]:[0-9][0-9]:[0-9][0-9]", "");
								//", " durch "\n" ersetzen (Man könnte auch noch prüfen, ob danach eine Zahl/ein Datum kommt - die dann aber behalten werden muss.)
								a=a.replaceAll(", ", "\n");
								//Führende und Trailende [] bei der Ausgabe (!) rauswerfen
								CSVWriteStringSanitized(a.substring(1,a.length()-1));
							}
						}
						CSVWriteColSep();
					}

					{
						List<String> outputs = rn.getTrace(Rechnung.OUTPUT);
						String a=outputs.toString();
						if (a!=null && a.length()>1) {
							//Die Uhrzeiten rauswerfen:
							a=a.replaceAll(", [0-9][0-9]:[0-9][0-9]:[0-9][0-9]", "");
							//", " durch "\n" ersetzen (Man könnte auch noch prüfen, ob danach eine Zahl/ein Datum kommt - die dann aber behalten werden muss.)
							a=a.replaceAll(", ", "\n");
							//Führende und Trailende [] bei der Ausgabe (!) rauswerfen
							CSVWriteStringSanitized(a.substring(1,a.length()-1));
						}
						CSVWriteColSep();
					}

					{
						List<String> payments = rn.getTrace(Rechnung.PAYMENT);
						String a=payments.toString();
						if (a!=null && a.length()>1) {
							//Die Uhrzeiten rauswerfen:
							a=a.replaceAll(", [0-9][0-9]:[0-9][0-9]:[0-9][0-9]", "");
							//", " durch "\n" ersetzen (Man könnte auch noch prüfen, ob danach eine Zahl/ein Datum kommt - die dann aber behalten werden muss.)
							a=a.replaceAll(", ", "\n");
							//Führende und Trailende [] bei der Ausgabe (!) rauswerfen
							CSVWriteStringSanitized(a.substring(1,a.length()-1));
						}
						CSVWriteColSep();
					}

					//201512210348js: Jetzt alles zum betroffenen Fall:
					CSVWriteStringSanitized(fall.getAbrechnungsSystem());
					CSVWriteColSep();
					CSVWriteStringSanitized(fall.getBezeichnung());
					CSVWriteColSep();
					CSVWriteStringSanitized(fall.getGrund());
					CSVWriteColSep();

					//201512210348js: Jetzt alles zum betroffenen Patienten:

					//System.out.print(p.getId());
					//CSVWriteColSep();
					CSVWriteStringSanitized(p.getKuerzel());	//Das liefert die "Patientennummer, da sie frei eingebbar ist, gebe ich sie sanitized aus.
					CSVWriteColSep();
					CSVWriteStringSanitized(p.getName());
					CSVWriteColSep();
					CSVWriteStringSanitized(p.getVorname());
					CSVWriteColSep();
					CSVWriteStringSanitized(p.getGeburtsdatum());
					CSVWriteColSep();

					{
						//ToDo: allenfalls wieder: auf n.a. oder so setzen...
						//ToDo: Ich möcht aber wissen, ob p (dürfte eigentlich nie der Fall sein) oder nk schuld sind, wenn nichts rauskommt.
						//ToDo: Na ja, eigentlich würd ich noch lieber wissen, WARUM da manchmal nichts rauskommt, obwohl eine kons sicher vhd ist.
						String lkDatum = "p==null";
						if (p!=null)	{
							Konsultation lk=p.getLetzteKons(false);
							if (lk!=null) {lkDatum=(lk.getDatum());} else {lkDatum="lk==null";}
							//201512210211js: Offenbar manchmal n.a. - vielleicht heisst das: Kein offener Fall mit Kons? Denn berechnet wurde ja etwas!
						}
						CSVWriteStringSanitized(lkDatum);
						CSVWriteColSep();
					}

					//201512210134js: Money p.getKontostand() und String p.getBalance() liefern (bis auf den Variablentyp) das gleiche Ergebnis
					//System.out.print(p.getKontostand());
					//CSVWriteColSep();
					CSVWriteStringSanitized(p.getBalance());		//returns: String
					CSVWriteColSep();
					CSVWriteStringSanitized(p.getAccountExcess().toString());	//returns: Money
					CSVWriteColSep();

					//201512210146js: Das Folgende ist aus BillSummary - dort wird dafür keine Funktion bereitgestellt,
					//ToDo: Prüfen, ob das eine Redundanz DORT und HIER ist vs. obenn erwähnter getKontostand(), getAccountExcess() etc.
					// maybe called from foreign thread
					{
						String totalText = ""; //$NON-NLS-1$
						String paidText = ""; //$NON-NLS-1$
						String openText = ""; //$NON-NLS-1$

						//Davon, dass p != null ist, darf man eigentlich ausgehen, da ja Rechnungen zu p gehören etc.
						if (p!= null) {
							Money total = new Money(0);
							Money paid = new Money(0);

							List<Rechnung> rechnungen = p.getRechnungen();
							for (Rechnung rechnung : rechnungen) {
								// don't consider canceled bills
								if (rechnung.getStatus() != RnStatus.STORNIERT) {
									total.addMoney(rechnung.getBetrag());
									for (Zahlung zahlung : rechnung.getZahlungen()) {
										paid.addMoney(zahlung.getBetrag());
									}
								}
							}

							Money open = new Money(total);
							open.subtractMoney(paid);
							totalText = total.toString();
							paidText = paid.toString();
							openText = open.toString();
						}

						CSVWriteStringSanitized(totalText);
						CSVWriteColSep();
						CSVWriteStringSanitized(paidText);
						CSVWriteColSep();
						CSVWriteStringSanitized(openText);
						//CSVWriteColSep();		//Nach der letzten Spalte: bitte auch kein TAB mehr ausgeben.
					}

					//Alle Felder zu dieser Rechnung wurden geschrieben - Zeile ist fertig.
					CSVWriteLineSep();
				}
			}
			catch ( IOException e)
			{
			}
			finally
			{
			    try
			    {
			        if ( CSVWriter != null) {
						CSVWriter.close( );
			        }
			    }
			    catch ( IOException e)
			    {
			    }
			}
		}
	}
}
