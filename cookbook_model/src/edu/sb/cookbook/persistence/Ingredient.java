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
import javax.validation.constraints.PositiveOrZero;

@Entity
@Table(schema = "cookbook", name="Ingredient")
@PrimaryKeyJoinColumn(name="ingredientIdentity")
@DiscriminatorValue("Ingredient")
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

	public Recipe getRecipe() {
		return recipe;
	}

	protected void setRecipe(Recipe recipe) {
		this.recipe = recipe;
	}

	public IngredientType getType() {
		return type;
	}

	public void setType(IngredientType type) {
		this.type = type;
	}

	public float getAmount() {
		return amount;
	}

	public void setAmount(float amount) {
		this.amount = amount;
	}

	public Ingredient.Unit getUnit() {
		return unit;
	}

	public void setUnit(Ingredient.Unit unit) {
		this.unit = unit;
	}
}
