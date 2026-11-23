package gov.ca.water.jdiagram.swing.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
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
import com.mindfusion.drawing.Pen;
import com.mindfusion.diagramming.XmlException;
import com.mindfusion.diagramming.export.SvgExporter;
import com.mindfusion.drawing.Align;
import com.mindfusion.drawing.TextFormat;
import gov.ca.water.jdiagram.core.SchematicContext;
import gov.ca.water.jdiagram.core.SchematicLicense;
import gov.ca.water.jdiagram.core.SchematicPanelBase;
import gov.ca.water.jdiagram.swing.PlatformHarness;
import gov.ca.water.jdiagram.swing.behavior.DiagramPropertiesTableModel;
import gov.ca.water.jdiagram.swing.behavior.DiagramSelectionProvider;
import gov.ca.water.jdiagram.swing.behavior.RectangleZoomBehavior;
import gov.ca.water.jdiagram.swing.dialog.DecimalDialog;
import gov.ca.water.jdiagram.swing.dialog.PDFOptionDialog;
import gov.ca.water.jdiagram.swing.panel.MagnifierPanel;
import gov.ca.water.jdiagram.swing.toolbars.DateCombo;
import gov.ca.water.jdiagram.swing.toolbars.SearchText;
import gov.ca.water.jdiagram.swing.toolbars.ToolBarContributor;

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

	private final transient PlatformHarness presenter;

	private transient Diagram diagram;

	private JPanel swingContainer;

	private Container contentPane;

	private float _zoomFactor = 100;

	protected transient DiagramAdapter listener;

	private SchematicOverview overview;

	private int index_lastnode = 0;

	private final transient DiagramSelectionProvider selectionProvider = new DiagramSelectionProvider();

	protected transient DateCombo dateCombo;

	private SearchText searchText;

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

	public SchematicPanel(SchematicPanelBase base, PlatformHarness presenter) {
		this.base = base;
		this.presenter = presenter;
	}

	public SchematicPanelBase getBase() {
		return base;
	}

	public PlatformHarness getDataPresenter() {
		return presenter;
	}

	public Component getContentPane(){
		return contentPane;
	}

	public SchematicOverview getOverview() {
		return overview;
	}

	public DateCombo getDateCombo() {
		return dateCombo;
	}

	public SearchText getSearchText() {
		return searchText;
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(JPanel parent) {
		// Pure Swing. The legacy Eclipse version wrapped the view in a heavyweight java.awt.Panel + JRootPane
		// (the SWT_AWT embedding pattern) and set sun.awt.noerasebackground. Lightweight popups such as the
		// date combo's list then left unrepainted grey boxes over the diagram when closed. A host that embeds
		// this in SWT should create that heavyweight bridge itself, around the parent it passes in here.
		swingContainer = new JPanel(new BorderLayout());
		JPanel content = new JPanel(new BorderLayout());
		swingContainer.add(content, BorderLayout.CENTER);
		parent.add(swingContainer);
		contentPane = content;
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

			private static final int delay = 250;


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
					if (base.getContext().getDataSource().getSchematicVariableData(i).containsKey(name)){
						try {
							data.add(base.getContext().getDataSource().getSchematicVariableData(i).get(name).getData());
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
							if (!base.getContext().getDataSource().isDebugging()){
								refreshValues(0, false);
							}else{
								if (base.getContext().getDataSource().target() != null) {
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

		overview = new SchematicOverview();
		overview.setDiagramView(diagramView);
		presenter.launchViewPart(overview, diagramView, swingContainer);

		makeActions();
		contributeToActionBars();
	}

	private void contributeToActionBars() {
		fillLocalToolBar(presenter.getToolBarContributor());
	}

	/** Toolbar contribution ids for the Swing controls. */
	public static final String DATE_COMBO_ID = "dateCombo";
	public static final String SEARCH_TEXT_ID = "searchText";

	/**
	 * Fills a Swing toolbar with the schematic actions, date combo and search field.
	 */
	public void fillLocalToolBar(JToolBar toolBar) {
		fillLocalToolBar(ToolBarContributor.of(toolBar));
	}

	/**
	 * Fills any toolbar (Swing {@link JToolBar}, Eclipse {@code IToolBarManager}, ...) with the schematic actions,
	 * date combo and search field. Call on the AWT event thread after {@link #createPartControl(JPanel)}.
	 * <p>
	 * The date combo and search field are single Swing components, so filling a second toolbar moves them there.
	 */
	public void fillLocalToolBar(ToolBarContributor toolBar) {
		if (dateCombo == null) {
			dateCombo = new DateCombo(this, presenter);
		}
		if (searchText == null) {
			searchText = new SearchText(this);
		}
		toolBar.addAction(openSchematicAction);
		toolBar.addAction(saveAsSchematicAction);
		toolBar.addAction(zoomInAction);
		toolBar.addAction(zoomOutAction);
		toolBar.addAction(zoomNormalAction);
		toolBar.addAction(zoomRectAction);
		toolBar.addAction(zoomMagnifier);
		toolBar.addAction(fontAction);
		toolBar.addAction(decimalAction);
		toolBar.addAction(tafTypeAction);
		toolBar.addComponent(DATE_COMBO_ID, dateCombo.getControl());
		toolBar.addAction(backwardAction);
		toolBar.addAction(forwardAction);
		toolBar.addComponent(SEARCH_TEXT_ID, searchText);
		toolBar.update();
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
					getBase().getContext().getDataSource().reportError(ex);
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
				getBase().getContext().getDataSource().reportError(ex);
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
			base.getContext().setZoomToRect(!base.getContext().zoomToRect());
			if (base.getContext().zoomToRect()) {
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
			base.getContext().setShowMagnifier(!base.getContext().showMagnifier());
			magnifier.setShowing(base.getContext().showMagnifier());
		});

		fontAction = buildAction("Font", icon("font.png"), e -> {
			presenter.launchDialog(() -> {
				Window window = presenter.getShell(diagramView);
				Frame frame = window instanceof Frame f ? f : null;
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
				Window window = presenter.getShell(diagramView);
				JFrame parent = window instanceof JFrame f ? f : null;
				DecimalDialog decimalDialog = new DecimalDialog(parent, this, presenter);
				decimalDialog.openDialog();
			});
		});

		if(getBase().getContext().getDataSource().getPreferences() == null)
		{
			getBase().getContext().getDataSource().setPreferences(Preferences.userNodeForPackage(getClass()));
		}
		getBase().getContext().setTAFMonthly(getBase().getContext().getDataSource().getPreferences().getBoolean(ISMONTHLYTAF, true));
		tafTypeAction = buildAction("TAF Type", null, e -> {
			presenter.launchDialog(() -> {
				base.getContext().setTAFMonthly(!base.getContext().isTAFMonthly());
				if (base.getContext().isTAFMonthly()) {
					getBase().getContext().getDataSource().getPreferences().putBoolean(ISMONTHLYTAF, true);
					tafTypeAction.putValue(Action.SMALL_ICON, icon("M.png"));
				}else{
					getBase().getContext().getDataSource().getPreferences().putBoolean(ISMONTHLYTAF, false);
					tafTypeAction.putValue(Action.SMALL_ICON, icon("Y.png"));
				}
			});
		});
		if (base.getContext().isTAFMonthly()) {
			tafTypeAction.putValue(Action.SMALL_ICON, icon("M.png"));
		} else {
			tafTypeAction.putValue(Action.SMALL_ICON, icon("Y.png"));
		}

		forwardAction = buildAction("Forward", icon("forward.png"), e -> {
			int twSize = base.getContext().getSchematicTwSelections().size();
			JComboBox<String> dateList = dateCombo.getDateList();
			int size = dateList.getItemCount();
			if (getBase().getContext().getSelectedIndex() < twSize){
				if (twSize < size){
					base.getContext().updateSelectedIndex(twSize);
					dateList.setSelectedIndex(base.getContext().getSelectedIndex());
				}
			} else {
				if (base.getContext().getSelectedIndex() + 1 < size){
					base.getContext().updateSelectedIndex(base.getContext().getSelectedIndex() + 1);
					dateList.setSelectedIndex(base.getContext().getSelectedIndex());
				}
			}
		});

		backwardAction = buildAction("Backward", icon("backward.png"), e -> {
			int twSize = base.getContext().getSchematicTwSelections().size();
			JComboBox<String> dateList = dateCombo.getDateList();
			int size = dateList.getItemCount();
			if (base.getContext().getSelectedIndex() <= twSize){
				if (twSize < size){
					base.getContext().updateSelectedIndex(twSize);
					dateList.setSelectedIndex(base.getContext().getSelectedIndex());
				}
			} else {
				if (base.getContext().getSelectedIndex() >= size){
					base.getContext().updateSelectedIndex(size - 1);
					dateList.setSelectedIndex(base.getContext().getSelectedIndex());
				} else {
					base.getContext().updateSelectedIndex(base.getContext().getSelectedIndex() - 1);
					dateList.setSelectedIndex(base.getContext().getSelectedIndex());
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

			final String selDate = base.getContext().getSelectedDateOption();
			if (selDate != null) {
				new Thread(() -> updateValues(index, selDate, visibleRect)).start();
			}
		}
	}

	public void updateValues(int index, String date, Rectangle2D.Float visibleRect) {
		Map<String, String>[] values = null;
		Map<String, Object> visibleNodes = getVisibleNodes(visibleRect);

		final Map<String, Object> names = new Hashtable<>();
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

	public void setValues(int index, Map<String, Object> visibleNodes, Map<String, String>[] values) {
		try {
			diagramView.suspendRepaint();
			synchronized (diagramView) {
				for (int studyId = 0; studyId < values.length; studyId++) {
					Map<String, String> valuesInStudy = values[studyId];
					for (String name : valuesInStudy.keySet()) {
						Object object = visibleNodes.get(name.toUpperCase());
						if (object == null) {
							continue;
						}
						String value = valuesInStudy.get(name);
						value = getBase().truncateAfterDecimal(value,
								getBase().getContext().getDataSource().getPreferences().getInt(DECIMAL_PLACES, 0),
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
								boolean isDiffAlt = getBase().getContext().mode().equals(SchematicContext.DIFF) && studyId > 0;
								setValueColor(diagramNode, isDiffAlt ? Color.red : Color.black);
								diagramNode.setToolTip(getBase().getToolTip(index, studyId,
										getBase().getContext().mode().equals(SchematicContext.DIFF)));
							}
						}
					}
				}
			}
		} finally {
			diagramView.resumeRepaint();
		}
	}

	public Map<String, Object> getVisibleNodes(Rectangle2D.Float visibleRect) {
		Map<String, Object> variables = new Hashtable<>();
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

	/**
	 * Finds the next node (after the previous match) whose text contains {@code text}, case-insensitively,
	 * then selects it and scrolls it into view. Repeated calls cycle through the matches.
	 *
	 * @return true if a match was found
	 */
	public boolean findInView(String text) {
		if (diagram == null || text == null || text.isBlank()) {
			return false;
		}
		String needle = text.trim().toLowerCase();
		DiagramNodeList nodes = diagram.getNodes();
		int count = nodes.size();
		for (int step = 1; step <= count; step++) {
			int index = (index_lastnode + step) % count;
			DiagramNode n = nodes.get(index);
			if (!isSearchable(n)) {
				continue;
			}
			String nodeText = ((InplaceEditable) n).getTextToEdit();
			if (nodeText != null && nodeText.toLowerCase().contains(needle)) {
				// Record the position now, not in invokeLater, so quick repeated searches advance
				index_lastnode = index;
				showSearchResult(n);
				return true;
			}
		}
		return false;
	}

	/**
	 * Scrolls so {@code n} is centered, keeping the current zoom. Does nothing if it is already fully visible.
	 * <p>
	 * Uses {@code zoomToFit(rect)} with a rect the size of the current view (the same mechanism as Zoom In/Out)
	 * rather than {@code DiagramView.bringIntoView}, which scrolled to an empty region in the editor.
	 */
	private void scrollToCenter(DiagramNode n) {
		Rectangle2D.Float visible = diagramView.deviceToDoc(diagramView.getVisibleRect());
		Rectangle2D.Float target = n.getBounds();
		if (visible.width <= 0 || visible.height <= 0 || visible.contains(target)) {
			return;
		}
		Rectangle2D.Float centred = new Rectangle2D.Float(
				(float) (target.getCenterX() - visible.width / 2),
				(float) (target.getCenterY() - visible.height / 2),
				visible.width, visible.height);
		diagramView.zoomToFit(centred);
		overview.setDiagramView(diagramView);
	}

	/** Value labels and transparent text (titles, notes) are not schematic elements, so search skips them. */
	private static boolean isSearchable(DiagramNode n) {
		if (VALUE_TEXT.equals(n.getId())) {
			return false;
		}
		return !(n instanceof ShapeNode s && s.getTransparent());
	}

	/**
	 * Selects the match and scrolls to it. Scrolling alone gave no feedback when the node was already
	 * on screen, so search looked broken.
	 */
	private void showSearchResult(DiagramNode n) {
		Runnable show = () -> {
			diagram.getSelection().clear();
			n.setSelected(true);
			scrollToCenter(n);
			diagramView.repaint();
			selectionProvider.setSelection(List.of(n));
		};
		if (SwingUtilities.isEventDispatchThread()) {
			show.run();
		} else {
			SwingUtilities.invokeLater(show);
		}
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
		// Give the label its own pen: sharing the parent's Pen instance means recolouring the label
		// (e.g. red in Diff mode) also recolours the parent node. getPen() is null when the node uses the
		// diagram default, so fall back to the effective pen.
		Pen parentPen = shapeNode.getPen() != null ? shapeNode.getPen() : shapeNode.getEffectivePen();
		transparentTextNode.setPen(parentPen != null ? parentPen.clone() : new Pen(Color.black));
		transparentTextNode.setBrush(shapeNode.getBrush());
		transparentTextNode.setTextFormat(tf);
		transparentTextNode.attachTo(shapeNode, attachPos);
		return transparentTextNode;
	}

	/**
	 * Sets the colour of a value label. Labels from older saved schematics may have no pen of their own
	 * ({@code getPen()} returns null when the item uses the diagram default), so create one if needed.
	 */
	private static void setValueColor(DiagramNode valueNode, Color color) {
		Pen pen = valueNode.getPen();
		if (pen == null) {
			valueNode.setPen(new Pen(color));
		} else {
			pen.setColor(color);
		}
	}

	public void clearValueBoxes(Rectangle2D.Float visibleRect) {
		if (visibleRect == null) {
			return;
		}
		// Scan the diagram directly: value labels are transparent, and getVisibleNodes() skips transparent
		// nodes, so going through it never found any labels and stale values (e.g. from a study that has
		// since been deselected) were never cleared.
		for (ShapeNode node : diagram.items(ShapeNode.class)) {
			if (VALUE_TEXT.equals(node.getId()) && node.getBounds().intersects(visibleRect)) {
				node.setText("");
			}
		}
	}

	public void save(final String filename) throws IOException, XmlException
	{
		if (filename == null)
			return;
		Cursor previousCursor = diagramView.getCursor();
		diagramView.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			if (filename.endsWith(".xml")) {
				diagram.saveToXml(filename);
			} else if (filename.endsWith(".pdf")) {
				presenter.launchDialog(() -> {
					Window window = presenter.getShell(diagramView);
					JFrame frame = window instanceof JFrame f ? f : null;
					PDFOptionDialog dialog= new PDFOptionDialog(frame, filename, diagram, base.getContext());
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
		Map<String, Object> allNodes = getVisibleNodes(diagram.getBounds());

		base.getContext().setAllSchematicVariableNames(new ArrayList<>());
		for (String v : allNodes.keySet()) {
			base.getContext().getAllSchematicVariableNames().add(v);
		}
	}
}
