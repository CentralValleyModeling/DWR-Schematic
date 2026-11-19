package gov.ca.water.jdiagram.swing.utils;

import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Utility class for creating actions. Left non-final to allow for behavior extension.
 */
public class ActionHelper {
	private ActionHelper() {
		throw new IllegalStateException("Utility class, cannot be instantiated");
	}

	public static Icon icon(String resource) {
		URL url = ActionHelper.class.getResource("/icons/" + resource);
		return url == null ? null : new ImageIcon(url);
	}

	public static Icon saveIcon() {
		return icon("Save.png");
	}

	public static Icon openIcon() {
		return icon("Open.png");
	}


	public static Action buildAction(String name, Icon icon, Consumer<ActionEvent> handler) {
		Action a = new AbstractAction(name, icon) {
			@Override
			public void actionPerformed(ActionEvent e) {
				handler.accept(e);
			}
		};
		a.putValue(Action.SMALL_ICON, icon);
		return a;
	}
}
