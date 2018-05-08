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

package ch.elexis.core.ui.contacts.actions;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.elexis.core.data.beans.ContactBean;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.util.KontaktUtil;
import ch.elexis.core.ui.UiDesk;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.core.ui.views.Messages;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.rgw.tools.StringTool;

public class ContactActions {
	private static int nrChanges;
	private static Logger log = LoggerFactory.getLogger(ContactActions.class);
	
	private static void setNrChanges(int x){
		nrChanges = x;
	}
	
	/**
	 * TODO: Should each field be capable of cleaning its content ? (Jörg Sigle & Niklaus Giger)
	 * <br>
	 * TODO: We must find a way to handle different languages + research actual content of database
	 * columns <br>
	 * TODO: Configurability following preferences of diffferent users <br>
	 * <br>
	 * please note if at least one field of a contact is changed, all fields of the contact will be
	 * appended to the clipboard. The result can be pasted into a spreadshead, and a macro exists to
	 * highlight then changed fields This allows checking whether your algorithm is good or not <br>
	 * Clean selected address(es): <br>
	 * * For all selected addresses do: <br>
	 * * If FLD_IS_PATIENT==true, then set FLD_IS_PERSON=true (otherwise, invalid xml invoices may
	 * be produced, addressed to institutions instead of persons) <br>
	 * * For each address field: remove leading and trailing spaces.
	 * 
	 * @param iSelection
	 *            CommonViewer
	 */
	public static Action getTidySelectedAddressesAction(StructuredViewer viewer){
		/*
		 * TODO: Should each field be capable of cleaning its content ? (Jörg Sigle &
		 * Niklaus Giger) TODO: We must find a way to handle different languages +
		 * research actual content of database columns TODO: Configurability following
		 * preferences of diffferent users
		 * 
		 * @remark please note if at least one field of a contact is changed, all fields
		 * of the contact will be appended to the clipboard. The result can be pasted
		 * into a spreadshead, and a macro exists to highlight then changed fields This
		 * allows checking whether your algorithm is good or not
		 * 
		 * Clean selected address(es): For all selected addresses do: If
		 * FLD_IS_PATIENT==true, then set FLD_IS_PERSON=true (otherwise, invalid xml
		 * invoices may be produced, addressed to institutions instead of persons) For
		 * each address field: remove leading and trailing spaces.
		 */
		Action tidySelectedAddressesAction =
			new Action(Messages.KontakteView_tidySelectedAddresses) {
				{
					setImageDescriptor(Images.IMG_WIZARD.getImageDescriptor());
					setToolTipText(Messages.KontakteView_tidySelectedAddresses);
				}

				@Override
				public void run(){
					log.info("tidySelectedAddresses started"); //$NON-NLS-1$
					IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
					Object[] sel = selection.toArray();
					StringBuffer SelectedContactInfosChangedList = new StringBuffer();
					
					ProgressMonitorDialog dialog = new ProgressMonitorDialog(null);
					try {
						dialog.run(true, true, new IRunnableWithProgress() {
							public void run(IProgressMonitor monitor){
								String text = "Kontaktadressen putzen";
								int nrChanged = 0;
								if (sel != null && sel.length > 0) {
									monitor.beginTask(text, sel.length);
									for (int i = 0; i < sel.length; i++) {
										if (monitor.isCanceled()) {
											monitor.done();
											log.debug("tidySelectedAddressesAction cancelled");
											break;
										}
										if (i % 100 == 0) {
											log.debug("KontakteView tidySelectedAddressesAction.run Processing entry {} of {}  ", i, sel.length);
										}
										monitor.worked(1);
										monitor.setTaskName(String.format(
											"%s von %s Kontaktaddressen geputzt", i, sel.length));
										Kontakt k = getKontactFromSelected(sel[i]);
										if (k == null) {
											break;
										}
										StringBuffer changed = KontaktUtil.tidyContactInfo(k);
										if (changed.length() > 0) {
											nrChanged++;
											setNrChanges(nrChanged);
											SelectedContactInfosChangedList.append(changed);
										}
										nrChanges = nrChanged;
									}
								} // if sel not empty
							}
						});
					} catch (InvocationTargetException | InterruptedException e) {
						log.info("Error in tidySelectedAddresses" + e.getMessage()); //$NON-NLS-1$
						e.printStackTrace();
					}
					
					/*
					 * In order to export the list of addresses that might warrant a manual review
					 * of Postadresse to the clipboard, I have added the clipboard export routine
					 * also used in the copyToClipboard... methods further below. If not for this
					 * purpose, building up the stringsBuffer content would not have been required,
					 * and neither would have been any kind of clipboard interaction.
					 *
					 * I would prefer to move the following code portions down behind the
					 * "if sel not empty" block, so that (a) debugging output can be produced and
					 * (b) the clipboard will be emptied when NO Contacts have been selected. I did
					 * this to avoid the case where a user would assume they had selected some
					 * address, copied data to the clipboard, and pasted them - and, even when they
					 * erred about their selection, which was indeed empty, they would not
					 * immediately notice that because some (old, unchanged) content would still
					 * come out of the clipboard.
					 * 
					 * But if I do so, and there actually is no address selected, I get an error
					 * window: Unhandled Exception ... not valid. So to avoid that message without
					 * any further research (I need to get this work fast now), I move the code back
					 * up and leave the clipboard unchanged for now, if no Contacts had been
					 * selected to process.
					 * 
					 */
					
					// Copy some generated object.toString() to the clipoard
					if ((SelectedContactInfosChangedList != null)
						&& (SelectedContactInfosChangedList.length() > 0)) {
						
						Clipboard clipboard = new Clipboard(UiDesk.getDisplay());
						TextTransfer textTransfer = TextTransfer.getInstance();
						Transfer[] transfers = new Transfer[] {
							textTransfer
						};
						Object[] data = new Object[] {
							SelectedContactInfosChangedList.toString()
						};
						clipboard.setContents(data, transfers);
						clipboard.dispose();
					}
					Kontakt k = getKontactFromSelected(selection.getFirstElement());
					if (k != null) {
						ElexisEventDispatcher.fireSelectionEvent(Kontakt.load(k.getId()));
					}
					
					String msgTitle =
						String.format("Putzen von %s Kontaktadressen abgeschlossen", sel.length);
					if (sel.length > 1) {
						MessageDialog.openInformation(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
							msgTitle,
							String.format("%s der %s ausgewählten Kontakte wurden verändert.\n"
								+ "Veränderte Einträge (vorher/nacher) wurden in die Zwischenablage kopiert.\n\n"
								+ "Diese können Sie in eine Tabellenkalkulation (OpenOffice Calc oder Excel) einfügen.",
								nrChanges, sel.length));
					}
					log.debug(msgTitle);
				}
			};
		return tidySelectedAddressesAction;
	}
	
	/**
	 * Copy selected address(es)  WITH email (multiline) to the clipboard,
	 * so it/they can be easily pasted into a letter for printing.
	 * 
	 * @param cv
	 *            CommonViewer
	 */
	public static Action postalAddress(StructuredViewer viewer){
		Action myAction =
			new Action(Messages.Patient_copyPostalAddressToClipboard) {
				{
					setImageDescriptor(Images.IMG_CLIPBOARD.getImageDescriptor());
					setToolTipText(Messages.Patient_copyPostalAddressToClipboard);
				}
				
				@Override
				public void run(){
					StringBuffer selectedAddressesText = new StringBuffer();
					IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
					Object[] sel = selection.toArray();
					if (sel != null && sel.length > 0) {
						Kontakt k = getKontactFromSelected(sel[0]);
						if (k == null) {
							return;
						}
						/*
						 * Synthesize the address lines to output from the entries in Kontakt k;
						 * added to implement the output format desired for the copyAddressToClipboard()
						 * buttons added to version 2.1.6.js as of 2012-01-28ff
						 *
						 * We might synthesize our own "Anschrift" for each Kontakt,
						 * completely according to our own requirements,
						 * OR use any of the methods defined for Kontakt like:
						 * getLabel...(), getPostAnschrift, createStandardAnschrift, List<BezugsKontakt>... -
						 * 
						 * The Declaration of Kontakt with field definitions is available in Kontakt.java, please look
						 * therein for additional details, please. Click-Right -> Declaration on Kontakt in Eclipse works.
						 * You can also look above to see the fields that printList would use.
						 */
						
						//getPostAnschriftPhoneFaxEmail() already returns a line separator after the address
						selectedAddressesText
							.append(KontaktUtil.getPostAnschriftPhoneFaxEmail(k, false, false));
						
						selectedAddressesText.append(System.getProperty("line.separator"));
						Clipboard clipboard = new Clipboard(UiDesk.getDisplay());
						TextTransfer textTransfer = TextTransfer.getInstance();
						Transfer[] transfers = new Transfer[] {
							textTransfer
						};
						Object[] data = new Object[] {
							selectedAddressesText.toString()
						};
						clipboard.setContents(data, transfers);
						clipboard.dispose();
					}
				}
		};
		return myAction;
	}
	/**
	 * Copy selected address(es)  WITH email (multiline) to the clipboard,
	 * so it/they can be easily pasted into a letter for printing.
	 * 
	 * @param cv
	 *            CommonViewer
	 */
	public static Action contactDataWithEmail(StructuredViewer viewer){
		Action myAction =
			new Action(Messages.Kontakte_copySelectedAddressesToClipboard) {
				{
					setImageDescriptor(Images.IMG_CLIPBOARD.getImageDescriptor());
					setToolTipText(Messages.Kontakte_copySelectedAddressesToClipboard);
				}
				@Override
				public void run(){
					setClipboard(viewer, true, true);
				}
		};
		return myAction;
	}

	/**
	 * Copy selected address(es) WITH email as oneliner to the clipboard,
	 * so it/they can be easily pasted into a letter for printing.
	 * 
	 * @param cv
	 *            CommonViewer
	 */
	public static Action contactDataWithEmailAsOneLiner(StructuredViewer viewer){
		Action myAction =
			new Action(Messages.Kontakte_copyDataWithMobileOneLine) {
				{
					setImageDescriptor(Images.IMG_CLIPBOARD.getImageDescriptor());
					setToolTipText(Messages.Kontakte_copyDataWithMobileOneLine);
				}
				
				@Override
				public void run(){
					setClipboard(viewer, false, true);
				};
			};
		return myAction;
	}
	
	/**
	 * Copy selected address(es)  WITHOUT email (multiline) to the clipboard,
	 * so it/they can be easily pasted into a letter for printing.
	 * 
	 * @param cv
	 *            CommonViewer
	 */
	public static Action contactDataWithoutEmail(StructuredViewer viewer){
		Action myAction =
			new Action(Messages.Kontakte_copyDataWithoutMobile) {
				{
					setImageDescriptor(Images.IMG_CLIPBOARD.getImageDescriptor());
					setToolTipText(Messages.Kontakte_copySelectedContactInfosToClipboard);
				}
				
				@Override
				public void run(){
					setClipboard(viewer, true, false);
				};
			};
		return myAction;
	}

	/**
	 * Copy selected address(es)  WITHOUT email (oneliner) to the clipboard,
	 * so it/they can be easily pasted into a letter for printing.
	 * 
	 * @param cv
	 *            CommonViewer
	 */
	public static Action contactDataWithoutEmailAsOneliner(StructuredViewer viewer){
		Action myAction =
			new Action(Messages.Kontakte_copyDataWithoutMobileOneLine) {
				{
					setImageDescriptor(Images.IMG_CLIPBOARD.getImageDescriptor());
					setToolTipText(Messages.Kontakte_copySelectedContactInfosToClipboardOneLine);
				}
				@Override
				public void run(){
					setClipboard(viewer, false, false);
				};
			};
		return myAction;
	}

	private static Kontakt getKontactFromSelected(Object obj){
		Kontakt k = null;
		if (obj.getClass().equals(Kontakt.class)) {
			return (Kontakt) obj;
		} else if (obj.getClass().equals(ContactBean.class)) {
			ContactBean bean = (ContactBean) obj;
			if (bean.isPatient()) {
				return Patient.loadByPatientID(bean.getPatientNr());
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	/**
	 * 
	 * @param viewer
	 * @param multiline
	 * @param including_phone
	 */
	private static void setClipboard(StructuredViewer viewer, boolean multiline, boolean including_phone) {
		StringBuffer selectedAddressesText = new StringBuffer();
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		Object[] sel = selection.toArray();
		if (sel != null && sel.length > 0) {
			for (int i = 0; i < sel.length; i++) {
				Kontakt k = getKontactFromSelected(sel[i]);
				if (k == null) {
					break;
				}
				//getPostAnschriftPhoneFaxEmail() already returns a line separator after the address
				selectedAddressesText
					.append(KontaktUtil.getPostAnschriftPhoneFaxEmail(k, multiline, including_phone));
				
				if (i < sel.length - 1) {
					selectedAddressesText.append(System.getProperty("line.separator"));
					
				}
			}
			Clipboard clipboard = new Clipboard(UiDesk.getDisplay());
			TextTransfer textTransfer = TextTransfer.getInstance();
			Transfer[] transfers = new Transfer[] {
				textTransfer
			};
			Object[] data = new Object[] {
				selectedAddressesText.toString()
			};
			clipboard.setContents(data, transfers);
			clipboard.dispose();
		}
	}
}
