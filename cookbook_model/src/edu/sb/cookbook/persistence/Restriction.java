package edu.sb.cookbook.persistence;

public enum Restriction {
	NONE, PESCATARIAN, LACTO_OVO_VEGETARIAN, LACTO_VEGETARIAN, VEGAN;
	
	public String getName() {
		return this.name();
	}
	
	public int getOrdinal() {
		return this.ordinal();
	}
	
}