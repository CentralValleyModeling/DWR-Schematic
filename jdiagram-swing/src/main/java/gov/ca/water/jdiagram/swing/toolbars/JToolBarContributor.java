package gov.ca.water.jdiagram.swing.toolbars;

import java.util.Objects;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JToolBar;

/**
 * {@link ToolBarContributor} that adds directly to a Swing {@link JToolBar}.
 */
public record JToolBarContributor(JToolBar toolBar) implements ToolBarContributor
{
	public JToolBarContributor(JToolBar toolBar)
	{
		this.toolBar = Objects.requireNonNull(toolBar, "toolBar");
	}

	@Override
	public void addAction(Action action)
	{
		toolBar.add(action);
	}

	@Override
	public void addComponent(String id, JComponent component)
	{
		if(component.getName() == null)
		{
			component.setName(id);
		}
		// JToolBar uses a BoxLayout, which stretches components up to their maximum size.
		if(!component.isMaximumSizeSet())
		{
			component.setMaximumSize(component.getPreferredSize());
		}
		toolBar.add(component);
	}

	@Override
	public void update()
	{
		toolBar.revalidate();
		toolBar.repaint();
	}
}
