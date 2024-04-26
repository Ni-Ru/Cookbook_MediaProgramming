package edu.sb.cookbook.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import com.sun.net.httpserver.HttpServer;
import edu.sb.tool.Copyright;
import edu.sb.tool.RestJpaLifecycleProvider;


/**
 * <p>This facade is used within a Java-SE VM to programmatically deploy REST services. Programmatic server-startup is solely
 * required in Java-SE, as any Java-EE engine must ship a built-in HTTP server implementation combined with an XML-based
 * configuration. The server factory class used is Jersey-specific, while the HTTP server class used is JDK-specific.
 * There are plenty HTTP server types more suitable for production environments, such as Apache Tomcat, Grizzly, Simple, etc;
 * however, they all require a learning curve for successful configuration, while this design auto-configures itself as long as
 * the package of the service classes matches this class's package.</p>
 * <p>Note that for LAZY fetching of entities within <i>EclipseLink</i> (dynamic weaving), add this to the JVM start parameters:
 * -javaagent:[path]eclipselink.jar</p>
 */
@Copyright(year=2013, holders="Sascha Baumeister")
public class ApplicationContainer {

	/**
	 * Application entry point.
	 * @param args the runtime arguments (service port, resource directory, key store file, key recovery password and key
	 *        management password, all optional)
	 * @throws IllegalArgumentException if the given port is not a valid port number
	 * @throws NotDirectoryException if the given directory path is not a directory
	 * @throws NoSuchFileException if the given key store file path is neither {@code null} nor representing a regular file
	 * @throws AccessDeniedException if key store file access is denied, or if any of the certificates within the key store
	 *         could not be loaded, if there is a key recovery problem (like incorrect passwords), or if there is a key
	 *         management problem (like key expiration)
	 * @throws IOException if there is an I/O related problem
	 * @throws ClassNotFoundException if a configured class cannot be found
	 */
	static public void main (final String[] args) throws IllegalArgumentException, NotDirectoryException, NoSuchFileException, IOException, ClassNotFoundException {
		final int servicePort = args.length > 0 ? Integer.parseInt(args[0]) : 8001;
		final Path resourceDirectory = Paths.get(args.length > 1 ? args[1] : "").toAbsolutePath();
		final Path keyStorePath = args.length > 2 ? Paths.get(args[2]).toAbsolutePath() : null;
		final String keyRecoveryPassword = args.length > 3 ? args[3] : "changeit";
		final String keyManagementPassword = args.length > 4 ? args[4] : keyRecoveryPassword;

		// Create container for REST service, internal resource (class loader), and external resource (file system) access
		final ResourceConfig configuration = new ResourceConfig().register(RestJpaLifecycleProvider.open("local_database"));
		try (InputStream byteSource = ApplicationContainer.class.getResourceAsStream("components.properties")) {
			final Properties properties = new Properties();
			properties.load(byteSource);
			for (final Object key : properties.keySet()) configuration.register(Class.forName(key.toString()));
		}

		final URI serviceURI = URI.create((keyStorePath == null ? "http://" : "https://") + TcpServers.localAddress().getCanonicalHostName() + ":" + servicePort + "/services");
		final SSLContext context = TcpServers.newTLSContext(keyStorePath, keyRecoveryPassword, keyManagementPassword);
		if (context != null) context.createSSLEngine(serviceURI.getHost(), serviceURI.getPort());

		final HttpServer container = JdkHttpServerFactory.createHttpServer(serviceURI, configuration, context);
		final HttpResourceHandler internalFileHandler = new HttpResourceHandler("/internal");
		final HttpResourceHandler externalFileHandler = new HttpResourceHandler("/external", resourceDirectory);
		container.createContext(internalFileHandler.getContextPath(), internalFileHandler);
		container.createContext(externalFileHandler.getContextPath(), externalFileHandler);

		try {
			final String origin = String.format("%s://%s:%s", serviceURI.getScheme(), serviceURI.getHost(), serviceURI.getPort());
			System.out.format("Web container running on origin %s, enter \"quit\" to stop.\n", origin);
			System.out.format("Context path \"%s\" is configured for REST service access.\n", serviceURI.getPath());
			System.out.format("Context path \"%s\" is configured for class loader access.\n", internalFileHandler.getContextPath());
			System.out.format("Context path \"%s\" is configured for file system access within \"%s\".\n", externalFileHandler.getContextPath(), resourceDirectory);
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		} finally {
			container.stop(0);
		}
	}
}