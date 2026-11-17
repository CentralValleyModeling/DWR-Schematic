package gov.ca.water.jdiagram.swing.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.table.AbstractTableModel;

import com.mindfusion.diagramming.DiagramItem;

/**
 * Two-column (Property, Value) table model showing the properties of the selected
 * diagram item. Swing replacement for the Eclipse Properties view.
 * <p>
 * Usage:
 * <pre>
 * DiagramPropertiesTableModel model = new DiagramPropertiesTableModel();
 * schematicPanel.getSelectionProvider().addSelectionChangedListener(model);
 * propertiesContainer.add(new JScrollPane(new JTable(model)));
 * </pre>
 */
public class DiagramPropertiesTableModel extends AbstractTableModel
		implements DiagramSelectionProvider.SelectionChangedListener
{
	private static final String[] COLUMNS = {"Property", "Value"};

	private final List<Map.Entry<String, String>> rows = new ArrayList<>();

	@Override
	public void selectionChanged(DiagramSelectionProvider.SelectionChangedEvent event)
	{
		// DiagramSelectionProvider already delivers events on the EDT
		setItem(event.getFirstElement());
	}

	public void setItem(DiagramItem item)
	{
		rows.clear();
		if (item != null)
		{
			rows.addAll(new DiagramItemProperties(item).asMap().entrySet());
		}
		fireTableDataChanged();
	}

	@Override
	public int getRowCount()
	{
		return rows.size();
	}

	@Override
	public int getColumnCount()
	{
		return COLUMNS.length;
	}

	@Override
	public String getColumnName(int column)
	{
		return COLUMNS[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		Map.Entry<String, String> row = rows.get(rowIndex);
		return columnIndex == 0 ? row.getKey() : row.getValue();
	}
}
