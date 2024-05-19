package edu.sb.cookbook.persistence;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
import edu.sb.tool.JsonProtectedPropertyStrategy;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.persistence.annotations.CacheIndex;

@Entity
@Table(schema = "cookbook", name = "Recipe")
@PrimaryKeyJoinColumn(name = "recipeIdentity")
@DiscriminatorValue("Recipe")
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
public class Recipe extends BaseEntity {
	
	static public enum Category {
		MAIN_COURSE, APPETIZER, SNACK, DESSERT, BREAKFAST, BUFFET, BARBEQUE, ADOLESCENT, INFANT
	}

	@ManyToOne(optional = false)
    @JoinColumn(nullable = false, updatable = true, name = "avatarReference")
    private Document avatar;

	@ManyToOne(optional = false)
	@JoinColumn(nullable = true, updatable = true, name = "ownerReference")
	private Person owner;

	@NotNull
	@OneToMany(mappedBy = "recipe", cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE})
	private Set<Ingredient> ingredients;

	@NotNull
	@ManyToMany
	@JoinTable(
			schema = "cookbook",
			name = "RecipeIllustrationAssociation",
			joinColumns = @JoinColumn(nullable = false, updatable = false, insertable = true, name = "recipeReference"),
			inverseJoinColumns = @JoinColumn(nullable = false, updatable = false, insertable = true, name = "documentReference"),
			uniqueConstraints = @UniqueConstraint(columnNames = { "recipeReference", "documentReference" })
	)
	private Set<Document> illustrations;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = true)
	private Category category;

	@NotNull @Size(max=128)
	@Column(nullable = false, updatable = true, length = 128, unique = true)
	@CacheIndex(updateable = true)
	private String title;

	@Size(max=4094)
	@Column(nullable = true, updatable = true, length = 4094)
	private String description;

	@Size(max=4094)
	@Column(nullable = true, updatable = true, length = 4094)
	private String instruction;

	public Recipe() {
		this.category = Category.MAIN_COURSE;
		this.ingredients = Collections.emptySet();
		this.illustrations = new HashSet<>();
	}

	@JsonbProperty
	public Document getAvatar() {
		return avatar;
	}

	public void setAvatar(Document avatar) {
		this.avatar = avatar;
	}
	
	@JsonbProperty
	protected long getOwnerReference(){
		return this.owner == null ? null : this.owner.getIdentity();
	}

	@JsonbTransient
	public Person getOwner() {
		return owner;
	}

	public void setOwner(Person owner) {
		this.owner = owner;
	}
	
	@JsonbProperty
	protected int getIngredientCount() {
		return this.ingredients == null ? null : this.ingredients.size();
	}

	@JsonbTransient
	public Set<Ingredient> getIngredients() {
		return ingredients;
	}

	protected void setIngredients(Set<Ingredient> ingredients) {
		this.ingredients = ingredients;
	}

	@JsonbTransient
	public Set<Document> getIllustrations() {
		return illustrations;
	}

	protected void setIllustrations(Set<Document> illustrations) {
		this.illustrations = illustrations;
	}

	@JsonbProperty
	public Recipe.Category getCategory() {
		return category;
	}

	public void setCategory(Recipe.Category category) {
		this.category = category;
	}

	@JsonbProperty
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@JsonbProperty
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@JsonbProperty
	public String getInstruction() {
		return instruction;
	}

	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}

	@JsonbProperty
	public Restriction getRestriction() {
		// Java.Collection.Stream API
		return this.ingredients.stream().map(Ingredient::getType).map(IngredientType::getRestriction).min(Comparator.naturalOrder()).orElse(Restriction.VEGAN);
				
//		if (ingredients.isEmpty()) {
//			return Restriction.NONE;
//		}
//
//		Restriction minRestriction = Restriction.VEGAN;
//		for (Ingredient ingredient : ingredients) {
//			Restriction ingredientRestriction = ingredient.getType().getRestriction();
//			if (ingredientRestriction.getOrdinal() < minRestriction.getOrdinal()) {
//				minRestriction = ingredientRestriction;
//			}
//		}
//		return minRestriction;
		

	}
	
}
