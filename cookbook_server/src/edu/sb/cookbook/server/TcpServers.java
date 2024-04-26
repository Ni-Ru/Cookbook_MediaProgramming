package edu.sb.cookbook.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import edu.sb.tool.Copyright;


/**
 * Facade for TCP server related operations. Note that the required key store file for a given host can be generated using the
 * {@code keytool} JDK utility. The default passwords should be "changeit", and the syntax depends on the keystore type:<ul>
 * <li>pkcs12: keytool -genkey -v -alias tabernakel -keyalg RSA -validity 3652 -storetype pkcs12 -keystore keystore.p12</li>.
 * <li>jks: keytool -genkey -v -alias tabernakel -keyalg RSA -validity 3652 -storetype jks -keystore keystore.jks</li>.
 * </ul>
 */
@Copyright(year=2014, holders="Sascha Baumeister")
public class TcpServers {
	static private final char[] DEFAULT_KEYSTORE_PASSWORD = "changeit".toCharArray();
	static private final String CRYPTOGRAPHIC_PROTOCOL = "TLS";


	/**
	 * Prevents external instantiation.
	 */
	private TcpServers () {}


	/**
	 * Reads all remaining bytes from the given byte source, and writes them to the given byte sink. Returns the number of bytes
	 * copied, and closes neither source nor sink. Note that large copy buffers speed up processing, but consume more memory.
	 * Also note that {@link SocketException} is treated as a kind of EOF due to to other side terminating the stream.
	 * @param byteSource the byte source
	 * @param byteSink the byte sink
	 * @param bufferSize the buffer size, in number of bytes
	 * @return the number of bytes copied
	 * @throws NullPointerException if any of the given arguments is {@code null}
	 * @throws IllegalArgumentException if the given buffer size is negative
	 * @throws IOException if there is an I/O related problem
	 */
	static public long copy (final InputStream byteSource, final OutputStream byteSink, final int bufferSize) throws IOException {
		if (bufferSize <= 0) throw new IllegalArgumentException();
		final byte[] buffer = new byte[bufferSize];

		long bytesCopied = 0;
		try {
			for (int bytesRead = byteSource.read(buffer); bytesRead != -1; bytesRead = byteSource.read(buffer)) {
				byteSink.write(buffer, 0, bytesRead);
				bytesCopied += bytesRead;
			}
		} catch (final EOFException | SocketException exception) {
			// treat as EOF because a TCP stream has been closed asynchronously
		}
		return bytesCopied;
	}


	/**
	 * Returns the local address.
	 * @return the local address, or the loopback address if there is no local address
	 */
	static public InetAddress localAddress () {
		try {
			return InetAddress.getLocalHost();
		} catch (final UnknownHostException exception) {
			return InetAddress.getLoopbackAddress();
		}
	}


	/**
	 * Returns a new TLS context based on a JKS key store and the most recent supported transport layer security (TLS) version.
	 * @param keyStorePath the key store file path (jks for Java 8-, pkcs12 for Java 9+), or {@code null} for none
	 * @param keyRecoveryPassword the key recovery password, or {@code null} for "changeit"
	 * @param keyManagementPassword the key management password, or {@code null} for "changeit"
	 * @return the SSL context created, or {@code null} if no key store is passed
	 * @throws NoSuchFileException if the given key store file path is neither {@code null} nor representing a regular file
	 * @throws AccessDeniedException if key store file access is denied, if any of the certificates within the key store could
	 *         not be loaded, if there is a key recovery problem (like incorrect passwords), or if there is a key management
	 *         problem (like key expiration)
	 * @throws IOException if there is an I/O related problem
	 */
	static public SSLContext newTLSContext (final Path keyStorePath, final String keyRecoveryPassword, final String keyManagementPassword) throws NoSuchFileException, AccessDeniedException, IOException {
		if (keyStorePath == null) return null;
		if (!Files.isRegularFile(keyStorePath)) throw new NoSuchFileException(keyStorePath.toString());

		try {
			final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			try (InputStream byteSource = Files.newInputStream(keyStorePath)) {
				keyStore.load(byteSource, keyRecoveryPassword == null ? DEFAULT_KEYSTORE_PASSWORD : keyRecoveryPassword.toCharArray());
			} catch (final IOException exception) {
				if (exception.getCause() instanceof GeneralSecurityException) throw (GeneralSecurityException) exception.getCause();
				throw exception;
			}

			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, keyManagementPassword == null ? DEFAULT_KEYSTORE_PASSWORD : keyManagementPassword.toCharArray());

			final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);

			final SSLContext context = SSLContext.getInstance(CRYPTOGRAPHIC_PROTOCOL);
			context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
			return context;
		} catch (final GeneralSecurityException exception) {
			throw (AccessDeniedException) new AccessDeniedException(keyStorePath.toAbsolutePath().toString(), null, exception.getMessage()).initCause(exception);
		}
	}


	/**
	 * Returns a new HTTP server instance if the given key store path is {@code null}, otherwise a new HTTPS server instance.
	 * @param serviceAddress the service address
	 * @param keyStorePath the key store file path (jks for Java 8-, pkcs12 for Java 9+), or {@code null} for none
	 * @param keyRecoveryPassword the optional key recovery password, or {@code null} for "changeit"
	 * @param keyManagementPassword the optional key management password, or {@code null} for "changeit"
	 * @return the HTTPS server created
	 * @throws NullPointerException if the given service address is {@code null}
	 * @throws NoSuchFileException if the given key store file path is neither {@code null} nor representing a regular file
	 * @throws AccessDeniedException if key store file access is denied, if any of the certificates within the key store could
	 *         not be loaded, if there is a key recovery problem (like incorrect passwords), or if there is a key management
	 *         problem (like key expiration)
	 * @throws IOException if there is an I/O related problem
	 */
	static public HttpServer newHttpServer (final InetSocketAddress serviceAddress, final Path keyStorePath, final String keyRecoveryPassword, final String keyManagementPassword) throws NullPointerException, NoSuchFileException, AccessDeniedException, IOException {
		if (serviceAddress == null) throw new NullPointerException();
		if (keyStorePath == null) return HttpServer.create(serviceAddress, 0);

		final SSLContext context = newTLSContext(keyStorePath, keyRecoveryPassword, keyManagementPassword);
		final HttpsConfigurator configurator = new HttpsConfigurator(context) {
			public void configure (final HttpsParameters parameters) {
				final SSLEngine engine = this.getSSLContext().createSSLEngine(serviceAddress.getHostName(), serviceAddress.getPort());
				parameters.setWantClientAuth(false);
				parameters.setNeedClientAuth(false);
				parameters.setProtocols(engine.getEnabledProtocols());
				parameters.setCipherSuites(engine.getEnabledCipherSuites());

				super.configure(parameters);
			}
		};

		final HttpsServer server = HttpsServer.create(serviceAddress, 0);
		server.setHttpsConfigurator(configurator);
		return server;
	}
}