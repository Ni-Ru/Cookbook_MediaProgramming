package edu.sb.cookbook.persistence;

import javax.persistence.Column;
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
public class Ingredient {
	
	@NotNull
	@ManyToOne
	@JoinColumn(name = "recipeReference", nullable = true, updatable = true)
	private Recipe recipe;
	
	@ManyToOne @JoinColumn
    private IngredientType type;
	
	@NotNull @PositiveOrZero
	@Column(nullable = false, updatable = true)
    private float amount;
	
	@NotNull  @Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = true)
    private Ingredient.Unit unit;

    public Ingredient() {
        this.unit = Ingredient.Unit.GRAM;
    }
    
	static public enum Unit {
		LITRE, GRAM, TEASPOON, TABLESPOON, PINCH, CUP, CAN, TUBE, BUSHEL, PIECE;

		public String getName() {
			return this.name();
		}

		public int getOrdinal() {
			return this.ordinal();
		}
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
