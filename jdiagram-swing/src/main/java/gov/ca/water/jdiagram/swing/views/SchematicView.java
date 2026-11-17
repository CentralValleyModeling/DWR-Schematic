package gov.ca.water.jdiagram.swing.views;

import gov.ca.water.jdiagram.core.SchematicPanelBase;
import gov.ca.water.jdiagram.swing.DataPresenter;

public class SchematicView extends SchematicPanel
{
	public static final String ID = SchematicView.class.getCanonicalName();

	public SchematicView(SchematicPanelBase panelBase, DataPresenter presenter) {
		super(panelBase, presenter);
	}
}
