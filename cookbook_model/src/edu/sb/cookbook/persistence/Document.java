package edu.sb.cookbook.persistence;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.eclipse.persistence.annotations.CacheIndex;

import edu.sb.tool.Copyright;
import edu.sb.tool.HashCodes;
import edu.sb.tool.JsonProtectedPropertyStrategy;



/**
 * Instances of this class model document entities.
 */
@Entity
@Table(schema="cookbook", name="Document", indexes={})
@PrimaryKeyJoinColumn(name="documentIdentity")
@DiscriminatorValue("Document")
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
@Copyright(year=2022, holders="Sascha Baumeister")
public class Document extends BaseEntity {
	static private final byte[] EMPTY_BYTES = {};

	@NotNull @Size(min=64, max=64)
	@Column(nullable=false, updatable=false, insertable=true, unique=true, length=64)
	@CacheIndex(updateable=false)
	private String hash;

	@NotEmpty @Size(max=63) @Pattern(regexp="[a-zA-Z_0-9\\.\\-\\+]+/[a-zA-Z_0-9\\.\\-\\+]+")
	@Column(nullable=false, updatable=true, length=63)
	private String type;

	@Size(max=127)
	private String description;

	@NotNull
	@Column(nullable=false, updatable=false, insertable=true, length=Integer.MAX_VALUE)
	private byte[] content;


	/**
	 * Initializes a new instance.
	 */
	public Document () {
		this(EMPTY_BYTES);
	}


	/**
	 * Initializes a new instance.
	 * @param content the content, or {@code null} for none
	 */
	public Document (byte[] content) {
		super();
		if (content == null) content = EMPTY_BYTES;

		this.hash = HashCodes.sha2HashText(256, content);
		this.type = "application/octet-stream";
		this.description = null;
		this.content = content;
	}


	/**
	 * Returns the hash.
	 * @return the hash
	 */
	@JsonbProperty
	public String getHash () {
		return this.hash;
	}


	/**
	 * Sets the hash.
	 * @param hash the hash
	 */
	protected void setHash (final String hash) {
		this.hash = hash;
	}


	/**
	 * Returns the type.
	 * @return the type
	 */
	@JsonbProperty
	public String getType () {
		return this.type;
	}


	/**
	 * Sets the type.
	 * @param type the type
	 */
	public void setType (final String type) {
		this.type = type;
	}

	
	

	/**
	 * Returns the description.
	 * @return the description, or {@code null} for none
	 */
	@JsonbProperty
	public String getDescription () {
		return this.description;
	}


	/**
	 * Sets the description.
	 * @param description the description, or {@code null} for none
	 */
	public void setDescription (final String description) {
		this.description = description;
	}


	/**
	 * Returns the size; note that this operation is present
	 * solely for marshaling purposes.
	 * @return the content's length
	 */
	@JsonbProperty
	protected int getSize () {
		return this.content.length;
	}


	/**
	 * Returns the content.
	 * @return the content
	 */
	@JsonbTransient
	public byte[] getContent () {
		return this.content;
	}


	/**
	 * Sets the content.
	 * @param content the content
	 */
	protected void setContent (byte[] content) {
		this.content = content;
	}
}