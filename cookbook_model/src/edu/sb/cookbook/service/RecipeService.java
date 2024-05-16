package edu.sb.cookbook.service;

import static edu.sb.cookbook.service.BasicAuthenticationReceiverFilter.REQUESTER_IDENTITY;

import java.util.Comparator;

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
import edu.sb.cookbook.persistence.Person;
import edu.sb.cookbook.persistence.Person.Group;
import edu.sb.cookbook.persistence.Recipe;
import edu.sb.cookbook.persistence.Recipe.Category;
import edu.sb.tool.RestJpaLifecycleProvider;

public class RecipeService {
	static private final String QUERY_RECIPES = "SELECT r.identity FROM Recipe AS r WHERE "
	            + "(:min-created is null or r.created >= :min-created) AND "
	            + "(:max-created is null or r.created <= :max-created) AND "
	            + "(:min-modified is null or r.modified >= :min-modified) AND "
	            + "(:max-modified is null or r.modified <= :max-modified) AND "
	            + "(:category is null or r.category = :category) AND "
	            + "(:title is null or r.title = :title) AND "
	            + "(:description is null or r.description = :description) AND "
	            + "(:instruction is null or r.instruction = :instruction)";
		
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Recipe[] queryRecipe(
	        @QueryParam("resultOffset") @PositiveOrZero final Integer resultOffset,
	        @QueryParam("resultLimit") @PositiveOrZero final Integer resultLimit,
	        @QueryParam("min-created") final Long minCreated,
			@QueryParam("max-created") final Long maxCreated,
			@QueryParam("min-modified") final Long minModified,
			@QueryParam("max-modified") final Long maxModified,
	        @QueryParam("category") final Category category,
	        @QueryParam("title") final String title,
	        @QueryParam("description") final String description,
	        @QueryParam("instruction") final String instruction
	) {
	    final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
	
	    final TypedQuery<Long> query = entityManager.createQuery(QUERY_RECIPES, Long.class);
	    if (resultOffset != null) query.setFirstResult(resultOffset);
	    if (resultLimit != null) query.setMaxResults(resultLimit);
	
	    final Recipe[] recipes = query
	            .setParameter("min-created", minCreated)
	            .setParameter("max-created", maxCreated)
	            .setParameter("min-modified", minModified)
	            .setParameter("max-modified", maxModified)
	            .setParameter("category", category)
	            .setParameter("title", title)
	            .setParameter("description", description)
	            .setParameter("instruction", instruction)
	            .getResultList()
	            .stream()
	            .map(identity -> entityManager.find(Recipe.class, identity))
	            .filter(recipe -> recipe != null)
	            .sorted(Comparator.naturalOrder())
	            .toArray(Recipe[]::new);
	
	    return recipes;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long createOrUpdateRecipe (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@NotNull @Valid final Recipe recipeTemplate
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person requester = entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);
		final boolean insertMode = recipeTemplate.getIdentity() == 0L;

		final Recipe recipe;
		final Document avatar;
		if (insertMode) {
			recipe = new Recipe();
			recipe.setOwner(requester);
			avatar = entityManager.find(Document.class, recipeTemplate.getAvatar() == null ? 1L : recipeTemplate.getAvatar().getIdentity());
		} else {
			recipe = entityManager.find(Recipe.class, recipeTemplate.getIdentity());
			if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
			avatar = recipeTemplate.getAvatar() == null ? recipe.getAvatar() : entityManager.find(Document.class, recipeTemplate.getAvatar().getIdentity());
		}

		if (requester.getGroup() != Group.ADMIN && recipe.getOwner() != requester) throw new ClientErrorException(Status.FORBIDDEN);
		if (avatar == null) throw new ClientErrorException(Status.NOT_FOUND);

		recipe.setModified(System.currentTimeMillis());
		recipe.setVersion(recipeTemplate.getVersion());
		recipe.setDescription(recipeTemplate.getDescription());
		recipe.setCategory(recipeTemplate.getCategory());
		recipe.setInstruction(recipeTemplate.getInstruction());
		recipe.setTitle(recipeTemplate.getTitle());
		recipe.setAvatar(avatar);

		try {
			if (insertMode)
				entityManager.persist(recipe);
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

		return recipe.getIdentity();
	}
	
	@DELETE
	@Path("{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public long removeRecipe (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long recipeIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person requester = entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

		final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
		if (requester.getGroup() != Group.ADMIN && requester != recipe.getOwner()) throw new ClientErrorException(Status.FORBIDDEN);

		try {
			entityManager.remove(recipe);

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

		return recipe.getIdentity();
	}
	
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Recipe findRecipe (
		@PathParam("id") @Positive final long recipeIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);

		return recipe;
	}
	
	@GET
    @Path("{id}/illustrations")
    @Produces(MediaType.APPLICATION_JSON)
    public Document[] findIllustrations(
            @PathParam("id") @Positive final long recipeIdentity
    ) {
        final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
        final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
        if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
        return recipe.getIllustrations().stream().sorted().toArray(Document[]::new);
    }
    
    @GET
    @Path("{id}/ingredients")
    @Produces(MediaType.APPLICATION_JSON)
    public Ingredient[] findIngredients(
            @PathParam("id") @Positive final long recipeIdentity
    ) {
        final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
        final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
        if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
        return recipe.getIngredients().stream().sorted().toArray(Ingredient[]::new);
    }
    
    @POST
    @Path("recipes/{id}/illustrations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
	public long addIllustration (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long recipeIdentity,
		@NotNull final Document illustration
		
	) {
    	final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
        final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
        if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
		final Person requester = entityManager.find(Person.class, requesterIdentity);

		if (requester.getGroup() != Group.ADMIN && recipe.getOwner() != requester) throw new ClientErrorException(Status.FORBIDDEN);

		recipe.getIllustrations().add(illustration);
		
		return recipe.getIdentity();
	}
	
	@POST
    @Path("recipes/{id}/ingredients")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
	public long addIngredients (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long recipeIdentity,
		@NotNull final Ingredient ingredient
		
	) {
    	final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
        final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
        if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
		final Person requester = entityManager.find(Person.class, requesterIdentity);

		if (requester.getGroup() != Group.ADMIN && recipe.getOwner() != requester) throw new ClientErrorException(Status.FORBIDDEN);

		recipe.getIngredients().add(ingredient);
		
		return recipe.getIdentity();
	}
	
	@DELETE
	@Path("{id1}/illustrations/{id2}")
	@Produces(MediaType.TEXT_PLAIN)
	public long removeIllustration (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id1") @Positive final long recipeIdentity,
		@PathParam("id2") @Positive final long illustrationIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person requester = entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

		final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
		final Document illustration = entityManager.find(Document.class, illustrationIdentity);
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
		if (requester.getGroup() != Group.ADMIN && requester != recipe.getOwner()) throw new ClientErrorException(Status.FORBIDDEN);

		try {
			recipe.getIllustrations().remove(illustration);

			entityManager.getTransaction().commit();
		} catch (final Exception e) {
			if (entityManager.getTransaction().isActive())
				entityManager.getTransaction().rollback();
			throw new ClientErrorException(Status.CONFLICT, e);
		} finally {
			entityManager.getTransaction().begin();
		}

		return recipe.getIdentity();
	}
	
	@DELETE
	@Path("{id1}/ingredients/{id2}")
	@Produces(MediaType.TEXT_PLAIN)
	public long removeIngredient (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id1") @Positive final long recipeIdentity,
		@PathParam("id2") @Positive final long ingredientIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person requester = entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

		final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
		final Ingredient ingredient = entityManager.find(Ingredient.class, ingredientIdentity);
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
		if (requester.getGroup() != Group.ADMIN && requester != recipe.getOwner()) throw new ClientErrorException(Status.FORBIDDEN);

		try {
			recipe.getIngredients().remove(ingredient);

			entityManager.getTransaction().commit();
		} catch (final Exception e) {
			if (entityManager.getTransaction().isActive())
				entityManager.getTransaction().rollback();
			throw new ClientErrorException(Status.CONFLICT, e);
		} finally {
			entityManager.getTransaction().begin();
		}

		return recipe.getIdentity();
	}
	
}
