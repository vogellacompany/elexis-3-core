/*******************************************************************************
 * Copyright (c) 2005-2011, G. Weirich and Elexis
 * Portions (c) 2012, Joerg M. Sigle (js, jsigle)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    M. Descher - Declarative access to the contextMenu
 *******************************************************************************/

package ch.elexis.core.ui.contacts.views;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.core.constants.Preferences;
import ch.elexis.core.data.activator.CoreHub;
import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.events.ElexisEventListener;
import ch.elexis.core.data.events.Heartbeat.HeartListener;
import ch.elexis.core.data.util.KontaktUtil;
import ch.elexis.core.model.ISticker;
import ch.elexis.core.ui.UiDesk;
import ch.elexis.core.ui.actions.GlobalActions;
import ch.elexis.core.ui.actions.GlobalEventDispatcher;
import ch.elexis.core.ui.actions.IActivationListener;
import ch.elexis.core.ui.actions.RestrictedAction;
import ch.elexis.core.ui.constants.UiResourceConstants;
import ch.elexis.core.ui.contacts.actions.ContactActions;
import ch.elexis.core.ui.contacts.dialogs.PatientErfassenDialog;
import ch.elexis.core.ui.data.UiSticker;
import ch.elexis.core.ui.events.ElexisUiEventListenerImpl;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.core.ui.util.ViewMenus;
import ch.elexis.core.ui.util.viewers.CommonViewer;
import ch.elexis.core.ui.util.viewers.DefaultControlFieldProvider;
import ch.elexis.core.ui.util.viewers.DefaultLabelProvider;
import ch.elexis.core.ui.util.viewers.SimpleWidgetProvider;
import ch.elexis.core.ui.util.viewers.ViewerConfigurer;
import ch.elexis.core.ui.util.viewers.ViewerConfigurer.ControlFieldListener;
import ch.elexis.core.ui.views.Messages;
import ch.elexis.data.Anwender;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Patient;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Person;
import ch.elexis.data.Query;
import ch.elexis.data.Reminder;
import ch.elexis.data.Sticker;
import ch.rgw.tools.StringTool;
import ch.rgw.tools.TimeTool;

/**
 * Display of Patients
 * 
 * @author gerry
 * 
 */
public class PatientenListeView extends ViewPart implements IActivationListener, ISaveablePart2, HeartListener {
	private CommonViewer cv;
	private ViewerConfigurer vc;
	private ViewMenus menus;
	private RestrictedAction newPatAction;
	private IAction filterAction,
			copySelectedPatInfosToClipboardOneLine,
			copySelectedPatInfosToClipboard,
			copyPostalAddress;
	private boolean initiated = false;
	private String[] currentUserFields;
	PatListFilterBox plfb;
	PatListeContentProvider plcp;
	DefaultControlFieldProvider dcfp;
	Composite parent;

	ElexisEventListener eeli_user = new ElexisUiEventListenerImpl(Anwender.class, ElexisEvent.EVENT_USER_CHANGED) {

		@Override
		public void runInUi(ElexisEvent ev) {
			UserChanged();
		}
	};

	@Override
	public void dispose() {
		plcp.stopListening();
		GlobalEventDispatcher.removeActivationListener(this, this);
		ElexisEventDispatcher.getInstance().removeListeners(eeli_user);
		super.dispose();
	}

	/**
	 * retrieve the patient that is currently selected in the list
	 * 
	 * @return the selected patient or null if none was selected
	 */
	public Patient getSelectedPatient() {
		Object[] sel = cv.getSelection();
		if (sel != null) {
			return (Patient) sel[0];
		}
		return null;
	}

	/**
	 * Refresh the contents of the list.
	 */
	public void reload() {
		plcp.invalidate();
		cv.notify(CommonViewer.Message.update);
	}

	@Override
	public void createPartControl(final Composite parent) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;

		this.parent = parent;
		this.parent.setLayout(layout);

		cv = new CommonViewer();

		collectUserFields();
		plcp = new PatListeContentProvider(cv, currentUserFields, this);
		makeActions();
		plfb = new PatListFilterBox(parent);
		plfb.setLayoutData(SWTHelper.getFillGridData(1, true, 1, false));
		((GridData) plfb.getLayoutData()).heightHint = 0;

		dcfp = new DefaultControlFieldProvider(cv, currentUserFields);
		updateFocusField();

		vc = new ViewerConfigurer(
				// new LazyContentProvider(cv,loader,
				// AccessControlDefaults.PATIENT_DISPLAY),
				plcp, new PatLabelProvider(), dcfp, new ViewerConfigurer.DefaultButtonProvider(), // cv,Patient.class),
				new SimpleWidgetProvider(SimpleWidgetProvider.TYPE_LAZYLIST, SWT.SINGLE, cv));
		cv.create(vc, parent, SWT.NONE, getViewSite());
		// let user select patient by pressing ENTER in the control fields
		cv.getConfigurer().getControlFieldProvider().addChangeListener(new ControlFieldSelectionListener());
		cv.getViewerWidget().getControl().setFont(UiDesk.getFont(Preferences.USR_DEFAULTFONT));

		plcp.startListening();
		ElexisEventDispatcher.getInstance().addListeners(eeli_user);
		GlobalEventDispatcher.addActivationListener(this, this);

		populateViewMenu();

		StructuredViewer viewer = cv.getViewerWidget();
		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				PropertyDialogAction pdAction = new PropertyDialogAction(new SameShellProvider(parent),
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite()
								.getSelectionProvider());

				if (pdAction.isApplicableForSelection())
					pdAction.run();
			}
		});
		getSite().registerContextMenu(menus.getContextMenu(), viewer);
		getSite().setSelectionProvider(viewer);

		// // ****DoubleClick Version Marlovits -> öffnet bei DoubleClick die
		// Patienten-Detail-Ansicht
		// cv.addDoubleClickListener(new DoubleClickListener() {
		// @Override
		// public void doubleClicked(PersistentObject obj, CommonViewer cv){
		// try {
		// PatientDetailView2 pdv =
		// (PatientDetailView2)
		// getSite().getPage().showView(PatientDetailView2.ID);
		// } catch (PartInitException e) {
		// ExHandler.handle(e);
		// }
		// }
		// });
	}

	private void updateFocusField() {
		String ff = CoreHub.userCfg.get(Preferences.USR_PATLIST_FOCUSFIELD, null);
		if (ff != null) {
			dcfp.setFocusField(ff);
		}
	}

	private void collectUserFields() {
		ArrayList<String> fields = new ArrayList<String>();
		initiated = !("".equals(CoreHub.userCfg.get(Preferences.USR_PATLIST_SHOWPATNR, "")));
		if (CoreHub.userCfg.get(Preferences.USR_PATLIST_SHOWPATNR, false)) {
			fields.add(Patient.FLD_PATID + Query.EQUALS + Messages.PatientenListeView_PatientNr); // $NON-NLS-1$
		}
		if (CoreHub.userCfg.get(Preferences.USR_PATLIST_SHOWNAME, true)) {
			fields.add(Patient.FLD_NAME + Query.EQUALS + Messages.PatientenListeView_PatientName); // $NON-NLS-1$
		}
		if (CoreHub.userCfg.get(Preferences.USR_PATLIST_SHOWFIRSTNAME, true)) {
			fields.add(Patient.FLD_FIRSTNAME + Query.EQUALS + Messages.PatientenListeView_PantientFirstName); // $NON-NLS-1$
		}
		if (CoreHub.userCfg.get(Preferences.USR_PATLIST_SHOWDOB, true)) {
			fields.add(Patient.BIRTHDATE + Query.EQUALS + Messages.PatientenListeView_PatientBirthdate); // $NON-NLS-1$
		}
		currentUserFields = fields.toArray(new String[fields.size()]);
	}

	private void populateViewMenu(){
		menus = new ViewMenus(getViewSite());
		
		menus.createToolbar(newPatAction, filterAction);
		
		menus.createToolbar(copySelectedPatInfosToClipboardOneLine);
		menus.createToolbar(copySelectedPatInfosToClipboard);
		
		PatientMenuPopulator pmp = new PatientMenuPopulator(
				this, cv.getViewerWidget());
		menus.createControlContextMenu(cv.getViewerWidget().getControl(), pmp);
		menus.getContextMenu().addMenuListener(pmp);
		
		menus.createMenu(newPatAction, filterAction);
		menus.createMenu(copySelectedPatInfosToClipboardOneLine);
		menus.createMenu(copySelectedPatInfosToClipboard);
		menus.createMenu(copyPostalAddress);
	}

	public PatListeContentProvider getContentProvider() {
		return plcp;
	}

	@Override
	public void setFocus() {
		vc.getControlFieldProvider().setFocus();
	}

	class PatLabelProvider extends DefaultLabelProvider implements ITableColorProvider {

		@Override
		public Image getColumnImage(final Object element, final int columnIndex) {
			if (element instanceof Patient) {
				Patient pat = (Patient) element;

				if (Reminder.findRemindersDueFor(pat, CoreHub.actUser, false).size() > 0) {
					return Images.IMG_AUSRUFEZ.getImage();
				}
				ISticker et = pat.getSticker();
				Image im = null;
				if (et != null && (im = new UiSticker((Sticker) et).getImage()) != null) {
					return im;
				} else {
					if (pat.getGeschlecht().equals(Person.MALE)) {
						return Images.IMG_MANN.getImage();
					} else {
						return Images.IMG_FRAU.getImage();
					}
				}
			} else {
				return super.getColumnImage(element, columnIndex);
			}
		}

		@Override
		public Color getBackground(final Object element, final int columnIndex) {
			if (element instanceof Patient) {
				Patient pat = (Patient) element;
				ISticker et = pat.getSticker();
				if (et != null) {
					return UiDesk.getColorFromRGB(et.getBackground());
				}
			}
			return null;
		}

		@Override
		public Color getForeground(final Object element, final int columnIndex) {
			if (element instanceof Patient) {
				Patient pat = (Patient) element;
				ISticker et = pat.getSticker();
				if (et != null) {
					return UiDesk.getColorFromRGB(et.getForeground());
				}
			}

			return null;
		}

	}

	public void reset() {
		vc.getControlFieldProvider().clearValues();
	}

	private void makeActions() {

		filterAction = new Action(Messages.PatientenListeView_FilteList, Action.AS_CHECK_BOX) { // $NON-NLS-1$
			{
				setImageDescriptor(Images.IMG_FILTER.getImageDescriptor());
				setToolTipText(Messages.PatientenListeView_FilterList); // $NON-NLS-1$
			}

			@Override
			public void run() {
				GridData gd = (GridData) plfb.getLayoutData();
				if (filterAction.isChecked()) {
					gd.heightHint = 80;
					plfb.reset();
					plcp.setFilter(plfb);

				} else {
					gd.heightHint = 0;
					plcp.removeFilter(plfb);
				}
				parent.layout(true);

			}

		};

		newPatAction = new RestrictedAction(AccessControlDefaults.PATIENT_INSERT,
				Messages.PatientenListeView_NewPatientAction) {
			{
				setImageDescriptor(Images.IMG_NEW.getImageDescriptor());
				setToolTipText(Messages.PatientenListeView_NewPationtToolTip);
			}

			@Override
			public void doRun() {
				HashMap<String, String> ctlFields = new HashMap<String, String>();
				String[] fx = vc.getControlFieldProvider().getValues();
				int i = 0;
				if (CoreHub.userCfg.get(Preferences.USR_PATLIST_SHOWPATNR, false)) {
					if (i < fx.length) {
						ctlFields.put(Patient.FLD_PATID, fx[i++]);
					}
				}
				if (CoreHub.userCfg.get(Preferences.USR_PATLIST_SHOWNAME, true)) {
					if (i < fx.length) {
						ctlFields.put(Patient.FLD_NAME, fx[i++]);
					}
				}
				if (CoreHub.userCfg.get(Preferences.USR_PATLIST_SHOWFIRSTNAME, true)) {
					if (i < fx.length) {
						ctlFields.put(Patient.FLD_FIRSTNAME, fx[i++]);
					}
				}
				if (CoreHub.userCfg.get(Preferences.USR_PATLIST_SHOWDOB, true)) {
					if (i < fx.length) {
						ctlFields.put(Patient.FLD_DOB, fx[i++]);
					}
				}

				PatientErfassenDialog ped = new PatientErfassenDialog(getViewSite().getShell(), ctlFields);
				if (ped.open() == Dialog.OK) {
					plcp.temporaryAddObject(ped.getResult());
					Patient pat = ped.getResult();
					for (int j = 0; j < currentUserFields.length; j++) {
						String current = currentUserFields[j];
						if (current.startsWith(Patient.FLD_PATID)) {
							dcfp.setValue(j, pat.getPatCode());
						} else if (current.startsWith(Patient.FLD_NAME) && pat.getName()!=null) {
							dcfp.setValue(j, pat.getName());
						} else if (current.startsWith(Patient.FLD_FIRSTNAME) && pat.getVorname()!=null) {
							dcfp.setValue(j, pat.getVorname());
						}
					}
					plcp.syncRefresh();
					TableViewer tv = (TableViewer) cv.getViewerWidget();
					tv.setSelection(new StructuredSelection(pat), true);
				}
			}
		};
		/*
		 * Copy selected PatientInfos as a single line to the clipboard, so it/they can be easily
		 * pasted into a letter for printing.
		 */
		copySelectedPatInfosToClipboardOneLine = new Action(
				Messages.Patient_copySelectedPatInfosToClipboardOneLine) { // $NON-NLS-1$
			{
				setImageDescriptor(Images.IMG_CLIPBOARD.getImageDescriptor());
				setToolTipText(Messages.Patient_copySelectedPatInfosToClipboardOneLine); // $NON-NLS-1$
			}

			@Override
			public void run() {
					patientToClipboard(cv, false);
				}
			};
		/*
		 * Copy selected PatientInfos to the (multiline) clipboard, so it/they can be easily
		 * pasted into a letter for printing.
		 */
		copySelectedPatInfosToClipboard = new Action(
				Messages.Patient_copySelectedPatInfosToClipboard) { // $NON-NLS-1$
			{
				setImageDescriptor(Images.IMG_CLIPBOARD.getImageDescriptor());
				setToolTipText(Messages.Patient_copySelectedPatInfosToClipboard); // $NON-NLS-1$
			}

			@Override
			public void run() {
					patientToClipboard(cv, true);
				}
			};

		copyPostalAddress = new Action(
				Messages.Patient_copySelectedAddressesToClipboard) { // $NON-NLS-1$
			{
				setImageDescriptor(Images.IMG_CLIPBOARD.getImageDescriptor());
				setToolTipText(Messages.Patient_copySelectedAddressesToClipboard); // $NON-NLS-1$
			}

			@Override
			public void run() {
				// Adopted from KontakteView.printList:
				StringBuffer selectedAddressesText = new StringBuffer();

				Object[] sel = cv.getSelection();
				if (sel != null && sel.length > 0) {
					for (int i = 0; i < sel.length; i++) {
						Patient k = (Patient) sel[i];
						selectedAddressesText.append(KontaktUtil.getPostAnschriftPhoneFaxEmail(k,true, true));
						if (i < sel.length - 1) {
							selectedAddressesText.append(System.getProperty("line.separator"));

						}
					}
					Clipboard clipboard = new Clipboard(UiDesk.getDisplay());
					TextTransfer textTransfer = TextTransfer.getInstance();
					Transfer[] transfers = new Transfer[] { textTransfer };
					Object[] data = new Object[] { selectedAddressesText.toString() };
					clipboard.setContents(data, transfers);
					clipboard.dispose();
				}
			};
		};

	}

	@Override
	public void activation(final boolean mode) {
		if (mode == true) {
			newPatAction.reflectRight();
			heartbeat();
			CoreHub.heart.addListener(this);
		} else {
			CoreHub.heart.removeListener(this);
		}
	}

	@Override
	public void visible(final boolean mode) {
		// TODO Auto-generated method stub
	}

	/*
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir
	 * benötigen das Interface nur, um das Schliessen einer View zu verhindern,
	 * wenn die Perspektive fixiert ist. Gibt es da keine einfachere Methode?
	 */
	@Override
	public int promptToSaveOnClose() {
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL : ISaveablePart2.NO;
	}

	@Override
	public void doSave(final IProgressMonitor monitor) { /* leer */
	}

	@Override
	public void doSaveAs() { /* leer */
	}

	@Override
	public boolean isDirty() {
		return GlobalActions.fixLayoutAction.isChecked();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public boolean isSaveOnCloseNeeded() {
		return true;
	}

	@Override
	public void heartbeat() {
		cv.notify(CommonViewer.Message.update);
	}

	/**
	 * Select Patient when user presses ENTER in the control fields. If mor than
	 * one Patients are listed, the first one is selected. (This listener only
	 * implements selected().)
	 */
	class ControlFieldSelectionListener implements ControlFieldListener {
		@Override
		public void changed(HashMap<String, String> values) {
			// nothing to do (handled by LazyContentProvider)
		}

		@Override
		public void reorder(final String field) {
			// nothing to do (handled by LazyContentProvider)
		}

		/**
		 * ENTER has been pressed in the control fields, select the first listed
		 * patient
		 */
		// this is also implemented in KontakteView
		@Override
		public void selected() {
			StructuredViewer viewer = cv.getViewerWidget();
			Object[] elements = cv.getConfigurer().getContentProvider().getElements(viewer.getInput());
			if ((elements != null) && (elements.length > 0)) {
				Object element = elements[0];
				/*
				 * just selecting the element in the viewer doesn't work if the
				 * control fields are not empty (i. e. the size of items
				 * changes): cv.setSelection(element, true); bug in TableViewer
				 * with style VIRTUAL? work-arount: just globally select the
				 * element without visual representation in the viewer
				 */
				if (element instanceof PersistentObject) {
					// globally select this object
					ElexisEventDispatcher.fireSelectionEvent((PersistentObject) element);
				}
			}
		}
	}

	public void UserChanged() {
		if (!initiated)
			SWTHelper.reloadViewPart(UiResourceConstants.PatientenListeView_ID);
		if (!cv.getViewerWidget().getControl().isDisposed()) {
			cv.getViewerWidget().getControl().setFont(UiDesk.getFont(Preferences.USR_DEFAULTFONT));
			cv.notify(CommonViewer.Message.update);

			collectUserFields();
			dcfp.updateFields(currentUserFields, true);
			plcp.updateFields(currentUserFields);

			updateFocusField();
			dcfp.getParent().layout(true);
		}
	}

	/**
	 * 
	 * @param cv CommonViewer
	 * @param multiline if false return one single line
	 */
	private void patientToClipboard(CommonViewer cv, boolean multiline) {
		StringBuffer selectedPatInfosText = new StringBuffer();
		Object[] sel = cv.getSelection();

		if (sel != null && sel.length > 0) {
			for (int i = 0; i < sel.length; i++) {
				Patient k = (Patient) sel[i];
				selectedPatInfosText.append(KontaktUtil.getPersonalia(k, multiline));
				if (i < sel.length - 1) {
					selectedPatInfosText.append(System.getProperty("line.separator"));
				}
			}
		}
		String result = selectedPatInfosText.toString();
		result = result.replaceAll("[\\r\\n]\\n", StringTool.lf); //$NON-NLS-1$
		if (!multiline)  {
			result = result.replaceAll("\\n", "," + StringTool.space); //$NON-NLS-1$
		}
		Clipboard clipboard = new Clipboard(UiDesk.getDisplay());
		TextTransfer textTransfer = TextTransfer.getInstance();
		Transfer[] transfers = new Transfer[] { textTransfer };
		Object[] data = new Object[] { result };
		clipboard.setContents(data, transfers);
		clipboard.dispose();
	}
}
