package gov.ca.water.jdiagram.core.utils;

import java.util.ArrayList;
import java.util.List;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class AttributeMapperTest {
	@Test
	void testLoadAttributesFromFile() {
		AttributeMapper mapper = new AttributeMapper();
		List<String> values = buildTestList();
		for (String value : values) {
			assertEquals(value, mapper.getAttribute(value).name);
		}
	}

	private List<String> buildTestList() {
		List<String> values = new ArrayList<>();
		values.add("c.calsim2");
		values.add("c.gaging.natural");
		values.add("c.gaging.manmade");
		values.add("c.discontinue.natural");
		values.add("c.discontinue.manmade");
		values.add("c.manmade.arc");
		values.add("c.manmade.node");
		values.add("c.natural.arc");
		values.add("c.natural.node");
		values.add("c.return.node");
		values.add("canallosses");
		values.add("du.ag.gw");
		values.add("du.ag.sgw");
		values.add("du.ag.sw");
		values.add("du.external");
		values.add("du.refuge.sgw");
		values.add("du.refuge.sw");
		values.add("du.urban.gw");
		values.add("du.urban.sgw");
		values.add("du.urban.sw");
		values.add("evaporation");
		values.add("flow.closureterm.arc");
		values.add("flow.closureterm.node");
		values.add("flow.deltaaccretion.arc");
		values.add("flow.deltaaccretion.node");
		values.add("flow.deltadepletion.arc");
		values.add("flow.deltadepletion.node");
		values.add("flow.diversion");
		values.add("flow.gwinflow");
		values.add("flow.inflow.arc");
		values.add("flow.inflow.node");
		values.add("flow.return");
		values.add("flow.spill");
		values.add("flow.surface.runoff");
		values.add("infotext.gageid");
		values.add("infotext.other");
		values.add("storage.arc");
		values.add("storage.node");
		values.add("ww.flow");
		values.add("ww.plant");
		values.add("infotext.legend");
		return values;
	}
}
