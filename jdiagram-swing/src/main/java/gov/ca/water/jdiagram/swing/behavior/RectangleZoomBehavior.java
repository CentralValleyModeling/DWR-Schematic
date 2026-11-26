package gov.ca.water.jdiagram.swing.behavior;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.SwingUtilities;

import com.mindfusion.common.ByRef;
import com.mindfusion.diagramming.BehaviorBase;
import com.mindfusion.diagramming.CursorHint;
import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.InteractionState;
import gov.ca.water.jdiagram.swing.utils.ImageUtil;
import gov.ca.water.jdiagram.swing.views.SchematicOverview;

public final class RectangleZoomBehavior extends BehaviorBase {

	SchematicOverview overview;

	public RectangleZoomBehavior(DiagramView flowChart, SchematicOverview overview) {
		super(flowChart);
		this.overview = overview;
	}

	public InteractionState startDraw(Point2D point, MouseEvent e) {
		return null;
	}

	@Override
	public void pointerDown(Point mousePosition, MouseEvent e) {
		ptStartDragDev = mousePosition;
	}

	@Override
	public void pointerUp(Point mousePosition, MouseEvent e) {
		getDiagramView().setCursor(getDiagramView().getPointerCursor());
		getDiagramView().recreateCacheImage();
		Rectangle r = createRectFromDiagonalPoints(ptStartDragDev, mousePosition);

		Rectangle2D rect = getDiagramView().deviceToDoc(r);
		getDiagramView().zoomToFit(rect);
		overview.setDiagramView(getDiagramView());
	}

	@Override
	public void pointerMove(Point mousePosition, MouseEvent e) {
		final DiagramView view = getDiagramView();
		final Rectangle r = createRectFromDiagonalPoints(ptStartDragDev,
				mousePosition);
		view.repaint(diagramView.getVisibleRect());
		SwingUtilities.invokeLater(() -> view.getGraphics().drawRect(r.x, r.y, r.width, r.height));
	}

	public static Rectangle createRectFromDiagonalPoints(Point p1, Point p2) {
		return createRectFromDiagonalPoints(p1.x, p1.y, p2.x, p2.y);
	}

	public static Rectangle createRectFromDiagonalPoints(int x1, int y1, int x2, int y2) {
		return new Rectangle(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
	}

	private Point ptStartDragDev;

	@Override
	protected CursorHint setMouseCursor(Point2D arg0, ByRef<Boolean> arg1) {
		Image icon = ImageUtil.createImageIcon("zoom_in.png").getImage();
		Cursor zoomRectCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				icon, new Point(0, 0), "zoom rectangle");
		getDiagramView().setCursor(zoomRectCursor);
		return CursorHint.DontChange;
	}
}
