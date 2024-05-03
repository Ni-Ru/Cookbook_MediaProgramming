package edu.sb.cookbook.check;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import edu.sb.cookbook.persistence.BaseEntity;
import edu.sb.cookbook.persistence.Document;
import edu.sb.cookbook.persistence.Ingredient;
import edu.sb.cookbook.persistence.IngredientType;
import edu.sb.cookbook.persistence.Person;
import edu.sb.cookbook.persistence.Recipe;


/**
 * JPA query-check non-interactive text application for database schema "local_database".
 */
public class PersistenceQueryCheckApp {
	static private final String PERSON_FILTER_QUERY = "select p from Person as p where "
		+ "(:forename is null or p.name.given = :forename) and "
		+ "(:surname is null or p.name.family = :surname) and "
		+ "(:city is null or p.address.city = :city) and "
		+ "(:country is null or p.address.country = :country)";
	static private final String DOCUMENT_QUERY = "select d from Document as d";
	static private final String INGREDIENT_TYPE_QUERY = "select t from IngredientType as t";
	static private final String RECIPE_QUERY = "select r from Recipe as r";
	static private final String RECIPE_INGREDIENT_QUERY = "select i from Ingredient as i";
	static private final String ENTITY_QUERY = "select e from BaseEntity as e";


	/**
	 * Application entry point.
	 * @param args the runtime arguments
	 */
	static public void main (final String[] args) {
		final String forename = args.length > 0 && !args[0].isBlank() ? args[0].trim() : null;
		final String surname = args.length > 1 && !args[1].isBlank() ? args[1].trim() : null;
		final String city = args.length > 2 && !args[2].isBlank() ? args[2].trim() : null;
		final String country = args.length > 3 && !args[3].isBlank() ? args[3].trim() : null;

		final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("local_database");
		try {
			processQuery(entityManagerFactory, forename, surname, city, country);
			System.out.println();
			processValidation(entityManagerFactory);
			System.out.println();
			processMarshaling(entityManagerFactory);
			System.out.println();
		} finally {
			entityManagerFactory.close();
		}
	}


	/**
	 * Processes entity queries.
	 * @param entityManagerFactory the entity manager factory
	 * @param forename the forename, or {@code null} for none
	 * @param surname the surname, or {@code null} for none
	 * @param city the city, or {@code null} for none
	 * @param country the country, or {@code null} for none
	 */
	static private void processQuery (final EntityManagerFactory entityManagerFactory, final String forename, final String surname, final String city, final String country) {
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			final Person person = entityManager.find(Person.class, 2L);

			System.out.println("\nPlayer 2:");
			if (person == null)
				System.out.println("There is no person with identity 2!");
			else
				System.out.println(person + " owning " + person.getRecipes().size() + " recipes, and " + person.getIngredientTypes().size() + " ingredient types");

			final TypedQuery<Person> query = entityManager.createQuery(PERSON_FILTER_QUERY, Person.class);
			query.setParameter("forename", forename);
			query.setParameter("surname", surname);
			query.setParameter("city", city);
			query.setParameter("country", country);

			System.out.println("\nMatching players:");
			final List<Person> players = query.getResultList();
			for (final Person player : players)
				System.out.println(player + " with " + player.getRecipes().size() + " recipes, and " + player.getIngredientTypes().size() + " ingredient types");

			System.out.println("\nAvailable documents:");
			for (final Document document : entityManager.createQuery(DOCUMENT_QUERY, Document.class).getResultList())
				System.out.println(document + " with size " + document.getContent().length + " bytes, type \"" + document.getType() + "\" and description \"" + document.getDescription() + "\"");

			System.out.println("\nAvailable ingredient types:");
			for (final IngredientType ingredientType : entityManager.createQuery(INGREDIENT_TYPE_QUERY, IngredientType.class).getResultList())
				System.out.println(ingredientType + " with alias \"" + ingredientType.getAlias() + "\"");

			System.out.println("\nAvailable recipes:");
			for (final Recipe recipe : entityManager.createQuery(RECIPE_QUERY, Recipe.class).getResultList())
				System.out.println(recipe + " titled \"" + recipe.getTitle() + "\" with " + recipe.getIngredients().size() + " ingredients and restriction " + recipe.getRestriction());

			System.out.println("\nAvailable recipe ingredients:");
			for (final Ingredient recipeIngredient : entityManager.createQuery(RECIPE_INGREDIENT_QUERY, Ingredient.class).getResultList())
				System.out.println(recipeIngredient + " associated with " + recipeIngredient.getRecipe());

			System.out.println("\n");
		} finally {
			entityManager.close();
		}
	}


	/**
	 * OUTLOOK: Process entity validation manually using a validator.
	 * @param entityManagerFactory the entity manager factory
	 */
	static private void processValidation (final EntityManagerFactory entityManagerFactory) {
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
			final Validator validator = validatorFactory.getValidator();

			final TypedQuery<BaseEntity> query = entityManager.createQuery(ENTITY_QUERY, BaseEntity.class);
			final List<BaseEntity> entities = query.getResultList();

			final Set<ConstraintViolation<BaseEntity>> constraintViolations = new HashSet<>();
			for (final BaseEntity entity : entities)
				constraintViolations.addAll(validator.validate(entity));

			System.out.println("\nValidation problems:");
			if (constraintViolations.isEmpty()) System.out.println("no problems!");
			for (final ConstraintViolation<BaseEntity> constraintViolation : constraintViolations)
				System.out.println(constraintViolation);

			System.out.println("\n");
		} finally {
			entityManager.close();
		}
	}


	/**
	 * OUTLOOK: Process entity JSON marshaling manually using a generator.
	 * @param entityManagerFactory the entity manager factory
	 */
	static private void processMarshaling (final EntityManagerFactory entityManagerFactory) {
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		try {
			final Jsonb jsonGenerator = JsonbBuilder.create();

			final TypedQuery<BaseEntity> query = entityManager.createQuery(ENTITY_QUERY, BaseEntity.class);
			final List<BaseEntity> entities = query.getResultList();

			System.out.println("\nJSON representation of all entities:");
			for (final BaseEntity entity : entities) {
				final String json = jsonGenerator.toJson(entity);
				System.out.println(json);
			}

			System.out.println("\n");
		} finally {
			entityManager.close();
		}
	}
}
