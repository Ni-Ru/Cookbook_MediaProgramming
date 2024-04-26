package edu.sb.cookbook.persistence;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

import edu.sb.tool.HashCodes;

import java.util.HashSet;

@Entity
@Table(schema = "cookbook", name="Person")
@PrimaryKeyJoinColumn(name="personIdentity")
public class Person{
	
	@NotNull
	@ManyToOne @JoinColumn
	private Document avatar;
	
	@NotNull
	@OneToMany(mappedBy = "owner", cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE})
    private Set<Recipe> recipes;
	
	@NotNull
	@OneToMany(mappedBy = "owner", cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE})
    private Set<IngredientType> ingredientTypes;
    
	@Email
	@Column(nullable = true, updatable = true)
    private String email;
    
    @NotNull
    @Column(nullable = false, updatable = true)
    private String passwordHash;
    
    @NotNull @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = true)
    private Person.Group group;
    
    @NotNull
    @Embedded
    @OneToOne @JoinColumn
    private Name name;
    
    @NotNull
    @Embedded
    @OneToOne @JoinColumn
    private Address address;
    
    @NotNull
    @Column(nullable = false, updatable = false)
    private Set<String> phone;
    
    static private final String DEFAULT_PASSWORD_HASH = HashCodes.sha2HashText(256, "changeit");
	
	public Person() {
        this.recipes = new HashSet<Recipe>();
        this.ingredientTypes = new HashSet<IngredientType>();
        this.group = Person.Group.USER;
        this.phone = new HashSet<String>();
        this.passwordHash = DEFAULT_PASSWORD_HASH;
    }
	
	static public enum Group {
		ADMIN,
		USER;
		
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

	public Set<Recipe> getRecipes() {
		return recipes;
	}

	protected void setRecipes(Set<Recipe> recipes) {
		this.recipes = recipes;
	}

	public Set<IngredientType> getIngredientTypes() {
		return ingredientTypes;
	}

	protected void setIngredientTypes(Set<IngredientType> ingredientTypes) {
		this.ingredientTypes = ingredientTypes;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Person.Group getGroup() {
		return group;
	}

	public void setGroup(Person.Group group) {
		this.group = group;
	}

	public Name getName() {
		return name;
	}

	protected void setName(Name name) {
		this.name = name;
	}

	public Address getAddress() {
		return address;
	}

	protected void setAddress(Address address) {
		this.address = address;
	}

	public Set<String> getPhone() {
		return phone;
	}

	protected void setPhone(Set<String> phone) {
		this.phone = phone;
	}
}
