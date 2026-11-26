package gov.ca.water.jdiagram.swing.toolbars;

import java.util.function.Consumer;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JToolBar;

import com.google.common.flogger.FluentLogger;

/**
 * Toolkit-neutral target for toolbar contributions. Lets {@code SchematicPanel} fill a Swing {@link JToolBar} or an
 * Eclipse {@code IToolBarManager} (see {@code EclipseToolBarContributor} in jdiagram-legacy) with the same code,
 * including Swing controls such as the date combo and search field.
 * <p>
 * Callers invoke these methods on the AWT event thread. Implementations that target another toolkit are responsible
 * for marshalling to that toolkit's UI thread, preserving contribution order.
 */
public interface ToolBarContributor
{
	/**
	 * Adds a button backed by {@code action}.
	 */
	void addAction(Action action);

	/**
	 * Adds a Swing control (e.g. a combo box or search panel) to the toolbar.
	 *
	 * @param id a stable identifier for the contribution; also used as the component name if it has none
	 * @param component the control to place on the toolbar. A Swing component has a single parent, so it can be on
	 * only one toolbar at a time.
	 */
	void addComponent(String id, JComponent component);

	/**
	 * Called once after a batch of contributions so the toolbar can lay itself out again. Optional.
	 */
	default void update() {
		// optional
	}

	/**
	 * @return a contributor that adds to {@code toolBar}
	 */
	static ToolBarContributor of(JToolBar toolBar) {
		return new JToolBarContributor(toolBar);
	}

	/**
	 * Adapts an action-only consumer (the older {@code PlatformHarness.getToolBarConsumer()} contract). Components
	 * cannot be placed through a consumer, so they are skipped and logged.
	 */
	static ToolBarContributor of(Consumer<Action> consumer) {
		return new ToolBarContributor() {
			private final FluentLogger logger = FluentLogger.forEnclosingClass();

			@Override
			public void addAction(Action action) {
				consumer.accept(action);
			}

			@Override
			public void addComponent(String id, JComponent component) {
				logger.atWarning().log("Toolbar target accepts actions only; '%s' was not added", id);
			}
		};
	}
}
