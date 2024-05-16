package edu.sb.cookbook.persistence;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
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
import javax.validation.constraints.PositiveOrZero;

import edu.sb.tool.JsonProtectedPropertyStrategy;

@Entity
@Table(schema = "cookbook", name="Ingredient")
@PrimaryKeyJoinColumn(name="ingredientIdentity")
@DiscriminatorValue("Ingredient")
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
public class Ingredient extends BaseEntity{
	
	static public enum Unit {
		LITRE, GRAM, TEASPOON, TABLESPOON, PINCH, CUP, CAN, TUBE, BUSHEL, PIECE
	}
	
	@ManyToOne(optional = false)
	@JoinColumn(nullable = false, updatable = false, insertable = true, name = "recipeReference")
	private Recipe recipe;
	
	@ManyToOne(optional = false)
	@JoinColumn(nullable = false, updatable = true, name = "typeReference")
    private IngredientType type;
	
	@PositiveOrZero
	@Column(nullable = false, updatable = true)
    private float amount;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = true)
    private Unit unit;

	protected Ingredient() {
		this(null);
	}
	
	// Braucht zwei Konstruktoren
    public Ingredient(Recipe recipe) {
    	this.recipe = recipe;
        this.unit = Unit.GRAM;
    }

	@JsonbProperty
	@JsonbTransient
	public Recipe getRecipe() {
		return recipe;
	}

	protected void setRecipe(Recipe recipe) {
		this.recipe = recipe;
	}

	@JsonbProperty
	@JsonbTransient
	public IngredientType getType() {
		return type;
	}

	public void setType(IngredientType type) {
		this.type = type;
	}

	@JsonbProperty
	@JsonbTransient
	public float getAmount() {
		return amount;
	}

	public void setAmount(float amount) {
		this.amount = amount;
	}

	@JsonbProperty
	@JsonbTransient
	public Ingredient.Unit getUnit() {
		return unit;
	}

	public void setUnit(Ingredient.Unit unit) {
		this.unit = unit;
	}
	
	@JsonbProperty
	@JsonbTransient
	protected long getRecipeReference() {
		return this.recipe == null ? null : this.recipe.getIdentity();
	}
}
