package gov.ca.water.jdiagram.swing.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import com.mindfusion.diagramming.DiagramItem;

/**
 * Provides selection for the schematic view. Left non-final for behavior extension.
 */
public class DiagramSelectionProvider {
	/** Listener notified when the diagram selection changes (replaces ISelectionChangedListener). */
	@FunctionalInterface
	public interface SelectionChangedListener extends EventListener {
		void selectionChanged(SelectionChangedEvent event);
	}

	/** Event describing a selection change (replaces JFace SelectionChangedEvent). */
	public static final class SelectionChangedEvent extends EventObject {
		private final List<DiagramItem> selection;

		SelectionChangedEvent(DiagramSelectionProvider source, List<DiagramItem> selection) {
			super(source);
			this.selection = selection;
		}

		@Override
		public DiagramSelectionProvider getSource() {
			return (DiagramSelectionProvider) super.getSource();
		}

		/** @return the selected items; never null, may be empty */
		public List<DiagramItem> getSelection() {
			return selection;
		}

		/** @return the first selected item, or null if the selection is empty */
		public DiagramItem getFirstElement() {
			return selection.isEmpty() ? null : selection.get(0);
		}

		public boolean isEmpty() {
			return selection.isEmpty();
		}
	}

	private final EventListenerList listeners = new EventListenerList();
	private volatile List<DiagramItem> theSelection = Collections.synchronizedList(Collections.emptyList());

	public void addSelectionChangedListener(SelectionChangedListener listener) {
		listeners.add(SelectionChangedListener.class, listener);
	}

	public void removeSelectionChangedListener(SelectionChangedListener listener) {
		listeners.remove(SelectionChangedListener.class, listener);
	}

	/** @return the current selection; never null, may be empty (replaces StructuredSelection.EMPTY) */
	public List<DiagramItem> getSelection() {
		return theSelection;
	}

	/**
	 * Sets the selection and notifies listeners asynchronously on the EDT.
	 * Listeners are notified even if the selection is unchanged, matching legacy behavior.
	 */
	public void setSelection(List<? extends DiagramItem> selection) {
		final List<DiagramItem> newSelection = selection == null || selection.isEmpty()
				? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<DiagramItem>(selection));
		theSelection = newSelection;

		final SelectionChangedEvent e = new SelectionChangedEvent(this, newSelection);
		// Snapshot the listeners now, as the legacy code did with listeners.toArray()
		final SelectionChangedListener[] snapshot =
				listeners.getListeners(SelectionChangedListener.class);

		for (final SelectionChangedListener listener : snapshot) {
			SwingUtilities.invokeLater(() -> listener.selectionChanged(e));
		}
	}
}
