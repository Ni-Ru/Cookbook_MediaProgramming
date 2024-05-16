package edu.sb.cookbook.persistence;

import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
import edu.sb.tool.JsonProtectedPropertyStrategy;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.persistence.annotations.CacheIndex;

import edu.sb.tool.HashCodes;

import java.util.Collections;
import java.util.HashSet;

@Entity
@Table(schema = "cookbook", name="Person")
@PrimaryKeyJoinColumn(name="personIdentity")
@DiscriminatorValue("Person")
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
public class Person extends BaseEntity{
	
	static public enum Group { ADMIN, USER }
    static private final String DEFAULT_PASSWORD_HASH = HashCodes.sha2HashText(256, "changeit");

	@NotNull @Email @Size(max=128)
	@Column(nullable = false, updatable = true, length = 128, unique = true)
	@CacheIndex(updateable = true)
    private String email;
    
    @NotNull @Size(min=64, max=64)
    @Column(nullable = false, updatable = true, length = 64)
    private String passwordHash;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = true, name = "groupAlias")
    private Person.Group group;
    
    @NotNull
    @Embedded
    private Name name;
    
    @NotNull
    @Embedded
    private Address address;
    
    @NotNull
    @ElementCollection
    @CollectionTable(
    		schema = "cookbook",
    		name="PhoneAssociation",
    		joinColumns = @JoinColumn(nullable = false, updatable = false, insertable = true, name = "personReference"),
    		uniqueConstraints = @UniqueConstraint(columnNames = {"personReference", "phone"})
    )
    @Column(nullable = false, updatable = false, insertable = true, length = 16, name = "phone")
    private Set<String> phones;
    
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false, updatable = true, name = "avatarReference")
    private Document avatar;
    
    // CascadeType.REMOVE ist nicht mit dabei weil on delete set null
    @NotNull
    @OneToMany(mappedBy = "owner", cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH})
    private Set<Recipe> recipes;
    
    // CascadeType.REMOVE ist nicht mit dabei weil on delete set null
    @NotNull
    @OneToMany(mappedBy = "owner", cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH})
    private Set<IngredientType> ingredientTypes;
   	
	public Person() {
		this.passwordHash = DEFAULT_PASSWORD_HASH;
		this.group = Person.Group.USER;
		this.name = new Name();
		this.address = new Address();
		this.phones = new HashSet<String>();
        this.recipes = Collections.emptySet();
        this.ingredientTypes = Collections.emptySet();
    }

	@JsonbProperty
	@JsonbTransient
	public Document getAvatar() {
		return avatar;
	}

	public void setAvatar(Document avatar) {
		this.avatar = avatar;
	}

	@JsonbProperty
	@JsonbTransient
	public Set<Recipe> getRecipes() {
		return recipes;
	}

	protected void setRecipes(Set<Recipe> recipes) {
		this.recipes = recipes;
	}

	@JsonbProperty
	@JsonbTransient
	public Set<IngredientType> getIngredientTypes() {
		return ingredientTypes;
	}

	protected void setIngredientTypes(Set<IngredientType> ingredientTypes) {
		this.ingredientTypes = ingredientTypes;
	}

	@JsonbProperty
	@JsonbTransient
	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@JsonbProperty
	@JsonbTransient
	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	@JsonbProperty
	@JsonbTransient
	public Person.Group getGroup() {
		return group;
	}

	public void setGroup(Person.Group group) {
		this.group = group;
	}

	@JsonbProperty
	@JsonbTransient
	public Name getName() {
		return name;
	}

	protected void setName(Name name) {
		this.name = name;
	}

	@JsonbProperty
	@JsonbTransient
	public Address getAddress() {
		return address;
	}

	protected void setAddress(Address address) {
		this.address = address;
	}

	@JsonbProperty
	@JsonbTransient
	public Set<String> getPhones() {
		return phones;
	}

	protected void setPhones(Set<String> phones) {
		this.phones = phones;
	}
}
