/*******************************************************************************
 * Copyright (c) 2006-2010, G. Weirich and Elexis
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    G. Weirich - initial implementation
 *    
 *******************************************************************************/

package ch.elexis.core.ui.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.part.ViewPart;

import ch.elexis.core.data.events.ElexisEvent;
import ch.elexis.core.data.events.ElexisEventDispatcher;
import ch.elexis.core.data.events.ElexisEventListener;
import ch.elexis.core.ui.actions.GlobalActions;
import ch.elexis.core.ui.actions.KonsFilter;
import ch.elexis.core.ui.dialogs.KonsFilterDialog;
import ch.elexis.core.ui.events.ElexisUiEventListenerImpl;
import ch.elexis.core.ui.events.RefreshingPartListener;
import ch.elexis.core.ui.icons.Images;
import ch.elexis.core.ui.util.SWTHelper;
import ch.elexis.core.ui.util.ViewMenus;
import ch.elexis.data.Fall;
import ch.elexis.data.Konsultation;
import ch.elexis.data.Patient;

public class KonsListe extends ViewPart implements IRefreshable, ISaveablePart2 {
	public static final String ID = "ch.elexis.HistoryView"; //$NON-NLS-1$
	HistoryDisplay liste;
	Patient actPatient;
	ViewMenus menus;
	private Action filterAction;
	private KonsFilter filter;
	
	private RefreshingPartListener udpateOnVisible = new RefreshingPartListener(this);
	
	private ElexisEventListener eeli_pat = new ElexisUiEventListenerImpl(Patient.class) {	
		public void runInUi(final ElexisEvent ev){
			if (ev.getType() == ElexisEvent.EVENT_SELECTED) {
				if ((actPatient == null)
					|| (!actPatient.getId().equals(((Patient) ev.getObject()).getId()))) {
					actPatient = (Patient) ev.getObject();
					restart(ev);
				}
			} else if (ev.getType() == ElexisEvent.EVENT_DESELECTED) {
				liste.stop();
				liste.load(null, true);
				liste.start(filter);
			}
		}
	};
	
	private ElexisEventListener eeli_fall = new ElexisUiEventListenerImpl(Fall.class) {
		
		public void runInUi(final ElexisEvent ev){
			Fall fall = (Fall) ev.getObject();
			if (fall != null) {
				actPatient = ((Fall) ev.getObject()).getPatient();
			} else {
				actPatient = null;
			}
			restart(ev);
		}
	};
	
	private ElexisEventListener eeli_kons = new ElexisEventListener() {
		private final ElexisEvent eetempl = new ElexisEvent(null, Konsultation.class,
			ElexisEvent.EVENT_UPDATE | ElexisEvent.EVENT_RELOAD | ElexisEvent.EVENT_CREATE
				| ElexisEvent.EVENT_DELETE);
		
		public ElexisEvent getElexisEventFilter(){
			return eetempl;
		}
		
		public void catchElexisEvent(ElexisEvent ev){
			restart(ev);
		}
	};
	
	@Override
	public void createPartControl(final Composite parent){
		parent.setLayout(new GridLayout());
		liste = new HistoryDisplay(parent, getViewSite());
		liste.setLayoutData(SWTHelper.getFillGridData(1, true, 1, true));
		makeActions();
		menus = new ViewMenus(getViewSite());
		menus.createToolbar(GlobalActions.neueKonsAction, filterAction);
		ElexisEventDispatcher.getInstance().addListeners(eeli_fall, eeli_pat, eeli_kons);
		getSite().getPage().addPartListener(udpateOnVisible);
	}
	
	@Override
	public void dispose(){
		getSite().getPage().removePartListener(udpateOnVisible);
		ElexisEventDispatcher.getInstance().removeListeners(eeli_fall, eeli_kons, eeli_pat);
		liste.stop();
		super.dispose();
	}
	
	@Override
	public void setFocus(){
		liste.setFocus();
		refresh();
	}
	
	@Override
	public void refresh(){
		eeli_pat.catchElexisEvent(ElexisEvent.createPatientEvent());		
	}
	
	private void restart(ElexisEvent ev){
		liste.stop();
		liste.load(actPatient, ev);
		liste.start(filter);
	}
	
	private void makeActions(){
		filterAction = new Action(Messages.KonsListe_FilterListAction, Action.AS_CHECK_BOX) { //$NON-NLS-1$
				{
					setImageDescriptor(Images.IMG_FILTER.getImageDescriptor());
					setToolTipText(Messages.KonsListe_FilterListToolTip); //$NON-NLS-1$
				}
				
				@Override
				public void run(){
					if (!isChecked()) {
						filter = null;
					} else {
						KonsFilterDialog kfd = new KonsFilterDialog(actPatient, filter);
						if (kfd.open() == Dialog.OK) {
							filter = kfd.getResult();
						} else {
							kfd = null;
							setChecked(false);
						}
					}
				restart(null);
				}
			};
	}
	
	/*
	 * Die folgenden 6 Methoden implementieren das Interface ISaveablePart2 Wir benötigen das
	 * Interface nur, um das Schliessen einer View zu verhindern, wenn die Perspektive fixiert ist.
	 * Gibt es da keine einfachere Methode?
	 */
	public int promptToSaveOnClose(){
		return GlobalActions.fixLayoutAction.isChecked() ? ISaveablePart2.CANCEL
				: ISaveablePart2.NO;
	}
	
	public void doSave(final IProgressMonitor monitor){ /* leer */
	}
	
	public void doSaveAs(){ /* leer */
	}
	
	public boolean isDirty(){
		return true;
	}
	
	public boolean isSaveAsAllowed(){
		return false;
	}
	
	public boolean isSaveOnCloseNeeded(){
		return true;
	}
	
	/**
	 * Not used
	 * 
	 * @deprecated @since 3.2
	 * @param clazz
	 */
	@Deprecated
	public void reloadContents(final Class clazz){
		if (clazz.equals(Konsultation.class)) {
			restart(null);
		}
		
	}
	
}
