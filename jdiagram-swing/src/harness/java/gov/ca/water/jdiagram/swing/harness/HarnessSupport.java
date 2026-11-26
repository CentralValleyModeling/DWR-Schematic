package gov.ca.water.jdiagram.swing.harness;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JToolBar;

/** Component lookup helpers shared by the harness and UI tests. */
public final class HarnessSupport
{
	private HarnessSupport() {
		throw new AssertionError("Utility class");
	}

	/** Finds the toolbar button backed by an {@link Action} whose {@link Action#NAME} is {@code actionName}. */
	public static Optional<AbstractButton> findToolbarButton(JToolBar toolBar, String actionName) {
		for (Component c : toolBar.getComponents()) {
			if (c instanceof AbstractButton button) {
				Action action = button.getAction();
				if (action != null && actionName.equals(action.getValue(Action.NAME))) {
					return Optional.of(button);
				}
			}
		}
		return Optional.empty();
	}

	/** @return the action names of every action-backed button on the toolbar, in order */
	public static List<String> toolbarActionNames(JToolBar toolBar) {
		List<String> names = new ArrayList<>();
		for (Component c : toolBar.getComponents()) {
			if (c instanceof AbstractButton button && button.getAction() != null) {
				names.add(String.valueOf(button.getAction().getValue(Action.NAME)));
			}
		}
		return names;
	}

	/** Depth-first search for a component of the given type, optionally matching {@link Component#getName()}. */
	public static <T extends Component> Optional<T> find(Container root, Class<T> type, String name) {
		for (Component c : root.getComponents()) {
			if (type.isInstance(c) && (name == null || name.equals(c.getName()))) {
				return Optional.of(type.cast(c));
			}
			if (c instanceof Container child) {
				Optional<T> found = find(child, type, name);
				if (found.isPresent()) {
					return found;
				}
			}
		}
		return Optional.empty();
	}
}
