package gov.ca.water.jdiagram.swing.dialog;

import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.google.common.flogger.FluentLogger;
import gov.ca.water.jdiagram.core.SchematicPanelBase;

public class AddTimeWindowDialog extends JDialog
{
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private final JComboBox<String> dateList;
	private static String[] _monthSelections = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
			"JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

	private JTextField year1, year2;
	private JComboBox<String> month1, month2;
	private String newTW = "OCT1921 - SEP2015";
	private final SchematicPanelBase _schematicPanelBase;

	public AddTimeWindowDialog(JFrame parent, SchematicPanelBase base, JComboBox<String> dateList) {

		super(parent);
		this.dateList = dateList;
		this._schematicPanelBase = base;
		setTitle("Add Time Window");
	}

	/** Shows this dialog, modal to its owner. */
	public void openDialog(){
		setModal(true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		createContents(this);
		setSize(400, 150);
		setLocationRelativeTo(getOwner());
		setVisible(true);
	}

	protected void createContents(final JDialog frame) {
		GridLayout layout=new GridLayout();
		layout.setColumns(5);
		layout.setHgap(20);
		layout.setVgap(20);
		frame.setLayout(layout);

		month1 = new JComboBox<>();
		for (int i = 0; i < _monthSelections.length; i++){
			month1.addItem(_monthSelections[i]);
		}
		month1.setSelectedIndex(9);
		frame.add(month1);

		year1= new JTextField();
		year1.setToolTipText("Year");
		year1.setText("1921");
		frame.add(year1);

		JLabel label1 = new JLabel();
		label1.setText("To");
		frame.add(label1);

		month2 = new JComboBox<>();
		for (int i = 0; i < _monthSelections.length; i++){
			month2.addItem(_monthSelections[i]);
		}
		month2.setSelectedIndex(8);
		frame.add(month2);

		year2= new JTextField();
		year2.setToolTipText("Year");
		year2.setText("2015");
		frame.add(year2);

		JButton ok = new JButton();
		ok.setText("OK");
		ok.addActionListener(e -> {
			int index = validateTW();
			if (index == _schematicPanelBase.getContext().dataSource().getSchematicTwSelections().size()) {
				// Register the window with the host before selecting it: selecting fires the refresh, which
				// looks the date up in getSchematicTwSelections() to decide whether it is a time window
				_schematicPanelBase.getContext().dataSource().getSchematicTwSelections().add(index, newTW);
				saveTWFile();
				dateList.insertItemAt(newTW, index);
				dateList.setSelectedIndex(index);
				frame.dispose();
			} else if (index >= 0) {
				dateList.setSelectedIndex(index);
				frame.dispose();
			}
		});
		frame.add(ok);

		JButton cancel = new JButton();
		cancel.setText("Cancel");
		cancel.addActionListener(e -> frame.dispose());
		frame.add(cancel);
	}

	public int validateTW() {
		int startyear = Integer.parseInt(year1.getText());
		int endyear = Integer.parseInt(year2.getText());
		if (startyear > endyear || (startyear == endyear && month1.getSelectedIndex() > month2.getSelectedIndex())) {
			showWarning();
			return -1;
		} else {
			// getSelectedItem(), not getName(): getName() is the Swing component name (null here)
			newTW = month1.getSelectedItem() + String.valueOf(startyear) + " - " + month2.getSelectedItem() + endyear;
			int size = _schematicPanelBase.getContext().dataSource().getSchematicTwSelections().size();
			int index = size;
			for (int i = 0; i < index; i++){
				if (dateList.getItemAt(i).equals(newTW)){
					return i;
				}
			}
			return index;
		}
	}

	public void showWarning() {
		_schematicPanelBase.getContext().dataSource().showWarning("Start date should be earlier than ending date");
	}

	public void saveTWFile(){
		try {
			File file = new File(_schematicPanelBase.getContext().dataSource().getDataDir(), _schematicPanelBase.getContext().dataSource().getTwFile());
			if (!file.exists()){
				file.createNewFile();
			}
			try (PrintWriter out = new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
				int size = _schematicPanelBase.getContext().dataSource().getSchematicTwSelections().size();
				for (int i = 1; i < size; i++) {
					out.println(_schematicPanelBase.getContext().dataSource().getSchematicTwSelections().get(i));
				}
			}
		} catch (IOException e) {
			logger.atSevere().withCause(e).log("Failed to save time window file");
		}
	}
}
