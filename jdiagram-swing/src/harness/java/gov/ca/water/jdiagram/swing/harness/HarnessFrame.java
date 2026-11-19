package gov.ca.water.jdiagram.swing.harness;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Set;
import java.util.TreeSet;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.mindfusion.diagramming.DiagramView;
import gov.ca.water.jdiagram.core.SchematicPanelBase;
import gov.ca.water.jdiagram.swing.views.DiagramPropertiesTableModel;
import gov.ca.water.jdiagram.swing.views.SchematicEditor;
import gov.ca.water.jdiagram.swing.views.SchematicOverview;
import gov.ca.water.jdiagram.swing.views.SchematicPanel;
import gov.ca.water.jdiagram.swing.views.SchematicView;

/**
 * Window that hosts a {@link SchematicView} or {@link SchematicEditor} wired to fake data, with side panels
 * for the overview, selected-item properties, an event log of host/presenter callbacks, and controls for
 * the data-source state. Used by {@link HarnessApp} and by the UI tests.
 * <p>
 * Must be created on the EDT via {@link #create(boolean, File, Preferences)}.
 */
public final class HarnessFrame extends JFrame
{
	public static final String SAMPLE_XML = "sample-schematic.xml";
	public static final String SAMPLE_TIME_WINDOW = "OCT1921 - SEP2015";

	private final boolean editor;
	private final File workDir;
	private final SampleData sampleData = new SampleData();
	private final transient MockSchematicHost host;
	private final transient MockSchematicDataSource dataSource;
	private final transient HarnessPresenter presenter = new HarnessPresenter();
	private final SchematicPanel schematic;
	private final DiagramPropertiesTableModel propertiesModel = new DiagramPropertiesTableModel();
	private final JTextArea log = new JTextArea();
	private final JPanel overviewHolder = new JPanel(new BorderLayout());

	private HarnessFrame(boolean editor, File workDir, Preferences preferences) {
		super(editor ? "jdiagram harness - SchematicEditor" : "jdiagram harness - SchematicView");
		this.editor = editor;
		this.workDir = workDir;
		this.host = new MockSchematicHost(sampleData, preferences);
		this.dataSource = new MockSchematicDataSource(workDir, host);

		log.setEditable(false);
		log.setName("eventLog");
		host.setEventSink(this::appendLog);
		presenter.setEventSink(this::appendLog);
		presenter.setOverviewSink(this::showOverview);
		presenter.setDialogParent(this);
		presenter.setLastDirectory(workDir);

		SchematicPanelBase base = new SchematicPanelBase(dataSource, host);
		schematic = editor ? new SchematicEditor(base, presenter) : new SchematicView(base, presenter);

		JPanel schematicContainer = new JPanel(new BorderLayout());
		schematicContainer.setName("schematicContainer");
		schematic.createPartControl(schematicContainer);
		schematic.getSelectionProvider().addSelectionChangedListener(propertiesModel);

		loadSampleDiagram();

		JTabbedPane side = new JTabbedPane();
		side.setName("sideTabs");
		side.addTab("Controls", buildControls());
		side.addTab("Properties", new JScrollPane(new JTable(propertiesModel)));
		side.addTab("Event log", new JScrollPane(log));
		side.setPreferredSize(new Dimension(320, 600));

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, schematicContainer, side);
		split.setResizeWeight(1.0);

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(presenter.getToolBar(), BorderLayout.NORTH);
		getContentPane().add(split, BorderLayout.CENTER);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setSize(1280, 800);
		setLocationByPlatform(true);

		// zoomToFit() in the constructor runs against a zero-size view; refit once the window is laid out.
		// SchematicPanel only writes values for nodes inside the visible rect, so a bad fit hides values.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				fitDiagramToView();
			}
		});
	}

	/** Zooms so the whole diagram is inside the visible area. Call on the EDT after the window is shown. */
	public void fitDiagramToView() {
		schematic.getDiagramView().zoomToFit();
	}

	/** @return text of every non-transparent node currently inside the visible area (what gets values) */
	public Set<String> visibleNodeNames() {
		DiagramView view = schematic.getDiagramView();
		return new TreeSet<>(schematic.getVisibleNodes(view.deviceToDoc(view.getVisibleRect())).keySet());
	}

	/**
	 * Creates the harness. Call on the EDT.
	 *
	 * @param editor      true for {@link SchematicEditor}, false for the read-only {@link SchematicView}
	 * @param workDir     scratch directory for the time-window file and sample XML
	 * @param preferences preference node backing {@code SchematicHost.getPreferences()}
	 */
	public static HarnessFrame create(boolean editor, File workDir, Preferences preferences) throws IOException {
		if (!SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException("HarnessFrame must be created on the EDT");
		}
		prepareWorkDir(workDir);
		return new HarnessFrame(editor, workDir, preferences);
	}

	/** Writes the time-window file read by DateCombo and a sample schematic for the Open action. */
	public static void prepareWorkDir(File workDir) throws IOException {
		Files.createDirectories(workDir.toPath());
		File tw = new File(workDir, MockSchematicHost.TW_FILE);
		if (!tw.exists()) {
			Files.writeString(tw.toPath(), SAMPLE_TIME_WINDOW + System.lineSeparator(), StandardCharsets.UTF_8);
		}
		try {
			SampleData.writeSampleXml(new File(workDir, SAMPLE_XML));
		} catch (Exception e) {
			throw new IOException("Unable to write sample schematic", e);
		}
	}

	/** Replaces the current diagram contents with the built-in sample and fits it to the view. */
	public void loadSampleDiagram() {
		schematic.getDiagramView().suspendRepaint();
		try {
			SampleData.populate(schematic.getDiagram());
			schematic.setupForEditor();
			schematic.collectAllSchematicVariables();
		} finally {
			schematic.getDiagramView().resumeRepaint();
		}
		schematic.getDiagramView().zoomToFit();
		appendLog("Loaded sample diagram (" + schematic.getDiagram().getNodes().size() + " nodes)");
	}

	/** Selects a date string in the toolbar combo, which triggers the library's refresh path. */
	public void selectDate(String date) {
		schematic.getDateCombo().getDateList().setSelectedItem(date);
	}

	/** Re-runs the value refresh for the currently selected date. */
	public void refreshValues() {
		host.clearLongTermAverages();
		schematic.refreshValues(dataSource.isDebugging() ? 1 : 0, true);
	}

	private JPanel buildControls() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel units = titled("Units", new FlowLayout(FlowLayout.LEFT));
		JRadioButton cfs = new JRadioButton("CFS", true);
		JRadioButton taf = new JRadioButton("TAF");
		ButtonGroup unitsGroup = new ButtonGroup();
		unitsGroup.add(cfs);
		unitsGroup.add(taf);
		cfs.addActionListener(e -> { dataSource.setUnits(MockSchematicDataSource.CFS); refreshValues(); });
		taf.addActionListener(e -> { dataSource.setUnits(MockSchematicDataSource.TAF); refreshValues(); });
		units.add(cfs);
		units.add(taf);
		panel.add(units);

		JPanel mode = titled("Mode", new FlowLayout(FlowLayout.LEFT));
		JComboBox<String> modeBox = new JComboBox<>(new String[]{MockSchematicDataSource.BASE, MockSchematicDataSource.DIFF});
		modeBox.addActionListener(e -> { dataSource.setMode((String) modeBox.getSelectedItem()); refreshValues(); });
		mode.add(modeBox);
		panel.add(mode);

		JPanel studies = titled("Studies", new GridLayout(0, 2));
		for (int i = 0; i < MockSchematicHost.STUDY_COUNT; i++) {
			final int index = i;
			JCheckBox box = new JCheckBox("Alt " + i, dataSource.getSelectedStudy(i));
			box.addActionListener(e -> { dataSource.setSelectedStudy(index, box.isSelected()); refreshValues(); });
			studies.add(box);
		}
		panel.add(studies);

		JPanel debug = titled("Debug", new FlowLayout(FlowLayout.LEFT));
		JCheckBox debugging = new JCheckBox("isDebugging()");
		JCheckBox target = new JCheckBox("target() != null");
		debugging.addActionListener(e -> dataSource.setDebugging(debugging.isSelected()));
		target.addActionListener(e -> dataSource.setTarget(target.isSelected() ? "harness-target" : null));
		debug.add(debugging);
		debug.add(target);
		panel.add(debug);

		JPanel actions = titled("Actions", new GridLayout(0, 1, 4, 4));
		JButton refresh = new JButton("Refresh values");
		refresh.addActionListener(e -> refreshValues());
		JButton reload = new JButton("Reload sample diagram");
		reload.addActionListener(e -> loadSampleDiagram());
		JButton openSample = new JButton("Open " + SAMPLE_XML + " via toolbar");
		openSample.addActionListener(e -> {
			presenter.queueFileDialogResult(new File(workDir, SAMPLE_XML).getAbsolutePath());
			fireToolbarAction("Open");
		});
		JButton clearLog = new JButton("Clear log");
		clearLog.addActionListener(e -> log.setText(""));
		actions.add(refresh);
		actions.add(reload);
		actions.add(openSample);
		actions.add(clearLog);
		panel.add(actions);

		JLabel info = new JLabel("<html>Work dir:<br>" + workDir.getAbsolutePath() + "</html>");
		info.setAlignmentX(0f);
		panel.add(info);
		panel.add(Box.createVerticalGlue());
		return panel;
	}

	/** Clicks the toolbar button whose action is named {@code name}. */
	public boolean fireToolbarAction(String name) {
		return HarnessSupport.findToolbarButton(presenter.getToolBar(), name)
				.map(b -> { b.doClick(); return true; })
				.orElse(false);
	}

	private static JPanel titled(String title, java.awt.LayoutManager layout) {
		JPanel p = new JPanel(layout);
		p.setBorder(BorderFactory.createTitledBorder(title));
		p.setAlignmentX(0f);
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 80));
		return p;
	}

	private void showOverview(SchematicOverview overview) {
		Runnable r = () -> {
			overviewHolder.removeAll();
			if (overview != null) {
				overviewHolder.add(overview, BorderLayout.CENTER);
			} else {
				overviewHolder.add(new JLabel("Library passed a null overview"), BorderLayout.CENTER);
			}
			overviewHolder.revalidate();
			overviewHolder.repaint();
		};
		if (SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}

	private void appendLog(String message) {
		String line = LocalTime.now().withNano(0) + "  " + message + "\n";
		if (SwingUtilities.isEventDispatchThread()) {
			log.append(line);
		} else {
			SwingUtilities.invokeLater(() -> log.append(line));
		}
	}

	/** Removes a preference node created for a test run. */
	public static void removePreferences(Preferences preferences) {
		try {
			preferences.removeNode();
		} catch (BackingStoreException | IllegalStateException e) {
			// best effort cleanup
		}
	}

	public boolean isEditor() {
		return editor;
	}

	public File getWorkDir() {
		return workDir;
	}

	public SampleData getSampleData() {
		return sampleData;
	}

	public MockSchematicHost getHost() {
		return host;
	}

	public MockSchematicDataSource getDataSource() {
		return dataSource;
	}

	public HarnessPresenter getPresenter() {
		return presenter;
	}

	public SchematicPanel getSchematic() {
		return schematic;
	}

	public DiagramPropertiesTableModel getPropertiesModel() {
		return propertiesModel;
	}

	public String getLogText() {
		return log.getText();
	}
}
