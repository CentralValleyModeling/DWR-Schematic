package gov.ca.water.jdiagram.swing.dialog;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import gov.ca.water.jdiagram.swing.UILauncher;
import gov.ca.water.jdiagram.swing.views.SchematicPanel;

/**
 * Dialog for selecting number of decimal places to display.
 */
public final class DecimalDialog extends JDialog
{
	private static String DECIMAL_PLACES = "decimalPlaces";
	private final SchematicPanel schematic;
	private final transient UILauncher launcher;

	/**
	 * Creates a new DecimalDialog. Uses JFrame as replacement for Eclipse Shell.
	 * @param parent the parent frame
	 * @param schematic the schematic panel
	 * @param launcher the UI presenter
	 */
	public DecimalDialog(JFrame parent, SchematicPanel schematic, UILauncher launcher) {
		super(parent);
		this.schematic = schematic;
		this.launcher = launcher;
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

	private void createContents(final JDialog frame) {
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
		decimalText.setText(String.valueOf(schematic.getBase().getContext().core().getPreferences().getInt(DECIMAL_PLACES, 0)));
		decimalText.setHorizontalAlignment(SwingConstants.LEFT);

		JButton ok = new JButton();
		ok.setText("OK");
		ok.setHorizontalAlignment(SwingConstants.LEFT);
		ok.addActionListener(e -> {
			final int numberDecimals = Integer.parseInt(decimalText.getText());
			schematic.getBase().getContext().core().getPreferences().putInt(DECIMAL_PLACES, numberDecimals);
			if (!schematic.getBase().getContext().dataSource().isDebugging()) {
				schematic.refreshValues(0, false);
			} else {
				if (schematic.getBase().getContext().dataSource().target() != null){
					schematic.refreshValues(1, false);
				}
			}
			if (launcher.mtp() != null){
				launcher.setMtpDecimalPlaces(numberDecimals);
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
