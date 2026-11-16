package gov.ca.water.jdiagram.swing.dialog;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import gov.ca.water.jdiagram.swing.views.SchematicPanel;

public class DecimalDialog extends JDialog
{
	private static String DECIMAL_PLACES = "decimalPlaces";
	private SchematicPanel schematic;

	public DecimalDialog(JFrame parent, SchematicPanel schematic) {
		super(parent);
		this.schematic = schematic;
		setTitle("Set Decimal Places");
	}

	public void openDialog(){
		JFrame frame = new JFrame(getTitle());
		createContents(frame);
		frame.setSize(325, 120);
		frame.setLocation(450, 300);
		frame.setVisible(true);
	}

	protected void createContents(final JFrame frame) {
		GridLayout layout = new GridLayout();
		layout.setColumns(14);
		layout.setVgap(20);
		layout.setHgap(20);
		frame.setLayout(layout);

		JLabel label = new JLabel();
		label.setText("Decimal Places:");
		label.setHorizontalAlignment(JLabel.LEFT);
		frame.add(label);

		final JTextField decimalText = new JTextField();
		frame.add(decimalText);
		decimalText.setText(String.valueOf(schematic.getBase().getHost().getPreferences().getInt(DECIMAL_PLACES, 0)));
		decimalText.setHorizontalAlignment(JTextField.LEFT);

		JButton ok = new JButton();
		ok.setText("OK");
		ok.setHorizontalAlignment(JButton.LEFT);
		ok.addActionListener(e -> {
			final int numberDecimals = Integer.parseInt(decimalText.getText());
			schematic.getBase().getHost().getPreferences().putInt(DECIMAL_PLACES, numberDecimals);
			if (!schematic.getBase().getDataSource().isDebugging()) {
				schematic.refreshValues(0, false);
			} else {
				if (schematic.getBase().getDataSource().target() != null){
					schematic.refreshValues(1, false);
				}
			}
			if (schematic.getBase().getHost().mtp() != null){
				schematic.getBase().getHost().setMtpDecimalPlaces(numberDecimals);
			}
			frame.setVisible(false);
		});
		frame.add(ok);

		JButton cancel = new JButton();
		cancel.setText("Cancel");
		cancel.addActionListener(e -> {
				frame.setVisible(false);
		});
		frame.add(cancel);

		frame.pack();
	}
}
