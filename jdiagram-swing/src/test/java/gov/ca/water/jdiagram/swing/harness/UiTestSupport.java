package gov.ca.water.jdiagram.swing.harness;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Window;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramNode;
import com.mindfusion.diagramming.DiagramNodeList;
import com.mindfusion.diagramming.Group;
import com.mindfusion.diagramming.ShapeNode;

import static org.junit.jupiter.api.Assertions.fail;

/** EDT and polling helpers for the UI tests. */
final class UiTestSupport
{
	static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

	private UiTestSupport() {
	}

	/** Runs {@code task} on the EDT and returns its result, rethrowing anything it throws. */
	static <T> T onEdt(Callable<T> task) throws Exception {
		if (SwingUtilities.isEventDispatchThread()) {
			return task.call();
		}
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Exception> failure = new AtomicReference<>();
		try {
			SwingUtilities.invokeAndWait(() -> {
				try {
					result.set(task.call());
				} catch (Exception e) {
					failure.set(e);
				}
			});
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Error err) {
				throw err;
			}
			throw new Exception(cause);
		}
		if (failure.get() != null) {
			throw failure.get();
		}
		return result.get();
	}

	static void runOnEdt(ThrowingRunnable task) throws Exception {
		onEdt(() -> {
			task.run();
			return null;
		});
	}

	/** Waits for pending EDT work (e.g. invokeLater from launchDialog) to drain. */
	static void flushEdt() throws Exception {
		runOnEdt(() -> { });
		runOnEdt(() -> { });
	}

	/** Polls {@code condition} on the EDT until it is true or {@link #DEFAULT_TIMEOUT} elapses. */
	static void waitUntil(String description, BooleanSupplier condition) throws Exception {
		waitUntil(description, DEFAULT_TIMEOUT, condition);
	}

	static void waitUntil(String description, Duration timeout, BooleanSupplier condition) throws Exception {
		waitUntil(description, timeout, condition, () -> "");
	}

	/** As {@link #waitUntil(String, BooleanSupplier)}, appending {@code diagnostics} (read on the EDT) on timeout. */
	static void waitUntil(String description, BooleanSupplier condition, Supplier<String> diagnostics) throws Exception {
		waitUntil(description, DEFAULT_TIMEOUT, condition, diagnostics);
	}

	static void waitUntil(String description, Duration timeout, BooleanSupplier condition,
			Supplier<String> diagnostics) throws Exception {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (Boolean.TRUE.equals(onEdt(condition::getAsBoolean))) {
				return;
			}
			Thread.sleep(50);
		}
		String detail = onEdt(diagnostics::get);
		fail("Timed out after " + timeout.toSeconds() + "s waiting for: " + description
				+ (detail == null || detail.isEmpty() ? "" : " [" + detail + "]"));
	}

	/**
	 * Waits for value refreshes started by the library to finish. {@code SchematicPanel.refreshValues} runs
	 * {@code updateValues} on an untracked {@code new Thread}, and viewport changes schedule a refresh on a
	 * 250 ms timer, so there is no handle to join: wait out the timer, then poll thread stacks.
	 */
	static void awaitValueRefreshes() throws Exception {
		Thread.sleep(400);
		flushEdt();
		long deadline = System.nanoTime() + DEFAULT_TIMEOUT.toNanos();
		while (System.nanoTime() < deadline) {
			boolean busy = Thread.getAllStackTraces().values().stream()
					.flatMap(Arrays::stream)
					.anyMatch(f -> f.getClassName().endsWith("SchematicPanel") && f.getMethodName().equals("updateValues"));
			if (!busy) {
				return;
			}
			Thread.sleep(50);
		}
	}

	/** @return the showing dialog with this title, if any */
	static Optional<Dialog> showingDialog(String title) {
		for (Window w : Window.getWindows()) {
			if (w.isShowing() && w instanceof Dialog d && title.equals(d.getTitle())) {
				return Optional.of(d);
			}
		}
		return Optional.empty();
	}

	/** Depth-first list of every component of {@code type} under {@code root}, in layout order. */
	static <T extends Component> List<T> findAll(Container root, Class<T> type) {
		List<T> found = new ArrayList<>();
		for (Component c : root.getComponents()) {
			if (type.isInstance(c)) {
				found.add(type.cast(c));
			}
			if (c instanceof Container child) {
				found.addAll(findAll(child, type));
			}
		}
		return found;
	}

	static ShapeNode node(Diagram diagram, String text) {
		for (ShapeNode n : diagram.items(ShapeNode.class)) {
			if (text.equals(n.getTextToEdit())) {
				return n;
			}
		}
		throw new AssertionError("No node with text " + text);
	}

	/** Text of the value label the library attaches to {@code node} for {@code studyId}, if any. */
	static Optional<String> valueText(ShapeNode node, int studyId) {
		Group group = node.getSubordinateGroup();
		if (group == null) {
			return Optional.empty();
		}
		DiagramNodeList attached = group.getAttachedNodes();
		if (studyId >= attached.size()) {
			return Optional.empty();
		}
		DiagramNode label = attached.get(studyId);
		return label instanceof ShapeNode s ? Optional.ofNullable(s.getText()) : Optional.empty();
	}

	/** The value label node attached to {@code node} for {@code studyId}; fails if there is none. */
	static DiagramNode valueNode(ShapeNode node, int studyId) {
		Group group = node.getSubordinateGroup();
		if (group == null || studyId >= group.getAttachedNodes().size()) {
			throw new AssertionError("No value label " + studyId + " on " + node.getTextToEdit());
		}
		return group.getAttachedNodes().get(studyId);
	}

	static String format(double value, int decimals, String units) {
		return String.format("%." + decimals + "f %s", value, units);
	}

	static void assertNoUncaught(List<Throwable> uncaught) {
		if (!uncaught.isEmpty()) {
			AssertionError error = new AssertionError("Uncaught exceptions on UI/worker threads: " + uncaught);
			uncaught.forEach(error::addSuppressed);
			throw error;
		}
	}

	@FunctionalInterface
	interface ThrowingRunnable
	{
		void run() throws Exception;
	}
}
