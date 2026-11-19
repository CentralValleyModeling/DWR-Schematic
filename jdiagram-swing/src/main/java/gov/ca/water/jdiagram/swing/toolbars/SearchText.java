package gov.ca.water.jdiagram.swing.toolbars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import gov.ca.water.jdiagram.swing.views.SchematicPanel;

/**
 * Toolbar control with a "Search" button and text field. Search (or Enter) selects the next node whose text
 * contains the entered string and scrolls it into view; repeating it cycles through matches. When nothing
 * matches, the field turns pink until the text is edited.
 */
public class SearchText extends JPanel
{
	/** Background of the search field when the last search found nothing. */
	public static final Color NOT_FOUND = new Color(255, 205, 205);

	private final transient SchematicPanel schematicView;
	private final JTextField text;
	private final JButton search;
	private final Color normalBackground;
	private boolean lastSearchFound = true;

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
		text.setToolTipText("Text to find; press Enter or Search again for the next match");
		normalBackground = text.getBackground();

		ActionListener doSearch = e -> runSearch();
		search.addActionListener(doSearch);
		text.addActionListener(doSearch);
		text.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				clearNotFound();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				clearNotFound();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// attribute changes only
			}
		});

		add(search);
		add(text);
		setMaximumSize(new Dimension(260, 30));
	}

	private void runSearch() {
		String query = text.getText();
		lastSearchFound = query.isBlank() || schematicView.findInView(query);
		text.setBackground(lastSearchFound ? normalBackground : NOT_FOUND);
		text.requestFocusInWindow();
	}

	private void clearNotFound() {
		if (!lastSearchFound) {
			lastSearchFound = true;
			text.setBackground(normalBackground);
		}
	}

	/** @return false if the most recent search found no match */
	public boolean isLastSearchFound() {
		return lastSearchFound;
	}

	public JTextField getTextField() {
		return text;
	}

	public JButton getSearchButton() {
		return search;
	}
}
