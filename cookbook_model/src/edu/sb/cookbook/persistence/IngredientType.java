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

@Entity
@Table(schema = "cookbook", name="IngredientType")
@PrimaryKeyJoinColumn(name="ingredientTypeIdentity")
@DiscriminatorValue("IngredientType")
public class IngredientType {
	
	@NotNull
	@ManyToOne
	private Document avatar;
	
	@ManyToOne
	@JoinColumn(name = "ingredientTypes", nullable = true, updatable = true)
	private Person owner;
	
	@NotNull
	@Column(nullable = true, updatable = true)
	private String alias;
	
	@NotNull  @Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = true)
	private Restriction restriction;
	
	@Column(nullable = true, updatable = true)
	private String description;
	
	public IngredientType() {
		this.restriction = Restriction.NONE;
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
