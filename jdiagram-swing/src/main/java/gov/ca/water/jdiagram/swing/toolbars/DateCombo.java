package gov.ca.water.jdiagram.swing.toolbars;

import java.awt.Composite;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.google.common.flogger.FluentLogger;
import gov.ca.water.jdiagram.swing.views.SchematicPanel;
import gov.ca.water.jdiagram.core.utils.TimeOps;
import gov.ca.water.jdiagram.swing.dialog.AddTimeWindowDialog;

public class DateCombo
{
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private SchematicPanel schematicView;

	private JComboBox<String> dateList;

	public DateCombo(SchematicPanel schematicPanel) {
		this.schematicView = schematicPanel;
	}

	protected JPanel createControl(JPanel parent) {
		JPanel container = new JPanel();
		GridLayout glContainer = new GridLayout(0, 1);
		glContainer.setHgap(0);
		glContainer.setVgap(0);
		container.setLayout(glContainer);
		dateList = new JComboBox<>();
		container.add(dateList);
		setDateCombo(10, 1921, 9, 3000);
		dateList.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				schematicView.getBase().getHost().updateSelectedIndex(dateList.getSelectedIndex());
				if (schematicView.getBase().getHost().getSelectedIndex() > 0){
					schematicView.getBase().getHost().setSelectedDate((String) dateList.getSelectedItem());
					if (!schematicView.getBase().getDataSource().isDebugging()) {
						schematicView.refreshValues(0, true);
					} else {
						if (schematicView.getBase().getDataSource().target() != null) {
							schematicView.refreshValues(1, true);
						}
					}
				} else if (schematicView.getBase().getHost().getSelectedIndex() == 0){
					schematicView.getBase().getHost().launchDialog(() -> {
						Composite window = schematicView.getBase().getHost().getShell(dateList);
						JFrame parentFrame = window instanceof JFrame ? (JFrame) window : null;
						AddTimeWindowDialog dialog = new AddTimeWindowDialog(parentFrame, schematicView.getBase(), getDateList());
						dialog.openDialog();
					});
				}
			}

		});
		parent.add(container);
		return container;
	}

	public void setDateCombo(int startMonth, int startYear, int endMonth, int endYear) {
		dateList.removeAll();
		procTWFile();
		for (int i = 0; i < schematicView.getBase().getHost().getSchematicTwSelections().size(); i++){
			dateList.addItem(schematicView.getBase().getHost().getSchematicTwSelections().get(i));
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

	public JComboBox<String> getDateList(){
		return dateList;
	}

	public void procTWFile(){
		try {
			File file = new File(schematicView.getBase().getDataSource().getDataDir(), schematicView.getBase().getHost().getTwFile());
			if (!file.exists()){
				file.createNewFile();
			} else {
				schematicView.getBase().getHost().clearSchematicTwSelections();
				schematicView.getBase().getHost().getSchematicTwSelections().add("Add...");
				FileInputStream fs = new FileInputStream(file.getAbsolutePath());
				BufferedReader br = new BufferedReader(new InputStreamReader(fs));
				String line;
				while((line = br.readLine()) != null){
					schematicView.getBase().getHost().getSchematicTwSelections().add(line);
				}
			}
		} catch (Exception e) {
			logger.atFine().withCause(e).log("Error processing TW file");
		}
	}
}
