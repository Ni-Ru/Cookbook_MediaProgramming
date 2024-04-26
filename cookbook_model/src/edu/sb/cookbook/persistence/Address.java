package edu.sb.cookbook.persistence;

import java.util.Comparator;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

@Embeddable
public class Address implements Comparable<Address>{

	@Size(max=15)
	@Column(nullable = true, updatable = true)
	private String postcode;

	@Column(nullable = true, updatable = true)
	private String street;

	@Column(nullable = true, updatable = true)
	private String city;

	@Column(nullable = true, updatable = true)
	private String country;

	public String getPostcode() {
		return postcode;
	}

	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	@Override
    public int compareTo(Address other) {
        return Comparator
                .comparing(Address::getCountry)
                .thenComparing(Address::getCity)
                .thenComparing(Address::getStreet)
                .thenComparing(Address::getPostcode)
                .compare(this, other);
    }

}
