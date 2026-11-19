package gov.ca.water.jdiagram.swing.harness;

import java.awt.Component;
import java.io.File;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import com.mindfusion.diagramming.DiagramView;
import gov.ca.water.jdiagram.swing.UILauncher;
import gov.ca.water.jdiagram.swing.views.SchematicOverview;

/**
 * {@link UILauncher} for the harness. Every callback is recorded in {@link #getEvents()} so tests can
 * assert that the library asked the host to do something. File dialogs can be scripted with
 * {@link #queueFileDialogResult(String)} so tests never block on a modal chooser.
 */
public class HarnessPresenter implements UILauncher
{
	public static final String NO_FILE = "\u0000cancel";

	private final JToolBar toolBar = new JToolBar("Schematic");
	private final List<String> events = new CopyOnWriteArrayList<>();
	private final Deque<String> scriptedFiles = new ConcurrentLinkedDeque<>();

	private Consumer<String> eventSink = s -> { };
	private Consumer<SchematicOverview> overviewSink = o -> { };
	private Component dialogParent;
	private File lastDirectory = new File(System.getProperty("user.dir"));
	private SchematicOverview overview;
	private DiagramView diagramView;
	private int mtpDecimalPlaces = -1;

	public HarnessPresenter() {
		toolBar.setName("schematicToolBar");
		toolBar.setFloatable(false);
	}

	public void setEventSink(Consumer<String> eventSink) {
		this.eventSink = eventSink == null ? s -> { } : eventSink;
	}

	public void setOverviewSink(Consumer<SchematicOverview> overviewSink) {
		this.overviewSink = overviewSink == null ? o -> { } : overviewSink;
	}

	public void setDialogParent(Component dialogParent) {
		this.dialogParent = dialogParent;
	}

	public void setLastDirectory(File lastDirectory) {
		this.lastDirectory = lastDirectory;
	}

	/**
	 * Makes the next {@link #launchFileDialog()} return {@code path} instead of opening a chooser.
	 * Pass {@link #NO_FILE} to simulate the user cancelling.
	 */
	public void queueFileDialogResult(String path) {
		scriptedFiles.add(path);
	}

	public List<String> getEvents() {
		return events;
	}

	public long countEvents(String prefix) {
		return events.stream().filter(e -> e.startsWith(prefix)).count();
	}

	public SchematicOverview getOverview() {
		return overview;
	}

	public DiagramView getDiagramView() {
		return diagramView;
	}

	public int getMtpDecimalPlaces() {
		return mtpDecimalPlaces;
	}

	private void eventRecord(String event) {
		events.add(event);
		eventSink.accept(event);
	}

	@Override
	public void launchDialog(Runnable showDialog) {
		eventRecord("launchDialog");
		UILauncher.super.launchDialog(showDialog);
	}

	@Override
	public JPanel mtp() {
		return null;
	}

	@Override
	public void setMtpDecimalPlaces(int numberDecimals) {
		mtpDecimalPlaces = numberDecimals;
		eventRecord("setMtpDecimalPlaces " + numberDecimals);
	}

	@Override
	public void showDssView() {
		eventRecord("showDssView");
	}

	@Override
	public void showDSSPerspective() {
		eventRecord("showDSSPerspective");
	}

	@Override
	public void launchFontDialog() {
		eventRecord("launchFontDialog");
	}

	@Override
	public String launchFileDialog() {
		String scripted = scriptedFiles.poll();
		if (scripted != null) {
			eventRecord("launchFileDialog (scripted) " + scripted);
			return NO_FILE.equals(scripted) ? null : scripted;
		}
		JFileChooser chooser = new JFileChooser(lastDirectory);
		chooser.setDialogTitle("Schematic file (.xml, .pdf, .svg, or MindFusion binary)");
		int result = chooser.showDialog(dialogParent, "Select");
		if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
			eventRecord("launchFileDialog cancelled");
			return null;
		}
		File file = chooser.getSelectedFile();
		lastDirectory = file.getParentFile();
		eventRecord("launchFileDialog " + file.getAbsolutePath());
		return file.getAbsolutePath();
	}

	@Override
	public void launchViewPart(SchematicOverview overview, DiagramView view, JPanel container) {
		this.overview = overview;
		this.diagramView = view;
		eventRecord("launchViewPart overview=" + (overview != null));
		overviewSink.accept(overview);
	}

	@Override
	public JToolBar getToolBar() {
		return toolBar;
	}
}
