package gov.ca.water.jdiagram.swing.dialog;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import gov.ca.water.jdiagram.swing.DataPresenter;
import gov.ca.water.jdiagram.swing.views.SchematicPanel;

public final class DecimalDialog extends JDialog
{
	private static String DECIMAL_PLACES = "decimalPlaces";
	private final SchematicPanel schematic;
	private final transient DataPresenter presenter;

	public DecimalDialog(JFrame parent, SchematicPanel schematic, DataPresenter presenter) {
		super(parent);
		this.schematic = schematic;
		this.presenter = presenter;
		setTitle("Set Decimal Places");
	}

	/** Shows this dialog, modal to its owner. */
	public void openDialog(){
		setModal(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		createContents(this);
		setSize(325, 120);
		setLocationRelativeTo(getOwner());
		setVisible(true);
	}

	protected void createContents(final JDialog frame) {
		GridLayout layout = new GridLayout();
		layout.setColumns(14);
		layout.setVgap(20);
		layout.setHgap(20);
		frame.setLayout(layout);

		JLabel label = new JLabel();
		label.setText("Decimal Places:");
		label.setHorizontalAlignment(SwingConstants.LEFT);
		frame.add(label);

		final JTextField decimalText = new JTextField();
		frame.add(decimalText);
		decimalText.setText(String.valueOf(schematic.getBase().getHost().getPreferences().getInt(DECIMAL_PLACES, 0)));
		decimalText.setHorizontalAlignment(SwingConstants.LEFT);

		JButton ok = new JButton();
		ok.setText("OK");
		ok.setHorizontalAlignment(SwingConstants.LEFT);
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
			if (presenter.mtp() != null){
				presenter.setMtpDecimalPlaces(numberDecimals);
			}
			frame.dispose();
		});
		frame.add(ok);

		JButton cancel = new JButton();
		cancel.setText("Cancel");
		cancel.addActionListener(e -> frame.dispose());
		frame.add(cancel);

		frame.pack();
	}
}
