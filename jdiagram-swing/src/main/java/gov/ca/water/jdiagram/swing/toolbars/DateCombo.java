package gov.ca.water.jdiagram.swing.toolbars;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.google.common.flogger.FluentLogger;
import gov.ca.water.jdiagram.core.utils.TimeOps;
import gov.ca.water.jdiagram.swing.PlatformHarness;
import gov.ca.water.jdiagram.swing.dialog.AddTimeWindowDialog;
import gov.ca.water.jdiagram.swing.views.SchematicPanel;

public final class DateCombo {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private final SchematicPanel schematicView;
	private final PlatformHarness presenter;

	private JComboBox<String> dateList;

	private JPanel control;

	public DateCombo(SchematicPanel schematicPanel, PlatformHarness presenter) {
		this.schematicView = schematicPanel;
		this.presenter = presenter;
	}

	/**
	 * @return the panel holding the date selector, for placing on a toolbar; created on first access
	 */
	public JComponent getControl() {
		if (control == null) {
			control = new JPanel(new GridLayout(1, 1));
			control.setName("dateCombo");
			control.setOpaque(false);
			control.add(getDateList());
			control.setMaximumSize(new Dimension(160, 30));
		}
		return control;
	}

	private JComboBox<String> buildDateList() {
		dateList = new JComboBox<>();
		dateList.setName("dateList");
		dateList.setMaximumSize(new Dimension(160, 30));
		setDateCombo(10, 1921, 9, 3000);
		// Register after populating: JComboBox fires ActionEvents when the first item is added,
		// which would otherwise pop the "Add time window" dialog on startup.
		dateList.addActionListener(e -> {
			schematicView.getBase().getContext().updateSelectedIndex(dateList.getSelectedIndex());
			if (schematicView.getBase().getContext().getSelectedIndex() > 0){
				schematicView.getBase().getContext().setSelectedDateOption((String) dateList.getSelectedItem());
				if (!schematicView.getBase().getContext().getDataSource().isDebugging()) {
					schematicView.refreshValues(0, true);
				} else {
					if (schematicView.getBase().getContext().getDataSource().target() != null) {
						schematicView.refreshValues(1, true);
					}
				}
			} else if (schematicView.getBase().getContext().getSelectedIndex() == 0){
				presenter.launchDialog(() -> {
					Window window = presenter.getShell(dateList);
					JFrame parentFrame = window instanceof JFrame frame ? frame : null;
					AddTimeWindowDialog dialog = new AddTimeWindowDialog(parentFrame, schematicView.getBase(), getDateList());
					dialog.openDialog();
				});
			}
		});
		return dateList;
	}

	public void setDateCombo(int startMonth, int startYear, int endMonth, int endYear) {
		dateList.removeAllItems();
		procTWFile();
		for (int i = 0; i < schematicView.getBase().getContext().getSchematicTwSelections().size(); i++) {
			dateList.addItem(schematicView.getBase().getContext().getSchematicTwSelections().get(i));
		}
		int j = startMonth;
		for (int i = startYear; i <= endYear; i++){
			while (j <= 12){
				dateList.addItem(TimeOps.getMonthText(j) + " " + i);
				if (j >= endMonth && i >= endYear){
					i = endYear + 1;
					j = 13;
				}
				j += 1;
			}
			j = 1;
		}
	}

	/**
	 * @return the date selector, created and populated on first access
	 */
	public JComboBox<String> getDateList(){
		if (dateList == null) {
			dateList = buildDateList();
		}
		return dateList;
	}

	public void procTWFile(){
		try {
			File file = new File(schematicView.getBase().getContext().getDataSource().getDataDir(), schematicView.getBase().getContext().getDataSource().getTwFile());
			if (!file.exists()) {
				file.createNewFile();
			} else {
				schematicView.getBase().getContext().clearSchematicTwSelections();
				schematicView.getBase().getContext().getSchematicTwSelections().add("Add...");
				try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
					String line;
					while((line = br.readLine()) != null) {
						schematicView.getBase().getContext().getSchematicTwSelections().add(line);
					}
				}
			}
		} catch (IOException e) {
			logger.atFine().withCause(e).log("Error processing TW file");
		}
	}
}
