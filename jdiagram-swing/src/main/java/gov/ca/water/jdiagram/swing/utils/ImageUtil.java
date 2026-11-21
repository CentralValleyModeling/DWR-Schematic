package gov.ca.water.jdiagram.swing.utils;

import java.net.URL;
import javax.swing.ImageIcon;

import com.google.common.flogger.FluentLogger;

/**
 * Utility class for creating image icons. Left non-final to allow for behavior extension.
 */
public class ImageUtil {
 private ImageUtil() {
   throw new IllegalStateException("This utility class should not be instantiated");
 }

	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	/** Base package for relative image paths such as {@code "images/copy.png"}. */
	private static final String RESOURCE_ROOT = "/icons/";

	/** Returns an ImageIcon, or an empty icon if the path was invalid. */
	public static ImageIcon createImageIcon(String path) {
		URL imgURL = ImageUtil.class.getResource(path.startsWith("/") ? path : RESOURCE_ROOT + path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			logger.atFine().log("Couldn't find file: %s", path);
			return new ImageIcon();
		}
	}
}
