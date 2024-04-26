module edu.sb.cookbook.model {
	requires transitive java.logging;
	requires transitive javax.annotation.api;
	requires transitive java.validation;
	requires transitive java.json.bind;
	requires transitive java.xml.bind;

	requires transitive javax.persistence;
	requires transitive eclipselink.minus.jpa;
	requires transitive java.ws.rs;

	exports edu.sb.cookbook.persistence;
	exports edu.sb.cookbook.service;
	exports edu.sb.tool;

	opens edu.sb.cookbook.persistence;
	opens edu.sb.cookbook.service;
}