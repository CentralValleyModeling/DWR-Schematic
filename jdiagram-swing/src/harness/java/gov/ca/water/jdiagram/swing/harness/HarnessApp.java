package gov.ca.water.jdiagram.swing.harness;

import java.io.File;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.google.common.flogger.FluentLogger;

/**
 * Interactive launcher for manually exercising the Swing schematic UI against fake data.
 * <pre>
 *   gradlew :jdiagram-swing:runHarness                          (SchematicView)
 *   gradlew :jdiagram-swing:runHarness -PharnessArgs="--editor" (SchematicEditor)
 *   gradlew :jdiagram-swing:runHarness -PharnessArgs="--dir C:/temp/harness"
 * </pre>
 * Or run this class directly from the IDE using the {@code harness} source set classpath.
 */
public final class HarnessApp
{
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private HarnessApp() {
	}

	public static void main(String[] args) {
		boolean editor = false;
		File workDir = new File(System.getProperty("java.io.tmpdir"), "jdiagram-harness");
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "--editor" -> editor = true;
				case "--view" -> editor = false;
				case "--dir" -> workDir = new File(args[++i]);
				default -> logger.atFine().log("Ignoring unknown argument: " + args[i]);
			}
		}
		final boolean useEditor = editor;
		final File dir = workDir;

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			logger.atSevere().withCause(e).log("Uncaught exception on %s", t.getName());
		});

		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
				// keep the default look and feel
				logger.atFiner().log("Failed to set system look and feel");
			}
			try {
				Preferences prefs = Preferences.userRoot().node("gov/ca/water/jdiagram/swing/harness");
				HarnessFrame frame = HarnessFrame.create(useEditor, dir, prefs);
				frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
				frame.setVisible(true);
			} catch (Exception e) {
				logger.atSevere().withCause(e).log("Harness failed to start");
				JOptionPane.showMessageDialog(null, "Harness failed to start:\n" + e, "jdiagram harness",
						JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
		});
	}
}
