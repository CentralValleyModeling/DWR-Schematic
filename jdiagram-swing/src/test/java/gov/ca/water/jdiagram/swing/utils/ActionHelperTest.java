package gov.ca.water.jdiagram.swing.utils;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import javax.swing.Action;
import javax.swing.Icon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class ActionHelperTest {
	@Test
	void testIconLoad() {
		Icon icon = ActionHelper.icon("paste.png");
		assertNotNull(icon);

		icon = ActionHelper.icon("copy.png");
		assertNotNull(icon);
	}

	@Test
	void testBuildAction() {
		Icon icon = ActionHelper.icon("paste.png");
		Consumer<ActionEvent> handler = e -> {
		};
		Action action = ActionHelper.buildAction("testIcon", icon, handler);
		assertNotNull(action);
		Icon value = (Icon) action.getValue(Action.SMALL_ICON);
		assertEquals(icon, value);
	}
}
