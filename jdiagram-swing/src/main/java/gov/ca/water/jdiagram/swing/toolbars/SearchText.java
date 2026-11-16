package gov.ca.water.jdiagram.swing.toolbars;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextField;

import gov.ca.water.jdiagram.swing.views.SchematicPanel;

public class SearchText extends JComponent
{
	SchematicPanel schematicView;
	private JTextField text;
	private JButton search;
	private Font font;

	public SearchText(SchematicPanel schematicPanelBase) {
		this.schematicView = schematicPanelBase;
	}

	protected JFrame createControl(JFrame parent) {
		JFrame container = new JFrame();
		GridLayout glContainer = new GridLayout(1, 2);
		glContainer.setHgap(0);
		glContainer.setVgap(0);
		container.setLayout(glContainer);
		search = new JButton();
		search.setText("Search");
		font = new Font("Arial", Font.PLAIN, 10);
		search.setFont(font);
		search.addMouseListener(new MouseListener(){

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseClicked(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				String searchText=text.getText();
				schematicView.findInView(searchText);
			}
		});
		container.add(search);
		text = new JTextField();
		text.setText("");
		text.setHorizontalAlignment(JTextField.CENTER);
		text.setBounds(0, 0, 140, 15);
		container.add(text);
		parent.add(container);
		return container;
	}
}
