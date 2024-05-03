package edu.sb.cookbook.persistence;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.persistence.annotations.CacheIndex;

@Entity
@Table(schema = "cookbook", name="IngredientType", indexes={})
@PrimaryKeyJoinColumn(name="ingredientTypeIdentity")
@DiscriminatorValue("IngredientType")
public class IngredientType extends BaseEntity {
	
	@ManyToOne(optional = false)
    @JoinColumn(nullable = false, updatable = true, name = "avatarReference")
    private Document avatar;
	
	@ManyToOne(optional = false)
	@JoinColumn(nullable = true, updatable = true, name = "ownerReference")
	private Person owner;
	
	@NotNull @Size(max=128)
	@Column(nullable = true, updatable = true, length = 128, unique = true)
	@CacheIndex(updateable = true)
	private String alias;
	
	@NotNull @Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = true)
	private Restriction restriction;
	
	@Size(max=4094)
	@Column(nullable = true, updatable = true, length = 4094)
	private String description;
	
	public IngredientType() {
		this.restriction = Restriction.VEGAN;
	}
	
	
	public Document getAvatar() {
		return avatar;
	}
	
	public void setAvatar(Document avatar) {
		this.avatar = avatar;
	}
	
	public Person getOwner() {
		return owner;
	}
	
	public void setOwner(Person owner) {
		this.owner = owner;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public Restriction getRestriction() {
		return restriction;
	}
	public void setRestriction(Restriction restriction) {
		this.restriction = restriction;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
}
