package gov.ca.water.jdiagram.swing.utils;

import javax.swing.ImageIcon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class ImageUtilTest {

	@Test
	void testImageIcon() {
		ImageIcon icon = ImageUtil.createImageIcon("Y.png");
		assertNotNull(icon);
		assertEquals(16, icon.getIconHeight());
		assertEquals(16, icon.getIconWidth());
	}
}
