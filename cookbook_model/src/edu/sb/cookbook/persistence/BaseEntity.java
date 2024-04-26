package edu.sb.cookbook.persistence;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import edu.sb.tool.Copyright;
import edu.sb.tool.JsonProtectedPropertyStrategy;


/**
 * This abstract class defines entities as the root of an inheritance tree. Having a common root entity class allows for the
 * unique generation of primary keys across all subclasses, and additionally for both polymorphic relationships and polymorphic
 * queries.
 */
@Entity
@Table(schema="cookbook", name="BaseEntity", indexes=@Index(columnList="discriminator"))
@Inheritance(strategy=InheritanceType.JOINED)
@DiscriminatorColumn(name="discriminator")
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
@Copyright(year=2012, holders="Sascha Baumeister")
public abstract class BaseEntity implements Comparable<BaseEntity> {

	@PositiveOrZero
	@Id	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long identity;

	@Positive
	@Version
	@Column(nullable=false, updatable=true)
	private int version;

	@Column(nullable=false, updatable=false, insertable=true)
	private long created;

	@Column(nullable=false, updatable=true)
	private long modified;


	/**
	 * Initializes a new instance.
	 */
	public BaseEntity () {
		this.version = 1;
		this.created = System.currentTimeMillis();
		this.modified = System.currentTimeMillis();
	}


	@JsonbProperty
	public long getIdentity () {
		return this.identity;
	}


	protected void setIdentity (final long identity) {
		this.identity = identity;
	}


	@JsonbProperty
	public int getVersion () {
		return this.version;
	}


	public void setVersion (final int version) {
		this.version = version;
	}


	@JsonbProperty
	public long getCreated () {
		return this.created;
	}


	protected void setCreated (final long created) {
		this.created = created;
	}


	@JsonbProperty
	public long getModified () {
		return this.modified;
	}


	public void setModified (final long modified) {
		this.modified = modified;
	}


	@Override
	public int compareTo (final BaseEntity other) {
		return Long.compare(this.identity, other.identity);
	}


	@Override
	public String toString () {
		return this.getClass().getName() + '#' + this.identity;
	}
}