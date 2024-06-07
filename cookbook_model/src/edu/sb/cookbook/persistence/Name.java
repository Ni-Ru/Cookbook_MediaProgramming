package edu.sb.cookbook.persistence;

import java.util.Comparator;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import edu.sb.tool.JsonProtectedPropertyStrategy;

@JsonbVisibility(JsonProtectedPropertyStrategy.class)
@Embeddable
public class Name implements Comparable<Name> {

	static public final Comparator<Name> COMPARATOR = 
		Comparator
            .comparing(Name::getTitle, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Name::getFamily)
            .thenComparing(Name::getGiven);
	
	@Size(max=15)
	@Column(nullable = true, updatable = true, length = 15)
	private String title;

	@NotNull @Size(max=31)
	@Column(nullable = false, updatable = true, length = 31, name = "surname")
	private String family;

	@NotNull @Size(max=31)
	@Column(nullable = false, updatable = true, length = 31, name = "forename")
	private String given;

	@JsonbProperty
	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@JsonbProperty
	public String getFamily() {
		return this.family;
	}

	public void setFamily(String family) {
		this.family = family;
	}

	@JsonbProperty
	public String getGiven() {
		return this.given;
	}

	public void setGiven(String given) {
		this.given = given;
	}

	@Override
    public int compareTo(Name other) {
        return COMPARATOR.compare(this, other);
    }

}
