package gov.ca.water.jdiagram.swing.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
	private static final Font FONT = new Font("Arial", Font.PLAIN, 12);

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
		setSize(250, 120);
		setResizable(false);
		setLocationRelativeTo(getOwner());
		setVisible(true);
	}

	private void createContents(final JDialog frame) {
		JPanel content = new JPanel(new BorderLayout(10, 12));
		content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		JPanel inputRow = new JPanel(new BorderLayout(8, 0));
		JLabel label = new JLabel("Decimal Places:");
		inputRow.add(label, BorderLayout.WEST);

		final JTextField decimalText = new JTextField(6);
		decimalText.setText(String.valueOf(schematic.getBase().getContext().core().getPreferences().getInt(DECIMAL_PLACES, 0)));
		decimalText.setHorizontalAlignment(SwingConstants.LEFT);
		decimalText.setFont(FONT);
		label.setLabelFor(decimalText);
		label.setFont(FONT);
		inputRow.add(decimalText, BorderLayout.CENTER);
		content.add(inputRow, BorderLayout.CENTER);

		JButton ok = new JButton("OK");
		ok.setFont(FONT);
		ok.addActionListener(e -> {
			final int numberDecimals = Integer.parseInt(decimalText.getText());
			schematic.getBase().getContext().core().getPreferences().putInt(DECIMAL_PLACES, numberDecimals);
			if (!schematic.getBase().getContext().dataSource().isDebugging()) {
				schematic.refreshValues(0, false);
			} else {
				if (schematic.getBase().getContext().dataSource().target() != null) {
					schematic.refreshValues(1, false);
				}
			}
			if (launcher.mtp() != null) {
				launcher.setMtpDecimalPlaces(numberDecimals);
			}
			frame.dispose();
		});
		JButton cancel = new JButton("Cancel");
		cancel.setFont(FONT);
		cancel.addActionListener(e -> frame.dispose());

		// Give both buttons the same width so they line up neatly.
		Dimension buttonSize = cancel.getPreferredSize();
		Dimension okSize = ok.getPreferredSize();
		buttonSize.width = Math.max(buttonSize.width, okSize.width);
		ok.setPreferredSize(buttonSize);
		cancel.setPreferredSize(buttonSize);

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		buttons.add(ok);
		buttons.add(cancel);
		content.add(buttons, BorderLayout.SOUTH);

		frame.setContentPane(content);
		frame.getRootPane().setDefaultButton(ok);
		frame.pack();
	}
}
