package gov.ca.water.jdiagram.core;

import gov.ca.water.jdiagram.core.interfaces.SchematicCore;
import gov.ca.water.jdiagram.core.interfaces.SchematicDataSource;

public record SchematicContext(SchematicCore core, SchematicDataSource dataSource) {
}
