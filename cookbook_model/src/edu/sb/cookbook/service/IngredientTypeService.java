package edu.sb.cookbook.service;

import static edu.sb.cookbook.service.BasicAuthenticationReceiverFilter.REQUESTER_IDENTITY;

import java.util.Arrays;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import edu.sb.cookbook.persistence.Document;
import edu.sb.cookbook.persistence.Ingredient;
import edu.sb.cookbook.persistence.IngredientType;
import edu.sb.cookbook.persistence.Person;
import edu.sb.cookbook.persistence.Person.Group;
import edu.sb.cookbook.persistence.Recipe;
import edu.sb.cookbook.persistence.Restriction;
import edu.sb.tool.RestJpaLifecycleProvider;


/**
 * Service class for ingredient types.
 */
@Path("ingredient-types")
public class IngredientTypeService {

	static private final String QUERY_TYPES = "select t.identity from IngredientType as t where "
		+ "(:minCreated is null or t.created >= :minCreated) and "
		+ "(:maxCreated is null or t.created <= :maxCreated) and "
		+ "(:minModified is null or t.modified >= :minModified) and "
		+ "(:maxModified is null or t.modified <= :maxModified) and "
		+ "(:aliasFragment is null or t.alias like concat('%', :aliasFragment, '%')) and "
		+ "(:descriptionFragment is null or t.description like concat('%', :descriptionFragment, '%')) and "
		+ "(t.restriction in :restrictions) and "
		+ "(:owned is null or t.owner is not null = :owned)";


	/**
	 * HTTP Signature: GET ingredient-types IN: - OUT: application/json
	 * @param resultOffset the result offset, or null for none
	 * @param resultSize the maximum result size, or null for none
	 * @param minCreated the minimum creation timestamp, or null for none
	 * @param maxCreated the maximum creation timestamp, or null for none
	 * @param minModified the minimum modification timestamp, or null for none
	 * @param maxModified the maximum modification timestamp, or null for none
	 * @param aliasFragment the alias fragment, or null for none
	 * @param descriptionFragment the description fragment, or null for none
	 * @param restriction the restriction value, or null for none
	 * @param owned whether or not ingredients have an owner, or null for none
	 * @return the matching ingredient types
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public IngredientType[] queryIngredientTypes (
		@QueryParam("result-offset") @PositiveOrZero final Integer resultOffset,
		@QueryParam("result-size") @PositiveOrZero final Integer resultSize,
		@QueryParam("min-created") final Long minCreated,
		@QueryParam("max-created") final Long maxCreated,
		@QueryParam("min-modified") final Long minModified,
		@QueryParam("max-modified") final Long maxModified,
		@QueryParam("alias-fragment") final String aliasFragment,
		@QueryParam("description-fragment") final String descriptionFragment,
		@QueryParam("restriction") final Restriction restriction,
		@QueryParam("owned") final Boolean owned
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Restriction[] restrictions = Restriction.values();

		final TypedQuery<Long> query = entityManager.createQuery(QUERY_TYPES, Long.class);
		if (resultOffset != null) query.setFirstResult(resultOffset);
		if (resultSize != null) query.setMaxResults(resultSize);
		query.setParameter("minCreated", minCreated);
		query.setParameter("maxCreated", maxCreated);
		query.setParameter("minModified", minModified);
		query.setParameter("maxModified", maxModified);
		query.setParameter("aliasFragment", aliasFragment);
		query.setParameter("descriptionFragment", descriptionFragment);
		query.setParameter("restrictions", Arrays.asList(restriction == null ? restrictions : Arrays.copyOfRange(restrictions, restriction.ordinal(), restrictions.length)));
		query.setParameter("owned", owned);

		final IngredientType[] types = query
			.getResultList()
			.stream()
			.map(identity -> entityManager.find(IngredientType.class, identity))
			.filter(type -> type != null)
			.sorted()
			.toArray(IngredientType[]::new);

		return types;
	}


	/**
	 * HTTP Signature: POST ingredient-types IN: application/json OUT: text/plain
	 * @param requesterIdentity the requester identity
	 * @param ingredientTypeTemplate the ingredient type template
	 * @return the ingredient type identity
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("{id}/ingredients")
	@Produces(MediaType.TEXT_PLAIN)
	public long createOrUpdateIngredientType (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@NotNull @Valid final IngredientType ingredientTypeTemplate
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person requester = entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);
		final boolean insertMode = ingredientTypeTemplate.getIdentity() == 0L;

		final IngredientType ingredientType;
		final Document avatar;
		if (insertMode) {
			ingredientType = new IngredientType();
			ingredientType.setOwner(requester);
			avatar = entityManager.find(Document.class, ingredientTypeTemplate.getAvatar() == null ? 1L : ingredientTypeTemplate.getAvatar().getIdentity());
		} else {
			ingredientType = entityManager.find(IngredientType.class, ingredientTypeTemplate.getIdentity());
			if (ingredientType == null) throw new ClientErrorException(Status.NOT_FOUND);
			avatar = ingredientTypeTemplate.getAvatar() == null ? ingredientType.getAvatar() : entityManager.find(Document.class, ingredientTypeTemplate.getAvatar().getIdentity());
		}

		if (requester.getGroup() != Group.ADMIN && ingredientType.getOwner() != requester) throw new ClientErrorException(Status.FORBIDDEN);
		if (avatar == null) throw new ClientErrorException(Status.NOT_FOUND);

		ingredientType.setModified(System.currentTimeMillis());
		ingredientType.setVersion(ingredientTypeTemplate.getVersion());
		ingredientType.setAlias(ingredientTypeTemplate.getAlias());
		ingredientType.setDescription(ingredientTypeTemplate.getDescription());
		ingredientType.setRestriction(ingredientTypeTemplate.getRestriction());
		ingredientType.setAvatar(avatar);

		try {
			if (insertMode)
				entityManager.persist(ingredientType);
			else
				entityManager.flush();

			entityManager.getTransaction().commit();
		} catch (final Exception e) {
			if (entityManager.getTransaction().isActive())
				entityManager.getTransaction().rollback();
			throw new ClientErrorException(Status.CONFLICT, e);
		} finally {
			entityManager.getTransaction().begin();
		}

		// 2nd level cache eviction if necessary
		final Cache secondLevelCache = entityManager.getEntityManagerFactory().getCache();
		if (insertMode) secondLevelCache.evict(Person.class, requester.getIdentity());

		return ingredientType.getIdentity();
	}


	/**
	 * HTTP Signature: DELETE ingredient-types/{id} IN: - OUT: text/plain
	 * @param requesterIdentity the requester identity
	 * @param ingredientTypeIdentity the ingredient type identity
	 * @return the ingredient type identity
	 */
	@DELETE
	@Path("{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public long removeIngredientType (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long ingredientTypeIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person requester = entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

		final IngredientType ingredientType = entityManager.find(IngredientType.class, ingredientTypeIdentity);
		if (ingredientType == null) throw new ClientErrorException(Status.NOT_FOUND);
		if (requester.getGroup() != Group.ADMIN && requester != ingredientType.getOwner()) throw new ClientErrorException(Status.FORBIDDEN);

		try {
			entityManager.remove(ingredientType);

			entityManager.getTransaction().commit();
		} catch (final Exception e) {
			if (entityManager.getTransaction().isActive())
				entityManager.getTransaction().rollback();
			throw new ClientErrorException(Status.CONFLICT, e);
		} finally {
			entityManager.getTransaction().begin();
		}

		// 2nd level cache eviction if necessary
		final Cache secondLevelCache = entityManager.getEntityManagerFactory().getCache();
		secondLevelCache.evict(Person.class, requester.getIdentity());
		secondLevelCache.evict(Ingredient.class);
		secondLevelCache.evict(Recipe.class);

		return ingredientType.getIdentity();
	}


	/**
	 * HTTP Signature: GET ingredient-types/{id} IN: - OUT: application/json
	 * @param ingredientTypeIdentity the ingredient type identity
	 * @return the ingredient type
	 */
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public IngredientType findIngredientType (
		@PathParam("id") @Positive final long ingredientTypeIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final IngredientType ingredientType = entityManager.find(IngredientType.class, ingredientTypeIdentity);
		if (ingredientType == null) throw new ClientErrorException(Status.NOT_FOUND);

		return ingredientType;
	}
}