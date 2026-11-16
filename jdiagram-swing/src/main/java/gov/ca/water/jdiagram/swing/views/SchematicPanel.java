package gov.ca.water.jdiagram.swing.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Panel;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.prefs.Preferences;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.Action;
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
import com.mindfusion.diagramming.DiagramLink;
import com.mindfusion.diagramming.DiagramLinkList;
import com.mindfusion.diagramming.DiagramNode;
import com.mindfusion.diagramming.DiagramNodeList;
import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.Group;
import com.mindfusion.diagramming.InplaceEditable;
import com.mindfusion.diagramming.LinkEvent;
import com.mindfusion.diagramming.NodeEvent;
import com.mindfusion.diagramming.ShapeNode;
import com.mindfusion.drawing.Align;
import com.mindfusion.drawing.TextFormat;
import gov.ca.water.jdiagram.core.SchematicHost;
import gov.ca.water.jdiagram.core.SchematicLicense;
import gov.ca.water.jdiagram.core.SchematicPanelBase;
import gov.ca.water.jdiagram.swing.toolbars.DateCombo;
import gov.ca.water.jdiagram.swing.toolbars.SearchText;

import hec.hecmath.HecMathException;
import hec.io.DataContainer;

public class SchematicPanel extends JComponent
{
	private static final FluentLogger logger = FluentLogger.forEnclosingClass();

	private DiagramView diagramView;

	private Diagram diagram;

	private JPanel swingContainer;

	protected DiagramAdapter listener;

	private Container contentPane;

	private SearchText searchText;

	private int index_lastnode = 0;

	private DiagramSelectionProvider selectionProvider;

	protected DateCombo dateCombo;

	private Action zoomInAction;

	private Action zoomOutAction;

	private Action zoomRectAction;

	private Action zoomMagnifier;

	private Rectangle2D.Float lastVisibleRect;

	private Action openSchematicAction;

	private Action saveAsSchematicAction;

	private float _zoomFactor = 100;

	private Action zoomNormalAction;

	private Action fontAction;

	private Action decimalAction;

	private Action tafTypeAction;

	private Action forwardAction;

	private Action backwardAction;

	private SchematicPanelBase base;

	private static final Object VALUE_TEXT = "__VT__";

	private static String DECIMAL_PLACES = "decimalPlaces";

	private static String ISMONTHLYTAF = "ismonthlytaf";

	public SchematicPanel(SchematicPanelBase base) {
		this.base = base;
	}

	public SchematicPanelBase getBase() {
		return base;
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(JPanel parent) {
		System.setProperty("sun.awt.noerasebackground", "true");
		swingContainer = new JPanel();
		Panel panel = new Panel(new BorderLayout()) {
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
		//JLayeredPane layeredPane = new JLayeredPane();
		//contentPane.add(layeredPane);
		diagramView = new DiagramView(diagram = new Diagram());
		SchematicLicense.apply();
		diagramView.setAllowInplaceEdit(false);
		diagramView.setBehavior(Behavior.Pan);
		selectionProvider = new DiagramSelectionProvider();
		diagram.addDiagramListener(listener=new DiagramAdapter() {
			private Timer refreshTimer = new Timer();
			private TimerTask refreshTask = new TimerTask(){
				@Override
				public void run() {
				}};

			private int delay = 250;


			@Override
			public void linkClicked(LinkEvent e) {
				selectionProvider.setSelection(new DiagramItemSelection(e.getLink()).toList());
			}

			@Override
			public void linkDoubleClicked(LinkEvent e) {
				showDSS(e.getLink().getTextToEdit());
			}

			@Override
			public void nodeClicked(NodeEvent e) {
				selectionProvider.setSelection(new DiagramItemSelection(e.getNode()).toList());
			}

			@Override
			public void nodeDoubleClicked(NodeEvent e) {
				DiagramNode n = e.getNode();
				showDSS(((InplaceEditable)n).getTextToEdit());
			}

			public void openDSSPerspective(final String perspectiveId, final String name) {
				getBase().getHost().showDSSPerspective();
			}

			public void showDSS(String name){
				if(name == null) {
					return;
				}
				name = name.toLowerCase();
				final Vector<DataContainer> data=new Vector<>();
				for (int i=0; i<4; i++){
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
				base.getHost().showDssView();
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
		});
		contentPane.add(new JScrollPane(diagramView));

		IWorkbenchPage activePage = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage();
		if (activePage != null) {
			IViewPart sview = activePage.findView(SchematicOverview.ID);
			if (sview != null) {
				overview = (SchematicOverview) sview;
				overview.setDiagramView(diagramView);
			}
		}
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(swingContainer, "gov.ca.dwr.calsim.jdiagram.viewer");
		makeActions();
		contributeToActionBars();
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalToolBar(JToolBar manager) {
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
		dateCombo = new DateCombo(this);
		manager.add(dateCombo.getDateList());
		manager.add(backwardAction);
		manager.add(forwardAction);
		searchText = new SearchText(this);
		manager.add(searchText);
	}


	@SuppressWarnings("deprecation")
	private void makeActions() {
		openSchematicAction = new Action("Open", PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OPEN_MARKER)) {
			public void run() {
				FileDialog dialog = new FileDialog(swingContainer.getShell(),
						SWT.OPEN);
				dialog.setFilterExtensions(new String[] { "*.xml", "*.cht" });
				final String file = dialog.open();
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
						getBase().collectAllSchematicVariables();
						getBase().loadAllSchematicVariableData();
					} catch (Exception ex) {
						logger.atFine().withCause(ex).log();
						getBase().getHost().reportError(ex);
					}
				});
			}
		};

		saveAsSchematicAction = new Action("Save As", PlatformUI.getWorkbench()
				.getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT)) {
			public void run() {
				FileDialog dialog = new FileDialog(swingContainer.getShell(),
						SWT.SAVE);
				dialog.setFilterExtensions(new String[] { "*.xml", "*.pdf", "*.svg","*.*"});
				String file = dialog.open();
				if (file == null) {
					return;
				}
				try {
					save(file);
				} catch (Exception e) {
					WPPException.handleException(e);
				}
			}
		};

		zoomNormalAction = new Action("Zoom 100",
				Activator.getImageDescriptor("ZoomNormal24.gif")) {

			public void run() {
				_zoomFactor = 100;
				diagramView.zoomToFit();
				overview.setDiagramView(diagramView);
			};
		};
		zoomInAction = new Action("Zoom In",
				Activator.getImageDescriptor("ZoomIn24.gif")) {

			public void run() {
				//_zoomFactor *= 2;
				//diagramView.setZoomFactor(_zoomFactor);
				Rectangle2D.Float rect = diagramView.deviceToDoc(diagramView.getVisibleRect());
				rect.setRect(rect.x+rect.width/4, rect.y+rect.height/4, rect.width/2, rect.height/2);
				diagramView.zoomToFit(rect);
				overview.setDiagramView(diagramView);
			};
		};
		zoomOutAction = new Action("Zoom Out",
				Activator.getImageDescriptor("ZoomOut24.gif")) {

			public void run() {
				//_zoomFactor /= 2;
				Rectangle2D.Float rect = diagramView.deviceToDoc(diagramView.getVisibleRect());
				rect.setRect(rect.x-rect.width/2, rect.y-rect.height/2, rect.width*2, rect.height*2);
				diagramView.zoomToFit(rect);
				overview.setDiagramView(diagramView);
			};
		};

		zoomRectAction = new Action("Zoom to Rectangular", Activator.getImageDescriptor("ZoomToFit24.gif")) {

			private Behavior behavior=Behavior.SelectOnly;

			public void run() {
				SchematicPluginCore.zoomToRect=!SchematicPluginCore.zoomToRect;
				if (SchematicPluginCore.zoomToRect) {
					behavior = diagramView.getBehavior();
					diagramView.setCustomBehavior(new RectangleZoomBehavior(
							diagramView, overview));
				} else {
					diagramView.setBehavior(Behavior.SelectOnly);
					diagramView.setBehavior(behavior);
				}
			}
		};

		zoomMagnifier = new Action("Magnifier", Activator.getImageDescriptor("zoom_magnifier.png")) {
			private MagnifierPanel magnifier;

			public void run() {
				if (magnifier == null) {
					magnifier = new MagnifierPanel(diagramView);
				}
				SchematicPluginCore.showMagnifier=!SchematicPluginCore.showMagnifier;
				magnifier.setShowing(SchematicPluginCore.showMagnifier);
			}
		};

		fontAction = new Action("Font", Activator.getImageDescriptor("font.png")) {

			public void run() {
				final Font font = getDiagram().getFont();
				final IWorkbench workbench = PlatformUI.getWorkbench();
				workbench.getDisplay().asyncExec(new Runnable() {
					public void run() {
						Shell shell = workbench.getActiveWorkbenchWindow().getShell();
						Display display = shell.getDisplay();
						Font oldFont = getDiagram().getFont();
						FontData oldFontData = FontUtil.toSwtFontData(display, oldFont, true);
						FontDialog fontDialog = new FontDialog(shell);
						fontDialog.setFontData(oldFontData);
						FontData fontData = fontDialog.open();
						if (fontData != null) {
							Font newFont = FontUtil.toAwtFont(display, fontData, true);
							//getDiagramView().suspendRepaint();
							getDiagram().setFont(newFont);
							Diagram diagram = getDiagram();
							for (DiagramItem item : diagram.items(DiagramItem.class)) {
								item.setFont(newFont);
								if (item instanceof ShapeNode) {
									ShapeNode snode = (ShapeNode) item;
								}
							}
						}
					}
				});
			};
		};

		decimalAction = new Action("Decimal", Activator.getImageDescriptor("decimal.png")){

			public void run(){
				final IWorkbench workbench=PlatformUI.getWorkbench();
				workbench.getDisplay().asyncExec(new Runnable(){
					public void run(){
						Shell shell=workbench.getActiveWorkbenchWindow().getShell();
						Display display=shell.getDisplay();
						DecimalDialog decimalDialog = new DecimalDialog(shell, getSchematic());
						decimalDialog.openDialog();
					}
				});
			};
		};

		if(getBase().getHost().getPreferences() == null)
		{
			getBase().getHost().setPreferences(Preferences.userNodeForPackage(getClass()));
		}
		getBase().getDataSource().isTAFMonthly() = getBase().getHost().getPreferences().getBoolean(ISMONTHLYTAF, true);
		tafTypeAction = new Action("TAF Type"){

			public void run(){
				final IWorkbench workbench=PlatformUI.getWorkbench();
				workbench.getDisplay().asyncExec(new Runnable(){
					public void run(){
						SchematicPluginCore.isTAFMonthly=!SchematicPluginCore.isTAFMonthly;
						if (SchematicPluginCore.isTAFMonthly){
							getBase().getHost().getPreferences().putBoolean(ISMONTHLYTAF, true);
							tafTypeAction.setImageDescriptor(Activator.getImageDescriptor("M.png"));
						}else{
							getBase().getHost().getPreferences().putBoolean(ISMONTHLYTAF, false);
							tafTypeAction.setImageDescriptor(Activator.getImageDescriptor("Y.png"));
						}
					}
				});
			};
		};
		if (SchematicPluginCore.isTAFMonthly){
			tafTypeAction.setImageDescriptor(Activator.getImageDescriptor("M.png"));
		}else{
			tafTypeAction.setImageDescriptor(Activator.getImageDescriptor("Y.png"));
		}

		forwardAction = new Action("Forward", Activator.getImageDescriptor("forward.png")){

			public void run(){
				int twSize = base.getHost().getSchematicTwSelections().size();
				JComboBox<String> dateList = dateCombo.getDateList();
				int size=dateList.getItemCount();
				if (getBase().getHost().getSelectedIndex() < twSize){
					if (twSize<size){
						base.getHost().updateSelectedIndex(twSize);
						dateList.setSelectedIndex(base.getHost().getSelectedIndex());
					}
				}else{
					if (base.getHost().getSelectedIndex()+1<size){
						base.getHost().updateSelectedIndex(base.getHost().getSelectedIndex() + 1);
						dateList.setSelectedIndex(base.getHost().getSelectedIndex());
					}
				}
			}

		};

		backwardAction = new Action("Backward", Activator.getImageDescriptor("backward.png")){

			public void run(){
				int twSize = getBase().getHost().getSchematicTwSelections().size();
				JComboBox<String> dateList = dateCombo.getDateList();
				int size=dateList.getItemCount();
				if (base.getHost().getSelectedIndex()<=twSize){
					if (twSize<size){
						base.getHost().updateSelectedIndex(twSize);
						dateList.setSelectedIndex(base.getHost().getSelectedIndex());
					}
				}else{
					if (base.getHost().getSelectedIndex() >= size){
						base.getHost().updateSelectedIndex(size-1);
						dateList.setSelectedIndex(base.getHost().getSelectedIndex());
					}else{
						base.getHost().updateSelectedIndex(base.getHost().getSelectedIndex()-1);
						dateList.setSelectedIndex(base.getHost().getSelectedIndex());
					}
				}
			}

		};
	}

	public void setupForEditor(){
		if (this instanceof SchematicEditor){
			((SchematicEditor)this).load();
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
	 * Selection wrapper for DiagramItem
	 *
	 * @author psandhu
	 *
	 */
	public static final class DiagramItemSelection implements
			IStructuredSelection, IAdaptable {

		private DiagramItem item;

		public DiagramItemSelection(DiagramItem item) {
			this.item = item;
		}

		public DiagramItem getItem() {
			return item;
		}

		public void setItem(DiagramItem item) {
			this.item = item;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Object getAdapter(Class adapter) {
			if (adapter == IPropertySource.class) {
				return new DiagramItemPropertySource(item);
			}
			return null;
		}

		@Override
		public Object getFirstElement() {
			return new DiagramItemPropertySource(item);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Iterator iterator() {
			return Collections.singleton(getFirstElement()).iterator();
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public Object[] toArray() {
			return new Object[] { getFirstElement() };
		}

		@SuppressWarnings("rawtypes")
		@Override
		public List toList() {
			return Collections.singletonList(getFirstElement());
		}

	}

	public static final class DiagramItemPropertySource implements
			IPropertySource {
		public static final String LABEL = "label";
		public static final String INCOMING = "incoming";
		public static final String OUTGOING = "outgoing";
		private DiagramItem item;

		public DiagramItemPropertySource(DiagramItem item) {
			this.item = item;
		}

		@Override
		public Object getEditableValue() {
			return null;
		}

		@Override
		public IPropertyDescriptor[] getPropertyDescriptors() {
			PropertyDescriptor labelDescriptor = new PropertyDescriptor(LABEL,
					"label");
			PropertyDescriptor incomingDescriptor = new PropertyDescriptor(
					INCOMING, "incoming");
			PropertyDescriptor outgoingDescriptor = new PropertyDescriptor(
					OUTGOING, "outgoing");
			return new IPropertyDescriptor[] { labelDescriptor,
					incomingDescriptor, outgoingDescriptor };
		}

		@Override
		public Object getPropertyValue(Object id) {
			if (id.equals(LABEL)) {
				if (item instanceof ShapeNode) {
					return ((ShapeNode) item).getPlainText();
				} else if (item instanceof DiagramLink) {
					return ((DiagramLink) item).getText();
				}
				return "";
			} else if (id.equals(INCOMING) || id.equals(OUTGOING)){
				if (item instanceof DiagramNode){
					DiagramNode node = (DiagramNode) item;
					if (id.equals(INCOMING)){
						return getIncomingNodes(node);
					} else {
						return getOutgoingNodes(node);
					}
				}
			}
			return "";
		}

		private Object getIncomingNodes(DiagramNode node) {
			DiagramLinkList incomingLinks = node.getIncomingLinks();
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < incomingLinks.size(); i++) {
				DiagramLink diagramLink = incomingLinks.get(i);
				if (i>0){
					buf.append(", ");
				}
				DiagramNode orig = diagramLink.getOrigin();
				buf.append(((InplaceEditable)orig).getTextToEdit());
			}
			return buf.toString();
		}

		private Object getOutgoingNodes(DiagramNode node) {
			DiagramLinkList outgoingLinks = node.getOutgoingLinks();
			StringBuffer buf = new StringBuffer();
			for (int i = 0; i < outgoingLinks.size(); i++) {
				DiagramLink diagramLink = outgoingLinks.get(i);
				if (i>0){
					buf.append(", ");
				}
				DiagramNode dest = diagramLink.getDestination();
				buf.append(((InplaceEditable)dest).getTextToEdit());
			}
			return buf.toString();
		}

		@Override
		public boolean isPropertySet(Object id) {
			return id.equals(LABEL);
		}

		@Override
		public void resetPropertyValue(Object id) {
		}

		@Override
		public void setPropertyValue(Object id, Object value) {
		}
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
}
