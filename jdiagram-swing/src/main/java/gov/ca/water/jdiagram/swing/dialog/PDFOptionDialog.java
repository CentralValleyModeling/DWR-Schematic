package gov.ca.water.jdiagram.swing.dialog;

import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.export.PdfExporter;
import com.mindfusion.pdf.AutoScale;
import com.mindfusion.pdf.PageSizesEnum;
import gov.ca.water.jdiagram.core.interfaces.SchematicDataSource;

public final class PDFOptionDialog extends JDialog
{
	private final String filename;
	private final transient SchematicDataSource dataSource;
	private final transient Diagram diagram;
	private final String[] pageSizeNames = {"A0",
			"A1",
			"A10",
			"A2",
			"A3",
			"A4",
			"A5",
			"A6",
			"A7",
			"A8",
			"A9",
			"ArchA",
			"ArchB",
			"ArchC",
			"ArchD",
			"ArchE",
			"B0",
			"B1",
			"B2",
			"B3",
			"B4",
			"B5",
			"Flsa",
			"HalfLetter",
			"Ledger",
			"Legal",
			"Letter",
			"Letter11x17",
			"Note"};
	private final Map<String, PageSizesEnum> pageSizeMap = new HashMap<>();

	public PDFOptionDialog(JFrame parent, String filename, Diagram diagram, SchematicDataSource dataSource) {
		super(parent);
		this.filename = filename;
		this.diagram = diagram;
		this.dataSource = dataSource;
		setTitle("PDF Options");

		pageSizeMap.put("A0", PageSizesEnum.A0);
		pageSizeMap.put("A1", PageSizesEnum.A1);
		pageSizeMap.put("A10", PageSizesEnum.A10);
		pageSizeMap.put("A2", PageSizesEnum.A2);
		pageSizeMap.put("A3", PageSizesEnum.A3);
		pageSizeMap.put("A4", PageSizesEnum.A4);
		pageSizeMap.put("A5", PageSizesEnum.A5);
		pageSizeMap.put("A6", PageSizesEnum.A6);
		pageSizeMap.put("A7", PageSizesEnum.A7);
		pageSizeMap.put("A8", PageSizesEnum.A8);
		pageSizeMap.put("A9", PageSizesEnum.A9);
		pageSizeMap.put("ArchA", PageSizesEnum.ArchA);
		pageSizeMap.put("ArchB", PageSizesEnum.ArchB);
		pageSizeMap.put("ArchC", PageSizesEnum.ArchC);
		pageSizeMap.put("ArchD", PageSizesEnum.ArchD);
		pageSizeMap.put("ArchE", PageSizesEnum.ArchE);
		pageSizeMap.put("B0", PageSizesEnum.B0);
		pageSizeMap.put("B1", PageSizesEnum.B1);
		pageSizeMap.put("B2", PageSizesEnum.B2);
		pageSizeMap.put("B3", PageSizesEnum.B3);
		pageSizeMap.put("B4", PageSizesEnum.B4);
		pageSizeMap.put("B5", PageSizesEnum.B5);
		pageSizeMap.put("Flsa", PageSizesEnum.Flsa);
		pageSizeMap.put("HalfLetter", PageSizesEnum.HalfLetter);
		pageSizeMap.put("Ledger", PageSizesEnum.Ledger);
		pageSizeMap.put("Legal", PageSizesEnum.Legal);
		pageSizeMap.put("Letter", PageSizesEnum.Letter);
		pageSizeMap.put("Letter11x17", PageSizesEnum.Letter11x17);
		pageSizeMap.put("Note", PageSizesEnum.Note);
	}

	public void openDialog(){
		JFrame frame = new JFrame();
		getParent().add(frame);
		frame.setTitle(getTitle());
		createContents(frame);
		frame.setSize(400, 150);
		frame.setLocation(450, 300);
		frame.setVisible(true);
	}

	private void createContents(final JFrame frame) {
  		JComboBox<String> pageSize;
		GridLayout layout=new GridLayout();
		layout.setColumns(14);
		layout.setHgap(20);
		layout.setVgap(20);
		frame.setLayout(layout);

		JLabel label = new JLabel();
		label.setText("Page Size:");
		label.setHorizontalAlignment(SwingConstants.LEFT);
		frame.add(label);

		pageSize = new JComboBox<>();
		for (int i = 0; i < pageSizeNames.length; i++){
			pageSize.addItem(pageSizeNames[i]);
		}
		pageSize.setSelectedIndex(0);
		frame.add(pageSize);

		JButton ok = new JButton();
		ok.setText("OK");
		ok.addActionListener(e -> {
			PageSizesEnum val = pageSizeMap.get(pageSize.getSelectedItem());
			dataSource.setPageSize(val);
			PdfExporter pdfExp = new PdfExporter();
			pdfExp.setPageSize(dataSource.pageSize());
			pdfExp.setAutoScale(AutoScale.FitToPage);
			pdfExp.export(diagram, filename);
			frame.setVisible(false);
		});
		frame.add(ok);

		JButton cancel = new JButton();
		cancel.setText("Cancel");
		cancel.addActionListener(e -> frame.setVisible(false));
		frame.add(cancel);

		frame.pack();
	}
}
