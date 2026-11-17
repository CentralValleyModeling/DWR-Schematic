package gov.ca.water.jdiagram.swing.utils;

import java.net.URL;
import javax.swing.ImageIcon;

import com.google.common.flogger.FluentLogger;

public class ImageUtil {
 private ImageUtil() {
   throw new AssertionError("This utility class should not be instantiated");
 }

	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	/** Returns an ImageIcon, or null if the path was invalid. */
	public static ImageIcon createImageIcon(String path) {
		URL imgURL = ImageUtil.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			logger.atFine().log("Couldn't find file: %s", path);
			return new ImageIcon();
		}
	}
}
