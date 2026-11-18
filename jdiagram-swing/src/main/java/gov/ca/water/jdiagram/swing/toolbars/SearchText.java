package gov.ca.water.jdiagram.swing.toolbars;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import gov.ca.water.jdiagram.swing.views.SchematicPanel;

/**
 * Toolbar control with a "Search" button and text field that brings the first matching node into view.
 */
public class SearchText extends JPanel
{
	private final transient SchematicPanel schematicView;
	private final JTextField text;
	private final JButton search;

	public SearchText(SchematicPanel schematicPanel) {
		super(new GridLayout(1, 2, 0, 0));
		this.schematicView = schematicPanel;

		search = new JButton("Search");
		search.setName("searchButton");
		search.setFont(new Font("Arial", Font.PLAIN, 10));

		text = new JTextField();
		text.setName("searchField");
		text.setHorizontalAlignment(SwingConstants.CENTER);
		text.setColumns(12);

		ActionListener doSearch = e -> schematicView.findInView(text.getText());
		search.addActionListener(doSearch);
		text.addActionListener(doSearch);

		add(search);
		add(text);
		setMaximumSize(new Dimension(260, 30));
	}

	public JTextField getTextField() {
		return text;
	}

	public JButton getSearchButton() {
		return search;
	}
}
