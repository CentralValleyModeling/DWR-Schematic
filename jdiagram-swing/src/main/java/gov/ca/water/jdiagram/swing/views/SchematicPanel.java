package gov.ca.water.jdiagram.swing.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import com.google.common.flogger.FluentLogger;
import com.mindfusion.diagramming.AttachToNode;
import com.mindfusion.diagramming.Behavior;
import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramAdapter;
import com.mindfusion.diagramming.DiagramItem;
import com.mindfusion.diagramming.DiagramNode;
import com.mindfusion.diagramming.DiagramNodeList;
import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.Group;
import com.mindfusion.diagramming.InplaceEditable;
import com.mindfusion.diagramming.LinkEvent;
import com.mindfusion.diagramming.NodeEvent;
import com.mindfusion.diagramming.ShapeNode;
import com.mindfusion.diagramming.export.SvgExporter;
import com.mindfusion.drawing.Align;
import com.mindfusion.drawing.TextFormat;
import gov.ca.water.jdiagram.core.SchematicLicense;
import gov.ca.water.jdiagram.core.SchematicPanelBase;
import gov.ca.water.jdiagram.swing.DataPresenter;
import gov.ca.water.jdiagram.swing.RectangleZoomBehavior;
import gov.ca.water.jdiagram.swing.dialog.DecimalDialog;
import gov.ca.water.jdiagram.swing.dialog.PDFOptionDialog;
import gov.ca.water.jdiagram.swing.panel.MagnifierPanel;
import gov.ca.water.jdiagram.swing.toolbars.DateCombo;
import gov.ca.water.jdiagram.swing.toolbars.SearchText;

import hec.hecmath.HecMathException;
import hec.io.DataContainer;
import rma.swing.FontChooser;

import static gov.ca.water.jdiagram.swing.utils.ActionHelper.buildAction;
import static gov.ca.water.jdiagram.swing.utils.ActionHelper.icon;
import static gov.ca.water.jdiagram.swing.utils.ActionHelper.openIcon;
import static gov.ca.water.jdiagram.swing.utils.ActionHelper.saveIcon;

public class SchematicPanel extends JComponent
{
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private DiagramView diagramView;

	private final transient DataPresenter presenter;

	private transient Diagram diagram;

	private JPanel swingContainer;

	private Container contentPane;

	private float _zoomFactor = 100;

	protected transient DiagramAdapter listener;

	private SchematicOverview overview;

	private int index_lastnode = 0;

	private final transient DiagramSelectionProvider selectionProvider = new DiagramSelectionProvider();

	protected transient DateCombo dateCombo;

	private transient Action zoomInAction;

	private transient Action zoomOutAction;

	private transient Action zoomRectAction;

	private transient Action zoomMagnifier;

	private Rectangle2D.Float lastVisibleRect;

	private transient Action openSchematicAction;

	private transient Action saveAsSchematicAction;

	private transient Action zoomNormalAction;

	private transient Action fontAction;

	private transient Action decimalAction;

	private transient Action tafTypeAction;

	private transient Action forwardAction;

	private transient Action backwardAction;

	private final transient SchematicPanelBase base;

	private MagnifierPanel magnifier;

	private Behavior behavior = Behavior.SelectOnly;

	private static final Object VALUE_TEXT = "__VT__";

	private static String DECIMAL_PLACES = "decimalPlaces";

	private static String ISMONTHLYTAF = "ismonthlytaf";

	public SchematicPanel(SchematicPanelBase base, DataPresenter presenter) {
		this.base = base;
		this.presenter = presenter;
	}

	public SchematicPanelBase getBase() {
		return base;
	}

	public DataPresenter getDataPresenter() {
		return presenter;
	}

	public Component getContentPane(){
		return contentPane;
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(JPanel parent) {
		System.setProperty("sun.awt.noerasebackground", "true");
		swingContainer = new JPanel();
		Panel panel = new Panel(new BorderLayout()) {
			@Override
			public void update(java.awt.Graphics g) {
				/* Do not erase the background */
				paint(g);
			}
		};
		swingContainer.add(panel);
		JRootPane root = new JRootPane();
		panel.add(root);
		parent.add(swingContainer);
		contentPane = root.getContentPane();
		diagram = new Diagram();
		diagramView = new DiagramView(diagram);
		SchematicLicense.apply();
		diagramView.setAllowInplaceEdit(false);
		diagramView.setBehavior(Behavior.Pan);

		listener = new DiagramAdapter() {
			private Timer refreshTimer = new Timer();
			private TimerTask refreshTask = new TimerTask(){
				@Override
				public void run() {
				}};

			private final int delay = 250;


			@Override
			public void linkClicked(LinkEvent e) {
				selectionProvider.setSelection(List.of(e.getLink()));
			}

			@Override
			public void linkDoubleClicked(LinkEvent e) {
				showDSS(e.getLink().getTextToEdit());
			}

			@Override
			public void nodeClicked(NodeEvent e) {
				selectionProvider.setSelection(List.of(e.getNode()));
			}

			@Override
			public void nodeDoubleClicked(NodeEvent e) {
				DiagramNode n = e.getNode();
				showDSS(((InplaceEditable)n).getTextToEdit());
			}

			public void openDSSPerspective(final String perspectiveId, final String name) {
				presenter.showDSSPerspective();
			}

			public void showDSS(String name) {
				if(name == null) {
					return;
				}
				name = name.toLowerCase();
				final Vector<DataContainer> data=new Vector<>();
				for (int i = 0; i < 4; i++){
					if (base.getHost().getSchematicVariableData(i).containsKey(name)){
						try {
							data.add(base.getHost().getSchematicVariableData(i).get(name).getData());
						} catch (HecMathException e) {
							logger.atFine().withCause(e).log("Error getting data for variable %s", name);
						}
					}
				}
				if(data.isEmpty()) {
					return;
				}
				presenter.showDssView();
			}

			@Override
			public void viewportChanged() {
				refreshTask.cancel();
				refreshTimer.cancel();
				refreshTimer = new Timer();
				refreshTask = new TimerTask() {

					@Override
					public void run() {
						SwingUtilities.invokeLater(() -> {
							if (!base.getDataSource().isDebugging()){
								refreshValues(0, false);
							}else{
								if (base.getDataSource().target() != null) {
									refreshValues(0, false);
								}
							}
						});
					}
				};
				refreshTimer.schedule(refreshTask, delay);
			}
		};
		diagram.addDiagramListener(listener);
		contentPane.add(new JScrollPane(diagramView));

		presenter.launchViewPart(overview, diagramView, swingContainer);

		makeActions();
		contributeToActionBars();
	}

	private void contributeToActionBars() {
		fillLocalToolBar(presenter.getToolBar());
	}

	private void fillLocalToolBar(JToolBar manager) {
  SearchText searchText;
		manager.add(openSchematicAction);
		manager.add(saveAsSchematicAction);
		manager.add(zoomInAction);
		manager.add(zoomOutAction);
		manager.add(zoomNormalAction);
		manager.add(zoomRectAction);
		manager.add(zoomMagnifier);
		manager.add(fontAction);
		manager.add(decimalAction);
		manager.add(tafTypeAction);
		dateCombo = new DateCombo(this, presenter);
		manager.add(dateCombo.getDateList());
		manager.add(backwardAction);
		manager.add(forwardAction);
		searchText = new SearchText(this);
		manager.add(searchText);
	}


	private void makeActions() {
  		_zoomFactor = 100;
		openSchematicAction = buildAction("Open", openIcon(), e -> {
			final String file = presenter.launchFileDialog();
			if (file == null) {
				return;
			}

			SwingUtilities.invokeLater(() -> {
				try {
					diagramView.suspendRepaint();
					if (file.endsWith(".xml")) {
						diagram.loadFromXml(file);
					}else {
						diagram.loadFrom(file);
					}
					Rectangle2D rect = diagram.getBounds();
					setupForEditor();
					diagramView.zoomToFit(rect);
					diagramView.resumeRepaint();
					collectAllSchematicVariables();
					getBase().loadAllSchematicVariableData();
				} catch (Exception ex) {
					logger.atFine().withCause(ex).log();
					getBase().getHost().reportError(ex);
				}
			});
		});

		saveAsSchematicAction = buildAction("Save As", saveIcon(), e -> {
			String file = presenter.launchFileDialog();
			if (file == null) {
				return;
			}
			try {
				save(file);
			} catch (Exception ex) {
				getBase().getHost().reportError(ex);
			}
		});

		zoomNormalAction = buildAction("Zoom 100", icon("ZoomNormal24.gif"), e -> {
			_zoomFactor = 100;
			diagramView.zoomToFit();
			overview.setDiagramView(diagramView);
		});

		zoomInAction = buildAction("Zoom In", icon("ZoomIn24.gif"), e -> {
			Rectangle2D.Float rect = diagramView.deviceToDoc(diagramView.getVisibleRect());
			rect.setRect(rect.x + rect.width / 4, rect.y + rect.height / 4, rect.width / 2, rect.height / 2);
			diagramView.zoomToFit(rect);
			overview.setDiagramView(diagramView);
		});

		zoomOutAction = buildAction("Zoom Out", icon("ZoomOut24.gif"), e -> {
			Rectangle2D.Float rect = diagramView.deviceToDoc(diagramView.getVisibleRect());
			rect.setRect(rect.x-rect.width/2, rect.y-rect.height/2, rect.width*2, rect.height*2);
			diagramView.zoomToFit(rect);
			overview.setDiagramView(diagramView);
		});

		zoomRectAction = buildAction("Zoom to Rectangular", icon("ZoomToFit24.gif"), e -> {
			base.getDataSource().setZoomToRect(!base.getDataSource().zoomToRect());
			if (base.getDataSource().zoomToRect()) {
				behavior = diagramView.getBehavior();
				diagramView.setCustomBehavior(new RectangleZoomBehavior(
						diagramView, overview));
			} else {
				diagramView.setBehavior(Behavior.SelectOnly);
				diagramView.setBehavior(behavior);
			}
		});

		zoomMagnifier = buildAction("Magnifier", icon("zoom_magnifier.png"), e -> {
			if (magnifier == null) {
				magnifier = new MagnifierPanel(diagramView);
			}
			base.getDataSource().setShowMagnifier(!base.getDataSource().showMagnifier());
			magnifier.setShowing(base.getDataSource().showMagnifier());
		});

		fontAction = buildAction("Font", icon("font.png"), e -> {
			presenter.launchDialog(() -> {
				Frame frame = (Frame) presenter.getShell(this);
				Font oldFont = getDiagram().getFont();
				presenter.launchFontDialog();
				FontChooser chooser = new FontChooser(frame, null, false, oldFont);
				chooser.setFont(oldFont);
				chooser.setVisible(true);

				Font newFont = chooser.getSelectedFont();
				getDiagram().setFont(newFont);
				Diagram diagram = getDiagram();
				for (DiagramItem item : diagram.items(DiagramItem.class)) {
					item.setFont(newFont);
					if (item instanceof ShapeNode) {
						ShapeNode snode = (ShapeNode) item;
					}
				}
			});
		});

		decimalAction = buildAction("Decimal", icon("decimal.png"), e -> {
			presenter.launchDialog(() -> {
				Composite parent = presenter.getShell(this);
				DecimalDialog decimalDialog = new DecimalDialog((JFrame) parent, this, presenter);
				decimalDialog.openDialog();
			});
		});

		if(getBase().getHost().getPreferences() == null)
		{
			getBase().getHost().setPreferences(Preferences.userNodeForPackage(getClass()));
		}
		getBase().getDataSource().setTAFMonthly(getBase().getHost().getPreferences().getBoolean(ISMONTHLYTAF, true));
		tafTypeAction = buildAction("TAF Type", null, e -> {
			presenter.launchDialog(() -> {
				base.getDataSource().setTAFMonthly(!base.getDataSource().isTAFMonthly());
				if (base.getDataSource().isTAFMonthly()) {
					getBase().getHost().getPreferences().putBoolean(ISMONTHLYTAF, true);
					tafTypeAction.putValue(Action.SMALL_ICON, icon("M.png"));
				}else{
					getBase().getHost().getPreferences().putBoolean(ISMONTHLYTAF, false);
					tafTypeAction.putValue(Action.SMALL_ICON, icon("Y.png"));
				}
			});
		});
		if (base.getDataSource().isTAFMonthly()) {
			tafTypeAction.putValue(Action.SMALL_ICON, icon("M.png"));
		} else {
			tafTypeAction.putValue(Action.SMALL_ICON, icon("Y.png"));
		}

		forwardAction = buildAction("Forward", icon("forward.png"), e -> {
			int twSize = base.getHost().getSchematicTwSelections().size();
			JComboBox<String> dateList = dateCombo.getDateList();
			int size = dateList.getItemCount();
			if (getBase().getHost().getSelectedIndex() < twSize){
				if (twSize < size){
					base.getHost().updateSelectedIndex(twSize);
					dateList.setSelectedIndex(base.getHost().getSelectedIndex());
				}
			} else {
				if (base.getHost().getSelectedIndex() + 1 < size){
					base.getHost().updateSelectedIndex(base.getHost().getSelectedIndex() + 1);
					dateList.setSelectedIndex(base.getHost().getSelectedIndex());
				}
			}
		});

		backwardAction = buildAction("Backward", icon("backward.png"), e -> {
			int twSize = getBase().getHost().getSchematicTwSelections().size();
			JComboBox<String> dateList = dateCombo.getDateList();
			int size = dateList.getItemCount();
			if (base.getHost().getSelectedIndex() <= twSize){
				if (twSize < size){
					base.getHost().updateSelectedIndex(twSize);
					dateList.setSelectedIndex(base.getHost().getSelectedIndex());
				}
			} else {
				if (base.getHost().getSelectedIndex() >= size){
					base.getHost().updateSelectedIndex(size - 1);
					dateList.setSelectedIndex(base.getHost().getSelectedIndex());
				} else {
					base.getHost().updateSelectedIndex(base.getHost().getSelectedIndex() - 1);
					dateList.setSelectedIndex(base.getHost().getSelectedIndex());
				}
			}
		});
	}

	public void setupForEditor(){
		if (this instanceof SchematicEditor edit) {
			edit.load();
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		swingContainer.requestFocusInWindow();
	}

	public Diagram getDiagram() {
		return diagram;
	}

	public DiagramView getDiagramView() {
		return diagramView;
	}

	/**
	 * @return the selection provider; register a
	 * {@link DiagramSelectionProvider.SelectionChangedListener} (e.g.
	 * {@link DiagramPropertiesTableModel}) to react to node/link clicks
	 */
	public DiagramSelectionProvider getSelectionProvider() {
		return selectionProvider;
	}

	public void refreshValues(final int index, boolean force) {
		synchronized (diagramView) {
			final Rectangle2D.Float visibleRect = diagramView.deviceToDoc(diagramView
					.getVisibleRect());
			if (!force && lastVisibleRect==null) {
				return;
			}
			if (lastVisibleRect == null) {
				lastVisibleRect = visibleRect;
			}
			clearValueBoxes(lastVisibleRect);
			lastVisibleRect = visibleRect;

			final String selDate = base.getHost().getSelectedDate();
			new Thread(() -> updateValues(index, selDate, visibleRect)).start();
		}
	}

	public void updateValues(int index, String date, Rectangle2D.Float visibleRect){
		Hashtable<String, String>[] values = null;
		Hashtable<String, Object> visibleNodes = getVisibleNodes(visibleRect);

		final Hashtable<String, Object> names = new Hashtable<String, Object>();
		for (String v : visibleNodes.keySet()) {
			names.put(v, v);
		}
		if (index==0){
			values = base.retrieveUndebug(date, names);
		}else{
			values = base.retrieveDebug(date, names);
		}
		setValues(index, visibleNodes, values);
	}

	public void setValues(int index, Hashtable<String, Object> visibleNodes, Hashtable<String, String>[] values) {
		try {
			diagramView.suspendRepaint();
			synchronized (diagramView) {
				for (int studyId = 0; studyId < values.length; studyId++) {
					Hashtable<String, String> valuesInStudy = values[studyId];
					for (String name : valuesInStudy.keySet()) {
						Object object = visibleNodes.get(name.toUpperCase());
						if (object == null) {
							continue;
						}
						String value = valuesInStudy.get(name);
						value = getBase().truncateAfterDecimal(value,
								getBase().getHost().getPreferences().getInt(DECIMAL_PLACES, 0),
								true);
						if (object instanceof ShapeNode) {
							ShapeNode shapeNode = (ShapeNode) object;
							Group subordinateGroup = shapeNode
									.getSubordinateGroup();
							DiagramNode diagramNode = null;
							if (subordinateGroup != null) {
								DiagramNodeList attachedNodes = subordinateGroup
										.getAttachedNodes();
								if (studyId > attachedNodes.size() - 1) {
									diagramNode = createTextNodeWithIntermediates(
											studyId, attachedNodes.size(),
											shapeNode);
								} else {
									diagramNode = attachedNodes.get(studyId);
								}
							} else {
								diagramNode = createTextNodeWithIntermediates(
										studyId, 0, shapeNode);
							}
							if (diagramNode != null) {
								((InplaceEditable) diagramNode).setEditedText(value);
								if (getBase().getDataSource().mode().equals(getBase().getDataSource().diff()) && studyId > 0) {
									diagramNode.getPen().setColor(Color.red);
								} else {
									diagramNode.getPen().setColor(Color.black);
								}
								diagramNode.setToolTip(getBase().getToolTip(index, studyId,
										getBase().getDataSource().mode().equals(getBase().getDataSource().diff())));
							}
						}
					}
				}
			}
		} finally {
			diagramView.resumeRepaint();
		}
	}

	public Hashtable<String, Object> getVisibleNodes(Rectangle2D.Float visibleRect) {
		Hashtable<String, Object> variables = new Hashtable<>();
		Iterable<ShapeNode> items = diagram.items(ShapeNode.class);
		for (ShapeNode item : items) {
			if (!item.getBounds().intersects(visibleRect))
				continue;
			if (item.getTransparent()) {// ignore transparent text nodes
				continue;
			}
			variables.put(item.getTextToEdit(), item);
		}
		return variables;
	}

	public boolean findInView(String text) {
		if (diagram == null || text == null) {
			return false;
		}
		text = text.toLowerCase();
		DiagramNodeList nodes = diagram.getNodes();
		int n_node = nodes.size();
		for (int i_node = 1; i_node <= n_node; i_node++){
			final int index_currentnode = (i_node + index_lastnode) % n_node;
			final DiagramNode n = nodes.get(index_currentnode);
			String nodeText = ((InplaceEditable) n).getTextToEdit();
			if (nodeText != null && nodeText.toLowerCase().contains(text)) {
				SwingUtilities.invokeLater(() -> {
					diagramView.bringIntoView(n);
					index_lastnode = index_currentnode;
				});
				return true;
			}
		}
		return false;
	}

	private ShapeNode createTextNodeWithIntermediates(int studyId,
			int startingWithId, ShapeNode shapeNode) {
		ShapeNode textNode = null;
		for (int id = startingWithId; id <= studyId; id++){
			textNode = createTextNode(id, shapeNode);
		}
		return textNode;
	}

	private ShapeNode createTextNode(int studyId, ShapeNode shapeNode) {
		Rectangle2D.Float r = shapeNode.getBounds();
		Rectangle2D.Float r2 = null;
		TextFormat tf = null;
		AttachToNode attachPos = AttachToNode.BottomLeft;
		switch (studyId) {
			case 0:
				attachPos = AttachToNode.BottomLeft;
				r2 = new Rectangle2D.Float(r.x, r.y + r.height / 2, r.width / 2,
						r.height / 2);
				tf = new TextFormat(Align.Near, Align.Far);
				break;
			case 1:
				attachPos = AttachToNode.BottomRight;
				r2 = new Rectangle2D.Float(r.x + r.width / 2, r.y + r.height / 2,
						r.width / 2, r.height / 2);
				tf = new TextFormat(Align.Far, Align.Far);
				break;
			case 2:
				attachPos = AttachToNode.TopLeft;
				r2 = new Rectangle2D.Float(r.x, r.y, r.width / 2, r.height / 2);
				tf = new TextFormat(Align.Near, Align.Near);
				break;
			case 3:
				attachPos = AttachToNode.TopRight;
				r2 = new Rectangle2D.Float(r.x + r.width / 2, r.y, r.width / 2,
						r.height / 2);
				tf = new TextFormat(Align.Far, Align.Near);
				break;
			default:
				attachPos = AttachToNode.TopCenter;
				r2 = new Rectangle2D.Float(r.x, r.y + r.height / 2, r.width / 2,
						r.height / 2);
				tf = new TextFormat(Align.Center, Align.Center);
				break;
		}

		ShapeNode transparentTextNode = diagram.getFactory()
				.createShapeNode(r2);
		transparentTextNode.setId(VALUE_TEXT);
		transparentTextNode.setTransparent(true);
		transparentTextNode.setLocked(true);
		transparentTextNode.setFont(shapeNode.getFont());
		transparentTextNode.setPen(shapeNode.getPen());
		transparentTextNode.setBrush(shapeNode.getBrush());
		transparentTextNode.setTextFormat(tf);
		transparentTextNode.attachTo(shapeNode, attachPos);
		return transparentTextNode;
	}

	public void clearValueBoxes(Rectangle2D.Float visibleRect) {
		if (visibleRect == null) {
			return;
		}
		Hashtable<String, Object> visibleNodes = getVisibleNodes(visibleRect);
		for (Object o : visibleNodes.values()) {
			if (o instanceof ShapeNode) {
				ShapeNode shapeNode = (ShapeNode) o;
				Object id = shapeNode.getId();
				if (id != null && id.equals(VALUE_TEXT)) {
					shapeNode.setText("");
				}
			}
		}
	}

	public void save(final String filename) throws Exception {
		if (filename == null)
			return;
		Cursor previousCursor = diagramView.getCursor();
		diagramView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			if (filename.endsWith(".xml")) {
				diagram.saveToXml(filename);
			} else if (filename.endsWith(".pdf")) {
				presenter.launchDialog(() -> {
					Composite window = presenter.getShell(diagramView);
					JFrame frame = (JFrame) window;
					PDFOptionDialog dialog= new PDFOptionDialog(frame, filename, diagram, base.getDataSource());
					dialog.openDialog();
				});
			} else if (filename.endsWith(".svg")) {
				SvgExporter svgExp = new SvgExporter(diagram, filename);
				svgExp.export();
			} else {
				diagram.saveTo(filename);
			}
		} finally {
			diagramView.setCursor(previousCursor);
		}
	}

	public void collectAllSchematicVariables() {
		if (this instanceof SchematicView){
			Hashtable<String, Object> allNodes = getVisibleNodes(diagram.getBounds());

			base.getHost().setAllSchematicVariableNames(new ArrayList<>());
			for (String v : allNodes.keySet()) {
				base.getHost().getAllSchematicVariableNames().add(v);
			}
		}
	}
}
