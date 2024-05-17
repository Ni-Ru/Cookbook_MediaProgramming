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
import edu.sb.cookbook.persistence.IngredientType;
import edu.sb.cookbook.persistence.Person;
import edu.sb.cookbook.persistence.Person.Group;
import edu.sb.cookbook.persistence.Recipe;
import edu.sb.tool.HashCodes;
import edu.sb.tool.RestJpaLifecycleProvider;

public class PersonService {
	static private final String QUERY_PEOPLE = "SELECT p.identity FROM Person AS p WHERE "
            + "(:min-created is null or p.created >= :min-created) AND "
            + "(:max-created is null or p.created <= :max-created) AND "
            + "(:min-modified is null or p.modified >= :min-modified) AND "
            + "(:max-modified is null or p.modified <= :max-modified) AND "
            + "(:email is null or p.email = :email) AND "
            + "(:group is null or p.group = :group) AND "
            + "(:givenName is null or p.name.given = :givenName) AND "
            + "(:familyName is null or p.name.family = :familyName) AND "
            + "(:title is null or p.name.title = :title) AND "
            + "(:country is null or p.address.country = :country) AND "
            + "(:postcode is null or p.address.postcode = :postcode) AND "
            + "(:street is null or p.address.street = :street) AND "
            + "(:city is null or p.address.city = :city)";
	
	static private final Comparator<Person> PERSON_COMPARATOR = Comparator
            .comparing(Person::getName)
            .thenComparing(Person::getEmail);

	@GET
    @Produces(MediaType.APPLICATION_JSON)
    public Person[] queryPeople(
            @QueryParam("resultOffset") @PositiveOrZero final Integer resultOffset,
            @QueryParam("resultLimit") @PositiveOrZero final Integer resultLimit,
            @QueryParam("min-created") final Long minCreated,
			@QueryParam("max-created") final Long maxCreated,
			@QueryParam("min-modified") final Long minModified,
			@QueryParam("max-modified") final Long maxModified,
            @QueryParam("email") final String email,
            @QueryParam("group") final Group group,
            @QueryParam("title") final String title,
            @QueryParam("givenName") final String givenName,
            @QueryParam("familyName") final String familyName,
            @QueryParam("street") final String street,
            @QueryParam("city") final String city,
            @QueryParam("country") final String country,
            @QueryParam("postcode") final String postcode
    ) {
        final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");

        final TypedQuery<Long> query = entityManager.createQuery(QUERY_PEOPLE, Long.class);
        if (resultOffset != null) query.setFirstResult(resultOffset);
        if (resultLimit != null) query.setMaxResults(resultLimit);

        final Person[] people = query
                .setParameter("min-created", minCreated)
                .setParameter("max-created", maxCreated)
                .setParameter("min-modified", minModified)
                .setParameter("max-modified", maxModified)
                .setParameter("email", email)
                .setParameter("group", group)
                .setParameter("title", title)
                .setParameter("givenName", givenName)
                .setParameter("familyName", familyName)
                .setParameter("street", street)
                .setParameter("city", city)
                .setParameter("country", country)
                .setParameter("postcode", postcode)
                .getResultList()
                .stream()
                .map(identity -> entityManager.find(Person.class, identity))
                .filter(person -> person != null)
                .sorted(PERSON_COMPARATOR)
                .toArray(Person[]::new);

        return people;
    }
    
	/**
	 * HTTP Signature: POST person IN: application/json OUT: text/plain
	 * @param requesterIdentity the requester identity
	 * @param personTemplate the person type template
	 * @return the person identity
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdatePerson (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@HeaderParam("X-Set-Password") final String password,
		@NotNull @Valid final Person personTemplate
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person requester = entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);
		
        final boolean insertMode = personTemplate.getIdentity() == 0;
        boolean isRequesterAdmin = requester.getGroup() == Group.ADMIN;
        boolean isThePerson = requester.getIdentity() == personTemplate.getIdentity();
        
        final Person person;
        final Document avatar;
        
		if (insertMode && isRequesterAdmin) {
			person = new Person();
			avatar = entityManager.find(Document.class, personTemplate.getAvatar() == null ? 1L : personTemplate.getAvatar().getIdentity());
		} else if (isRequesterAdmin || isThePerson) {
			person = entityManager.find(Person.class, personTemplate.getIdentity());
			if (person == null) throw new ClientErrorException(Status.NOT_FOUND);
			avatar = personTemplate.getAvatar() == null ? person.getAvatar() : entityManager.find(Document.class, personTemplate.getAvatar().getIdentity());
		} else {
			throw new ClientErrorException(Status.FORBIDDEN);
		}
		
		if (avatar == null) throw new ClientErrorException(Status.NOT_FOUND);

		person.setVersion(personTemplate.getVersion());
        person.setModified(System.currentTimeMillis());
        person.setEmail(personTemplate.getEmail());
        if (requester.getGroup() == Group.ADMIN) person.setGroup(personTemplate.getGroup());
        person.getName().setTitle(personTemplate.getName().getTitle());
        person.getName().setGiven(personTemplate.getName().getGiven());
        person.getName().setFamily(personTemplate.getName().getFamily());
        person.getAddress().setStreet(personTemplate.getAddress().getStreet());
        person.getAddress().setCity(personTemplate.getAddress().getCity());
        person.getAddress().setCountry(personTemplate.getAddress().getCountry());
        person.getAddress().setPostcode(personTemplate.getAddress().getPostcode());
        person.getPhones().retainAll(personTemplate.getPhones());
        person.getPhones().addAll(personTemplate.getPhones());
        person.setAvatar(avatar);
        if (password != null) person.setPasswordHash(HashCodes.sha2HashText(256, password));

		try {
			if (insertMode)
				entityManager.persist(person);
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

		return person.getIdentity();
	}
	
	/**
	 * HTTP Signature: DELETE person/{id} IN: - OUT: text/plain
	 * @param requesterIdentity the requester identity
	 * @param personIdentity the person identity
	 * @return the person identity
	 */
	@DELETE
	@Path("{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public long removePerson (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@PathParam("id") @Positive final long personIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person requester = entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

		final Person person = entityManager.find(Person.class, personIdentity);
		if (person == null) throw new ClientErrorException(Status.NOT_FOUND);
		boolean isRequesterAdmin = requester.getGroup() == Group.ADMIN;
        boolean isThePerson = requester.getIdentity() == personIdentity;
		if (!isRequesterAdmin || !isThePerson) throw new ClientErrorException(Status.FORBIDDEN);

		try {
		    person.getRecipes().forEach(recipe -> recipe.setOwner(null));
		    person.getIngredientTypes().forEach(type -> type.setOwner(null));
			entityManager.remove(person);
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

		return person.getIdentity();
	}
	
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Person findPerson (
		@PathParam("id") @Positive final long personIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person person = entityManager.find(Person.class, personIdentity);
		if (person == null) throw new ClientErrorException(Status.NOT_FOUND);

		return person;
	}
	
	@GET
	@Path("requester")
	@Produces(MediaType.APPLICATION_JSON)
	public Person findRequester(@HeaderParam("X-Requester-Identity") @Positive long requesterIdentity){
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person person = entityManager.find(Person.class, requesterIdentity);
		if (person == null) throw new ClientErrorException(Status.NOT_FOUND);
		
		return person;
	}
	
	@GET
    @Path("{id}/recipes")
    @Produces(MediaType.APPLICATION_JSON)
    public Recipe[] findRecipes(
            @PathParam("id") @Positive final long personIdentity
    ) {
        final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
        final Person person = entityManager.find(Person.class, personIdentity);
        if (person == null) throw new ClientErrorException(Status.NOT_FOUND);
        return person.getRecipes().stream().sorted().toArray(Recipe[]::new);
    }
    
    @GET
    @Path("{id}/ingredient-types")
    @Produces(MediaType.APPLICATION_JSON)
    public IngredientType[] findIngredientTypes(
            @PathParam("id") @Positive final long personIdentity
    ) {
        final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
        final Person person = entityManager.find(Person.class, personIdentity);
        if (person == null) throw new ClientErrorException(Status.NOT_FOUND);
        return person.getIngredientTypes().stream().sorted().toArray(IngredientType[]::new);
    }

}
