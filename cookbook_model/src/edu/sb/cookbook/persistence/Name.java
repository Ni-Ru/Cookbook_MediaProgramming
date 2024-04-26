package edu.sb.cookbook.persistence;

import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

@Embeddable
public class Name implements Comparable<Name> {

	@Column(nullable = true, updatable = true)
	private String title;

	@Size(max=30,min=2)
	@Column(nullable = true, updatable = true)
	private String family;

	@Size(max=30,min=2)
	@Column(nullable = true, updatable = true)
	private String given;

	public String getTitle() {
		return this.title;
	}

	protected void setTitle(String title) {
		this.title = title;
	}

	public String getFamily() {
		return this.family;
	}

	protected void setFamily(String family) {
		this.family = family;
	}

	public String getGiven() {
		return this.given;
	}

	protected void setGiven(String given) {
		this.given = given;
	}

	@Override
    public int compareTo(Name other) {
        return Comparator
                .comparing(Name::getTitle, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Name::getFamily)
                .thenComparing(Name::getGiven)
                .compare(this, other);
    }

}
