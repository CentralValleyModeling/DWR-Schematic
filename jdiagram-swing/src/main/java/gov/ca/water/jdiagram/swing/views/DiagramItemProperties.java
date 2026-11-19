package gov.ca.water.jdiagram.swing.views;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.mindfusion.diagramming.DiagramItem;
import com.mindfusion.diagramming.DiagramLink;
import com.mindfusion.diagramming.DiagramLinkList;
import com.mindfusion.diagramming.DiagramNode;
import com.mindfusion.diagramming.InplaceEditable;
import com.mindfusion.diagramming.ShapeNode;

/**
 * Read-only property view of a {@link DiagramItem}.
 * <p>
 * Plain-Java replacement for the Eclipse {@code IPropertySource} implementation
 * ({@code DiagramItemPropertySource}) previously nested in {@code SchematicPanel}.
 */
public record DiagramItemProperties(DiagramItem item)
{
	public static final String LABEL = "label";
	public static final String INCOMING = "incoming";
	public static final String OUTGOING = "outgoing";

	public DiagramItemProperties(DiagramItem item)
	{
		this.item = Objects.requireNonNull(item, "item");
	}

	/**
	 * @return property id to display value, in display order
	 */
	public Map<String, String> asMap()
	{
		Map<String, String> map = new LinkedHashMap<>();
		map.put(LABEL, getLabel());
		map.put(INCOMING, getIncoming());
		map.put(OUTGOING, getOutgoing());
		return Collections.unmodifiableMap(map);
	}

	public String getLabel()
	{
		if (item instanceof ShapeNode shapeNode) {
			return nullToEmpty(shapeNode.getPlainText());
		} else if (item instanceof DiagramLink link) {
			return nullToEmpty(link.getText());
		}
		return "";
	}

	public String getIncoming() {
		return item instanceof DiagramNode node
				? joinNodeNames(node.getIncomingLinks(), DiagramLink::getOrigin)
				: "";
	}

	public String getOutgoing() {
		return item instanceof DiagramNode node
				? joinNodeNames(node.getOutgoingLinks(), DiagramLink::getDestination)
				: "";
	}

	private static String joinNodeNames(DiagramLinkList links, Function<DiagramLink, DiagramNode> end) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < links.size(); i++) {
			if (i > 0) {
				buf.append(", ");
			}
			DiagramNode node = end.apply(links.get(i));
			if (node instanceof InplaceEditable editable) {
				buf.append(nullToEmpty(editable.getTextToEdit()));
			}
		}
		return buf.toString();
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}
}
