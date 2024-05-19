package edu.sb.cookbook.persistence;

import java.util.Comparator;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

import edu.sb.tool.JsonProtectedPropertyStrategy;

@JsonbVisibility(JsonProtectedPropertyStrategy.class)
@Embeddable 
public class Address implements Comparable<Address>{
	
	static public final Comparator<Address> COMPARATOR = 
		Comparator
            .comparing(Address::getCountry)
            .thenComparing(Address::getCity)
            .thenComparing(Address::getStreet)
            .thenComparing(Address::getPostcode);

	@Size(max=15)
	@Column(nullable = true, updatable = true, length = 15)
	private String postcode;

	@Size(max=63)
	@Column(nullable = true, updatable = true, length = 63)
	private String street;

	@Size(max=63)
	@Column(nullable = true, updatable = true, length = 63)
	private String city;

	@Size(max=63)
	@Column(nullable = true, updatable = true, length = 63)
	private String country;

	@JsonbProperty
	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	@JsonbProperty
	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	@JsonbProperty
	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	@JsonbProperty
	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Override
    public int compareTo(Address other) {
        return COMPARATOR.compare(this, other);
    }

}
