package gov.ca.water.jdiagram.swing.views;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import com.google.common.flogger.FluentLogger;
import com.mindfusion.diagramming.Behavior;
import com.mindfusion.diagramming.CompositeCmd;
import com.mindfusion.diagramming.CustomDraw;
import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramAdapter;
import com.mindfusion.diagramming.DiagramEvent;
import com.mindfusion.diagramming.DiagramItem;
import com.mindfusion.diagramming.DiagramItemList;
import com.mindfusion.diagramming.DiagramLink;
import com.mindfusion.diagramming.DiagramLinkList;
import com.mindfusion.diagramming.DiagramNode;
import com.mindfusion.diagramming.DiagramNodeList;
import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.DrawNodeEvent;
import com.mindfusion.diagramming.DummyNode;
import com.mindfusion.diagramming.GridStyle;
import com.mindfusion.diagramming.HandlesStyle;
import com.mindfusion.diagramming.InplaceEditable;
import com.mindfusion.diagramming.LinkEvent;
import com.mindfusion.diagramming.NodeEvent;
import com.mindfusion.diagramming.Orientation;
import com.mindfusion.diagramming.RerouteLinks;
import com.mindfusion.diagramming.RoutingOptions;
import com.mindfusion.diagramming.Shape;
import com.mindfusion.diagramming.ShapeNode;
import com.mindfusion.diagramming.UndoEvent;
import com.mindfusion.diagramming.UndoManager;
import com.mindfusion.diagramming.XmlException;
import com.mindfusion.drawing.Brush;
import com.mindfusion.drawing.DashStyle;
import com.mindfusion.drawing.Pen;
import com.mindfusion.drawing.PointList;
import com.mindfusion.drawing.SolidBrush;
import gov.ca.water.jdiagram.core.SchematicContext;
import gov.ca.water.jdiagram.core.SchematicPanelBase;
import gov.ca.water.jdiagram.core.utils.AttributeMapper;
import gov.ca.water.jdiagram.core.utils.XmlUtilities;
import gov.ca.water.jdiagram.swing.PlatformHarness;
import gov.ca.water.jdiagram.swing.panel.EditSchematicElementPanel;
import org.w3c.dom.Document;

import gov.ca.water.jdiagram.swing.toolbars.ToolBarContributor;

import static gov.ca.water.jdiagram.swing.utils.ActionHelper.buildAction;
import static gov.ca.water.jdiagram.swing.utils.ActionHelper.icon;

public final class SchematicEditor extends SchematicPanel {
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private final class CompartorOnNodeId implements Comparator<DiagramNode> {
		@Override
		public int compare(DiagramNode o1, DiagramNode o2) {
			if (o1 == null) {
				if (o2 == null) {
					return 0;
				} else {
					return -1;
				}
			} else {
				if (o2 == null) {
					return 1;
				} else {
					String id1 = getIdFor(o1);
					String id2 = getIdFor(o2);
					if (id1 == null) {
						if (id2 == null) {
							return 0;
						} else {
							return -1;
						}
					} else {
						if (id2 == null) {
							return 1;
						} else {
							return id1.compareTo(id2);
						}
					}
				}
			}
		}
	}

	JPopupMenu popupMenu;
	JPopupMenu pastePopupMenu;
	JMenuItem deleteMenuItem;
	JMenuItem copyMenuItem;
	private transient JMenuItem pasteMenuItem;
	private transient Action exportAction;
	private transient Action importAction;
	private transient Action undoAction;
	private transient Action redoAction;
	private transient Action copyAction;
	private transient Action pasteAction;
	private transient Action verticalAlignAction;
	private transient Action horizontalAlignAction;
	private transient JMenuItem shortenPointsMenuItem;
	private transient JMenuItem editShapeMenuItem;
	private transient JMenuItem shrinkToElementsMenuItem;
	private transient JMenuItem changePointsMenuItem;

	public SchematicEditor(SchematicPanelBase base, PlatformHarness presenter) {
		super(base, presenter);
	}

	@Override
	public void createPartControl(JPanel parent) {
		super.createPartControl(parent);

		getDiagramView().setBehavior(Behavior.Modify);
		getDiagramView().setAllowInplaceEdit(true);

		makeEditorActions();
		contributeToEditorActionBars();

		getDiagramView().getInputMap().put(
				KeyStroke.getKeyStroke("ctrl typed c"), copyAction);
		getDiagramView().getInputMap().put(
				KeyStroke.getKeyStroke("ctrl typed v"), pasteAction);
		getDiagramView().getInputMap().put(
				KeyStroke.getKeyStroke("ctrl typed z"), undoAction);

		deleteMenuItem = new JMenuItem();
		deleteMenuItem.setName("_menuItemDelete");
		deleteMenuItem.setText("Delete");
		deleteMenuItem.addActionListener(e -> onDelete());

		copyMenuItem = new JMenuItem();
		copyMenuItem.setName("_menuItemCopy");
		copyMenuItem.setText("Copy");
		copyMenuItem.addActionListener(e -> onCopy());

		pasteMenuItem = new JMenuItem();
		pasteMenuItem.setName("_menuItemPaste");
		pasteMenuItem.setText("Paste");
		pasteMenuItem.addActionListener(e -> onPaste());

		shortenPointsMenuItem = new JMenuItem("Delete Intermediate Points");
		shortenPointsMenuItem.addActionListener(e -> {
			if (clickedItem instanceof DiagramLink link) {
				onShortenPoints(link);
			}
		});

		changePointsMenuItem = new JMenuItem(
				"Change Number of Intermediate Points");
		changePointsMenuItem.addActionListener(e -> {
			if (clickedItem instanceof DiagramLink link) {
				onChangePoints(link);
			}
		});

		editShapeMenuItem = new JMenuItem("Edit Shape");
		editShapeMenuItem.addActionListener(e -> {
			if (clickedItem instanceof DiagramItem) {
				if (clickedItem instanceof DiagramLink) {
					Pen p = clickedItem.getPen().clone();
					clickedItem.setPen(p);
					clickedItem.getPen().setDashStyle(DashStyle.Dash);
					return;
				}

				ShapeNode snode = (ShapeNode) clickedItem;
				snode.setCustomDraw(CustomDraw.Additional);
				try {
					addToShapeLibrary();
				} catch (Exception e1) {
					logger.atFine().withCause(e1).log("Failed to add shape to library");
				}
			}
		});

		shrinkToElementsMenuItem = new JMenuItem("Shrink Bounds to Elements");
		shrinkToElementsMenuItem.addActionListener(e -> {
			try {
				shrinkDiagramToElements();
			} catch (Exception ex) {
				logger.atFine().withCause(ex).log("Failed to shrink diagram to elements");
			}
		});

		getDiagram().removeDiagramListener(listener);
		getDiagram().addDiagramListener(new DiagramAdapter() {
			@Override
			public void nodeSelected(NodeEvent e) {
				e.getNode().setHandlesStyle(HandlesStyle.EasyMove);
			}

			@Override
			public void linkSelected(LinkEvent e) {
				e.getLink().setSelected(true);
			}

			@Override
			public void nodeClicked(NodeEvent e) {
				if (e.getNode() instanceof ShapeNode) {
					boxClicked(e);
				}
			}

			@Override
			public void linkClicked(LinkEvent e) {
				arrowClicked(e);
			}

			@Override
			public void clicked(DiagramEvent e) {
				diagramClicked(e);
			}

			@Override
			/**
			 * Raised when a diagram node must be custom drawn.
			 */
			public void drawNode(DrawNodeEvent e) {
				Rectangle2D rect = e.getBounds();
				rect.setFrame(rect.getMinX() + 9, rect.getMinY() + 9, rect
						.getWidth() - 16, rect.getHeight() - 16);

				Graphics2D g = e.getGraphics();
				Pen pen = e.getNode().getPen().clone();
				pen.setWidth(4);
				pen.applyTo(g);
				g.draw(rect);
			}

			// undo/redo events

			@Override
			public void actionRecorded(UndoEvent e) {
				undoAction.setEnabled(true);
				redoAction.setEnabled(false);
			}

			@Override
			public void actionRedone(UndoEvent e) {
				undoAction.setEnabled(true);
				if (getDiagram().getUndoManager().getHistory().getNextRedo() == null) {
					redoAction.setEnabled(false);
				}
			}

			@Override
			public void actionUndone(UndoEvent e) {
				redoAction.setEnabled(true);
				if (getDiagram().getUndoManager().getHistory().getNextUndo() == null) {
					undoAction.setEnabled(false);
				}
			}

			private Rectangle2D.Float previousBounds = null;

			@Override
			public void viewportChanged() {
				Rectangle2D.Float bounds = getDiagram().getBounds();
				if (previousBounds == null) {
					previousBounds = bounds;
				}
				if (badValue(bounds.x) || badValue(bounds.y)
						|| badValue(bounds.height) || badValue(bounds.width)) {
					getDiagram().setBounds(previousBounds);
				} else {
					previousBounds = bounds;
				}
				float zoomFactor = getDiagramView().getZoomFactor();
				getDiagram().setAdjustmentHandlesSize(
						(float) Math.round(10 + 150 / zoomFactor));
			}

			private boolean badValue(float x) {
				return java.lang.Float.isNaN(x)
						|| java.lang.Float.isInfinite(x)
						|| x == java.lang.Float.MIN_VALUE
						|| x == java.lang.Float.MAX_VALUE
						|| ((int) x == Integer.MAX_VALUE)
						|| ((int) x == Integer.MIN_VALUE);
			}
		});

		pastePopupMenu = new JPopupMenu("Paste Popup");
		pastePopupMenu.setName("pastePopup");
		pastePopupMenu.add(pasteMenuItem);

		popupMenu = new JPopupMenu("Edit Popup");
		popupMenu.setName("editPopup");
		popupMenu.add(copyMenuItem);
		popupMenu.add(deleteMenuItem);
		popupMenu.add(changePointsMenuItem);
	}

	private void makeEditorActions() {
		exportAction = buildAction("Export Attributes", icon("export_attr.png"), e -> {
			String fileToSave = chooseFileToExport();
			try {
				saveAttributesTo(fileToSave);
			} catch (Exception ex) {
				getBase().getContext().getDataSource().reportError(ex);
			}
		});

		importAction = buildAction("Import Attributes", icon("import_attr.png"), e -> {
			String fileToImport = chooseFileToImport();
			try {
				loadAttributesFrom(fileToImport);
			} catch (Exception ex) {
				getBase().getContext().getDataSource().reportError(ex);
			}
		});

		undoAction = buildAction("Undo", icon("undo.png"), e -> {
			getDiagram().getUndoManager().undo();
			if (e instanceof Action && getDiagram().getUndoManager().getHistory().getNextUndo() == null) {
				setEnabled(false);
			}
		});
		undoAction.setEnabled(false);

		redoAction = buildAction("Redo", icon("redo.png"), e -> {
			getDiagram().getUndoManager().redo();
			if (e instanceof Action && getDiagram().getUndoManager().getHistory().getNextRedo() == null) {
				setEnabled(false);
			}
		});
		redoAction.setEnabled(false);

		copyAction = buildAction("Copy", icon("copy.png"), e -> onCopy());

		pasteAction = buildAction("Paste", icon("paste.png"), e -> onPaste());

		verticalAlignAction = buildAction("Align Vertical", icon("align_vertical.png"), e -> {
			alignSelectedNodes(Orientation.Vertical);
			alignSelectedLinks(Orientation.Vertical);
		});

		horizontalAlignAction = buildAction("Align Horizontal", icon("align_horizontal.png"), e -> {
			alignSelectedNodes(Orientation.Horizontal);
			alignSelectedLinks(Orientation.Horizontal);
		});
	}

	private void contributeToEditorActionBars() {
		fillEditorToolBar(getDataPresenter().getToolBarContributor());
	}

	/**
	 * Adds the editor actions. The date combo is already placed by {@link #fillLocalToolBar(ToolBarContributor)}.
	 */
	public void fillEditorToolBar(ToolBarContributor toolBar) {
		toolBar.addAction(importAction);
		toolBar.addAction(exportAction);
		toolBar.addAction(undoAction);
		toolBar.addAction(redoAction);
		toolBar.addAction(copyAction);
		toolBar.addAction(pasteAction);
		toolBar.addAction(verticalAlignAction);
		toolBar.addAction(horizontalAlignAction);
		toolBar.update();
	}

	public void onDelete() {
		Diagram d = getDiagram();
		CompositeCmd compositeCmd = d.getUndoManager().startComposite(
				"DeleteALot");
		if (!(d.getSelection().getNodes().isEmpty())) {
			for (DiagramNode n : d.getSelection().getNodes()) {
				n.setLocked(false);
			}
			d.getNodes().removeAll(d.getSelection().getNodes());
		}
		if (!(d.getSelection().getLinks().isEmpty())) {
			for (DiagramLink l : d.getSelection().getLinks()) {
				l.setLocked(false);
			}
			d.getLinks().removeAll(d.getSelection().getLinks());
		}
		compositeCmd.execute();
	}

	private transient Point2D lastMouseClickedPosition;
	private DiagramItem clickedItem;

	public void onCopy() {
		Diagram d = getDiagram();
		CompositeCmd compositeCmd = d.getUndoManager().startComposite("CopyALot");
		onCopy(d);
		compositeCmd.execute();

		DiagramView diagramView=getDiagramView();
		try {
			diagramView.copyToClipboard(true);
		} catch (IOException | XmlException e) {
			getBase().getContext().getDataSource().reportError(e);
		}
		SchematicContext.setDiagram(d);
	}

	public void onCopy(Diagram d) {
		getBase().getContext().setDiagramItems(new ArrayList<>());
		HashMap<DiagramNode, DiagramNode> selectedToAddedMap = new HashMap<DiagramNode, DiagramNode>();
		if (!(d.getSelection().getNodes().isEmpty())) {
			DiagramNodeList nodes = d.getSelection().getNodes();
			for (DiagramNode node : nodes) {
				if (node instanceof ShapeNode snode) {
					ShapeNode clone = (ShapeNode) snode.clone(true);
					getBase().getContext().getDiagramItems().add(clone);
					selectedToAddedMap.put(snode, clone);
				}
			}
		}
		if (!(d.getSelection().getLinks().isEmpty())) {
			DiagramLinkList links = d.getSelection().getLinks();
			for (DiagramLink link : links) {
				if (link instanceof DiagramLink) {
					DiagramLink linkClone = (DiagramLink) link.clone(true);
					getBase().getContext().getDiagramItems().add(linkClone);
					// connect it up if origin or destination nodes were
					// selected and
					// copied over
					DiagramNode origin = link.getOrigin();
					if (origin != null) {
						DiagramNode originCopy = selectedToAddedMap.get(origin);
						if (originCopy != null) {
							linkClone.setOrigin(originCopy);
						}
					}
					DiagramNode dest = link.getDestination();
					if (dest != null) {
						DiagramNode destCopy = selectedToAddedMap.get(dest);
						if (destCopy != null) {
							linkClone.setDestination(destCopy);
						}
					}
				}
			}
		}
	}

	public void onPaste() {
		if (lastMouseClickedPosition == null || getBase().getContext().getDiagramItems() == null
				|| getBase().getContext().getDiagramItems().isEmpty()) {
			return;
		}

		Diagram diagram = getDiagram();
		DiagramView diagramView = getDiagramView();

		CompositeCmd compositeCmd = diagram.getUndoManager().startComposite(
				"PasteALot");
		diagram.getLinkRouter().Suspend();

		DiagramItem diagramItem = getBase().getContext().getDiagramItems().getFirst();
		java.awt.geom.Rectangle2D.Float bounds = diagramItem.getBounds();
		float delX = (float) lastMouseClickedPosition.getX() - bounds.x;
		float delY = (float) lastMouseClickedPosition.getY() - bounds.y;

		if (SchematicContext.getDiagram().equals(diagram)){
			//first add all nodes
			HashMap<DiagramNode, DiagramNode> selectedToAddedMap = new HashMap<>();
			for (DiagramItem itemOriginal : getBase().getContext().getDiagramItems()) {
				if (itemOriginal == null) {
					continue;
				}
				if (!(itemOriginal instanceof DiagramNode)) {
					continue;
				}
				DiagramItem item = itemOriginal.clone(true);
				diagram.add(item, true);
				java.awt.geom.Rectangle2D.Float bounds2 = item.getBounds();
				bounds2.x = bounds2.x + delX;
				bounds2.y = bounds2.y + delY;
				((DiagramNode) item).setBounds(bounds2);
				selectedToAddedMap.put((DiagramNode) itemOriginal,
						(DiagramNode) item);
				//FIXME: alignTo grid
			}
			//then add links connecting them to nodes if selected and added
			for (DiagramItem itemOriginal : getBase().getContext().getDiagramItems()) {
				if (itemOriginal == null) {
					continue;
				}
				if (!(itemOriginal instanceof DiagramLink)) {
					continue;
				}
				DiagramLink linkOriginal = (DiagramLink) itemOriginal;
				DiagramItem item = itemOriginal.clone(true);
				diagram.add(item, true);
				java.awt.geom.Rectangle2D.Float bounds2 = item.getBounds();
				bounds2.x = bounds2.x + delX;
				bounds2.y = bounds2.y + delY;
				DiagramLink link = (DiagramLink) item;
				for (Point2D.Float point : link.getControlPoints()) {
					point.x = point.x + delX;
					point.y = point.y + delY;
				}
				link.updateFromPoints();
				// connect it up if origin or destination nodes were selected and
				// copied over
				DiagramNode origin = linkOriginal.getOrigin();
				if (origin != null) {
					DiagramNode originCopy = selectedToAddedMap.get(origin);
					if (originCopy != null) {
						link.setOrigin(originCopy);
					}
				}
				DiagramNode dest = linkOriginal.getDestination();
				if (dest != null) {
					DiagramNode destCopy = selectedToAddedMap.get(dest);
					if (destCopy != null) {
						link.setDestination(destCopy);
					}
				}
				// FIXME: alignTo grid
			}

			compositeCmd.execute();
		} else {
			try {
				diagramView.pasteFromClipboard(delX, delY, false);
			} catch (IOException | XmlException e) {
				getBase().getContext().getDataSource().reportError(e);
			}
		}
	}

	public void boxClicked(NodeEvent e) {
		if (e.getMouseButton() == MouseEvent.BUTTON3) {
			e.getNode().setLocked(false);

			Component contentPane = getContentPane();
			java.awt.Point mousePos = contentPane.getMousePosition();
			clickedItem = e.getNode();
			popupMenu.show(contentPane, mousePos.x, mousePos.y);
		}
	}

	public void arrowClicked(LinkEvent e) {
		if (e.getMouseButton() == MouseEvent.BUTTON3) {
			e.getLink().setLocked(false);

			Component contentPane = getContentPane();
			java.awt.Point mousePos = contentPane.getMousePosition();

			clickedItem = e.getLink();
			popupMenu.show(contentPane, mousePos.x, mousePos.y);
		}
	}

	public void diagramClicked(DiagramEvent e) {
		if (e.getMouseButton() == MouseEvent.BUTTON3) {

			Point2D event = e.getPointerPosition();
			Component contentPane = getContentPane();
			java.awt.Point mousePos = contentPane.getMousePosition();

			// display context menu at right mouse click
			pastePopupMenu.show(contentPane, mousePos.x, mousePos.y);
			lastMouseClickedPosition = event;
		}
	}


	public void onShortenPoints(DiagramLink link) {
		try {
			PointList controlPoints = link.getControlPoints();
			for (int i = 1; i <= Math.max(1, link.getSegmentCount() - 1); i++) {
				controlPoints.remove(i);
			}
			link.updateFromPoints();
		} catch (Exception e1) {
			logger.atFiner().withCause(e1).log();
		}
	}

	public void onChangePoints(DiagramLink link) {//Corrected by Liheng Zhong, 05/23/2012
		try {
			PointList controlPoints = link.getControlPoints();
			String result = JOptionPane.showInputDialog(
					"Set points in link to: ", controlPoints.size() - 2);
			if (result == null){
				return;
			}
			int npoints = Integer.parseInt(result);
			if (npoints < 0 || npoints == controlPoints.size() - 2)
				return;
			if (npoints < controlPoints.size() - 2) {
				int n_point_delete = controlPoints.size() - 2 - npoints;
				for (int i = 1; i <= n_point_delete; i++){
					controlPoints.remove(1);
				}
			} else {
				java.awt.geom.Point2D.Float float0 = controlPoints.get(0);
				java.awt.geom.Point2D.Float float1 = controlPoints.get(1);
				int n_point_add = npoints - controlPoints.size() + 2;
				for (int i = 1; i <= n_point_add; i++) {
					java.awt.geom.Point2D.Float float2 = (Point2D.Float) float0.clone();
					float2.setLocation((float0.getX() + float1.getX()) / 2, (float0.getY() + float1.getY()) / 2);
					controlPoints.add(1, float2);
					float1 = float2;
				}
			}
			link.updateFromPoints(true);
		} catch (Exception e1) {
			logger.atFiner().withCause(e1).log();
		}
	}

	public void alignSelectedLinks(Orientation direction) {
		for (DiagramLink link : getDiagram().getSelection().getLinks()) {
			alignLinkPoints(link, direction);
		}
		getDiagram().repaint();
	}

	public void alignLinkPoints(DiagramLink link, Orientation direction) {
		PointList controlPoints = link.getControlPoints();
		if (link.getOrigin() instanceof DummyNode
				|| link.getDestination() instanceof DummyNode) {
			logger.atFiner().log("Link: " + link
					+ " is missing origin or destination connections");
			return;
		}
		if (direction == Orientation.Vertical) {
			controlPoints.getFirst().x = (float) link.getOrigin().getBounds()
					.getCenterX();
			controlPoints.getLast().x = (float) link
					.getDestination().getBounds().getCenterX();
		} else if (direction == Orientation.Horizontal) {
			controlPoints.getFirst().y = (float) link.getOrigin().getBounds()
					.getCenterY();
			controlPoints.getLast().y = (float) link
					.getDestination().getBounds().getCenterY();
		}

		Point2D.Float alignPoint = controlPoints.getFirst();
		Point2D.Float endAlignPoint = controlPoints
				.getLast();
		int size = controlPoints.size();
		float stepX = 1;
		float stepY = 1;
		if (size > 1) {
			stepX = (endAlignPoint.x - alignPoint.x) / (size - 1);
			stepY = (endAlignPoint.y - alignPoint.y) / (size - 1);
		}
		int index = 0;
		for (Point2D.Float p : controlPoints) {
			if (index == size - 1)
				break; // don't do the end points
			if (direction == Orientation.Vertical) {
				p.x = alignPoint.x;
				p.y = alignPoint.y + index * stepY;
			} else if (direction == Orientation.Horizontal) {
				p.y = alignPoint.y;
				p.x = alignPoint.x + index * stepX;
			}
			index++;
		}

		try {
			link.updateFromPoints(false);
		} catch (Exception e) {
			logger.atFine().log("Exception while updating link points", e);
		}
	}

	private static final boolean spaceNodesEvenly = false;

	public void alignSelectedNodes(final Orientation orientation) {
		Diagram diagram = getDiagram();
		DiagramNodeList nodes = diagram.getSelection().getNodes();
		List<DiagramNode> sortedNodes = new ArrayList<>(nodes);
		sortedNodes.sort((arg0, arg1) -> switch(orientation)
		{
			case Orientation.Horizontal -> (int) Math.signum(arg0.getBounds().x - arg1.getBounds().x);
			case Orientation.Vertical -> (int) Math.signum(arg0.getBounds().y - arg1.getBounds().y);
			default -> 0;
		});
		if (nodes.isEmpty()) {
			return;
		}
		DiagramNode alignNode = sortedNodes.getFirst();
		Rectangle2D.Float bounds0 = alignNode.getBounds();
		double centerX = bounds0.getCenterX();
		double centerY = bounds0.getCenterY();
		DiagramNode endAlignNode = sortedNodes.getLast();
		Rectangle2D.Float bounds1 = endAlignNode.getBounds();
		int size = nodes.size();
		float spaceX = 1;
		float spaceY = 1;
		if (size > 1) {
			spaceX = (bounds1.x - bounds0.x) / (size - 1);
			spaceY = (bounds1.y - bounds0.y) / (size - 1);
		}
		int index = 0;
		for (DiagramNode n : sortedNodes) {
			java.awt.geom.Rectangle2D.Float bounds = n.getBounds();
			if (orientation == Orientation.Vertical) {
				double delX = bounds.getCenterX() - centerX;
				bounds.x = bounds.x - (float) delX;
				if (spaceNodesEvenly) {
					bounds.y = bounds0.y + index * spaceY - bounds.height / 2;
				}
			} else if (orientation == Orientation.Horizontal) {
				double delY = bounds.getCenterY() - centerY;
				bounds.y = bounds.y - (float) delY;
				if (spaceNodesEvenly) {
					bounds.x = bounds0.x + index * spaceX - bounds.width / 2;
				}
			}
			n.moveTo(bounds.x, bounds.y);
			index++;
		}
		diagram.repaint();
	}

	public void showShapeDialog() {
		JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(getContentPane()),
				"Edit Shape", Dialog.ModalityType.APPLICATION_MODAL);
		EditSchematicElementPanel editPanel = new EditSchematicElementPanel();
		dialog.getContentPane().add(editPanel);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		editPanel.setDiagramItem(clickedItem);
		dialog.pack();
		dialog.show();
		clickedItem.getPen().setColor(editPanel.getColor());
		if (clickedItem instanceof ShapeNode shapeNode) {
			shapeNode.getShape().setId(editPanel.getShape());
			shapeNode.getPen().setColor(editPanel.getColor());
		}
		if (clickedItem instanceof DiagramLink link) {
			link.getHeadPen().setColor(editPanel.getColor());
		}
	}

	public void shrinkDiagramToElements() {
		DiagramItemList items = getDiagram().getItems();
		Rectangle2D.Float calculatedBounds = null;
		for (DiagramItem item : items) {
			java.awt.geom.Rectangle2D.Float itemBounds = item.getBounds();
			if (calculatedBounds == null) {
				calculatedBounds = (Rectangle2D.Float) itemBounds.clone();
			} else {
				calculatedBounds.add(itemBounds);
			}
		}
		getDiagram().setBounds(calculatedBounds);
		getDiagram().repaint();
	}

	public void addToShapeLibrary() throws Exception {
		String filename = getBase().getContext().getDataSource().shapeLibPath();
		if (clickedItem instanceof ShapeNode snode) {
			Shape s = snode.getShape();
			Document doc = XmlUtilities.newDocument();
			doc.appendChild(s.toDOM(doc));
			XmlUtilities.saveTo(doc, filename);
		}
	}

	private void saveAttributesTo(String fileToSave) throws IOException {
		AttributeMapper mapper = new AttributeMapper();
		Diagram d = getDiagram();
		try (PrintWriter wr = new PrintWriter(fileToSave)) {
			ArrayList<DiagramNode> nodes = new ArrayList<>();
			for(DiagramNode n : d.items(DiagramNode.class)) {
				nodes.add(n);
			}

			nodes.sort(new CompartorOnNodeId());

			for(DiagramNode n : nodes) {
				if(n instanceof ShapeNode s) {
					Pen pen = s.getPen();
					Color fillColor = null;
					Brush brush = s.getBrush();
					Shape shape = s.getShape();
					CustomDraw customdraw = s.getCustomDraw();
					if(brush instanceof SolidBrush brushSolid) {
						fillColor = brushSolid.getColor();
					} else {
						logger.atFine().log("brush type is :" + brush.getClass()
								+ " for node: " + ((InplaceEditable) n).getTextToEdit());
					}
					String id = getIdFor(n);
					if(id.length() > 40) {
						id = id.substring(0, 39);
					}

					String attributeName = mapper.getAttributeName(pen, brush, shape, customdraw);
					wr.printf("%-45s|%s|%s|%3.1f|%s|%s|%s|%s%n", id, mapper
							.formatColor(pen.getColor()), mapper.formatStyle(pen
							.getDashStyle()), pen.getWidth(), mapper
							.formatColor(fillColor), mapper.formatShape(shape), mapper.formatCustomDraw(customdraw), attributeName);
				}
			}
		}
	}

	private void loadAttributesFrom(String fileToImport) throws IOException {
		AttributeMapper mapper = new AttributeMapper();
		Diagram d = getDiagram();
		try (LineNumberReader reader = new LineNumberReader(new FileReader(fileToImport)))
		{
			String line = null;
			HashMap<String, AttributeMapper.Attribute> idToAttributeMap = new HashMap<>();
			while((line = reader.readLine()) != null)
			{
				String[] fields = line.split("\\|");
				String id = fields[0].trim();
				AttributeMapper.Attribute attr = null;
				if(fields.length > 7)
				{
					String attributeName = fields[7];
					if(attributeName != null && !attributeName.isEmpty())
					{
						attr = mapper.getAttribute(attributeName);
					}
				}
				if(attr == null)
				{
					String line_attr = line.substring(line.indexOf("|") + 1);

					Pen pen = mapper.parsePen(line_attr);
					Brush brush = mapper.parseBrush(line_attr);
					Shape shape = mapper.parseShape(line_attr);
					CustomDraw customdraw = mapper.parseCustomDraw(line_attr);

					attr = new AttributeMapper.Attribute();
					attr.pen = pen;
					attr.brush = brush;
					attr.shape = shape;
					attr.customdraw = customdraw;
				}

				idToAttributeMap.put(id, attr);
			}

			DiagramNodeList nodes = d.getNodes();
			getDiagramView().suspendRepaint();
			for(DiagramNode n : nodes)
			{
				String id = getIdFor(n);
				if(id.length() > 40)
				{
					id = id.substring(0, 39);
				}
				AttributeMapper.Attribute attribute = idToAttributeMap.get(id);
				if(attribute == null)
				{
					continue;
				}
				if(n instanceof ShapeNode s)
				{
					s.setPen(attribute.pen);
					s.setBrush(attribute.brush);
					s.setShape(attribute.shape);
					s.setCustomDraw(attribute.customdraw);
				}
			}
		}
		getDiagramView().resumeRepaint();
	}

	public String getIdFor(DiagramNode n){
		String text = ((InplaceEditable)n).getTextToEdit();
		//return text == null ? null : text.split("\n")[0].trim();
		return text == null ? null : text.trim().replace("\n", "*#*");
	}

	public String chooseFileToImport() {
		Preferences p = Preferences.userNodeForPackage(SchematicEditor.class);
		String currentDirectory = p.get("SCHEMATICS_DIRECTORY", System
				.getProperty("user.dir"));
		JFileChooser chooser = new JFileChooser(currentDirectory);
		chooser.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "text";
			}

			@Override
			public boolean accept(File f) {
				return f != null && f.isFile()
						&& (f.getName().toLowerCase().endsWith(".txt"))
						|| f.isDirectory();
			}

		});
		int rval = chooser.showOpenDialog(getContentPane());
		if (rval != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		File selectedFile = chooser.getSelectedFile();
		p.put("SCHEMATICS_DIRECTORY", selectedFile.getParent());
		return selectedFile.getAbsolutePath();
	}

	public String chooseFileToExport() {
		Preferences p = Preferences.userNodeForPackage(SchematicEditor.class);
		String currentDirectory = p.get("SCHEMATICS_DIRECTORY", System
				.getProperty("user.dir"));
		JFileChooser chooser = new JFileChooser(currentDirectory);
		chooser.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "text";
			}

			@Override
			public boolean accept(File f) {
				return f != null && f.isFile() || f.isDirectory();
			}
		});
		int rval = chooser.showSaveDialog(getContentPane());
		if (rval != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		File selectedFile = chooser.getSelectedFile();
		p.put("SCHEMATICS_DIRECTORY", selectedFile.getParent());
		return selectedFile.getAbsolutePath();
	}

	public void load() {
		getDiagram().setAdjustmentHandlesSize(6);
		getDiagram().setAdjustmentHandlesSize(8);

		getDiagram().setDynamicLinks(true);
		getDiagram().setAlignToGrid(true);

		getDiagram().setGridColor(Color.blue);
		getDiagram().setGridSizeX(10);
		getDiagram().setGridSizeY(10);
		getDiagram().setGridStyle(GridStyle.Lines);

		RoutingOptions routingOptions = getDiagram().getRoutingOptions();
		routingOptions.setEndOrientation(Orientation.Auto);
		routingOptions.setStartOrientation(Orientation.Auto);
		routingOptions.setLengthCost((short) 100);
		routingOptions.setSmartPolylineEnds(true);
		routingOptions.setMinSegmentsMode();
		routingOptions.setTriggerRerouting(EnumSet.noneOf(RerouteLinks.class));

		UndoManager undoManager = getDiagram().getUndoManager();
		undoManager.setUndoEnabled(true);
		redoAction.setEnabled(false);
		undoAction.setEnabled(false);
	}
}
