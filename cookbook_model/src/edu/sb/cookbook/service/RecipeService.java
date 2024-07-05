package edu.sb.cookbook.service;

import static edu.sb.cookbook.service.BasicAuthenticationReceiverFilter.REQUESTER_IDENTITY;

import java.util.Objects;

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
import edu.sb.cookbook.persistence.Recipe.Category;
import edu.sb.tool.RestJpaLifecycleProvider;

@Path("recipes")
public class RecipeService {
	static private final String QUERY_RECIPES = "SELECT r.identity FROM Recipe AS r WHERE "
	            + "(:minCreated is null or r.created >= :minCreated) AND "
	            + "(:maxCreated is null or r.created <= :maxCreated) AND "
	            + "(:minModified is null or r.modified >= :minModified) AND "
	            + "(:maxModified is null or r.modified <= :maxModified) AND "
	            + "(:category is null or r.category = :category) AND "
	            + "(:title is null or r.title = :title) AND "
	            + "(:descriptionFragment is null or r.description LIKE concat('%', :descriptionFragment, '%')) AND "
	            + "(:instructionFragment is null or r.instruction LIKE concat('%', :instructionFragment, '%')) AND "
	            + "(:minIngredientCount is null or size(r.ingredients) >= :minIngredientCount) AND "
	            + "(:maxIngredientCount is null or size(r.ingredients) <= :maxIngredientCount)";
		
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Recipe[] queryRecipe(
	        @QueryParam("resultOffset") @PositiveOrZero final Integer resultOffset,
	        @QueryParam("resultLimit") @PositiveOrZero final Integer resultLimit,
	        @QueryParam("minCreated") final Long minCreated,
			@QueryParam("maxCreated") final Long maxCreated,
			@QueryParam("minModified") final Long minModified,
			@QueryParam("maxModified") final Long maxModified,
	        @QueryParam("category") final Category category,
	        @QueryParam("title") final String title,
	        @QueryParam("description-fragment") final String descriptionFragment,
	        @QueryParam("instruction-fragment") final String instructionFragment,
	        @QueryParam("min-ingredient-count") @PositiveOrZero final Integer minIngredientCount,
	        @QueryParam("max-ingredient-count") @PositiveOrZero final Integer maxIngredientCount
	) {
	    final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
	
	    final TypedQuery<Long> query = entityManager.createQuery(QUERY_RECIPES, Long.class);
	    if (resultOffset != null) query.setFirstResult(resultOffset);
	    if (resultLimit != null) query.setMaxResults(resultLimit);
	
	    final Recipe[] recipes = query
	            .setParameter("minCreated", minCreated)
	            .setParameter("maxCreated", maxCreated)
	            .setParameter("minModified", minModified)
	            .setParameter("maxModified", maxModified)
	            .setParameter("category", category)
	            .setParameter("title", title)
	            .setParameter("descriptionFragment", descriptionFragment)
	            .setParameter("instructionFragment", instructionFragment)
	            .setParameter("minIngredientCount", minIngredientCount)
	            .setParameter("maxIngredientCount", maxIngredientCount)
	            .getResultList()
	            .stream()
	            .map(identity -> entityManager.find(Recipe.class, identity))
	            .filter(Objects::nonNull)
	            .sorted()
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
        final boolean isRequesterAdmin = requester.getGroup() == Group.ADMIN;
        // final boolean isTheOwner = requester == recipeTemplate.getOwner();

		final Recipe recipe;
		final Document avatar;
		
		if (insertMode) {
			recipe = new Recipe();
			recipe.setOwner(requester);
			avatar = entityManager.find(Document.class, recipeTemplate.getAvatar() == null ? 1L : recipeTemplate.getAvatar().getIdentity());
			if (avatar == null) throw new IllegalStateException("Database is inconsistent");
		} else {
			recipe = entityManager.find(Recipe.class, recipeTemplate.getIdentity());
			if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
			if (recipe.getOwner() != requester && !isRequesterAdmin) throw new ClientErrorException(Status.FORBIDDEN);
			avatar = recipeTemplate.getAvatar() == null ? recipe.getAvatar() : entityManager.find(Document.class, recipeTemplate.getAvatar().getIdentity());
			if (avatar == null) throw new ClientErrorException(Status.NOT_FOUND);
		}

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
		
		// second-level cache eviction if necessary
		final Cache secondLevelCache = entityManager.getEntityManagerFactory().getCache();
		if(insertMode) secondLevelCache.evict(Person.class, requester.getIdentity());

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
		final boolean isRequesterAdmin = requester.getGroup() == Group.ADMIN;
        final boolean isTheOwner = requester == recipe.getOwner();
		if (!isRequesterAdmin && !isTheOwner) throw new ClientErrorException(Status.FORBIDDEN);
		
		try {
			recipe.getIngredients().forEach(ingredient -> entityManager.remove(ingredient));
			entityManager.remove(recipe);
			entityManager.getTransaction().commit();
		} catch (final Exception e) {
			if (entityManager.getTransaction().isActive())
				entityManager.getTransaction().rollback();
			throw new ClientErrorException(Status.CONFLICT, e);
		} finally {
			entityManager.getTransaction().begin();
		}

		final Cache secondLevelCache = entityManager.getEntityManagerFactory().getCache();
		if(recipe.getOwner() != null) secondLevelCache.evict(Person.class, recipe.getOwner().getIdentity());

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
        
        final Document[] illustrations = recipe.getIllustrations().stream().sorted().toArray(Document[]::new);
        
        return illustrations;
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
        
        final Ingredient[] ingredients = recipe.getIngredients().stream().sorted().toArray(Ingredient[]::new);
        
        return ingredients;
    }
    
    
    @POST
    @Path("{id}/illustrations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
	public long addIllustration (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long recipeIdentity,
		@NotNull @Valid final Document illustrationTemplate
		
	) {
    	final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
    	final Person requester = entityManager.find(Person.class, requesterIdentity);
    	if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);
    	
        final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
        if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);

		final boolean isRequesterAdmin = requester.getGroup() == Group.ADMIN;
        final boolean isTheOwner = requester == recipe.getOwner();
		if (!isRequesterAdmin && !isTheOwner) throw new ClientErrorException(Status.FORBIDDEN);

		final Document illustration = entityManager.find(Document.class, illustrationTemplate.getIdentity());
		recipe.setModified(System.currentTimeMillis());
		recipe.getIllustrations().add(illustration);
		
		try {
			entityManager.flush();
			entityManager.getTransaction().commit();
		} catch (final Exception e) {
			if (entityManager.getTransaction().isActive())
				entityManager.getTransaction().rollback();
			throw new ClientErrorException(Status.CONFLICT, e);
		} finally {
			entityManager.getTransaction().begin();
		}
		
		final Cache secondLevelCache = entityManager.getEntityManagerFactory().getCache();
		secondLevelCache.evict(Recipe.class, recipe.getIdentity());
		
		return recipe.getIdentity();
	}
    
	
	@POST
    @Path("{id}/ingredients")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdateIngredient (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long recipeIdentity,
		@NotNull @Valid final Ingredient ingredientTemplate
		
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
    	final Person requester = entityManager.find(Person.class, requesterIdentity);
    	if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);
    	
        final Recipe recipe = entityManager.find(Recipe.class, recipeIdentity);
        if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);

		final boolean isRequesterAdmin = requester.getGroup() == Group.ADMIN;
        final boolean isTheOwner = requester == recipe.getOwner();
		if (!isRequesterAdmin && !isTheOwner) throw new ClientErrorException(Status.FORBIDDEN);

		final boolean insertMode = ingredientTemplate.getIdentity() == 0L;
		final Ingredient ingredient;
		if(insertMode) {
			ingredient = new Ingredient(recipe);
		} else {
			ingredient = entityManager.find(Ingredient.class, ingredientTemplate.getIdentity());
			if(ingredient == null) throw new ClientErrorException(Status.NOT_FOUND);
			if(ingredient.getRecipe() != recipe) throw new ClientErrorException(Status.BAD_REQUEST);
		}
		
		final IngredientType ingredientType = entityManager.find(IngredientType.class, ingredientTemplate.getType().getIdentity());
		if(ingredientType == null) throw new ClientErrorException(Status.NOT_FOUND);
		ingredient.setModified(System.currentTimeMillis());
		ingredient.setVersion(ingredientTemplate.getVersion());
		ingredient.setType(ingredientType);
		ingredient.setUnit(ingredientTemplate.getUnit());
		ingredient.setAmount(ingredientTemplate.getAmount());
		
		try {
			if(insertMode) entityManager.persist(ingredient);
			else entityManager.flush();
			entityManager.getTransaction().commit();
		} catch (final Exception e) {
			if (entityManager.getTransaction().isActive())
				entityManager.getTransaction().rollback();
			throw new ClientErrorException(Status.CONFLICT, e);
		} finally {
			entityManager.getTransaction().begin();
		}
		
		final Cache secondLevelCache = entityManager.getEntityManagerFactory().getCache();
		if(insertMode) secondLevelCache.evict(Recipe.class, recipe.getIdentity());
		
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
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
		final Document illustration = entityManager.find(Document.class, illustrationIdentity);
		if (illustration == null) throw new ClientErrorException(Status.NOT_FOUND);
		
		final boolean isRequesterAdmin = requester.getGroup() == Group.ADMIN;
        final boolean isTheOwner = requester == recipe.getOwner();
		if (!isRequesterAdmin && !isTheOwner) throw new ClientErrorException(Status.FORBIDDEN);

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

		final Cache secondLevelCache = entityManager.getEntityManagerFactory().getCache();
		secondLevelCache.evict(Recipe.class, recipe.getIdentity());
				
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
		if (recipe == null) throw new ClientErrorException(Status.NOT_FOUND);
		final Ingredient ingredient = entityManager.find(Ingredient.class, ingredientIdentity);
		if (ingredient == null) throw new ClientErrorException(Status.NOT_FOUND);
		if(ingredient.getRecipe() != recipe) throw new ClientErrorException(Status.BAD_REQUEST);
		
		final boolean isRequesterAdmin = requester.getGroup() == Group.ADMIN;
        final boolean isTheOwner = requester == recipe.getOwner();
		if (!isRequesterAdmin && !isTheOwner) throw new ClientErrorException(Status.FORBIDDEN);

		try {
			entityManager.remove(ingredient);
			entityManager.getTransaction().commit();
		} catch (final Exception e) {
			if (entityManager.getTransaction().isActive())
				entityManager.getTransaction().rollback();
			throw new ClientErrorException(Status.CONFLICT, e);
		} finally {
			entityManager.getTransaction().begin();
		}
		
		final Cache secondLevelCache = entityManager.getEntityManagerFactory().getCache();
		secondLevelCache.evict(Recipe.class, recipe.getIdentity());

		return recipe.getIdentity();
	}
	
}
