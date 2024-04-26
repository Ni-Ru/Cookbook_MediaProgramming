package edu.sb.cookbook.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;



/**
 * HTTP server application facade serving both external and embedded file content.
 */
@edu.sb.tool.Copyright(year=2014, holders="Sascha Baumeister")
public final class HttpServer {

	/**
	 * Prevents external instantiation.
	 */
	private HttpServer () {}


	/**
	 * Application entry point. The given arguments are expected to be an optional service port (default is 8001), an optional
	 * resource directory path (default is the VM temp directory), and an optional key store file path (default is null).
	 * @param args the runtime arguments
	 * @throws IllegalArgumentException if the given port is not a valid port number
	 * @throws NotDirectoryException if the given directory path is not a directory
	 * @throws NoSuchFileException if the given key store file path is neither {@code null} nor representing a regular file
	 * @throws AccessDeniedException if key store file access is denied, or if any of the certificates within the key store
	 *         could not be loaded, if there is a key recovery problem (like incorrect passwords), or if there is a key
	 *         management problem (like key expiration)
	 * @throws IOException if there is an I/O related problem
	 */
	static public void main (final String[] args) throws IllegalArgumentException, NotDirectoryException, NoSuchFileException, AccessDeniedException, IOException {
		final int servicePort = args.length > 0 ? Integer.parseInt(args[0]) : 8001;
		final Path resourceDirectory = Paths.get(args.length > 1 ? args[1] : "").toAbsolutePath();
		final Path keyStorePath = args.length > 2 ? Paths.get(args[2]).toAbsolutePath() : null;
		final String keyRecoveryPassword = args.length > 3 ? args[3] : "changeit";
		final String keyManagementPassword = args.length > 4 ? args[4] : keyRecoveryPassword;

		final boolean transportLayerSecurity = keyStorePath != null;
		final InetSocketAddress serviceAddress = new InetSocketAddress(TcpServers.localAddress(), servicePort);
		final com.sun.net.httpserver.HttpServer server = TcpServers.newHttpServer(serviceAddress, keyStorePath, keyRecoveryPassword, keyManagementPassword);

		final HttpResourceHandler internalFileHandler = new HttpResourceHandler("/internal");
		final HttpResourceHandler externalFileHandler = new HttpResourceHandler("/external", resourceDirectory);
		server.createContext(internalFileHandler.getContextPath(), internalFileHandler);
		server.createContext(externalFileHandler.getContextPath(), externalFileHandler);

		server.start();
		try {
			final String origin = String.format("%s://%s:%s/", transportLayerSecurity ? "https" : "http", serviceAddress.getHostName(), serviceAddress.getPort());
			System.out.format("Web server running on origin %s, enter \"quit\" to stop.\n", origin);
			System.out.format("Service path \"%s\" is configured for class loader access.\n", internalFileHandler.getContextPath());
			System.out.format("Service path \"%s\" is configured for file system access within \"%s\".\n", externalFileHandler.getContextPath(), resourceDirectory);
			final BufferedReader charSource = new BufferedReader(new InputStreamReader(System.in));
			while (!"quit".equals(charSource.readLine()));
		} finally {
			server.stop(0);
		}
	}
}