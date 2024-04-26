package edu.sb.cookbook.persistence;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(schema = "cookbook", name="Recipe")
@PrimaryKeyJoinColumn(name="recipeIdentity")
@DiscriminatorValue("Recipe")
public class Recipe extends BaseEntity{

	@ManyToOne
	private Document avatar;
	
	@ManyToOne
	private Person owner;
	
	@NotNull
	@JoinColumn
	@OneToMany
	private Set<Ingredient> ingredients;
	
	@NotNull
	@ManyToMany
	@JoinColumn
	private Set<Document> illustrations;
	
	@NotNull  @Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = true)
	private Recipe.Category category;
	
	@NotNull
	@Column(nullable = false, updatable = true)
	private String title;
	
	@Column(nullable = false, updatable = true)
	private String description;
	
	@NotNull
	@Column(nullable = false, updatable = true)
	private String instruction;
	
	public Recipe() {
		this.category = Recipe.Category.MAIN_COURSE;
		this.ingredients = new HashSet<Ingredient>();
		this.illustrations = new HashSet<Document>();
	}
	
	static public enum Category {
		MAIN_COURSE,
		APPETIZER,
		SNACK,
		DESSERT,
		BREAKFAST,
		BUFFET,
		BARBEQUE,
		ADOLESCENT,
		INFANT;
		
		public String getName() {
			return this.name();
		}
		
		public int getOrdinal() {
			return this.ordinal();
		}
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

	public Set<Ingredient> getIngredients() {
        return ingredients;
    }

    protected void setIngredients(Set<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

	public Set<Document> getIllustrations() {
		return illustrations;
	}

	protected void setIllustrations(Set<Document> illustrations) {
		this.illustrations = illustrations;
	}

	public Recipe.Category getCategory() {
		return category;
	}

	public void setCategory(Recipe.Category category) {
		this.category = category;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}
	
	public Restriction getRestriction() {
		
		if (ingredients.isEmpty()) {
            return Restriction.NONE;
        }
        
        Restriction minRestriction = Restriction.VEGAN;
        for (Ingredient ingredient : ingredients) {
            Restriction ingredientRestriction = ingredient.getType().getRestriction();
            if (ingredientRestriction.getOrdinal() < minRestriction.getOrdinal()) {
                minRestriction = ingredientRestriction;
            }
        }
        return minRestriction;
		
	}
	
}
