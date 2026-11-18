package gov.ca.water.jdiagram.swing.harness;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
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
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (Boolean.TRUE.equals(onEdt(condition::getAsBoolean))) {
				return;
			}
			Thread.sleep(50);
		}
		fail("Timed out after " + timeout.toSeconds() + "s waiting for: " + description);
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
