package edu.sb.cookbook.check;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import edu.sb.cookbook.persistence.Document;


/**
 * JPA sanity-check non-interactive text application for database schema "local_database".
 */
public class PersistenceSanityCheckApp {

	/**
	 * Application entry point.
	 * @param args the runtime arguments
	 */
	static public void main (final String[] args) {
		final EntityManagerFactory factory = Persistence.createEntityManagerFactory("local_database");
		final EntityManager entitymanager = factory.createEntityManager();

		final Document defaultAvatar = entitymanager.find(Document.class, 1L);
		if(defaultAvatar==null) throw new NullPointerException();
		System.out.println("Default avatar size: " + defaultAvatar.getContent().length + " Bytes");
	}
}
