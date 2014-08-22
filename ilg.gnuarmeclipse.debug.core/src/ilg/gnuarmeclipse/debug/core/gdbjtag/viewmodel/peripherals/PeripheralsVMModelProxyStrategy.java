/*******************************************************************************
 * Copyright (c) 2014 Liviu Ionescu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Liviu Ionescu - initial version
 *******************************************************************************/

package ilg.gnuarmeclipse.debug.core.gdbjtag.viewmodel.peripherals;

import org.eclipse.cdt.dsf.ui.viewmodel.AbstractVMProvider;
import org.eclipse.cdt.dsf.ui.viewmodel.DefaultVMModelProxyStrategy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ICheckboxModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.viewers.TreePath;

@SuppressWarnings("restriction")
public class PeripheralsVMModelProxyStrategy extends
		DefaultVMModelProxyStrategy implements ICheckboxModelProxy {

	// ------------------------------------------------------------------------

	public PeripheralsVMModelProxyStrategy(AbstractVMProvider provider,
			Object rootElement) {
		super(provider, rootElement);
	}

	@Override
	public boolean setChecked(IPresentationContext context, Object viewerInput,
			TreePath path, boolean checked) {
		// TODO Auto-generated method stub
		return false;
	}

	// ------------------------------------------------------------------------

}
