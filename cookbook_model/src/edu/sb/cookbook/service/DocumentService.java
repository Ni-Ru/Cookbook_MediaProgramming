package edu.sb.cookbook.service;

import static edu.sb.cookbook.service.BasicAuthenticationReceiverFilter.REQUESTER_IDENTITY;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import edu.sb.cookbook.persistence.Document;
import edu.sb.cookbook.persistence.Person;
import edu.sb.cookbook.persistence.Person.Group;
import edu.sb.tool.HashCodes;
import edu.sb.tool.RestJpaLifecycleProvider;


/**
 * Service class for documents.
 * Note that in MySql and MariaDB the predefined limit for binary content is 64MB.
 * To change this, this key/value association has to be altered:
 * max_allowed_packet=64M to max_allowed_packet=1G
 */
@Path("documents")
public class DocumentService {
	static private final String FIND_BY_HASH = "select d from Document as d where d.hash = :hash";
	static private final String FILTER_QUERY = "select d.identity from Document as d where "
		+ "(:minCreated is null or d.created >= :minCreated) and "
		+ "(:maxCreated is null or d.created <= :maxCreated) and "
		+ "(:minModified is null or d.modified >= :minModified) and "
		+ "(:maxModified is null or d.modified <= :maxModified) and "
		+ "(:minSize is null or length(d.content) >= :minSize) and "
		+ "(:maxSize is null or length(d.content) <= :maxSize) and "
		+ "(:hash is null or d.hash = :hash) and "
		+ "(:typeFragment is null or d.type like concat('%', :typeFragment, '%')) and "
		+ "(:descriptionFragment is null or d.description like concat('%', :descriptionFragment, '%'))";


	/**
	 * HTTP Signature: GET documents IN: - OUT: application/json
	 * @param resultOffset the result offset, or null for none
	 * @param resultSize the maximum result size, or null for none
	 * @param minCreated the minimum creation timestamp, or null for none
	 * @param maxCreated the maximum creation timestamp, or null for none
	 * @param minModified the minimum modification timestamp, or null for none
	 * @param maxModified the maximum modification timestamp, or null for none
	 * @param minSize the minimum size in bytes, or null for none
	 * @param maxSize the maximum size in bytes, or null for none
	 * @param hash the content hash code, or null for none
	 * @param type-fragment the content type fragment, or null for none
	 * @param description-fragment the content description fragment, or null for none
	 * @return the matching documents
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Document[] queryDocuments (
		@QueryParam("result-offset") @PositiveOrZero final Integer resultOffset,
		@QueryParam("result-limit") @PositiveOrZero final Integer resultLimit,
		@QueryParam("min-created") final Integer minCreated,
		@QueryParam("max-created") final Integer maxCreated,
		@QueryParam("min-modified") final Integer minModified,
		@QueryParam("max-modified") final Integer maxModified,
		@QueryParam("min-size") @PositiveOrZero final Integer minSize,
		@QueryParam("max-size") @PositiveOrZero final Integer maxSize,
		@QueryParam("hash") @Size(min=64, max=64) final String hash,
		@QueryParam("type-fragment") @Size(min=1) final String typeFragment,
		@QueryParam("description-fragment") @Size(min=1) final String descriptionFragment
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final TypedQuery<Long> query = entityManager.createQuery(FILTER_QUERY, Long.class);
		if (resultOffset != null) query.setFirstResult(resultOffset);
		if (resultLimit != null) query.setMaxResults(resultLimit);

		final Document[] documents = query
			.setParameter("minCreated", minCreated)
			.setParameter("maxCreated", maxCreated)
			.setParameter("minModified", minModified)
			.setParameter("maxModified", maxModified)
			.setParameter("minSize", minSize)
			.setParameter("maxSize", maxSize)
			.setParameter("hash", hash)
			.setParameter("typeFragment", typeFragment)
			.setParameter("descriptionFragment", descriptionFragment)
			.getResultList()
			.stream()
			.map(identity -> entityManager.find(Document.class, identity))
			.filter(document -> document != null)
			.sorted()
			.toArray(Document[]::new);

		return documents;
	}


	/**
	 * HTTP Signature: POST documents IN: * OUT: text/plain
	 * @param requesterIdentity the requester identity
	 * @param documentType the document type
	 * @param documentContent the document content
	 * @return the document identity
	 */
	@POST
	@Consumes({ "image/*", "audio/*", "video/*", "text/*", "application/pdf" })
	@Produces(MediaType.TEXT_PLAIN)
	public long insertOrUpdateDocument (
		@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
		@HeaderParam(HttpHeaders.CONTENT_TYPE) @NotNull @Size(min=1, max=63) final String documentType,
		@QueryParam("description") @Size(min=1, max=127) final String documentDescription,
		@NotNull final byte[] documentContent
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Person requester = entityManager.find(Person.class, requesterIdentity);
		if (requester == null) throw new ClientErrorException(Status.FORBIDDEN);

		final Document document = entityManager
			.createQuery(FIND_BY_HASH, Document.class)
			.setParameter("hash", HashCodes.sha2HashText(256, documentContent))
			.getResultList()
			.stream()
			.findFirst()
			.orElseGet(() -> new Document(documentContent));

		if (document.getIdentity() == 0 || requester.getGroup() == Group.ADMIN) {
			document.setModified(System.currentTimeMillis());
			document.setType(documentType);
			document.setDescription(documentDescription);
		}

		try {
			if (document.getIdentity() == 0)
				entityManager.persist(document);
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

		return document.getIdentity();
	}


	/**
	 * HTTP Signature: GET documents/{id} IN: - OUT: *
	 * @param acceptableContentTypes the HTTP "Accept" request header value
	 * @param documentIdentity the document identity
	 * @return the HTTP response built from the document's type and content
	 */
	@GET
	@Path("{id}")
	@Produces({ "image/*", "audio/*", "video/*", "text/*", "application/pdf" })
	public Response findDocument (
		@HeaderParam(HttpHeaders.ACCEPT) @NotEmpty final String acceptableContentTypes,
		@PathParam("id") @Positive final long documentIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Document document = entityManager.find(Document.class, documentIdentity);
		if (document == null) throw new ClientErrorException(Status.NOT_FOUND);

		boolean compatible = false;
		for (final String acceptableContentType : acceptableContentTypes.split("\\s*,\\s*"))
			compatible |= isCompatible(document.getType(), acceptableContentType);
		if (!compatible) throw new ClientErrorException(Status.NOT_ACCEPTABLE);

		return Response.ok(document.getContent(), document.getType()).build();
	}


	/**
	 * HTTP Signature: GET documents/{id} IN: - OUT: application/json
	 * @param documentIdentity the document identity
	 * @return the document
	 */
	@GET
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Document findDocument (
		@PathParam("id") @Positive final long documentIdentity
	) {
		final EntityManager entityManager = RestJpaLifecycleProvider.entityManager("local_database");
		final Document document = entityManager.find(Document.class, documentIdentity);
		if (document == null) throw new ClientErrorException(Status.NOT_FOUND);

		return document;
	}


	/**
	 * Returns whether or not the given content types are compatible to each other.
	 * @param leftContentType the left content type
	 * @param rightContentType the right content type
	 * @return true if both content types are compatible, false otherwise
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if any of the given arguments is not a valid content type
	 * 			because it contains more or less that one slash character
	 */
	static private boolean isCompatible (String leftContentType, String rightContentType) throws NullPointerException, IllegalArgumentException {
		leftContentType = leftContentType.split(";")[0];
		rightContentType = rightContentType.split(";")[0];

		final String[] leftContentParts = leftContentType.split("/");
		final String[] rightContentParts = rightContentType.split("/");
		if (leftContentParts.length != 2 | rightContentParts.length != 2) throw new IllegalArgumentException();

		for (int index = 0; index < leftContentParts.length; ++index) {
			if ("*".equals(leftContentParts[index]) | "*".equals(rightContentParts[index])) continue;
			if (!leftContentParts[index].equals(rightContentParts[index])) return false;
		}

		return true;
	}
}
