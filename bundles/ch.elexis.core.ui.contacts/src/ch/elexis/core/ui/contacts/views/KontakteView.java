/*******************************************************************************
 * Copyright (c) 2005-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 * 
 *******************************************************************************/

package ch.elexis.core.ui.contacts.views;

import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import ch.elexis.admin.AccessControlDefaults;
import ch.elexis.core.constants.StringConstants;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.status.ElexisStatus;
import ch.elexis.core.data.util.KontaktUtil;
import ch.elexis.core.ui.UiDesk;
import ch.elexis.core.ui.actions.FlatDataLoader;
import ch.elexis.core.ui.actions.GlobalActions;
import ch.elexis.core.ui.actions.PersistentObjectLoader;
import ch.elexis.core.ui.contacts.Activator;
import ch.elexis.core.ui.contacts.actions.ContactActions;
import ch.elexis.core.ui.dialogs.GenericPrintDialog;
import ch.elexis.core.ui.dialogs.KontaktErfassenDialog;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.core.ui.locks.LockedRestrictedAction;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.core.ui.util.ViewMenus;
import ch.elexis.core.ui.util.viewers.CommonViewer;
import ch.elexis.core.ui.util.viewers.DefaultControlFieldProvider;
import ch.elexis.core.ui.util.viewers.DefaultLabelProvider;
import ch.elexis.core.ui.util.viewers.SimpleWidgetProvider;
import ch.elexis.core.ui.util.viewers.ViewerConfigurer;
import ch.elexis.core.ui.util.viewers.ViewerConfigurer.ControlFieldListener;
import ch.elexis.core.ui.views.Messages;
import ch.elexis.data.BezugsKontakt;
import ch.elexis.data.Kontakt;
import ch.elexis.data.Organisation;
import ch.elexis.data.PersistentObject;
import ch.elexis.data.Person;
import ch.elexis.data.Query;
import ch.rgw.tools.StringTool;

public class KontakteView extends ViewPart implements ControlFieldListener, ISaveablePart2 {
	public static final String ID = "ch.elexis.Kontakte"; //$NON-NLS-1$
	private CommonViewer cv;
	private ViewerConfigurer vc;

	IAction dupKontakt, delKontakt, createKontakt, printList,
		tidySelectedAddressesAction,
		copyKontactWithMobileOneLiner,
		copyKontactWithMobile,
		copyKontactWithoutMobileOneLiner,
		copyKontactWithoutMobile,
		copyPostalAddress;

	PersistentObjectLoader loader;

	private final String[] fields = { Kontakt.FLD_SHORT_LABEL + Query.EQUALS + Messages.KontakteView_shortLabel,
			Kontakt.FLD_NAME1 + Query.EQUALS + Messages.KontakteView_text1,
			Kontakt.FLD_NAME2 + Query.EQUALS + Messages.KontakteView_text2,
			Kontakt.FLD_STREET + Query.EQUALS + Messages.KontakteView_street,
			Kontakt.FLD_ZIP + Query.EQUALS + Messages.KontakteView_zip,
			Kontakt.FLD_PLACE + Query.EQUALS + Messages.KontakteView_place };
	private ViewMenus menu;

	public KontakteView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		cv = new CommonViewer();
		loader = new FlatDataLoader(cv, new Query<Kontakt>(Kontakt.class));
		loader.setOrderFields(
				new String[] { Kontakt.FLD_NAME1, Kontakt.FLD_NAME2, Kontakt.FLD_STREET, Kontakt.FLD_PLACE });
		vc = new ViewerConfigurer(loader, new KontaktLabelProvider(), new DefaultControlFieldProvider(cv, fields),
				new ViewerConfigurer.DefaultButtonProvider(),
				new SimpleWidgetProvider(SimpleWidgetProvider.TYPE_LAZYLIST, SWT.MULTI, null));
		cv.create(vc, parent, SWT.NONE, getViewSite());
		makeActions();
		cv.setObjectCreateAction(getViewSite(), createKontakt);
		menu = new ViewMenus(getViewSite());

		tidySelectedAddressesAction = ContactActions.getTidySelectedAddressesAction(cv.getViewerWidget());
		copyKontactWithoutMobile = ContactActions.contactDataWithoutEmail(cv.getViewerWidget());
		copyKontactWithoutMobileOneLiner = ContactActions.contactDataWithoutEmailAsOneliner(cv.getViewerWidget());
		copyKontactWithMobile = ContactActions.contactDataWithEmail(cv.getViewerWidget());
		copyKontactWithMobileOneLiner = ContactActions.contactDataWithEmailAsOneLiner(cv.getViewerWidget());
		menu.createViewerContextMenu(cv.getViewerWidget(), delKontakt, dupKontakt);
		menu.createMenu(tidySelectedAddressesAction);
		menu.createMenu(copyKontactWithoutMobileOneLiner);
		menu.createMenu(copyKontactWithoutMobile);
		menu.createMenu(copyKontactWithMobileOneLiner);
		menu.createMenu(copyKontactWithMobile);
		menu.createMenu(copyPostalAddress);
		menu.createMenu(printList);
		
		menu.createToolbar(tidySelectedAddressesAction);
		menu.createToolbar(copyKontactWithoutMobileOneLiner);
		menu.createToolbar(copyKontactWithoutMobile);
		menu.createToolbar(printList);
		vc.getContentProvider().startListening();
		vc.getControlFieldProvider().addChangeListener(this);
		cv.addDoubleClickListener(new CommonViewer.DoubleClickListener() {
			public void doubleClicked(PersistentObject obj, CommonViewer cv) {
				try {
					getSite().getPage().showView(KontaktDetailView.ID);
					ElexisEventDispatcher.fireSelectionEvent(obj);
					//					kdv.kb.catchElexisEvent(new ElexisEvent(obj, obj.getClass(), ElexisEvent.EVENT_SELECTED));
				} catch (PartInitException e) {
					ElexisStatus es = new ElexisStatus(ElexisStatus.ERROR, Activator.PLUGIN_ID, ElexisStatus.CODE_NONE,
							"Fehler beim Öffnen", e);
					ElexisEventDispatcher.fireElexisStatusEvent(es);
				}

			}
		});
	}

	public void dispose() {
		vc.getContentProvider().stopListening();
		vc.getControlFieldProvider().removeChangeListener(this);
		super.dispose();
	}

	@Override
	public void setFocus() {
		vc.getControlFieldProvider().setFocus();
	}

	public void changed(HashMap<String, String> values) {
		ElexisEventDispatcher.clearSelection(Kontakt.class);
	}

	public void reorder(String field) {
		loader.reorder(field);
	}

	/**
	 * ENTER has been pressed in the control fields, select the first listed
	 * patient
	 */
	// this is also implemented in PatientenListeView
	public void selected() {
		StructuredViewer viewer = cv.getViewerWidget();
		Object[] elements = cv.getConfigurer().getContentProvider().getElements(viewer.getInput());

		if (elements != null && elements.length > 0) {
			Object element = elements[0];
			/*
			 * just selecting the element in the viewer doesn't work if the
			 * control fields are not empty (i. e. the size of items changes):
			 * cv.setSelection(element, true); bug in TableViewer with style
			 * VIRTUAL? work-arount: just globally select the element without
			 * visual representation in the viewer
			 */
			if (element instanceof PersistentObject) {
				// globally select this object
				ElexisEventDispatcher.fireSelectionEvent((PersistentObject) element);
			}
		}
	}

	/*
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir
	 * benötigen das Interface nur, um das Schliessen einer View zu verhindern,
	 * wenn die Perspektive fixiert ist. Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose() {
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL : ISaveablePart2.NO;
	}

	public void doSave(IProgressMonitor monitor) { /* leer */
	}

	public void doSaveAs() { /* leer */
	}

	public boolean isDirty() {
		return true;
	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public boolean isSaveOnCloseNeeded() {
		return true;
	}

	private void makeActions() {
		delKontakt = new LockedRestrictedAction<Kontakt>(AccessControlDefaults.KONTAKT_DELETE,
				Messages.KontakteView_delete) {
			@Override
			public void doRun(Kontakt k) {
				if (SWTHelper.askYesNo("Wirklich löschen?", k.getLabel())) {
					k.delete();
					cv.getConfigurer().getControlFieldProvider().fireChangedEvent();
				}
			}

			@Override
			public Kontakt getTargetedObject() {
				return (Kontakt) cv.getViewerWidgetFirstSelection();
			}
		};
		dupKontakt = new Action(Messages.KontakteView_duplicate) {
			@Override
			public void run() {
				Object[] o = cv.getSelection();
				if (o != null) {
					Kontakt k = (Kontakt) o[0];
					Kontakt dup;
					if (k.istPerson()) {
						Person p = Person.load(k.getId());
						dup = new Person(p.getName(), p.getVorname(), p.getGeburtsdatum(), p.getGeschlecht());
					} else {
						Organisation org = Organisation.load(k.getId());
						dup = new Organisation(org.get(Organisation.FLD_NAME1), org.get(Organisation.FLD_NAME2));
					}
					dup.setAnschrift(k.getAnschrift());
					cv.getConfigurer().getControlFieldProvider().fireChangedEvent();
					// cv.getViewerWidget().refresh();
				}
			}
		};
		createKontakt = new Action(Messages.KontakteView_create) {
			@Override
			public void run() {
				String[] flds = cv.getConfigurer().getControlFieldProvider().getValues();
				String[] predef = new String[] { flds[1], flds[2], StringConstants.EMPTY, flds[3], flds[4], flds[5] };
				KontaktErfassenDialog ked = new KontaktErfassenDialog(getViewSite().getShell(), predef);
				ked.open();
			}
		};

		printList = new Action("Markierte Adressen drucken") {
			{
				setImageDescriptor(Images.IMG_PRINTER.getImageDescriptor());
				setToolTipText("Die in der Liste markierten Kontakte als Tabelle ausdrucken");
			}

			public void run() {
				Object[] sel = cv.getSelection();
				String[][] adrs = new String[sel.length][];
				if (sel != null && sel.length > 0) {
					GenericPrintDialog gpl = new GenericPrintDialog(getViewSite().getShell(), "Adressliste",
							"Adressliste");
					gpl.create();
					for (int i = 0; i < sel.length; i++) {
						Kontakt k = (Kontakt) sel[i];
						String[] f = new String[] { Kontakt.FLD_NAME1, Kontakt.FLD_NAME2, Kontakt.FLD_NAME3,
								Kontakt.FLD_STREET, Kontakt.FLD_ZIP, Kontakt.FLD_PLACE, Kontakt.FLD_PHONE1 };
						String[] v = new String[f.length];
						k.get(f, v);
						adrs[i] = new String[4];
						adrs[i][0] = new StringBuilder(v[0]).append(StringConstants.SPACE).append(v[1])
								.append(StringConstants.SPACE).append(v[2]).toString();
						adrs[i][1] = v[3];
						adrs[i][2] = new StringBuilder(v[4]).append(StringConstants.SPACE).append(v[5]).toString();
						adrs[i][3] = v[6];
					}
					gpl.insertTable("[Liste]", adrs, null);
					gpl.open();
				}
			}
		};
		copyPostalAddress = new Action(Messages.Patient_copyPostalAddressToClipboard) {
			{
				setImageDescriptor(Images.IMG_CLIPBOARD.getImageDescriptor());
				setToolTipText(Messages.Patient_copyPostalAddressToClipboard);
			}
			@Override
			public void run(){
				Object[] sel = cv.getSelection();
				if (sel != null && sel.length > 0) {
					Kontakt k = (Kontakt) sel[0];
					if (k == null) {
						return;
					}
					StringBuffer selectedAddressesText = new StringBuffer();
					selectedAddressesText.append(KontaktUtil.getPostAnschriftPhoneFaxEmail(k, true, false));
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
			};
		};
	}

	class KontaktLabelProvider extends DefaultLabelProvider {

		@Override
		public String getText(Object element) {
			String[] fields = new String[] { Kontakt.FLD_NAME1, Kontakt.FLD_NAME2, Kontakt.FLD_NAME3,
					Kontakt.FLD_STREET, Kontakt.FLD_ZIP, Kontakt.FLD_PLACE, Kontakt.FLD_PHONE1 };
			String[] values = new String[fields.length];
			((Kontakt) element).get(fields, values);
			return StringTool.join(values, StringConstants.COMMA);
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}
}
